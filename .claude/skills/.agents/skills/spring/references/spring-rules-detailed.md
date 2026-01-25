# SmartAdmin Spring Rules - Detailed Specification

> Comprehensive rule definitions for Spring Pattern Checker

This document provides detailed specifications for all SmartAdmin Spring rules enforced by the `/spring` skill and `ArchitectureTest.java`.

---

## Rule 1: @Transactional Placement

### Severity: CRITICAL

### Rule Statement
`@Transactional` annotation MUST ONLY appear in classes ending with `Manager` (Manager layer).

### Rationale
- **Explicit transaction boundaries**: Transaction scope must be clearly visible and managed in a dedicated layer
- **Prevent hidden nesting**: Prevents accidental nested transactions with unpredictable propagation behavior
- **Proper rollback scope**: Ensures `rollbackFor = Throwable.class` is consistently applied
- **Architectural clarity**: Separates business logic (Service) from transactional concerns (Manager)

### Violations

#### Violation 1.1: @Transactional in Controller
```java
@RestController
public class UserController {
    @Transactional  // ❌ CRITICAL VIOLATION
    @PostMapping("/users")
    public ResponseDTO<Void> createUser() {
        // ...
    }
}
```

**Why wrong**: Controllers handle HTTP concerns, not transaction boundaries. Controllers should delegate to Service layer, which orchestrates business logic and may call transactional Managers.

**Fix**:
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/users")
    public ResponseDTO<Void> createUser() {
        return userService.createUser();  // Service handles business logic
    }
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;

    public ResponseDTO<Void> createUser() {
        return userManager.createUser();  // Manager handles transaction
    }
}

@Service
@RequiredArgsConstructor
public class UserManager {
    private final UserDao userDao;

    @Transactional(rollbackFor = Throwable.class)  // ✅ CORRECT
    public ResponseDTO<Void> createUser() {
        // Transactional logic here
    }
}
```

#### Violation 1.2: @Transactional in Service
```java
@Service
public class EmployeeService {
    @Transactional(rollbackFor = Exception.class)  // ❌ CRITICAL VIOLATION
    public ResponseDTO<Void> updateEmployee(EmployeeUpdateForm form) {
        // business logic
    }
}
```

**Why wrong**: Service layer orchestrates business logic but should not define transaction boundaries. If transaction is needed, create a Manager.

**Fix**:
```java
// EmployeeService.java - Orchestration only
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeManager employeeManager;

    public ResponseDTO<Void> updateEmployee(EmployeeUpdateForm form) {
        return employeeManager.updateEmployee(form);
    }
}

// EmployeeManager.java - Transaction boundary
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final RoleDao roleDao;

    @Transactional(rollbackFor = Throwable.class)  // ✅ CORRECT
    public ResponseDTO<Void> updateEmployee(EmployeeUpdateForm form) {
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.updateById(entity);

        // Multiple Dao operations in single transaction
        if (form.getRoleIds() != null) {
            roleDao.updateByEmployeeId(entity.getId(), form.getRoleIds());
        }

        return ResponseDTO.ok();
    }
}
```

#### Violation 1.3: Wrong rollbackFor scope
```java
@Service
public class UserManager {
    @Transactional  // ❌ HIGH VIOLATION - Missing rollbackFor
    public void updateUser() { }

    @Transactional(rollbackFor = Exception.class)  // ❌ HIGH VIOLATION - Should be Throwable.class
    public void deleteUser() { }
}
```

**Why wrong**:
- Default rollback behavior only rolls back on `RuntimeException` and `Error`, not checked exceptions
- `Exception.class` misses `Error` and `Throwable` subclasses

**Fix**:
```java
@Service
public class UserManager {
    @Transactional(rollbackFor = Throwable.class)  // ✅ CORRECT
    public void updateUser() { }

