# 有效投注額計算邏輯詳解

> **規範來源**: [02-04-02_Calculation_Logic.md](../../source-archive/02_Finance_Center/02-04-diagrams/02-04-02_Calculation_Logic.md)
> **目標讀者**: 架構師、後端開發者
> **業務需求**: None
> **同步時間**: 2026-02-08

---

## 1. 核心概念定義

| 術語 | 說明 | 儲存位置 |
|------|------|----------|
| **effectiveStake** | 判斷玩家是否達成流水要求的關鍵指標 | `player_wallet.effective_stake` |
| **lockAmount** | 主錢包中的不可提領金額，透過有效投注額解鎖 | `player_wallet.lock_amount`（僅主錢包） |
| **wagerRequirement** | 促銷錢包必須達成的流水門檻 | `player_wallet.wager_requirement`（僅促銷錢包） |
| **turnoverRequired** | 玩家必須完成的總流水 = 主錢包 lockAmount + SUM(促銷錢包 wagerRequirement - effectiveStake) | 計算值，非資料庫欄位 |
| **rebateEffectiveStake** | 可計入返水計算的有效投注額 | `transaction.rebate_effective_stake` |

---

## 2. 完整有效投注額計算流程

### 2.1 端對端流程

```mermaid
flowchart TD
    subgraph DEPOSIT["存款 / 獎金入帳"]
        A1[玩家存款 / 領取獎金] --> A2{哪個錢包？}
        A2 -->|主錢包| A3["cash 增加<br/>lockAmount 同步增加"]
        A2 -->|促銷錢包| A4["cash/bonus 增加<br/>設定 wagerRequirement"]
    end

    subgraph BET["投注扣款"]
        B1[玩家下注] --> B2[依錢包優先順序扣款]
        B2 --> B3{cash 足夠？}
        B3 -->|是| B4[僅扣 cash]
        B3 -->|否| B5[用盡 cash 後扣 bonus]
        B4 --> B6[記錄 isPromotion 標記]
        B5 --> B6
    end

    subgraph SETTLE["投注結算"]
        C1[結算觸發] --> C2[計算 effectiveStake]
        C2 --> C3{遊戲類型？}
        C3 -->|體育 / 電競| C4["effectiveStake = |winAmount + lossAmount|"]
        C3 -->|真人娛樂城| C5{派彩結果？}
        C3 -->|其他| C6["effectiveStake = betAmount"]
        C5 -->|和局| C7["effectiveStake = 0"]
        C5 -->|贏| C8["effectiveStake = min(winAmount, betAmount)"]
        C5 -->|輸| C9["effectiveStake = betAmount"]
        C4 --> C10[累計 effectiveStake]
        C6 --> C10
        C7 --> C10
        C8 --> C10
        C9 --> C10
        C10 --> C11["lockAmount 減少<br/>lockAmount -= effectiveStake"]
    end

    subgraph REBATE["返水計算"]
        D1[計算 rebateEffectiveStake] --> D2{isPromotion？}
        D2 -->|否| D3["rebateEffectiveStake = effectiveStake"]
        D2 -->|是| D4[計算剩餘流水要求]
        D4 --> D5["totalRequirement = SUM(wagerRequirement - effectiveStake) + lockAmount"]
        D5 --> D6["rebateEffectiveStake = max(0, effectiveStake - totalRequirement)"]
    end

    subgraph WITHDRAW["提款檢查"]
        E1[玩家申請提款] --> E2[計算 turnoverRequired]
        E2 --> E3["turnoverRequired = main.lockAmount + SUM(promo.wagerRequirement - promo.effectiveStake)"]
        E3 --> E4{lockAmount == 0？}
        E4 -->|是| E5[可提領 = cash]
        E4 -->|否| E6[可提領 = cash - lockAmount]
    end

    DEPOSIT --> BET --> SETTLE --> REBATE
    SETTLE --> WITHDRAW
```

---

## 3. 有效投注額 (effectiveStake) 計算

