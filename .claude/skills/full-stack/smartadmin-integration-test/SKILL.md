---
name: smartadmin-integration-test
description: [P0 - Critical] Auto-generate Spring Boot integration tests with Testcontainers for SmartAdmin's layered architecture. Use when creating integration tests for Service/Manager/Controller classes, testing database persistence, validating @Transactional behavior, verifying cache operations with Redis, testing full request/response cycles, or ensuring cross-layer integration works correctly. Triggers automatically when (1) User asks to create/generate integration tests, (2) User mentions testing database operations or transactions, (3) User wants to test Redis cache behavior, (4) After implementing new Service/Manager/Controller methods that need integration validation, (5) User explicitly requests Testcontainers setup.
---

# SmartAdmin Integration Test Generator

Auto-generate Spring Boot integration tests with Testcontainers for SmartAdmin's strict layered architecture (Controller → Service → Manager → Dao).

## Quick Start

**Most common usage:**
```
User: "Create integration tests for EmployeeService"
```

You will:
1. Analyze the target class (Service/Manager/Controller)
2. Identify dependencies and database operations
3. Generate integration test class extending BaseIntegrationTest
4. Create test fixtures (builders for test data)
5. Generate test cases covering CRUD operations
6. Add assertions for ResponseDTO, database state, cache behavior

## Core Test Patterns

SmartAdmin has **three integration test patterns** based on the layer being tested:

### Pattern 1: Service Layer Tests

Test business logic with real database and dependencies.

```java
@SpringBootTest
@Transactional
class EmployeeServiceIntegrationTest extends BaseIntegrationTest {
    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeDao employeeDao;

    private Long testDepartmentId;

    @BeforeEach
    void setUp() {
        // Create FK dependencies
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

**Key characteristics:**
- Extends `BaseIntegrationTest` (provides @SpringBootTest + @Transactional)
- Uses `@Autowired` for real beans (not @Mock)
- Verifies both ResponseDTO **and** database state
- `@Transactional` causes auto-rollback after each test

### Pattern 2: Manager Layer Tests

Test @Transactional methods coordinating multiple DAOs.

```java
@SpringBootTest
@Transactional
class EmployeeManagerIntegrationTest extends BaseIntegrationTest {
    @Autowired private EmployeeManager employeeManager;
    @Autowired private EmployeeDao employeeDao;
    @Autowired private RoleEmployeeDao roleEmployeeDao;

    @Test
    @DisplayName("Should save employee and roles in single transaction")
    void saveEmployee_ValidData_CommitsTransaction() {
        // Given
        EmployeeEntity employee = EmployeeTestFixture.createEntity();
        List<Long> roleIds = List.of(1L, 2L);

        // When
        employeeManager.saveEmployee(employee, roleIds);

        // Then - Verify employee saved
        EmployeeEntity saved = employeeDao.selectById(employee.getEmployeeId());
        assertNotNull(saved);

        // Then - Verify roles saved
        List<RoleEmployeeEntity> roles = roleEmployeeDao.selectList(
            Wrappers.<RoleEmployeeEntity>lambdaQuery()
                .eq(RoleEmployeeEntity::getEmployeeId, employee.getEmployeeId())
        );
        assertEquals(2, roles.size());
    }
}
```

### Pattern 3: Controller Tests (MockMvc)

Test full HTTP request/response cycle with authentication.

```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIntegrationTest extends BaseIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private EmployeeDao employeeDao;

    @Test
    @DisplayName("Should query employees with pagination")
    void queryEmployee_ValidRequest_ReturnsPaginatedResults() throws Exception {
        // Given - Insert test data
        employeeDao.insert(EmployeeTestFixture.createEntity("emp001"));
        employeeDao.insert(EmployeeTestFixture.createEntity("emp002"));

        // When
        MvcResult result = mockMvc.perform(
            post("/employee/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"pageNum\":1,\"pageSize\":10}")
        ).andExpect(status().isOk()).andReturn();

        // Then - Parse ResponseDTO<PageResult<EmployeeVO>>
        ResponseDTO<PageResult<EmployeeVO>> response =
            JsonUtil.parseObject(result.getResponse().getContentAsString(),
                new TypeReference<>() {});

        assertTrue(response.getOk());
        assertTrue(response.getData().getTotal() >= 2);
    }
}
```

## Test Generation Workflow

### Step 1: Analyze Target Class

Read the target class to understand:
- Layer (Controller/Service/Manager)
- Dependencies (@Autowired or constructor injection)
- Methods to test (public methods)
- Database operations (DAO calls)
- Cache operations (@Cacheable, @CacheEvict)

### Step 2: Generate Test Class

**File location pattern:**
```
Source: sa-admin/src/main/java/.../service/EmployeeService.java
Test:   sa-admin/src/test/java/.../service/EmployeeServiceIntegrationTest.java
```

**Class structure:**
```java
@SpringBootTest
@Transactional
@DisplayName("{ClassName} Integration Tests")
class {ClassName}IntegrationTest extends BaseIntegrationTest {
    @Autowired private {ClassName} {fieldName};
    @Autowired private {Required}Dao {dao}; // For state verification

