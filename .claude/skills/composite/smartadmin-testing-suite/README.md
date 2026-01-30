# SmartAdmin Testing Suite

> 完整的測試解決方案：整合測試（Integration Tests）+ 測試夾具（Test Fixtures）+ 單元測試（Unit Tests）+ E2E 測試

## 🚀 快速開始（5 分鐘上手）

### 最簡單的使用方式

```bash
# 場景：剛實現了 EmployeeService，需要創建測試
User: "Create integration tests for EmployeeService"

# AI 自動檢測並生成整合測試
/test EmployeeService --mode=integration

# 自動生成：
# 1. 整合測試類（EmployeeServiceIntegrationTest.java）
# 2. 測試夾具（EmployeeTestFixture.java）
# 3. Testcontainers 配置（PostgreSQL + Redis + Kafka）
# 4. 測試數據初始化

# 時間：約 10 分鐘
```

### 預期輸出

生成的檔案結構：
```
smart-admin-api-java21-springboot3/sa-admin/src/test/java/
└── net/lab1024/sa/admin/module/business/employee/
    ├── EmployeeServiceIntegrationTest.java    # 整合測試類
    ├── fixture/
    │   └── EmployeeTestFixture.java           # 測試夾具（Test Data Builder）
    └── config/
        └── TestContainersConfig.java          # Testcontainers 配置
```

**測試代碼示例**：
```java
@SpringBootTest
@Testcontainers
class EmployeeServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @Autowired
    private EmployeeService employeeService;

    @Test
    void testAddEmployee() {
        // 使用測試夾具創建測試數據
        EmployeeAddForm form = EmployeeTestFixture.builder()
            .withName("張三")
            .withDepartmentId(1L)
            .build();

        // 執行測試
        ResponseDTO<Long> response = employeeService.add(form);

        // 驗證結果
        assertThat(response.isOk()).isTrue();
        assertThat(response.getData()).isNotNull();
    }
}
```

---

## 📖 執行模式（如何選擇？）

### Mode 1: `--mode=integration` 整合測試模式（推薦）

**何時使用**：測試 Service/Manager/Controller 層，需要真實的數據庫和依賴

```bash
/test EmployeeService --mode=integration
```

**生成內容**：
- ✅ **整合測試類**
  - 使用 `@SpringBootTest` 啟動完整 Spring 上下文
  - 自動注入真實的 Service/Manager/Dao
  - 使用 Testcontainers 啟動真實數據庫

- ✅ **Testcontainers 配置**
  - PostgreSQL 容器（用於數據庫測試）
  - Redis 容器（用於緩存測試）
  - Kafka 容器（用於消息隊列測試）

- ✅ **測試數據初始化**
  - 使用 `@Sql` 執行初始化腳本
  - 每個測試方法後自動清理數據（`@Transactional`）

- ✅ **測試夾具**
  - Test Data Builder 模式
  - 快速構建測試數據

**測試層級**：
- Controller 層：測試 REST API（HTTP 請求/響應）
- Service 層：測試業務邏輯（Option 返回值處理）
- Manager 層：測試事務管理（`@Transactional` 驗證）
- Dao 層：測試 SQL 查詢（MyBatis-Plus 驗證）

**時間**：約 10 分鐘
**適用場景**：Service/Manager/Controller 測試、數據庫交互測試、事務測試

**整合替代技能**：前 `smartadmin-integration-test`

---

### Mode 2: `--mode=fixtures` 測試夾具模式

**何時使用**：需要快速構建複雜的測試數據對象

```bash
/test Employee --mode=fixtures
```

**生成內容**：
- ✅ **Test Data Builder**
  - 流式 API（`.withName()`, `.withDepartmentId()`）
  - 預設合理值（避免手動填寫所有字段）
  - 可組合性（支持嵌套對象構建）

