# 02-funding TDD Plan — iGaming Funding Domain

> **Companion to**: claude-plan.md v1.1
> **Testing Stack**: JUnit 5 + Mockito + Testcontainers (PostgreSQL + Redis + Kafka) + Spring Boot Test
> **Philosophy**: Write tests BEFORE implementation. Each section below defines what to test.

---

## Testing Approach (New Project)

- **Unit tests**: JUnit 5 + Mockito for isolated business logic
- **Integration tests**: Testcontainers for real PostgreSQL, Redis Cluster, and Kafka
- **Concurrency tests**: Java CountDownLatch/CyclicBarrier for race condition verification
- **Contract tests**: Spring Cloud Contract or Pact for cross-domain API contracts
- **Naming convention**: `{ClassName}Test.java` for unit, `{ClassName}IntegrationTest.java` for integration
- **Coverage target**: 90%+ for wallet-core and wallet-transaction (critical financial paths)

---

## 3. Wallet Core (wallet-core)

### 3.1 Data Model
- Test: Wallet entity creation initializes all three wallet types with zero balance
- Test: DB CHECK constraint rejects negative CASH balance on INSERT/UPDATE
- Test: DB CHECK constraint rejects negative BONUS balance on INSERT/UPDATE
- Test: DB CHECK constraint allows negative CREDIT balance
- Test: Unique index on (playerId, walletType, tenantId) prevents duplicates

### 3.2 Playable Balance Calculation
- Test: Standard mode = CASH + BONUS - locked - inProgress
- Test: Credit mode = creditLimit - usedAmount
- Test: GP that rejects BONUS → playable balance excludes BONUS wallet
- Test: Precision: result always has exactly 4 decimal places
- Test: Balance calculation with zero balance across all wallets returns Money(0)
- Test: Versioned cache key returns cached value when version matches
- Test: Cache miss triggers DB read replica query
- Test: Balance mutation increments version, old cache key naturally expires

### 3.3 Deduction Priority Engine
- Test: Default priority is BONUS → CASH → CREDIT
- Test: Layer 1 (player) override takes precedence over all others
- Test: Layer 2 (GP contract) override applies when no Layer 1 exists
- Test: Layer 3 (game level) override for Live Casino = CASH only
- Test: Layer 4 (system default) applies when no overrides exist
- Test: Multi-wallet deduction: BONUS insufficient → remainder from CASH
- Test: Each DeductionStep records the applied layer for audit
- Test: All deduction steps sum to exactly the requested amount
- Test: Empty wallet → next wallet in priority order

---

## 4. Wallet Transaction Processing (wallet-transaction)

### 4.1 Transaction Processor — Critical Path
- Test: Successful debit acquires Redisson lock, updates DB, releases lock
- Test: Concurrent debits for same player are serialized (only one succeeds at a time)
- Test: Multi-wallet deduction steps execute in a single DB transaction
- Test: Partial deduction failure → entire transaction rolled back (no partial debit)
- Test: `balance >= amount` guard prevents over-deduction even without Redisson lock
- Test: Lock acquisition timeout (>3s wait) → fail fast with appropriate error
- Test: Event written to outbox table within same transaction as balance update
- Test: 10+ concurrent debit attempts for same player → zero over-deduction (stress test)

### 4.2 Nine Scenario Handlers
- Test: Scenario A — Insufficient balance returns error with available balance breakdown
- Test: Scenario A — GP rejects BONUS → BONUS wallet excluded from availability check
- Test: Scenario B — Concurrent bets serialize correctly under Redisson lock
- Test: Scenario C — Duplicate transactionId returns cached response (idempotent)
- Test: Scenario C — Duplicate transactionId with different body → error + warning log
- Test: Scenario D — Win before Bet → parked in DB + Redis; dequeued when Bet arrives
- Test: Scenario D — Parked Win survives Redis restart (DB persistence)
- Test: Scenario D — Parked Win expires after 30min → error log + GP notification
- Test: Scenario D — Parked Win dequeue fails → error handling + CS escalation
- Test: Scenario E — Rollback with missing original → return Success (idempotent)
- Test: Scenario E — Rollback restores balance and updates round state
- Test: Scenario F — Resettlement allows controlled negative balance
- Test: Scenario F — Negative balance triggers account lock within 5 seconds
- Test: Scenario G — Free spin with Bet=0 is accepted; Win>0 credits CASH
- Test: Scenario H — Deduction follows §2.4 priority (BONUS → CASH)
- Test: Scenario I — Win>$10K with manual-approval GP → creates approval task
- Test: Scenario I — Win>$10K with auto-credit GP → credits + freezes for review