    @BeforeEach
    void setUp() {
        // Create FK dependencies
    }

    // Tests...
}
```

### Step 3: Generate Test Cases

For each public method, generate tests following this naming pattern:

**Pattern:** `{methodName}_{scenario}_{expectedBehavior}`

**Examples:**
- `addEmployee_ValidForm_PersistsToDatabase`
- `updateEmployee_NonexistentId_ReturnsError`
- `queryEmployee_WithPagination_ReturnsPagedResults`

**Test structure:**
```java
@Test
@DisplayName("Should {expected behavior}")
void {methodName}_{scenario}_{expectedBehavior}() {
    // Given - Arrange test data

    // When - Call method under test

    // Then - Assert ResponseDTO

    // Then - Assert database state
}
```

### Step 4: Create Test Fixtures

Generate fixture class with builder methods:

```java
public class EmployeeTestFixture {

    public static EmployeeAddForm createAddForm(Long departmentId) {
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("test_" + System.currentTimeMillis()); // Unique
        form.setActualName("Test Employee");
        form.setPhone("13800000001");
        form.setDepartmentId(departmentId);
        return form;
    }

    public static EmployeeEntity createEntity() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setLoginName("test_" + System.currentTimeMillis());
        entity.setActualName("Test Employee");
        entity.setDepartmentId(1L);
        entity.setDeletedFlag(false);
        return entity;
    }
}
```

**File location:**
```
Fixture: sa-admin/src/test/java/.../EmployeeTestFixture.java
```

## Common Test Scenarios

### Foreign Key Validation

```java
@Test
@DisplayName("Should fail when department does not exist")
void addEmployee_InvalidDepartment_ReturnsError() {
    EmployeeAddForm form = EmployeeTestFixture.createAddForm(99999L); // Non-existent
    ResponseDTO<String> response = employeeService.addEmployee(form);
    assertFalse(response.getOk());
    assertTrue(response.getMsg().contains("部门"));
}
```

### Unique Constraint Validation

```java
@Test
@DisplayName("Should fail when login name already exists")
void addEmployee_DuplicateLoginName_ReturnsError() {
    EmployeeAddForm form1 = EmployeeTestFixture.createAddForm(testDepartmentId);
    employeeService.addEmployee(form1);

    EmployeeAddForm form2 = EmployeeTestFixture.createAddForm(testDepartmentId);
    form2.setLoginName(form1.getLoginName()); // Duplicate
    ResponseDTO<String> response = employeeService.addEmployee(form2);

    assertFalse(response.getOk());
}
```

### Pagination Testing

```java
@Test
@DisplayName("Should return paginated results")
void queryEmployee_WithPagination_ReturnsPaginatedResults() {
    // Given - Insert 15 employees
    for (int i = 0; i < 15; i++) {
        employeeDao.insert(EmployeeTestFixture.createEntity());
    }

    // When - Query page 1 (pageSize 10)
    EmployeeQueryForm queryForm = new EmployeeQueryForm();
    queryForm.setPageNum(1);
    queryForm.setPageSize(10);
    ResponseDTO<PageResult<EmployeeVO>> response = employeeService.queryEmployee(queryForm);

    // Then
    assertTrue(response.getOk());
    PageResult<EmployeeVO> page = response.getData();
    assertEquals(10, page.getList().size());
    assertTrue(page.getTotal() >= 15);
}
```

### Cache Behavior Testing

```java
@Test
@DisplayName("Should cache result on first query")
void queryDepartment_FirstCall_CachesResult() {
    // Given
    DepartmentEntity dept = DepartmentTestFixture.createEntity();
    departmentDao.insert(dept);

    // When - First call (cache miss)
    DepartmentEntity result1 = departmentCacheManager.queryDepartment(dept.getDepartmentId());

    // Then - Verify cached
    Cache cache = cacheManager.getCache("department");
    assertNotNull(cache.get(dept.getDepartmentId()));

    // When - Second call (cache hit)
    DepartmentEntity result2 = departmentCacheManager.queryDepartment(dept.getDepartmentId());

    // Then - Same result
    assertEquals(result1.getDepartmentName(), result2.getDepartmentName());
}
```

## Best Practices

### 1. Create FK Dependencies in @BeforeEach

```java
@BeforeEach
void setUp() {
    DepartmentEntity dept = DepartmentTestFixture.createDepartment("Test Dept");
    departmentDao.insert(dept);
    testDepartmentId = dept.getDepartmentId();
}
```

### 2. Always Verify Both ResponseDTO and Database

```java
// Verify ResponseDTO
assertTrue(response.getOk());

