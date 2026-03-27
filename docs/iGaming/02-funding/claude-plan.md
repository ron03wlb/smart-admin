# 02-funding Implementation Plan — iGaming Funding Domain

> **Version**: 1.1 (post-Opus review integration)
> **Date**: 2026-03-27
> **Domain**: Wallet System + Payment System
> **Tech Stack**: Java 17+ / Spring Boot 3.x / SmartAdmin V3.0 / PostgreSQL / Redis Cluster / Kafka
> **Scale Target**: 5,000–20,000 concurrent Seamless Wallet requests/sec

---

## 1. What We're Building

An iGaming platform's **Funding Domain** — the financial engine that handles all money movement. This domain has two tightly coupled sub-systems:

1. **Wallet System**: Manages three wallet types (CASH, BONUS, CREDIT) per player, processes game bets/wins through a Seamless Wallet API consumed by 10+ Game Providers (GPs), handles complex transaction scenarios (concurrent bets, out-of-order requests, rollbacks, resettlement), and performs three-layer reconciliation against GP records.

2. **Payment System**: Routes deposits and withdrawals through 5+ Payment Service Providers (PSPs) using a smart weighted routing algorithm, processes fiat and cryptocurrency (BTC/ETH/USDT) payments, manages chargeback lifecycles, and enforces jurisdiction-specific payment restrictions.

The domain sits in the middle of the platform dependency graph: it depends on the Governance domain (tenant isolation, RBAC, agent credit) and provides APIs consumed by the Gaming domain (Seamless Wallet), Player domain (balance queries), Risk domain (transaction events via Kafka), and Data domain (transaction logs).

### 1.1 Why This Architecture

**Double-layer concurrency control** (Redisson + DB atomic update) is chosen because the platform targets 5K–20K concurrent wallet requests/sec with strict correctness requirements (zero over-deduction). A single DB lock would bottleneck under this load; pure Redis locking without DB guard leaves a safety gap if Redis fails.

**Kafka for event streaming** enables the Risk domain to monitor transactions asynchronously without adding latency to the critical payment path, while the pre-transaction risk check remains a synchronous API call.

**Deposit-time currency conversion** simplifies the ledger to a single settlement currency per wallet, avoiding the complexity of multi-currency accounting while still supporting crypto at launch.

---

## 2. Architecture Overview

### 2.1 Module Structure

```
02-funding/
├── wallet-core/              # Wallet CRUD, balance engine, deduction priority
│   ├── model/                # Wallet, Balance, DeductionRule entities
│   ├── service/              # WalletService, BalanceCalculator, DeductionEngine
│   └── repository/           # WalletRepository, BalanceSnapshotRepository
│
├── wallet-transaction/       # Transaction processing, round lifecycle
│   ├── model/                # Transaction, Round, TransactionScenario entities
│   ├── service/              # TransactionProcessor, RoundLifecycleManager
│   ├── scenario/             # Nine scenario handlers (Strategy pattern)
│   └── statemachine/         # Round state machine configuration
│
├── seamless-wallet-api/      # GP-facing API layer
│   ├── controller/           # Five endpoint controllers
│   ├── security/             # HMAC token validator
│   ├── idempotency/          # IdempotencyStore (Redis-backed)
│   └── adapter/              # GP-specific request/response adapters
│
├── wallet-reconciliation/    # Three-layer reconciliation
│   ├── service/              # RealtimeReconciler, HourlyReconciler, DailyReconciler
│   ├── detector/             # OrphanRoundDetector, DiscrepancyResolver
│   └── scheduler/            # Scheduled recon jobs
│
├── payment-gateway/          # PSP orchestration layer
│   ├── router/               # SmartRouter, RoutingScoreCalculator
│   ├── circuitbreaker/       # PSP health monitor, circuit breaker
│   ├── adapter/              # Per-PSP adapter implementations
│   └── model/                # PaymentOrder, PSPResponse, RoutingDecision
│
├── payment-deposit/          # Deposit flow
│   ├── service/              # DepositService, CashierFrictionEvaluator
│   ├── callback/             # PSP callback handlers
│   └── limit/                # DepositLimitChecker
│
├── payment-withdrawal/       # Withdrawal flow
│   ├── service/              # WithdrawalService, WageringVerifier
│   ├── approval/             # Multi-tier approval workflow
│   └── scheduler/            # Holiday SLA adjuster
│
├── payment-dispute/          # Chargeback management
│   ├── service/              # ChargebackService, EvidenceCollector
│   ├── lifecycle/            # Chargeback state machine
│   └── penalty/              # PlayerPenaltyEngine
│
├── currency-engine/          # Multi-currency + crypto
│   ├── service/              # CurrencyConverter, FXRateManager
│   ├── crypto/               # BlockchainMonitor, WalletAddressGenerator
│   └── precision/            # MoneyValueObject, PrecisionGuard
│
└── shared/                   # Cross-module shared code
    ├── event/                # Kafka event publishers
    ├── lock/                 # Distributed lock abstraction
    ├── config/               # DB-configurable parameter reader
    └── stub/                 # Governance domain test stubs
```