    @Transactional(rollbackFor = Throwable.class)  // ✅ CORRECT
    public void deleteUser() { }
}
```

### Detection Logic
```python
def check_transactional_placement(file_path, content):
    violations = []

    # Check if file has @Transactional
    if '@Transactional' in content:
        # Determine layer by class name
        class_name = extract_class_name(content)

        if class_name.endswith('Controller'):
            violations.append({
                'severity': 'CRITICAL',
                'rule': 'Rule 1.1',
                'message': '@Transactional in Controller layer',
                'layer': 'Controller'
            })
        elif class_name.endswith('Service'):
            violations.append({
                'severity': 'CRITICAL',
                'rule': 'Rule 1.2',
                'message': '@Transactional in Service layer',
                'layer': 'Service'
            })
        elif class_name.endswith('Manager'):
            # Check rollbackFor attribute
            if 'rollbackFor = Throwable.class' not in content:
                violations.append({
                    'severity': 'HIGH',
                    'rule': 'Rule 1.3',
                    'message': '@Transactional missing rollbackFor = Throwable.class',
                    'layer': 'Manager'
                })
        else:
            violations.append({
                'severity': 'CRITICAL',
                'rule': 'Rule 1',
                'message': '@Transactional in unknown layer',
                'layer': 'Unknown'
            })

    return violations
```

### References
- ArchUnit test: `ArchitectureTest#managerLayerRules()`
- Documentation: `.agent/rules/09-manager-layer.md`
- Pattern guide: `.claude/shared/knowledge/smartadmin-patterns.md#transaction-management`

---

## Rule 2: Dependency Injection Style

### Severity: CRITICAL

### Rule Statement
All Spring Bean dependencies MUST use constructor injection with `@RequiredArgsConstructor` and `private final` fields. Field injection with `@Autowired` is FORBIDDEN.

### Rationale
- **Immutability**: `final` fields ensure dependencies cannot be changed after construction
- **Explicit dependencies**: Constructor signature clearly shows all dependencies
- **Testability**: Easy to create instances in tests with mock dependencies
- **Circular dependency prevention**: Constructor injection fails fast on circular dependencies
- **NPE prevention**: Cannot construct bean with null dependencies

### Violations

#### Violation 2.1: Field injection
```java
@Service
public class UserService {
    @Autowired  // ❌ CRITICAL VIOLATION
    private UserDao userDao;

    @Autowired  // ❌ CRITICAL VIOLATION
    private UserManager userManager;
}
```

**Why wrong**: Field injection hides dependencies, allows mutability, makes testing harder, and can hide circular dependency issues until runtime.

**Fix**:
```java
@Service
@RequiredArgsConstructor  // ✅ Lombok generates constructor
public class UserService {
    private final UserDao userDao;  // ✅ CORRECT - Constructor injected
    private final UserManager userManager;  // ✅ CORRECT
}
```

#### Violation 2.2: Setter injection
```java
@Service
public class EmployeeService {
    private EmployeeDao employeeDao;

    @Autowired  // ❌ CRITICAL VIOLATION
    public void setEmployeeDao(EmployeeDao dao) {
        this.employeeDao = dao;
    }
}
```

**Why wrong**: Setter injection allows dependencies to be changed after construction, violating immutability principle.

**Fix**:
```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;  // ✅ CORRECT
}
```

#### Violation 2.3: Mixed injection styles
```java
@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentDao departmentDao;  // ✅ Constructor injection

    @Autowired  // ❌ CRITICAL VIOLATION - Mixed style
    private CacheManager cacheManager;
}
```

**Why wrong**: Inconsistent dependency injection makes code harder to understand and maintain.

**Fix**:
```java
@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentDao departmentDao;  // ✅ CORRECT
    private final CacheManager cacheManager;  // ✅ CORRECT - All constructor injected
}
```

### Detection Logic
```python
def check_dependency_injection(file_path, content):
    violations = []

    # Check for @Autowired on fields
    autowired_field_pattern = r'@Autowired\s+private\s+\w+'
    if re.search(autowired_field_pattern, content):
        violations.append({
            'severity': 'CRITICAL',
            'rule': 'Rule 2.1',
            'message': 'Field injection with @Autowired',
            'pattern': '@Autowired on field'
        })

    # Check for @Autowired on setters
    autowired_setter_pattern = r'@Autowired\s+public\s+void\s+set\w+'
    if re.search(autowired_setter_pattern, content):
        violations.append({
            'severity': 'CRITICAL',
            'rule': 'Rule 2.2',
            'message': 'Setter injection with @Autowired',
            'pattern': '@Autowired on setter'
        })

    # Check if class has @Service/@Controller but no @RequiredArgsConstructor
    if ('@Service' in content or '@Controller' in content or '@RestController' in content):
        if '@RequiredArgsConstructor' not in content:
            # Check if there are any private final fields (likely dependencies)
            if 'private final' in content:
                violations.append({
                    'severity': 'HIGH',
                    'rule': 'Rule 2',
                    'message': 'Missing @RequiredArgsConstructor with private final fields',
                    'suggestion': 'Add @RequiredArgsConstructor to generate constructor'
                })

    return violations
```

