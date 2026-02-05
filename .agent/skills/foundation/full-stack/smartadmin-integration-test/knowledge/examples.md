# SmartAdmin Integration Test - Examples

## 範例 1: 完整 CRUD 測試

```java
class EmployeeServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeDao employeeDao;

    @Test
    @Transactional
    void add_ValidForm_CreatesEmployee() {
        // Given
        EmployeeAddForm form = EmployeeTestFixture.createAddForm();

        // When
        ResponseDTO<String> result = employeeService.add(form);

        // Then
        assertThat(result.getOk()).isTrue();
        EmployeeEntity saved = employeeDao.getByEmail(form.getEmail());
        assertThat(saved).isNotNull();
        assertThat(saved.getFirstName()).isEqualTo(form.getFirstName());
    }

    @Test
    @Transactional
    void update_ExistingEmployee_UpdatesSuccessfully() {
        // Given
        EmployeeEntity entity = EmployeeTestFixture.createEntity();
        employeeDao.insert(entity);

        EmployeeUpdateForm form = new EmployeeUpdateForm();
        form.setEmployeeId(entity.getEmployeeId());
        form.setFirstName("Updated");

        // When
        ResponseDTO<String> result = employeeService.update(form);

        // Then
        assertThat(result.getOk()).isTrue();
        EmployeeEntity updated = employeeDao.selectById(entity.getEmployeeId());
        assertThat(updated.getFirstName()).isEqualTo("Updated");
    }

    @Test
    @Transactional
    void delete_ExistingEmployee_SoftDeletes() {
        // Given
        EmployeeEntity entity = EmployeeTestFixture.createEntity();
        employeeDao.insert(entity);

        // When
        ResponseDTO<String> result = employeeService.delete(entity.getEmployeeId());

        // Then
        assertThat(result.getOk()).isTrue();
        EmployeeEntity deleted = employeeDao.selectById(entity.getEmployeeId());
        assertThat(deleted.getDeleted()).isTrue();
    }

    @Test
    @Transactional
    void query_WithPagination_ReturnsPageResult() {
        // Given
        for (int i = 0; i < 15; i++) {
            employeeDao.insert(EmployeeTestFixture.createEntity());
        }

        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);

        // When
        ResponseDTO<PageResult<EmployeeVO>> result = employeeService.query(form);

        // Then
        assertThat(result.getOk()).isTrue();
        assertThat(result.getData().getList()).hasSize(10);
        assertThat(result.getData().getTotal()).isEqualTo(15);
    }
}
```

## 範例 2: API Controller 測試

```java
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeDao employeeDao;

    @Test
    @Transactional
    void add_ValidRequest_Returns200() throws Exception {
        // Given
        EmployeeAddForm form = EmployeeTestFixture.createAddForm();

        // When & Then
        mockMvc.perform(post("/admin/employee/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));

        // Verify in database
        EmployeeEntity saved = employeeDao.getByEmail(form.getEmail());
        assertThat(saved).isNotNull();
    }

    @Test
    @Transactional
    void add_DuplicateEmail_ReturnsError() throws Exception {
        // Given - existing employee
        EmployeeEntity existing = EmployeeTestFixture.createEntity();
        employeeDao.insert(existing);

        EmployeeAddForm form = new EmployeeAddForm();
        form.setEmail(existing.getEmail());  // duplicate

        // When & Then
        mockMvc.perform(post("/admin/employee/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(false))
            .andExpect(jsonPath("$.msg").value(containsString("已存在")));
    }
}
```

## 範例 3: Redis 快取測試

```java
@SpringBootTest
@Testcontainers
class CacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", redis::getFirstMappedPort);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void cache_SetAndGet_Works() {
        // Given
        String key = "test:key";
        String value = "test-value";

        // When
        redisTemplate.opsForValue().set(key, value);

        // Then
        assertThat(redisTemplate.opsForValue().get(key)).isEqualTo(value);
    }
}
```
