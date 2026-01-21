# Fix: EmployeeService.updatePassword() @Transactional Violation

> **Violation Type**: `@Transactional` in Service layer
> **Estimated Time**: 15 minutes
> **Difficulty**: Easy

---

## Problem Description

**Location**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/employee/service/EmployeeService.java:361`

**Issue**: The `updatePassword()` method has `@Transactional` annotation in the Service layer, violating the architecture rule that transactions should only be in the Manager layer.

```java
// Current (VIOLATION)
@Service
public class EmployeeService {

    @Transactional(rollbackFor = Throwable.class)  // ❌ Wrong layer
    public synchronized ResponseDTO<String> updatePassword(EmployeeUpdatePasswordForm form) {
        // Password update logic with database operations
        // 1. Update employee password
        // 2. Insert password log (if enabled)
    }
}
```

**Why this violates architecture**:
- Service layer should contain business logic and validation
- Manager layer should handle database transactions
- Mixing concerns makes code harder to test and maintain

---

## Solution Overview

**Strategy**: Move the transactional database operations to `EmployeeManager`, keep validation logic in `EmployeeService`.

**Changes Required**:
1. Add `updatePassword()` method to `EmployeeManager` (with `@Transactional`)
2. Remove `@Transactional` from `EmployeeService.updatePassword()`
3. Update `EmployeeService` to call the Manager method

---

## Step 1: Add Method to EmployeeManager

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/employee/manager/EmployeeManager.java`

**Add this method**:

```java
/**
 * Update employee password with password history logging.
 * This is transactional to ensure atomicity of password update and log insertion.
 *
 * @param employeeId The employee ID
 * @param encryptedPassword The new encrypted password
 * @param passwordLog The password history log entity (can be null if history tracking disabled)
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
```

**Dependencies needed** (add to EmployeeManager if not already present):

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {

    private final EmployeeDao employeeDao;
    private final PasswordLogDao passwordLogDao;

    // ... existing methods ...
    // ... add updatePassword() method above ...
}
```

**Key points**:
- `@Transactional` ensures atomic update: both password update and log insertion succeed or both fail
- `passwordLog` is nullable (depends on security configuration)
- Manager handles pure database operations, no validation logic

---

## Step 2: Update EmployeeService

**File**: `sa-admin/src/main/java/net/lab1024/sa/admin/module/system/employee/service/EmployeeService.java`

**Changes**:

1. **Remove** `@Transactional` annotation from `updatePassword()` method
2. **Update** method to call `employeeManager.updatePassword()`

```java
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
```

**Ensure EmployeeManager is injected** (should already exist):

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeManager employeeManager;
    // ... other dependencies ...
}
```

---

## Verification Checklist

After making the changes, verify the fix:

- [ ] `EmployeeManager` has `updatePassword()` method with `@Transactional(rollbackFor = Throwable.class)`
- [ ] `EmployeeManager` has `PasswordLogDao` dependency
- [ ] `EmployeeService.updatePassword()` has **NO** `@Transactional` annotation
- [ ] `EmployeeService` calls `employeeManager.updatePassword()` with correct parameters
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

### Verify No @Transactional in Service

```bash
# Search for remaining violations
grep -r "@Transactional" --include="*Service.java" sa-admin/src/main/java

# Expected: No matches (or only in test files)
```

---

## Troubleshooting

### Issue 1: PasswordLogDao Not Found in EmployeeManager

**Symptom**: Compilation error - `passwordLogDao` cannot be resolved.

**Solution**: Add `PasswordLogDao` to EmployeeManager dependencies:

```java
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final PasswordLogDao passwordLogDao;  // Add this
}
```

### Issue 2: Method Signature Mismatch

**Symptom**: Service cannot call Manager method.

**Solution**: Ensure parameters match exactly:
- `employeeId` (Long)
- `encryptedPassword` (String)
- `passwordLog` (PasswordLogEntity, nullable)

### Issue 3: ArchitectureTest Still Fails

**Check**:
1. Did you remove `@Transactional` from Service method?
2. Did you add `@Transactional` to Manager method?
3. Run clean build: `./gradlew clean :sa-admin:test --tests ArchitectureTest`

---

## Before/After Summary

### Before (Violation)
```
EmployeeService.updatePassword()
├─ @Transactional ❌ (wrong layer)
├─ Validation logic
└─ Database operations
```

### After (Fixed)
```
EmployeeService.updatePassword()
├─ Validation logic ✅
└─ Calls: employeeManager.updatePassword()

EmployeeManager.updatePassword()
├─ @Transactional ✅ (correct layer)
└─ Database operations
```

---

## Related Documentation

- **Architecture Overview**: [overview.md](./overview.md)
- **RoleService Fix**: [fix-role-transactional.md](./fix-role-transactional.md)
- **Testing Strategy**: [../testing-strategy.md](../testing-strategy.md)
- **Quick Reference**: [../quick-reference.md](../quick-reference.md)

---

## Next Steps

After fixing this violation:

1. Fix remaining violation: [RoleService](./fix-role-transactional.md)
2. Verify all architecture tests pass
3. Proceed to Phase 1: [Implementation Plan](../unit-test-implementation-plan.md)
