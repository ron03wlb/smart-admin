<!-- PROJECT_CONFIG
runtime: java-maven
test_command: ./mvnw test
END_PROJECT_CONFIG -->

<!-- SECTION_MANIFEST
section-01-foundation
section-02-tenant-isolation
section-03-rbac-auth
section-04-agent-hierarchy
section-05-commission-engine
section-06-settlement
section-07-compliance-flags
section-08-audit-system
section-09-tenant-lifecycle
section-10-integration
END_MANIFEST -->

# Implementation Sections Index — 01-governance-agent

## Dependency Graph

| Section | Depends On | Blocks | Parallelizable |
|---------|------------|--------|----------------|
| section-01-foundation | - | all | Yes (standalone) |
| section-02-tenant-isolation | 01 | 03, 04, 07, 08, 09 | No (sequential after 01) |
| section-03-rbac-auth | 01, 02 | 04, 09 | Yes (parallel with 07, 08) |
| section-04-agent-hierarchy | 01, 02, 03 | 05 | No (sequential) |
| section-05-commission-engine | 04 | 06 | No (sequential) |
| section-06-settlement | 01, 02, 04, 05 | 10 | No (sequential) |
| section-07-compliance-flags | 01, 02, 03 | 10 | Yes (parallel with 04, 08, 09) |
| section-08-audit-system | 01, 02 | 10 | Yes (parallel with 03, 07) |
| section-09-tenant-lifecycle | 02, 03 | 10 | Yes (parallel with 04-06) |
| section-10-integration | all above | - | No (final) |

## Execution Order

1. **Batch 1**: section-01-foundation (no dependencies)
2. **Batch 2**: section-02-tenant-isolation (after 01)
3. **Batch 3**: section-03-rbac-auth, section-08-audit-system (parallel after 02)
4. **Batch 4**: section-04-agent-hierarchy, section-07-compliance-flags, section-09-tenant-lifecycle (parallel after 03)
5. **Batch 5**: section-05-commission-engine (after 04)
6. **Batch 6**: section-06-settlement (after 05)
7. **Batch 7**: section-10-integration (final, after all)

## Section Summaries

### section-01-foundation
**Plan sections**: 2.1-2.2 (Architecture Overview, Module Structure)
**Scope**: Maven module setup (`sa-module-governance`), SmartAdmin V3.0 four-layer package structure, base entity classes, Flyway migration framework, PostgreSQL schema creation for all 16 core tables, RLS policy SQL for all tenant-scoped tables, `FORCE ROW LEVEL SECURITY` on all tables, ArchUnit tests for layer dependencies.

### section-02-tenant-isolation
**Plan sections**: 3.1-3.4 (Multi-Tenant Data Isolation)
**Scope**: `TenantContextFilter` (OncePerRequestFilter), `TenantContext` (ThreadLocal + ScopedValue for Java 21), `TenantRoutingDataSource` (AbstractRoutingDataSource), `TenantRlsInterceptor` (MyBatis), HikariCP connection pool hooks (set/reset `rls.tenant_id`), `TenantAwareTaskDecorator` for async propagation, tenant hierarchy CRUD (`TenantService`), materialized path management, cross-tenant data sharing rules. TDD: cross-tenant leak tests, RLS bypass tests, virtual thread propagation tests.

### section-03-rbac-auth
**Plan sections**: 4.1-4.4 (RBAC), 5.1-5.4 (Keycloak)
**Scope**: Keycloak realm/group configuration, `PermissionCheckService` with Redis Sets caching, role template and tenant-role management, `resource:action` permission model, permission bundles, custom role CRUD (max 20/tenant), `ImpersonationService` with session tracking, `MakerCheckerService` with state machine (PENDING/APPROVED/REJECTED/EXPIRED), `MakerCheckerExpiryJob`, `@RequireReAuth` annotation + interceptor, IP whitelist enforcement, session management (single session, lockout). TDD: permission check latency, impersonation scope tests, double-approval prevention.

### section-04-agent-hierarchy
**Plan sections**: 6.1-6.4 (Agent Hierarchy & Credit), 17 (Hierarchy Locking)
**Scope**: Agent tree model (10-level max), `AgentEntity` with materialized path + closure table, `AgentCreditService` with write-ahead pattern (CreditTransactionLog → Redis Lua → DB commit), `CreditRecoveryService` for WAL replay, Redis credit cache with AOF persistence, `CreditMonitorJob` (defense-in-depth), risk threshold alerts, self-exclusion integration (`PlayerSelfExclusionEvent` listener), `HierarchyChangeService` with advisory locks, concurrency control (optimistic lock + serialized deduction). TDD: write-ahead durability, Lua script atomicity, recovery replay, hierarchy lock during settlement.

