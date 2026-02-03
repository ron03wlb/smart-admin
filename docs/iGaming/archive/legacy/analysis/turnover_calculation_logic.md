# 流水計算邏輯確認文件

## 1. 核心概念定義

| 術語 | 說明 | 儲存位置 |
|------|------|----------|
| **effectiveStake（有效投注）** | 計算玩家是否滿足流水要求的關鍵指標 | `player_wallet.effective_stake` |
| **lockAmount（鎖定金額）** | 主錢包中無法提款的金額，需透過有效投注解鎖 | `player_wallet.lock_amount`（僅主錢包） |
| **wagerRequirement（流水要求）** | 優惠錢包需達成的投注流水門檻 | `player_wallet.wager_requirement`（僅促銷錢包） |
| **turnoverRequired（提款流水需求）** | 玩家需達成的總流水 = 主錢包 lockAmount + Σ(促銷錢包 wagerRequirement - effectiveStake) | 計算值，非資料庫欄位 |
| **rebateEffectiveStake（返水有效投注）** | 可參與返水計算的有效投注 | `transaction.rebate_effective_stake` |

---

## 2. 流水計算完整流程

### 2.1 整體流程圖

```mermaid
flowchart TD
    subgraph DEPOSIT["存款/優惠入帳"]
        A1[玩家存款/領取優惠] --> A2{入帳至哪種錢包?}
        A2 -->|主錢包| A3["cash 增加\nlockAmount 同步增加"]
        A2 -->|促銷錢包| A4["cash/bonus 增加\n設定 wagerRequirement"]
    end

    subgraph BET["投注扣款"]
        B1[玩家下注] --> B2[依錢包優先順序扣款]
        B2 --> B3{cash 是否足夠?}
        B3 -->|是| B4[僅扣 cash]
        B3 -->|否| B5[cash 扣完後扣 bonus]
        B4 --> B6[記錄 isPromotion 標記]
        B5 --> B6
    end

    subgraph SETTLE["投注結算"]
        C1[結算觸發] --> C2[計算 effectiveStake]
        C2 --> C3{遊戲類型?}
        C3 -->|SPORTS/E-SPORTS| C4["effectiveStake = |winAmount + lossAmount|"]
        C3 -->|CASINO| C5{派彩情況?}
        C3 -->|其他| C6["effectiveStake = betAmount"]
        C5 -->|和局| C7["effectiveStake = 0"]
        C5 -->|贏錢| C8["effectiveStake = min(winAmount, betAmount)"]
        C5 -->|輸錢| C9["effectiveStake = betAmount"]
        C4 --> C10[累加 effectiveStake]
        C6 --> C10
        C7 --> C10
        C8 --> C10
        C9 --> C10
        C10 --> C11["lockAmount 減少\nlockAmount -= effectiveStake"]
    end

    subgraph REBATE["返水計算"]
        D1[計算 rebateEffectiveStake] --> D2{是否促銷投注?}
        D2 -->|否| D3["rebateEffectiveStake = effectiveStake"]
        D2 -->|是| D4[計算剩餘流水需求]
        D4 --> D5["totalRequirement = Σ(wagerRequirement - effectiveStake) + lockAmount"]
        D5 --> D6["rebateEffectiveStake = max(0, effectiveStake - totalRequirement)"]
    end

    subgraph WITHDRAW["提款檢查"]
        E1[玩家申請提款] --> E2[計算 turnoverRequired]
        E2 --> E3["turnoverRequired = main.lockAmount + Σ(promo.wagerRequirement - promo.effectiveStake)"]
        E3 --> E4{lockAmount == 0?}
        E4 -->|是| E5[可提款金額 = cash]
        E4 -->|否| E6[可提款金額 = cash - lockAmount]
    end

    DEPOSIT --> BET --> SETTLE --> REBATE
    SETTLE --> WITHDRAW
```

---

## 3. 有效投注 (effectiveStake) 計算邏輯

### 3.1 計算時機

