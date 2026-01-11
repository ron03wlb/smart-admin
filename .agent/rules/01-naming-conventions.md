---
trigger: always_on
---

# 命名規範 (基於阿里巴巴黃山版)

## 強制級別規則

### 1. 類名規範
- **規則**: 使用 UpperCamelCase，領域模型除外
- **正例**: `UserService`, `OrderDTO`, `PaymentVO`
- **反例**: `userService`, `orderDto`
```java
// ✅ 正確
public class UserServiceImpl implements UserService { }
public class OrderDTO { }

// ❌ 錯誤
public class userService { }
public class Orderdto { }
```

### 2. 方法/變量命名
- **規則**: 使用 lowerCamelCase
- **正例**: `getUserById()`, `localValue`
- **反例**: `GetUserById()`, `LocalValue`

### 3. 常量命名
- **規則**: 全大寫，下劃線分隔
- **正例**: `MAX_RETRY_COUNT = 3`
- **反例**: `maxRetryCount = 3`

### 4. 布爾變量
- **規則**: 禁止 is 前綴（POJO 類）
- **正例**: `private Boolean success;`
- **反例**: `private Boolean isSuccess;`
- **原因**: 部分框架解析 getter/setter 時產生序列化問題

### 5. Service/DAO 方法命名
| 操作類型 | 前綴          | 示例                  |
| -------- | ------------- | --------------------- |
| 獲取單個 | get           | `getUserById()`       |
| 獲取列表 | list          | `listUsersByStatus()` |
| 統計數量 | count         | `countActiveUsers()`  |
| 新增保存 | save/insert   | `saveUser()`          |
| 刪除操作 | delete/remove | `deleteUserById()`    |
| 更新操作 | update        | `updateUserStatus()`  |

### 6. 包名規範
- **規則**: 全小寫，單數形式
- **正例**: `com.example.user.service`
- **反例**: `com.example.Users.Services`

## 自動檢查配置
```xml
<!-- Checkstyle 配置 -->
<module name="TypeName">
    <property name="format" value="^[A-Z][a-zA-Z0-9]*$"/>
</module>
<module name="MethodName">
    <property name="format" value="^[a-z][a-zA-Z0-9]*$"/>
</module>
<module name="ConstantName">
    <property name="format" value="^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$"/>
</module>
```