# Section 05 -- Wallet Reconciliation

> **Module**: `wallet-reconciliation`
> **Depends on**: section-01-foundation (base entities, Money VO, test infra), section-02-wallet-core (Wallet entity, WalletRepository, balance operations)
> **Tech stack**: Java 17+ / Spring Boot 3.x / PostgreSQL / Redis / Kafka
> **Key invariant**: GP is always authoritative for reconciliation -- both amount and existence disputes (INV-8)

---

## 1. Overview

The wallet-reconciliation module ensures that the platform's internal transaction ledger remains consistent with every Game Provider's (GP) canonical records. It operates at three layers -- real-time bookkeeping, hourly cross-referencing, and daily aggregation -- with automated discrepancy resolution and safeguards against mass-reversal incidents.

The module also owns scheduled orphan-round detection: rounds that remain OPEN beyond a configurable timeout are probed against the GP API and either auto-closed or escalated.

### Module Structure

```
wallet-reconciliation/
  service/              # RealtimeReconciler, HourlyReconciler, DailyReconciler
  detector/             # OrphanRoundDetector, DiscrepancyResolver
  scheduler/            # Scheduled recon jobs (cron-driven)
```

---

## 2. Tests (Write These First)

All tests derive from the TDD plan. Implement these before any production code.

### 2.1 Three-Layer Model Tests (HourlyReconcilerTest / DiscrepancyResolverTest)

Seven tests covering Layer 2 hourly reconciliation and its safety mechanisms.

```java
// ── File: wallet-reconciliation/src/test/java/com/funding/reconciliation/service/HourlyReconcilerTest.java

/**
 * Test: Layer 2 -- Amount mismatch <$1 -> auto-correct
 *
 * Given a platform transaction of $100.30 and a GP record of $100.05 (diff = $0.25),
 * when the hourly reconciler processes this pair,
 * then the platform transaction amount is auto-corrected to match the GP record
 * and a MINOR_ADJUSTMENT audit entry is created.
 */
@Test
void shouldAutoCorrectWhenAmountMismatchUnderOneDollar() { }

/**
 * Test: Layer 2 -- Amount mismatch >$100 -> manual alert
 *
 * Given a platform transaction of $500.00 and a GP record of $350.00 (diff = $150),
 * when the hourly reconciler processes this pair,
 * then no auto-correction occurs,
 * and a MANUAL_REVIEW discrepancy alert is raised with severity HIGH,
 * and the discrepancy record includes both amounts and the GP report snapshot.
 */
@Test
void shouldRaiseManualAlertWhenAmountMismatchOverOneHundredDollars() { }

/**
 * Test: Layer 2 -- Time mismatch <5 min -> auto-match
 *
 * Given a platform transaction at 14:00:00 and a GP record at 14:03:30 (diff = 3.5 min),
 * when the hourly reconciler attempts to match records,
 * then they are treated as the same transaction (auto-matched)
 * and no discrepancy is flagged.
 */
@Test
void shouldAutoMatchWhenTimeMismatchUnderFiveMinutes() { }

/**
 * Test: Layer 2 -- Missing on platform -> create transaction from GP record
 *
 * Given a GP record with transactionId "gp-tx-999" that has no matching platform record,
 * when the hourly reconciler processes this GP record,
 * then a new platform transaction is created from the GP record (GP is authoritative),
 * and the wallet balance is adjusted accordingly,
 * and a GP_AUTHORITATIVE_CREATE audit entry is persisted.
 */
@Test
void shouldCreateTransactionFromGpRecordWhenMissingOnPlatform() { }

/**
 * Test: Layer 2 -- Missing on GP -> reverse platform transaction
 *
 * Given a platform transaction "plat-tx-123" that has no matching GP record,
 * when the hourly reconciler processes the orphaned platform record,
 * then the platform transaction is reversed (GP is authoritative),
 * and the wallet balance is restored,
 * and a GP_AUTHORITATIVE_REVERSAL audit entry with the GP report snapshot is persisted.
 */
@Test
void shouldReversePlatformTransactionWhenMissingOnGp() { }
```

