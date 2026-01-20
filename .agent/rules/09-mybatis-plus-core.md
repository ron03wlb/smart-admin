---
trigger: always_on
description: MyBatis Plus 核心用法 - LambdaQueryWrapper、分頁、IEnum
tags: [mybatis-plus, lambda-query, pagination, ienum]
positioning: ideal
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
related_rules:
  - rules/05-postgresql-mybatis-integration.md
  - rules/08-vavr-mybatis-integration.md
  - rules/10-architecture-rules.md
last_updated: 2025-01-21
---

# MyBatis Plus 核心規範

**TL;DR**: 使用 MyBatis Plus LambdaQueryWrapper 實現類型安全查詢，IEnum 映射狀態，分頁查詢優化。

**定位說明**: 本規範定義理想架構：LambdaQueryWrapper 為主，XML Mapper 為輔。當前專案使用 XML Mapper，但應逐步遷移至 LambdaQueryWrapper 以獲得更好的類型安全和重構友好性。

## 【強制】LambdaQueryWrapper 使用

### 必須使用 Lambda 版本
```java
// ✅ 正確 - 類型安全，重構友好
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>()
    .eq(User::getStatus, StatusEnum.ACTIVE)
    .ge(User::getAge, 18)
    .like(User::getName, keyword)
    .orderByDesc(User::getCreatedAt);

List<User> users = userMapper.selectList(wrapper);

// ❌ 錯誤 - 字符串容易出錯
QueryWrapper<User> wrapper = new QueryWrapper<>()
    .eq("status", "ACTIVE")  // 字段名可能拼錯
    .ge("age", 18);
```

### 條件構建
```java
// ✅ 正確 - 條件判斷
String name = request.getName();
Integer minAge = request.getMinAge();

LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
    .eq(StringUtils.isNotBlank(name), User::getName, name)
    .ge(minAge != null, User::getAge, minAge);
```

## 【強制】IEnum 狀態映射

### 枚舉定義
```java
@Getter
@AllArgsConstructor
public enum StatusEnum implements IEnum<Integer> {
    PENDING(0, "待處理"),
    ACTIVE(1, "啟用"),
    DISABLED(2, "停用");

    private final Integer value;
    private final String description;

    @Override
    public Integer getValue() {
        return this.value;
    }
}
```

### 配置
```yaml
mybatis-plus:
  configuration:
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
  type-enums-package: com.example.enums
```

## 【強制】分頁配置

### 插件配置
```java
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分頁插件
        PaginationInnerInterceptor pagination =
            new PaginationInnerInterceptor(DbType.POSTGRE_SQL);
        pagination.setMaxLimit(500L);
        pagination.setOverflow(true);

        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
```

### 使用方式
```java
public IPage<User> listUsers(int page, int size, String status) {
    Page<User> pageParam = new Page<>(page, size);

    LambdaQueryWrapper<User> wrapper = Wrappers.<User>lambdaQuery()
        .eq(StringUtils.isNotBlank(status), User::getStatus, status)
        .orderByDesc(User::getCreatedAt)
        .orderByDesc(User::getId);  // 保證排序唯一性

    return userMapper.selectPage(pageParam, wrapper);
}
```

## 【強制】邏輯刪除

### 全局配置
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted_at
      logic-delete-value: NOW()
      logic-not-delete-value: 'NULL'
```

### Entity 配置
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

## 【強制】SQL 注入防護

### 安全查詢
```java
// ✅ 安全 - Lambda 方式
userMapper.selectList(
    Wrappers.<User>lambdaQuery()
        .eq(User::getName, userInput)  // 參數綁定
);

// ❌ 危險 - 字符串拼接
String sql = "SELECT * FROM user WHERE name = '" + userInput + "'";
```

### 動態排序安全處理
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

## 性能優化

### 批量操作
```java
@Transactional(rollbackFor = Exception.class)
public void batchInsert(List<Order> orders) {
    // 自動分批（推薦）
    this.saveBatch(orders, 1000);
}
```

### 索引策略
```sql
-- B-Tree 索引（常規查詢）
CREATE INDEX idx_user_status ON t_user(status);
CREATE INDEX idx_user_created_at ON t_user(created_at DESC);

-- 複合索引
CREATE INDEX idx_user_status_created ON t_user(status, created_at DESC);

-- 部分索引
CREATE INDEX idx_active_users ON t_user(created_at)
WHERE deleted_at IS NULL;
```

## Checklist

### 新增 Entity 檢查項
- [ ] 使用 `@TableName` 指定表名
- [ ] 主鍵使用 `@TableId(type = IdType.AUTO)`
- [ ] 狀態字段使用 `IEnum` 枚舉
- [ ] 邏輯刪除字段使用 `@TableLogic`
- [ ] JSONB/數組字段標記 `autoResultMap = true`

### 查詢檢查項
- [ ] 優先使用 `LambdaQueryWrapper` 替代字符串查詢
- [ ] 分頁查詢必須添加唯一排序字段
- [ ] 動態排序使用白名單過濾
- [ ] 避免 N+1 查詢，使用批量查詢

### 性能檢查項
- [ ] 批量操作使用 `saveBatch()` 分批處理
- [ ] 頻繁查詢字段建立索引
- [ ] 複合查詢使用複合索引
- [ ] 分頁查詢最大限制設為 500

## 相關規範
- **PostgreSQL 整合**: [09-mybatis-plus-postgresql.md](./09-mybatis-plus-postgresql.md)
- **PostgreSQL 高級特性**: [05-postgresql-advanced.md](./05-postgresql-advanced.md)
- **資料庫設計**: [04-database-design.md](./04-database-design.md)