### References
- ArchUnit test: `ArchitectureTest#dependencyRules()`
- Documentation: `.agent/rules/01-naming-conventions.md`
- Pattern guide: `.claude/shared/knowledge/smartadmin-patterns.md#dependency-injection`

---

## Rule 3: Layered Architecture Calls

### Severity: CRITICAL

### Rule Statement
Strict layered architecture must be followed: `Controller → Service → Manager → Dao`

**Allowed calls:**
- Controller → Service
- Service → Manager OR Dao
- Manager → Dao ONLY

**Forbidden calls:**
- Controller → Manager (skipping Service)
- Controller → Dao (skipping Service and Manager)
- Service → Service (horizontal coupling)
- Manager → Service (upward call)
- Manager → Manager (horizontal coupling at transaction layer)

### Rationale
- **Separation of concerns**: Each layer has distinct responsibilities
- **Prevent circular dependencies**: Strict hierarchy prevents dependency cycles
- **Transaction clarity**: Manager layer provides clear transaction boundaries
- **Testability**: Each layer can be tested independently
- **Maintainability**: Changes in one layer have predictable impact

### Violations

#### Violation 3.1: Controller → Manager (skipping Service)
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserManager userManager;  // ❌ CRITICAL VIOLATION

    @PostMapping("/users")
    public ResponseDTO<Void> createUser(UserAddForm form) {
        return userManager.createUser(form);  // Skipping Service layer
    }
}
```

**Why wrong**: Controllers should call Service layer for business logic orchestration. Direct Manager calls bypass business logic and validation.

**Fix**:
```java
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;  // ✅ CORRECT

    @PostMapping("/users")
    public ResponseDTO<Void> createUser(@Valid @RequestBody UserAddForm form) {
        return userService.createUser(form);
    }
}

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserManager userManager;

    public ResponseDTO<Void> createUser(UserAddForm form) {
        // Business logic, validation, etc.
        return userManager.createUser(form);
    }
}
```

#### Violation 3.2: Controller → Dao (skipping layers)
```java
@RestController
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeDao employeeDao;  // ❌ CRITICAL VIOLATION

    @GetMapping("/employees/{id}")
    public ResponseDTO<EmployeeVO> getById(@PathVariable Long id) {
        EmployeeEntity entity = employeeDao.selectById(id);
        return ResponseDTO.ok(SmartBeanUtil.copy(entity, EmployeeVO.class));
    }
}
```

**Why wrong**: Controllers directly accessing Dao bypasses all business logic, validation, and potential transaction management.

**Fix**:
```java
@RestController
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;  // ✅ CORRECT

    @GetMapping("/employees/{id}")
    public ResponseDTO<EmployeeVO> getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }
}

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;

    public ResponseDTO<EmployeeVO> getById(Long id) {
        EmployeeEntity entity = employeeDao.selectById(id);
        if (entity == null) {
            throw new BusinessException(EmployeeErrorCode.NOT_FOUND);
        }
        return ResponseDTO.ok(SmartBeanUtil.copy(entity, EmployeeVO.class));
    }
}
```

#### Violation 3.3: Service → Service (horizontal coupling)
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserService userService;  // ❌ CRITICAL VIOLATION
    private final ProductService productService;  // ❌ CRITICAL VIOLATION

    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        // Validate user
        userService.validateUser(form.getUserId());

        // Validate product
        productService.validateProduct(form.getProductId());

        // Create order...
    }
}
```

**Why wrong**: Service-to-Service calls create tight coupling and can lead to circular dependencies. If multiple Services are needed, use a Manager to orchestrate.

