# Section 08 -- Payment Deposit (`payment-deposit`)

> **Module**: `payment-deposit`
> **Depends on**: section-01 (Foundation), section-06 (Currency Engine), section-07 (Payment Gateway)
> **Parallelizable with**: section-09 (Payment Withdrawal), section-10 (Payment Dispute)
> **Tech**: Java 17+ / Spring Boot 3.x / PostgreSQL / Redis Cluster / Kafka

---

## Overview

This module implements the complete deposit lifecycle for an iGaming platform. A deposit begins when a player selects an amount and payment method in the cashier, passes through limit verification and friction evaluation, is routed to a PSP via the Payment Gateway, and concludes when the PSP callback confirms the payment and the player's CASH wallet is credited.

For EU/PSD2 and UK markets, card deposits must pass through 3D Secure 2.0 / Strong Customer Authentication (SCA). Without 3DS integration, the platform cannot operate in regulated EU/UK jurisdictions.

The module contains five core components:

1. **DepositService** -- Orchestrates the full deposit flow from initiation to wallet credit.
2. **DepositLimitChecker** -- Enforces single-transaction, daily-cumulative, and monthly-cumulative limits.
3. **CashierFrictionEvaluator** -- Determines the number of verification steps based on player risk profile (3 tiers).
4. **PSP Callback Handler** -- Receives and verifies asynchronous payment confirmations from PSPs.
5. **ThreeDSHandler** -- Manages 3D Secure challenge flow and SCA compliance for EU/UK card deposits.

---

## 1. Tests (Write These First)

All tests use JUnit 5 + Mockito for unit tests and Testcontainers (PostgreSQL + Redis + Kafka) for integration tests.

Naming convention: `{ClassName}Test.java` for unit tests, `{ClassName}IntegrationTest.java` for integration tests.

---

### 1.1 Deposit Flow Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/service/DepositServiceTest.java`

```java
/**
 * Unit tests for the deposit orchestration service.
 * Uses Mockito to mock DepositLimitChecker, CashierFrictionEvaluator,
 * SmartRouter (from payment-gateway), PSPAdapter, CurrencyEngine,
 * WalletCreditService, and KafkaOutboxPublisher.
 */
class DepositServiceTest {

    /**
     * Test: Successful deposit end-to-end: limits -> friction -> route -> PSP -> callback -> credit
     *
     * Given a player with valid KYC, sufficient limits, and a configured PSP:
     * 1. DepositLimitChecker.verify() returns APPROVED
     * 2. CashierFrictionEvaluator.evaluate() returns LOW
     * 3. SmartRouter.route() returns a PSP selection
     * 4. PSPAdapter.initiateDeposit() returns a redirect URL
     * 5. (Simulated) PSP callback arrives with SUCCESS status
     * 6. CallbackHandler verifies signature and parses result
     * 7. CurrencyEngine converts if needed (no-op when same currency)
     * 8. WalletCore credits the CASH wallet
     * 9. Kafka outbox receives a DepositCompletedEvent
     * 10. Player notification is dispatched
     *
     * Assert: deposit status is COMPLETED, wallet credit amount matches deposit,
     *         all collaborators invoked in correct order.
     */
    @Test
    void successfulDeposit_endToEnd_creditsWalletAndPublishesEvent() {}

    /**
     * Test: Deposit exceeding daily limit -> rejected
     *
     * Given a player who has already deposited the maximum daily amount:
     * - DepositLimitChecker.verify() returns REJECTED with reason DAILY_LIMIT_EXCEEDED
     *
     * Assert: deposit is rejected before reaching the SmartRouter,
     *         response includes remaining daily limit amount,
     *         no PSP interaction occurs, no wallet credit.
     */
    @Test
    void depositExceedingDailyLimit_rejected() {}
}
```

---

### 1.2 Cashier Friction Evaluator Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/service/CashierFrictionEvaluatorTest.java`

