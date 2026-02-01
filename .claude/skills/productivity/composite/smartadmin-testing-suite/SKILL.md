---
name: smartadmin-testing-suite
description: [P2 - Productivity] Comprehensive testing suite for SmartAdmin with multiple test modes (integration tests with Testcontainers, test fixtures, unit tests, E2E tests). Use when creating integration tests for Service/Manager/Controller, generating test data builders, implementing TDD workflows, or setting up E2E tests. Triggers when (1) User requests "create/generate tests", (2) User mentions "integration test", "test fixture", "TDD", or "E2E test", (3) After implementing new Service/Manager methods, (4) User wants "test data builders", (5) User explicitly requests Testcontainers or testing setup.
---

# SmartAdmin Testing Suite

**Version**: 2.0.0 (Testing Suite Consolidation)
**Consolidates**: smartadmin-integration-test, test-fixture-generator, test-driven-development, webapp-testing

Unified testing solution providing multiple test modes for SmartAdmin's layered architecture.

---

## Quick Start

**Most common usage:**
```
User: "Create integration tests for EmployeeService"
```

**AI Detection**:
- Mode: --integration (default for Service/Manager/Controller tests)
- Generates: Integration test class + Test fixtures + Testcontainers setup
- Time: ~10 minutes

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "create tests" - Test generation request
- "integration test" - Integration test creation
- "test fixture" - Test fixture generation
- "TDD" - Test-Driven Development
- "E2E test" - End-to-end test creation

**Secondary Keywords** (Medium confidence):
- "Testcontainers" - Context: integration test with Testcontainers
- "unit test" - Context: unit test generation
- "test coverage" - Context: improve test coverage

**Phrase Patterns**:
- "Create [test type] for [component]" - Example: "Create integration tests for EmployeeService"
- "Generate test fixture for [entity]" - Example: "Generate test fixture for Product"
- "Add [test coverage] to [module]" - Example: "Add test coverage to Order module"

**Example User Requests**:
```
User: "Create integration tests for EmployeeService"
User: "Generate test fixture for Product entity"
User: "Add unit tests to OrderManager"
User: "Create E2E tests for checkout flow"
```

**Note**: This skill can also be manually invoked via `/smartadmin-testing-suite` command. Supports modes: `--integration`, `--unit`, `--e2e`, `--fixture`.

---

## v2.0.0 Testing Suite Consolidation

**NEW**: This skill consolidates 4 previously separate testing skills into a unified test orchestrator.

### Consolidated Skills

This skill **replaces and consolidates**:
- ✅ **smartadmin-integration-test** (Mode 1: Integration Tests) → `--mode=integration`
- ✅ **test-fixture-generator** (Mode 2: Test Fixtures) → `--mode=fixtures`
- ✅ **test-driven-development** (Mode 3: Unit Tests/TDD) → `--mode=unit` (future)
- ✅ **webapp-testing** (Mode 4: E2E Tests) → `--mode=e2e` (future)

### Mode-Based Execution

**Integration Tests (Default)**:
```bash
/test EmployeeService --mode=integration
# Generates: IntegrationTest + Fixtures + Testcontainers
# Time: ~10 minutes
# Consolidates: former /integration-test command
```

**Test Fixtures Only**:
```bash
/test Employee --mode=fixtures
# Generates: EmployeeTestFixture with builders
# Time: ~5 minutes
# Consolidates: former /test-fixture command
```

**Unit Tests (TDD)**:
```bash
/test EmployeeService --mode=unit
# Generates: Unit test with mocks (future feature)
# Time: ~8 minutes
```

**E2E Tests**:
```bash
/test employee-management --mode=e2e
# Generates: Cypress/Playwright E2E tests (future feature)
# Time: ~15 minutes
```

**All Test Types**:
```bash
/test EmployeeService --mode=all
# Generates: Integration + Unit + Fixtures
# Time: ~20 minutes
```

### Backward Compatibility

**Old commands still work** with deprecation warnings (until 2026-06-30):

```bash
# ⚠️ Deprecated (routes to /test --mode=integration)
/integration-test EmployeeService

# ⚠️ Deprecated (routes to /test --mode=fixtures)
/test-fixture Employee
```

**See**: [skill-aliases.json](../skill-aliases.json) for routing configuration

---

## Test Modes

### Mode 1: Integration Tests (--mode=integration)

**Purpose**: Test Service/Manager/Controller with real database and dependencies

**Input Required**:
- Target class (Service/Manager/Controller)
- Related entities for FK setup
- Test scenarios (CRUD, validation, transactions)

