# Credit Network Business Requirements

> **Canonical Source**: [source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md](../../source-archive/07_Agent_Center/07-02_Credit_Network_Logic.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [Credit Network Architecture](../../architecture/07_Agent_Service/Credit_Network_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 1. Business Overview

The Credit Network is a unique operating model in the Asian market. The core difference from the Cash Market is the **"play first, settle later"** principle. The system must support credit limit allocation, consumption, and repayment, as well as position taking calculations within the agent hierarchy.

---

## 2. Dual Wallet System

To support both cash and credit modes, each player account must have two independent wallets:

| Wallet Type | Description | Key Rules |
|-------------|-------------|-----------|
| **Cash Wallet** | Funded through deposits | Balance must be > 0 to place bets |
| **Credit Wallet** | Credit allocated by parent agent | Bets increase exposure; settlement updates used credit |

### Credit Wallet Key Concepts

| Concept | Definition |
|---------|------------|
| **Credit Limit** | Maximum loss amount granted by the parent agent |
| **Used Credit** | Current unsettled win/loss total |
| **Available Credit** | Credit Limit minus Used Credit |
| **Exposure** | Accumulated bet risk before settlement |

**Business Rule**: When placing bets under credit mode, balance is not deducted. Instead, exposure increases. Settlement occurs periodically to update used credit.

---

## 3. Credit Propagation (Hierarchy)

Credit is distributed from the top level downward in a tree structure:

**Platform > Master Agent > Agent Level 1 > Agent Level 2 > Player**

### 3.1 Allocation Rules

- Parent agents may adjust child credit limits at any time
- Allocations flow top-down only
- Total allocated to children must not exceed parent's available credit

### 3.2 Risk Alert Thresholds

| Usage Range | Risk Level | Automated Action | Notification Recipients |
|-------------|------------|------------------|------------------------|
| **0-80%** | Normal | None | None |
| **81-90%** | Warning | Send alert email | Agent + Parent |
| **91-99%** | High Risk | Reduce child allocation cap | Agent + Parent + Risk Control |
| **100%+** | Critical | Freeze all downstream betting | All levels + Emergency handling |

### 3.3 Liquidation (Margin Call) Rules

- When child's `Used / Limit > 90%` (warning line), the system sends a notification
- When `Used >= Limit` (blowout), the system automatically suspends all downstream player betting rights
- Parent agent's available credit is simultaneously reduced
- Cascading monitoring: parent usage recalculated after child blowout

### 3.4 Credit Network Key Metrics

| Level | Agent | Credit Limit | Used Credit | Usage % | Risk Status | Position % | Downstream Allocation |
|-------|-------|-------------|-------------|---------|-------------|------------|----------------------|
| Platform | Platform | $10M | $8M | 80% | Normal | 10% | $8M |
| Master | Master A | $5M | $3.5M | 70% | Normal | 20% | $4M |
| Master | Master B | $3M | $2.85M | 95% | Margin Call | 15% | $2.8M |
| L1 | Agent A1 | $2M | $1.2M | 60% | Normal | 40% | $800k |
| L1 | Agent A2 | $1.5M | $1.5M | 100% | Frozen | 35% | $0 (Blowout) |
| L1 | Agent A3 | $500k | $300k | 60% | Normal | 30% | $200k |
| L1 | Agent A4 | $2.8M | $2.6M | 93% | Warning | 45% | $200k |
| L2 | Agent A5 | $800k | $500k | 62.5% | Normal | 50% | $300k |
| L2 | Agent A6 | $200k | $150k | 75% | Normal | 45% | $50k |

---

## 4. Position Taking Mechanism

Position taking determines how agents and their parents share win/loss risk from players.

### 4.1 Position Taking Formula

**Agent Win/Loss = Player Win/Loss x Agent Position %**

### 4.2 Position Taking Example

When a player loses $10,000:

| Party | Position % | Amount | Action |
|-------|-----------|--------|--------|
| Player | - | -$10,000 | Pays to direct agent |
| Agent A (Direct) | 40% | +$4,000 | Collects $10k, remits $6k upward |
| Agent B (Parent) | 30% | +$3,000 | Receives from Agent A |
| Master Agent C | 20% | +$2,000 | Receives from Agent B |
| Platform | 10% | +$1,000 | Final platform revenue |

### 4.3 Position Taking Normalization

When total declared positions exceed 100%, normalization is applied:

**Normalization Coefficient = Actual Player Loss / (Player Loss x Total Position %)**

**Example with 120% total positions**:

| Agent | Declared Position | Normalized Amount | Actual Share |
|-------|------------------|-------------------|--------------|
| Agent L2-A5 | 50% | $41,665 | Collects, keeps share, remits remainder |
| Agent L1-A1 | 40% | $33,332 | Receives from child, keeps share |
| Master Agent A | 20% | $16,666 | Receives from child, keeps share |
| Platform | 10% | $8,333 | Final platform revenue |
| **Total** | **120%** | **$99,996** | Approximately equals $100,000 |

### 4.4 Position Constraints

| Rule | Description |
|------|-------------|
| **Max Position** | Child position must not exceed parent-set upper limit |
| **Auto-Hedge** | Agent may set position to 0% (remit all, earn only commission/rebate) |

---

## 5. Settlement Process

### 5.1 Settlement Cycle

Credit networks typically use **weekly** or **monthly** settlement.

| Phase | Time Window | Primary Actions | Responsible Party | Key Metric |
|-------|------------|-----------------|-------------------|------------|
| **Phase 1: Freeze & Calculate** | Monday 12:00 - 13:00 | Freeze accounts, calculate positions, generate statements | System Automated | Accuracy: 100% |
| **Phase 2: Payment Collection** | Monday 13:00 - Friday 18:00 | Agent remittance, transaction verification | Agents | On-time payment rate: > 95% |
| **Phase 3: Verification & Reset** | Friday 18:00 - Saturday 12:00 | Verify payments, restore credit limits | Parent Agents | Verification accuracy: 100% |
| **Phase 4: Final Report** | Saturday 12:00 | Generate weekly report, audit report | Platform | Report completeness: 100% |

### 5.2 Settlement Work Orders

The system provides a "settlement ticket" function:

| Field | Options |
|-------|---------|
| **Type** | Deposit (remit winnings) / Refill (cover losses) |
| **Status** | Pending > Reviewed > Completed |

### 5.3 Position Distribution Calculation (Bottom-Up Aggregation)

1. **Step 1**: Calculate Agent L2 position from direct player losses
2. **Step 2**: Aggregate L1 position from children + direct players
3. **Step 3**: Aggregate Master Agent position from children
4. **Step 4**: Calculate final platform revenue

### 5.4 Overdue Payment Rules

| Overdue Days | Automated Action | Credit Impact | Business Impact |
|-------------|------------------|---------------|-----------------|
| **0-3 days** | Send collection email | None | None |
| **4-7 days** | Reduce Credit Limit by 50% | Existing limit halved | Downstream players partially restricted |
| **8-14 days** | Freeze Credit Limit | Credit Limit = 0 | All downstream cannot bet |
| **15+ days** | Account suspension + Legal action | Permanently frozen | Close agent account |

### 5.5 Settlement Exception Handling

| Exception Type | Detection Method | Handling Strategy | Notification |
|---------------|-----------------|-------------------|--------------|
| Position calculation error | SUM(child_positions) != player_loss | Trigger manual review + rollback | Tech team + Finance |
| Duplicate settlement | settlement_week already exists | Reject re-execution + alert | System admin |
| Credit over-limit | Available Credit < 0 after restore | Limit credit to 0 + manual check | Risk control + Parent agent |
| Payment verification failure | Payment not found | Mark as overdue + send collection notice | Agent + Parent |

---

## 6. Credit Allocation Business Rules

| Operation Type | Parent Available | Child Used | Allowed? | Automated Action |
|---------------|-----------------|-----------|----------|------------------|
| **First allocation** | >= Amount | N/A (new) | Yes | Create child record |
| **Increase limit** | >= Delta | Any | Yes | Parent allocated += delta |
| **Decrease limit** | Any | <= New Limit | Yes | Parent allocated -= delta |
| **Decrease limit** | Any | > New Limit | No | Reject: Child used credit exceeds new limit |
| **Revoke limit** | Any | > 0 | No | Reject: Must settle child used credit first |
| **Revoke limit** | Any | = 0 | Yes | Set child limit = 0, parent allocated -= old_limit |

### 6.1 Notification Trigger Rules

| Condition | Trigger Threshold | Recipients | Method | Example |
|-----------|-------------------|------------|--------|---------|
| Large limit increase | Delta > $10,000 | Child + Parent + Risk | Email + SMS | "Your credit limit increased to $1.5M (+$500k)" |
| Large position change | Position change > 10% | Child + Parent + Finance | Email | "Position changed from 40% to 50%" |
| Limit revocation | New Limit < Old Limit | Child + Parent | Email + In-App | "Credit limit reduced to $500k due to [Reason]" |
| Near limit | Used / Limit > 90% | Child + Parent | Email + SMS | "Warning: 92% credit used ($920k / $1M)" |
| Over limit | Used > Limit | Child + Parent + Risk | Urgent: Phone + Email | "CRITICAL: Credit limit exceeded. Account frozen." |

---

## 7. Risk Control Rules

### 7.1 Abnormal Position Monitoring

If an agent suddenly adjusts position to 100% and downstream players win large amounts, a "joint arbitrage" alert is triggered.

### 7.2 Credit Scoring

Based on historical settlement punctuality, agents receive a credit score that affects the maximum obtainable credit limit.

### 7.3 Risk Scenario Matrix

| Risk Scenario | Detection Indicator | Threshold | Handling Action |
|--------------|---------------------|-----------|-----------------|
| **Joint arbitrage** | Sudden position increase + large player wins | Position change > 50% AND player net win > $100k | Freeze credit + Manual review |
| **Credit fraud** | New agent high-amount application | Registered < 30 days AND requested > $50k | Reduce limit + Enhanced KYC |
| **Settlement overdue** | High historical overdue rate | Overdue >= 3 times/year | Lower credit score + Limit restriction |
| **Multi-account abuse** | Same device multiple agents | Device fingerprint linked >= 3 agents | Block allocation + Investigation |

### 7.4 Cascading Impact on Blowout

When an agent reaches 100% usage:
1. All downstream players are automatically frozen from betting
2. System sends margin call notification
3. Parent agent's available credit simultaneously decreases
4. Cascading monitoring: Parent usage recalculated (may approach warning line)
5. Settlement frozen: Agent must cover losses before weekly settlement to restore credit

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Agent Network Team