**測試夾具示例**：
```java
public class EmployeeTestFixture {
    private String name = "測試員工";
    private Long departmentId = 1L;
    private String phone = "13800138000";
    private Boolean deleted = false;

    public static EmployeeTestFixture builder() {
        return new EmployeeTestFixture();
    }

    public EmployeeTestFixture withName(String name) {
        this.name = name;
        return this;
    }

    public EmployeeTestFixture withDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
        return this;
    }

    public EmployeeAddForm buildForm() {
        EmployeeAddForm form = new EmployeeAddForm();
        form.setName(name);
        form.setDepartmentId(departmentId);
        form.setPhone(phone);
        return form;
    }

    public EmployeeEntity buildEntity() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setName(name);
        entity.setDepartmentId(departmentId);
        entity.setPhone(phone);
        entity.setDeleted(deleted);
        return entity;
    }
}
```

**使用示例**：
```java
// 簡單場景：使用預設值
EmployeeAddForm form = EmployeeTestFixture.builder().buildForm();

// 複雜場景：自定義部分字段
EmployeeAddForm form = EmployeeTestFixture.builder()
    .withName("李四")
    .withDepartmentId(2L)
    .buildForm();
```

**時間**：約 5 分鐘
**適用場景**：複雜領域對象、測試數據準備、單元測試

**整合替代技能**：前 `test-fixture-generator`

---

### Mode 3: `--mode=unit` 單元測試模式（TDD）

**何時使用**：測試單一類的邏輯，使用 Mock 隔離依賴

```bash
/test EmployeeService --mode=unit
```

**生成內容**（未來功能）：
- ✅ **單元測試類**
  - 使用 `@ExtendWith(MockitoExtension.class)`
  - Mock 依賴（`@Mock`）
  - 注入被測試對象（`@InjectMocks`）

- ✅ **Mock 驗證**
  - 驗證方法調用次數（`verify(mock, times(1))`）
  - 驗證參數傳遞（`ArgumentCaptor`）

- ✅ **TDD 工作流支持**
  - 先寫測試（紅燈）
  - 再寫實現（綠燈）
  - 重構（重構期保持綠燈）

