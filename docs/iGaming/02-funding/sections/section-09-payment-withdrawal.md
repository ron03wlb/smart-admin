# Section 09 -- Payment Withdrawal (`payment-withdrawal`)

> **Module**: `payment-withdrawal`
> **Depends on**: section-01 (foundation), section-02 (wallet-core), section-06 (currency-engine), section-07 (payment-gateway)
> **Blocks**: section-12 (integration-tests)
> **Parallelizable with**: section-08 (payment-deposit), section-10 (payment-dispute)
> **Tech Stack**: Java 17+ / Spring Boot 3.x / PostgreSQL / Redis Cluster / Kafka

---

## Overview

This module implements the full withdrawal lifecycle for an iGaming funding domain. The flow proceeds through five stages: **instant amount locking**, **wagering verification** (synchronous call to the gaming domain), **multi-tier approval routing**, **PSP execution** via the payment-gateway smart router, and **final wallet debit**. The module enforces a critical safety invariant (INV-7): when the wagering verification service is unavailable, a withdrawal request must NEVER be auto-approved.

### Withdrawal Flow Summary

```
Player requests withdrawal
  -> Lock withdrawal amount instantly (<50ms)
  -> WageringVerifier: sync query gaming domain for wagering progress (<200ms)
     -> Met: auto-convert BONUS to CASH -> proceed
     -> Not met: reject + show remaining wagering amount
     -> Service unavailable (>500ms): queue as WAGERING_CHECK_PENDING
       -> NEVER auto-approve
       -> 2h timeout -> escalate to CS
  -> ApprovalWorkflow: route to appropriate approver based on amount
  -> Approver approves -> SmartRouter selects PSP -> execute withdrawal
  -> PSP confirms -> unlock amount -> debit CASH wallet
  -> Kafka: publish WithdrawalCompletedEvent
```

---

## 1. Tests (Write These First)

All tests must be written before the corresponding implementation. Naming convention: `{ClassName}Test.java` for unit tests, `{ClassName}IntegrationTest.java` for integration tests.

### 1.1 Withdrawal Flow Tests

These tests validate the end-to-end withdrawal pipeline and instant locking guarantee.

```java
/**
 * WithdrawalServiceTest.java
 * Location: payment-withdrawal/src/test/java/com/funding/withdrawal/service/
 */
class WithdrawalServiceTest {

    /**
     * Test: Successful withdrawal end-to-end: lock -> wagering check -> approve -> PSP -> debit
     *
     * Given a player with sufficient CASH balance and wagering requirements met,
     * when a withdrawal is requested,
     * then the flow executes all five stages in order:
     *   1. Amount is locked on the player's wallet
     *   2. WageringVerifier returns COMPLETE
     *   3. ApprovalWorkflow routes to the correct approver tier
     *   4. After approval, SmartRouter selects a PSP and executes the payout
     *   5. On PSP confirmation, the locked amount is released and CASH wallet is debited
     *   6. A WithdrawalCompletedEvent is published to Kafka (via outbox)
     *
     * Mocks: WageringVerifier, ApprovalWorkflow, SmartRouter (from payment-gateway),
     *        WalletService (from wallet-core), KafkaOutbox (from shared-infrastructure)
     */
    @Test
    void successfulWithdrawal_endToEnd_allStagesExecuteInOrder() {}

    /**
     * Test: Amount locked within 50ms of request
     *
     * Given a valid withdrawal request,
     * when the request is received,
     * then the wallet's lockedAmount is increased within 50ms.
     *
     * This verifies the "instant locking" guarantee. The lock must happen
     * BEFORE any downstream calls (wagering check, approval routing).
     * Use System.nanoTime() or StopWatch to assert the timing constraint.
     *
     * Mock: WalletService.lockAmount() -- verify it is the FIRST call in the flow
     *       and completes within the 50ms budget.
     */
    @Test
    void withdrawalRequest_locksAmountWithin50ms() {}
}
```

### 1.2 Multi-Tier Approval Tests

These tests validate the amount-based routing to the correct approval tier, including SLA enforcement.

