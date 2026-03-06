---
trigger: always_on
description: PostgreSQL + MyBatis Plus Integration - Configuration, TypeHandlers, SQL Optimization, Migration, Vavr Integration, Performance Tuning
tags: [postgresql, mybatis-plus, typehandler, jsonb, arrays, vavr, migration, performance, optimization]
positioning: comprehensive
prerequisites:
  - technology/database/D01-postgresql-basics.md
  - technology/database/09-mybatis-plus-core.md
  - technology/database/D02-postgresql-advanced.md
related_rules:
  - technology/functional/P03-vavr-mybatis-integration.md
last_updated: 2025-01-27
---

# PostgreSQL + MyBatis Plus Integration

**TL;DR**: Complete guide for PostgreSQL + MyBatis Plus integration covering configuration, custom TypeHandlers (JSONB/arrays), SQL optimization (prohibit SELECT *, pagination optimization), MySQL→PostgreSQL migration (type mapping, syntax differences), Vavr functional patterns (Option/Try/Either), and performance tuning (VACUUM, connection pool, GIN indexes).

**Prerequisites**:
- [05-postgresql-basics.md](./D01-postgresql-basics.md) - PostgreSQL Basic Rules
- [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - MyBatis Plus Core Configuration
- [05-postgresql-advanced.md](./D02-postgresql-advanced.md) - JSONB and Advanced Features

---

## 【Mandatory】DataSource Configuration

### Spring Boot Configuration

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3?useSSL=false&serverTimezone=Asia/Shanghai
    username: smartadmin
    password: SmartAdmin@2024
    hikari:
      maximum-pool-size: 20       # Maximum connections
      minimum-idle: 5             # Minimum idle connections
      connection-timeout: 30000   # Connection timeout (ms)
      idle-timeout: 600000        # Idle timeout (10 minutes)
      max-lifetime: 1800000       # Maximum lifetime (30 minutes)
      connection-test-query: SELECT 1

mybatis-plus:
  configuration:
    # PostgreSQL identifiers use double quotes
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # PostgreSQL primary key strategy
      id-type: AUTO  # PostgreSQL SERIAL/BIGSERIAL
      # Logical delete field
      logic-delete-field: deletedAt
      logic-delete-value: 'NOW()'
      logic-not-delete-value: 'NULL'
```

### Dependencies

```xml
<!-- PostgreSQL driver -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.5</version>
</dependency>

<!-- P6Spy supports PostgreSQL -->
<dependency>
    <groupId>p6spy</groupId>
    <artifactId>p6spy</artifactId>
    <version>3.9.1</version>
</dependency>
```

---

## 【Mandatory】JSONB Type Handling

### TypeHandler Implementation

```java
package com.example.config.typehandler;

import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import java.sql.*;

@MappedTypes({Object.class})
@MappedJdbcTypes(JdbcType.OTHER)
public class JsonbTypeHandler extends AbstractJsonTypeHandler<Object> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final Class<?> type;

    public JsonbTypeHandler(Class<?> type) {
        this.type = type;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    Object parameter, JdbcType jdbcType)
            throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(MAPPER.writeValueAsString(parameter));
        ps.setObject(i, jsonObject);
    }

    @Override
    public Object getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return parseJson(rs.getString(columnName));
    }

    @Override
    protected Object parse(String json) {
        return parseJson(json);
    }

    @Override
    protected String toJson(Object obj) {
        return MAPPER.writeValueAsString(obj);
    }

    private Object parseJson(String json) {
        if (json == null || json.isEmpty()) return null;
        return MAPPER.readValue(json, type);
    }
}
```

### Entity Usage

```java
@Data
@TableName(value = "t_order", autoResultMap = true)
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    // JSONB field
    @TableField(typeHandler = JsonbTypeHandler.class)
    private List<OrderItem> items;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> metadata;
}
```

### JSONB Queries

```xml
<!-- OrderMapper.xml -->
<select id="findByProductId" resultType="com.example.entity.Order">
    SELECT * FROM t_order
    WHERE items @> CAST('[{"productId": #{productId}}]' AS jsonb)
