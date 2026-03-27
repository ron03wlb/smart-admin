# Opus Review Integration Notes

**Date**: 2026-03-27

---

## Integrating (8 items)

| # | Opus Finding | Action | Section |
|---|-------------|--------|---------|
| 1 | **DB CHECK constraint incorrect** | ✅ Fix to `CHECK (balance >= 0 OR walletType = 'CREDIT')` | §3.1 |
| 2 | **Multi-wallet deduction atomicity missing** | ✅ Add explicit single-TX wrapping for all DeductionSteps | §4.1 |
| 3 | **GP recon needs reversal safeguards** | ✅ Add reversal circuit breaker (max N per GP/hour) | §6.1 |
| 4 | **Lock + version check conflict** | ✅ Remove version check from critical path UPDATE; keep version for optimistic reads | §4.1 |
| 5 | **Parked Wins must persist to DB** | ✅ Add DB persistence for parked transactions with Redis as fast lookup | §4.2 |
| 6 | **3D Secure / SCA completely missing** | ✅ Add §8.4 covering 3DS flow in deposit path | §8 |
| 7 | **Kafka publish failures unhandled** | ✅ Add transactional outbox pattern | §12.2 |
| 8 | **Crypto precision contradictory** | ✅ Add CryptoAmount value object + separate DB column | §11 |

## Integrating (Minor, 5 items)

| # | Opus Finding | Action |
|---|-------------|--------|
| 9 | Idempotency key missing tenantId | ✅ Add tenantId to key composition |
| 10 | Param key naming (ch2 → funding.wallet) | ✅ Rename to domain-meaningful prefixes |
| 11 | GetBalance cache race condition | ✅ Switch to versioned cache key approach |
| 12 | Per-GP rate limiting missing | ✅ Add rate limiting mention to §5 |
| 13 | PSP routing zero-score edge case | ✅ Add NO_ELIGIBLE_PSP result handling |

## NOT Integrating (4 items)

| # | Opus Finding | Reason |
|---|-------------|--------|
| A | Redis fallback to DB advisory lock under-specified | **Agree it's under-specified but removing it entirely.** The DB atomic update IS the safety net. Redis fallback to advisory locks adds complexity without clear benefit. Drop the advisory lock fallback. |
| B | Spring Statemachine over-engineered for round lifecycle | **Disagree.** The round lifecycle has 6 states but complex guards (amount thresholds, GP status, pending review conditions). Spring Statemachine provides declarative guard/action/listener configuration that pays off during testing and audit. Keep it. |
| C | wallet-core and wallet-transaction should be one module | **Disagree.** Separation enables independent testing and clearer ownership boundaries. The coupling is via well-defined interfaces, not shared mutable state. Keep separate. |
| D | Data retention / archival strategy | **Defer.** Valid concern but belongs in 07-data-infrastructure domain plan, not this funding domain plan. Note as cross-domain dependency. |

## Deferred Observations (address in section-level planning)

- DB transaction isolation levels → specify REPEATABLE READ for critical path in section detail
- API versioning strategy → address in seamless-wallet-api section
- Partial withdrawal handling → address in payment-withdrawal section
- Holiday calendar source → address in payment-withdrawal section
- Token anti-replay Redis structure → address in seamless-wallet-api section
- BS-06 missing note → add explicit out-of-scope note
