# Section 07 -- Payment Gateway

> **Module**: `payment-gateway`
> **Plan refs**: claude-plan.md SS7 (Payment Gateway)
> **TDD refs**: claude-plan-tdd.md SS7.1--7.4
> **Dependencies**: section-01-foundation, section-06-currency-engine
> **Blocks**: section-08-payment-deposit, section-09-payment-withdrawal
> **Java**: 17+ / Spring Boot 3.x / Gradle

---

## Table of Contents

1. [Tests (TDD Stubs)](#1-tests-tdd-stubs)
   - 1.1 Smart Router Tests
   - 1.2 Circuit Breaker Tests
   - 1.3 PSP Adapter Layer Tests
   - 1.4 Retry Strategy Tests
2. [Implementation Stubs](#2-implementation-stubs)
   - 2.1 Domain Model
   - 2.2 Smart Router
   - 2.3 Circuit Breaker
   - 2.4 PSP Adapter Layer
   - 2.5 Retry Strategy
   - 2.6 PSP Health Monitoring
3. [File Paths](#3-file-paths)
4. [Dependencies](#4-dependencies)

---

## 1. Tests (TDD Stubs)

> Write all tests FIRST. Implementation stubs follow in Section 2.

### 1.1 Smart Router Tests (6 tests)

**File**: `payment-gateway/src/test/java/com/funding/gateway/router/SmartRouterTest.java`

```java
package com.funding.gateway.router;

import com.funding.gateway.circuitbreaker.CircuitBreakerRegistry;
import com.funding.gateway.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmartRouterTest {

    @Mock
    private PSPConfigRepository pspConfigRepository;

    @Mock
    private RoutingScoreCalculator scoreCalculator;

    @Mock
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @InjectMocks
    private SmartRouterImpl smartRouter;

    private PaymentRequest baseRequest;

    @BeforeEach
    void setUp() {
        baseRequest = PaymentRequest.builder()
                .tenantId(UUID.randomUUID())
                .playerId(UUID.randomUUID())
                .amount(new BigDecimal("100.0000"))
                .currency("USD")
                .jurisdiction("MT")        // Malta
                .paymentMethod(PaymentMethod.CARD)
                .vipLevel(VipLevel.STANDARD)
                .build();
    }

    @Nested
    @DisplayName("PSP Selection")
    class PspSelection {

        @Test
        @DisplayName("Routes to highest-scoring PSP")
        void routesToHighestScoringPsp() {
            // Given: three PSPs with different composite scores
            //   PSP_A: score 0.85, PSP_B: score 0.72, PSP_C: score 0.60
            // When: route(request) is called
            // Then: RoutingDecision.primaryPSP == PSP_A
            // And: fallbackPSPs == [PSP_B, PSP_C] in descending score order
            // And: scoringBreakdown is populated for audit
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("Jurisdiction-ineligible PSP scores 0")
        void jurisdictionIneligiblePspScoresZero() {
            // Given: PSP_A is not licensed for jurisdiction "MT"
            //   PSP_B is licensed for "MT" with score 0.72
            // When: route(request) with jurisdiction="MT"
            // Then: PSP_A gets composite score = 0
            // And: PSP_B is selected as primary
            // And: scoringBreakdown for PSP_A shows reason JURISDICTION_INELIGIBLE
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("All PSPs score 0 returns NO_ELIGIBLE_PSP error")
        void allPspsScoreZeroReturnsError() {
            // Given: all configured PSPs are ineligible for the request jurisdiction
            // When: route(request)
            // Then: throws NoEligiblePspException
            // And: exception contains list of PSPs with their disqualification reasons
            // And: this is an explicit error, NOT a silent fallback
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("Circuit breaker OPEN on top PSP routes to next")
        void circuitBreakerOpenOnTopPspRoutesToNext() {
            // Given: PSP_A has highest score but circuit breaker state == OPEN
            //   PSP_B has second-highest score, circuit breaker state == CLOSED
            // When: route(request)
            // Then: RoutingDecision.primaryPSP == PSP_B
            // And: PSP_A is excluded from fallbackPSPs
            // And: routingReason contains "PSP_A skipped: circuit breaker OPEN"
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("Scoring Weights")
    class ScoringWeights {

        @Test
        @DisplayName("Scoring weights sum correctly (50 + 30 + 15 + 5 = 100%)")
        void scoringWeightsSumCorrectly() {
            // Given: a PSP with known metrics:
            //   successRate = 0.95, feeRate = 0.02, speedScore = 0.88, vipBonus = 0.0
            // When: calculateScore(pspMetrics, request)
            // Then: score = 0.95*0.50 + (1-0.02)*0.30 + 0.88*0.15 + 0.0*0.05
            //      = 0.475 + 0.294 + 0.132 + 0.0 = 0.901
            // And: all four weight components are present in the breakdown
            // And: weights sum to exactly 1.0
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("VIP player gets VIP bonus factor applied")
        void vipPlayerGetsVipBonusFactor() {
            // Given: player has VipLevel.PLATINUM
            //   PSP_A supports VIP fast-track with vipBonus = 1.0
            //   PSP_B has no VIP support, vipBonus = 0.0
            // When: route(request with vipLevel=PLATINUM)
            // Then: PSP_A score includes vipBonus*0.05 component
            // And: PSP_A score is higher than without VIP factor
            // And: scoringBreakdown shows VIP bonus contribution
            fail("Not yet implemented");
        }
    }
}
```

---

### 1.2 Circuit Breaker Tests (6 tests)

**File**: `payment-gateway/src/test/java/com/funding/gateway/circuitbreaker/PspCircuitBreakerTest.java`

```java
package com.funding.gateway.circuitbreaker;

import com.funding.gateway.model.CircuitBreakerState;
import com.funding.shared.event.KafkaEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PspCircuitBreakerTest {

    @Mock
    private CircuitBreakerConfigProvider configProvider;

    @Mock
    private KafkaEventPublisher eventPublisher;

    private Clock clock;
    private PspCircuitBreaker circuitBreaker;

    private static final String PSP_ID = "PSP_STRIPE";

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-03-27T10:00:00Z"), ZoneId.of("UTC"));
        // Default thresholds: 5 consecutive failures, 10% failure rate in 60s,
        // 60s cooldown, 3 probe successes to close
        when(configProvider.getConfig(PSP_ID)).thenReturn(
                CircuitBreakerConfig.builder()
                        .consecutiveFailureThreshold(5)
                        .failureRateThreshold(0.10)
                        .failureRateWindowSeconds(60)
                        .cooldownSeconds(60)
                        .probeSuccessThreshold(3)
                        .build()
        );
        circuitBreaker = new PspCircuitBreaker(PSP_ID, configProvider, eventPublisher, clock);
    }

    @Nested
    @DisplayName("CLOSED -> OPEN transitions")
    class ClosedToOpen {

        @Test
        @DisplayName("CLOSED -> OPEN after 5 consecutive failures")
        void opensAfterConsecutiveFailures() {
            // Given: circuit breaker is in CLOSED state
            assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreakerState.CLOSED);

            // When: 5 consecutive failures are recorded
            // Then: state transitions to OPEN after the 5th failure
            // And: a Kafka event is published with type CIRCUIT_BREAKER_OPENED
            // And: event contains pspId, previousState=CLOSED, newState=OPEN, trigger=CONSECUTIVE_FAILURES
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("CLOSED -> OPEN after >10% failure rate in 60s window")
        void opensAfterFailureRateExceeded() {
            // Given: circuit breaker is in CLOSED state
            // When: within a 60-second window, >10% of calls fail
            //   e.g., 100 calls total, 11 failures (11% > 10% threshold)
            // Then: state transitions to OPEN
            // And: a Kafka event is published with trigger=FAILURE_RATE_EXCEEDED
            // And: event includes failureRate and windowDuration
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("OPEN -> HALF_OPEN transition")
    class OpenToHalfOpen {

        @Test
        @DisplayName("OPEN -> HALF_OPEN after 60s cooldown")
        void transitionsToHalfOpenAfterCooldown() {
            // Given: circuit breaker is in OPEN state
            // When: 60 seconds have elapsed since opening
            // Then: state transitions to HALF_OPEN
            // And: probe traffic is allowed (5% of requests)
            // And: a Kafka event is published with type CIRCUIT_BREAKER_HALF_OPENED
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("HALF_OPEN transitions")
    class HalfOpenTransitions {

        @Test
        @DisplayName("HALF_OPEN -> CLOSED after 3 consecutive probe successes")
        void closesAfterProbeSuccesses() {
            // Given: circuit breaker is in HALF_OPEN state
            // When: 3 consecutive probe requests succeed
            // Then: state transitions to CLOSED
            // And: all traffic is resumed
            // And: a Kafka event is published with type CIRCUIT_BREAKER_CLOSED
            // And: failure counters are reset to zero
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("HALF_OPEN -> OPEN on any probe failure")
        void opensOnAnyProbeFailure() {
            // Given: circuit breaker is in HALF_OPEN state
            //   2 probe successes have been recorded (not yet 3)
            // When: the next probe request fails
            // Then: state transitions back to OPEN immediately
            // And: cooldown timer resets to 60s from this moment
            // And: a Kafka event is published with type CIRCUIT_BREAKER_REOPENED
            // And: probe success counter resets to 0
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("Event Publishing")
    class EventPublishing {

        @Test
        @DisplayName("State changes publish Kafka events")
        void stateChangesPublishKafkaEvents() {
            // Given: circuit breaker undergoes a full lifecycle:
            //   CLOSED -> OPEN -> HALF_OPEN -> CLOSED
            // When: each transition occurs
            // Then: exactly 3 Kafka events are published (one per transition)
            // And: each event has correct pspId, previousState, newState, timestamp
            // And: events are published to topic "payment.psp.circuit-breaker.state-change"
            fail("Not yet implemented");
        }
    }
}
```

---

### 1.3 PSP Adapter Layer Tests (3 tests)

**File**: `payment-gateway/src/test/java/com/funding/gateway/adapter/PspAdapterTest.java`

```java
package com.funding.gateway.adapter;

import com.funding.gateway.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PspAdapterTest {

    @Nested
    @DisplayName("Decline Code Mapping")
    class DeclineCodeMapping {

        @Test
        @DisplayName("Each adapter correctly maps internal decline codes")
        void adapterMapsDeclineCodesCorrectly() {
            // Given: a PSP-specific raw error code (e.g., Stripe "card_declined",
            //   Adyen "Refused", Worldpay "5")
            // When: the adapter's mapDeclineCode(rawCode) is called
            // Then: returns the correct internal DeclineCode enum value
            //
            // Verify mappings for each adapter:
            //   Stripe "card_declined"      -> DeclineCode.CARD_DECLINED
            //   Stripe "fraudulent"         -> DeclineCode.FRAUD_SUSPECTED
            //   Stripe "processing_error"   -> DeclineCode.PROCESSOR_ERROR
            //   Adyen  "Refused"            -> DeclineCode.CARD_DECLINED
            //   Adyen  "Fraud"              -> DeclineCode.FRAUD_SUSPECTED
            //
            // And: unknown PSP codes map to DeclineCode.UNKNOWN (soft decline)
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("Decline Code Classification")
    class DeclineCodeClassification {

        @Test
        @DisplayName("Hard decline (fraud) -> NEVER retry")
        void hardDeclineNeverRetries() {
            // Given: PSP response with decline code FRAUD_SUSPECTED
            // When: classify the decline code
            // Then: DeclineCategory == HARD
            // And: isRetryable() returns false
            //
            // Also verify these hard decline codes:
            //   FRAUD_SUSPECTED   -> HARD, not retryable
            //   DO_NOT_RETRY      -> HARD, not retryable
            //   CARD_STOLEN       -> HARD, not retryable
            //   CARD_LOST         -> HARD, not retryable
            //   INVALID_CARD      -> HARD, not retryable
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("Soft decline (timeout) -> eligible for retry via fallback PSP")
        void softDeclineIsRetryable() {
            // Given: PSP response with decline code NETWORK_TIMEOUT
            // When: classify the decline code
            // Then: DeclineCategory == SOFT
            // And: isRetryable() returns true
            //
            // Also verify these soft decline codes:
            //   NETWORK_TIMEOUT    -> SOFT, retryable
            //   PROCESSOR_ERROR    -> SOFT, retryable
            //   INSUFFICIENT_FUNDS -> SOFT, retryable
            //   CARD_EXPIRED       -> SOFT, retryable
            //   UNKNOWN            -> SOFT, retryable (safe default)
            fail("Not yet implemented");
        }
    }
}
```

---

### 1.4 Retry Strategy Tests (4 tests)

**File**: `payment-gateway/src/test/java/com/funding/gateway/retry/PaymentRetryStrategyTest.java`

```java
package com.funding.gateway.retry;

import com.funding.gateway.adapter.PSPAdapter;
import com.funding.gateway.adapter.PSPAdapterRegistry;
import com.funding.gateway.model.*;
import com.funding.gateway.router.SmartRouter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRetryStrategyTest {

    @Mock
    private SmartRouter smartRouter;

    @Mock
    private PSPAdapterRegistry adapterRegistry;

    @InjectMocks
    private PaymentRetryStrategy retryStrategy;

    private PaymentRequest baseRequest;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        idempotencyKey = UUID.randomUUID().toString();
        baseRequest = PaymentRequest.builder()
                .tenantId(UUID.randomUUID())
                .playerId(UUID.randomUUID())
                .amount(new BigDecimal("50.0000"))
                .currency("EUR")
                .idempotencyKey(idempotencyKey)
                .build();
    }

    @Nested
    @DisplayName("Retry Decisions")
    class RetryDecisions {

        @Test
        @DisplayName("Hard decline -> no retry, return failure immediately")
        void hardDeclineNoRetry() {
            // Given: PSP_A returns a hard decline (FRAUD_SUSPECTED)
            // When: retryStrategy.execute(request, routingDecision) processes the failure
            // Then: no retry attempt is made
            // And: result status == FAILED
            // And: result.declineCode == FRAUD_SUSPECTED
            // And: result.retryAttempts == 0
            // And: no fallback PSPs are contacted
            fail("Not yet implemented");
        }

        @Test
        @DisplayName("Soft decline -> retry via next fallback PSP")
        void softDeclineRetriesViaFallback() {
            // Given: PSP_A returns a soft decline (NETWORK_TIMEOUT)
            //   RoutingDecision has fallbackPSPs = [PSP_B, PSP_C]
            //   PSP_B succeeds
            // When: retryStrategy.execute(request, routingDecision)
            // Then: PSP_B is attempted after PSP_A failure
            // And: result status == SUCCESS (from PSP_B)
            // And: result.retryAttempts == 1
            // And: result.attemptLog contains both PSP_A failure and PSP_B success
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("Retry Limits")
    class RetryLimits {

        @Test
        @DisplayName("Maximum 3 retry attempts")
        void maximumThreeRetryAttempts() {
            // Given: primary PSP_A fails with PROCESSOR_ERROR (soft decline)
            //   Fallback PSP_B fails with NETWORK_TIMEOUT (soft decline)
            //   Fallback PSP_C fails with PROCESSOR_ERROR (soft decline)
            //   Fallback PSP_D fails with NETWORK_TIMEOUT (soft decline)
            //   (4 PSPs available, but only 3 retries allowed after initial attempt)
            // When: retryStrategy.execute(request, routingDecision)
            // Then: total attempts == 4 (1 primary + 3 retries)
            // And: PSP_D is never contacted (max retries reached after PSP_C)
            //   Wait -- actually: primary=PSP_A(1), retry1=PSP_B(2), retry2=PSP_C(3), retry3=PSP_D(4)
            //   But max 3 retries means total 4 attempts. Let's clarify:
            //   The spec says "Maximum 3 retry attempts" meaning 3 retries AFTER the initial.
            //   So: PSP_A(initial) + PSP_B(retry1) + PSP_C(retry2) + PSP_D(retry3) = 4 total
            //   All fail -> final result is FAILED
            // And: result.retryAttempts == 3
            // And: result.exhaustedAllRetries == true
            fail("Not yet implemented");
        }
    }

    @Nested
    @DisplayName("Idempotency Across Retries")
    class IdempotencyAcrossRetries {

        @Test
        @DisplayName("Idempotency key persists across PSP retries")
        void idempotencyKeyPersistsAcrossRetries() {
            // Given: payment request with idempotencyKey = "idem-key-123"
            //   PSP_A fails with NETWORK_TIMEOUT
            //   PSP_B succeeds
            // When: retryStrategy.execute(request, routingDecision)
            // Then: PSP_A.initiateDeposit() is called with idempotencyKey = "idem-key-123"
            // And: PSP_B.initiateDeposit() is called with the SAME idempotencyKey = "idem-key-123"
            // And: this prevents double-charging if PSP_A actually processed but timed out
            //
            // Verify via ArgumentCaptor that both PSP calls receive identical idempotency key
            fail("Not yet implemented");
        }
    }
}
```

---

## 2. Implementation Stubs

> All classes below are **stubs/signatures only** -- method bodies to be implemented after tests are written.

### 2.1 Domain Model

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PaymentRequest.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable payment request used by the smart router and retry strategy.
 * Carries all information needed for PSP selection and scoring.
 */
@Value
@Builder
public class PaymentRequest {
    UUID tenantId;
    UUID playerId;
    BigDecimal amount;          // always 4 decimal places via Money VO (section-01)
    String currency;            // ISO 4217 code
    String jurisdiction;        // ISO 3166-1 alpha-2 code
    PaymentMethod paymentMethod;
    VipLevel vipLevel;
    String idempotencyKey;      // persists across retries
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PaymentMethod.java`

```java
package com.funding.gateway.model;

public enum PaymentMethod {
    CARD,
    BANK_TRANSFER,
    E_WALLET,
    CRYPTO_BTC,
    CRYPTO_ETH,
    CRYPTO_USDT
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/VipLevel.java`

```java
package com.funding.gateway.model;

public enum VipLevel {
    STANDARD,
    SILVER,
    GOLD,
    PLATINUM,
    DIAMOND
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/RoutingDecision.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Output of the SmartRouter. Contains primary PSP, ordered fallbacks,
 * scoring breakdown for audit, and a human-readable routing reason.
 */
@Value
@Builder
public class RoutingDecision {
    String primaryPSP;
    List<String> fallbackPSPs;                 // ordered by descending score, excludes OPEN breakers
    Map<String, ScoringBreakdown> scoringBreakdown;  // keyed by pspId
    String routingReason;                       // human-readable explanation
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/ScoringBreakdown.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Detailed scoring breakdown per PSP for audit and monitoring.
 * Weights: successRate=50%, fees=30%, speed=15%, vipBonus=5%
 */
@Value
@Builder
public class ScoringBreakdown {
    String pspId;
    BigDecimal successRateScore;    // raw metric * 0.50 weight
    BigDecimal feeScore;            // (1 - feeRate) * 0.30 weight
    BigDecimal speedScore;          // raw metric * 0.15 weight
    BigDecimal vipBonusScore;       // raw metric * 0.05 weight
    BigDecimal compositeScore;      // sum of above
    boolean eligible;               // false if jurisdiction-ineligible or breaker OPEN
    String disqualificationReason;  // null if eligible
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/DeclineCode.java`

```java
package com.funding.gateway.model;

/**
 * Normalized internal decline codes. Each PSP adapter maps its native
 * error codes to one of these. The retry strategy uses the category
 * (HARD vs SOFT) to decide whether to retry.
 */
public enum DeclineCode {

    // --- Hard declines: NEVER retry ---
    FRAUD_SUSPECTED(DeclineCategory.HARD),
    DO_NOT_RETRY(DeclineCategory.HARD),
    CARD_STOLEN(DeclineCategory.HARD),
    CARD_LOST(DeclineCategory.HARD),
    INVALID_CARD(DeclineCategory.HARD),

    // --- Soft declines: eligible for retry via fallback PSP ---
    INSUFFICIENT_FUNDS(DeclineCategory.SOFT),
    CARD_EXPIRED(DeclineCategory.SOFT),
    PROCESSOR_ERROR(DeclineCategory.SOFT),
    NETWORK_TIMEOUT(DeclineCategory.SOFT),
    CARD_DECLINED(DeclineCategory.SOFT),
    UNKNOWN(DeclineCategory.SOFT);    // safe default: treat unknown as retryable

    private final DeclineCategory category;

    DeclineCode(DeclineCategory category) {
        this.category = category;
    }

    public DeclineCategory getCategory() {
        return category;
    }

    public boolean isRetryable() {
        return category == DeclineCategory.SOFT;
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/DeclineCategory.java`

```java
package com.funding.gateway.model;

/**
 * Hard: fraud, invalid card, DO_NOT_RETRY -- NEVER retry.
 * Soft: timeout, processor error, insufficient funds -- retry via fallback PSP.
 */
public enum DeclineCategory {
    HARD,
    SOFT
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/CircuitBreakerState.java`

```java
package com.funding.gateway.model;

/**
 * Three-state circuit breaker model per PSP.
 * <p>
 * CLOSED  -- normal operation, all traffic flows through
 * OPEN    -- PSP is unhealthy, traffic is blocked
 * HALF_OPEN -- cooldown expired, probe traffic (5%) is allowed
 */
public enum CircuitBreakerState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PSPDepositResult.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

/**
 * Result returned by a PSP adapter after a deposit initiation attempt.
 */
@Value
@Builder
public class PSPDepositResult {
    boolean success;
    String pspTransactionId;
    DeclineCode declineCode;        // null on success
    String rawErrorCode;            // PSP-native code for logging
    String redirectUrl;             // for 3DS or PSP-hosted payment page
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PSPWithdrawalResult.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

/**
 * Result returned by a PSP adapter after a withdrawal initiation attempt.
 */
@Value
@Builder
public class PSPWithdrawalResult {
    boolean success;
    String pspTransactionId;
    DeclineCode declineCode;
    String rawErrorCode;
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PSPCallbackResult.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Parsed and normalized callback result from a PSP webhook.
 */
@Value
@Builder
public class PSPCallbackResult {
    String pspTransactionId;
    String internalOrderId;
    boolean success;
    BigDecimal amount;
    String currency;
    DeclineCode declineCode;
    boolean signatureValid;
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/HealthCheckResult.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

@Value
@Builder
public class HealthCheckResult {
    String pspId;
    boolean healthy;
    Duration latency;
    String errorMessage;     // null when healthy
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PaymentAttemptResult.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Final result after the retry strategy has executed.
 * Contains the outcome, all attempts made, and retry metadata.
 */
@Value
@Builder
public class PaymentAttemptResult {
    PaymentStatus status;             // SUCCESS or FAILED
    String successfulPspId;           // null if FAILED
    String pspTransactionId;          // null if FAILED
    DeclineCode finalDeclineCode;     // null if SUCCESS
    int retryAttempts;                // 0 if first PSP succeeded
    boolean exhaustedAllRetries;
    List<AttemptLog> attemptLog;      // ordered list of all attempts
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/PaymentStatus.java`

```java
package com.funding.gateway.model;

public enum PaymentStatus {
    SUCCESS,
    FAILED,
    PENDING         // awaiting async callback (e.g., 3DS)
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/model/AttemptLog.java`

```java
package com.funding.gateway.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Record of a single PSP attempt within a retry sequence.
 */
@Value
@Builder
public class AttemptLog {
    String pspId;
    int attemptNumber;          // 0 = primary, 1-3 = retries
    boolean success;
    DeclineCode declineCode;
    String rawErrorCode;
    Instant timestamp;
}
```

---

### 2.2 Smart Router

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/SmartRouter.java`

```java
package com.funding.gateway.router;

import com.funding.gateway.model.PaymentRequest;
import com.funding.gateway.model.RoutingDecision;

/**
 * Selects the optimal PSP for a payment request using single-pass weighted scoring.
 * <p>
 * Algorithm:
 * 1. Get all configured PSPs for the tenant
 * 2. For each PSP, calculate composite score:
 *    - If jurisdiction prohibits this PSP+method: score = 0
 *    - Otherwise: successRate*0.50 + (1-feeRate)*0.30 + speedScore*0.15 + vipBonus*0.05
 * 3. Skip PSPs with circuit breaker OPEN
 * 4. Select highest-scoring PSP (score > 0) as primary
 * 5. If ALL eligible PSPs score 0: throw NoEligiblePspException
 * 6. Return RoutingDecision with primary + ordered fallback PSPs
 */
public interface SmartRouter {

    RoutingDecision route(PaymentRequest request);
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/SmartRouterImpl.java`

```java
package com.funding.gateway.router;

import com.funding.gateway.circuitbreaker.CircuitBreakerRegistry;
import com.funding.gateway.model.*;
import com.funding.gateway.router.exception.NoEligiblePspException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmartRouterImpl implements SmartRouter {

    private final PSPConfigRepository pspConfigRepository;
    private final RoutingScoreCalculator scoreCalculator;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public SmartRouterImpl(PSPConfigRepository pspConfigRepository,
                           RoutingScoreCalculator scoreCalculator,
                           CircuitBreakerRegistry circuitBreakerRegistry) {
        this.pspConfigRepository = pspConfigRepository;
        this.scoreCalculator = scoreCalculator;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    @Override
    public RoutingDecision route(PaymentRequest request) {
        // TODO: Implement single-pass scoring algorithm
        // 1. Fetch PSP configs for request.tenantId
        // 2. For each PSP: check jurisdiction eligibility, calculate composite score
        // 3. Filter out PSPs with circuit breaker OPEN
        // 4. Sort by composite score descending
        // 5. If none eligible: throw NoEligiblePspException
        // 6. Return RoutingDecision(primary, fallbacks, breakdown, reason)
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/RoutingScoreCalculator.java`

```java
package com.funding.gateway.router;

import com.funding.gateway.model.PaymentRequest;
import com.funding.gateway.model.ScoringBreakdown;
import org.springframework.stereotype.Component;

/**
 * Calculates composite routing score for a single PSP given a payment request.
 * <p>
 * Weights (DB-configurable, defaults below):
 *   - Success rate:  50%  (rolling 1h window with decay, stored in Redis)
 *   - Fees:          30%  (1 - feeRate)
 *   - Speed:         15%  (normalized response time)
 *   - VIP bonus:      5%  (1.0 if PSP supports VIP fast-track and player is VIP)
 */
@Component
public class RoutingScoreCalculator {

    private static final double WEIGHT_SUCCESS_RATE = 0.50;
    private static final double WEIGHT_FEES = 0.30;
    private static final double WEIGHT_SPEED = 0.15;
    private static final double WEIGHT_VIP_BONUS = 0.05;

    /**
     * @param pspConfig    PSP configuration including fee rates and jurisdiction list
     * @param pspMetrics   live PSP metrics (success rate, avg latency) from Redis
     * @param request      current payment request (for jurisdiction check, VIP level)
     * @return scoring breakdown with composite score; eligible=false if jurisdiction blocks it
     */
    public ScoringBreakdown calculate(PSPConfig pspConfig, PSPMetrics pspMetrics, PaymentRequest request) {
        // TODO: Implement scoring formula
        // 1. Check jurisdiction eligibility -> if ineligible, return score=0, eligible=false
        // 2. successRateScore = pspMetrics.successRate * WEIGHT_SUCCESS_RATE
        // 3. feeScore = (1 - pspConfig.feeRate) * WEIGHT_FEES
        // 4. speedScore = pspMetrics.normalizedSpeed * WEIGHT_SPEED
        // 5. vipBonusScore = (isVip && pspSupportsVip) ? 1.0 * WEIGHT_VIP_BONUS : 0.0
        // 6. compositeScore = sum of above
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/PSPConfigRepository.java`

```java
package com.funding.gateway.router;

import java.util.List;
import java.util.UUID;

/**
 * Repository for PSP configuration data (DB-backed).
 * Returns all PSPs configured for a given tenant.
 */
public interface PSPConfigRepository {

    List<PSPConfig> findByTenantId(UUID tenantId);

    PSPConfig findByPspId(String pspId);
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/PSPConfig.java`

```java
package com.funding.gateway.router;

import com.funding.gateway.model.PaymentMethod;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DB-configurable PSP configuration.
 */
@Value
@Builder
public class PSPConfig {
    String pspId;
    UUID tenantId;
    String displayName;
    BigDecimal feeRate;                         // e.g., 0.0250 for 2.5%
    Set<String> supportedJurisdictions;          // ISO 3166 codes
    Set<PaymentMethod> supportedPaymentMethods;
    boolean vipFastTrackSupported;
    boolean enabled;
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/PSPMetrics.java`

```java
package com.funding.gateway.router;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

/**
 * Live PSP performance metrics stored in Redis as sliding window counters.
 * Updated on every PSP response. Used by the routing score calculator.
 */
@Value
@Builder
public class PSPMetrics {
    String pspId;
    BigDecimal successRate;       // 0.0 to 1.0, rolling 1h window with decay
    BigDecimal normalizedSpeed;   // 0.0 to 1.0, lower latency = higher score
    long totalRequests;           // in current window
    long failedRequests;          // in current window
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/router/exception/NoEligiblePspException.java`

```java
package com.funding.gateway.router.exception;

import com.funding.gateway.model.ScoringBreakdown;

import java.util.Map;

/**
 * Thrown when ALL PSPs score 0 for a given payment request.
 * This is an explicit error -- not a silent fallback.
 */
public class NoEligiblePspException extends RuntimeException {

    private final Map<String, ScoringBreakdown> scoringBreakdown;

    public NoEligiblePspException(String message, Map<String, ScoringBreakdown> scoringBreakdown) {
        super(message);
        this.scoringBreakdown = scoringBreakdown;
    }

    /**
     * @return breakdown per PSP showing why each scored 0 (for diagnostics)
     */
    public Map<String, ScoringBreakdown> getScoringBreakdown() {
        return scoringBreakdown;
    }
}
```

---

### 2.3 Circuit Breaker

**File**: `payment-gateway/src/main/java/com/funding/gateway/circuitbreaker/PspCircuitBreaker.java`

```java
package com.funding.gateway.circuitbreaker;

import com.funding.gateway.model.CircuitBreakerState;
import com.funding.shared.event.KafkaEventPublisher;

import java.time.Clock;

/**
 * Three-state circuit breaker for a single PSP.
 * <p>
 * State transitions (all thresholds DB-configurable):
 *   CLOSED -> OPEN:      5 consecutive failures OR >10% failure rate in 60s window
 *   OPEN -> HALF_OPEN:   after 60s cooldown
 *   HALF_OPEN -> CLOSED: 3 consecutive probe successes
 *   HALF_OPEN -> OPEN:   any probe failure (cooldown resets)
 * <p>
 * Every state change publishes a Kafka event to topic:
 *   "payment.psp.circuit-breaker.state-change"
 */
public class PspCircuitBreaker {

    private final String pspId;
    private final CircuitBreakerConfigProvider configProvider;
    private final KafkaEventPublisher eventPublisher;
    private final Clock clock;

    private volatile CircuitBreakerState state;
    private int consecutiveFailures;
    private int probeSuccesses;
    // Sliding window counters for failure rate calculation

    public PspCircuitBreaker(String pspId,
                             CircuitBreakerConfigProvider configProvider,
                             KafkaEventPublisher eventPublisher,
                             Clock clock) {
        this.pspId = pspId;
        this.configProvider = configProvider;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.state = CircuitBreakerState.CLOSED;
        this.consecutiveFailures = 0;
        this.probeSuccesses = 0;
    }

    public CircuitBreakerState getState() {
        // TODO: Check if OPEN -> HALF_OPEN transition is due (cooldown expired)
        return state;
    }

    /**
     * Record a successful PSP call. Resets consecutive failure counter.
     * In HALF_OPEN: increments probe success counter; transitions to CLOSED at threshold.
     */
    public void recordSuccess() {
        // TODO: Implement state-aware success recording
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Record a failed PSP call. Increments failure counters.
     * In CLOSED: may trigger OPEN transition.
     * In HALF_OPEN: immediately transitions back to OPEN.
     */
    public void recordFailure() {
        // TODO: Implement state-aware failure recording
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @return true if traffic should be allowed through (CLOSED or HALF_OPEN probe)
     */
    public boolean allowRequest() {
        // TODO: CLOSED -> allow all, OPEN -> block all, HALF_OPEN -> allow 5% probe
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Publish state change event to Kafka.
     */
    private void publishStateChangeEvent(CircuitBreakerState previousState,
                                         CircuitBreakerState newState,
                                         String trigger) {
        // TODO: Build and publish CircuitBreakerStateChangeEvent to Kafka
        // Topic: "payment.psp.circuit-breaker.state-change"
        // Payload: pspId, previousState, newState, trigger, timestamp
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/circuitbreaker/CircuitBreakerRegistry.java`

```java
package com.funding.gateway.circuitbreaker;

import com.funding.gateway.model.CircuitBreakerState;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry that manages one PspCircuitBreaker instance per PSP.
 * Lazily creates breakers on first access.
 */
@Component
public class CircuitBreakerRegistry {

    private final Map<String, PspCircuitBreaker> breakers = new ConcurrentHashMap<>();
    private final CircuitBreakerConfigProvider configProvider;

    public CircuitBreakerRegistry(CircuitBreakerConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public PspCircuitBreaker getBreaker(String pspId) {
        // TODO: Lazily create and cache PspCircuitBreaker per pspId
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public CircuitBreakerState getState(String pspId) {
        return getBreaker(pspId).getState();
    }

    /**
     * @return snapshot of all PSP circuit breaker states (for health dashboard)
     */
    public Map<String, CircuitBreakerState> getAllStates() {
        // TODO: Return current state of each registered breaker
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/circuitbreaker/CircuitBreakerConfig.java`

```java
package com.funding.gateway.circuitbreaker;

import lombok.Builder;
import lombok.Value;

/**
 * DB-configurable circuit breaker thresholds per PSP.
 */
@Value
@Builder
public class CircuitBreakerConfig {
    int consecutiveFailureThreshold;    // default: 5
    double failureRateThreshold;        // default: 0.10 (10%)
    int failureRateWindowSeconds;       // default: 60
    int cooldownSeconds;                // default: 60
    int probeSuccessThreshold;          // default: 3
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/circuitbreaker/CircuitBreakerConfigProvider.java`

```java
package com.funding.gateway.circuitbreaker;

/**
 * Provides DB-configurable circuit breaker thresholds per PSP.
 * Falls back to system defaults if no PSP-specific config exists.
 */
public interface CircuitBreakerConfigProvider {

    CircuitBreakerConfig getConfig(String pspId);
}
```

---

### 2.4 PSP Adapter Layer

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/PSPAdapter.java`

```java
package com.funding.gateway.adapter;

import com.funding.gateway.model.*;

/**
 * Contract for PSP-specific adapter implementations.
 * Each PSP has a dedicated adapter that handles:
 *   - API authentication
 *   - Request format normalization
 *   - Response parsing
 *   - Error code mapping to internal DeclineCode enum
 *   - Webhook/callback signature verification
 */
public interface PSPAdapter {

    /** Unique PSP identifier (e.g., "STRIPE", "ADYEN", "WORLDPAY") */
    String getPSPId();

    /** Initiate a deposit via this PSP */
    PSPDepositResult initiateDeposit(DepositRequest request);

    /** Initiate a withdrawal via this PSP */
    PSPWithdrawalResult initiateWithdrawal(WithdrawalRequest request);

    /** Parse and verify an incoming PSP webhook callback */
    PSPCallbackResult parseCallback(RawCallback callback);

    /** Active health check against PSP API */
    HealthCheckResult healthCheck();

    /**
     * Map a PSP-native error code to internal DeclineCode.
     * Unknown codes map to DeclineCode.UNKNOWN (soft decline, retryable).
     */
    DeclineCode mapDeclineCode(String rawErrorCode);
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/PSPAdapterRegistry.java`

```java
package com.funding.gateway.adapter;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of all PSP adapter implementations.
 * Adapters are auto-discovered via Spring DI (all beans implementing PSPAdapter).
 */
@Component
public class PSPAdapterRegistry {

    private final Map<String, PSPAdapter> adapters;

    public PSPAdapterRegistry(List<PSPAdapter> adapterList) {
        this.adapters = adapterList.stream()
                .collect(Collectors.toMap(PSPAdapter::getPSPId, Function.identity()));
    }

    public PSPAdapter getAdapter(String pspId) {
        PSPAdapter adapter = adapters.get(pspId);
        if (adapter == null) {
            throw new IllegalArgumentException("No adapter registered for PSP: " + pspId);
        }
        return adapter;
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/DepositRequest.java`

```java
package com.funding.gateway.adapter;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Normalized deposit request sent to PSP adapters.
 */
@Value
@Builder
public class DepositRequest {
    UUID orderId;
    UUID playerId;
    BigDecimal amount;
    String currency;
    String idempotencyKey;
    String paymentMethodToken;     // tokenized card/bank details
    String returnUrl;              // redirect after 3DS or PSP page
    String callbackUrl;            // webhook URL for async result
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/WithdrawalRequest.java`

```java
package com.funding.gateway.adapter;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Normalized withdrawal request sent to PSP adapters.
 */
@Value
@Builder
public class WithdrawalRequest {
    UUID orderId;
    UUID playerId;
    BigDecimal amount;
    String currency;
    String idempotencyKey;
    String destinationToken;      // tokenized bank/card/crypto address
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/RawCallback.java`

```java
package com.funding.gateway.adapter;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Raw webhook callback from a PSP, before parsing.
 */
@Value
@Builder
public class RawCallback {
    String pspId;
    Map<String, String> headers;
    String body;
    String signature;
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/AbstractPSPAdapter.java`

```java
package com.funding.gateway.adapter;

import com.funding.gateway.model.DeclineCode;

import java.util.Map;

/**
 * Base class for PSP adapters. Provides common decline code mapping logic.
 * Subclasses provide PSP-specific code mappings and API integration.
 */
public abstract class AbstractPSPAdapter implements PSPAdapter {

    /**
     * @return PSP-specific mapping of raw error codes to internal DeclineCode.
     *         Subclasses must populate this map.
     */
    protected abstract Map<String, DeclineCode> getDeclineCodeMapping();

    @Override
    public DeclineCode mapDeclineCode(String rawErrorCode) {
        if (rawErrorCode == null) {
            return DeclineCode.UNKNOWN;
        }
        return getDeclineCodeMapping().getOrDefault(rawErrorCode, DeclineCode.UNKNOWN);
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/impl/StripePSPAdapter.java`

```java
package com.funding.gateway.adapter.impl;

import com.funding.gateway.adapter.*;
import com.funding.gateway.model.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stripe PSP adapter implementation.
 * Maps Stripe-specific error codes to internal DeclineCode enum.
 */
@Component
public class StripePSPAdapter extends AbstractPSPAdapter {

    private static final String PSP_ID = "STRIPE";

    private static final Map<String, DeclineCode> DECLINE_CODE_MAP = Map.of(
            "card_declined", DeclineCode.CARD_DECLINED,
            "fraudulent", DeclineCode.FRAUD_SUSPECTED,
            "processing_error", DeclineCode.PROCESSOR_ERROR,
            "expired_card", DeclineCode.CARD_EXPIRED,
            "insufficient_funds", DeclineCode.INSUFFICIENT_FUNDS,
            "stolen_card", DeclineCode.CARD_STOLEN,
            "lost_card", DeclineCode.CARD_LOST
    );

    @Override
    public String getPSPId() {
        return PSP_ID;
    }

    @Override
    protected Map<String, DeclineCode> getDeclineCodeMapping() {
        return DECLINE_CODE_MAP;
    }

    @Override
    public PSPDepositResult initiateDeposit(DepositRequest request) {
        // TODO: Implement Stripe API call for deposit
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PSPWithdrawalResult initiateWithdrawal(WithdrawalRequest request) {
        // TODO: Implement Stripe API call for withdrawal
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PSPCallbackResult parseCallback(RawCallback callback) {
        // TODO: Verify Stripe webhook signature, parse event payload
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public HealthCheckResult healthCheck() {
        // TODO: Call Stripe health/status endpoint
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-gateway/src/main/java/com/funding/gateway/adapter/impl/AdyenPSPAdapter.java`

```java
package com.funding.gateway.adapter.impl;

import com.funding.gateway.adapter.*;
import com.funding.gateway.model.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Adyen PSP adapter implementation.
 * Maps Adyen-specific refusal reason codes to internal DeclineCode enum.
 */
@Component
public class AdyenPSPAdapter extends AbstractPSPAdapter {

    private static final String PSP_ID = "ADYEN";

    private static final Map<String, DeclineCode> DECLINE_CODE_MAP = Map.of(
            "Refused", DeclineCode.CARD_DECLINED,
            "Fraud", DeclineCode.FRAUD_SUSPECTED,
            "Not enough balance", DeclineCode.INSUFFICIENT_FUNDS,
            "Expired Card", DeclineCode.CARD_EXPIRED,
            "Acquirer Error", DeclineCode.PROCESSOR_ERROR
    );

    @Override
    public String getPSPId() {
        return PSP_ID;
    }

    @Override
    protected Map<String, DeclineCode> getDeclineCodeMapping() {
        return DECLINE_CODE_MAP;
    }

    @Override
    public PSPDepositResult initiateDeposit(DepositRequest request) {
        // TODO: Implement Adyen API call for deposit
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PSPWithdrawalResult initiateWithdrawal(WithdrawalRequest request) {
        // TODO: Implement Adyen API call for withdrawal
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PSPCallbackResult parseCallback(RawCallback callback) {
        // TODO: Verify Adyen HMAC signature, parse notification payload
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public HealthCheckResult healthCheck() {
        // TODO: Call Adyen health endpoint
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.5 Retry Strategy

**File**: `payment-gateway/src/main/java/com/funding/gateway/retry/PaymentRetryStrategy.java`

```java
package com.funding.gateway.retry;

import com.funding.gateway.adapter.PSPAdapterRegistry;
import com.funding.gateway.model.*;
import com.funding.gateway.router.SmartRouter;
import org.springframework.stereotype.Service;

/**
 * Executes payment attempts with retry logic based on decline code classification.
 * <p>
 * Rules:
 * 1. Hard decline (fraud, invalid card, DO_NOT_RETRY) -> stop immediately, no retry
 * 2. Soft decline (timeout, processor error) -> retry via next fallback PSP
 * 3. Maximum 3 retry attempts (after initial attempt = 4 total PSP calls max)
 * 4. Idempotency key persists across ALL retries (prevents double-charging)
 * 5. After all retries exhausted -> return FAILED with user-friendly message
 */
@Service
public class PaymentRetryStrategy {

    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final SmartRouter smartRouter;
    private final PSPAdapterRegistry adapterRegistry;

    public PaymentRetryStrategy(SmartRouter smartRouter, PSPAdapterRegistry adapterRegistry) {
        this.smartRouter = smartRouter;
        this.adapterRegistry = adapterRegistry;
    }

    /**
     * Execute a payment request with retry logic.
     *
     * @param request          the payment request (idempotencyKey must be set)
     * @param routingDecision  routing decision from SmartRouter (primary + fallbacks)
     * @return final result after all attempts, including full attempt log
     */
    public PaymentAttemptResult execute(PaymentRequest request, RoutingDecision routingDecision) {
        // TODO: Implement retry loop
        // 1. Attempt primary PSP
        // 2. On hard decline -> return FAILED immediately
        // 3. On soft decline -> attempt next fallback PSP
        // 4. Repeat up to MAX_RETRY_ATTEMPTS
        // 5. Ensure idempotencyKey is passed to every PSP call
        // 6. Build and return PaymentAttemptResult with full attempt log
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.6 PSP Health Monitoring

**File**: `payment-gateway/src/main/java/com/funding/gateway/health/PspHealthMonitor.java`

```java
package com.funding.gateway.health;

import com.funding.gateway.adapter.PSPAdapterRegistry;
import com.funding.gateway.circuitbreaker.CircuitBreakerRegistry;
import com.funding.gateway.model.CircuitBreakerState;
import com.funding.gateway.model.HealthCheckResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Periodic health monitor for all registered PSPs.
 * Feeds results into the circuit breaker to maintain accurate state.
 * Exposes real-time PSP health grid for the monitoring dashboard.
 */
@Component
public class PspHealthMonitor {

    private final PSPAdapterRegistry adapterRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PspHealthMonitor(PSPAdapterRegistry adapterRegistry,
                            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.adapterRegistry = adapterRegistry;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Periodic health check for all PSPs. Runs every 30 seconds.
     * Results feed into the circuit breaker: unhealthy -> recordFailure, healthy -> recordSuccess.
     */
    @Scheduled(fixedDelayString = "${payment.gateway.health-check-interval-ms:30000}")
    public void checkAllPspHealth() {
        // TODO: Iterate all registered PSP adapters
        // 1. Call adapter.healthCheck()
        // 2. Feed result to circuit breaker: recordSuccess() or recordFailure()
        // 3. Log results at DEBUG level, WARN for unhealthy PSPs
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * @return real-time PSP health grid for dashboard
     */
    public Map<String, CircuitBreakerState> getHealthGrid() {
        return circuitBreakerRegistry.getAllStates();
    }
}
```

---

## 3. File Paths

### Source Files

```
payment-gateway/src/main/java/com/funding/gateway/
  model/
    PaymentRequest.java
    PaymentMethod.java
    VipLevel.java
    RoutingDecision.java
    ScoringBreakdown.java
    DeclineCode.java
    DeclineCategory.java
    CircuitBreakerState.java
    PSPDepositResult.java
    PSPWithdrawalResult.java
    PSPCallbackResult.java
    HealthCheckResult.java
    PaymentAttemptResult.java
    PaymentStatus.java
    AttemptLog.java
  router/
    SmartRouter.java
    SmartRouterImpl.java
    RoutingScoreCalculator.java
    PSPConfigRepository.java
    PSPConfig.java
    PSPMetrics.java
    exception/
      NoEligiblePspException.java
  circuitbreaker/
    PspCircuitBreaker.java
    CircuitBreakerRegistry.java
    CircuitBreakerConfig.java
    CircuitBreakerConfigProvider.java
  adapter/
    PSPAdapter.java
    PSPAdapterRegistry.java
    AbstractPSPAdapter.java
    DepositRequest.java
    WithdrawalRequest.java
    RawCallback.java
    impl/
      StripePSPAdapter.java
      AdyenPSPAdapter.java
  retry/
    PaymentRetryStrategy.java
  health/
    PspHealthMonitor.java
```

### Test Files

```
payment-gateway/src/test/java/com/funding/gateway/
  router/
    SmartRouterTest.java              (6 tests)
  circuitbreaker/
    PspCircuitBreakerTest.java        (6 tests)
  adapter/
    PspAdapterTest.java               (3 tests)
  retry/
    PaymentRetryStrategyTest.java     (4 tests)
```

**Total**: 19 tests across 4 test classes.

---

## 4. Dependencies

### On Other Sections

| Section | What We Use | Interface Point |
|---------|-------------|-----------------|
| **section-01-foundation** | `Money` value object, base entity classes, Gradle module setup, Testcontainers config, Kafka config | `com.funding.shared.*` |
| **section-06-currency-engine** | `CurrencyConverter` for settlement currency conversion on PSP callbacks | `com.funding.currency.service.CurrencyConverter` |

### Shared Infrastructure Used

| Component | From | Usage |
|-----------|------|-------|
| `KafkaEventPublisher` | `shared/event/` | Circuit breaker state change events |
| DB-configurable params | `shared/config/` | Circuit breaker thresholds, routing weights |

### What Depends on Us

| Section | What They Use |
|---------|---------------|
| **section-08-payment-deposit** | `SmartRouter.route()`, `PSPAdapter.initiateDeposit()`, `PaymentRetryStrategy.execute()` |
| **section-09-payment-withdrawal** | `SmartRouter.route()`, `PSPAdapter.initiateWithdrawal()`, `PaymentRetryStrategy.execute()` |

### External Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot Starter Web | 3.x | REST controller, DI |
| Spring Boot Starter Test | 3.x | Test infrastructure |
| Lombok | latest | `@Value`, `@Builder` for immutable models |
| JUnit 5 | 5.10+ | Test framework |
| Mockito | 5.x | Mocking in unit tests |
| AssertJ | 3.x | Fluent test assertions |
| Spring Kafka | 3.x | Circuit breaker event publishing |
| Micrometer | 1.12+ | PSP metrics, health monitoring |
