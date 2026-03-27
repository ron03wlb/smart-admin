# Opus Review

**Model:** claude-opus-4
**Generated:** 2026-03-27T15:30:00+08:00

---

# Implementation Plan Review: 02-funding (iGaming Funding Domain)

**Plan file**: `claude-plan.md`
**Supporting files reviewed**: `claude-spec.md`, `claude-research.md`, `claude-interview.md`

---

## Overall Assessment

This is a well-structured, thorough plan. The double-layer concurrency model is sound, the nine-scenario strategy pattern is clean, and the operational details (orphan detection, reconciliation, circuit breaker) are refreshingly concrete. However, there are several gaps, ambiguities, and potential problems that could cause real pain during implementation or production. They range from data integrity risks to missing failure modes to scope concerns.

---

## 1. Critical Issues (High Risk)

### 1.1 Redisson Lock + DB Version Check: Conflicting Mechanisms on the Critical Path

**Section 4.1**, step 3 uses both an optimistic version check (`WHERE version = :currentVersion`) and the balance guard (`WHERE balance >= :amount`) in the same UPDATE statement. However, the Redisson lock in step 1 already serializes access per player. This creates a problem:

- If the Redisson lock is working correctly, the version check will never fail because no concurrent mutation is happening. The version check is redundant while the lock holds.
- If the Redisson lock fails or expires prematurely, the version check becomes the safety net. But step 4 says "retry max 3" on zero rows updated. A version conflict retry inside an expired lock means you are now retrying without holding the lock, which is exactly the unsafe condition you are trying to prevent.

**Recommendation**: Separate the two safety layers clearly. The DB guard should be `UPDATE wallet SET balance = balance - :amount WHERE id = :id AND balance >= :amount` without the version check (the atomic conditional update is sufficient as the DB-layer safety net). Reserve the version column for other use cases (optimistic reads, audit). Alternatively, if you keep the version check, the retry logic after lock expiry must re-acquire the lock before retrying, not just retry the SQL.

### 1.2 DB CHECK Constraint in Section 3.1 Is Incorrect

The constraint `CHECK (balance + lockedAmount >= 0 OR walletType = 'CREDIT')` does not prevent negative balances. Consider: balance = -500, lockedAmount = 600. The check passes (-500 + 600 = 100 >= 0) but the balance itself is negative. The intent is to prevent the *balance* from going negative, not the sum of balance and lockedAmount. The correct constraint for preventing negative cash/bonus balances should be `CHECK (balance >= 0 OR walletType = 'CREDIT')`. The current formulation allows a player's actual balance to be negative as long as enough is locked, which is exactly the wrong invariant.

### 1.3 GP Authoritative Reconciliation Creates a Financial Attack Vector

**Section 6.1**, Layer 2 reconciliation: "Missing on GP: Reverse the platform transaction (GP is authoritative)." This is an extremely aggressive posture. A compromised or buggy GP reporting API could cause the platform to reverse legitimate transactions. If a GP's reporting endpoint drops records due to a pagination bug, the platform would auto-reverse real player wins.

The stakeholder decision (Interview Q11) confirms this, but the plan lacks any safeguards. There is no mention of:
- Rate limiting on auto-reversals (e.g., max reversals per GP per hour)
- Human approval threshold for bulk reversals
- Audit trail preservation (snapshot of the GP report that triggered reversal)
- Rollback mechanism if GP later corrects their report

**Recommendation**: Add a "reversal circuit breaker" to the reconciliation module. If a single GP's hourly recon triggers more than N reversals (configurable), pause auto-reversal and escalate to manual review. This preserves the GP-authoritative principle while protecting against data quality failures.

### 1.4 Missing: Transaction Atomicity Between Multi-Wallet Deduction Steps

**Section 4.1** describes the deduction flow: resolve deduction order, then execute atomic DB update. But Section 3.3 shows the `DeductionEngine` can return multiple `DeductionStep` entries (e.g., deduct 30 from BONUS, then 70 from CASH). The plan does not specify whether these multiple wallet updates happen in a single DB transaction.

