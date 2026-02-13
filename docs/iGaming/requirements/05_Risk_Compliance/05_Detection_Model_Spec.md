# 檢測模型規格（Detection Model Specification）

> **Canonical Source**: [05-02-01_Detection_Model.md](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md)
> **Audience**: 高階主管、風險營運、合規官、產品經理
> **Related Architecture**: [Detection_Model_Implementation.md](../../architecture/05_Risk_Engine/03_Detection_Model_Implementation.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: 技術細節（TCC 模式、SAGA 流程實現）已移至架構層。本文檔專注於業務規則和營運政策。

---

## 1. 執行摘要（Executive Summary）

SmartAdmin iGaming v2.1.0 引入了配置驅動風險控制系統（Configuration-Driven Risk Control System），允許營運團隊配置每個風險規則的處理方式——無需程式碼更改或新部署。

### 核心價值主張（Core Value Proposition）

| 功能 | 描述 | 業務價值 |
|---------|-------------|----------------|
| 配置驅動 | 規則行動類型（BLOCK / FLAG / IGNORE）通過資料庫設定管理 | 無需程式碼更改即可調整風險策略 |
| 以人為本的審核 | 異常生成風險提案供人工審核 | 簡化決策邏輯；避免過度自動化 |
| 平等待遇 | 所有玩家都經過風險檢查（無 VIP 豁免） | 監管合規；公平原則 |
| 營運商選擇 | 營運商決定自己的風險容忍度 | 支援多市場策略；提升客戶滿意度 |

---

## 2. 從傳統風險控制的演進（Evolution from Traditional Risk Control）

| 維度 | 傳統風險（v2.0.0） | 配置驅動風險（v2.1.0） |
|-----------|--------------------------|--------------------------------------|
| 規則分類 | 硬編碼 P0/P1/P2（30%/50%/20%） | 配置驅動（營運商為每條規則選擇 BLOCK/FLAG） |
| VIP 豁免 | VIP Level 3+ 豁免風險檢查（不合規） | 所有玩家平等對待（無豁免） |
| 異常處理 | 複雜的自動門檻邏輯 | 人工審核作為主要決策機制 |
| 策略調整 | 需要程式碼更改 + 部署 | 僅需資料庫配置更新 |
| 靈活性 | 固定百分比，不可調整 | 每條規則可獨立配置 |

---

## 3. 風險規則行動類型（Risk Rule Action Types）

每條風險規則可以配置為三種行動類型之一：

| 行動類型 | 行為 | 使用案例 |
|-------------|-----------|----------|
| **BLOCK** | 在投注成功後生成高優先級風險提案（異步） | 嚴重欺詐模式——機器人檢測、同場對沖、同 IP 套利 |
| **FLAG** | 在投注成功後生成中優先級風險提案（異步） | 可疑但風險較低的模式——跨場對沖、低賠率流水操縱 |
| **IGNORE** | 僅記錄事件；不生成提案 | 實驗性規則、數據收集規則 |

**重要**：BLOCK 規則不會拒絕投注。所有投注首先被處理；風險分析隨後異步運行。資金攔截發生在提款階段。

---

## 4. 檢測層級（Detection Layers）

系統在五個層級運作，每個層級都有特定的業務規則：

### Layer 1: 同步阻斷（Synchronous Blocking，即時，<10ms）

只有最嚴重的情況會即時阻斷：

| 場景 | 描述 |
|----------|-------------|
| 黑名單玩家 | 確認的欺詐者；投注立即拒絕 |
| IP 封鎖 | 已知攻擊來源 |
| 帳戶凍結 | 正在人工審核中 |
| 自我排除名單 | 監管要求（UKGC/MGA） |

### Layer 2: 交易處理（Transaction Processing）

投注使用兩階段交易模式處理。玩家立即看到投注結果。風險分析不會延遲或影響投注體驗。

→ **[TCC Pattern Implementation](../../architecture/05_Risk_Engine/03_Detection_Model_Implementation.md#transaction-processing)** - Try-Confirm-Cancel 技術細節

### Layer 3: 異步風險分析（Asynchronous Risk Analysis，約 5 秒內）

規則在投注成功後評估。檢測到的模式生成風險提案：

**BLOCK 規則（高優先級）**：

| 規則 | 描述 |
|------|-------------|
| 機器人檢測 | 行為模式分析識別自動投注 |
| 同場對沖 | 同一玩家在同一場賽事下相反投注 |
| 同 IP 套利 | 來自同一 IP 的關聯帳戶進行對沖投注 |
| 異常賠率檢測 | 賠率選擇模式的統計異常 |
| 流水操縱 | 人為生成流水以滿足提款要求 |

**FLAG 規則（中優先級）**：

| 規則 | 描述 |
|------|-------------|
| 跨場對沖 | 跨不同賽事的相反投注（風險較低） |
| 低賠率流水 | 賠率低於 1.5 的投注（需要人工判斷） |
| 異常投注模式 | 不尋常的模式，可能是誤報 |
| 高頻投注 | 每分鐘超過 10 次投注（需要趨勢觀察） |

### Layer 4: 人工審核與處置（Human Review and Disposition）

風險提案由人工營運人員審核。三種可能結果：

| 決策 | 行動 |
|----------|--------|
| 通過（Approved） | 不採取行動；玩家繼續被監控 |
| 拒絕（Rejected） | 凍結帳戶；標記可疑資金；更新風險檔案 |
| 部分（Partial） | 應用部分帳戶凍結 |

### Layer 5: 提款延遲檢查（Withdrawal Deferred Check）

當玩家請求提款時，系統查詢歷史風險提案（30 天窗口）：

→ **[SAGA Implementation](../../architecture/05_Risk_Engine/03_Detection_Model_Implementation.md#withdrawal-deferred-check)** - SAGA Step 2.5 技術流程

| 條件 | 結果 |
|-----------|---------|
| 可疑金額 = 0 | 提款正常進行 |
| 可疑金額 > 0 | 凍結資金；生成人工審核提案 |

---

## 5. 關鍵設計原則（Key Design Principles）

| 原則 | 描述 | 業務收益 |
|-----------|-------------|------------------|
| 最小同步阻斷 | 僅黑名單 / IP 封鎖 / 帳戶凍結同步檢查 | 對投注的延遲影響接近零 |
| 投注優先完成 | 投注總是在風險分析運行前成功 | 無誤拒；對無辜玩家零影響 |
| 異步風險分析 | 所有 BLOCK/FLAG 規則在投注後異步運行 | 5 秒分析窗口；無投注延遲 |
| 以人為主的審核 | 自動化生成提案；人工做最終決策 | 避免 ML 模型誤報 |
| 事後資金攔截 | 在提款時攔截可疑資金 | 符合行業最佳實踐 |

---

## 6. 風險門檻和政策（Risk Thresholds and Policies）

### 風險評分模型（Risk Score Model）

系統不使用複合風險評分進行單次投注決策。相反，每條規則根據其配置的行動類型獨立觸發提案。

### 提款風險聚合（Withdrawal Risk Aggregation）

| 時間窗口 | 範圍 |
|-------------|-------|
| 30 天 | 窗口內所有未解決的風險提案 |
| 計算 | 所有匹配提案的可疑金額總和 |
| 門檻 | 任何可疑金額 > 0 觸發審核 |

### 行業基準（Industry Benchmarks）

| 營運商 / 監管機構 | 實踐 |
|----------------------|----------|
| DraftKings / FanDuel (US) | 僅黑名單同步；其他均為異步 |
| Bet365 (UK) | 對沖檢測在投注後 5 分鐘內分析 |
| UKGC Compliance Framework | 建議事後風險控制 + 提款攔截 |

---

## 7. 與先前架構的比較（Comparison with Previous Architecture）

| 指標 | 先前（v2.1.0 同步） | 當前（v3.0.0 異步） |
|--------|------------------------|------------------------|
| 風險觸發時機 | 投注請求期間（同步） | 投注成功後（異步） |
| BLOCK 規則處理 | 拒絕投注 | 生成高優先級提案 |
| FLAG 規則處理 | 允許投注 + 生成提案 | 生成中優先級提案 |
| 黑名單檢查 | 與其他規則混合 | 獨立同步檢查層 |
| 資金攔截時機 | 投注時（阻斷） | 提款時（延遲） |
| 誤報影響 | 5--10% 的正常玩家被拒絕 | 零誤拒（投注已成功） |
| 系統可用性 | 單點故障（風險宕機 = 投注失敗） | 高可用性（風險宕機不影響投注） |

---

## 8. 驗收標準（Acceptance Criteria）

- 所有玩家無論 VIP 狀態如何都經過相同的風險檢查
- BLOCK/FLAG 行動類型可在無需程式碼部署的情況下按規則配置
- 投注永不會被異步風險規則拒絕（零誤殺）
- 風險提案在投注完成後約 5 秒內生成
- 提款檢查查詢 30 天窗口內所有未解決提案
- 所有配置更改記錄在審計日誌中

---

## 9. 相關文檔（Related Documents）

- [05-02-02 Rule Configuration](../../source-archive/05_Risk_Control/05-02-02_Rule_Configuration.md) -- 風險提案服務和延遲檢查
- [05-02-03 ML Integration](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) -- 多維度風險規則
- [05-02-04 Operations Tools](../../source-archive/05_Risk_Control/05-02-04_Operations_Tools.md) -- SmartAdmin 架構映射和監控
