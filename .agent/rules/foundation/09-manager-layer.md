---
trigger: on_demand
description: Manager Layer Architecture Rules - Transaction Boundary and Cache Management
tags: [architecture, manager, transaction, cache, smart-admin]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
archunit_test: ArchitectureTest#managerShouldNotAccessBusinessService,ArchitectureTest#transactionalMustUseRollbackForThrowable
last_updated: 2026-01-25
---

# Manager Layer Architecture Rules

> **Core Principle**: @Transactional and cache annotations (@Cacheable/@CacheEvict/@CachePut) **only in Manager Layer**

---

## 🤖 AI Instructions Block

### When to Apply This Rule
- ✅ User requests to generate operations across multiple Mapper/DAO with transactions
- ✅ User requests to add caching functionality
- ✅ Service layer method becomes complex (calling multiple Mappers)
- ✅ Code Review detects Service layer using @Transactional/@Cacheable

### Mandatory Enforcement Checklist
- [ ] `@Transactional` only appears in Manager classes
- [ ] `@Cacheable/@CacheEvict/@CachePut` only appears in Manager classes
- [ ] Manager classes end with `Manager`
- [ ] Manager classes use Constructor Injection (`@RequiredArgsConstructor`)
- [ ] **Manager layer prohibits calling Service layer (strictly enforced)**
- [ ] **Manager layer prohibits calling other business Managers (avoid transaction nesting)**

### AI Decision Tree
```
Detect code generation request → Determine if Manager layer needed
  ├─ Need @Transactional? (multi-table operations)
  │   └─ Yes → Create Manager with @Transactional
  ├─ Need @Cacheable/@CacheEvict? (caching)
  │   └─ Yes → Create CacheManager with cache annotations
  ├─ Single Dao/Mapper operation? (single-table CRUD)
  │   └─ Yes → Service directly calls Dao (NO Manager needed)
  └─ Complex orchestration? (multiple Dao calls without transaction)
      └─ Optional → Can extract to Manager for reusability
```

### When Manager Layer is REQUIRED

**MUST use Manager layer** when:
1. ✅ Method needs `@Transactional` annotation
   - Multi-table insert/update/delete operations
   - Cascading delete (e.g., delete role + delete role_menu + delete role_employee)
   - Operations requiring atomicity across multiple steps

2. ✅ Method needs `@Cacheable/@CacheEvict/@CachePut` annotation
   - Query result caching
   - Cache invalidation management

3. ✅ Complex cross-table aggregation
   - Even without transaction, complex logic can be extracted to Manager for reusability

### When Manager Layer is NOT NEEDED

**Service can directly call Dao** when:
- ✅ Single-table query (selectById, selectList, query with pagination)
- ✅ Single-table insert (insert single entity)
- ✅ Single-table update (updateById, updatePassword, updateAvatar)
- ✅ Single-table delete (deleteById, logically delete)
- ✅ **As long as NO @Transactional or @Cacheable is needed**

---

## Practical Pattern Comparison

### Pattern A: Service Directly Calls Dao (Single-Table, No Transaction)

```java
// ✅ Correct: Single-table update, no transaction needed
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;

    public ResponseDTO<String> updateAvatar(EmployeeUpdateAvatarForm form) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setEmployeeId(form.getEmployeeId());
        entity.setAvatar(form.getAvatar());
        employeeDao.updateById(entity);  // ✅ Single update, no @Transactional
        return ResponseDTO.ok();
    }

    public ResponseDTO<String> resetPassword(Long employeeId) {
        String newPassword = securityPasswordService.randomPassword();
        String encryptedPassword = securityPasswordService.getEncryptPwd(newPassword);
        employeeDao.updatePassword(employeeId, encryptedPassword);  // ✅ Single update
        return ResponseDTO.ok(newPassword);
    }
}
```

### Pattern B: Service Delegates to Manager (Multi-Table, Requires Transaction)

```java
// ✅ Correct: Multi-table operation, delegate to Manager
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;

    public ResponseDTO<String> addEmployee(EmployeeAddForm form) {
        // Business validation in Service
        EmployeeEntity existing = employeeDao.getByLoginName(form.getLoginName());
        if (existing != null) {
            return ResponseDTO.userErrorParam("Login name duplicate");
        }

        // Delegate to Manager for transactional multi-table operation
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeManager.saveEmployeeTransaction(entity, form.getRoleIds());
        return ResponseDTO.ok();
    }
}

// Manager handles transaction
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final RoleEmployeeDao roleEmployeeDao;

    @Transactional(rollbackFor = Throwable.class)
    public void saveEmployeeTransaction(EmployeeEntity employee, List<Long> roleIds) {
        // Insert employee + insert role associations (atomic operation)
        employeeDao.insert(employee);
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleIds.forEach(roleId ->
                roleEmployeeDao.insert(new RoleEmployeeEntity(roleId, employee.getId())));
        }
    }
}
```