### 2.2 Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Concurrency | Redisson distributed lock + `UPDATE ... WHERE balance >= amount` | Double-layer: serialize at app level, safety net at DB level |
| Idempotency | Redis Cluster, TTL 24h, key = transactionId scoped by endpoint+GP | Industry standard for GP late-retry; fingerprint validation |
| Events | Kafka topics per event type | Async monitoring for risk/data; decoupled from critical path |
| Currency | `Money(BigDecimal, Currency)` value object | Never naked numerics; JSR 354 Moneta or custom |
| State machines | Spring Statemachine for Round lifecycle + Chargeback lifecycle | Declarative transitions; auditable; guards enforce invariants |
| PSP routing | Single-pass weighted scoring with jurisdiction as factor | Simpler pipeline; ineligible PSPs score 0 |
| GP adapters | Strategy pattern per GP; shared Seamless Wallet contract | 10+ GPs with varying quirks; adapters normalize to internal model |
| Config | Three-layer DB config (Jurisdiction → Brand → Global) | All thresholds, rates, limits are DB-configurable, never hardcoded |

---

## 3. Wallet Core (wallet-core)

### 3.1 Data Model

**Wallet entity** fields:
- `id` (UUID), `playerId` (UUID), `tenantId` (UUID), `walletType` (enum: CASH/BONUS/CREDIT), `currency` (ISO 4217), `balance` (DECIMAL 19,4), `lockedAmount` (DECIMAL 19,4), `version` (long — optimistic lock), `status` (enum: ACTIVE/SUSPENDED/CLOSED), `createdAt`, `updatedAt`

**DB constraint**: `CHECK (balance >= 0 OR walletType = 'CREDIT')` as last-resort negative balance prevention for CASH/BONUS. (Note: The constraint checks `balance` alone, not `balance + lockedAmount`, because the invariant is that the actual balance must never go negative for non-CREDIT wallets.)

**Indexing**: Composite index on `(playerId, walletType, tenantId)` — unique; partitioning by `playerId` hash for horizontal scaling.

### 3.2 Playable Balance Calculation

The `BalanceCalculator` service computes playable balance on every request:

```java
Money calculatePlayableBalance(UUID playerId, GameContext ctx);
/**
 * Standard mode: CASH.balance + BONUS.balance - totalLocked - totalInProgress
 * Credit mode:   CREDIT.limit - CREDIT.usedAmount
 *
 * GameContext determines which wallets participate (some GPs reject BONUS).
 * Returns Money value object with 4-decimal precision.
 */
```

This is a **hot path** called on every GetBalance request (<50ms SLA). Implementation should:
- Use **versioned cache key**: `balance:{playerId}:v{walletVersion}` — eliminates write-invalidation race condition (classic cache-aside problem)
- On balance mutation, the version increments automatically; old cache key expires naturally via TTL (2 seconds)
- Use read replica for the DB query as fallback when cache misses

### 3.3 Deduction Priority Engine

The `DeductionEngine` determines which wallet(s) to debit for a given bet:

```java
List<DeductionStep> resolveDeductionOrder(UUID playerId, Money amount, DeductionContext ctx);
/**
 * Evaluates 4-layer priority override:
 *   Layer 1: Player-level rule (e.g., VIP prefers CASH)
 *   Layer 2: GP contract rule (e.g., GP rejects BONUS)
 *   Layer 3: Game-level rule (e.g., Live Casino = CASH only)
 *   Layer 4: System default (BONUS → CASH → CREDIT)
 *
 * Returns ordered list of DeductionStep(walletType, amount).
 * Each step records which layer triggered it for audit trail.
 */
```

**DeductionStep** fields: `walletType`, `amount`, `appliedLayer` (1-4), `ruleId` (for audit).

Rules are stored in the DB configurable parameter system with three-layer override (Jurisdiction → Brand → Global).

---

## 4. Wallet Transaction Processing (wallet-transaction)

### 4.1 Transaction Processor — The Critical Path

This is the most complex and performance-critical component. Every GP bet/win/rollback flows through here.

**Core flow for Debit (Bet)**:
1. Acquire Redisson distributed lock on `player:{playerId}` (TTL: 5s, wait: 3s)
2. Inside lock: resolve deduction order via DeductionEngine → returns List<DeductionStep>
3. **Execute ALL deduction steps in a single DB transaction**:
   - For each DeductionStep: `UPDATE wallet SET balance = balance - :amount WHERE id = :id AND balance >= :amount`
   - No version check on the critical UPDATE (the Redisson lock serializes; the `balance >= amount` guard is the DB safety net)
   - Version column reserved for optimistic reads and audit, not for the write path
   - If ANY step returns zero rows updated → rollback entire transaction → insufficient balance