```mermaid
sequenceDiagram
    participant GP as Game Provider
    participant GS as GridService
    participant GAS as GridAbstractService
    participant WT as WalletTransaction
    participant DB as Database

    GP->>GS: result() 結算請求
    GS->>GAS: getEffectiveStake()
    GAS->>GAS: 依遊戲類型計算
    GAS-->>GS: effectiveStake
    GS->>GS: getRebateEffectiveStake()
    GS->>WT: addEffectiveStake(effectiveStake)
    WT->>WT: this.effectiveStake += effectiveStake
    WT->>WT: this.addedLockAmount -= effectiveStake
    GS->>DB: updateWallets()
```

### 3.2 各遊戲類型計算公式

| 遊戲類型 | 條件 | effectiveStake 公式 |
|----------|------|---------------------|
| **SPORTS / E-SPORTS** | - | `|winAmount + lossAmount|` |
| **CASINO** | 和局 (payout == betAmount) | `0` |
| **CASINO** | 贏錢 (winAmount > 0) | `min(winAmount, betAmount)` |
| **CASINO** | 輸錢 (winAmount == 0) | `betAmount` |
| **其他類型** | - | `betAmount` |

### 3.3 程式碼位置

- **計算方法**：`GridAbstractService.java:171-193`
- **累加方法**：`WalletTransaction.java:119-125`

---

## 4. lockAmount 變化邏輯

### 4.1 lockAmount 生命週期

```mermaid
stateDiagram-v2
    [*] --> Created: 存款/優惠入帳
    Created --> Locked: lockAmount = 入帳金額
    Locked --> Decreasing: 投注結算
    Decreasing --> Decreasing: lockAmount -= effectiveStake
    Decreasing --> Zero: lockAmount <= 0
    Zero --> [*]: 可自由提款
    
    note right of Locked : lockAmount 產生條件:<br/>- DEPOSIT<br/>- PROMOTION<br/>- VIP<br/>- RED_ENVELOPES<br/>- WALLET_DEPOSIT
    
    note right of Decreasing : 每次結算減少<br/>lockAmount = max(0, lockAmount - effectiveStake)
```

### 4.2 lockAmount 變化時機

| 操作     | lockAmount 變化       | 說明                 |
| ------ | ------------------- | ------------------ |
| 存款入帳   | **+金額**             | 需完成流水才能提款          |
| 領取優惠   | **+金額**             | 同上                 |
| VIP 獎勵 | **+金額**             | 同上                 |
| 投注下注   | **無變化**             | 僅扣款，不影響 lockAmount |
| 投注結算   | **-effectiveStake** | 解鎖等值金額             |
| 投注取消   | **+effectiveStake** | 回復原本鎖定             |

### 4.3 程式碼位置

- **增加邏輯**：`WalletTransaction.java:52-101`
- **減少邏輯**：`WalletTransaction.java:119-125`

---

## 5. 促銷錢包流水邏輯

### 5.1 促銷錢包 vs 主錢包

```mermaid
flowchart LR
    subgraph Main["主錢包"]
        M1[cash]
        M2[bonus]
        M3[lockAmount]
        M4[effectiveStake]
    end

    subgraph Promo["促銷錢包"]
        P1[cash]
        P2[bonus]
        P3[wagerRequirement]
        P4[effectiveStake]
    end

    M3 -.->|"主錢包用 lockAmount 控制流水"| M4
    P3 -.->|"促銷錢包用 wagerRequirement 控制流水"| P4
```

### 5.2 促銷錢包流水達成判斷

```
促銷錢包流水達成 = (effectiveStake >= wagerRequirement)
```

### 5.3 促銷錢包轉主錢包時的流水計算

當促銷錢包轉出時，會按比例轉移剩餘流水需求：

```
transferWagerRequirement = (wagerRequirement - effectiveStake) × (transferAmount / (cash + bonus))
```

---

## 6. 返水有效投注 (rebateEffectiveStake) 計算

### 6.1 計算流程

```mermaid
flowchart TD
    A[投注結算] --> B{isPromotion == true?}
    B -->|否| C["rebateEffectiveStake = effectiveStake"]
    B -->|是| D[取得投注相關錢包]
    D --> E[計算剩餘流水需求]
    E --> F["totalRequirement = Σ(wagerRequirement - effectiveStake) + (openSts ? 0 : lockAmount)"]
    F --> G{effectiveStake > totalRequirement?}
    G -->|是| H["rebateEffectiveStake = effectiveStake - totalRequirement"]
    G -->|否| I["rebateEffectiveStake = 0"]
```