**Fix Option 1** - Extract to Manager:
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderManager orderManager;  // ✅ Delegate to Manager

    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        return orderManager.createOrder(form);
    }
}

@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // ✅ Manager calls Dao only
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

**Fix Option 2** - If no transaction needed, keep in Service with Dao calls:
```java
@Service
@RequiredArgsConstructor
public class OrderService {
    private final UserDao userDao;  // ✅ Service can call Dao
    private final ProductDao productDao;
    private final OrderDao orderDao;

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

        // Create order (single insert, no transaction needed)
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);

        return ResponseDTO.ok();
    }
}
```

#### Violation 3.4: Manager → Service (upward call)
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserService userService;  // ❌ CRITICAL VIOLATION
    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        // Validate user via Service
        userService.validateUser(form.getUserId());  // ❌ Manager calling Service

        // Create order...
    }
}
```

**Why wrong**: Manager layer is the transaction boundary and should not depend on Service layer (which may have its own business logic and dependencies).

**Fix**:
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // ✅ CORRECT - Call Dao directly
    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        // Validate user directly via Dao
        UserEntity user = userDao.selectById(form.getUserId());
        if (user == null) {
            throw new BusinessException(UserErrorCode.NOT_FOUND);
        }

        // Create order
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);

        return ResponseDTO.ok();
    }
}
```

#### Violation 3.5: Manager → Manager (transaction nesting risk)
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserManager userManager;  // ❌ CRITICAL VIOLATION
    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        userManager.updateLastOrderTime(form.getUserId());  // ❌ Nested transaction
        // ...
    }
}
```

**Why wrong**: Manager-to-Manager calls can create complex nested transactions with unpredictable propagation behavior and difficult-to-debug rollback issues.

**Fix**:
```java
@Service
@RequiredArgsConstructor
public class OrderManager {
    private final UserDao userDao;  // ✅ CORRECT - Call Dao directly
    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createOrder(OrderAddForm form) {
        // Update user directly in this transaction
        UserEntity user = userDao.selectById(form.getUserId());
        user.setLastOrderTime(LocalDateTime.now());
        userDao.updateById(user);

        // Create order
        OrderEntity order = SmartBeanUtil.copy(form, OrderEntity.class);
        orderDao.insert(order);

        return ResponseDTO.ok();
    }
}
```

### Detection Logic
```python
def check_layer_dependencies(file_path, content):
    violations = []

    class_name = extract_class_name(content)
    layer = determine_layer(class_name)
    dependencies = extract_dependencies(content)

    if layer == 'Controller':
        for dep in dependencies:
            dep_layer = determine_layer(dep)
            if dep_layer == 'Manager':
                violations.append({
                    'severity': 'CRITICAL',
                    'rule': 'Rule 3.1',
                    'message': 'Controller calls Manager (skipping Service)',
                    'dependency': dep
                })
            elif dep_layer == 'Dao':
                violations.append({
                    'severity': 'CRITICAL',
                    'rule': 'Rule 3.2',
                    'message': 'Controller calls Dao (skipping Service)',
                    'dependency': dep
                })

    elif layer == 'Service':
        for dep in dependencies:
            dep_layer = determine_layer(dep)
            if dep_layer == 'Service':
                violations.append({
                    'severity': 'CRITICAL',
                    'rule': 'Rule 3.3',
                    'message': 'Service calls another Service',
                    'dependency': dep
                })

    elif layer == 'Manager':
        for dep in dependencies:
            dep_layer = determine_layer(dep)
            if dep_layer == 'Service':
                violations.append({
                    'severity': 'CRITICAL',
                    'rule': 'Rule 3.4',
                    'message': 'Manager calls Service (upward call)',
                    'dependency': dep
                })
            elif dep_layer == 'Manager':
                violations.append({
                    'severity': 'CRITICAL',
                    'rule': 'Rule 3.5',
                    'message': 'Manager calls another Manager',
                    'dependency': dep
                })

    return violations
```

### References
- ArchUnit test: `ArchitectureTest#layerDependencyRules()`
- Documentation: `.agent/rules/10-architecture-rules.md`
- Pattern guide: `.claude/shared/knowledge/smartadmin-patterns.md#mandatory-layered-architecture`