```java
/**
 * ApprovalWorkflowTest.java
 * Location: payment-withdrawal/src/test/java/com/funding/withdrawal/approval/
 */
class ApprovalWorkflowTest {

    /**
     * Test: <$100 -> CS Agent approval route
     *
     * Given a withdrawal request for $99.99,
     * when determineRoute() is called,
     * then the returned ApprovalRoute has:
     *   - approverRole = CS_AGENT
     *   - No explicit SLA (auto-approve if pre-conditions met)
     *
     * Thresholds are DB-configurable per tenant; this test uses default thresholds.
     */
    @Test
    void amount_below100_routesToCsAgent() {}

    /**
     * Test: $100-$1K -> CS Supervisor with 1h SLA
     *
     * Given a withdrawal request for $500.00,
     * when determineRoute() is called,
     * then the returned ApprovalRoute has:
     *   - approverRole = CS_SUPERVISOR
     *   - sla = Duration.ofHours(1)
     */
    @Test
    void amount_100to1000_routesToCsSupervisorWith1hSla() {}

    /**
     * Test: $1K-$10K -> CFO with 4h SLA
     *
     * Given a withdrawal request for $5,000.00,
     * when determineRoute() is called,
     * then the returned ApprovalRoute has:
     *   - approverRole = CFO
     *   - sla = Duration.ofHours(4)
     */
    @Test
    void amount_1000to10000_routesToCfoWith4hSla() {}

    /**
     * Test: >$10K -> CFO + CEO dual approval with 24h SLA
     *
     * Given a withdrawal request for $15,000.00,
     * when determineRoute() is called,
     * then the returned ApprovalRoute has:
     *   - approverRoles = [CFO, CEO] (dual approval required)
     *   - sla = Duration.ofHours(24)
     *   - dualApproval = true
     *
     * Both approvals must be received before the withdrawal can proceed.
     */
    @Test
    void amount_above10000_routesToCfoAndCeoDualApprovalWith24hSla() {}
}
```

### 1.3 Wagering Verification Tests

These tests validate the synchronous wagering check against the gaming domain, including all three outcomes (met, not met, unavailable) and the critical invariant INV-7.

```java
/**
 * WageringVerifierTest.java
 * Location: payment-withdrawal/src/test/java/com/funding/withdrawal/service/
 */
class WageringVerifierTest {

    /**
     * Test: Wagering met -> BONUS auto-converts to CASH -> proceed
     *
     * Given a player whose wagering requirements are fully met,
     * when verify() is called,
     * then:
     *   1. WageringStatus.COMPLETE is returned
     *   2. Any remaining BONUS balance is converted to CASH
     *      (calls WalletService.convertBonusToCash())
     *   3. The withdrawal flow proceeds to the approval stage
     *
     * Mock: Gaming domain API returns COMPLETE status
     *       WalletService.convertBonusToCash() is invoked
     */
    @Test
    void wageringMet_bonusConvertedToCash_withdrawalProceeds() {}

    /**
     * Test: Wagering not met -> reject with remaining amount displayed
     *
     * Given a player with $200 remaining wagering requirement,
     * when verify() is called,
     * then:
     *   1. WageringStatus.INCOMPLETE is returned with remainingAmount = $200
     *   2. The withdrawal is rejected
     *   3. The locked amount is released (unlock)
     *   4. The rejection response includes the remaining wagering amount
     *      so the player knows how much more they need to wager
     *
     * Mock: Gaming domain API returns INCOMPLETE with remaining=$200
     */
    @Test
    void wageringNotMet_rejectWithRemainingAmount() {}

    /**
     * Test: Wagering service timeout >500ms -> queue as WAGERING_CHECK_PENDING
     *
     * Given the gaming domain's wagering API does not respond within 500ms,
     * when verify() is called,
     * then:
     *   1. WageringStatus.UNAVAILABLE is returned
     *   2. The withdrawal is queued with status WAGERING_CHECK_PENDING
     *   3. The locked amount remains locked (NOT released)
     *   4. A background retry job is scheduled (retries every 5 minutes)
     *
     * Mock: Gaming domain API call throws SocketTimeoutException after 500ms
     */
    @Test
    void wageringServiceTimeout_queuedAsWageringCheckPending() {}

    /**
     * Test: WAGERING_CHECK_PENDING NEVER auto-approves (critical invariant INV-7)
     *
     * THIS IS THE MOST CRITICAL TEST IN THIS MODULE.
     *
     * Given a withdrawal in WAGERING_CHECK_PENDING status,
     * when any automated process (scheduler, retry job, event handler) runs,
     * then the withdrawal is NEVER moved to APPROVED status automatically.
     *
     * Verify:
     *   - No code path from WAGERING_CHECK_PENDING leads to APPROVED
     *     without an explicit human action (CS agent resolution)
     *   - The retry job only re-attempts the wagering check; it does NOT
     *     approve the withdrawal if the check keeps failing
     *   - Even after maximum retries are exhausted, the status remains
     *     WAGERING_CHECK_PENDING (never APPROVED, never auto-rejected)
     *
     * Key invariant: INV-7 -- Wagering service unavailable -> withdrawal NEVER auto-approved
     */
    @Test
    void wageringCheckPending_neverAutoApproves_invariantInv7() {}

    /**
     * Test: WAGERING_CHECK_PENDING escalates to CS after 2 hours
     *
     * Given a withdrawal stuck in WAGERING_CHECK_PENDING for 2 hours,
     * when the escalation scheduler runs,
     * then:
     *   1. A CS escalation ticket is created with withdrawal details
     *   2. The withdrawal status transitions to ESCALATED (or remains
     *      WAGERING_CHECK_PENDING with an escalation flag)
     *   3. The CS team is notified (via internal notification system)
     *   4. The locked amount remains locked until manual resolution
     *
     * Mock: Clock or time provider to simulate 2h passage
     */
    @Test
    void wageringCheckPending_escalatesToCsAfter2Hours() {}
}
```

