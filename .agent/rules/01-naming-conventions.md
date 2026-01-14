---
trigger: always_on
description: 命名規範（類、方法、變量、常量）
tags: [naming, code-style, alibaba-java-coding-guidelines]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
ask_before_fix: false
prerequisites: []
conflicts_with: []
related_rules:
  - rules/10-architecture-rules.md
  - rules/02-oop-principles.md
archunit_test: ArchitectureTest#controllerNaming
checkstyle_rule: TypeName,MethodName,ConstantName
spotbugs_rule: none
last_updated: 2025-01-13
---

# 命名規範 (基於阿里巴巴黃山版)

---

## 🤖 AI 指令區塊

### 何時應用此規則
- ✅ **永遠**: 生成任何 Java 代碼時都必須遵守命名規範
- ✅ Code Review 時檢查命名是否規範
- ✅ 檢測到不規範的命名（如 userService 作為類名）
- ✅ 檢測到魔法數字（應該定義為常量）

### 強制執行檢查清單
生成或審查代碼時，必須確認：
- [ ] 類名使用 **UpperCamelCase**（如 UserService）
- [ ] 方法名和變量名使用 **lowerCamelCase**（如 getUserById）
- [ ] 常量使用 **UPPER_SNAKE_CASE**（如 MAX_RETRY_COUNT）
- [ ] Boolean 變量/字段不使用 **is** 前綴（POJO 類中）
- [ ] 集合變量使用複數形式（如 users, orderList）
- [ ] Service/DAO 方法前綴正確（get/list/count/save/delete/update）

### AI 決策樹
```
命名檢查流程:
  ├─ 1️⃣ 識別代碼元素類型
  │   ├─ 類/接口? → UpperCamelCase
  │   ├─ 方法/變量? → lowerCamelCase
  │   ├─ 常量? → UPPER_SNAKE_CASE
  │   └─ 包名? → lowercase（單數）
  │
  ├─ 2️⃣ 檢查特殊規則
  │   ├─ Boolean 字段? → 不用 is 前綴
  │   ├─ 集合? → 使用複數
  │   ├─ Service 方法? → 檢查前綴
  │   └─ Controller/Service/Dao? → 檢查後綴
  │
  └─ 3️⃣ 運行自動檢查
      ├─ Checkstyle: mvn checkstyle:check
      └─ ArchUnit: mvn test -Dtest=ArchitectureTest#controllerNaming
```

### 錯誤模式檢測與自動修正

#### 模式 1: 類名不規範
```java
// ❌ 檢測到錯誤
public class userService { } // 應該首字母大寫
public class Userdao { }     // 應該是 UserDao（駝峰）
public class USER_ENTITY { } // 不應該全大寫

// ✅ 自動修正為
public class UserService { }
public class UserDao { }
public class UserEntity { }
```

#### 模式 2: 方法名不規範
```java
// ❌ 檢測到錯誤
public User GetUserById(Long id) { }      // 首字母不應該大寫
public List<User> QueryUserList() { }     // 首字母不應該大寫
public void Delete_User(Long id) { }      // 不應該使用下劃線

// ✅ 自動修正為
public User getUserById(Long id) { }
public List<User> listUsers() { }        // 或 queryUserList
public void deleteUser(Long id) { }
```

#### 模式 3: 常量命名不規範
```java
// ❌ 檢測到錯誤
private final int maxRetryCount = 3;     // 常量應該全大寫
public static int Max_Size = 100;        // 應該全大寫且用下劃線
private static final String apiUrl = "..."; // 應該全大寫

// ✅ 自動修正為
private static final int MAX_RETRY_COUNT = 3;
public static final int MAX_SIZE = 100;
private static final String API_URL = "...";
```

#### 模式 4: Boolean 字段命名（POJO）
```java
// ❌ 檢測到錯誤（在 POJO 類中）
@Data
public class User {
    private Boolean isSuccess;  // ❌ POJO 禁止 is 前綴
    private Boolean isDeleted;  // ❌
}

// ✅ 自動修正為
@Data
public class User {
    private Boolean success;    // ✅ 去掉 is 前綴
    private Boolean deleted;    // ✅
}

// ⚠️ 注意: 方法名仍可以用 is 前綴
public boolean isActive() {  // ✅ 方法名沒問題
    return this.status == Status.ACTIVE;
}
```

