# SmartAdmin Testing Suite - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: smartadmin-testing-suite (P2 - Productivity/Composite)

---

## Command Quick Reference

| Command | Purpose | Duration |
|---------|---------|----------|
| Unit Tests | Run JUnit tests | ~5 min |
| Integration Tests | Testcontainers + DB | ~15 min |
| ArchUnit Tests | Architecture validation | ~3 min |
| Test Coverage | Generate JaCoCo report | ~8 min |
| Full Test Suite | All tests + coverage | ~30 min |

---

## Testing Suite Overview

**Composite Skill** - Integrates multiple testing approaches:

1. **Unit Tests** (JUnit 5 + Mockito)
2. **Integration Tests** (Spring Boot Test + Testcontainers)
3. **ArchUnit Tests** (Architecture enforcement)
4. **E2E Tests** (Optional - API level)

**Test Pyramid**:
```
     /\
    /E2E\        5% (Slow, brittle)
   /─────\
  /Integ-\      15% (Medium speed)
 /─ration─\
/──────────\
|   Unit   |    80% (Fast, reliable)
└──────────┘
```

---

## Testing Strategy Matrix

| Layer | Test Type | Tool | Coverage Target | Duration |
|-------|-----------|------|-----------------|----------|
| **Controller** | Integration | MockMvc + Testcontainers | 80% | 5-10 min |
| **Service** | Unit | JUnit + Mockito | 90% | 3-5 min |
| **Manager** | Integration | @Transactional + DB | 85% | 8-12 min |
| **Dao** | Integration | MyBatis Test + Testcontainers | 90% | 5-8 min |
| **Architecture** | ArchUnit | Static analysis | 100% | 2-3 min |

---

## Pattern 1: Unit Tests (Service Layer)

**Use**: [SmartAdmin CRUD Generator](../../foundation/full-stack/smartadmin-crud-generator/) includes unit test templates

**Example**:
```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeDao employeeDao;

    @Mock
    private EmployeeManager employeeManager;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void queryPage_shouldReturnPagedResult() {
        // Given
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);

        List<EmployeeEntity> entities = Arrays.asList(
            createEmployee(1L, "Alice"),
            createEmployee(2L, "Bob")
        );

        when(employeeDao.queryPage(any())).thenReturn(entities);

        // When
        PageResult<EmployeeVO> result = employeeService.queryPage(form);

        // Then
        assertThat(result.getList()).hasSize(2);
        assertThat(result.getList().get(0).getName()).isEqualTo("Alice");
        verify(employeeDao).queryPage(any());
    }

    @Test
    void addEmployee_shouldDelegateToManager() {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName("Charlie");

        // When
        employeeService.addEmployee(form);

        // Then
        verify(employeeManager).addEmployee(form);
    }

    private EmployeeEntity createEmployee(Long id, String name) {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }
}
```

**Coverage Target**: 90%+
**Time to Write**: 5-10 minutes per service

---

## Pattern 2: Integration Tests (Controller + DB)

**Use**: [SmartAdmin Integration Test](../../foundation/full-stack/smartadmin-integration-test/)

**Setup**:
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EmployeeDao employeeDao;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void addEmployee_shouldCreateRecord() throws Exception {
        // Given
        String requestBody = """
            {
                "name": "Alice",
                "email": "alice@example.com",
                "deptId": 1
            }
            """;

        // When & Then
        mockMvc.perform(post("/api/employee/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").exists());

        // Verify database
        List<EmployeeEntity> employees = employeeDao.selectAll();
        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void queryPage_shouldReturnPagedData() throws Exception {
        // Given - Insert test data
        insertTestEmployees(5);

        // When & Then
        mockMvc.perform(post("/api/employee/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pageNum\":1,\"pageSize\":3}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.list").isArray())
            .andExpect(jsonPath("$.data.list.length()").value(3))
            .andExpect(jsonPath("$.data.total").value(5));
    }

    private void insertTestEmployees(int count) {
        for (int i = 1; i <= count; i++) {
            EmployeeEntity entity = new EmployeeEntity();
            entity.setName("Employee" + i);
            entity.setEmail("emp" + i + "@example.com");
            employeeDao.insert(entity);
        }
    }
}
```

**Coverage Target**: 80%+
**Time to Write**: 10-15 minutes per controller

---

## Pattern 3: ArchUnit Tests (Architecture Validation)

**Use**: [ArchUnit Test Generator](../../foundation/backend/archunit-test-generator/)

**Example**:
```java
@AnalyzeClasses(packages = "net.lab1024.sa")
class ArchitectureTest {

