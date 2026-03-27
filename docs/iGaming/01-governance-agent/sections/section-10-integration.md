# Section 10: Integration -- Cross-Domain APIs, Resilience, Monitoring, Versioning, Migrations, Keycloak HA, and DR

## Overview

This is the **final integration section**. It wires together all previously implemented subsystems (sections 01-09) by exposing cross-domain REST APIs, defining event contracts, configuring resilience patterns, establishing monitoring/alerting, and specifying operational infrastructure.

**Depends on**: All previous sections (01 through 09 must be complete).

---

## Tests

### Cross-Domain API Contract Tests (Spring Cloud Contract + REST Assured)

```java
// Test: GET /api/v1/tenants/{id}/context -- returns tenant config
// Test: POST /api/v1/rbac/check -- returns permission boolean
// Test: GET /api/v1/compliance/rules -- returns rules by jurisdiction
// Test: GET /api/v1/agents/{id}/credit -- returns credit summary
// Test: POST /api/v1/agents/credit/deduct -- deducts credit
// Test: POST /api/v1/agents/credit/release -- releases credit
// Test: GET /api/v1/agents/player/{playerId} -- returns agent relationship
// Test: GET /api/v1/flags/{key} -- returns flag status
```

### Infrastructure Tests

```java
// Test: Flyway migrations apply to both shared and dedicated databases
// Test: Every new table has RLS policy (CI validation query returns 0 rows)
// Test: CreditRecoveryService rebuilds Redis from WAL entries after failure
// Test: Credit reconciliation detects and resolves DB/Redis mismatch
// Test: Keycloak failover -- existing sessions continue after primary node failure
```

---

## 1. Cross-Domain REST API Endpoints (8 Exposed)

| Endpoint | Method | Consumer | Delegates To |
|----------|--------|----------|--------------|
| `/api/v1/tenants/{id}/context` | GET | All domains | TenantService (section-02) |
| `/api/v1/rbac/check` | POST | All domains | PermissionCheckService (section-03) |
| `/api/v1/compliance/rules` | GET | 03-player, 05-risk, 06-frontend | ComplianceService (section-07) |
| `/api/v1/agents/{id}/credit` | GET | 02-funding | AgentCreditService (section-04) |
| `/api/v1/agents/credit/deduct` | POST | 02-funding | AgentCreditService (section-04) |
| `/api/v1/agents/credit/release` | POST | 02-funding | AgentCreditService (section-04) |
| `/api/v1/agents/player/{playerId}` | GET | 02-funding, 04-gaming | AgentService (section-04) |
| `/api/v1/flags/{key}` | GET | All domains | FeatureFlagService (section-07) |

Controllers: `controller/api/TenantApiController.java`, `RbacApiController.java`, `ComplianceApiController.java`, `AgentCreditApiController.java`, `AgentPlayerApiController.java`, `FeatureFlagApiController.java`

---

## 2. Consumed APIs (Outbound, with Circuit Breakers)

| Endpoint | Provider | Purpose |
|----------|----------|---------|
| `GET /api/v1/players/{id}/self-exclusion` | 03-player | Check exclusion before credit ops |
| `GET /api/v1/fx/rate/{from}/{to}` | 02-funding | FX rate for multi-currency settlement |
| `GET /api/v1/risk/score/{entityId}` | 05-risk | Risk assessment |
| `POST /api/v1/payments/payout` | 02-funding | Settlement payout execution |

Client wrappers: `infrastructure/client/PlayerServiceClient.java`, `FundingServiceClient.java`, `RiskServiceClient.java`

---

## 3. Event Bus (Kafka)

7 published event types, all partitioned by `tenant_id`:

| Event | Trigger | Subscribers |
|-------|---------|-------------|
| `TenantCreatedEvent` | Tenant provisioned | All domains |
| `TenantSuspendedEvent` | Billing/compliance suspension | All domains |
| `CreditDeductedEvent` | Credit used for bet | 02-funding |
| `CreditReleasedEvent` | Credit returned after settlement | 02-funding |
| `SettlementCompletedEvent` | Commission payout | 02-funding |
| `ComplianceRuleChangedEvent` | Jurisdiction rule updated | All domains |
| `FeatureFlagChangedEvent` | Flag toggled | All domains |

Inbound: `PlayerSelfExclusionEvent` from 03-player.

---

## 4. Resilience (Resilience4j)

File: `config/ResilienceConfig.java`

Circuit breakers on all outbound synchronous calls. Default timeout: 5s.

**Credit Operation Failure Matrix**:

| Failure | Response | Recovery |
|---------|----------|----------|
| Redis unavailable | Fall back to DB with optimistic lock | Auto-reconnect; rebuild cache |
| DB transaction conflict | Retry with backoff (max 3x) | 503 if still failing |
| Credit > available | `INSUFFICIENT_CREDIT` immediately | No retry |

**Settlement Failure Matrix**:

| Failure | Response | Recovery |
|---------|----------|----------|
| Calculation error | Halt for that agent only | Manual investigation |
| Payment API failure | Retry 1 business day (max 3x) | Escalate to manual queue |
| Approval timeout | Reminder at 50% SLA | Auto-escalate at 100% |

---

## 5. API Versioning

| Rule | Policy |
|------|--------|
| URL prefix | `/api/v1/...` |
| Breaking changes | New version (v2) |
| Co-existence | 90 days minimum |
| Deprecation notice | 30 days before removal |
| Contract tests | Spring Cloud Contract required before merge |

---

## 6. Migration Validation

CI step runs after all Flyway migrations:

```sql
SELECT tablename FROM pg_tables
WHERE schemaname = 'public'
AND tablename LIKE 't_%'
AND tablename NOT IN (SELECT tablename FROM pg_policies);
-- Must return 0 rows
```

Test: `MigrationRlsValidationTest.java` (Testcontainers)

---

## 7. Keycloak High Availability

| Component | Configuration |
|-----------|--------------|
| Keycloak nodes | Min 2, active-active behind LB |
| Session cache | Infinispan clustered |
| Health check | `/health/ready`, 30s interval |

**Degraded mode**: Existing sessions continue (local JWT validation). New logins blocked. Sensitive ops blocked. Tenant provisioning queued.

---

## 8. Monitoring and Observability

File: `config/MonitoringConfig.java`

| Metric | Target | Alert |
|--------|--------|-------|
| Permission check p99 | <10ms | >20ms |
| Credit deduction p99 | <50ms | >100ms |
| RLS overhead | <5% | >8% |
| Unauthorized cross-tenant | 0 | Any = P0 |
| Audit chain integrity | 100% | Any break = P0 |
| Settlement on-time | >95% | <90% |

**5 Health Checks**:

| Check | Frequency |
|-------|-----------|
| RLS policy active | Every 5 min |
| Redis permission cache | Every 1 min |
| Audit chain head | Every 10 min |
| Keycloak connectivity | Every 30 sec |
| Credit cache consistency | Every 5 min |

---

## 9. Disaster Recovery

**Backup Strategy**:

| Component | Frequency | Retention | Tool |
|-----------|-----------|-----------|------|
| PostgreSQL | Continuous WAL + daily base | 30 days | WAL-G |
| Redis | AOF (everysec) + hourly RDB | 7 days | Native |
| Keycloak DB | Same as PostgreSQL | 30 days | WAL-G |

**Recovery Procedures**:

| Scenario | RTO |
|----------|-----|
| Redis failure | <5 min (WAL replay) |
| Audit chain break | <4 hr (P0 investigation) |
| Credit DB/Redis divergence | <30 min (reconciliation) |
| Full PostgreSQL failure | <15 min (WAL-G restore) |
| Keycloak failure | <2 min (failover) |

**CreditReconciliationJob**: Runs every 5 min, samples credit records, compares Redis vs DB. On divergence: freeze agents, replay WAL, rebuild Redis, unfreeze.

---

## File Summary

**Controllers**: `TenantApiController`, `RbacApiController`, `ComplianceApiController`, `AgentCreditApiController`, `AgentPlayerApiController`, `FeatureFlagApiController`

**Clients**: `PlayerServiceClient`, `FundingServiceClient`, `RiskServiceClient`

**Config**: `ResilienceConfig`, `MonitoringConfig`, `KeycloakHealthIndicator`

**Manager**: `CreditReconciliationJob`

**Tests**: 6 contract tests, `MigrationRlsValidationTest`, `KeycloakFailoverTest`, `CreditRecoveryIntegrationTest`, `HealthCheckTest`

**Contract DSLs**: 8 Spring Cloud Contract files (one per endpoint)

---

## Implementation Checklist

1. Write all contract test stubs and infrastructure test stubs.
2. Create 6 cross-domain API controllers (thin facades delegating to existing services).
3. Create 3 outbound client wrappers with Resilience4j circuit breakers.
4. Configure `ResilienceConfig` with circuit breaker settings.
5. Consolidate event publishing in `GovernanceEventPublisher` (7 event types).
6. Implement `MigrationRlsValidationTest` (CI enforcement).
7. Configure `KeycloakHealthIndicator`.
8. Implement `MonitoringConfig` with Micrometer metrics.
9. Implement `CreditReconciliationJob`.
10. Create Spring Cloud Contract DSL files for all 8 endpoints.
11. Run full test suite green.
