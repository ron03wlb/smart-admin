# TDD Plan — 01-governance-agent

**Testing Stack**: JUnit 5 + Mockito + Testcontainers (PostgreSQL + Redis + Keycloak) + ArchUnit + REST Assured

Each section mirrors `claude-plan.md` and defines tests to write BEFORE implementation.

---

## 2. Architecture Overview

```java
// ArchUnit: SmartAdmin V3.0 layer dependency enforcement
// Test: Controller layer only depends on Service layer
// Test: Service layer only depends on Manager and DAO layers
// Test: Manager layer can depend on Service and DAO layers
// Test: DAO layer has no upward dependencies
// Test: No circular package dependencies within sa-module-governance
// Test: All entities are in dao/entity package
// Test: All mappers are in dao/mapper package and implement BaseMapper
```

---

## 3. Multi-Tenant Data Isolation

### 3.1 Hybrid Strategy

```java
// Test: RLS policy blocks queries without tenant context set
// Test: RLS policy returns only matching tenant rows when context is set
// Test: FORCE ROW LEVEL SECURITY blocks table owner from bypassing
// Test: Connection pool checkout hook sets rls.tenant_id
// Test: Connection pool return hook resets rls.tenant_id to -1
// Test: Non-superuser role used for all application connections
// Test: RLS overhead < 5% (JMH benchmark: query latency with vs without RLS)
```

### 3.2 Tenant Context Flow

```java
// Test: TenantContextFilter extracts tenant_id from JWT and sets ThreadLocal
// Test: TenantContextFilter clears ThreadLocal on request completion
// Test: TenantRoutingDataSource routes shared-tier tenant to shared DB
// Test: TenantRoutingDataSource routes dedicated-tier tenant to dedicated DB
// Test: TenantRlsInterceptor sets rls.tenant_id before every MyBatis query
// Test: ScopedValue propagation works with virtual threads
// Test: TenantAwareTaskDecorator propagates context to @Async child threads
// Test: Kafka consumer correctly reads tenant_id from message headers
// Test: Cross-tenant data leak test — Tenant A data invisible to Tenant B query
```

### 3.3 Tenant Hierarchy Model

```java
// Test: Materialized path correctly generated on tenant creation
// Test: Subtree query via LIKE returns all descendants
// Test: Direct children query via parent_id is correct
// Test: Ancestry extraction from path string is correct
// Test: Shared data rules — blacklist visible within brand, invisible across brands
// Test: Cross-tenant query audit — every cross-tenant query creates audit record
```

---

## 4. RBAC & Authorization

### 4.1-4.2 Permission Check

```java
// Test: Permission check returns true for granted resource:action
// Test: Permission check returns false for missing permission
// Test: Redis cache hit returns correct result (SISMEMBER)
// Test: Cache miss triggers DB load and Redis Set population
// Test: Cache invalidation on role change (via Pub/Sub)
// Test: Permission check latency < 10ms p99 (JMH benchmark with Redis)
// Test: Custom role creation respects max 20 per tenant limit
// Test: Permission bundle expansion (billing:manage → view/create/refund)
// Test: Three-question auth check — resource belongs to tenant, user in tenant, role grants permission
```

### 4.3 Impersonation

```java
// Test: Super Admin can impersonate any Brand/Tenant/Agent
// Test: Brand Admin can impersonate only their own tenants/agents
// Test: Tenant Admin cannot impersonate other tenants
// Test: Impersonation session creates audit record with both actor and target
// Test: Actions during impersonation cannot exceed target's permissions
// Test: JWT contains impersonating=true, actorId, targetId claims
```

### 4.4 Maker-Checker

```java
// Test: PendingChange created with PENDING status on maker initiation
// Test: Different user with approve permission can approve
// Test: Maker cannot approve their own change
// Test: Double approval prevented (atomic CAS on status=PENDING)
// Test: Expired changes (>72h) swept to EXPIRED by job
// Test: Approved change applied atomically within transaction
// Test: Rejected change not applied, checker comment recorded
// Test: Hierarchy level comparison — checker at higher level can approve
```