```java
/**
 * Unit tests for the cashier friction evaluator.
 * Mocks the Risk Domain sync API client and the Player Profile service
 * (for KYC level and deposit history).
 */
class CashierFrictionEvaluatorTest {

    /**
     * Test: KYC L2+ with 3+ deposits and risk<30 -> LOW friction (<=3 steps)
     *
     * Given a player with:
     * - KYC level = L2 or higher
     * - 3 or more previous successful deposits
     * - Current risk score = 25 (below 30)
     *
     * Assert: evaluate() returns FrictionTier.LOW
     * Assert: maxSteps on the returned result is <= 3
     */
    @Test
    void kycL2Plus_threeDeposits_lowRisk_returnsLowFriction() {}

    /**
     * Test: KYC L1 -> MEDIUM friction
     *
     * Given a player with:
     * - KYC level = L1
     * - Risk score = 45 (between 30 and 70)
     *
     * Assert: evaluate() returns FrictionTier.MEDIUM
     * Assert: maxSteps on the returned result is <= 5
     */
    @Test
    void kycL1_returnsMediumFriction() {}

    /**
     * Test: KYC L0 or risk>=70 -> HIGH friction
     *
     * Two sub-cases:
     * (a) Player with KYC level = L0, regardless of risk score
     *     -> evaluate() returns FrictionTier.HIGH, maxSteps <= 7
     *
     * (b) Player with KYC level = L2, but risk score = 75 (>= 70)
     *     -> evaluate() returns FrictionTier.HIGH, maxSteps <= 7
     *
     * HIGH friction also applies when the jurisdiction requires extra verification.
     */
    @Test
    void kycL0_returnsHighFriction() {}

    @Test
    void riskScoreAbove70_returnsHighFriction() {}
}
```

---

### 1.3 Deposit Limit Checker Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/limit/DepositLimitCheckerTest.java`

```java
/**
 * Unit tests for the three-dimensional deposit limit checker.
 * Mocks the ConfigParameterReader (from shared/) for retrieving
 * DB-configurable limits, and the DepositHistoryRepository for
 * cumulative totals.
 */
class DepositLimitCheckerTest {

    /**
     * Test: Single transaction amount within min/max range -> APPROVED
     */
    @Test
    void singleTransactionWithinRange_approved() {}

    /**
     * Test: Single transaction amount below minimum -> REJECTED with MIN_AMOUNT_VIOLATION
     */
    @Test
    void singleTransactionBelowMin_rejected() {}

    /**
     * Test: Single transaction amount above maximum -> REJECTED with MAX_AMOUNT_VIOLATION
     */
    @Test
    void singleTransactionAboveMax_rejected() {}

    /**
     * Test: Daily cumulative total (existing + new) exceeds cap -> REJECTED with DAILY_LIMIT_EXCEEDED
     * Response includes the remaining daily allowance.
     */
    @Test
    void dailyCumulativeExceedsCap_rejected() {}

    /**
     * Test: Monthly cumulative total exceeds cap -> REJECTED with MONTHLY_LIMIT_EXCEEDED
     * Response includes the remaining monthly allowance.
     */
    @Test
    void monthlyCumulativeExceedsCap_rejected() {}

    /**
     * Test: Limits resolved via three-layer override: Jurisdiction -> Brand -> Global
     * When jurisdiction-specific limit exists, it takes precedence over brand and global.
     */
    @Test
    void limitResolution_jurisdictionOverridesTakesPrecedence() {}
}
```

---

### 1.4 PSP Callback Handler Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/callback/PspCallbackHandlerTest.java`

```java
/**
 * Unit tests for PSP callback processing.
 * Mocks PSPAdapter (for signature verification), DepositRepository,
 * CurrencyEngine, WalletCreditService, and KafkaOutboxPublisher.
 */
class PspCallbackHandlerTest {

    /**
     * Test: Valid callback with SUCCESS status -> verifies signature, credits wallet, publishes event
     */
    @Test
    void validSuccessCallback_creditsWalletAndPublishes() {}

    /**
     * Test: Callback with invalid signature -> rejected, deposit status unchanged, alert raised
     */
    @Test
    void invalidSignature_rejectedWithAlert() {}

    /**
     * Test: Callback with FAILED status -> deposit marked FAILED, no wallet credit, player notified
     */
    @Test
    void failedCallback_depositsMarkedFailed() {}

    /**
     * Test: Duplicate callback (same transactionId) -> idempotent, returns 200 but no double credit
     */
    @Test
    void duplicateCallback_idempotentNoDoubleCredit() {}

    /**
     * Test: Callback for unknown deposit -> logged, returns 404, alert raised
     */
    @Test
    void unknownDeposit_returns404WithAlert() {}
}
```

---

### 1.5 3D Secure / SCA Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/threeds/ThreeDSHandlerTest.java`

