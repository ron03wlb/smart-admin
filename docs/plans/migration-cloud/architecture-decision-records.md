# Architecture Decision Records (ADR)
## SmartAdmin 微服務遷移架構決策記錄

**文檔類型**: 架構決策記錄集合
**版本**: 1.0.0
**創建日期**: 2026-02-02

---

## ADR 索引

| ADR # | 標題 | 狀態 | 日期 |
|-------|------|------|------|
| ADR-001 | Manager 層保留決策 | ✅ 已批准 | 2026-02-02 |
| ADR-002 | ArchUnit 規則增強 | ✅ 已批准 | 2026-02-02 |
| ADR-003 | Vavr Option 跨服務使用 | ✅ 已批准 | 2026-02-02 |
| ADR-004 | 數據庫漸進式拆分 | ✅ 已批准 | 2026-02-02 |
| ADR-005 | 構造器注入強制約束 | ✅ 已批准 | 2026-02-02 |
| ADR-006 | 微服務拆分範圍界定 | ✅ 已批准 | 2026-02-02 |

---

## ADR-001: Manager 層保留決策

### 狀態
✅ **已批准** - 2026-02-02

### 上下文

SmartAdmin v4.0.0 採用獨特的四層架構 (Controller → Service → Manager → Dao)，其中 Manager 層專門處理 `@Transactional` 和 `@Cacheable` 註解。RuoYi-Cloud-Plus 採用傳統三層架構 (Controller → Service → Dao)，Service 層直接使用 `@Transactional`。

遷移時需要決定：是否移除 Manager 層以符合 RuoYi-Cloud-Plus 標準？

### 決策

**保留 Manager 層**，作為 SmartAdmin 微服務架構的核心差異化特徵。

### 理由

#### 1. 事務邊界清晰化 (最核心優勢)

**問題**: Service 層混合業務邏輯與事務管理

```java
// ❌ 反模式 (RuoYi-Cloud-Plus 風格)
@Service
public class GoodsService {
    @Transactional(rollbackFor = Exception.class)
    public void addGoods(GoodsForm form) {
        // 業務邏輯 + 事務管理混合
        GoodsEntity entity = convert(form);
        goodsDao.insert(entity);
        dataTracerService.insert(entity.getGoodsId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateGoods(GoodsForm form) {
        // 另一個事務方法
        goodsDao.updateById(convert(form));
    }

    // 問題: 如果 Service 方法互相調用，事務傳播行為難以預測
    public void batchUpdate(List<GoodsForm> forms) {
        forms.forEach(this::updateGoods);  // 事務傳播: REQUIRED? REQUIRES_NEW?
    }
}
```

**解決方案**: Manager 層隔離事務邏輯

```java
// ✅ SmartAdmin 風格
@Service
@RequiredArgsConstructor
public class GoodsService {
    private final GoodsManager goodsManager;

    // Service 層: 純業務邏輯，無事務註解
    public Option<GoodsVO> addGoods(GoodsForm form) {
        GoodsEntity entity = SmartBeanUtil.copy(form, GoodsEntity.class);
        goodsManager.addGoodsTransaction(entity);  // 委託給 Manager
        return Option.of(entity).map(this::toVO);
    }
}

@Component
@RequiredArgsConstructor
public class GoodsManager {
    private final GoodsDao goodsDao;
    private final DataTracerService dataTracerService;

    // Manager 層: 專注事務管理
    @Transactional(rollbackFor = Throwable.class)
    public void addGoodsTransaction(GoodsEntity entity) {
        goodsDao.insert(entity);
        dataTracerService.insert(entity.getGoodsId(), DataTracerTypeEnum.GOODS);
        // 兩操作在同一事務中，保證原子性
    }
}
```

**優勢對比**:

| 維度 | RuoYi 風格 (Service 事務) | SmartAdmin 風格 (Manager 事務) |
|------|---------------------------|-------------------------------|
| **職責分離** | ❌ 業務邏輯 + 事務管理混合 | ✅ Service 業務邏輯，Manager 事務管理 |
| **事務傳播** | ❌ Service 互相調用時，傳播行為難預測 | ✅ Service 調用 Manager，事務邊界清晰 |
| **測試友好** | ❌ 單元測試需要處理事務邏輯 | ✅ Service 測試無需事務，Manager 單獨測試事務 |
| **架構驗證** | ❌ 無法自動化驗證事務位置 | ✅ ArchUnit 強制驗證 (編譯時檢查) |

#### 2. ArchUnit 自動化驗證

