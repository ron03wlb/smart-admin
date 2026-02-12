# 有效投注額與遊戲對帳需求（Turnover & Game Reconciliation Requirements）

> **Canonical Source**: [source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md)
> **Audience**: 高階主管、合規官員、產品經理、財務團隊
> **Related Architecture**: [Turnover_Calculation_Architecture.md](../../architecture/02_Finance_Service/Turnover_Calculation_Architecture.md)
> **Last Synced**: 2026-02-09
>
> **精煉說明**：技術細節（三層驗證架構、Mermaid 流程圖、短路優化）已移至架構層。本文件僅專注於業務規則。

---

## 1. 有效投注額業務規則（Valid Turnover Business Rules）

### 1.1 有效投注額定義

**核心原則**：只有產生輸贏結果、承擔風險且通過風控驗證的投注才計入有效投注額。

**公式**：`ValidTurnover = BetAmount x GameWeight x OddsFactor x StatusFactor x RiskFactor`

- **RiskFactor**：`1`（通過）或 `0`（拒絕，例如檢測到對沖/套利）

### 1.2 投注狀態判定（Status Factor）

**前置條件**：投注必須通過 Layer 1 風控驗證後，才會套用狀態因子。

並非所有投注都計入投注額。每筆投注必須根據遊戲結果進行過濾：

| 狀態 | 說明 | 投注額計算 | 備註 |
|--------|-------------|---------------------|-------|
| **WIN** | 玩家贏 | 100% | 正常計算 |
| **LOSS** | 玩家輸 | 100% | 正常計算 |
| **DRAW / TIE** | 平局 / 推注 | **0%** | 無風險，不計入投注額 |
| **CANCEL / VOID** | 取消 / 作廢 | **0%** | 投注無效 |
| **HALF WIN** | 半贏 | **100%** | 標準本金法（v2.0.0 推薦） |
| **HALF LOSS** | 半輸 | **100%** | 標準本金法（v2.0.0 推薦） |
| **RUNNING** | 進行中 | 0% | 必須等待結算後才計算 |

**v2.0.0 重要變更**：
- HALF_WIN/HALF_LOSS 現在計為 100% 投注額（標準本金法）
- 「實際風險法」（50% 計算）已廢棄 —— 違反公平性原則
- 理由：相同投注行為應產生相同投注額貢獻，與風險鎖定邏輯一致

### 1.3 賠率門檻（Odds Factor）

為防止玩家利用低風險投注（Low Risk Betting）來洗投注額：

- **體育博彩要求**：
    - **歐洲盤（Decimal）**：>= 1.5（或 1.7，可配置）
    - **香港盤（HK）**：>= 0.5（或 0.7）
    - **馬來盤（MY）**：絕對值 > 0.5（負賠率完全計入）
    - **印尼盤（ID）**：<= -1.2（較高損失）或 >= 1.2

### 1.4 遊戲貢獻權重（Game Contribution Weight）

不同遊戲類型具有不同的「投注額洗刷難度」，需要加權貢獻：

| 遊戲類型 | 權重 | 理由 |
|-----------|--------|-----------|
| **Slots**（老虎機） | 100% | 純機率，適合洗投注額 |
| **Live Casino**（真人娛樂場） | 10-50% | 取決於營運策略；百家樂通常較低 |
| **Sports**（體育博彩） | 100% | 高風險 |
| **Lottery**（彩票） | 10-20% | 雙邊投注（大/小、奇/偶）容易對沖 |
| **PVP（牌類/對戰）** | 0% | 通常不計入，因為玩家對玩家轉帳 |

### 1.5 活動投注額計算（Bonus Turnover）

活動投注要求與一般有效投注額不同，規則更嚴格：

**主要差異**：
- **一般投注額**：全平台通用，門檻較低（例如賠率 0.5+ 計入）
- **活動投注額**：特定於紅利活動，門檻較高（例如僅特定遊戲、賠率 0.7+、最大貢獻上限）

**公式**：`ActivityTurnover = Min(BetAmount, MaxContribution) x GameContribution%`

**關鍵業務邏輯**：
1. **遊戲白名單**：如果遊戲不在活動允許清單中，貢獻強制為 0%（或可能完全阻止遊戲）
2. **單筆貢獻上限**：例如「每筆投注最多貢獻 $5 投注額」—— 防止玩家下單筆 $1,000 投注快速解鎖紅利
3. **多重紅利優先級（FIFO 原則）**：當玩家同時參與多個紅利時，投注額優先填充最早領取的紅利；或優先解鎖與鎖定現金錢包綁定的紅利

