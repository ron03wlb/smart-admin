# Foundation Packages Migration Guide

**Version:** 1.0
**Last Updated:** 2026-01-23
**Target Audience:** SmartAdmin users upgrading from v3.6.0+ to v4.0.0

---

## Overview

SmartAdmin v3.6.0 introduced a new modular foundation architecture with improved package organization. The old `net.lab1024.sa.common.core.*` packages have been deprecated and will be removed in **v4.0.0 (Q4 2026)**.

**⚠️ CRITICAL:** You must migrate your imports before upgrading to v4.0.0, or your code will fail to compile.

---

## Migration Timeline

| Version | Date | Status | Action Required |
|---------|------|--------|-----------------|
| **v3.6.0** | Q4 2025 | Bridge classes created | None (backward compatible) |
| **v3.7.0** | Q2 2026 | Runtime warnings (INFO) | 📋 Start planning migration |
| **v3.8.0** | Q2 2026 | Runtime warnings (WARN) | ⚠️ Complete migration recommended |
| **v3.9.0** | Q3 2026 | Runtime warnings (ERROR) | 🔴 **Last compatible version** |
| **v4.0.0** | Q4 2026 | Bridge classes removed | ❌ **Compilation will fail** |

**You have approximately 9 months** from v3.7.0 release to complete your migration.

---

## Quick Migration (Recommended)

### Option 1: Automated Migration (Safest)

```bash
# Navigate to your project root
cd your-project

# Run the automated migration tool
./gradlew migrateToFoundation

# Verify changes
git diff

# Run tests to ensure nothing broke
./gradlew test

# Commit the changes
git add .
git commit -m "refactor: migrate to foundation packages"
```

The automated tool will update all imports automatically and safely.

---

## Manual Migration

If you prefer to migrate manually or the automated tool doesn't cover your use case, follow this mapping:

### Package Mapping Reference

| Old Package | New Package |
|-------------|-------------|
| `net.lab1024.sa.common.core.domain.ResponseDTO` | `net.lab1024.sa.foundation.domain.response.ResponseDTO` |
| `net.lab1024.sa.common.core.domain.PageResult` | `net.lab1024.sa.foundation.domain.response.PageResult` |
| `net.lab1024.sa.common.core.domain.RequestUser` | `net.lab1024.sa.foundation.domain.request.RequestUser` |
| `net.lab1024.sa.common.core.domain.PageParam` | `net.lab1024.sa.foundation.domain.request.PageParam` |
| `net.lab1024.sa.common.core.code.*` | `net.lab1024.sa.foundation.domain.code.*` |
| `net.lab1024.sa.common.core.constant.*` | `net.lab1024.sa.foundation.domain.constant.*` |
| `net.lab1024.sa.common.core.exception.*` | `net.lab1024.sa.foundation.domain.exception.*` |
| `net.lab1024.sa.common.core.enumeration.*` | `net.lab1024.sa.foundation.domain.enumeration.*` |

**Note:** `SmartBeanUtil` remains in `net.lab1024.sa.common.core.util` (not moved to foundation).

### Manual Migration Steps

1. **Search and Replace** (IntelliJ IDEA):
   ```
   Find: import net.lab1024.sa.common.core.domain.ResponseDTO
   Replace: import net.lab1024.sa.foundation.domain.response.ResponseDTO
   ```

2. **Repeat for all packages** listed in the mapping table above.

3. **Verify with grep:**
   ```bash
   # Check for remaining old imports
   grep -r "net.lab1024.sa.common.core" src/
   # Should return only SmartBeanUtil (which is intentional)
   ```

4. **Compile and test:**
   ```bash
   ./gradlew clean build
   ./gradlew test
   ```

---

## IDE Migration

### IntelliJ IDEA

1. Open **Refactor → Migrate Packages and Classes**
2. Add migration rules:
   - From: `net.lab1024.sa.common.core.domain`
   - To: `net.lab1024.sa.foundation.domain.{response|request}`
3. Click **Run** and review changes
4. Test thoroughly

### Eclipse

1. Use **Search → File Search**
2. Search for: `import net.lab1024.sa.common.core`
3. Replace manually using the mapping table
4. Recompile and test

---

## Common Migration Scenarios

### Scenario 1: Simple Controller

**Before (v3.6.0 - v3.9.0):**
```java
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.code.UserErrorCode;

@RestController
public class EmployeeController {

    public ResponseDTO<String> addEmployee() {
        return ResponseDTO.ok("Success");
    }

    public ResponseDTO<String> error() {
        return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
    }
}
```

