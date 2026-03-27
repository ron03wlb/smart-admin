# Section 01 -- Foundation

## Overview

This section establishes the structural foundation for the `sa-module-governance` Maven module. It covers the project skeleton, package layout following SmartAdmin V3.0 conventions, base entity classes, Flyway migration infrastructure, PostgreSQL schema creation for all 16 core tables, RLS policies on all tenant-scoped tables, and ArchUnit tests enforcing layer dependency rules.

**This section has no dependencies.** It is the first thing to implement and all other sections depend on it.

---

## 1. Tests First

Write these tests before any implementation code. The testing stack is JUnit 5 + Mockito + Testcontainers (PostgreSQL) + ArchUnit.

### 1.1 ArchUnit Layer Dependency Tests

File: `sa-module-governance/src/test/java/com/sa/governance/architecture/LayerDependencyTest.java`

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

These tests use ArchUnit's `Architectures.layeredArchitecture()` API to define the four layers (controller, service, manager, dao) and assert that dependency arrows only flow downward (controller -> service -> manager/dao), with the manager layer being the exception that can reach both service and dao. The final two tests use `ArchRuleDefinition.classes()` with `.resideInAPackage()` to verify entity and mapper placement conventions.

### 1.2 RLS Policy Tests (Testcontainers)

File: `sa-module-governance/src/test/java/com/sa/governance/rls/RlsPolicyIntegrationTest.java`

These tests run against a real PostgreSQL instance via Testcontainers. They apply the Flyway migrations to a fresh container and then verify RLS behavior.

```java
// Test: RLS policy blocks queries without tenant context set
//   - Connect as app_user (non-superuser), do NOT call SET rls.tenant_id
//   - Execute SELECT * FROM t_agent
//   - Assert 0 rows returned (even though data exists)

// Test: RLS policy returns only matching tenant rows when context is set
//   - Insert rows for tenant_id=1 and tenant_id=2
//   - SET rls.tenant_id = '1'
//   - SELECT * FROM t_agent
//   - Assert only tenant_id=1 rows returned

// Test: FORCE ROW LEVEL SECURITY blocks table owner from bypassing
//   - Connect as the table owner role
//   - Attempt SELECT without setting rls.tenant_id
//   - Assert 0 rows returned (FORCE prevents bypass)

// Test: Non-superuser role used for all application connections
//   - Validate that the datasource configuration uses role 'app_user'
//   - Assert app_user is NOT a superuser (SELECT rolsuper FROM pg_roles)
```

### 1.3 Flyway Migration Validation Test

File: `sa-module-governance/src/test/java/com/sa/governance/migration/FlywayMigrationTest.java`

```java
// Test: All Flyway migrations apply successfully to a clean database
//   - Start Testcontainers PostgreSQL
//   - Run Flyway migrate
//   - Assert no errors, all migrations applied

// Test: All 16 core tables exist after migration
//   - Query information_schema.tables
//   - Assert presence of all 16 table names listed in section 2.4

// Test: Every tenant-scoped table has an RLS policy
//   - Execute CI validation query:
//     SELECT tablename FROM pg_tables
//     WHERE schemaname = 'public' AND tablename LIKE 't_%'
//     AND tablename NOT IN (SELECT tablename FROM pg_policies);
//   - Assert 0 rows returned

// Test: Every tenant-scoped table has FORCE ROW LEVEL SECURITY enabled
//   - Query pg_class for relforcerowsecurity = true on all tenant-scoped tables
```

---

## 2. Maven Module Setup

### 2.1 Module Declaration

Create the Maven module `sa-module-governance` under the parent `smart-admin-api` project. The module's `pom.xml` must declare the following dependencies (versions managed by parent BOM):

- `spring-boot-starter-web`
- `spring-boot-starter-data-redis`
- `mybatis-plus-boot-starter` (MyBatis-Plus for DAO layer)
- `flyway-core` and `flyway-database-postgresql`
- `postgresql` (JDBC driver)
- `keycloak-spring-boot-starter` or `spring-boot-starter-oauth2-resource-server` for JWT validation
- `spring-kafka`
- Test dependencies: `spring-boot-starter-test`, `archunit-junit5`, `testcontainers` (postgresql module), `rest-assured`