### 3.1 計算觸發時序

```mermaid
sequenceDiagram
    participant GP as 遊戲供應商
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

### 3.2 依遊戲類型的公式

| 遊戲類型 | 條件 | effectiveStake 公式 |
|----------|------|---------------------|
| **體育 / 電競** | 所有結果 | `abs(winAmount + lossAmount)` |
| **真人娛樂城** | 和局 (payout == betAmount) | `0` |
| **真人娛樂城** | 贏 (winAmount > 0) | `min(winAmount, betAmount)` |
| **真人娛樂城** | 輸 (winAmount == 0) | `betAmount` |
| **其他類型** | 所有結果 | `betAmount` |

### 3.3 程式碼參考

```java
// GridAbstractService.java:171-193
protected long getEffectiveStake(GameType gameType, long betAmount, long winAmount, long lossAmount) {
    return switch (gameType) {
        case SPORTS, E_SPORTS -> Math.abs(winAmount + lossAmount);
        case CASINO -> {
            if (winAmount == betAmount) yield 0L;          // 和局
            else if (winAmount > 0) yield Math.min(winAmount, betAmount); // 贏
            else yield betAmount;                            // 輸
        }
        default -> betAmount;
    };
}
```

```java
// WalletTransaction.java:119-125
public void addEffectiveStake(long effectiveStake) {
    this.effectiveStake += effectiveStake;
    this.addedLockAmount -= effectiveStake;
}
```

---

## 4. lockAmount 生命週期

### 4.1 狀態圖

```mermaid
stateDiagram-v2
    [*] --> Created: 存款 / 獎金入帳
    Created --> Locked: lockAmount = 入帳金額
    Locked --> Decreasing: 投注結算
    Decreasing --> Decreasing: lockAmount -= effectiveStake
    Decreasing --> Zero: lockAmount <= 0
    Zero --> [*]: 可自由提款

    note right of Locked
        lockAmount 觸發情境：
        DEPOSIT（存款）
        PROMOTION（促銷）
        VIP（VIP 獎勵）
        RED_ENVELOPES（紅包）
        WALLET_DEPOSIT（錢包存款）
    end note

    note right of Decreasing
        每次結算減少 lockAmount
        lockAmount = max(0, lockAmount - effectiveStake)
    end note
```

### 4.2 lockAmount 變更事件

| 操作 | lockAmount 變化 | 說明 |
|------|-----------------|------|
| 存款入帳 | **+金額** | 完成流水才能提款 |
| 領取獎金 | **+金額** | 同上 |
| VIP 獎勵 | **+金額** | 同上 |
| 下注 | **無變化** | 僅扣除資金，不影響 lockAmount |
| 投注結算 | **-effectiveStake** | 解鎖等值金額 |
| 投注取消 | **+effectiveStake** | 恢復原本鎖定 |

### 4.3 程式碼參考

- **增加邏輯**: `WalletTransaction.java:52-101`
- **減少邏輯**: `WalletTransaction.java:119-125`

---

## 5. 促銷錢包流水邏輯

### 5.1 主錢包 vs. 促銷錢包

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

    M3 -.->|"主錢包使用 lockAmount<br/>控制流水"| M4
    P3 -.->|"促銷錢包使用 wagerRequirement<br/>控制流水"| P4
```

### 5.2 促銷錢包完成檢查

```
促銷錢包流水達成 = (effectiveStake >= wagerRequirement)
```

### 5.3 促銷轉主錢包流水計算

從促銷錢包轉出時，剩餘流水要求按比例轉移：

```
transferWagerRequirement = (wagerRequirement - effectiveStake) * (transferAmount / (cash + bonus))
```

---

## 6. 返水有效投注額 (rebateEffectiveStake) 計算

### 6.1 流程