```java
/**
 * Unit tests for 3D Secure and SCA integration.
 * Mocks PSPAdapter (for 3DS challenge initiation and callback handling),
 * JurisdictionResolver (to determine if 3DS is required),
 * and ScaCumulativeTracker (Redis-backed cumulative amount tracking).
 */
class ThreeDSHandlerTest {

    /**
     * Test: EU card deposit triggers 3DS challenge via PSP
     *
     * Given: a deposit request with a card issued in an EU jurisdiction
     * When: the deposit is initiated
     * Then: PSPAdapter.initiate3DSChallenge() is called
     * And: the response contains a 3DS challenge URL for the player to complete
     * And: the deposit status is set to AWAITING_3DS
     */
    @Test
    void euCardDeposit_triggers3DSChallenge() {}

    /**
     * Test: 3DS AUTHENTICATED -> proceed with payment
     *
     * Given: a 3DS callback with authentication result = AUTHENTICATED
     * When: ThreeDSHandler.handle3DSCallback() is invoked
     * Then: the deposit proceeds to PSP payment processing
     * And: the 3DS result is logged for audit
     */
    @Test
    void threeDsAuthenticated_proceedsWithPayment() {}

    /**
     * Test: 3DS FAILED -> reject deposit
     *
     * Given: a 3DS callback with authentication result = FAILED
     * When: ThreeDSHandler.handle3DSCallback() is invoked
     * Then: the deposit is rejected with status THREEDS_FAILED
     * And: the failure reason is logged
     * And: the player is notified of the rejection
     * And: no wallet credit occurs
     */
    @Test
    void threeDsFailed_rejectsDeposit() {}

    /**
     * Test: 3DS ATTEMPTED (issuer not enrolled) -> proceed (liability shift)
     *
     * Given: a 3DS callback with authentication result = ATTEMPTED
     *        (meaning the card issuer does not support 3DS)
     * When: ThreeDSHandler.handle3DSCallback() is invoked
     * Then: the deposit proceeds to payment processing
     * And: liability shift flag is set to ISSUER (fraud liability is on the issuer)
     * And: the ATTEMPTED status is logged for audit
     */
    @Test
    void threeDsAttempted_proceedsWithLiabilityShift() {}

    /**
     * Test: UK cumulative amount tracking triggers SCA when threshold exceeded
     *
     * Given: a UK player with cumulative card deposits of GBP 85 in the
     *        current 24h sliding window (tracked in Redis)
     * When: a new GBP 20 deposit is initiated (cumulative would reach GBP 105,
     *        exceeding the GBP 100 threshold)
     * Then: SCA is triggered even if the single transaction amount (GBP 20) is
     *        below the GBP 30 single-transaction threshold
     * And: ScaCumulativeTracker records the new cumulative total upon success
     *
     * Also verify: single transaction >GBP 30 always triggers SCA regardless
     * of cumulative total.
     */
    @Test
    void ukCumulativeAmountExceedsThreshold_triggersSca() {}
}
```

---

### 1.6 SCA Cumulative Tracker Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/threeds/ScaCumulativeTrackerTest.java`

```java
/**
 * Integration tests for the Redis-backed SCA cumulative amount tracker.
 * Uses Testcontainers with Redis.
 */
class ScaCumulativeTrackerIntegrationTest {

    /**
     * Test: Cumulative amount correctly tracks deposits within 24h sliding window
     */
    @Test
    void tracksDepositsWithin24hWindow() {}

    /**
     * Test: Deposits older than 24h are excluded from cumulative total
     */
    @Test
    void depositsOlderThan24h_excludedFromCumulative() {}

    /**
     * Test: Cumulative tracking is scoped per player per payment method
     */
    @Test
    void trackingScopedPerPlayerPerPaymentMethod() {}

    /**
     * Test: Redis key expires after 24h (TTL-based cleanup)
     */
    @Test
    void redisKeyExpiresAfter24h() {}
}
```

---

### 1.7 Deposit Notification Tests

**File**: `payment-deposit/src/test/java/com/funding/deposit/notification/DepositNotificationServiceTest.java`

```java
/**
 * Unit tests for deposit notification dispatching.
 */
class DepositNotificationServiceTest {

    /**
     * Test: Successful deposit -> player notified with amount and new balance
     */
    @Test
    void successfulDeposit_playerNotifiedWithDetails() {}

    /**
     * Test: Failed deposit -> player notified with failure reason
     */
    @Test
    void failedDeposit_playerNotifiedWithReason() {}

    /**
     * Test: Notification delivery failure -> logged, does not block deposit completion
     */
    @Test
    void notificationFailure_doesNotBlockDeposit() {}
}
```