The module packaging is `jar`. It integrates into the SmartAdmin V3.0 parent build.

### 2.2 Application Properties Skeleton

File: `sa-module-governance/src/main/resources/application.yml`

Define placeholder configuration groups for:

- PostgreSQL datasource (shared DB connection, HikariCP pool)
- Redis connection (for permission cache and credit cache)
- Flyway (enabled, locations, baseline-on-migrate)
- Keycloak (realm, auth-server-url, resource)
- Kafka (bootstrap servers, consumer group)
- Custom properties namespace `governance.*` for application-specific settings

---

## 3. SmartAdmin V3.0 Four-Layer Package Structure

### 3.1 Package Layout

Create the following directory structure under `sa-module-governance/src/main/java/com/sa/governance/`:

```
com.sa.governance/
  controller/        # REST endpoints (entry layer)
    tenant/
    rbac/
    agent/
    commission/
    settlement/
    audit/
    compliance/
    feature-flag/
  service/           # Business logic layer
    tenant/
    rbac/
    agent/
    commission/
    settlement/
    audit/
    compliance/
    feature-flag/
  manager/           # Orchestration layer (cross-service coordination)
  dao/               # Data access layer
    entity/
    mapper/          # MyBatis-Plus mappers
    cache/           # Redis cache operations
  domain/            # Domain objects, enums, value objects
  config/            # Spring configuration
  event/             # Domain events (publish/subscribe)
  infrastructure/    # Cross-cutting: tenant context, RLS, audit interceptor
```

### 3.2 Layer Rules

These rules are what the ArchUnit tests enforce:

| Layer | Can Depend On | Cannot Depend On |
|-------|---------------|------------------|
| controller | service | manager, dao, infrastructure |
| service | manager, dao | controller |
| manager | service, dao | controller |
| dao | (nothing above it) | controller, service, manager |

The `domain`, `config`, `event`, and `infrastructure` packages are cross-cutting and can be referenced by any layer.

---

## 4. Base Entity Classes

### 4.1 BaseEntity

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/BaseEntity.java`

A base entity class providing common fields shared by all tenant-scoped entities:

- `Long id` -- primary key (auto-generated)
- `Long tenantId` -- non-nullable, used by RLS policies
- `Instant createdAt` -- auto-populated on insert
- `Instant updatedAt` -- auto-populated on update
- `String createdBy` -- user who created the record
- `String updatedBy` -- user who last modified the record
- `Boolean deleted` -- logical delete flag (MyBatis-Plus `@TableLogic`)

Use MyBatis-Plus annotations (`@TableId`, `@TableField` with fill strategy) for auto-population of audit fields.

### 4.2 Enums

File: `sa-module-governance/src/main/java/com/sa/governance/domain/enums/`

Define the following enums used across the schema. Each enum should be a simple Java enum with no business logic:

- `TenantLevel` -- `PLATFORM`, `BRAND`, `TENANT`
- `TenantStatus` -- `ACTIVE`, `SUSPENDED`, `MIGRATING`, `CLOSED`
- `IsolationTier` -- `SHARED`, `DEDICATED`
- `AgentType` -- `MASTER`, `INTERMEDIATE`, `TERMINAL`
- `AgentStatus` -- `ACTIVE`, `SUSPENDED`, `FROZEN`, `CLOSED`
- `ChangeStatus` -- `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`
- `BillingModel` -- `FIXED`, `REVENUE_SHARE`, `HYBRID`
- `BillingCycle` -- `MONTHLY`
- `NegativeCarryPolicy` -- `CARRY`, `RESET_MONTHLY`, `RESET_WEEKLY`, `CAP_AT_AMOUNT`
- `SettlementCycle` -- `WEEKLY`, `MONTHLY`
- `BatchStatus` -- `RUNNING`, `COMPLETED`, `FAILED_PARTIAL`
- `Jurisdiction` -- `MGA`, `UKGC`, `CURACAO`, `PAGCOR`
- `EntryType` -- `EARNED`, `DEDUCTED`, `CARRY_FORWARD`, `RESET`
- `SettlementStatus` -- `PENDING_REVIEW`, `APPROVED`, `COMPLETED`, `ESCALATED`, `FAILED`
- `CreditTransactionStatus` -- `PENDING`, `COMMITTED`, `REJECTED`, `ROLLED_BACK`

---

## 5. Flyway Migration Framework

### 5.1 Configuration

File: `sa-module-governance/src/main/resources/application.yml` (Flyway section)

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true
    validate-on-migrate: true
```

