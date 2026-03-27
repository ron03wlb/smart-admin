# Implementation Plan — 01-governance-agent (Governance & Agent Domain)

**Version**: 1.0
**Date**: 2026-03-26
**Target**: Spring Boot 3.x + Java 21 + PostgreSQL + Keycloak + Redis

---

## 1. Executive Summary

This plan describes the implementation of the **Governance & Agent Domain** — the foundational layer of a multi-jurisdiction iGaming platform. Every other business domain (funding, player, gaming, risk, frontend, infrastructure) depends on this split for tenant isolation, role-based access control, and the agent credit network.

The system serves **10+ brands, 50+ tenants, 1000+ agents** across four regulatory jurisdictions (MGA, UKGC, Curacao, PAGCOR). It is built on Spring Boot 3.x with Java 21, uses PostgreSQL with a hybrid row-level security (RLS) isolation strategy, Keycloak for identity management, and Redis for permission and credit caching.

The domain has three major subsystems:
1. **Governance Engine** — Multi-tenant hierarchy, RBAC, data isolation, white-label, billing
2. **Agent Network** — Agent hierarchy (10 levels), credit management, commission calculation, settlement
3. **Platform Services** — Feature flags, immutable audit trail, compliance matrix, cross-domain APIs

---

## 2. Architecture Overview

### 2.1 Module Structure (SmartAdmin V3.0 Four-Layer)

```
smart-admin-api/
  └── sa-module-governance/
        ├── controller/        # REST endpoints (entry layer)
        │   ├── tenant/
        │   ├── rbac/
        │   ├── agent/
        │   ├── commission/
        │   ├── settlement/
        │   ├── audit/
        │   ├── compliance/
        │   └── feature-flag/
        ├── service/           # Business logic layer
        │   ├── tenant/
        │   ├── rbac/
        │   ├── agent/
        │   ├── commission/
        │   ├── settlement/
        │   ├── audit/
        │   ├── compliance/
        │   └── feature-flag/
        ├── manager/           # Orchestration layer (cross-service coordination)
        │   ├── TenantLifecycleManager
        │   ├── AgentCreditManager
        │   ├── SettlementOrchestrator
        │   └── ComplianceEnforcementManager
        ├── dao/               # Data access layer
        │   ├── entity/
        │   ├── mapper/        # MyBatis-Plus mappers
        │   └── cache/         # Redis cache operations
        ├── domain/            # Domain objects, enums, value objects
        ├── config/            # Spring configuration
        ├── event/             # Domain events (publish/subscribe)
        └── infrastructure/    # Cross-cutting: tenant context, RLS, audit interceptor
```

### 2.2 Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Data isolation | Hybrid RLS + Dedicated DB | Balance cost and compliance; UKGC brands get physical isolation |
| Auth provider | Keycloak self-hosted | SAML/OIDC, impersonation, RBAC built-in; self-hosted for data sovereignty |
| Permission cache | Redis Sets | O(1) `SISMEMBER` checks; sub-10ms p99 |
| Credit performance | Redis + event-driven sync | O(1) credit lookup; no recursive tree walk per bet |
| Commission engine | Strategy pattern | Configurable per agent; supports single or concurrent models |
| Audit storage | Hash-chain + tiered | Immutable event log; hot/warm/cold storage tiers |
| Config override | 3-layer merge | Jurisdiction → Brand → Global with strictest-rule-applies for compliance |
| Maker-Checker | Broadest scope | All sensitive operations including config overrides |

---

## 3. Multi-Tenant Data Isolation

### 3.1 Hybrid Strategy

The system uses two isolation tiers:

**Shared Tier (RLS)**: PostgreSQL Row-Level Security applied to all tables. A session variable `rls.tenant_id` is set by the `TenantContextFilter` on each request. RLS policies filter all queries to the current tenant's data. Every table includes a non-nullable `tenant_id` column with a B-tree index.

**Dedicated Tier**: Brands requiring physical isolation (e.g., UKGC-licensed operators) get their own PostgreSQL database. The `TenantRoutingDataSource` (extending `AbstractRoutingDataSource`) routes connections based on tenant configuration. Schema is identical across shared and dedicated databases to allow tenant migration between tiers.

### 3.2 Tenant Context Flow

1. API Gateway extracts `tenant_id` from JWT claims (set by Keycloak at login)
2. `TenantContextFilter` (Spring `OncePerRequestFilter`) stores it in `TenantContext` (ThreadLocal)
3. `TenantRoutingDataSource` routes to shared or dedicated database based on tenant config
4. For shared DB: `TenantRlsInterceptor` (MyBatis interceptor) executes `SET rls.tenant_id = ?` before every query
5. On request completion, `TenantContextFilter` clears ThreadLocal to prevent context leakage

**Async propagation**: For `@Async` methods and message consumers, a `TenantAwareTaskDecorator` copies `TenantContext` to the child thread. For Kafka consumers, tenant_id is read from message headers.

**Virtual thread safety (Java 21)**: `ThreadLocal` does not auto-propagate to virtual threads. The system uses `ScopedValue` (Java 21+) as the primary mechanism for tenant context within request-scoped code paths. `ThreadLocal` is retained as fallback for libraries that don't support ScopedValue. All contexts requiring propagation are documented:
- Virtual thread executors (structured concurrency)
- CompletableFuture chains on ForkJoinPool
- Parallel stream operations within service methods
- Integration tests verify tenant isolation under virtual thread execution