---

## 2. Implementation Details

### 2.1 Deposit Flow Orchestration

The deposit flow is a linear pipeline of six stages. Each stage can halt the flow with an appropriate error response.

```
Player initiates deposit
  -> DepositLimitChecker: verify single/daily/monthly limits
  -> CashierFrictionEvaluator: determine risk tier (LOW/MEDIUM/HIGH)
  -> SmartRouter: select optimal PSP (from payment-gateway module)
  -> PSPAdapter: redirect to PSP payment page
  -> (Player completes payment on PSP page)
  -> PSP sends callback
  -> CallbackHandler: verify signature, parse result
  -> CurrencyEngine: convert to settlement currency (if different)
  -> WalletCore: credit CASH wallet
  -> Kafka: publish DepositCompletedEvent
  -> Notification: confirm to player
```

**Key design points**:

- The flow is stateful. A `DepositOrder` entity tracks the deposit through each stage with status transitions: `INITIATED -> LIMITS_CHECKED -> FRICTION_EVALUATED -> PSP_SELECTED -> REDIRECTED -> AWAITING_CALLBACK -> PROCESSING -> COMPLETED` (or `FAILED` / `REJECTED` at any stage).
- Currency conversion happens at deposit time, not at the wallet level. The wallet stores a single settlement currency. The `CurrencyEngine` (section-06) handles the conversion and locks the FX rate for 15 minutes.
- The CASH wallet is the only wallet credited by deposits. BONUS and CREDIT wallets are managed by separate processes.

**File**: `payment-deposit/src/main/java/com/funding/deposit/service/DepositService.java`

```java
/**
 * Orchestrates the complete deposit lifecycle.
 *
 * Dependencies:
 * - DepositLimitChecker (this module)
 * - CashierFrictionEvaluator (this module)
 * - SmartRouter (from payment-gateway, section-07)
 * - PSPAdapter (from payment-gateway, section-07)
 * - CurrencyEngine (from currency-engine, section-06)
 * - WalletCreditService (from wallet-core, via internal API)
 * - KafkaOutboxPublisher (from shared, section-01)
 * - DepositNotificationService (this module)
 */
@Service
public class DepositService {

    /**
     * Initiates a deposit request. Validates limits, evaluates friction,
     * routes to PSP, and returns a redirect URL for the player.
     *
     * @param request contains playerId, tenantId, amount, currency, paymentMethod
     * @return DepositInitiationResult with redirectUrl or rejection reason
     */
    public DepositInitiationResult initiateDeposit(DepositRequest request) {
        // 1. Verify limits
        // 2. Evaluate friction tier
        // 3. Route to PSP
        // 4. Create DepositOrder in DB with status INITIATED
        // 5. Call PSPAdapter.initiateDeposit() (or initiate3DSChallenge for EU cards)
        // 6. Return redirect URL to player
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.2 Cashier Friction Evaluator

Determines verification friction for a deposit based on the player's KYC level, deposit history, and real-time risk score. The risk score is fetched synchronously from the Risk Domain API with a 100ms SLA.

**Tier definitions**:

| Tier | Max Steps | Criteria |
|------|-----------|----------|
| LOW | 3 | KYC Level 2 or higher AND 3+ previous successful deposits AND risk score < 30 |
| MEDIUM | 5 | KYC Level 1 OR risk score 30-70 OR first time using this payment method |
| HIGH | 7 | KYC Level 0 OR risk score >= 70 OR jurisdiction requires extra verification |

All three LOW criteria must be met simultaneously. For MEDIUM, any single condition is sufficient. For HIGH, any single condition is sufficient (and HIGH takes precedence over MEDIUM).

**File**: `payment-deposit/src/main/java/com/funding/deposit/service/CashierFrictionEvaluator.java`

```java
/**
 * Evaluates the friction tier for a deposit based on player risk profile.
 *
 * Calls the Risk Domain sync API for the current risk score (<100ms SLA).
 * Falls back to HIGH friction if the Risk Domain is unavailable.
 */
public interface CashierFrictionEvaluator {