**Execution Steps**:
1. Read [modes/mode-1-integration.md](modes/mode-1-integration.md)
2. Analyze target class dependencies
3. Generate IntegrationTest extending BaseIntegrationTest
4. Generate test fixtures for test data
5. Generate test cases covering:
   - CRUD operations
   - Business logic validation
   - @Transactional rollback
   - ResponseDTO assertions
   - Database state verification

**Output**:
```
sa-admin/src/test/java/net/lab1024/sa/admin/module/{module}/service/
├── {Entity}ServiceIntegrationTest.java
└── {Entity}TestFixture.java (if not exists)
```

**Time**: ~10 minutes
**Consolidates**: smartadmin-integration-test

---

### Mode 2: Test Fixtures (--mode=fixtures)

**Purpose**: Generate reusable test data builders for complex domain objects

**Input Required**:
- Entity name
- Field specifications (types, constraints, FK relationships)
- Unique constraint fields

**Execution Steps**:
1. Read [modes/mode-2-fixtures.md](modes/mode-2-fixtures.md)
2. Analyze Entity structure (fields, FKs, constraints)
3. Generate TestFixture class with:
   - AtomicInteger counter for uniqueness
   - Static factory methods (createEntity, createAddForm, createUpdateForm)
   - Override variants for FK customization
   - Related entity factories
   - Counter reset and unique value helpers

**Output**:
```
sa-admin/src/test/java/net/lab1024/sa/admin/module/{module}/domain/
└── {Entity}TestFixture.java
```

**Time**: ~5 minutes
**Consolidates**: test-fixture-generator

---

### Mode 3: Unit Tests (--mode=unit) [Future Feature]

**Purpose**: TDD workflow with RED-GREEN-REFACTOR cycle

**Planned Features**:
- Generate unit tests with @Mock dependencies
- Test service logic in isolation
- Fast test execution (no database)
- RED-GREEN-REFACTOR guidance

**Status**: Planned for v2.1.0

---

### Mode 4: E2E Tests (--mode=e2e) [Future Feature]

**Purpose**: End-to-end UI testing with Cypress/Playwright

**Planned Features**:
- Frontend E2E test generation
- API integration testing
- User workflow scenarios

**Status**: Planned for v2.2.0

---

## Mode Detection Logic

When user makes a request, determine test mode:

**Mode 1: Integration Tests (--mode=integration)**
- **Triggers**:
  - User says "create integration test"
  - User wants to "test Service/Manager/Controller"
  - User mentions "Testcontainers" or "real database"
  - Former `/integration-test` command users
- **Time**: ~10 minutes

**Mode 2: Test Fixtures (--mode=fixtures)**
- **Triggers**:
  - User says "generate test fixtures"
  - User wants "test data builders"
  - User mentions "AtomicInteger" or "unique test values"
  - Former `/test-fixture` command users
- **Time**: ~5 minutes

**Mode 3: Unit Tests (--mode=unit)** [Future]
- **Triggers**:
  - User says "create unit test" or "TDD"
  - User wants "test with mocks"
  - User mentions "fast tests" or "no database"

**Mode 4: E2E Tests (--mode=e2e)** [Future]
- **Triggers**:
  - User says "create E2E test"
  - User wants "test UI workflow"
  - User mentions "Cypress" or "Playwright"

---

## Core Test Patterns

### Pattern 1: Service Layer Integration Test

**Goal**: Test business logic with real database

```java
@SpringBootTest
@Transactional
class EmployeeServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeDao employeeDao;

    private Long testDepartmentId;

    @BeforeEach
    void setUp() {
        // Create FK dependencies using fixture
        DepartmentEntity dept = EmployeeTestFixture.createDepartment("Test Dept");
        departmentDao.insert(dept);
        testDepartmentId = dept.getDepartmentId();
    }

    @Test
    @DisplayName("Should add employee and persist to database")
    void addEmployee_ValidForm_PersistsToDatabase() {
        // Given
        EmployeeAddForm form = EmployeeTestFixture.createAddForm(testDepartmentId);

        // When
        ResponseDTO<String> response = employeeService.addEmployee(form);

        // Then - Verify ResponseDTO
        assertTrue(response.getOk(), "Expected success: " + response.getMsg());

        // Then - Verify database state
        List<EmployeeEntity> employees = employeeDao.selectList(
            Wrappers.<EmployeeEntity>lambdaQuery()
                .eq(EmployeeEntity::getLoginName, form.getLoginName())
        );
        assertEquals(1, employees.size());
        assertEquals(form.getActualName(), employees.get(0).getActualName());
    }
}
```

**Key Features**:
- ✅ Extends `BaseIntegrationTest` (@SpringBootTest + @Transactional)
- ✅ Real Spring beans (not mocks)
- ✅ Verifies ResponseDTO AND database state
- ✅ Auto-rollback after test (@Transactional)
- ✅ Test fixtures for clean test data

### Pattern 2: Test Fixture Builder