### 3.3 Tenant Hierarchy Model

```java
@Table("t_tenant")
class TenantEntity {
    Long id;
    Long parentId;           // null for Platform
    String materializedPath; // "/1/5/12" — enables LIKE prefix queries
    TenantLevel level;       // PLATFORM, BRAND, TENANT
    String name;
    String fqdn;             // white-label domain
    IsolationTier tier;      // SHARED, DEDICATED
    String dbIdentifier;     // null for SHARED; DB name for DEDICATED
    TenantStatus status;     // ACTIVE, SUSPENDED, MIGRATING, CLOSED
    JsonNode configJson;     // tenant-level overrides
}
```

The `materializedPath` enables efficient hierarchy queries:
- "All tenants under Brand 5": `WHERE materialized_path LIKE '/1/5/%'`
- "Direct children of Brand 5": `WHERE parent_id = 5`
- "Full ancestry of Tenant 12": Split path string into ancestor IDs

### 3.4 Data Sharing Rules

| Data Type | Scope | Controlled By |
|-----------|-------|--------------|
| Player blacklist | Brand-level (opt-in) | Brand Admin |
| Game Provider list | Global | Super Admin |
| Payment providers | Tenant-independent (own merchant ID) | Tenant Admin |
| Risk rules | Hierarchical (global default + tenant override) | Each level |
| Compliance matrix | Global (SSOT here) | Platform + jurisdiction rules |

Cross-tenant queries are restricted to a whitelist: brand aggregate reports, global admin views, tenant migration. Every cross-tenant query is audited with actor, timestamp, scope, and data volume returned.

---

## 4. RBAC & Authorization

### 4.1 Permission Architecture

Permissions follow a `resource:action` pattern stored as string identifiers:

```java
class Permission {
    String code;        // "agents:create", "settlements:approve", "config:override"
    String resource;    // "agents"
    String action;      // "create"
    String description;
}
```

**Role templates** are defined globally and cloneable per tenant:

```java
class RoleTemplate {
    Long id;
    String name;              // "brand_admin", "tenant_finance", "agent_manager"
    TenantLevel applicableAt; // Which hierarchy level can use this template
    Set<String> permissions;  // Default permission set
    boolean isSystem;         // true = cannot delete, only extend
}

class TenantRole {
    Long id;
    Long tenantId;
    Long templateId;          // null if fully custom
    String name;
    Set<String> permissions;  // Tenant's customized permission set
}
```

Maximum 20 custom roles per tenant (configurable via platform settings).

### 4.2 Permission Check Flow

1. Request arrives with JWT containing `userId`, `tenantId`, `roles[]`
2. `PermissionCheckService.hasPermission(userId, tenantId, requiredPermission)`:
   a. Check Redis: `SISMEMBER perm:{tenantId}:{userId} "agents:create"` → O(1)
   b. Cache miss → load from DB, compute full permission set from all roles, store in Redis Set with 300s TTL
3. Return 403 if permission not found

**Cache invalidation**: When a role is modified or user's role assignment changes, publish `PermissionInvalidateEvent` to Redis Pub/Sub channel `permission:invalidate`. All application nodes subscribe and delete affected cache keys.

### 4.3 Impersonation

`ImpersonationService` manages admin impersonation sessions:

```java
class ImpersonationSession {
    Long sessionId;
    Long actorId;           // The admin performing impersonation
    Long targetTenantId;    // The tenant being impersonated
    Long targetUserId;      // Optional: specific user being impersonated
    Instant startTime;
    Instant endTime;        // null while active
    String reason;          // Mandatory field
}
```

During impersonation:
- A secondary JWT is issued with `impersonating=true`, `actorId`, and `targetId` claims
- **All audit events** during the session include both `actorId` and `impersonatorId`
- The frontend displays a non-dismissable banner: "You are impersonating [target] as [actor]"
- Impersonated actions **cannot exceed** the target's normal permissions (enforced server-side)

### 4.4 Maker-Checker Workflow

All sensitive operations go through a pending-change lifecycle:

```java
class PendingChange {
    Long id;
    String entityType;       // "TENANT", "AGENT_CREDIT", "COMMISSION_MODEL", "CONFIG_OVERRIDE"
    String entityId;
    JsonNode changeDiff;     // Before/after JSON diff
    Long makerId;
    Long checkerId;          // null until reviewed
    ChangeStatus status;     // PENDING, APPROVED, REJECTED, EXPIRED
    String makerComment;
    String checkerComment;
    Instant createdAt;
    Instant resolvedAt;
    Instant expiresAt;       // Auto-expire after 72h if not reviewed
}
```

**Operations requiring Maker-Checker**:
- Agent: credit adjustments, creation/deactivation, commission model changes, hierarchy changes
- Settlement: approval at all tiers
- Governance: tenant creation, billing model changes
- Configuration: ALL 3-layer config overrides

**State machine** (with atomic transitions):
```
PENDING → APPROVED (by checker with {entity}:approve at same/higher level)
PENDING → REJECTED (by checker)
PENDING → EXPIRED (by sweep job after 72h)
```

Transitions use `UPDATE pending_changes SET status = 'APPROVED', checker_id = ? WHERE id = ? AND status = 'PENDING'` — the `WHERE status = 'PENDING'` clause serves as an atomic guard preventing double-approval.

**Expiry sweep**: `MakerCheckerExpiryJob` runs every 15 minutes, marking stale PENDING changes as EXPIRED. Reminder notifications sent at 50% SLA elapsed.