```java
// ArchitectureTest.java (SmartAdmin 核心保障)
@ArchTest
static final ArchRule transactionalMustInManagerLayer =
    methods()
        .that().areAnnotatedWith(Transactional.class)
        .should().beDeclaredInClassesThat().haveSimpleNameEndingWith("Manager")
        .because("@Transactional 只能在 Manager 層使用，確保事務邊界清晰");

@ArchTest
static final ArchRule serviceShouldNotHaveTransactional =
    noMethods()
        .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Service")
        .should().beAnnotatedWith(Transactional.class)
        .because("Service 層禁止 @Transactional，必須委託給 Manager 層");
```

**量化價值**:
- ✅ **100% 強制執行**: 違規代碼無法通過 CI
- ✅ **零人工成本**: 無需 Code Review 檢查事務位置
- ✅ **防止架構腐化**: 長期維護中，新人無法違反規則

**實際案例**:
```java
// 如果開發者誤在 Service 層使用 @Transactional
@Service
public class GoodsService {
    @Transactional  // ❌ 違規
    public void addGoods(GoodsForm form) {
        // ...
    }
}

// 編譯時 ArchUnit 測試失敗:
// ❌ Architecture Violation:
//    Method GoodsService.addGoods() is annotated with @Transactional,
//    but @Transactional is only allowed in Manager layer.
//
//    Violation at: net.lab1024.sa.admin.module.business.goods.service.GoodsService (GoodsService.java:42)
```

#### 3. 可測試性提升

**Service 層單元測試** (無需事務)

```java
@ExtendWith(MockitoExtension.class)
class GoodsServiceTest {

    @Mock
    private GoodsManager goodsManager;  // Mock Manager 層

    @InjectMocks
    private GoodsService goodsService;

    @Test
    void testAddGoods_success() {
        // Arrange
        GoodsForm form = new GoodsForm();
        form.setGoodsName("測試商品");

        // Act
        Option<GoodsVO> result = goodsService.addGoods(form);

        // Assert
        assertTrue(result.isDefined());
        verify(goodsManager, times(1)).addGoodsTransaction(any());
        // 無需啟動事務，測試速度快
    }
}
```

**Manager 層事務測試** (專注事務邏輯)

```java
@SpringBootTest
@Transactional
class GoodsManagerTest {

    @Autowired
    private GoodsManager goodsManager;

    @Autowired
    private GoodsDao goodsDao;

    @Test
    @Rollback(false)
    void testAddGoodsTransaction_commit() {
        // Arrange
        GoodsEntity goods = new GoodsEntity();
        goods.setGoodsName("測試商品");

        // Act
        goodsManager.addGoodsTransaction(goods);

        // Assert
        GoodsEntity saved = goodsDao.selectById(goods.getGoodsId());
        assertNotNull(saved);
    }

    @Test
    void testAddGoodsTransaction_rollback() {
        // Arrange
        GoodsEntity goods = new GoodsEntity();
        // 模擬異常場景
        when(dataTracerService.insert(any(), any())).thenThrow(new RuntimeException());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            goodsManager.addGoodsTransaction(goods);
        });

        // Verify: 事務回滾，goodsDao.insert 也應回滾
        long count = goodsDao.selectCount(null);
        assertEquals(0, count);
    }
}
```

**測試效率對比**:

| 測試類型 | RuoYi 風格 | SmartAdmin 風格 | 提升 |
|---------|-----------|----------------|------|
| **Service 單元測試** | 需要啟動事務 (~500ms/test) | 無需事務 (~50ms/test) | ⚡ 10x |
| **事務邏輯測試** | 混在 Service 測試中 | Manager 獨立測試 | 🎯 清晰 |
| **Mock 複雜度** | 需要 Mock 事務管理 | 只需 Mock Manager | ✅ 簡單 |

#### 4. 微服務架構兼容性

**Manager 層在微服務內部仍然適用**:

```
每個微服務內部分層:
┌──────────────────────────────────┐
│  Microservice (ruoyi-business)   │
│  ┌────────────────────────────┐  │
│  │ Controller → Service       │  │
│  │              ↓             │  │
│  │           Manager (事務層)  │  │
│  │              ↓             │  │
│  │           Dao → Entity     │  │
│  └────────────────────────────┘  │
└──────────────────────────────────┘

跨服務調用:
Business Service → [Dubbo RPC] → System Service
                                      ↓
                                  Manager 層
```

**Manager 層在微服務場景的新職責**:

1. **封裝 RPC 調用** (Service 層禁止直接調用 RPC)
   ```java
   @Component
   public class GoodsManager {
       @DubboReference
       private RemoteUserService remoteUserService;  // ✅ Manager 可以

       public Option<GoodsVO> getGoodsWithCreator(Long id) {
           return Option.of(goodsDao.selectById(id))
               .flatMap(goods -> {
                   // Manager 封裝 RPC 複雜性
                   Option<UserDTO> creator = remoteUserService.getUserById(goods.getCreateUserId());
                   return creator.map(user -> {
                       GoodsVO vo = toVO(goods);
                       vo.setCreateUserName(user.getActualName());
                       return vo;
                   });
               });
       }
   }
   ```

