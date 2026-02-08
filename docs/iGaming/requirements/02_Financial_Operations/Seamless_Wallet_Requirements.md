# Seamless Wallet Business Requirements

> **Canonical Source**: [03-03_Seamless_Wallet_Analysis.md](../../source/03_Game_Center/03-03_Seamless_Wallet_Analysis.md)
> **Audience**: Executives, Product Managers, Compliance Officers
> **Related Doc**: [Seamless Wallet Analysis (Architecture)](../../architecture/02_Finance_Service/Seamless_Wallet_Analysis.md)
> **Last Synced**: 2026-02-08

---

## 1. Purpose

This document defines the business rules, policies, and operational requirements for the Seamless Wallet (Single Wallet) integration with Game Providers (GP). It covers transaction types, settlement rules, player-facing policies, risk scenarios, and compliance requirements.

---

## 2. Game Provider Integration Types

### 2.1 Transaction Model Classification

| Type | Description | Representative Providers | Business Implications |
|:-----|:------------|:------------------------|:---------------------|
| **Transaction-Based** | Each request is an independent transaction (Debit/Credit) with Amount or Type | PG Soft, JILI | Simplest model. TransactionId must be globally unique. |
| **Round-Based** | Requests bound to a RoundId. Bet opens a Round, Win closes it. | Evolution, Pragmatic Play | Risk of "orphaned rounds" if Bet succeeds but Win is lost. Requires scheduled monitoring. |
| **Transfer-Based** | Legacy architecture simulating TransferIn / TransferOut | Certain legacy sportsbooks | Semantic confusion with "transfer wallet." Must ensure actions are system-triggered, not manual. |
| **Result-Only** | Only sends game results (Win/Loss Amount); no Bet/Win distinction | Certain lottery/card games | Risk control difficulty: cannot validate balance at bet time, may result in negative balance. |

### 2.2 Wallet Behavior Classification

| Behavior | Description | Risk & Policy |
|:---------|:------------|:-------------|
| **GetBalance Only** | GP only queries balance; balance changes recorded internally by GP with periodic settlement | **Extremely high risk.** Not recommended unless operating under a credit model. |
| **Async Callback** | Platform returns HTTP 200 as acknowledgment only; actual deduction result delivered via callback | Requires bidirectional state machine; doubles process complexity. |
| **Batch Processing** | GP combines multiple player transactions into a single HTTP request | Must support batch transaction processing. Business must decide: All-or-Nothing vs. Partial Success. |

---

## 3. Round Lifecycle Management (Round-Based Model)

### 3.1 Round State Definitions

| State | Trigger Condition | Automated Action | Manual Intervention |
|-------|------------------|-----------------|-------------------|
| **OPEN** | Bet request succeeds | Debit balance + create round record | None |
| **CLOSED** | Win request succeeds | Credit balance + close round | None |
| **TIMEOUT** | Open for more than 2 hours without Win | Query GP API for final status | If GP API fails, escalate to PENDING_REVIEW |
| **PENDING_REVIEW** | GP status unknown | Create CS support ticket | Required: CS agent manually closes or cancels |
| **CANCELLED** | Rollback request received | Refund bet + mark as cancelled | None |
| **ADJUSTED** | Resettlement request received | Adjust balance (may go negative) | If negative balance results, trigger risk control lock |

### 3.2 Orphaned Round Policy

An orphaned round occurs when a Bet succeeds but a Win never arrives.

- **Detection**: Scheduled job scans every 15 minutes for rounds in OPEN state for more than 2 hours
- **Resolution**: Query GP API for final status; auto-close if confirmed, or escalate to manual review
- **Escalation Criteria**:
  - Round amount greater than $1,000: HIGH priority, 24-hour SLA
  - Round amount $1,000 or less: MEDIUM priority, 24-hour SLA

### 3.3 Duplicate Win Policy

- Duplicate Win requests (same roundId + txId) must return the original execution result
- No duplicate credit shall be applied
- Enforcement via unique constraint on (roundId, txId) plus idempotency checks