</select>

<select id="findByMetadata" resultType="com.example.entity.Order">
    SELECT * FROM t_order
    WHERE metadata @> CAST(#{condition} AS jsonb)
</select>
```

> **Reference**: For JSONB operators and indexes, see [05-postgresql-advanced.md](./D02-postgresql-advanced.md#jsonb-operations)

---

## 【Mandatory】Array Type Handling

### TypeHandler Implementation

```java
package com.example.config.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import java.sql.*;
import java.util.*;

@MappedTypes({List.class})
@MappedJdbcTypes(JdbcType.ARRAY)
public class StringArrayTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                    List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        Array array = ps.getConnection().createArrayOf("text", parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName)
            throws SQLException {
        return extractArray(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex)
            throws SQLException {
        return extractArray(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex)
            throws SQLException {
        return extractArray(cs.getArray(columnIndex));
    }

    private List<String> extractArray(Array array) throws SQLException {
        if (array == null) return null;
        return Arrays.asList((String[]) array.getArray());
    }
}
```

### Entity Usage

```java
@Data
@TableName(value = "t_article", autoResultMap = true)
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // TEXT[] array
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private List<String> tags;
}
```

### Array Queries

```xml
<!-- ArticleMapper.xml -->
<select id="findByTags" resultType="com.example.entity.Article">
    SELECT * FROM t_article
    WHERE tags &amp;&amp; ARRAY[
    <foreach collection="tags" item="tag" separator=",">
        #{tag}
    </foreach>
    ]::text[]
</select>
```

---

## 【Mandatory】Boolean Type Handling (SMALLINT Mapping)

### Background

PostgreSQL typically uses `SMALLINT` (2-byte integer) to store Boolean values for compatibility with external systems and legacy databases, while Java uses `Boolean` type. This mapping requires explicit type conversion to avoid runtime errors.

**Key Challenge**: PostgreSQL strictly distinguishes between types and refuses implicit `SMALLINT = BOOLEAN` conversion, unlike MySQL which allows `TINYINT(1) = BOOLEAN` auto-conversion.

### TypeHandler Implementation

SmartAdmin provides `BooleanToSmallintTypeHandler` for automatic conversion:

```java
package net.lab1024.sa.common.mybatis.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Boolean → SMALLINT TypeHandler
 * - Java Boolean → PostgreSQL SMALLINT
 * - true → 1, false → 0, null → null
 */
@MappedTypes(Boolean.class)
@MappedJdbcTypes(JdbcType.SMALLINT)
public class BooleanToSmallintTypeHandler extends BaseTypeHandler<Boolean> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Boolean parameter, JdbcType jdbcType) throws SQLException {
        ps.setShort(i, parameter ? (short) 1 : (short) 0);
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, String columnName) throws SQLException {
        short value = rs.getShort(columnName);
        return rs.wasNull() ? null : value == 1;
    }

    @Override
    public Boolean getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        short value = rs.getShort(columnIndex);
        return rs.wasNull() ? null : value == 1;
    }

    @Override
    public Boolean getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        short value = cs.getShort(columnIndex);
        return cs.wasNull() ? null : value == 1;
    }
}
```

### Entity Layer Usage

```java
package net.lab1024.sa.system.employee.domain.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler;

@Data
@TableName("t_employee")
public class EmployeeEntity {
    @TableId(type = IdType.AUTO)
    private Long employeeId;

    private String loginName;

    /** Disabled flag: 0=enabled, 1=disabled */
    @TableField(typeHandler = BooleanToSmallintTypeHandler.class)
    private Boolean disabledFlag;

    /** Deleted flag: 0=not deleted, 1=deleted */
    @TableField(typeHandler = BooleanToSmallintTypeHandler.class)
    private Boolean deletedFlag;
}
```

> **Note**: `@TableField(typeHandler=...)` only affects MyBatis Plus auto-generated CRUD methods. Hand-written XML SQL requires explicit typeHandler declaration (see below).

---

### 【CRITICAL】XML Mapper Parameter Binding Rules

**Rule 1: Mandatory typeHandler Declaration**

All Boolean parameters mapped to PostgreSQL SMALLINT columns **MUST** explicitly specify `typeHandler` attribute in XML Mapper files.

```xml
<!-- ❌ WRONG: Missing typeHandler (causes PSQLException) -->
<select id="getByLoginName">
    SELECT * FROM t_employee
    WHERE login_name = #{loginName}
      AND deleted_flag = #{deletedFlag}
