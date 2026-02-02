# Spring Pattern Checker - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: spring-pattern-checker (P1 - Extended/Quality)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Example |
|---------|---------|---------|
| Check Current Changes | Validate current uncommitted changes | /spring |
| Check Specific Files | Validate specific files or directories | /spring path/to/files |
| Full Project Scan | Validate entire project | /spring --all |
| Show Fixes | Display fix suggestions | /spring --show-fixes |
| Severity Filter | HIGH severity only | /spring --severity HIGH |

### Rapid Development Workflow

Time estimate: 5-15 minutes per violation

| Step | Action | Time |
|------|--------|------|
| 1. Detect Violations | Run /spring on changed files | ~1 min |
| 2. Review Report | Check violation types and locations | ~2 min |
| 3. Apply Quick Fixes | Use fix templates (see matrix below) | 2-15 min |
| 4. Verify | Run ArchUnit tests | ~3 min |
| 5. Build & Test | Ensure compilation and tests pass | ~5 min |

---

## 4 Core Spring Pattern Rules (Decision Matrix)

### Rule 1: @Transactional Placement

**Trigger Keywords**: "@Transactional", "transaction", "rollback", "Manager layer"

**Severity**: 🚨 CRITICAL

**Rule**: `@Transactional` ONLY in Manager layer (classes ending with "Manager")

**Violations**:
```java
// ❌ WRONG - Service layer
@Service
public class UserService {
    @Transactional  // VIOLATION
    public void updateUser() { }
}

// ❌ WRONG - Controller layer
@RestController
public class UserController {
    @Transactional  // VIOLATION
    public void deleteUser() { }
}
```

**Correct Pattern**:
```java
// ✅ CORRECT - Manager layer only
@Service
public class UserManager {
    @Transactional(rollbackFor = Throwable.class)  // CORRECT
    public void updateUser() { }
}
```

**Quick Fix Template**:
1. Create `*Manager.java` if doesn't exist
2. Move `@Transactional` method to Manager
3. Update Service to call Manager

**Time to Fix**: 5-10 minutes

**Why This Rule**: Transaction boundaries must be explicit and managed in dedicated layer to prevent hidden transaction nesting and ensure proper rollback scope.

**References**:
- `.agent/rules/foundation/09-manager-layer.md`
- `.claude/shared/knowledge/smartadmin-patterns.md#transaction-management`

---

### Rule 2: Dependency Injection Style

**Trigger Keywords**: "@Autowired", "field injection", "constructor injection", "@RequiredArgsConstructor"

**Severity**: 🚨 CRITICAL

**Rule**: Constructor injection ONLY via `@RequiredArgsConstructor` + `private final`

**Violations**:
```java
// ❌ WRONG - Field injection
@Service
public class UserService {
    @Autowired  // VIOLATION
    private UserDao userDao;
}

// ❌ WRONG - Setter injection
@Service
public class UserService {
    private UserDao userDao;

    @Autowired  // VIOLATION
    public void setUserDao(UserDao dao) {
        this.userDao = dao;
    }
}
```

**Correct Pattern**:
```java
// ✅ CORRECT - Constructor injection
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class UserService {
    private final UserDao userDao;  // CORRECT
    private final UserManager userManager;
}
```

**Quick Fix Template**:
1. Add `@RequiredArgsConstructor` to class
2. Change `@Autowired private Type field;` to `private final Type field;`
3. Remove `@Autowired` import

**Quick Replace Regex**:
```regex
# Find
@Autowired\s+private\s+(\w+)\s+(\w+);

# Replace
private final $1 $2;
```

**Time to Fix**: 2-5 minutes

**Why This Rule**: Constructor injection ensures immutability, makes dependencies explicit, enables easier testing, prevents circular dependency issues.

**References**:
- `.claude/shared/knowledge/smartadmin-patterns.md#dependency-injection`
- `.agent/rules/foundation/01-naming-conventions.md`

---

### Rule 3: Layered Architecture Calls

**Trigger Keywords**: "layered architecture", "Controller → Service", "Service → Manager", "layer dependency"

**Severity**: 🚨 CRITICAL

**Rule**: Strict call hierarchy - `Controller → Service → Manager → Dao`

**Violations**:

**Violation 1: Controller → Manager (Skipping Service)**
```java
// ❌ WRONG
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;  // VIOLATION

    @PostMapping("/users")
    public ResponseDTO<Void> add() {
        return userManager.createUser();  // Must call Service
    }
}
```

**Violation 2: Service → Service (Cross-Service Call)**
```java
// ❌ WRONG
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserService userService;  // VIOLATION
    private final ProductService productService;  // VIOLATION
}
```

**Violation 3: Manager → Service (Upward Call)**
```java
// ❌ WRONG
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserService userService;  // VIOLATION
}
```

