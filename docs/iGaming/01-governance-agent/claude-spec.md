# Combined Specification — 01-governance-agent (Governance & Agent Domain)

**Version**: 1.0
**Date**: 2026-03-26
**Status**: Ready for implementation planning

---

## 1. Domain Overview

The Governance & Agent domain is the **foundation split** of the iGaming platform. All other 6 business domains depend on it for tenant isolation, RBAC authorization, and the agent credit network. It encompasses:

- **Governance** (Ch7): Multi-tenant hierarchy, RBAC, data isolation, white-label, billing, compliance
- **Agent Operations** (Ch8): Agent hierarchy, credit management, commission engine, settlement

### Scale Target
- **10+ brands**, **50+ tenants**, **1000+ agents** across multiple jurisdictions (MGA, UKGC, Curacao, PAGCOR)

### Technology Stack
- **Spring Boot 3.x** + **Java 21** + **PostgreSQL** (SmartAdmin V3.0 four-layer architecture)
- **Keycloak** self-hosted for identity/SSO (SAML/OIDC)
- **Redis** for permission caching and agent credit cache

---

## 2. Multi-Tenant Architecture

### 2.1 Four-Layer Hierarchy

```
Platform (Super Admin)
  └── Brand (Brand Admin) — 集團層級
        └── Tenant (Tenant Admin) — 獨立站點
              └── Agent — 代理 (up to 10 levels deep)
```

### 2.2 Data Isolation Strategy: Hybrid RLS + Dedicated DB

**Decision (Q1)**: Hybrid approach combining PostgreSQL Row-Level Security for shared tenants with dedicated databases for premium/regulated brands.

| Tier | Isolation | Use Case |
|------|-----------|----------|
| **Shared** | PostgreSQL RLS (`SET rls.tenant_id` session variable) | Small tenants, agents, standard brands |
| **Dedicated** | Separate database per brand | UKGC-licensed brands, large operators, regulatory requirements |

- RLS policies use simple predicates on indexed `tenant_id` columns — target **<5% overhead**
- Materialized path column (`/platform/brand_x/tenant_y/agent_z`) for fast subtree lookups
- Tenant context resolved at API gateway via JWT claims → `ThreadLocal` in Spring Boot
- **Zero cross-tenant data leakage** — verified by mandatory isolation tests

### 2.3 Configuration Override (3-Layer)

```
Resolution: Jurisdiction → Brand → Global
Compliance: Strictest Rule Applies
Changes: Maker-Checker + Audit Trail + ≤5s Hot-Reload (Redis Pub/Sub)
```

All thresholds (billing, approval, credit, commission) follow this pattern. No hard-coding.

---

## 3. RBAC & Authorization

### 3.1 Permission Model

**Hybrid Role Templates** with `resource:action` permissions:
- Global base templates cloneable by each hierarchy level
- Custom roles allowed (max 20 per tenant)
- Permission bundles for UX simplicity (e.g., `billing:manage` bundles view/create/refund)

**Three-question authorization check** per request:
1. Does the resource belong to the accessed tenant?
2. Is the user a member of that tenant?
3. Does the user's role grant this action on this resource type?

### 3.2 Permission Matrix

| Operation | Super Admin | Brand Admin | Tenant Admin | Agent |
|-----------|:-----------:|:-----------:|:------------:|:-----:|
| Create brand | YES | -- | -- | -- |
| Create tenant | YES | YES | -- | -- |
| Global game switch | YES | -- | -- | -- |
| Brand budget adjust | YES | YES | -- | -- |
| Player management | YES | YES | YES | -- |
| Commission reports | YES | YES | YES | YES |
| Risk rule config | YES | YES (brand) | YES (tenant) | -- |

### 3.3 Permission Caching

- **Redis Sets** with key pattern `perm:{tenantId}:{userId}` — O(1) via `SISMEMBER`
- TTL: 60-300 seconds
- Invalidation: Redis Pub/Sub on `permission:invalidate` channel
- Target: **<10ms p99** for permission checks

