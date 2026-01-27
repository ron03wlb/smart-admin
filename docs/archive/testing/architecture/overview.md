# SmartAdmin Architecture Rules - Quick Reference

> **Version**: 1.0 | **Last Updated**: 2026-01-21
> **Priority**: MUST fix violations before writing tests

---

## Layer Architecture Diagram

```
┌─────────────┐
│ Controller  │  @RestController, @Valid
└──────┬──────┘
       │ calls
┌──────▼──────┐
│  Service    │  Business logic, validation
└──────┬──────┘
       │ calls
┌──────▼──────┐
│  Manager    │  @Transactional, @Cacheable
└──────┬──────┘
       │ calls
┌──────▼──────┐
│    Dao      │  MyBatis Plus (BaseMapper)
└──────┬──────┘
       │ maps
┌──────▼──────┐
│   Entity    │  @TableName
└─────────────┘
```

---

## Architecture Rules Table

| Rule | Description | Enforcement |
|------|-------------|-------------|
| **@Transactional Location** | ONLY in Manager layer | ArchUnit |
| **rollbackFor Parameter** | Must use `Throwable.class`, NOT `Exception.class` | ArchUnit |
| **Manager Dependencies** | Manager CANNOT call Service or other Managers | ArchUnit |
| **Dependency Injection** | Constructor injection only (no `@Autowired` fields) | ArchUnit |
| **Layer Calls** | Controller → Service → Manager → Dao (no shortcuts) | ArchUnit |

---

## Current Violations (Phase 0)

### Summary

| Violation | Location | Type | Priority |
|-----------|----------|------|----------|
| **Violation 1** | `EmployeeService.updatePassword()` line 361 | `@Transactional` in Service | CRITICAL |
| **Violation 2** | `RoleService` lines 51+ | `@Transactional` in Service + wrong `rollbackFor` | CRITICAL |

**Status**: 🔴 Must fix before implementing unit tests

---

## Violation Detection

### Run Architecture Test

```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**Expected output when violations exist**:
```
ArchitectureTest > testTransactionalOnlyInManager() FAILED
    java.lang.AssertionError: Architecture Violation
    classes that are annotated with @Transactional should reside in a package '..manager..'
```

### Manual Detection Commands

```bash
# Find @Transactional in Service classes
grep -r "@Transactional" --include="*Service.java" sa-admin/src/main/java

# Find wrong rollbackFor usage
grep -r "rollbackFor = Exception.class" sa-admin/src/main/java

# Find @Autowired field injection
grep -r "@Autowired" --include="*.java" sa-admin/src/main/java | grep -v "constructor"
```

---

## Fix Workflow

### Step-by-Step Process

```
1. Identify violation type
   └─ Run: ./gradlew :sa-admin:test --tests ArchitectureTest

2. Refer to specific fix guide
   ├─ @Transactional in EmployeeService → fix-employee-transactional.md
   ├─ @Transactional in RoleService → fix-role-transactional.md
   └─ Other violations → Apply pattern from examples

3. Apply fix
   └─ Follow step-by-step instructions in fix guide

4. Verify fix
   └─ Run: ./gradlew :sa-admin:test --tests ArchitectureTest
```

---

## Fix Guides

### Violation 1: EmployeeService.updatePassword()

**Problem**: `@Transactional` annotation in Service layer

**Fix Guide**: [fix-employee-transactional.md](./fix-employee-transactional.md)

**Summary**:
1. Add `updatePassword()` method to `EmployeeManager` with `@Transactional`
2. Remove `@Transactional` from `EmployeeService.updatePassword()`
3. Update Service to call Manager method

**Estimated Time**: 15 minutes

---

### Violation 2: RoleService @Transactional

**Problem**: Multiple `@Transactional` methods in Service layer, wrong `rollbackFor`

**Fix Guide**: [fix-role-transactional.md](./fix-role-transactional.md)

**Summary**:
1. Create `RoleManager.java`
2. Add transactional methods: `saveRole()`, `updateRole()`, `deleteRole()`
3. Remove `@Transactional` from `RoleService` methods
4. Update Service to call Manager methods

**Estimated Time**: 30 minutes

---

## Verification Commands

### After Fixing Violations

```bash
# 1. Run architecture test
./gradlew :sa-admin:test --tests ArchitectureTest
# Expected: BUILD SUCCESSFUL

# 2. Verify no @Transactional in Service
grep -r "@Transactional" --include="*Service.java" sa-admin/src/main/java
# Expected: No matches

# 3. Verify rollbackFor uses Throwable
grep -r "rollbackFor = Exception.class" sa-admin/src/main/java
# Expected: No matches

# 4. Run full test suite
./gradlew :sa-admin:test
# Expected: BUILD SUCCESSFUL
```

---

## Common Fix Patterns

### Pattern 1: Move @Transactional to Manager

```java
// Before (VIOLATION)
@Service
public class MyService {
    @Transactional(rollbackFor = Throwable.class)
    public ResponseDTO<String> doSomething() {
        // Multi-step database operations
    }
}

// After (CORRECT)
@Service
public class MyManager {
    @Transactional(rollbackFor = Throwable.class)
    public void doSomething(params) {
        // Multi-step database operations
    }
}

@Service
@RequiredArgsConstructor
public class MyService {
    private final MyManager myManager;

    public ResponseDTO<String> doSomething() {
        // Validation logic
        myManager.doSomething(params);
        return ResponseDTO.ok();
    }
}
```

### Pattern 2: Fix rollbackFor

```java
// Wrong
@Transactional(rollbackFor = Exception.class)

// Correct
@Transactional(rollbackFor = Throwable.class)
```

### Pattern 3: Fix Dependency Injection

```java
// Wrong
@Service
public class MyService {
    @Autowired
    private MyDao myDao;
}

// Correct
@Service
@RequiredArgsConstructor
public class MyService {
    private final MyDao myDao;
}
```

---

## Troubleshooting

### Issue 1: DAO Method Not Found

**Problem**: Manager needs `deleteByRoleId()` but DAO doesn't have it.

**Solution**: Add method to DAO interface:
```java
public interface RoleMenuDao extends BaseMapper<RoleMenuEntity> {
    default void deleteByRoleId(Long roleId) {
        delete(Wrappers.<RoleMenuEntity>lambdaQuery()
            .eq(RoleMenuEntity::getRoleId, roleId));
    }
}
```

### Issue 2: Circular Dependency

**Problem**: Manager tries to call Service.

**Solution**: Manager should ONLY call DAO. Move business logic to Service if needed.

```
Correct:   Service → Manager → Dao
Incorrect: Manager → Service (circular dependency)
```

### Issue 3: Multiple Violations in One Class

**Solution**: Fix them one at a time:
1. Create Manager class
2. Move all `@Transactional` methods to Manager
3. Update all Service methods to call Manager
4. Verify after each step

---

## Related Documentation

- **Detailed Fixes**:
  - [EmployeeService Fix](./fix-employee-transactional.md)
  - [RoleService Fix](./fix-role-transactional.md)
- **Testing Strategy**: [../testing-strategy.md](../testing-strategy.md)
- **Implementation Plan**: [../unit-test-implementation-plan.md](../unit-test-implementation-plan.md)
- **Quick Reference**: [../quick-reference.md](../quick-reference.md)
- **Project Conventions**: [../../../CLAUDE.md](../../../CLAUDE.md)
