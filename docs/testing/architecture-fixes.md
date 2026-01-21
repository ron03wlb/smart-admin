# SmartAdmin V3 Architecture Fixes Guide

> **Document Version**: 1.0
> **Created**: 2026-01-21
> **Priority**: MUST complete before writing tests

---

## Overview

Before implementing unit tests, the following architecture violations must be fixed. These violations are enforced by `ArchitectureTest.java` and will cause test failures if not addressed.

### Architectural Rules (Enforced by ArchUnit)

```
Controller -> Service -> Manager -> Dao -> Entity
```

| Rule | Description |
|------|-------------|
| `@Transactional` | ONLY in Manager layer |
| `rollbackFor` | Must use `Throwable.class`, not `Exception.class` |
| Manager Dependencies | Cannot call Service or other Managers |
| Injection | Constructor injection only (no `@Autowired` fields) |

---

## Phase 0: Pre-requisite Architecture Violations

### Violation 1: EmployeeService.updatePassword()

#### Problem Description

**Location**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/employee/service/EmployeeService.java` (line 361)

**Issue**: `@Transactional` annotation is used in the Service layer, violating the architecture rule that transactions should only be in the Manager layer.

```java
// Current (VIOLATION)
@Service
public class EmployeeService {

    @Transactional(rollbackFor = Throwable.class)  // Should not be here
    public synchronized ResponseDTO<String> updatePassword(EmployeeUpdatePasswordForm form) {
        // Password update logic with multiple database operations
        // ...
    }
}
```

#### Fix Steps

**Step 1**: Add method to `EmployeeManager.java`

```java
package net.lab1024.sa.admin.module.system.employee.manager;

import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.common.securityprotect.dao.PasswordLogDao;
import net.lab1024.sa.admin.module.support.securityprotect.domain.entity.PasswordLogEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeManager {

    private final EmployeeDao employeeDao;
    private final PasswordLogDao passwordLogDao;

    // ... existing methods ...

    /**
     * Update employee password with password history logging.
     * This is transactional to ensure atomicity.
     *
     * @param employeeId The employee ID
     * @param encryptedPassword The new encrypted password
     * @param passwordLog The password history log entity (optional, can be null if disabled)
     */
    @Transactional(rollbackFor = Throwable.class)
    public void updatePassword(Long employeeId, String encryptedPassword, PasswordLogEntity passwordLog) {
        // Update password
        EmployeeEntity updateEntity = new EmployeeEntity();
        updateEntity.setEmployeeId(employeeId);
        updateEntity.setLoginPwd(encryptedPassword);
        employeeDao.updateById(updateEntity);

        // Log password change if password history is enabled
        if (passwordLog != null) {
            passwordLogDao.insert(passwordLog);
        }
    }
}
```

**Step 2**: Update `EmployeeService.updatePassword()`

```java
package net.lab1024.sa.admin.module.system.employee.service;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeManager employeeManager;
    private final SecurityPasswordService securityPasswordService;
    // ... other dependencies ...

    // Remove @Transactional annotation
    public synchronized ResponseDTO<String> updatePassword(EmployeeUpdatePasswordForm form) {
        // 1. Validate current password
        EmployeeEntity employee = employeeDao.selectById(form.getEmployeeId());
        if (employee == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        // 2. Validate old password matches
        String oldPasswordSalt = generateSaltPassword(form.getOldPassword(), employee.getEmployeeUid());
        if (!passwordEncryptService.matches(oldPasswordSalt, employee.getLoginPwd())) {
            return ResponseDTO.userErrorParam("Current password is incorrect");
        }

        // 3. Validate new password complexity
        ResponseDTO<String> complexityResult = securityPasswordService.validatePasswordComplexity(form.getNewPassword());
        if (!complexityResult.getOk()) {
            return complexityResult;
        }

        // 4. Validate password not recently used
        RequestUser requestUser = AdminRequestUtil.getRequestUser();
        ResponseDTO<String> repeatResult = securityPasswordService.validatePasswordRepeatTimes(requestUser, form.getNewPassword());
        if (!repeatResult.getOk()) {
            return repeatResult;
        }

        // 5. Prepare password update
        String newPasswordSalt = generateSaltPassword(form.getNewPassword(), employee.getEmployeeUid());
        String encryptedPassword = passwordEncryptService.encrypt(newPasswordSalt);

        // 6. Prepare password log (if history tracking is enabled)
        PasswordLogEntity passwordLog = null;
        if (securityConfigProvider.getRegularChangePasswordNotAllowRepeatTimes() > 0) {
            passwordLog = new PasswordLogEntity();
            passwordLog.setUserId(employee.getEmployeeId());
            passwordLog.setUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
            passwordLog.setOldPassword(encryptedPassword);
            passwordLog.setCreateTime(LocalDateTime.now());
        }

        // 7. Execute update via Manager (transactional)
        employeeManager.updatePassword(employee.getEmployeeId(), encryptedPassword, passwordLog);

        return ResponseDTO.ok();
    }
}
```

---

### Violation 2: RoleService with @Transactional

#### Problem Description

**Location**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/role/service/RoleService.java` (lines 51+)