**Checker eligibility**: The checker must be a different user from the maker, with `{entity}:approve` permission at the same or higher hierarchy level. Hierarchy comparison uses the `materializedPath` — the checker's path must be a prefix of (or equal to) the maker's path.

---

## 5. Authentication & Security (Keycloak Integration)

### 5.1 Keycloak Setup

A self-hosted Keycloak instance manages identity for all admin and agent users. Each brand maps to a Keycloak **realm** (for SSO isolation), while tenants within a brand share the realm but use **groups** for tenant scoping.

**SSO modes** are configured per brand:
| Mode | Keycloak Config |
|------|----------------|
| Fully isolated | Separate realm per tenant |
| Brand SSO | Shared realm, tenant groups |
| Federated | External IdP via Keycloak Identity Brokering (SAML/OIDC) |

**Invariant**: Regardless of SSO mode, wallets remain tenant-isolated.

### 5.2 MFA Enforcement

Keycloak's authentication flow is configured to **require** TOTP for roles: Super Admin, Brand Admin, Tenant Admin, all Agent accounts, finance, risk control, DBA, DevOps.

First login → forced TOTP setup → 10 one-time backup codes generated → cannot perform any operation until MFA is configured.

Device loss recovery: SMS/email OTP (24-48h SLA, requires upper-level manager approval).

### 5.3 Agent Session Security

| Control | Implementation |
|---------|---------------|
| IP whitelist (max 5) | Custom Keycloak SPI that checks source IP against `agent_ip_whitelist` table |
| Session timeout | Keycloak realm setting: 30min idle |
| Single session | Keycloak `max-sessions: 1` with "terminate oldest" policy |
| Lockout | Keycloak brute-force detection: 5→15min, 10→1hr+notify, 20→permanent |
| Re-authentication | Custom annotation `@RequireReAuth` on sensitive controller methods |

### 5.4 Sensitive Operation Re-Auth

Methods annotated with `@RequireReAuth` trigger a server-side check: the request must include a fresh TOTP code in the `X-MFA-Code` header. The `ReAuthInterceptor` validates the code against Keycloak's TOTP service. If missing or invalid, return 403 with `REAUTH_REQUIRED` error code.

Sensitive operations: credit adjustments, settlement approvals, agent creation/deactivation, commission model changes, hierarchy changes.

---

## 6. Agent Hierarchy & Credit Network

### 6.1 Agent Tree Model

```java
@Table("t_agent")
class AgentEntity {
    Long id;
    Long tenantId;
    Long parentAgentId;       // null for Master Agent (tenant-direct)
    String materializedPath;  // "/tenant_5/agent_1/agent_7/agent_15"
    Integer level;            // 1 = Master, 2-10 = intermediate/terminal
    AgentType type;           // MASTER, INTERMEDIATE, TERMINAL
    AgentStatus status;       // ACTIVE, SUSPENDED, FROZEN, CLOSED
    // Credit
    BigDecimal creditLimit;
    BigDecimal creditUsed;
    Long creditVersion;       // Optimistic lock
    // Commission
    Long commissionAgreementId;
    // Security
    String mfaSecret;
    Set<String> ipWhitelist;
}
```

**Hierarchy rules**:
- Max depth: 10 levels
- Credit allocation: top-down only (parent → child)
- Child credit limit ≤ parent's remaining allocatable credit
- New agent creation: requires parent agent or tenant admin approval (Maker-Checker)

### 6.2 Credit Management (Write-Ahead Pattern)

**Credit check on bet placement** (consumed by 02-funding via API):

1. `AgentCreditService.checkAndDeductCredit(agentId, amount)`:
   a. **Write-ahead**: Insert a `CreditTransactionLog` record in PostgreSQL (`status=PENDING`, `agent_id`, `amount`, `idempotency_key`) — this is the durable source of truth
   b. Read from Redis: `credit:{agentId}` → `{limit, used, available}`
   c. If `available >= amount`: deduct in Redis atomically (Lua script referencing the log entry ID)
   d. If insufficient: **reject immediately** (mark log entry as REJECTED, return INSUFFICIENT_CREDIT)
   e. Update PostgreSQL `t_agent.credit_used` with optimistic lock; mark log entry as COMMITTED
   f. Publish `CreditDeductedEvent` to update parent's view

2. On bet settlement (win/loss): reverse flow — credit released or consumed permanently

**Durability guarantee**: The PostgreSQL WAL write (step a) occurs BEFORE the Redis update. If Redis crashes after step c, the recovery process replays all PENDING log entries to rebuild Redis state from the durable log. No phantom credits.

**Recovery process** (`CreditRecoveryService`, runs on startup and on Redis reconnect):
1. Query `CreditTransactionLog WHERE status = PENDING AND created_at > now() - interval '1 hour'`
2. For each: verify DB state, update Redis to match, mark as COMMITTED or ROLLED_BACK
3. Rebuild full Redis credit cache from DB if divergence detected

**Redis Lua script for atomic deduction** ensures no race condition:
- Read `available`; if >= amount, decrement `available` and increment `used`; return success
- Otherwise return failure — no partial deduction

**Cache rebuild**: On cache miss or startup, rebuild from DB. On Redis failure, fall back to DB with optimistic locking (higher latency but correct). Redis persistence: AOF with `appendfsync everysec` as minimum for credit data.

### 6.3 Risk Thresholds