### 1.4 Holiday SLA Adjuster Tests

```java
/**
 * HolidaySlaAdjusterTest.java
 * Location: payment-withdrawal/src/test/java/com/funding/withdrawal/scheduler/
 */
class HolidaySlaAdjusterTest {

    /**
     * Test: Bank transfer SLA clock pauses on bank non-business days
     *
     * Given a withdrawal via bank transfer with a 4h SLA starting Friday 4pm,
     * when the SLA is calculated,
     * then the SLA deadline accounts for the weekend (pauses Saturday/Sunday)
     * and resumes counting on Monday morning.
     *
     * SLA adjustment applies ONLY to bank transfers.
     */
    @Test
    void bankTransfer_slaPausesOnBankHolidays() {}

    /**
     * Test: E-wallet and crypto withdrawals have 24/7 SLA (no adjustment)
     *
     * Given a withdrawal via e-wallet or cryptocurrency,
     * when the SLA is calculated,
     * then the SLA clock runs continuously without pause,
     * regardless of holidays or weekends.
     */
    @Test
    void ewalletAndCrypto_slaRuns247_noAdjustment() {}
}
```

---

## 2. Implementation Details

### 2.1 Module Structure

```
payment-withdrawal/
  src/main/java/com/funding/withdrawal/
    model/
      WithdrawalRequest.java
      WithdrawalStatus.java          # Enum: INITIATED, AMOUNT_LOCKED, WAGERING_CHECK_PENDING,
                                     #        WAGERING_PASSED, PENDING_APPROVAL, APPROVED,
                                     #        PROCESSING, COMPLETED, REJECTED, ESCALATED, FAILED
      ApprovalRoute.java
      ApprovalTier.java              # Enum: CS_AGENT, CS_SUPERVISOR, CFO, CFO_CEO_DUAL
      WageringStatus.java            # Enum: COMPLETE, INCOMPLETE, UNAVAILABLE
    service/
      WithdrawalService.java         # Orchestrates the full withdrawal flow
      WageringVerifier.java          # Interface: sync call to gaming domain
      WageringVerifierClient.java    # Implementation: REST client to gaming domain API
    approval/
      ApprovalWorkflow.java          # Interface: determines approval route
      ApprovalWorkflowImpl.java      # Implementation: amount-threshold routing
      ApprovalThresholdConfig.java   # DB-configurable thresholds per tenant
    scheduler/
      HolidaySlaAdjuster.java        # Adjusts SLA deadlines for bank non-business days
      WageringCheckRetryJob.java     # Retries WAGERING_CHECK_PENDING every 5min
      WageringEscalationJob.java     # Escalates unresolved WAGERING_CHECK_PENDING after 2h
    repository/
      WithdrawalRequestRepository.java
  src/test/java/com/funding/withdrawal/
    service/
      WithdrawalServiceTest.java
      WageringVerifierTest.java
    approval/
      ApprovalWorkflowTest.java
    scheduler/
      HolidaySlaAdjusterTest.java
      WageringCheckRetryJobTest.java
      WageringEscalationJobTest.java
```