```java
// ── File: wallet-reconciliation/src/test/java/com/funding/reconciliation/detector/ReversalCircuitBreakerTest.java

/**
 * Test: Reversal circuit breaker -- >N reversals per GP/hour -> pause + escalate
 *
 * Given the max_reversals_per_gp_per_hour threshold is configured as 10,
 * when a single GP's hourly reconciliation triggers 11 reversals,
 * then auto-reversal is paused for that GP,
 * and a CIRCUIT_BREAKER_TRIPPED escalation event is published,
 * and subsequent reversals for that GP are queued for manual review.
 */
@Test
void shouldPauseAndEscalateWhenReversalCountExceedsThreshold() { }

/**
 * Test: Reversal preserves GP report snapshot for audit
 *
 * Given an auto-reversal is executed for platform transaction "plat-tx-456",
 * when the reversal completes,
 * then the GP report data that triggered the reversal is stored as a JSON snapshot,
 * and the snapshot is linked to the reversal audit record,
 * and the snapshot includes: gpId, reportTimestamp, reportedTransactions, rawPayload.
 */
@Test
void shouldPreserveGpReportSnapshotOnReversal() { }
```

### 2.2 Orphan Round Detection Tests (OrphanRoundDetectorTest)

Four tests covering the scheduled orphan-round scanner.

```java
// ── File: wallet-reconciliation/src/test/java/com/funding/reconciliation/detector/OrphanRoundDetectorTest.java

/**
 * Test: Scan finds OPEN rounds older than 2 hours
 *
 * Given 5 rounds in the database:
 *   - 2 with status OPEN and updated_at 3 hours ago,
 *   - 1 with status OPEN and updated_at 30 minutes ago,
 *   - 1 with status CLOSED,
 *   - 1 with status PENDING_REVIEW,
 * when the orphan detector runs,
 * then exactly 2 rounds are identified as orphans.
 */
@Test
void shouldFindOpenRoundsOlderThanTwoHours() { }

/**
 * Test: GP API returns result -> auto-close round
 *
 * Given an orphan round "round-001" in OPEN state,
 * and the GP round-status API returns a WIN settlement of $50.00,
 * when the orphan detector processes this round,
 * then the settlement is applied (player credited $50.00),
 * and the round transitions to CLOSED,
 * and an ORPHAN_AUTO_CLOSED audit entry is created.
 */
@Test
void shouldAutoCloseRoundWhenGpApiReturnsResult() { }

/**
 * Test: GP API fails -> mark PENDING_REVIEW
 *
 * Given an orphan round "round-002" in OPEN state,
 * and the GP round-status API returns a timeout/error,
 * when the orphan detector processes this round,
 * then the round transitions to PENDING_REVIEW,
 * and the failure reason is recorded.
 */
@Test
void shouldMarkPendingReviewWhenGpApiFails() { }

/**
 * Test: $1,000+ orphan -> creates priority CS ticket
 *
 * Given an orphan round "round-003" with total bet amount $1,500,
 * when the orphan detector processes this round,
 * then a priority CS ticket is created with 24-hour SLA,
 * regardless of the GP API call outcome.
 */
@Test
void shouldCreatePriorityTicketForHighValueOrphan() { }
```

---

## 3. Implementation Details

### 3.1 Three-Layer Reconciliation Model

#### Layer 1 -- Real-time Ledger

Every Seamless Wallet API call updates a `realtime_ledger` table. This table is the source of truth for current balances and serves as the platform-side dataset for hourly comparison.

No reconciliation logic runs at this layer; it is purely bookkeeping. The `RealtimeReconciler` service is responsible for writing ledger entries transactionally alongside wallet balance mutations (this happens inside the transaction processor from section-03, but the ledger entity and repository live in this module).

#### Layer 2 -- Hourly Reconciliation

A scheduled job runs every hour. It compares platform transaction records (from `realtime_ledger`) against GP-provided transaction reports. GPs either push reports or the platform pulls via the GP Reporting API.

**Discrepancy resolution rules** (GP is always authoritative):

| Discrepancy Type | Condition | Action |
|------------------|-----------|--------|
| Amount mismatch | Difference < $1 | Auto-correct platform record to match GP |
| Amount mismatch | Difference > $100 | Raise manual alert; no auto-correction |
| Amount mismatch | $1 <= difference <= $100 | Auto-correct + log for review |
| Time mismatch | Difference < 5 minutes | Auto-match (treat as same transaction) |
| Missing on platform | GP has record, platform does not | Create transaction from GP record |
| Missing on GP | Platform has record, GP does not | Reverse the platform transaction |
| Volume anomaly | > 10 missing transactions/hour for one GP | Alert: check GP API health |

#### Layer 3 -- Daily Analytics

A batch job runs at 02:00 UTC daily. It aggregates hourly reconciliation results into the data warehouse for daily player summaries and reporting. The `DailyReconciler` produces aggregate match-rate metrics consumed by monitoring dashboards.