2. **批量查詢優化** (減少 RPC 次數)
   ```java
   @Component
   public class GoodsManager {
       public List<GoodsVO> batchGetGoodsWithCreators(List<Long> goodsIds) {
           // 1. 批量查詢商品
           List<GoodsEntity> goodsList = goodsDao.selectBatchIds(goodsIds);

           // 2. 提取創建人 ID
           List<Long> userIds = goodsList.stream()
               .map(GoodsEntity::getCreateUserId)
               .distinct()
               .collect(Collectors.toList());

           // 3. 批量 RPC 查詢 (1 次 RPC vs N 次)
           Map<Long, UserDTO> userMap = remoteUserService.batchGetUsers(userIds);

           // 4. 組裝 VO
           return goodsList.stream()
               .map(goods -> {
                   GoodsVO vo = toVO(goods);
                   UserDTO creator = userMap.get(goods.getCreateUserId());
                   if (creator != null) {
                       vo.setCreateUserName(creator.getActualName());
                   }
                   return vo;
               })
               .collect(Collectors.toList());
       }
   }
   ```

### 替代方案

#### 方案 B: 移除 Manager 層

**優勢**:
- ✅ 符合 RuoYi-Cloud-Plus 標準
- ✅ 減少一層抽象

**劣勢**:
- ❌ 喪失 SmartAdmin 核心優勢
- ❌ 事務邊界模糊
- ❌ 無法使用 ArchUnit 自動化驗證
- ❌ 代碼重構成本高 (40+ Manager 類)
- ❌ 測試複雜度增加

**決策**: ❌ 不採納

### 後果

#### 正面影響

1. ✅ **保留核心競爭力**: SmartAdmin 架構質量優勢延續到微服務時代
2. ✅ **質量保障**: ArchUnit 持續驗證事務規範
3. ✅ **團隊一致性**: 開發者無需學習新架構模式
4. ✅ **向後兼容**: 已有代碼無需大規模重構

#### 負面影響

1. ⚠️ **學習成本**: 新團隊成員需要理解 Manager 層設計理念
2. ⚠️ **非標準架構**: 不符合 RuoYi-Cloud-Plus 標準 (需文檔說明)

#### 緩解措施

1. 📚 **完善文檔**: 編寫 Manager 層設計原則與最佳實踐
2. 🎓 **團隊培訓**: 新人入職培訓包含 Manager 層講解
3. 📖 **代碼示例**: 提供標準 CRUD 範例代碼

### 合規性

- ✅ **ArchUnit 驗證**: 已在 ArchitectureTest.java 中定義規則
- ✅ **CI/CD 集成**: 構建失敗如果違反 Manager 層約束
- ✅ **Code Review**: 檢查清單包含 Manager 層檢查項

### 相關 ADR

