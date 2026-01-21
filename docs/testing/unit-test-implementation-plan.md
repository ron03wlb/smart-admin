# SmartAdmin V3 Unit Test Implementation Plan

> **Document Version**: 1.0
> **Created**: 2026-01-21
> **Target**: sa-admin business modules
> **Timeline**: 6 weeks

---

## Executive Summary

This document outlines a comprehensive plan to implement unit tests for SmartAdmin V3, targeting **80% line coverage** and **70% branch coverage** for the `sa-admin` business modules. The current test coverage is approximately 0%, with only architecture validation tests in place.

### Key Objectives

| Objective | Target | Current State |
|-----------|--------|---------------|
| Line Coverage | >= 80% | ~0% |
| Branch Coverage | >= 70% | ~0% |
| Service Layer Coverage | >= 85% | ~0% |
| Manager Layer Coverage | >= 80% | ~0% |
| Test Execution Time | < 5 minutes | N/A |

### Strategic Approach

1. **Phase 0**: Fix architecture violations before writing tests
2. **Pure Unit Tests**: Use `@Mock` (Mockito) for fast execution (~10ms per test)
3. **Security First**: Prioritize authentication and authorization modules
4. **Progressive Coverage**: Build from critical paths to edge cases

---

## Current State Analysis

### Existing Test Infrastructure

| Component | Status | Notes |
|-----------|--------|-------|
| JUnit 5 (Jupiter) | ✅ Ready | Configured via spring-boot-starter-test |
| Mockito | ✅ Ready | Available in test dependencies |
| JaCoCo | ⚠️ Needs Configuration | Version 0.8.12 available |
| Test Configuration | ✅ Ready | application.yaml with H2 database |
| ArchUnit | ✅ Active | Architecture rules enforced |

### Existing Test Files

```
sa-admin/src/test/java/net/lab1024/sa/admin/
|-- ArchitectureTest.java       # Architecture validation (keep as-is)
|-- AdminApplicationTest.java   # Disabled integration test
```

### Missing Tests

- **~100+ business classes** have zero unit tests
- **Critical security services** are untested
- **No fixture or mock infrastructure** exists

---

## Target State

### Coverage Requirements