### 4.3 Round Lifecycle State Machine
- Test: OPEN → CLOSED transition on Win/Loss settlement
- Test: OPEN → CANCELLED transition on Rollback
- Test: OPEN → TIMEOUT after 2 hours without activity
- Test: TIMEOUT → CLOSED when GP query returns result
- Test: TIMEOUT → PENDING_REVIEW when GP query fails
- Test: CLOSED → ADJUSTED on resettlement
- Test: Invalid transitions are rejected (e.g., CLOSED → OPEN)
- Test: Orphan detector finds rounds OPEN > 2 hours
- Test: High-value orphan ($1,000+) creates priority ticket

---

## 5. Seamless Wallet API (seamless-wallet-api)

### 5.1 Endpoint Design
- Test: All five endpoints return updated balance alongside result
- Test: Request flow: Token → Idempotency → Adapter → Processor → Response
- Test: Per-GP rate limiting: requests beyond limit return HTTP 429
- Test: Rate limit counter resets correctly per second window

### 5.2 Token Validation
- Test: Valid HMAC-SHA256 token accepted
- Test: Expired token (>5 min) rejected for mutation endpoints
- Test: Expired token with 30s grace accepted for GetBalance (LENIENT mode)
- Test: One-time token: second use of same token rejected (anti-replay)
- Test: Invalid signature rejected
- Test: Missing token rejected

### 5.3 Idempotency Store
- Test: First request with transactionId → process and cache
- Test: Duplicate transactionId → return cached response without reprocessing
- Test: Cache TTL 24h → key expires after 24 hours
- Test: Transient error (5xx) is NOT cached → next request reprocesses
- Test: Deterministic error (insufficient balance) IS cached
- Test: Key composition includes tenantId to prevent cross-tenant collision
- Test: Request fingerprint mismatch → error response + warning log

### 5.4 GP Adapter Layer
- Test: Each GP adapter normalizes request to internal model
- Test: Each GP adapter formats response back to GP-specific format
- Test: Unknown GP ID → appropriate error response

---

## 6. Wallet Reconciliation (wallet-reconciliation)

### 6.1 Three-Layer Model
- Test: Layer 2 — Amount mismatch <$1 → auto-correct
- Test: Layer 2 — Amount mismatch >$100 → manual alert
- Test: Layer 2 — Time mismatch <5 min → auto-match
- Test: Layer 2 — Missing on platform → create transaction from GP record
- Test: Layer 2 — Missing on GP → reverse platform transaction
- Test: Reversal circuit breaker: >N reversals per GP/hour → pause + escalate
- Test: Reversal preserves GP report snapshot for audit

### 6.2 Orphan Round Detection
- Test: Scan finds OPEN rounds older than 2 hours
- Test: GP API returns result → auto-close round
- Test: GP API fails → mark PENDING_REVIEW
- Test: $1,000+ orphan → creates priority CS ticket

---

## 7. Payment Gateway (payment-gateway)

### 7.1 Smart Router
- Test: Routes to highest-scoring PSP
- Test: Jurisdiction-ineligible PSP scores 0
- Test: All PSPs score 0 → return NO_ELIGIBLE_PSP error
- Test: Circuit breaker OPEN on top PSP → route to next
- Test: Scoring weights sum correctly (50 + 30 + 15 + 5 = 100%)
- Test: VIP player gets VIP bonus factor applied

### 7.2 Circuit Breaker
- Test: CLOSED → OPEN after 5 consecutive failures
- Test: CLOSED → OPEN after >10% failure rate in 60s window
- Test: OPEN → HALF_OPEN after 60s cooldown
- Test: HALF_OPEN → CLOSED after 3 consecutive probe successes
- Test: HALF_OPEN → OPEN on any probe failure
- Test: State changes publish Kafka events

### 7.3 PSP Adapter Layer
- Test: Each adapter correctly maps internal decline codes
- Test: Hard decline (fraud) → NEVER retry
- Test: Soft decline (timeout) → eligible for retry via fallback PSP

### 7.4 Retry Strategy
- Test: Hard decline → no retry, return failure immediately
- Test: Soft decline → retry via next fallback PSP
- Test: Maximum 3 retry attempts
- Test: Idempotency key persists across PSP retries

---

## 8. Payment Deposit (payment-deposit)

### 8.1 Deposit Flow
- Test: Successful deposit end-to-end: limits → friction → route → PSP → callback → credit
- Test: Deposit exceeding daily limit → rejected

### 8.2 Cashier Friction Evaluator
- Test: KYC L2+ with 3+ deposits and risk<30 → LOW friction (≤3 steps)
- Test: KYC L1 → MEDIUM friction
- Test: KYC L0 or risk≥70 → HIGH friction

