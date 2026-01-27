# Foundation Package Migration Guide

**Version:** v4.0.0
**Last Updated:** 2026-01-23
**Migration Tool:** `./gradlew migrateToFoundation`

---

## Overview

SmartAdmin v4.0.0 completes the foundation package naming standardization by **removing all bridge classes** in `net.lab1024.sa.common.core.*`. Code must migrate to `net.lab1024.sa.foundation.domain.*` packages.

**Timeline:**
- **v3.7.0 (Q2 2026):** INFO warnings, migration tool released
- **v3.8.0 (Q2 2026):** WARN warnings
- **v3.9.0 (Q3 2026):** ERROR warnings - **LAST compatible version**
- **v4.0.0 (Q4 2026):** Bridge classes **REMOVED** (breaking change)

---

## Quick Migration

### Option 1: Automated Migration (Recommended)

```bash
cd smart-admin-api-java21-springboot3

# Run migration tool
./gradlew migrateToFoundation

# Verify changes
git diff

# Build and test
./gradlew clean build
./gradlew test
./gradlew :sa-admin:test --tests ArchitectureTest
```

**What the tool does:**
- Scans all `.java` files (excluding `SmartBeanUtil.java`)
- Updates import statements
- Updates static imports
- Updates JavaDoc `{@link}` references
- Generates migration report

### Option 2: Manual Migration

1. **Search and replace imports** in your IDE:
   ```java
   // OLD (v3.x)
   import net.lab1024.sa.common.core.domain.ResponseDTO;
   import net.lab1024.sa.common.core.domain.PageResult;
   import net.lab1024.sa.common.core.code.UserErrorCode;

   // NEW (v4.0.0+)
   import net.lab1024.sa.foundation.domain.response.ResponseDTO;
   import net.lab1024.sa.foundation.domain.response.PageResult;
   import net.lab1024.sa.foundation.domain.code.UserErrorCode;
   ```

2. **Verify no old imports remain:**
   ```bash
   grep -r "import net.lab1024.sa.common.core" src/ | grep -v "SmartBeanUtil"
   # Expected: 0 matches
   ```

3. **Build and test:**
   ```bash
   ./gradlew clean build
   ./gradlew test
   ```

---

## Package Mappings

### Domain Objects

| v3.x (REMOVED) | v4.0.0+ (Use This) |
|----------------|-------------------|
| `net.lab1024.sa.common.core.domain.ResponseDTO` | `net.lab1024.sa.foundation.domain.response.ResponseDTO` |
| `net.lab1024.sa.common.core.domain.PageResult` | `net.lab1024.sa.foundation.domain.response.PageResult` |
| `net.lab1024.sa.common.core.domain.PageParam` | `net.lab1024.sa.foundation.domain.request.PageParam` |
| `net.lab1024.sa.common.core.domain.RequestUser` | `net.lab1024.sa.foundation.domain.request.RequestUser` |

### Error Codes

| v3.x (REMOVED) | v4.0.0+ (Use This) |
|----------------|-------------------|
| `net.lab1024.sa.common.core.code.ErrorCode` | `net.lab1024.sa.foundation.domain.code.ErrorCode` |
| `net.lab1024.sa.common.core.code.SystemErrorCode` | `net.lab1024.sa.foundation.domain.code.SystemErrorCode` |
| `net.lab1024.sa.common.core.code.UserErrorCode` | `net.lab1024.sa.foundation.domain.code.UserErrorCode` |
| `net.lab1024.sa.common.core.code.UnexpectedErrorCode` | `net.lab1024.sa.foundation.domain.code.UnexpectedErrorCode` |

### Constants

| v3.x (REMOVED) | v4.0.0+ (Use This) |
|----------------|-------------------|
| `net.lab1024.sa.common.core.constant.StringConst` | `net.lab1024.sa.foundation.domain.constant.StringConst` |
| `net.lab1024.sa.common.core.constant.RequestHeaderConst` | `net.lab1024.sa.foundation.domain.constant.RequestHeaderConst` |

### Enumerations

| v3.x (REMOVED) | v4.0.0+ (Use This) |
|----------------|-------------------|
| `net.lab1024.sa.common.core.enumeration.BaseEnum` | `net.lab1024.sa.foundation.domain.enumeration.BaseEnum` |

### Exceptions