**Goal**: Reusable test data with guaranteed uniqueness

```java
public class EmployeeTestFixture {

    private static final AtomicInteger counter = new AtomicInteger(0);

    // Default builder (all required fields)
    public static EmployeeEntity createEntity() {
        int id = counter.incrementAndGet();

        EmployeeEntity entity = new EmployeeEntity();
        entity.setActualName("Test Employee " + id);
        entity.setLoginName("employee" + id);
        entity.setEmail("employee" + id + "@test.com");
        entity.setPhone("138" + String.format("%08d", id));
        entity.setDepartmentId(1L);  // Default FK
        entity.setIsDisabled(false);
        entity.setIsLeave(false);
        entity.setRemark("Test employee " + id);
        return entity;
    }

    // Override variant (custom FK)
    public static EmployeeEntity createEntity(Long departmentId) {
        EmployeeEntity entity = createEntity();
        entity.setDepartmentId(departmentId);
        return entity;
    }

    // Form builder (for service tests)
    public static EmployeeAddForm createAddForm(Long departmentId) {
        int id = counter.incrementAndGet();

        EmployeeAddForm form = new EmployeeAddForm();
        form.setActualName("Test Employee " + id);
        form.setLoginName("employee" + id);
        form.setPassword("password123");
        form.setEmail("employee" + id + "@test.com");
        form.setPhone("138" + String.format("%08d", id));
        form.setDepartmentId(departmentId);
        form.setIsDisabled(false);
        form.setIsLeave(false);
        form.setRemark("Test employee " + id);
        return form;
    }

    // Related entity factory (FK dependency)
    public static DepartmentEntity createDepartment(String name) {
        DepartmentEntity dept = new DepartmentEntity();
        dept.setDepartmentName(name);
        dept.setParentId(0L);
        dept.setSort(1);
        return dept;
    }

    // Counter reset (test isolation)
    public static void resetCounter() {
        counter.set(0);
    }
}
```

**Key Features**:
- ✅ AtomicInteger for sequential IDs
- ✅ Static factory methods (no constructors)
- ✅ All required fields populated
- ✅ Unique values (no constraint violations)
- ✅ Override variants for customization
- ✅ Related entity factories for FK setup

---

## Validation After Each Mode

### Mode 1 Validation (Integration Tests)

- [ ] Test class extends BaseIntegrationTest
- [ ] Uses @SpringBootTest and @Transactional
- [ ] Real Spring beans (@Autowired, not @Mock)
- [ ] Verifies both ResponseDTO and database state
- [ ] Test fixtures used for test data
- [ ] FK dependencies set up in @BeforeEach
- [ ] Tests pass: `./gradlew :sa-admin:test --tests {Entity}ServiceIntegrationTest`

### Mode 2 Validation (Test Fixtures)

- [ ] AtomicInteger counter for uniqueness
- [ ] Static factory methods (createEntity, createAddForm)
- [ ] All required fields populated with unique values
- [ ] Override variants for FK customization
- [ ] Related entity factories for dependencies
- [ ] Counter reset method for test isolation
- [ ] Can be used across multiple test classes

---

## Troubleshooting

### Common Issues

**Problem**: "Testcontainers failed to start"
- **Cause**: Docker not running
- **Solution**: Start Docker Desktop, ensure 4GB+ memory

**Problem**: "Unique constraint violation in tests"
- **Cause**: Not using test fixtures, hardcoded values
- **Solution**: Use {Entity}TestFixture.createEntity() with AtomicInteger

**Problem**: "Test data not rolled back"
- **Cause**: Missing @Transactional on test class
- **Solution**: Add `@Transactional` on test class for auto-rollback

**Problem**: "FK constraint violation"
- **Cause**: Related entity not created
- **Solution**: Use TestFixture.createRelatedEntity() in @BeforeEach

---

## Time Savings

**Before Consolidation** (separate skills):
- Integration test setup: 8 minutes
- Test fixture creation: 5 minutes
- Manual coordination: 5 minutes
- **Total: 18 minutes**

**After Consolidation** (unified suite):
- One command execution: 10 minutes (auto-includes fixtures)
- No coordination overhead
- **Total: 10 minutes**

**Time Savings**: 18 min → 10 min (44% improvement)

---

## Version History

**v2.0.0** (2026-01-27):
- Consolidated 2 testing skills (integration-test, fixture-generator) into mode-based workflow
- Added --mode=integration, --mode=fixtures, --mode=unit (future), --mode=e2e (future)
- Reduced test setup time: 18 min → 10 min (44% improvement)
- Backward compatibility via skill-aliases.json
- Unified test orchestration

**v1.0.0** (2025-12-01):
- Initial separate testing skills (integration-test, fixture-generator)