**狀態轉換**：
- `Pending`（進行中）-> `Completed`（投注額目標達成，餘額解鎖至現金）
- `Pending` -> `Expired`（已過期，紅利和獎金扣除）

---

## 1.6 免費旋轉投注額規則（Free Spins Turnover Rules）

### 1.6.1 核心原則

免費旋轉的**投注額（Turnover）**和**有效投注（Valid Bet）**必須分開處理，因為它們服務於完全不同的目的：

| 指標 | 定義 | 計算方法 | 目的 |
|--------|-----------|-------------------|---------|
| **Turnover**（投注額） | 流經遊戲的總金額 | **免費旋轉面額總和** | 財務報表、GGR 計算 |
| **Valid Bet**（有效投注） | 計入投注要求的金額 | **0（不計入）** | 紅利活動、返水計算 |

**為何投注額不是 0**：如果免費旋轉投注額記錄為 $0，GGR 計算會變得不準確。例如，發放 10 次面額 $1 的免費旋轉，玩家贏得 $8.50，會顯示 GGR 為 -$8.50（表面虧損），而非正確的 $1.50（實際促銷淨成本）。

### 1.6.2 行業標準

所有主要遊戲供應商採用此邏輯：

| 供應商 | Turnover | Valid Bet | 依據 |
|----------|----------|-----------|-------|
| **Evolution Gaming** | 面額總和 | 0 | 官方 API 文檔 |
| **Pragmatic Play** | 面額總和 | 0 | 官方 API 文檔 |
| **Hub88（聚合商）** | 面額總和 | 0 | 技術白皮書 |

**上市公司報告標準**（Evolution Gaming 2023 年度報告）：免費旋轉以面額記錄為投注額，以實際贏額記錄為支付，淨成本（面額減支付）記為營銷費用。

### 1.6.3 決策總結

**推薦方法**：Turnover = 面額總和，Valid Bet = 0

**理由**：
1. **財務準確性**：正確反映促銷成本
2. **GGR 計算**：符合會計標準
3. **行業標準**：所有主要遊戲供應商採用此邏輯
4. **上市合規**：符合財務報表披露要求

---

## 1.7 投注額驗證整合

本模組計算的 `valid_turnover_finance` 是一個增量值，最終服務於**方法 A：存款/提款快照法**。

- **資料流**：`valid_turnover_finance` -> `t_player_statistics.total_valid_turnover`（累積）
- **驗證時機**：當玩家請求提款時
- **驗證邏輯**：讀取 `total_valid_turnover`，減去 `Last_Snapshot`，與目標值比較

---

## 1.8 跨模組投注額一致性要求

為確保財務系統與活動系統之間的一致性，兩者必須共享統一的基礎驗證邏輯。

**關鍵原則**：
- 財務和活動模組必須使用一致的投注額計算規則
- 風險引擎驗證結果必須被財務和活動模組遵守
- 配置參數必須集中在統一配置服務中（禁止本地硬編碼）

**資料交換**：
- 財務模組計算 `valid_turnover_finance` 並發布到事件匯流排
- 活動系統消費此值並套用遊戲權重：`activity_valid_turnover = valid_turnover_finance x GAME_WEIGHTS[game_type]`

**配置同步**：
財務和活動模組必須從統一配置服務讀取以下參數：
1. **賠率門檻**：由風險引擎定義（EUR: 1.5, HK: 0.5, MY: 0.5, ID: 1.2）
2. **遊戲權重**：由活動模組定義（SLOTS: 1.0, SPORTS: 1.0, BACCARAT: 0.15 等）
3. **狀態因子**：由財務模組定義（WIN: 1.0, LOSS: 1.0, DRAW: 0.0 等）

