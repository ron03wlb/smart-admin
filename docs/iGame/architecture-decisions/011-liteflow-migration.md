# ADR-011: 從 Evrete 遷移至 LiteFlow 流程編排引擎

**狀態**: ✅ 已採納

**日期**: 2026-01-23

**作者**: 後端團隊, 架構團隊

**審查者**: CTO, 產品團隊

**相關文檔**: [ADR-010: Evrete 規則引擎](./010-evrete-rules-engine.md), [P1-12: 獎金引擎](../technical-specs/P1-important/12-bonus-engine.md), [P1-11: VIP 系統設計](../technical-specs/P1-important/11-vip-system-design.md), [LiteFlow 遷移指南](../../plans/liteflow/migration-guide.md)

---

## 情境 (Context)

iGaming 平台在 ADR-010 中選擇了 Evrete 規則引擎，經過 3 個月的使用後，發現以下問題：

### 現有問題

**1. 複雜多步驟工作流支援不足**:
- VIP 升級流程：需要 7 個連續步驟（驗證積分 → 檢查存款 → 計算 GGR → 評估等級 → 發送通知 → 更新數據庫 → 觸發事件）
- Evrete 設計為單次規則評估（fact → rules → action），不適合有狀態的多步驟工作流
- 當前解決方案：在 Service 層手動編排 Evrete 規則調用（代碼耦合，難以維護）

**2. 數據庫持久化和熱加載缺失**:
- Evrete 規則硬編碼在 Java 類中（每次修改需要重新部署）
- 產品團隊無法通過 UI 修改規則（必須通過開發者）
- 規則變更週期：3 天（修改代碼 → QA → 部署）vs 預期 30 分鐘
- 實現了自定義 JSON 規則加載器，但功能有限且維護成本高

**3. 可視化和調試困難**:
- 複雜規則衝突難以調試（rule A 觸發 rule B 觸發 rule C，無法可視化執行路徑）
- 缺少執行日誌和審計追蹤（監管要求完整的規則執行歷史）
- 沒有監控面板（無法查看規則執行成功率、延遲、錯誤率）

**4. 腳本引擎支援有限**:
- Evrete 主要依賴 Java 註解和 DSL（學習曲線陡峭）
- 產品團隊要求使用類似 SQL 的表達式語言（易於學習和修改）
- 當前方案：使用 QLExpress，但與 Evrete 整合困難

### 業務需求變化

**原始需求（ADR-010 時期）**:
- 純規則評估（fact-based inference）
- 複雜的規則衝突解決（Rete 算法優勢）
- 低延遲（<10ms）

**當前需求（3 個月後）**:
- **工作流編排** > 規則推理（70% 場景是順序/條件執行，30% 才是複雜規則）
- 數據庫存儲 + 熱加載 + UI 管理（非技術人員修改規則）
- 可視化執行路徑 + 審計日誌（監管合規）
- 多種腳本引擎支援（QLExpress, Groovy, JavaScript）

### 約束條件

- **零停機遷移**: 不能影響現有業務運行
- **向後兼容**: 現有 Evrete 規則需要有遷移路徑
- **性能要求**: 執行延遲 <100ms p95（比 Evrete 的 <10ms 寬鬆，因為多步驟工作流本身需要更長時間）
- **學習成本**: 團隊需在 2 週內掌握 LiteFlow
- **時間窗口**: 6 週完成遷移（避免影響 Q2 產品路線圖）

### 成功標準

- ✅ 產品團隊可通過 UI 修改規則（30 分鐘內完成修改+測試+上線）
- ✅ 所有工作流支援可視化查看（執行路徑、節點狀態、執行日誌）
- ✅ 規則執行延遲 <100ms p95
- ✅ 100% 審計追蹤覆蓋（所有規則執行記錄到數據庫）
- ✅ 零業務中斷（遷移期間通過 feature flag 平滑切換）

---

## 決策 (Decision)

**我們將從 Evrete 遷移至 LiteFlow 2.15.3（Dromara 基金會流程編排引擎），作為 SmartAdmin 的核心業務規則和工作流引擎。**

### 關鍵組件

**1. LiteFlow 流程編排引擎**:
```java
@Service
@RequiredArgsConstructor
public class VipUpgradeService {
    private final LiteFlowExecutionService liteFlowExecutionService;

    public ResponseDTO<VipUpgradeResultVO> evaluateVipUpgrade(Long memberId) {
        // 執行 LiteFlow 鏈
        LiteFlowExecutionForm form = new LiteFlowExecutionForm();
        form.setChainCode("vip-upgrade-evaluation-chain");
        form.setInputParams(Map.of("memberId", memberId));

        ResponseDTO<LiteFlowExecutionResultVO> result =
            liteFlowExecutionService.execute(form);

        return result;
    }
}
```