### 3.4 Impersonation

| Rule | Detail |
|------|--------|
| Super Admin | Can impersonate any Brand / Tenant / Agent |
| Brand Admin | Can impersonate tenants/agents under their brand |
| Visual indicator | Full-screen banner during impersonation |
| Audit | Every action logs `actor_id + impersonated_id + action + timestamp` |
| Scope limit | Cannot exceed impersonated role's permissions |
| Keycloak integration | Uses Keycloak's FGAP V2 impersonation scope |

### 3.5 Maker-Checker (Broadest Scope)

**Decision (Q5)**: All sensitive operations require dual approval.

| Category | Operations |
|----------|-----------|
| Agent operations | Credit adjustments, agent creation/deactivation, commission model changes, hierarchy changes |
| Settlement | Settlement approval (per tier) |
| Governance | Tenant creation, billing model changes |
| Configuration | ALL 3-layer config overrides (jurisdiction/brand/global) |

Implementation: `pending_changes` table with `(entity_type, entity_id, change_diff_json, maker_id, checker_id, status, timestamps)`

---

## 4. Authentication (Keycloak)

**Decision (Q11)**: Keycloak self-hosted for compliance and data sovereignty.

| Feature | Implementation |
|---------|---------------|
| SSO modes | Isolated / Brand SSO / Federated (SAML/OIDC) |
| MFA | Mandatory TOTP for all admin + agent accounts; backup codes (10); SMS/email recovery |
| Wallet isolation | Regardless of SSO mode, wallets always per-tenant |
| Agent IP whitelist | Max 5 IPs per account; changes require upper-level approval |
| Session | 30min idle timeout; 1 active session per account; new login kicks old |
| Lockout | 5 fails → 15min lock; 10 fails → 1hr + notify upper; 20 fails → permanent lock |
| Re-auth | MFA re-required for: credit adjust, settlement approve, agent create/disable, commission change |

---

## 5. Agent Hierarchy & Credit Network

### 5.1 Hierarchy Structure
- **Max 10 levels**: Master Agent → intermediate agents → end agents
- Tree modeled with `agent_id, parent_agent_id, level, materialized_path`
- New agent creation requires upper-level or tenant admin approval

### 5.2 Dual Wallet Model
| Wallet | Purpose | Withdrawal |
|--------|---------|-----------|
| CASH | Player's own funds | Per KYC limits |
| CREDIT | Agent-granted credit | Not withdrawable; betting only |

### 5.3 Credit Management

| Item | Rule |
|------|------|
| Initial credit | Agent-set (default 0) |
| Adjust | Up/down; cannot go below used amount |
| Upper limit | Constrained by parent's allocation |
| Self-exclusion | Check before any credit operation (→ Ch15 SSOT) |
| Exhaustion | **Reject bet immediately** (Decision Q3) |

**Risk Thresholds**:
| Usage | Level | Action |
|-------|-------|--------|
| ≤80% | Normal | — |
| 81-90% | Warning | Notify agent |
| 91-100% | High | Block new bets, notify upper |
| >100% | Critical | Freeze account, force settlement |

### 5.4 Credit Performance (Redis Cache)

**Decision (Q10)**: Pre-computed credit summaries in Redis, event-driven sync.

- Key: `credit:{agentId}` → `{limit, used, available, lastUpdated}`
- Events: credit adjustment, bet placed, bet settled, round completed
- O(1) lookup per bet — no recursive tree walk at query time
- Fallback to DB on cache miss; rebuild cache from event log

### 5.5 Concurrency Control
- Optimistic locking (`version` field) for credit adjustments
- High concurrency: serialize credit deductions per player (consistent with Ch2 §2.7 scenario B)
- Momentary over-limit: reject subsequent bets, don't rollback already-succeeded bets

---

## 6. Commission Engine