**單元測試示例**（未來功能）：
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
    void testGetDetail_found() {
        // Given: Mock 數據
        EmployeeEntity entity = new EmployeeEntity();
        entity.setId(1L);
        entity.setName("張三");
        when(employeeDao.selectById(1L)).thenReturn(entity);

        // When: 調用方法
        Option<EmployeeVO> result = employeeService.getDetail(1L);

        // Then: 驗證結果
        assertThat(result.isDefined()).isTrue();
        assertThat(result.get().getName()).isEqualTo("張三");

        // 驗證 Mock 調用
        verify(employeeDao, times(1)).selectById(1L);
    }
}
```

**時間**：約 8 分鐘
**適用場景**：純業務邏輯測試、快速反饋循環、TDD 開發

---

### Mode 4: `--mode=e2e` 端到端測試模式

**何時使用**：測試完整的用戶流程（前端 + 後端）

```bash
/test employee-management --mode=e2e
```

**生成內容**（未來功能）：
- ✅ **Cypress 測試**
  - 模擬用戶操作（點擊、輸入、提交）
  - 驗證頁面元素（斷言文本、狀態）
  - 截圖和視頻錄製（失敗時）

**E2E 測試示例**（未來功能）：
```javascript
describe('員工管理', () => {
  it('應該能創建新員工', () => {
    // 訪問員工列表頁
    cy.visit('/employee/list')

    // 點擊「新增」按鈕
    cy.contains('新增員工').click()

    // 填寫表單
    cy.get('[data-test="name"]').type('王五')
    cy.get('[data-test="department"]').select('技術部')
    cy.get('[data-test="phone"]').type('13900139000')

    // 提交表單
    cy.contains('確定').click()

    // 驗證成功消息
    cy.contains('創建成功').should('be.visible')

    // 驗證列表中出現新員工
    cy.contains('王五').should('be.visible')
  })
})
```

**時間**：約 15 分鐘
**適用場景**：用戶流程測試、前後端整合測試、回歸測試

---

### Mode 5: `--mode=all` 完整測試套件

**何時使用**：新模塊開發，需要完整的測試覆蓋

```bash
/test EmployeeService --mode=all
```

**生成內容**：
- ✅ 整合測試（Integration Tests）
- ✅ 單元測試（Unit Tests）
- ✅ 測試夾具（Test Fixtures）

**時間**：約 20 分鐘
**適用場景**：全新模塊、完整測試覆蓋、CI/CD 集成

---

## 🎯 使用場景

### ✅ When to Use（何時使用）

1. **新實現的 Service/Manager 方法**
   - 場景：剛寫完 `EmployeeService.add()` 方法
   - 選擇：`--mode=integration`（驗證業務邏輯和數據庫交互）

2. **複雜的業務邏輯測試**
   - 場景：涉及多個依賴、事務管理、緩存
   - 選擇：`--mode=integration`（使用真實依賴）

3. **測試數據準備複雜**
   - 場景：需要創建多個關聯對象（Employee → Department → Company）
   - 選擇：`--mode=fixtures`（快速構建測試數據）

4. **TDD 開發模式**
   - 場景：先寫測試再寫實現
   - 選擇：`--mode=unit`（快速反饋循環）

5. **用戶流程驗證**
   - 場景：驗證「創建員工 → 編輯 → 刪除」完整流程
   - 選擇：`--mode=e2e`（端到端測試）

### ❌ When NOT to Use（何時不使用）

1. **已有完整測試**
   - 場景：測試覆蓋率已達到 80%+
   - 原因：無需重複生成

2. **純工具類測試**
   - 場景：靜態工具類（`StringUtil`, `DateUtil`）
   - 原因：不需要整合測試，使用簡單的單元測試即可

3. **第三方庫測試**
   - 場景：測試 MyBatis-Plus、Spring Boot 框架本身
   - 原因：框架已有自己的測試

---

## 🔧 組合能力說明

### 組合 Skill 1: smartadmin-integration-test（整合測試）

**原技能功能**：
- Spring Boot 整合測試框架
- Testcontainers 自動化（PostgreSQL, Redis, Kafka）
- 測試數據初始化和清理
- Controller/Service/Manager/Dao 層測試

**整合到本技能**：
- 執行模式：`--mode=integration`
- 時間：10 分鐘
- 輸出：整合測試類 + Testcontainers 配置

**獨立使用場景**：
如果僅需要整合測試，建議使用：
```bash
/integration-test EmployeeService  # ⚠️ 已廢棄，自動路由到 /test --mode=integration
```

---

### 組合 Skill 2: test-fixture-generator（測試夾具）

**原技能功能**：
- Test Data Builder 模式
- 流式 API 構建測試數據
- 預設合理值
- 支持嵌套對象

**整合到本技能**：
- 執行模式：`--mode=fixtures`
- 時間：5 分鐘
- 輸出：測試夾具類（`*TestFixture.java`）

**獨立使用場景**：
如果僅需要測試夾具，建議使用：
```bash
/test-fixture Employee  # ⚠️ 已廢棄，自動路由到 /test --mode=fixtures
```

---

## ⚙️ 常見問題（FAQ）

### Q1: Testcontainers 啟動很慢（> 30 秒），如何優化？

**A**: 使用 Testcontainers 重用模式（Reusable Containers）：

**配置**：
```java
// 在測試基類中啟用容器重用
@Testcontainers
@SpringBootTest
abstract class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withReuse(true);  // 啟用重用
}
```

**~/.testcontainers.properties**：
```properties
testcontainers.reuse.enable=true
```

**效果**：
- 首次啟動：30 秒
- 後續測試：< 2 秒（重用已啟動的容器）

---

### Q2: 如何測試事務回滾（`@Transactional` 驗證）？

**A**: 整合測試自動包含事務回滾驗證：

**測試示例**：
```java
@Test
void testUpdateEmployee_rollbackOnException() {
    // Given: 準備測試數據
    Long employeeId = 1L;
    EmployeeUpdateForm form = new EmployeeUpdateForm();
    form.setName(null);  // 觸發異常（非空約束）

    // When & Then: 驗證拋出異常
    assertThatThrownBy(() -> employeeService.update(employeeId, form))
        .isInstanceOf(BusinessException.class);

    // 驗證數據未被修改（事務回滾）
    EmployeeEntity entity = employeeDao.selectById(employeeId);
    assertThat(entity.getName()).isEqualTo("原始名稱");  // 未被修改
}
```

---

### Q3: 測試夾具如何處理嵌套對象（例如，Employee 包含 Department）？

**A**: 生成的測試夾具支持嵌套對象構建：

**嵌套對象測試夾具**：
```java
public class EmployeeTestFixture {
    private DepartmentTestFixture departmentFixture = DepartmentTestFixture.builder();

