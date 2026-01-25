---
trigger: always_on
description: MyBatis Plus and PostgreSQL Integration - JSONB, Arrays, Vavr
tags: [mybatis-plus, postgresql, jsonb, arrays, vavr, integration]
positioning: ideal
prerequisites:
  - rules/09-mybatis-plus-core.md
  - rules/05-postgresql-advanced.md
related_rules:
  - rules/05-postgresql-mybatis-integration.md
  - rules/08-vavr-mybatis-integration.md
last_updated: 2025-01-12
---

# MyBatis Plus and PostgreSQL Integration

**TL;DR**: Use custom TypeHandler to handle PostgreSQL JSONB and array types, combined with Vavr functional programming for type safety and exception handling.

## [Mandatory] DataSource Configuration

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://localhost:5432/smart_admin_v3
    username: smartadmin
    password: SmartAdmin@2024

    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

mybatis-plus:
  global-config:
    db-config:
      id-type: AUTO  # PostgreSQL SERIAL/BIGSERIAL
      logic-delete-field: deleted_at
      logic-delete-value: NOW()
      logic-not-delete-value: 'NULL'
```

## [Mandatory] JSONB Type Handling

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

> **Reference**: For JSONB operators and indexes, see [05-postgresql-advanced.md](./05-postgresql-advanced.md#jsonb-operations)

## [Mandatory] Array Type Handling

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

## [Recommended] Vavr Functional Integration

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

> **Reference**: For detailed Vavr usage, see [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md)

## Performance Optimization

### Index Strategy
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

### Batch Operations
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

## Checklist

### JSONB Usage Checklist
- [ ] Entity has `autoResultMap = true`
- [ ] TypeHandler uses `PGobject`
- [ ] GIN index created
- [ ] Queries use `@>` operator
- [ ] Avoid `::text` conversion in queries

### Array Usage Checklist
- [ ] TypeHandler uses `java.sql.Array`
- [ ] GIN index created
- [ ] Queries use `&&` or `@>` operator
- [ ] Array type explicitly specified `::text[]`

### Vavr Integration Checklist
- [ ] Mapper returns native types
- [ ] Service layer uses Option/Try/Either
- [ ] Avoid using Vavr types in Mapper layer
- [ ] Transactional methods use Try wrapping

## Related Specifications
- **Core Usage**: [09-mybatis-plus-core.md](./09-mybatis-plus-core.md)
- **PostgreSQL Advanced Features**: [05-postgresql-advanced.md](./05-postgresql-advanced.md)
- **PostgreSQL-MyBatis Integration**: [05-postgresql-mybatis-integration.md](./05-postgresql-mybatis-integration.md)
- **Vavr-MyBatis Integration**: [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md)