A background `CreditMonitorJob` (scheduled every 60 seconds) scans all active agents as a **defense-in-depth** measure. The real-time Lua script already rejects bets when `available < amount`, so the >100% threshold should only trigger from bugs, manual adjustments, or Redis recovery lag:
| Usage | Level | Action |
|-------|-------|--------|
| ≤80% | Normal | No action |
| 81-90% | Warning | `CreditWarningEvent` → notify agent via portal + email |
| 91-100% | High | `CreditHighRiskEvent` → block new bets, notify upper agent |
| >100% | Critical | `CreditCriticalEvent` → freeze account, trigger forced settlement |

Thresholds are configurable per brand/jurisdiction via the 3-layer config system.

### 6.4 Self-Exclusion Integration

When `03-player` broadcasts a `PlayerSelfExclusionEvent`:
1. `AgentCreditListener` receives the event
2. Looks up all credit relationships for the excluded player
3. Freezes the player's CREDIT wallet (sets `credit_status = FROZEN`)
4. Notifies the responsible agent via portal notification and email
5. Logs an audit event with exclusion reference

Before any credit operation (grant, adjust, unfreeze), `AgentCreditService` calls `03-player`'s self-exclusion check API. If excluded → reject operation.

---

## 7. Commission Engine

### 7.1 Strategy Pattern

The commission engine uses the Strategy pattern to support four models. Each agent has a `CommissionAgreement` that specifies which model(s) to use:

```java
class CommissionAgreement {
    Long id;
    Long agentId;
    List<CommissionModelConfig> activeModels; // 1 or more
    NegativeCarryPolicy carryPolicy;          // CARRY, RESET_MONTHLY, RESET_WEEKLY, CAP_AT_AMOUNT
    BigDecimal capAmount;                     // For CAP_AT_AMOUNT policy
    String settlementCycle;                   // WEEKLY, MONTHLY
    Instant effectiveFrom;
    Instant effectiveTo;                      // null = indefinite
}
```

**Model implementations**:

| Model | Strategy | Key Inputs | Output |
|-------|----------|------------|--------|
| `RevenueShareStrategy` | Net P&L × tiered rate | GGR, tier thresholds, share rates | Commission amount |
| `TurnoverRebateStrategy` | Valid bets × game-type rate | Bet amounts, game categories, rebate rates | Rebate amount |
| `CpaStrategy` | Count × fixed amount per FTD | New depositing player count, CPA rate | CPA payout |
| `HybridStrategy` | Delegates to RevShare + Turnover | Combined inputs | Sum of both |

For agents with **multiple concurrent models**: the engine runs each strategy independently and sums the results. The `CommissionAgreement.activeModels` list determines which strategies to execute.

### 7.2 Revenue Share Tiers (Default, DB-Configurable)

| Monthly Net P&L | Share Rate |
|-----------------|-----------|
| < $10,000 | 30% |
| $10,000 - $50,000 | 35% |
| $50,000 - $200,000 | 40% |
| > $200,000 | 45% |

### 7.3 Position Holding

When an agent declares a position:
- `PositionHoldingService.calculateRiskDistribution(betId, agentPath)` walks the agent chain
- Each agent's `risk_position_percentage` determines their share of win/loss
- If sub-agent positions sum > 100%, normalize proportionally
- Position records stored in `t_agent_position` with `(agent_id, bet_id, position_pct, potential_payout)`

### 7.4 Negative Carry-Forward

The commission ledger tracks running balances using double-entry bookkeeping:

```java
class CommissionLedgerEntry {
    Long id;
    Long agentId;
    String periodKey;         // "2026-W13" or "2026-03"
    EntryType type;           // EARNED, DEDUCTED, CARRY_FORWARD, RESET
    BigDecimal amount;
    BigDecimal runningBalance;
    String description;
    Instant createdAt;
}
```

At period end:
- If `runningBalance < 0`: carry forward to next period (or reset based on `carryPolicy`)
- If `runningBalance > 0`: eligible for settlement payout
- Annual reset option: requires CFO approval (Maker-Checker)

---

## 8. Settlement System

### 8.1 Settlement Workflow

```
[Cron trigger: Monday 00:00 UTC (weekly) or 1st 00:00 UTC (monthly)]
  ↓
SettlementOrchestrator.runSettlement(cycle)
  ↓
1. Freeze the period — no more commission entries for this period
  ↓
2. For each eligible agent:
   a. CommissionEngine.calculateTotal(agent, period)
   b. Apply negative carry-forward from previous period
   c. Generate SettlementRecord (amount, status=PENDING_REVIEW)
  ↓
3. Route to approval tier based on amount:
   - <$10K: auto-approve → APPROVED
   - $10K-$50K: Manager queue (4h SLA)
   - $50K-$100K: CFO queue (24h SLA)
   - ≥$100K: CFO + CEO queue (48h SLA)
  ↓
4. On approval: trigger payout via 02-funding payment API
  ↓
5. On payout success: status=COMPLETED, update ledger
  ↓
6. On payout failure: auto-retry up to 3x over 3 business days
   - After 3 failures: status=ESCALATED, manual finance queue, alert
```

### 8.2 Anomaly Detection

`SettlementMonitorService` runs post-calculation checks:

| Metric | Threshold | Action |
|--------|-----------|--------|
| Single agent monthly total | ≥$500K | Trigger upgrade review |
| Single agent weekly growth | >200% vs 4-week average | FLAG + manual review |
| New agent first-month settlement | ≥$50K | Auto FLAG |

