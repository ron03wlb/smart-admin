# Sa-Token Authentication Testing Guide

> **Challenge #1**: Testing authentication and permissions in SmartAdmin
> **Last Updated**: 2026-01-22

---

## Table of Contents

1. [Introduction](#introduction)
2. [The Sa-Token Challenge](#the-sa-token-challenge)
3. [Unit Test Strategy](#unit-test-strategy)
4. [Integration Test Strategy](#integration-test-strategy)
5. [Real SmartAdmin Examples](#real-smartadmin-examples)
6. [Common Issues and Solutions](#common-issues-and-solutions)

---

## Introduction

SmartAdmin uses **Sa-Token** for authentication and authorization. Sa-Token provides powerful features through static methods, but this creates challenges for testing:

- `StpUtil.getLoginId()` - Get current user ID
- `StpUtil.getTokenValue()` - Get current token
- `StpUtil.checkPermission(String)` - Check permission
- `@SaCheckPermission("perm")` - Controller method annotation

This guide shows how to effectively test code that depends on Sa-Token authentication.

---

## The Sa-Token Challenge

### Problem

Sa-Token uses static methods which are difficult to test:

```java
@Service
public class EmployeeService {

    public ResponseDTO<String> updatePassword(EmployeeUpdatePasswordForm form) {
        // This calls StpUtil.getLoginId() internally
        Long employeeId = SmartRequestUtil.getRequestUserId();

        // ... business logic
    }
}
```

**Testing challenges:**
1. Static methods can't be mocked with traditional `@Mock`
2. Need to simulate authenticated vs unauthenticated states
3. Permission checks (`@SaCheckPermission`) need real token or mock
4. Integration tests need real login flow

### SmartAdmin Authentication Flow

```
1. User → POST /login (username, password)
2. LoginService validates credentials
3. StpUtil.login(employeeId) creates session
4. Return LoginResultVO with token
5. Client sends token in "x-access-token" header
6. Sa-Token interceptor validates token
7. Controller method executes with authentication context
```

---

## Unit Test Strategy

### Option 1: MockedStatic (Recommended for Unit Tests)

Use Mockito's `MockedStatic` to mock Sa-Token's static methods:

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeService employeeService;

    @Mock
    private EmployeeDao employeeDao;

    @Mock
    private EmployeeManager employeeManager;

    @Test
    void testUpdatePassword_ValidOldPassword_Success() {
        // Mock Sa-Token static methods
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            // Setup: mock getLoginId to return test user ID
            stpUtil.when(() -> StpUtil.getLoginId()).thenReturn(1L);

            // Given
            EmployeeUpdatePasswordForm form = new EmployeeUpdatePasswordForm();
            form.setOldPassword("old123");
            form.setNewPassword("new456");

            EmployeeEntity employee = new EmployeeEntity();
            employee.setEmployeeId(1L);
            employee.setLoginPwd("encrypted_old123");

            when(employeeDao.selectById(1L)).thenReturn(employee);

            // When
            ResponseDTO<String> result = employeeService.updatePassword(form);

            // Then
            assertTrue(result.getOk());
            verify(employeeManager).updatePassword(eq(1L), anyString(), any());
        }
    }
}
```

### Option 2: Mock SmartRequestUtil (Alternative)

If `SmartRequestUtil` is not final, you can mock it directly:

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void testUpdateCenter_UpdatesCurrentUser() {
        try (MockedStatic<SmartRequestUtil> requestUtil = mockStatic(SmartRequestUtil.class)) {
            // Mock current user ID
            requestUtil.when(() -> SmartRequestUtil.getRequestUserId()).thenReturn(5L);

            // Test code that uses SmartRequestUtil.getRequestUserId()
            EmployeeUpdateCenterForm form = new EmployeeUpdateCenterForm();
            form.setActualName("New Name");

            ResponseDTO<String> result = employeeService.updateCenter(form);

            // Verify
            assertTrue(result.getOk());
            verify(employeeDao).updateById(argThat(e -> e.getEmployeeId().equals(5L)));
        }
    }
}
```

### Testing Permission Checks

```java
@Test
void testCheckUserPermission_HasPermission_Success() {
    try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
        // Mock permission check to return true
        stpUtil.when(() -> StpUtil.hasPermission("system:employee:add"))
               .thenReturn(true);

        // When
        boolean hasPermission = StpUtil.hasPermission("system:employee:add");

        // Then
        assertTrue(hasPermission);
    }
}
```

---

## Integration Test Strategy

For integration tests, use **real authentication** flow instead of mocking.

### Step 1: Setup Integration Test Base Class

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
public abstract class BaseControllerIntTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Login and get authentication token
     */
    protected String loginAndGetToken(String username, String password) throws Exception {
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginName(username);
        loginForm.setLoginPwd(password);

        MvcResult result = mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginForm)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        JsonNode rootNode = objectMapper.readTree(responseBody);
        return rootNode.get("data").get("token").asText();
    }

    /**
     * Login as admin user (has all permissions)
     */
    protected String loginAsAdmin() throws Exception {
        return loginAndGetToken("admin", "123456");
    }
}
```

### Step 2: Test with Real Authentication

```java
class EmployeeControllerIntTest extends BaseControllerIntTest {

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login as admin (has system:employee:add permission)
        adminToken = loginAsAdmin();

        // Login as normal user (no special permissions)
        userToken = loginAndGetToken("normal_user", "password");
    }

    @Test
    @DisplayName("POST /employee/add - with permission - creates employee")
    void testAddEmployee_WithPermission_Success() throws Exception {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("new_employee");
        form.setActualName("New Employee");
        form.setPhone("13800138000");
        form.setDepartmentId(1L);
        form.setRoleIdList(List.of(1L));

        // When/Then
        mockMvc.perform(post("/employee/add")
                .header("x-access-token", adminToken)  // Admin has permission
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data").isNotEmpty());
    }

    @Test
    @DisplayName("POST /employee/add - without permission - returns error")
    void testAddEmployee_WithoutPermission_Error() throws Exception {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("new_employee");
        form.setActualName("New Employee");
        form.setDepartmentId(1L);

        // When/Then - normal user doesn't have system:employee:add permission
        mockMvc.perform(post("/employee/add")
                .header("x-access-token", userToken)  // No permission
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().is4xxClientError());  // 403 or permission error
    }

    @Test
    @DisplayName("POST /employee/add - no token - unauthorized")
    void testAddEmployee_NoToken_Unauthorized() throws Exception {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("new_employee");

        // When/Then - no token provided
        mockMvc.perform(post("/employee/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().is4xxClientError());  // 401 unauthorized
    }
}
```

### Step 3: Testing @NoNeedLogin Endpoints

```java
@Test
@DisplayName("POST /login - no authentication needed")
void testLogin_NoToken_Success() throws Exception {
    // Given
    LoginForm form = new LoginForm();
    form.setLoginName("admin");
    form.setLoginPwd("123456");

    // When/Then - @NoNeedLogin annotation allows access without token
    mockMvc.perform(post("/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.data.token").isNotEmpty());
}
```

---

## Real SmartAdmin Examples

### Example 1: Testing EmployeeController.addEmployee()

**Method Signature:**

```java
@PostMapping("/employee/add")
@SaCheckPermission("system:employee:add")
public ResponseDTO<String> addEmployee(@Valid @RequestBody EmployeeAddForm form) {
    return employeeService.addEmployee(form);
}
```

**Integration Test:**

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional
class EmployeeControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeDao employeeDao;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = loginAndGetToken("admin", "123456");
    }

    @Test
    @DisplayName("Add employee with valid data and permission")
    void testAddEmployee_ValidData_Success() throws Exception {
        // Given
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("test_emp_" + System.currentTimeMillis());
        form.setActualName("Test Employee");
        form.setPhone("13800138000");
        form.setDepartmentId(1L);
        form.setRoleIdList(List.of(1L));

        // When
        MvcResult result = mockMvc.perform(post("/employee/add")
                .header("x-access-token", adminToken)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true))
            .andReturn();

        // Then - verify employee was created in database
        String responseJson = result.getResponse().getContentAsString();
        // Password is returned as data
        assertNotNull(responseJson);

        // Verify in database
        EmployeeEntity created = employeeDao.selectByLoginName(form.getLoginName());
        assertNotNull(created);
        assertEquals(form.getActualName(), created.getActualName());
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginName(username);
        loginForm.setLoginPwd(password);

        MvcResult result = mockMvc.perform(post("/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginForm)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("data").get("token").asText();
    }
}
```

### Example 2: Testing EmployeeController.updateCenter()

**Method Signature:**

```java
@PostMapping("/employee/update/center")
public ResponseDTO<String> updateCenter(@Valid @RequestBody EmployeeUpdateCenterForm form) {
    form.setEmployeeId(SmartRequestUtil.getRequestUserId());  // Uses Sa-Token
    return employeeService.updateCenter(form);
}
```

**Unit Test:**

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @InjectMocks
    private EmployeeService employeeService;

    @Mock
    private EmployeeDao employeeDao;

    @Test
    void testUpdateCenter_UsesCurrentUserId() {
        try (MockedStatic<SmartRequestUtil> requestUtil = mockStatic(SmartRequestUtil.class)) {
            // Mock current user ID
            requestUtil.when(() -> SmartRequestUtil.getRequestUserId()).thenReturn(10L);

            // Given
            EmployeeUpdateCenterForm form = new EmployeeUpdateCenterForm();
            form.setActualName("Updated Name");
            form.setPhone("13900139000");

            EmployeeEntity existingEmployee = new EmployeeEntity();
            existingEmployee.setEmployeeId(10L);
            existingEmployee.setActualName("Old Name");

            when(employeeDao.selectById(10L)).thenReturn(existingEmployee);
            when(employeeDao.updateById(any())).thenReturn(1);

            // When
            ResponseDTO<String> result = employeeService.updateCenter(form);

            // Then
            assertTrue(result.getOk());
            verify(employeeDao).updateById(argThat(emp ->
                emp.getEmployeeId().equals(10L) &&
                emp.getActualName().equals("Updated Name")
            ));
        }
    }
}
```

**Integration Test:**

```java
@Test
@DisplayName("Update center - uses logged-in user ID")
void testUpdateCenter_UsesLoggedInUser() throws Exception {
    // Given - login as specific user
    String userToken = loginAndGetToken("test_user", "password");

    EmployeeUpdateCenterForm form = new EmployeeUpdateCenterForm();
    form.setActualName("New Name");
    form.setPhone("13900139000");

    // When/Then
    mockMvc.perform(post("/employee/update/center")
            .header("x-access-token", userToken)
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true));

    // Verify - the logged-in user's data was updated
    // (not some other user)
}
```

### Example 3: Testing LoginController.getLoginInfo()

**Method Signature:**

```java
@GetMapping("/login/getLoginInfo")
public ResponseDTO<LoginResultVO> getLoginInfo() {
    String tokenValue = StpUtil.getTokenValue();
    LoginResultVO loginResult = loginService.getLoginResult(
        AdminRequestUtil.getRequestUser(), tokenValue);
    loginResult.setToken(tokenValue);
    return ResponseDTO.ok(loginResult);
}
```

**Integration Test:**

```java
@Test
@DisplayName("GET /login/getLoginInfo - returns current user info")
void testGetLoginInfo_ReturnsCurrentUser() throws Exception {
    // Given - login first
    String token = loginAndGetToken("admin", "123456");

    // When
    MvcResult result = mockMvc.perform(get("/login/getLoginInfo")
            .header("x-access-token", token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andExpect(jsonPath("$.data.token").value(token))
        .andReturn();

    // Then - verify response contains user info
    String json = result.getResponse().getContentAsString();
    JsonNode data = objectMapper.readTree(json).get("data");

    assertNotNull(data.get("employeeId"));
    assertNotNull(data.get("employeeName"));
    assertEquals("admin", data.get("loginName").asText());
}
```

---

## Common Issues and Solutions

### Issue 1: "NotLoginException: Not logged in"

**Cause**: Sa-Token session not established or token not provided

**Solution for Unit Tests:**

```java
try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
    stpUtil.when(() -> StpUtil.getLoginId()).thenReturn(1L);
    stpUtil.when(() -> StpUtil.checkLogin()).thenReturn(true);
    // Test code
}
```

**Solution for Integration Tests:**

```java
// Always include token in requests
mockMvc.perform(post("/some/endpoint")
    .header("x-access-token", token)  // Don't forget this!
    .content(json))
```

### Issue 2: "NotPermissionException: No permission"

**Cause**: User doesn't have required permission for `@SaCheckPermission`

**Solution:**

```java
// Login as user WITH the required permission
String adminToken = loginAsAdmin();  // Admin has all permissions

// Or create test user with specific permissions in test setup
createUserWithPermissions("test_user", List.of("system:employee:add"));
```

### Issue 3: MockedStatic Not Cleaning Up

**Cause**: `MockedStatic` not closed after test

**Solution: Always use try-with-resources:**

```java
@Test
void testMethod() {
    // ✅ Correct - auto-closes
    try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
        // Test code
    }

    // ❌ Wrong - must manually close
    MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class);
    // Test code
    stpUtil.close();  // Easy to forget!
}
```

### Issue 4: AdminRequestUtil vs SmartRequestUtil

**Problem**: Which one should I mock?

**Answer:**
- `SmartRequestUtil.getRequestUserId()` - Returns `Long` user ID
- `AdminRequestUtil.getRequestUser()` - Returns `RequestEmployee` object

Mock based on what the code uses:

```java
// Code uses SmartRequestUtil.getRequestUserId()
try (MockedStatic<SmartRequestUtil> util = mockStatic(SmartRequestUtil.class)) {
    util.when(() -> SmartRequestUtil.getRequestUserId()).thenReturn(1L);
}

// Code uses AdminRequestUtil.getRequestUser()
try (MockedStatic<AdminRequestUtil> util = mockStatic(AdminRequestUtil.class)) {
    RequestEmployee requestUser = new RequestEmployee();
    requestUser.setEmployeeId(1L);
    util.when(() -> AdminRequestUtil.getRequestUser()).thenReturn(requestUser);
}
```

### Issue 5: Token Header Name

**Problem**: Token not recognized - which header to use?

**Answer**: SmartAdmin uses `x-access-token` (configured in Sa-Token):

```java
mockMvc.perform(post("/endpoint")
    .header("x-access-token", token))  // ✅ Correct

mockMvc.perform(post("/endpoint")
    .header("Authorization", "Bearer " + token))  // ❌ Wrong for SmartAdmin
```

---

## Best Practices

### 1. Use Real Login for Integration Tests

```java
// ✅ Recommended
@BeforeEach
void setUp() throws Exception {
    adminToken = loginAndGetToken("admin", "123456");
}

// ❌ Avoid - brittle
@BeforeEach
void setUp() {
    adminToken = "hardcoded-token-12345";  // Won't work
}
```

### 2. Create Test Users with Specific Permissions

```java
@Sql(scripts = "/test-data/users-with-permissions.sql")
class PermissionIntTest {
    // Test data loaded with specific permission grants
}
```

### 3. Test Both Authorized and Unauthorized Scenarios

```java
@Test
void testEndpoint_WithPermission_Success() { }

@Test
void testEndpoint_WithoutPermission_Forbidden() { }

@Test
void testEndpoint_NotLoggedIn_Unauthorized() { }
```

### 4. Verify Permission Checks

```java
@Test
@DisplayName("Endpoint requires specific permission")
void testEndpoint_VerifyPermissionRequired() throws Exception {
    // User without permission should get 403
    String userToken = loginAsNormalUser();

    mockMvc.perform(post("/admin/sensitive-operation")
            .header("x-access-token", userToken))
        .andExpect(status().isForbidden());
}
```

---

## Summary

| Testing Scenario | Strategy | Tool |
|------------------|----------|------|
| **Unit Test - Service Layer** | Mock static methods | `MockedStatic<StpUtil>` |
| **Unit Test - Permission Check** | Mock permission result | `MockedStatic<StpUtil>` |
| **Integration - Controller** | Real login flow | `MockMvc` + real token |
| **Integration - @SaCheckPermission** | Real user with permission | Login + real token |
| **Integration - @NoNeedLogin** | No token needed | `MockMvc` without header |

**Key Takeaway**: Use **MockedStatic for unit tests**, **real authentication for integration tests**.

---

## Related Documentation

- [Integration Testing Quick Reference](../integration-testing-quick-reference.md) - Quick patterns
- [Testing Strategy](../testing-strategy.md) - Overall testing philosophy
- [Controller Integration Testing](./controller-testing.md) - More controller patterns (coming soon)

---

**Happy Testing! 🧪**
