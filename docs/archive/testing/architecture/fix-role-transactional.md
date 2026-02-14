# Fix: RoleService @Transactional Violations

> **Violation Type**: `@Transactional` in Service layer + wrong `rollbackFor`
> **Estimated Time**: 30 minutes
> **Difficulty**: Medium

---

## Problem Description

**Location**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/role/service/RoleService.java:51+`

**Issues**:
1. `@Transactional` annotations in Service layer (should be in Manager)
2. Uses `rollbackFor = Exception.class` (should be `Throwable.class`)

```java
// Current (VIOLATIONS)
@Service
public class RoleService {

    @Transactional(rollbackFor = Exception.class)  // ❌ Wrong layer AND wrong rollbackFor
    public ResponseDTO<Long> addRole(RoleAddForm form) {
        // Insert role + role-menu associations
    }

    @Transactional(rollbackFor = Exception.class)  // ❌ Wrong layer AND wrong rollbackFor
    public ResponseDTO<String> updateRole(RoleUpdateForm form) {
        // Update role + replace role-menu associations
    }

    @Transactional(rollbackFor = Exception.class)  // ❌ Wrong layer AND wrong rollbackFor
    public ResponseDTO<String> deleteRole(Long roleId) {
        // Delete role + role-menu + role-employee associations
    }
}
```

**Why this violates architecture**:
- Service layer should handle validation and business logic
- Manager layer should handle transactional database operations
- `Throwable.class` catches more errors than `Exception.class` (includes `Error` subclasses)

---

## Solution Overview

**Strategy**: Create `RoleManager` class to handle transactional operations, update `RoleService` to delegate to Manager.

**Changes Required**:
1. Create new file: `RoleManager.java`
2. Add three transactional methods: `saveRole()`, `updateRole()`, `deleteRole()`
3. Remove `@Transactional` from `RoleService` methods
4. Update `RoleService` to call Manager methods

---

## Step 1: Create RoleManager

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/role/manager/RoleManager.java`

**Create this new file**:

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
 *
 * @author 1024创新实验室
 */
@Service
@RequiredArgsConstructor
public class RoleManager {

    private final RoleDao roleDao;
    private final RoleMenuDao roleMenuDao;
    private final RoleEmployeeDao roleEmployeeDao;

    /**
     * Save a new role with its menu permissions.
     * Atomically inserts role and its menu associations.
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
     * Atomically updates role and replaces menu associations.
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
     * Atomically deletes role, role-menu, and role-employee associations.
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

**Key points**:
- Three methods handle three transactional operations
- All use `rollbackFor = Throwable.class`
- Pure database operations, no validation logic
- Each method is atomic (all succeed or all fail)

---

## Step 2: Update RoleService

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/role/service/RoleService.java`

### 2.1 Add RoleManager Dependency

```java
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleDao roleDao;
    private final RoleMenuDao roleMenuDao;
    private final RoleEmployeeDao roleEmployeeDao;
    private final RoleManager roleManager;  // Add this dependency

    // ... methods ...
}
```

### 2.2 Update addRole() Method

```java
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
```

### 2.3 Update updateRole() Method

```java
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
```

### 2.4 Update deleteRole() Method

```java
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
```

### 2.5 Helper Method (Keep in Service)

```java
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
```

---

## Step 3: Add Missing DAO Methods

### 3.1 RoleMenuDao.deleteByRoleId()

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/role/dao/RoleMenuDao.java`

```java
public interface RoleMenuDao extends BaseMapper<RoleMenuEntity> {

    /**
     * Delete all menu permissions for a role
     */
    default void deleteByRoleId(Long roleId) {
        delete(Wrappers.<RoleMenuEntity>lambdaQuery()
            .eq(RoleMenuEntity::getRoleId, roleId));
    }
}
```

### 3.2 RoleEmployeeDao Methods

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/role/dao/RoleEmployeeDao.java`

```java
public interface RoleEmployeeDao extends BaseMapper<RoleEmployeeEntity> {

    /**
     * Delete all role assignments for employees with this role
     */
    default void deleteByRoleId(Long roleId) {
        delete(Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getRoleId, roleId));
    }

    /**
     * Count employees assigned to this role
     */
    default Long countByRoleId(Long roleId) {
        return selectCount(Wrappers.<RoleEmployeeEntity>lambdaQuery()
            .eq(RoleEmployeeEntity::getRoleId, roleId));
    }
}
```

---

## Verification Checklist

