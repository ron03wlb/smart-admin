# Integration Testing Quick Reference Card

> **Version**: 1.0 | **Last Updated**: 2026-01-22
> **Print-friendly cheatsheet for integration testing**

---

## Testing Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **Spring Boot Test** | 3.5.4 | Integration test framework |
| **H2 Database** | Latest | Unit tests (in-memory PostgreSQL mode) |
| **Testcontainers** | 1.20.4 | Integration tests (real PostgreSQL) |
| **MockMvc** | 3.5.4 | Controller testing |
| **EmbeddedKafka** | Latest | Kafka integration tests |
| **Testcontainers Redis** | Latest | Cache integration tests |
| **Mockito** | Latest | Mocking framework |

---

## Commands

### Run Integration Tests

```bash
# All tests (unit + integration)
./gradlew :sa-admin:test

# Specific integration test class
./gradlew :sa-admin:test --tests EmployeeControllerIntTest

# With Testcontainers reuse (faster repeated runs)
./gradlew :sa-admin:test -Dtestcontainers.reuse.enable=true

# Integration tests with coverage
./gradlew :sa-admin:test jacocoTestReport
```

### Testcontainers Setup

```bash
# Enable container reuse (add to testcontainers.properties)
testcontainers.reuse.enable=true

# Ensure Docker is running
docker ps

# Windows: Set Docker host environment variable
$env:DOCKER_HOST="npipe:////./pipe/docker_engine"

# Linux/Mac: Set Docker socket
export DOCKER_HOST=unix:///var/run/docker.sock
```

---

## 4 Critical Testing Challenges - Quick Solutions

### Challenge 1: Testing Sa-Token Authentication

**Problem**: Sa-Token uses static methods (StpUtil.getLoginId()) which are hard to mock

**Solution 1: Mock Static Methods (Unit Tests)**

```java
@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Test
    void testUpdatePassword_WithAuth_Success() {
        // Mock Sa-Token static methods
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(() -> StpUtil.getLoginId()).thenReturn(1L);

            // Test code that calls SmartRequestUtil.getRequestUserId()
            ResponseDTO<String> result = employeeService.updatePassword(form);

            assertSuccess(result);
        }
    }
}
```

**Solution 2: Real Authentication (Integration Tests)**

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class EmployeeControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        // Login to get real token
        adminToken = loginAndGetToken("admin", "123456");
    }

    @Test
    @DisplayName("POST /employee/add - with permission - creates employee")
    void testAddEmployee_WithPermission_Success() throws Exception {
        EmployeeAddForm form = new EmployeeAddForm();
        // ... set form fields

        mockMvc.perform(post("/employee/add")
                .header("x-access-token", adminToken)  // Real token
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }

    // Helper method
    private String loginAndGetToken(String username, String password) throws Exception {
        LoginForm loginForm = new LoginForm();
        loginForm.setLoginName(username);
        loginForm.setLoginPwd(password);

        MvcResult result = mockMvc.perform(post("/login")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginForm)))
            .andExpect(status().isOk())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(json);
        return node.get("data").get("token").asText();
    }
}
```

**Testing @SaCheckPermission**:

```java
@Test
@DisplayName("POST /employee/add - without permission - returns 403 or error")
void testAddEmployee_WithoutPermission_Forbidden() throws Exception {
    // Login as user without "system:employee:add" permission
    String userToken = loginAndGetToken("normal_user", "password");

    mockMvc.perform(post("/employee/add")
            .header("x-access-token", userToken)
            .contentType(APPLICATION_JSON)
            .content(json))
        .andExpect(status().is4xxClientError());  // 403 or permission error
}
```

---

### Challenge 2: Testing @Transactional Manager Methods

**Problem**: Need to verify transaction commits or rollbacks correctly

**Solution 1: Test Successful Commit (Multiple DAO Operations)**

```java
@SpringBootTest
@Transactional  // Auto-rollback after each test
class EmployeeManagerTransactionTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @Autowired
    private RoleEmployeeDao roleEmployeeDao;

    @Test
    @DisplayName("saveEmployee - success - commits employee and roles")
    void testSaveEmployee_Success_CommitsBothTables() {
        // Given
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("test_employee");
        employee.setActualName("Test User");
        employee.setDepartmentId(1L);

        List<Long> roleIds = List.of(1L, 2L);

        // When
        employeeManager.saveEmployee(employee, roleIds);

        // Then - verify employee saved
        assertNotNull(employee.getEmployeeId());
        EmployeeEntity saved = employeeDao.selectById(employee.getEmployeeId());
        assertNotNull(saved);
        assertEquals("test_employee", saved.getLoginName());

        // Verify roles saved
        List<RoleEmployeeEntity> roles =
            roleEmployeeDao.selectRoleByEmployeeId(employee.getEmployeeId());
        assertEquals(2, roles.size());
    }
}
```

**Solution 2: Test Rollback on Exception**

```java
@SpringBootTest
@Transactional
class EmployeeManagerTransactionTest {