Migration files go in: `sa-module-governance/src/main/resources/db/migration/`

### 5.2 Migration File Naming

Follow Flyway convention: `V{version}__{description}.sql`

The foundation section creates three migration files:

1. `V1__create_roles_and_rls_setup.sql` -- Creates the `app_user` database role and sets up RLS session variable defaults
2. `V2__create_core_tables.sql` -- Creates all 16 core tables
3. `V3__enable_rls_policies.sql` -- Enables RLS, creates policies, and applies FORCE ROW LEVEL SECURITY

Splitting into three files keeps each migration focused and makes troubleshooting easier.

---

## 6. PostgreSQL Schema -- All 16 Core Tables

### 6.1 Table Definitions

Migration file: `V2__create_core_tables.sql`

Create all 16 tables. Below is the complete list with key columns. Every tenant-scoped table includes `tenant_id BIGINT NOT NULL` and standard audit columns (`created_at`, `updated_at`, `created_by`, `updated_by`, `deleted`). Primary keys use `BIGSERIAL`.

**Table 1: `t_tenant`** -- Tenant hierarchy

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| parent_id | BIGINT | FK to t_tenant.id, NULL for Platform |
| materialized_path | VARCHAR(500) | e.g. "/1/5/12" |
| level | VARCHAR(20) | PLATFORM, BRAND, TENANT |
| name | VARCHAR(200) | |
| fqdn | VARCHAR(255) | White-label domain |
| tier | VARCHAR(20) | SHARED, DEDICATED |
| db_identifier | VARCHAR(100) | NULL for SHARED tier |
| status | VARCHAR(20) | ACTIVE, SUSPENDED, MIGRATING, CLOSED |
| config_json | JSONB | Tenant-level config overrides |
| created_at, updated_at | TIMESTAMPTZ | |
| created_by, updated_by | VARCHAR(100) | |
| deleted | BOOLEAN DEFAULT FALSE | |

Note: `t_tenant` does NOT have a `tenant_id` column because it is the tenant definition table itself. RLS on this table uses a different policy (the `id` column or `materialized_path` for hierarchical access control). Platform-level rows are visible to super admins only.

**Table 2: `t_tenant_billing`** -- Billing configuration

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | FK to t_tenant.id |
| model | VARCHAR(20) | FIXED, REVENUE_SHARE, HYBRID |
| fixed_amount | NUMERIC(18,2) | |
| share_rate | NUMERIC(5,4) | 2-5% |
| cycle | VARCHAR(20) | MONTHLY |
| grace_period_days | INTEGER DEFAULT 7 | |
| auto_renew | BOOLEAN DEFAULT TRUE | |
| Standard audit columns | | |

**Table 3: `t_role_template`** -- Global role templates

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| name | VARCHAR(100) | e.g. "brand_admin" |
| applicable_level | VARCHAR(20) | PLATFORM, BRAND, TENANT |
| permissions_json | JSONB | Default permission set |
| is_system | BOOLEAN DEFAULT FALSE | Cannot delete if true |
| Standard audit columns | | |

Note: `t_role_template` is a global table (no `tenant_id`). It defines templates that are cloned per tenant.

**Table 4: `t_tenant_role`** -- Tenant-customized roles

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| template_id | BIGINT | FK to t_role_template.id, NULL if fully custom |
| name | VARCHAR(100) | |
| permissions_json | JSONB | |
| Standard audit columns | | |

**Table 5: `t_user_role_mapping`** -- User-role assignments

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| user_id | VARCHAR(100) NOT NULL | Keycloak user ID |
| tenant_id | BIGINT NOT NULL | |
| role_id | BIGINT NOT NULL | FK to t_tenant_role.id |
| Standard audit columns | | |
| UNIQUE(user_id, tenant_id, role_id) | | |

