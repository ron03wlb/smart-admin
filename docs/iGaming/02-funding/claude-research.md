# 02-funding Research Findings

> **Date**: 2026-03-27
> **Scope**: Web research on 4 topics for iGaming funding domain (Wallet + Payment)
> **Codebase**: N/A (requirements-only focus per user instruction)

---

## Topic 1: Seamless Wallet API Idempotency Patterns

### Key Findings

**Single Wallet is Industry Standard**
- Player maintains one unified balance across all games/verticals
- GP communicates real-time with operator's central wallet for every bet/win/loss
- Eliminates friction of older Transfer Wallet model

**Transaction Flow (5 Endpoints)**
1. GetBalance — query balance before bet
2. Debit (Bet) — deduct stake
3. Credit (Win) — apply outcome
4. Rollback — revert settled bet
5. Adjust — resettlement

**Idempotency Best Practices**
- Every mutation endpoint must accept an **idempotency key** (typically `transactionId`), UUID ≥128 bits
- Check fast persistent store (Redis) before processing; if key exists, return cached response
- **Scope the key**: endpoint + API key + user context to prevent collisions
- **TTL**: 24-48 hours on stored keys (spec says 1h — may be aggressive; consider extending)
- **Only cache successful ops** or deterministic errors; NOT temporary failures (5xx)
- Game-type-specific dedup: Sports = same transferCode cannot debit twice; Casino = same transferCode may allow second deduction if amount strictly greater

**Out-of-Order Request Handling**
- **Parking/buffering**: Hold Win in pending queue with short timeout (5-10s), retry
- **Round ID correlation**: Group related transactions by roundId
- **Idempotent state machines**: Model round as state machine; queue out-of-sequence events
- **Error code signaling**: Return specific code (e.g., "Bet not exists") telling GP to retry

### Anti-Patterns to Avoid
1. Sequential/predictable idempotency keys — replay attack risk
2. Caching error responses from temporary failures
3. Conflating idempotency keys with resource IDs
4. Skipping request fingerprinting (check key + body match)
5. No TTL on idempotency storage — unbounded memory
6. Single-point-of-failure wallet without redundancy

