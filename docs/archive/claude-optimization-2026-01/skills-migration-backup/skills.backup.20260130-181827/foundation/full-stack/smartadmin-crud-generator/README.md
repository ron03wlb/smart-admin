# SmartAdmin Full-Stack CRUD Generator

> 一鍵生成完整的全棧 CRUD 模組：後端（Entity/Dao/Manager/Service/Controller）+ 前端（Vue 3 元件）+ API 文檔 + 測試

## 🚀 快速開始（5 分鐘上手）

### 最簡單的使用方式

```bash
# 創建完整的 CRUD 模組
User: "Create a Product CRUD module with name, price, category, and stock fields"

# 系統會自動：
# 1. 收集實體需求（欄位、類型、驗證規則）
# 2. 生成後端層（Entity → Dao → Manager → Service → Controller）
# 3. 生成前端元件（列表頁 + 表單彈窗 + API 客戶端）
# 4. 生成測試（單元測試 + 整合測試）
# 5. 生成 API 文檔（Swagger 註解）
# 6. 驗證架構合規性（ArchitectureTest）
```

### 預期輸出

生成的檔案結構：
```
smart-admin-api-java21-springboot3/sa-admin/src/main/java/
└── net/lab1024/sa/admin/module/business/product/
    ├── domain/
    │   ├── entity/ProductEntity.java
    │   ├── form/ProductAddForm.java
    │   ├── form/ProductUpdateForm.java
    │   ├── form/ProductQueryForm.java
    │   └── vo/ProductVO.java
    ├── dao/ProductDao.java
    ├── manager/ProductManager.java
    ├── service/ProductService.java
    └── controller/ProductController.java

smart-admin-web/src/
├── views/product/
│   ├── product-list.vue          # 列表頁（表格 + 搜索 + 分頁）
│   └── product-form-modal.vue    # 表單彈窗（新增/編輯）
└── api/product-api.ts             # API 客戶端

smart-admin-api-java21-springboot3/sa-admin/src/test/java/
└── net/lab1024/sa/admin/module/business/product/
    ├── service/ProductServiceTest.java
    └── integration/ProductControllerIntegrationTest.java
```

---

## 📖 執行模式（如何選擇？）

### Mode 1: `--all-phases` 完整模式（推薦）

**何時使用**：全新模組開發，需要完整的前後端 + 測試

```bash
/crud Employee --all-phases
```

**生成內容**：
- ✅ Phase 1: 後端層（Entity, Dao, Manager, Service, Controller）
- ✅ Phase 2: 前端元件（Vue 3 + Ant Design Vue）
- ✅ Phase 3: API 文檔（Knife4j/Swagger 註解）
- ✅ Phase 4: 測試（單元測試 + 整合測試 with Testcontainers）

**時間**：約 20 分鐘
**適用場景**：新業務模組、完整功能開發

---

### Mode 2: `--backend-only` 僅後端模式

**何時使用**：只需要後端 API，前端由其他團隊開發

```bash
/crud Order --backend-only
```

**生成內容**：
- ✅ Entity（實體類 + MyBatis-Plus 註解）
- ✅ Dao（MyBatis Mapper 接口）
- ✅ Manager（事務管理層，包含 `@Transactional`）
- ✅ Service（業務邏輯層，使用 `io.vavr.control.Option`）
- ✅ Controller（REST API 層，返回 `ResponseDTO`）
- ✅ Domain 物件（Form, VO, QueryForm）

**時間**：約 10 分鐘
**適用場景**：API 開發、微服務後端、僅需數據層

**整合替代技能**：前 `smartadmin-mybatis`

---

### Mode 3: `--frontend-only` 僅前端模式

**何時使用**：後端 API 已存在，需要快速生成前端介面

```bash
/crud Brand --frontend-only
```

**生成內容**：
- ✅ Vue 3 列表元件（Composition API + `<script setup>`）
- ✅ 表單彈窗元件（Ant Design Vue 4）
- ✅ API 客戶端（TypeScript + Axios）
- ✅ TypeScript 類型定義（與後端 DTO 同步）

**時間**：約 8 分鐘
**適用場景**：前端開發、快速原型、UI 迭代

**整合替代技能**：前 `smartadmin-vue-crud`

---

### Mode 4: `--docs-only` 僅 API 文檔模式

**何時使用**：已有代碼，需要補充 API 文檔

```bash
/crud Product --docs-only
```

**生成內容**：
- ✅ Knife4j 註解（`@Tag`, `@Operation`, `@Schema`）
- ✅ 自動生成 API 文檔（http://localhost:1024/doc.html）
- ✅ 請求/響應示例

**時間**：約 3 分鐘
**適用場景**：補充文檔、API 對外開放、團隊協作