### 3.4 Resettlement Negative Balance Policy

When a GP claws back overpaid winnings and the player balance is insufficient:

- The platform allows the balance to go negative
- The player account is automatically locked
- A risk control alert is triggered
- Recovery follows the negative balance handling policy (see Section 7)

---

## 4. Transaction Scenarios & Business Rules

### 4.1 Normal Transaction Flow (Happy Path)

**Bet (Debit)**:
1. GP sends debit request with user, amount, roundId, txId
2. Platform verifies balance is sufficient
3. Platform deducts balance, records transaction, returns new balance

**Win (Credit)**:
1. GP sends credit request with user, amount, roundId, txId, refTxId
2. Platform credits balance, records transaction, returns new balance
3. The refTxId links the win back to the original bet

### 4.2 Insufficient Funds (Scenario A)

- When player balance is less than bet amount, the platform returns an INSUFFICIENT_FUNDS error
- The GP must display an insufficient balance message and prevent game play
- If a bonus wallet is available and the game supports it, dual-wallet deduction applies (see Section 6)

### 4.3 Concurrent Betting (Scenario B)

- When multiple bet requests arrive simultaneously for the same player, the platform must prevent over-deduction
- Business rule: Only one bet may be processed at a time per player
- If contention is detected and unresolvable within retry limits, return "System Busy - Retry Later"

### 4.4 Timeout & Retry (Scenario C)

- When a GP times out waiting for a response, it may retry with the same txId
- Business rule: Retries with the same txId must return the cached original result with no re-execution
- A transaction without a txId must be rejected (HTTP 400)

### 4.5 Out-of-Order Requests (Scenario D)

When a Win request arrives before its corresponding Bet:

| Strategy | Description | Pros | Cons | Recommendation |
|----------|------------|------|------|---------------|
| **Strategy 1: Immediate Reject** | Return BET_NOT_FOUND error | Simple, stateless | Win may be lost if GP does not retry | Suitable for reliable GPs with retry mechanisms |
| **Strategy 2: Allow Orphan Win** | Credit win without matching bet | Player does not lose winnings | High risk of double credit; reconciliation difficulty | **Not recommended** except as emergency fallback |
| **Strategy 3: Pending Queue** | Store win in pending queue (TTL 30 min) | Automated resolution; full audit trail | Higher implementation complexity | **Recommended for production** |

**Production recommendation**: Strategy 3 as default, with automatic degradation to Strategy 1 under system pressure.

### 4.6 Rollback / Refund (Scenario E)

- When a game round is cancelled (e.g., sports event interrupted, system failure), the GP sends a Rollback request
- Platform finds the original transaction and executes a reverse operation (refund)
- If the original transaction is not found (bet never arrived), the platform returns Success (goal achieved: no deduction occurred)

### 4.7 Resettlement / Adjustment (Scenario F)

- When the GP discovers an incorrect payout (e.g., wrong odds), it sends an Adjust request
- Platform supports multiple credits to the same round, including negative credits (clawback)
- If clawback causes a negative balance, the platform allows it, locks the account, and triggers a risk alert
- Manual recovery process: freeze withdrawals, contact player, arrange installment recovery

### 4.8 Jackpot & Big Win (Scenario I)

- Triggered when a player wins more than $10,000 or hits a progressive jackpot
- **Manual Approval mode**: GP sends NotifyWin; funds are held pending human review before crediting
- **Auto Credit mode**: Funds credited immediately, account frozen, risk review triggered
- Protocol mode depends on the GP agreement

---

## 5. Promotion & Bonus Rules

### 5.1 Free Spin / Free Round (Scenario G)

- GP notifies the platform with a Bet amount of 0 and actual Win amount
- Platform must accept Amount=0 transactions
- Platform must validate the player's free spin quota before acceptance
- If quota is exhausted, reject with "Free Spin Quota Exceeded"

### 5.2 Bonus Wallet (Scenario H)