If each `DeductionStep` is a separate SQL UPDATE, a failure between step 1 and step 2 leaves the player in an inconsistent state (partially deducted). The plan must explicitly state that all deduction steps within a single bet are wrapped in a single database transaction, or describe a compensation mechanism if partial deduction occurs.

---

## 2. Significant Issues (Medium Risk)

### 2.1 Redis Fallback to DB Advisory Lock Is Under-Specified

**Section 12.1** states: "Fallback: If Redis is unavailable, degrade to DB advisory lock." This is a one-liner for an extremely complex scenario. Questions that must be answered:

- How is Redis unavailability detected? Timeout on lock acquisition? Redis Cluster health check?
- What is the transition behavior? If Redis goes down mid-operation (lock acquired in Redis, then Redis dies before release), the advisory lock is not held, allowing concurrent access.
- DB advisory locks in PostgreSQL are session-scoped. If the connection pool returns the connection to the pool, the advisory lock is released. This is a known footgun with HikariCP.
- How do you prevent split-brain where some nodes use Redis locks and others use DB locks simultaneously?

**Recommendation**: Either commit to Redis as the sole locking mechanism (with the DB atomic update as the safety net, which is already present), or fully design the fallback mode with its own section. The current one-liner invites a half-baked implementation that creates more problems than it solves.

### 2.2 Out-of-Order Parking (Scenario D) Has a Money Integrity Gap

**Section 4.2**, `OutOfOrderHandler`: "Win arrives before Bet -> park in Redis queue, TTL 30min; background job retries every 5s." But what happens to the player's balance during those 30 minutes? The Win is parked, not credited. If the player checks their balance, they see no win. If they complain, CS has no tool mentioned to manually force-process a parked Win.

More critically: what if the Bet arrives and succeeds, the parked Win is dequeued, but the Win processing fails (e.g., idempotency conflict, amount mismatch)? The plan does not describe error handling for the dequeue-and-process step.

Also, storing parked Wins only in Redis with a 30-minute TTL is risky. If Redis evicts the key or restarts, the parked Win is lost permanently. Since this involves money, parked Wins should be persisted to the database with Redis as a fast lookup layer.

### 2.3 Crypto Precision Handling Is Contradictory

**Section 11.3** states: "BTC uses 8 decimal places internally, ETH uses 18 (stored as String or BigInteger for wei)." But **Section 11.1** states the `Money` value object uses `BigDecimal` with 4 decimal places for storage. These are incompatible.

ETH amounts in wei (18 decimals) cannot be losslessly stored in `DECIMAL(19,4)`. The plan says "converting to 4-decimal fiat only at the point of currency conversion" but does not specify where the native-precision crypto amounts are stored before conversion. Is there a separate crypto ledger table? A different column type? The `Money` value object as defined cannot hold 18-decimal ETH values.

**Recommendation**: Add a `CryptoAmount` value object or extend `Money` to support variable precision. Define a separate DB column type for pre-conversion crypto amounts (e.g., `NUMERIC` without fixed scale, or `VARCHAR` for wei values). The current design will silently truncate crypto amounts.

### 2.4 3D Secure / SCA Is Referenced in the Spec But Absent from the Plan

The spec (`claude-spec.md` Section 3.7) explicitly requires 3D Secure 2.0 for EU/PSD2 and SCA for UK transactions. The plan has zero coverage of this. The `PSPAdapter` interface does not include any 3DS-related methods. There is no mention of the 3DS challenge flow, the redirect/iframe handling, or how frictionless vs. challenge authentication is decided.

3DS is not optional for EU-licensed iGaming operators. This is a compliance gap that would block launch in regulated markets.