**Correct Patterns**:
```java
// ✅ CORRECT: Controller → Service
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // CORRECT
}

// ✅ CORRECT: Service → Manager OR Dao
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;  // CORRECT
    private final UserDao userDao;  // CORRECT (if no Manager needed)
}

// ✅ CORRECT: Manager → Dao ONLY
@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserDao userDao;  // CORRECT
    private final RoleDao roleDao;  // CORRECT
}
```

**Quick Fix Templates**:
- Controller → Manager: Add Service layer (10 min)
- Service → Service: Extract to Manager (15 min)
- Manager → Service: Call Dao directly (5 min)

**Time to Fix**: 5-15 minutes (depends on complexity)

**Why This Rule**: Enforces separation of concerns, prevents circular dependencies, ensures transaction boundaries are clear, maintains testability.

**References**:
- `.claude/shared/knowledge/smartadmin-patterns.md#mandatory-layered-architecture`
- `.agent/rules/foundation/10-architecture-rules.md`

---

### Rule 4: Spring Bean Naming Conventions

**Trigger Keywords**: "bean naming", "class naming", "Controller suffix", "Service suffix"

**Severity**: ⚠️ HIGH

**Rule**: Class names must match annotation type with proper suffix

**Violations**:
```java
// ❌ WRONG - Missing suffix
@RestController
public class User { }  // VIOLATION: Must be UserController

@Service
public class Employee { }  // VIOLATION: Must be EmployeeService or EmployeeManager

@Repository
public class Product { }  // VIOLATION: Must be ProductDao
```

**Correct Pattern**:
```java
// ✅ CORRECT - Proper suffixes
@RestController
public class UserController { }

@Service
public class EmployeeService { }

@Service
public class EmployeeManager { }

@Repository
public class ProductDao { }
```

**Naming Rules**:
- `@RestController` → `*Controller`
- `@Service` (business logic) → `*Service`
- `@Service` (transaction/cache) → `*Manager`
- `@Repository` / MyBatis Plus Mapper → `*Dao`

**Quick Fix**: IDE Refactor → Rename (2 minutes)

**Time to Fix**: 2 minutes

**Why This Rule**: Ensures code consistency, makes layer identification immediate, improves code navigation.

**References**:
- `.agent/rules/foundation/01-naming-conventions.md`

---

## Quick Fix Matrix

| Violation | Quick Fix | Time | Difficulty | Steps |
|-----------|-----------|------|------------|-------|
| @Transactional in Service | Move to Manager | 5-10 min | Easy | Create Manager → Move method → Update Service |
| @Autowired field injection | Constructor injection | 2-5 min | Easy | Add @RequiredArgsConstructor → Change to final |
| Controller → Manager | Add Service layer | 10 min | Medium | Create Service → Update Controller |
| Service → Service | Extract to Manager | 15 min | Medium | Create Manager → Inline Dao calls |
| Manager → Service | Call Dao directly | 5 min | Easy | Replace Service with Dao |
| Manager → Manager | Inline or Dao | 10 min | Medium | Inline logic OR split transactions |
| Wrong bean naming | Rename class | 2 min | Easy | IDE Refactor → Rename |

---

## Common Errors and Quick Fixes

### Error 1: @Transactional in Service Layer

**Symptom**: ArchitectureTest fails with "transactionalMustUseRollbackForThrowable" or "@Transactional in non-Manager layer"

**Cause**: Transaction annotation in Service instead of Manager

**Quick Fix**:
```java
// Step 1: Create Manager
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee(Long id, String name) {
        // Move logic here
    }
}

// Step 2: Update Service
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;

    public void updateEmployee(Long id, String name) {
        employeeManager.updateEmployee(id, name);  // Delegate
    }
}
```

**Verification**: `./gradlew :sa-admin:test --tests ArchitectureTest#managerLayerRules`

---

### Error 2: Field Injection Detected

**Symptom**: ArchitectureTest fails with "noFieldInjection"

**Cause**: Using `@Autowired` on fields instead of constructor injection

**Quick Fix (One-Liner)**:
```java
// ❌ Before
@Service
public class UserService {
    @Autowired
    private UserDao userDao;
}

// ✅ After
@Service
@RequiredArgsConstructor  // Add this
public class UserService {
    private final UserDao userDao;  // Remove @Autowired, add final
}
```

**Batch Fix Script**:
```bash
# Find files with field injection
grep -r "@Autowired" --include="*.java" sa-admin/src/main/java/ \
  | grep "private" \
  | cut -d: -f1 \
  | sort -u
```

**Verification**: `./gradlew :sa-admin:test --tests ArchitectureTest#noFieldInjection`

---

### Error 3: Controller Directly Calls Manager

**Symptom**: ArchitectureTest fails with "layerDependencyRules"

**Cause**: Controller skips Service layer and calls Manager directly

