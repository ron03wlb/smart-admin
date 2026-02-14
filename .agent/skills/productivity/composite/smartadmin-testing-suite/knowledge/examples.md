# SmartAdmin Testing Suite - Examples

## 範例 1: Service 層單元測試

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
    void getById_WhenExists_ReturnsOption() {
        // Given
        Long employeeId = 1L;
        EmployeeEntity entity = EmployeeTestFixture.createEntity();
        entity.setEmployeeId(employeeId);
        when(employeeDao.selectById(employeeId)).thenReturn(entity);

        // When
        Option<EmployeeVO> result = employeeService.getById(employeeId);

        // Then
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().getEmployeeId()).isEqualTo(employeeId);
    }

    @Test
    void getById_WhenNotExists_ReturnsNone() {
        // Given
        Long employeeId = 999L;
        when(employeeDao.selectById(employeeId)).thenReturn(null);

        // When
        Option<EmployeeVO> result = employeeService.getById(employeeId);

        // Then
        assertThat(result.isEmpty()).isTrue();
    }
}
```

## 範例 2: 整合測試 (Testcontainers)

```java
class EmployeeIntegrationTest extends BaseIntegrationTest {

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
    void add_DuplicateEmail_ReturnsError() {
        // Given
        EmployeeEntity existing = EmployeeTestFixture.createEntity();
        employeeDao.insert(existing);

        EmployeeAddForm form = new EmployeeAddForm();
        form.setEmail(existing.getEmail());  // 重複 email

        // When
        ResponseDTO<String> result = employeeService.add(form);

        // Then
        assertThat(result.getOk()).isFalse();
        assertThat(result.getMsg()).contains("已存在");
    }
}
```

## 範例 3: Controller 整合測試

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
            .andExpect(jsonPath("$.ok").value(true))
            .andExpect(jsonPath("$.data.list").isArray());
    }

    @Test
    @Transactional
    void add_ValidForm_Returns200() throws Exception {
        // Given
        EmployeeAddForm form = EmployeeTestFixture.createAddForm();

        // When & Then
        mockMvc.perform(post("/admin/employee/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ok").value(true));
    }
}
```

## 範例 4: 測試數據建構器

```java
public class EmployeeTestFixture {

    private static final AtomicInteger COUNTER = new AtomicInteger(0);

    // 基本實體
    public static EmployeeEntity createEntity() {
        int id = COUNTER.incrementAndGet();
        EmployeeEntity entity = new EmployeeEntity();
        entity.setFirstName("Test" + id);
        entity.setLastName("Employee" + id);
        entity.setEmail("test" + id + "@example.com");
        entity.setDepartmentId(1L);
        entity.setDeleted(false);
        entity.setCreateTime(LocalDateTime.now());
        return entity;
    }

    // 帶部門的實體
    public static EmployeeEntity createEntityWithDepartment(Long departmentId) {
        EmployeeEntity entity = createEntity();
        entity.setDepartmentId(departmentId);
        return entity;
    }

    // 新增表單
    public static EmployeeAddForm createAddForm() {
        int id = COUNTER.incrementAndGet();
        EmployeeAddForm form = new EmployeeAddForm();
        form.setFirstName("Test" + id);
        form.setLastName("Employee" + id);
        form.setEmail("test" + id + "@example.com");
        form.setDepartmentId(1L);
        return form;
    }

    // 更新表單
    public static EmployeeUpdateForm createUpdateForm(Long employeeId) {
        EmployeeUpdateForm form = new EmployeeUpdateForm();
        form.setEmployeeId(employeeId);
        form.setFirstName("Updated");
        form.setLastName("Name");
        return form;
    }

    // 查詢表單
    public static EmployeeQueryForm createQueryForm() {
        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(10);
        return form;
    }
}
```

## 常見問題

### Q1: Testcontainers 啟動失敗

**錯誤:** `Could not find a valid Docker environment`

**解決:**
1. 確保 Docker Desktop 已啟動
2. 確保至少 4GB 記憶體分配給 Docker

### Q2: 測試不回滾

**錯誤:** 測試後資料庫有殘留數據

**解決:**
```java
@Test
@Transactional  // ← 確保添加此註解
void myTest() { ... }
```