---

## Rule 4: Spring Bean Naming Conventions

### Severity: HIGH

### Rule Statement
Spring Bean class names MUST match their annotation type:
- `@RestController` / `@Controller` → `*Controller`
- `@Service` (business logic) → `*Service`
- `@Service` (transaction/cache) → `*Manager`
- `@Repository` / MyBatis Mapper → `*Dao`
- `@Component` (utilities) → `*Util`, `*Helper`, `*Factory`, `*Provider`

### Rationale
- **Architectural clarity**: Class name immediately reveals its layer
- **Code navigation**: Easy to locate classes by layer
- **Convention over configuration**: Predictable naming reduces cognitive load
- **ArchUnit validation**: Enables automated architecture enforcement

### Violations

#### Violation 4.1: Controller without suffix
```java
@RestController
public class User {  // ❌ HIGH VIOLATION
    // ...
}
```

**Fix**:
```java
@RestController
public class UserController {  // ✅ CORRECT
    // ...
}
```

#### Violation 4.2: Service without suffix
```java
@Service
public class Employee {  // ❌ HIGH VIOLATION
    // ...
}
```

**Fix**:
```java
@Service
public class EmployeeService {  // ✅ CORRECT - Business logic
    // ...
}

// OR

@Service
public class EmployeeManager {  // ✅ CORRECT - Transaction/cache
    // ...
}
```

#### Violation 4.3: Dao with wrong suffix
```java
@Repository
public interface UserMapper extends BaseMapper<UserEntity> {  // ⚠️ MEDIUM VIOLATION
    // Should be UserDao for consistency
}
```

**Fix**:
```java
public interface UserDao extends BaseMapper<UserEntity> {  // ✅ CORRECT
    // MyBatis Plus Mapper - @Repository not needed
}
```

### Detection Logic
```python
def check_bean_naming(file_path, content):
    violations = []

    class_name = extract_class_name(content)
    annotations = extract_annotations(content)

    if '@RestController' in annotations or '@Controller' in annotations:
        if not class_name.endswith('Controller'):
            violations.append({
                'severity': 'HIGH',
                'rule': 'Rule 4.1',
                'message': 'Controller class must end with "Controller"',
                'class_name': class_name
            })

    if '@Service' in annotations:
        if not (class_name.endswith('Service') or class_name.endswith('Manager')):
            violations.append({
                'severity': 'HIGH',
                'rule': 'Rule 4.2',
                'message': 'Service class must end with "Service" or "Manager"',
                'class_name': class_name
            })

    if '@Repository' in annotations or 'extends BaseMapper' in content:
        if not class_name.endswith('Dao'):
            violations.append({
                'severity': 'MEDIUM',
                'rule': 'Rule 4.3',
                'message': 'Repository/Mapper should end with "Dao"',
                'class_name': class_name
            })

    return violations
```

### References
- ArchUnit test: `ArchitectureTest#namingConventionRules()`
- Documentation: `.agent/rules/01-naming-conventions.md`
- Pattern guide: `.claude/shared/knowledge/smartadmin-patterns.md#layer-responsibilities`

---

## Additional Rules

### Rule 5: @Cacheable Placement
**Severity**: HIGH

`@Cacheable`, `@CacheEvict`, `@CachePut` annotations ONLY in Manager layer (typically in classes named `*CacheManager`).

### Rule 6: Transaction Propagation
**Severity**: MEDIUM

Avoid explicit `propagation` settings unless absolutely necessary. Default `REQUIRED` is correct for 99% of cases.

### Rule 7: Read-only Transactions
**Severity**: LOW

Use `@Transactional(readOnly = true)` for read-heavy operations in Manager layer to enable optimizations.

```java
@Service
public class EmployeeCacheManager {
    @Transactional(readOnly = true)  // ✅ Optimization for read operations
    @Cacheable(AdminCacheConst.Employee.EMPLOYEE_LIST_CACHE)
    public List<EmployeeVO> listAll() {
        // ...
    }
}
```

---

## Version

**Document Version**: 1.0.0
**Last Updated**: 2026-01-23
**Aligned with**: SmartAdmin v4.0.0, ArchitectureTest.java (latest)
