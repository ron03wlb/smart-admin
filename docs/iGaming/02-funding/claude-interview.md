# 02-funding Interview Transcript

**Date**: 2026-03-27
**Interviewer**: Claude (Claudian)
**Interviewee**: Ron
**Subject**: iGaming Funding Domain — Wallet System + Payment System Implementation Planning

---

## Q1: Idempotency Key TTL & Out-of-Order Parking Timeout

**Question**: Spec mentions idempotency key cache TTL = 1h, but industry best practice is 24-48h (GPs may retry much later). Also, the out-of-order Win parking timeout is 30min. Should we follow industry standards or keep the shorter windows?

**Answer**: Follow industry: 24h idempotency + 30min parking.

**Decision**: Extend idempotency cache TTL from spec's 1h to **24h** per industry standard. Keep parking timeout at **30min** as spec'd. This gives GPs ample retry window while keeping the parking queue bounded.

---

## Q2: Phase 1 MVP Scope

**Question**: The spec suggests 9 sub-modules. Which are launch-critical vs. deferrable to Phase 2?

**Answer**: All 9 sub-modules are Phase 1.

**Decision**: Full scope — all 9 sub-modules (wallet-core, wallet-transaction, seamless-wallet-api, wallet-reconciliation, payment-gateway, payment-deposit, payment-withdrawal, payment-dispute, currency-engine) are Phase 1 launch requirements.

---

## Q3: Concurrency Strategy for Balance Deduction

**Question**: For the critical balance deduction path (Bet placement), what's your preference between DB pessimistic lock vs Redis distributed lock + DB atomic update?

**Answer**: Redis distributed lock (Redisson) + DB atomic update.

**Decision**: Double-layer concurrency control:
- **Layer 1 (Application)**: Redisson distributed lock keyed by `playerId` to serialize access
- **Layer 2 (Database)**: Atomic conditional update `UPDATE wallet SET balance = balance - :amount WHERE player_id = :id AND balance >= :amount`
- This supports the 5K-20K concurrent target while maintaining correctness

---

## Q4: PSP and GP Integration Scale

**Question**: How many PSPs and Game Providers at launch?

**Answer**: 5+ PSPs + 10+ GPs.

**Decision**: Full payment orchestration layer with smart routing, circuit breaker per PSP, and cascading failover required from day 1. Seamless Wallet API must handle 10+ distinct GP adapter patterns.

---

## Q5: PSP Routing — Jurisdiction Filtering Strategy

**Question**: Should jurisdiction-specific restrictions (e.g., UKGC credit card ban) be a hard filter BEFORE weighted scoring, or handled in one routing pass?

**Answer**: All handled in one routing pass.

**Decision**: Single-pass routing algorithm where jurisdiction compliance is a scoring factor (ineligible = score 0) rather than a separate pre-filter step. This simplifies the routing pipeline and allows future rule flexibility.

---

## Q6: 01-governance-agent Dependency Strategy

**Question**: Assume governance APIs exist, or design stubs?

**Answer**: Both — interface contract + local stubs for testing.

**Decision**: Design against defined interface contracts (tenant_id, RBAC, agent credit APIs). Funding domain builds its own test stubs/mocks to enable independent development and testing. Stubs are swapped for real governance APIs at integration time.

---

## Q7: Crypto Support Scope

**Question**: Is crypto deposit/withdrawal a Phase 1 launch requirement?

**Answer**: Crypto is Phase 1 launch requirement.

**Decision**: Full BTC/ETH/USDT deposit AND withdrawal support at launch. This means:
- Blockchain monitoring infrastructure (confirmation tracking)
- AML/wallet risk scoring integration
- Crypto-specific FX handling (30s rate updates, slippage protection)
- Currency precision handling for crypto (BTC 8 decimals, ETH 18, USDT 6)

---

## Q8: Chargeback Evidence Collection

**Question**: Fully automated evidence collection, manual workflow, or hybrid?

**Answer**: Hybrid — auto-collect data, human reviews and submits.

**Decision**: System auto-prepares evidence package (transaction logs, login history, device fingerprint, play history, IP records). CS team reviews, edits, and submits to PSP within the 7-day evidence window. System tracks deadlines and SLA.

---

## Q9: Seamless Wallet Peak Concurrency

**Question**: Expected peak concurrent requests for Seamless Wallet endpoints?

**Answer**: 5,000-20,000 concurrent wallet requests/sec.

**Decision**: Very high scale target. Architecture implications:
- Connection pooling: HikariCP tuned for high concurrency
- Redis: Cluster mode for Redisson locks
- DB: Read replicas for GetBalance; primary for mutations
- Idempotency cache: Redis Cluster with 24h TTL
- Consider partitioning wallet tables by player_id hash for horizontal scaling

---

## Q10: Cross-Domain Event Pattern

**Question**: Event streaming to risk-compliance domain — message broker or API polling?

**Answer**: Event-driven via Kafka.

**Decision**: Publish all wallet/payment events to Kafka topics. Risk domain subscribes for async monitoring and analytics. Note: The spec's 5-layer risk pipeline still requires **synchronous pre-TX risk checks** (Layer 1 blocking) — this is a direct API call to risk domain, separate from the Kafka event stream. So the pattern is effectively hybrid:
- **Pre-TX**: Sync API call to risk domain (blocking, <100ms SLA)
- **Post-TX**: Async Kafka event for monitoring, analytics, and delayed risk review

---

## Q11: Reconciliation Authority

**Question**: When a discrepancy is found between platform and GP records, who is authoritative for transaction existence disputes?

**Answer**: GP is always authoritative (both amount and existence).

**Decision**: If GP says a transaction doesn't exist, platform reverses it. If GP says different amount, platform adjusts to GP's amount. This is aggressive — platform must have robust internal logging/audit trail as evidence for disputes, but GP is the source of truth for operational reconciliation.

---

## Summary of Key Decisions

| # | Decision | Impact |
|---|----------|--------|
| 1 | 24h idempotency TTL | Extends from spec's 1h; more robust GP retry handling |
| 2 | All 9 modules Phase 1 | Full scope; no deferred modules |
| 3 | Redisson + DB atomic update | Double-layer concurrency for 5K-20K req/sec |
| 4 | 5+ PSPs + 10+ GPs | Full orchestration from day 1 |
| 5 | Single-pass PSP routing | Jurisdiction as scoring factor, not pre-filter |
| 6 | Contract + stubs for governance | Independent development; swap at integration |
| 7 | Crypto Phase 1 | BTC/ETH/USDT full support at launch |
| 8 | Hybrid chargeback | Auto-collect + human review/submit |
| 9 | 5K-20K wallet req/sec | Redis Cluster + DB partitioning + read replicas |
| 10 | Kafka for events | Async events + sync pre-TX risk API |
| 11 | GP always authoritative | Both amount and existence disputes |
