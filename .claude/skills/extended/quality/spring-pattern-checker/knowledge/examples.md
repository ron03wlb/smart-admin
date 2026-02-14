# Spring Pattern Checker - Real-World Examples

**Version**: 1.0.0
**Last Updated**: 2026-02-02

This document provides complete, production-ready examples of Spring pattern violations detected and fixed in SmartAdmin codebase.

---

## Example 1: @Transactional in Service Layer Fix

### Problem Identification

**Class**: `EmployeeService.java:85-92`
**Violation**: `@Transactional` annotation in Service layer
**Rule**: @Transactional ONLY in Manager layer
**Severity**: 🚨 CRITICAL

### Original Code (Violating)

```java
package net.lab1024.sa.business.employee.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeDao employeeDao;
    private final RoleDao roleDao;

    /**
     * Create employee with roles
     * ❌ VIOLATION: @Transactional in Service layer
     */
    @Transactional(rollbackFor = Throwable.class)  // ← VIOLATION
    public ResponseDTO<Void> createEmployee(EmployeeAddForm form) {
        // Create employee
        EmployeeEntity employee = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.insert(employee);

        // Assign roles
        if (form.getRoleIds() != null) {
            roleDao.batchInsertEmployeeRoles(employee.getEmployeeId(), form.getRoleIds());
        }

        return ResponseDTO.ok();
    }
}
```

### Detection Output

```
╔══════════════════════════════════════════════════════════════════
║ Spring Pattern Violation Detected
╠══════════════════════════════════════════════════════════════════
║ File: EmployeeService.java:85
║ Violation: @Transactional in Service layer
║ Rule: Rule 1 - @Transactional Placement
║ Severity: CRITICAL
║
║ Issue:
║   @Transactional annotation found in Service layer class
║   EmployeeService.createEmployee() [Line 85]
║
║ Expected:
║   @Transactional should ONLY appear in Manager layer classes
║   (classes ending with "Manager")
║
║ Fix Suggestion:
║   1. Create EmployeeManager.java if not exists
║   2. Move transactional method to EmployeeManager
║   3. Update EmployeeService to delegate to EmployeeManager
╚══════════════════════════════════════════════════════════════════
```

### Fixed Code

**Step 1: Create EmployeeManager.java**

```java
package net.lab1024.sa.business.employee.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.employee.dao.EmployeeDao;
import net.lab1024.sa.business.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.business.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.system.role.dao.RoleDao;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeManager {

    private final EmployeeDao employeeDao;
    private final RoleDao roleDao;

    /**
     * Create employee with roles (transactional)
     * ✅ CORRECT: @Transactional in Manager layer
     */
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<Void> createEmployee(EmployeeAddForm form) {
        // Create employee
        EmployeeEntity employee = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeDao.insert(employee);

        // Assign roles
        if (form.getRoleIds() != null) {
            roleDao.batchInsertEmployeeRoles(employee.getEmployeeId(), form.getRoleIds());
        }

        return ResponseDTO.ok();
    }
}
```

**Step 2: Update EmployeeService.java**

```java
package net.lab1024.sa.business.employee.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.employee.domain.form.EmployeeAddForm;
import net.lab1024.sa.business.employee.manager.EmployeeManager;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeManager employeeManager;  // Changed: Inject Manager

    /**
     * Create employee with roles
     * ✅ CORRECT: Delegates to Manager for transactional operation
     */
    public ResponseDTO<Void> createEmployee(EmployeeAddForm form) {
        return employeeManager.createEmployee(form);  // Delegate to Manager
    }
}
```

### Verification

```bash
# Run ArchUnit tests
./gradlew :smartadmin-app:test --tests ArchitectureTest#managerLayerRules

# Expected output:
✅ ArchitectureTest > managerLayerRules() PASSED
   - All @Transactional annotations in Manager layer: PASS
   - All Manager methods use rollbackFor = Throwable.class: PASS
```

### Impact Analysis

| Metric | Before | After |
|--------|--------|-------|
| Violations | 1 (CRITICAL) | 0 |
| Layer Separation | ❌ Mixed | ✅ Clear |
| Transaction Boundaries | ❌ Implicit | ✅ Explicit |
| Testability | ⚠️ Harder | ✅ Easier |
| Time to Fix | - | 8 minutes |

---

## Example 2: Field Injection to Constructor Injection

### Problem Identification

**Class**: `UserService.java`
**Violation**: `@Autowired` field injection
**Rule**: Constructor injection ONLY
**Severity**: 🚨 CRITICAL

### Original Code (Violating)

```java
package net.lab1024.sa.business.user.service;

import net.lab1024.sa.business.user.dao.UserDao;
import net.lab1024.sa.business.user.manager.UserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /**
     * ❌ VIOLATION: Field injection
     */
    @Autowired
    private UserDao userDao;

    @Autowired
    private UserManager userManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public void updateUser(Long userId, String password) {
        // Business logic...
    }
}
```

### Detection Output