</select>

<!-- ✅ CORRECT: Explicit typeHandler declaration -->
<select id="getByLoginName">
    SELECT * FROM t_employee
    WHERE login_name = #{loginName}
      AND deleted_flag = #{deletedFlag, jdbcType=SMALLINT,
          typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
</select>
```

**Rule 2: Common Patterns Requiring TypeHandler**

Apply typeHandler to Boolean parameters in these scenarios:

1. **WHERE Clause Queries**:
```xml
<select id="queryEmployee">
    SELECT * FROM t_employee
    <where>
        <if test="queryForm.deletedFlag != null">
            AND deleted_flag = #{queryForm.deletedFlag, jdbcType=SMALLINT,
                typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
        </if>
    </where>
</select>
```

2. **UPDATE SET Statements**:
```xml
<update id="updateDisableFlag">
    UPDATE t_employee
    SET disabled_flag = #{disabledFlag, jdbcType=SMALLINT,
        typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
    WHERE employee_id = #{employeeId}
</update>
```

3. **INSERT VALUES Statements**:
```xml
<insert id="insertEmployee">
    INSERT INTO t_employee (login_name, deleted_flag)
    VALUES (#{loginName}, #{deletedFlag, jdbcType=SMALLINT,
        typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler})
</insert>
```

4. **Collection Parameter Binding**:
```xml
<select id="batchQuery">
    SELECT * FROM t_employee
    WHERE employee_id IN
    <foreach collection="employeeIds" item="id" separator="," open="(" close=")">
        #{id}
    </foreach>
    AND deleted_flag = #{deletedFlag, jdbcType=SMALLINT,
        typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
</select>
```

**Rule 3: Error Diagnosis**

When PostgreSQL throws this error:

```
org.postgresql.util.PSQLException: ERROR: operator does not exist: smallint = boolean
Hint: No operator matches the given name and argument types. You might need to add explicit type casts.
Position: 111

SQL: SELECT * FROM t_employee WHERE deleted_flag = ?
Parameters: false(Boolean)
```

**Diagnosis Steps**:
1. Locate the SQL line number (Position: 111 → approximately character 111 in SQL)
2. Find the parameter binding (`deleted_flag = ?`)
3. Check if `typeHandler=BooleanToSmallintTypeHandler` is present
4. If missing → Add typeHandler attribute

**Rule 4: Validation Commands**

**Pre-commit Validation** (planned in follow-up improvements):
```bash
# Validate all Boolean parameters in XML Mapper files
bash .claude/scripts/validate-mybatis-boolean-params.sh
```

**Gradle Validation** (planned in follow-up improvements):
```bash
# Run automated validation task
./gradlew validateMyBatisBooleanParams
```

**Manual Validation** (current approach):
```bash
# Search for potential violations
cd smart-admin-api-java21-springboot3

# Find deleted_flag without typeHandler
grep -rn "deleted_flag\s*=\s*#{" --include="*.xml" . | grep -v "typeHandler="

# Find disabled_flag without typeHandler
grep -rn "disabled_flag\s*=\s*#{" --include="*.xml" . | grep -v "typeHandler="

# Expected result: No output (all parameters have typeHandler)
```

### Affected Fields

Common SmartAdmin Boolean fields using SMALLINT mapping:

| Field Name | Type | Values | Description |
|------------|------|--------|-------------|
| `deletedFlag` | SMALLINT | 0=active, 1=deleted | Soft delete flag |
| `disabledFlag` | SMALLINT | 0=enabled, 1=disabled | Enable/disable flag |
| `administratorFlag` | SMALLINT | 0=normal, 1=admin | Admin privilege flag |
| `lockedFlag` | SMALLINT | 0=unlocked, 1=locked | Account lock flag |

> **Convention**: All `*Flag` fields with Boolean Java type mapped to PostgreSQL SMALLINT require explicit typeHandler.

### PostgreSQL Type System Limitation

**Why PostgreSQL Refuses Implicit Conversion**:

PostgreSQL's type system is stricter than MySQL:

```sql
-- PostgreSQL (STRICT)
❌ SELECT * FROM t_employee WHERE deleted_flag = FALSE;
   ERROR: operator does not exist: smallint = boolean

