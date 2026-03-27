# Section 07: Compliance Flags

## Overview

This section implements two closely related platform services within the Governance & Agent Domain:

1. **Compliance Matrix** -- A single source of truth (SSOT) for jurisdiction-specific regulatory rules across MGA, UKGC, Curacao, and PAGCOR, with effective date tracking and cross-jurisdiction conflict resolution.
2. **Feature Flag System** -- A lightweight, tenant-aware feature flag mechanism with three-layer resolution, Redis caching, and Maker-Checker governance.

Both systems are tenant-scoped and must respect the RLS data isolation model established in sections 01 and 02.

**Technology stack**: Spring Boot 3.x, Java 21, PostgreSQL (with RLS), Redis, MyBatis-Plus, Kafka.

---

## Dependencies

| Dependency | Section | What is needed |
|------------|---------|----------------|
| Base entities, Flyway migrations, PostgreSQL tables `t_compliance_rule` and `t_feature_flag` | section-01-foundation | Tables must exist with RLS policies enabled |
| Tenant context (ThreadLocal/ScopedValue), RLS interceptor, `TenantContext` | section-02-tenant-isolation | `TenantContext.getTenantId()` must be available; RLS must be active on both tables |
| Maker-Checker service (`MakerCheckerService`, `t_pending_change` table) | section-03-rbac-auth | Feature flag changes flow through Maker-Checker approval |

This section can be implemented in **Batch 4** (parallel with section-04-agent-hierarchy and section-09-tenant-lifecycle), after sections 01, 02, and 03 are complete.

---

## Tests (Write These First)

All tests use JUnit 5 + Mockito + Testcontainers (PostgreSQL + Redis). Write these test stubs before any implementation code.

### Compliance Matrix Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/compliance/ComplianceServiceTest.java`

```java
// Test: ComplianceService returns correct rule for jurisdiction + category + key
// Test: Effective date filtering -- only current rules returned
// Test: Cross-jurisdiction conflict -- strictest rule applied
// Test: Compliance rule change creates ComplianceRuleChangedEvent
// Test: 2026 deadlines seeded correctly (UKGC Jan/Mar/Apr/Jun, PAGCOR Mar)
```

### Feature Flag Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/featureflag/FeatureFlagServiceTest.java`

```java
// Test: Flag enabled for tenant returns true
// Test: Flag disabled for tenant returns false
// Test: Resolution order -- tenant override beats brand default beats platform default
// Test: Flag change requires Maker-Checker
// Test: Flag cached in Redis with 60s TTL
// Test: Flag invalidated via Pub/Sub on change
```

### Integration Tests (Testcontainers)

File: `sa-module-governance/src/test/java/com/sa/governance/integration/ComplianceFlagIntegrationTest.java`

```java
// Test: Full compliance rule lifecycle -- create, query, update, verify event published
// Test: Full feature flag lifecycle -- create, cache, update via Maker-Checker, invalidate cache
// Test: RLS isolation -- tenant A compliance rules invisible to tenant B
// Test: RLS isolation -- tenant A feature flags invisible to tenant B
```

---

## Part 1: Compliance Matrix

### 1.1 Entity

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/ComplianceRuleEntity.java`

Maps to `t_compliance_rule`. Fields: id, tenantId, jurisdiction (Jurisdiction enum), category, ruleKey, ruleValue, description, effectiveFrom, effectiveTo, isStrictest.

### 1.2 Jurisdiction Enum

File: `sa-module-governance/src/main/java/com/sa/governance/domain/enums/Jurisdiction.java`

Values: `MGA`, `UKGC`, `CURACAO`, `PAGCOR`

### 1.3 Service

File: `sa-module-governance/src/main/java/com/sa/governance/service/compliance/ComplianceService.java`

Key methods:

- `getEffectiveRule(Jurisdiction jurisdiction, String category, String ruleKey)`: Query by jurisdiction + category + ruleKey, filtered by effective date range.

- `resolveStrictestRule(List<Jurisdiction> jurisdictions, String category, String ruleKey)`: For each jurisdiction, get effective rule. Apply **strictest-rule-wins** logic: for numeric limits, the lower value is stricter; for boolean restrictions, `true` (restricted) is stricter.

- `updateRule(ComplianceRuleEntity rule)`: Persist and publish `ComplianceRuleChangedEvent` to Spring event bus and Kafka topic.

### 1.4 Domain Event

File: `sa-module-governance/src/main/java/com/sa/governance/event/ComplianceRuleChangedEvent.java`

Published to Spring ApplicationEventPublisher and Kafka topic `compliance-rule-changes`.

### 1.5 Controller

File: `sa-module-governance/src/main/java/com/sa/governance/controller/compliance/ComplianceRuleController.java`

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /api/v1/compliance/rules` | GET | Query rules by jurisdiction + category (cross-domain API) |
| `GET /api/v1/compliance/rules/{id}` | GET | Get single rule by ID |
| `PUT /api/v1/compliance/rules/{id}` | PUT | Update rule (triggers event) |

