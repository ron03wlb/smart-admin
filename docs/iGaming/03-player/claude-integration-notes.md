# Integration Notes — Opus Review Feedback

## Integrating (High Impact)

### 1. State machine migration data transformation (Review 1.2)
**Integrating**: Add explicit `UPDATE` statements in V7 migration mapping old enum values to new ones.
**Why**: Critical — without this, existing data would be corrupted.

### 2. Replace, not extend, state transitions (Review 1.3)
**Integrating**: Clarify that `VALID_TRANSITIONS` is a complete replacement. `LOCKED` and `PENDING_VERIFICATION` are removed.
**Why**: The language "extend" was misleading. Tests need full rewrite.

### 3. Transactional boundary clarification (Review 1.4)
**Integrating**: Explicitly state that `DepositLimitEnforcementService` and `LossLimitEnforcementService` delegate atomic check-and-deduct to their Manager counterparts. Add `ProblemGamblingAlertManager` for transactional alert writes.
**Why**: SmartAdmin rule — `@Transactional` only in Manager.

### 4. Unique constraints on limit tables (Review 2.3)
**Integrating**: Add `UNIQUE(tenant_id, player_id, period)` to both `t_deposit_limit` and `t_loss_limit`.
**Why**: Prevents duplicate limits per player per period.

### 5. Kafka consumer idempotency (Review 6.1)
**Integrating**: Add `event_id` column to all event-driven write tables. Consumers check for duplicate `event_id` before processing.
**Why**: At-least-once delivery means duplicates are inevitable at scale.

### 6. Distributed locking for scheduled jobs (Review 6.5)
**Integrating**: Specify Redisson `RLock` for all 7 scheduled jobs. Each job acquires a named lock before execution.
**Why**: Multi-instance deployment would cause double processing without distributed locks.

### 7. Registration saga / compensation (Review 6.4)
**Integrating**: Add compensation logic — if wallet creation fails, mark player as `REGISTRATION_INCOMPLETE` and retry via async queue.
**Why**: Distributed transaction between player and wallet domains needs explicit error handling.

### 8. VIP points balance strategy (Review 5.3, 2.5)
**Integrating**: Use Redis atomic counter for real-time balance, with periodic DB flush. Make table partitioning a Phase 1 requirement.
**Why**: Row-level lock contention on `t_player` at high bet volumes.

### 9. Error code range fix (Review 7.3)
**Integrating**: Keep all player codes within `303xx` (repack customer service codes to fit). Or explicitly claim `304xx` with documented justification.
**Why**: Range overflow violates the module convention.

### 10. Circuit breaker for 360° view (Review 6.2)
**Integrating**: Add Resilience4j circuit breaker config for each downstream call in `PlayerViewAggregationService`. Specify: 3s timeout, 3 retries, 50% failure threshold.
**Why**: Without this, a slow downstream service blocks the entire aggregation.

## Integrating (Medium Impact)

### 11. Self-exclusion enforcement 5-min wait (Review 6.3)
**Integrating**: Implement wait via scheduled task (check every 30s for 5min) with persistence in `t_self_exclusion_registry_check` for restart recovery.
**Why**: Service restart during wait would lose enforcement state.

### 12. Webhook security (Review 4.1)
**Integrating**: Add rate limiting, timestamp validation (reject >5min old), and nonce tracking to KYC webhook endpoints.
**Why**: Replay attack prevention is standard.

### 13. REGISTERED → ACTIVE trigger clarification (Review 7.2)
**Integrating**: Clarify that the trigger is jurisdiction-dependent. UKGC: first deposit (after KYC L1 completion). Others: first deposit.
**Why**: Spec specifies jurisdiction-dependent activation.

## NOT Integrating

### A. `java.util.Optional` in Manager layer (Review 1.1)
**Not integrating**: The Vavr `Option` rule applies to Service layer per ArchitectureTest. Manager layer using `Optional` for `DomainEventPublisher` (an infrastructure concern, not domain logic) is acceptable. The ArchitectureTest does not enforce this in Manager.
**Why**: This is not a violation — the rule scope is Service layer only.

### B. Currency conversion for limits (Review 2.4)
**Not integrating in this plan**: Multi-currency limit enforcement is a complex topic that spans 02-funding domain. The initial implementation assumes single-currency limits per player. Multi-currency is a separate ADR.
**Why**: Scope control. This is a Phase 2 feature.

### C. Knowledge base seeding (Review 3.2)
**Not integrating**: KB articles are operational content, not implementation artifacts. The plan provides the CRUD system. Content creation is an ops task.
**Why**: This is a content/ops concern, not an engineering plan concern.

### D. SLA compensation Maker-Checker (Review 3.4)
**Not integrating in detail**: The plan correctly identifies the need for Maker-Checker but the detailed workflow (approval table, pending state, etc.) can be designed during implementation as it follows standard SmartAdmin approval patterns.
**Why**: Standard CRUD pattern, not architecturally complex.

### E. Chatbot prompt injection (Review 4.2)
**Not integrating**: This will be handled by the `IntentClassificationAdapter` implementation. The adapter is responsible for sanitization before forwarding to LLM. Adding a separate sanitization layer in the plan is premature.
**Why**: Implementation detail of the adapter, not a plan-level concern.

### F. Payment method cross-referencing (Review 3.3)
**Not integrating**: Payment method data is owned by 02-funding domain. The anti-circumvention section already calls 05-risk-compliance for device fingerprint. Payment method matching should be added to 05-risk-compliance's scope, not duplicated here.
**Why**: Respects SSOT boundaries — payment data belongs to 02-funding, risk detection to 05-risk-compliance.

### G. Data retention implementation detail (Review 3.1)
**Not integrating in detail**: The spec explicitly states "crypto-shredding delegated to 05-risk-compliance." This domain holds the retention period table (already in spec §5.11) but does not implement deletion. Adding more detail would violate SSOT.
**Why**: SSOT boundary — deletion mechanism is 05-risk-compliance's responsibility.

### H. `t_session_config` default values (Review 2.2)
**Not integrating**: This is an implementation detail. The pattern of "no row = use system defaults" is standard in SmartAdmin config tables.
**Why**: Standard pattern, doesn't need plan-level specification.
