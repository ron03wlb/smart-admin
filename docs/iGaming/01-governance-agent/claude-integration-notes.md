# Integration Notes — Opus Review Feedback

**Date**: 2026-03-26

---

## Suggestions INTEGRATING (12 of 17)

### 1. Credit Deduction: Write-Ahead Pattern (Critical, #1)
**Integrating**: Change from "Redis-first, async DB" to write-ahead log approach.
**Reason**: Opus is 100% correct — Redis-first for financial data is unacceptable. Will add a durable WAL table write before Redis update. This ensures recovery on Redis failure.

### 2. Audit Hash Chain: Partition by Tenant (Critical, #2)
**Integrating**: Partition hash chains by `tenant_id`.
**Reason**: Single global chain is indeed a bottleneck at scale. Per-tenant chains allow parallel hashing and eliminate cross-node coordination.

### 3. ScopedValue for Virtual Threads (High, #3)
**Integrating**: Add ScopedValue alongside ThreadLocal.
**Reason**: Java 21 target makes this essential. Will document all propagation contexts.

### 4. RLS Hardening (High, #4)
**Integrating**: All 4 recommendations — FORCE RLS, non-superuser, pool-level tenant_id setting, reset on return.
**Reason**: Critical for data isolation guarantees. These are industry best practices.

### 5. Materialized Path Concurrency (High, #5)
**Integrating**: Advisory locks during hierarchy changes + closure table as secondary index.
**Reason**: Valid concern for 10-level trees with 1000+ agents.

### 6. Maker-Checker State Machine (High, #6)
**Integrating**: State machine with atomic transitions and expiry sweep job.
**Reason**: Race conditions on dual approval is a real risk.

### 7. Settlement Idempotency (Medium, #7)
**Integrating**: SettlementBatch entity and idempotent per-agent calculation.
**Reason**: Essential for crash recovery of financial processes.

### 8. Credit Monitor Clarification (Medium, #8)
**Integrating**: Clarify >100% as defense-in-depth.
**Reason**: Documentation gap, easy fix.

### 9. API Versioning Strategy (Medium, #9)
**Integrating**: Add API versioning section.
**Reason**: Foundation split needs explicit stability guarantees.

### 10. Keycloak HA (Medium, #11)
**Integrating**: Add HA topology (min 2 nodes + degraded mode).
**Reason**: Critical infrastructure needs HA planning.

### 11. Database Migration Strategy (Missing, #16)
**Integrating**: Add Flyway section with shared/dedicated strategy and RLS checklist.
**Reason**: Legitimate gap — migrations are fundamental.

### 12. DR Procedures (Missing, #17)
**Integrating**: Add backup/recovery section.
**Reason**: Financial system needs documented DR.

---

## Suggestions NOT Integrating (5 of 17)

### 10. Data Encryption at Rest (Medium, #10)
**Not integrating in plan**: This is a cross-cutting infrastructure concern better addressed in `07-data-infrastructure` or `05-risk-compliance`.
**Reason**: MFA secrets in Keycloak (not our DB). PII masking rules are in `05-risk-compliance` SSOT. Plan will add a note to reference these.

### 12. Feature Flag Rollout Semantics (Low-Medium, #12)
**Not integrating**: Too detailed for this planning phase. Will be defined during implementation.
**Reason**: Feature flag is intentionally "basic" in this split. Advanced rollout semantics can be refined later.

### 13. Settlement Timezone (Low-Medium, #13)
**Partially integrating**: Will note configurability but not redesign the settlement section.
**Reason**: The 3-layer config system already supports per-jurisdiction configuration. The plan mentions this implicitly.

### 14. Rate Limiting (Low, #14)
**Not integrating**: Inter-service rate limiting is better handled at the infrastructure/service-mesh level.
**Reason**: Not governance-domain-specific.

### 15. Spec/Plan Inconsistencies (Low, #15)
**Partially integrating**: Will clarify CASH wallet boundary and agent fraud detection references. Portal APIs and -$1M default are spec-level details, not plan-level.
