# 05-02 欺詐檢測 (Fraud Detection)

## 文檔信息

| 屬性 | 值 |
|------|-----|
| **文檔版本** | 4.1.0 |
| **最後更新** | 2026-02-07 |
| **維護團隊** | Risk Team & Backend Team |
| **文檔類型** | 索引文檔 |

**前置依賴**:
- [05-01 風控框架](./05-01_Risk_Framework.md) - 配置驅動風控規則引擎 (§9)
- [01-05 出金風控](../01_Player_Center/01-05_Withdrawal_Risk.md) - SAGA Step 2.5 延遲風控檢查
- [02-04 流水計算](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - Layer 1 處理流程

---

## 執行摘要

SmartAdmin iGaming v2.1.0 引入「配置驅動風控系統」(Configuration-Driven Risk Control System)，允許運營方自行配置每條風控規則的處理方式（實時阻斷 BLOCK vs 延遲檢查 FLAG），實現靈活的風控策略管理。

### 核心特性

| 特性 | 說明 | 業務價值 |
|------|------|---------|
| **配置驅動** | action_type 由數據庫配置決定（BLOCK/FLAG/IGNORE） | 無需修改代碼即可調整風控策略 |
| **人工為主** | 異常時生成風控提案，人工審核處理 | 簡化決策邏輯，避免過度自動化 |
| **平等對待** | 所有玩家都經過風控（無 VIP 豁免） | 合規要求，公平性原則 |
| **客戶選擇** | 運營方自行決定風控強度 | 提升客戶滿意度，支持多市場策略 |

### 與傳統風控的差異

| 維度 | 傳統風控 (v2.0.0) | 配置驅動風控 (v2.1.0) |
|------|-----------------|---------------------|
| **規則分類** | 硬編碼 P0/P1/P2 (30%/50%/20%) | 配置驅動（客戶自選 BLOCK/FLAG） |
| **VIP 豁免** | ❌ VIP Level ≥3 豁免風控 (違規做法) | ✅ 所有玩家平等（無豁免） |
| **異常處理** | 複雜自動化閾值判斷 | 人工審核為主 |
| **策略調整** | 需要修改代碼 + 發布 | 僅需更新配置表 |
| **靈活性** | 固定百分比，無法調整 | 每條規則獨立配置 |

---

## 子文檔導航

本文檔已拆分為 4 個專門子文檔，以提高可讀性和維護性：

| 編號 | 文檔名稱 | 內容概述 | 目標讀者 |
|------|---------|---------|---------|
| **05-02-01** | [檢測模型](./05-02-01_Detection_Model.md) | 系統概述、配置驅動概念、5 層架構、設計原則 | 架構師、技術主管 |
| **05-02-02** | [規則配置](./05-02-02_Rule_Configuration.md) | 風控提案服務、延遲檢查機制、SAGA Step 2.5 整合 | 後端開發、風控團隊 |
| **05-02-03** | [ML 整合](./05-02-03_ML_Integration.md) | 多維度風控規則、遊戲類型配置、玩家風險分級 | 風控團隊、數據分析師 |
| **05-02-04** | [運營工具](./05-02-04_Operations_Tools.md) | SmartAdmin 架構映射、性能優化、監控告警、測試策略 | 後端開發、SRE |
| **05-02-05** | [多帳戶檢測](./05-02-05_Multi_Account_Detection.md) | 設備指紋、家庭網絡、帳戶關聯、自我排除逃避檢測 | 風控團隊、合規 |

---

## 快速參考

### 系統架構概覽

```
Layer 1: 同步黑名單快速檢查 (<10ms)
    ↓
Layer 2: TCC 交易處理 (投注成功)
    ↓
Layer 3: 異步風控分析 (~5s, Kafka + Flink)
    ↓
Layer 4: 人工審核與處置
    ↓
Layer 5: 提款時延遲檢查 (SAGA Step 2.5)
```

### 關鍵表結構

| 表名 | 用途 | 詳細說明 |
|-----|------|---------|
| `t_risk_proposal` | 風控提案表 | [→ 05-02-02](./05-02-02_Rule_Configuration.md#11-提案數據模型) |
| `t_risk_rule_config` | 風控規則配置 | [→ 05-02-03](./05-02-03_ML_Integration.md#1-遊戲類型維度配置) |
| `t_player_risk_profile` | 玩家風險檔案 | [→ 05-02-03](./05-02-03_ML_Integration.md#3-個別玩家風險分級) |

### 提案優先級規則

| 優先級 | 觸發條件 | 處置方式 |
|-------|---------|---------|
| **URGENT** | 金額 > $10,000 或黑名單/IP封禁 | 立即阻斷 |
| **HIGH** | 金額 > $5,000 或機器人檢測 | 阻斷並人工審核 |
| **MEDIUM** | 金額 > $1,000 或異常投注 | 人工審核 |
| **LOW** | 金額 ≤ $1,000 | 正常監控 |

### SmartAdmin 架構映射

| 層級 | 類名 | 職責 |
|-----|------|------|
| **Controller** | `RiskProposalController` | API 接口 |
| **Service** | `RiskProposalService` | 業務邏輯（Vavr Option） |
| **Manager** | `RiskProposalManager` | 事務管理（@Transactional） |
| **Dao** | `RiskProposalDao` | 數據訪問 |
| **Entity** | `RiskProposalEntity` | 數據實體 |

---

## 核心 API

### 查詢待審核提案

```
GET /risk/proposal/player/{playerId}/pending?days=30
```

**權限**: `risk:proposal:query`

### 批准風控提案

```
POST /risk/proposal/{proposalId}/approve
```

**權限**: `risk:proposal:approve`

### 分頁查詢提案

```
POST /risk/proposal/query
```

**權限**: `risk:proposal:query`

---

## 監控指標快覽

| 指標 | 告警閾值 | 說明 |
|-----|---------|------|
| `risk_proposal_pending_count` | >100 | 待審核提案積壓 |
| `risk_query_duration_ms (P95)` | >200ms | 查詢性能下降 |
| `risk_cache_hit_rate` | <90% | 緩存命中率下降 |
| `risk_kafka_event_publish_errors` | >10/min | Kafka 發布失敗 |

詳細監控配置請參考 [05-02-04 運營工具](./05-02-04_Operations_Tools.md#3-監控與告警)。

---

## 變更日誌 (Change Log)

### v4.1.0 (2026-02-07)

**新增子文檔**：
- 新增 05-02-05 多帳戶檢測 (設備指紋、家庭網絡、帳戶關聯分析)
- 涵蓋 UKGC LCCP 17.1.1 自我排除逃避檢測
- Gamstop 整合規範

### v4.0.0 (2026-02-07)

**文檔拆分重構**：
- 將 2,203 行文檔拆分為 4 個專門子文檔
- 保留索引文檔作為導航入口
- 改善可讀性和維護性

### v3.0.0 (2026-02-05)

**重大變更 - 規則獨立觸發模式**：
- 取款審核從「分數累加」改為「按優先級判斷」
- 可疑金額按優先級分別計算
- 審計追溯性提升（記錄具體觸發規則）

### v2.0.0 (2026-02-04)

**重大變更 - 架構優化**：
- 風控系統從同步阻斷改為異步分析 + 事後處置
- BLOCK 規則不再拒絕投注，改為生成高優先級提案
- 新增 5 層防護體系（Layer 1-5）

### v1.0.0 (2026-02-02)

**初始版本**：
- 完整配置驅動風控系統設計
- SmartAdmin 五層架構映射
- 性能優化與監控告警

---

## 相關文檔

- [05-01 風控框架](./05-01_Risk_Framework.md) - 風控系統整體架構
- [01-05 出金風控](../01_Player_Center/01-05_Withdrawal_Risk.md) - 出金 SAGA 流程
- [02-04 流水計算](../02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 流水與對賬分析
