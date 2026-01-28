# 02-06 統一錢包模型 (Unified Wallet Model)

## 1. 系統概述 (System Overview)

為了解決「現金網 (Cash Market)」與「信用網 (Credit Network)」在同一平台共存的架構衝突，本模組定義了 **"統一錢包 (Unified Wallet)"** 模型。
此模型將玩家的所有資產與負債標準化為統一的數據結構，確保 **交易處理 (Transaction Processing)**、**風控 (Risk)** 與 **財務對帳 (Reconciliation)** 能透過一套邏輯同時處理兩種模式。

---

## 2. 核心概念模型 (Core Concept Model)

### 2.1 資產 vs 負債 (Asset vs Liability)

系統將錢包餘額分為兩大類屬性：

| 屬性 | 類型 | 定義 | 數學符號 | 例子 |
| :--- | :--- | :--- | :--- | :--- |
| **Cash Balance** | Asset | 玩家擁有的真實資金，可自由提款。 | `(+)` | USDT 充值、現金網贏利 |
| **Bonus Balance** | Asset (Locked) | 玩家擁有的資金，但被鎖定需完成流水。 | `(+)` | 首存紅利、救援金 |
| **Credit Limit** | Liability Cap | 代理授予的透支權限 (不可提款)。 | `(Limit)` | 信用網額度 |
| **Outstanding** | Liability | 玩家已使用的信用額度 (對平台的負債)。 | `(-)` | 信用網輸錢 |

### 2.2 統一餘額公式 (Unified Balance Formula)

前往遊戲大廳時，玩家的 **"可下注餘額 (Playable Balance)"** 計算如下：

```math
Playable Balance = (Cash + Bonus) + (Credit Limit - Outstanding)
```

*   **Cash Mode 玩家**：Credit Limit = 0, Playable = Cash + Bonus
*   **Credit Mode 玩家**：Cash = 0, Playable = Available Credit
*   **Hybrid Mode 玩家**：同時擁有現金與額度 (較少見，但系統需支援)。

---

## 3. 交易扣款邏輯 (Deduction Logic)

當玩家下注 $100 時，系統需依據 **"資金優先級 (Fund Priority)"** 決定扣除哪部分的餘額。

### 3.1 優先級配置 (Priority Configuration)

**系統預設優先級**: **Bonus -> Cash -> Credit**

**配置層級** (優先級從高到低):
1.  **玩家特殊規則** (Player-Specific Rules): VIP 玩家、活動參與者可擁有自定義扣款順序
2.  **遊戲廠商配置** (Game Provider Override): 特定遊戲廠商可配置自定義順序
3.  **遊戲配置** (Game Configuration): 單一遊戲可覆蓋預設規則
4.  **系統預設** (System Default): 如無特殊配置，使用 Bonus -> Cash -> Credit

**配置範例**:
*   **默認情境**: Bonus -> Cash -> Credit (適用於大部分遊戲)
*   **老虎機遊戲覆蓋**: Cash -> Bonus -> Credit (優先扣現金以累積流水)
*   **VIP 玩家特權**: Credit -> Cash -> Bonus (優先使用信用額度，保留現金權益)

**實作邏輯** (Pseudo-code):
```java
DeductionSequence getDeductionSequence(Long playerId, String gameCode, String providerId) {
    // 1. 檢查玩家特殊規則
    return playerRuleEngine.getSequence(playerId)
        // 2. 檢查遊戲配置
        .orElse(gameConfigService.getSequence(gameCode))
        // 3. 檢查廠商配置
        .orElse(providerConfigService.getSequence(providerId))
        // 4. 使用系統預設
        .orElse(DEFAULT_SEQUENCE); // Bonus -> Cash -> Credit
}
```

**預設流程**:
1.  **Try Bonus**: 檢查是否有適用該遊戲的 Bonus Balance。
2.  **Try Cash**: 若 Bonus 不足，扣除 Cash Balance。
3.  **Try Credit**: 若 Cash 不足，增加 Outstanding (需檢查 Limit)。