All thresholds are configurable per brand/jurisdiction via the 3-layer config system.

### 8.3 Multi-Currency Settlement

Agent settlement currency is fixed per agreement. For agents with players in different currencies:
- Bet amounts converted to agent's settlement currency at the bet-time exchange rate
- Settlement calculated in agent's currency
- FX risk between bet-date and settlement-date is borne by the agent (per Ch2 §2.13 FX SSOT)
- Exchange rates sourced from 02-funding's FX rate API

---

## 9. Compliance Matrix (SSOT)

### 9.1 Data Model

```java
class ComplianceRule {
    Long id;
    Jurisdiction jurisdiction;    // MGA, UKGC, CURACAO, PAGCOR
    String category;              // "KYC", "AML", "RESPONSIBLE_GAMBLING", "DATA_RETENTION"
    String ruleKey;               // "max_wagering_multiplier"
    String ruleValue;             // "10" (for UKGC), "35" (for MGA)
    String description;
    Instant effectiveFrom;
    Instant effectiveTo;          // null = currently effective
    boolean isStrictest;          // Pre-computed flag for quick lookups
}
```

### 9.2 Rule Resolution

`ComplianceService.getEffectiveRule(jurisdiction, category, ruleKey)`:
1. Query `t_compliance_rule` for the jurisdiction + category + key combination
2. Filter by `effectiveFrom <= now < effectiveTo`
3. Return the rule value

For **cross-jurisdiction** scenarios (player in jurisdiction A, on brand licensed in jurisdiction B):
- Apply the **strictest rule** between both jurisdictions
- Log the conflict resolution in audit trail

### 9.3 Key 2026 Deadlines to Track

| Deadline | Jurisdiction | Impact |
|----------|-------------|--------|
| Jan 19, 2026 | UKGC | Ban mixed-product incentives; wagering cap 10x |
| Mar 19, 2026 | UKGC | Shareholder reporting threshold 3% → 5% |
| Mar 31, 2026 | PAGCOR | All B2B providers must be accredited |
| Apr 6, 2026 | UKGC | Digital Markets Act alignment |
| Jun 30, 2026 | UKGC | RTS 12 financial limit changes |

These rules must be seeded in the compliance matrix with appropriate effective dates.

---

## 10. Feature Flag System

### 10.1 Design

A lightweight, tenant-aware feature flag system:

```java
class FeatureFlag {
    String flagKey;           // "game_integration_v2", "sports_betting", "new_cashier"
    Long tenantId;            // null = platform-level default
    boolean enabled;
    Integer rolloutPercentage; // 0-100 for gradual rollout
    JsonNode metadata;        // Additional context
}
```

**Resolution order**: Tenant override → Brand default → Platform default

