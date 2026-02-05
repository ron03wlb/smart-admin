# SmartAdmin Testing Suite

> Complete testing solution: Integration Tests + Test Fixtures + Unit Tests + E2E Tests

For complete reference, see [SKILL.md](SKILL.md).

---

## Quick Start

```bash
# Scenario: Just implemented EmployeeService, need tests
User: "Create integration tests for EmployeeService"

# AI auto-detects and generates integration tests
/test EmployeeService --mode=integration

# Auto-generates:
# 1. Integration test class (EmployeeServiceIntegrationTest.java)
# 2. Test fixture (EmployeeTestFixture.java)
# 3. Testcontainers config (PostgreSQL + Redis + Kafka)
# 4. Test data initialization
```

### Generated File Structure

```
smart-admin-api-java21-springboot3/smartadmin-app/src/test/java/
└── net/lab1024/sa/business/employee/
    ├── EmployeeServiceIntegrationTest.java    # Integration test class
    ├── fixture/
    │   └── EmployeeTestFixture.java           # Test Data Builder
    └── config/
        └── TestContainersConfig.java          # Testcontainers config
```

---

## Execution Modes

| Mode | Command | Time | Use When |
|------|---------|------|----------|
| **Integration** (recommended) | `--mode=integration` | ~10 min | Testing Service/Manager/Controller with real DB |
| **Fixtures** | `--mode=fixtures` | ~5 min | Building complex test data objects |
| **Unit** (TDD) | `--mode=unit` | ~8 min | Testing single class logic with Mocks |
| **E2E** | `--mode=e2e` | ~15 min | Full user flow testing (frontend + backend) |
| **All** | `--mode=all` | ~20 min | New module, need complete test coverage |

### Mode 1: `--mode=integration` (Recommended)

Test Service/Manager/Controller layers with real databases via Testcontainers.

```bash
/test EmployeeService --mode=integration
```

**Generates**: Integration test class + Testcontainers config + test data init + test fixtures

**Test layers covered**: Controller (REST API), Service (business logic + Option), Manager (@Transactional), Dao (SQL/MyBatis-Plus)

### Mode 2: `--mode=fixtures`

Generate Test Data Builders with fluent API for complex domain objects.

```bash
/test Employee --mode=fixtures
```

**Generates**: `*TestFixture.java` with `.withName()`, `.withDepartmentId()` fluent builders and sensible defaults.

### Mode 3: `--mode=unit` (TDD)

Generate unit tests using Mockito to isolate dependencies. Uses `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.

```bash
/test EmployeeService --mode=unit
```

### Mode 4: `--mode=e2e`

Generate Cypress end-to-end tests for full user flow validation.

```bash
/test employee-management --mode=e2e
```

### Mode 5: `--mode=all`

Generate integration tests + unit tests + test fixtures in one pass.

```bash
/test EmployeeService --mode=all
```

---

## When to Use / When NOT to Use

**Use when**:
- New Service/Manager methods need testing
- Complex business logic with multiple dependencies
- Test data preparation is complex (nested objects)
- TDD development workflow
- Full user flow verification needed

**Do NOT use when**:
- Tests already have 80%+ coverage
- Pure utility class tests (use simple unit tests)
- Testing third-party library behavior

---

## Composed Skills

This suite integrates two previously standalone skills:

| Original Skill | Integrated As | Deprecated Command |
|---------------|---------------|-------------------|
| `smartadmin-integration-test` | `--mode=integration` | `/integration-test` |
| `test-fixture-generator` | `--mode=fixtures` | `/test-fixture` |

Deprecated commands still work with migration warnings during the soft-deprecation period.

---

## Related Resources

- **[SKILL.md](SKILL.md)** - Full technical specification and implementation details
- **Mode docs**: [Integration](modes/mode-1-integration.md) | [Fixtures](modes/mode-2-fixtures.md) | [Unit](modes/mode-3-unit.md) | [E2E](modes/mode-4-e2e.md) | [All](modes/mode-all.md)
- **SmartAdmin**: [Testing Strategy](../../../../docs/testing/testing-strategy.md) | [Integration Testing Guide](../../../../docs/testing/integration-testing-quick-reference.md)
- **External**: [Testcontainers](https://www.testcontainers.org/) | [JUnit 5](https://junit.org/junit5/) | [Mockito](https://site.mockito.org/) | [Cypress](https://www.cypress.io/)

---

## Running Generated Tests

```bash
# Single test class
./gradlew :smartadmin-app:test --tests EmployeeServiceIntegrationTest

# All integration tests
./gradlew :smartadmin-app:test --tests '*IntegrationTest'
```

> Note: Testcontainers requires Docker. Enable container reuse with `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` for faster subsequent runs.

---

**Skill Version**: 2.0.0 | **Status**: Stable | **Compatible with**: SmartAdmin v4.0.0+

**Last Updated**: 2026-01-30
