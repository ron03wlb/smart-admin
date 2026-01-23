---
trigger: on_demand
description: Manager Layer Architecture Rules - Transaction Boundary and Cache Management
tags: [architecture, manager, transaction, cache, smart-admin]
positioning: current-standard
ai_role: code_reviewer_and_generator
auto_apply: true
archunit_test: ArchitectureTest#managerLayerRules
last_updated: 2025-01-17
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
  ├─ Need transaction across multiple Mappers? → Yes → Create Manager
  ├─ Need caching? → Yes → Create CacheManager
  └─ Single Mapper operation? → No → Use Service layer only
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
