# Seamless Wallet Technical Architecture

> **Canonical Source**: [03-03_Seamless_Wallet_Analysis.md](../../source/03_Game_Center/03-03_Seamless_Wallet_Analysis.md)
> **Audience**: Architects, Backend Developers, DevOps
> **Related Doc**: [Seamless Wallet Requirements](../../requirements/02_Financial_Operations/Seamless_Wallet_Requirements.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

This document covers the technical architecture and implementation details for the Seamless Wallet integration system. It includes API specifications, concurrency control mechanisms, state machines, database schemas, monitoring configurations, and SmartAdmin architecture mapping.

---

## 2. Round-Based State Machine

### 2.1 State Diagram

```mermaid
stateDiagram-v2
    [*] --> IDLE: No Active Round

    IDLE --> OPEN: Bet Request Received

    state OPEN {
        [*] --> WaitingWin: Bet Success
        WaitingWin --> WaitingWin: Additional Bet (Same Round)
    }

    OPEN --> CLOSED: Win Request Received
    OPEN --> CANCELLED: Rollback Request
    OPEN --> TIMEOUT: No Win After 2 Hours

    state TIMEOUT {
        [*] --> QueryGP: Query GP API
        QueryGP --> GPClosed: GP Status = CLOSED
        QueryGP --> GPPending: GP Status = PENDING
        QueryGP --> GPError: GP API Error
    }

    GPClosed --> CLOSED: Auto Close Round
    GPPending --> PENDING_REVIEW: Manual Review Required
    GPError --> PENDING_REVIEW

    state PENDING_REVIEW {
        [*] --> CSReview: CS Agent Investigation
        CSReview --> ManualClose: Approve Close
        CSReview --> ManualCancel: Approve Cancel
    }

    ManualClose --> CLOSED
    ManualCancel --> CANCELLED
    CLOSED --> ADJUSTED: Resettlement Request

    ADJUSTED --> [*]
    CLOSED --> [*]
    CANCELLED --> [*]

    note right of OPEN
        Bet Request Received
        Action: Debit Balance
        Create Round Record
        Status = OPEN
        Additional Bet: Accumulate bet amount
    end note

    note right of CLOSED
        Win Request Received
        Action: Credit Balance
        Update Round
        Status = CLOSED
        Auto Close: Credit Win Amount
        Source: GP Query
    end note

    note right of CANCELLED
        Rollback Request
        Action: Refund Bet
        Status = CANCELLED
    end note

    note right of TIMEOUT
        No Win After 2 Hours
        Orphaned Round
        Trigger: Scheduled Job
        Run every 15 minutes
        Check Rounds WHERE status=OPEN
        AND created_at < NOW() - 2 hours
        Query GP API for final status
        GET /round/{id}/status
    end note

    note right of PENDING_REVIEW
        Manual Review Required
        Notify CS Team
        Escalate if > $1000
        HIGH Priority: Amount > $1000
        MEDIUM Priority: Amount <= $1000
        SLA: 24 hours response
    end note

    note right of ADJUSTED
        Resettlement Request
        Action: Adjust Balance
        Status = ADJUSTED
    end note
```

---

## 3. Idempotency Implementation

### 3.1 Timeout & Retry Sequence Diagram

```mermaid
sequenceDiagram
    participant GP as Game Provider
    participant GW as API Gateway
    participant Platform as Wallet Service
    participant Redis as Redis Cache
    participant DB as Database

    Note over GP,DB: Scenario C: Timeout & Retry with Idempotency

    rect rgb(255, 230, 230)
        Note over GP,DB: First Attempt - Timeout Occurs

        GP->>GW: POST /debit<br/>{txId: "TX001", amount: 100}
        GW->>Platform: validateRequest(txId, amount)

        Platform->>Redis: GET idempotency:TX001
        Redis-->>Platform: null (Not Exists)

        Platform->>DB: BEGIN TRANSACTION

        Platform->>DB: SELECT balance FROM wallet<br/>WHERE player_id = ? FOR UPDATE
        DB-->>Platform: balance = 500

        Platform->>DB: UPDATE wallet<br/>SET balance = 400<br/>WHERE player_id = ?
        DB-->>Platform: OK (1 row affected)

        Platform->>DB: INSERT INTO transactions<br/>(tx_id, type, amount, balance_after)<br/>VALUES ('TX001', 'DEBIT', 100, 400)
        DB-->>Platform: OK

        Platform->>DB: COMMIT

        Platform->>Redis: SETEX idempotency:TX001<br/>value: {status: SUCCESS, balance: 400}<br/>TTL: 3600
        Redis-->>Platform: OK

        Platform->>GW: Response: {status: SUCCESS, balance: 400}

        Note over GW,GP: Network Error - Response Lost!
        GW-xGP: TCP Connection Reset / Timeout

        Note over GP: GP receives TIMEOUT<br/>Status Unknown<br/>Decide to RETRY
    end

    rect rgb(230, 255, 230)
        Note over GP,DB: Second Attempt - Idempotent Retry (Same txId)

        GP->>GW: POST /debit<br/>{txId: "TX001", amount: 100}<br/>SAME txId

        GW->>Platform: validateRequest(txId, amount)

        Platform->>Redis: GET idempotency:TX001
        Redis-->>Platform: {status: SUCCESS, balance: 400}<br/>EXISTS!

        Note over Platform: Idempotency Check PASSED<br/>Return Cached Response<br/>NO Database Operation!

        Platform-->>GW: Response: {status: SUCCESS, balance: 400}<br/>Source: CACHED

        GW-->>GP: Response: {status: SUCCESS, balance: 400}

        Note over GP: Retry SUCCESS<br/>Received Response<br/>Player Balance: 400
    end

    rect rgb(230, 230, 255)
        Note over GP,DB: Third Attempt - Different txId (New Transaction)

        GP->>GW: POST /debit<br/>{txId: "TX002", amount: 50}<br/>NEW txId

        GW->>Platform: validateRequest(txId, amount)

        Platform->>Redis: GET idempotency:TX002
        Redis-->>Platform: null (Not Exists)

        Platform->>DB: BEGIN TRANSACTION

        Platform->>DB: SELECT balance FROM wallet<br/>WHERE player_id = ? FOR UPDATE
        DB-->>Platform: balance = 400

        Platform->>DB: UPDATE wallet<br/>SET balance = 350<br/>WHERE player_id = ?
        DB-->>Platform: OK

        Platform->>DB: INSERT INTO transactions<br/>(tx_id, type, amount, balance_after)<br/>VALUES ('TX002', 'DEBIT', 50, 350)
        DB-->>Platform: OK

        Platform->>DB: COMMIT

        Platform->>Redis: SETEX idempotency:TX002<br/>value: {status: SUCCESS, balance: 350}<br/>TTL: 3600
        Redis-->>Platform: OK

        Platform->>GW: Response: {status: SUCCESS, balance: 350}
        GW-->>GP: Response: {status: SUCCESS, balance: 350}

        Note over GP: New Transaction SUCCESS<br/>Player Balance: 350
    end

    style Platform fill:#E6E6FA
    style Redis fill:#FFE4B5
    style DB fill:#ADD8E6
```

### 3.2 Idempotency Component Design

| Component | Strategy | Key Parameters |
|-----------|----------|---------------|
| **Uniqueness guarantee** | txId as Redis Key + DB unique constraint | `UNIQUE INDEX (tx_id)` |
| **Cache TTL** | Redis SETEX to avoid permanent memory occupation | 3600 seconds (1 hour) |
| **Response consistency** | Cache full response (balance, status) | JSON format storage |
| **Concurrency protection** | Redis SETNX or Lua Script | Atomic operation |
| **Cleanup strategy** | TTL auto-expiry + periodic batch cleanup | Daily cleanup of records older than 24 hours |

### 3.3 Error Handling

1. **Redis unavailable**: Degrade to DB unique constraint check only (performance degraded but correctness preserved)
2. **DB unique constraint conflict**: Catch exception, query original record, return original result
3. **txId format error**: 400 Bad Request, reject request

---

## 4. Out-of-Order Decision Tree

### 4.1 Strategy Decision Flowchart

```mermaid
flowchart TD
    START[Receive Win Request] --> CHECK{"Check refTxId<br/>SELECT * FROM transactions<br/>WHERE tx_id = refTxId"}

    CHECK -->|Bet Found| NORMAL["Normal Flow<br/>Credit Win Amount<br/>Link to Bet<br/>Return SUCCESS"]

    CHECK -->|Bet NOT Found| STRATEGY{Choose Strategy}

    STRATEGY -->|Strategy 1<br/>Immediate Reject| S1["Return Error:<br/>BET_NOT_FOUND<br/>HTTP 400<br/>Error Code: 1001"]

    S1 --> S1A{GP Retries Win?}
    S1A -->|Yes - After Bet Arrives| S1B["Next Retry:<br/>Bet Exists then SUCCESS"]
    S1A -->|No - GP Gives Up| S1C["Player Lost Win<br/>Requires Manual Investigation"]

    STRATEGY -->|Strategy 2<br/>Allow Orphan Win| S2["Allow Win Without Bet<br/>Credit Amount<br/>Mark: ORPHAN_WIN<br/>Flag for Review"]

    S2 --> S2A{Bet Arrives Later?}
    S2A -->|Yes| S2B["Problem:<br/>Double Credit Risk<br/>Player got Win twice"]
    S2A -->|No| S2C["Reconciliation Mismatch<br/>GP has Bet, Platform has only Win"]

    STRATEGY -->|Strategy 3<br/>Pending Queue| S3["Store Win in Pending Queue<br/>INSERT INTO pending_wins<br/>(ref_tx_id, amount, expires_at)<br/>TTL: 30 minutes"]

    S3 --> S3A{Bet Arrives Within 30 min?}
    S3A -->|Yes| S3B["Auto Process:<br/>1. Process Bet then Debit<br/>2. Process Pending Win then Credit<br/>3. Remove from Queue"]
    S3A -->|No - Timeout| S3C["Escalate to Manual Review<br/>Create Ticket<br/>Priority: HIGH"]

    S3B --> SUCCESS1["Complete<br/>Both Bet and Win Processed"]
    S1B --> SUCCESS2["Complete<br/>After Retry"]
    S3C --> REVIEW[Manual Review Required]
    S1C --> REVIEW
    S2B --> REVIEW
    S2C --> REVIEW

    NORMAL --> END1[End - Normal]
    SUCCESS1 --> END2[End - Success]
    SUCCESS2 --> END2
    REVIEW --> END3[End - Manual Intervention]

    style NORMAL fill:#90EE90
    style S1 fill:#FFB6C1
    style S2 fill:#FF6B6B
    style S3 fill:#FFD93D
    style SUCCESS1 fill:#90EE90
    style SUCCESS2 fill:#90EE90
    style REVIEW fill:#DDA0DD
```

### 4.2 Strategy Switching Decision Tree

```mermaid
flowchart TD
    START[Win Request Received] --> CHECK_STRATEGY{Current Strategy?}

    CHECK_STRATEGY -->|Strategy 3 Active| CHECK_CONDITIONS{Check System Health}

    CHECK_CONDITIONS -->|Queue Size > 500| DEGRADE_S1["Degrade to Strategy 1<br/>Send Alert<br/>Return BET_NOT_FOUND"]
    CHECK_CONDITIONS -->|Redis Down| DEGRADE_S1
    CHECK_CONDITIONS -->|DB Latency > 5s| DEGRADE_S1
    CHECK_CONDITIONS -->|CPU > 90%| DEGRADE_S1

    CHECK_CONDITIONS -->|All Healthy| EXECUTE_S3["Execute Strategy 3<br/>Store in Pending Queue<br/>TTL: 30 min"]

    CHECK_STRATEGY -->|Strategy 1 Active| CHECK_RECOVERY{Check Recovery Conditions}

    CHECK_RECOVERY -->|Queue < 100<br/>for 10 min| UPGRADE_S3["Upgrade to Strategy 3<br/>Send Notification<br/>Restart Pending Queue"]
    CHECK_RECOVERY -->|Redis Healthy<br/>+ 5 min stable| UPGRADE_S3
    CHECK_RECOVERY -->|DB Latency < 1s<br/>for 10 min| UPGRADE_S3
    CHECK_RECOVERY -->|CPU < 70%<br/>for 10 min| UPGRADE_S3

    CHECK_RECOVERY -->|Conditions Not Met| EXECUTE_S1["Execute Strategy 1<br/>Return BET_NOT_FOUND<br/>Depend on GP Retry"]

    DEGRADE_S1 --> NOTIFY_OPS["Alert Notification<br/>Slack: #game-ops<br/>PagerDuty: On-Call"]

    EXECUTE_S3 --> CHECK_GP_RELIABILITY{"GP Retry Rate<br/>< 80%?"}
    CHECK_GP_RELIABILITY -->|Yes - GP Unreliable| EMERGENCY_S2["Emergency Switch to Strategy 2<br/>Allow Orphan Win<br/>Mark for Manual Review"]
    CHECK_GP_RELIABILITY -->|No - GP Reliable| END_S3[End - S3 Processing]

    UPGRADE_S3 --> NOTIFY_RECOVERY["Recovery Notification<br/>Slack: #game-ops"]

    NOTIFY_OPS --> END_S1[End - S1 Processing]
    EXECUTE_S1 --> END_S1
    EMERGENCY_S2 --> END_S2[End - S2 Processing]
    NOTIFY_RECOVERY --> END_S3

    style DEGRADE_S1 fill:#FFB6C1
    style UPGRADE_S3 fill:#90EE90
    style EMERGENCY_S2 fill:#FF6B6B
    style EXECUTE_S3 fill:#FFD93D
    style EXECUTE_S1 fill:#E0E0E0
    style NOTIFY_OPS fill:#FFA07A
    style NOTIFY_RECOVERY fill:#98FB98
```

---

## 5. Comprehensive Extreme Scenario Decision Matrix

### 5.1 Unified Exception Handling Flowchart

```mermaid
flowchart TD
    START[Receive GP Request] --> TYPE{Request Type?}

    TYPE -->|Bet Request| SCENARIO_CHECK_BET{"Scenario Detection"}

    SCENARIO_CHECK_BET -->|Scenario G: Free Spin| FREE_SPIN["Free Spin Detected<br/>amount = 0"]
    FREE_SPIN --> VALIDATE_FREE[Validate Free Spin Quota]
    VALIDATE_FREE -->|Quota Valid| ACCEPT_FREE["Accept amount=0 Transaction<br/>Record Free Spin Flag"]
    VALIDATE_FREE -->|Quota Invalid| REJECT_FREE[Reject: Free Spin Quota Exceeded]

    SCENARIO_CHECK_BET -->|Scenario H: Bonus Wallet| BONUS_WALLET[Bonus Wallet Check]
    BONUS_WALLET --> GAME_TYPE{Game Supports Bonus?}
    GAME_TYPE -->|Supported| DUAL_WALLET["Dual Wallet Deduction<br/>1. Deduct Cash First<br/>2. Then Deduct Bonus"]
    GAME_TYPE -->|Not Supported| CASH_ONLY["Cash Wallet Only<br/>Ignore Bonus Balance"]

    SCENARIO_CHECK_BET -->|Scenario A: Normal Bet| NORMAL_BET[Normal Bet Flow]
    NORMAL_BET --> CONCURRENCY_CHECK{Scenario B: Concurrency Detection}

    CONCURRENCY_CHECK -->|Concurrent Detected| RACE_CONDITION[Race Condition Detected]
    RACE_CONDITION --> ACQUIRE_LOCK["Acquire Distributed Lock<br/>Redis: SETNX lock:player:{id}"]
    ACQUIRE_LOCK -->|Success| BALANCE_CHECK
    ACQUIRE_LOCK -->|Fail - Retry 3x| RETRY_LOCK["Exponential Backoff Retry<br/>50ms, 100ms, 200ms"]
    RETRY_LOCK -->|Still Fail| REJECT_CONCURRENCY[Reject: System Busy - Retry Later]

    CONCURRENCY_CHECK -->|No Concurrency| BALANCE_CHECK{Scenario A: Balance Check}

    BALANCE_CHECK -->|Insufficient| INSUFFICIENT_FUNDS[Insufficient Funds Detected]
    INSUFFICIENT_FUNDS --> CHECK_BONUS_ELIGIBLE{Bonus Available?}
    CHECK_BONUS_ELIGIBLE -->|Available| DUAL_WALLET
    CHECK_BONUS_ELIGIBLE -->|Not Available| REJECT_INSUFFICIENT["Reject: Insufficient Balance<br/>errorCode: INSUFFICIENT_FUNDS"]

    BALANCE_CHECK -->|Sufficient| OPTIMISTIC_LOCK["Optimistic Lock Deduction<br/>UPDATE balance WHERE version={v}"]
    OPTIMISTIC_LOCK -->|Success| BET_SUCCESS[Return Success + txId]
    OPTIMISTIC_LOCK -->|Version Conflict| RETRY_LOCK

    DUAL_WALLET --> OPTIMISTIC_LOCK
    CASH_ONLY --> BALANCE_CHECK
    ACCEPT_FREE --> BET_SUCCESS

    TYPE -->|Win Request| SCENARIO_CHECK_WIN{"Scenario Detection"}

    SCENARIO_CHECK_WIN -->|Scenario I: Jackpot| JACKPOT["Jackpot Detected<br/>amount > $10,000"]
    JACKPOT --> JACKPOT_MODE{GP Protocol Mode?}
    JACKPOT_MODE -->|Manual Approval| JACKPOT_NOTIFY["Send NotifyWin<br/>Pending Human Review"]
    JACKPOT_NOTIFY --> JACKPOT_PENDING["Status: PENDING_APPROVAL<br/>Do Not Credit Yet"]
    JACKPOT_MODE -->|Auto Credit| JACKPOT_AUTO["Auto Credit + Freeze Account<br/>Trigger Risk Review"]

    SCENARIO_CHECK_WIN -->|Scenario D: Out-of-Order| OUT_OF_ORDER["Out-of-Order Detected<br/>Win Before Bet"]
    OUT_OF_ORDER --> PENDING_QUEUE["Store in Pending Win Queue<br/>Wait for Bet Request<br/>TTL: 2 Hours"]

    SCENARIO_CHECK_WIN -->|Scenario C: Normal Win| NORMAL_WIN[Normal Win Flow]
    NORMAL_WIN --> IDEMPOTENCY_CHECK{"Idempotency Check<br/>txId Already Processed?"}
    IDEMPOTENCY_CHECK -->|Already Processed| IDEMPOTENT_RESPONSE["Return Cached Result<br/>Redis: idempotency:{txId}"]
    IDEMPOTENCY_CHECK -->|Not Processed| CREDIT_BALANCE["Credit to Wallet<br/>UPDATE balance += amount"]
    CREDIT_BALANCE --> WIN_SUCCESS["Return Success<br/>Cache Result TTL=1h"]

    TYPE -->|Rollback/Refund Request| SCENARIO_E[Scenario E: Rollback Compensation]
    SCENARIO_E --> FIND_ORIGINAL_TX{Find Original TX by txId}
    FIND_ORIGINAL_TX -->|Found| ROLLBACK_EXECUTE["Execute Reverse Operation<br/>Debit to Credit<br/>Credit to Debit"]
    ROLLBACK_EXECUTE --> MARK_ROLLED_BACK["Mark Original TX ROLLED_BACK<br/>Record Audit Log"]
    MARK_ROLLED_BACK --> ROLLBACK_SUCCESS[Return Success]

    FIND_ORIGINAL_TX -->|Not Found| ROLLBACK_NOT_FOUND["Original TX Not Found<br/>Bet Request Never Arrived"]
    ROLLBACK_NOT_FOUND --> ROLLBACK_SUCCESS_ANYWAY["Return Success<br/>Goal Achieved: No Deduction Made"]

    TYPE -->|Adjust/Resettlement Request| SCENARIO_F[Scenario F: Resettlement]
    SCENARIO_F --> ADJUST_TYPE{Adjustment Type?}
    ADJUST_TYPE -->|Overpaid - Clawback| NEGATIVE_ADJUST["Execute Negative Credit<br/>balance -= adjustment_amount"]
    NEGATIVE_ADJUST --> CHECK_NEGATIVE{Balance After Deduction < 0?}
    CHECK_NEGATIVE -->|Yes| ALLOW_NEGATIVE["Allow Negative Balance<br/>Trigger Risk Alert<br/>Mark: MANUAL_RECOVERY"]
    CHECK_NEGATIVE -->|No| ADJUST_SUCCESS[Return Success]

    ADJUST_TYPE -->|Underpaid - Supplement| POSITIVE_ADJUST["Execute Positive Credit<br/>balance += adjustment_amount"]
    POSITIVE_ADJUST --> ADJUST_SUCCESS

    ALLOW_NEGATIVE --> MANUAL_REVIEW["Manual Intervention<br/>1. Freeze Withdrawals<br/>2. Contact Player<br/>3. Installment Recovery"]

    BET_SUCCESS --> TIMEOUT_MONITOR{Scenario C: Timeout Monitor}
    TIMEOUT_MONITOR -->|GP No Response 2h| TIMEOUT_DETECTED[Timeout Detected]
    TIMEOUT_DETECTED --> QUERY_GP["Active Query GP Status<br/>GET /query?roundId={id}"]
    QUERY_GP -->|GP Settled| COMPENSATE_WIN["Compensatory Credit<br/>Prevent Funds Stuck"]
    QUERY_GP -->|GP Still Processing| EXTEND_TIMEOUT["Extend Timeout<br/>Continue Waiting"]
    QUERY_GP -->|GP No Record| ROUND_CANCELLED["Mark Round CANCELLED<br/>Refund to Player"]

    style FREE_SPIN fill:#E8F5E9
    style BONUS_WALLET fill:#E3F2FD
    style RACE_CONDITION fill:#FFF3E0
    style INSUFFICIENT_FUNDS fill:#FFEBEE
    style JACKPOT fill:#F3E5F5
    style OUT_OF_ORDER fill:#E0F2F1
    style SCENARIO_E fill:#FFF9C4
    style SCENARIO_F fill:#FCE4EC
    style TIMEOUT_DETECTED fill:#ECEFF1

    style REJECT_FREE fill:#FFCDD2
    style REJECT_CONCURRENCY fill:#FFCDD2
    style REJECT_INSUFFICIENT fill:#FFCDD2
    style ALLOW_NEGATIVE fill:#FF9800

    style BET_SUCCESS fill:#C8E6C9
    style WIN_SUCCESS fill:#C8E6C9
    style ROLLBACK_SUCCESS fill:#C8E6C9
    style ADJUST_SUCCESS fill:#C8E6C9
```

---

## 6. Negative Balance State Machine

```mermaid
stateDiagram-v2
    [*] --> Normal
    Normal --> Deduction : GP Request (Debit/Rollback)
    Deduction --> Normal : Balance >= Amount
    Deduction --> NegativeLocked : Balance < Amount (Force Deduct)

    state NegativeLocked {
        [*] --> UpdateBalance : Set Balance < 0
        UpdateBalance --> LockAccount : Status = SUSPENDED_RISK
        LockAccount --> CreateTicket : Notify Risk Team
    }

    NegativeLocked --> Normal : Manual Deposit / Debt Cleared (Admin Unlock)
```

---

## 7. Integrated Transaction Flow

```mermaid
flowchart TD

    Req["GP Request (Debit/Credit)"] --> CheckID{"Has TransactionId?"}

    CheckID -- No --> Err400["Error 400: Missing ID"]

    CheckID -- Yes --> CheckCache{"Idempotency Check<br/>(Redis Key Exists?)"}

    CheckCache -- Yes --> ReturnCache["Return Cached Response"]

    CheckCache -- No --> LoadConfig["Load Game Config<br/>(Get Wallet Priority)"]

    LoadConfig --> TypeCheck{"Request Type?"}

    TypeCheck -- Debit (Bet) --> CalcDeduction["Calculate Deduction Order<br/>(e.g. Bonus then Cash)"]

    CalcDeduction --> CheckBal{"Balance Sufficient?"}

    CheckBal -- No --> ErrFund["Error: Insufficient Funds"]

    CheckBal -- Yes --> ExecDebit["Execute Debit on Wallets"]

    TypeCheck -- Credit (Win/Rollback) --> ExecCredit["Execute Credit/Adjustment"]

    ExecCredit --> CheckNeg{"Result Balance < 0?"}

    CheckNeg -- Yes --> LockAcc["Update Balance and<br/>SET STATUS = LOCKED"]

    LockAcc --> AlertRisk["Trigger Risk Alert"]

    CheckNeg -- No --> NormalUpdate["Update Balance"]

    ExecDebit --> SaveTx["Save Transaction Log"]

    ErrFund --> SaveTx

    LockAcc --> SaveTx

    NormalUpdate --> SaveTx

    SaveTx --> CacheRes["Cache Response to Redis"]

    CacheRes --> Resp["Return API Response"]
```

---

## 8. Wallet Deduction Order Configuration

### 8.1 Game Configuration Schema

```json
{
  "game_code": "slot_001",
  "wallet_priority": ["BONUS_WALLET", "CASH_WALLET"],
  "use_system_default": false
}
```

### 8.2 Execution Flow

1. Receive Bet request
2. Check `game_config.wallet_priority` existence
3. If exists and `use_system_default = false`: use game configuration
4. If not exists: use system default (Bonus -> Cash -> Credit)
5. Deduct balance in priority order
6. If first-priority wallet has insufficient balance, allow mixed deduction (record split details)

---

## 9. Monitoring & Alerting Configuration

### 9.1 Prometheus AlertManager Rules

```yaml
# Prometheus AlertManager Rules
groups:
  - name: seamless_wallet_alerts
    rules:
      # Scenario A: Abnormal insufficient funds rate
      - alert: HighInsufficientFundsRate
        expr: (rate(bet_rejected_insufficient_funds_total[5m]) / rate(bet_requests_total[5m])) > 0.3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Insufficient funds rejection rate > 30%, possible player fund depletion or deposit anomaly"

      # Scenario B: High lock contention
      - alert: HighLockContentionRate
        expr: rate(redis_lock_timeout_total[5m]) > 10
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Redis lock timeout frequency too high, system concurrency pressure excessive"

      # Scenario C: Abnormal GP timeout rate
      - alert: HighGPTimeoutRate
        expr: (rate(gp_timeout_total[1h]) / rate(bet_requests_total[1h])) > 0.01
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "GP timeout rate > 1%, check GP service health"

      # Scenario D: Out-of-order backlog
      - alert: PendingWinQueueBacklog
        expr: pending_win_queue_size > 100
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Pending Win Queue backlog > 100, possible Bet request delay or loss"

      # Scenario F: Negative balance accumulation
      - alert: NegativeBalanceAccumulation
        expr: sum(player_negative_balance) < -10000
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Negative balance accumulation exceeds $10,000, trigger financial risk control review"

      # Scenario I: Unusual jackpot frequency
      - alert: UnusualJackpotFrequency
        expr: rate(jackpot_win_total[1h]) > 5
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Jackpot trigger > 5 times in 1 hour, suspected game logic anomaly or fraud"
```

### 9.2 Strategy Switching Alerts

```yaml
# Prometheus AlertManager Rules
alerts:
  - name: out_of_order_strategy_degraded
    expr: out_of_order_strategy != 3
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "Out-of-Order strategy degraded to Strategy {{ $value }}"
      description: "Current strategy: {{ if eq $value 1 }}Immediate Reject{{ else if eq $value 2 }}Orphan Win (Emergency){{ end }}"

  - name: out_of_order_emergency_mode
    expr: out_of_order_strategy == 2
    for: 1m
    labels:
      severity: critical
    annotations:
      summary: "Out-of-Order entered emergency mode (Strategy 2 - Orphan Win)"
      description: "GP retry rate too low, allowing orphan wins, immediate manual intervention required"
```

---

## 10. SmartAdmin Architecture Mapping

### 10.1 Five-Layer Module Design

| Layer | Class Pattern | Responsibility | Annotation Restrictions |
|-------|--------------|---------------|----------------------|
| **Controller** | `SeamlessWalletController` | Receive GP HTTP requests, parameter validation, return ResponseDTO | No @Transactional |
| **Service** | `SeamlessWalletService` | Business coordination, idempotency checks, call Manager | No @Transactional |
| **Manager** | `SeamlessWalletTransactionManager` | Transaction management, distributed locks, wallet debit/credit | @Transactional allowed ONLY here |
| **Dao** | `GameTransactionRecordDao` | Database CRUD, MyBatis Mapper | No business logic |
| **Entity** | `GameTransactionRecordEntity` | Data model, maps to table structure | No business logic |

### 10.2 Dependency Rules (Enforced by ArchitectureTest)

```text
Controller -> Service (ALLOWED)
Service -> Dao      (ALLOWED - for idempotency record queries)
Service -> Manager  (ALLOWED - when @Transactional needed)
Manager -> Dao      (ALLOWED)

Controller -> Dao   (FORBIDDEN - violates layering)
Controller -> Manager (FORBIDDEN - violates layering)
```

### 10.3 Entity Layer

**GameTransactionRecordEntity.java** - Maps to the game transaction record table, tracking all Debit/Credit/Rollback/Adjust operations with txId, roundId, type, amount, balance_after, and status fields.

**RoundStateEntity.java** - Maps to the round state table for Round-Based GP integrations, tracking roundId, status (OPEN/CLOSED/CANCELLED/ADJUSTED/TIMEOUT/PENDING_REVIEW), total bet amount, total win amount, and timestamps.

### 10.4 DAO Layer

**GameTransactionRecordDao.java** - MyBatis Mapper for game transaction CRUD operations, including queries by txId for idempotency checks and batch queries for reconciliation.

**RoundStateDao.java** - MyBatis Mapper for round state management, including queries for orphaned rounds (status=OPEN and created_at older than threshold).

### 10.5 Manager Layer

**SeamlessWalletTransactionManager.java** - Handles all transactional operations:
- `processBet()`: Distributed lock acquisition, balance check, debit execution within @Transactional
- `processWin()`: Credit execution, round closure within @Transactional
- `processRollback()`: Reverse operation execution within @Transactional
- `processAdjustment()`: Resettlement handling with negative balance support within @Transactional

### 10.6 Service Layer

**SeamlessWalletService.java** - Orchestrates business logic:
- Idempotency check via Redis cache lookup
- Delegates to Manager for transactional operations
- Handles out-of-order pending queue logic
- Coordinates with Risk Engine for anomaly detection

### 10.7 Controller Layer

**SeamlessWalletController.java** - HTTP interface:
- Receives GP requests (Debit, Credit, Rollback, Adjust, GetBalance)
- Parameter validation
- Returns ResponseDTO with standardized error codes

### 10.8 Foundation Module Dependencies

| Foundation Module | Purpose | Reference Location |
|------------------|---------|-------------------|
| **foundation.redis-lock** | Distributed locks to prevent concurrent deduction (Scenario B) | SeamlessWalletTransactionManager.processBet/processWin/processRollback() |
| **foundation.cache** | Caffeine + Redis cache for idempotency | SeamlessWalletService.checkIdempotency() |
| **foundation.audit-log** | Audit log recording | Auto-recorded after all transactions |
| **foundation.retry** | Failure retry strategy | GP API call failure retries |

### 10.9 Idempotency TTL Standardization

From v2.0.0, unified idempotency TTL is **3600 seconds (1 hour)**, consistent with the Turnover Calculation module for operational and monitoring alignment.

---

## 11. Architecture Impact Analysis

1. **Game Config Service**: Schema must be extended to support `wallet_priority` configuration
2. **Wallet Service**:
   - `debit()` method must support accepting a `priority_list` parameter
   - Must handle "Split Transaction" (e.g., 20 from Bonus, 80 from Cash)
3. **Risk Engine**: Must listen to `BALANCE_CHANGE` events and trigger account lock when `NewBalance < 0`

---

## 12. Key Implementation Notes

1. **Scenario priority**: Decision tree processes P0 -> P1 -> P2 order, ensuring high-priority scenarios are handled first
2. **Idempotency protection**: All Credit/Debit operations must pass through `idempotency:{txId}` check
3. **Distributed locks**: Concurrency scenarios use Redis SETNX + TTL=5s to prevent permanent lock hold
4. **Degradation strategy**: When Redis/GP unavailable, prioritize fund safety (reject rather than incorrectly deduct)
5. **Audit logs**: All exception scenarios (negative balance, jackpot, rollback) must record complete audit logs
6. **Manual intervention**: Clearly predefined manual intervention thresholds to prevent automated decision runaway

---

## 13. Related Documents

### Upstream Architecture
- **Unified Wallet Model** - Overall wallet architecture, bettable balance formula
- **Transaction Processing Flow** - Event-driven architecture, Outbox Pattern

### Deep Technical Analysis
Detailed seamless wallet topic analyses (13 topics) providing fine-grained implementation guidance:

- **Seamless Wallet Topic Index** - Complete navigation and learning path
  - Token Verification - API authentication decision tree (Bet vs Result API verification strategy)
  - Idempotency Design - Three-layer protection (Redis -> DB -> Distributed Lock), anti-duplicate deduction
  - Sports Betting Logic - Valid Bet calculation, HALF WIN/LOSS handling
  - Turnover Concurrency Accumulation - Lua script atomicity, TOCTOU attack protection
  - Error Recovery - Exception handling, compensating transactions, rollback strategies

### Risk Integration
- **Risk Framework** - Transaction risk checks, hedging detection

---

## 14. Change Log

### v2.0.0 (2026-01-29)

**Major Changes**:
1. Added SmartAdmin Architecture Mapping (Section 10)
   - Complete five-layer architecture (Entity/Dao/Manager/Service/Controller)
   - Seamless wallet core logic implementation (Bet/Win/Rollback)
   - Distributed lock (RedisLock) and idempotency (Redis Cache) integration
   - Round state management (Round-Based GP support)
   - Foundation module dependency documentation (redis-lock, cache, audit-log, retry)
   - ArchitectureTest validation rules

2. Unified idempotency TTL to 3600 seconds (1 hour)
   - Consistent with Turnover Calculation module
   - Removed inconsistent descriptions (1-2h)

### v1.0.0 (2026-01-28)

**Initial Version**:
- Seamless wallet integration scenario analysis (Sections 1-2)
- 9 extreme scenario comprehensive decision matrix (Section 5)
- Idempotency, concurrency control, out-of-order handling (Sections 3-4)
- Round-Based state machine (Section 2)
- Negative balance handling, wallet deduction order (Section 6-8)