**Issues**:
1. `@Transactional` annotations in Service layer
2. Uses `rollbackFor = Exception.class` instead of `Throwable.class`

```java
// Current (VIOLATIONS)
@Service
public class RoleService {

    @Transactional(rollbackFor = Exception.class)  // Wrong layer AND wrong rollbackFor
    public ResponseDTO<Long> addRole(RoleAddForm form) {
        // ...
    }

    @Transactional(rollbackFor = Exception.class)  // Wrong layer AND wrong rollbackFor
    public ResponseDTO<String> updateRole(RoleUpdateForm form) {
        // ...
    }

    @Transactional(rollbackFor = Exception.class)  // Wrong layer AND wrong rollbackFor
    public ResponseDTO<String> deleteRole(Long roleId) {
        // ...
    }
}
```

#### Fix Steps

**Step 1**: Create `RoleManager.java`

```java
package net.lab1024.sa.admin.module.system.role.manager;

import net.lab1024.sa.admin.module.system.role.dao.RoleDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleMenuDao;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleMenuEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Role Manager - handles transactional operations for Role module.
 * All @Transactional methods should be in this class.
 */
@Service
@RequiredArgsConstructor
public class RoleManager {

    private final RoleDao roleDao;
    private final RoleMenuDao roleMenuDao;
    private final RoleEmployeeDao roleEmployeeDao;

    /**
     * Save a new role with its menu permissions.
     *
     * @param roleEntity The role entity to insert
     * @param roleMenuList The menu permissions for this role
     * @return The generated role ID
     */
    @Transactional(rollbackFor = Throwable.class)
    public Long saveRole(RoleEntity roleEntity, List<RoleMenuEntity> roleMenuList) {
        // Insert role
        roleDao.insert(roleEntity);
        Long roleId = roleEntity.getRoleId();

        // Insert menu permissions
        if (roleMenuList != null && !roleMenuList.isEmpty()) {
            for (RoleMenuEntity roleMenu : roleMenuList) {
                roleMenu.setRoleId(roleId);
                roleMenuDao.insert(roleMenu);
            }
        }

        return roleId;
    }

    /**
     * Update role and its menu permissions.
     *
     * @param roleEntity The role entity to update
     * @param roleMenuList The new menu permissions (will replace existing)
     */
    @Transactional(rollbackFor = Throwable.class)
    public void updateRole(RoleEntity roleEntity, List<RoleMenuEntity> roleMenuList) {
        // Update role
        roleDao.updateById(roleEntity);

        // Delete existing menu permissions
        roleMenuDao.deleteByRoleId(roleEntity.getRoleId());

        // Insert new menu permissions
        if (roleMenuList != null && !roleMenuList.isEmpty()) {
            for (RoleMenuEntity roleMenu : roleMenuList) {
                roleMenu.setRoleId(roleEntity.getRoleId());
                roleMenuDao.insert(roleMenu);
            }
        }
    }

    /**
     * Delete role and all associated data.
     *
     * @param roleId The role ID to delete
     */
    @Transactional(rollbackFor = Throwable.class)
    public void deleteRole(Long roleId) {
        // Delete role
        roleDao.deleteById(roleId);

        // Delete menu permissions
        roleMenuDao.deleteByRoleId(roleId);

        // Delete employee-role associations
        roleEmployeeDao.deleteByRoleId(roleId);
    }
}
```

