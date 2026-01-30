# SmartAdmin Spring Quick Fix Guide

> Fast reference for fixing common Spring pattern violations

Use this guide to quickly fix violations reported by `/spring` or `ArchitectureTest.java`.

---

## Fix Template Matrix

| Violation | Quick Fix | Time | Difficulty |
|-----------|-----------|------|------------|
| @Transactional in Service | Move to Manager | 5min | Easy |
| @Autowired field injection | Constructor injection | 2min | Easy |
| Controller → Manager | Add Service layer | 10min | Medium |
| Service → Service | Extract to Manager | 15min | Medium |
| Manager → Service | Call Dao directly | 5min | Easy |
| Manager → Manager | Inline or call Dao | 10min | Medium |
| Wrong bean naming | Rename class | 2min | Easy |

---

## 1. @Transactional in Service → Move to Manager

### Scenario
```java
// ❌ VIOLATION
@Service
public class EmployeeService {
    @Transactional
    public void updateEmployee(Long id, String name) {
        // ...
    }
}
```

### Quick Fix Steps

**Step 1**: Create `EmployeeManager.java` (if doesn't exist)
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee(Long id, String name) {
        // Move logic here
    }
}
```

**Step 2**: Update `EmployeeService.java`
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;  // Add dependency

    public void updateEmployee(Long id, String name) {
        employeeManager.updateEmployee(id, name);  // Delegate
    }
}
```

**Step 3**: Verify
```bash
./gradlew :sa-admin:test --tests ArchitectureTest#managerLayerRules
```

---

## 2. @Autowired Field Injection → Constructor Injection

### Scenario
```java
// ❌ VIOLATION
@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    @Autowired
    private UserManager userManager;
}
```

### Quick Fix (One-Liner)

```java
// ✅ FIXED
@Service
@RequiredArgsConstructor  // Add this
public class UserService {
    private final UserDao userDao;  // Change to final, remove @Autowired
    private final UserManager userManager;  // Change to final, remove @Autowired
}
```

### Quick Replace Regex
```regex
# Find
@Autowired\s+private\s+(\w+)\s+(\w+);

# Replace
private final $1 $2;
```

Then add `@RequiredArgsConstructor` to class.

---

## 3. Controller → Manager → Add Service Layer

### Scenario
```java
// ❌ VIOLATION
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;

    @PostMapping("/users")
    public ResponseDTO<Void> create() {
        return userManager.createUser();
    }
}
```

### Quick Fix Steps

**Step 1**: Create/update `UserService.java`
```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;

    public ResponseDTO<Void> createUser() {
        return userManager.createUser();
    }
}
```

**Step 2**: Update `UserController.java`
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // Changed from Manager

    @PostMapping("/users")
    public ResponseDTO<Void> create() {
        return userService.createUser();  // Changed from Manager
    }
}
```

---

## 4. Service → Service → Extract to Manager

### Scenario
```java
// ❌ VIOLATION
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserService userService;
    private final ProductService productService;

    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        userService.validateUser(form.getUserId());
        productService.validateProduct(form.getProductId());
        // create order...
    }
}
```

### Quick Fix: Extract to Manager

```java
// ✅ FIXED - OrderService.java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderManager orderManager;

    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        return orderManager.createOrder(form);
    }
}

// ✅ NEW - OrderManager.java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // Call Dao directly
    private final ProductDao productDao;
    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        // Validate user
        UserEntity user = userDao.selectById(form.getUserId());
        if (user == null) {
            throw new BusinessException(UserErrorCode.NOT_FOUND);
        }

        // Validate product
        ProductEntity product = productDao.selectById(form.getProductId());
        if (product == null) {
            throw new BusinessException(ProductErrorCode.NOT_FOUND);
        }

        // Create order
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);

        return ResponseDTO.ok();
    }
}
```

---

## 5. Manager → Service → Call Dao Directly

### Scenario
```java
// ❌ VIOLATION
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserService userService;  // Calling Service from Manager

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Long userId) {
        userService.validateUser(userId);  // ❌ Upward call
    }
}
```

### Quick Fix

```java
// ✅ FIXED
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // Changed to Dao

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Long userId) {
        // Validate directly via Dao
        UserEntity user = userDao.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.NOT_FOUND);
        }
    }
}
```

---

## 6. Manager → Manager → Inline or Dao

### Scenario
```java
// ❌ VIOLATION
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserManager userManager;  // Manager calling Manager

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Long userId) {
        userManager.updateLastOrderTime(userId);
    }
}
```

### Quick Fix Option 1: Inline Logic

```java
// ✅ FIXED
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // Call Dao directly

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Long userId) {
        // Inline the logic
        UserEntity user = userDao.selectById(userId);
        user.setLastOrderTime(LocalDateTime.now());
        userDao.updateById(user);
    }
}
```

### Quick Fix Option 2: If Complex, Split Transactions

```java
// ✅ FIXED - OrderService.java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserManager userManager;
    private final OrderManager orderManager;

    public ResponseDTO<Void> createOrder(Long userId) {
        // Execute in separate transactions
        userManager.updateLastOrderTime(userId);
        return orderManager.createOrder(userId);
    }
}
```

---

## 7. Wrong Bean Naming → Rename Class

### Scenario
```java
// ❌ VIOLATION
@RestController
public class User {  // Should be UserController
}

