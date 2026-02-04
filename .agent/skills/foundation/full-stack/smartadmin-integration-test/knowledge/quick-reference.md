# SmartAdmin Integration Test - Quick Reference

## 基礎測試類別

```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("smartadmin_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

## 測試依賴

```groovy
// build.gradle
testImplementation 'org.testcontainers:testcontainers:1.19.3'
testImplementation 'org.testcontainers:postgresql:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
```

## Service 整合測試

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
    }
}
```

## Controller 整合測試

```java
@AutoConfigureMockMvc
class EmployeeControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Transactional
    void query_ReturnsPageResult() throws Exception {
        // Given
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);

        // When & Then
        mockMvc.perform(post("/admin/employee/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }
}
```

## 常用 Assertions

```java
// ResponseDTO 驗證
assertThat(result.getOk()).isTrue();
assertThat(result.getCode()).isEqualTo(1);
assertThat(result.getData()).isNotNull();

// MockMvc JSON 驗證
.andExpect(jsonPath("$.ok").value(true))
.andExpect(jsonPath("$.data.list").isArray())
.andExpect(jsonPath("$.data.total").isNumber())
```

## 執行命令

```bash
# 運行所有整合測試
./gradlew integrationTest

# 運行特定測試
./gradlew test --tests EmployeeIntegrationTest
```
