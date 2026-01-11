---
trigger: always_on
---

# MyBatis Plus 規範

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
        
        // 分頁插件必須最後添加
        PaginationInnerInterceptor pagination = 
            new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);  // 單頁最大 500
        pagination.setOverflow(true);  // 超出範圍回到首頁
        
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
        .eq(StringUtils.isNotBlank(status), User::getStatus, status);
    
    return userMapper.selectPage(pageParam, wrapper);
}
```

## 【強制】邏輯刪除

### 全局配置
```yaml
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
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
    @TableField(select = false)  // 查詢時排除
    private Integer deleted;
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

// ✅ 安全 - XML 中使用 #{}
// <select id="findByName">
//     SELECT * FROM user WHERE name = #{name}
// </select>

// ❌ 危險 - 字符串拼接
String sql = "SELECT * FROM user WHERE name = '" + userInput + "'";

// ❌ 危險 - XML 中使用 ${}
// SELECT * FROM user WHERE name = '${name}'
```

### 動態排序安全處理
```java
private static final Set<String> ALLOWED_SORT_FIELDS = 
    Set.of("created_at", "updated_at", "name", "status");

public List<User> listUsers(String sortField) {
    // 白名單驗證
    if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
        sortField = "created_at";
    }
    
    return userMapper.selectList(
        Wrappers.<User>lambdaQuery()
            .orderByDesc(User::getCreatedAt)  // 使用 Lambda
    );
}

### 分頁需要唯一值強制排序
```