4. Write transaction record + deduction audit trail (inside same DB transaction)
5. Write event to **transactional outbox table** (inside same DB transaction) — guarantees at-least-once delivery to Kafka
6. Release lock
7. Outbox relay (separate process): polls outbox → publishes to Kafka → marks as published

**Why both Redisson AND DB conditional update**: The Redis lock prevents most contention at the application layer (fast serialization). The DB `WHERE balance >= amount` guard is the safety net — if Redis fails or a lock expires prematurely, the DB still prevents over-deduction via atomic conditional update. The version column is NOT used in the write path to avoid redundant conflict with the Redis lock; it is reserved for optimistic reads and auditing.

### 4.2 Nine Scenario Handlers (Strategy Pattern)

Each of the nine transaction scenarios has a dedicated handler implementing a common interface:

```java
interface TransactionScenarioHandler {
    boolean canHandle(TransactionContext ctx);
    TransactionResult execute(TransactionContext ctx);
}
```

| Scenario | Handler | Key Logic |
|----------|---------|-----------|
| A: Insufficient balance | `InsufficientBalanceHandler` | Check multi-wallet availability; skip BONUS if GP rejects it; return structured error with available balance breakdown |
| B: Concurrent bets | `ConcurrentBetHandler` | Serialize via Redisson lock per player; retry with exponential backoff on lock contention |
| C: Timeout/retry | `IdempotentRetryHandler` | Check idempotency store first; cached response → return immediately; not cached → process normally |
| D: Out-of-order | `OutOfOrderHandler` | Win arrives before Bet → **persist to DB `parked_transactions` table** + Redis fast lookup (`parking:{roundId}`, TTL 30min); background job retries every 5s; if Bet never arrives within 30min → expire with error log and GP notification; CS can manually force-process parked transactions |
| E: Rollback | `RollbackHandler` | Original bet not found → return Success (idempotent); found → reverse deduction, restore balance, update round state |
| F: Resettlement | `ResettlementHandler` | Calculate difference; allow controlled negative balance; trigger negative balance protection flow if needed |
| G: Free spin | `FreeSpinHandler` | Validate free spin quota; Bet=0 is legal; Win>0 → credit to CASH wallet |
| H: Bonus bet | `BonusBetHandler` | Strictly follow §2.4 deduction priority (BONUS → CASH); override only if Layer 1-3 rule applies |
| I: Jackpot | `JackpotHandler` | Win>$10K → check GP contract for auto-credit vs manual-approval; if manual → create approval task; if auto → credit and freeze for risk review |

### 4.3 Round Lifecycle State Machine

Use Spring Statemachine to model the round lifecycle declaratively:

**States**: OPEN, CLOSED, CANCELLED, TIMEOUT, PENDING_REVIEW, ADJUSTED

**Transitions**: As defined in the spec state machine diagram. Each transition has:
- **Guard**: Validates preconditions (e.g., can only transition OPEN→CLOSED if all bets have corresponding settlements)
- **Action**: Executes business logic (e.g., update balances, publish events)
- **Listener**: Logs state transition for audit

**Orphan round detector**: A scheduled job (every 15 minutes) scans for rounds in OPEN state older than 2 hours. For each orphan:
1. Query GP API for round status
2. GP responds with result → auto-close round
3. GP API unavailable → transition to PENDING_REVIEW
4. Rounds with amount > $1,000 → create high-priority CS ticket with 24h SLA

---

## 5. Seamless Wallet API (seamless-wallet-api)

### 5.1 Endpoint Design

All five endpoints share the same authentication, idempotency, and **rate limiting** middleware. The API is GP-facing (external).

**Per-GP Rate Limiting**: Each GP has a configurable request rate limit (e.g., 2,000 req/sec default). Implemented via Redis sliding window counter per GP. Exceeding the limit returns HTTP 429 with `Retry-After` header. This prevents a misbehaving GP from consuming the entire platform's 5K-20K capacity.

**Request flow**: HTTP Request → Token Validator → Idempotency Check → GP Adapter (normalize) → Transaction Processor → Response

```
POST /api/v1/seamless/balance      → GetBalance
POST /api/v1/seamless/debit        → Debit (Bet)
POST /api/v1/seamless/credit       → Credit (Win)
POST /api/v1/seamless/rollback     → Rollback
POST /api/v1/seamless/adjust       → Adjust (Resettlement)
```

All mutation endpoints return the updated balance alongside the transaction result.

### 5.2 Token Validation

```java
interface TokenValidator {
    TokenValidationResult validate(String token, String playerId, TokenStrictness strictness);
}
```

- **Algorithm**: HMAC-SHA256
- **TTL**: 5 minutes
- **Usage**: One-time (store used tokens in Redis set with 5min TTL for anti-replay)
- **GetBalance**: `LENIENT` — allows slightly expired tokens (adds 30s grace)
- **All mutations**: `STRICT` — reject expired tokens immediately

**Token expired on Win/Credit**: Return specific error code. GP should use the Resettlement endpoint or raise a CS ticket for credit reconciliation. The wagering progress still counts per BS-01 decision.