### 6.1 Four Models

**Decision (Q2)**: Configurable per agent agreement — single model OR multiple concurrent models.

| Model | Calculation | Use Case |
|-------|-------------|----------|
| Revenue Share | Net P&L × tier rate (30-45%) | Most common |
| Turnover Rebate | Valid bets × game-type rate (0.2-0.5%) | High-volume agents |
| CPA | Fixed amount per FTD ($50-250) | Acquisition campaigns |
| Hybrid | RevShare + Turnover combined | Large agents |

### 6.2 Position Holding
- Agent declares risk percentage on bets
- Player loses → agent gets declared % of profit
- Player wins → agent bears declared % of loss
- Sub-agent positions normalized to ≤100% total

### 6.3 Settlement

| Cycle | Schedule | Settlement Day |
|-------|----------|---------------|
| Weekly | Standard | Every Monday |
| Monthly | Large/special | 1st of month |

**Flow**: System calculate → Finance review → Manager approve (large) → Auto-payout

**Failure handling (Q6)**: Auto-retry up to 3 times over 3 business days; if still failing, escalate to manual queue with alert.

**Approval tiers**:
| Amount | Approver | SLA |
|--------|----------|-----|
| <$10K | Auto | Instant |
| $10K-$50K | Manager | 4h |
| $50K-$100K | CFO | 24h |
| ≥$100K | CFO + CEO | 48h |

### 6.4 Negative Carry-Forward
- Losses carry to next period by default
- Reset threshold: -$1M (requires approval)
- Annual reset: optional with approval
- Configurable policy per agent: `CARRY`, `RESET_MONTHLY`, `RESET_WEEKLY`, `CAP_AT_AMOUNT`
- FX risk: agent bears exchange rate fluctuation between bet date and settlement date

---

## 7. Tenant Governance

### 7.1 White-Label Customization
| Item | Required |
|------|----------|
| Domain (FQDN + SSL) | Yes |
| Visual identity (logo, colors, footer) | Yes |
| Email templates (branded, own sender) | Yes |
| SMS Sender ID | Recommended |
| Currency settings | Yes |
| Language support | Recommended |

### 7.2 Billing
Three modes: Fixed monthly / Revenue share (2-5%) / Hybrid (base + share)

**Overdue escalation**: 0-7d email → 8-14d high-risk flag → 15-30d registration freeze → 31d+ full suspension

### 7.3 Tenant Migration + GDPR
- Migration checklist: player data, transactions, wallet balances, pending withdrawals, audit logs, regulatory notices
- GDPR Art 20: JSON/CSV export with schema manifest, 1-month response time
- EU Data Act readiness: interoperable formats, no switching fees

---

## 8. Multi-Jurisdiction Compliance Matrix (SSOT)

This split is the **authoritative owner** of the compliance matrix.

| Jurisdiction | License | Key Requirements |
|-------------|---------|------------------|
| MGA (Malta) | Class 1-4 | 5yr retention, EU AML 5, player fund isolation |
| UKGC (UK) | Remote Operating | Strictest: GAMSTOP, affordability, 10x wagering cap (Jan 2026) |
| Curacao (LOK) | Under new LOK framework | Crypto-friendly, Tier-IV servers, transitioning from sublicense model |
| PAGCOR (PH) | POGO | Local servers, AMLC oversight, B2B accreditation by Mar 2026 |

**Conflict resolution**: Strictest Rule Applies + geo-fencing per player jurisdiction.

---

## 9. Feature Flags (Tenant-Level)

**Decision (Q7)**: Implement basic tenant-level feature flag system in this split.

- Flag schema: `(flag_key, tenant_id, enabled, rollout_percentage, metadata_json)`
- API: `GET /api/v1/flags/{flagKey}?tenantId={id}` → boolean
- Resolution order: tenant override → brand default → platform default
- Other splits consume this API for progressive feature rollout
- Maker-Checker required for flag changes

