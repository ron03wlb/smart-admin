# Turnover Calculation Logic Detail

> **Canonical Source**: [02-04-02_Calculation_Logic.md](../../source/02_Finance_Center/02-04-diagrams/02-04-02_Calculation_Logic.md)
> **Audience**: Architects, Backend Developers
> **Business Requirements**: None
> **Last Synced**: 2026-02-08

---

## 1. Core Concept Definitions

| Term | Description | Storage Location |
|------|------|----------|
| **effectiveStake** | Key metric for determining whether player meets wagering requirements | `player_wallet.effective_stake` |
| **lockAmount** | Non-withdrawable amount in main wallet, unlocked via effective stakes | `player_wallet.lock_amount` (main wallet only) |
| **wagerRequirement** | Wagering threshold that must be met for promotional wallet | `player_wallet.wager_requirement` (promo wallet only) |
| **turnoverRequired** | Total turnover player must complete = main wallet lockAmount + SUM(promo wallet wagerRequirement - effectiveStake) | Calculated value, not a DB column |
| **rebateEffectiveStake** | Effective stake eligible for rebate calculation | `transaction.rebate_effective_stake` |

---

## 2. Complete Turnover Calculation Flow

### 2.1 End-to-End Process

```mermaid
flowchart TD
    subgraph DEPOSIT["Deposit / Bonus Credit"]
        A1[Player Deposits / Claims Bonus] --> A2{Which Wallet?}
        A2 -->|Main Wallet| A3["cash increases<br/>lockAmount increases in sync"]
        A2 -->|Promo Wallet| A4["cash/bonus increases<br/>set wagerRequirement"]
    end

    subgraph BET["Bet Deduction"]
        B1[Player Places Bet] --> B2[Deduct by Wallet Priority]
        B2 --> B3{cash sufficient?}
        B3 -->|Yes| B4[Deduct cash only]
        B3 -->|No| B5[Exhaust cash then deduct bonus]
        B4 --> B6[Record isPromotion flag]
        B5 --> B6
    end

    subgraph SETTLE["Bet Settlement"]
        C1[Settlement Triggered] --> C2[Calculate effectiveStake]
        C2 --> C3{Game Type?}
        C3 -->|SPORTS / E-SPORTS| C4["effectiveStake = |winAmount + lossAmount|"]
        C3 -->|CASINO| C5{Payout Outcome?}
        C3 -->|Other| C6["effectiveStake = betAmount"]
        C5 -->|Draw| C7["effectiveStake = 0"]
        C5 -->|Win| C8["effectiveStake = min(winAmount, betAmount)"]
        C5 -->|Loss| C9["effectiveStake = betAmount"]
        C4 --> C10[Accumulate effectiveStake]
        C6 --> C10
        C7 --> C10
        C8 --> C10
        C9 --> C10
        C10 --> C11["lockAmount decreases<br/>lockAmount -= effectiveStake"]
    end

    subgraph REBATE["Rebate Calculation"]
        D1[Calculate rebateEffectiveStake] --> D2{isPromotion?}
        D2 -->|No| D3["rebateEffectiveStake = effectiveStake"]
        D2 -->|Yes| D4[Calculate remaining wagering requirement]
        D4 --> D5["totalRequirement = SUM(wagerRequirement - effectiveStake) + lockAmount"]
        D5 --> D6["rebateEffectiveStake = max(0, effectiveStake - totalRequirement)"]
    end

    subgraph WITHDRAW["Withdrawal Check"]
        E1[Player Requests Withdrawal] --> E2[Calculate turnoverRequired]
        E2 --> E3["turnoverRequired = main.lockAmount + SUM(promo.wagerRequirement - promo.effectiveStake)"]
        E3 --> E4{lockAmount == 0?}
        E4 -->|Yes| E5[Withdrawable = cash]
        E4 -->|No| E6[Withdrawable = cash - lockAmount]
    end

    DEPOSIT --> BET --> SETTLE --> REBATE
    SETTLE --> WITHDRAW
```

---

## 3. Effective Stake (effectiveStake) Calculation