### 5.3 Idempotency Store

```java
interface IdempotencyStore {
    Optional<CachedResponse> get(IdempotencyKey key);
    void put(IdempotencyKey key, CachedResponse response, Duration ttl);
}
```

**IdempotencyKey** composition: `{tenantId}:{endpoint}:{gpId}:{transactionId}` — scoped to prevent cross-GP and cross-tenant collisions in multi-tenant environment.

**Rules**:
- **TTL**: 24 hours (stakeholder decision, extended from original 1h)
- **Only cache successful responses** and deterministic errors (invalid player, insufficient balance)
- **Never cache**: transient errors (timeout, 5xx, lock contention)
- **Fingerprint validation**: On cache hit, compare request body hash with stored hash. Mismatch → log warning + return error (prevents accidentally reusing a transactionId with different parameters)
- **Storage**: Redis Cluster for high availability and horizontal scaling

### 5.4 GP Adapter Layer

Each GP has slightly different request/response formats. The adapter layer normalizes them:

```java
interface GPAdapter {
    String getGPId();
    InternalDebitRequest normalizeDebit(RawGPRequest request);
    RawGPResponse formatDebitResponse(InternalDebitResponse response);
    // ... same for Credit, Rollback, Adjust, GetBalance
}
```

New GPs are integrated by implementing a new adapter — no changes to core transaction logic. Target: GP technical integration < 5 business days.

---

## 6. Wallet Reconciliation (wallet-reconciliation)

### 6.1 Three-Layer Model

**Layer 1 — Real-time**: Every Seamless Wallet API call updates a `realtime_ledger` table. This is the source of truth for current balances.

**Layer 2 — Hourly reconciliation**: A scheduled job (every hour) compares platform transaction records against GP-provided transaction reports (GP pushes or platform pulls via GP Reporting API). Discrepancy detection:
- Amount mismatch: Flag if difference > $1 (auto-correct if < $1)
- Time mismatch: Auto-match if within 5 minutes
- Missing on platform: **Create transaction based on GP record** (GP is authoritative)
- Missing on GP: **Reverse the platform transaction** (GP is authoritative — stakeholder decision)
- Alert thresholds: >$100 discrepancy → manual alert; >10 missing transactions/hour → check GP API health

**Reversal Circuit Breaker** (safeguard against GP data quality issues): If a single GP's hourly reconciliation triggers more than N reversals (configurable, default 10), auto-reversal is paused for that GP and escalated to manual review. This protects against compromised or buggy GP reporting APIs that could cause mass legitimate transaction reversals. Every auto-reversal preserves a snapshot of the GP report that triggered it for audit and potential rollback.

**Layer 3 — Daily analytics**: 02:00 UTC batch job aggregates into the data warehouse for daily player summaries and reporting.

### 6.2 Orphan Round Detection

Runs every 15 minutes. Query: `SELECT * FROM rounds WHERE status = 'OPEN' AND updated_at < NOW() - INTERVAL '2 hours'`

For each orphan:
1. Call GP's round status API
2. If GP returns settlement result → process settlement and close round
3. If GP API fails → mark as PENDING_REVIEW
4. If round amount > $1,000 → create priority ticket (24h SLA)

---

## 7. Payment Gateway (payment-gateway)

### 7.1 Smart Router

The `SmartRouter` is the brain of the payment system. For every payment request, it selects the optimal PSP in a single pass:

```java
interface SmartRouter {
    RoutingDecision route(PaymentRequest request);
    /**
     * Single-pass algorithm:
     * 1. Get all configured PSPs for the tenant
     * 2. For each PSP, calculate composite score:
     *    - If jurisdiction prohibits this PSP+method: score = 0
     *    - Otherwise: successRate*0.5 + (1-feeRate)*0.3 + speedScore*0.15 + vipBonus*0.05
     * 3. Select highest-scoring PSP (score > 0)
     * 4. If ALL PSPs score 0 → return NO_ELIGIBLE_PSP (explicit error, not silent fallback)
     * 5. If top PSP circuit breaker is OPEN: select next highest
     * 6. Return RoutingDecision with primary + fallback PSPs
     */
}
```

**RoutingDecision** fields: `primaryPSP`, `fallbackPSPs` (ordered list), `scoringBreakdown` (for audit/monitoring), `routingReason` (human-readable).

**Success rate calculation**: Rolling window (configurable, default 1 hour) with decay algorithm — recent outcomes weighted more heavily than older data. Stored in Redis as a sliding window counter per PSP per BIN range per currency.

### 7.2 Circuit Breaker (per PSP)

Three states: CLOSED → OPEN → HALF_OPEN

**Transition thresholds** (all DB-configurable):
- CLOSED → OPEN: 5 consecutive failures OR >10% failure rate in 60-second window
- OPEN duration: 60 seconds (cooldown)
- HALF_OPEN → CLOSED: 3 consecutive successes from probe traffic
- HALF_OPEN → OPEN: Any failure during probing

