---
trigger: always_on
description: Naming Convention (Classes, Methods, Variables, Constants)
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
archunit_test: ArchitectureTest#controllerNaming,ArchitectureTest#noBooleanFieldWithIsPrefix
checkstyle_rule: TypeName,MethodName,ConstantName
spotbugs_rule: none
last_updated: 2025-01-13
---

# Naming Convention (Based on Alibaba Java Coding Guidelines)

---

## 🤖 AI Instructions Block

### When to Apply This Rule
- ✅ **Always**: Must follow naming conventions when generating any Java code
- ✅ Check naming convention during Code Review
- ✅ Detected non-compliant naming (e.g., userService as a class name)
- ✅ Detected magic numbers (should be defined as constants)

### Mandatory Enforcement Checklist
When generating or reviewing code, must confirm:
- [ ] Class names use **UpperCamelCase** (e.g., UserService)
- [ ] Method names and variable names use **lowerCamelCase** (e.g., getUserById)
- [ ] Constants use **UPPER_SNAKE_CASE** (e.g., MAX_RETRY_COUNT)
- [ ] Boolean variables/fields do not use **is** prefix (in POJO classes)
- [ ] Collection variables use plural form (e.g., users, orderList)
- [ ] Service/DAO method prefixes are correct (get/list/count/save/delete/update)

### AI Decision Tree
```
Naming Check Flow:
  ├─ 1️⃣ Identify Code Element Type
  │   ├─ Class/Interface? → UpperCamelCase
  │   ├─ Method/Variable? → lowerCamelCase
  │   ├─ Constant? → UPPER_SNAKE_CASE
  │   └─ Package Name? → lowercase (singular)
  │
  ├─ 2️⃣ Check Special Rules
  │   ├─ Boolean Field? → No is prefix
  │   ├─ Collection? → Use plural
  │   ├─ Service Method? → Check prefix
  │   └─ Controller/Service/Dao? → Check suffix
  │
  └─ 3️⃣ Run Automatic Checks
      ├─ Checkstyle: mvn checkstyle:check
      └─ ArchUnit: mvn test -Dtest=ArchitectureTest#controllerNaming
```

### Error Pattern Detection and Auto-Fix

#### Pattern 1: Non-Compliant Class Names
```java
// ❌ Detected Error
public class userService { } // Should start with uppercase
public class Userdao { }     // Should be UserDao (camelCase)
public class USER_ENTITY { } // Should not be all uppercase

// ✅ Auto-Fix to
public class UserService { }
public class UserDao { }
public class UserEntity { }
```

#### Pattern 2: Non-Compliant Method Names
```java
// ❌ Detected Error
public User GetUserById(Long id) { }      // First letter should not be uppercase
public List<User> QueryUserList() { }     // First letter should not be uppercase
public void Delete_User(Long id) { }      // Should not use underscore

// ✅ Auto-Fix to
public User getUserById(Long id) { }
public List<User> listUsers() { }        // or queryUserList
public void deleteUser(Long id) { }
```

#### Pattern 3: Non-Compliant Constant Naming
```java
// ❌ Detected Error
private final int maxRetryCount = 3;     // Constants should be all uppercase
public static int Max_Size = 100;        // Should be all uppercase with underscores
private static final String apiUrl = "..."; // Should be all uppercase

// ✅ Auto-Fix to
private static final int MAX_RETRY_COUNT = 3;
public static final int MAX_SIZE = 100;
private static final String API_URL = "...";
```

#### Pattern 4: Boolean Field Naming (POJO)
```java
// ❌ Detected Error (in POJO class)
@Data
public class User {
    private Boolean isSuccess;  // ❌ POJO prohibits is prefix
    private Boolean isDeleted;  // ❌
}

// ✅ Auto-Fix to
@Data
public class User {
    private Boolean success;    // ✅ Remove is prefix
    private Boolean deleted;    // ✅
}

// ⚠️ Note: Method names can still use is prefix
public boolean isActive() {  // ✅ Method name is fine
    return this.status == Status.ACTIVE;
}
```

#### Pattern 5: Collection Naming
```java
// ❌ Detected Error
List<User> user;              // Should use plural
Map<Long, Order> orderMap;    // Map can keep Map suffix (but plural recommended)
Set<String> tagSet;           // Set can keep Set suffix (but plural recommended)

// ✅ Fix to
List<User> users;             // ✅ Plural
Map<Long, Order> orders;      // ✅ or orderMap (optional)
Set<String> tags;             // ✅ or tagSet (optional)
```

#### Pattern 6: Service/Dao Method Prefixes
```java
// ❌ Detected Error
public User findUserById(Long id) { }       // Service should use get
public List<User> getUserList() { }         // Should use list prefix
public int totalUsers() { }                 // Should use count prefix

// ✅ Auto-Fix to
public User getUserById(Long id) { }        // ✅ get for single object
public List<User> listUsers() { }           // ✅ list for collections
public int countUsers() { }                 // ✅ count for statistics
```

