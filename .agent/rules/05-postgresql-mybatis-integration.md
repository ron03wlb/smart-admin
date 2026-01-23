---
trigger: always_on
description: PostgreSQL + MyBatis Plus - SQL Optimization, MySQL Migration, Performance Tuning
tags: [postgresql, mybatis-plus, migration, performance, optimization]
positioning: migration
prerequisites:
  - rules/05-postgresql-basics.md
  - rules/09-mybatis-plus-core.md
related_rules:
  - rules/08-vavr-mybatis-integration.md
  - rules/09-mybatis-plus-postgresql.md
last_updated: 2025-01-12
---

# PostgreSQL + MyBatis Plus Integration

**TL;DR**: MySQL migration to PostgreSQL requires attention to type mapping (DATETIME→TIMESTAMPTZ, JSON→JSONB), syntax differences (ON DUPLICATE KEY→ON CONFLICT), primary key strategy (AUTO_INCREMENT→BIGSERIAL). SQL optimization prohibits SELECT *, left-like queries, pagination uses primary key range instead of OFFSET. Performance tuning runs VACUUM ANALYZE periodically, connection pool configured as `(core_count * 2) + spindle_count`.

**Prerequisites**:
- [05-postgresql-basics.md](./05-postgresql-basics.md) - PostgreSQL Basic Rules
- [09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - MyBatis Plus Core Configuration

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

#### 2.4 LIMIT Syntax

```sql
-- MySQL and PostgreSQL same
SELECT * FROM t_user LIMIT 10 OFFSET 20;
```

#### 2.5 Date Functions

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

### 3. MyBatis Plus Configuration Adjustment

```yaml
# application.yml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3?useSSL=false&serverTimezone=Asia/Shanghai
    username: smartadmin
    password: SmartAdmin@2024
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

mybatis-plus:
  configuration:
    # PostgreSQL identifiers use double quotes
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # PostgreSQL primary key strategy
      id-type: AUTO
      # Logical delete field
      logic-delete-field: deletedAt
      logic-delete-value: 'NOW()'
      logic-not-delete-value: 'NULL'
```

### 4. Dependency Update

```xml
<!-- Remove MySQL driver -->
<!-- <dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency> -->

<!-- Add PostgreSQL driver -->
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

### 5. Entity Adjustment

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

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20       # Maximum connections
      minimum-idle: 5             # Minimum idle connections
      connection-timeout: 30000   # Connection timeout (ms)
      idle-timeout: 600000        # Idle timeout (10 minutes)
      max-lifetime: 1800000       # Maximum lifetime (30 minutes)
      connection-test-query: SELECT 1
```

**Connection Count Formula**:
```
connections = ((core_count * 2) + effective_spindle_count)
```

Example: 4-core CPU + 1 disk = 9 connections

**Notes**:
- PostgreSQL default max connections 100
- Excessive connections consume memory (each ~10MB)
- Recommend using connection pool instead of frequently creating connections

### 4. Slow Query Log

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

### 5. Index Maintenance

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

## Checklist

**SQL Optimization**:
- ✅ Prohibit SELECT *
- ✅ Pagination uses primary key range or deferred join
- ✅ Prohibit left-like query
- ✅ JOIN fields have indexes
- ✅ Small collection use IN, subquery use EXISTS

**MySQL Migration**:
- ✅ Type mapping correct (DATETIME→TIMESTAMPTZ)
- ✅ Primary key uses BIGSERIAL
- ✅ UPSERT uses ON CONFLICT
- ✅ Dependency updated to PostgreSQL driver
- ✅ Entity time fields use OffsetDateTime

**Performance Optimization**:
- ✅ Use EXPLAIN ANALYZE to analyze slow queries
- ✅ Run VACUUM ANALYZE periodically
- ✅ Connection pool configured properly
- ✅ Enable slow query log
- ✅ Remove unused indexes

---

## Related Rules

- **Basic Rules**: [rules/05-postgresql-basics.md](./05-postgresql-basics.md) - Table Creation and Indexing
- **Advanced Features**: [rules/05-postgresql-advanced.md](./05-postgresql-advanced.md) - JSONB, CTE, Window Functions
- **MyBatis Plus Core**: [rules/09-mybatis-plus-core.md](./09-mybatis-plus-core.md) - Basic Configuration
- **MyBatis Plus PostgreSQL**: [rules/09-mybatis-plus-postgresql.md](./09-mybatis-plus-postgresql.md) - PostgreSQL-Specific Configuration
- **Vavr Integration**: [rules/08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md) - Functional Query