**Probe traffic in HALF_OPEN**: Route 5% of traffic to the recovering PSP (never risk-critical transactions — only small deposits).

**Monitoring**: Expose circuit breaker state changes as Kafka events for the data/monitoring domain. Dashboard should show real-time PSP health grid.

### 7.3 PSP Adapter Layer

Similar to GP adapters, each PSP has a dedicated adapter:

```java
interface PSPAdapter {
    String getPSPId();
    PSPDepositResult initiateDeposit(DepositRequest request);
    PSPWithdrawalResult initiateWithdrawal(WithdrawalRequest request);
    PSPCallbackResult parseCallback(RawCallback callback);
    HealthCheckResult healthCheck();
}
```

The adapter handles: API authentication, request format normalization, response parsing, error code mapping to internal decline codes, and webhook/callback signature verification.

**Decline code normalization**: Each PSP uses different decline codes. The adapter maps them to an internal enum: `INSUFFICIENT_FUNDS`, `CARD_EXPIRED`, `FRAUD_SUSPECTED`, `DO_NOT_RETRY`, `PROCESSOR_ERROR`, `NETWORK_TIMEOUT`, etc. The routing engine uses this to decide retry strategy.

### 7.4 Retry Strategy

On payment failure:
1. Check decline code category: **Hard decline** (fraud, invalid card, DO_NOT_RETRY) → stop immediately
2. **Soft decline** (insufficient funds, timeout, processor error) → retry via next fallback PSP
3. Maximum 3 retry attempts across different PSPs
4. **Idempotency key persists across retries** — prevents double-charging if primary PSP actually processed but timed out
5. After all retries exhausted → return failure to player with user-friendly message

---

## 8. Payment Deposit (payment-deposit)

### 8.1 Deposit Flow

```
Player initiates deposit
  → DepositLimitChecker: verify single/daily/monthly limits
  → CashierFrictionEvaluator: determine risk tier (low/medium/high)
  → SmartRouter: select optimal PSP
  → PSPAdapter: redirect to PSP payment page
  → (Player completes payment on PSP page)
  → PSP sends callback
  → CallbackHandler: verify signature, parse result
  → CurrencyEngine: convert to settlement currency (if different)
  → WalletCore: credit CASH wallet
  → Kafka: publish DepositCompletedEvent
  → Notification: confirm to player
```

### 8.2 Cashier Friction Evaluator

Determines the number of verification steps based on player risk profile:

```java
interface CashierFrictionEvaluator {
    FrictionTier evaluate(UUID playerId, PaymentMethod method, Money amount);
    /**
     * LOW (≤3 steps): KYC L2+, 3+ successful deposits, risk score < 30
     * MEDIUM (≤5 steps): KYC L1, risk 30-70, or first time using this payment method
     * HIGH (≤7 steps): KYC L0, risk ≥ 70, or jurisdiction requires extra verification
     *
     * Calls risk domain sync API for current risk score (<100ms SLA).
     */
}
```

### 8.3 Deposit Limits

Three-dimensional limit checking:
- **Single transaction**: min/max per payment method (merchant config)
- **Daily cumulative**: per player tier (KYC level determines cap)
- **Monthly cumulative**: per VIP level

All limits are DB-configurable via the three-layer override system (Jurisdiction → Brand → Global).

---

### 8.4 3D Secure / SCA Integration

For EU/PSD2 and UK markets, card deposits must go through 3D Secure 2.0 / SCA. This is a compliance requirement — no 3DS = no launch in regulated EU/UK markets.

**Flow integration** (inserted between PSP redirect and payment completion):

```
Player submits card deposit
  → SmartRouter selects PSP
  → PSPAdapter.initiateDeposit() → PSP returns 3DS challenge URL (if required)
  → Player completes 3DS challenge on issuer's page
  → PSP sends 3DS callback with authentication result
  → ThreeDSHandler.handle3DSCallback() → verify authentication status
     → AUTHENTICATED → proceed with payment
     → FAILED → reject deposit, log reason
     → ATTEMPTED (issuer not enrolled) → proceed (liability shift to issuer)
  → PSP processes payment → callback → credit wallet
```

The `PSPAdapter` interface includes 3DS-specific methods:

```java
interface PSPAdapter {
    // ... existing methods ...
    ThreeDSChallengeResult initiate3DSChallenge(DepositRequest request);
    ThreeDSAuthResult handle3DSCallback(RawCallback callback);
}
```

**3DS decision logic**: The PSP determines whether frictionless or challenge flow is needed based on card issuer's risk assessment. The platform always sends full 3DS data (device fingerprint, billing address, transaction history) to maximize frictionless approval rates.

**SCA triggers** (UK-specific): Transactions >GBP 30 or cumulative >GBP 100 since last SCA. Track cumulative amount per player per payment method in Redis with 24h sliding window.

---

## 9. Payment Withdrawal (payment-withdrawal)

### 9.1 Withdrawal Flow