### 3.2 Discrepancy Resolver

The `DiscrepancyResolver` encapsulates the resolution logic from the table above. It receives a `DiscrepancyRecord` (containing platform record, GP record, mismatch type) and returns a `ResolutionAction`.

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/detector/DiscrepancyResolver.java

public class DiscrepancyResolver {

    /**
     * Resolve a single discrepancy between platform and GP records.
     *
     * @param discrepancy the detected mismatch (contains both sides + type)
     * @return the resolution action to execute (AUTO_CORRECT, REVERSE, CREATE, MANUAL_REVIEW)
     */
    public ResolutionAction resolve(DiscrepancyRecord discrepancy) { ... }
}
```

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/detector/DiscrepancyRecord.java

public record DiscrepancyRecord(
    String platformTransactionId,   // null if missing on platform
    String gpTransactionId,         // null if missing on GP
    Money platformAmount,           // null if missing on platform
    Money gpAmount,                 // null if missing on GP
    Instant platformTimestamp,      // null if missing on platform
    Instant gpTimestamp,            // null if missing on GP
    String gpId,
    DiscrepancyType type            // AMOUNT_MISMATCH, TIME_MISMATCH, MISSING_ON_PLATFORM, MISSING_ON_GP
) {}
```

```java
public enum ResolutionAction {
    AUTO_CORRECT,       // adjust platform record to match GP
    REVERSE,            // reverse the platform-only transaction
    CREATE_FROM_GP,     // create platform transaction from GP record
    MANUAL_REVIEW,      // escalate to human operator
    AUTO_MATCH          // time mismatch within tolerance; no action needed
}
```

### 3.3 Reversal Circuit Breaker

Safeguard against GP data quality issues (compromised or buggy GP reporting APIs). Without this, a single bad GP report could trigger mass reversal of legitimate transactions.

**Behavior:**
- Track reversal count per GP per hour in a sliding window (Redis counter with 1-hour TTL).
- Threshold is DB-configurable via `funding.payment.recon.max_reversals_per_gp_per_hour` (default: 10).
- When the count exceeds the threshold for a given GP:
  1. Auto-reversal is **paused** for that GP for the remainder of the hour.
  2. A `CIRCUIT_BREAKER_TRIPPED` event is published to Kafka.
  3. All pending reversals for that GP are queued for manual review.
  4. An escalation notification is sent to the operations team.
- The circuit breaker resets at the start of the next hourly window.

**Audit requirement:** Every auto-reversal must persist a snapshot of the GP report that triggered it. The snapshot is stored as a JSON column in the `reconciliation_audit` table and includes: `gpId`, `reportTimestamp`, `reportedTransactions` (the full list from the GP report), and `rawPayload`.

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/detector/ReversalCircuitBreaker.java

public class ReversalCircuitBreaker {

    /**
     * Check whether auto-reversals are allowed for the given GP.
     *
     * @param gpId the game provider identifier
     * @return true if reversals are permitted; false if threshold is exceeded
     */
    public boolean isReversalAllowed(String gpId) { ... }

    /**
     * Record a reversal event for the given GP, incrementing the sliding window counter.
     *
     * @param gpId the game provider identifier
     */
    public void recordReversal(String gpId) { ... }

    /**
     * Pause all auto-reversals for a GP and publish escalation event.
     *
     * @param gpId the game provider identifier
     */
    public void tripBreaker(String gpId) { ... }
}
```

### 3.4 Orphan Round Detection

A scheduled job runs every 15 minutes (configurable via `funding.wallet.orphan_scan_interval_minutes`, default: 15). It scans for rounds in OPEN state that have not been updated within the timeout window (configurable via `funding.wallet.orphan_timeout_hours`, default: 2).

**Query:**

```sql
SELECT * FROM rounds
WHERE status = 'OPEN'
  AND updated_at < NOW() - INTERVAL '2 hours'
```

**Processing logic per orphan round:**

1. Call the GP's round status API with the round identifier.
2. **GP returns settlement result** -- process the settlement (credit/debit the player), transition the round to CLOSED, and create an `ORPHAN_AUTO_CLOSED` audit entry.
3. **GP API fails or times out** -- transition the round to PENDING_REVIEW. The round will be retried on the next scan.
4. **Round amount > $1,000** -- regardless of the GP API outcome, create a priority CS ticket with a 24-hour SLA.

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/detector/OrphanRoundDetector.java

public class OrphanRoundDetector {

    /**
     * Scan for orphan rounds and process each one.
     * Called by the scheduler every N minutes.
     */
    public void detectAndResolve() { ... }

    /**
     * Process a single orphan round: query GP, apply settlement or escalate.
     *
     * @param round the orphan round entity
     * @return the resolution outcome (AUTO_CLOSED, PENDING_REVIEW, ESCALATED)
     */
    public OrphanResolution processOrphan(Round round) { ... }
}
```