- Players may have both a Cash wallet and a Bonus wallet (e.g., Cash 100 + Bonus 50 for slots only)
- When the game does not support bonus play, only the Cash wallet is used
- If Cash balance is insufficient (e.g., bet 120 but Cash is only 100), the platform returns insufficient funds even if total assets are 150
- When the game supports bonus play, dual-wallet deduction applies (Cash first, then Bonus if insufficient)

---

## 6. Wallet Deduction Order Policy

### 6.1 Default Deduction Sequence

The system default deduction order is: **Bonus -> Cash -> Credit**

### 6.2 Game-Level Override

- Each game or GP may configure a custom `wallet_priority` that overrides the system default
- The configuration must explicitly mark whether it overrides the default

### 6.3 Deduction Execution Rules

1. Receive Bet request
2. Check if the game has a custom wallet_priority configuration
3. If custom configuration exists and override is enabled, use the game configuration
4. If no custom configuration exists, use the system default
5. Deduct balance in priority order
6. Mixed deduction is permitted when the first-priority wallet has insufficient balance (split transaction details must be recorded)

---

## 7. Negative Balance Handling Policy

### 7.1 Decision

Allow negative balance with automatic lock and manual intervention.

### 7.2 Trigger Scenarios

GP-initiated Rollback or Resettlement that deducts funds exceeding the player's current balance.

### 7.3 Handling Procedure

1. Force the deduction, setting balance below zero
2. Immediately change account status to LOCKED or SUSPENDED
3. Block all new logins, bets, and withdrawals
4. Send a high-priority alert to the Risk Control team
5. Only administrators with RISK_MANAGER permission may unlock the account

---

## 8. Authentication & Session Policy

### 8.1 Session Expiry Rules

| Request Type | Player Token Expired | Business Rule |
|-------------|---------------------|--------------|
| **Bet** | Reject (Error: Token Expired) | Player cannot initiate new bets after session expiry |
| **Win** | **Must Accept** | Bet occurred during valid session; winnings are player's rightful asset regardless of settlement timing |

---

## 9. Currency & Exchange Policy

### 9.1 Precision Requirement

- All monetary calculations must support 4 decimal places
- Rounding errors from 0.0001-level cumulative differences must be prevented

### 9.2 Multi-Currency Policy

- If the GP does not support the player's currency, an intermediate exchange layer is required
- **Recommendation**: Configure the GP to operate directly in the player's currency to avoid exchange risk
- Unit conversion (e.g., cents vs. dollars) must be explicitly defined per GP integration

---

## 10. Reconciliation Requirements

### 10.1 Daily Reconciliation

- The platform must download each GP's Transaction Report daily and compare it against the platform database
- Automated Diff Reports must be generated for: mismatched transaction IDs, mismatched amounts

### 10.2 Extreme Case Policy

Even with retries, edge cases may occur where the GP considers a transaction successful while the platform considers it failed (e.g., database commit failure after successful response). Daily reconciliation is the safety net for these scenarios.

---

## 11. Scenario Priority Matrix

| Scenario | Detection Point | Priority | Response Time | Risk Level | Automation Level |
|----------|----------------|----------|--------------|------------|-----------------|
| A - Insufficient Funds | Bet Request | P0 | < 100ms | LOW | 100% automated |
| B - Concurrent Race | Bet Request | P0 | < 200ms | MEDIUM | 100% automated (distributed lock) |
| C - Timeout Retry | Win Timeout | P1 | 2 hours | MEDIUM | 90% automated (active query) |
| D - Out-of-Order | Win Request | P1 | < 2 hours | MEDIUM | 95% automated (pending queue) |
| E - Rollback / Refund | Rollback Request | P1 | < 500ms | MEDIUM | 100% automated |
| F - Resettlement | Adjust Request | P2 | < 1s | HIGH | 50% automated (negative balance requires manual) |
| G - Free Spin | Bet Request | P0 | < 100ms | LOW | 100% automated |
| H - Bonus Wallet | Bet Request | P0 | < 150ms | LOW | 100% automated |
| I - Jackpot | Win Request | P0 | Manual review | HIGH | 0-50% automated (depends on GP agreement) |