    @Autowired
    private EmployeeManager employeeManager;

    @Autowired
    private EmployeeDao employeeDao;

    @SpyBean  // Use SpyBean to partially mock
    private RoleEmployeeDao roleEmployeeDao;

    @Test
    @DisplayName("saveEmployee - role insert fails - rollbacks employee")
    void testSaveEmployee_RoleInsertFails_RollbacksEmployee() {
        // Given
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("rollback_test");
        employee.setActualName("Rollback Test");
        employee.setDepartmentId(1L);

        List<Long> roleIds = List.of(1L);

        // Mock roleEmployeeDao to throw exception on insert
        doThrow(new RuntimeException("Simulated DB error"))
            .when(roleEmployeeDao).insert(any(RoleEmployeeEntity.class));

        // When/Then - expect exception
        assertThrows(RuntimeException.class, () -> {
            employeeManager.saveEmployee(employee, roleIds);
        });

        // Verify employee NOT saved due to rollback
        if (employee.getEmployeeId() != null) {
            EmployeeEntity result = employeeDao.selectById(employee.getEmployeeId());
            assertNull(result, "Employee should be rolled back");
        }
    }
}
```

**Solution 3: Verify Operation Order**

```java
@Test
@DisplayName("updateEmployeeRole - deletes old roles then inserts new ones")
void testUpdateEmployeeRole_DeletesThenInserts() {
    // Given
    Long employeeId = 1L;
    List<RoleEmployeeEntity> newRoles = List.of(
        new RoleEmployeeEntity(3L, employeeId),
        new RoleEmployeeEntity(4L, employeeId)
    );

    // When
    employeeManager.updateEmployeeRole(employeeId, newRoles);

    // Then - verify delete called before insert
    InOrder inOrder = inOrder(roleEmployeeDao);
    inOrder.verify(roleEmployeeDao).deleteByEmployeeId(employeeId);
    inOrder.verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
}
```

---

### Challenge 3: Testing @Cached Methods in Manager Layer

**Problem**: Need to verify caching behavior (cache hit/miss, eviction)

**Solution 1: Test Cache Hit/Miss with Redis**

```java
@SpringBootTest
@Testcontainers
class LoginManagerCacheIntTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379)
        .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private LoginManager loginManager;

    @SpyBean  // Spy to count DAO method invocations
    private EmployeeDao employeeDao;

    @Test
    @DisplayName("getRequestEmployee - first call - hits database")
    void testGetRequestEmployee_FirstCall_HitsDatabase() {
        // Given
        Long employeeId = 1L;
        clearInvocations(employeeDao);

        // When - first call (cache miss)
        RequestEmployee result = loginManager.getRequestEmployee(employeeId);

        // Then
        assertNotNull(result);
        verify(employeeDao, times(1)).selectById(employeeId);
    }

    @Test
    @DisplayName("getRequestEmployee - second call - uses cache")
    void testGetRequestEmployee_SecondCall_UsesCache() {
        // Given
        Long employeeId = 1L;

        // First call to warm up cache
        loginManager.getRequestEmployee(employeeId);
        clearInvocations(employeeDao);

        // When - second call (cache hit)
        RequestEmployee result = loginManager.getRequestEmployee(employeeId);

        // Then - DAO not called again
        assertNotNull(result);
        verify(employeeDao, never()).selectById(employeeId);
    }
}
```

**Solution 2: Test @CacheInvalidate**

```java
@Test
@DisplayName("clearUserLoginInfo - invalidates cache")
void testClearUserLoginInfo_InvalidatesCache() {
    // Given
    Long employeeId = 1L;

    // Warm up cache
    loginManager.getRequestEmployee(employeeId);
    clearInvocations(employeeDao);

    // When - clear cache
    loginManager.clearUserLoginInfo(employeeId);

    // Then - next call should hit database again
    loginManager.getRequestEmployee(employeeId);
    verify(employeeDao, times(1)).selectById(employeeId);
}
```

**Solution 3: Test @CacheUpdate**

```java
@Test
@DisplayName("loadLoginInfo - updates cache")
void testLoadLoginInfo_UpdatesCache() {
    // Given
    EmployeeEntity employee = employeeDao.selectById(1L);

    // When - call @CacheUpdate method
    RequestEmployee result = loginManager.loadLoginInfo(employee);
    clearInvocations(employeeDao);

    // Then - subsequent getRequestEmployee should use updated cache
    RequestEmployee cached = loginManager.getRequestEmployee(1L);
    verify(employeeDao, never()).selectById(1L);  // Cache was updated
    assertEquals(result.getEmployeeId(), cached.getEmployeeId());
}
```

---

### Challenge 4: Testing Kafka & Async Operations

**Problem**: Async operations complete after test finishes; need to wait for results

**Solution 1: Use @EmbeddedKafka**

```java
@SpringBootTest
@EmbeddedKafka(
    topics = {"smart-admin-sample", "smart-admin-sample.dlq"},
    partitions = 3
)
class KafkaIntegrationTest {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @Test
    @DisplayName("sendAsync - sends message and consumer processes it")
    void testSendAndReceive() throws Exception {
        // Given
        String topic = "smart-admin-sample";
        String key = "test-key";
        String message = "Hello Integration Test";

        CountDownLatch latch = new CountDownLatch(1);

        // When
        kafkaProducerService.sendAsync(topic, key, message);

        // Then - wait for async processing
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertTrue(received, "Message should be received within 5 seconds");
    }
}
```

**Solution 2: Test Async Methods with CompletableFuture**

```java
@Test
@DisplayName("asyncMethod - completes successfully")
void testAsyncMethod_CompletesSuccessfully() throws Exception {
    // When
    CompletableFuture<String> future = asyncService.processAsync(data);

    // Then - wait for completion
    String result = future.get(3, TimeUnit.SECONDS);
    assertEquals("expected", result);
}
```

**Solution 3: Test Async with @Async Annotation**

```java
@SpringBootTest
@TestConfiguration
class AsyncTestConfig {
    @Bean
    public Executor taskExecutor() {
        return new SyncTaskExecutor();  // Synchronous for testing
    }
}

