# Opus Review

**Model:** claude-opus-4
**Generated:** 2026-03-26T12:00:00Z

---

# Architecture Review: 01-governance-agent Implementation Plan

**File reviewed**: `claude-plan.md`
**Companion files consulted**: `claude-spec.md`, `spec.md`, `claude-research.md`, `deep_plan_config.json`, project `README.md`

---

## Overall Assessment

This is a thorough and well-structured plan. The subsystem breakdown is sound, the data model is reasonable for the scale, and the cross-domain contracts are clear. The areas below represent genuine risks that should be addressed before implementation begins.

---

## 1. Critical: Redis-as-Source-of-Truth for Credit Deductions (Section 6.2)

This is the single most dangerous design in the plan.

The credit deduction flow performs the deduction atomically in Redis first, then asynchronously persists to PostgreSQL. This makes Redis the authoritative source for financial data during the window between deduction and async write-back. Several problems arise:

**Data loss on Redis failure.** If Redis crashes between the Lua-script deduction and the async DB write, the deduction is lost. The bet has been accepted, the player is playing, but the credit was never recorded in the durable store. On cache rebuild from DB, the available credit will be higher than it should be, creating a phantom credit.

**Reconciliation gap.** If the optimistic lock fails on the async DB write, what happens? The plan does not describe a retry or dead-letter mechanism. A deduction that succeeds in Redis but fails in PostgreSQL leaves the system inconsistent.

**Recommendation:** Either (a) make the DB write synchronous (accept latency -- still sub-50ms with pooling), or (b) use a write-ahead approach (Kafka topic, PostgreSQL WAL table) written before Redis update, enabling replay on failure.

---

## 2. Critical: Audit Hash Chain is a Scalability Bottleneck (Section 11)

The hash chain computes `eventHash = SHA-256(prevHash + canonical(eventData))`. This creates a strict serial dependency. For multi-tenant platforms generating thousands of events/second, this is a severe bottleneck.

**Problems:**
- Multiple nodes must agree on chain ordering. Plan does not describe coordination.
- Two nodes reading same `prevHash` simultaneously fork the chain.
- `ChainVerificationJob` checking only "latest 1000" events is insufficient at scale.

**Recommendation:** Partition chain by `tenant_id` (independent hash chains per tenant). Use Kafka partition keyed by tenant_id to sequence events. Verification should sample full range.

---

## 3. High: ThreadLocal Tenant Context Leaks with Virtual Threads (Section 3.2)

Plan uses `ThreadLocal` for tenant context. With Java 21 target, virtual threads make ThreadLocal problematic. Values don't propagate to virtual threads or on carrier thread reassignment.

Not addressed: virtual thread executors, WebFlux paths, parallel streams, CompletableFuture chains on ForkJoinPool.

**Recommendation:** Use `ScopedValue` (Java 21+) instead of/alongside ThreadLocal. Document all propagation contexts. Add integration tests under virtual thread execution.

---

## 4. High: RLS Bypass Vectors (Section 3.1, 14.2)

Bypass vectors:
- **Superuser connections** bypass RLS silently
- **Routing bugs** in `TenantRoutingDataSource` could route shared tenant to dedicated DB
- **Non-MyBatis code paths** (native JDBC, JPA) skip the session variable setup

**Recommendation:** (a) Add `FORCE ROW LEVEL SECURITY`. (b) Connect as non-superuser. (c) Set `rls.tenant_id` at connection pool level (checkout hook), not just MyBatis. (d) Reset to invalid value (-1) on connection return.

---

## 5. High: Materialized Path Concurrency During Hierarchy Changes (Sections 3.3, 6.1)

Moving an agent requires updating all descendants' paths -- large UPDATE with locks. Not safe during concurrent reads on old paths. Spec says "not during settlement" but enforcement mechanism missing.

**Recommendation:** (a) DB advisory locks preventing hierarchy changes during active settlement. (b) Consider closure table as secondary index alongside materialized path.

---

## 6. High: Maker-Checker Expiry and Race Conditions (Section 4.4)

**Gaps:**
- No expiry enforcement mechanism described
- No double-approval prevention
- Hierarchy level comparison for checker eligibility not detailed