### 3.1 Calculation Trigger Sequence

```mermaid
sequenceDiagram
    participant GP as Game Provider
    participant GS as GridService
    participant GAS as GridAbstractService
    participant WT as WalletTransaction
    participant DB as Database

    GP->>GS: result() settlement request
    GS->>GAS: getEffectiveStake()
    GAS->>GAS: Calculate by game type
    GAS-->>GS: effectiveStake
    GS->>GS: getRebateEffectiveStake()
    GS->>WT: addEffectiveStake(effectiveStake)
    WT->>WT: this.effectiveStake += effectiveStake
    WT->>WT: this.addedLockAmount -= effectiveStake
    GS->>DB: updateWallets()
```

### 3.2 Formulas by Game Type

| Game Type | Condition | effectiveStake Formula |
|----------|------|---------------------|
| **SPORTS / E-SPORTS** | All outcomes | `abs(winAmount + lossAmount)` |
| **CASINO** | Draw (payout == betAmount) | `0` |
| **CASINO** | Win (winAmount > 0) | `min(winAmount, betAmount)` |
| **CASINO** | Loss (winAmount == 0) | `betAmount` |
| **Other Types** | All outcomes | `betAmount` |

### 3.3 Code References

```java
// GridAbstractService.java:171-193
protected long getEffectiveStake(GameType gameType, long betAmount, long winAmount, long lossAmount) {
    return switch (gameType) {
        case SPORTS, E_SPORTS -> Math.abs(winAmount + lossAmount);
        case CASINO -> {
            if (winAmount == betAmount) yield 0L;          // Draw
            else if (winAmount > 0) yield Math.min(winAmount, betAmount); // Win
            else yield betAmount;                            // Loss
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

## 4. lockAmount Lifecycle

### 4.1 State Diagram

```mermaid
stateDiagram-v2
    [*] --> Created: Deposit / Bonus Credit
    Created --> Locked: lockAmount = credited amount
    Locked --> Decreasing: Bet Settlement
    Decreasing --> Decreasing: lockAmount -= effectiveStake
    Decreasing --> Zero: lockAmount <= 0
    Zero --> [*]: Free to Withdraw

    note right of Locked
        lockAmount triggers:
        DEPOSIT
        PROMOTION
        VIP
        RED_ENVELOPES
        WALLET_DEPOSIT
    end note

    note right of Decreasing
        Each settlement reduces lockAmount
        lockAmount = max(0, lockAmount - effectiveStake)
    end note
```

### 4.2 lockAmount Change Events

| Operation | lockAmount Change | Description |
|------|-----------------|------|
| Deposit Credit | **+amount** | Must complete wagering before withdrawal |
| Claim Bonus | **+amount** | Same as above |
| VIP Reward | **+amount** | Same as above |
| Place Bet | **No change** | Only deducts funds, does not affect lockAmount |
| Bet Settlement | **-effectiveStake** | Unlocks equivalent amount |
| Bet Cancellation | **+effectiveStake** | Restores original lock |

### 4.3 Code References

- **Increase logic**: `WalletTransaction.java:52-101`
- **Decrease logic**: `WalletTransaction.java:119-125`

---

## 5. Promotional Wallet Wagering Logic

### 5.1 Main Wallet vs. Promotional Wallet

```mermaid
flowchart LR
    subgraph Main["Main Wallet"]
        M1[cash]
        M2[bonus]
        M3[lockAmount]
        M4[effectiveStake]
    end

    subgraph Promo["Promotional Wallet"]
        P1[cash]
        P2[bonus]
        P3[wagerRequirement]
        P4[effectiveStake]
    end

    M3 -.->|"Main wallet uses lockAmount<br/>to control wagering"| M4
    P3 -.->|"Promo wallet uses wagerRequirement<br/>to control wagering"| P4
