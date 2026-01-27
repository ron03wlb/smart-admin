# Architecture Rules Clarification - Service Layer Can Directly Call Dao

**Document Type**: Architecture Rule Clarification
**Last Updated**: 2026-01-27
**Status**: ✅ Confirmed and Documented

---

## 🎯 Purpose

This document clarifies a frequently misunderstood SmartAdmin architecture rule:

> **Service layer CAN directly call Dao/Mapper for single-table CRUD operations**

This clarification was created after multiple instances of AI agents incorrectly flagging `Service → Dao` direct calls as architecture violations.

---

## ✅ Correct Architecture Rules

### Rule 1: Service CAN Directly Call Dao/Mapper

**✅ ALLOWED**: Service layer can directly access Dao/Mapper when:
- Single-table query (selectById, selectList, pagination)
- Single-table insert (insert single entity)
- Single-table update (updateById, updatePassword, updateAvatar)
- Single-table delete (deleteById, logical delete)
- **As long as NO @Transactional or @Cacheable is needed**

**Example (CORRECT)**:
```java
// ✅ CORRECT: Service directly calls Dao
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;  // ✅ Direct Dao injection is OK

    public EmployeeEntity getById(Long id) {
        return employeeDao.selectById(id);  // ✅ Single query, no transaction
    }

    public void updateAvatar(Long id, String avatar) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setEmployeeId(id);
        entity.setAvatar(avatar);
        employeeDao.updateById(entity);  // ✅ Single update, no transaction
    }
}
```

---

### Rule 2: Service CANNOT Use @Transactional or @Cacheable

**❌ PROHIBITED**: Service layer using @Transactional/@Cacheable annotations

**Example (VIOLATION)**:
```java
// ❌ VIOLATION: Service using @Transactional
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final RoleEmployeeDao roleEmployeeDao;

    @Transactional  // ❌ Service CANNOT use @Transactional!
    public void updateEmployee(EmployeeEntity employee, List<Long> roleIds) {
        employeeDao.updateById(employee);
        roleEmployeeDao.deleteByEmployeeId(employee.getId());
        roleEmployeeDao.batchInsert(roleIds, employee.getId());
    }
}
```

**Fix**: Delegate to Manager layer:
```java
// ✅ CORRECT: Service delegates to Manager for transactional operation
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeDao employeeDao;
    private final EmployeeManager employeeManager;  // ✅ Manager handles transactions

    public void updateEmployee(EmployeeUpdateForm form) {
        // Business validation in Service
        EmployeeEntity existing = employeeDao.selectById(form.getId());  // ✅ Direct Dao call OK
        if (existing == null) {
            return ResponseDTO.error("Employee not found");
        }

        // Delegate to Manager for transactional multi-table operation
        EmployeeEntity entity = SmartBeanUtil.copy(form, EmployeeEntity.class);
        employeeManager.updateEmployeeTransaction(entity, form.getRoleIds());  // ✅ Manager with @Transactional
        return ResponseDTO.ok();
    }
}

// Manager handles transaction
@Service
@RequiredArgsConstructor
public class EmployeeManager {
    private final EmployeeDao employeeDao;
    private final RoleEmployeeDao roleEmployeeDao;

    @Transactional(rollbackFor = Throwable.class)  // ✅ @Transactional in Manager only
    public void updateEmployeeTransaction(EmployeeEntity employee, List<Long> roleIds) {
        employeeDao.updateById(employee);
        roleEmployeeDao.deleteByEmployeeId(employee.getId());
        if (CollectionUtils.isNotEmpty(roleIds)) {
            roleIds.forEach(roleId ->
                roleEmployeeDao.insert(new RoleEmployeeEntity(roleId, employee.getId())));
        }
    }
}
```

---

## 🤖 AI Agent Instructions

### When Reviewing Code

**DO NOT flag as violation**:
- ✅ Service class has `private final XxxDao xxxDao;`
- ✅ Service method calls `xxxDao.selectById()`, `xxxDao.updateById()`, etc.
- ✅ Service method does NOT have `@Transactional` or `@Cacheable`

**DO flag as violation**:
- ❌ Service class or method has `@Transactional` annotation
- ❌ Service class or method has `@Cacheable/@CacheEvict/@CachePut` annotation

### Decision Tree

```
Code Review: Service class detected
  ├─ Check: Does Service method have @Transactional?
  │   ├─ YES → ❌ VIOLATION (flag as P0 architecture violation)
  │   └─ NO → Continue
  │
  ├─ Check: Does Service method have @Cacheable/@CacheEvict/@CachePut?
  │   ├─ YES → ❌ VIOLATION (flag as P0 architecture violation)
  │   └─ NO → Continue
  │
  └─ Check: Does Service directly call Dao?
      ├─ YES → ✅ ALLOWED (no violation, this is correct pattern)
      └─ NO → ✅ ALLOWED (Service may also call Manager)
```

---

## 📚 Reference Documentation

1. **Primary Source**: [.agent/rules/foundation/10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md)
   - Lines 90-164: "Service Layer Can Directly Access Dao/Mapper"

2. **Secondary Source**: [.agent/rules/foundation/09-manager-layer.md](.agent/rules/foundation/09-manager-layer.md)
   - Lines 47-70: "When Manager Layer is NOT NEEDED"

3. **Quick Reference**: [CLAUDE.md](../../CLAUDE.md)
   - Section: "Key Constraints (Always Apply)"
   - Updated 2026-01-27 to emphasize this rule

---

## 🔍 ArchUnit Test Reference

**Test Location**: `ArchitectureTest.java`

**Relevant Tests**:
- ✅ `layerDependencies` - Allows Service → Dao access
- ✅ `serviceNotUseTransactional` - Service cannot use @Transactional
- ✅ `managerTransactionMethodNaming` - @Transactional methods must be in Manager with "Transaction" suffix

---

## 📝 Version History

| Version | Date | Change |
|---------|------|--------|
| 1.0 | 2026-01-27 | Initial clarification document created |
| - | - | Updated CLAUDE.md with explicit "Service CAN call Dao" statement |
| - | - | Created [ARCHITECTURE-AUDIT-REPORT-CORRECTED.md](../../docs/archive/2026-01-audit/ARCHITECTURE-AUDIT-REPORT-CORRECTED.md) with rule corrections |

---

## ⚠️ Important Note for Future Reference

**If an AI agent flags "Service directly calls Dao" as a violation**:

1. ❌ This is **INCORRECT**
2. ✅ Refer agent to **this document**
3. ✅ Refer agent to [10-architecture-rules.md](.agent/rules/foundation/10-architecture-rules.md) lines 90-164
4. ✅ Only flag as violation if Service uses `@Transactional` or `@Cacheable`

**The ONLY constraint is**: Service layer CANNOT use `@Transactional` or `@Cacheable` annotations.

---

**Document Owner**: SmartAdmin Architecture Team
**Review Frequency**: Every major version release
**Next Review**: v5.0.0