✅ SELECT * FROM t_employee WHERE deleted_flag = 0;
   Success

-- MySQL (PERMISSIVE)
✅ SELECT * FROM t_employee WHERE deleted_flag = FALSE;
   Success (auto-converts TINYINT(1) ↔ BOOLEAN)
```

**Design Rationale**:
- PostgreSQL: Explicit type safety prevents runtime errors
- MySQL: Implicit conversion for developer convenience
- SmartAdmin: Uses TypeHandler for PostgreSQL compatibility while maintaining Java Boolean semantics

### Migration from MySQL

When migrating from MySQL to PostgreSQL:

1. **Database Column Type**:
```sql
-- MySQL
deleted_flag TINYINT(1) NOT NULL DEFAULT 0

-- PostgreSQL
deleted_flag SMALLINT NOT NULL DEFAULT 0
```

2. **Entity Field Type** (no change):
```java
// Both MySQL and PostgreSQL use same Java type
private Boolean deletedFlag;
```

3. **XML Mapper Files** (critical change):
```xml
<!-- MySQL (no typeHandler needed) -->
WHERE deleted_flag = #{deletedFlag}

<!-- PostgreSQL (typeHandler REQUIRED) -->
WHERE deleted_flag = #{deletedFlag, jdbcType=SMALLINT,
    typeHandler=net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler}
```

### Checklist

#### Entity Layer
- [ ] Entity uses `@TableField(typeHandler = BooleanToSmallintTypeHandler.class)`
- [ ] Boolean field names follow convention (e.g., `deletedFlag`, `disabledFlag`)

#### XML Mapper Layer
- [ ] All Boolean parameters explicitly specify typeHandler attribute
- [ ] WHERE clauses with Boolean conditions use typeHandler
- [ ] UPDATE SET statements with Boolean values use typeHandler
- [ ] INSERT VALUES statements with Boolean values use typeHandler

#### Testing & Validation
- [ ] Test null value conversion (null → null, not 0)
- [ ] Test true/false conversion (true → 1, false → 0)
- [ ] Pre-commit hook validation passes (after implementation)
- [ ] No PSQLException "operator does not exist: smallint = boolean" errors

#### Database Schema
- [ ] PostgreSQL columns use SMALLINT type (not BOOLEAN)
- [ ] Default values use numeric literals (DEFAULT 0, not DEFAULT FALSE)
- [ ] CHECK constraints use numeric ranges (CHECK (deleted_flag IN (0, 1)))

---

## 【Mandatory】SQL Statement Optimization

### 1. Prohibit SELECT *

```java
// ✅ Correct
@Select("SELECT id, name, email, status FROM t_user WHERE id = #{id}")
User selectById(Long id);

// ❌ Incorrect
@Select("SELECT * FROM t_user WHERE id = #{id}")
User selectById(Long id);
```

**Reasons**:
- Wastes network bandwidth (transmitting unneeded fields)
- Cannot utilize covering index optimization
- Table structure changes affect application stability

### 2. Pagination Optimization

```sql
-- ❌ Inefficient: OFFSET scans massive data
SELECT * FROM t_order
LIMIT 20 OFFSET 100000;

-- ✅ Efficient: Use primary key range
SELECT * FROM t_order
WHERE id > 100000
ORDER BY id
LIMIT 20;