#### 模式 5: 集合命名
```java
// ❌ 檢測到錯誤
List<User> user;              // 應該用複數
Map<Long, Order> orderMap;    // Map 可以保留 Map 後綴（但建議複數）
Set<String> tagSet;           // Set 可以保留 Set 後綴（但建議複數）

// ✅ 修正為
List<User> users;             // ✅ 複數
Map<Long, Order> orders;      // ✅ 或 orderMap（可選）
Set<String> tags;             // ✅ 或 tagSet（可選）
```

#### 模式 6: Service/Dao 方法前綴
```java
// ❌ 檢測到錯誤
public User findUserById(Long id) { }       // Service 應該用 get
public List<User> getUserList() { }         // 應該用 list 前綴
public int totalUsers() { }                 // 應該用 count 前綴

// ✅ 自動修正為
public User getUserById(Long id) { }        // ✅ get 單個對象
public List<User> listUsers() { }           // ✅ list 列表
public int countUsers() { }                 // ✅ count 統計
```

### 生成代碼標準模板

#### 模板 1: Controller 類
```java
// ✅ 命名標準
@RestController
@RequestMapping("/api/v1/users")
public class UserController {  // ✅ UpperCamelCase + Controller 後綴

    private final UserService userService;  // ✅ lowerCamelCase

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long userId) {  // ✅ lowerCamelCase
        ...
    }
}
```

#### 模板 2: Service 類
```java
// ✅ 命名標準
@Service
public class UserService {  // ✅ UpperCamelCase + Service 後綴

    private final UserMapper userMapper;
    private static final int MAX_RETRY_COUNT = 3;  // ✅ 常量全大寫

    public Option<User> getUserById(Long id) {  // ✅ get 前綴（單個）
        ...
    }

    public List<User> listUsersByStatus(String status) {  // ✅ list 前綴（列表）
        ...
    }

    public int countActiveUsers() {  // ✅ count 前綴（統計）
        ...
    }

    @Transactional
    public void saveUser(User user) {  // ✅ save 前綴（保存）
        ...
    }

    @Transactional
    public void deleteUser(Long id) {  // ✅ delete 前綴（刪除）
        ...
    }
}
```

#### 模板 3: Entity/DTO 類
```java
// ✅ 命名標準
@Data
@TableName("t_user")
public class User {  // ✅ UpperCamelCase（不用 Entity 後綴）

    @TableId(type = IdType.AUTO)
    private Long userId;  // ✅ lowerCamelCase

    private String username;
    private String email;
    private Boolean deleted;  // ✅ Boolean 不用 is 前綴

    // ✅ 方法可以用 is 前綴
    public boolean isActive() {
        return !deleted;
    }
}

// DTO 示例
public class UserCreateDTO {  // ✅ UpperCamelCase + DTO 後綴
    private String username;
    private String email;
}

// VO 示例
public class UserVO {  // ✅ UpperCamelCase + VO 後綴
    private Long userId;
    private String username;
}
```

#### 模板 4: 常量類/枚舉
```java
// ✅ 常量類
public class UserConstants {  // ✅ UpperCamelCase + Constants 後綴

    // ✅ 常量全大寫，下劃線分隔
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String DEFAULT_ROLE = "USER";

    private UserConstants() {  // 私有構造函數
        throw new UnsupportedOperationException();
    }
}

// ✅ 枚舉
public enum UserStatus {  // ✅ UpperCamelCase
    ACTIVE,     // ✅ 枚舉值全大寫
    INACTIVE,
    DELETED
}
```

### 驗證命令
生成代碼後，自動運行以下命令驗證命名規範：

```bash
# 1. Checkstyle 檢查命名規範
mvn checkstyle:check

# 2. ArchUnit 檢查類命名後綴
mvn test -Dtest=ArchitectureTest#controllerNaming
mvn test -Dtest=ArchitectureTest#serviceNaming
mvn test -Dtest=ArchitectureTest#daoNaming

# 3. 查看詳細錯誤（如果失敗）
mvn checkstyle:check -Dcheckstyle.console=true
```

**預期結果**: 0 errors, 0 warnings

---

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