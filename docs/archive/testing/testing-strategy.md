# SmartAdmin V3 Testing Strategy Guide

> **Document Version**: 1.0
> **Created**: 2026-01-21
> **Scope**: sa-admin business modules

---

## Overview

This guide defines the testing strategy for SmartAdmin V3, focusing on **pure unit tests** with Mockito for fast execution and high maintainability.

### Testing Philosophy

| Principle | Implementation |
|-----------|----------------|
| Fast Feedback | Pure unit tests (~10ms per test) |
| Isolation | Mock all dependencies |
| Deterministic | No random data, no time-dependent assertions |
| Maintainable | Centralized fixtures and base classes |

---

## Mock Strategy

### Pure Unit Tests with @Mock

SmartAdmin uses **pure Mockito tests** without Spring context loading for maximum speed.

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityLoginServiceTest extends BaseServiceTest {

    @InjectMocks
    private SecurityLoginService securityLoginService;

    @Mock
    private LoginFailDao loginFailDao;

    @Mock
    private SecurityConfigProvider securityConfigProvider;

    // Tests run in ~10ms each
}
```

### When to Use Each Mock Type

| Annotation | Use Case | Speed | Spring Context |
|------------|----------|-------|----------------|
| `@Mock` | Service/Manager unit tests | Fast (~10ms) | None |
| `@MockBean` | Controller tests with MockMvc | Medium (~500ms) | Partial |
| `@SpyBean` | Partial mocking of real beans | Slow (~2s) | Full |
| `MockedStatic` | Static method mocking (SA-Token) | Fast | None |

### Recommended Usage

```
Service Layer  -> @Mock (pure Mockito)
Manager Layer  -> @Mock (pure Mockito)
Controller     -> @WebMvcTest + @MockBean
SA-Token calls -> MockedStatic<StpUtil>
Cache methods  -> Mock Manager layer
```

---

## Test Infrastructure Design

### Base Test Classes

#### BaseServiceTest

**Purpose**: Provides ResponseDTO assertion helpers for Service layer tests.

**Key methods**:
```java
protected void assertSuccess(ResponseDTO<T> response)
protected void assertSuccessWithData(ResponseDTO<T> response, T expectedData)
protected void assertError(ResponseDTO<?> response)
protected void assertErrorContains(ResponseDTO<?> response, String expectedSubstring)
protected void assertErrorCode(ResponseDTO<?> response, int expectedCode)
```

**Usage**: Extend this class in your Service tests.

**Implementation**: `sa-admin/src/test/java/net/lab1024/sa/admin/base/BaseServiceTest.java`

---

#### BaseManagerTest

**Purpose**: Common functionality for Manager layer tests.

**Key design principles**:
- Verify DAO methods are called in correct order (use `InOrder`)
- Ensure transaction boundaries (multiple DAO calls should be atomic)
- Verify cache invalidation triggers

**Usage**: Extend this class in your Manager tests.

**Implementation**: `sa-admin/src/test/java/net/lab1024/sa/admin/base/BaseManagerTest.java`

---

#### BaseControllerTest

**Purpose**: Common functionality for Controller layer tests with MockMvc.

**Key features**:
```java
@Autowired
protected MockMvc mockMvc;

@Autowired
protected ObjectMapper objectMapper;

protected String toJson(Object obj) throws Exception
protected String getResponseBody(MvcResult result) throws Exception
```

**Usage**: Extend this class in your Controller tests with `@WebMvcTest`.

**Implementation**: `sa-admin/src/test/java/net/lab1024/sa/admin/base/BaseControllerTest.java`

---

### Fixture Classes

Fixtures provide reusable test data with builder patterns.

#### Design Principles

1. **Immutable Defaults**: Constants for commonly used values
2. **Builder Pattern**: Flexible construction for variations
3. **Named Factories**: Methods for common scenarios (e.g., `disabledEmployee()`)
4. **Domain-Specific**: One fixture per domain entity

```java
// Good: Named factory methods for common scenarios
public static EmployeeEntity adminEmployee() {
    return defaultEmployee()
        .administratorFlag(true)
        .loginName("superadmin")
        .build();
}