-- ✅ Efficient: Deferred join (complex conditions)
SELECT a.* FROM t_order a
INNER JOIN (
    SELECT id FROM t_order
    WHERE status = 'PAID'
    ORDER BY created_at DESC
    LIMIT 20 OFFSET 100000
) b ON a.id = b.id;
```

**Performance Comparison**:
- `OFFSET 100000`: Scans 100020 rows, discards 100000 rows
- `WHERE id > 100000`: Direct positioning, scans 20 rows
- Deferred join: Subquery selects only id (index coverage), outer query only 20 times

### 3. Prohibit Left-Like Query

```sql
-- ❌ Cannot use index
WHERE email LIKE '%@gmail.com';

-- ✅ Can use index
WHERE email LIKE 'john%';

-- ✅ Full-text search (when needed)
WHERE search_vector @@ to_tsquery('gmail');
```

### 4. JOIN Optimization

```sql
-- ✅ Small table drives large table
SELECT o.id, o.amount, u.name
FROM t_order o
INNER JOIN t_user u ON o.user_id = u.id
WHERE u.status = 'ACTIVE';  -- Filter users first

-- Ensure JOIN fields have indexes
CREATE INDEX idx_order_user_id ON t_order(user_id);
CREATE INDEX idx_user_id ON t_user(id);  -- Primary key auto-indexed
```

### 5. IN vs EXISTS

```sql
-- ✅ Small collection use IN
SELECT * FROM t_order
WHERE user_id IN (1, 2, 3);

-- ✅ Subquery use EXISTS
SELECT * FROM t_order o
WHERE EXISTS (
    SELECT 1 FROM t_user u
    WHERE u.id = o.user_id
      AND u.vip = TRUE
);
```

---

## 【Mandatory】Data Migration (MySQL → PostgreSQL)

### 1. Type Mapping

| MySQL           | PostgreSQL     | Description              |
| --------------- | -------------- | ------------------------ |
| TINYINT         | SMALLINT       | 1 byte → 2 bytes         |
| INT             | INTEGER        | Same                     |
| BIGINT          | BIGINT         | Same                     |
| BIGINT UNSIGNED | BIGINT         | Remove UNSIGNED          |
| DATETIME        | TIMESTAMPTZ    | Recommended with timezone|
| TIMESTAMP       | TIMESTAMPTZ    | Recommended with timezone|
| VARCHAR(n)      | VARCHAR(n)     | Same                     |
| TEXT            | TEXT           | Same                     |
| DECIMAL(m,n)    | NUMERIC(m,n)   | Recommend NUMERIC        |
| JSON            | JSONB          | Recommend JSONB (indexed)|
| ENUM            | VARCHAR + CHECK| Or use ENUM type         |

### 2. SQL Syntax Differences

#### 2.1 UPSERT (Insert or Update)

```sql
-- MySQL
INSERT INTO t_user VALUES (1, 'John')
ON DUPLICATE KEY UPDATE name='John';

-- PostgreSQL
INSERT INTO t_user VALUES (1, 'John')
ON CONFLICT (id) DO UPDATE SET name='John';
```

#### 2.2 Auto-Increment Primary Key

```sql
-- MySQL
id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY

-- PostgreSQL
id BIGSERIAL PRIMARY KEY
-- or
id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
```

#### 2.3 String Concatenation

```sql
-- MySQL
CONCAT(first_name, ' ', last_name)

-- PostgreSQL
first_name || ' ' || last_name
-- or
CONCAT(first_name, ' ', last_name)  -- Also supported
```

#### 2.4 Date Functions

```sql
-- MySQL
NOW()
CURDATE()
DATE_ADD(NOW(), INTERVAL 1 DAY)

-- PostgreSQL
NOW()
CURRENT_DATE
NOW() + INTERVAL '1 day'
```

### 3. Entity Adjustment

```java
// MySQL Entity
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;  // MySQL: DATETIME
}

// PostgreSQL Entity
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;  // PostgreSQL: TIMESTAMPTZ
}
```

---

## 【Recommended】Vavr Functional Integration

### Option Wrapping Query Results

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // Use Vavr Option instead of Java Optional
    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }

    // Option method chaining
    public Option<String> getUserEmail(Long id) {
        return findById(id)
            .filter(user -> user.getStatus() == StatusEnum.ACTIVE)
            .map(User::getEmail);
    }
}
```