**Table 6: `t_agent`** -- Agent hierarchy and credit

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| parent_agent_id | BIGINT | FK to t_agent.id, NULL for Master Agent |
| materialized_path | VARCHAR(500) | e.g. "/tenant_5/agent_1/agent_7" |
| level | INTEGER NOT NULL | 1 = Master, 2-10 = sub-agents |
| type | VARCHAR(20) | MASTER, INTERMEDIATE, TERMINAL |
| status | VARCHAR(20) | ACTIVE, SUSPENDED, FROZEN, CLOSED |
| credit_limit | NUMERIC(18,2) NOT NULL DEFAULT 0 | |
| credit_used | NUMERIC(18,2) NOT NULL DEFAULT 0 | |
| credit_version | BIGINT NOT NULL DEFAULT 0 | Optimistic lock |
| commission_agreement_id | BIGINT | FK to t_commission_agreement.id |
| mfa_secret | VARCHAR(255) | Encrypted |
| Standard audit columns | | |

**Table 7: `t_agent_ip_whitelist`** -- Agent IP restrictions

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| agent_id | BIGINT NOT NULL | FK to t_agent.id |
| ip_address | INET NOT NULL | |
| approved_by | VARCHAR(100) | |
| approved_at | TIMESTAMPTZ | |
| Standard audit columns | | |

**Table 8: `t_commission_agreement`** -- Commission config per agent

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| agent_id | BIGINT NOT NULL | FK to t_agent.id |
| models_json | JSONB | Active commission model configs |
| carry_policy | VARCHAR(30) | CARRY, RESET_MONTHLY, etc. |
| cap_amount | NUMERIC(18,2) | For CAP_AT_AMOUNT policy |
| settlement_cycle | VARCHAR(20) | WEEKLY, MONTHLY |
| effective_from | TIMESTAMPTZ NOT NULL | |
| effective_to | TIMESTAMPTZ | NULL = indefinite |
| Standard audit columns | | |

**Table 9: `t_commission_ledger`** -- Double-entry commission log

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| agent_id | BIGINT NOT NULL | FK to t_agent.id |
| period_key | VARCHAR(20) NOT NULL | e.g. "2026-W13" or "2026-03" |
| type | VARCHAR(20) NOT NULL | EARNED, DEDUCTED, CARRY_FORWARD, RESET |
| amount | NUMERIC(18,2) NOT NULL | |
| running_balance | NUMERIC(18,2) NOT NULL | |
| description | VARCHAR(500) | |
| Standard audit columns | | |

**Table 10: `t_agent_position`** -- Position holding records

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| agent_id | BIGINT NOT NULL | FK to t_agent.id |
| bet_id | VARCHAR(100) NOT NULL | Reference to bet in gaming domain |
| position_pct | NUMERIC(5,2) NOT NULL | Percentage of risk held |
| potential_payout | NUMERIC(18,2) | |
| Standard audit columns | | |

**Table 11: `t_settlement_record`** -- Settlement lifecycle

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| agent_id | BIGINT NOT NULL | FK to t_agent.id |
| period_key | VARCHAR(20) NOT NULL | |
| amount | NUMERIC(18,2) NOT NULL | |
| currency | VARCHAR(3) NOT NULL | ISO 4217 |
| status | VARCHAR(20) NOT NULL | PENDING_REVIEW, APPROVED, COMPLETED, ESCALATED, FAILED |
| retry_count | INTEGER NOT NULL DEFAULT 0 | |
| approved_by | VARCHAR(100) | |
| approved_at | TIMESTAMPTZ | |
| batch_id | BIGINT | FK to t_settlement_batch.id |
| Standard audit columns | | |
| UNIQUE(agent_id, period_key) | | For idempotency |

**Table 12: `t_settlement_batch`** -- Settlement batch tracking

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| period_key | VARCHAR(20) NOT NULL | |
| cycle | VARCHAR(20) NOT NULL | WEEKLY, MONTHLY |
| status | VARCHAR(20) NOT NULL | RUNNING, COMPLETED, FAILED_PARTIAL |
| total_agents | INTEGER | |
| processed_agents | INTEGER DEFAULT 0 | |
| failed_agents | INTEGER DEFAULT 0 | |
| input_checksum | VARCHAR(64) | SHA-256 |
| started_at | TIMESTAMPTZ | |
| completed_at | TIMESTAMPTZ | |
| Standard audit columns | | |