### 2.2 Withdrawal Status Lifecycle

```
INITIATED
  -> AMOUNT_LOCKED                (amount locked on wallet within 50ms)
  -> WAGERING_PASSED              (wagering check returns COMPLETE)
  -> WAGERING_CHECK_PENDING       (wagering service timeout >500ms)
     -> WAGERING_PASSED           (retry succeeds, wagering met)
     -> REJECTED                  (retry succeeds, wagering not met)
     -> ESCALATED                 (2h timeout, CS escalation)
  -> REJECTED                     (wagering not met)
  -> PENDING_APPROVAL             (wagering passed, routed to approver)
  -> APPROVED                     (approver(s) approved)
  -> PROCESSING                   (PSP execution in progress)
  -> COMPLETED                    (PSP confirms, wallet debited)
  -> FAILED                       (PSP failure after retries exhausted)
```

**Critical constraint**: There is NO direct transition from `WAGERING_CHECK_PENDING` to `APPROVED` or `PENDING_APPROVAL`. The only paths out of `WAGERING_CHECK_PENDING` are: `WAGERING_PASSED` (when retry succeeds and wagering is met), `REJECTED` (when retry succeeds and wagering is not met), or `ESCALATED` (after 2h timeout). This enforces INV-7.

### 2.3 WithdrawalService -- Flow Orchestrator

```java
/**
 * Orchestrates the withdrawal lifecycle.
 *
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/service/WithdrawalService.java
 */
public interface WithdrawalService {

    /**
     * Initiates a withdrawal request.
     *
     * Steps:
     *   1. Validate request (amount > 0, player active, not frozen)
     *   2. Lock amount on wallet (must complete < 50ms)
     *      - Calls WalletService.lockAmount(playerId, amount) from wallet-core
     *      - If lock fails (insufficient balance), reject immediately
     *   3. Verify wagering requirements
     *      - Calls WageringVerifier.verify(playerId, tenantId)
     *      - COMPLETE -> proceed to step 4
     *      - INCOMPLETE -> unlock amount, reject with remaining wagering info
     *      - UNAVAILABLE -> persist as WAGERING_CHECK_PENDING, schedule retry
     *   4. Determine approval route
     *      - Calls ApprovalWorkflow.determineRoute(amount, tenantId)
     *      - Creates approval task for the determined tier
     *   5. Wait for approval (async -- approval comes via separate endpoint)
     *   6. On approval, execute payout via SmartRouter (from payment-gateway)
     *   7. On PSP confirmation, unlock amount + debit CASH wallet
     *   8. Publish WithdrawalCompletedEvent via Kafka outbox
     *
     * @param playerId   the player requesting the withdrawal
     * @param amount     the withdrawal amount (Money value object from foundation)
     * @param tenantId   tenant for multi-tenancy isolation
     * @param paymentMethod  the target payment method (bank, e-wallet, crypto)
     * @return WithdrawalRequest with initial status
     */
    WithdrawalRequest initiate(UUID playerId, Money amount, UUID tenantId, PaymentMethod paymentMethod);

    /**
     * Called by the approval endpoint when an approver acts on a withdrawal.
     */
    void onApprovalDecision(UUID withdrawalId, ApprovalDecision decision, UUID approverId);

    /**
     * Called by PSP callback when payout result arrives.
     */
    void onPspCallback(UUID withdrawalId, PspResult result);
}
```

### 2.4 WageringVerifier -- Gaming Domain Integration