### Try Wrapping Exception Handling

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    @Transactional(rollbackFor = Exception.class)
    public Try<Order> createOrder(OrderCreateDTO dto) {
        return Try.of(() -> {
            Order order = buildOrder(dto);
            orderMapper.insert(order);
            return order;
        });
    }
}
```

### Either for Business Logic Branching

```java
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserMapper userMapper;

    public Either<ValidationError, User> registerUser(UserCreateDTO dto) {
        return validateEmail(dto.getEmail())
            .flatMap(email -> validatePassword(dto.getPassword()))
            .flatMap(password -> createUser(dto));
    }

    private Either<ValidationError, String> validateEmail(String email) {
        if (!EmailValidator.isValid(email)) {
            return Either.left(new ValidationError("INVALID_EMAIL"));
        }
        return Either.right(email);
    }
}
```

> **Reference**: For detailed Vavr usage, see [P03-vavr-mybatis-integration.md](../functional/P03-vavr-mybatis-integration.md)

---

## 【Recommended】Performance Optimization

### 1. EXPLAIN ANALYZE

```sql
-- View execution plan
EXPLAIN ANALYZE
SELECT * FROM t_user
WHERE email = 'test@example.com'
  AND status = 'ACTIVE';

-- Sample output:
-- Seq Scan on t_user  (cost=0.00..15.25 rows=1 width=100)
--   Filter: ((email = 'test@example.com') AND (status = 'ACTIVE'))
--   Rows Removed by Filter: 99
-- Planning Time: 0.123 ms
-- Execution Time: 0.456 ms
```

**Key Metrics**:
- `Seq Scan`: Sequential scan (slow) → Need to add index
- `Index Scan`: Index scan (fast)
- `Bitmap Index Scan`: Bitmap index scan (medium)
- `rows`: Estimated rows vs actual rows
- `cost`: Estimated cost
- `Execution Time`: Actual execution time

### 2. VACUUM and ANALYZE

```sql
-- Clean dead tuples, update statistics
VACUUM ANALYZE t_user;

-- Update statistics only
ANALYZE t_user;

-- Full vacuum (locks table, use cautiously)
VACUUM FULL t_user;

-- Auto configuration
ALTER TABLE t_user SET (
    autovacuum_enabled = true,
    autovacuum_vacuum_scale_factor = 0.1,
    autovacuum_analyze_scale_factor = 0.05
);
```

**When to Execute**:
- After massive data update/delete
- When query performance degrades
- Regular maintenance (weekly/monthly)

### 3. Connection Pool Configuration

**Connection Count Formula**:
```
connections = ((core_count * 2) + effective_spindle_count)
```

Example: 4-core CPU + 1 disk = 9 connections

**Notes**:
- PostgreSQL default max connections 100
- Excessive connections consume memory (each ~10MB)
- Recommend using connection pool instead of frequently creating connections

### 4. GIN Index Strategy (JSONB/Arrays)

```sql
-- GIN index (JSONB/arrays)
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- Expression index
CREATE INDEX idx_order_source ON t_order ((metadata ->> 'source'));

-- Partial index
CREATE INDEX idx_active_orders ON t_order USING GIN(metadata)
WHERE status = 'ACTIVE';
```

**Index Usage**:
```sql
-- ✅ Uses index
WHERE items @> '[{"productId": 123}]'::jsonb

