# SmartAdmin Testing Documentation

> **Last Updated**: 2026-01-21
> **Status**: In Progress - Target 80% Coverage

---

## Quick Navigation

| I want to... | Go to |
|--------------|-------|
| **Run tests now** | [Quick Reference → Commands](#commands) |
| **Fix architecture violation** | [Architecture Overview](./architecture/overview.md) |
| **Write my first test** | [Testing Strategy](./testing-strategy.md) |
| **Understand the roadmap** | [Implementation Plan](./unit-test-implementation-plan.md) |
| **Look up rules/commands** | [Quick Reference](./quick-reference.md) |

---

## Documentation Map

### For Developers (Writing Tests)

**Start here if you need to write unit tests for SmartAdmin modules.**

1. **Prerequisites**: [Fix Architecture Violations](./architecture/overview.md)
   - **MUST complete before writing tests**
   - Fix 2 `@Transactional` violations
   - Verify with: `./gradlew :sa-admin:test --tests ArchitectureTest`

2. **Learn Testing Strategy**: [Testing Strategy Guide](./testing-strategy.md)
   - Mock strategy with `@Mock` (Mockito)
   - Test infrastructure (BaseServiceTest, fixtures)
   - Naming conventions
   - Best practices

3. **Reference Commands**: [Quick Reference Card](./quick-reference.md)
   - Common Gradle commands
   - Architecture rules cheatsheet
   - Mock strategy decision tree

4. **Integration Testing**: [Integration Testing Quick Reference](./integration-testing-quick-reference.md)
   - Testing Sa-Token authentication
   - Testing @Transactional methods
   - Testing cache with JetCache
   - Testing Kafka & async operations

**Read time**: 60 minutes

---

### For Technical Leads (Planning)

**Start here if you need to plan test implementation or review coverage.**

1. **Review Timeline**: [Implementation Plan](./unit-test-implementation-plan.md)
   - 6-week roadmap (Phases 0-4)
   - Coverage targets by layer
   - Resource requirements
   - Risk mitigation

2. **Understand Strategy**: [Testing Strategy Guide](./testing-strategy.md)
   - Coverage requirements
   - Test execution strategy
   - Quality gates

3. **Check Architecture**: [Architecture Overview](./architecture/overview.md)
   - Current violations
   - Fix workflow

**Read time**: 30 minutes

---

### For Maintainers (Fixing Issues)

**Start here if tests are failing or you need to fix violations.**

1. **Identify Violation**:
   ```bash
   ./gradlew :sa-admin:test --tests ArchitectureTest
   ```

2. **Find Fix Guide**:
   - **EmployeeService `@Transactional`** → [Employee Fix Guide](./architecture/fix-employee-transactional.md)
   - **RoleService `@Transactional`** → [Role Fix Guide](./architecture/fix-role-transactional.md)
   - **General violations** → [Architecture Overview](./architecture/overview.md)

3. **Verify Fix**:
   ```bash
   ./gradlew :sa-admin:test --tests ArchitectureTest
   ```

**Read time**: 10 minutes per violation

---

## Document Index

### Core Documentation

| Document | Purpose | Audience | Read Time |
|----------|---------|----------|-----------|
| **[README.md](./README.md)** (this file) | Navigation hub | All | 5 min |
| **[Quick Reference](./quick-reference.md)** | Commands & rules cheatsheet | All | 2 min |
| **[Testing Strategy](./testing-strategy.md)** | How to write tests | Developers | 30 min |
| **[Implementation Plan](./unit-test-implementation-plan.md)** | 6-week roadmap | Tech Leads | 20 min |

### Integration Testing

| Document | Purpose | Audience | Read Time |
|----------|---------|----------|-----------|
| **[Integration Testing Quick Reference](./integration-testing-quick-reference.md)** | Commands, patterns, 4 challenges | All | 3 min |
| **[Sa-Token Testing](./integration/sa-token-testing.md)** | Authentication testing | Developers | 15 min |
| **[Transaction Testing](./integration/transaction-testing.md)** | @Transactional testing | Developers | 12 min |
| **[Caching Testing](./integration/caching-testing.md)** | JetCache testing | Developers | 15 min |
| **[Kafka & Async Testing](./integration/kafka-async-testing.md)** | Async operations testing | Developers | 12 min |

### Architecture Fixes

| Document | Purpose | Audience | Read Time |
|----------|---------|----------|-----------|
| **[Architecture Overview](./architecture/overview.md)** | Rules & violations | Maintainers | 10 min |
| **[Employee Fix Guide](./architecture/fix-employee-transactional.md)** | Fix EmployeeService | Maintainers | 15 min |
| **[Role Fix Guide](./architecture/fix-role-transactional.md)** | Fix RoleService | Maintainers | 15 min |

---

## Common Tasks

### Running Tests

```bash
# All tests
./gradlew :sa-admin:test

# Specific test class
./gradlew :sa-admin:test --tests SecurityLoginServiceTest

# Architecture validation
./gradlew :sa-admin:test --tests ArchitectureTest
```

→ See [Quick Reference](./quick-reference.md#commands) for more commands

### Checking Coverage

```bash
# Generate report
./gradlew :sa-admin:jacocoTestReport

# Open report
open sa-admin/build/reports/jacoco/test/html/index.html

# Verify thresholds
./gradlew :sa-admin:jacocoTestCoverageVerification
```

→ See [Testing Strategy](./testing-strategy.md#coverage-requirements-by-layer) for coverage targets

### Writing a Service Test

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest extends BaseServiceTest {
    @InjectMocks
    private EmployeeService employeeService;

    @Mock
    private EmployeeDao employeeDao;

    @Test
    void testAddEmployee_ValidForm_ReturnsSuccess() {
        // Given
        EmployeeAddForm form = EmployeeFixture.defaultAddForm();
        when(employeeDao.selectByLoginName(anyString())).thenReturn(null);

        // When
        ResponseDTO<Long> result = employeeService.addEmployee(form);

        // Then
        assertSuccess(result);
    }
}
```

→ See [Testing Strategy](./testing-strategy.md#service-layer-85-coverage) for detailed examples

---

## Project Status

### Current Metrics

| Metric | Target | Current | Status |
|--------|--------|---------|--------|
| **Line Coverage** | ≥ 80% | ~0% | 🔴 Not Started |
| **Service Layer** | ≥ 85% | ~0% | 🔴 Not Started |
| **Manager Layer** | ≥ 80% | ~0% | 🔴 Not Started |
| **Architecture Violations** | 0 | 2 | 🟡 Fix in Progress |

### Phase Status

| Phase | Status | Completion |
|-------|--------|------------|
| **Phase 0**: Architecture Fixes | 🟡 In Progress | 0% |
| **Phase 1**: Security Services | ⚪ Not Started | 0% |
| **Phase 2**: Employee Management | ⚪ Not Started | 0% |
| **Phase 3**: Role & Permissions | ⚪ Not Started | 0% |
| **Phase 4**: Departments & Business | ⚪ Not Started | 0% |

---

## Key Architecture Rules

| Rule | Correct | Wrong |
|------|---------|-------|
| **Transactions** | `@Transactional` in Manager | ❌ `@Transactional` in Service |
| **rollbackFor** | `Throwable.class` | ❌ `Exception.class` |
| **Injection** | Constructor injection | ❌ `@Autowired` fields |
| **Layer calls** | Controller → Service → Manager → Dao | ❌ Controller → Dao |

→ See [Architecture Overview](./architecture/overview.md) for details

---

## Testing Philosophy

| Principle | Implementation |
|-----------|----------------|
| **Fast Feedback** | Pure unit tests (~10ms per test) |
| **Isolation** | Mock all dependencies with `@Mock` |
| **Deterministic** | No random data, fixed time references |
| **Maintainable** | Centralized fixtures and base classes |

→ See [Testing Strategy](./testing-strategy.md#testing-philosophy) for details

---

## Related Documentation

### Project Documentation
- [Project Coding Standards](../../CLAUDE.md) - SmartAdmin conventions
- [Architecture Rules](../../.agent/rules/10-architecture-rules.md) - Layer architecture enforcement

### External Resources
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)

---

## Getting Help

### Documentation Issues
- Found a broken link? Check [Quick Reference](./quick-reference.md) for correct paths
- Need clarification? See specific guides: [Architecture](./architecture/overview.md) | [Testing Strategy](./testing-strategy.md) | [Implementation Plan](./unit-test-implementation-plan.md)

### Test Failures
1. Check violation type: `./gradlew :sa-admin:test --tests ArchitectureTest`
2. Find fix guide in [Architecture Overview](./architecture/overview.md)
3. Verify after fix: `./gradlew :sa-admin:test`

### Coverage Issues
- View detailed report: `open sa-admin/build/reports/jacoco/test/html/index.html`
- Check targets: [Testing Strategy - Coverage Requirements](./testing-strategy.md#coverage-requirements-by-layer)
- Add tests following: [Testing Strategy - Best Practices](./testing-strategy.md#best-practices-for-smartadmin-testing)