### 1.6 Seed Data

File: `sa-module-governance/src/main/resources/db/migration/V007__seed_compliance_2026_deadlines.sql`

| Effective Date | Jurisdiction | Category | Rule Key | Description |
|----------------|-------------|----------|----------|-------------|
| 2026-01-19 | UKGC | RESPONSIBLE_GAMBLING | ban_mixed_product_incentives | Ban mixed-product incentives; wagering cap 10x |
| 2026-03-19 | UKGC | REPORTING | shareholder_reporting_threshold | Shareholder reporting threshold 3% to 5% |
| 2026-03-31 | PAGCOR | LICENSING | b2b_provider_accreditation_required | All B2B providers must be accredited |
| 2026-04-06 | UKGC | REGULATORY | digital_markets_act_alignment | Digital Markets Act alignment |
| 2026-06-30 | UKGC | FINANCIAL | rts12_financial_limit_changes | RTS 12 financial limit changes |

---

## Part 2: Feature Flag System

### 2.1 Entity

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/FeatureFlagEntity.java`

Maps to `t_feature_flag`. Fields: id, flagKey, tenantId (nullable), enabled, rolloutPercentage, metadata (JsonNode).

### 2.2 Service

File: `sa-module-governance/src/main/java/com/sa/governance/service/featureflag/FeatureFlagService.java`

Key methods:

- `isEnabled(String flagKey)`:
  1. Get current tenant ID from `TenantContext`
  2. Look up tenant's brand ID
  3. **Resolution order**: Redis cache -> tenant override -> brand default -> platform default
  4. Cache resolved value in Redis with 60-second TTL
  5. Apply `rolloutPercentage` via deterministic hash if set

- `updateFlag(String flagKey, FeatureFlagUpdateRequest request)`: Creates `PendingChange` via `MakerCheckerService`. Not applied immediately.

- `applyFlagChange(PendingChange change)`: Updates DB, invalidates Redis cache, publishes via Pub/Sub and Kafka.

### 2.3 Redis Cache Layer

File: `sa-module-governance/src/main/java/com/sa/governance/dao/cache/FeatureFlagCache.java`

- **Cache key pattern**: `ff:{tenantId}:{flagKey}`
- **TTL**: 60 seconds
- **Invalidation**: On change approval, `DEL` cache key + publish to Redis Pub/Sub channel `feature-flag-changes`
- **Pub/Sub listener**: All application instances subscribe and evict stale entries

### 2.4 Controller

File: `sa-module-governance/src/main/java/com/sa/governance/controller/featureflag/FeatureFlagController.java`

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /api/v1/flags/{key}` | GET | Check flag status for current tenant (cross-domain API) |
| `GET /api/v1/flags` | GET | List all flags for current tenant |
| `PUT /api/v1/flags/{key}` | PUT | Update flag (creates Maker-Checker pending change) |

---

## File Summary

**Production code:**

- `domain/enums/Jurisdiction.java`
- `dao/entity/ComplianceRuleEntity.java`
- `dao/entity/FeatureFlagEntity.java`
- `dao/mapper/ComplianceRuleMapper.java`
- `dao/mapper/FeatureFlagMapper.java`
- `dao/cache/FeatureFlagCache.java`
- `service/compliance/ComplianceService.java`
- `service/featureflag/FeatureFlagService.java`
- `event/ComplianceRuleChangedEvent.java`
- `event/FeatureFlagChangedEvent.java`
- `controller/compliance/ComplianceRuleController.java`
- `controller/featureflag/FeatureFlagController.java`
- `db/migration/V007__seed_compliance_2026_deadlines.sql`

**Test code:**

- `service/compliance/ComplianceServiceTest.java`
- `service/featureflag/FeatureFlagServiceTest.java`
- `integration/ComplianceFlagIntegrationTest.java`

---

## Key Design Decisions

1. **Strictest-rule-wins**: For cross-jurisdiction conflicts, the more restrictive rule applies. Lower numeric limits are stricter; `true` (restricted) beats `false`.
2. **Three-layer flag resolution**: Tenant -> Brand -> Platform. Mirrors the 3-layer config merge pattern.
3. **Redis cache with Pub/Sub invalidation**: 60s TTL + active invalidation on write for consistency.
4. **Maker-Checker on flag changes**: All mutations require approval to prevent unauthorized feature enablement.
5. **Compliance rules are tenant-scoped with RLS**: Each tenant has its own rules, seeded from jurisdiction defaults.
6. **Feature flag table access control**: Platform-level flags (`tenant_id = NULL`) are handled via service-layer logic since RLS would exclude them.