```
Player requests withdrawal
  → Lock withdrawal amount instantly (<50ms)
  → WageringVerifier: sync query gaming domain for wagering progress (<200ms)
     → Met: auto-convert BONUS to CASH → proceed
     → Not met: reject + show remaining wagering amount
     → Service unavailable (>500ms): queue as WAGERING_CHECK_PENDING
       → NEVER auto-approve
       → 2h timeout → escalate to CS
  → ApprovalWorkflow: route to appropriate approver based on amount
  → Approver approves → SmartRouter selects PSP → execute withdrawal
  → PSP confirms → unlock amount → debit CASH wallet
  → Kafka: publish WithdrawalCompletedEvent
```

### 9.2 Multi-Tier Approval Workflow

```java
interface ApprovalWorkflow {
    ApprovalRoute determineRoute(Money amount, UUID tenantId);
    /**
     * < $100     → CS Agent (auto-approve if pre-conditions met, instant SLA)
     * $100-$1K   → CS Supervisor (1h SLA)
     * $1K-$10K   → CFO (4h SLA)
     * > $10K     → CFO + CEO dual approval (24h SLA)
     *
     * Thresholds are DB-configurable per tenant.
     * SLA clock pauses on bank non-business days for bank transfers.
     * E-wallets and crypto: 24/7, no SLA adjustment.
     */
}
```

### 9.3 Wagering Verification Integration

The funding domain calls the gaming domain's wagering API synchronously during withdrawal:

```java
interface WageringVerifier {
    WageringStatus verify(UUID playerId, UUID tenantId);
    /**
     * Calls gaming domain API: GET /api/v1/wagering/{playerId}/status
     * SLA: < 200ms
     *
     * Returns:
     *   COMPLETE → wagering requirement met, proceed
     *   INCOMPLETE(remaining) → not met, show remaining to player
     *
     * Degradation:
     *   Timeout > 500ms → UNAVAILABLE
     *   → Queue withdrawal as WAGERING_CHECK_PENDING
     *   → Background job retries every 5min
     *   → After 2h unresolved → escalate to CS
     *   → CRITICAL: NEVER auto-approve when wagering service is unavailable
     */
}
```

---

## 10. Payment Dispute (payment-dispute)

### 10.1 Chargeback Lifecycle State Machine

States: `NOTIFICATION → EVIDENCE_COLLECTION → REPRESENTMENT → WON | LOST | PRE_ARBITRATION → ARBITRATION → WON | LOST`

Modeled as Spring Statemachine with:
- **Guards**: Deadline enforcement (7-day evidence window), required fields validation
- **Actions**: Auto-collect evidence on NOTIFICATION→EVIDENCE_COLLECTION transition, notify CS team, update player risk score
- **Timers**: SLA countdown per state; auto-escalate on timeout

### 10.2 Hybrid Evidence Collection

On chargeback notification, the `EvidenceCollector` automatically gathers:
- Transaction details (amount, timestamp, payment method, PSP reference)
- Player login history around the transaction time
- Device fingerprint and IP address
- Game session data (which games played, bet patterns)
- KYC verification status at time of transaction
- Previous interaction history

This produces a structured evidence package. CS team reviews, edits (adds context/removes irrelevant data), and submits to the PSP through the dispute management interface.

### 10.3 Player Penalty Escalation

```java
interface PlayerPenaltyEngine {
    PenaltyAction evaluate(UUID playerId, int chargebackCount, Money cumulativeAmount);
    /**
     * 1 CB → FLAG (monitoring only)
     * 2 CB → RESTRICT (e-wallet/crypto only, block cards)
     * 3 CB → FREEZE (account frozen + manual review required)
     * Cumulative > $1,000 → PERMANENT_CARD_BAN
     */
}
```

---

## 11. Currency Engine (currency-engine)

### 11.1 Money Value Object

The foundational type for all monetary operations across the entire funding domain:

```java
@Value
class Money {
    BigDecimal amount;    // Always via new BigDecimal("0.00") — NEVER from double
    Currency currency;    // ISO 4217

    // Factory methods, arithmetic ops, comparison
    // All arithmetic specifies RoundingMode.HALF_UP explicitly
    // Scale: 4 decimal places for storage, up to 8 for intermediate calculations
}
```

**DB storage**: `DECIMAL(19, 4)` for all monetary columns. Internal intermediate calculations use `DECIMAL(19, 8)` and round to 4 at the point of storage.

### 11.2 FX Rate Management

```java
interface FXRateManager {
    ExchangeRate getRate(Currency from, Currency to);
    FrozenRate freezeRate(Currency from, Currency to, Duration freezeWindow);
    /**
     * Rate sources: external FX provider API
     * Cache: Redis with TTL
     *   - Fiat: refresh every 5 minutes
     *   - Crypto: refresh every 30 seconds
     *
     * Freeze: lock rate for 15 minutes after deposit/withdrawal confirmation
     *
     * FX Risk Sharing (SSOT values, all DB-configurable):
     *   Fiat: platform absorbs ≤0.5% fluctuation; beyond = 50/50 split
     *   Crypto: platform absorbs ≤2%; beyond = player bears (UI confirmation)
     *   Agent settlement: agent bears 100% (settlement day rate)
     */
}
```