```java
public enum OrphanResolution {
    AUTO_CLOSED,       // GP returned result; round settled and closed
    PENDING_REVIEW,    // GP API failed; round awaits next scan
    ESCALATED          // high-value round; priority CS ticket created
}
```

### 3.5 GP Report Snapshot Entity

Every auto-reversal and GP-authoritative action stores the GP report data for audit and potential rollback.

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/model/GpReportSnapshot.java

/**
 * Immutable snapshot of the GP report that triggered a reconciliation action.
 * Stored as a JSON column in reconciliation_audit for traceability.
 */
public record GpReportSnapshot(
    String gpId,
    Instant reportTimestamp,
    List<GpTransactionRecord> reportedTransactions,
    String rawPayload       // full GP response body preserved verbatim
) {}
```

### 3.6 Reconciliation Audit Entity

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/model/ReconciliationAudit.java

/**
 * Audit trail for every reconciliation action taken by the system.
 */
@Entity
@Table(name = "reconciliation_audit")
public class ReconciliationAudit extends BaseEntity {

    private String gpId;
    private String platformTransactionId;
    private String gpTransactionId;

    @Enumerated(EnumType.STRING)
    private ResolutionAction action;           // AUTO_CORRECT, REVERSE, CREATE_FROM_GP, MANUAL_REVIEW, AUTO_MATCH

    @Enumerated(EnumType.STRING)
    private ReconciliationLayer layer;         // REALTIME, HOURLY, DAILY

    private Money platformAmount;
    private Money gpAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    private GpReportSnapshot gpReportSnapshot; // nullable; present for REVERSE and CREATE_FROM_GP

    private String notes;
}
```

### 3.7 Reconciliation Scheduling

All scheduled jobs use Spring `@Scheduled` with cron expressions. Intervals are DB-configurable via the shared config parameter reader (section-01).

| Job | Schedule | Class | Description |
|-----|----------|-------|-------------|
| Hourly reconciliation | `0 0 * * * *` (top of every hour) | `HourlyReconciler` | Compare platform vs GP records; resolve discrepancies |
| Orphan round scan | `0 */15 * * * *` (every 15 minutes) | `OrphanRoundDetector` | Find and process OPEN rounds > 2 hours old |
| Daily analytics | `0 0 2 * * *` (02:00 UTC daily) | `DailyReconciler` | Aggregate to data warehouse; compute match-rate metrics |

```java
// ── File: wallet-reconciliation/src/main/java/com/funding/reconciliation/scheduler/ReconciliationScheduler.java

@Component
public class ReconciliationScheduler {

    private final HourlyReconciler hourlyReconciler;
    private final OrphanRoundDetector orphanRoundDetector;
    private final DailyReconciler dailyReconciler;

    @Scheduled(cron = "0 0 * * * *")
    public void runHourlyReconciliation() {
        hourlyReconciler.reconcile();
    }

    @Scheduled(cron = "0 */15 * * * *")
    public void runOrphanRoundDetection() {
        orphanRoundDetector.detectAndResolve();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void runDailyAnalytics() {
        dailyReconciler.aggregate();
    }
}
```

---

## 4. File Manifest

All paths are relative to the `wallet-reconciliation` module root.

### Production Code

| Path | Purpose |
|------|---------|
| `src/main/java/.../reconciliation/model/DiscrepancyRecord.java` | Record describing a platform-vs-GP mismatch |
| `src/main/java/.../reconciliation/model/GpReportSnapshot.java` | Immutable GP report snapshot for audit |
| `src/main/java/.../reconciliation/model/ReconciliationAudit.java` | JPA entity for reconciliation audit trail |
| `src/main/java/.../reconciliation/model/GpTransactionRecord.java` | DTO for a single transaction from a GP report |
| `src/main/java/.../reconciliation/detector/DiscrepancyResolver.java` | Stateless resolution logic for discrepancies |
| `src/main/java/.../reconciliation/detector/ReversalCircuitBreaker.java` | Per-GP reversal rate limiter with Redis counter |
| `src/main/java/.../reconciliation/detector/OrphanRoundDetector.java` | Scheduled scanner for stale OPEN rounds |
| `src/main/java/.../reconciliation/service/RealtimeReconciler.java` | Writes real-time ledger entries (Layer 1) |
| `src/main/java/.../reconciliation/service/HourlyReconciler.java` | Hourly GP-vs-platform comparison (Layer 2) |
| `src/main/java/.../reconciliation/service/DailyReconciler.java` | Daily aggregation into data warehouse (Layer 3) |
| `src/main/java/.../reconciliation/scheduler/ReconciliationScheduler.java` | Cron-driven job orchestrator |