**Table 13: `t_pending_change`** -- Maker-Checker queue

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| entity_type | VARCHAR(50) NOT NULL | TENANT, AGENT_CREDIT, COMMISSION_MODEL, CONFIG_OVERRIDE |
| entity_id | VARCHAR(100) NOT NULL | |
| change_diff | JSONB NOT NULL | Before/after JSON diff |
| maker_id | VARCHAR(100) NOT NULL | |
| checker_id | VARCHAR(100) | NULL until reviewed |
| status | VARCHAR(20) NOT NULL DEFAULT 'PENDING' | PENDING, APPROVED, REJECTED, EXPIRED |
| maker_comment | TEXT | |
| checker_comment | TEXT | |
| expires_at | TIMESTAMPTZ NOT NULL | Default now() + 72h |
| resolved_at | TIMESTAMPTZ | |
| Standard audit columns | | |

**Table 14: `t_compliance_rule`** -- Jurisdiction rules matrix

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| jurisdiction | VARCHAR(20) NOT NULL | MGA, UKGC, CURACAO, PAGCOR |
| category | VARCHAR(50) NOT NULL | KYC, AML, RESPONSIBLE_GAMBLING, DATA_RETENTION |
| rule_key | VARCHAR(100) NOT NULL | e.g. "max_wagering_multiplier" |
| rule_value | VARCHAR(500) NOT NULL | |
| description | TEXT | |
| effective_from | TIMESTAMPTZ NOT NULL | |
| effective_to | TIMESTAMPTZ | NULL = currently effective |
| is_strictest | BOOLEAN DEFAULT FALSE | Pre-computed flag |
| Standard audit columns | | |

Note: `t_compliance_rule` is a global table (no `tenant_id`). Rules apply across all tenants based on jurisdiction.

**Table 15: `t_feature_flag`** -- Feature toggles

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| flag_key | VARCHAR(100) NOT NULL | |
| tenant_id | BIGINT | NULL = platform-level default |
| enabled | BOOLEAN NOT NULL DEFAULT FALSE | |
| rollout_pct | INTEGER | 0-100 for gradual rollout |
| metadata | JSONB | |
| Standard audit columns | | |
| UNIQUE(flag_key, tenant_id) | | |

**Table 16: `t_audit_event`** -- Immutable audit log

| Column | Type | Notes |
|--------|------|-------|
| event_id | UUID PK | |
| tenant_id | BIGINT NOT NULL | |
| prev_hash | VARCHAR(64) | Hash of previous event in tenant's chain |
| event_hash | VARCHAR(64) NOT NULL | SHA-256(prevHash + canonical(eventData)) |
| timestamp | TIMESTAMPTZ NOT NULL | NTP-synchronized |
| actor_id | UUID NOT NULL | |
| impersonator_id | UUID | NULL if not impersonating |
| action | VARCHAR(100) NOT NULL | e.g. "AGENT_CREDIT_ADJUST" |
| resource_type | VARCHAR(50) | |
| resource_id | VARCHAR(100) | |
| change_payload | JSONB | Before/after diff |

This table is append-only. It does NOT have `updated_at`, `updated_by`, or `deleted` columns. UPDATE and DELETE operations are blocked by a trigger (see section 6.2).

**Additional table: `t_impersonation_session`** -- Impersonation tracking

| Column | Type | Notes |
|--------|------|-------|
| session_id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| actor_id | UUID NOT NULL | Admin performing impersonation |
| target_tenant_id | BIGINT NOT NULL | |
| target_user_id | UUID | Specific user being impersonated |
| start_time | TIMESTAMPTZ NOT NULL | |
| end_time | TIMESTAMPTZ | NULL while active |
| reason | TEXT NOT NULL | |
| Standard audit columns | | |

**Additional table: `t_credit_transaction_log`** -- Write-ahead log for credit operations

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | |
| tenant_id | BIGINT NOT NULL | |
| agent_id | BIGINT NOT NULL | FK to t_agent.id |
| amount | NUMERIC(18,2) NOT NULL | |
| idempotency_key | VARCHAR(100) NOT NULL UNIQUE | |
| status | VARCHAR(20) NOT NULL | PENDING, COMMITTED, REJECTED, ROLLED_BACK |
| Standard audit columns | | |