**After (v4.0.0+):**
```java
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;

@RestController
public class EmployeeController {

    public ResponseDTO<String> addEmployee() {
        return ResponseDTO.ok("Success");
    }

    public ResponseDTO<String> error() {
        return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
    }
}
```

**Changes:** Only import statements - no code logic changes required.

### Scenario 2: Service with Pagination

**Before:**
```java
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.PageParam;

@Service
public class EmployeeService {

    public PageResult<EmployeeVO> query(EmployeeQueryForm form) {
        Page<?> page = SmartPageUtil.convert2PageQuery(form);
        // ... query logic
        return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
    }
}
```

**After:**
```java
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.request.PageParam;

@Service
public class EmployeeService {

    public PageResult<EmployeeVO> query(EmployeeQueryForm form) {
        Page<?> page = SmartPageUtil.convert2PageQuery(form);
        // ... query logic
        return SmartPageUtil.convert2PageResult(page, list, EmployeeVO.class);
    }
}
```

**Changes:** Only import statements - no code logic changes required.

### Scenario 3: Custom Enumerations

**Before:**
```java
import net.lab1024.sa.common.core.enumeration.BaseEnum;

public enum GenderEnum implements BaseEnum {
    MALE(1, "Male"),
    FEMALE(2, "Female");
    // ...
}
```

**After:**
```java
import net.lab1024.sa.foundation.domain.enumeration.BaseEnum;

public enum GenderEnum implements BaseEnum {
    MALE(1, "Male"),
    FEMALE(2, "Female");
    // ...
}
```

**Changes:** Only import statements - no code logic changes required.

---

## Verification Checklist

After migration, verify the following:

- [ ] **Compilation succeeds** - `./gradlew clean build`
- [ ] **All tests pass** - `./gradlew test`
- [ ] **Architecture tests pass** - `./gradlew :sa-admin:test --tests ArchitectureTest`
- [ ] **No old imports remain** (except SmartBeanUtil):
  ```bash
  grep -r "net.lab1024.sa.common.core" src/ | grep -v SmartBeanUtil
  # Should return no results
  ```
- [ ] **Application starts successfully** - Run and access endpoints
- [ ] **Key functionality works** - Test critical business flows
- [ ] **No runtime warnings** - Check logs for deprecation warnings

---

## Troubleshooting

### Issue: Compilation fails with "package does not exist"

**Cause:** You're using v4.0.0+ but haven't migrated imports.

**Solution:**
```bash
# Downgrade to v3.9.0 first
git checkout v3.9.0

# Run migration
./gradlew migrateToFoundation

# Test thoroughly
./gradlew test

# Then upgrade to v4.0.0
git checkout v4.0.0
```

### Issue: Runtime ClassCastException with bridge classes

**Cause:** Pre-existing bug in bridge class implementation (v3.6.0 - v3.9.0).

**Solution:** This is resolved in the atomic migration (v3.7.0+). Upgrade to v3.7.0+ where internal codebase uses foundation packages.

### Issue: Automated tool doesn't migrate all files

**Cause:** Tool may miss edge cases like static imports or fully qualified names.

**Solution:**
```bash
# Check for remaining old imports
grep -r "net.lab1024.sa.common.core" src/

# Manually update any remaining occurrences
# Refer to the package mapping table
```

---

## Need Help?

- **Migration Issues:** Open an issue at [GitHub Issues](https://github.com/1024-lab/smart-admin/issues)
- **Documentation:** Check [SmartAdmin Docs](https://docs.smartadmin.cn)
- **Community Support:** Join our discussion forum

---

## FAQ

**Q: Do I need to change any code logic?**
A: No, only import statements need to be updated. The API remains identical.

**Q: What if I can't migrate before v4.0.0?**
A: Stay on v3.9.0 until you complete migration. Do not upgrade to v4.0.0 without migrating.

**Q: Can I mix old and new imports?**
A: Yes, during v3.7.0 - v3.9.0, but not recommended. Complete migration to avoid confusion.

**Q: What about SmartBeanUtil?**
A: SmartBeanUtil stays in `net.lab1024.sa.common.core.util` - do not migrate this class.

**Q: Will this affect my Vue.js frontend?**
A: No, frontend code is not affected. This only impacts Java backend imports.

---

**Document Version:** 1.0
**Status:** Official Migration Guide
**Applies to:** SmartAdmin v3.6.0 → v4.0.0

For the latest version of this document, visit: https://docs.smartadmin.cn/migration/v4.0.0