**Recommendation:** State machine with optimistic locking. `UPDATE ... WHERE status = 'PENDING'` as atomic guard. Expiry sweep job.

---

## 7. Medium: Commission Calculation Lacks Idempotency (Section 7, 8)

If orchestrator crashes mid-settlement: no mechanism to resume partial run. No "settlement batch" concept.

**Recommendation:** `SettlementBatch` entity tracking run state. Idempotent per-agent calculation (skip if record exists for agent_id + period_key). Input data checksum.

---

## 8. Medium: Credit Monitor vs Real-time Deduction Inconsistency (Section 6.3)

Monitor runs every 60s, but Lua script already rejects over-limit bets. How does usage ever exceed 100%?

**Recommendation:** Clarify that >100% threshold is defense-in-depth against bugs or manual adjustments. Make explicit.

---

## 9. Medium: Missing API Versioning Strategy (Section 13)

Foundation split consumed by 6 other domains. No breaking change management, v1/v2 coexistence, contract testing enforcement, or circuit breaker patterns described.

**Recommendation:** API versioning section, deprecation policy, Spring Cloud Contract enforcement, inter-service circuit breakers.

---

## 10. Medium: Missing Data Encryption at Rest

`mfaSecret` and `ipWhitelist` in `t_agent`. No column-level encryption described. PII in audit `changePayload` not masked. No key management for hash chain.

**Recommendation:** Encryption-at-rest strategy, column-level encryption for secrets, PII masking in audit payloads, key rotation procedures.

---

## 11. Medium: Keycloak Single Point of Failure (Section 5)

Keycloak down blocks: MFA re-auth, impersonation, tenant provisioning. No HA topology described.

**Recommendation:** Keycloak HA (min 2 nodes). Degraded-mode for critical operations during outages.

---

## 12. Low-Medium: Feature Flag Rollout Semantics Undefined (Section 10)

`rolloutPercentage` unit undefined (player? agent? session?). No deterministic hashing described.

**Recommendation:** Define rollout semantics with deterministic hash of entity_type:entity_id mod 100.

---

## 13. Low-Medium: Settlement Timezone Issues (Section 8.1)

"Monday 00:00 UTC" creates confusion for PAGCOR (UTC+8) agents.

**Recommendation:** Configurable settlement timezone per jurisdiction/brand via 3-layer config.

---

## 14. Low: Missing Rate Limiting on Exposed APIs (Section 13.1)

Permission check API could be called millions of times/day. No rate limiting described.

**Recommendation:** Rate limits per consumer, service mesh auth, circuit breakers for consumed APIs.

---

## 15. Low: Spec/Plan Inconsistencies

- Negative carry-forward reset default `-$1M` not seeded
- CASH wallet boundary unclear (02-funding?)
- Agent portal backend APIs not defined (spec FR-AGT-12)
- Agent fraud detection absent (spec FR-AGT-13)

---

## 16. Missing: Database Migration Strategy

No migration tool specified (Flyway/Liquibase). No shared vs dedicated migration strategy. No RLS policy checklist for new tables.

---

## 17. Missing: Disaster Recovery Procedures

No PostgreSQL backup strategy. No Redis persistence config. No hash chain break recovery. No credit divergence runbook.

---

## Summary of Priority Actions

| Priority | Item | Section |
|----------|------|---------|
| **Critical** | Fix Redis-first credit deduction to ensure durability | 6.2 |
| **Critical** | Partition audit hash chain by tenant; solve multi-node ordering | 11 |
| **High** | Address ThreadLocal/virtual thread tenant context propagation | 3.2 |
| **High** | Harden RLS bypass vectors (superuser, connection pool, non-MyBatis) | 3.1, 14.2 |
| **High** | Add concurrency controls for materialized path updates | 3.3, 6.1 |
| **High** | Define Maker-Checker state machine with atomic transitions | 4.4 |
| **Medium** | Add idempotency to settlement orchestrator | 7, 8 |
| **Medium** | Add API versioning and contract testing strategy | 13 |
| **Medium** | Add data-at-rest encryption and PII masking | Throughout |
| **Medium** | Document Keycloak HA topology | 5 |