### 11.3 Cryptocurrency Integration

**Deposit flow**:
1. `WalletAddressGenerator` creates a unique deposit address per player per transaction
2. `BlockchainMonitor` watches for incoming transactions via node RPC or third-party service
3. Confirmation thresholds: BTC ≥3, ETH ≥12 confirmations → display "pending" until met
4. Pre-credit AML check: wallet address risk scoring via Elliptic/TRM Labs/Chainalysis
5. High-risk address (mixers, darknet, sanctioned) → reject deposit
6. Single deposit >$10K equivalent → Enhanced Due Diligence (EDD)
7. On sufficient confirmations + AML pass → convert to settlement fiat at current rate → credit CASH wallet

**Withdrawal flow**: Generate withdrawal to player's verified wallet address. Require address whitelisting (24h cooling period for new addresses). Rate lock at withdrawal confirmation time.

**Precision handling**: A separate `CryptoAmount` value object handles native crypto precision:

```java
@Value
class CryptoAmount {
    BigDecimal amount;        // Native precision (BTC: 8, ETH: 18, USDT: 6)
    CryptoCurrency currency;  // Enum with precision metadata
    // Conversion: CryptoAmount → Money only at FX conversion point
}
```

- **DB storage for pre-conversion crypto**: `NUMERIC` (PostgreSQL) without fixed scale, or `VARCHAR` for wei values
- **DB storage for post-conversion fiat**: `DECIMAL(19,4)` as standard `Money`
- BTC uses 8 decimal places, ETH uses 18, USDT uses 6
- Conversion from `CryptoAmount` to `Money` happens exactly once — at the deposit-time FX conversion point
- The `crypto_transactions` table stores both original `CryptoAmount` and converted `Money` for audit

---

## 12. Shared Infrastructure

### 12.1 Distributed Lock Abstraction

```java
interface DistributedLock {
    <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action);
    /**
     * Implementation: Redisson RLock with watchdog auto-renewal
     * Default lease: 5 seconds (extended by watchdog if still executing)
     * Default wait: 3 seconds (fail fast if lock not acquired)
     *
     * If Redis is unavailable: the DB atomic conditional update (balance >= amount)
     *   serves as the sole safety net. No advisory lock fallback — it adds complexity
     *   without clear benefit given the DB guard already prevents over-deduction.
     * Monitoring: Expose lock acquisition time, contention rate, timeout rate,
     *   and Redis availability via Micrometer
     */
}
```

### 12.2 Kafka Event Publishing (Transactional Outbox Pattern)

To guarantee at-least-once event delivery without coupling DB transactions to Kafka availability, all events are written to a **transactional outbox table** as part of the same DB transaction that performs the business operation. A separate outbox relay process polls the outbox and publishes to Kafka.

```java
// Inside the same @Transactional method as the wallet update:
outboxRepository.save(new OutboxEvent(topic, key, payload, PENDING));

// Separate relay process (Debezium CDC or polling scheduler):
// Reads PENDING events → publishes to Kafka → marks as PUBLISHED
// Failed publishes → retry with exponential backoff → DLQ after max retries
```

Event types published by this domain:

| Topic | Event | Consumer |
|-------|-------|----------|
| `funding.wallet.debit` | WalletDebitEvent | risk, data |
| `funding.wallet.credit` | WalletCreditEvent | risk, data |
| `funding.wallet.rollback` | WalletRollbackEvent | risk, data |
| `funding.wallet.negative-balance` | NegativeBalanceAlert | risk (priority) |
| `funding.payment.deposit` | DepositCompletedEvent | player, data |
| `funding.payment.withdrawal` | WithdrawalCompletedEvent | player, data |
| `funding.payment.chargeback` | ChargebackEvent | risk, data |
| `funding.psp.circuit-breaker` | PSPStateChangeEvent | data (monitoring) |

All events include: `tenantId`, `playerId`, `timestamp`, `transactionId`, `amount`, `currency`, and event-specific fields.

### 12.3 Governance Domain Stubs

For independent development, the funding domain maintains test stubs:

```java
interface GovernanceClient {
    TenantContext resolveTenant(String tenantId);
    boolean hasPermission(String userId, String permission);
    CreditQuota getAgentCreditQuota(String agentId);
}
```

**Stub implementation**: Returns configurable test data. Swap for real HTTP client at integration time via Spring profile (`@Profile("!stub")`).

### 12.4 DB-Configurable Parameters

All magic numbers, thresholds, rates, and limits are stored in the configurable parameter system with three-layer override:

```
Jurisdiction config → Brand config → Global config (fallback)
```

