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

Purpose: Common functionality for Service layer tests.

```java
package net.lab1024.sa.admin.base;

import net.lab1024.sa.base.common.domain.ResponseDTO;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseServiceTest {

    /**
     * Assert that ResponseDTO indicates success
     */
    protected <T> void assertSuccess(ResponseDTO<T> response) {
        assertNotNull(response, "Response should not be null");
        assertTrue(response.getOk(),
            "Expected success but got: " + response.getMsg());
    }

    /**
     * Assert that ResponseDTO indicates success with specific data
     */
    protected <T> void assertSuccessWithData(ResponseDTO<T> response, T expectedData) {
        assertSuccess(response);
        assertEquals(expectedData, response.getData());
    }

    /**
     * Assert that ResponseDTO indicates error
     */
    protected void assertError(ResponseDTO<?> response) {
        assertNotNull(response, "Response should not be null");
        assertFalse(response.getOk(), "Expected error but got success");
    }

    /**
     * Assert error with specific message substring
     */
    protected void assertErrorContains(ResponseDTO<?> response, String expectedSubstring) {
        assertError(response);
        assertTrue(response.getMsg().contains(expectedSubstring),
            "Expected message containing '" + expectedSubstring +
            "' but got: " + response.getMsg());
    }

    /**
     * Assert error with specific error code
     */
    protected void assertErrorCode(ResponseDTO<?> response, int expectedCode) {
        assertError(response);
        assertEquals(expectedCode, response.getCode());
    }
}
```

#### BaseManagerTest

Purpose: Common functionality for Manager layer tests.

```java
package net.lab1024.sa.admin.base;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseManagerTest {

    /**
     * Manager layer tests should verify:
     * 1. Correct DAO methods are called in correct order
     * 2. Transaction boundaries (multiple DAO calls should be atomic)
     * 3. Cache invalidation triggers
     */
}
```

#### BaseControllerTest

Purpose: Common functionality for Controller layer tests with MockMvc.

```java
package net.lab1024.sa.admin.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@ActiveProfiles("test")
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Convert object to JSON string
     */
    protected String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    /**
     * Extract response body as string
     */
    protected String getResponseBody(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }
}
```

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
        // ... default values
}
```

### Mock Configurations

Centralize mock configurations for reuse across tests.

```java
package net.lab1024.sa.admin.mock;

import net.lab1024.sa.common.securityprotect.service.SecurityConfigProvider;
import static org.mockito.Mockito.*;

public class MockSecurityConfig {

    /**
     * Default security configuration for most tests
     */
    public static SecurityConfigProvider defaultConfig() {
        SecurityConfigProvider mock = mock(SecurityConfigProvider.class);
        when(mock.getLoginFailMaxTimes()).thenReturn(5);
        when(mock.getLoginFailLockSeconds()).thenReturn(1800L);
        when(mock.isPasswordComplexityEnabled()).thenReturn(true);
        when(mock.getRegularChangePasswordDays()).thenReturn(90);
        when(mock.getRegularChangePasswordNotAllowRepeatTimes()).thenReturn(3);
        return mock;
    }

    /**
     * Security disabled for simpler tests
     */
    public static SecurityConfigProvider securityDisabled() {
        SecurityConfigProvider mock = mock(SecurityConfigProvider.class);
        when(mock.getLoginFailMaxTimes()).thenReturn(0);
        when(mock.isPasswordComplexityEnabled()).thenReturn(false);
        when(mock.getRegularChangePasswordDays()).thenReturn(0);
        return mock;
    }
}
```

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

---

## Best Practices for SmartAdmin Testing

### 1. ResponseDTO Assertions

Always use helper methods from BaseServiceTest:

```java
// Good
assertSuccess(response);
assertError(response);
assertErrorContains(response, "already exists");

// Avoid
assertTrue(response.getOk());  // Less informative on failure
```

### 2. Mock Setup with lenient()

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

### 3. Verify Interactions

For Manager layer, verify the sequence of DAO calls:

```java
@Test
void testUpdateEmployeeRole_DeletesThenInserts() {
    Long employeeId = 1L;

    employeeManager.updateEmployeeRole(employeeId, newRoles);

    InOrder inOrder = inOrder(roleEmployeeDao);
    inOrder.verify(roleEmployeeDao).deleteByEmployeeId(employeeId);
    inOrder.verify(roleEmployeeDao, times(2)).insert(any());
}
```

### 4. Handle SA-Token Static Methods

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

### 5. Time-Based Testing

Avoid flaky time-based assertions:

```java
// Bad - may fail due to timing
assertTrue(lockTime.isBefore(LocalDateTime.now()));

// Good - use fixed time reference
LocalDateTime fixedNow = LocalDateTime.of(2026, 1, 21, 10, 0, 0);
LoginFailEntity locked = SecurityFixture.locked(5, 10); // Locked 10 min ago
assertTrue(locked.getLoginLockBeginTime().plusSeconds(lockSeconds).isBefore(fixedNow));
```

### 6. Deterministic Test Data

Use constants from fixtures, not random values:

```java
// Good - deterministic
EmployeeEntity employee = EmployeeFixture.defaultEmployee().build();
assertEquals(EmployeeFixture.DEFAULT_LOGIN_NAME, employee.getLoginName());

// Avoid - random data
EmployeeEntity employee = new EmployeeEntity();
employee.setLoginName(UUID.randomUUID().toString()); // Flaky
```

### 7. One Assertion Focus

Each test should verify one logical assertion (or closely related assertions):

```java
// Good - focused test
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

### 8. Test Data Cleanup

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

### LoginService with 13 Dependencies

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

## Test Execution Strategy

### Parallel Execution

Configure Gradle for parallel test execution:

```kotlin
// build.gradle.kts
tasks.test {
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
}
```

### Test Filtering

```bash
# Run specific test class
./gradlew :sa-admin:test --tests "LoginServiceTest"

# Run tests matching pattern
./gradlew :sa-admin:test --tests "*Security*"

# Run single test method
./gradlew :sa-admin:test --tests "LoginServiceTest.testLogin_ValidCredentials_ReturnsToken"
```

### CI/CD Integration

```yaml
# .github/workflows/test.yml
- name: Run Unit Tests
  run: ./gradlew :sa-admin:test

- name: Generate Coverage Report
  run: ./gradlew :sa-admin:jacocoTestReport

- name: Verify Coverage Thresholds
  run: ./gradlew :sa-admin:jacocoTestCoverageVerification
```

---

## Anti-Patterns to Avoid

| Anti-Pattern | Problem | Correct Approach |
|--------------|---------|------------------|
| `@SpringBootTest` for unit tests | Slow, 2s+ startup | Use `@ExtendWith(MockitoExtension.class)` |
| Random test data | Flaky tests | Use deterministic fixtures |
| Testing private methods | Fragile tests | Test through public API |
| Multiple assertions per test | Hard to diagnose failures | One logical assertion per test |
| Mocking what you own | Tight coupling | Test real collaborators where possible |
| Time.now() in assertions | Race conditions | Use fixed time references |
| Shared mutable state | Test interference | Reset in @BeforeEach |
| Testing implementation details | Brittle tests | Test behavior, not implementation |

---

## Related Documentation

- [Implementation Plan](./unit-test-implementation-plan.md) - 6-week roadmap
- [Architecture Fixes](./architecture-fixes.md) - Required fixes before testing
- [Test Templates](./test-templates.md) - Code examples
- [Quick Start](./quick-start.md) - Getting started
