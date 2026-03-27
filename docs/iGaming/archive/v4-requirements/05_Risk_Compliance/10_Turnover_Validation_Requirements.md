# 流水驗證需求（Turnover Validation Requirements）

> **規範來源**: [05-07_Turnover_Validation_Scheme.md](../../source-archive/05_Risk_Control/05-07_Turnover_Validation_Scheme.md)
> **目標讀者**: 高階主管、產品經理、風險營運、合規官
> **相關架構**: [Turnover_Validation_Architecture.md](../../architecture/05_Risk_Engine/10_Turnover_Validation_Architecture.md)
> **最後同步**: 2026-02-08

---

## 1. 問題陳述（Problem Statement）

### 1.1 長週期流水驗證效能（Long-Cycle Turnover Validation Performance）

當玩家長時間未提款（例如 3-5 年），系統必須計算自上次提款以來的所有流水。傳統方法需要掃描數百萬筆投注記錄，導致：

| 問題 | 業務影響 |
|---------|-----------------|
| 查詢時間 | 掃描多年數據可能超過 30 秒 |
| 資料庫壓力 | 全表掃描造成 I/O 尖峰 |
| 逾時風險 | 提款請求可能因查詢逾時而失敗 |

### 1.2 促銷風險控制缺口（Promotion Risk Control Gap）

目前系統缺乏足夠的優惠利用保護：

| 濫用模式 | 描述 |
|---------------|-------------|
| 低賠率流水（Low-Odds Turnover） | 玩家使用低風險投注完成流水需求（套利） |
| 對沖投注（Hedge Betting） | 對沖投注消除風險後提取優惠資金 |
| 缺少作廢機制（Missing Invalidation） | 無機制標記某些投注貢獻零流水 |

---

## 2. 檢查點快照解決方案（Checkpoint Snapshot Solution）

### 2.1 核心機制（Core Mechanism）

每次成功提款創建流水快照。後續提款驗證僅需計算當前累計流水與上次快照之間的差值 -- 無論經過多少時間。

**驗證公式（Validation Formula）**：

| 組件 | 定義 |
|-----------|------------|
| 當前總有效投注（Current Total Valid Bets） | 至今所有有效投注的即時彙總 |
| 上次快照有效投注（Last Snapshot Valid Bets） | 上次提款時記錄的累計有效投注 |
| **期間有效流水（Period Valid Turnover）** | **當前總計 - 上次快照總計** |

**效能**：驗證為 O(1) -- 無論距離上次提款多久，都是常數時間。

### 2.2 快照觸發類型（Snapshot Trigger Types）

| 觸發類型 | 何時創建 | 目的 |
|--------------|-------------|---------|
| WITHDRAWAL | 每次成功提款後 | 標準檢查點 |
| MANUAL | 營運商主動 | 例外處理或更正 |
| SCHEDULED | 定期批次處理 | 長期不活躍玩家的預防性快照 |

### 2.3 快照捕獲數據（Snapshot Data Captured）

每個快照在創建時記錄以下累計總計：

| 數據點 | 描述 |
|------------|-------------|
| 總投注金額（Total Bet Amount） | 所有投注的累計總額 |
| 總有效投注金額（Total Valid Bet Amount） | 計入有效流水的投注累計總額 |
| 總贏額（Total Win Amount） | 所有獎金的累計總額 |
| 快照時間戳（Snapshot Timestamp） | 快照拍攝的確切時間 |
| 觸發參考（Trigger Reference） | 提款訂單 ID 或其他觸發識別碼 |

---

## 3. 時間窗口與風險提案對齊（Time Window Alignment with Risk Proposals）

關鍵合規需求是流水驗證窗口和風險提案查詢窗口使用相同的時間邊界：

| 維度 | 查詢範圍 | 描述 |
|-----------|-------------|-------------|
| 風險提案查詢（Risk Proposal Query） | 上次快照時間至現在 | 自上次提款以來所有未解決提案 |
| 流水驗證（Turnover Validation） | 上次快照時間至現在 | 差值計算 |
| 長週期處理（Long-Cycle Handling） | 無固定天數限制 | 即使間隔 5 年，也查詢自上次快照以來的所有 URGENT/HIGH 提案 |