### Sources
- [DB Gaming: Seamless API Wallet System](https://dbgaming-api.com/api-transfer-wallet/)
- [DEV: Idempotency Keys Safety Net](https://dev.to/leonardkachi/idempotency-keys-your-apis-safety-net-against-chaos-j1b)
- [Zuplo: Implementing Idempotency Keys](https://zuplo.com/learning-center/implementing-idempotency-keys-in-rest-apis-a-complete-guide)
- [568win Seamless Wallet 2.0 API](http://api-doc.568win.com/seamlessWalletv2.html)
- [Playa Solutions: iGaming Integration](https://www.theplaya.solutions/post/seamless-integration-for-igaming-platforms)

---

## Topic 2: Distributed Locking for Financial Transactions (Java/Spring)

### Key Findings

**Optimistic vs Pessimistic Locking**

| Aspect | Pessimistic | Optimistic |
|--------|------------|-----------|
| Mechanism | `SELECT FOR UPDATE`; queue | Version column check at commit |
| Throughput | Lower (sequential) | Higher (parallel) |
| Deadlock Risk | High (multi-table) | None |
| Best For | Low-volume high-conflict | High-volume low-conflict |

**Recommendation**: Pessimistic (`SELECT FOR UPDATE`) for balance deduction path; Optimistic (versioned rows) for reads, audit, reporting.

**Database-Level Strategies**
1. `SELECT FOR UPDATE` — locks specific player row during TX
2. Advisory Locks (PostgreSQL) — app-defined locks using hash of playerId
3. Versioned Rows — `version` column + `WHERE version = ?` + increment

**Redis Distributed Locking (Redisson)**
- Uses Redis Pub/Sub (not polling) for efficient notification
- Watchdog mechanism prevents premature lock expiry
- Spring Boot: `redisson-spring-boot-starter` v3.24.3+
- Spring Integration 7.0+ native `DistributedLock` interface

**Key Redis Patterns for Finance**

| Pattern | Relevance | Use Case |
|---------|-----------|----------|
| Fencing Token Lock | Critical | Monotonic token rejects stale clients; protects against zombie/GC pause |
| Multi-Resource Lock | Critical | Locks multiple keys in deterministic order for multi-account transfers |
| Fair Lock | High | FIFO for payment queues; prevents starvation |
| Heartbeat Lock | High | Background renewal for long-running settlements |
| Redlock | Medium | Multi-master safety; operational complexity trade-off |

**Negative Balance Protection**
- **Atomic check-then-deduct**: `UPDATE wallet SET balance = balance - :amount WHERE player_id = :id AND balance >= :amount`
- **DB CHECK constraint**: `CHECK (balance >= 0)` as last-resort safety net
- **Double-layer**: Redis distributed lock (app) + DB atomic update (inner guard)

### Anti-Patterns to Avoid
1. Infinite lock TTLs
2. Releasing locks without ownership verification (use Lua script)
3. Simple Redis locks in master-replica without Redlock
4. Separate balance check and deduction (race window)
5. Locking at too coarse granularity (table vs row)
6. Not monitoring lock acquisition times/contention
7. No fallback when Redis is down

### Sources
- [Modern Treasury: Pessimistic vs Optimistic Locking](https://www.moderntreasury.com/learn/pessimistic-locking-vs-optimistic-locking)
- [Medium: Twelve Redis Locking Patterns](https://medium.com/@navidbarsalari/the-twelve-redis-locking-patterns-every-distributed-systems-engineer-should-know-06f16dfe7375)
- [EliteDev: Distributed Locks with Redis + Spring Boot](https://java.elitedev.in/java/complete-guide-implementing-distributed-locks-with-redis-and-spring-boot-for-microservices-race-con-30b91527/)
- [Spring Integration Distributed Locks (Official)](https://docs.spring.io/spring-integration/reference/distributed-locks.html)
- [Vlad Mihalcea: Optimistic vs Pessimistic Locking](https://vladmihalcea.com/optimistic-vs-pessimistic-locking/)
- [Alibaba Cloud: Distributed Lock Best Practices](https://www.alibabacloud.com/blog/implementation-principles-and-best-practices-of-distributed-lock_600811)

---

## Topic 3: PSP Smart Routing & Failover Strategies

### Key Findings

**Payment Orchestration Architecture**
- Sits between merchant and multiple PSPs; single API exposure
- Standardization layer normalizes response codes, decline reasons, statuses
- Key players: Primer, Spreedly, Gr4vy, Yuno, Hyperswitch

**Four Primary Routing Strategies**
1. **Auth Rate Based**: Highest historical approval rate for BIN/card type/currency/geography; decay algorithm
2. **Least Cost**: Lowest processing fee for transaction type/corridor
3. **Elimination**: Progressively removes underperforming PSPs from candidate pool
4. **Contracts-Based**: Routes per volume commitments and negotiated rate tiers

**Circuit Breaker Pattern (3 States)**

| State | Behavior |
|-------|----------|
| Closed (normal) | Track failure rates in rolling window |
| Open (tripped) | >threshold failures → reroute all traffic; give PSP recovery time |
| Half-Open (probing) | After cooldown, send small % traffic; success → Closed, fail → Open |

Additional: slow call rate threshold; latency measurement; PSP switchover < 200ms.

**Failover & Retry**
- **Cascading failover**: primary → secondary → tertiary automatic
- **Smart retry**: each retry through different processor
- **Decline code analysis**: hard declines (fraud) = never retry; soft declines (insufficient funds) = safe to retry
- **Idempotency keys persist across retries** — critical to prevent double-charge

**Business Impact**
- 7-13pp approval rate improvement
- 30-40% cost reduction at 10M+ TX/month
- 8% failed transactions recovered through fallbacks
- Best-in-class failover MTTR: < 200ms

### Anti-Patterns to Avoid
1. Single PSP dependency — no failover path
2. Retrying hard declines — scheme fines
3. Flooding backup PSPs with retries — partnership risk
4. Static routing rules — ignore changing performance
5. No decline code normalization — unreliable routing logic
6. Manual failover — too slow
7. Ignoring regional optimization — use local acquirers

### Sources
- [Primer: Payment Routing Guide](https://primer.io/blog/a-guide-to-payment-routing-everything-you-need-to-know)
- [Spreedly: Intelligent Payment Routing](https://www.spreedly.com/blog/guide-to-intelligent-payment-routing)
- [Yuno: PSP Fallback and Routing Logic](https://y.uno/post/choosing-the-right-psp-fallback-and-routing-logic-to-increase-conversions)
- [Gr4vy: PSP Outage Risk Elimination](https://gr4vy.com/posts/downtime-in-payments-how-payment-orchestration-eliminates-psp-outage-risk/)
- [Gr4vy: Credit Card Retries and Routing Logic](https://gr4vy.com/posts/credit-card-retries-and-routing-logic-an-updated-guide/)
- [Slicker: Multi-Gateway Smart Payment Retries](https://www.slickerhq.com/resources/blog/multi-gateway-smart-payment-retries-setup-guide-2025)

---

## Topic 4: Multi-Currency Payment System Design

### Key Findings

**Currency Precision — Cardinal Rule**
> **NEVER use `float`/`double` for monetary values.** IEEE 754 cannot exactly represent most decimal fractions.

| Approach | Pros | Cons |
|----------|------|------|
| BigDecimal (Java) | Exact; flexible scale | Slower; must use String constructor |
| Integer Minor Units | Fast; no precision issues | Conversion layer needed; Int64 insufficient for crypto 18 decimals |
| DB DECIMAL(p,s) | Native support; no conversion | Must define p/s upfront |

**Critical Precision Notes**:
- Most fiat: 2 decimals; JPY/KRW: 0; BHD/KWD: 3
- Crypto: up to 18 decimals (ETH wei)
- Internal calculations: Store with 4-8 extra decimals above display scale
- Rounding: ROUND_HALF_UP or banker's rounding (ROUND_HALF_EVEN) — always explicit
- **Money Pattern** (Fowler): Encapsulate amount + currency as single object; Libraries: Joda-Money, JSR 354 Moneta

**FX Rate Management**
- **Rate freezing**: Lock rates for defined window (3d card acquiring, 30d payouts)
- **Rate caching**: Short TTLs (30s-5min); netting programs up to 12h
- **Markup transparency**: Separate auditable line item, not baked into rate
- **Risk hedging**: Forward contracts or internal netting

**Deposit-Time Conversion vs Multi-Wallet per Currency**

| Aspect | Deposit-Time Conversion | Multi-Wallet |
|--------|------------------------|-------------|
| FX Exposure | Platform bears none post-deposit | Player bears; platform has multi-currency liabilities |
| Simplicity | Single ledger currency | N balances × N ledgers × N settlements |
| Player UX | Sees converted amount | Plays in native currency |
| **Recommendation** | Start here for simplicity | Evolve when business demands (crypto, multi-jurisdiction) |

**Crypto Integration Flow**
1. Generate unique deposit address per player/TX
2. Monitor blockchain via node RPC or service (Alchemy, Infura)
3. **Confirmation thresholds**: 3 for ETH, 6 for BTC → display "pending"
4. **AML screening at deposit**: Elliptic/TRM Labs/Chainalysis wallet risk scoring
5. Credit wallet (crypto or fiat at locked rate)

### Anti-Patterns to Avoid
1. Using `float`/`double` for money — **THE cardinal sin**
2. `new BigDecimal(0.1)` — use `new BigDecimal("0.1")` or `BigDecimal.valueOf()`
3. Using SQL MONEY type — limited precision
4. Assuming all currencies have 2 decimals
5. Passing naked amounts without currency context
6. No rounding mode specification
7. Crediting crypto before sufficient confirmations
8. Skipping AML screening on crypto deposits
9. Baking FX markup into displayed rate

### Sources
- [Cardinal: Storing Currency Values Best Practices](https://cardinalby.github.io/blog/post/best-practices/storing-currency-values-data-types/)
- [DEV: Essential BigDecimal Practices](https://dev.to/luke_tong_d4f228249f32d86/beyond-double-essential-bigdecimal-practices-for-accurate-financial-calculations-52m1)
- [DZone: Never Use Float/Double for Money](https://dzone.com/articles/never-use-float-and-double-for-monetary-calculatio)
- [Altimetrik: Modeling Money in Java](https://www.altimetrik.com/blog/modeling-money-in-java-pitfalls-solutions/)
- [Lightspark: Multi-Currency Processing 2026](https://www.lightspark.com/knowledge/multi-currency-payment-processing)
- [TRM Labs: Crypto AML Compliance 2026](https://www.trmlabs.com/resources/blog/what-is-the-best-crypto-aml-and-compliance-solution-in-2026)

---

## Cross-Topic Insights for 02-funding

### Architecture Recommendations

1. **Double-layer concurrency control**: Redis distributed lock (Redisson) for serialization + DB atomic conditional update (`balance >= amount`) as inner guard
2. **Money value object**: JSR 354 Moneta or custom `Money(BigDecimal, Currency)` — never pass naked numerics
3. **Idempotency store**: Redis with TTL 24h (extend from spec's 1h for GP retry safety)
4. **Round state machine**: FSM for round lifecycle with queued out-of-sequence events
5. **Payment orchestration layer**: Unified adapter abstracting PSP specifics + circuit breaker per PSP
6. **Deposit-time conversion**: Start simple; crypto wallets can be separate currency stores

### Testing Focus Areas
- Concurrent debit race conditions (parallel bet placement)
- Out-of-order Win-before-Bet scenarios
- PSP circuit breaker state transitions
- FX precision arithmetic (cumulative rounding over 10K+ transactions)
- Crypto confirmation reorg handling
- Chargeback lifecycle state machine
- Negative balance edge cases (Resettlement, GP Rollback)
