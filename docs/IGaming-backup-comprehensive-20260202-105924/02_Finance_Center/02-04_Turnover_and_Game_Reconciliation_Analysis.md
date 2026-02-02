# 02-04 流水計算與遊戲對帳分析 (Turnover & Game Reconciliation Analysis)

> **三層風控架構定位**: **Layer 2 - 財務狀態因子**
> 本模塊負責根據遊戲結果 (WIN/LOSS/DRAW) 計算有效流水的狀態因子。
> 需依賴 Layer 1 (05-01) 完成風控驗證後才執行。
> 完整架構參見: [00-00 文檔地圖 §流水計算邏輯](../00_Concept_&_Analysis/00-00_Document_Map.md#-流水計算邏輯)

本文件詳細定義「有效流水 (Valid Turnover)」的計算邏輯以及「遊戲商對帳 (Game Reconciliation)」的完整流程，確保數據精確度與資金安全。

## 📚 補充資料

本文件提供流水計算的核心規則與業務邏輯。如需查看詳細的技術實作、流程圖與時序圖，請參考以下補充文檔：

- [流程圖與時序圖](./02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md) - Mermaid 圖表、系統互動時序圖
- [計算邏輯詳解](./02-04-diagrams/02-04-02_Calculation_Logic.md) - 詳細計算流程、邊界條件處理
- [實作細節與代碼](./02-04-diagrams/02-04-03_Implementation_Details.md) - 代碼範例、資料庫設計、API 介面

## 1. 流水計算邏輯 (Turnover Calculation Logic)

### 1.1 有效流水定義
**核心原則**：僅計算「產生輸贏結果」、「具備風險」且「通過風控驗證」的注單。
公式：`ValidTurnover = BetAmount * GameWeight * OddsFactor * StatusFactor * RiskFactor`
- **RiskFactor**: `1` (Pass) 或 `0` (Reject, e.g. Hedge/Arbitrage)

### 1.2 狀態判定 (Status Factor) - Layer 2 核心邏輯

**前置條件**: ✅ 必須先通過 Layer 1 風控驗證 (05-01 §3.1 ValidateBet)

並非所有下注都算流水，需根據遊戲結果進行過濾：

| 狀態 (Status) | 描述 | 流水計算 | 備註 |
| :--- | :--- | :--- | :--- |
| **WIN** | 玩家贏 | 100% | 正常計算 |
| **LOSS** | 玩家輸 | 100% | 正常計算 |
| **DRAW / TIE** | 和局/走水 | **0%** | 無風險，不計流水 |
| **CANCEL / VOID** | 取消/作廢 | **0%** | 注單無效 |
| **HALF WIN** | 贏半 | **100%** | ✅ 標準本金法 (v2.0.0 推薦) |
| **HALF LOSS** | 輸半 | **100%** | ✅ 標準本金法 (v2.0.0 推薦) |
| **RUNNING** | 進行中 | 0% | 必須等待結算 (Settled) 後才計算 |

> **v2.0.0 重要變更 (2026-01-28)**:
> - **HALF_WIN/HALF_LOSS 現在計入 100% 流水** (採用標準本金法)
> - **「實際風險法」(50% 計算) 已廢棄** - 違反公平性原則
> - **理由**: 相同投注行為應有相同流水貢獻,與風控鎖定邏輯一致
> - **詳細分析**: [體育博彩 Valid Bet 計算邏輯](../seamless_wallet_analysis/03_sports_betting_valid_bet_logic.md)

### 1.3 賠率門檻 (Odds Factor)
避免玩家透過低風險投注 (Low Risk Betting) 洗水。

- **體育博彩要求**：
    - **歐洲盤 (Decimal)**: >= 1.5 (或 1.7，視配置)
    - **香港盤 (HK)**: >= 0.5 (或 0.7)
    - **馬來盤 (MY)**: 絕對值 > 0.5 (負盤全算)
    - **印尼盤 (ID)**: <= -1.2 (輸得更多) 或 >= 1.2

- **邏輯實作**：

### 1.4 遊戲權重 (Game Contribution Weight)
不同遊戲類型的「刷水難度」不同，需設置權重。

| 遊戲類型 | 權重 | 說明 |
| :--- | :--- | :--- |
| **Slots (老虎機)** | 100% | 純機率，適合洗水 |
| **Live (真人)** | 10-50% | 視運營策略，百家樂通常較低 |
| **Sports (體育)** | 100% | 風險高 |
| **Lottery (彩票)** | 10-20% | 雙面盤 (大小單雙) 容易對押 |
| **PVP (棋牌/對戰)** | 0% | 通常不計，因涉及玩家間轉移 |

### 1.5 活動流水特別計算 (Activity Wagering Calculation)
區別於一般「有效流水 (Valid Turnover)」(用於返水/VIP)，活動流水 (Wagering Requirement) 具有更嚴格的限制與計算規則。

- **區別定義**：
    - **General Turnover**：全站通用，門檻低 (e.g. 賠率 0.5 以上即算)。
    - **Activity Wagering**：僅針對特定紅利活動，門檻高 (e.g. 僅特定遊戲、賠率 0.7 以上、有最高貢獻上限)。

- **計算公式**：
  `ActivityTurnover = Min(BetAmount, MaxContribution) * GameContribution%`

- **關鍵邏輯**：
  1.  **遊戲白名單 (Whitelist)**：若遊戲不在該活動允許列表中，貢獻度強制為 0% (甚至禁止遊玩)。
  2.  **單筆貢獻上限 (Max Contribution Cap)**：例如「每筆注單最多僅計算 $5 流水」。防止玩家用 $1000 一注梭哈快速解鎖。
  3.  **多重活動優先級 (Multiple Bonus Priority)**：
      - 若玩家同時參加 A 活動 (存送) 與 B 活動 (救援金)。
      - **FIFO 原則**：流水優先填補「最早領取」的活動。
      - **鎖定原則**：或者依據「資金鎖定」順序，先解鎖現金錢包綁定的活動。

- **狀態流轉**：
  `Pending` (進行中) -> `Completed` (流水達標，餘額解鎖 -> Cash)
  `Pending` -> `Expired` (過期，扣除 Bonus 與 Win)

---

## 1.6 免費旋轉流水計算 (Free Spins Turnover Calculation) ✅ v2.0.0

### 1.6.1 核心原則

**關鍵概念**: 免費旋轉的 **Turnover** 與 **Valid Bet** 必須分開處理,用途完全不同。

| 指標 | 定義 | 計算方式 | 用途 |
|------|------|---------|------|
| **Turnover** | 遊戲中流動的金額總和 | **免費旋轉面額總和** | 財務報表、GGR 計算 |
| **Valid Bet** | 計入流水要求的金額 | **0 (不計入)** | 優惠活動、返水計算 |

**為何 Turnover ≠ 0？**
```yaml
場景: 贈送 10 次免費旋轉，每次面額 $1
遊戲結果: 玩家贏了 $8.50

❌ 錯誤計算 (Turnover = 0):
  Turnover = $0
  Payout = $8.50
  GGR = $0 - $8.50 = -$8.50  ← 看起來像虧損

✅ 正確計算 (Turnover = 面額總和):
  Turnover = $10.00
  Payout = $8.50
  GGR = $10.00 - $8.50 = $1.50  ← 促銷淨成本
```text

### 1.6.2 業界標準

所有主流遊戲供應商均採用此邏輯:

| 供應商 | Turnover | Valid Bet | 依據 |
|-------|----------|-----------|------|
| **Evolution Gaming** | ✅ 面額總和 | 0 | 官方 API 文檔 |
| **Pragmatic Play** | ✅ 面額總和 | 0 | 官方 API 文檔 |
| **Hub88 (Aggregator)** | ✅ 面額總和 | 0 | 技術白皮書 |

**上市公司財報範例** (Evolution Gaming Annual Report 2023):
```yaml
"Free spins provided to players are recorded as:
 - Turnover: At the face value of the free spin
 - Payout: At the actual win amount
 - Marketing Expense: Net cost (face value - payout)"
```text

### 1.6.3 交易記錄設計


### 1.6.4 GGR 計算實現

```typescript
/**
 * 計算 GGR (包含免費旋轉成本)
 */
public calculateGGR(date: LocalDate): GgrReport {
    const transactions = this.transactionRepository.findByDate(date);

    let totalTurnover = 0;
    let totalPayout = 0;
    let freespinTurnover = 0;  // 促銷成本追蹤

    for (const tx of transactions) {
        // Turnover 計算 (包含免費旋轉面額)
        if (tx.transactionType.endsWith('_BET')) {
            totalTurnover += tx.turnover;

            if (tx.isFreeRpin) {
                freespinTurnover += tx.turnover;  // ✅ 記錄促銷成本
            }
        }

        // Payout 計算
        if (tx.transactionType.endsWith('_WIN')) {
            totalPayout += tx.amount;
        }
    }

    // GGR = Turnover - Payout
    const ggr = totalTurnover - totalPayout;

    return {
        date,
        totalTurnover,
        freespinTurnover,      // 促銷成本
        cashTurnover: totalTurnover - freespinTurnover,
        totalPayout,
        ggr
    };
}
```text

### 1.6.5 關鍵決策總結

✅ **推薦方案**: Turnover = 面額總和, Valid Bet = 0

**理由**:
1. **財務準確性**: 正確反映促銷成本
2. **GGR 計算**: 符合會計準則
3. **業界標準**: 所有主流 GP 都採用此邏輯
4. **上市合規**: 符合財報披露要求

**詳細分析**: [免費旋轉 Turnover 計算邏輯](../seamless_wallet_analysis/04_free_spins_turnover_calculation.md)

---

## 1.7 跨模組流水一致性保障 (Cross-Module Turnover Consistency)

為確保 **Finance System (財務系統, 本文件)** 與 **Activity System (活動系統, 04-01)** 的流水計算一致性,兩者必須共用統一的基礎驗證邏輯。本節定義財務模組在整體流水驗證架構中的角色與責任。

### 1.6.1 三層驗證架構中的財務層定位

財務模組負責 **Layer 2: Status-Based Turnover Adjustment**,在 Risk Engine 基礎驗證之上應用狀態因子 (Status Factor)。

#### 三層架構職責劃分 (v2.0.0 ✅)

**關鍵原則**: 每層僅負責自己的職責,拒絕決策由 Layer 1 統一處理,後續層級僅做數值調整。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   Unified Turnover Validation Stack                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Layer 1: Risk Validation (Risk Engine - 05-01)                            │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │ 職責: 拒絕決策 (Rejection Decision)                         │           │
│  │ 檢查: Hedge/Arbitrage/Low Odds/同 IP 對沖                   │           │
│  │ 輸出: { is_valid: boolean, effective_turnover_base: number }│           │
│  │                                                             │           │
│  │ ❌ is_valid = false → 直接返回 0 (不進入 Layer 2/3)        │           │
│  │ ✅ is_valid = true  → 返回 effective_turnover_base         │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                           ↓ (僅當 is_valid = true 時)                       │
│  Layer 2: Finance Layer (THIS MODULE - 02-04) ✅                           │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │ 職責: 狀態因子調整 (Status Factor Adjustment)              │           │
│  │ 檢查: WIN/LOSS/DRAW/CANCEL/HALF_WIN/HALF_LOSS               │           │
│  │ 輸出: valid_turnover_finance                                │           │
│  │     = effective_turnover_base × status_factor              │           │
│  │                                                             │           │
│  │ ⚠️ 不負責拒絕決策 (No Rejection Logic Here)                │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                           ↓                                                 │
│  Layer 3: Activity Layer (04-01)                                           │
│  ┌─────────────────────────────────────────────────────────────┐           │
│  │ 職責: 遊戲權重應用 (Game Weight Adjustment)                │           │
│  │ 檢查: SLOTS/SPORTS/BACCARAT/LOTTERY 等遊戲類型             │           │
│  │ 輸出: activity_valid_turnover                               │           │
│  │     = valid_turnover_finance × game_weight                 │           │
│  │                                                             │           │
│  │ ⚠️ 不負責拒絕決策 (No Rejection Logic Here)                │           │
│  └─────────────────────────────────────────────────────────────┘           │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```text

**職責矩陣**:

| 職責類型 | Layer 1 (Risk Engine) | Layer 2 (Finance) | Layer 3 (Activity) |
|---------|----------------------|-------------------|-------------------|
| **拒絕決策** | ✅ 唯一負責 | ❌ 不參與 | ❌ 不參與 |
| **狀態因子調整** | ❌ 不參與 | ✅ 唯一負責 | ❌ 不參與 |
| **遊戲權重應用** | ❌ 不參與 | ❌ 不參與 | ✅ 唯一負責 |
| **短路返回** | ✅ is_valid=false 直接返回 0 | ❌ 信任 Layer 1 結果 | ❌ 信任 Layer 2 結果 |
| **性能影響** | 🔴 執行所有注單 | 🟢 僅執行通過 Layer 1 的注單 | 🟢 僅執行有活動的注單 |

### 1.6.2 財務層處理流程 (Finance Layer Processing) ✅ v2.0.0

財務模組必須嚴格遵循以下步驟計算有效流水,**Layer 1 拒絕後直接短路返回**。

#### 調用層次 (Calling Hierarchy)

**❌ 錯誤方式** (v1.x - 已廢棄):
```typescript
// ❌ Layer 2 參與拒絕決策,違反職責分離
if (!riskValidation.is_valid) {
  return {
    valid_turnover_finance: 0,      // Layer 2 返回拒絕結果
    rejection_reason: riskValidation.risk_code
  };
}
// 問題: Layer 2 不應參與拒絕決策,應該由 Layer 1 直接處理
```text

**✅ 正確方式** (v2.0.0):
```typescript
// ✅ Layer 1 拒絕後直接短路,不調用 Layer 2
const riskValidation = await RiskEngine.validateTurnover({...});
if (!riskValidation.is_valid) {
  // Layer 1 直接返回 0,不進入 Layer 2/3
  return {
    effective_turnover_base: 0,
    valid_turnover_finance: 0,
    activity_valid_turnover: 0,
    rejected_by: 'RISK_ENGINE',
    risk_code: riskValidation.risk_code
  };
}

// Layer 2 僅負責狀態因子調整 (信任 Layer 1 已通過驗證)
const valid_turnover_finance = calculateFinanceTurnover(
  riskValidation.effective_turnover_base,
  bet.status
);
```text

#### Step 1: Layer 1 基礎驗證 (Risk Engine Validation)

**職責**: 拒絕決策 (Hedge/Arbitrage/Low Odds)

```typescript
/**
 * Layer 1: Risk Engine 驗證
 * 負責: 拒絕決策 (Rejection Decision)
 * 返回: { is_valid: boolean, effective_turnover_base: number, risk_code: string }
 */
const riskValidation = await RiskEngine.validateTurnover({
  bet_id: bet.id,
  player_id: bet.player_id,
  game_type: bet.game_type,
  bet_amount: bet.amount,
  odds: bet.odds,
  odds_type: bet.odds_type
});

// ✅ Layer 1 拒絕後直接短路返回 (不進入 Layer 2/3)
if (!riskValidation.is_valid) {
  log.info(`[Layer 1 Rejected] bet_id=${bet.id}, risk_code=${riskValidation.risk_code}`);

  // 直接返回全 0,不調用 Layer 2/3
  return {
    bet_id: bet.id,
    player_id: bet.player_id,
    effective_turnover_base: 0,
    valid_turnover_finance: 0,
    activity_valid_turnover: 0,
    rejected_by: 'RISK_ENGINE',       // 標記拒絕來源
    risk_code: riskValidation.risk_code,
    calculated_at: new Date()
  };
}

// ✅ Layer 1 通過,獲取基礎流水 (進入 Layer 2)
const effective_turnover_base = riskValidation.effective_turnover_base;
log.info(`[Layer 1 Passed] bet_id=${bet.id}, effective_turnover_base=${effective_turnover_base}`);
```text

#### Step 2: Layer 2 狀態因子調整 (Finance Status Factor)

**職責**: 僅負責狀態因子調整,不參與拒絕決策

```typescript
/**
 * Layer 2: Finance Layer 狀態因子調整
 * 職責: WIN/LOSS/DRAW/CANCEL 狀態因子應用
 * 前置條件: Layer 1 已通過驗證 (is_valid = true)
 *
 * ⚠️ 此層不負責拒絕決策,信任 Layer 1 結果
 */
const status_factor = getStatusFactor(bet.status);
const valid_turnover_finance = effective_turnover_base * status_factor;

log.info(`[Layer 2] bet_id=${bet.id}, status=${bet.status}, status_factor=${status_factor}, valid_turnover_finance=${valid_turnover_finance}`);

/**
 * 狀態因子映射表
 * v2.0.0: HALF_WIN/HALF_LOSS = 1.0 (標準本金法)
 */
function getStatusFactor(status: BetStatus): number {
  const STATUS_FACTORS = {
    'WIN': 1.0,        // 玩家贏 - 全額流水
    'LOSS': 1.0,       // 玩家輸 - 全額流水
    'DRAW': 0.0,       // 和局 - 無風險,不計流水
    'TIE': 0.0,        // 走水 - 同和局
    'VOID': 0.0,       // 作廢 - 注單無效
    'CANCEL': 0.0,     // 取消 - 注單無效
    'HALF_WIN': 1.0,   // ✅ v2.0.0: 贏半 - 全額流水 (標準本金法)
    'HALF_LOSS': 1.0,  // ✅ v2.0.0: 輸半 - 全額流水 (標準本金法)
    'RUNNING': 0.0     // 進行中 - 未結算不計
  };
  return STATUS_FACTORS[status] ?? 0.0;
}
```text

#### Step 3: 記錄三層流水 (Record All Layers)

**目的**: 記錄每層計算結果,便於審計與對帳

```typescript
/**
 * Step 3: 記錄三層流水 (用於審計與對帳)
 * - effective_turnover_base: Layer 1 結果
 * - valid_turnover_finance:  Layer 2 結果
 * - activity_valid_turnover: Layer 3 結果 (若適用)
 */
await db.transaction(async (tx) => {
  await tx.insertInto('bet_turnover_record').values({
    bet_id: bet.id,
    player_id: bet.player_id,
    game_type: bet.game_type,

    // Layer 1 結果
    effective_turnover_base: effective_turnover_base,
    risk_code: riskValidation.risk_code,

    // Layer 2 結果
    status: bet.status,
    status_factor: status_factor,
    valid_turnover_finance: valid_turnover_finance,

    // Layer 3 結果 (若有活動)
    activity_valid_turnover: activity_valid_turnover ?? 0,
    game_weight: game_weight ?? 1.0,

    calculated_at: new Date(),
    layer_breakdown: JSON.stringify({
      layer1: { effective_turnover_base, risk_code: riskValidation.risk_code },
      layer2: { status_factor, valid_turnover_finance },
      layer3: { game_weight, activity_valid_turnover }
    })
  });
});

log.info(`[All Layers Recorded] bet_id=${bet.id}`);
```yaml

#### 性能優化效果 (v2.0.0)

**修正前** (v1.x):
- Layer 1 拒絕後,Layer 2 仍執行計算邏輯
- 性能影響: 100% 注單執行 Layer 2 代碼

**修正後** (v2.0.0):
- Layer 1 拒絕後直接短路返回
- 性能影響: 僅通過 Layer 1 的注單 (~95%) 執行 Layer 2
- **節省性能**: ~5% CPU 與 DB 查詢

### 1.6.3 與活動系統的數據交換 (Data Exchange with Activity System)

財務模組計算完成後,需將 `valid_turnover_finance` 發布至事件總線供活動系統使用:

```typescript
// Publish finance turnover result to Kafka
await kafkaProducer.send({
  topic: 'finance.turnover.calculated',
  messages: [{
    key: bet.player_id,
    value: JSON.stringify({
      bet_id: bet.id,
      player_id: bet.player_id,
      game_type: bet.game_type,
      effective_turnover_base: effective_turnover_base,
      valid_turnover_finance: valid_turnover_finance,
      status: bet.status,
      status_factor: status_factor,
      timestamp: new Date().toISOString()
    })
  }]
});
```text

**活動系統訂閱此事件**後,在 `valid_turnover_finance` 基礎上應用遊戲權重 (Game Weight):
```typescript
// Activity System consumes this event
const activity_valid_turnover = message.valid_turnover_finance * GAME_WEIGHTS[message.game_type];
```text

### 1.6.4 每日對帳驗證程序 (Daily Reconciliation Procedure)

**執行時間**: 每日凌晨 03:00 (UTC+8)

**對帳邏輯**:


3. **異常處理流程** (Exception Handling):
   - **偏差 <0.01%**: 自動標記為已驗證 (Auto-verified)
   - **偏差 0.01%-1%**: 發送警報至 Slack #finance-ops 頻道
   - **偏差 >1%**: 觸發 PagerDuty 緊急警報,需立即人工介入

4. **偏差閾值設定依據** (Deviation Threshold Rationale) ✅ v2.0.0:

| 偏差範圍 | 閾值設定 | 業務影響分析 | 設定依據 |
|---------|---------|-------------|---------|
| **< 0.01%** | 可接受範圍 | 幾乎無影響 (單個玩家每日流水 $1000 → 偏差 $0.1) | 浮點數精度誤差、時區轉換誤差、遊戲權重配置微調 |
| **0.01% - 1%** | 警告區間 | 中等影響 (可能是配置錯誤) | 遊戲權重配置錯誤、狀態因子映射錯誤、對帳時間窗口不一致 |
| **> 1%** | 緊急區間 | 嚴重影響 (資金風險) | 系統 Bug、數據丟失、惡意攻擊、雙重扣款 |

**計算範例**:
```
假設玩家日流水 = $10,000
0.01% 偏差 = $1 (可接受)
1% 偏差 = $100 (需警報)
10% 偏差 = $1,000 (緊急)

業界標準: 大部分 iGaming 平台採用 0.01%-0.1% 作為自動驗證閾值
SmartAdmin 選擇: 0.01% (更嚴格,降低風險)
```markdown

5. **自動修正流程** (Auto-Correction Workflow) ✅ v2.0.0:

**觸發條件**: 偏差在 0.01%-1% 之間且滿足以下條件之一:
- 遊戲權重配置在對帳期間發生變更
- 狀態因子映射錯誤 (HALF_WIN/HALF_LOSS 計算錯誤)
- 時區轉換導致的邊界注單計入差異

**自動修正步驟**:
```typescript
/**
 * 自動修正流程 (僅限低風險偏差)
 */
async function autoCorrectDeviation(reconciliationRecord: ReconciliationRecord): Promise<boolean> {
    // Step 1: 分析偏差原因
    const rootCause = analyzeDeviationCause(reconciliationRecord);

    if (rootCause.type === 'GAME_WEIGHT_CONFIG_CHANGE') {
        // 遊戲權重配置變更 → 重新計算 Activity Turnover
        await recalculateActivityTurnover(
            reconciliationRecord.playerId,
            reconciliationRecord.date,
            rootCause.newGameWeight
        );

        log.info('[Auto-Correction] Game weight config updated, recalculated activity turnover');
        return true;
    }

    if (rootCause.type === 'STATUS_FACTOR_MISMATCH') {
        // 狀態因子錯誤 → 重新計算 Finance Turnover
        await recalculateFinanceTurnover(
            reconciliationRecord.playerId,
            reconciliationRecord.date,
            rootCause.correctStatusFactor
        );

        log.info('[Auto-Correction] Status factor corrected, recalculated finance turnover');
        return true;
    }

    if (rootCause.type === 'TIMEZONE_BOUNDARY_ISSUE') {
        // 時區邊界問題 → 調整對帳時間窗口
        await adjustReconciliationTimeWindow(
            reconciliationRecord.playerId,
            reconciliationRecord.date
        );

        log.info('[Auto-Correction] Timezone boundary adjusted');
        return true;
    }

    // 無法自動修正,轉人工處理
    log.warn('[Auto-Correction Failed] Root cause not auto-correctable, escalating to manual review');
    return false;
}
```markdown

**自動修正限制**:
- **僅限低風險偏差** (0.01%-1%)
- **單日單玩家偏差金額 < $100**
- **修正次數上限**: 每日每玩家最多自動修正 3 次,超過轉人工
- **審計日誌**: 所有自動修正必須記錄完整審計日誌

6. **補償機制** (Compensation Mechanism) ✅ v2.0.0:

當對帳發現偏差且無法自動修正時,啟動補償機制:

**補償類型矩陣**:

| 偏差類型 | 補償方式 | 觸發條件 | 執行者 | SLA |
|---------|---------|---------|-------|-----|
| **Finance < Activity** | 增加 Finance Turnover | Activity 計算過高 | 自動補償 | 1 小時 |
| **Finance > Activity** | 增加 Activity Turnover | Activity 計算過低 | 自動補償 | 1 小時 |
| **遊戲對帳差異 (GP ≠ Platform)** | 以 GP 為準調整平台記錄 | GP 報表與平台不一致 | 需人工審批 | 24 小時 |
| **負偏差 (平台多扣)** | 退款至玩家錢包 | 平台扣款過多 | 需人工審批 | 12 小時 |
| **正偏差 (平台少扣)** | 從玩家錢包扣回 | 平台扣款過少 | 需人工審批 + 風控審核 | 48 小時 |

**補償執行流程**:
```typescript
/**
 * 補償執行流程
 */
async function executeCompensation(deviation: DeviationRecord): Promise<CompensationResult> {
    const compensationType = determineCompensationType(deviation);

    // Step 1: 創建補償記錄
    const compensation = await db.insert('compensation_records').values({
        deviation_id: deviation.id,
        player_id: deviation.playerId,
        compensation_type: compensationType,
        original_amount: deviation.originalAmount,
        corrected_amount: deviation.correctedAmount,
        compensation_amount: Math.abs(deviation.originalAmount - deviation.correctedAmount),
        status: 'PENDING_APPROVAL',
        created_at: new Date()
    });

    // Step 2: 根據類型決定自動或人工
    if (compensationType === 'AUTO_ADJUST_FINANCE' || compensationType === 'AUTO_ADJUST_ACTIVITY') {
        // 自動補償 (僅限內部流水調整)
        await adjustTurnoverRecord(deviation.playerId, deviation.date, compensation.compensationAmount);

        compensation.status = 'COMPLETED';
        compensation.approved_at = new Date();
        compensation.approved_by = 'SYSTEM_AUTO';

        log.info('[Compensation] Auto-adjusted turnover for player_id={}, amount={}',
            deviation.playerId, compensation.compensationAmount);

    } else {
        // 需人工審批 (涉及錢包餘額變動)
        await createApprovalWorkflow(compensation);

        await notifyFinanceTeam({
            type: 'COMPENSATION_APPROVAL_REQUIRED',
            compensationId: compensation.id,
            playerId: deviation.playerId,
            amount: compensation.compensationAmount,
            priority: compensation.compensationAmount > 1000 ? 'HIGH' : 'MEDIUM'
        });

        log.info('[Compensation] Pending approval for player_id={}, amount={}',
            deviation.playerId, compensation.compensationAmount);
    }

    return compensation;
}
```text

**補償監控指標**:
- **補償觸發率**: `(compensations_count / total_reconciliations) × 100%` → 目標 < 0.1%
- **自動補償成功率**: >= 95%
- **人工審批響應時間**: P50 < 2h, P95 < 12h

7. **對帳報告生成** (Reconciliation Report):
   ```typescript
   interface DailyReconciliationReport {
     date: string;
     total_bets_processed: number;
     total_finance_turnover: number;
     total_activity_turnover: number;
     expected_ratio: number;
     actual_ratio: number;
     deviation_percentage: number;
     mismatched_players: {
       player_id: string;
       finance_turnover: number;
       activity_turnover: number;
       deviation: number;
     }[];
     status: 'VERIFIED' | 'WARNING' | 'CRITICAL';
   }
   ```markdown

### 1.6.5 配置同步要求 (Configuration Synchronization)

財務模組與活動模組必須從 **統一配置中心 (Unified Config Service)** 讀取以下參數,禁止本地硬編碼:

1. **賠率門檻 (Odds Thresholds)** - 由 Risk Engine 定義,財務模組需遵守:
   ```json
   {
     "odds_thresholds": {
       "EUR": 1.5,  // European decimal odds
       "HK": 0.5,   // Hong Kong odds
       "MY": 0.5,   // Malay odds
       "ID": 1.2    // Indonesian odds
     }
   }
   ```markdown

2. **遊戲權重表 (Game Weights)** - 由活動模組定義,財務模組需知曉用於驗證:
   ```json
   {
     "game_weights": {
       "SLOTS": 1.0,
       "SPORTS": 1.0,
       "BACCARAT": 0.15,
       "BLACKJACK": 0.10,
       "ROULETTE": 0.20,
       "VIDEO_POKER": 0.15,
       "LOTTERY": 0.10,
       "PVP": 0.0
     }
   }
   ```markdown

3. **狀態因子表 (Status Factors)** - 由財務模組定義與維護:
   ```json
   {
     "status_factors": {
       "WIN": 1.0,
       "LOSS": 1.0,
       "DRAW": 0.0,
       "TIE": 0.0,
       "VOID": 0.0,
       "CANCEL": 0.0,
       "HALF_WIN": 0.5,
       "HALF_LOSS": 0.5,
       "RUNNING": 0.0
     }
   }
   ```markdown

### 1.6.6 監控指標與 SLA (Monitoring Metrics & SLA)

**關鍵監控指標**:
- `finance.turnover.calculation.latency_p99` < 100ms
- `finance.turnover.risk_engine.call.success_rate` > 99.9%
- `finance.turnover.daily_reconciliation.deviation_rate` < 0.01%
- `finance.turnover.event_publish.success_rate` > 99.99%

**警報規則**:
```yaml
alerts:
  - name: turnover_calculation_latency_high
    condition: finance.turnover.calculation.latency_p99 > 500ms
    severity: WARNING
    notify: slack:#finance-ops

  - name: risk_engine_call_failure
    condition: finance.turnover.risk_engine.call.success_rate < 99%
    severity: CRITICAL
    notify: pagerduty:finance-oncall

  - name: daily_reconciliation_deviation
    condition: finance.turnover.daily_reconciliation.deviation_rate > 0.01
    severity: WARNING
    notify: slack:#finance-ops, email:finance-team@company.com

  - name: event_publish_failure
    condition: finance.turnover.event_publish.success_rate < 99.9
    severity: CRITICAL
    notify: pagerduty:finance-oncall
```sql

---

## 2. 遊戲對帳計算邏輯 (Game Reconciliation Logic)

遊戲對帳旨在解決「平台資料庫」與「遊戲商 (GP) 紀錄」不一致的問題。

### 2.1 三層對帳體系 (Three-Layer Reconciliation)

#### **Layer 1: 即時串流對帳 (Real-time Stream Check)**
- **時機**：每次收到 `GameEnd` 或 `Settlement` Webhook 後 1-5 分鐘。
- **機制**：
    - 透過 GP API 查詢單筆詳情 (`GetTransactionStatus`)。
    - 比對 `Amount`, `Status`, `WinLoss`。
    - **目的**：快速修復即時掉單 (Latency Issue)。

#### **Layer 2: 準即時週期對帳 (Near Real-time Batch)**
- **時機**：每 10-30 分鐘執行一次。
- **機制**：
    - 呼叫 GP 的 `FetchHistory` API (依時間區間 Time Range)。
    - 拉取過去 30 分鐘的所有注單。
    - 與 DB 進行 `Anti-Join` (找出 GP 有但 DB 無的單)。
    - **目的**：補錄 Callback 丟失的注單 (Self-Healing)。

#### **Layer 3: T+1 全量日結對帳 (Daily Final Settlement)**
- **時機**：每日凌晨 (e.g. 02:00)，GP 產出昨日完整報表後。
- **機制**：
    - 下載 GP 提供的 Settlement Files (CSV/XML/JSON)。
    - 將數據載入 Staging Table。
    - 執行 `Full Outer Join` 比對：
        1.  **GP 有，DB 無** -> 補單 (Recover)。
        2.  **GP 無，DB 有** -> 標記異常 (Invaild/Rollback)，需人工確認是否扣錯款。
        3.  **金額不符** -> 產出 `<DiffReport>`, 以 GP 最終結算金額為準進行調帳 (Adjustment)。

### 2.2 異常處理矩陣
| 異常情境 | 系統行為 | 處置方式 |
| :--- | :--- | :--- |
| **補單 (Recover)** | 發現 GP 有單但 DB 無 | 自動建立注單，補扣/補發金額，記錄 Source="Reconciliation" |
| **金額差異 (Diff)** | 雙方金額不一致 | 若差異 < 容差 (e.g. 0.01)，自動平帳；否則 Alert |
| **狀態衝突 (Conflict)** | DB=Win, GP=Loss | 以 **GP 報表** 為準，執行 `Reverse + Re-settle` |
| **幽靈單 (Ghost)** | DB 有單，GP 查無 | 極度危險 (可能是駭客注入)。**凍結帳號**，人工查核。 |

## 3. 對帳數據流向圖 (Data Flow)

```mermaid
---

config:

  theme: dark

  themeVariables:

    primaryColor: '#2d2d2d'

    primaryTextColor: '#fff'

    primaryBorderColor: '#7C0000'

    lineColor: '#00ff00'

    secondaryColor: '#006100'

    tertiaryColor: '#fff'

    darkMode: true

    background: '#1e1e1e'

---

graph TD

    classDef database fill:#003366,stroke:#00ccff,stroke-width:2px,color:#fff;

    classDef process fill:#333333,stroke:#fff,stroke-width:2px,color:#fff;

    classDef decision fill:#800080,stroke:#ff00ff,stroke-width:2px,color:#fff;

    classDef alert fill:#990000,stroke:#ff3333,stroke-width:2px,color:#fff;

    classDef success fill:#006600,stroke:#00ff00,stroke-width:2px,color:#fff;

    GP_API[Game Provider API/File]:::process -->|1. Fetch/Download| Staging[Staging Area\nRaw Data]:::process

    Platform_DB[(Platform Ledger)]:::database -->|2. Extract| Reconciliation_Engine[Reconciliation Engine]:::process

    Staging --> Reconciliation_Engine

    Reconciliation_Engine -->|3. Compare Logic| Logic{Match?}:::decision

    Logic -- Yes --> Mark_Verified[Mark as Verified]:::success

    Logic -- No: Missing --> Action_Recover[Create Missing Transaction]:::process

    Logic -- No: Diff --> Action_Adjust[Create Adjustment Record]:::process

    Logic -- No: Ghost --> Alert_Risk[Trigger Risk Alert]:::alert

    Action_Recover --> SaveTx[Save Transaction]:::database

    Action_Adjust --> SaveTx

    Mark_Verified --> End((Process End)):::process

    SaveTx --> Platform_DB

    SaveTx --> CacheRes[Update Redis Cache]:::process

    CacheRes --> Resp[Generate Admin Report/API]:::process
```text

---

## 4. 流水計算流程圖 (Turnover Calculation Flow)

此圖展示單筆注單如何同時計算「一般流水」與「活動流水」。

```mermaid
---

config:

  theme: dark

  themeVariables:

    primaryColor: '#2d2d2d'

    primaryTextColor: '#fff'

    primaryBorderColor: '#ff9900'

    lineColor: '#00ff00'

    secondaryColor: '#006100'

    tertiaryColor: '#fff'

    darkMode: true

    background: '#1e1e1e'

---

flowchart TD

    classDef startend fill:#003366,stroke:#00ccff,stroke-width:2px,color:#fff;

    classDef process fill:#333333,stroke:#fff,stroke-width:2px,color:#fff;

    classDef decision fill:#800080,stroke:#ff00ff,stroke-width:2px,color:#fff;

    classDef fail fill:#990000,stroke:#ff3333,stroke-width:2px,color:#fff;

    classDef success fill:#006600,stroke:#00ff00,stroke-width:2px,color:#fff;

    Bet([Bet Settle Trigger]):::startend --> CheckStatus{1. Status Valid?\nNo Draw/Cancel}:::decision

  

    CheckStatus -- No --> Invalid[Turnover = 0\nEffective = 0]:::fail

    CheckStatus -- Yes --> CheckOdds{2. Odds >= 0.5?\nAnti-Arbitrage}:::decision

    CheckOdds -- No --> Invalid

    CheckOdds -- Yes --> RiskCheck{3. Risk Engine\nValidate?}:::decision

    RiskCheck -- No (Hedge) --> Invalid

    RiskCheck -- Yes --> GeneralCalc[4. Calc General Turnover\n= Bet * GameWeight]:::process

    GeneralCalc --> HasBonus{5. Has Active Bonus?}:::decision

    HasBonus -- No --> EndNormal([End Process]):::startend

    HasBonus -- Yes --> BonusRule[Load Bonus Rules\nWhitelist, Cap, Contribution]:::process

    BonusRule --> CheckWhite{Game Allowed?}:::decision

    CheckWhite -- No --> BonusZero[Activity Turnover = 0]:::fail

    CheckWhite -- Yes --> CalcCap[Apply Max Contribution Cap]:::process

    CalcCap --> CalcBonusTO[6. Calc Activity Turnover\n= CappedBet * BonusWeight]:::success

    BonusZero --> UpdateProgress

    CalcBonusTO --> UpdateProgress[Update Wagering Progress]:::process

    UpdateProgress --> EndBonus([End Process]):::startend

    Invalid --> EndNormal
```java

---

## 5. SmartAdmin 架構映射 (Architecture Mapping) ✅ v2.0.0

### 5.1 流水計算模組分層設計

SmartAdmin 採用嚴格的五層架構,確保代碼職責清晰、易於測試與維護。

#### 分層職責表

| 層級 | 類名模式 | 職責 | 註解限制 |
|------|---------|------|---------|
| **Controller** | `TurnoverController` | 接收 HTTP 請求,參數校驗,返回 ResponseDTO | 無 @Transactional |
| **Service** | `TurnoverService` | 業務協調,調用 Manager/Dao,返回 Option/Try | 無 @Transactional |
| **Manager** | `TurnoverCalculationManager` | 事務管理,跨表操作,緩存控制 | ✅ @Transactional 僅此層 |
| **Dao** | `BetTurnoverRecordDao` | 數據庫 CRUD,MyBatis Mapper | 無業務邏輯 |
| **Entity** | `BetTurnoverRecordEntity` | 數據模型,與表結構一一對應 | 無業務邏輯 |

#### 依賴規則 (ArchitectureTest 強制)

```text
Controller → Service (✅ 允許)
Service → Dao      (✅ 允許,單表 CRUD)
Service → Manager  (✅ 允許,需要 @Transactional 時)
Manager → Dao      (✅ 允許)

Controller → Dao   (❌ 禁止,違反分層)
Controller → Manager (❌ 禁止,違反分層)
```

---

### 5.2 實體層 (Entity Layer)

#### BetTurnoverRecordEntity.java


---

### 5.3 DAO 層 (Data Access Layer)

#### BetTurnoverRecordDao.java


#### BetTurnoverRecordDao.xml (MyBatis Mapper)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="net.lab1024.sa.admin.module.business.finance.turnover.dao.BetTurnoverRecordDao">

    <!-- 根據玩家 ID 與日期查詢流水記錄 -->
    <select id="selectByPlayerIdAndDate" resultType="net.lab1024.sa.admin.module.business.finance.turnover.domain.entity.BetTurnoverRecordEntity">
        SELECT *
        FROM t_bet_turnover_record
        WHERE player_id = #{playerId}
          AND DATE(calculated_at) = #{date}
          AND deleted = 0
        ORDER BY calculated_at DESC
    </select>

    <!-- 根據注單 ID 查詢流水記錄 -->
    <select id="selectByBetId" resultType="net.lab1024.sa.admin.module.business.finance.turnover.domain.entity.BetTurnoverRecordEntity">
        SELECT *
        FROM t_bet_turnover_record
        WHERE bet_id = #{betId}
          AND deleted = 0
        LIMIT 1
    </select>
</mapper>
```

---

### 5.4 Manager 層 (Transaction Management Layer)

#### TurnoverCalculationManager.java


---

### 5.5 Service 層 (Business Logic Layer)

#### TurnoverService.java


---

### 5.6 Controller 層 (HTTP Interface Layer)

#### TurnoverController.java


---

### 5.7 Foundation 模組依賴

流水計算模組依賴以下 SmartAdmin Foundation 模組:

| Foundation 模組 | 用途 | 引用位置 |
|----------------|------|---------|
| **foundation.redis-lock** | 分佈式鎖,防止重複計算 | TurnoverCalculationManager |
| **foundation.cache** | Caffeine + Redis 緩存 | TurnoverCalculationManager.getTurnoverByBetId() |
| **foundation.audit-log** | 審計日誌記錄 | 流水計算完成後自動記錄 |
| **foundation.mq** | Kafka 事件發布 | 流水計算完成後發布 finance.turnover.calculated 事件 |
| **foundation.retry** | 失敗重試策略 | Risk Engine 調用失敗時重試 |

#### 引用示例 (RedisLock)


---

### 5.8 ArchitectureTest 驗證規則

以下 ArchUnit 規則確保流水計算模組符合 SmartAdmin 架構規範:


---

## 6. 變更日誌 (Change Log)

### v2.0.0 (2026-01-29)

**重大變更**:
1. ✅ **Major #4 修正**: 澄清三層驗證架構職責 (1.6.1, 1.6.2)
   - Layer 1 拒絕後直接短路返回,不進入 Layer 2/3
   - 明確職責矩陣: Layer 1 = 拒絕決策, Layer 2 = 狀態調整, Layer 3 = 權重應用
   - 性能優化: 節省 ~5% CPU 與 DB 查詢

2. ✅ **Major #3 修正**: 新增 SmartAdmin 架構映射 (§5)
   - 完整五層架構代碼示例 (Entity/Dao/Manager/Service/Controller)
   - Foundation 模組依賴說明 (redis-lock, cache, audit-log, mq, retry)
   - ArchitectureTest 驗證規則

**向下兼容**:
- v1.x API 保持不變,僅內部實現優化

### v1.0.0 (2026-01-28)

**初始版本**:
- 流水計算邏輯 (1.1-1.6)
- 遊戲對帳邏輯 (2.1-2.2)
- 流程圖與數據流向圖 (3-4)
- HALF_WIN/HALF_LOSS = 100% 流水 (v2.0.0 標準本金法)

---

**文檔版本**: 2.0.0
**最後更新**: 2026-01-29
**維護團隊**: Finance Team & Backend Team

---

## 📚 相關文檔

### 前置依賴
- [02-06 統一錢包模型](./02-06_Unified_Wallet_Model.md) - 錢包架構
- [03-03 無縫錢包分析](../03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - GP API 規範

### 核心依賴
- [02-04 流程圖](./02-04-diagrams/02-04-01_Flowcharts_and_Sequences.md) - Mermaid 流程圖
- [02-04 計算邏輯](./02-04-diagrams/02-04-02_Calculation_Logic.md) - 流水計算公式

### 延伸閱讀
- [Seamless Wallet 專題](./seamless-wallet/00_INDEX.md) - 深度技術分析
- [02-03 對帳系統](./02-03_Reconciliation_System.md) - 財務對帳