**Additional table: `t_agent_closure`** -- Closure table for agent hierarchy queries

| Column | Type | Notes |
|--------|------|-------|
| ancestor_id | BIGINT NOT NULL | FK to t_agent.id |
| descendant_id | BIGINT NOT NULL | FK to t_agent.id |
| depth | INTEGER NOT NULL | 0 = self, 1 = direct child, etc. |
| PRIMARY KEY (ancestor_id, descendant_id) | | |

### 6.2 Indexes

Create B-tree indexes on all `tenant_id` columns (essential for RLS performance). Additional indexes:

- `t_tenant`: index on `materialized_path`, `parent_id`
- `t_agent`: index on `materialized_path`, `parent_agent_id`, `tenant_id`
- `t_agent_ip_whitelist`: index on `agent_id`
- `t_commission_ledger`: index on `(agent_id, period_key)`
- `t_settlement_record`: unique index on `(agent_id, period_key)`
- `t_pending_change`: index on `(status, expires_at)` for the expiry sweep job
- `t_compliance_rule`: index on `(jurisdiction, category, rule_key, effective_from)`
- `t_feature_flag`: unique index on `(flag_key, tenant_id)`
- `t_audit_event`: index on `(tenant_id, timestamp)` for hash chain queries
- `t_credit_transaction_log`: index on `(status, created_at)` for recovery queries

---

## 7. RLS Policies and Hardening

### 7.1 Role Setup

Migration file: `V1__create_roles_and_rls_setup.sql`

Create a non-superuser application role and set the default session variable:

```sql
-- Create application role (non-superuser)
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_user') THEN
    CREATE ROLE app_user LOGIN PASSWORD 'changeme';  -- password managed via secrets
  END IF;
END $$;

-- Set default for rls.tenant_id (returns -1 when not explicitly set, matching no rows)
ALTER DATABASE current_database SET rls.tenant_id = '-1';
```

Grant `app_user` the necessary privileges: `SELECT, INSERT, UPDATE, DELETE` on all `t_*` tables, `USAGE, SELECT` on all sequences. Do NOT grant `BYPASSRLS` or `SUPERUSER`.

### 7.2 RLS Policies

Migration file: `V3__enable_rls_policies.sql`

Apply RLS to every tenant-scoped table. The following tables are tenant-scoped (they have a `tenant_id` column):

- `t_tenant_billing`
- `t_tenant_role`
- `t_user_role_mapping`
- `t_agent`
- `t_agent_ip_whitelist`
- `t_commission_agreement`
- `t_commission_ledger`
- `t_agent_position`
- `t_settlement_record`
- `t_settlement_batch`
- `t_pending_change`
- `t_feature_flag`
- `t_audit_event`
- `t_impersonation_session`
- `t_credit_transaction_log`

For each tenant-scoped table, apply:

```sql
ALTER TABLE {table_name} ENABLE ROW LEVEL SECURITY;
ALTER TABLE {table_name} FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON {table_name}
  USING (tenant_id = current_setting('rls.tenant_id')::bigint);
```

The `FORCE ROW LEVEL SECURITY` statement ensures even the table owner cannot bypass RLS.

Tables that are NOT tenant-scoped (global tables):
- `t_tenant` -- hierarchical access, not filtered by a single tenant_id
- `t_role_template` -- global templates shared across all tenants
- `t_compliance_rule` -- global jurisdiction rules
- `t_agent_closure` -- referential table, access controlled through t_agent RLS

For `t_feature_flag`, note that `tenant_id` can be NULL (platform-level defaults). The RLS policy should use:

```sql
CREATE POLICY tenant_isolation ON t_feature_flag
  USING (tenant_id = current_setting('rls.tenant_id')::bigint OR tenant_id IS NULL);
```

This allows platform-level flags (tenant_id IS NULL) to be visible to all tenants while tenant-specific flags are isolated.

### 7.3 Audit Event Append-Only Trigger

Include in the migration an append-only trigger for `t_audit_event`:

```sql
CREATE OR REPLACE FUNCTION prevent_audit_mutation() RETURNS TRIGGER AS $$
BEGIN
  RAISE EXCEPTION 'UPDATE and DELETE are not permitted on t_audit_event';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_no_update
  BEFORE UPDATE OR DELETE ON t_audit_event
  FOR EACH ROW EXECUTE FUNCTION prevent_audit_mutation();
```

---

## 8. Application Datasource Configuration

### 8.1 HikariCP Connection Pool Hooks

File: `sa-module-governance/src/main/java/com/sa/governance/config/DataSourceConfig.java`

The HikariCP pool must set `rls.tenant_id` on connection checkout and reset it on connection return. This is a critical safety net ensuring that even raw JDBC or third-party library queries respect tenant isolation.

Key configuration points:

- `connectionInitSql`: Not used for per-request tenant setting (it runs only once on connection creation). Instead, use HikariCP's `setConnectionCustomizer` or a custom `DataSource` wrapper.
- On connection checkout: execute `SET rls.tenant_id = ?` using the current tenant context value
- On connection return: execute `RESET rls.tenant_id` or `SET rls.tenant_id = '-1'`
- The datasource connects as role `app_user` (non-superuser)

The actual tenant context filter and routing datasource implementation belong to section-02-tenant-isolation. This section only establishes the datasource bean and pool configuration.

---

## 9. Implementation Checklist

1. Create Maven module `sa-module-governance` with `pom.xml` and dependencies
2. Create the four-layer package structure (empty packages with `package-info.java`)
3. Write ArchUnit tests (section 1.1) -- they will fail initially
4. Create `BaseEntity` class and all enum types
5. Write Flyway migration `V1__create_roles_and_rls_setup.sql`
6. Write Flyway migration `V2__create_core_tables.sql` with all tables and indexes
7. Write Flyway migration `V3__enable_rls_policies.sql` with all RLS policies, FORCE RLS, and the audit append-only trigger
8. Write and run Flyway migration tests (section 1.3) to verify schema correctness
9. Write and run RLS integration tests (section 1.2) to verify tenant isolation at the database level
10. Configure `application.yml` with datasource and Flyway settings
11. Create `DataSourceConfig` with HikariCP pool hooks for RLS context management
12. Verify all ArchUnit tests pass with the current package structure

---

## 10. Key Design Decisions (Context)

These decisions from the architecture plan inform choices made in this section:

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Data isolation | Hybrid RLS + Dedicated DB | Balance cost and compliance; UKGC brands get physical isolation |
| ORM | MyBatis-Plus | SmartAdmin V3.0 convention; all mappers extend `BaseMapper` |
| Migration tool | Flyway | Version-numbered SQL migrations; applies to both shared and dedicated databases |
| DB role | Non-superuser `app_user` | Combined with FORCE ROW LEVEL SECURITY, prevents any bypass |
| RLS variable | `rls.tenant_id` session parameter | Set at connection pool level, verified by MyBatis interceptor (defense-in-depth) |
| Audit immutability | PostgreSQL trigger blocking UPDATE/DELETE | Database-level enforcement, cannot be bypassed by application code |

---

## 11. Dependencies on Other Sections

This section has **no upstream dependencies**. It is implemented first.

The following sections depend on this foundation:

- **section-02-tenant-isolation**: Uses the schema, RLS policies, and datasource config created here. Adds `TenantContextFilter`, `TenantRoutingDataSource`, and `TenantRlsInterceptor`.
- **section-03-rbac-auth**: Uses `t_role_template`, `t_tenant_role`, `t_user_role_mapping`, `t_pending_change`, `t_impersonation_session` tables.
- **section-04-agent-hierarchy**: Uses `t_agent`, `t_agent_closure`, `t_credit_transaction_log`, `t_agent_ip_whitelist` tables.
- **section-05-commission-engine**: Uses `t_commission_agreement`, `t_commission_ledger`, `t_agent_position` tables.
- **section-06-settlement**: Uses `t_settlement_record`, `t_settlement_batch` tables.
- **section-07-compliance-flags**: Uses `t_compliance_rule`, `t_feature_flag` tables.
- **section-08-audit-system**: Uses `t_audit_event` table and its append-only trigger.
- **section-09-tenant-lifecycle**: Uses `t_tenant`, `t_tenant_billing` tables.