Key param_keys for this domain (using domain-meaningful prefixes, not chapter numbers):
- `funding.wallet.fx.fiat_absorption_pct` (default: 0.5)
- `funding.wallet.fx.crypto_absorption_pct` (default: 2.0)
- `funding.wallet.idempotency_ttl_hours` (default: 24)
- `funding.wallet.parking_ttl_minutes` (default: 30)
- `funding.wallet.orphan_scan_interval_minutes` (default: 15)
- `funding.wallet.orphan_timeout_hours` (default: 2)
- `funding.payment.psp.circuit_breaker_failure_threshold` (default: 5)
- `funding.payment.psp.circuit_breaker_cooldown_seconds` (default: 60)
- `funding.payment.psp.routing_weight_success` (default: 0.5)
- `funding.payment.psp.routing_weight_fee` (default: 0.3)
- `funding.payment.psp.routing_weight_speed` (default: 0.15)
- `funding.payment.psp.routing_weight_vip` (default: 0.05)
- `funding.payment.withdrawal.approval_tier_1` (default: 100)
- `funding.payment.withdrawal.approval_tier_2` (default: 1000)
- `funding.payment.withdrawal.approval_tier_3` (default: 10000)
- `funding.wallet.gp_rate_limit_per_sec` (default: 2000)
- `funding.payment.recon.max_reversals_per_gp_per_hour` (default: 10)

---

## 13. Cross-Domain Integration Points

### 13.1 Pre-Transaction Risk Check (Synchronous)

Before processing any deposit or high-value transaction, the funding domain calls the risk domain:

```
GET /api/v1/risk/score/{playerId}
→ Returns: { riskScore: 45, riskLevel: "MEDIUM", flags: ["NEW_DEVICE"] }
SLA: < 100ms
```

This score feeds into:
- Cashier friction tier evaluation
- Transaction monitoring alerts
- Large withdrawal flagging

### 13.2 Wagering Progress Query (Synchronous)

On withdrawal:
```
GET /api/v1/wagering/{playerId}/status
→ Returns: { status: "INCOMPLETE", remaining: 500.00, progress: 75% }
SLA: < 200ms
```

### 13.3 KYC Level Query (Synchronous)

On withdrawal and deposit limit checks:
```
GET /api/v1/player/{playerId}/kyc
→ Returns: { level: "L2", withdrawalLimit: { daily: 10000, monthly: 50000 } }
SLA: < 50ms
```

---

## 14. Monitoring & Observability

### 14.1 Key Metrics (Micrometer → Prometheus)

**Wallet metrics**:
- `wallet.balance.accuracy` — gauge: calculated vs actual (target: 99.99%)
- `wallet.transaction.duration` — histogram: per scenario type
- `wallet.transaction.success_rate` — counter: success/total
- `wallet.lock.contention` — counter: lock wait timeouts
- `wallet.orphan_rounds.count` — gauge: current orphan count
- `wallet.negative_balance.total` — gauge: sum of all negative balances

**Payment metrics**:
- `payment.deposit.success_rate` — per PSP
- `payment.psp.circuit_breaker.state` — per PSP (0=closed, 1=half-open, 2=open)
- `payment.routing.optimality` — % of transactions routed to highest-scoring PSP
- `payment.chargeback.rate` — rolling 30-day CB count / total transactions
- `payment.withdrawal.approval_sla` — histogram: time from request to approval

### 14.2 Alert Rules

| Alert | Condition | Severity |
|-------|-----------|----------|
| Over-deduction detected | Any transaction results in unintended negative balance | P0 — immediate |
| Fund loss detected | Reconciliation shows unaccounted money | P0 — immediate |
| All PSPs unavailable | Every circuit breaker is OPEN | P0 — immediate |
| Negative balance exceeds threshold | Platform total < -$10,000 | P1 — 15min |
| Chargeback rate red | ≥ 1.0% rolling | P1 — notify CFO |
| Orphan rounds accumulating | > 50 unresolved orphans | P2 — 1h |
| Reconciliation match dropping | Daily match < 99.5% | P2 — 4h |

---

## 15. Key Invariants (Must-Hold Properties)

These properties must be true at all times. Every test suite should validate them:

1. **Playable Balance ≥ 0** for all player-initiated operations
2. **Every transaction has a unique transactionId** — enforced by DB unique constraint + idempotency store
3. **Deduction order is auditable** — every transaction records which priority layer was applied
4. **Amount precision: 4 decimal places** — cumulative error never exceeds 0.0001
5. **Credit wallet balance is never withdrawable** — enforced at API level and DB constraint
6. **Negative balance triggers account lock within ≤5 seconds** — monitored via metrics
7. **Wagering service unavailable → withdrawal NEVER auto-approved** — enforced by code and tested explicitly
8. **GP is always authoritative for reconciliation** — both amount and existence disputes
9. **Crypto deposits require confirmation threshold before crediting** — BTC≥3, ETH≥12
10. **Idempotency store only caches deterministic outcomes** — transient failures (5xx) are never cached
