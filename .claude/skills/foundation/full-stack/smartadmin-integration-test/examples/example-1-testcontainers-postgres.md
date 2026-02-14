# 範例 1: Testcontainers PostgreSQL 整合測試

**技能**: smartadmin-integration-test
**難度**: ⭐⭐⭐☆☆（中高等）
**預估時間**: 15-20 分鐘

---

## 場景描述

使用 Testcontainers 建立真實 PostgreSQL 環境進行整合測試，確保 DAO 層、Service 層與數據庫的交互正確。

---

## 測試配置

### 1. 依賴引入

**build.gradle.kts**:
```kotlin
dependencies {
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}
```

---

### 2. 測試基類

**BaseIntegrationTest.java**:
```java
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = Replace.NONE) // 使用 Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("smartadmin_test")
        .withUsername("test")
        .withPassword("test");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }
}
```

---

## 整合測試範例

### EmployeeService 整合測試

**EmployeeServiceIntegrationTest.java**:
```java
@SpringBootTest
class EmployeeServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private EmployeeDao employeeDao;

    @Test
    @DisplayName("新增員工 - 完整流程測試")
    void testAddEmployee() {
        // Arrange
        EmployeeAddForm form = new EmployeeAddForm();
        form.setEmployeeName("張三");
        form.setDepartmentId(1L);
        form.setPosition("工程師");
        form.setJoinDate(LocalDate.of(2024, 1, 1));
        form.setStatus(1);

        // Act
        ResponseDTO<Long> response = employeeService.add(form);

        // Assert
        assertThat(response.getOk()).isTrue();
        Long employeeId = response.getData();
        assertThat(employeeId).isNotNull();

        // Verify database
        EmployeeEntity saved = employeeDao.selectById(employeeId);
        assertThat(saved.getEmployeeName()).isEqualTo("張三");
        assertThat(saved.getDepartmentId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("查詢員工 - 分頁測試")
    void testQueryEmployees() {
        // Arrange: 插入測試數據
        insertTestEmployees(10);

        EmployeeQueryForm form = new EmployeeQueryForm();
        form.setPageNum(1);
        form.setPageSize(5);

        // Act
        ResponseDTO<PageResult<EmployeeVO>> response = employeeService.query(form);

        // Assert
        assertThat(response.getOk()).isTrue();
        PageResult<EmployeeVO> page = response.getData();
        assertThat(page.getList()).hasSize(5);
        assertThat(page.getTotal()).isEqualTo(10);
    }

    private void insertTestEmployees(int count) {
        for (int i = 1; i <= count; i++) {
            EmployeeEntity entity = new EmployeeEntity();
            entity.setEmployeeName("測試員工" + i);
            entity.setDepartmentId(1L);
            entity.setPosition("工程師");
            entity.setJoinDate(LocalDate.now());
            entity.setStatus(1);
            employeeDao.insert(entity);
        }
    }
}
```

---

## 執行結果

```bash
./gradlew :smartadmin-app:test --tests EmployeeServiceIntegrationTest
```

**輸出**:
```
EmployeeServiceIntegrationTest > testAddEmployee() PASSED
EmployeeServiceIntegrationTest > testQueryEmployees() PASSED

BUILD SUCCESSFUL in 18s
```

**Testcontainers 日誌**:
```
Creating container for image: postgres:16-alpine
Container postgres:16-alpine started in PT2.145S
Database available at: jdbc:postgresql://localhost:52341/smartadmin_test
```

---

## 優勢

✅ **真實環境**: 使用真實 PostgreSQL，不是 H2 Mock
✅ **隔離性**: 每次測試獨立容器，互不干擾
✅ **可重複性**: 每次測試環境一致

---

## 相關規則

- **[Testing Strategy](../../../../../docs/testing/testing-strategy.md)**
  - 整合測試覆蓋 DAO + Service 層
  - Testcontainers 用於數據庫測試