---

## 5. Authentication & Security (Keycloak)

```java
// Test: MFA required for all forced-role accounts (cannot login without TOTP)
// Test: First login forces MFA setup before any operation
// Test: IP whitelist blocks non-whitelisted IPs
// Test: Session timeout after 30min idle
// Test: Single session enforcement — new login kicks old session
// Test: Lockout after 5 failures (15min), 10 failures (1hr), 20 failures (permanent)
// Test: @RequireReAuth annotation blocks operation without fresh TOTP code
// Test: Valid X-MFA-Code header passes re-auth check
// Test: Keycloak HA — failover to standby node on primary failure
```

---

## 6. Agent Hierarchy & Credit Network

### 6.1 Agent Tree

```java
// Test: Max 10 levels enforced on agent creation
// Test: Credit allocation is top-down only (parent → child)
// Test: Child credit limit cannot exceed parent's available allocation
// Test: Agent creation requires Maker-Checker approval
```

### 6.2 Credit Management (Write-Ahead)

```java
// Test: Credit deduction writes WAL entry BEFORE Redis update
// Test: Successful deduction: WAL entry PENDING → COMMITTED
// Test: Insufficient credit: WAL entry marked REJECTED, Redis unchanged
// Test: Redis Lua script atomically deducts (no race condition under concurrent calls)
// Test: Redis failure fallback: DB with optimistic lock succeeds
// Test: Recovery process replays PENDING WAL entries to rebuild Redis state
// Test: Credit reconciliation detects and resolves DB/Redis divergence
// Test: Credit deduction latency < 50ms p99 (JMH benchmark)
```

### 6.3 Risk Thresholds

```java
// Test: CreditMonitorJob detects 81-90% usage → publishes CreditWarningEvent
// Test: CreditMonitorJob detects 91-100% → publishes CreditHighRiskEvent
// Test: CreditMonitorJob detects >100% → publishes CreditCriticalEvent (freeze)
// Test: Thresholds configurable per brand via 3-layer config
```

### 6.4 Self-Exclusion Integration

```java
// Test: PlayerSelfExclusionEvent triggers credit wallet freeze
// Test: Credit operation rejected when player is self-excluded
// Test: Agent notified on player exclusion
```

---

## 7. Commission Engine

### 7.1 Strategy Pattern

```java
// Test: RevenueShareStrategy calculates correct commission for each tier
// Test: TurnoverRebateStrategy applies correct game-type rates
// Test: CpaStrategy counts only first-time depositing players
// Test: HybridStrategy sums RevShare + Turnover correctly
// Test: Single model agent — only one strategy executed
// Test: Multi-model agent — all active strategies executed and summed
```

### 7.2 Revenue Share Tiers

```java
// Test: GGR < $10K → 30% share
// Test: GGR $10K-$50K → 35% share
// Test: GGR $50K-$200K → 40% share
// Test: GGR > $200K → 45% share
// Test: Boundary values ($9,999.99, $10,000.00, $50,000.00)
```

### 7.3 Position Holding

```java
// Test: Position distribution calculated correctly across agent chain
// Test: Sub-agent positions > 100% normalized proportionally
// Test: Player loss → agent receives position percentage of profit
// Test: Player win → agent bears position percentage of loss
```

### 7.4 Negative Carry-Forward

```java
// Test: Negative balance carries to next period (CARRY policy)
// Test: RESET_MONTHLY resets balance to 0 at month boundary
// Test: CAP_AT_AMOUNT caps carry-forward at configured amount
// Test: Double-entry bookkeeping — all entries balance
```

---

## 8. Settlement System

### 8.1 Settlement Batch & Workflow