    /**
     * @param playerId   the player initiating the deposit
     * @param method     the payment method being used
     * @param amount     the deposit amount
     * @return FrictionTier (LOW, MEDIUM, HIGH) with the max allowed steps
     */
    FrictionTier evaluate(UUID playerId, PaymentMethod method, Money amount);
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/model/FrictionTier.java`

```java
public enum FrictionTier {

    LOW(3),
    MEDIUM(5),
    HIGH(7);

    private final int maxSteps;

    FrictionTier(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getMaxSteps() {
        return maxSteps;
    }
}
```

---

### 2.3 Deposit Limit Checker

Enforces three dimensions of deposit limits. All limits are DB-configurable via the three-layer override system (Jurisdiction -> Brand -> Global) provided by the shared config module (section-01).

**Three dimensions**:

| Dimension | Scope | Determined By |
|-----------|-------|---------------|
| Single transaction | min/max per payment method | Merchant configuration (PSP constraints + platform policy) |
| Daily cumulative | per player | KYC level determines the cap |
| Monthly cumulative | per player | VIP level determines the cap |

The checker queries `DepositHistoryRepository` for cumulative totals in the current day/month window. Daily resets at midnight UTC; monthly resets at the 1st of each month UTC.

**File**: `payment-deposit/src/main/java/com/funding/deposit/limit/DepositLimitChecker.java`

```java
/**
 * Verifies a deposit amount against single-transaction, daily, and monthly limits.
 *
 * Dependencies:
 * - ConfigParameterReader (from shared/, section-01) for limit thresholds
 * - DepositHistoryRepository (this module) for cumulative totals
 */
@Service
public class DepositLimitChecker {