@Test
void testAsyncMethod_ExecutesSynchronously() {
    // Async method executes synchronously in test
    asyncService.doSomethingAsync();

    // Verify immediately (no need to wait)
    verify(mockDao).insert(any());
}
```

---

## Test Annotation Patterns

### Controller Integration Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional  // Auto-rollback after each test
class EmployeeControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Tests here
}
```

### Controller Unit Tests (Faster)

```java
@WebMvcTest(EmployeeController.class)
@Import({SecurityConfig.class, JacksonConfig.class})
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmployeeService employeeService;

    // Tests here
}
```

### Service Integration Tests (with Database)

```java
@SpringBootTest
@Transactional
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Testcontainers
class EmployeeServiceIntTest {

    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EmployeeService employeeService;

    // Tests here
}
```

### Manager Integration Tests (with Cache)

```java
@SpringBootTest
@Testcontainers
class LoginManagerIntTest {

    @Container
    static GenericContainer<?> redis =
        new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379)
            .withReuse(true);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private LoginManager loginManager;

    @SpyBean
    private EmployeeDao employeeDao;

    // Tests here
}
```

---

## Test Data Management

### Fixture Pattern

```java
public class EmployeeTestFixture {

    public static EmployeeEntity defaultEmployee() {
        EmployeeEntity employee = new EmployeeEntity();
        employee.setLoginName("test_employee");
        employee.setActualName("Test User");
        employee.setPhone("13800138000");
        employee.setDepartmentId(1L);
        employee.setDisabledFlag(false);
        employee.setDeletedFlag(false);
        employee.setAdministratorFlag(false);
        return employee;
    }

    public static EmployeeAddForm defaultAddForm() {
        EmployeeAddForm form = new EmployeeAddForm();
        form.setLoginName("new_employee");
        form.setActualName("New User");
        form.setPhone("13900139000");
        form.setDepartmentId(1L);
        form.setRoleIdList(List.of(1L));
        return form;
    }
}
```

### Database Seeding with @Sql

```java
@SpringBootTest
@Transactional
@Sql(scripts = "/test-data/employees-setup.sql",
     executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/test-data/cleanup.sql",
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class EmployeeServiceIntTest {

    @Test
    void testQueryEmployee_WithExistingData_ReturnsResults() {
        // Test data already loaded from employees-setup.sql
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageSize(10);
        form.setPageNum(1);

        PageResult<EmployeeVO> result = employeeService.queryEmployee(form);

        assertTrue(result.getTotal() > 0);
    }
}
```