// Good: Builder for custom scenarios
public static EmployeeEntity.EmployeeEntityBuilder defaultEmployee() {
    return EmployeeEntity.builder()
        .employeeId(DEFAULT_EMPLOYEE_ID)
        .loginName(DEFAULT_LOGIN_NAME)
        // ... default values
}
```

---

### Mock Configurations

Centralize mock configurations for reuse across tests.

**Example**: `MockSecurityConfig.java`

```java
/**
 * Default security configuration for most tests
 */
public static SecurityConfigProvider defaultConfig() {
    SecurityConfigProvider mock = mock(SecurityConfigProvider.class);
    when(mock.getLoginFailMaxTimes()).thenReturn(5);
    when(mock.getLoginFailLockSeconds()).thenReturn(1800L);
    when(mock.isPasswordComplexityEnabled()).thenReturn(true);
    return mock;
}
```

**Implementation**: `sa-admin/src/test/java/net/lab1024/sa/admin/mock/MockSecurityConfig.java`

---

## Naming Conventions

### Test Class Names

| Component | Pattern | Example |
|-----------|---------|---------|
| Service Test | `{ServiceName}Test` | `LoginServiceTest` |
| Manager Test | `{ManagerName}Test` | `EmployeeManagerTest` |
| Controller Test | `{ControllerName}Test` | `EmployeeControllerTest` |

### Test Method Names

Pattern: `test{Method}_{Scenario}_{ExpectedResult}`

| Example | Description |
|---------|-------------|
| `testLogin_ValidCredentials_ReturnsToken` | Happy path |
| `testLogin_InvalidPassword_ReturnsError` | Error case |
| `testLogin_AccountLocked_ReturnsLockMessage` | Edge case |
| `testAddEmployee_DuplicateLoginName_ReturnsError` | Validation error |

### DisplayName Annotations

Use `@DisplayName` for human-readable test names in reports:

```java
@Test
@DisplayName("Valid credentials should return token and user info")
void testLogin_ValidCredentials_ReturnsToken() { }

@Test
@DisplayName("Wrong password should record failure and return error")
void testLogin_WrongPassword_RecordsFailureAndReturnsError() { }
```

### Nested Test Classes

Organize related tests using `@Nested`:

```java
@Nested
@DisplayName("Login Tests")
class LoginTests {
    @Test void testLogin_ValidCredentials_ReturnsToken() { }
    @Test void testLogin_InvalidPassword_ReturnsError() { }
}