### section-05-commission-engine
**Plan sections**: 7.1-7.4 (Commission Engine)
**Scope**: `CommissionAgreement` model (single or multi-model per agent), Strategy pattern with 4 implementations (`RevenueShareStrategy`, `TurnoverRebateStrategy`, `CpaStrategy`, `HybridStrategy`), tiered revenue share calculation, position holding distribution, `CommissionLedger` with double-entry bookkeeping, negative carry-forward with configurable policy (CARRY/RESET_MONTHLY/RESET_WEEKLY/CAP_AT_AMOUNT). TDD: tier boundary calculations, position normalization, carry-forward policies, multi-model summation.

### section-06-settlement
**Plan sections**: 8.1-8.3 (Settlement), 18 (Settlement Batch)
**Scope**: `SettlementBatch` entity for run state tracking, `SettlementOrchestrator` with idempotent per-agent calculation, approval tier routing (<$10K auto / $10K-$50K Manager / $50K-$100K CFO / ≥$100K CFO+CEO), auto-retry on payout failure (3x over 3 business days → escalation), `SettlementMonitorService` for anomaly detection ($500K monthly, 200% weekly growth, new agent $50K), multi-currency settlement with FX rate from 02-funding. TDD: idempotent calculation, crash recovery, approval routing, anomaly flagging.

### section-07-compliance-flags
**Plan sections**: 9 (Compliance Matrix), 10 (Feature Flags)
**Scope**: `ComplianceRule` model with jurisdiction/category/key/effective dates, `ComplianceService.getEffectiveRule()` with strictest-rule-wins conflict resolution, 2026 deadline seeding (UKGC, PAGCOR), `ComplianceRuleChangedEvent`. Feature flag: `FeatureFlag` model, resolution order (tenant → brand → platform), Redis cache with 60s TTL + Pub/Sub invalidation, Maker-Checker on flag changes, REST API for flag queries. TDD: jurisdiction conflict resolution, effective date filtering, flag resolution order.

### section-08-audit-system
**Plan sections**: 11 (Immutable Audit System)
**Scope**: Per-tenant hash-chain audit events, Kafka topic `audit-events` partitioned by `tenant_id`, `AuditChainWriter` consumer for hash computation, `@Audited` Spring AOP aspect for automatic before/after capture, append-only PostgreSQL table with UPDATE/DELETE triggers, `ChainVerificationJob` (daily integrity check), tiered storage pipeline (hot 90d PostgreSQL → warm 1yr S3-Parquet → cold 7yr archive), `AuditArchivalJob` (weekly). TDD: hash chain integrity, append-only enforcement, cross-tier boundary verification, partition independence.

### section-09-tenant-lifecycle
**Plan sections**: 12 (White-Label & Tenant Lifecycle)
**Scope**: `TenantLifecycleManager.provisionTenant()` orchestration (Keycloak realm, DB record, dedicated DB migration, white-label config, default data seeding, admin user with forced MFA, verification checklist), `BillingConfig` model + `BillingEnforcementJob` (overdue escalation), `TenantMigrationService` (tier migration + cross-brand transfer, pre-migration checklist), `DataPortabilityService` for GDPR Art 20 export (ZIP with JSON + manifest). TDD: provisioning flow, billing escalation, migration checklist validation, GDPR export format.

### section-10-integration
**Plan sections**: 13 (Cross-Domain APIs), 15 (Error Handling), 16 (Monitoring), 19 (API Versioning), 20 (Migrations), 21 (Keycloak HA), 22 (DR)
**Scope**: REST API endpoints for all cross-domain contracts (8 exposed APIs), Spring Cloud Contract tests for all consumers, event bus (8 event types via Kafka), Resilience4j circuit breakers on outbound calls, API versioning strategy (/api/v1/ with 90-day co-existence), Flyway migration validation (CI step: all tables have RLS), Keycloak HA topology (2 nodes, Infinispan), health checks (5 types), monitoring metrics and alert thresholds, DR procedures (PostgreSQL WAL-G, Redis AOF, chain break runbook, credit divergence reconciliation).