```java
/**
 * Synchronous client to the gaming domain's wagering API.
 *
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/service/WageringVerifier.java
 */
public interface WageringVerifier {

    /**
     * Calls gaming domain API: GET /api/v1/wagering/{playerId}/status
     * SLA: < 200ms target, hard timeout at 500ms
     *
     * Returns:
     *   COMPLETE -> wagering requirement met, proceed with withdrawal
     *   INCOMPLETE(remaining) -> not met, remaining amount provided for player display
     *
     * Degradation:
     *   Timeout > 500ms -> UNAVAILABLE
     *   -> Queue withdrawal as WAGERING_CHECK_PENDING
     *   -> Background job retries every 5 minutes
     *   -> After 2h unresolved -> escalate to CS
     *   -> CRITICAL: NEVER auto-approve when wagering service is unavailable (INV-7)
     *
     * When wagering is COMPLETE and the player has remaining BONUS balance,
     * the BONUS is auto-converted to CASH before the withdrawal proceeds.
     *
     * @param playerId  the player whose wagering status to check
     * @param tenantId  tenant context
     * @return WageringCheckResult containing status and optional remaining amount
     */
    WageringCheckResult verify(UUID playerId, UUID tenantId);
}
```

### 2.5 ApprovalWorkflow -- Multi-Tier Routing

```java
/**
 * Determines the approval route based on withdrawal amount.
 *
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/approval/ApprovalWorkflow.java
 */
public interface ApprovalWorkflow {

    /**
     * Routes to the appropriate approver tier based on amount thresholds.
     *
     * Default thresholds (DB-configurable per tenant):
     *   < $100     -> CS Agent (auto-approve if pre-conditions met, instant SLA)
     *   $100-$1K   -> CS Supervisor (1h SLA)
     *   $1K-$10K   -> CFO (4h SLA)
     *   > $10K     -> CFO + CEO dual approval (24h SLA)
     *
     * SLA clock behavior:
     *   - Bank transfers: SLA pauses on bank non-business days (weekends, public holidays)
     *   - E-wallets and crypto: 24/7, no SLA adjustment
     *
     * For dual approval (>$10K):
     *   - Both CFO and CEO must approve
     *   - Either party can reject (rejection is immediate)
     *   - SLA applies to the LAST approval received
     *
     * @param amount    the withdrawal amount
     * @param tenantId  tenant for loading tenant-specific thresholds
     * @return ApprovalRoute containing approver role(s), SLA duration, and dual-approval flag
     */
    ApprovalRoute determineRoute(Money amount, UUID tenantId);
}
```

### 2.6 ApprovalRoute -- Value Object

```java
/**
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/model/ApprovalRoute.java
 */
public record ApprovalRoute(
    List<ApprovalTier> approverRoles,   // e.g., [CFO, CEO] for dual approval
    Duration sla,                        // SLA duration (null for CS_AGENT auto-approve)
    boolean dualApproval,                // true if all listed roles must approve
    PaymentMethod paymentMethod          // needed by HolidaySlaAdjuster
) {
    /**
     * Calculates the effective SLA deadline from the given start time,
     * adjusting for bank holidays if the payment method is BANK_TRANSFER.
     */
    public Instant effectiveDeadline(Instant startTime, HolidaySlaAdjuster adjuster) {
        // Delegates to HolidaySlaAdjuster for bank transfers
        // Returns startTime + sla for e-wallet/crypto
    }
}
```

### 2.7 HolidaySlaAdjuster -- Scheduler

```java
/**
 * Adjusts SLA deadlines to exclude bank non-business days.
 *
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/scheduler/HolidaySlaAdjuster.java
 */
public class HolidaySlaAdjuster {

    /**
     * Calculates the effective SLA deadline, pausing the clock on
     * bank non-business days (weekends + configured public holidays).
     *
     * Only applies to BANK_TRANSFER payment method.
     * E-wallets and crypto run 24/7 with no adjustment.
     *
     * Bank holidays are loaded from a DB-configurable calendar per jurisdiction.
     *
     * @param startTime      when the SLA clock started
     * @param slaDuration    the raw SLA duration
     * @param jurisdiction   the player's jurisdiction (for holiday calendar lookup)
     * @return adjusted deadline that excludes non-business hours/days
     */
    public Instant adjustDeadline(Instant startTime, Duration slaDuration, String jurisdiction) {
        // Implementation: walk forward through time, skipping non-business days
    }
}
```

### 2.8 WageringCheckRetryJob -- Background Retry