**Step 2**: Update `RoleService.java`

```java
package net.lab1024.sa.admin.module.system.role.service;

import net.lab1024.sa.admin.module.system.role.manager.RoleManager;
// ... other imports ...

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleDao roleDao;
    private final RoleMenuDao roleMenuDao;
    private final RoleEmployeeDao roleEmployeeDao;
    private final RoleManager roleManager;  // Add manager dependency

    /**
     * Add a new role.
     * Validation is done in Service, transaction is handled by Manager.
     */
    // Remove @Transactional annotation
    public ResponseDTO<Long> addRole(RoleAddForm form) {
        // 1. Validate role name uniqueness
        RoleEntity existingRole = roleDao.selectByRoleName(form.getRoleName());
        if (existingRole != null) {
            return ResponseDTO.userErrorParam("Role name already exists");
        }

        // 2. Prepare role entity
        RoleEntity roleEntity = SmartBeanUtil.copy(form, RoleEntity.class);

        // 3. Prepare menu permissions
        List<RoleMenuEntity> roleMenuList = buildRoleMenuList(form.getMenuIdList());

        // 4. Save via Manager (transactional)
        Long roleId = roleManager.saveRole(roleEntity, roleMenuList);

        return ResponseDTO.ok(roleId);
    }

    /**
     * Update an existing role.
     * Validation is done in Service, transaction is handled by Manager.
     */
    // Remove @Transactional annotation
    public ResponseDTO<String> updateRole(RoleUpdateForm form) {
        // 1. Validate role exists
        RoleEntity existingRole = roleDao.selectById(form.getRoleId());
        if (existingRole == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        // 2. Validate role name uniqueness (exclude current role)
        RoleEntity duplicateRole = roleDao.selectByRoleNameExcludeId(form.getRoleName(), form.getRoleId());
        if (duplicateRole != null) {
            return ResponseDTO.userErrorParam("Role name already exists");
        }

        // 3. Prepare role entity
        RoleEntity roleEntity = SmartBeanUtil.copy(form, RoleEntity.class);

        // 4. Prepare menu permissions
        List<RoleMenuEntity> roleMenuList = buildRoleMenuList(form.getMenuIdList());

        // 5. Update via Manager (transactional)
        roleManager.updateRole(roleEntity, roleMenuList);

        return ResponseDTO.ok();
    }

    /**
     * Delete a role.
     * Validation is done in Service, transaction is handled by Manager.
     */
    // Remove @Transactional annotation
    public ResponseDTO<String> deleteRole(Long roleId) {
        // 1. Validate role exists
        RoleEntity existingRole = roleDao.selectById(roleId);
        if (existingRole == null) {
            return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
        }

        // 2. Check if role is assigned to any employees
        Long employeeCount = roleEmployeeDao.countByRoleId(roleId);
        if (employeeCount > 0) {
            return ResponseDTO.userErrorParam("Cannot delete role: " + employeeCount + " employees are assigned to this role");
        }

        // 3. Delete via Manager (transactional)
        roleManager.deleteRole(roleId);

        return ResponseDTO.ok();
    }

    /**
     * Helper method to build RoleMenuEntity list from menu IDs.
     */
    private List<RoleMenuEntity> buildRoleMenuList(List<Long> menuIdList) {
        if (menuIdList == null || menuIdList.isEmpty()) {
            return List.of();
        }
        return menuIdList.stream()
            .map(menuId -> {
                RoleMenuEntity entity = new RoleMenuEntity();
                entity.setMenuId(menuId);
                return entity;
            })
            .toList();
    }
}
```