→ **[三層驗證架構](../../architecture/02_Finance_Service/Turnover_Calculation_Architecture.md#three-layer-validation)** - 技術責任矩陣、效能優化、短路實作

---

## 2. 遊戲對帳業務規則（Game Reconciliation Business Rules）

遊戲對帳解決平台資料庫與遊戲供應商（GP）記錄之間的不一致問題。

### 2.1 三層對帳系統

#### Layer 1：即時串流檢查（Real-time Stream Check）
- **時機**：在接收每個 `GameEnd` 或 `Settlement` webhook 後 1-5 分鐘
- **機制**：透過 GP API（`GetTransactionStatus`）查詢單筆交易詳情；比較 `Amount`、`Status`、`WinLoss`
- **目的**：快速修復即時掉單（延遲問題）

#### Layer 2：近即時批次對帳（Near Real-time Batch Reconciliation）
- **時機**：每 10-30 分鐘執行
- **機制**：呼叫 GP 的 `FetchHistory` API（依時間範圍）；拉取過去 30 分鐘的所有投注；對 DB 執行 `Anti-Join`（找出 GP 有但 DB 沒有的記錄）
- **目的**：恢復丟失的回呼投注（自我修復）

#### Layer 3：T+1 每日最終結算（T+1 Daily Final Settlement）
- **時機**：每日凌晨（例如 02:00），在 GP 產生完整前一日報表後
- **機制**：下載 GP 結算檔案（CSV/XML/JSON）；載入暫存表；執行 `Full Outer Join` 比較：
    1. **GP 有、DB 沒有** -> 恢復（建立缺失交易）
    2. **GP 沒有、DB 有** -> 標記為異常（Invalid/Rollback）；需要人工確認
    3. **金額不符** -> 產生 `DiffReport`；根據 GP 最終結算金額調整

### 2.2 例外處理矩陣

| 例外場景 | 系統行為 | 解決方式 |
|--------------------|----------------|------------|
| **恢復（缺失投注）** | GP 有記錄但 DB 沒有 | 自動建立投注，補充扣款/派彩，記錄 Source="Reconciliation" |
| **金額差異** | 雙方金額不符 | 如果差異 < 容忍度（例如 0.01），自動平衡；否則 Alert |
| **狀態衝突** | DB=Win，GP=Loss | 以 **GP 報表**為準，執行 `Reverse + Re-settle` |
| **幽靈投注** | DB 有記錄，GP 沒有 | 極度危險（可能駭客注入）。**凍結帳戶**，需要人工調查。 |

---

## 3. 每日對帳驗證（Daily Reconciliation Verification）

### 執行時程
- **時間**：每日 03:00（UTC+8）

### 例外處理門檻

| 偏差範圍 | 門檻設定 | 業務影響 | 理由 |
|----------------|-------------------|-----------------|-----------|
| **< 0.01%** | 可接受範圍 | 幾乎無影響（玩家每日投注額 $1,000 -> 偏差 $0.10） | 浮點精度誤差、時區轉換錯誤、遊戲權重微調 |
| **0.01% - 1%** | 警告區 | 中度影響（可能配置錯誤） | 遊戲權重配置錯誤、狀態因子映射錯誤、對帳時間窗口不一致 |
| **> 1%** | 緊急區 | 嚴重影響（財務風險） | 系統錯誤、資料丟失、惡意攻擊、雙重扣款 |

### 升級流程
- **偏差 < 0.01%**：自動標記為已驗證
- **偏差 0.01% - 1%**：發送警報至 Slack #finance-ops 頻道
- **偏差 > 1%**：觸發 PagerDuty 緊急警報；需立即人工介入

### 自動校正規則

**觸發條件**：偏差介於 0.01%-1% 且符合以下任一條件：
- 對帳期間遊戲權重配置變更
- 狀態因子映射錯誤（HALF_WIN/HALF_LOSS 計算錯誤）
- 時區轉換導致邊界投注差異

**自動校正限制**：
- 僅適用於低風險偏差（0.01%-1%）
- 單日單玩家偏差金額 < $100
- 每個玩家每日最多 3 次自動校正；超過此數升級為人工審查
- 所有自動校正必須記錄在完整審計日誌中

### 補償機制

當對帳發現無法自動校正的偏差時，啟動補償機制：

| 偏差類型 | 補償方法 | 觸發條件 | 執行者 | SLA |
|---------------|--------------------|--------------------|----------|-----|
| **Finance < Activity** | 增加財務投注額 | 活動計算過高 | 自動補償 | 1 小時 |
| **Finance > Activity** | 增加活動投注額 | 活動計算過低 | 自動補償 | 1 小時 |
| **GP vs 平台不符** | 以 GP 為準調整平台記錄 | GP 報表與平台不一致 | 需人工核准 | 24 小時 |
| **負偏差（平台多扣）** | 退款至玩家錢包 | 平台扣款過多 | 需人工核准 | 12 小時 |
| **正偏差（平台少扣）** | 從玩家錢包扣款 | 平台扣款過少 | 需人工核准 + 風控審查 | 48 小時 |

### 補償監控 KPI

- **補償觸發率**：`(compensations_count / total_reconciliations) x 100%` —— 目標：< 0.1%
- **自動補償成功率**：>= 95%
- **人工核准回應時間**：P50 < 2 小時，P95 < 12 小時

---

## 4. 監控指標與 SLA

### 關鍵監控指標

| 指標 | 目標 | 說明 |
|--------|--------|-------------|
| 投注額計算延遲（P99） | < 100ms | 每筆投注端到端計算時間 |
| Risk Engine 呼叫成功率 | > 99.9% | Layer 1 驗證可用性 |
| 每日對帳偏差率 | < 0.01% | 可接受偏差門檻 |
| 事件發布成功率 | > 99.99% | 保證關鍵財務事件傳遞至下游系統 |

→ **[事件發布架構](../../architecture/02_Finance_Service/Turnover_Calculation_Architecture.md#event-publishing)**

### 警報規則

| 警報名稱 | 條件 | 嚴重性 | 通知 |
|-----------|-----------|----------|-------------|
| 投注額計算延遲高 | P99 > 500ms | WARNING | Slack: #finance-ops |
| Risk Engine 呼叫失敗 | 成功率 < 99% | CRITICAL | PagerDuty: finance-oncall |
| 每日對帳偏差 | 偏差率 > 0.01% | WARNING | Slack + Email: finance-team |
| 事件發布失敗 | 成功率 < 99.9% | CRITICAL | PagerDuty: finance-oncall |

---

## 5. 投注額計算要求（Turnover Calculation Requirements）

系統必須為每筆投注同時計算一般投注額和活動投注額。

**業務流程**：
1. 驗證投注狀態（非平局/取消）
2. 檢查賠率門檻
3. 使用 Risk Engine 驗證
4. 計算一般投注額
5. 如果玩家有活躍紅利：套用遊戲白名單檢查、貢獻上限，並計算活動投注額
6. 更新投注進度

→ **[技術流程圖](../../architecture/02_Finance_Service/Turnover_Calculation_Architecture.md#calculation-flow)** - Mermaid 流程圖、決策樹、錯誤處理

---

## 6. 變更紀錄（Change Log）

### v2.1.0 (2026-02-02)

**主要變更**：
1. 更新 Layer 1 處理以支援配置驅動的風控
   - 新增 action_type（BLOCK/FLAG/PASS）支援
   - BLOCK 規則即時阻擋（投注額 = 0）
   - FLAG 規則標記但允許（正常投注額計算 + 產生風險提案）

2. 整合風控系統 v2.1.0 配置驅動架構

### v2.0.0 (2026-01-29)

**主要變更**：
1. 釐清三層驗證架構責任
   - Layer 1 拒絕停止後續處理（不進入 Layer 2/3）
   - 定義清晰的責任矩陣

2. HALF_WIN/HALF_LOSS 標準化為 100% 投注額（標準本金法）

### v1.0.0 (2026-01-28)

**初始版本**：
- 投注額計算邏輯（第 1.1-1.6 節）
- 遊戲對帳邏輯（第 2.1-2.2 節）
- 免費旋轉投注額規則

---

## 相關文件（Related Documentation）

→ **[投注額計算邏輯（詳細）](../../architecture/02_Finance_Service/Turnover_Calculation_Logic_Detail.md)** - 投注狀態因子矩陣、遊戲權重表、HALF_WIN/HALF_LOSS 處理演算法

→ **[投注額流程圖](../../architecture/02_Finance_Service/Turnover_Flowcharts.md)** - 三層驗證架構、對帳流程和例外處理的視覺化工作流程圖

→ **[投注額系統實作](../../architecture/02_Finance_Service/Turnover_Implementation.md)** - 完整技術實作，包含 Java 程式碼、SQL schemas、Redis 快取策略和 Kafka 事件串流

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-08
**Maintenance Team**: 財務團隊與產品團隊