### Enums

| Path | Values |
|------|--------|
| `src/main/java/.../reconciliation/model/DiscrepancyType.java` | `AMOUNT_MISMATCH`, `TIME_MISMATCH`, `MISSING_ON_PLATFORM`, `MISSING_ON_GP` |
| `src/main/java/.../reconciliation/model/ResolutionAction.java` | `AUTO_CORRECT`, `REVERSE`, `CREATE_FROM_GP`, `MANUAL_REVIEW`, `AUTO_MATCH` |
| `src/main/java/.../reconciliation/model/ReconciliationLayer.java` | `REALTIME`, `HOURLY`, `DAILY` |
| `src/main/java/.../reconciliation/model/OrphanResolution.java` | `AUTO_CLOSED`, `PENDING_REVIEW`, `ESCALATED` |

### Test Code

| Path | Purpose |
|------|---------|
| `src/test/java/.../reconciliation/service/HourlyReconcilerTest.java` | 5 tests: auto-correct, manual alert, auto-match, create-from-GP, reverse |
| `src/test/java/.../reconciliation/detector/ReversalCircuitBreakerTest.java` | 2 tests: threshold breach + snapshot preservation |
| `src/test/java/.../reconciliation/detector/OrphanRoundDetectorTest.java` | 4 tests: scan query, auto-close, pending-review, priority ticket |

### Database Migration

| Path | Purpose |
|------|---------|
| `src/main/resources/db/migration/V5_001__create_realtime_ledger.sql` | Real-time ledger table |
| `src/main/resources/db/migration/V5_002__create_reconciliation_audit.sql` | Audit trail with JSON snapshot column |

---

## 5. DB-Configurable Parameters

These parameters are read via the shared config parameter reader established in section-01. They follow the three-layer override pattern: Jurisdiction config > Brand config > Global config (fallback).

| Parameter Key | Default | Description |
|---------------|---------|-------------|
| `funding.wallet.orphan_scan_interval_minutes` | 15 | How often the orphan round detector runs |
| `funding.wallet.orphan_timeout_hours` | 2 | A round OPEN longer than this is considered orphaned |
| `funding.payment.recon.max_reversals_per_gp_per_hour` | 10 | Circuit breaker threshold for auto-reversals per GP |

---

## 6. Monitoring Integration

The following metrics should be emitted via Micrometer (setup from section-01):

| Metric | Type | Description |
|--------|------|-------------|
| `wallet.orphan_rounds.count` | Gauge | Current number of unresolved orphan rounds |
| `wallet.reconciliation.match_rate` | Gauge | Hourly match rate (target: 99.5%+) |
| `wallet.reconciliation.auto_corrections` | Counter | Number of auto-corrections applied |
| `wallet.reconciliation.reversals` | Counter | Number of auto-reversals executed |
| `wallet.reconciliation.circuit_breaker.trips` | Counter | Number of times the reversal circuit breaker tripped |

**Alert rules** (from the monitoring plan):

| Alert | Condition | Severity |
|-------|-----------|----------|
| Fund loss detected | Reconciliation shows unaccounted money | P0 -- immediate |
| Orphan rounds accumulating | > 50 unresolved orphans | P2 -- 1h |
| Reconciliation match dropping | Daily match < 99.5% | P2 -- 4h |

---

## 7. Dependencies

This section depends on the following prior sections (reference only):

- **section-01-foundation**: `BaseEntity`, `Money` value object, Testcontainers setup, DB migration framework, Micrometer configuration, DB-configurable parameter reader.
- **section-02-wallet-core**: `Wallet` entity, `WalletRepository`, balance mutation operations used when creating/reversing transactions during reconciliation.

The `Round` entity and its state machine are defined in section-03 (wallet-transaction). The orphan round detector queries rounds by status and updated_at but delegates settlement processing back to the transaction processor. For the purposes of this module, `Round` is treated as a read dependency -- this module queries round data but does not own the entity definition.