**整合替代技能**：前 `smartadmin-api-docs`

---

## 🎯 使用場景

### ✅ When to Use（何時使用）

1. **全新業務模組開發**
   - 場景：新增「訂單管理」、「商品管理」、「用戶管理」等模組
   - 選擇：`--all-phases`（完整模式）

2. **快速原型驗證**
   - 場景：產品需求驗證、技術可行性測試
   - 選擇：`--all-phases`（快速生成可交互的原型）

3. **API 開發（前後端分離）**
   - 場景：後端團隊單獨開發 API
   - 選擇：`--backend-only`

4. **前端獨立開發**
   - 場景：後端 API 已就緒，需要前端介面
   - 選擇：`--frontend-only`

5. **遺留代碼文檔補充**
   - 場景：舊項目缺少 API 文檔
   - 選擇：`--docs-only`

6. **整合測試缺失**
   - 場景：代碼已有，但缺少測試覆蓋
   - 選擇：執行 Phase 4（或使用 `smartadmin-integration-test` 技能）

### ❌ When NOT to Use（何時不使用）

1. **複雜業務邏輯**
   - 場景：需要多表關聯、複雜事務、工作流編排
   - 原因：自動生成僅覆蓋標準 CRUD，複雜邏輯需手動實現
   - 建議：先生成基礎代碼，再手動擴展

2. **高度自定義的 UI**
   - 場景：設計師提供特殊視覺設計、複雜交互邏輯
   - 原因：生成的前端元件基於標準模板
   - 建議：使用 `--backend-only`，前端手動開發

3. **非標準 RESTful API**
   - 場景：需要 GraphQL、gRPC、WebSocket 等非標準 API
   - 原因：生成器基於 RESTful + JSON
   - 建議：手動實現

4. **已存在的成熟模組**
   - 場景：模組已開發並在生產環境運行
   - 原因：覆寫風險高
   - 建議：不要使用（除非有明確備份和測試計劃）

---

## ⚙️ 配置選項

### 自定義模板

如果標準模板不滿足需求，可以自定義模板檔案：

**模板位置**：
```
.claude/skills/foundation/full-stack/smartadmin-crud-generator/templates/
├── Entity.java.template
├── Dao.java.template
├── Manager.java.template
├── Service.java.template
├── Controller.java.template
├── Form.java.template
├── VO.java.template
├── QueryForm.java.template
├── list-view.vue.template
├── form-modal.vue.template
└── api-client.ts.template
```

**自定義步驟**：
1. 複製 `templates/` 目錄為 `templates-custom/`
2. 修改 `templates-custom/` 中的檔案
3. 在 `config.yml` 中指定自定義模板路徑：
   ```yaml
   output:
     templates:
       entity: "templates-custom/Entity.java.template"
   ```

### 命名規則

**後端命名**：
- Entity: `ProductEntity.java`
- Dao: `ProductDao.java`
- Manager: `ProductManager.java`
- Service: `ProductService.java`
- Controller: `ProductController.java`

**前端命名**：
- List View: `product-list.vue`
- Form Modal: `product-form-modal.vue`
- API Client: `product-api.ts`

**自定義命名**（可選）：
```bash
# 使用自定義命名前綴
/crud Employee --prefix=hr  # 生成 HrEmployeeService.java
```

### 生成路徑

**後端路徑**：
```
smart-admin-api-java21-springboot3/sa-admin/src/main/java/
net/lab1024/sa/admin/module/business/{module-name}/
```

**前端路徑**：
```
smart-admin-web/src/views/{module-name}/
smart-admin-web/src/api/{module-name}-api.ts
```

**自定義路徑**（可選）：
```bash
# 使用自定義模組路徑
/crud Order --module-path=business.order.management
# 生成路徑：net/lab1024/sa/admin/module/business/order/management/
```

---

## 🔧 進階用法

### 1. 與現有模組整合

如果需要生成的 CRUD 與現有模組整合（例如，新增的 Order 依賴已存在的 Customer）：

**步驟**：
1. 先運行 `--backend-only` 生成基礎代碼
2. 手動添加依賴關係（例如，在 OrderService 中注入 CustomerService）
3. 運行 ArchitectureTest 驗證架構合規性：
   ```bash
   ./gradlew :sa-admin:test --tests ArchitectureTest
   ```

### 2. 自定義業務邏輯擴展

生成的 Manager 層預留了業務邏輯擴展點：

**生成的代碼**：
```java
@Service
@RequiredArgsConstructor
public class ProductManager {
    private final ProductDao productDao;

    @Transactional(rollbackFor = Throwable.class)
    public void updateProduct(ProductUpdateForm form) {
        // TODO: 在此添加複雜業務邏輯
        ProductEntity entity = SmartBeanUtil.copy(form, ProductEntity.class);
        productDao.updateById(entity);
    }
}
```