---

## 10. Immutable Audit System

**Decision (Q8)**: Full audit lifecycle owned by this split.

### 10.1 Event Schema
```
audit_events (
  event_id        UUID PK,
  prev_hash       VARCHAR(64),
  event_hash      VARCHAR(64),  -- SHA-256(prev_hash + event_data)
  timestamp       TIMESTAMP,
  actor_id        UUID,
  impersonator_id UUID NULL,
  tenant_id       UUID,
  action          VARCHAR(100),
  resource_type   VARCHAR(50),
  resource_id     VARCHAR(100),
  change_payload  JSONB
)
```

### 10.2 Tiered Storage
| Tier | Duration | Storage | Access |
|------|----------|---------|--------|
| Hot | 0-90 days | PostgreSQL | Real-time query |
| Warm | 90 days - 1 year | Object storage (S3-compatible) | Minutes |
| Cold | 1-7 years | Archive storage | 24h (regulatory on-demand) |

### 10.3 Integrity
- Append-only table (triggers prevent UPDATE/DELETE)
- Hash chain: each event links to previous via SHA-256
- Background integrity verification job (daily)
- Retention: 5 years (MGA/UKGC minimum), 7 years (financial records)

---

## 11. Cross-Domain Contracts

### This Split Provides
| Contract | Consumer | Description |
|----------|----------|-------------|
| `tenant_id` isolation | All domains | Every entity carries tenant_id |
| RBAC permission API | All domains | Unified permission check endpoint |
| Tenant config API | All domains | White-label settings, currency, language |
| Compliance matrix | 03-player, 05-risk, 06-frontend | Jurisdiction-specific rules |
| Agent-player lookup | 02-funding, 04-gaming | Player-agent relationship |
| Agent credit query | 02-funding | CREDIT wallet available balance |
| Feature flag API | All domains | Tenant-level feature toggles |

### This Split Consumes
| Contract | Provider | Purpose |
|----------|----------|---------|
| Self-exclusion check | 03-player (Ch15) | Before credit operations |
| Self-exclusion broadcast | 03-player (Ch15) | Freeze CREDIT on exclusion |
| FX rate service | 02-funding (Ch2) | Multi-currency settlement |
| Risk score query | 05-risk (Ch6) | Agent risk assessment |

---

## 12. Non-Functional Requirements

| Category | Metric | Target |
|----------|--------|--------|
| Performance | Multi-tenant overhead | <5% |
| Performance | Permission check latency | <10ms p99 |
| Performance | Credit deduction latency | <50ms p99 |
| Performance | New tenant onboarding | <24 hours |
| Security | MFA coverage (forced roles) | 100% |
| Security | Data isolation compliance | 100% |
| Security | Audit completeness | 100% |
| Availability | Agent portal uptime | ≥99.5% |
| Accuracy | Settlement accuracy | 100% |
| Accuracy | Credit zero-over-limit | 100% |
| Business | Commission dispute rate | <1% |
| Business | Settlement on-time rate | >95% |

---

## 13. Testing Strategy (New Project)

| Layer | Framework | Scope |
|-------|-----------|-------|
| Unit | JUnit 5 + Mockito | Commission calculation, permission logic, credit operations |
| Integration | Spring Boot Test + Testcontainers | PostgreSQL RLS, Keycloak integration, Redis cache |
| Architecture | ArchUnit | Layer dependencies, SmartAdmin V3.0 compliance |
| API | REST Assured / MockMvc | All REST endpoints |
| Performance | JMH + Gatling | Permission cache, RLS overhead, credit throughput |
| Contract | Spring Cloud Contract | Cross-domain API contracts |

### Critical Test Categories
1. Cross-tenant data leak tests (mandatory for every repository method)
2. RLS policy bypass tests
3. Hierarchy boundary tests
4. Audit chain integrity tests
5. Commission calculation accuracy tests
6. Credit concurrency stress tests
