# Turnover Validation Requirements

> **Canonical Source**: [05-07_Turnover_Validation_Scheme.md](../../source/05_Risk_Control/05-07_Turnover_Validation_Scheme.md)
> **Audience**: Executives, Product Managers, Risk Operations, Compliance Officers
> **Related Doc**: [Turnover_Validation_Architecture.md](../../architecture/05_Risk_Engine/Turnover_Validation_Architecture.md)
> **Last Synced**: 2026-02-08

---

## 1. Problem Statement

### 1.1 Long-Cycle Turnover Validation Performance

When a player has not withdrawn for an extended period (e.g., 3--5 years), the system must calculate all turnover since the last withdrawal. The traditional approach requires scanning millions of bet records, causing:

| Problem | Business Impact |
|---------|-----------------|
| Query Duration | Scanning years of data can exceed 30 seconds |
| Database Pressure | Full table scans cause I/O spikes |
| Timeout Risk | Withdrawal requests may fail due to query timeout |

### 1.2 Promotion Risk Control Gap

The current system lacks adequate protection against bonus exploitation:

| Abuse Pattern | Description |
|---------------|-------------|
| Low-Odds Turnover | Players use low-risk bets to complete turnover requirements (arbitrage) |
| Hedge Betting | Offsetting bets eliminate risk before withdrawing bonus funds |
| Missing Invalidation | No mechanism to mark certain bets as contributing zero turnover |

---

## 2. Checkpoint Snapshot Solution

### 2.1 Core Mechanism

Each successful withdrawal creates a turnover snapshot. Subsequent withdrawal validations only need to calculate the difference between the current cumulative turnover and the last snapshot -- regardless of how much time has passed.

**Validation Formula**:

| Component | Definition |
|-----------|------------|
| Current Total Valid Bets | Real-time aggregation of all valid bets to date |
| Last Snapshot Valid Bets | Cumulative valid bets recorded at the last withdrawal |
| **Period Valid Turnover** | **Current Total - Last Snapshot Total** |

**Performance**: Validation is O(1) -- constant time regardless of the time span since last withdrawal.

### 2.2 Snapshot Trigger Types

| Trigger Type | When Created | Purpose |
|--------------|-------------|---------|
| WITHDRAWAL | After each successful withdrawal | Standard checkpoint |
| MANUAL | Operator-initiated | Exception handling or corrections |
| SCHEDULED | Periodic batch process | Preventive snapshots for long-inactive players |

### 2.3 Snapshot Data Captured

Each snapshot records the following cumulative totals at the moment of creation:

| Data Point | Description |
|------------|-------------|
| Total Bet Amount | Cumulative total of all bets placed |
| Total Valid Bet Amount | Cumulative total of bets counted as valid turnover |
| Total Win Amount | Cumulative total of all winnings |
| Snapshot Timestamp | Exact time the snapshot was taken |
| Trigger Reference | The withdrawal order ID or other trigger identifier |

---

## 3. Time Window Alignment with Risk Proposals

A key compliance requirement is that the turnover validation window and the risk proposal query window use the same time boundaries:

| Dimension | Query Range | Description |
|-----------|-------------|-------------|
| Risk Proposal Query | Last Snapshot Time to present | All unresolved proposals since last withdrawal |
| Turnover Validation | Last Snapshot Time to present | Difference calculation |
| Long-Cycle Handling | No fixed day limit | Even for 5-year gaps, query all URGENT/HIGH proposals since last snapshot |

This alignment ensures that risk flags and turnover calculations always refer to the same period, eliminating gaps in compliance coverage.

---

## 4. Activity Risk Integration (Dual-Layer Protection)

### 4.1 Prevention Layer (Pre-Emptive)

Controls applied before or during betting:

| Control | Mechanism |
|---------|-----------|
| Claim Interception | IP checks, device fingerprint validation before bonus issuance |
| Bet Interception -- Low Odds | Bets below odds threshold contribute zero turnover |
| Bet Interception -- Hedging | Detected hedge bet pairs contribute zero turnover |

### 4.2 Detection Layer (Post-Hoc)

Controls applied at withdrawal or during periodic review:

| Control | Mechanism |
|---------|-----------|
| Withdrawal Pre-Scan | Check for abnormal behaviour proposals before approving withdrawal |
| Risk Rule Triggers | Automated rules flag suspicious turnover patterns |
| Reporting and Analytics | Activity ROI monitoring; abuser identification lists |

---

## 5. Invalid Turnover Rules

The following betting behaviours contribute zero to valid turnover:

| Rule Code | Condition | Rationale |
|-----------|-----------|-----------|
| LOW_ODDS | Odds below 1.3 | Low-odds arbitrage risk |
| HEDGE_BET | Detected hedge bet combination | Risk-free arbitrage |
| MIN_BET_BONUS | Minimum bet amount + activity turnover | Bonus chasing behaviour |
| SAME_EVENT_OPPOSITE | Opposite bets on the same event | Guaranteed-outcome betting |

---

## 6. UI Requirements

### 6.1 Proposal Review Page

- Highlight tags: "Associated Risk", "Activity Arbitrage", "Bonus Chasing"
- Display the associated promotion name and turnover completion progress

### 6.2 Bet Details Page

- Mark invalid turnover bets with labels (e.g., "[Low Odds] Turnover: 0.00", "[Hedge] Turnover: 0.00")
- Provide a valid turnover percentage statistic for the player

---

## 7. Acceptance Criteria

| Requirement | Threshold |
|-------------|-----------|
| Snapshot Mechanism | Automatic snapshot creation after each successful withdrawal |
| Validation Performance | Turnover validation under 50ms (P95), independent of time span |
| Invalid Turnover | Low-odds / hedge / bonus chasing bets counted as zero turnover |
| Time Alignment | Risk proposal query window = turnover validation window = last snapshot to present |
| UI Alerts | Review page shows activity risk tags; bet page marks invalid turnover |

---

## 8. Compliance Requirements

- Turnover validation must apply equally to all players regardless of VIP status
- All snapshot creation events must be recorded in the audit log
- Invalid turnover rules must be configurable by operators without code changes
- Time window alignment must be maintained for regulatory reporting purposes

---

## 9. Related Documents

- [05-01 Risk Framework](../../source/05_Risk_Control/05-01_Risk_Framework.md) -- Configuration-driven risk rule engine
- [05-05 Risk Proposal Workflow](../../source/05_Risk_Control/05-05_Risk_Proposal_Workflow.md) -- Proposal lifecycle management
- [05-06 Withdrawal Risk Correlation](../../source/05_Risk_Control/05-06_Withdrawal_Risk_Correlation.md) -- Withdrawal risk scoring

---

## 10. Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-05 | Initial version -- Checkpoint Snapshot definition, dual-layer protection, invalid turnover rules |
