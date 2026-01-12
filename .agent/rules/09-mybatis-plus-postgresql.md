---
trigger: always_on
description: MyBatis Plus 與 PostgreSQL 整合 - JSONB、陣列、Vavr
tags: [mybatis-plus, postgresql, jsonb, arrays, vavr, integration]
positioning: ideal
prerequisites: [rules/09-mybatis-plus-core.md, rules/05-postgresql-advanced.md]
related_rules: [rules/05-postgresql-mybatis-integration.md, rules/08-vavr-mybatis-integration.md]
last_updated: 2025-01-12
---

# MyBatis Plus 與 PostgreSQL 整合

**TL;DR**: 使用自定義 TypeHandler 處理 PostgreSQL JSONB 和數組類型，結合 Vavr 函數式編程實現類型安全和異常處理。

## 【強制】數據源配置

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

## 【強制】JSONB 類型處理

### TypeHandler 實現
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

### Entity 使用
```java
@Data
@TableName(value = "t_order", autoResultMap = true)
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    // JSONB 字段
    @TableField(typeHandler = JsonbTypeHandler.class)
    private List<OrderItem> items;

    @TableField(typeHandler = JsonbTypeHandler.class)
    private Map<String, Object> metadata;
}
```

### JSONB 查詢
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

> **參考**: JSONB 操作符和索引詳見 [05-postgresql-advanced.md](./05-postgresql-advanced.md#jsonb-操作)

## 【強制】數組類型處理

### TypeHandler 實現
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

### Entity 使用
```java
@Data
@TableName(value = "t_article", autoResultMap = true)
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    // TEXT[] 數組
    @TableField(typeHandler = StringArrayTypeHandler.class)
    private List<String> tags;
}
```

### 數組查詢
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

## 【推薦】Vavr 函數式整合

### Option 包裝查詢結果
```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // 使用 Vavr Option 替代 Java Optional
    public Option<User> findById(Long id) {
        return Option.of(userMapper.selectById(id));
    }

    // Option 鏈式調用
    public Option<String> getUserEmail(Long id) {
        return findById(id)
            .filter(user -> user.getStatus() == StatusEnum.ACTIVE)
            .map(User::getEmail);
    }
}
```

### Try 包裝異常處理
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

### Either 業務邏輯分支
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

> **參考**: Vavr 詳細用法見 [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md)

## 性能優化

### 索引策略
```sql
-- GIN 索引（JSONB/數組）
CREATE INDEX idx_order_items ON t_order USING GIN(items);
CREATE INDEX idx_article_tags ON t_article USING GIN(tags);

-- 表達式索引
CREATE INDEX idx_order_source ON t_order ((metadata ->> 'source'));

-- 部分索引
CREATE INDEX idx_active_orders ON t_order USING GIN(metadata)
WHERE status = 'ACTIVE';
```

### 批量操作
```xml
<!-- 批量插入 JSONB -->
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

## 常見問題

**Q1: JSONB 查詢性能差？**
```sql
-- ✅ 使用索引
WHERE items @> '[{"productId": 123}]'::jsonb

-- ❌ 無法使用索引
WHERE items::text LIKE '%"productId":123%'
```

**Q2: TypeHandler 不生效？**
```java
// 確保添加 autoResultMap = true
@TableName(value = "t_order", autoResultMap = true)
```

**Q3: 數組查詢報錯？**
```xml
<!-- 確保類型轉換 -->
WHERE tags &amp;&amp; ARRAY[#{tag}]::text[]
```

**Q4: Vavr Option 和 MyBatis Plus 沖突？**
```java
// Mapper 層 - 返回原生類型
User selectById(Long id);

// Service 層 - 包裝為 Option
public Option<User> findById(Long id) {
    return Option.of(userMapper.selectById(id));
}
```

## Checklist

### JSONB 使用檢查項
- [ ] Entity 添加 `autoResultMap = true`
- [ ] TypeHandler 使用 `PGobject`
- [ ] 建立 GIN 索引
- [ ] 查詢使用 `@>` 操作符
- [ ] 避免 `::text` 轉換查詢

### 數組使用檢查項
- [ ] TypeHandler 使用 `java.sql.Array`
- [ ] 建立 GIN 索引
- [ ] 查詢使用 `&&` 或 `@>` 操作符
- [ ] 明確指定數組類型 `::text[]`

### Vavr 整合檢查項
- [ ] Mapper 返回原生類型
- [ ] Service 層使用 Option/Try/Either
- [ ] 避免在 Mapper 層使用 Vavr 類型
- [ ] 事務方法使用 Try 包裝

## 相關規範
- **核心用法**: [09-mybatis-plus-core.md](./09-mybatis-plus-core.md)
- **PostgreSQL 高級特性**: [05-postgresql-advanced.md](./05-postgresql-advanced.md)
- **PostgreSQL-MyBatis 整合**: [05-postgresql-mybatis-integration.md](./05-postgresql-mybatis-integration.md)
- **Vavr-MyBatis 整合**: [08-vavr-mybatis-integration.md](./08-vavr-mybatis-integration.md)