| v3.x (REMOVED) | v4.0.0+ (Use This) |
|----------------|-------------------|
| `net.lab1024.sa.common.core.exception.BusinessException` | `net.lab1024.sa.foundation.domain.exception.BusinessException` |

### Exception: SmartBeanUtil

**NOT migrated - remains unchanged:**
- ✅ `net.lab1024.sa.common.core.util.SmartBeanUtil`

This is intentional and documented in `build.gradle.kts`. Do NOT migrate this class.

---

## Migration Examples

### Example 1: Controller Layer

**Before (v3.x):**
```java
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.code.UserErrorCode;

@RestController
public class UserController {

    public ResponseDTO<UserVO> getUser(Long id) {
        if (id == null) {
            return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
        }
        UserVO user = userService.getById(id);
        return ResponseDTO.ok(user);
    }

    public ResponseDTO<PageResult<UserVO>> queryPage(UserQueryForm form) {
        PageResult<UserVO> result = userService.queryPage(form);
        return ResponseDTO.ok(result);
    }
}
```

**After (v4.0.0+):**
```java
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;

@RestController
public class UserController {

    public ResponseDTO<UserVO> getUser(Long id) {
        if (id == null) {
            return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
        }
        UserVO user = userService.getById(id);
        return ResponseDTO.ok(user);
    }

    public ResponseDTO<PageResult<UserVO>> queryPage(UserQueryForm form) {
        PageResult<UserVO> result = userService.queryPage(form);
        return ResponseDTO.ok(result);
    }
}
```

**Changes:** Only imports changed. Code logic remains identical.

### Example 2: Service Layer with Exception

**Before (v3.x):**
```java
import net.lab1024.sa.common.core.exception.BusinessException;
import net.lab1024.sa.common.core.code.UserErrorCode;

@Service
public class EmployeeService {

    public EmployeeVO getByUid(String employeeUid) {
        EmployeeEntity employee = employeeDao.selectByUid(employeeUid);
        if (employee == null) {
            throw new BusinessException(UserErrorCode.DATA_NOT_EXIST);
        }
        return SmartBeanUtil.copy(employee, EmployeeVO.class);
    }
}
```

**After (v4.0.0+):**
```java
import net.lab1024.sa.foundation.domain.exception.BusinessException;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;

@Service
public class EmployeeService {

    public EmployeeVO getByUid(String employeeUid) {
        EmployeeEntity employee = employeeDao.selectByUid(employeeUid);
        if (employee == null) {
            throw new BusinessException(UserErrorCode.DATA_NOT_EXIST);
        }
        return SmartBeanUtil.copy(employee, EmployeeVO.class);
    }
}
```

**Changes:** Only imports changed. `SmartBeanUtil` import remains unchanged.

### Example 3: Custom Error Code Enum

**Before (v3.x):**
```java
import net.lab1024.sa.common.core.code.ErrorCode;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(50001, "订单不存在"),
    ORDER_ALREADY_PAID(50002, "订单已支付");

    private final int code;
    private final String msg;

    // Constructor and methods...
}
```

**After (v4.0.0+):**
```java
import net.lab1024.sa.foundation.domain.code.ErrorCode;

public enum OrderErrorCode implements ErrorCode {
    ORDER_NOT_FOUND(50001, "订单不存在"),
    ORDER_ALREADY_PAID(50002, "订单已支付");

    private final int code;
    private final String msg;

    // Constructor and methods...
}
```

**Changes:** Only import changed. Enum definition unchanged.

---

## Troubleshooting

### Issue 1: "Package does not exist" after migration

**Cause:** Migration tool missed some imports or circular dependency

**Solution:**
```bash
# 1. Validate all old imports are removed
grep -r "net.lab1024.sa.common.core" src/ | grep -v "SmartBeanUtil"
# Expected: 0 matches

# 2. Manually replace any remaining imports

# 3. Clean and rebuild
./gradlew clean --refresh-dependencies
./gradlew build
```

### Issue 2: "Cannot find symbol ResponseDTO"

**Cause:** Import not updated or missing foundation:domain dependency

**Solution:**
```groovy
// Verify in build.gradle.kts
dependencies {
    implementation project(":sa-base:foundation:domain")
    // ...
}
```

### Issue 3: IDE shows errors after migration

**Solution:**
- **IntelliJ IDEA:** File → Invalidate Caches → Restart
- **Eclipse:** Project → Clean
- **VS Code:** Reload Java Language Server

### Issue 4: ArchitectureTest fails after migration

