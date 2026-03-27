# Research Findings — 01-governance-agent

**Date**: 2026-03-26
**Scope**: Multi-tenant governance + agent management for iGaming platform

---

## Topic 1: Multi-Tenant SaaS Architecture Patterns in iGaming (2025-2026)

### Row-Level Security vs Schema-Per-Tenant vs Database-Per-Tenant

Three primary isolation strategies exist, each with distinct tradeoffs:

| Pattern | Isolation | Cost per Tenant | Scale | Operational Complexity |
|---|---|---|---|---|
| **Shared DB + RLS** | Low (logical) | Lowest | Millions of tenants | Low-Medium |
| **Schema-per-Tenant** | Medium (schema) | Low-Medium | Thousands | Medium (schema sprawl) |
| **Database-per-Tenant** | High (physical) | Highest | Hundreds to thousands | High |

**Recommendation: Hybrid approach.** Microsoft's Azure SQL patterns document describes a "Hybrid Sharded Multi-Tenant" model as optimal for platforms with heterogeneous tenant sizes. Small tenants share a multi-tenant database with RLS, while premium/high-volume tenants get dedicated databases. This allows moving tenants between tiers without schema changes. For an iGaming platform, this maps well: small agents share a database; large brands or operators with regulatory requirements (e.g., UKGC-licensed brands requiring strong data isolation) get their own.

**RLS Performance:** PostgreSQL RLS adds **1-5% overhead** when policies use simple predicates that the query planner can push down to index scans. The key optimization is ensuring that RLS policies reference indexed `tenant_id` columns and avoid sub-queries per row. Complex policies with sub-selects can cause exponential scaling degradation. A `SET rls.tenant_id` session variable approach (recommended by Crunchy Data) avoids creating one database user per tenant.