    @ArchTest
    static final ArchRule transactionalMustBeInManager =
        methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
            .because("@Transactional must only be in Manager layer");

    @ArchTest
    static final ArchRule serviceLayerDependencyRule =
        classes()
            .that().resideInAPackage("..service..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("..service..", "..dao..", "..entity..", "..domain..", "java..")
            .because("Service should not depend on Controller or Manager");

    @ArchTest
    static final ArchRule noFieldInjection =
        fields()
            .should().notBeAnnotatedWith(Autowired.class)
            .because("Use constructor injection instead of field injection");

    @ArchTest
    static final ArchRule servicesShouldUseVavrOption =
        methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..service..")
            .and().haveRawReturnType(Optional.class)
            .should(new ArchCondition<>("use Vavr Option instead of Optional") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    String message = String.format(
                        "Method %s returns java.util.Optional instead of io.vavr.control.Option",
                        method.getFullName()
                    );
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            });
}
```

**Coverage**: 100% (architecture rules always enforced)
**Time to Run**: 2-3 minutes

---

## Pattern 4: Test Coverage with JaCoCo

**Setup**:
```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.11"
}

jacocoTestReport {
    dependsOn test

    reports {
        xml.required = true
        html.required = true
        csv.required = false
    }
}

jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80% coverage
            }
        }

        rule {
            element = 'CLASS'
            limit {
                minimum = 0.60  // 60% per class
            }
        }
    }
}
```

**Run Coverage**:
```bash
# Generate coverage report
./gradlew test jacocoTestReport

# Verify coverage threshold
./gradlew jacocoTestCoverageVerification

# View report
open build/reports/jacoco/test/html/index.html
```

**Coverage Targets**:
- **Overall**: ≥80%
- **Service Layer**: ≥90%
- **Manager Layer**: ≥85%
- **Controller**: ≥80%

**Time to Generate**: 8-10 minutes

---

## Pattern 5: Test Fixtures (Reusable Test Data)

**Use**: [Test Fixture Generator](../../foundation/testing/test-fixture-generator/)

**Example**:
```java
public class EmployeeFixtures {

    /**
     * Create default employee for testing
     */
    public static EmployeeEntity defaultEmployee() {
        return EmployeeEntity.builder()
            .id(1L)
            .name("Test Employee")
            .email("test@example.com")
            .deptId(1L)
            .status(EmployeeStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .deleted(false)
            .build();
    }

    /**
     * Create employee with custom name
     */
    public static EmployeeEntity employeeWithName(String name) {
        return defaultEmployee().toBuilder()
            .name(name)
            .email(name.toLowerCase() + "@example.com")
            .build();
    }

    /**
     * Create multiple employees
     */
    public static List<EmployeeEntity> employees(int count) {
        return IntStream.range(1, count + 1)
            .mapToObj(i -> employeeWithName("Employee" + i))
            .collect(Collectors.toList());
    }
}

// Usage
@Test
void testEmployeeQuery() {
    EmployeeEntity employee = EmployeeFixtures.defaultEmployee();
    employeeDao.insert(employee);

    List<EmployeeEntity> result = employeeDao.selectAll();
    assertThat(result).hasSize(1);
}
```

**Time to Create**: 5-8 minutes per fixture class

---

## Full Test Suite Execution

### Command Sequence

```bash
# 1. Unit tests (fast)
./gradlew test --tests "*Test" --exclude-task integrationTest

# 2. Integration tests (slower)
./gradlew integrationTest

# 3. ArchUnit tests (architecture validation)
./gradlew test --tests ArchitectureTest

# 4. Coverage report
./gradlew jacocoTestReport

# 5. Coverage verification
./gradlew jacocoTestCoverageVerification

# Full suite (all in one)
./gradlew clean build
```

**Time Breakdown**:
- Unit tests: 5-8 minutes
- Integration tests: 12-18 minutes
- ArchUnit: 2-3 minutes
- Coverage: 3-5 minutes
- **Total**: ~30 minutes

---

## CI/CD Integration

**GitHub Actions**:
```yaml
name: Test Suite

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'gradle'

