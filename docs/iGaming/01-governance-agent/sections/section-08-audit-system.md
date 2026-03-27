# Section 08 -- Immutable Audit System

## Overview

This section implements the immutable, per-tenant hash-chain audit system for the Governance & Agent domain. It covers event creation and publishing via Kafka, hash chain computation and persistence, a Spring AOP aspect for automatic before/after capture, append-only enforcement at the database level, daily integrity verification, and a tiered storage archival pipeline.

The audit system is a critical compliance requirement: every sensitive action in the platform must be recorded in a tamper-evident log that regulators can inspect on demand.

**Plan section**: 11 (Immutable Audit System)

---

## Dependencies

| Dependency | What it provides |
|------------|-----------------|
| section-01-foundation | `t_audit_event` table schema, append-only trigger, RLS policy, Flyway migrations, base entity classes |
| section-02-tenant-isolation | `TenantContext`, Kafka header propagation of `X-Tenant-Id`, RLS enforcement |

This section does NOT depend on RBAC, agent hierarchy, commission, or settlement. It can be implemented in parallel with section-03 and section-07.

---

## Tests -- Write First

Testing stack: JUnit 5 + Mockito + Testcontainers (PostgreSQL + Kafka) + ArchUnit.

### Hash Chain Integrity Tests

File: `sa-module-governance/src/test/java/com/sa/governance/audit/AuditChainIntegrityTest.java`

```java
// Test: Audit event creates hash chain entry linked to previous event
// Test: Hash chain partitioned by tenant_id (independent chains)
// Test: ChainVerificationJob detects tampered event (hash mismatch)
// Test: Impersonation audit includes both actor and impersonator IDs
```

### Append-Only Enforcement Tests

File: `sa-module-governance/src/test/java/com/sa/governance/audit/AuditAppendOnlyTest.java`

```java
// Test: Append-only table -- UPDATE blocked by trigger
// Test: Append-only table -- DELETE blocked by trigger
```

### Kafka Partition Ordering Tests

File: `sa-module-governance/src/test/java/com/sa/governance/audit/AuditKafkaPartitionTest.java`

```java
// Test: Kafka partition by tenant_id ensures per-tenant ordering
```

### Archival Pipeline Tests

File: `sa-module-governance/src/test/java/com/sa/governance/audit/AuditArchivalTest.java`

```java
// Test: Hot-to-warm archival: correct row count + hash verification after move
// Test: Cold archive accessible within 24h on regulatory request
```

### AOP Aspect Tests

File: `sa-module-governance/src/test/java/com/sa/governance/audit/AuditedAspectTest.java`

```java
// Test: @Audited annotation on service method captures before/after state
// Test: @Audited captures correct action name from annotation parameter
// Test: @Audited captures resource type and resource ID from method parameters
```

---

## Implementation Details

### Package Layout

```
sa-module-governance/
  controller/audit/
    AuditController.java              # Read-only REST endpoints for audit queries
  service/audit/
    AuditService.java                 # Core service: log events, query history
    AuditChainWriter.java             # Kafka consumer: hash computation + DB write
    ChainVerificationJob.java         # Scheduled daily integrity check
  dao/
    entity/AuditEventEntity.java      # MyBatis entity for t_audit_event
    mapper/AuditEventMapper.java      # MyBatis-Plus mapper
  domain/
    AuditEvent.java                   # Domain value object
    AuditAction.java                  # Enum of auditable actions
  infrastructure/
    AuditedAnnotation.java            # @Audited annotation definition
    AuditedAspect.java                # Spring AOP @Audited aspect
    AuditArchivalJob.java             # Scheduled weekly archival pipeline
  config/
    AuditKafkaConfig.java             # Kafka producer/consumer configuration
```

### AuditEvent Domain Object

Immutable value object:
- `eventId` (UUID), `prevHash`, `eventHash`, `timestamp` (Instant, NTP-synchronized)
- `actorId` (UUID), `impersonatorId` (UUID, nullable), `tenantId` (Long)
- `action` (String), `resourceType`, `resourceId`, `changePayload` (JsonNode)

`eventHash` and `prevHash` are computed by the `AuditChainWriter` consumer, NOT by the caller.

### AuditAction Enum

All auditable action types:
- `AGENT_CREDIT_ADJUST`, `AGENT_CREDIT_DEDUCT`, `AGENT_CREDIT_RELEASE`
- `SETTLEMENT_APPROVE`, `SETTLEMENT_REJECT`, `SETTLEMENT_PAYOUT`
- `ROLE_CHANGE`, `PERMISSION_GRANT`, `PERMISSION_REVOKE`
- `TENANT_CONFIG_CHANGE`, `TENANT_PROVISION`, `TENANT_SUSPEND`
- `COMMISSION_AGREEMENT_CHANGE`, `COMPLIANCE_RULE_CHANGE`, `FEATURE_FLAG_CHANGE`
- `IMPERSONATION_START`, `IMPERSONATION_END`
- `MAKER_CHECKER_SUBMIT`, `MAKER_CHECKER_APPROVE`, `MAKER_CHECKER_REJECT`