**Quick Fix**:
```java
// Step 1: Create/update Service
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;

    public ResponseDTO<Void> createUser() {
        return userManager.createUser();
    }
}

// Step 2: Update Controller
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // Changed from UserManager

    @PostMapping("/users")
    public ResponseDTO<Void> create() {
        return userService.createUser();  // Changed
    }
}
```

**Verification**: `./gradlew :sa-admin:test --tests ArchitectureTest#layerDependencyRules`

---

### Error 4: Service Calls Another Service

**Symptom**: ArchitectureTest fails with "Service cannot call Service"

**Cause**: Cross-service dependencies (Service → Service)

**Quick Fix (Extract to Manager)**:
```java
// ❌ Before
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserService userService;  // VIOLATION
    private final ProductService productService;  // VIOLATION
}

// ✅ After - Extract to Manager
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderManager orderManager;  // Delegate to Manager
}

@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // Call Dao directly
    private final ProductDao productDao;  // Call Dao directly

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder() {
        // Inline validation logic using Dao
    }
}
```

**Verification**: `./gradlew :sa-admin:test --tests ArchitectureTest`

---

### Error 5: Manager Calls Service (Upward Call)

**Symptom**: ArchitectureTest fails with "Manager cannot call Service"

**Cause**: Manager layer calling Service layer (violates hierarchy)

**Quick Fix (Call Dao Directly)**:
```java
// ❌ Before
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserService userService;  // VIOLATION

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Long userId) {
        userService.validateUser(userId);  // Upward call
    }
}

// ✅ After
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

**Verification**: `./gradlew :sa-admin:test --tests ArchitectureTest`

---

## Common Pattern Templates

### Pattern 1: Simple CRUD (No Manager Needed)

**Use When**: Single-table operations without transactions

```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserDao userDao;  // Direct Dao access OK

    public ResponseDTO<UserVO> getById(Long id) {
        UserEntity entity = userDao.selectById(id);
        return ResponseDTO.ok(SmartBeanUtil.copy(entity, UserVO.class));
    }
}
```

**Time to Implement**: 5-10 minutes

---

### Pattern 2: Transactional Operations (Manager Required)

**Use When**: Multi-table operations, need @Transactional

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
        UserEntity user = SmartBeanUtil.copy(form, UserEntity.class);
        userDao.insert(user);

        if (form.getRoleIds() != null) {
            roleDao.batchInsert(user.getId(), form.getRoleIds());
        }

        return ResponseDTO.ok();
    }
}
```

**Time to Implement**: 10-15 minutes

---

### Pattern 3: Cached Queries (CacheManager)

**Use When**: Need @Cacheable or @CacheEvict annotations

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

**Time to Implement**: 10-15 minutes

---

## Time Estimates (Production Data)

| Violation Type | Detection | Fix | Verification | Total |
|---------------|-----------|-----|--------------|-------|
| @Transactional in Service | 1 min | 5-10 min | 3 min | 9-14 min |
| Field Injection | 1 min | 2-5 min | 2 min | 5-8 min |
| Controller → Manager | 1 min | 10 min | 3 min | 14 min |
| Service → Service | 1 min | 15 min | 5 min | 21 min |
| Manager → Service | 1 min | 5 min | 3 min | 9 min |
| Manager → Manager | 1 min | 10 min | 3 min | 14 min |
| Wrong Bean Naming | 1 min | 2 min | 2 min | 5 min |

**Full Project Scan**: 5-10 minutes (depends on project size)

---

## Verification Checklist

Before committing, verify all checks pass:

### 1. ArchUnit Tests
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected Results**:
- ✅ `managerLayerRules()` PASSED
- ✅ `dependencyRules()` PASSED
- ✅ `layerDependencyRules()` PASSED
- ✅ `namingConventionRules()` PASSED

### 2. Spring Pattern Check
```bash
/spring
```

**Expected Results**:
- ✅ All checks passed
- ✅ No violations found

### 3. Build & Test
```bash
./gradlew clean build
./gradlew :sa-admin:bootRun
```

### 4. Manual Code Review Checklist
- [ ] All `@Transactional` only in Manager classes
- [ ] All dependencies use constructor injection (`@RequiredArgsConstructor` + `private final`)
- [ ] Controller → Service → Manager → Dao flow respected
- [ ] All Spring Beans have proper name suffixes
- [ ] No circular dependencies
- [ ] No `@Autowired` field injection

---

**See Also**:
- [Quick Fix Guide](../references/quick-fix-guide.md) - Detailed fix procedures for all 7 violations
- [Spring Rules Detailed](../references/spring-rules-detailed.md) - Complete rule explanations
- [Examples Guide](examples.md) - Real SmartAdmin violation fixes
- [SmartAdmin Patterns](../../../../.claude/shared/knowledge/smartadmin-patterns.md) - Complete pattern reference
- [Architecture Rules](../../../../.agent/rules/foundation/10-architecture-rules.md) - Comprehensive architecture rules