      - name: Unit Tests
        run: ./gradlew test --exclude-task integrationTest

      - name: Integration Tests
        run: ./gradlew integrationTest

      - name: ArchUnit Tests
        run: ./gradlew test --tests ArchitectureTest

      - name: Coverage Report
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          files: build/reports/jacoco/test/jacocoTestReport.xml

      - name: Verify Coverage
        run: ./gradlew jacocoTestCoverageVerification
```

---

## Testing Checklist

### Before Commit

- [ ] All unit tests pass (`./gradlew test`)
- [ ] Integration tests pass (`./gradlew integrationTest`)
- [ ] ArchUnit tests pass (no architecture violations)
- [ ] Coverage ≥80% (`./gradlew jacocoTestCoverageVerification`)
- [ ] No test code in production code (verify with grep)

### Before PR

- [ ] All tests pass in CI/CD pipeline
- [ ] Coverage report uploaded to Codecov
- [ ] No flaky tests (run 3 times to verify)
- [ ] Test execution time < 30 minutes
- [ ] Documentation updated for new tests

### Before Release

- [ ] Full regression test suite passes
- [ ] Load testing completed (if applicable)
- [ ] Manual smoke testing on staging
- [ ] All critical paths covered by tests

---

## Common Testing Patterns

### Pattern: Test @Transactional Manager

```java
@SpringBootTest
@Transactional
class EmployeeManagerTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @Test
    void addEmployee_shouldCommitTransaction() {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName("Alice");

        // When
        employeeManager.addEmployee(form);

        // Then - Verify transaction committed
        List<EmployeeEntity> employees = employeeDao.selectAll();
        assertThat(employees).hasSize(1);
    }

    @Test
    void addEmployee_shouldRollbackOnError() {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName(null);  // Invalid

        // When & Then
        assertThatThrownBy(() -> employeeManager.addEmployee(form))
            .isInstanceOf(BusinessException.class);

        // Verify rollback
        List<EmployeeEntity> employees = employeeDao.selectAll();
        assertThat(employees).isEmpty();
    }
}
```

---

### Pattern: Test Exception Handling

```java
@Test
void addEmployee_shouldReturnErrorWhenValidationFails() throws Exception {
    // Given - Invalid email
    String requestBody = """
        {
            "name": "Alice",
            "email": "invalid-email",
            "deptId": 1
        }
        """;

    // When & Then
    mockMvc.perform(post("/api/employee/add")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(10001))  // USER_ERROR_PARAM
        .andExpect(jsonPath("$.msg").value(containsString("email")));
}
```

---

## Time Estimates

| Task | Duration |
|------|----------|
| Write unit test (simple) | 5 min |
| Write unit test (complex) | 10 min |
| Write integration test | 15 min |
| Write ArchUnit rule | 8 min |
| Create test fixture | 5 min |
| Generate coverage report | 8 min |
| **Full test suite** | **30 min** |

---

**See Also**:
- [SmartAdmin CRUD Generator](../../foundation/full-stack/smartadmin-crud-generator/) - Includes test templates
- [SmartAdmin Integration Test](../../foundation/full-stack/smartadmin-integration-test/) - Testcontainers setup
- [ArchUnit Test Generator](../../foundation/backend/archunit-test-generator/) - Architecture tests
- [Test Fixture Generator](../../foundation/testing/test-fixture-generator/) - Reusable test data