- [ADR-002: ArchUnit 規則增強](#adr-002-archunit-規則增強)
- [ADR-006: 微服務拆分範圍界定](#adr-006-微服務拆分範圍界定)

---

## ADR-002: ArchUnit 規則增強

### 狀態
✅ **已批准** - 2026-02-02

### 上下文

SmartAdmin v4.0.0 有 545 行 ArchUnit 規則 (ArchitectureTest.java)，用於強制架構約束。微服務架構下，需要新增跨服務調用規則。

### 決策

**保留並增強 ArchUnit 規則**，適配微服務架構場景。

### 新增規則

#### 規則 1: Service 層禁止直接調用 Dubbo RPC

```java
@ArchTest
static final ArchRule serviceShouldNotCallRemoteServiceDirectly =
    noClasses()
        .that().resideInAPackage("..service..")
        .and().areNotAnnotatedWith(Component.class)  // 排除 @DubboService 實現類
        .should().dependOnClassesThat()
        .areAnnotatedWith(DubboReference.class)
        .because("Service 層禁止直接調用 Dubbo RPC，應通過 Manager 層封裝");
```

**設計原理**:
- **單一職責**: Service 層專注業務邏輯，不關心遠程調用細節
- **測試友好**: Service 層單元測試可以 Mock Manager 層
- **性能優化集中化**: Manager 層統一處理批量查詢、緩存策略

#### 規則 2: Manager 層可以調用 Dubbo RPC

```java
@ArchTest
static final ArchRule managerCanCallRemoteService =
    classes()
        .that().resideInAPackage("..manager..")
        .should().onlyAccessClassesThat(
            resideInAnyPackage(
                "..manager..",
                "..dao..",
                "..api..",        // Dubbo API 契約
                "java..",
                "io.vavr..",
                "org.springframework.."
            )
        )
        .because("Manager 層可以調用 Dubbo RPC、Dao 層、API 契約");
```

#### 規則 3: Dubbo DTO 必須實現 Serializable

```java
@ArchTest
static final ArchRule dtoMustBeSerializable =
    classes()
        .that().resideInAPackage("..api..domain..")
        .should().implement(Serializable.class)
        .because("Dubbo DTO 必須實現 Serializable 接口，確保網絡傳輸");
```

#### 規則 4: Dubbo API 必須使用 Vavr Option

```java
@ArchTest
static final ArchRule dubboApiMustUseVavrOption =
    methods()
        .that().areDeclaredInClassesThat().resideInAPackage("..api..")
        .and().arePublic()
        .should().haveRawReturnType(
            assignableTo(Option.class)
                .or(assignableTo(Try.class))
                .or(isPrimitive())
                .or(assignableTo(void.class))
        )
        .because("Dubbo API 必須使用 Vavr Option/Try，保證跨服務類型安全");
```

### 理由

1. **微服務架構約束**: 新增規則適配跨服務調用場景
2. **類型安全傳遞**: 確保 Vavr Option 全鏈路使用
3. **編譯時驗證**: 100% 強制執行，零人工成本

### 後果

- ✅ **質量保障**: 微服務架構下仍保持高質量
- ⚠️ **團隊學習**: 開發者需要理解新規則

---

## ADR-003: Vavr Option 跨服務使用

### 狀態
✅ **已批准** - 2026-02-02

### 上下文

SmartAdmin Service 層強制使用 `io.vavr.control.Option`，微服務化後需要決定 Dubbo API 是否繼續使用 Vavr Option。

### 決策

**保留並擴展 Vavr Option**，Dubbo API 也使用 `Option<T>` 返回類型。

### 理由

1. **全棧類型安全**: Service → RPC → Remote Service 全鏈路無 NullPointerException
2. **函數式組合**: 跨服務調用可以使用 flatMap 組合
3. **技術可行**: Dubbo 支持 Kryo 序列化 Vavr 類型

### 實施

```java
// Dubbo API 定義
@DubboService(version = "1.0.0")
public interface RemoteUserService {
    Option<UserDTO> getUserById(Long userId);  // ✅ Vavr Option
}

// Dubbo 序列化配置
dubbo:
  protocol:
    serialization: kryo  # Kryo 支持 Vavr 序列化
```

### 後果

- ✅ **類型安全**: 全鏈路 Option 保證
- ⚠️ **序列化開銷**: Kryo 序列化略慢於 Hessian (~5%)

---

## ADR-004: 數據庫漸進式拆分

### 狀態
✅ **已批准** - 2026-02-02

### 決策

**分階段數據庫拆分**: Phase 1 Schema 隔離 → Phase 2 獨立數據庫

### Phase 1: Schema 隔離 (前 3 個月)

```
PostgreSQL (smart_admin)
├── system_schema     → System Service
├── business_schema   → Business Service
├── job_schema        → Job Service
└── resource_schema   → Resource Service
```

**優勢**:
- ✅ 無需分布式事務
- ✅ 開發效率高
- ✅ 漸進式遷移

### Phase 2: 獨立數據庫 (後續)

**條件**: 業務穩定後實施
**引入**: Seata 分布式事務

### 理由

1. **風險可控**: Schema 隔離先驗證架構可行性
2. **成本較低**: 無需立即引入分布式事務
3. **漸進式演進**: 可隨時升級到獨立數據庫

---

## ADR-005: 構造器注入強制約束

### 狀態
✅ **已批准** - 2026-02-02

### 決策

**繼續強制構造器注入**，禁止 `@Autowired` 字段注入。

### 理由

1. ✅ **不可變性保證**: `private final` 字段
2. ✅ **單元測試友好**: 構造器注入
3. ✅ **循環依賴檢測**: 編譯時錯誤

---

## ADR-006: 微服務拆分範圍界定

### 狀態
✅ **已批准** - 2026-02-02

### 決策

**方案 A: 保守策略 - 保留單體核心 + 選擇性微服務化**

### 拆分範圍

**拆分為微服務**:
- ✅ Job Service (定時任務)
- ✅ Resource Service (文件/郵件)
- ✅ Workflow Service (LiteFlow，可選)

**保留在單體**:
- ❌ System Service (用戶/角色/菜單)
- ❌ Business Service (商品/品牌)

### 理由

1. **保留核心優勢**: Manager + ArchUnit + Vavr + Java 21
2. **降低複雜度**: 核心業務仍在單體
3. **成本可控**: $16,000 + $2,000/年

### 適用場景

- 團隊 < 15人
- 業務增長 < 3倍/年
- 運維能力有限

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-02
**維護**: 架構組