```mermaid
flowchart TD
    A[投注結算] --> B{isPromotion == true？}
    B -->|否| C["rebateEffectiveStake = effectiveStake"]
    B -->|是| D[取得投注關聯的錢包]
    D --> E[計算剩餘流水要求]
    E --> F["totalRequirement = SUM(wagerRequirement - effectiveStake) + (openSts ? 0 : lockAmount)"]
    F --> G{effectiveStake > totalRequirement？}
    G -->|是| H["rebateEffectiveStake = effectiveStake - totalRequirement"]
    G -->|否| I["rebateEffectiveStake = 0"]
```

### 6.2 關鍵邏輯摘要

| 投注類型 | 條件 | rebateEffectiveStake |
|----------|------|----------------------|
| 非促銷投注 | isPromotion = false | `effectiveStake` |
| 促銷投注 | 流水已達成 | `effectiveStake` |
| 促銷投注 | 流水未達成 | `max(0, effectiveStake - remainingRequirement)` |

### 6.3 程式碼參考

- `GridAbstractService.java:195-239`

---

## 7. 提款流水要求 (turnoverRequired)

### 7.1 公式

```
turnoverRequired = main.lockAmount + SUM(promo.wagerRequirement - promo.effectiveStake)
```

其中：
- `main.lockAmount`：主錢包鎖定金額
- `SUM(...)`：所有**活躍**促銷錢包剩餘流水要求的總和

### 7.2 程式碼參考

- `PlayerManager.java:709-718`

---

## 8. 資料庫更新邏輯

### 8.1 錢包更新 SQL

```sql
UPDATE player_wallet
SET
    cash = cash + #{cashDelta},
    bonus = bonus + #{bonusDelta},
    clean_amount = GREATEST(clean_amount + #{cleanDelta}, 0),
    lock_amount = GREATEST(lock_amount + #{lockDelta}, 0),
    effective_stake = effective_stake + #{effectiveStakeDelta}
WHERE
    id = #{walletId}
    AND cash + #{cashDelta} >= 0
    AND bonus + #{bonusDelta} >= 0;
```

> **注意**：`GREATEST(..., 0)` 確保 `clean_amount` 和 `lock_amount` 永不為負。

---

## 9. 完整資料流

```mermaid
flowchart TB
    subgraph Entry["入口層"]
        API[遊戲供應商 API]
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

## 10. 驗證清單

### 10.1 有效投注額計算

- [ ] 體育/電競使用 `abs(winAmount + lossAmount)` -- 確認正確性
- [ ] 真人娛樂城和局：effectiveStake = 0 -- 確認預期行為
- [ ] 真人娛樂城贏：`min(winAmount, betAmount)` -- 確認邏輯

### 10.2 lockAmount 邏輯

- [ ] 僅主錢包有 lockAmount -- 確認正確性
- [ ] lockAmount 觸發類型（DEPOSIT、PROMOTION、VIP、RED_ENVELOPES）-- 確認完整性
- [ ] lockAmount 最小為 0（永不為負）-- 確認正確性

### 10.3 返水計算

- [ ] 促銷投注需完成流水才有返水資格 -- 確認預期行為
- [ ] `REBATE_BETTING_LOCKED` 開關邏輯 -- 確認是否需調整

### 10.4 提款流水

- [ ] turnoverRequired 公式正確性 -- 確認
- [ ] 僅計算「活躍」促銷錢包 -- 確認正確性

---

## 11. 程式碼索引

| 功能 | 檔案 | 方法 |
|------|------|----------|
| 投注扣款 | GridAbstractService.java | `deduct()` |
| 有效投注額計算 | GridAbstractService.java | `getEffectiveStake()` |
| 返水有效投注額 | GridAbstractService.java | `getRebateEffectiveStake()` |
| 錢包交易處理 | WalletTransaction.java | `deduct()`, `addEffectiveStake()` |
| 投注結算 | GridService.java | `result()` |
| 提款流水查詢 | PlayerManager.java | `getBalance()` |
| 資料庫更新 | PlayerWalletServiceImpl.java | 第 69-86 行 |

---

**文檔版本**: 1.0.0
**最後更新**: 2026-02-08
**維護團隊**: 財務團隊 & 後端團隊