### 10.2 API

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/flags/{key}` | Check flag status for current tenant |
| `GET /api/v1/flags` | List all flags for current tenant |
| `PUT /api/v1/flags/{key}` | Update flag (Maker-Checker required) |

**Caching**: Flags are cached in Redis with 60s TTL. Invalidated via Pub/Sub on change.

Other business domain splits consume the flag API to progressively enable features per tenant.

---

## 11. Immutable Audit System

### 11.1 Event Creation

Every auditable action passes through `AuditService.log(AuditEvent)`:

```java
class AuditEvent {
    UUID eventId;
    String prevHash;
    String eventHash;         // SHA-256(prevHash + canonical(eventData))
    Instant timestamp;        // NTP-synchronized
    UUID actorId;
    UUID impersonatorId;      // null if not impersonating
    UUID tenantId;
    String action;            // "AGENT_CREDIT_ADJUST", "SETTLEMENT_APPROVE", etc.
    String resourceType;      // "Agent", "Tenant", "Commission"
    String resourceId;
    JsonNode changePayload;   // Before/after diff
}
```

**Per-tenant hash chains**: Each tenant has its own independent hash chain (partitioned by `tenant_id`). This eliminates the cross-node ordering bottleneck — multiple nodes can write audit events for different tenants concurrently without coordination.

**Event sequencing**: A Kafka topic `audit-events` partitioned by `tenant_id` ensures strict per-tenant ordering. The `AuditChainWriter` consumer reads from each partition and computes `eventHash = SHA-256(prevHash + canonical(eventData))`, writing the finalized event with hash to PostgreSQL.

A Spring AOP aspect `@Audited` can be placed on service methods to automatically capture before/after state and publish to the Kafka audit topic.

### 11.2 Storage Tiers

| Tier | Duration | Storage | Access Pattern |
|------|----------|---------|----------------|
| **Hot** | 0-90 days | PostgreSQL `t_audit_event` (append-only, triggers block UPDATE/DELETE) | Real-time query, dashboard |
| **Warm** | 90 days - 1 year | S3-compatible object storage (Parquet format) | Minutes; batch query via Athena/Trino |
| **Cold** | 1-7 years | Archive storage (S3 Glacier or equivalent) | 24h retrieval; regulatory on-demand |

### 11.3 Integrity Verification

A `ChainVerificationJob` runs daily:
1. Select latest 1000 hot-tier events
2. Re-compute each `eventHash` from `prevHash` + event data
3. Verify chain is unbroken (no gaps, no tampering)
4. If any mismatch: raise P0 alert, mark affected range for investigation
5. Monthly: verify boundary between hot and warm tiers matches

### 11.4 Archival Pipeline

`AuditArchivalJob` runs weekly:
1. Select hot-tier events older than 90 days
2. Export to Parquet format, partition by `tenant_id` and `month`
3. Upload to warm storage with server-side encryption
4. Verify upload integrity (row count + hash comparison)
5. Delete from hot tier only after verified upload
6. Events older than 1 year in warm storage: move to cold archive (same verification)

---

## 12. White-Label & Tenant Lifecycle

### 12.1 Onboarding Flow

`TenantLifecycleManager.provisionTenant(request)` orchestrates:

1. **Validate**: Brand exists, creator has permission, billing info present
2. **Create Keycloak realm/group** (depending on SSO mode)
3. **Create tenant record** with materialized path
4. **If dedicated tier**: provision new database, apply migrations, configure routing
5. **Apply white-label config**: domain, SSL cert (via Let's Encrypt or provided), branding assets
6. **Seed default data**: roles from templates, compliance rules from jurisdiction, feature flags from brand defaults
7. **Create initial admin user** with forced MFA setup
8. **Run verification checklist**: data isolation test, permission test, config override test

Target: **<24 hours** from creation to operational (most automated; manual steps only for SSL and regulatory notification if needed).

### 12.2 Billing Engine

```java
class TenantBillingConfig {
    Long tenantId;
    BillingModel model;        // FIXED, REVENUE_SHARE, HYBRID
    BigDecimal fixedAmount;    // For FIXED / HYBRID
    BigDecimal shareRate;      // For REVENUE_SHARE / HYBRID (2-5%)
    BillingCycle cycle;        // MONTHLY
    Integer gracePeriodDays;   // Default 7
    boolean autoRenew;
}
```

**Overdue escalation**: Managed by `BillingEnforcementJob` (daily):
- 0-7d: email warning (daily)
- 8-14d: set tenant status to `HIGH_RISK`
- 15-30d: disable new player registration
- 31d+: suspend all tenant operations (existing players can withdraw but not bet)

### 12.3 Tenant Migration

`TenantMigrationService` handles both tier migration (shared↔dedicated) and cross-brand transfer:

**Pre-migration checklist** (automated validation):
- [ ] No pending withdrawals
- [ ] No active settlement cycles
- [ ] Player data integrity verified (row count + checksum)
- [ ] Transaction history complete
- [ ] Wallet balances reconciled
- [ ] Audit log archived for source context

**GDPR data portability**: `DataPortabilityService.exportTenantData(tenantId)` produces a ZIP archive:
```
export_tenant_12_20260326/
  ├── manifest.json           # Schema descriptions
  ├── players.json            # Player registration data
  ├── transactions.json       # Transaction history
  ├── game_history.json       # Game records
  ├── communications.json     # Messages and notifications
  └── marketing_consents.json # GDPR consent records
```

Response time: within 1 month (GDPR Art 20). Format: JSON with schema manifest.

---

## 13. Cross-Domain API Contracts

### 13.1 APIs This Split Exposes

| Endpoint | Consumer | Purpose |
|----------|----------|---------|
| `GET /api/v1/tenants/{id}/context` | All domains | Tenant config, white-label, currency, language |
| `POST /api/v1/rbac/check` | All domains | Permission check: `{userId, tenantId, permission}` → boolean |
| `GET /api/v1/compliance/rules` | 03-player, 05-risk, 06-frontend | Compliance matrix query by jurisdiction + category |
| `GET /api/v1/agents/{id}/credit` | 02-funding | Agent credit: `{limit, used, available}` |
| `POST /api/v1/agents/credit/deduct` | 02-funding | Deduct credit for bet placement |
| `POST /api/v1/agents/credit/release` | 02-funding | Release credit on bet settlement |
| `GET /api/v1/agents/player/{playerId}` | 02-funding, 04-gaming | Agent-player relationship lookup |
| `GET /api/v1/flags/{key}` | All domains | Feature flag status |

### 13.2 APIs This Split Consumes

| Endpoint | Provider | Purpose |
|----------|----------|---------|
| `GET /api/v1/players/{id}/self-exclusion` | 03-player | Check exclusion status before credit ops |
| `EVENT: PlayerSelfExclusionEvent` | 03-player | Freeze credit on exclusion |
| `GET /api/v1/fx/rate/{from}/{to}` | 02-funding | Exchange rate for multi-currency settlement |
| `GET /api/v1/risk/score/{entityId}` | 05-risk | Risk score for agent risk assessment |
| `POST /api/v1/payments/payout` | 02-funding | Settlement payout execution |

### 13.3 Event Bus

| Event | Publisher | Subscribers | Trigger |
|-------|-----------|-------------|---------|
| `TenantCreatedEvent` | Governance | All domains | New tenant provisioned |
| `TenantSuspendedEvent` | Governance | All domains | Tenant suspended (billing/compliance) |
| `CreditDeductedEvent` | Governance | 02-funding | Agent credit used for bet |
| `CreditReleasedEvent` | Governance | 02-funding | Credit returned after settlement |
| `SettlementCompletedEvent` | Governance | 02-funding | Commission payout triggered |
| `ComplianceRuleChangedEvent` | Governance | All domains | Jurisdiction rule updated |
| `FeatureFlagChangedEvent` | Governance | All domains | Flag toggled |
| `PlayerSelfExclusionEvent` | 03-player | Governance | Player excluded → freeze credit |

---

## 14. Database Schema Overview

### 14.1 Core Tables

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `t_tenant` | Tenant hierarchy | id, parent_id, materialized_path, level, status, config_json |
| `t_tenant_billing` | Billing configuration | tenant_id, model, fixed_amount, share_rate, grace_days |
| `t_role_template` | Global role templates | id, name, applicable_level, permissions_json, is_system |
| `t_tenant_role` | Tenant-customized roles | id, tenant_id, template_id, name, permissions_json |
| `t_user_role_mapping` | User-role assignments | user_id, tenant_id, role_id |
| `t_agent` | Agent hierarchy + credit | id, tenant_id, parent_id, path, level, credit_limit, credit_used, credit_version |
| `t_agent_ip_whitelist` | IP restrictions | agent_id, ip_address, approved_by, approved_at |
| `t_commission_agreement` | Commission config per agent | id, agent_id, models_json, carry_policy, settlement_cycle |
| `t_commission_ledger` | Double-entry commission log | id, agent_id, period_key, type, amount, running_balance |
| `t_agent_position` | Position holding records | id, agent_id, bet_id, position_pct, potential_payout |
| `t_settlement_record` | Settlement lifecycle | id, agent_id, period, amount, status, retry_count, approved_by |
| `t_pending_change` | Maker-Checker queue | id, entity_type, entity_id, diff_json, maker_id, checker_id, status |
| `t_compliance_rule` | Jurisdiction rules matrix | id, jurisdiction, category, rule_key, rule_value, effective_from/to |
| `t_feature_flag` | Feature toggles | flag_key, tenant_id, enabled, rollout_pct |
| `t_audit_event` | Immutable audit log | event_id, prev_hash, event_hash, timestamp, actor_id, tenant_id, action, payload |
| `t_impersonation_session` | Impersonation tracking | session_id, actor_id, target_tenant_id, start_time, end_time, reason |

### 14.2 RLS Policy

Every tenant-scoped table has:
```sql
ALTER TABLE t_agent ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON t_agent
  USING (tenant_id = current_setting('rls.tenant_id')::bigint);