```java
/**
 * Scheduled job that retries wagering verification for WAGERING_CHECK_PENDING withdrawals.
 *
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/scheduler/WageringCheckRetryJob.java
 */
@Component
public class WageringCheckRetryJob {

    /**
     * Runs every 5 minutes.
     *
     * Finds all withdrawals with status WAGERING_CHECK_PENDING.
     * For each:
     *   1. Re-call WageringVerifier.verify()
     *   2. If COMPLETE -> transition to WAGERING_PASSED, then proceed to approval
     *   3. If INCOMPLETE -> transition to REJECTED, unlock amount
     *   4. If UNAVAILABLE -> leave as WAGERING_CHECK_PENDING (retry on next run)
     *
     * CRITICAL: This job NEVER transitions a withdrawal to APPROVED.
     * It only determines wagering status. Approval requires the full
     * ApprovalWorkflow to run after wagering passes.
     */
    @Scheduled(fixedRate = 300_000) // 5 minutes
    public void retryPendingWageringChecks() {}
}
```

### 2.9 WageringEscalationJob -- 2-Hour Escalation

```java
/**
 * Scheduled job that escalates WAGERING_CHECK_PENDING withdrawals after 2 hours.
 *
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/scheduler/WageringEscalationJob.java
 */
@Component
public class WageringEscalationJob {

    /**
     * Runs every 15 minutes.
     *
     * Finds all withdrawals with status WAGERING_CHECK_PENDING
     * where createdAt < now() - 2 hours.
     *
     * For each:
     *   1. Create a CS escalation ticket with full withdrawal details
     *   2. Transition status to ESCALATED (or flag as escalated)
     *   3. Notify CS team via internal notification
     *   4. Keep the amount locked (manual resolution required)
     *
     * CRITICAL: This job NEVER auto-approves. Escalation means
     * a human must resolve the withdrawal.
     */
    @Scheduled(fixedRate = 900_000) // 15 minutes
    public void escalateStaleWageringChecks() {}
}
```

### 2.10 WithdrawalRequest -- Entity

```java
/**
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/model/WithdrawalRequest.java
 *
 * DB Table: withdrawal_requests
 */
@Entity
@Table(name = "withdrawal_requests")
public class WithdrawalRequest {
    @Id
    private UUID id;
    private UUID playerId;
    private UUID tenantId;

    @Embedded
    private Money amount;               // Uses Money value object from foundation (section-01)

    @Enumerated(EnumType.STRING)
    private WithdrawalStatus status;    // See status enum in section 2.2

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant slaDeadline;        // Calculated by HolidaySlaAdjuster
    private Instant escalatedAt;        // Set when escalated to CS
    private UUID approvedBy;            // Approver who approved (or first approver for dual)
    private UUID secondApprovedBy;      // Second approver for dual approval (>$10K)
    private String rejectionReason;
    private String pspReference;        // PSP transaction reference after execution

    @Version
    private Long version;               // Optimistic locking
}
```

### 2.11 WithdrawalStatus -- Enum

```java
/**
 * Location: payment-withdrawal/src/main/java/com/funding/withdrawal/model/WithdrawalStatus.java
 */
public enum WithdrawalStatus {
    INITIATED,
    AMOUNT_LOCKED,
    WAGERING_CHECK_PENDING,   // Wagering service unavailable -- NEVER transitions to APPROVED
    WAGERING_PASSED,
    PENDING_APPROVAL,
    APPROVED,
    PROCESSING,
    COMPLETED,
    REJECTED,
    ESCALATED,                // After 2h in WAGERING_CHECK_PENDING
    FAILED
}
```

---

## 3. Database Schema