**2. 數據庫持久化（PostgreSQL）**:
```sql
-- 鏈定義表
CREATE TABLE t_liteflow_chain (
    id BIGSERIAL PRIMARY KEY,
    chain_name VARCHAR(255) NOT NULL,
    chain_code VARCHAR(64) NOT NULL UNIQUE,
    chain_type INT DEFAULT 1,  -- 1: 主鏈, 2: 子鏈
    chain_data TEXT NOT NULL,  -- EL 表達式: THEN(a, b, c)
    version INT DEFAULT 1,
    is_enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 腳本節點表
CREATE TABLE t_liteflow_script (
    id BIGSERIAL PRIMARY KEY,
    script_name VARCHAR(255) NOT NULL,
    script_code VARCHAR(64) NOT NULL UNIQUE,
    script_type VARCHAR(32) DEFAULT 'qlexpress',  -- qlexpress, groovy, js
    script_data TEXT NOT NULL,  -- 腳本代碼
    version INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**3. EL 表達式鏈定義**:
```javascript
// VIP 升級評估鏈（數據庫存儲）
THEN(
  validateMember,           // 驗證會員資格
  calculatePoints,          // 計算積分
  IF(
    checkUpgradeEligibility, // 檢查升級資格
    THEN(                    // 符合條件
      upgradeVipTier,
      WHEN(sendEmail, sendSMS, updateCache), // 並行執行
      triggerUpgradeEvent
    ),
    logNoUpgrade            // 不符合條件
  )
)
```

**4. QLExpress 腳本節點**:
```sql
-- 腳本: checkUpgradeEligibility
INSERT INTO t_liteflow_script (script_name, script_code, script_type, script_data) VALUES
('檢查升級資格', 'checkUpgradeEligibility', 'qlexpress',
'// 從上下文獲取數據
member = context.getData("member");
points = context.getData("points");

// VIP Gold 升級條件
if (member.currentTier == "SILVER" &&
    points >= 10000 &&
    member.last30DaysDepositCount >= 10) {
    context.setData("upgradeResult", true);
    context.setData("newTier", "GOLD");
    return true;
}

context.setData("upgradeResult", false);
return false;');
```

**5. 執行日誌和指標**:
```sql
-- 執行日誌表
CREATE TABLE t_liteflow_execution_log (
    id BIGSERIAL PRIMARY KEY,
    chain_code VARCHAR(64) NOT NULL,
    request_id VARCHAR(64),
    input_params JSONB,
    output_result JSONB,
    execution_status INT,  -- 0: 失敗, 1: 成功
    execution_time_ms INT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_chain_code_status (chain_code, execution_status),
    INDEX idx_created_at (created_at)
);

-- 聚合指標表（每日統計）
CREATE TABLE t_liteflow_execution_metrics (
    metric_date DATE NOT NULL,
    chain_code VARCHAR(64) NOT NULL,
    total_executions BIGINT DEFAULT 0,
    success_count BIGINT DEFAULT 0,
    failure_count BIGINT DEFAULT 0,
    avg_execution_time_ms INT,
    p95_execution_time_ms INT,

    PRIMARY KEY (metric_date, chain_code)
);
```

**6. 管理 UI（Vue 3 Admin Panel）**:
- 鏈管理：CRUD 操作、版本管理、啟用/禁用
- 腳本管理：在線編輯、語法高亮、測試執行
- 監控面板：執行成功率、延遲分佈、錯誤日誌
- 熱加載：一鍵重載流程引擎（無需重啟服務）

### 實施方法

**階段式遷移（6 週計劃）**:

1. **Phase 1: 並行部署（Week 1-2）**
   - 安裝 LiteFlow 模組（與 Evrete 共存）
   - 選擇 1-2 個低風險流程作為試點（郵件通知、簡單驗證）
   - 驗證 LiteFlow 性能和穩定性

2. **Phase 2: 規則遷移（Week 3-4）**
   - 將 Evrete 規則轉換為 LiteFlow 鏈和腳本
   - 使用轉換表進行模式映射（見遷移指南）
   - 並行運行測試（Evrete vs LiteFlow 結果對比）

3. **Phase 3: 代碼重構（Week 5）**
   - 更新 Service/Manager 層調用 LiteFlow 而非 Evrete
   - 使用 feature flag 控制流量分配（10% → 50% → 100%）

4. **Phase 4: 全面測試（Week 5-6）**
   - 單元測試、整合測試、性能測試
   - 壓測驗證（500 TPS 吞吐量，P95 <100ms）

5. **Phase 5: 漸進上線（Week 6）**
   - 10% 流量（監控 3 天）→ 50% 流量（監控 5 天）→ 100% 流量
   - Evrete 保留為熱備（如有問題可立即回滾）

6. **Phase 6: Evrete 下線（Week 6+2）**
   - 100% 流量運行 2 週後，移除 Evrete 依賴
   - 歸檔 Evrete 規則到 docs/legacy/evrete/

---

## 結果 (Consequences)

### 正面影響

- ✅ **工作流編排能力**: LiteFlow 原生支援多步驟 DAG 執行（THEN, WHEN, IF, SWITCH），比 Evrete 更適合複雜業務流程
- ✅ **數據庫持久化**: 規則存儲在 PostgreSQL，支援版本控制、回滾、審計日誌（滿足監管要求）
- ✅ **熱加載機制**: 內置熱加載（無需重啟服務），規則修改立即生效
- ✅ **可視化和監控**: 執行日誌、指標統計、性能分析面板（提升運維效率）
- ✅ **多腳本引擎**: 支援 QLExpress, Groovy, JavaScript（降低學習曲線，產品團隊可用 QLExpress）
- ✅ **社區支持**: Dromara 基金會項目（活躍社區，持續更新）
- ✅ **SmartAdmin 整合**: 原生支援 SmartAdmin 架構模式（ResponseDTO, PageResult, 權限控制）

### 負面影響

- ❌ **學習曲線**: 團隊需學習 LiteFlow EL 表達式和新的編排概念（預估 2 週培訓）
- ❌ **遷移成本**: 6 週遷移時間（包含轉換、測試、上線）
- ❌ **性能妥協**: 執行延遲 <100ms（vs Evrete <10ms），但對多步驟工作流來說可接受
- ❌ **規則推理能力弱化**: LiteFlow 不是純規則引擎（無 Rete 算法），複雜規則衝突解決能力較弱（但實際業務中 70% 場景不需要）

### 風險

- ⚠️ **遷移過程中的業務中斷**:
  - **緩解**: 使用 feature flag 漸進式遷移（10% → 50% → 100%），保留 Evrete 作為熱備

- ⚠️ **規則轉換錯誤（Evrete → LiteFlow 語義不一致）**:
  - **緩解**: 並行運行測試（同樣輸入，對比 Evrete 和 LiteFlow 輸出），100% 功能等價性驗證

- ⚠️ **性能退化（延遲增加）**:
  - **緩解**: 壓測驗證 P95 <100ms，二級緩存（Caffeine + Redis）優化熱點鏈

- ⚠️ **LiteFlow 社區支持不足（vs Drools 企業級）**:
  - **緩解**: Dromara 基金會項目（國內最大開源社區），活躍維護，文檔完善

### 成效指標

| 指標 | Evrete（現狀） | LiteFlow（目標） | 改善幅度 |
|------|---------------|-----------------|---------|
| **規則修改週期** | 3 天 | 30 分鐘 | **99% 減少** |
| **非技術人員可編輯** | ❌ 否 | ✅ 是（UI + QLExpress） | **質的飛躍** |
| **執行延遲 P95** | <10ms | <100ms | **可接受** |
| **可視化支援** | ❌ 無 | ✅ 完整（執行路徑、日誌、指標） | **新增能力** |
| **熱加載支援** | ⚠️ 自定義（不穩定） | ✅ 內置 | **穩定性提升** |
| **審計日誌覆蓋** | 0% | 100% | **監管合規** |

---

## 替代方案 (Alternatives Considered)

### 替代方案 1: 繼續使用 Evrete + 增強自定義功能

**描述**: 保留 Evrete，自行開發數據庫持久化、熱加載、UI 管理功能

**優點**:
- ✅ 無遷移成本（不需要轉換規則）
- ✅ 保留 Rete 算法優勢（複雜規則推理）
- ✅ 團隊已熟悉 Evrete API

**缺點**:
- ❌ 高昂的開發和維護成本（估計 3-4 人月開發 + 持續維護）
- ❌ Evrete 架構不適合多步驟工作流（需要大量 workaround）
- ❌ 自定義功能質量難以保證（vs 成熟的 LiteFlow 功能）
- ❌ 社區支持有限（Evrete 社區較小）

**拒絕理由**: **"自建輪子"成本過高，且 Evrete 本質不適合工作流編排**。LiteFlow 提供開箱即用的完整解決方案（數據庫存儲、熱加載、監控、UI），成熟度和維護性遠超自研。

---

### 替代方案 2: 使用 Drools 規則引擎

**描述**: 遷移至 Drools（企業級規則引擎，Red Hat 支持）

**優點**:
- ✅ 企業級支持（Red Hat 商業支持）
- ✅ 強大的 CEP（複雜事件處理）能力
- ✅ 成熟的規則推理（Rete 算法）
- ✅ 大量文檔和案例

**缺點**:
- ❌ 重量級（15MB JAR 依賴 vs LiteFlow 500KB）
- ❌ 學習曲線陡峭（DRL 語法複雜）
- ❌ 性能開銷（規則編譯 100ms vs LiteFlow 10ms）
- ❌ 仍不適合工作流編排（Drools 是規則引擎，不是工作流引擎）
- ❌ 缺少內置數據庫存儲和熱加載（需要整合 Drools Workbench，部署複雜）

**拒絕理由**: **Drools 過於重量級且仍無法解決工作流編排問題**。我們需要的是流程編排引擎（DAG 執行），而非更強的規則推理引擎。Drools 的 CEP 能力在當前場景中用不到（70% 場景是順序執行，不是複雜事件處理）。

---

### 替代方案 3: 使用 Camunda BPMN 工作流引擎

**描述**: 使用 Camunda（BPMN 2.0 標準工作流引擎）

**優點**:
- ✅ 標準化（BPMN 2.0 OMG 標準）
- ✅ 可視化設計器（拖拽式流程設計）
- ✅ 強大的工作流編排能力
- ✅ 企業級支持

**缺點**:
- ❌ 需要額外基礎設施（Camunda 服務器，2GB 內存）
- ❌ 高延遲（HTTP API 調用，50-100ms）
- ❌ 重量級（適合長時間運行的業務流程，不適合毫秒級規則評估）
- ❌ 許可成本（Camunda Enterprise 需付費，約 $10K/年）
- ❌ 學習曲線（BPMN 建模需要專門培訓）

**拒絕理由**: **Camunda 設計用於有狀態的長時間運行工作流（訂單處理、審批流程），而非無狀態的快速規則評估**。我們的場景需要 <100ms 的低延遲執行，Camunda 的架構（外部服務 + HTTP 調用）引入了不必要的複雜度和延遲。LiteFlow 更輕量且滿足需求。

---

### 替代方案 4: 不採取任何行動（維持 Evrete 現狀）

**描述**: 接受現有問題，不進行遷移

**優點**:
- ✅ 無遷移成本（節省 6 週開發時間）
- ✅ 無變更風險（避免遷移過程中的潛在問題）

**缺點**:
- ❌ 產品團隊無法自助修改規則（持續依賴開發者，3 天週期）
- ❌ 監管合規問題（缺少審計日誌，監管審查時存在風險）
- ❌ 技術債累積（自定義 Evrete 整合代碼越來越複雜，維護成本持續上升）
- ❌ 競爭劣勢（競爭對手使用更靈活的規則系統，可快速響應市場變化）

**拒絕理由**: **監管合規和業務敏捷性要求我們必須改進**。缺少審計日誌在監管審查時是嚴重風險（罰款可達數百萬美元）。產品團隊要求的 30 分鐘規則修改週期是關鍵競爭力（促銷活動、市場響應），維持 3 天週期將失去市場機會。

---

## 相關決策

- [ADR-010: Evrete 規則引擎](./010-evrete-rules-engine.md) - **本決策替代 ADR-010**
- [ADR-001: 雙式記賬法](./001-double-entry-ledger-accounting.md) - 工作流中的財務交易記賬
- [ADR-006: 多租戶隔離](./006-multi-tenant-row-level-isolation.md) - 規則和鏈的租戶隔離

---

## 實施備註

### 時間線

- **提案日期**: 2026-01-23
- **採納日期**: 2026-01-23
- **實施開始**: 2026-02-03（Week 5）
- **預計完成**: 2026-03-17（Week 11）

### 受影響組件

| 組件 | 變更描述 | 影響程度 |
|------|---------|---------|
| **BonusRulesEngine** | 遷移至 LiteFlow 鏈 `bonus-evaluation-chain` | 高 |
| **VipRulesEngine** | 遷移至 LiteFlow 鏈 `vip-upgrade-evaluation-chain` | 高 |
| **FraudRulesEngine** | 遷移至 LiteFlow 鏈 `fraud-detection-chain` | 中 |
| **OrderService/Manager** | 調用 LiteFlowExecutionService 替代 Evrete | 中 |
| **sa-base/support/liteflow/** | 新增 LiteFlow 模組 | 新增 |
| **數據庫 Schema** | 新增 4 張表（chain, script, log, metrics） | 低 |

### 遷移策略

詳見 [LiteFlow 遷移指南](../../plans/liteflow/migration-guide.md)

**概要步驟**:
1. **Week 5-6**: 並行部署 LiteFlow（與 Evrete 共存）
2. **Week 7-8**: 轉換規則（Evrete → LiteFlow）
3. **Week 9**: 代碼重構（Service/Manager 層）
4. **Week 10**: 測試驗證（功能、性能、壓測）
5. **Week 11**: 漸進上線（10% → 50% → 100%）
6. **Week 13+**: Evrete 下線（運行 2 週後移除依賴）

**回滾計劃**:
```yaml
# 緊急回滾（設置流量為 0%，全部流量回到 Evrete）
smart:
  liteflow:
    migration:
      percentage: 0  # 立即回滾
```

---

## 參考資料

- [LiteFlow 官方文檔](https://liteflow.cc/)
- [LiteFlow GitHub](https://github.com/dromara/liteflow)
- [Dromara 基金會](https://dromara.org/)
- [LiteFlow 模組實施計劃](../../plans/liteflow/implementation-plan.md)
- [LiteFlow 遷移指南](../../plans/liteflow/migration-guide.md)
- [LiteFlow 架構文檔](../../plans/liteflow/architecture.md)
- [Evrete vs LiteFlow 性能對比](../../plans/liteflow/migration-guide.md#evrete-vs-liteflow-comparison)

---

## 審查歷史

| 日期 | 審查者 | 評論 | 結果 |
|------|-------|------|------|
| 2026-01-23 | 產品團隊 | 確認 30 分鐘規則修改週期滿足業務需求 | ✅ 批准 |
| 2026-01-23 | 後端團隊 | 驗證 LiteFlow 性能測試（P95 <100ms） | ✅ 批准 |
| 2026-01-23 | CTO | 批准遷移計劃，要求 2 週運行後再下線 Evrete | ✅ 批准 |

---

## 備註

**為什麼不保留 Evrete 與 LiteFlow 雙引擎？**

ADR-010 曾計劃 "Evrete 為主 + Drools 為輔"，但實踐證明雙引擎維護成本過高：
- 團隊需要同時掌握兩套 API（學習成本翻倍）
- 規則分散在兩個引擎（管理複雜度高）
- 需要決策邏輯（何時用 Evrete，何時用 LiteFlow？容易出錯）

**最終決定：LiteFlow 單引擎**，覆蓋 95% 場景。如果未來遇到 LiteFlow 無法處理的複雜規則推理（<5% 場景），再考慮引入 Drools 作為補充（漸進式演進）。

**LiteFlow 是否支援複雜規則衝突解決？**

LiteFlow 不是純規則引擎（無 Rete 算法），但通過以下機制處理規則邏輯：
- **優先級控制**: 鏈節點按順序執行，可通過 `THEN` 控制執行順序
- **條件分支**: 使用 `IF`, `SWITCH` 處理條件邏輯
- **腳本節點**: 在 QLExpress 腳本中實現複雜邏輯（支援變量、函數、條件判斷）

實踐中，iGaming 平台的 70% 規則是順序執行或簡單條件分支，30% 是複雜邏輯但可在腳本中實現，真正需要 Rete 算法的場景 <5%。

**性能影響評估**

| 場景 | Evrete | LiteFlow | 差異 |
|------|--------|----------|------|
| **簡單規則評估** | 5ms | 15ms | +10ms（可接受） |
| **多步驟工作流** | 50ms（手動編排） | 80ms（內置編排） | +30ms（換取可維護性） |
| **複雜規則推理** | 8ms | 不適用 | LiteFlow 不支持 |

結論：對於多步驟工作流場景（70%），LiteFlow 的編排能力帶來的可維護性提升遠超 30ms 延遲增加。對於簡單規則評估（30%），15ms 仍在 <100ms 目標內。

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-23
**維護者**: 1024創新實驗室
