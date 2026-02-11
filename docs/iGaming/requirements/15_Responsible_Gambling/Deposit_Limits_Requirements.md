# Deposit & Loss Limits Requirements (存款及虧損限額業務需求)

> **Canonical Source**: [15-02_Deposit_Limits.md](../../source-archive/15_Responsible_Gambling/15-02_Deposit_Limits.md), [15-06_Loss_Limits.md](../../source-archive/15_Responsible_Gambling/15-06_Loss_Limits.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [Deposit_Loss_Limits_Architecture.md](../../architecture/15_Responsible_Gambling/Deposit_Loss_Limits_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This requirements document delivers strategic value by:
- **Regulatory Compliance**: Implements UKGC LCCP SR 3.4.1 mandatory pre-deposit limit setup (effective 2025-10-31) and Germany GlüStV EUR 1,000/month cap
- **Player Harm Prevention**: Establishes asymmetric cooling-off rules (immediate effect for limit decreases, 24-72h delay for increases) preventing impulsive limit removal
- **Loss Limit Protection**: Differentiates deposit limits (input control) from loss limits (outcome control) with real-time net loss tracking
- **Reconciliation Assurance**: Mandates zero-tolerance for system-caused limit breaches with UKGC 24-hour reporting requirement

---

## Acceptance Criteria

- [ ] Pre-deposit limit setup enforced for all new UKGC players (cannot skip to deposit page)
- [ ] Daily, weekly, and monthly deposit limits function correctly with UTC 00:00 reset
- [ ] Limit hierarchy validated: Daily <= Weekly <= Monthly (recommended)
- [ ] Lowering limits takes effect immediately without cooling-off
- [ ] Raising limits enters 24-72 hour cooling-off period (per jurisdiction)
- [ ] Player can cancel limit increase request during cooling-off
- [ ] Loss limit calculation: Net Loss = Total Bets - Total Wins (real-time)
- [ ] Betting blocked when loss limit reached; re-enabled when wins reduce net loss
- [ ] 80% limit warning notification sent to player
- [ ] System-caused limit breaches trigger P0 alert with UKGC reporting within 24 hours

---

## 1. Overview

Deposit Limits and Loss Limits are core player protection tools that help players control their gambling expenditure. This document defines the business requirements for both deposit and loss limit management, including time-based limits, cooling-off periods for changes, and reconciliation.

---

## 2. Regulatory Requirements

### 2.1 Deposit Limits

| Regulator | Clause | Limit Types | Special Requirements |
|-----------|--------|------------|---------------------|
| **UKGC** | LCCP SR 3.4.1 | Daily/Weekly/Monthly | Must prompt before first deposit (2025-10-31) |
| **MGA** | Player Protection Directive | Daily/Weekly/Monthly | Must provide limit options |
| **PAGCOR** | Responsible Gaming Guidelines | Daily/Weekly/Monthly | Operator-defined |
| **Netherlands** | KOA Remote Gambling Act | Daily/Weekly/Monthly | Mandatory upper limits |
| **Germany** | GlüStV 2021 | Monthly | Mandatory cap at EUR 1,000/month |

### 2.2 Loss Limits (Additional)

Loss limits differ from deposit limits in calculation basis:

| Feature | Deposit Limit | Loss Limit |
|---------|--------------|------------|
| **Calculation basis** | Deposit amount | Net loss amount |
| **Purpose** | Control funds deposited | Control actual losses |
| **Formula** | Cumulative deposits | Total bets - Total wins |
| **Win impact** | Not affected | Reduces accumulated loss |

---

## 3. Deposit Limit Types

### 3.1 Time-Based Limits

| Limit Type | Period | Reset Time | Description |
|-----------|--------|-----------|-------------|
| Daily limit | 24 hours | UTC 00:00 | Restricts total daily deposits |
| Weekly limit | 7 days | Monday UTC 00:00 | Restricts total weekly deposits |
| Monthly limit | 30 days | 1st of month UTC 00:00 | Restricts total monthly deposits |

### 3.2 Hierarchy Rules

- Monthly limit >= Weekly limit (recommended)
- Weekly limit >= Daily limit (recommended)
- Validation order: Daily -> Weekly -> Monthly
- Any limit reached -> Block deposit

### 3.3 Default Limits by Jurisdiction

| Jurisdiction | Default Daily | Default Weekly | Default Monthly |
|-------------|--------------|----------------|-----------------|
| UK | No default | No default | No default |
| Netherlands | EUR 200 | EUR 700 | EUR 2,000 |
| Germany | - | - | EUR 1,000 (legal cap) |

---

## 4. Business Rules

### 4.1 Lowering Limits (Deposit & Loss)

- **Effective immediately**, no cooling-off period
- New limit applies to current period accumulation
- If current period deposits already exceed new limit: no retroactive action, but block further deposits
- Confirmation notification sent to player

### 4.2 Raising Limits (Deposit & Loss)

- **24-72 hour cooling-off period** required (per jurisdiction)
- Player can cancel the request during cooling-off
- Requires secondary confirmation to take effect
- Reason recording (optional)

### 4.3 Removing Limits

- Same process as raising limits
- Sets limit to "no limit"
- Cooling-off period applies

---

## 5. Pre-Deposit Limit Setup (UKGC 2025-10-31)

### 5.1 Compliance Requirements

Per UKGC LCCP revised SR 3.4.1, effective October 31, 2025:

| Requirement | Description | Status |
|-------------|-------------|--------|
| **Pre-deposit mandatory setup** | Player must set at least one limit before first deposit | NEW 2025-10-31 |
| **Limit type selection** | At least one of daily/weekly/monthly | Mandatory |
| **Cannot be skipped** | No bypass to deposit page without setting limits | Mandatory |
| **Clear disclosure** | Inform player they can lower limits anytime (immediate effect) | Mandatory |

### 5.2 Registration Flow Integration

```
Registration -> Account Verification -> Mandatory Limit Setup -> Confirm -> Allow Deposit
                                           |
                                           +-- Must complete before accessing deposit page
```

---

## 6. Loss Limit Calculation

### 6.1 Net Loss Formula

```
Net Loss = Total Bets - Total Wins

Where:
- Total Bets: All player bets in the calculation period
- Total Wins: All player winnings (including Jackpots) in the period
```

### 6.2 Calculation Example

```
Scenario: Player sets daily loss limit of $500

Timeline:
09:00 - Deposit $1,000
10:00 - Bet $300, Win $100 -> Net Loss: $200
11:00 - Bet $200, Lose -> Net Loss: $400
12:00 - Bet $100, Lose -> Net Loss: $500 -> LIMIT REACHED
12:01 - Attempt to bet -> BLOCKED
13:00 - Win $150 (from open bet) -> Net Loss: $350 -> CAN CONTINUE
```

### 6.3 Limit Breach Behavior Options

| Option | Description | Use Case |
|--------|------------|----------|
| Block betting | Prohibit new bets | Default behavior |
| Warning + continue | Display warning, player may continue | Some jurisdictions allow |
| Cooling-off | Mandatory 1-hour pause | Strict mode |

---

## 7. Deposit Limit Reconciliation

### 7.1 Reconciliation Purpose

Ensure deposit limits are correctly enforced, preventing system bugs from allowing players to exceed limits.

**Regulatory requirement**: UKGC LCCP SR 3.4.1 - Limits must be effectively enforced. Any violations must be recorded and reported.

### 7.2 Breach Handling Matrix

| Breach Type | Cause | Handling | Reporting |
|------------|-------|---------|-----------|
| **Limit exceeded** | System bug | Refund excess amount + P0 alert | UKGC within 24h |
| **Limit not set** | UKGC player without limit | Block deposits + force setup | Record |
| **Near-limit warning** | > 90% usage | Notify player | None |

---

## 8. Notification Requirements

| Event | Timing | Content |
|-------|--------|---------|
| Limit set/changed | Immediately | Confirmation with new limit values |
| Limit decrease | Immediately | New limit effective immediately |
| Limit increase requested | On request | Cooling-off period info, cancellation option |
| Limit increase effective | After cooling-off | New limit now active |
| Approaching limit (80%) | When 80% reached | Current usage, remaining amount |
| Limit reached | When 100% reached | Limit details, next reset time |

---

## 9. Effectiveness Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| Limit adoption rate | > 15% (UKGC) | Players with active limits / active players |
| Limit breach count | 0 (system) | System-caused limit violations |
| Limit trigger count | Monitor | How often players hit their limits |
| Limit adjustment requests | Monitor | Increase vs. decrease ratio |
| Pending limit increases | Monitor | Volume in cooling-off period |

---

## 10. Testing Scenarios

### Deposit Limits

| Scenario | Expected Result |
|----------|----------------|
| Deposit amount <= remaining limit | Deposit succeeds |
| Deposit amount > remaining limit | Reject, show remaining deposit allowance |
| Lower limit | Immediate effect |
| Raise limit | Enter 24-hour cooling-off |
| Cancel during cooling-off | Successfully cancelled, original limit maintained |
| Cooling-off period ends | New limit auto-activated |

### Loss Limits

| Scenario | Expected Result |
|----------|----------------|
| Bet when net loss < limit | Bet allowed |
| Bet when net loss at limit | Bet blocked, show remaining loss allowance |
| Win reduces net loss below limit | Betting re-enabled |
| Set daily loss limit | Tracks bets minus wins per day |

---

## Related Documents

- [Self_Exclusion_Requirements.md](Self_Exclusion_Requirements.md) - Self-exclusion
- [Session_Protection_Requirements.md](Session_Protection_Requirements.md) - Session protection
- [Affordability_Requirements.md](Affordability_Requirements.md) - Affordability assessment
- [Deposit_Loss_Limits_Architecture.md](../../architecture/15_Responsible_Gambling/Deposit_Loss_Limits_Architecture.md) - Technical architecture

---

**Return**: [Responsible Gambling Module](../../source-archive/15_Responsible_Gambling/README.md) | [iGaming Home](../../source-archive/README.md)