**Sources:**
- [Microsoft: Multitenant SaaS Patterns - Azure SQL Database](https://learn.microsoft.com/en-us/azure/azure-sql/database/saas-tenancy-app-design-patterns?view=azuresql)
- [Crunchy Data: Row Level Security for Tenants in Postgres](https://www.crunchydata.com/blog/row-level-security-for-tenants-in-postgres)
- [Bytebase: PostgreSQL RLS Limitations and Alternatives](https://www.bytebase.com/blog/postgres-row-level-security-limitations-and-alternatives/)
- [Nile: Shipping Multi-Tenant SaaS Using Postgres RLS](https://www.thenile.dev/blog/multi-tenant-rls)

### Tenant Hierarchy Pattern (Platform -> Brand -> Tenant -> Agent)

No single off-the-shelf pattern exists for deep iGaming hierarchies. The recommended approach combines:

1. **Adjacency list with materialized path** in the tenant table: `tenant_id, parent_tenant_id, path (e.g., "/platform/brand_x/tenant_y/agent_z")`, enabling both recursive queries and fast subtree lookups via LIKE on the path column.
2. **Tenant context resolution** at the API gateway layer using a `TenantContextFilter` that extracts tenant ID from JWT claims or subdomain, storing it in a `ThreadLocal` (Spring Boot pattern via `AbstractRoutingDataSource`).
3. **RLS policies** that filter on the materialized path prefix, so a brand admin sees all data under their path, while an agent sees only their subtree.

**Sources:**
- [Baeldung: Multitenancy With Spring Data JPA](https://www.baeldung.com/multitenancy-with-spring-data-jpa)
- [OneUpTime: How to Build Multi-Tenant SaaS Apps in Spring Boot](https://oneuptime.com/blog/post/2026-01-25-multi-tenant-saas-apps-spring-boot/view)

### Configuration Override Pattern (Jurisdiction -> Brand -> Global)

**Recommendation: Layered configuration with inheritance and merge semantics.**

The pattern follows a CSS-like specificity model:
1. **Global defaults** (platform-wide settings)
2. **Jurisdiction overrides** (MGA, UKGC, Curacao-specific compliance settings)
3. **Brand overrides** (branding, limits, product toggles)
4. **Tenant/Agent overrides** (agent-specific commission rates, permissions)

Implementation: Store configuration as JSON documents in a `tenant_config` table with `(tenant_id, config_scope, config_key, config_json)`. At runtime, merge configurations from global to most specific, with more specific values overriding less specific ones. Spring Cloud Config supports a similar inheritance model.

**Hot-Reload:** Use a pub/sub invalidation channel (Redis Pub/Sub or Kafka topic). When configuration changes, publish an invalidation event. Application nodes subscribe and refresh their local config cache. Spring Boot's `@RefreshScope` combined with Spring Cloud Bus provides this out of the box.

---

## Topic 2: Agent/Affiliate Management Systems in Online Gambling

### Agent Hierarchy and Credit Networks

The Asian agent system (the gold standard for agent hierarchies in iGaming) operates as a pyramid with typically **5-10 levels**:

1. **Operator/Bookmaker** — Opens the book, sets global limits
2. **Master Agent (MA)** — Appointed per country/region, deposits a risk guarantee
3. **Super Master Agent (SMA)** — Manages sub-regions
4. **Agent** — Recruits players and lower-level agents
5. **Sub-Agent** — Front-line player recruiters
6-10. **Additional tiers** as needed in deep markets

**Credit Network:** The operator grants credit to the Master Agent, who deposits a predetermined amount as a risk guarantee. The MA then allocates portions of their credit to sub-agents. Each level has a credit limit, and once exposure reaches that limit, risk is passed up the chain. Settlement is typically **weekly or monthly**.

**Recommendation:** Model the hierarchy as a tree structure with `agent_id, parent_agent_id, level, credit_limit, credit_used, risk_percentage`. Implement cascading credit checks on bet placement that walk up the tree to verify available credit at each level.

**Sources:**
- [Sports Trading Network: The Asian Agent System](https://www.sportstradingnetwork.com/article/the-asian-agent-system/)
- [PartnerMatrix: Agent Management System](https://partnermatrix.com/agent-system/)
- [PartnerMatrix: Understanding Agent Networks](https://partnermatrix.com/resource/understanding-agent-networks-in-casinos-and-sports-betting/)

### Commission Models

| Model | Description | Typical Rates | Best For |
|---|---|---|---|
| **Revenue Share (RevShare)** | Percentage of GGR | Sportsbook: 15-40%; Casino: up to 60% | Long-term partnerships |
| **CPA (Cost Per Acquisition)** | One-time fee per first depositing player | $50-$250 per FTD | Volume-focused agents |
| **Turnover Rebate** | Percentage of total wagers | Varies by product | Asian markets, high-volume |
| **Hybrid** | CPA upfront + lower RevShare ongoing | Lower than standalone rates | Balanced cash flow |

### Position Holding (Agent Risk-Sharing)

In the Asian model, the operator allows Master Agents to **take a position on bets** for greater profit potential. Each agent level decides how much risk to retain versus pass up. This creates a distributed risk network.

**Implementation recommendation:** Track `risk_position_percentage` per agent level. When a bet is placed, calculate the risk distribution up the chain. Store position records with `agent_id, bet_id, position_percentage, potential_payout` for settlement calculations.

### Negative Carry-Forward

**Industry trend:** Many competitive programs now offer **No Negative Carryover (NNCO)**, resetting balances to zero each period.

**Recommendation:** Make carry-forward policy configurable per agent agreement: `carry_forward_policy: ENUM('CARRY', 'RESET_MONTHLY', 'RESET_WEEKLY', 'CAP_AT_AMOUNT')`. Store running balances in a `commission_ledger` table with double-entry bookkeeping.

**Sources:**
- [Wynta: No Negative Carryover in iGaming](https://wynta.com/blog/no-negative-carryover-in-igaming-affiliate-marketing/)
- [AffNook: How Revenue Share Models Shape iGaming](https://affnook.com/revenue-share/)

### Agent Self-Service Portal Best Practices

Based on analysis of leading platforms (PartnerMatrix, Gamingtec, BetConstruct):
- Real-time dashboards: GGR, active players, commission earned, credit balance
- Player management: View referred players, activity, deposits/withdrawals
- Sub-agent management: Recruit, configure commission splits, allocate credit
- Financial operations: Transfer credit/money to players and sub-agents
- Reporting: Filterable by date range, product, agent tier; CSV/Excel export
- Average onboarding <10 minutes; 150-500 active players per agent typical

**Sources:**
- [Gamingtec: GT Agent System](https://gamingtec.com/gt-agent-system)
- [BetConstruct: Agent Management System](https://www.betconstruct.com/products/agent-system)

---

## Topic 3: RBAC + Fine-Grained Permission Patterns for Multi-Tenant Platforms

### Fine-Grained RBAC (Resource + Action)

**Recommendation: Hybrid Role Templates with resource:action permissions.**

1. **Global Roles** — Simple but inflexible
2. **Tenant-Scoped Roles** — Causes role explosion
3. **Hybrid Role Templates** (recommended) — Global base templates that tenants can clone, extend, or customize

**Three-question authorization check:**
1. Does the resource belong to the accessed tenant?
2. Is the user a member of that tenant?
3. Does the user's role in that tenant grant this action on this resource type?

**Sources:**
- [WorkOS: How to Design Multi-Tenant RBAC for SaaS](https://workos.com/blog/how-to-design-multi-tenant-rbac-saas)
- [Permit.io: Best Practices for Multi-Tenant Authorization](https://www.permit.io/blog/best-practices-for-multi-tenant-authorization)

### Permission Inheritance Across Tenant Hierarchy

Authorization enforcement at multiple layers:
- **Gateway layer:** Coarse tenant and authentication checks
- **Service layer:** Business logic validation (ownership, status)
- **Data/Policy layer:** Canonical enforcement and audit logging (RLS)

**Critical warning:** Keep resolution "boring and consistent" — explicit permission assignment rather than clever dynamic derivation.

### Impersonation Patterns with Audit Trails

- Log **both the acting user and the target user** in every audit record
- Maintain a separate `impersonation_sessions` table with `admin_id, impersonated_tenant_id, start_time, end_time, reason`

### Maker-Checker Dual Approval Workflow

Implementation: Use a `pending_changes` table with `(id, entity_type, entity_id, change_diff_json, maker_id, checker_id, status, created_at, resolved_at)`.

**Sources:**
- [Xtrm: Maker-Checker Process](https://blog.xtrm.com/posts/maker-checker-process)

### Permission Caching for < 10ms p99 Latency

**Redis Sets** — O(1) membership checks via `SISMEMBER`.
- Key pattern: `perm:{tenantId}:{userId}` — always include tenant ID to prevent cross-tenant leaks
- TTL: 60-300 seconds
- Invalidation: Redis Pub/Sub on `permission:invalidate` channel
- Batch checks: Use Redis pipelines for multiple permissions in a single round-trip

**Sources:**
- [OneUpTime: How to Store User Permissions in Redis](https://oneuptime.com/blog/post/2026-01-21-redis-user-permissions/view)
- [Oso: 10ms or Less — The New Standard for Enterprise Permission Control](https://www-webflow.osohq.com/post/new-standard-for-enterprise-permission-control)

---

## Topic 4: iGaming Regulatory Compliance (MGA/UKGC/Curacao/PAGCOR) 2026

### Licensing Requirements Comparison Matrix

| Requirement | MGA (Malta) | UKGC (UK) | Curacao (CGA/LOK) | PAGCOR (Philippines) |
|---|---|---|---|---|
| **License Term** | 5 years (renewable) | Indefinite | Indefinite (under LOK) | 3 years initial |
| **Data Retention** | 5 years (EU AML) | 5 years after end | 3 years | AML-aligned |
| **Data Location** | EU-based preferred | UK-accessible | Tier-IV in Curacao | Physical servers in PH |
| **GDPR** | Full GDPR | UK GDPR | Only if targeting EU | PH Data Privacy Act |

### Key 2026 Compliance Deadlines

- **UKGC Jan 19, 2026:** Ban on mixed-product incentives; bonus wagering capped at 10x
- **UKGC Mar 19, 2026:** Shareholder reporting threshold raised from 3% to 5%
- **PAGCOR Mar 31, 2026:** All B2B providers must be accredited
- **UKGC Apr 6, 2026:** Digital Markets Act alignment
- **UKGC Jun 30, 2026:** RTS 12 financial limit changes
- **Curacao ongoing:** All operators must transition to new LOK framework

**Sources:**
- [EE Gaming: 2026 iGaming Regulatory Roadmap](https://eegaming.org/latest-news/2026/01/06/131653/2026-igaming-regulatory-roadmap-key-compliance-deadlines/)
- [MGA: 2026 Supervisory Priorities](https://www.mga.org.mt/mga-enhances-regulatory-oversight-and-outlines-2026-supervisory-priorities/)
- [PAGCOR B2B Accreditation](https://europeangaming.eu/portal/latest-news/2025/10/15/193928/pagcor-enforces-accreditation-for-all-igaming-service-providers-by-2026/)

### Cross-Jurisdiction Conflict Resolution

1. **Apply the strictest rule** where jurisdictions conflict
2. **Geo-fencing** to enforce jurisdiction-specific rules at the player level
3. **Dynamic T&Cs** that display local tax, dispute bodies, and regulatory notices
4. **Compliance matrix database** mapping `(jurisdiction, requirement_category, rule, effective_date, expiry_date)`

### Audit Trail Requirements

**Hash-chain immutability implementation:**
- Each audit log entry is cryptographically tied to the previous one via hash pointers
- Append-only tables with write-once semantics
- Every entry records: **Who**, **What**, **When**, **Where**
- Recommendation: `audit_events` table with `(event_id, prev_hash, event_hash, timestamp, actor_id, impersonator_id, tenant_id, action, resource_type, resource_id, change_payload_json)`
- Tiered storage: hot (90 days PostgreSQL), warm (1 year object storage), cold (5-7 years archive)

**Sources:**
- [HubiFi: Immutable Audit Trails Complete Guide](https://www.hubifi.com/blog/immutable-audit-log-basics)
- [ISMS Online: Data Retention for Gaming Compliance](https://www.isms.online/gaming-gambling/data-retention-and-logging-for-gaming-compliance/)

### GDPR Data Portability for Tenant Migration

- Response time: **1 month** (extendable to 3 months)
- Format: JSON or CSV exports
- **EU Data Act (Sep 2025):** Extends portability; SaaS providers must eliminate switching fees by Sep 2027

**Recommendation:** Build a tenant data export API from day one. Produce a ZIP archive with JSON files organized by entity type + manifest.json describing schema.

---

## Testing Recommendations (New Project)

### Recommended Frameworks for Java/Spring Boot

| Layer | Framework | Purpose |
|---|---|---|
| **Unit Testing** | JUnit 5 + Mockito | Service logic, commission calculations, permission checks |
| **Integration Testing** | Spring Boot Test + Testcontainers | Database queries, RLS verification, API endpoints |
| **Architecture Testing** | ArchUnit | Enforce layer dependencies, package structure |
| **API Testing** | REST Assured or MockMvc | Controller endpoints, request/response validation |
| **Performance Testing** | JMH + Gatling/k6 | Permission cache latency, RLS overhead |
| **Contract Testing** | Spring Cloud Contract or Pact | API contracts between services |

### Critical Multi-Tenant Isolation Tests

1. **Cross-tenant data leak tests:** Create data for Tenant A, authenticate as Tenant B, verify zero results
2. **RLS policy bypass tests:** Direct SQL without tenant context variable → empty results or error
3. **Tenant context propagation tests:** Async operations correctly set/clear tenant context
4. **Hierarchy boundary tests:** Brand admin sees only their tenants, Agent sees only their subtree
5. **Audit isolation tests:** Each tenant's audit log contains only their own records
6. **Permission boundary tests:** Unauthorized resource access returns 403

### Compliance Testing Automation

1. **Jurisdiction rule engine tests:** Parameterized `(jurisdiction, player_action, expected_result)` against compliance matrix
2. **Responsible gaming tool verification:** Deposit limits, session timers, self-exclusion across profiles
3. **Audit trail completeness tests:** After business operation, verify all expected events exist with correct hash chains
4. **Data retention policy tests:** Time-travel tests for retention window + proper anonymization after expiry

---

## Key Architectural Recommendations Summary

1. **Data Layer:** Hybrid multi-tenant — PostgreSQL RLS for shared tenants + DB-per-tenant for premium/regulated brands. Target <5% overhead.
2. **Agent Hierarchy:** Tree with materialized paths, 10+ levels. Credit cascades up; commissions flow down.
3. **Commission Engine:** Configurable per agreement: RevShare/CPA/Turnover/Hybrid with tiered rates and configurable negative carry-forward.
4. **Authorization:** Hybrid role templates with resource:action permissions. Redis Sets for sub-10ms lookups. Maker-checker for high-risk operations.
5. **Audit:** Append-only hash-chained event log. 5-year retention (UKGC/MGA), 3-year (Curacao). Tiered storage.
6. **Compliance:** Jurisdiction rule engine with "strictest rule wins". Geo-fencing, dynamic T&Cs, automated compliance matrix validation.
7. **Testing:** Testcontainers for real-DB tests, ArchUnit for architecture enforcement, parameterized cross-tenant isolation tests.