@Service
public class Employee {  // Should be EmployeeService or EmployeeManager
}
```

### Quick Fix

**IDE Refactor** (Recommended):
1. Right-click class name
2. Select "Refactor" → "Rename"
3. Enter new name with proper suffix
4. IDE updates all references automatically

**Manual** (if needed):
```java
// ✅ FIXED
@RestController
public class UserController {  // Added "Controller" suffix
}

@Service
public class EmployeeService {  // Added "Service" suffix
}
```

---

## Common Patterns Reference

### Pattern 1: Simple CRUD Service (No Manager Needed)

```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;  // Direct Dao access OK if no transaction

    public ResponseDTO<UserVO> getById(Long id) {
        UserEntity entity = userDao.selectById(id);
        return ResponseDTO.ok(SmartBeanUtil.copy(entity, UserVO.class));
    }
}
```

### Pattern 2: Transactional Operations (Manager Required)

```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;  // Delegate to Manager
}

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserDao userDao;
    private final RoleDao roleDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createUser(UserAddForm form) {
        // Multiple Dao operations
        UserEntity user = SmartBeanUtil.copy(form, UserEntity.class);
        userDao.insert(user);

        if (form.getRoleIds() != null) {
            roleDao.batchInsert(user.getId(), form.getRoleIds());
        }

        return ResponseDTO.ok();
    }
}
```

### Pattern 3: Cached Queries (CacheManager)

```java
@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentCacheManager cacheManager;
}

@Service
@RequiredArgsConstructor
public class DepartmentCacheManager {
    private final DepartmentDao departmentDao;

    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> listAll() {
        List<DepartmentEntity> list = departmentDao.selectList(null);
        return SmartBeanUtil.copyList(list, DepartmentVO.class);
    }

    @CacheEvict(value = AdminCacheConst.Department.DEPARTMENT_LIST_CACHE, allEntries = true)
    public void evictCache() {
        // Cache eviction
    }
}
```

---

## Verification Checklist

After fixing violations, run these checks:

### 1. ArchUnit Tests
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest
```

Should see:
```
✅ managerLayerRules() PASSED
✅ dependencyRules() PASSED
✅ layerDependencyRules() PASSED
✅ namingConventionRules() PASSED
```

### 2. Spring Skill Check
```bash
/spring
```

Should see:
```
✅ All checks passed
No violations found
```

### 3. Build & Run
```bash
./gradlew clean build
./gradlew :sa-admin:bootRun
```

### 4. Manual Code Review
- [ ] All `@Transactional` only in Manager classes
- [ ] All dependencies use constructor injection
- [ ] Controller → Service → Manager → Dao flow
- [ ] All Spring Beans have proper name suffixes

---

## Emergency Quick Fix Script

For batch fixing `@Autowired` field injection:

```bash
# Find files with @Autowired field injection
grep -r "@Autowired" --include="*.java" sa-admin/src/main/java/ \
  | grep "private" \
  | cut -d: -f1 \
  | sort -u

# For each file:
# 1. Replace @Autowired private with private final
# 2. Add @RequiredArgsConstructor to class
# 3. Remove @Autowired import
```

**⚠️ WARNING**: Always review changes manually before committing!

---

## When to Ask for Help

If you encounter:
- Complex nested transactions
- Circular dependency issues
- Unclear layer placement
- Performance concerns

**Ask on team chat or review with architect.**

---

## Version

**Document Version**: 1.0.0
**Last Updated**: 2026-01-23
**Aligned with**: SmartAdmin v4.0.0