    public EmployeeTestFixture withDepartment(DepartmentTestFixture department) {
        this.departmentFixture = department;
        return this;
    }

    public EmployeeEntity buildEntity() {
        EmployeeEntity entity = new EmployeeEntity();
        entity.setDepartmentId(departmentFixture.buildEntity().getId());
        return entity;
    }
}
```

**使用示例**：
```java
// 構建包含部門的員工
EmployeeEntity employee = EmployeeTestFixture.builder()
    .withDepartment(
        DepartmentTestFixture.builder()
            .withName("技術部")
            .withCode("TECH")
    )
    .buildEntity();
```

---

### Q4: 如何測試緩存功能（`@Cacheable` 驗證）？

**A**: 整合測試可以驗證緩存行為：

**測試示例**：
```java
@Test
void testGetById_cacheable() {
    // Given: 準備測試數據
    Long employeeId = 1L;

    // When: 第一次調用（從數據庫讀取）
    EmployeeVO result1 = employeeService.getById(employeeId);

    // 修改數據庫（繞過緩存）
    employeeDao.updateById(...);

    // When: 第二次調用（從緩存讀取）
    EmployeeVO result2 = employeeService.getById(employeeId);

    // Then: 驗證緩存生效（數據未更新）
    assertThat(result1.getName()).isEqualTo(result2.getName());

    // 清除緩存後驗證
    cacheManager.getCache("employee").clear();
    EmployeeVO result3 = employeeService.getById(employeeId);
    assertThat(result3.getName()).isNotEqualTo(result1.getName());
}
```

---

### Q5: 整合測試如何初始化測試數據？

**A**: 使用 `@Sql` 註解執行 SQL 腳本：

**測試類**：
```java
@SpringBootTest
@Testcontainers
@Sql("/test-data/employee-setup.sql")  // 測試前執行
@Sql(scripts = "/test-data/employee-cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class EmployeeServiceIntegrationTest {
    // 測試方法
}
```

**SQL 腳本**（`employee-setup.sql`）：
```sql
INSERT INTO t_department (id, name, code) VALUES (1, '技術部', 'TECH');
INSERT INTO t_employee (id, name, department_id, phone) VALUES (1, '張三', 1, '13800138000');
```

---

### Q6: 如何運行生成的測試？

**A**: 三種運行方式：

**方式 1: Gradle（單個測試類）**：
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests EmployeeServiceIntegrationTest
```

**方式 2: Gradle（所有整合測試）**：
```bash
./gradlew :sa-admin:test --tests '*IntegrationTest'
```

**方式 3: IDE（IntelliJ IDEA）**：
- 右鍵點擊測試類 → Run 'EmployeeServiceIntegrationTest'

---

### Q7: Testcontainers 需要 Docker 嗎？

**A**: 是的，Testcontainers 需要 Docker Desktop：

