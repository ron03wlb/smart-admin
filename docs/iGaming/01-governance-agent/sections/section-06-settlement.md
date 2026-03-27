# Section 06 -- Settlement System

## Overview

This section implements the **Settlement System** for the Governance & Agent Domain. Settlement is the process by which calculated commissions are finalized, approved, paid out, and monitored for anomalies. It operates as a batch workflow orchestrated by `SettlementOrchestrator`, with idempotent per-agent calculation, tiered approval routing, automatic retry on payout failure, and post-calculation anomaly detection.

**Plan sections covered**: 8.1-8.3 (Settlement), 18 (Settlement Batch and Idempotency), plus relevant parts of 15.2 (Settlement Failures).

**Dependencies**: This section depends on:
- **section-01-foundation** -- Maven module structure, base entity classes, Flyway migration framework, PostgreSQL schema for `t_settlement_record` and `t_settlement_batch`
- **section-02-tenant-isolation** -- Tenant context propagation (TenantContext, RLS), all settlement data is tenant-scoped
- **section-04-agent-hierarchy** -- Agent entity model, agent tree traversal for settlement eligibility
- **section-05-commission-engine** -- `CommissionEngine.calculateTotal(agent, period)`, `CommissionLedger` for carry-forward lookup

**Blocks**: section-10-integration (cross-domain API contracts, `SettlementCompletedEvent` publishing)

---

## Tests (Write These First)

All tests use JUnit 5 + Mockito + Testcontainers (PostgreSQL + Redis).

### 1.1 Settlement Batch and Workflow Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/settlement/SettlementOrchestratorTest.java`

```java
// Test: Settlement batch created with RUNNING status
// Test: Idempotent calculation -- existing record for (agent, period) is skipped
// Test: Amount < $10K auto-approved
// Test: Amount $10K-$50K routed to Manager queue
// Test: Amount $50K-$100K routed to CFO queue
// Test: Amount >= $100K requires CFO + CEO dual approval
// Test: Payout failure triggers auto-retry (up to 3x over 3 business days)
// Test: After 3 failures -> status ESCALATED, manual queue alert
// Test: Crash recovery -- partial batch resumes correctly
```

### 1.2 Settlement Batch Idempotency Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/settlement/SettlementBatchIdempotencyTest.java`

```java
// Test: Batch idempotency -- same period_key does not create duplicate batch
// Test: Agent with PENDING_REVIEW record is skipped on reprocessing
// Test: Agent with APPROVED record is skipped on reprocessing
// Test: Agent with FAILED record IS reprocessed
// Test: Input checksum captures snapshot of calculation inputs
```

### 1.3 Anomaly Detection Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/settlement/SettlementMonitorServiceTest.java`

```java
// Test: Monthly total >= $500K triggers upgrade review
// Test: Weekly growth > 200% vs 4-week average triggers FLAG
// Test: New agent first-month >= $50K triggers auto FLAG
```

### 1.4 Multi-Currency Settlement Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/settlement/MultiCurrencySettlementTest.java`

```java
// Test: Settlement calculated in agent's configured currency
// Test: FX rate sourced from 02-funding API
// Test: Bet amounts converted at bet-time exchange rate
```

### 1.5 Settlement Failure Handling Tests

File: `sa-module-governance/src/test/java/com/sa/governance/service/settlement/SettlementFailureHandlingTest.java`

```java
// Test: Calculation error halts settlement for that agent only
// Test: Approval timeout sends reminder at 50% SLA elapsed
// Test: After SLA breach, auto-escalate to next approval tier
```

---

## Data Model

