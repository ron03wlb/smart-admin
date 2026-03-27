# Section 09 -- Tenant Lifecycle (White-Label & Tenant Lifecycle)

## Overview

This section implements the full tenant lifecycle for the iGaming governance platform, covering automated provisioning, billing enforcement, tier/brand migration, and GDPR data portability. It corresponds to plan section 12 (White-Label & Tenant Lifecycle).

**Target stack**: Spring Boot 3.x, Java 21, PostgreSQL, Keycloak, Redis.

---

## Dependencies

| Dependency | Section | What this section uses |
|---|---|---|
| Tenant Isolation | section-02 | `TenantContext`, `TenantRoutingDataSource`, `TenantEntity` model, `TenantService`, Flyway infrastructure |
| RBAC & Auth | section-03 | Keycloak realm/group management, role template seeding, `MakerCheckerService`, permission model, MFA enforcement |

---

## Tests (Write These First)

All tests use JUnit 5 + Mockito + Testcontainers (PostgreSQL + Keycloak).

### 09.1 Tenant Provisioning Tests

File: `sa-module-governance/src/test/java/com/sa/governance/manager/TenantLifecycleManagerTest.java`

```java
// Test: Tenant provisioning creates Keycloak realm/group
// Test: Tenant provisioning creates DB record with materialized path
// Test: Dedicated-tier tenant gets new database with full migration set
// Test: White-label config (domain, branding) applied correctly
// Test: Default roles seeded from templates
// Test: Initial admin user created with forced MFA
// Test: Verification checklist runs after provisioning
// Test: Provisioning validation rejects invalid requests
```

### 09.2 Billing Engine Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/tenant/BillingEnforcementJobTest.java`

```java
// Test: Billing overdue escalation: 7d email warning
// Test: Billing overdue escalation: 14d high-risk
// Test: Billing overdue escalation: 30d freeze
// Test: Billing overdue escalation: 31d+ suspend
```

### 09.3 Tenant Migration Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/tenant/TenantMigrationServiceTest.java`

```java
// Test: Pre-migration checklist validation blocks migration when incomplete
// Test: Tier migration (shared to dedicated) transfers data correctly
// Test: Cross-brand transfer updates materialized path for tenant and descendants
```

### 09.4 GDPR Data Portability Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/tenant/DataPortabilityServiceTest.java`

```java
// Test: GDPR export produces valid JSON with manifest
```

---

## Implementation Details

### TenantLifecycleManager (Manager Layer)

File: `sa-module-governance/src/main/java/com/sa/governance/manager/TenantLifecycleManager.java`

**`provisionTenant(ProvisionTenantRequest request)`** orchestrates:

1. **Validate**: Brand exists and is ACTIVE. Caller has `tenants:create` permission. Billing config present.
2. **Create Keycloak realm or group** depending on SSO mode (FULLY_ISOLATED / BRAND_SSO / FEDERATED).
3. **Create tenant record** in `t_tenant`: compute `materialized_path`, set level/status/tier.
4. **If dedicated tier**: Create new PostgreSQL database, run all Flyway migrations, register in `TenantRoutingDataSource`.
5. **Apply white-label config**: Store `fqdn` and branding in `config_json`.
6. **Seed default data**: Clone role templates, seed compliance rules, copy feature flag defaults from brand.
7. **Create initial admin user** in Keycloak with forced MFA (`CONFIGURE_TOTP` required action).
8. **Run verification checklist**: Data isolation, permissions, config override tests.
9. **Publish `TenantCreatedEvent`** via Kafka.

Maker-Checker integration: Provisioning only executes after `PendingChange` is APPROVED.

### BillingEnforcementJob

File: `sa-module-governance/src/main/java/com/sa/governance/service/tenant/BillingEnforcementJob.java`

Runs **daily**. Escalation timeline:

| Days Overdue | Action |
|---|---|
| 0-7 days | Send email warning (daily) |
| 8-14 days | Set tenant status to `HIGH_RISK` |
| 15-30 days | Disable new player registration |
| 31+ days | Set status to `SUSPENDED`; publish `TenantSuspendedEvent`; players can withdraw but not bet |

**Billing models**: FIXED (flat fee), REVENUE_SHARE (2-5% of GGR), HYBRID (base + share).

### TenantMigrationService

File: `sa-module-governance/src/main/java/com/sa/governance/service/tenant/TenantMigrationService.java`

**Tier migration** (shared <-> dedicated):
1. Set status to `MIGRATING`.
2. Run pre-migration checklist: no pending withdrawals, no active settlements, data integrity verified, wallet balances reconciled.
3. Copy data to target database.
4. Verify integrity (row count + checksum).
5. Update routing, tenant record.
6. Set status back to `ACTIVE`.

**Cross-brand transfer**:
1. Same pre-migration checklist.
2. Update `parent_id`, recompute `materialized_path` for tenant and descendants.
3. Update Keycloak membership.
4. Re-seed brand-level defaults.

Both require Maker-Checker approval.

### DataPortabilityService (GDPR Art 20)

File: `sa-module-governance/src/main/java/com/sa/governance/service/tenant/DataPortabilityService.java`

**`exportTenantData(Long tenantId)`** produces a ZIP archive:

```
export_tenant_{id}_{date}/
  manifest.json           -- Schema descriptions
  players.json
  transactions.json
  game_history.json
  communications.json
  marketing_consents.json
```

Requirements: within 1 month (GDPR), JSON format with manifest, streamed for large exports, audited.

### Domain Events

- **`TenantCreatedEvent`**: Published after successful provisioning. Contains tenantId, brandId, name, tier, fqdn, jurisdiction.
- **`TenantSuspendedEvent`**: Published on billing/compliance suspension. Contains tenantId, reason, suspendedAt.

---

## File Summary

| File | Layer | Purpose |
|---|---|---|
| `manager/TenantLifecycleManager.java` | Manager | Orchestrates tenant provisioning |
| `service/tenant/BillingEnforcementJob.java` | Service | Daily billing escalation |
| `service/tenant/TenantMigrationService.java` | Service | Tier migration and cross-brand transfer |
| `service/tenant/DataPortabilityService.java` | Service | GDPR data export |
| `dao/entity/TenantBillingConfig.java` | DAO | Billing config entity |
| `domain/BillingModel.java` | Domain | Enum: FIXED, REVENUE_SHARE, HYBRID |
| `domain/BillingCycle.java` | Domain | Enum: MONTHLY |
| `event/TenantCreatedEvent.java` | Event | Domain event for new tenant |
| `event/TenantSuspendedEvent.java` | Event | Domain event for suspended tenant |

---

## Implementation Checklist

1. Write all test stubs (4 test files).
2. Create `BillingModel` and `BillingCycle` enums.
3. Create `TenantBillingConfig` entity.
4. Implement `TenantLifecycleManager.provisionTenant()` with all 9 steps.
5. Implement `BillingEnforcementJob` with 4-tier escalation.
6. Implement `TenantMigrationService` with checklist, tier migration, cross-brand transfer.
7. Implement `DataPortabilityService.exportTenantData()`.
8. Create domain event classes.
9. Verify all tests pass.