- [ ] Created new file: `RoleManager.java`
- [ ] `RoleManager` has `saveRole()` with `@Transactional(rollbackFor = Throwable.class)`
- [ ] `RoleManager` has `updateRole()` with `@Transactional(rollbackFor = Throwable.class)`
- [ ] `RoleManager` has `deleteRole()` with `@Transactional(rollbackFor = Throwable.class)`
- [ ] `RoleService.addRole()` has **NO** `@Transactional`
- [ ] `RoleService.updateRole()` has **NO** `@Transactional`
- [ ] `RoleService.deleteRole()` has **NO** `@Transactional`
- [ ] `RoleService` injects `RoleManager` dependency
- [ ] All Service methods call corresponding Manager methods
- [ ] Added `deleteByRoleId()` to `RoleMenuDao`
- [ ] Added `deleteByRoleId()` and `countByRoleId()` to `RoleEmployeeDao`
- [ ] Run architecture test:
  ```bash
  ./gradlew :sa-admin:test --tests ArchitectureTest
  ```
- [ ] Architecture test passes: `BUILD SUCCESSFUL`

---

## Testing the Fix

### Manual Testing

```bash
# Run architecture validation
./gradlew :sa-admin:test --tests ArchitectureTest

# Expected output
> Task :sa-admin:test
ArchitectureTest > testTransactionalOnlyInManager() PASSED
BUILD SUCCESSFUL
```

### Verify No Violations

```bash
# No @Transactional in Service
grep -r "@Transactional" --include="*Service.java" sa-admin/src/main/java
# Expected: No matches

# No Exception.class usage
grep -r "rollbackFor = Exception.class" sa-admin/src/main/java
# Expected: No matches
```

---

## Troubleshooting

### Issue 1: DAO Methods Not Found

**Symptom**: Compilation errors for `deleteByRoleId()` or `countByRoleId()`.

**Solution**: Add missing methods to DAO interfaces (see Step 3).

### Issue 2: RoleManager Not Found

**Symptom**: `RoleManager` cannot be resolved in `RoleService`.

**Solution**:
1. Ensure `RoleManager.java` is in correct package: `..role.manager`
2. Ensure `@Service` annotation on `RoleManager`
3. Rebuild project: `./gradlew clean build`

### Issue 3: Circular Dependency Error

**Symptom**: Application fails to start with circular dependency.

**Solution**: Manager should ONLY depend on DAO classes, never on Service.

```
Correct:   RoleService → RoleManager → RoleDao
Incorrect: RoleManager → RoleService (circular dependency)
```

### Issue 4: selectByRoleNameExcludeId() Not Found

**Symptom**: Method doesn't exist in `RoleDao`.

**Solution**: Add to `RoleDao`:
```java
default RoleEntity selectByRoleNameExcludeId(String roleName, Long excludeId) {
    return selectOne(Wrappers.<RoleEntity>lambdaQuery()
        .eq(RoleEntity::getRoleName, roleName)
        .ne(RoleEntity::getRoleId, excludeId));
}
```

---

## Before/After Summary

### Before (Violations)
```
RoleService
├─ addRole() with @Transactional ❌
├─ updateRole() with @Transactional ❌
├─ deleteRole() with @Transactional ❌
└─ rollbackFor = Exception.class ❌
```

### After (Fixed)
```
RoleService (Validation only)
├─ addRole() → calls roleManager.saveRole() ✅
├─ updateRole() → calls roleManager.updateRole() ✅
└─ deleteRole() → calls roleManager.deleteRole() ✅

RoleManager (Transactions)
├─ saveRole() with @Transactional(rollbackFor = Throwable.class) ✅
├─ updateRole() with @Transactional(rollbackFor = Throwable.class) ✅
└─ deleteRole() with @Transactional(rollbackFor = Throwable.class) ✅
```

---

## Common Pitfalls

| Pitfall | Problem | Solution |
|---------|---------|----------|
| Forgot to remove `@Transactional` | Still violates architecture | Remove all `@Transactional` from RoleService |
| Used `Exception.class` in Manager | Wrong error handling | Always use `Throwable.class` |
| Manager calls Service | Circular dependency | Manager should only call DAO |
| Missing DAO methods | Compilation errors | Add default methods to DAO interfaces |

---

## Related Documentation

- **Architecture Overview**: [overview.md](./overview.md)
- **EmployeeService Fix**: [fix-employee-transactional.md](./fix-employee-transactional.md)
- **Testing Strategy**: [../testing-strategy.md](../testing-strategy.md)
- **Quick Reference**: [../quick-reference.md](../quick-reference.md)

---

## Next Steps

After fixing both violations:

1. Verify all architecture tests pass:
   ```bash
   ./gradlew :sa-admin:test --tests ArchitectureTest
   ```
2. Run full test suite:
   ```bash
   ./gradlew :sa-admin:test
   ```
3. Proceed to Phase 1: [Implementation Plan](../unit-test-implementation-plan.md)