---

## Verification Steps

### Step 1: Run Architecture Test

After making the fixes, verify that all architecture rules pass:

```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected Output**:
```
BUILD SUCCESSFUL
```

### Step 2: Verify No @Transactional in Service Layer

Search for any remaining `@Transactional` annotations in Service classes:

```bash
# In the sa-admin/src/main/java directory
grep -r "@Transactional" --include="*Service.java" .
```

**Expected Output**: No matches (or only in test files)

### Step 3: Verify rollbackFor Uses Throwable

Search for any remaining `Exception.class` usage:

```bash
grep -r "rollbackFor = Exception.class" --include="*.java" .
```

**Expected Output**: No matches

### Step 4: Run Full Test Suite

```bash
./gradlew :sa-admin:test
```

**Expected Output**:
```
BUILD SUCCESSFUL
```

---

## Checklist

### Violation 1: EmployeeService.updatePassword()

- [ ] Add `updatePassword()` method to `EmployeeManager`
- [ ] Add `PasswordLogDao` dependency to `EmployeeManager`
- [ ] Remove `@Transactional` from `EmployeeService.updatePassword()`
- [ ] Update `EmployeeService` to call `employeeManager.updatePassword()`
- [ ] Verify `ArchitectureTest` passes

### Violation 2: RoleService @Transactional

- [ ] Create new file: `RoleManager.java`
- [ ] Add `saveRole()` method to `RoleManager`
- [ ] Add `updateRole()` method to `RoleManager`
- [ ] Add `deleteRole()` method to `RoleManager`
- [ ] Remove `@Transactional` from `RoleService.addRole()`
- [ ] Remove `@Transactional` from `RoleService.updateRole()`
- [ ] Remove `@Transactional` from `RoleService.deleteRole()`
- [ ] Update `RoleService` to inject `RoleManager`
- [ ] Update methods to call corresponding Manager methods
- [ ] Verify `ArchitectureTest` passes

### Final Verification

- [ ] `./gradlew :sa-admin:test --tests ArchitectureTest` passes
- [ ] `./gradlew :sa-admin:test` passes (all tests)
- [ ] No `@Transactional` in any `*Service.java` files
- [ ] All `@Transactional` use `rollbackFor = Throwable.class`

---

## Common Issues

### Issue 1: Missing DAO Method

**Problem**: `RoleMenuDao.deleteByRoleId()` may not exist.

**Solution**: Add the method to `RoleMenuDao`:

```java
public interface RoleMenuDao extends BaseMapper<RoleMenuEntity> {

    default void deleteByRoleId(Long roleId) {
        delete(Wrappers.<RoleMenuEntity>lambdaQuery()
            .eq(RoleMenuEntity::getRoleId, roleId));
    }
}
```

### Issue 2: Missing RoleEmployeeDao Method

**Problem**: `RoleEmployeeDao.deleteByRoleId()` or `countByRoleId()` may not exist.

**Solution**: Add the methods to `RoleEmployeeDao`:

```java
public interface RoleEmployeeDao extends BaseMapper<RoleEmployeeEntity> {

    default void deleteByRoleId(Long roleId) {
        delete(Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getRoleId, roleId));
    }

    default Long countByRoleId(Long roleId) {
        return selectCount(Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getRoleId, roleId));
    }
}
```

### Issue 3: Circular Dependency

**Problem**: If `RoleManager` depends on `RoleService` (or vice versa in an unexpected way).

**Solution**: Manager should ONLY depend on DAO classes. If business logic is needed, it should remain in the Service layer.

```
Correct: Service -> Manager -> Dao
Incorrect: Manager -> Service (creates circular dependency)
```

---

## Related Documentation

- [Implementation Plan](./unit-test-implementation-plan.md) - Overall testing plan
- [Testing Strategy](./testing-strategy.md) - Mock strategy and best practices
- [Test Templates](./test-templates.md) - Code examples for test classes
- [Quick Start](./quick-start.md) - Getting started with running tests