### SettlementBatch Entity

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/SettlementBatchEntity.java`

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

### SettlementRecord Entity

File: `sa-module-governance/src/main/java/com/sa/governance/dao/entity/SettlementRecordEntity.java`

Status values (enum `SettlementStatus`):
- `PENDING_REVIEW` -- Calculated, awaiting approval
- `APPROVED` -- Approved, ready for payout
- `COMPLETED` -- Payout succeeded
- `FAILED` -- Calculation or payout failed (eligible for reprocessing)
- `ESCALATED` -- Retry exhausted, moved to manual finance queue

---

## Implementation Details

### SettlementOrchestrator (Manager Layer)

File: `sa-module-governance/src/main/java/com/sa/governance/manager/SettlementOrchestrator.java`

**Method: `runSettlement(SettlementCycle cycle)`**

1. **Freeze the period** -- No more commission entries accepted for the period being settled.
2. **Create or resume SettlementBatch** -- Idempotent: if batch exists with COMPLETED, return; if RUNNING/FAILED_PARTIAL, resume.
3. **For each eligible agent**:
   - Check if `SettlementRecord` exists for `(agent_id, period_key)`:
     - COMPLETED/PENDING_REVIEW/APPROVED: **skip** (idempotent)
     - FAILED: **reprocess**
     - No record: **calculate**
   - Call `CommissionEngine.calculateTotal(agent, period)`
   - Apply negative carry-forward from previous period
   - Create `SettlementRecord` with net amount
4. **Route to approval tier**:

   | Amount Range | Approval Tier | SLA |
   |---|---|---|
   | < $10,000 | AUTO | Immediate |
   | $10,000 - $49,999.99 | MANAGER | 4 hours |
   | $50,000 - $99,999.99 | CFO | 24 hours |
   | >= $100,000 | CFO_CEO | 48 hours |

5. **On approval**: trigger payout via 02-funding `POST /api/v1/payments/payout`
6. **On payout success**: status -> COMPLETED, publish `SettlementCompletedEvent`
7. **On payout failure**: retry up to 3x over 3 business days, then ESCALATED
8. **Batch completion**: COMPLETED or FAILED_PARTIAL

**Error handling per agent**: Single agent failure does not abort the batch.

### SettlementMonitorService

File: `sa-module-governance/src/main/java/com/sa/governance/service/settlement/SettlementMonitorService.java`

Post-calculation anomaly detection:

| Metric | Default Threshold | Action |
|---|---|---|
| Single agent monthly total | >= $500,000 | Raise `UPGRADE_REVIEW` flag |
| Single agent weekly growth | > 200% vs 4-week average | Raise `GROWTH_ANOMALY` flag |
| New agent first-month settlement | >= $50,000 | Raise `NEW_AGENT_HIGH_SETTLEMENT` flag |

### Multi-Currency Settlement

- Agent settlement currency is fixed per `CommissionAgreement`
- Bet amounts converted at **bet-time exchange rate** (not settlement-time)
- FX rates sourced from 02-funding: `GET /api/v1/fx/rate/{from}/{to}`

### Approval SLA Monitoring

- At 50% SLA elapsed: send reminder notification
- At 100% SLA breach: auto-escalate to next tier

### Cron Triggers

- **Weekly**: Monday 00:00 UTC
- **Monthly**: 1st of month 00:00 UTC

---

## Key File Paths

| Purpose | Path |
|---|---|
| SettlementBatch entity | `dao/entity/SettlementBatchEntity.java` |
| SettlementRecord entity | `dao/entity/SettlementRecordEntity.java` |
| SettlementOrchestrator | `manager/SettlementOrchestrator.java` |
| SettlementMonitorService | `service/settlement/SettlementMonitorService.java` |
| SettlementController | `controller/settlement/SettlementController.java` |
| SettlementCompletedEvent | `event/SettlementCompletedEvent.java` |

---

## External Integrations (Consumed APIs)

| API | Provider | Purpose |
|---|---|---|
| `POST /api/v1/payments/payout` | 02-funding | Execute settlement payout |
| `GET /api/v1/fx/rate/{from}/{to}` | 02-funding | Fetch exchange rate |

---

## Implementation Checklist

1. Write all test stubs (5 test files listed above)
2. Create enums: `BatchStatus`, `SettlementStatus`, `ApprovalTier`, `SettlementCycle`, `AnomalyType`
3. Create `SettlementBatchEntity` and `SettlementRecordEntity`
4. Create MyBatis mappers for both entities
5. Implement `SettlementOrchestrator.runSettlement()` with full idempotent workflow
6. Implement approval tier routing logic
7. Implement payout retry logic (3x over 3 business days, then ESCALATED)
8. Implement `SettlementMonitorService` with 3 anomaly detection rules
9. Implement multi-currency settlement helper
10. Implement approval SLA monitoring (50% reminder, 100% escalation)
11. Create `SettlementCompletedEvent` and publish on payout success
12. Configure cron triggers
13. Run all tests green