**擴展示例**：
```java
@Transactional(rollbackFor = Throwable.class)
public void updateProduct(ProductUpdateForm form) {
    // 1. 驗證庫存
    if (form.getStock() < 0) {
        throw new BusinessException("庫存不能為負數");
    }

    // 2. 更新數據
    ProductEntity entity = SmartBeanUtil.copy(form, ProductEntity.class);
    productDao.updateById(entity);

    // 3. 發送庫存更新事件（Kafka）
    eventPublisher.publishStockUpdatedEvent(entity.getId(), entity.getStock());
}
```

### 3. 多模式組合執行

如果需要分階段生成（例如，先生成後端，驗證後再生成前端）：

```bash
# 第 1 步：生成後端
/crud Product --backend-only

# 第 2 步：手動測試後端 API（Postman/curl）
curl -X POST http://localhost:1024/product/add -d '{...}'

# 第 3 步：後端驗證通過後，生成前端
/crud Product --frontend-only

# 第 4 步：整合測試通過後，補充文檔
/crud Product --docs-only
```

### 4. 性能優化建議

生成的代碼包含基本的性能優化，但可根據實際需求進一步優化：

**緩存優化**（可選）：
```java
// 在 Service 層添加緩存註解
@Service
@RequiredArgsConstructor
public class ProductService {
    @Cacheable(value = "product", key = "#id")
    public Option<ProductVO> getDetail(Long id) {
        return productDao.selectById(id)
            .map(entity -> SmartBeanUtil.copy(entity, ProductVO.class));
    }
}
```

**分頁查詢優化**：
- 使用 `SmartPageUtil.convert2PageQuery(form)` 轉換分頁參數
- MyBatis-Plus 自動處理分頁查詢（不需要手動處理 LIMIT/OFFSET）

---

## ❓ 常見問題（FAQ）

### Q1: 如何自定義生成的代碼模板？

**A**: 複製 `templates/` 目錄為 `templates-custom/`，修改後在 `config.yml` 中指定自定義模板路徑。

