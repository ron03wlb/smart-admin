---
trigger: always_on
description: MyBatis Plus Core Usage - LambdaQueryWrapper, Pagination, IEnum
tags: [mybatis-plus, lambda-query, pagination, ienum]
positioning: ideal
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - technology/database/05-postgresql-mybatis.md
  - technology/functional/08-vavr-mybatis-integration.md
  - foundation/10-architecture-rules.md
last_updated: 2025-01-21
---

# MyBatis Plus Core Standards

**TL;DR**: Use MyBatis Plus LambdaQueryWrapper for type-safe queries, IEnum for status mapping, and optimized pagination queries.

**Positioning**: This specification defines ideal architecture: LambdaQueryWrapper as primary, XML Mapper as secondary. Current projects use XML Mapper, but should gradually migrate to LambdaQueryWrapper for better type safety and refactoring friendliness.

## [Mandatory] LambdaQueryWrapper Usage

### Must Use Lambda Version
```java
// ✅ Correct - type-safe, refactoring-friendly
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>()
    .eq(User::getStatus, StatusEnum.ACTIVE)
    .ge(User::getAge, 18)
    .like(User::getName, keyword)
    .orderByDesc(User::getCreatedAt);

List<User> users = userMapper.selectList(wrapper);

// ❌ Wrong - string is error-prone
QueryWrapper<User> wrapper = new QueryWrapper<>()
    .eq("status", "ACTIVE")  // Field name might be misspelled
    .ge("age", 18);
```

### Condition Building
```java
// ✅ Correct - conditional construction
String name = request.getName();
Integer minAge = request.getMinAge();

LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
    .eq(StringUtils.isNotBlank(name), User::getName, name)
    .ge(minAge != null, User::getAge, minAge);
```

## [Mandatory] IEnum Status Mapping

### Enum Definition
```java
@Getter
@AllArgsConstructor
public enum StatusEnum implements IEnum<Integer> {
    PENDING(0, "Pending"),
    ACTIVE(1, "Active"),
    DISABLED(2, "Disabled");

    private final Integer value;
    private final String description;

    @Override
    public Integer getValue() {
        return this.value;
    }
}
```

### Configuration
```yaml
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
  type-enums-package: com.example.enums
```

## [Mandatory] Pagination Configuration

### Plugin Configuration
```java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Pagination plugin
        PaginationInnerInterceptor pagination =
            new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit(500L);
        pagination.setOverflow(true);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
```

### Usage
```java
public IPage<User> listUsers(int page, int size, String status) {
    Page<User> pageParam = new Page<>(page, size);

    LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
        .eq(StringUtils.isNotBlank(status), User::getStatus, status)
        .orderByDesc(User::getCreatedAt)
        .orderByDesc(User::getId);  // Ensure sort uniqueness

    return userMapper.selectPage(pageParam, wrapper);
}
```

## [Mandatory] Logical Delete

### Global Configuration
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted_at
      logic-delete-value: NOW()
      logic-not-delete-value: 'NULL'
```

### Entity Configuration
```java
@Data
@TableName("t_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @TableLogic
    @TableField(select = false)
    private LocalDateTime deletedAt;
}
```

## [Mandatory] SQL Injection Protection

### Safe Queries
```java
// ✅ Safe - Lambda approach
userMapper.selectList(
    Wrappers.<User>lambdaQuery()
        .eq(User::getName, userInput)  // Parameter binding
);

// ❌ Dangerous - String concatenation
String sql = "SELECT * FROM user WHERE name = '" + userInput + "'";
```

### Dynamic Sorting Safe Handling
```java
private static final Set<String> ALLOWED_SORT_FIELDS =
    Set.of("created_at", "updated_at", "name", "status");

public List<User> listUsers(String sortField) {
    if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
        sortField = "created_at";
    }

    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .orderByDesc(User::getCreatedAt)
    );
}
```

## Performance Optimization

### Batch Operations
```java
@Transactional(rollbackFor = Exception.class)
public void batchInsert(List<Order> orders) {
    // Auto-batching (recommended)
    this.saveBatch(orders, 1000);
}
```

### Index Strategy
```sql
-- B-Tree index (regular queries)
CREATE INDEX idx_user_status ON t_user(status);
CREATE INDEX idx_user_created_at ON t_user(created_at DESC);

-- Composite index
CREATE INDEX idx_user_status_created ON t_user(status, created_at DESC);

-- Partial index
CREATE INDEX idx_active_users ON t_user(created_at)
WHERE deleted_at IS NULL;
```

## Checklist

### New Entity Checklist
- [ ] Use `@TableName` to specify table name
- [ ] Primary key uses `@TableId(type = IdType.AUTO)`
- [ ] Status fields use `IEnum` enumeration
- [ ] Logical delete field uses `@TableLogic`
- [ ] JSONB/array fields marked `autoResultMap = true`

### Query Checklist
- [ ] Prefer `LambdaQueryWrapper` over string queries
- [ ] Pagination queries must add unique sort field
- [ ] Dynamic sorting uses whitelist filtering
- [ ] Avoid N+1 queries, use batch queries

### Performance Checklist
- [ ] Batch operations use `saveBatch()` batching
- [ ] Frequently queried fields have indexes
- [ ] Composite queries use composite indexes
- [ ] Pagination query max limit set to 500

## Related Specifications
- **PostgreSQL + MyBatis Integration**: [05-postgresql-mybatis.md](./05-postgresql-mybatis.md)
- **PostgreSQL Advanced Features**: [05-postgresql-advanced.md](./05-postgresql-advanced.md)
- **Database Design**: [04-database-design.md](./04-database-design.md)