```

### 5.2 Promo Wallet Completion Check

```
Promotional wallet wagering met = (effectiveStake >= wagerRequirement)
```

### 5.3 Promo-to-Main Transfer Wagering Calculation

When transferring out of a promotional wallet, remaining wagering requirement transfers proportionally:

```
transferWagerRequirement = (wagerRequirement - effectiveStake) * (transferAmount / (cash + bonus))
```

---

## 6. Rebate Effective Stake (rebateEffectiveStake) Calculation

### 6.1 Flow

```mermaid
flowchart TD
    A[Bet Settlement] --> B{isPromotion == true?}
    B -->|No| C["rebateEffectiveStake = effectiveStake"]
    B -->|Yes| D[Get wallets associated with bet]
    D --> E[Calculate remaining wagering requirement]
    E --> F["totalRequirement = SUM(wagerRequirement - effectiveStake) + (openSts ? 0 : lockAmount)"]
    F --> G{effectiveStake > totalRequirement?}
    G -->|Yes| H["rebateEffectiveStake = effectiveStake - totalRequirement"]
    G -->|No| I["rebateEffectiveStake = 0"]
```

### 6.2 Key Logic Summary

| Bet Type | Condition | rebateEffectiveStake |
|----------|------|----------------------|
| Non-promotional bet | isPromotion = false | `effectiveStake` |
| Promotional bet | Wagering completed | `effectiveStake` |
| Promotional bet | Wagering incomplete | `max(0, effectiveStake - remainingRequirement)` |

### 6.3 Code Reference

- `GridAbstractService.java:195-239`

---

## 7. Withdrawal Turnover Requirement (turnoverRequired)

### 7.1 Formula

```
turnoverRequired = main.lockAmount + SUM(promo.wagerRequirement - promo.effectiveStake)
```

Where:
- `main.lockAmount`: Main wallet locked amount
- `SUM(...)`: Sum of remaining wagering requirements across all **active** promotional wallets

### 7.2 Code Reference

- `PlayerManager.java:709-718`

---

## 8. Database Update Logic

### 8.1 Wallet Update SQL

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

> **Note**: `GREATEST(..., 0)` ensures `clean_amount` and `lock_amount` never go below 0.

---

## 9. Complete Data Flow

```mermaid
flowchart TB
    subgraph Entry["Entry Layer"]
        API[Game Provider API]
    end

    subgraph Service["Service Layer"]
        GS[GridService]
        GAS[GridAbstractService]
    end

    subgraph Model["Model Layer"]
        WT[WalletTransaction]
        WTH[WalletTransactionHistory]
    end

    subgraph Data["Data Layer"]
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

## 10. Verification Checklist

### 10.1 Effective Stake Calculation

- [ ] SPORTS/E-SPORTS uses `abs(winAmount + lossAmount)` -- confirm correctness
- [ ] CASINO draw: effectiveStake = 0 -- confirm expected behavior
- [ ] CASINO win: `min(winAmount, betAmount)` -- confirm logic

### 10.2 lockAmount Logic

- [ ] Only main wallet has lockAmount -- confirm correctness
- [ ] lockAmount trigger types (DEPOSIT, PROMOTION, VIP, RED_ENVELOPES) -- confirm completeness
- [ ] lockAmount minimum is 0 (never negative) -- confirm correctness

### 10.3 Rebate Calculation

- [ ] Promotional bets require completed wagering for rebate eligibility -- confirm expected behavior
- [ ] `REBATE_BETTING_LOCKED` switch logic -- confirm if adjustments needed

### 10.4 Withdrawal Turnover

- [ ] turnoverRequired formula correctness -- confirm
- [ ] Only counting "active" promotional wallets -- confirm correctness

---

## 11. Code Index

| Function | File | Method |
|------|------|----------|
| Bet Deduction | GridAbstractService.java | `deduct()` |
| Effective Stake Calculation | GridAbstractService.java | `getEffectiveStake()` |
| Rebate Effective Stake | GridAbstractService.java | `getRebateEffectiveStake()` |
| Wallet Transaction Processing | WalletTransaction.java | `deduct()`, `addEffectiveStake()` |
| Bet Settlement | GridService.java | `result()` |
| Withdrawal Turnover Query | PlayerManager.java | `getBalance()` |
| Database Update | PlayerWalletServiceImpl.java | Lines 69-86 |

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Finance Team & Backend Team