詳見：[配置選項 > 自定義模板](#自定義模板)

---

### Q2: 如何與現有模塊集成（例如，訂單依賴商品）？

**A**: 分兩步執行：
1. 先生成基礎 CRUD（`--backend-only`）
2. 手動添加依賴注入和業務邏輯：
   ```java
   @Service
   @RequiredArgsConstructor
   public class OrderService {
       private final OrderDao orderDao;
       private final ProductService productService;  // 手動注入

       public Option<OrderVO> create(OrderAddForm form) {
           // 驗證商品是否存在
           productService.getDetail(form.getProductId())
               .onEmpty(() -> throw new BusinessException("商品不存在"));
           // ...
       }
   }
   ```

---

### Q3: 如何處理複雜的業務邏輯（例如，訂單下單涉及庫存扣減、支付、物流）？

**A**: 自動生成僅覆蓋標準 CRUD，複雜業務邏輯需要：
1. 使用 `--backend-only` 生成基礎代碼
2. 在 Manager 層擴展業務邏輯（保留 `@Transactional` 註解）
3. 如果需要工作流編排，建議使用 `liteflow-rule-builder` 技能

---

### Q4: 生成的前端元件是 Vue 2 還是 Vue 3？

**A**: **Vue 3** + Composition API（`<script setup>`）+ Ant Design Vue 4

- Options API → Composition API 遷移指南：[Vue 3 Migration Guide](https://v3-migration.vuejs.org/)
- 如果項目仍使用 Vue 2，請使用舊技能 `smartadmin-vue-crud`（已廢棄，將於 2026-06-30 移除）

---

### Q5: 如何驗證生成的代碼符合 SmartAdmin 架構規範？

**A**: 執行 ArchitectureTest：
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:test --tests ArchitectureTest
```

**常見違規**：
- ❌ Service 層使用 `@Transactional`（應在 Manager 層）
- ❌ Controller 直接注入 Dao（應注入 Service）
- ❌ 使用 `@Autowired` 欄位注入（應使用 `@RequiredArgsConstructor` 構造器注入）

---

### Q6: 生成的代碼是否支援軟刪除（Soft Delete）？

**A**: 是的，預設支援軟刪除：
- Entity 包含 `deleted` 欄位（`Boolean`，預設 `false`）
- MyBatis-Plus 自動處理邏輯刪除（配置 `@TableLogic`）
- 查詢自動過濾已刪除記錄

**如果不需要軟刪除**：
在收集需求時明確說明：
```
User: "Create Product CRUD, no soft delete needed"
```

---

### Q7: 前端表單支援哪些驗證類型？

**A**: 生成的前端表單自動包含驗證規則（與後端 `@NotNull`, `@Size` 同步）：
- 必填驗證（`required: true`）
- 長度驗證（`max: 100`）
- 數字範圍驗證（`min: 0, max: 999`）
- 自定義正則驗證（例如，手機號碼、Email）

**自定義驗證**：
在 `form-modal.vue` 中手動添加：
```typescript
const rules = {
  email: [
    { required: true, message: '請輸入Email' },
    { type: 'email', message: 'Email 格式錯誤' }
  ]
}
```

---

### Q8: 生成的測試使用哪些框架？

**A**:
- **單元測試**: JUnit 5 + Mockito
- **整合測試**: Spring Boot Test + Testcontainers（PostgreSQL, Redis, Kafka）

**整合測試自動化**：
- 自動啟動 PostgreSQL 容器（Testcontainers）
- 自動初始化測試數據
- 測試結束後自動清理

---

### Q9: 能否只生成測試（Phase 4）？

**A**: 目前不支援單獨執行 Phase 4。
- 建議使用 `smartadmin-integration-test` 技能單獨生成整合測試
- 或使用 `test-fixture-generator` 技能生成測試夾具

---

### Q10: 生成後如何運行和驗證？

**A**: 三步驗證流程：

**1. 編譯驗證**：
```bash
cd smart-admin-api-java21-springboot3
./gradlew :sa-admin:compileJava
```

**2. 架構驗證**：
```bash
./gradlew :sa-admin:test --tests ArchitectureTest
```

**3. 功能驗證**：
```bash
# 啟動後端
./gradlew :sa-admin:bootRun

# 訪問 API 文檔
open http://localhost:1024/doc.html

# 測試 API（Knife4j 在線調試）
```

**4. 前端驗證**（如果生成了前端）：
```bash
cd smart-admin-web
npm run dev

# 訪問列表頁
open http://localhost:7080/product
```

---

## 🔗 相關資源

### SKILL.md（技術規格）
詳細的技術規格和實現細節：[SKILL.md](SKILL.md)

### 階段文檔
- [Phase 1: Backend Generation](phases/phase-1-backend.md) - 後端層生成（MyBatis, 分層架構）
- [Phase 2: Frontend Generation](phases/phase-2-frontend.md) - Vue 3 + Ant Design + TypeScript
- [Phase 3: API Docs Generation](phases/phase-3-api-docs.md) - Knife4j/OpenAPI 註解
- [Phase 4: Test Generation](phases/phase-4-tests.md) - 整合測試 with Testcontainers

### 示例代碼
- [examples/](examples/) - 完整的示例代碼（Product CRUD）

### SmartAdmin 文檔
- [SmartAdmin Patterns](../../../.claude/shared/knowledge/smartadmin-patterns.md) - SmartAdmin 開發模式
- [Architecture Rules](../../../../.agent/rules/foundation/10-architecture-rules.md) - 架構規則
- [Manager Layer Rules](../../../../.agent/rules/foundation/09-manager-layer.md) - Manager 層規則

### 外部文檔
- [MyBatis-Plus Documentation](https://baomidou.com/) - MyBatis-Plus 官方文檔
- [Vue 3 Documentation](https://vuejs.org/) - Vue 3 官方文檔
- [Ant Design Vue](https://antdv.com/) - Ant Design Vue 官方文檔
- [Knife4j Documentation](https://doc.xiaominfo.com/) - Knife4j 官方文檔

---

## 📊 版本信息

**Skill Version**: 2.0.0
**Last Updated**: 2026-01-29
**Status**: Stable
**Compatible with**: SmartAdmin v4.0.0+

### v2.0.0 變更（2026-01-27）
- ✅ **整合 4 個技能**：將 `smartadmin-mybatis`, `smartadmin-vue-crud`, `smartadmin-api-docs`, 整合測試模式整合為統一的階段式工作流
- ✅ **時間效率提升 56%**：從 45 分鐘（4 個獨立技能）降至 20 分鐘（統一工作流）
- ✅ **支援獨立階段執行**：`--backend-only`, `--frontend-only`, `--docs-only`, `--all-phases`
- ✅ **向後兼容**：舊命令（`/mybatis`, `/vue-crud`, `/api-docs`）仍可使用，會顯示遷移警告

### 遷移時間線（v2.0.0）
- **Weeks 1-12** (Soft Deprecation): 舊命令可用，顯示警告
- **Weeks 13-24** (Hard Deprecation): 舊命令顯示錯誤 + 遷移指南
- **Week 25+** (Removal): 舊命令永久移除

詳見：[skill-aliases.json](../skill-aliases.json) 路由配置

---

**Last Updated**: 2026-01-30