### Code Generation Standard Templates

#### Template 1: Controller Class
```java
// ✅ Naming Standard
@RestController
@RequestMapping("/api/v1/users")
public class UserController {  // ✅ UpperCamelCase + Controller suffix

    private final UserService userService;  // ✅ lowerCamelCase

    @GetMapping("/{id}")
    public ResponseDTO<UserVO> getUser(@PathVariable Long userId) {  // ✅ lowerCamelCase
        ...
    }
}
```

#### Template 2: Service Class
```java
// ✅ Naming Standard
@Service
public class UserService {  // ✅ UpperCamelCase + Service suffix

    private final UserMapper userMapper;
    private static final int MAX_RETRY_COUNT = 3;  // ✅ Constants all uppercase

    public Option<User> getUserById(Long id) {  // ✅ get prefix (single)
        ...
    }

    public List<User> listUsersByStatus(String status) {  // ✅ list prefix (collection)
        ...
    }

    public int countActiveUsers() {  // ✅ count prefix (statistics)
        ...
    }

    @Transactional
    public void saveUser(User user) {  // ✅ save prefix (save)
        ...
    }

    @Transactional
    public void deleteUser(Long id) {  // ✅ delete prefix (delete)
        ...
    }
}
```

#### Template 3: Entity/DTO Class
```java
// ✅ Naming Standard
@Data
@TableName("t_user")
public class User {  // ✅ UpperCamelCase (no Entity suffix)

    @TableId(type = IdType.AUTO)
    private Long userId;  // ✅ lowerCamelCase

    private String username;
    private String email;
    private Boolean deleted;  // ✅ Boolean no is prefix

    // ✅ Methods can use is prefix
    public boolean isActive() {
        return !deleted;
    }
}

// DTO example
public class UserCreateDTO {  // ✅ UpperCamelCase + DTO suffix
    private String username;
    private String email;
}

// VO example
public class UserVO {  // ✅ UpperCamelCase + VO suffix
    private Long userId;
    private String username;
}
```

#### Template 4: Constants Class/Enum
```java
// ✅ Constants Class
public class UserConstants {  // ✅ UpperCamelCase + Constants suffix

    // ✅ Constants all uppercase, underscore separated
    public static final int MAX_USERNAME_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final String DEFAULT_ROLE = "USER";

    private UserConstants() {  // Private constructor
        throw new UnsupportedOperationException();
    }
}

// ✅ Enum
public enum UserStatus {  // ✅ UpperCamelCase
    ACTIVE,     // ✅ Enum values all uppercase
    INACTIVE,
    DELETED
}
```

### Validation Commands
After generating code, automatically run the following commands to validate naming conventions:

```bash
# 1. Checkstyle check naming conventions
mvn checkstyle:check

# 2. ArchUnit check class naming suffixes
mvn test -Dtest=ArchitectureTest#controllerNaming
mvn test -Dtest=ArchitectureTest#serviceNaming
mvn test -Dtest=ArchitectureTest#daoNaming

# 3. View detailed errors (if failed)
mvn checkstyle:check -Dcheckstyle.console=true
```

**Expected Result**: 0 errors, 0 warnings

---

## Mandatory Level Rules

### 1. Class Naming
- **Rule**: Use UpperCamelCase, except domain models
- **Correct**: `UserService`, `OrderDTO`, `PaymentVO`
- **Incorrect**: `userService`, `orderDto`
```java
// ✅ Correct
public class UserServiceImpl implements UserService { }
public class OrderDTO { }

// ❌ Incorrect
public class userService { }
public class Orderdto { }
```

### 2. Method/Variable Naming
- **Rule**: Use lowerCamelCase
- **Correct**: `getUserById()`, `localValue`
- **Incorrect**: `GetUserById()`, `LocalValue`

### 3. Constant Naming
- **Rule**: All uppercase, underscore separated
- **Correct**: `MAX_RETRY_COUNT = 3`
- **Incorrect**: `maxRetryCount = 3`

### 4. Boolean Variables
- **Rule**: Prohibit is prefix (POJO classes)
- **Correct**: `private Boolean success;`
- **Incorrect**: `private Boolean isSuccess;`
- **Reason**: Some frameworks have serialization issues when parsing getter/setter

### 5. Service/DAO Method Naming
| Operation Type | Prefix        | Example               |
| -------------- | ------------- | --------------------- |
| Get Single     | get           | `getUserById()`       |
| Get List       | list          | `listUsersByStatus()` |
| Count          | count         | `countActiveUsers()`  |
| Add/Save       | save/insert   | `saveUser()`          |
| Delete         | delete/remove | `deleteUserById()`    |
| Update         | update        | `updateUserStatus()`  |

### 6. Package Naming
- **Rule**: All lowercase, singular form
- **Correct**: `com.example.user.service`
- **Incorrect**: `com.example.Users.Services`

## Automatic Check Configuration
```xml
<!-- Checkstyle Configuration -->
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