```
╔══════════════════════════════════════════════════════════════════
║ Spring Pattern Violations Detected (3)
╠══════════════════════════════════════════════════════════════════
║ File: UserService.java
║ Violation: Field injection detected
║ Rule: Rule 2 - Dependency Injection Style
║ Severity: CRITICAL
║
║ Issues:
║   1. @Autowired private UserDao userDao; [Line 12]
║   2. @Autowired private UserManager userManager; [Line 15]
║   3. @Autowired private PasswordEncoder passwordEncoder; [Line 18]
║
║ Expected:
║   Use constructor injection with @RequiredArgsConstructor
║
║ Quick Fix:
║   - Add @RequiredArgsConstructor to class
║   - Change all @Autowired fields to "private final"
║   - Remove @Autowired annotations
╚══════════════════════════════════════════════════════════════════
```

### Fixed Code

```java
package net.lab1024.sa.business.user.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.user.dao.UserDao;
import net.lab1024.sa.business.user.manager.UserManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * ✅ CORRECT: Constructor injection with @RequiredArgsConstructor
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDao userDao;
    private final UserManager userManager;
    private final PasswordEncoder passwordEncoder;

    public void updateUser(Long userId, String password) {
        // Business logic...
    }
}
```

### Verification

```bash
# Run ArchUnit tests
./gradlew :smartadmin-app:test --tests ArchitectureTest#noFieldInjection

# Expected output:
✅ ArchitectureTest > noFieldInjection() PASSED
   - No @Autowired field injection detected: PASS
```

### Impact Analysis

| Metric | Before | After |
|--------|--------|-------|
| Violations | 3 (CRITICAL) | 0 |
| Immutability | ❌ Mutable fields | ✅ Immutable (final) |
| Testability | ⚠️ Harder (reflection needed) | ✅ Easy (constructor) |
| Circular Dependencies | ⚠️ Hidden | ✅ Compile-time detection |
| Time to Fix | - | 2 minutes |

---

## Example 3: Controller → Manager (Skipping Service Layer)

### Problem Identification

**Class**: `UserController.java`
**Violation**: Controller directly calls Manager
**Rule**: Controller → Service → Manager → Dao
**Severity**: 🚨 CRITICAL

### Original Code (Violating)

```java
package net.lab1024.sa.business.user.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.user.domain.form.UserAddForm;
import net.lab1024.sa.business.user.manager.UserManager;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

/**
 * ❌ VIOLATION: Controller directly injects Manager
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserManager userManager;  // ← VIOLATION: Should inject Service

    @PostMapping("/add")
    public ResponseDTO<Void> add(@RequestBody UserAddForm form) {
        return userManager.createUser(form);  // Skipping Service layer
    }
}
```

### Detection Output

```
╔══════════════════════════════════════════════════════════════════
║ Spring Pattern Violation Detected
╠══════════════════════════════════════════════════════════════════
║ File: UserController.java:15
║ Violation: Controller → Manager (skipping Service)
║ Rule: Rule 3 - Layered Architecture Calls
║ Severity: CRITICAL
║
║ Issue:
║   Controller directly injects and calls Manager layer
║   UserController → UserManager (missing Service layer)
║
║ Expected:
║   Controller → Service → Manager
║
║ Fix Suggestion:
║   1. Create UserService if not exists
║   2. Move Manager call to UserService
║   3. Update UserController to inject UserService
╚══════════════════════════════════════════════════════════════════
```

### Fixed Code

**Step 1: Create/Update UserService.java**

```java
package net.lab1024.sa.business.user.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.user.domain.form.UserAddForm;
import net.lab1024.sa.business.user.manager.UserManager;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

/**
 * ✅ CORRECT: Service layer mediates between Controller and Manager
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserManager userManager;

    public ResponseDTO<Void> createUser(UserAddForm form) {
        return userManager.createUser(form);
    }
}
```

**Step 2: Update UserController.java**

```java
package net.lab1024.sa.business.user.controller;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.user.domain.form.UserAddForm;
import net.lab1024.sa.business.user.service.UserService;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.*;

/**
 * ✅ CORRECT: Controller injects Service (not Manager)
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;  // Changed from UserManager

    @PostMapping("/add")
    public ResponseDTO<Void> add(@RequestBody UserAddForm form) {
        return userService.createUser(form);  // Call Service instead
    }
}
```

### Verification

```bash
# Run ArchUnit tests
./gradlew :smartadmin-app:test --tests ArchitectureTest#layerDependencyRules

# Expected output:
✅ ArchitectureTest > layerDependencyRules() PASSED
   - Controller → Service: PASS
   - Service → Manager: PASS
   - Manager → Dao: PASS
```

### Impact Analysis

| Metric | Before | After |
|--------|--------|-------|
| Violations | 1 (CRITICAL) | 0 |
| Layer Separation | ❌ Violated | ✅ Enforced |
| Flexibility | ⚠️ Tight coupling | ✅ Loose coupling |
| Testability | ⚠️ Harder | ✅ Easier (mock Service) |
| Time to Fix | - | 10 minutes |

---

## Example 4: Service → Service Cross-Dependency

### Problem Identification

**Class**: `OrderService.java`
**Violation**: Service calls another Service
**Rule**: Service cannot call Service (extract to Manager)
**Severity**: 🚨 CRITICAL