**關聯文檔**: 詳細遊戲配置邏輯參見 [03-03 無縫錢包分析 §4.1](../03_Game_Center/03-03_Seamless_Wallet_Analysis.md#41-錢包扣款順序-wallet-deduction-order)

### 3.2 混合支付範例 (Hybrid Payment Example)

*   **情境**：玩家有 Bonus $10, Cash $50, Credit Limit $1000 (Used $0)。
*   **動作**：下注 $100。
*   **處理流程**：
    1.  扣除 Bonus $10 (剩餘 $0)。
    2.  扣除 Cash $50 (剩餘 $0)。
    3.  增加 Outstanding $40 (Limit $1000 - $40 > 0，通過)。
*   **結果**：交易成功，涵蓋了三種資金來源。

---

## 4. 結算與派彩邏輯 (Settlement & Payout)

### 4.1 贏錢分配 (Win Distribution)

當玩家贏錢時，資金回補的順序通常與扣款 **相反** 或者 **特定指定**。

*   **原則**：贏的錢應優先填平負債 (Credit)，再進入資產 (Cash/Bonus)。
*   **流程**：
    1.  **Repay Credit**: 若 Outstanding > 0，優先抵扣 Outstanding。
    2.  **Credit to Cash**: 若 Outstanding 已透過贏錢還清 (變為 0)，剩餘贏利進入 **Cash Wallet**。
    3.  **Bonus Win**: 若該注單是由 Bonus 產生，贏利進入 Bonus Wallet (並更新流水進度)。

### 4.2 信用週結 (Credit Settlement)

針對使用 Credit 的玩家，需定期執行 **"Reset"**：

1.  **結算日 (e.g. 週一 12:00)**：
    *   計算本週總輸贏：`Net Win/Loss = Current Cash - Initial Cash + Initial Outstanding - Current Outstanding`
2.  **歸零 (Reset)**：
    *   若玩家輸 (Outstanding > 0)：代理向玩家收錢，系統將 Outstanding 重置為 0。
    *   若玩家贏 (Cash > 0 且由 Credit 產生)：代理支付玩家，系統將 Cash 扣除 (視為已提款)。

---

## 5. 數據結構設計 (Data Structure)

### 5.1 Wallet Table

```sql
CREATE TABLE player_wallet (
    player_id UUID PRIMARY KEY,
    tenant_id INT NOT NULL, -- 多商戶隔離
    agent_id UUID, -- 信用歸屬代理 (Credit Network 必填)
    currency VARCHAR(3) NOT NULL,
    
    -- Asset Columns
    cash_balance DECIMAL(18, 4) DEFAULT 0,
    bonus_balance DECIMAL(18, 4) DEFAULT 0,
    
    -- Liability Columns
    credit_limit DECIMAL(18, 4) DEFAULT 0,
    outstanding_amount DECIMAL(18, 4) DEFAULT 0, -- 已用額度
    
    -- Risk Control
    is_locked BOOLEAN DEFAULT FALSE,
    risk_group_id INT, -- 風控分群 ID (0-100)
    dynamic_max_bet DECIMAL(18, 4), -- 動態限紅 (由 Risk Engine 計算)
    last_update_ts TIMESTAMP,
    
    INDEX idx_tenant_agent (tenant_id, agent_id)
);

```

### 5.2 Bonus Ledger (New in V4)
為支持多重紅利共存 (Multi-Bonus) 與差異化流水權重，需將紅利餘額從 `player_wallet` 拆分為獨立帳本。

```sql
CREATE TABLE player_bonus_ledger (
    ledger_id UUID PRIMARY KEY,
    player_id UUID,
    tenant_id INT,
    
    -- Bonus Identity
    promo_id VARCHAR(64), -- 關聯 Activity ID
    bonus_type ENUM('DEPOSIT_MATCH', 'FREE_CASH', 'REBATE'),
    
    -- Finance Status
    initial_amount DECIMAL(18, 4),
    current_balance DECIMAL(18, 4), -- 剩餘金額
    
    -- Wagering Status
    wagering_target DECIMAL(18, 4), -- 目標流水 (e.g. 100 * 20 = 2000)
    wagering_progress DECIMAL(18, 4), -- 當前流水 (e.g. 500)
    
    -- Lifecycle
    state ENUM('ACTIVE', 'QUEUED', 'FULFILLED', 'EXPIRED', 'FORFEITED'),
    expiry_time TIMESTAMP,
    created_at TIMESTAMP,
    
    INDEX idx_player_state (player_id, state)
);
```

### 5.3 Transaction Log (Unified)

```sql
CREATE TABLE wallet_transaction (
    tx_id UUID PRIMARY KEY,
    player_id UUID,
    
    -- Amount Breakdown (關鍵：詳細記錄資金來源)
    deduct_cash DECIMAL(18, 4) DEFAULT 0,
    deduct_bonus DECIMAL(18, 4) DEFAULT 0,
    increase_outstanding DECIMAL(18, 4) DEFAULT 0, -- 使用信用
    
    total_amount DECIMAL(18, 4) NOT NULL,
    
    type ENUM('BET', 'WIN', 'DEPOSIT', 'WITHDRAW', 'CREDIT_RESET'),
    reference_id VARCHAR(64) -- Game Round ID or Payment ID
);
```

---

## 6. 風控整合 (Risk Integration)

*   **曝光度監控 (Exposure Check)**：
    *   `Exposure Ratio = Outstanding / Credit Limit`
    *   當 Ratio > 90% 時，發送 **"瀕臨爆倉"** 警報。
    *   當 Ratio >= 100% 時，自動拒絕下注。

*   **異常負債 (Negative Protection)**：
    *   因結算延遲 (Latency) 可能導致 `Outstanding > Limit`。
    *   系統需支援 **"超額容忍度 (Overdraft Tolerance)"** 配置 (e.g. 允許超額 1%)，避免頻繁報錯影響體驗。

---

## 7. 結論

統一錢包模型透過將 "資產" 與 "負債" 納入同一計算體系，解決了 Cash 與 Credit 模式的兼容性問題。這使得平台可以同時服務 **現金網玩家 (充值玩)** 與 **信用網玩家 (賒帳玩)**，甚至支援兩者混合的過渡模式。

---

## 📚 相關文檔

### 前置知識（必讀）
- [00-03 數據模型總覽](../00_Concept_&_Analysis/00-03_Data_Model_Overview.md) - Wallet表結構設計、索引策略、數據庫層設計

### 核心依賴
- [02-07 交易處理流程](./02-07_Transaction_Processing_Flow.md) - 錢包餘額更新的事務處理機制、TCC模式、冪等性設計
- [02-04 流水計算與對帳](./02-04_Turnover_and_Game_Reconciliation_Analysis.md) - 從錢包交易記錄計算流水、三層驗證架構

### 相關實作
- [04-01 活動系統設計](../04_Activity_Center/04-01_Activity_System_Design.md) - 獎金錢包整合、流水要求計算、獎金餘額扣除優先級
- [06-02 信用網絡邏輯](../06_Agent_Center/06-02_Credit_Network_Logic.md) - Credit錢包使用、額度轉移、信用結算流程
- [02-01 提款風控](./02-01_Withdrawal_Risk_Control.md) - 可提餘額驗證、鎖定餘額處理

### 延伸閱讀
- [05-01 風控系統](../05_Risk_Management/05-01_Risk_Control_System.md) - 曝光度監控、異常負債檢測、風險等級評估
- [03-03 無縫錢包分析](../03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - 遊戲投注與錢包扣款的極端場景處理

---

**文檔版本**: 1.1.0
**最後更新**: 2026-01-27
**維護團隊**: Finance Team & Architecture Team