@Nested
@DisplayName("Account Locking Tests")
class AccountLockingTests {
    @Test void testCheckLogin_AccountLocked_ReturnsFail() { }
    @Test void testCheckLogin_LockExpired_AllowsLogin() { }
}
```

---

## Coverage Requirements by Layer

### Service Layer (>= 85% coverage)

**What to Test**:
- Business logic paths
- Input validation
- Error handling
- ResponseDTO construction
- Interactions with Manager/Dao

**What NOT to Test**:
- Simple getter/setter methods
- Pass-through methods with no logic

```java
@Test
void testAddEmployee_ValidForm_CallsManagerAndReturnsSuccess() {
    // Given
    EmployeeAddForm form = EmployeeFixture.defaultAddForm();
    when(employeeDao.selectByLoginName(anyString())).thenReturn(null);
    when(departmentDao.selectById(anyLong())).thenReturn(new DepartmentEntity());

    // When
    ResponseDTO<Long> result = employeeService.addEmployee(form);

    // Then
    assertSuccess(result);
    verify(employeeManager).saveEmployee(any(), anyList());
}
```

### Manager Layer (>= 80% coverage)

**What to Test**:
- DAO call sequences
- Transaction boundaries (verify all expected calls are made)
- Cache operations (invalidation triggers)

**What NOT to Test**:
- Transaction rollback behavior (integration test territory)
- Actual cache implementation

```java
@Test
void testSaveEmployee_InsertsEmployeeAndRoles() {
    // Given
    EmployeeEntity employee = EmployeeFixture.defaultEmployee().build();
    List<Long> roleIds = List.of(1L, 2L);

    // When
    employeeManager.saveEmployee(employee, roleIds);

    // Then - verify correct order
    InOrder inOrder = inOrder(employeeDao, roleEmployeeDao);
    inOrder.verify(employeeDao).insert(employee);
    inOrder.verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
}
```

### Controller Layer (>= 60% coverage)

**What to Test**:
- Request mapping
- Request validation (@Valid)
- Response structure
- HTTP status codes

**What NOT to Test**:
- Business logic (tested in Service layer)
- Detailed error scenarios (tested in Service layer)

```java
@Test
void testGetEmployee_ValidId_Returns200() throws Exception {
    // Given
    when(employeeService.getEmployeeById(1L))
        .thenReturn(ResponseDTO.ok(new EmployeeVO()));

    // When/Then
    mockMvc.perform(get("/admin/employee/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));
}
```

### Coverage Targets Summary

| Layer | Line Coverage | Branch Coverage |
|-------|---------------|-----------------|
| **Service** | ≥ 85% | ≥ 75% |
| **Manager** | ≥ 80% | ≥ 70% |
| **Controller** | ≥ 60% | ≥ 50% |
| **Overall** | ≥ 80% | ≥ 70% |

**Excluded**:
- DTO/VO/Form classes (data containers)
- Entity classes
- Constants/Enums

---

## Best Practices

### 1. Use ResponseDTO Assertion Helpers

Always use helper methods from BaseServiceTest for clearer failure messages:

```java
// ✅ Good - clear failure messages
assertSuccess(response);
assertError(response);
assertErrorContains(response, "already exists");

// ❌ Avoid - less informative
assertTrue(response.getOk());
```

### 2. Verify Interaction Order

For Manager layer, verify the sequence of DAO calls using `InOrder`:

```java
@Test
void testUpdateEmployeeRole_DeletesThenInserts() {
    Long employeeId = 1L;
    List<Long> newRoles = List.of(2L, 3L);

    employeeManager.updateEmployeeRole(employeeId, newRoles);

    InOrder inOrder = inOrder(roleEmployeeDao);
    inOrder.verify(roleEmployeeDao).deleteByEmployeeId(employeeId);
    inOrder.verify(roleEmployeeDao, times(2)).insert(any());
}
```

### 3. Handle SA-Token Static Methods

Use `MockedStatic` for SA-Token calls:

```java
@Test
void testLogin_Success_SetsToken() {
    try (MockedStatic<StpUtil> mockedStatic = mockStatic(StpUtil.class)) {
        mockedStatic.when(StpUtil::getTokenValue).thenReturn("mock-token");

        ResponseDTO<LoginResultVO> result = loginService.login(loginForm);

        assertSuccess(result);
        assertEquals("mock-token", result.getData().getToken());
    }
}
```

### 4. Use Deterministic Test Data

Use constants from fixtures, not random values:

```java
// ✅ Good - deterministic
EmployeeEntity employee = EmployeeFixture.defaultEmployee().build();
assertEquals(EmployeeFixture.DEFAULT_LOGIN_NAME, employee.getLoginName());

// ❌ Avoid - random data causes flaky tests
EmployeeEntity employee = new EmployeeEntity();
employee.setLoginName(UUID.randomUUID().toString());
```

### 5. Use lenient() for Setup Stubs

Use `lenient()` for setup stubs that may not be used in all tests:

```java
@BeforeEach
void setUp() {
    lenient().when(configService.getConfigValue(ConfigKeyEnum.SUPER_PASSWORD))
        .thenReturn("super_secret");
    lenient().when(level3ProtectConfigService.isTwoFactorLoginEnabled())
        .thenReturn(false);
}
```

### 6. Avoid Time-Based Flaky Assertions

```java
// ❌ Bad - may fail due to timing
assertTrue(lockTime.isBefore(LocalDateTime.now()));

// ✅ Good - use fixed time reference
LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 21, 10, 0, 0);
LoginFailEntity locked = SecurityFixture.locked(5, 10); // Locked 10 min ago
assertTrue(locked.getLoginLockBeginTime().plusSeconds(lockSeconds).isBefore(fixedNow));
```

### 7. One Logical Assertion per Test

Each test should verify one logical assertion (or closely related assertions):

```java
// ✅ Good - focused test
@Test
void testLogin_WrongPassword_ReturnsError() {
    // Given
    setupWrongPasswordScenario();

    // When
    ResponseDTO<?> result = loginService.login(form);

    // Then
    assertError(result);
}

@Test
void testLogin_WrongPassword_RecordsFailure() {
    // Given
    setupWrongPasswordScenario();

    // When
    loginService.login(form);

    // Then
    verify(securityLoginService).recordLoginFail(anyLong(), any(), anyString(), any());
}
```

### 8. Reset Mocks Between Tests

Reset mocks between tests using `@BeforeEach`:

```java
@BeforeEach
void setUp() {
    // Reset any state that might leak between tests
    reset(employeeDao, roleEmployeeDao);

    // Setup common mock behaviors
    setupDefaultMocks();
}
```

---

## Mocking Complex Dependencies

### Example: LoginService with Multiple Dependencies

```java
@ExtendWith(MockitoExtension.class)
class LoginServiceTest extends BaseServiceTest {

    @InjectMocks
    private LoginService loginService;

    // Core dependencies - always needed
    @Mock private EmployeeDao employeeDao;
    @Mock private CaptchaService captchaService;
    @Mock private ApiEncryptService apiEncryptService;
    @Mock private SecurityLoginService securityLoginService;
    @Mock private SecurityPasswordService protectPasswordService;
    @Mock private LoginManager loginManager;
    @Mock private LoginLogService loginLogService;

    // Secondary dependencies - mock with defaults
    @Mock private ConfigService configService;
    @Mock private RoleEmployeeDao roleEmployeeDao;
    @Mock private RoleMenuDao roleMenuDao;
    @Mock private Level3ProtectConfigService level3ProtectConfigService;
    @Mock private MailService mailService;
    @Mock private CacheService cacheService;

    @BeforeEach
    void setUp() {
        // Setup common behaviors for secondary dependencies
        lenient().when(configService.getConfigValue(ConfigKeyEnum.SUPER_PASSWORD))
            .thenReturn("super_secret");
        lenient().when(level3ProtectConfigService.isTwoFactorLoginEnabled())
            .thenReturn(false);
        lenient().when(apiEncryptService.decrypt(anyString()))
            .thenAnswer(inv -> inv.getArgument(0)); // Return input unchanged
    }
}
```

### JetCache/Cache Operations

Mock the Manager layer instead of testing cache directly:

```java
@Test
void testGetLoginEmployee_CallsManagerCorrectly() {
    // Given
    RequestEmployee expected = new RequestEmployee();
    when(loginManager.getRequestEmployee(1L)).thenReturn(expected);

    // When
    RequestEmployee result = loginService.getLoginEmployee("1:1", mockRequest);

    // Then
    assertEquals(expected, result);
    verify(loginManager).getRequestEmployee(1L);
}
```

---

## Test Execution

### Run Tests

```bash
# All tests
./gradlew :sa-admin:test

# Specific test class
./gradlew :sa-admin:test --tests "LoginServiceTest"

# Tests matching pattern
./gradlew :sa-admin:test --tests "*Security*"

# Single test method
./gradlew :sa-admin:test --tests "LoginServiceTest.testLogin_ValidCredentials_ReturnsToken"
```

### Generate Coverage Reports

```bash
# Generate JaCoCo report
./gradlew :sa-admin:jacocoTestReport

# View HTML report
open sa-admin/build/reports/jacoco/test/html/index.html

# Verify coverage thresholds
./gradlew :sa-admin:jacocoTestCoverageVerification
```

→ For more commands, see [Quick Reference](./quick-reference.md)

---

## Anti-Patterns to Avoid

| Anti-Pattern | Problem | Correct Approach |
|--------------|---------|------------------|
| `@SpringBootTest` for unit tests | Slow, 2s+ startup | Use `@ExtendWith(MockitoExtension.class)` |
| Random test data | Flaky tests | Use deterministic fixtures |
| Testing private methods | Fragile tests | Test through public API |
| Multiple assertions per test | Hard to diagnose failures | One logical assertion per test |
| Mocking what you own | Tight coupling | Test real collaborators where possible |
| `Time.now()` in assertions | Race conditions | Use fixed time references |
| Shared mutable state | Test interference | Reset in `@BeforeEach` |
| Testing implementation details | Brittle tests | Test behavior, not implementation |

---

## Related Documentation

- **Implementation Plan**: [unit-test-implementation-plan.md](./unit-test-implementation-plan.md) - 6-week roadmap
- **Architecture Fixes**: [architecture/overview.md](./architecture/overview.md) - Required fixes before testing
- **Quick Reference**: [quick-reference.md](./quick-reference.md) - Commands and rules cheatsheet
- **Project Conventions**: [../../CLAUDE.md](../../CLAUDE.md) - SmartAdmin coding standards