此對齊確保風險標記和流水計算始終參考同一時段，消除合規覆蓋的缺口。

---

## 4. 活動風險整合（雙層保護）

### 4.1 預防層（Pre-Emptive）

在投注前或投注期間應用的控制：

| 控制 | 機制 |
|---------|-----------|
| 領取攔截（Claim Interception） | 優惠發放前的 IP 檢查、設備指紋驗證 |
| 投注攔截 -- 低賠率（Bet Interception -- Low Odds） | 低於賠率閾值的投注貢獻零流水 |
| 投注攔截 -- 對沖（Bet Interception -- Hedging） | 偵測到的對沖投注對貢獻零流水 |

### 4.2 偵測層（Post-Hoc）

在提款或定期審核時應用的控制：

| 控制 | 機制 |
|---------|-----------|
| 提款預掃描（Withdrawal Pre-Scan） | 批准提款前檢查異常行為提案 |
| 風險規則觸發（Risk Rule Triggers） | 自動化規則標記可疑流水模式 |
| 報告與分析（Reporting and Analytics） | 活動 ROI 監控；濫用者識別名單 |

---

## 5. 無效流水規則（Invalid Turnover Rules）

以下投注行為對有效流水貢獻為零：

| 規則代碼 | 條件 | 理由 |
|-----------|-----------|-----------|
| LOW_ODDS | 賠率低於 1.3 | 低賠率套利風險 |
| HEDGE_BET | 偵測到對沖投注組合 | 無風險套利 |
| MIN_BET_BONUS | 最小投注金額 + 活動流水 | 優惠追逐行為 |
| SAME_EVENT_OPPOSITE | 同一事件的反向投注 | 保證結果的投注 |

---

## 6. UI 需求

### 6.1 提案審核頁面（Proposal Review Page）

- 突出標籤：「關聯風險」、「活動套利」、「優惠追逐」
- 顯示關聯促銷名稱和流水完成進度

### 6.2 投注詳情頁面（Bet Details Page）

- 用標籤標記無效流水投注（例如「[低賠率] 流水：0.00」、「[對沖] 流水：0.00」）
- 為玩家提供有效流水百分比統計

---

## 7. 驗收標準（Acceptance Criteria）

| 需求 | 閾值 |
|-------------|-----------|
| 快照機制 | 每次成功提款後自動創建快照 |
| 驗證效能 | 流水驗證低於 50ms（P95），與時間跨度無關 |
| 無效流水 | 低賠率 / 對沖 / 優惠追逐投注計為零流水 |
| 時間對齊 | 風險提案查詢窗口 = 流水驗證窗口 = 上次快照至現在 |
| UI 警報 | 審核頁面顯示活動風險標籤；投注頁面標記無效流水 |

---

## 8. 合規需求（Compliance Requirements）

- 流水驗證必須平等應用於所有玩家，無論 VIP 狀態
- 所有快照創建事件必須記錄在稽核日誌中
- 無效流水規則必須可由營運商在無需更改程式碼的情況下配置
- 必須維持時間窗口對齊以用於監管報告目的

---

## 9. 相關文件（Related Documents）

- [05-01 Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) -- 配置驅動的風險規則引擎
- [05-05 Risk Proposal Workflow](../../source-archive/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) -- 提案生命週期管理
- [05-06 Withdrawal Risk Correlation](../../source-archive/05_Risk_Control/05-06_Withdrawal_Risk_Correlation.md) -- 提款風險評分

### 技術實施（Technical Implementation）

→ **[Turnover Validation Architecture](../../architecture/05_Risk_Engine/10_Turnover_Validation_Architecture.md)** - 檢查點快照機制、雙層保護演算法、無效流水偵測規則和即時驗證工作流程

---

## 10. 版本歷史（Version History）

| 版本 | 日期 | 變更 |
|---------|------|---------|
| 1.0.0 | 2026-02-05 | 初始版本 -- 檢查點快照定義、雙層保護、無效流水規則 |