```sql
-- Location: payment-withdrawal/src/main/resources/db/migration/V9_001__create_withdrawal_tables.sql

CREATE TABLE withdrawal_requests (
    id              UUID PRIMARY KEY,
    player_id       UUID NOT NULL,
    tenant_id       UUID NOT NULL,
    amount          DECIMAL(19,4) NOT NULL CHECK (amount > 0),
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'INITIATED',
    payment_method  VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    sla_deadline    TIMESTAMP WITH TIME ZONE,
    escalated_at    TIMESTAMP WITH TIME ZONE,
    approved_by     UUID,
    second_approved_by UUID,
    rejection_reason TEXT,
    psp_reference   VARCHAR(255),
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_withdrawal_player ON withdrawal_requests (player_id, tenant_id);
CREATE INDEX idx_withdrawal_status ON withdrawal_requests (status) WHERE status IN ('WAGERING_CHECK_PENDING', 'PENDING_APPROVAL', 'PROCESSING');
CREATE INDEX idx_withdrawal_escalation ON withdrawal_requests (status, created_at) WHERE status = 'WAGERING_CHECK_PENDING';

-- Approval thresholds per tenant (DB-configurable)
CREATE TABLE approval_thresholds (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    tier        VARCHAR(20) NOT NULL,    -- CS_AGENT, CS_SUPERVISOR, CFO, CFO_CEO_DUAL
    min_amount  DECIMAL(19,4) NOT NULL,
    max_amount  DECIMAL(19,4),           -- NULL means no upper bound
    sla_hours   INT,                     -- NULL for auto-approve tier
    UNIQUE (tenant_id, tier)
);

-- Bank holiday calendar for SLA adjustment
CREATE TABLE bank_holidays (
    id              UUID PRIMARY KEY,
    jurisdiction    VARCHAR(10) NOT NULL,
    holiday_date    DATE NOT NULL,
    description     VARCHAR(255),
    UNIQUE (jurisdiction, holiday_date)
);
```

---

## 4. Kafka Events

```java
/**
 * Published to topic: funding.withdrawal.completed
 * Published via transactional outbox (from shared-infrastructure, section-11)
 */
public record WithdrawalCompletedEvent(
    UUID withdrawalId,
    UUID playerId,
    UUID tenantId,
    Money amount,
    PaymentMethod paymentMethod,
    String pspReference,
    Instant completedAt
) {}

/**
 * Published to topic: funding.withdrawal.escalated
 */
public record WithdrawalEscalatedEvent(
    UUID withdrawalId,
    UUID playerId,
    UUID tenantId,
    Money amount,
    Instant escalatedAt,
    String reason   // e.g., "WAGERING_CHECK_PENDING for 2+ hours"
) {}
```

---

## 5. Configuration

```yaml
# Location: payment-withdrawal/src/main/resources/application.yml (relevant fragment)

funding:
  withdrawal:
    lock-timeout-ms: 50            # Max time to lock wallet amount
    wagering:
      timeout-ms: 500              # Hard timeout for gaming domain API call
      target-sla-ms: 200           # Target response time
      retry-interval-ms: 300000    # 5 minutes between retries
      escalation-threshold-ms: 7200000  # 2 hours before CS escalation
    approval:
      default-thresholds:          # Overridden per tenant via DB
        cs-agent-max: 100
        cs-supervisor-max: 1000
        cfo-max: 10000
        # Above cfo-max -> CFO + CEO dual approval
```

---

## 6. Dependencies on Other Sections

| Dependency | What Is Used | How |
|---|---|---|
| **section-01 (foundation)** | `Money` value object, base entity classes, DB migration framework, test infrastructure (Testcontainers) | Amount representation throughout; entity base class for `WithdrawalRequest` |
| **section-02 (wallet-core)** | `WalletService.lockAmount()`, `WalletService.unlockAmount()`, `WalletService.debit()`, `WalletService.convertBonusToCash()` | Lock/unlock during withdrawal lifecycle; debit on completion; bonus conversion on wagering pass |
| **section-06 (currency-engine)** | `Money` arithmetic, currency handling | Amount comparisons against thresholds; multi-currency withdrawal support |
| **section-07 (payment-gateway)** | `SmartRouter.route()`, PSP adapter execution | PSP selection and payout execution after approval |

---

## 7. Key Invariant: INV-7

> **Wagering service unavailable -> withdrawal NEVER auto-approved**

This invariant is enforced at multiple levels:

1. **State machine**: No transition from `WAGERING_CHECK_PENDING` to `APPROVED` or `PENDING_APPROVAL` exists.
2. **WageringCheckRetryJob**: Only transitions to `WAGERING_PASSED` (if wagering met on retry) or `REJECTED` (if not met). Never to `APPROVED`.
3. **WageringEscalationJob**: Transitions to `ESCALATED` after 2 hours. Never to `APPROVED`.
4. **Code review checklist item**: Any PR touching withdrawal status transitions must verify INV-7 is preserved.
5. **Dedicated test**: `wageringCheckPending_neverAutoApproves_invariantInv7()` explicitly verifies no automated code path leads from `WAGERING_CHECK_PENDING` to `APPROVED`.