### AuditService

**`log(AuditEvent)`**: Populate eventId, timestamp, tenantId (from TenantContext). Publish to Kafka topic `audit-events` with key = tenantId. Do NOT compute eventHash here.

**`queryByTenant/queryByActor/queryByResource`**: Query `t_audit_event` with pagination.

### AuditChainWriter (Kafka Consumer)

Per message:
1. Deserialize `AuditEvent` from Kafka.
2. Fetch latest `event_hash` for this `tenant_id` from `t_audit_event`.
3. Set `prevHash` (null for first event per tenant).
4. Compute `eventHash = SHA-256(prevHash + canonicalJson(eventData))` with deterministic JSON (sorted keys, compact format).
5. INSERT into `t_audit_event`.
6. Commit Kafka offset only after successful DB insert.

**Concurrency**: One consumer per partition. All events for a tenant go to the same partition, so hash chain computation is single-threaded per tenant.

**Error handling**: DB failure = don't commit offset (Kafka redelivers). Duplicate = `ON CONFLICT DO NOTHING` (UUID PK). Bad message = send to `audit-events-dlq`.

### @Audited Annotation and AOP Aspect

`@Audited(action = "...", resourceType = "...", resourceIdParam = "id")` on service methods.

AOP `@Around` aspect:
1. BEFORE: Load entity state as "before" snapshot.
2. Execute method.
3. AFTER: Load "after" snapshot. Build changePayload as JSON diff. Call `AuditService.log()`.
4. On exception: Log event with `_FAILED` suffix and exception message.

### ChainVerificationJob

Scheduled daily (3 AM). For each active tenant:
1. SELECT latest 1000 events.
2. Re-compute hash chain. Compare with stored values.
3. On mismatch: P0 alert (`ChainIntegrityViolationEvent`).
4. Monthly: verify hot/warm boundary (last warm event hash = first hot event prevHash).

### AuditArchivalJob

Scheduled weekly (Sunday 2 AM). Tiered storage:

| Tier | Retention | Storage | Format |
|------|-----------|---------|--------|
| Hot | 90 days | PostgreSQL | Rows |
| Warm | 1 year | S3 | Parquet (partitioned by tenant/year/month) |
| Cold | 7 years | S3 Glacier | Parquet |

Hot-to-warm: Export to Parquet, verify row count + hash integrity, delete from PostgreSQL via dedicated archival role (bypasses append-only trigger).

### Kafka Configuration

- Topic: `audit-events`, partitioned by `tenant_id`
- Producer: `acks=all`, idempotence enabled, JSON serializer
- Consumer: group `audit-chain-writer`, manual commit, `max.poll.records=100`
- DLQ: `audit-events-dlq`
- Partition count: start with 16, scale with tenant count
- Retention: 7 days (events are persisted to PostgreSQL)

---

## Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Hash chain per tenant | Independent chains | Eliminates cross-node ordering bottleneck |
| Hash computation location | Kafka consumer | Ensures strict ordering via partition-level processing |
| Canonical JSON | Jackson with sorted keys | Deterministic hash reproduction |
| Append-only enforcement | PostgreSQL trigger | Cannot be bypassed by application code |
| Tiered storage | Hot 90d / Warm 1yr / Cold 7yr | Balances performance, cost, and compliance |
| Archival deletion | Dedicated PostgreSQL role | Separation of duties from app_user |

---

## Monitoring and Alerting

| Check | Frequency | Alert |
|-------|-----------|-------|
| Chain head integrity | Every 10 min | P0 on mismatch |
| Full chain integrity | Daily 3 AM | P0 on mismatch |
| Hot/warm boundary | Monthly | P0 on mismatch |
| Kafka consumer lag | Continuous | Warning if >1000 |
| DLQ depth | Continuous | Alert if any messages |

---

## Implementation Checklist

1. Write all test stubs (5 test files).
2. Create `AuditEvent` domain value object and `AuditAction` enum.
3. Create `AuditEventEntity` and `AuditEventMapper`.
4. Implement `AuditKafkaConfig`.
5. Implement `AuditService`.
6. Implement `AuditChainWriter`.
7. Implement `@Audited` annotation and `AuditedAspect`.
8. Implement `ChainVerificationJob`.
9. Implement `AuditArchivalJob`.
10. Implement `AuditController`.
11. Run all tests and verify green.