**安裝 Docker**：
- macOS: [Docker Desktop for Mac](https://www.docker.com/products/docker-desktop)
- Windows: [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop)
- Linux: `sudo apt install docker.io`

**驗證 Docker**：
```bash
docker --version
docker ps  # 確認 Docker 運行正常
```

**CI/CD 環境**：
- GitHub Actions: 預裝 Docker
- GitLab CI: 使用 `docker:dind` 服務
- Jenkins: 安裝 Docker 插件

---

### Q8: 測試數據如何隔離（避免測試間相互影響）？

**A**: 生成的測試自動包含數據隔離策略：

**策略 1: `@Transactional`（推薦）**：
```java
@SpringBootTest
@Transactional  // 測試方法結束後自動回滾
class EmployeeServiceIntegrationTest {
    @Test
    void testAddEmployee() {
        // 測試邏輯
        // 測試結束後，數據自動回滾，不影響其他測試
    }
}
```

**策略 2: `@DirtiesContext`（重量級）**：
```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EmployeeServiceIntegrationTest {
    // 每個測試方法後重建 Spring 上下文（慢，但完全隔離）
}
```

**策略 3: 測試數據清理**：
```java
@AfterEach
void cleanup() {
    employeeDao.deleteAll();
    departmentDao.deleteAll();
}
```

---

## 🔗 相關資源

### SKILL.md（技術規格）
詳細的技術規格和實現細節：[SKILL.md](SKILL.md)

### 模式文檔
- [Mode 1: Integration Tests](modes/mode-1-integration.md) - Spring Boot 整合測試 + Testcontainers
- [Mode 2: Test Fixtures](modes/mode-2-fixtures.md) - Test Data Builder 模式
- [Mode 3: Unit Tests (TDD)](modes/mode-3-unit.md) - Mock 單元測試
- [Mode 4: E2E Tests](modes/mode-4-e2e.md) - Cypress 端到端測試
- [Mode All: Complete Suite](modes/mode-all.md) - 完整測試套件

### 整合技能文檔
- [smartadmin-integration-test](../../foundation/full-stack/smartadmin-integration-test/SKILL.md) - 原整合測試技能（已整合）
- [test-fixture-generator](../../foundation/testing/test-fixture-generator/SKILL.md) - 原測試夾具技能（已整合）

### SmartAdmin 測試文檔
- [Testing Strategy](../../../../docs/testing/testing-strategy.md) - SmartAdmin 測試策略
- [Integration Testing Guide](../../../../docs/testing/integration-testing-quick-reference.md) - 整合測試快速參考

### 外部文檔
- [Testcontainers Documentation](https://www.testcontainers.org/) - Testcontainers 官方文檔
- [JUnit 5 Documentation](https://junit.org/junit5/) - JUnit 5 官方文檔
- [Mockito Documentation](https://site.mockito.org/) - Mockito 官方文檔
- [Cypress Documentation](https://www.cypress.io/) - Cypress 官方文檔

---

## 📊 版本信息

**Skill Version**: 2.0.0
**Last Updated**: 2026-01-29
**Status**: Stable
**Compatible with**: SmartAdmin v4.0.0+

### v2.0.0 變更（2026-01-29）
- ✅ **整合 2 個技能**：將 `smartadmin-integration-test`, `test-fixture-generator` 整合為統一的模式化工作流
- ✅ **支援 5 種模式**：`--mode=integration`, `--mode=fixtures`, `--mode=unit`, `--mode=e2e`, `--mode=all`
- ✅ **向後兼容**：舊命令（`/integration-test`, `/test-fixture`）仍可使用，會顯示遷移警告
- ✅ **未來擴展**：預留 `unit` 和 `e2e` 模式（未來版本實現）

### 遷移時間線（v2.0.0）
- **Weeks 1-12** (Soft Deprecation): 舊命令可用，顯示警告
- **Weeks 13-24** (Hard Deprecation): 舊命令顯示錯誤 + 遷移指南
- **Week 25+** (Removal): 舊命令永久移除

詳見：[skill-aliases.json](../skill-aliases.json) 路由配置

---

**Last Updated**: 2026-01-30