    /**
     * @param playerId      the player requesting the deposit
     * @param tenantId      tenant for config resolution
     * @param amount        the deposit amount
     * @param paymentMethod the payment method (limits vary per method)
     * @return LimitCheckResult with status (APPROVED/REJECTED) and details
     */
    public LimitCheckResult verify(UUID playerId, UUID tenantId,
                                   Money amount, PaymentMethod paymentMethod) {
        // 1. Resolve limits via three-layer override: Jurisdiction -> Brand -> Global
        // 2. Check single transaction min/max
        // 3. Query daily cumulative and check against cap
        // 4. Query monthly cumulative and check against cap
        // 5. Return APPROVED or REJECTED with specific violation reason and remaining allowance
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/limit/LimitCheckResult.java`

```java
public record LimitCheckResult(
    LimitCheckStatus status,
    LimitViolationReason reason,      // null when APPROVED
    Money remainingDaily,             // remaining daily allowance
    Money remainingMonthly            // remaining monthly allowance
) {
    public enum LimitCheckStatus { APPROVED, REJECTED }
    public enum LimitViolationReason {
        MIN_AMOUNT_VIOLATION,
        MAX_AMOUNT_VIOLATION,
        DAILY_LIMIT_EXCEEDED,
        MONTHLY_LIMIT_EXCEEDED
    }
}
```

---

### 2.4 PSP Callback Handler

Receives asynchronous callbacks from PSPs after the player completes (or fails) payment on the PSP's page. Each PSP uses a different callback format; the handler delegates to the appropriate `PSPAdapter` (from section-07) for signature verification and payload parsing.

**File**: `payment-deposit/src/main/java/com/funding/deposit/callback/PspCallbackHandler.java`

```java
/**
 * Handles PSP callbacks for deposit confirmation.
 *
 * Callback processing is idempotent: duplicate callbacks for the same
 * depositOrderId are acknowledged (HTTP 200) without double-crediting.
 *
 * On success:
 * 1. Verify callback signature via PSPAdapter
 * 2. Parse and validate the callback payload
 * 3. Convert currency if needed (via CurrencyEngine)
 * 4. Credit player's CASH wallet (via WalletCreditService)
 * 5. Write DepositCompletedEvent to Kafka outbox
 * 6. Dispatch player notification
 *
 * On failure:
 * - Invalid signature -> reject callback, raise security alert
 * - Payment FAILED -> update DepositOrder status, notify player
 */
@Service
public class PspCallbackHandler {

    public CallbackProcessingResult handleCallback(
            String pspId, RawCallback rawCallback) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/callback/PspCallbackController.java`

```java
/**
 * REST controller exposing PSP callback endpoints.
 * Each PSP has a dedicated callback URL:
 *   POST /api/v1/deposit/callback/{pspId}
 *
 * These endpoints are publicly accessible (no player auth)
 * but secured by PSP-specific signature verification.
 */
@RestController
@RequestMapping("/api/v1/deposit/callback")
public class PspCallbackController {

    @PostMapping("/{pspId}")
    public ResponseEntity<String> handleCallback(
            @PathVariable String pspId,
            @RequestBody String rawBody,
            @RequestHeader Map<String, String> headers) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.5 3D Secure / SCA Integration

For EU (PSD2) and UK markets, card deposits must go through 3D Secure 2.0. This is a regulatory requirement -- no 3DS means no launch in regulated EU/UK markets.

**3DS flow** (inserted between PSP redirect and payment completion):

```
Player submits card deposit
  -> SmartRouter selects PSP
  -> PSPAdapter.initiateDeposit() -> PSP returns 3DS challenge URL (if required)
  -> Player completes 3DS challenge on issuer's page
  -> PSP sends 3DS callback with authentication result
  -> ThreeDSHandler.handle3DSCallback() -> verify authentication status
     -> AUTHENTICATED -> proceed with payment
     -> FAILED -> reject deposit, log reason
     -> ATTEMPTED (issuer not enrolled) -> proceed (liability shift to issuer)
  -> PSP processes payment -> callback -> credit wallet
```

**3DS authentication result handling**:

| Result | Action | Liability |
|--------|--------|-----------|
| `AUTHENTICATED` | Proceed with payment processing | Issuer bears fraud liability |
| `FAILED` | Reject deposit, notify player, log failure reason | N/A -- no transaction |
| `ATTEMPTED` | Proceed with payment (issuer not enrolled in 3DS) | Liability shifts to issuer |

**3DS decision logic**: The PSP determines whether frictionless or challenge flow is needed based on the card issuer's risk assessment. The platform always sends full 3DS data (device fingerprint, billing address, transaction history) to maximize frictionless approval rates.

**SCA triggers (UK-specific)**:
- Single transaction > GBP 30 -> SCA required
- Cumulative amount > GBP 100 since last SCA -> SCA required (even if single transaction is below GBP 30)
- Cumulative tracking uses a Redis 24h sliding window, scoped per player per payment method

The `PSPAdapter` interface (defined in section-07) is extended with 3DS-specific methods:

```java
/**
 * 3DS-specific extension to the PSPAdapter interface.
 * These methods are added to the PSPAdapter defined in section-07.
 */
public interface PSPAdapter {
    // ... existing methods from section-07 ...

    /**
     * Initiates a 3DS challenge with the PSP.
     * Sends device fingerprint, billing address, and transaction history
     * to maximize frictionless approval rates.
     */
    ThreeDSChallengeResult initiate3DSChallenge(DepositRequest request);

    /**
     * Handles the 3DS authentication callback from the PSP.
     * Parses the authentication result (AUTHENTICATED/FAILED/ATTEMPTED).
     */
    ThreeDSAuthResult handle3DSCallback(RawCallback callback);
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/threeds/ThreeDSHandler.java`

```java
/**
 * Manages 3D Secure authentication flow for card deposits.
 *
 * Determines whether 3DS is required based on:
 * - Jurisdiction (EU/UK markets require 3DS)
 * - SCA cumulative threshold (UK: GBP 100 in 24h sliding window)
 * - Single transaction threshold (UK: GBP 30)
 *
 * Dependencies:
 * - JurisdictionResolver (determines if 3DS is required for the market)
 * - PSPAdapter (initiates 3DS challenge and handles callback)
 * - ScaCumulativeTracker (Redis-backed cumulative amount tracker)
 */
@Service
public class ThreeDSHandler {

    /**
     * Determines if 3DS is needed and initiates the challenge if so.
     * @return ThreeDSInitiationResult with challengeUrl (if 3DS needed) or passthrough flag
     */
    public ThreeDSInitiationResult initiate(DepositRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Handles the 3DS callback after the player completes (or fails) the challenge.
     * @return ThreeDSOutcome: PROCEED, REJECT, or PROCEED_WITH_LIABILITY_SHIFT
     */
    public ThreeDSOutcome handle3DSCallback(String depositOrderId,
                                            RawCallback callback) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/threeds/ThreeDSAuthResult.java`

```java
public enum ThreeDSAuthStatus {
    AUTHENTICATED,   // Full 3DS authentication successful
    FAILED,          // 3DS authentication failed
    ATTEMPTED        // Issuer not enrolled in 3DS (liability shift)
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/threeds/ScaCumulativeTracker.java`

```java
/**
 * Tracks cumulative deposit amounts per player per payment method
 * in a Redis 24h sliding window. Used to determine SCA trigger
 * for UK transactions.
 *
 * Key format: sca:cumulative:{playerId}:{paymentMethodHash}
 * Uses Redis sorted sets with timestamps as scores for windowed queries.
 * TTL: 24 hours on the key.
 *
 * Thresholds (UK-specific, DB-configurable):
 * - Single transaction: GBP 30
 * - Cumulative (24h): GBP 100
 */
@Service
public class ScaCumulativeTracker {

    /**
     * Returns the cumulative amount in the 24h sliding window.
     */
    public Money getCumulativeAmount(UUID playerId, PaymentMethod method) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Records a completed deposit amount in the sliding window.
     */
    public void recordDeposit(UUID playerId, PaymentMethod method, Money amount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Determines if SCA is required based on single and cumulative thresholds.
     */
    public boolean isScaRequired(UUID playerId, PaymentMethod method,
                                 Money transactionAmount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.6 Deposit Notification

Dispatches notifications to the player upon deposit completion or failure. Notification delivery failures must never block or roll back the deposit transaction.

**File**: `payment-deposit/src/main/java/com/funding/deposit/notification/DepositNotificationService.java`

```java
/**
 * Sends deposit outcome notifications to the player.
 * Notification is fire-and-forget from the deposit flow perspective:
 * failures are logged but do not affect deposit status.
 */
@Service
public class DepositNotificationService {

    public void notifySuccess(UUID playerId, Money amount, Money newBalance) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void notifyFailure(UUID playerId, Money amount, String reason) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.7 Domain Model

**File**: `payment-deposit/src/main/java/com/funding/deposit/model/DepositOrder.java`

```java
/**
 * Tracks a deposit through its lifecycle.
 * Persisted in PostgreSQL with full audit trail.
 */
@Entity
@Table(name = "deposit_orders")
public class DepositOrder {

    @Id
    private UUID id;
    private UUID playerId;
    private UUID tenantId;

    @Embedded
    private Money amount;              // Original deposit amount

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "settlement_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "settlement_currency"))
    })
    private Money settlementAmount;    // After FX conversion (null until conversion step)

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    private DepositStatus status;

    @Enumerated(EnumType.STRING)
    private FrictionTier frictionTier;

    private String pspId;
    private String pspTransactionRef;

    @Enumerated(EnumType.STRING)
    private ThreeDSAuthStatus threeDsStatus;   // null for non-card or non-EU/UK

    private boolean liabilityShiftToIssuer;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
}
```

**File**: `payment-deposit/src/main/java/com/funding/deposit/model/DepositStatus.java`

```java
public enum DepositStatus {
    INITIATED,
    LIMITS_CHECKED,
    FRICTION_EVALUATED,
    PSP_SELECTED,
    REDIRECTED,
    AWAITING_3DS,
    AWAITING_CALLBACK,
    PROCESSING,
    COMPLETED,
    FAILED,
    REJECTED
}
```

---

## 3. File Structure Summary

```
payment-deposit/
├── src/main/java/com/funding/deposit/
│   ├── service/
│   │   ├── DepositService.java                    # Flow orchestration
│   │   └── CashierFrictionEvaluator.java          # Friction tier evaluation
│   ├── limit/
│   │   ├── DepositLimitChecker.java               # Three-dimensional limit verification
│   │   └── LimitCheckResult.java                  # Limit check response record
│   ├── callback/
│   │   ├── PspCallbackHandler.java                # PSP callback processing
│   │   └── PspCallbackController.java             # REST endpoint for PSP callbacks
│   ├── threeds/
│   │   ├── ThreeDSHandler.java                    # 3DS flow management
│   │   ├── ThreeDSAuthResult.java                 # 3DS authentication status enum
│   │   └── ScaCumulativeTracker.java              # Redis-backed SCA cumulative tracker
│   ├── notification/
│   │   └── DepositNotificationService.java        # Player notification dispatch
│   ├── model/
│   │   ├── DepositOrder.java                      # Deposit lifecycle entity
│   │   ├── DepositStatus.java                     # Status enum
│   │   ├── FrictionTier.java                      # Friction tier enum (LOW/MEDIUM/HIGH)
│   │   ├── DepositRequest.java                    # Inbound deposit request DTO
│   │   └── DepositInitiationResult.java           # Deposit initiation response DTO
│   └── repository/
│       ├── DepositOrderRepository.java            # JPA repository for DepositOrder
│       └── DepositHistoryRepository.java          # Queries for cumulative totals
│
└── src/test/java/com/funding/deposit/
    ├── service/
    │   ├── DepositServiceTest.java                # Deposit flow orchestration tests
    │   └── CashierFrictionEvaluatorTest.java      # Friction tier tests
    ├── limit/
    │   └── DepositLimitCheckerTest.java           # Limit checker tests
    ├── callback/
    │   └── PspCallbackHandlerTest.java            # Callback handler tests
    ├── threeds/
    │   ├── ThreeDSHandlerTest.java                # 3DS flow tests
    │   └── ScaCumulativeTrackerIntegrationTest.java  # SCA tracker integration tests
    └── notification/
        └── DepositNotificationServiceTest.java    # Notification tests
```

---

## 4. Dependencies on Other Sections

| Dependency | What Is Used | How It Is Consumed |
|------------|-------------|-------------------|
| **section-01 (Foundation)** | `Money` value object, `ConfigParameterReader` (three-layer override: Jurisdiction -> Brand -> Global), base entity classes, Kafka outbox publisher, Testcontainers setup | `Money` wraps all monetary amounts. `ConfigParameterReader` resolves deposit limits and SCA thresholds. Kafka outbox publishes `DepositCompletedEvent`. |
| **section-06 (Currency Engine)** | `CurrencyEngine.convert()`, FX rate freezing (15-minute lock) | Called during callback processing when the deposit currency differs from the player's wallet settlement currency. The FX rate is frozen at conversion time. |
| **section-07 (Payment Gateway)** | `SmartRouter.route()`, `PSPAdapter` interface (including `initiateDeposit()`, `initiate3DSChallenge()`, `handle3DSCallback()`), circuit breaker per PSP | SmartRouter selects the optimal PSP. PSPAdapter handles PSP-specific protocols. Circuit breaker state is respected (OPEN PSPs are not routed to). |

These modules must be implemented (at minimum as interfaces/stubs) before this section can compile and run its tests.

---

## 5. Kafka Events

| Event | Topic | Published When |
|-------|-------|---------------|
| `DepositCompletedEvent` | `funding.deposit.completed` | After wallet credit succeeds |
| `DepositFailedEvent` | `funding.deposit.failed` | On PSP failure or 3DS rejection |
| `DepositRejectedEvent` | `funding.deposit.rejected` | On limit violation or high-friction rejection |

All events are written to the transactional outbox table (from section-01) within the same DB transaction as the business operation, ensuring exactly-once delivery semantics.

---

## 6. Database Tables

```sql
-- Deposit order tracking
CREATE TABLE deposit_orders (
    id                      UUID PRIMARY KEY,
    player_id               UUID NOT NULL,
    tenant_id               UUID NOT NULL,
    amount                  NUMERIC(19,4) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    settlement_amount       NUMERIC(19,4),
    settlement_currency     VARCHAR(3),
    payment_method          VARCHAR(32) NOT NULL,
    status                  VARCHAR(32) NOT NULL,
    friction_tier           VARCHAR(8),
    psp_id                  VARCHAR(64),
    psp_transaction_ref     VARCHAR(128),
    three_ds_status         VARCHAR(16),
    liability_shift_issuer  BOOLEAN DEFAULT FALSE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at            TIMESTAMPTZ,

    CONSTRAINT chk_status CHECK (status IN (
        'INITIATED','LIMITS_CHECKED','FRICTION_EVALUATED','PSP_SELECTED',
        'REDIRECTED','AWAITING_3DS','AWAITING_CALLBACK','PROCESSING',
        'COMPLETED','FAILED','REJECTED'
    ))
);

CREATE INDEX idx_deposit_player_status ON deposit_orders(player_id, status);
CREATE INDEX idx_deposit_tenant_created ON deposit_orders(tenant_id, created_at);

-- Deposit limits (managed via ConfigParameterReader three-layer override)
-- Limits are stored in the shared config_parameters table (section-01).
-- Key format: deposit.limit.{dimension}.{paymentMethod}
-- Example: deposit.limit.single.max.CARD = 5000.00
--          deposit.limit.daily.KYC_L1 = 2000.00
--          deposit.limit.monthly.VIP_GOLD = 50000.00
```
