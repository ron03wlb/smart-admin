# Opus Review

**Model:** claude-opus-4-6
**Generated:** 2026-03-27T00:00:00Z

---

## Implementation Plan Review: 03-player (Player Domain)

### 1. Critical Architecture Issues

**1.1 PlayerStateManager uses `java.util.Optional` -- plan does not address this violation**

The existing `PlayerStateManager` uses `java.util.Optional<DomainEventPublisher>` for the `domainEventPublisher` field. The plan says it will "extend" this class but never flags this existing violation of the Vavr `Option` rule. Since Manager layer is not Service layer, `Optional` might be technically permissible in Manager, but the plan should explicitly acknowledge the convention and clarify whether Manager layer is also subject to the Vavr requirement.

**1.2 State machine enum mismatch -- the plan invents states that do not exist**

The existing `PlayerStatusEnum` has: `ACTIVE(1), LOCKED(2), SUSPENDED(3), PENDING_VERIFICATION(4), CLOSED(5), SELF_EXCLUDED(6)`. The plan proposes 7 states: `REGISTERED, ACTIVE, DORMANT, SELF_EXCLUDED, COOLING_OFF, SUSPENDED, CLOSED`. This means:
- `LOCKED` and `PENDING_VERIFICATION` are being removed (plan does not mention this)
- `REGISTERED`, `DORMANT`, and `COOLING_OFF` are being added
- The integer values will change (plan uses `REGISTERED(1)`, but existing `ACTIVE(1)`)

This is a **breaking migration** that will invalidate all existing status values in the database. The plan says "clean break migration" but never specifies an `UPDATE t_player SET status = ...` data migration step. If there are any existing players in the system, this will corrupt their state.

**1.3 Existing `VALID_TRANSITIONS` map must be completely replaced, not extended**

The plan says "expand `PlayerStateManager` to support the full 7-state model" (Section 3.1). But the existing transition map includes transitions like `LOCKED -> ACTIVE` and `PENDING_VERIFICATION -> ACTIVE` that do not exist in the new model. The plan should explicitly state this is a **replacement**, not an extension, and call out that tests will need full rewrite.

**1.4 Missing Manager classes for services that need transactions**

The plan creates `DepositLimitService` and `LossLimitService` (Section 7.2) and says enforcement "must be atomic -- check and deduct in the same transaction." But enforcement logic is placed in services rather than Manager layer. Per SmartAdmin rules, `@Transactional` can only be in Manager. The directory structure shows `DepositLimitManager` and `LossLimitManager`, but the text in Section 7.2 describes the enforcement in Service classes. This ambiguity will lead to incorrect placement.

Similarly, `ProblemGamblingDetectionService` writes to `t_problem_gambling_alert` and publishes events -- if this needs to be transactional, it belongs in a Manager.

### 2. Database Design Issues

**2.1 Missing `t_player_audit_log` schema update**

The plan references this table but the migration section does not mention whether it needs schema changes to support the new states.

**2.2 No `t_session_config` default values specified**

Section 2.2 lists `t_session_config` but does not specify if these are nullable or have defaults. For players who never configure session protection, should a row exist?

**2.3 `t_deposit_limit` and `t_loss_limit` lack unique constraints**

There is no unique constraint for `(tenant_id, player_id, period)`. Without this, a player could have multiple DAILY limits.

**2.4 Currency handling for limits is underspecified**

The plan does not explain how limits interact with multi-currency deposits. If a player sets a GBP 500 daily limit but deposits in EUR, how is the conversion handled?

**2.5 `t_vip_points_transaction` will grow unboundedly**

At 1M players, this table could hit billions of rows quickly. Partitioning should be a Phase 1 requirement, not a "consider" suggestion. Also, updating `t_player.vip_points_balance` on every bet is a contention point.

**2.6 Migration version conflict**

The plan says `V7__player_domain_expansion.sql`, but the uncommitted `V6__governance_domain_tables.sql` and the integration test module's different versioning scheme need addressing.

### 3. Missing Spec Coverage

**3.1 Data Retention / GDPR crypto-shredding** -- Zero implementation detail. No scheduled job, no retention policy table, no delegation interface.

**3.2 Knowledge Base seeding** -- 145+ articles, 6 languages. No seeding strategy defined.

**3.3 Anti-circumvention: payment method cross-referencing** -- Spec mentions it, plan covers device fingerprint and IP but not payment method matching.

**3.4 SLA compensation Maker-Checker flow** -- No table for pending compensations, no approval workflow.

### 4. Security Concerns

**4.1** KYC webhook: No rate limiting, IP whitelisting, or replay prevention.
**4.2** Chatbot: No prompt injection mitigation or content filtering.
**4.3** 360-degree view: No data-level masking for PII across domains.

### 5. Performance Concerns

**5.1** RFM daily calculation: No strategy for aggregation at scale (pre-materialized views? read replicas?).
**5.2** SLA escalation: Job runs every minute but query pattern unspecified.
**5.3** VIP points balance: Row-level lock contention on `t_player` for active players.

### 6. Architectural Gaps

**6.1** No idempotency strategy for Kafka consumers (at-least-once delivery = double processing risk).
**6.2** No circuit breaker config for 360-degree view cross-domain calls.
**6.3** Self-exclusion 5-minute wait implementation unspecified (how to survive service restart?).
**6.4** No compensation logic for partial registration failure (wallet creation fails).
**6.5** No distributed locking for scheduled jobs (multi-instance deployment).

### 7. Spec-Plan Inconsistencies

**7.1** Agent capacity: "3-5" conversations vs "20" tickets are different metrics but not clarified.
**7.2** REGISTERED → ACTIVE trigger: Plan says "first deposit" but spec has jurisdiction-dependent rules.
**7.3** Error code range: 30400+ is outside `303xx` -- this is `304xx` territory.

### 8. Concrete Suggestions

1. Add "Migration Data Transformation" subsection with explicit `UPDATE` statements mapping old enum values to new.
2. Create `PlayerRegistrationSaga` for partial failure compensation.
3. Add `event_id` idempotency key to all event-driven write tables.
4. Move enforcement logic descriptions to explicitly reference Manager classes.
5. Add distributed lock specification for all scheduled jobs (Redisson v3.50.0).
6. Add "Security" section: webhook replay prevention, prompt injection, PII masking, rate limiting.
7. Specify VIP points balance update strategy (Redis counter or computed SUM).
8. Make table partitioning Phase 1 requirement for append-only tables.
9. Add timeout/retry/circuit-breaker configuration for cross-domain calls (concrete values).
10. Add testing strategy section.

### Summary

The plan is comprehensive in scope and covers functional requirements well. Main risks: (a) destructive state machine migration without data mapping, (b) ambiguous Service/Manager transactional boundaries, (c) no idempotency for event-driven writes, (d) unaddressed distributed job execution, (e) missing security considerations. The plan needs more precision on distributed error handling and hot-path performance.