// Verify database state
EmployeeEntity saved = employeeDao.selectById(employeeId);
assertNotNull(saved);
assertEquals(expected, saved.getActualName());
```

### 3. Use Unique Test Data

Prevent collisions with timestamps:

```java
form.setLoginName("test_" + System.currentTimeMillis());
```

### 4. Leverage @Transactional Auto-Rollback

No manual cleanup needed:

```java
@Test
@Transactional // Auto-rollback after test
void testAdd() {
    employeeService.addEmployee(form); // Will be rolled back
}
```

### 5. Group Tests with @Nested

```java
@Nested
@DisplayName("addEmployee() Tests")
class AddEmployeeTests {
    @Test
    void addEmployee_ValidForm_Success() { }

    @Test
    void addEmployee_InvalidDepartment_Error() { }
}
```

## SmartAdmin-Specific Assertions

### For ResponseDTO

```java
// Success
assertTrue(response.getOk(), "Expected success: " + response.getMsg());

// Error
assertFalse(response.getOk());
assertEquals(expectedErrorCode.getCode(), response.getCode());
```

### For PageResult

```java
PageResult<EmployeeVO> page = response.getData();
assertNotNull(page);
assertTrue(page.getTotal() > 0);
assertTrue(page.getList().size() <= page.getPageSize());
```

### For Database State

```java
EmployeeEntity saved = employeeDao.selectById(employeeId);
assertNotNull(saved, "Employee should be saved");
assertEquals(form.getActualName(), saved.getActualName());
```

## References

Detailed patterns and advanced scenarios:
- [references/testcontainers-patterns.md](references/testcontainers-patterns.md) - Testcontainers configuration
- [references/test-fixtures-patterns.md](references/test-fixtures-patterns.md) - Advanced fixture patterns
- [references/assertion-patterns.md](references/assertion-patterns.md) - Comprehensive assertion examples

## Validation Checklist

Before completing test generation:

- [ ] Test class extends `BaseIntegrationTest`
- [ ] Test class has `@SpringBootTest` and `@Transactional`
- [ ] Dependencies use `@Autowired` (not `@Mock`)
- [ ] FK dependencies created in `@BeforeEach`
- [ ] Each test verifies ResponseDTO **and** database state
- [ ] Test names follow `{method}_{scenario}_{expected}` pattern
- [ ] `@DisplayName` annotations present
- [ ] Test fixtures in separate class
- [ ] Unique test data (timestamps) to avoid collisions
- [ ] Tests cover happy path and error cases