→ See [Testing Strategy - Coverage Requirements](./testing-strategy.md#coverage-requirements-by-layer) for detailed targets

**Summary**:

| Layer | Line Coverage | Branch Coverage | Priority |
|-------|---------------|-----------------|----------|
| Service | >= 85% | >= 75% | HIGH |
| Manager | >= 80% | >= 70% | HIGH |
| Controller | >= 60% | >= 50% | MEDIUM |
| DTO/VO/Entity | Excluded | Excluded | N/A |

### Test Infrastructure to Create

→ See [Testing Strategy - Test Infrastructure](./testing-strategy.md#test-infrastructure-design) for design details

**Key files to create**:
- `base/BaseServiceTest.java` - ResponseDTO assertion helpers
- `base/BaseManagerTest.java` - DAO interaction verification helpers
- `base/BaseControllerTest.java` - MockMvc helpers
- `fixture/` - Test data builders (EmployeeFixture, LoginFixture, etc.)
- `mock/MockSecurityConfig.java` - Centralized mock configurations

---

## 6-Week Implementation Roadmap

### Phase 0: Architecture Fixes (Days 1-2)

> **Prerequisite**: MUST complete before writing tests

**Violations to fix**: 2 `@Transactional` violations in Service layer

→ **Detailed fix guides**: [Architecture Overview](./architecture/overview.md)

**Quick summary**:
- **Violation 1**: `EmployeeService.updatePassword()` - Move @Transactional to EmployeeManager
- **Violation 2**: `RoleService` methods - Create RoleManager, move transactional operations

**Verification**:
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

→ For complete verification commands, see [Quick Reference](./quick-reference.md#commands)

---

### Phase 1: Test Infrastructure + Security Services (Week 1-2)

**Deliverables**:
- Base test classes (BaseServiceTest, BaseManagerTest, BaseControllerTest)
- All fixture classes (EmployeeFixture, LoginFixture, RoleFixture, DepartmentFixture, SecurityFixture)
- Mock configurations (MockSecurityConfig)

**Test Classes**:

| Class | Test Cases | Coverage Target |
|-------|------------|-----------------|
| SecurityLoginServiceTest | 15-20 | 100% |
| SecurityPasswordServiceTest | 12-15 | 100% |
| LoginServiceTest | 25-30 | 90%+ |
| LoginManagerTest | 10-12 | 100% |

**Expected Overall Coverage**: ~25%

---

### Phase 2: Employee Management (Week 3-4)

**Test Classes**:

| Class | Test Cases | Coverage Target |
|-------|------------|-----------------|
| EmployeeServiceTest | 30-35 | 100% |
| EmployeeManagerTest | 8-10 | 100% |
| EmployeeControllerTest | 15-18 | 80%+ |

**Key Testing Challenges**:
- Synchronized methods (`addEmployee`, `updateEmployee`)
- Custom password salt format: `password_UID_UPPER_uid_lower`
- Uniqueness validation (login name, phone, email)

**Expected Overall Coverage**: ~60%

---

### Phase 3: Role & Permissions (Week 5)

**Test Classes**:

| Class | Test Cases | Coverage Target |
|-------|------------|-----------------|
| RoleServiceTest | 12-15 | 100% |
| RoleMenuServiceTest | 15-18 | 100% |
| RoleEmployeeServiceTest | 8-10 | 100% |

**Key Testing Challenges**:
- Recursive permission tree traversal
- Role deletion constraints
- Permission caching

**Expected Overall Coverage**: ~75%

---

### Phase 4: Department & Business Services (Week 6)

**Test Classes**:

| Class | Test Cases | Coverage Target |
|-------|------------|-----------------|
| DepartmentServiceTest | 12-15 | 100% |
| DepartmentCacheManagerTest | 10-12 | 100% |
| NoticeServiceTest | 12-15 | 90%+ |
| EnterpriseServiceTest | 10-12 | 90%+ |

**Key Testing Challenges**:
- Hierarchical department validation
- Complex visibility rules (NoticeService)
- Multi-tenant audit trails

**Expected Overall Coverage**: **>= 80%**

---

## Critical Test Scenarios

### LoginService.login() Method (~27 Test Cases)

This is the most complex method requiring comprehensive testing:

| Category | Test Cases | Description |
|----------|------------|-------------|
| Device Validation | 3 | Valid/Invalid/Null device types |
| Captcha Validation | 2 | Valid/Invalid captcha codes |
| User Lookup | 2 | User found/not found |
| Account Status | 3 | Active/Deleted/Disabled accounts |
| Super Password | 3 | Bypass security, 30-min session |
| Normal Password | 6 | Wrong password, record failure, success |
| 2FA Email Code | 4 | Disabled/Missing/Invalid/Valid |
| Post-Login | 3 | Load permissions, return token |

### SecurityLoginService Time-Based Tests

| Scenario | Expected Behavior |
|----------|-------------------|
| Account locked (within lock period) | Return lock error with remaining time |
| Account locked (lock expired) | Allow login, reset failure count |
| Reach max failures | Trigger lock, set lock begin time |
| Below max failures | Record failure, return remaining attempts |

### EmployeeService Password Salt Format

```java
// Critical: Salt format verification
String expectedSalt = password + "_" + uid.toUpperCase() + "_" + uid.toLowerCase();
// Example: "Admin@123_ABC123DEF456_abc123def456"
```

---

## Success Criteria

### Coverage Targets

→ See [Testing Strategy - Coverage Targets](./testing-strategy.md#coverage-targets-summary) for detailed thresholds

**Build enforcement**:
- Line Coverage >= 80% (build fails if not met)
- Branch Coverage >= 70% (build fails if not met)

### Quality Gates

- [ ] All tests pass: `./gradlew :sa-admin:test`
- [ ] Architecture rules pass: `./gradlew :sa-admin:test --tests ArchitectureTest`
- [ ] Coverage verification: `./gradlew :sa-admin:jacocoTestCoverageVerification`
- [ ] No flaky tests (run 3 times, all pass)
- [ ] Test execution time < 5 minutes

→ For all verification commands, see [Quick Reference](./quick-reference.md#commands)

### Documentation Requirements

- [ ] Test naming follows convention: `test{Method}_{Scenario}_{ExpectedResult}`
- [ ] Complex scenarios have explanatory comments
- [ ] Fixtures use builder patterns for clarity
- [ ] Mock configurations are centralized and reusable

---

## Risk Mitigation

### Technical Risks

| Risk | Mitigation Strategy |
|------|---------------------|
| SA-Token static methods | Use `MockedStatic<StpUtil>` for critical tests |
| Synchronized methods | Mockito handles fine; concurrent tests need real threads |
| Cache invalidation | Mock Manager layer methods |
| Complex dependencies (13+ in LoginService) | Use `lenient()` for optional mocks |

### Timeline Risks

| Risk | Mitigation Strategy |
|------|---------------------|
| Underestimated complexity | Phase 1 validates estimates; adjust if needed |
| Architecture fixes take longer | 2-day buffer allocated for Phase 0 |
| JaCoCo configuration issues | Test locally before CI/CD integration |

### Quality Risks

| Risk | Mitigation Strategy |
|------|---------------------|
| Flaky tests | Use deterministic data; avoid time-based assertions |
| Low edge case coverage | Focus on branch coverage, not just line coverage |
| Test maintenance burden | Use fixtures and base classes to reduce duplication |

---

## Estimated Effort

### Test Case Summary

| Phase | Test Classes | Test Cases | Effort (days) |
|-------|--------------|------------|---------------|
| Phase 0 | 0 | 0 | 2 |
| Phase 1 | 4 | 62-77 | 8 |
| Phase 2 | 3 | 53-63 | 8 |
| Phase 3 | 3 | 35-43 | 4 |
| Phase 4 | 4 | 44-54 | 4 |
| **Total** | **14+** | **194-237** | **26 days** |

### Resource Requirements

- **Developer Time**: 1 developer, 6 weeks full-time
- **Review Time**: ~2 hours per phase for code review
- **CI/CD Integration**: ~4 hours for JaCoCo setup and verification

---

## Related Documentation

### Core Testing Documentation

- **[Testing Strategy](./testing-strategy.md)** - Mock strategy, best practices, naming conventions
- **[Architecture Fixes](./architecture/overview.md)** - Phase 0 violations and detailed fix guides
- **[Quick Reference](./quick-reference.md)** - Commands, rules cheatsheet, common patterns

### Specific Fix Guides

- **[Employee Fix Guide](./architecture/fix-employee-transactional.md)** - EmployeeService @Transactional violation
- **[Role Fix Guide](./architecture/fix-role-transactional.md)** - RoleService @Transactional violations

### Project Standards

- **[Project Conventions](../../CLAUDE.md)** - SmartAdmin coding standards
- **[Architecture Rules](../../.agent/rules/10-architecture-rules.md)** - Layer architecture enforcement

---

## Appendix: Key Source Files

### Files Requiring Tests (Priority Order)

| Module | Path | Priority |
|--------|------|----------|
| SecurityLoginService | `module/support/securityprotect/service/SecurityLoginService.java` | CRITICAL |
| SecurityPasswordService | `module/support/securityprotect/service/SecurityPasswordService.java` | CRITICAL |
| LoginService | `module/system/login/service/LoginService.java` | CRITICAL |
| EmployeeService | `module/system/employee/service/EmployeeService.java` | HIGH |
| RoleService | `module/system/role/service/RoleService.java` | HIGH |
| DepartmentService | `module/system/department/service/DepartmentService.java` | MEDIUM |

---

## Getting Started

### Step 1: Fix Architecture Violations

Before writing any tests:

1. Read: [Architecture Overview](./architecture/overview.md)
2. Fix: [EmployeeService](./architecture/fix-employee-transactional.md)
3. Fix: [RoleService](./architecture/fix-role-transactional.md)
4. Verify: `./gradlew :sa-admin:test --tests ArchitectureTest`

### Step 2: Set Up Test Infrastructure

Follow: [Testing Strategy - Test Infrastructure](./testing-strategy.md#test-infrastructure-design)

Create:
- Base test classes
- Fixture classes
- Mock configurations

### Step 3: Write Your First Test

Follow: [Testing Strategy - Best Practices](./testing-strategy.md#best-practices)

Start with: `SecurityLoginServiceTest` (simplest critical service)

### Step 4: Verify Coverage

```bash
# Generate coverage report
./gradlew :sa-admin:jacocoTestReport

# View report
open sa-admin/build/reports/jacoco/test/html/index.html
```

---

**Ready to start? Begin with [Architecture Fixes](./architecture/overview.md)!**