---

## 12. Exception Handling Decision Table

| Exception Type | Detection Method | Degradation Strategy | Compensation Mechanism | Manual Intervention Threshold |
|---------------|-----------------|---------------------|----------------------|------------------------------|
| Lock timeout | 3 retry failures | Return "System Busy" | Player retries | Lock wait exceeds 10s |
| GP timeout | 2 hours no response | Active GP query | Compensatory credit or refund | Query fails 3 consecutive times |
| Out-of-order backlog | Pending queue exceeds 100 | Alert + scale capacity | Extend TTL to 4 hours | Queue exceeds 500 |
| Negative balance | Balance drops below 0 after deduction | Allow negative balance | Freeze withdrawals + manual recovery | Negative balance below -$1,000 |
| Jackpot anomaly | Amount exceeds $50k | Force manual review | Withhold credit | All jackpots |
| Idempotency cache expiry | Redis cache expired | Query database | Rebuild cache (TTL 1 hour) | No record in database |

---

## 13. Strategy Switching Policy (Out-of-Order Handling)

### 13.1 Degradation Triggers

| Trigger Condition | Metric | Threshold | Switch To | Recovery Condition |
|------------------|--------|-----------|-----------|-------------------|
| Pending queue backlog | pending_win_queue_size | > 500 | Strategy 3 to Strategy 1 | Queue < 100 for 10 minutes |
| Redis unavailable | redis_connection_status | DISCONNECTED | Strategy 3 to Strategy 1 | Redis restored + 5-minute stability |
| Database latency too high | db_query_latency_p99 | > 5s | Strategy 3 to Strategy 1 | Latency < 1s for 10 minutes |
| System CPU too high | system_cpu_usage | > 90% | Strategy 3 to Strategy 1 | CPU < 70% for 10 minutes |
| GP retry rate too low | gp_retry_success_rate | < 80% | Strategy 1 to Strategy 2 (emergency) | Manual recovery only |

### 13.2 Implementation Guidelines

1. **Default strategy**: Strategy 3 (Pending Queue)
2. **Automatic degradation**: When system pressure exceeds thresholds, degrade to Strategy 1
3. **Emergency mode**: When GP retry rate drops below 80%, switch to Strategy 2 (requires manual recovery)
4. **Automatic recovery**: When system metrics stabilize, auto-upgrade to Strategy 3
5. **Alerting**: All strategy switches must trigger Slack alerts and audit log entries

---

## 14. Monitoring KPIs

| KPI | Formula | Target |
|-----|---------|--------|
| Out-of-order frequency | (pending_wins_count / total_wins) x 100% | < 0.1% |
| Pending queue size | Real-time monitoring | Alert threshold > 100 |
| Timeout escalation rate | (escalated_to_manual / pending_wins) x 100% | < 1% |
| Insufficient funds rejection rate | (rejected_insufficient / total_bets) x 100% | Alert if > 30% |
| Negative balance accumulation | sum(player_negative_balance) | Alert if < -$10,000 |
| Jackpot frequency | jackpot_win_count per hour | Alert if > 5 per hour |

---

## 15. Idempotency Requirements

### 15.1 Mandatory TransactionId

- All GP integrations **must** provide a globally unique `transaction_id` (or `request_id`) in the request header or body
- Requests without a transaction ID are rejected with HTTP 400 Bad Request

### 15.2 Duplicate Handling

- Duplicate transaction IDs must return the previously cached response
- Business logic must never execute twice for the same transaction ID
- Cache TTL: 3,600 seconds (1 hour), unified across all wallet modules

---

## 16. Related Documents

### Core Dependencies
- Unified Wallet Model - Wallet balance updates, locking mechanisms
- Game Integration Standard - GP API specifications, security design

### Business Integration
- Turnover Calculation & Reconciliation - Game reconciliation, bet validation
- Risk Framework - Abnormal betting detection, negative balance alerts

### Extended Reading
- Game Lobby Management - Game entry management
- Maintenance Procedures - Game maintenance and balance synchronization
