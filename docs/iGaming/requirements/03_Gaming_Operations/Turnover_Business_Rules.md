# Turnover Business Rules

> **Canonical Source**: [source/03_Game_Center/03-04_Turnover_Calculation.md](../../source/03_Game_Center/03-04_Turnover_Calculation.md)
> **Audience**: Executives, Product Managers
> **Related Doc**: [Turnover_Calculation_Logic.md](../../architecture/03_Game_Integration/Turnover_Calculation_Logic.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## 1. Executive Summary

This document defines the business rules for turnover (wagering) calculation in the iGaming platform. Turnover calculation is critical for:

- **Player Wagering Progress**: Tracking bonus unlock requirements
- **Rebate Calculations**: Determining cashback amounts
- **VIP Tier Advancement**: Measuring player activity levels
- **Regulatory Compliance**: Accurate GGR (Gross Gaming Revenue) reporting

**Core Principle**: Only bets that produce a win/loss outcome, carry actual risk, and pass risk validation contribute to valid turnover.

---

## 2. Terminology Definitions

| Term (EN) | Term (ZH) | Definition | Scope |
|-----------|-----------|------------|-------|
| **Bet Amount** | 投注額 | Original stake placed on a single bet | Per Bet |
| **Turnover** | 流水 | Cumulative sum of bet amounts over time | Cumulative |
| **Valid Bet** | 有效投注額 | Single bet amount after risk filtering | Per Bet |
| **Wagering Requirement** | 流水要求 | Total valid turnover needed to unlock bonus | Cumulative |

---

## 3. Three-Layer Validation Architecture

The turnover calculation follows a strict three-layer validation model, ensuring separation of concerns and clear accountability.

### 3.1 Layer Overview

| Layer | Module | Responsibility | Output |
|-------|--------|----------------|--------|
| **Layer 1** | Risk Engine | Rejection decisions (BLOCK/FLAG/PASS) | `effective_turnover_base` |
| **Layer 2** | Finance Center | Settlement status recording | `valid_turnover_finance` |
| **Layer 3** | Activity System | Game weight application | `activity_valid_turnover` |

### 3.2 Key Principle

**Layer 1 is the ONLY layer responsible for rejection decisions**. Subsequent layers (Layer 2/3) only perform value adjustments, never rejection.

### 3.3 Layer Responsibilities Matrix

| Responsibility | Layer 1 (Risk) | Layer 2 (Finance) | Layer 3 (Activity) |
|----------------|----------------|-------------------|-------------------|
| **Rejection Decision** | Yes (Only) | No | No |
| **Status Factor Adjustment** | No | Yes (Only) | No |
| **Game Weight Application** | No | No | Yes (Only) |
| **Early Return on Block** | Yes | No | No |

---

## 4. Status Factor Definitions

### 4.1 Standard Principal Method

**Core Rule**: Valid Bet = Bet Amount (regardless of settlement status)

The "Standard Principal Method" treats all bet amounts equally based on the risk taken at bet placement, not the outcome.

### 4.2 Status Factor Table

| Status | Factor | Explanation |
|--------|--------|-------------|
| **WIN** | 100% | Normal calculation |
| **LOSS** | 100% | Normal calculation |
| **HALF_WIN** | 100% | Standard Principal Method (v2.0.0) |
| **HALF_LOSS** | 100% | Standard Principal Method (v2.0.0) |
| **DRAW / TIE** | 0% | No risk exposure, no turnover |
| **VOID / CANCEL** | 0% | Invalid bet |
| **RUNNING** | 0% | Unsettled, pending result |

### 4.3 Business Rationale for HALF_WIN/HALF_LOSS

**Why 100% (not 50%)?**

Consider two players both betting $100 on Asian Handicap -0.25:
- Player A: Full win result → Valid Bet = $100
- Player B: Half loss result → Valid Bet should also be $100

**Reasoning**:
1. Both players took the same betting action
2. Both exposed the same risk amount ($100)
3. Valid Bet reflects betting behavior, not settlement outcome
4. Simplifies calculation without waiting for settlement

---

## 5. Game Weight Policies

### 5.1 Weight Configuration Table

| Game Type | Weight | Business Rationale |
|-----------|--------|-------------------|
| **Slots** | 100% | Pure probability, high platform edge |
| **Sports Betting** | 100% | High risk, unpredictable outcomes |
| **E-Sports** | 100% | Similar risk profile to sports |
| **Roulette** | 20% | Medium risk, house edge ~2.7% |
| **Baccarat** | 15% | High RTP (~98.9%), low platform risk |
| **Live Casino** | 15% | Varies by operator strategy |
| **Blackjack** | 10% | Skill-based, low house edge |
| **Video Poker** | 5% | High skill factor |
| **Poker** | 5% | Skill-based game |
| **Lottery** | 10-20% | Two-sided betting (Big/Small) easy to hedge |
| **PVP Games** | 0% | Player-to-player transfer, no platform risk |

### 5.2 Weight Adjustment Considerations

Game weights are configurable per promotion. Factors to consider:
- **Platform RTP**: Higher RTP = lower weight
- **Abuse Potential**: Games easy to hedge receive lower weights
- **Skill Factor**: High-skill games receive lower weights
- **Market Competition**: May adjust based on competitor offerings

---

## 6. Wagering Requirement Rules

### 6.1 Core Decision: Withdrawal-Time Verification

**Business Rule**: Wagering progress is accumulated during play, but verification occurs ONLY at withdrawal time.

### 6.2 Comparison of Approaches

| Aspect | Auto-Unlock on Bet (Wrong) | Withdrawal Verification (Correct) |
|--------|---------------------------|----------------------------------|
| **Player reaches target then loses all** | Bonus already unlocked, operator loss | Bonus still locked, risk protected |
| **Player Experience** | Hidden risk | Transparent at withdrawal |
| **Risk Control** | Low protection | High protection |
| **Industry Standard** | Non-compliant | Compliant |

### 6.3 Example Scenario

1. Player receives $1,000 deposit + $1,000 bonus (100% match)
2. Wagering Requirement: ($1,000 + $1,000) x 5 = $10,000
3. Player completes $10,000 valid turnover
4. Player continues playing and loses $500
5. At withdrawal request: System verifies wagering complete, but available balance reflects losses

**Result**: Operator protected from releasing bonus funds that were subsequently lost.

---

## 7. Free Spins Turnover Rules

### 7.1 Core Principle

| Metric | Definition | Calculation | Purpose |
|--------|------------|-------------|---------|
| **Turnover** | Total game flow amount | Free spin face value sum | Financial reporting, GGR |
| **Valid Bet** | Wagering contribution | 0 (not counted) | Bonus progress, rebates |

### 7.2 Business Rationale

**Why Turnover = Face Value (not 0)?**

Example: Player receives 10 free spins at $1 each, wins $8.50

| Calculation Method | Turnover | Payout | GGR | Interpretation |
|-------------------|----------|--------|-----|----------------|
| Turnover = 0 (Wrong) | $0 | $8.50 | -$8.50 | Appears as loss |
| Turnover = Face Value (Correct) | $10.00 | $8.50 | $1.50 | Reflects promotion cost |

### 7.3 Industry Standard

All major game providers use this logic:

| Provider | Turnover | Valid Bet |
|----------|----------|-----------|
| Evolution Gaming | Face Value Sum | 0 |
| Pragmatic Play | Face Value Sum | 0 |
| Hub88 (Aggregator) | Face Value Sum | 0 |

---

## 8. Risk Control Actions

### 8.1 Action Types (v2.1.0)

| Action | Description | Valid Bet | Turnover | Risk Proposal |
|--------|-------------|-----------|----------|---------------|
| **BLOCK** | Real-time block | 0 | Not counted | Not generated |
| **FLAG** | Mark but allow | bet_amount | Normal | Generated |
| **PASS** | Normal pass | bet_amount | Normal | Not generated |

### 8.2 Detection Types

- **Hedge Detection**: Same player, same round, opposite bets
- **Arbitrage Detection**: Cross-platform/cross-market exploitation
- **Low Odds Filter**: Bets below minimum odds threshold (default: 1.5)
- **Same-IP Hedging**: Multiple accounts from same IP betting opposite sides

---

## 9. Compliance Requirements

### 9.1 Monitoring SLA

| Metric | Target | Alert Threshold |
|--------|--------|-----------------|
| Turnover calculation latency (P99) | < 100ms | > 500ms |
| Risk engine call success rate | > 99.9% | < 99% |
| Daily reconciliation deviation | < 0.01% | > 0.01% |
| Event publish success rate | > 99.99% | < 99.9% |

### 9.2 Reconciliation Thresholds

| Deviation Range | Threshold | Business Impact |
|-----------------|-----------|-----------------|
| **< 0.01%** | Acceptable | Minimal (floating point precision) |
| **0.01% - 1%** | Warning | Possible configuration error |
| **> 1%** | Critical | Potential fund risk |

### 9.3 Daily Reconciliation Schedule

- **Execution Time**: 03:00 UTC+8 daily
- **Deviation Threshold**: 0.01% (stricter than industry standard 0.1%)
- **Alert Channels**: Slack + Email for warning, PagerDuty for critical

---

## 10. Recalculation Policy

### 10.1 Trigger Scenarios

| Scenario | Priority | Recalculation Scope | SLA |
|----------|----------|---------------------|-----|
| Status factor config error | P0 | Affected transactions | Immediate |
| Game weight adjustment | P1 | Specified game type | 24 hours |
| GP settlement discrepancy | P1 | Single or batch | 4 hours |
| Promotion rule change | P2 | Promotion transactions | Batch job |

### 10.2 Audit Requirements

All recalculations must:
1. Require approval before execution
2. Create backup of original data
3. Log all changes with before/after values
4. Notify downstream systems upon completion

---

## 11. Key Business Decisions Summary

### Decision 1: HALF_WIN/HALF_LOSS Uses Standard Principal Method
- **Choice**: 100% turnover (not 50%)
- **Reason**: Same betting behavior deserves same turnover contribution

### Decision 2: Free Spins Turnover Calculation
- **Choice**: Turnover = Face Value Sum, Valid Bet = 0
- **Reason**: Industry standard, accurate GGR reporting

### Decision 3: Withdrawal-Time Wagering Verification
- **Choice**: Verify at withdrawal, not auto-unlock on bet
- **Reason**: Protects operator from bonus abuse

### Decision 4: Layer 1 Configuration-Driven Risk Control (v2.1.0)
- **Choice**: Support BLOCK/FLAG/PASS action types
- **Reason**: Flexible risk response without code changes

---

## 12. Related Documents

### Prerequisites
- [00-03 Terminology Standards](../../source/00_Foundation/concepts/00-03_Terminology_Standards.md) - Required reading

### Technical Implementation
- [Turnover_Calculation_Logic.md](../../architecture/03_Game_Integration/Turnover_Calculation_Logic.md) - Technical architecture

### Dependencies
- [05-01 Risk Framework](../../source/05_Risk_Control/05-01_Risk_Framework.md) - Layer 1 risk engine
- [04-04 Activity Bonus](../../source/04_Activity_Center/04-04_Activity_Bonus.md) - Layer 3 activity system

---

**Document Version**: 1.0.0 (derived from source v4.0.0)
**Last Updated**: 2026-02-08
**Maintainers**: Finance Team, Product Team