### Original Code (Violating)

```java
package net.lab1024.sa.business.order.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.product.service.ProductService;
import net.lab1024.sa.business.user.service.UserService;
import org.springframework.stereotype.Service;

/**
 * ❌ VIOLATION: Service → Service cross-dependency
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final UserService userService;  // ← VIOLATION
    private final ProductService productService;  // ← VIOLATION

    public void createOrder(Long userId, Long productId) {
        userService.validateUser(userId);  // Cross-service call
        productService.validateProduct(productId);  // Cross-service call
        // Create order...
    }
}
```

### Detection Output

```
╔══════════════════════════════════════════════════════════════════
║ Spring Pattern Violations Detected (2)
╠══════════════════════════════════════════════════════════════════
║ File: OrderService.java
║ Violation: Service → Service cross-dependency
║ Rule: Rule 3 - Layered Architecture Calls
║ Severity: CRITICAL
║
║ Issues:
║   1. OrderService → UserService [Line 13]
║   2. OrderService → ProductService [Line 14]
║
║ Expected:
║   Service should call Manager or Dao, not other Services
║
║ Fix Suggestion:
║   Extract validation logic to OrderManager
║   Call UserDao and ProductDao directly from Manager
╚══════════════════════════════════════════════════════════════════
```

### Fixed Code

**Step 1: Update OrderService.java**

```java
package net.lab1024.sa.business.order.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.order.manager.OrderManager;
import org.springframework.stereotype.Service;

/**
 * ✅ CORRECT: Service delegates to Manager
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderManager orderManager;  // Changed: Inject Manager only

    public void createOrder(Long userId, Long productId) {
        orderManager.createOrder(userId, productId);  // Delegate to Manager
    }
}
```

**Step 2: Create/Update OrderManager.java**

```java
package net.lab1024.sa.business.order.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.business.order.dao.OrderDao;
import net.lab1024.sa.business.product.dao.ProductDao;
import net.lab1024.sa.business.user.dao.UserDao;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.code.ProductErrorCode;
import net.lab1024.sa.common.core.domain.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ✅ CORRECT: Manager calls Dao directly (no cross-service)
 */
@Service
@RequiredArgsConstructor
public class OrderManager {

    private final UserDao userDao;  // Call Dao directly
    private final ProductDao productDao;  // Call Dao directly
    private final OrderDao orderDao;

    @Transactional(rollbackFor = Throwable.class)
    public void createOrder(Long userId, Long productId) {
        // Validate user directly via Dao
        UserEntity user = userDao.selectById(userId);
        if (user == null) {
            throw new BusinessException(UserErrorCode.NOT_FOUND);
        }

        // Validate product directly via Dao
        ProductEntity product = productDao.selectById(productId);
        if (product == null) {
            throw new BusinessException(ProductErrorCode.NOT_FOUND);
        }

        // Create order
        OrderEntity order = new OrderEntity();
        order.setUserId(userId);
        order.setProductId(productId);
        orderDao.insert(order);
    }
}
```

### Verification

```bash
# Run ArchUnit tests
./gradlew :smartadmin-app:test --tests ArchitectureTest

# Expected output:
✅ ArchitectureTest > layerDependencyRules() PASSED
✅ ArchitectureTest > managerLayerRules() PASSED
```

### Impact Analysis

| Metric | Before | After |
|--------|--------|-------|
| Violations | 2 (CRITICAL) | 0 |
| Cross-Dependencies | ❌ Service ↔ Service | ✅ None |
| Transaction Boundaries | ⚠️ Unclear | ✅ Clear (Manager) |
| Testability | ⚠️ Complex mocking | ✅ Simple mocking |
| Time to Fix | - | 15 minutes |

---

## Summary

### Fix Effectiveness

| Example | Violation Type | Time to Fix | Difficulty | Lines Changed |
|---------|---------------|-------------|------------|---------------|
| Example 1 | @Transactional in Service | 8 min | Easy | +25, -5 |
| Example 2 | Field Injection | 2 min | Easy | +1, -4 |
| Example 3 | Controller → Manager | 10 min | Medium | +15, -1 |
| Example 4 | Service → Service | 15 min | Medium | +30, -3 |

### Key Takeaways

1. **@Transactional Placement**: Always extract to Manager layer (5-10 min fix)
2. **Constructor Injection**: One-line fix with `@RequiredArgsConstructor`
3. **Layer Hierarchy**: Enforce Controller → Service → Manager → Dao (10-15 min fix)
4. **Cross-Service Calls**: Extract to Manager for proper layer separation (15 min fix)

### Testing Strategy

All fixes should be verified with:
1. ArchUnit tests (`./gradlew :smartadmin-app:test --tests ArchitectureTest`)
2. Spring Pattern Check (`/spring`)
3. Full build (`./gradlew clean build`)
4. Manual code review (checklist in quick-reference.md)

---

**See Also**:
- [Quick Reference](quick-reference.md) - Command reference and quick fix matrix
- [Quick Fix Guide](../references/quick-fix-guide.md) - Detailed fix procedures
- [Spring Rules Detailed](../references/spring-rules-detailed.md) - Complete rule explanations