### 6.2 關鍵邏輯

| 投注類型 | 條件 | rebateEffectiveStake |
|----------|------|----------------------|
| 非促銷投注 | isPromotion = false | `effectiveStake` |
| 促銷投注 | 流水已完成 | `effectiveStake` |
| 促銷投注 | 流水未完成 | `max(0, effectiveStake - 剩餘流水需求)` |

### 6.3 程式碼位置

- `GridAbstractService.java:195-239`

---

## 7. 提款流水需求 (turnoverRequired) 計算

### 7.1 計算公式

```
turnoverRequired = main.lockAmount + Σ(promo.wagerRequirement - promo.effectiveStake)
```

其中：
- `main.lockAmount`：主錢包的鎖定金額
- `Σ(...)`：所有**開啟中**促銷錢包的剩餘流水需求總和

### 7.2 程式碼位置

- `PlayerManager.java:709-718`

---

## 8. 資料庫更新邏輯

### 8.1 錢包更新 SQL

```sql
UPDATE player_wallet
SET bonus = ?,
    cash = ?,
    clean_amount = GREATEST(clean_amount + ?, 0),
    lock_amount = GREATEST(lock_amount + ?, 0),
    effective_stake = ?
WHERE id = ?
```

> **注意**：`GREATEST(..., 0)` 確保 `cleanAmount` 和 `lockAmount` 不會低於 0。

---

## 9. 完整數據流向圖

```mermaid
flowchart TB
    subgraph Entry["入口層"]
        API[Game Provider API]
    end

    subgraph Service["服務層"]
        GS[GridService]
        GAS[GridAbstractService]
    end

    subgraph Model["模型層"]
        WT[WalletTransaction]
        WTH[WalletTransactionHistory]
    end

    subgraph Data["資料層"]
        PWS[PlayerWalletService]
        DB[(MySQL)]
    end

    API -->|"bet/result/cancel"| GS
    GS -->|"deduct()"| GAS
    GAS -->|"new WalletTransaction()"| WT
    GS -->|"getEffectiveStake()"| GAS
    GAS -->|"addEffectiveStake()"| WT
    WT -->|"deduct()"| WTH
    GS -->|"updateWallets()"| GAS
    GAS -->|"updateWallets()"| PWS
    PWS -->|"UPDATE SQL"| DB
```

---

## 10. 需求確認清單

### 10.1 有效投注計算

- [ ] SPORTS/E-SPORTS 使用 `|winAmount + lossAmount|` 是否正確？
- [ ] CASINO 和局時 effectiveStake = 0 是否符合預期？
- [ ] CASINO 贏錢時取 `min(winAmount, betAmount)` 邏輯是否正確？

### 10.2 lockAmount 邏輯

- [ ] 僅主錢包有 lockAmount 是否正確？
- [ ] lockAmount 的產生類型（DEPOSIT, PROMOTION, VIP, RED_ENVELOPES）是否完整？
- [ ] lockAmount 最小值為 0（不會負數）是否正確？

### 10.3 返水計算

- [ ] 促銷投注需完成流水才能獲得返水是否為預期行為？
- [ ] `REBATE_BETTING_LOCKED` 開關邏輯是否需要調整？

### 10.4 提款流水

- [ ] turnoverRequired 計算公式是否正確？
- [ ] 僅計算「開啟中」促銷錢包是否正確？

---

## 11. 相關程式碼索引

| 功能 | 檔案 | 方法名稱 |
|------|------|----------|
| 投注扣款 | GridAbstractService.java | `deduct()` |
| 有效投注計算 | GridAbstractService.java | `getEffectiveStake()` |
| 返水有效投注 | GridAbstractService.java | `getRebateEffectiveStake()` |
| 錢包交易處理 | WalletTransaction.java | `deduct()`, `addEffectiveStake()` |
| 投注結算 | GridService.java | `result()` |
| 提款流水查詢 | PlayerManager.java | `getBalance()` |
| 資料庫更新 | PlayerWalletServiceImpl.java | 行 69-86 |

---

**文檔版本**: 1.0.0
**最後更新**: 2026-01-28
**維護團隊**: Finance Team & Backend Team