### 8.4 3D Secure / SCA
- Test: EU card deposit triggers 3DS challenge via PSP
- Test: 3DS AUTHENTICATED → proceed with payment
- Test: 3DS FAILED → reject deposit
- Test: 3DS ATTEMPTED (issuer not enrolled) → proceed (liability shift)
- Test: UK cumulative amount tracking triggers SCA when threshold exceeded

---

## 9. Payment Withdrawal (payment-withdrawal)

### 9.1 Withdrawal Flow
- Test: Successful withdrawal end-to-end: lock → wagering check → approve → PSP → debit
- Test: Amount locked within 50ms of request

### 9.2 Multi-Tier Approval
- Test: <$100 → CS Agent approval route
- Test: $100-$1K → CS Supervisor with 1h SLA
- Test: $1K-$10K → CFO with 4h SLA
- Test: >$10K → CFO + CEO dual approval with 24h SLA

### 9.3 Wagering Verification
- Test: Wagering met → BONUS auto-converts to CASH → proceed
- Test: Wagering not met → reject with remaining amount displayed
- Test: Wagering service timeout >500ms → queue as WAGERING_CHECK_PENDING
- Test: WAGERING_CHECK_PENDING NEVER auto-approves (critical invariant)
- Test: WAGERING_CHECK_PENDING escalates to CS after 2 hours

---

## 10. Payment Dispute (payment-dispute)

### 10.1 Chargeback Lifecycle
- Test: State transitions follow defined lifecycle
- Test: 7-day evidence deadline enforced
- Test: Timeout → auto-escalation

### 10.2 Evidence Collection
- Test: Auto-collects transaction details, login history, device fingerprint, game data
- Test: Evidence package structured correctly for CS review

### 10.3 Player Penalty
- Test: 1 CB → FLAG
- Test: 2 CB → RESTRICT (cards blocked)
- Test: 3 CB → FREEZE
- Test: Cumulative >$1K → PERMANENT_CARD_BAN

---

## 11. Currency Engine (currency-engine)

### 11.1 Money Value Object
- Test: BigDecimal created from String (NEVER from double)
- Test: Arithmetic always specifies ROUND_HALF_UP
- Test: Scale is always 4 for storage
- Test: Money(100, USD) + Money(50, EUR) → throws CurrencyMismatchException

### 11.2 FX Rate Management
- Test: Fiat rate cached with 5min TTL
- Test: Crypto rate cached with 30sec TTL
- Test: Rate freeze locks rate for 15 minutes
- Test: FX risk sharing: fiat <0.5% → platform absorbs
- Test: FX risk sharing: fiat >0.5% → 50/50 split

### 11.3 Cryptocurrency
- Test: Unique deposit address generated per player per transaction
- Test: BTC deposit with <3 confirmations → status PENDING, not credited
- Test: BTC deposit with ≥3 confirmations → proceed to AML check
- Test: High-risk wallet address → deposit rejected
- Test: CryptoAmount(ETH, 18 decimals) stored without truncation
- Test: CryptoAmount → Money conversion preserves value within 0.0001 precision
- Test: Deposit >$10K equivalent → triggers EDD

---

## 12. Shared Infrastructure

### 12.1 Distributed Lock
- Test: Lock acquired within timeout → action executes
- Test: Lock not acquired within timeout → fail fast
- Test: Lock auto-renewed by watchdog during long operations
- Test: Redis unavailable → DB atomic update still prevents over-deduction

### 12.2 Kafka Outbox
- Test: Event written to outbox in same DB transaction as business operation
- Test: Outbox relay publishes events to correct Kafka topic
- Test: Published events marked as PUBLISHED in outbox
- Test: Failed publish → retry with exponential backoff
- Test: Max retries exhausted → event moves to DLQ

### 12.3 Governance Stubs
- Test: Stub returns configurable test data
- Test: Stub profile ("stub") activates via Spring profile

---

## 15. Key Invariants (Cross-Cutting Tests)

These tests verify the 10 must-hold properties from the plan:

- Test: **INV-1** — No player-initiated operation results in negative CASH/BONUS balance
- Test: **INV-2** — Every transaction has a unique transactionId (DB constraint enforced)
- Test: **INV-3** — Every deduction records which priority layer was applied
- Test: **INV-4** — 10,000 sequential transactions accumulate ≤0.0001 rounding error
- Test: **INV-5** — CREDIT wallet balance cannot be withdrawn via any API path
- Test: **INV-6** — Negative balance detected → account locked within ≤5 seconds
- Test: **INV-7** — Wagering service down → withdrawal NEVER auto-approved
- Test: **INV-8** — Reconciliation follows GP as authority for both amount and existence
- Test: **INV-9** — Crypto deposit credited only after confirmation threshold met
- Test: **INV-10** — Transient errors (5xx) never cached in idempotency store