```java
// Test: Settlement batch created with RUNNING status
// Test: Idempotent calculation — existing record for (agent, period) is skipped
// Test: Amount < $10K auto-approved
// Test: Amount $10K-$50K routed to Manager queue
// Test: Amount $50K-$100K routed to CFO queue
// Test: Amount ≥ $100K requires CFO + CEO dual approval
// Test: Payout failure triggers auto-retry (up to 3x over 3 business days)
// Test: After 3 failures → status ESCALATED, manual queue alert
// Test: Crash recovery — partial batch resumes correctly
```

### 8.2 Anomaly Detection

```java
// Test: Monthly total ≥$500K triggers upgrade review
// Test: Weekly growth >200% vs 4-week average triggers FLAG
// Test: New agent first-month ≥$50K triggers auto FLAG
```

---

## 9. Compliance Matrix

```java
// Test: ComplianceService returns correct rule for jurisdiction + category + key
// Test: Effective date filtering — only current rules returned
// Test: Cross-jurisdiction conflict → strictest rule applied
// Test: Compliance rule change creates ComplianceRuleChangedEvent
// Test: 2026 deadlines seeded correctly (UKGC Jan/Mar/Apr/Jun, PAGCOR Mar)
```

---

## 10. Feature Flag System

```java
// Test: Flag enabled for tenant returns true
// Test: Flag disabled for tenant returns false
// Test: Resolution order: tenant override → brand default → platform default
// Test: Flag change requires Maker-Checker
// Test: Flag cached in Redis with 60s TTL
// Test: Flag invalidated via Pub/Sub on change
```

---

## 11. Immutable Audit System

```java
// Test: Audit event creates hash chain entry linked to previous event
// Test: Hash chain partitioned by tenant_id (independent chains)
// Test: Append-only table — UPDATE and DELETE blocked by trigger
// Test: ChainVerificationJob detects tampered event (hash mismatch)
// Test: Kafka partition by tenant_id ensures per-tenant ordering
// Test: Impersonation audit includes both actor and impersonator IDs
// Test: Hot-to-warm archival: correct row count + hash verification after move
// Test: Cold archive accessible within 24h on regulatory request
```

---

## 12. White-Label & Tenant Lifecycle

```java
// Test: Tenant provisioning creates Keycloak realm/group
// Test: Tenant provisioning creates DB record with materialized path
// Test: Dedicated-tier tenant gets new database with full migration set
// Test: White-label config (domain, branding) applied correctly
// Test: Default roles seeded from templates
// Test: GDPR export produces valid JSON with manifest
// Test: Billing overdue escalation: 7d email, 14d high-risk, 30d freeze, 31d suspend
```

---

## 13. Cross-Domain API Contracts

```java
// Spring Cloud Contract tests for all exposed APIs:
// Test: GET /api/v1/tenants/{id}/context — returns tenant config
// Test: POST /api/v1/rbac/check — returns permission boolean
// Test: GET /api/v1/compliance/rules — returns rules by jurisdiction
// Test: GET /api/v1/agents/{id}/credit — returns credit summary
// Test: POST /api/v1/agents/credit/deduct — deducts credit
// Test: POST /api/v1/agents/credit/release — releases credit
// Test: GET /api/v1/agents/player/{playerId} — returns agent relationship
// Test: GET /api/v1/flags/{key} — returns flag status
```

---

## 17. Hierarchy Change Locking

```java
// Test: Advisory lock prevents concurrent hierarchy changes on same subtree
// Test: Hierarchy change rejected during active settlement
// Test: Materialized path update is atomic for all descendants
// Test: Closure table maintained correctly on agent move
// Test: Redis caches invalidated for affected agents after hierarchy change
```

---

## 18-22. Infrastructure Concerns

```java
// Test: Flyway migrations apply to both shared and dedicated databases
// Test: Every new table has RLS policy (CI validation query returns 0 rows)
// Test: CreditRecoveryService rebuilds Redis from WAL entries after failure
// Test: Credit reconciliation detects and resolves DB/Redis mismatch
// Test: Keycloak failover — existing sessions continue after primary node failure
```