### Pattern C: WRONG - Service Uses @Transactional (VIOLATION)

```java
// ❌ VIOLATION: Service layer using @Transactional
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final RoleEmployeeDao roleEmployeeDao;

    @Transactional(rollbackFor = Throwable.class)  // ❌ Service cannot use @Transactional!
    public ResponseDTO<String> updateEmployee(EmployeeUpdateForm form) {
        employeeDao.updateById(employee);
        roleEmployeeDao.deleteByEmployeeId(employee.getId());
        roleEmployeeDao.batchInsert(form.getRoleIds(), employee.getId());
        return ResponseDTO.ok();
    }
}

// ✅ Fix: Move @Transactional to Manager
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ Correct in Manager
    public void updateEmployeeTransaction(EmployeeEntity employee, List<Long> roleIds) {
        employeeDao.updateById(employee);
        roleEmployeeDao.deleteByEmployeeId(employee.getId());
        roleEmployeeDao.batchInsert(roleIds, employee.getId());
    }
}
```

---

## Error Pattern Detection and Auto-Fix

### Pattern 1: Service Layer Using @Transactional
```java
// ❌ Service layer using transaction
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Exception.class)  // ❌ Prohibited
    public void saveEmployee(Employee e) { }
}

// ✅ Create EmployeeManager
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ Manager layer
    public void saveEmployee(Employee e, List<Long> roleIds) { }
}
```

### Pattern 2: Service Layer Using Cache Annotations
```java
// ❌ Service layer using cache
@Service
public class DepartmentService {
    @Cacheable("deptList")  // ❌ Prohibited
    public List<Department> listAll() { }
}

// ✅ Create DepartmentCacheManager
@Service
public class DepartmentCacheManager {
    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> getDepartmentList() { }
}
```

---

## Invocation Constraints (Strictly Enforced)

> For detailed layered architecture, refer to [10-architecture-rules.md](./10-architecture-rules.md)

| Constraint                 | Description                      |
| -------------------------- | -------------------------------- |
| **✗ Manager → Service**    | Prohibit upward invocation       |
| **✗ ManagerA → ManagerB**  | Prohibit lateral invocation (avoid transaction nesting) |
| **✓ Manager → DAO/Mapper** | Allow calling DAO layer          |
| **✓ Service → Manager**    | Allow Service calling Manager    |

---

## Manager Layer Three Responsibilities

### 1. Transaction Management (Across Multiple Mapper Operations)
```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeMapper employeeMapper;
    private final RoleEmployeeMapper roleEmployeeMapper;

    @Transactional(rollbackFor = Throwable.class)
    public void updateEmployee(Employee employee, List<Long> roleIds) {
        employeeMapper.updateById(employee);
        roleEmployeeMapper.deleteByEmployeeId(employee.getId());
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleEmployeeMapper.batchInsert(roleIds, employee.getId());
        }
    }
}
```

### 2. Cache Management
```java
@Service
public class DepartmentCacheManager {
    @Cacheable(AdminCacheConst.Department.DEPARTMENT_LIST_CACHE)
    public List<DepartmentVO> getDepartmentList() { }

    @CacheEvict(value = AdminCacheConst.Department.DEPARTMENT_LIST_CACHE, allEntries = true)
    public void clearCache() { }
}
```

### 3. Complex Business Orchestration (Only Call DAO/Mapper)
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final OrderMapper orderMapper;
    private final InventoryMapper inventoryMapper;  // ✅ Only call Mapper
    // ❌ private final InventoryService inventoryService;  // Prohibited

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Order order, Integer quantity) {
        inventoryMapper.deductStock(order.getProductId(), quantity);
        orderMapper.insert(order);
    }
}
```

---

## Transaction Annotation Rules

```java
// ✅ Correct: rollbackFor = Throwable.class
@Transactional(rollbackFor = Throwable.class)
public void saveEmployee(Employee employee) { }

// ❌ Incorrect: rollbackFor = Exception.class (cannot catch Error)
@Transactional(rollbackFor = Exception.class)

// ❌ Incorrect: No rollbackFor specified
@Transactional
```

---

## Naming Convention

- **Transaction Management**: `{Entity}Manager` (e.g., `EmployeeManager`)
- **Cache Management**: `{Entity}CacheManager` (e.g., `DepartmentCacheManager`)
- **Mixed Functionality**: `{Domain}Manager` (e.g., `LoginManager`)

---

## ArchUnit Test Rules

```java
@ArchTest
static final ArchRule transactionalOnlyInManager = methods()
    .that().areAnnotatedWith(Transactional.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
    .because("@Transactional can only be used in Manager layer");

@ArchTest
static final ArchRule cacheableOnlyInManager = methods()
    .that().areAnnotatedWith(Cacheable.class)
    .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
    .because("Cache annotations can only be used in Manager layer");
```

---

**Last Updated**: 2025-01-17
**Mandatory Level**: 🚫 ArchUnit blocks PR merge
