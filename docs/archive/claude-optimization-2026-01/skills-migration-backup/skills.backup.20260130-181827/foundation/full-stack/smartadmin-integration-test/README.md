# SmartAdmin Integration Test

> Spring Boot 整合測試：Testcontainers 自動化 + 真實數據庫 + 完整 Spring 上下文

## 🚀 快速開始

```bash
# 場景：需要測試 EmployeeService 的業務邏輯
User: "Create integration test for EmployeeService"

# 自動生成：
# 1. 整合測試類（EmployeeServiceIntegrationTest.java）
# 2. Testcontainers 配置（PostgreSQL + Redis）
# 3. 測試數據初始化（@Sql 腳本）
# 4. 測試清理邏輯（@Transactional）

# 時間：約 5 分鐘
```

## 核心功能

### 1. Testcontainers 自動化

**自動啟動真實數據庫容器**：

```java
@SpringBootTest
@Testcontainers
class EmployeeServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("smartadmin_test")
        .withUsername("test")
        .withPassword("test")
        .withReuse(true);  // 容器重用，加快測試速度

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7")
        .withExposedPorts(6379)
        .withReuse(true);

    @Autowired
    private EmployeeService employeeService;

    @Test
    void testAddEmployee() {
        // 使用真實數據庫測試
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName("張三");
        form.setDepartmentId(1L);

        ResponseDTO<Long> response = employeeService.add(form);

        assertThat(response.isOk()).isTrue();
        assertThat(response.getData()).isNotNull();
    }
}
```

---

### 2. Spring Boot 測試配置

**完整 Spring 上下文**：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EmployeeControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testAddEmployeeApi() {
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName("李四");

        ResponseEntity<ResponseDTO> response = restTemplate.postForEntity(
            "/api/employee/add",
            form,
            ResponseDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isOk()).isTrue();
    }
}
```

---

### 3. 測試數據準備和清理

**使用 @Sql 初始化數據**：

```java
@SpringBootTest
@Testcontainers
@Sql("/test-data/employee-setup.sql")  // 測試前執行
@Sql(scripts = "/test-data/employee-cleanup.sql",
     executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)  // 測試後清理
class EmployeeServiceIntegrationTest {
    @Test
    void testGetDetail() {
        // 數據已由 setup.sql 初始化
        EmployeeVO vo = employeeService.getDetail(1L);
        assertThat(vo).isNotNull();
    }
}
```

**或使用 @Transactional 自動回滾**：

```java
@SpringBootTest
@Testcontainers
@Transactional  // 測試方法結束後自動回滾，無需手動清理
class EmployeeServiceIntegrationTest {
    @Test
    void testAddEmployee() {
        // 測試邏輯
        // 測試結束後數據自動回滾
    }
}
```

---

## 測試層級

### Controller 層測試

**測試 REST API**：

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class EmployeeControllerIntegrationTest {
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testAddEmployee() {
        EmployeeAddForm form = new EmployeeAddForm();
        ResponseEntity<ResponseDTO> response = restTemplate.postForEntity(
            "/api/employee/add", form, ResponseDTO.class
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
```

---

### Service 層測試

**測試業務邏輯（Option 返回值）**：

```java
@SpringBootTest
@Testcontainers
class EmployeeServiceIntegrationTest {
    @Autowired
    private EmployeeService employeeService;

    @Test
    void testGetDetail_found() {
        Option<EmployeeVO> result = employeeService.getDetail(1L);
        assertThat(result.isDefined()).isTrue();
    }

    @Test
    void testGetDetail_notFound() {
        Option<EmployeeVO> result = employeeService.getDetail(999L);
        assertThat(result.isEmpty()).isTrue();
    }
}
```

---

### Manager 層測試

**測試事務管理**：

```java
@SpringBootTest
@Testcontainers
class EmployeeManagerIntegrationTest {
    @Autowired
    private EmployeeManager employeeManager;

    @Test
    void testUpdateEmployee_rollbackOnException() {
        EmployeeUpdateForm form = new EmployeeUpdateForm();
        form.setName(null);  // 觸發異常

        assertThatThrownBy(() -> employeeManager.updateEmployee(1L, form))
            .isInstanceOf(BusinessException.class);

        // 驗證事務回滾
        EmployeeEntity entity = employeeDao.selectById(1L);
        assertThat(entity.getName()).isEqualTo("原始名稱");
    }
}
```

---

### Dao 層測試

**測試 SQL 查詢**：

```java
@SpringBootTest
@Testcontainers
class EmployeeDaoIntegrationTest {
    @Autowired
    private EmployeeDao employeeDao;

    @Test
    void testSelectByDepartmentId() {
        List<EmployeeEntity> employees = employeeDao.selectList(
            new LambdaQueryWrapper<EmployeeEntity>()
                .eq(EmployeeEntity::getDepartmentId, 1L)
        );
        assertThat(employees).isNotEmpty();
    }
}
```

---

## 使用場景

### ✅ When to Use

1. **Controller 層 API 測試**
   - 測試 HTTP 請求/響應
   - 驗證 ResponseDTO 格式
   - 測試權限驗證

2. **Service 層業務邏輯測試**
   - 測試複雜業務流程
   - 驗證 Option 返回值處理
   - 測試異常處理

3. **Manager 層事務測試**
   - 驗證 `@Transactional` 回滾
   - 測試多表操作
   - 測試併發安全

4. **Dao 層 SQL 測試**
   - 驗證 MyBatis-Plus 查詢
   - 測試複雜 JOIN 查詢
   - 測試索引效果

### ❌ When NOT to Use

1. **純邏輯單元測試**
   - 使用 Mockito 單元測試更快

2. **不依賴數據庫的邏輯**
   - 使用普通 JUnit 測試

---

## 常見問題

### Q1: Testcontainers 啟動慢怎麼辦？

**A**: 啟用容器重用：

```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
    .withReuse(true);  // 啟用重用
```

**配置** `~/.testcontainers.properties`：
```properties
testcontainers.reuse.enable=true
```

**效果**：首次 30 秒，後續 < 2 秒

---

### Q2: 如何測試事務回滾？

**A**: 見 Manager 層測試示例（觸發異常，驗證數據未變）

---

### Q3: 如何復用數據庫容器？

**A**: 使用 `withReuse(true)`（見 Q1）

---

## 相關資源

- [SKILL.md](SKILL.md) - 詳細技術規格
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Testing Strategy](../../../../docs/testing/testing-strategy.md)

---

**Version**: 1.0.0
**Last Updated**: 2026-01-30
**Status**: Stable