**Recommendation**: Add a subsection under payment-gateway or payment-deposit covering 3DS integration. At minimum, the `PSPAdapter` needs methods for `initiate3DSChallenge`, `handle3DSCallback`, and the deposit flow (Section 8.1) must include the 3DS step between PSP redirect and payment completion.

### 2.5 No Handling of Kafka Publish Failures

**Section 4.1**, step 6: "Publish WalletDebitEvent to Kafka." What happens if Kafka is unavailable? The wallet debit has already been committed to the DB (step 3-5). If the Kafka publish fails, the risk domain never sees the event, and the data domain has a gap.

The plan does not mention:
- Outbox pattern (write events to a DB outbox table, relay to Kafka asynchronously)
- Retry policy for failed publishes
- Dead letter handling
- Whether Kafka publish is inside or outside the DB transaction

For a financial system, the outbox pattern is standard practice to guarantee at-least-once event delivery without coupling DB transaction commits to Kafka availability.

### 2.6 GetBalance Cache Invalidation Race Condition

**Section 3.2**: "Cache the computed value in Redis with short TTL (1-2 seconds); Invalidate cache on any balance mutation." This creates a race condition:

1. Thread A reads balance from DB (stale), writes to cache
2. Thread B mutates balance, invalidates cache
3. Thread A's cache write lands after Thread B's invalidation
4. Cache now holds stale data for up to 2 seconds

This is the classic "cache-aside write invalidation race." At 5K-20K requests/sec, this will happen frequently. For a system targeting 99.99% balance accuracy, a 2-second stale window under high concurrency is problematic.

**Recommendation**: Use a versioned cache key (include the wallet version in the cache key or value) or skip caching for GetBalance entirely and rely on read replicas. At <50ms SLA, a well-indexed read replica query should suffice without a cache layer that introduces consistency hazards.

---

## 3. Missing Considerations

### 3.1 No Rate Limiting on Seamless Wallet API
### 3.2 No Mention of Database Transaction Isolation Levels
### 3.3 No Data Retention or Archival Strategy
### 3.4 No Disaster Recovery or Multi-Region Considerations
### 3.5 Missing BS-06 from Cross-Domain Boundary Scenarios
### 3.6 Withdrawal Flow Does Not Address Partial Withdrawals
### 3.7 No API Versioning Strategy

---

## 4. Architectural Observations

### 4.1 Module Coupling Between wallet-core and wallet-transaction
### 4.2 Spring Statemachine for Round Lifecycle May Be Over-Engineered
### 4.3 Single-Pass PSP Routing Score of Zero for Ineligible PSPs Is Fragile

---

## 5. Minor Issues and Nitpicks

### 5.1 Inconsistent BTC Confirmation Threshold (research vs plan)
### 5.2 Idempotency Key Does Not Include tenantId
### 5.3 Configurable Parameter Naming Inconsistency (ch2/ch3 → funding.wallet/payment)
### 5.4 Token Anti-Replay Storage Unbounded (Redis SET limitation)
### 5.5 Holiday SLA Adjuster Needs Calendar Source

---

## Summary of Recommendations (Priority Order)

1. **Fix the DB CHECK constraint** (Section 3.1) -- data integrity bug
2. **Clarify multi-wallet deduction atomicity** (Section 4.1) -- data integrity gap
3. **Add safeguards to GP-authoritative reconciliation** -- financial attack vector
4. **Resolve the Redisson lock + version check conflict** (Section 4.1) -- correctness under failure
5. **Persist parked Wins to DB**, not just Redis (Section 4.2) -- money loss risk
6. **Add 3D Secure / SCA coverage** -- regulatory compliance gap
7. **Add outbox pattern for Kafka events** -- event delivery guarantee
8. **Design crypto precision handling properly** (Section 11) -- data truncation risk
9. **Remove or fully design the Redis-to-DB lock fallback** (Section 12.1) -- half-baked fallback is worse than none
10. **Add per-GP rate limiting** to the Seamless Wallet API -- availability protection