-- ❌ Cannot use index
WHERE items::text LIKE '%"productId":123%'
```

### 5. Batch Operations

```xml
<!-- Batch insert JSONB -->
<insert id="batchInsert">
    INSERT INTO t_order (order_no, items, metadata)
    VALUES
    <foreach collection="list" item="order" separator=",">
        (#{order.orderNo},
         #{order.items, typeHandler=com.example.config.typehandler.JsonbTypeHandler},
         #{order.metadata, typeHandler=com.example.config.typehandler.JsonbTypeHandler})
    </foreach>
</insert>
```

### 6. Slow Query Log

```sql
-- postgresql.conf
log_min_duration_statement = 1000  -- Log queries exceeding 1 second
log_line_prefix = '%t [%p]: [%l-1] user=%u,db=%d '
log_statement = 'all'  -- Log all statements (use 'none' in production)
```

**Query Slow Query Log**:
```sql
-- Use pg_stat_statements extension
CREATE EXTENSION pg_stat_statements;

-- Query slowest 10 queries
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;
```

### 7. Index Maintenance

```sql
-- Find missing indexes (foreign keys without index)
SELECT
    tc.table_name,
    kcu.column_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
    ON tc.constraint_name = kcu.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY'
  AND NOT EXISTS (
      SELECT 1 FROM pg_indexes
      WHERE tablename = tc.table_name
        AND indexdef LIKE '%' || kcu.column_name || '%'
  );

-- Find unused indexes
SELECT
    schemaname,
    tablename,
    indexname,
    idx_scan
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND indexrelname NOT LIKE '%_pkey'
ORDER BY tablename, indexname;
```

---

## Common Issues

**Q1: Poor JSONB query performance?**
```sql
-- ✅ Uses index
WHERE items @> '[{"productId": 123}]'::jsonb

-- ❌ Cannot use index
WHERE items::text LIKE '%"productId":123%'
```

**Q2: TypeHandler not working?**
```java
// Ensure autoResultMap = true is added
@TableName(value = "t_order", autoResultMap = true)
```

**Q3: Array query error?**
```xml
<!-- Ensure type casting -->
WHERE tags &amp;&amp; ARRAY[#{tag}]::text[]
```

**Q4: Vavr Option conflicts with MyBatis Plus?**
```java
// Mapper layer - return native type
User selectById(Long id);

// Service layer - wrap as Option
public Option<User> findById(Long id) {
    return Option.of(userMapper.selectById(id));
}
```

**Q5: TIMESTAMPTZ vs TIMESTAMP?**
```java
// ✅ Recommended: Use TIMESTAMPTZ + OffsetDateTime (timezone aware)
@TableField(fill = FieldFill.INSERT)
private OffsetDateTime createdAt;

// ❌ Avoid: TIMESTAMP + LocalDateTime (no timezone)
```

---

## Checklist

### Configuration & Setup
- [ ] DataSource configured with HikariCP settings
- [ ] PostgreSQL driver dependency added
- [ ] MyBatis Plus id-type set to AUTO

### TypeHandlers
- [ ] JSONB TypeHandler uses `PGobject`
- [ ] Array TypeHandler uses `java.sql.Array`
- [ ] Entities have `autoResultMap = true`
- [ ] GIN indexes created for JSONB/array columns

### SQL Optimization
- [ ] Prohibit SELECT *
- [ ] Pagination uses primary key range or deferred join
- [ ] Prohibit left-like query
- [ ] JOIN fields have indexes
- [ ] Small collection use IN, subquery use EXISTS

### MySQL Migration
- [ ] Type mapping correct (DATETIME→TIMESTAMPTZ)
- [ ] Primary key uses BIGSERIAL
- [ ] UPSERT uses ON CONFLICT
- [ ] Entity time fields use OffsetDateTime

### Vavr Integration
- [ ] Mapper returns native types
- [ ] Service layer uses Option/Try/Either
- [ ] Avoid using Vavr types in Mapper layer
- [ ] Transactional methods use Try wrapping

### Performance Optimization
- [ ] Use EXPLAIN ANALYZE to analyze slow queries
- [ ] Run VACUUM ANALYZE periodically
- [ ] Connection pool configured properly
- [ ] GIN indexes for JSONB/arrays
- [ ] Enable slow query log
- [ ] Remove unused indexes

---

## Related Rules

- **Basic Rules**: [05-postgresql-basics.md](./D01-postgresql-basics.md) - Table Creation and Indexing
- **Advanced Features**: [05-postgresql-advanced.md](./D02-postgresql-advanced.md) - JSONB, CTE, Window Functions
- **MyBatis Plus Core**: [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - Basic Configuration
- **Vavr Integration**: [../functional/P03-vavr-mybatis-integration.md](../functional/P03-vavr-mybatis-integration.md) - Functional Query