```

**RLS Hardening** (per Opus review):
- `ALTER TABLE ... FORCE ROW LEVEL SECURITY` on all tenant-scoped tables (prevents even table owners from bypassing)
- Application connects as a **non-superuser** role (`app_user`, not `postgres`)
- `rls.tenant_id` is set at **connection pool level** (HikariCP `connectionInitSql` on checkout), not only in MyBatis — this protects native JDBC, JPA, and third-party library queries
- On connection return to pool: reset `rls.tenant_id` to `-1` (invalid) to prevent stale context leakage
- The `TenantRlsInterceptor` (MyBatis) provides a secondary layer for explicit tenant context verification

---

## 15. Error Handling & Resilience

### 15.1 Credit Operation Failures

| Failure | Response | Recovery |
|---------|----------|----------|
| Redis unavailable | Fall back to DB with optimistic lock | Auto-reconnect; rebuild cache |
| DB transaction conflict | Retry with backoff (max 3x) | If still failing, return 503 |
| Credit deduction > available | Return INSUFFICIENT_CREDIT immediately | No retry |
| Parent credit exhausted | Block child operations | Notify parent agent |

### 15.2 Settlement Failures

| Failure | Response | Recovery |
|---------|----------|----------|
| Calculation error | Halt settlement for that agent | Manual investigation |
| Payment API failure | Retry after 1 business day (max 3x) | After 3: escalate to manual queue |
| Approval timeout | Send reminder at 50% SLA elapsed | After SLA: auto-escalate to next tier |

### 15.3 Keycloak Failures

| Failure | Response | Recovery |
|---------|----------|----------|
| Keycloak down | Return 503 for login; existing sessions continue (JWT validation is local) | Auto-reconnect |
| Token validation failure | Reject request (401) | User must re-login |
| MFA service failure | Block sensitive operations; non-sensitive allowed | Keycloak restart |

---

## 16. Monitoring & Observability

### 16.1 Key Metrics

| Metric | Target | Alert Threshold |
|--------|--------|----------------|
| Permission check p99 | <10ms | >20ms |
| Credit deduction p99 | <50ms | >100ms |
| RLS overhead | <5% | >8% |
| Cross-tenant data queries | 0 unauthorized | Any unauthorized = P0 |
| Audit chain integrity | 100% | Any break = P0 |
| Settlement on-time rate | >95% | <90% |
| Maker-Checker approval SLA | Per tier | >50% SLA elapsed with no action |
| Agent portal uptime | 99.5% | <99% |

### 16.2 Health Checks

| Check | Frequency | Method |
|-------|-----------|--------|
| RLS policy active | Every 5 min | Test query without tenant context → must return 0 rows |
| Redis permission cache | Every 1 min | Sentinel/Cluster health |
| Audit chain head | Every 10 min | Verify last N events' hash chain |
| Keycloak connectivity | Every 30 sec | Token introspection endpoint |
| Credit cache consistency | Every 5 min | Compare Redis sum vs DB sum (sample) |

---

## 17. Hierarchy Change Locking

When an agent hierarchy change is requested (FR-AGT-11):

1. `HierarchyChangeService` acquires a PostgreSQL advisory lock on the subtree root's `agent_id`
2. Verify no active settlement cycle exists for any agent in the subtree (`SELECT COUNT(*) FROM t_settlement_record WHERE status IN ('PENDING_REVIEW', 'APPROVED') AND agent_id IN (subtree)`)
3. If settlement is active: reject the change, return `SETTLEMENT_IN_PROGRESS`
4. Execute materialized path update within a single transaction
5. Update all descendants' paths atomically
6. Invalidate Redis caches for affected agents
7. Release advisory lock

**Closure table**: A secondary `t_agent_closure` table `(ancestor_id, descendant_id, depth)` provides an alternative subtree query path that doesn't rely on string LIKE patterns. Maintained via triggers on `t_agent` insert/update/delete.

---

## 18. Settlement Batch & Idempotency

### 18.1 Settlement Batch Entity

```java
class SettlementBatch {
    Long id;
    String periodKey;        // "2026-W13"
    String cycle;            // WEEKLY, MONTHLY
    BatchStatus status;      // RUNNING, COMPLETED, FAILED_PARTIAL
    Integer totalAgents;
    Integer processedAgents;
    Integer failedAgents;
    String inputChecksum;    // SHA-256 of input data snapshot
    Instant startedAt;
    Instant completedAt;
}
```

### 18.2 Idempotent Calculation

`SettlementOrchestrator` processes each agent idempotently:
1. Check if `SettlementRecord` already exists for `(agent_id, period_key)` with status != FAILED
2. If exists and status is COMPLETED: skip (already processed)
3. If exists and status is PENDING_REVIEW or APPROVED: skip (in progress)
4. Otherwise: calculate and create record

On crash recovery: restart the batch, skip already-processed agents, continue with remaining.

---

## 19. API Versioning & Stability

### 19.1 Versioning Strategy

As the **foundation split** consumed by all 6 other domains:

| Rule | Policy |
|------|--------|
| URL prefix | `/api/v{N}/...` (v1 initially) |
| Breaking changes | Require new version (v2) |
| Co-existence | v1 and v2 run simultaneously for min 90 days |
| Deprecation notice | 30 days before removal of old version |
| Contract tests | Spring Cloud Contract required for all exposed APIs before merge |

### 19.2 Inter-Service Communication

| Pattern | Implementation |
|---------|---------------|
| Authentication | Service mesh mTLS or API keys in `X-Service-Key` header |
| Circuit breaker | Resilience4j on all outbound calls (to 02-funding, 03-player, 05-risk) |
| Rate limiting | Per-consumer limits enforced at service mesh or gateway level |
| Timeout | 5s default for synchronous calls; configurable per endpoint |

---

## 20. Database Migration Strategy

### 20.1 Tool & Process

| Item | Choice |
|------|--------|
| Migration tool | **Flyway** (version-numbered SQL migrations) |
| Shared DB | Migrations applied via CI/CD pipeline on deploy |
| Dedicated DB | Same migration set applied on tenant provisioning and on each deploy |
| RLS checklist | **Every migration creating a new table MUST include**: (1) `tenant_id` column, (2) RLS policy, (3) `FORCE ROW LEVEL SECURITY` |

### 20.2 Migration Validation

A CI step verifies all tenant-scoped tables have RLS policies:
```sql
-- Check: no tenant-scoped table without RLS
SELECT tablename FROM pg_tables
WHERE schemaname = 'public'
AND tablename LIKE 't_%'
AND tablename NOT IN (SELECT tablename FROM pg_policies);
-- Must return 0 rows
```

---

## 21. Keycloak High Availability

### 21.1 Topology

| Component | Configuration |
|-----------|--------------|
| Keycloak nodes | Minimum 2, active-active behind load balancer |
| Session cache | Infinispan clustered (embedded, JDBC persistence fallback) |
| Database | PostgreSQL with streaming replication (same as or separate from app DB) |
| Health check | `/health/ready` endpoint, 30s interval |

### 21.2 Degraded Mode

During Keycloak outage:
| Operation | Behavior |
|-----------|----------|
| Existing sessions | Continue (JWT validated locally via public key cache) |
| New logins | Blocked (503 with retry-after) |
| MFA re-auth | Blocked for sensitive ops; non-sensitive allowed |
| Impersonation | Blocked (requires new JWT issuance) |
| Tenant provisioning | Queued, auto-retry on Keycloak recovery |

---

## 22. Disaster Recovery

### 22.1 Backup Strategy

| Component | Frequency | Retention | Tool |
|-----------|-----------|-----------|------|
| PostgreSQL (shared) | Continuous WAL archival + daily base backup | 30 days | pg_basebackup + WAL-G |
| PostgreSQL (dedicated) | Same as shared, per-database | 30 days | pg_basebackup + WAL-G |
| Redis | AOF (appendfsync everysec) + hourly RDB snapshot | 7 days | Redis native |
| Keycloak DB | Same as PostgreSQL | 30 days | pg_basebackup |

### 22.2 Recovery Procedures

| Scenario | Procedure | RTO |
|----------|-----------|-----|
| Redis failure | Auto-reconnect; `CreditRecoveryService` replays WAL entries to rebuild cache | <5 min |
| Audit chain break | P0 alert; isolate affected tenant chain; investigate gap; manual chain re-link with documented anomaly record | <4 hr |
| Credit DB/Redis divergence | `CreditReconciliationJob` detects; freeze affected agents; reconcile from WAL log; resume | <30 min |
| Full PostgreSQL failure | Restore from WAL archive to point-in-time; verify RLS policies; run data integrity checks | <15 min (Tier 1) |
| Keycloak failure | Failover to standby node; if both down: existing sessions continue, new logins queued | <2 min (failover) |