**Example SQL Script** (`test-data/employees-setup.sql`):

```sql
-- Insert test department
INSERT INTO t_department (department_id, department_name, deleted_flag)
VALUES (999, 'Test Department', 0);

-- Insert test employees
INSERT INTO t_employee (employee_id, login_name, actual_name, department_id, disabled_flag, deleted_flag)
VALUES
(1001, 'test_user1', 'Test User 1', 999, 0, 0),
(1002, 'test_user2', 'Test User 2', 999, 0, 0);
```

---

## Common Patterns

### Pattern: Login Helper

```java
protected String loginAndGetToken(String username, String password) throws Exception {
    LoginForm form = new LoginForm();
    form.setLoginName(username);
    form.setLoginPwd(password);

    MvcResult result = mockMvc.perform(post("/login")
            .contentType(APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(form)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.ok").value(true))
        .andReturn();

    String json = result.getResponse().getContentAsString();
    JsonNode rootNode = objectMapper.readTree(json);
    return rootNode.get("data").get("token").asText();
}
```

### Pattern: Testcontainers Reuse

**File**: `src/test/resources/testcontainers.properties`

```properties
testcontainers.reuse.enable=true
```

**In Test**:

```java
@Container
static PostgreSQLContainer<?> postgres =
    new PostgreSQLContainer<>("postgres:16-alpine")
        .withReuse(true)  // Reuse container across test runs
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
```

### Pattern: ResponseDTO Assertions

```java
// Assert success
ResponseDTO<String> response = employeeService.addEmployee(form);
assertTrue(response.getOk());
assertNotNull(response.getData());

// Assert error
ResponseDTO<String> response = employeeService.addEmployee(invalidForm);
assertFalse(response.getOk());
assertEquals(UserErrorCode.PARAM_ERROR.getCode(), response.getCode());
```

---

## Troubleshooting

### Issue: Tests fail with "Connection refused" (Docker)

**Cause**: Testcontainers cannot connect to Docker daemon

**Solution**:

```bash
# Windows
$env:DOCKER_HOST="npipe:////./pipe/docker_engine"

# Linux/Mac
export DOCKER_HOST=unix:///var/run/docker.sock

# Verify Docker is running
docker ps
```

### Issue: Sa-Token "Not logged in" error

**Cause**: StpUtil not properly mocked or token not set

**Solution 1: Unit Tests**

```java
try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
    stpUtil.when(() -> StpUtil.getLoginId()).thenReturn(1L);
    stpUtil.when(() -> StpUtil.getTokenValue()).thenReturn("mock-token");
    // Test code
}
```

**Solution 2: Integration Tests**

```java
// Use real login to get token
String token = loginAndGetToken("admin", "123456");

// Include token in request
mockMvc.perform(post("/employee/add")
    .header("x-access-token", token)  // Sa-Token reads from this header
    .content(json))
```

### Issue: Cache not invalidating in tests

**Cause**: Redis container not running or wrong configuration

**Solution**:

```java
@Testcontainers
class CacheIntTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Ensure properties match application configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }
}
```

### Issue: Flaky Kafka tests

**Cause**: Race condition - test completes before message processed

**Solution**: Use CountDownLatch or Awaitility

```java
@Test
void testKafkaMessage() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);

    // Send message
    kafkaProducerService.sendAsync(topic, key, message);

    // Wait with timeout
    boolean received = latch.await(10, TimeUnit.SECONDS);
    assertTrue(received, "Should receive message within timeout");
}
```

---

## Quick Links

| Topic | Document |
|-------|----------|
| **Sa-Token Testing** | [integration/sa-token-testing.md](./integration/sa-token-testing.md) |
| **Transaction Testing** | [integration/transaction-testing.md](./integration/transaction-testing.md) |
| **Caching Testing** | [integration/caching-testing.md](./integration/caching-testing.md) |
| **Kafka & Async Testing** | [integration/kafka-async-testing.md](./integration/kafka-async-testing.md) |
| **Unit Testing Strategy** | [testing-strategy.md](./testing-strategy.md) |
| **Architecture Rules** | [architecture/overview.md](./architecture/overview.md) |
| **Project Conventions** | [../../CLAUDE.md](../../CLAUDE.md) |

---

**Print this page for quick reference during integration test development!**