**Symptom:**
```
v4.0.0 removed all bridge classes. Use net.lab1024.sa.foundation.domain.* instead.
```

**Cause:** Code still depends on removed bridge packages

**Solution:**
```bash
# Find violating code
grep -rn "import net.lab1024.sa.common.core" src/ | grep -v "SmartBeanUtil"

# Migrate manually or re-run migration tool
./gradlew migrateToFoundation
```

---

## FAQ

### Q: Do I need to change any code logic?

**A:** No. Only import statements change. The API is 100% identical between bridge classes and foundation classes.

### Q: What about SmartBeanUtil?

**A:** **CRITICAL:** `SmartBeanUtil` stays in `net.lab1024.sa.common.core.util.*` - **DO NOT migrate it**. This is intentional and documented.

### Q: Can I mix old and new imports during transition?

**A:** Yes in v3.7.0-v3.9.0, but **NOT in v4.0.0+**. Bridge classes are completely removed in v4.0.0.

### Q: What if I can't migrate before v4.0.0?

**A:** Stay on v3.9.x. It receives security patches until **Q2 2027** (6 months after v4.0.0 release).

### Q: Will this affect my frontend code?

**A:** No. Frontend (Vue.js) is unaffected. This only impacts Java backend imports.

### Q: How do I verify migration is complete?

**A:**
```bash
# 1. No old imports (except SmartBeanUtil)
grep -r "import net.lab1024.sa.common.core" src/ | grep -v "SmartBeanUtil"
# Expected: 0 matches

# 2. Build succeeds
./gradlew clean build
# Expected: BUILD SUCCESSFUL

# 3. Architecture tests pass
./gradlew :sa-admin:test --tests ArchitectureTest
# Expected: All rules PASSING including noBridgeClassesInV4
```

### Q: What if I'm using a custom module extending SmartAdmin?

**A:** Run `./gradlew migrateToFoundation` in your custom module's root directory. The tool scans and updates all `.java` files.

---

## Extended Support

### v3.9.x Security Patches

- **Duration:** Until Q2 2027 (6 months after v4.0.0 release)
- **Scope:** Critical security vulnerabilities and breaking dependency updates
- **Not included:** New features, bug fixes, performance improvements

### Migration Support Window

- **Duration:** Q4 2026 - Q1 2027 (3 months)
- **Support:** GitHub Issues, documentation updates, migration tool fixes

---

## Verification Checklist

After migration, verify:

- [ ] No compilation errors: `./gradlew clean build`
- [ ] All tests pass: `./gradlew test`
- [ ] Architecture tests pass: `./gradlew :sa-admin:test --tests ArchitectureTest`
- [ ] No old imports remain: `grep -r "import net.lab1024.sa.common.core" src/ | grep -v "SmartBeanUtil"` (expect 0 matches)
- [ ] Application starts successfully: `./gradlew :sa-admin:bootRun`
- [ ] Smoke test critical features (login, CRUD operations, etc.)

---

## Migration Tool Details

### Gradle Task: `migrateToFoundation`

**Location:** `smart-admin-api-java21-springboot3/build.gradle.kts` (lines 266-369)

**What it does:**
1. Scans all `.java` files (excluding build directories)
2. Skips `SmartBeanUtil.java` (documented exception)
3. Replaces import statements (regular and static)
4. Updates JavaDoc `{@link}` references
5. Prints migration report

**Order of replacements:**
- Specific class imports first (e.g., `ResponseDTO`)
- Package-level imports second (e.g., `code.*`)

**Dry run:**
```bash
./gradlew migrateToFoundation --dry-run
```

**Verbose output:**
```bash
./gradlew migrateToFoundation --info
```

---

## Related Documentation

- [CLAUDE.md](../../CLAUDE.md) - Quick reference with v4.0.0 breaking changes
- [CHANGELOG.md](../../CHANGELOG.md) - Full v4.0.0 release notes
- [ArchitectureTest.java](../../smart-admin-api-java21-springboot3/sa-admin/src/test/java/net/lab1024/sa/admin/ArchitectureTest.java) - Architecture governance rules

---

## Contact & Support

- **GitHub Issues:** https://github.com/1024-lab/smart-admin/issues
- **Migration Tool Issues:** Tag with `migration` label
- **Documentation:** Check CLAUDE.md and CHANGELOG.md first

---

**Last Updated:** 2026-01-23
**Applies to:** SmartAdmin v4.0.0+
