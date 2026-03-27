# Section 10 — Payment Dispute (`payment-dispute`)

> **Depends on**: section-01-foundation
> **Blocks**: section-12-integration-tests
> **Parallelizable with**: section-08-payment-deposit, section-09-payment-withdrawal
> **Plan refs**: claude-plan.md §10 Payment Dispute
> **TDD refs**: claude-plan-tdd.md §10.1, §10.2, §10.3

---

## Overview

The `payment-dispute` module manages the full chargeback lifecycle from PSP notification through
evidence collection, representment, and final resolution. It comprises three core engines:

1. **Chargeback Lifecycle State Machine** (Spring Statemachine) - drives the dispute through
   well-defined states with SLA-enforced deadlines and auto-escalation on timeout.
2. **Hybrid Evidence Collector** - auto-gathers transaction data, login history, device fingerprint,
   game session data, KYC status, and interaction history, then packages it for CS human review
   before PSP submission.
3. **Player Penalty Engine** - applies escalating sanctions based on cumulative chargeback count
   and dollar amount.

Chargeback events are published to Kafka topic `funding.payment.chargeback` for consumption by
the risk and data domains. The module exposes Micrometer metric `payment.chargeback.rate`
(rolling 30-day CB count / total transactions) and triggers a P1 alert when the rate reaches 1.0%.

---

## 1. Tests (TDD Stubs)

All tests are written FIRST. Implementation follows.

### 1.1 Chargeback Lifecycle Tests

**File**: `payment-dispute/src/test/java/com/funding/dispute/lifecycle/ChargebackLifecycleTest.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.model.Chargeback;
import com.funding.dispute.model.ChargebackEvent;
import com.funding.dispute.model.ChargebackState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateMachine;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChargebackLifecycleTest {

    @Mock
    private ChargebackRepository chargebackRepository;

    @Mock
    private EvidenceCollector evidenceCollector;

    @Mock
    private ChargebackNotifier chargebackNotifier;

    @Mock
    private Clock clock;

    @InjectMocks
    private ChargebackLifecycleManager lifecycleManager;

    private UUID chargebackId;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        chargebackId = UUID.randomUUID();
        playerId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("10.1 — State transitions follow defined lifecycle")
    class StateTransitions {

        @Test
        @DisplayName("NOTIFICATION -> EVIDENCE_COLLECTION triggers evidence auto-collect")
        void notificationToEvidenceCollection_triggersAutoCollect() {
            // Given: a chargeback in NOTIFICATION state
            // When: transition to EVIDENCE_COLLECTION fires
            // Then: evidenceCollector.autoCollect(chargebackId) is invoked
            // And: CS team is notified
            // And: state persisted as EVIDENCE_COLLECTION
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("EVIDENCE_COLLECTION -> REPRESENTMENT requires evidence package")
        void evidenceCollectionToRepresentment_requiresEvidence() {
            // Given: a chargeback in EVIDENCE_COLLECTION with completed evidence
            // When: CS submits evidence for representment
            // Then: state transitions to REPRESENTMENT
            // And: evidence package is forwarded to PSP
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("REPRESENTMENT -> WON | LOST | PRE_ARBITRATION based on PSP response")
        void representment_branchesOnPspResponse() {
            // Given: a chargeback in REPRESENTMENT
            // When: PSP responds with won / lost / escalate
            // Then: state transitions to WON, LOST, or PRE_ARBITRATION respectively
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("PRE_ARBITRATION -> ARBITRATION -> WON | LOST completes lifecycle")
        void preArbitrationToArbitration_completesLifecycle() {
            // Given: a chargeback in PRE_ARBITRATION
            // When: escalated to ARBITRATION, then resolved
            // Then: final state is WON or LOST
            // And: ChargebackEvent published to Kafka topic funding.payment.chargeback
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("Invalid transitions are rejected")
        void invalidTransitions_rejected() {
            // Given: a chargeback in WON state
            // When: attempt to transition to EVIDENCE_COLLECTION
            // Then: IllegalStateTransitionException thrown
            // And: state remains WON
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }

    @Nested
    @DisplayName("10.1 — 7-day evidence deadline enforced")
    class EvidenceDeadline {

        @Test
        @DisplayName("Evidence submitted within 7 days is accepted")
        void evidenceWithinDeadline_accepted() {
            // Given: a chargeback entered EVIDENCE_COLLECTION 5 days ago
            // When: CS submits evidence package
            // Then: transition to REPRESENTMENT succeeds
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("Evidence submitted after 7 days is rejected by guard")
        void evidenceAfterDeadline_rejectedByGuard() {
            // Given: a chargeback entered EVIDENCE_COLLECTION 8 days ago
            // When: CS attempts to submit evidence
            // Then: EvidenceDeadlineExceededException thrown
            // And: state remains EVIDENCE_COLLECTION (guard blocks transition)
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }

    @Nested
    @DisplayName("10.1 — Timeout -> auto-escalation")
    class TimeoutEscalation {

        @Test
        @DisplayName("EVIDENCE_COLLECTION timeout auto-escalates to REPRESENTMENT with partial evidence")
        void evidenceTimeout_autoEscalates() {
            // Given: a chargeback in EVIDENCE_COLLECTION for 7 days with no submission
            // When: SLA timer fires
            // Then: auto-escalate to REPRESENTMENT with whatever evidence was auto-collected
            // And: CS team notified of auto-escalation
            // And: incident logged for audit trail
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("REPRESENTMENT timeout auto-escalates to PRE_ARBITRATION")
        void representmentTimeout_autoEscalates() {
            // Given: a chargeback in REPRESENTMENT past SLA deadline
            // When: SLA timer fires
            // Then: auto-escalate to PRE_ARBITRATION
            // And: management notified
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }
}
```

### 1.2 Evidence Collection Tests

**File**: `payment-dispute/src/test/java/com/funding/dispute/evidence/EvidenceCollectorTest.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.Chargeback;
import com.funding.dispute.model.EvidencePackage;
import com.funding.dispute.model.EvidenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceCollectorTest {

    @Mock
    private TransactionDetailsFetcher transactionDetailsFetcher;

    @Mock
    private LoginHistoryFetcher loginHistoryFetcher;

    @Mock
    private DeviceFingerprintFetcher deviceFingerprintFetcher;

    @Mock
    private GameSessionFetcher gameSessionFetcher;

    @Mock
    private KycStatusFetcher kycStatusFetcher;

    @Mock
    private InteractionHistoryFetcher interactionHistoryFetcher;

    @InjectMocks
    private HybridEvidenceCollector evidenceCollector;

    private UUID chargebackId;
    private UUID playerId;
    private UUID transactionId;

    @BeforeEach
    void setUp() {
        chargebackId = UUID.randomUUID();
        playerId = UUID.randomUUID();
        transactionId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("10.2 — Auto-collects transaction details, login history, device fingerprint, game data")
    class AutoCollection {

        @Test
        @DisplayName("Auto-collect gathers all six evidence types on chargeback notification")
        void autoCollect_gathersAllEvidenceTypes() {
            // Given: a chargeback notification received for transactionId
            // When: autoCollect(chargebackId) is invoked
            // Then: transactionDetailsFetcher retrieves amount, timestamp, payment method, PSP ref
            // And: loginHistoryFetcher retrieves login sessions around transaction time
            // And: deviceFingerprintFetcher retrieves device fingerprint and IP address
            // And: gameSessionFetcher retrieves game session data and bet patterns
            // And: kycStatusFetcher retrieves KYC verification status at transaction time
            // And: interactionHistoryFetcher retrieves previous CS interaction history
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("Partial fetcher failure does not block other evidence collection")
        void partialFetcherFailure_doesNotBlockOthers() {
            // Given: loginHistoryFetcher throws an exception
            // When: autoCollect(chargebackId) is invoked
            // Then: remaining five evidence types are still collected
            // And: EvidencePackage marks login history as UNAVAILABLE
            // And: warning logged for failed fetcher
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }

    @Nested
    @DisplayName("10.2 — Evidence package structured correctly for CS review")
    class PackageStructure {

        @Test
        @DisplayName("Evidence package contains all required sections and metadata")
        void evidencePackage_structuredCorrectly() {
            // Given: all fetchers return valid data
            // When: autoCollect(chargebackId) completes
            // Then: EvidencePackage contains:
            //   - chargebackId, playerId, transactionId
            //   - createdAt timestamp
            //   - status = PENDING_REVIEW
            //   - sections keyed by EvidenceType enum
            //   - each section has raw data + summary
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("CS review can add context and remove irrelevant evidence sections")
        void csReview_canEditPackage() {
            // Given: an evidence package in PENDING_REVIEW status
            // When: CS agent adds manual notes and removes irrelevant sections
            // Then: package status transitions to REVIEWED
            // And: audit trail records who edited and what changed
            // And: package is ready for PSP submission
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }
}
```

### 1.3 Player Penalty Tests

**File**: `payment-dispute/src/test/java/com/funding/dispute/penalty/PlayerPenaltyEngineTest.java`

```java
package com.funding.dispute.penalty;

import com.funding.common.model.Money;
import com.funding.dispute.model.PenaltyAction;
import com.funding.dispute.model.PenaltyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlayerPenaltyEngineTest {

    @Mock
    private PlayerPenaltyRepository penaltyRepository;

    @Mock
    private PlayerAccountService playerAccountService;

    @Mock
    private PenaltyNotifier penaltyNotifier;

    @InjectMocks
    private DefaultPlayerPenaltyEngine penaltyEngine;

    private UUID playerId;
    private Currency usd;

    @BeforeEach
    void setUp() {
        playerId = UUID.randomUUID();
        usd = Currency.getInstance("USD");
    }

    @Nested
    @DisplayName("10.3 — 1 CB -> FLAG")
    class FirstChargeback {

        @Test
        @DisplayName("First chargeback results in FLAG — monitoring only, no restrictions")
        void firstChargeback_flag() {
            // Given: player has 1 chargeback, cumulative amount < $1,000
            int chargebackCount = 1;
            Money cumulativeAmount = new Money(new BigDecimal("150.00"), usd);

            // When: penaltyEngine.evaluate(playerId, chargebackCount, cumulativeAmount)
            // Then: PenaltyAction.type == FLAG
            // And: player account is NOT restricted
            // And: player is added to monitoring watchlist
            // And: penaltyNotifier sends internal alert (no player-facing action)
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }

    @Nested
    @DisplayName("10.3 — 2 CB -> RESTRICT (cards blocked)")
    class SecondChargeback {

        @Test
        @DisplayName("Second chargeback results in RESTRICT — cards blocked, e-wallet/crypto only")
        void secondChargeback_restrict() {
            // Given: player has 2 chargebacks, cumulative amount < $1,000
            int chargebackCount = 2;
            Money cumulativeAmount = new Money(new BigDecimal("350.00"), usd);

            // When: penaltyEngine.evaluate(playerId, chargebackCount, cumulativeAmount)
            // Then: PenaltyAction.type == RESTRICT
            // And: playerAccountService.blockPaymentMethod(playerId, CARD) is called
            // And: player can still deposit/withdraw via e-wallet and crypto
            // And: player notified of card restriction
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }

    @Nested
    @DisplayName("10.3 — 3 CB -> FREEZE")
    class ThirdChargeback {

        @Test
        @DisplayName("Third chargeback results in FREEZE — account frozen, manual review required")
        void thirdChargeback_freeze() {
            // Given: player has 3 chargebacks, cumulative amount < $1,000
            int chargebackCount = 3;
            Money cumulativeAmount = new Money(new BigDecimal("500.00"), usd);

            // When: penaltyEngine.evaluate(playerId, chargebackCount, cumulativeAmount)
            // Then: PenaltyAction.type == FREEZE
            // And: playerAccountService.freezeAccount(playerId) is called
            // And: all deposits and withdrawals blocked
            // And: CS ticket created for manual review
            // And: player notified of account freeze
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }

    @Nested
    @DisplayName("10.3 — Cumulative >$1K -> PERMANENT_CARD_BAN")
    class CumulativeThreshold {

        @Test
        @DisplayName("Cumulative amount >$1,000 triggers PERMANENT_CARD_BAN regardless of count")
        void cumulativeOverThreshold_permanentCardBan() {
            // Given: player has 2 chargebacks but cumulative amount > $1,000
            int chargebackCount = 2;
            Money cumulativeAmount = new Money(new BigDecimal("1200.00"), usd);

            // When: penaltyEngine.evaluate(playerId, chargebackCount, cumulativeAmount)
            // Then: PenaltyAction.type == PERMANENT_CARD_BAN
            // And: playerAccountService.permanentlyBlockCards(playerId) is called
            // And: RESTRICT penalty from count=2 is also applied (cards already blocked)
            // And: permanent ban flag stored in player profile
            // And: ban cannot be reversed without CFO approval
            throw new UnsupportedOperationException("Test not yet implemented");
        }

        @Test
        @DisplayName("Cumulative exactly $1,000 does NOT trigger PERMANENT_CARD_BAN")
        void cumulativeExactlyThreshold_noCardBan() {
            // Given: player has 1 chargeback with cumulative amount exactly $1,000
            int chargebackCount = 1;
            Money cumulativeAmount = new Money(new BigDecimal("1000.00"), usd);

            // When: penaltyEngine.evaluate(playerId, chargebackCount, cumulativeAmount)
            // Then: PenaltyAction.type == FLAG (from count-based rule only)
            // And: PERMANENT_CARD_BAN is NOT applied (threshold is strictly greater than)
            throw new UnsupportedOperationException("Test not yet implemented");
        }
    }
}
```

---

## 2. Implementation — Chargeback Lifecycle State Machine

### 2.1 State and Event Enums

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/ChargebackState.java`

```java
package com.funding.dispute.model;

/**
 * States for the chargeback lifecycle state machine.
 *
 * Flow: NOTIFICATION -> EVIDENCE_COLLECTION -> REPRESENTMENT
 *       -> WON | LOST | PRE_ARBITRATION -> ARBITRATION -> WON | LOST
 */
public enum ChargebackState {
    NOTIFICATION,
    EVIDENCE_COLLECTION,
    REPRESENTMENT,
    PRE_ARBITRATION,
    ARBITRATION,
    WON,
    LOST
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/ChargebackEvent.java`

```java
package com.funding.dispute.model;

/**
 * Events that trigger state transitions in the chargeback lifecycle.
 */
public enum ChargebackEvent {
    /** PSP sends chargeback notification. */
    CB_RECEIVED,
    /** Evidence auto-collection completed; CS review begins. */
    EVIDENCE_COLLECTED,
    /** CS submits evidence to PSP for representment. */
    EVIDENCE_SUBMITTED,
    /** PSP rules in platform's favour. */
    REPRESENTMENT_WON,
    /** PSP rules in player's (cardholder's) favour. */
    REPRESENTMENT_LOST,
    /** Neither party accepts; escalated to card scheme arbitration. */
    ESCALATE_TO_ARBITRATION,
    /** Card scheme rules in platform's favour. */
    ARBITRATION_WON,
    /** Card scheme rules in player's (cardholder's) favour. */
    ARBITRATION_LOST,
    /** SLA timer expired for current state. */
    SLA_TIMEOUT
}
```

### 2.2 Chargeback Entity

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/Chargeback.java`

```java
package com.funding.dispute.model;

import com.funding.common.model.Money;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent entity representing a chargeback dispute.
 * State is driven by the Spring Statemachine; this entity persists
 * the current state and audit trail.
 */
@Entity
@Table(name = "chargebacks", indexes = {
    @Index(name = "idx_cb_player", columnList = "playerId"),
    @Index(name = "idx_cb_transaction", columnList = "transactionId"),
    @Index(name = "idx_cb_state", columnList = "state")
})
public class Chargeback {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID transactionId;

    /** PSP-assigned chargeback/dispute reference. */
    @Column(nullable = false, unique = true)
    private String pspDisputeReference;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "disputed_amount", precision = 19, scale = 4)),
        @AttributeOverride(name = "currency", column = @Column(name = "disputed_currency"))
    })
    private Money disputedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargebackState state;

    /** Reason code provided by card scheme (e.g. Visa 10.4, MC 4853). */
    @Column(nullable = false)
    private String reasonCode;

    @Column(nullable = false)
    private Instant receivedAt;

    /** Deadline for evidence submission (receivedAt + 7 days). */
    @Column(nullable = false)
    private Instant evidenceDeadline;

    @Column(nullable = false)
    private Instant stateEnteredAt;

    private Instant resolvedAt;

    @Version
    private Long version;

    // --- Constructor, getters, domain methods (stubs) ---

    protected Chargeback() { /* JPA */ }

    public Chargeback(UUID id, UUID tenantId, UUID playerId, UUID transactionId,
                      String pspDisputeReference, Money disputedAmount, String reasonCode,
                      Instant receivedAt) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** Returns true if the current time is past the 7-day evidence deadline. */
    public boolean isEvidenceDeadlineExceeded(Instant now) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /** Transition state; records stateEnteredAt for SLA tracking. */
    public void transitionTo(ChargebackState newState, Instant now) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // Getters omitted for brevity — generate via Lombok @Getter or IDE
}
```

### 2.3 State Machine Configuration

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackStateMachineConfig.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.model.ChargebackEvent;
import com.funding.dispute.model.ChargebackState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

/**
 * Spring Statemachine configuration for the chargeback lifecycle.
 *
 * States: NOTIFICATION -> EVIDENCE_COLLECTION -> REPRESENTMENT
 *         -> WON | LOST | PRE_ARBITRATION -> ARBITRATION -> WON | LOST
 *
 * Guards enforce:
 *   - 7-day evidence deadline on EVIDENCE_COLLECTION -> REPRESENTMENT
 *   - Required fields validation before each transition
 *
 * Actions trigger:
 *   - Auto-collect evidence on NOTIFICATION -> EVIDENCE_COLLECTION
 *   - Notify CS team on state entry
 *   - Update player risk score on resolution
 *
 * Timers:
 *   - SLA countdown per state; auto-escalate on timeout
 */
@Configuration
@EnableStateMachineFactory
public class ChargebackStateMachineConfig
        extends EnumStateMachineConfigurerAdapter<ChargebackState, ChargebackEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<ChargebackState, ChargebackEvent> states)
            throws Exception {
        // Configure: initial=NOTIFICATION, end={WON, LOST}, all states from enum
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<ChargebackState, ChargebackEvent> transitions)
            throws Exception {
        // External transitions:
        //   NOTIFICATION      --CB_RECEIVED-->           EVIDENCE_COLLECTION  (action: autoCollectEvidence)
        //   EVIDENCE_COLLECTION --EVIDENCE_SUBMITTED-->   REPRESENTMENT        (guard: evidenceDeadlineGuard)
        //   EVIDENCE_COLLECTION --SLA_TIMEOUT-->          REPRESENTMENT        (action: autoEscalateWithPartialEvidence)
        //   REPRESENTMENT     --REPRESENTMENT_WON-->     WON
        //   REPRESENTMENT     --REPRESENTMENT_LOST-->    LOST
        //   REPRESENTMENT     --ESCALATE_TO_ARBITRATION--> PRE_ARBITRATION
        //   REPRESENTMENT     --SLA_TIMEOUT-->           PRE_ARBITRATION       (action: autoEscalate)
        //   PRE_ARBITRATION   --ESCALATE_TO_ARBITRATION--> ARBITRATION
        //   ARBITRATION       --ARBITRATION_WON-->       WON
        //   ARBITRATION       --ARBITRATION_LOST-->      LOST
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 2.4 Guards and Actions

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/EvidenceDeadlineGuard.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.model.ChargebackEvent;
import com.funding.dispute.model.ChargebackState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Guard that blocks EVIDENCE_COLLECTION -> REPRESENTMENT if the
 * 7-day evidence deadline has been exceeded.
 */
@Component
public class EvidenceDeadlineGuard implements Guard<ChargebackState, ChargebackEvent> {

    private final Clock clock;

    public EvidenceDeadlineGuard(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean evaluate(StateContext<ChargebackState, ChargebackEvent> context) {
        // Extract Chargeback from state machine extended state
        // Return true if Instant.now(clock) is before evidenceDeadline
        // Return false (block transition) if deadline exceeded
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/AutoCollectEvidenceAction.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.evidence.HybridEvidenceCollector;
import com.funding.dispute.model.ChargebackEvent;
import com.funding.dispute.model.ChargebackState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action triggered on NOTIFICATION -> EVIDENCE_COLLECTION transition.
 * Invokes the hybrid evidence collector to auto-gather all available evidence,
 * then notifies the CS team that a review is required.
 */
@Component
public class AutoCollectEvidenceAction implements Action<ChargebackState, ChargebackEvent> {

    private final HybridEvidenceCollector evidenceCollector;
    private final ChargebackNotifier notifier;

    public AutoCollectEvidenceAction(HybridEvidenceCollector evidenceCollector,
                                     ChargebackNotifier notifier) {
        this.evidenceCollector = evidenceCollector;
        this.notifier = notifier;
    }

    @Override
    public void execute(StateContext<ChargebackState, ChargebackEvent> context) {
        // 1. Extract chargebackId from context
        // 2. evidenceCollector.autoCollect(chargebackId)
        // 3. notifier.notifyCSTeam(chargebackId, "Evidence auto-collected; review required")
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/SlaTimeoutAction.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.model.ChargebackEvent;
import com.funding.dispute.model.ChargebackState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action triggered when an SLA timer fires on a chargeback state.
 * Auto-escalates the dispute to the next state and logs the timeout incident.
 */
@Component
public class SlaTimeoutAction implements Action<ChargebackState, ChargebackEvent> {

    private final ChargebackNotifier notifier;

    public SlaTimeoutAction(ChargebackNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public void execute(StateContext<ChargebackState, ChargebackEvent> context) {
        // 1. Log auto-escalation with current state and chargeback details
        // 2. Notify CS/management of SLA breach
        // 3. Record audit trail entry for timeout escalation
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 2.5 Lifecycle Manager (Orchestrator)

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackLifecycleManager.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.model.Chargeback;
import com.funding.dispute.model.ChargebackEvent;
import com.funding.dispute.model.ChargebackState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates chargeback lifecycle transitions.
 * Hydrates a StateMachine per chargeback, sends events,
 * and persists the resulting state.
 */
@Service
public class ChargebackLifecycleManager {

    private final StateMachineFactory<ChargebackState, ChargebackEvent> stateMachineFactory;
    private final ChargebackRepository chargebackRepository;

    public ChargebackLifecycleManager(
            StateMachineFactory<ChargebackState, ChargebackEvent> stateMachineFactory,
            ChargebackRepository chargebackRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.chargebackRepository = chargebackRepository;
    }

    /**
     * Receives a new chargeback notification from a PSP.
     * Creates the Chargeback entity and fires the initial state transition.
     */
    public Chargeback handleNotification(UUID tenantId, UUID playerId, UUID transactionId,
                                         String pspDisputeReference, String reasonCode) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Fires an event on an existing chargeback's state machine.
     * Validates the transition, persists the new state, and returns the updated entity.
     *
     * @throws IllegalStateTransitionException if the event is not valid for the current state
     */
    public Chargeback sendEvent(UUID chargebackId, ChargebackEvent event) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Hydrates a Spring Statemachine to the persisted state of the given chargeback.
     */
    private StateMachine<ChargebackState, ChargebackEvent> restoreStateMachine(Chargeback chargeback) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 2.6 Repository

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackRepository.java`

```java
package com.funding.dispute.lifecycle;

import com.funding.dispute.model.Chargeback;
import com.funding.dispute.model.ChargebackState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChargebackRepository extends JpaRepository<Chargeback, UUID> {

    Optional<Chargeback> findByPspDisputeReference(String pspDisputeReference);

    List<Chargeback> findByPlayerIdOrderByReceivedAtDesc(UUID playerId);

    /** Count chargebacks for a player (for penalty evaluation). */
    int countByPlayerId(UUID playerId);

    /** Find chargebacks that have exceeded their SLA deadline in a given state. */
    @Query("SELECT c FROM Chargeback c WHERE c.state = :state AND c.stateEnteredAt < :deadline")
    List<Chargeback> findExceededSla(ChargebackState state, Instant deadline);
}
```

---

## 3. Implementation — Hybrid Evidence Collector

### 3.1 Evidence Model

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/EvidenceType.java`

```java
package com.funding.dispute.model;

/** Types of evidence auto-collected for chargeback representment. */
public enum EvidenceType {
    TRANSACTION_DETAILS,
    LOGIN_HISTORY,
    DEVICE_FINGERPRINT,
    GAME_SESSION_DATA,
    KYC_STATUS,
    INTERACTION_HISTORY
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/EvidenceSection.java`

```java
package com.funding.dispute.model;

import java.time.Instant;

/**
 * A single section of collected evidence.
 * Each section corresponds to one EvidenceType.
 */
public record EvidenceSection(
    EvidenceType type,
    EvidenceFetchStatus status,
    String rawData,
    String summary,
    Instant collectedAt
) {
    public enum EvidenceFetchStatus {
        COLLECTED,
        UNAVAILABLE,
        ERROR
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/EvidencePackage.java`

```java
package com.funding.dispute.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Structured evidence package for a chargeback dispute.
 * Auto-populated by the evidence collector, then reviewed/edited by CS before submission.
 */
@Entity
@Table(name = "evidence_packages")
public class EvidencePackage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID chargebackId;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EvidencePackageStatus status;

    @Column(columnDefinition = "jsonb")
    private String sectionsJson; // Serialized List<EvidenceSection>

    /** CS agent notes added during review. */
    @Column(columnDefinition = "text")
    private String reviewNotes;

    private UUID reviewedBy;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant reviewedAt;
    private Instant submittedAt;

    @Version
    private Long version;

    public enum EvidencePackageStatus {
        PENDING_REVIEW,
        REVIEWED,
        SUBMITTED,
        EXPIRED
    }

    protected EvidencePackage() { /* JPA */ }

    // Factory, getters, domain methods — stubs
    public static EvidencePackage create(UUID chargebackId, UUID playerId, UUID transactionId,
                                         List<EvidenceSection> sections) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void markReviewed(UUID agentId, String notes, List<EvidenceType> removedSections) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void markSubmitted() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 3.2 Evidence Fetcher Interfaces

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/EvidenceFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;

import java.util.UUID;

/**
 * Common interface for all evidence data fetchers.
 * Each implementation retrieves a specific type of evidence
 * for a chargeback dispute.
 */
public interface EvidenceFetcher {

    /** The evidence type this fetcher provides. */
    EvidenceType type();

    /**
     * Fetches evidence for the given chargeback context.
     * Implementations should NOT throw exceptions — return an UNAVAILABLE
     * section on failure so that other fetchers can proceed.
     */
    EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId);
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/TransactionDetailsFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fetches transaction amount, timestamp, payment method, PSP reference. */
@Component
public class TransactionDetailsFetcher implements EvidenceFetcher {

    @Override
    public EvidenceType type() { return EvidenceType.TRANSACTION_DETAILS; }

    @Override
    public EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/LoginHistoryFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fetches player login sessions around the transaction time. */
@Component
public class LoginHistoryFetcher implements EvidenceFetcher {

    @Override
    public EvidenceType type() { return EvidenceType.LOGIN_HISTORY; }

    @Override
    public EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/DeviceFingerprintFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fetches device fingerprint and IP address at transaction time. */
@Component
public class DeviceFingerprintFetcher implements EvidenceFetcher {

    @Override
    public EvidenceType type() { return EvidenceType.DEVICE_FINGERPRINT; }

    @Override
    public EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/GameSessionFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fetches game session data: games played, bet patterns. */
@Component
public class GameSessionFetcher implements EvidenceFetcher {

    @Override
    public EvidenceType type() { return EvidenceType.GAME_SESSION_DATA; }

    @Override
    public EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/KycStatusFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fetches KYC verification status at time of transaction. */
@Component
public class KycStatusFetcher implements EvidenceFetcher {

    @Override
    public EvidenceType type() { return EvidenceType.KYC_STATUS; }

    @Override
    public EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/InteractionHistoryFetcher.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidenceSection;
import com.funding.dispute.model.EvidenceType;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Fetches previous CS interaction history for the player. */
@Component
public class InteractionHistoryFetcher implements EvidenceFetcher {

    @Override
    public EvidenceType type() { return EvidenceType.INTERACTION_HISTORY; }

    @Override
    public EvidenceSection fetch(UUID chargebackId, UUID playerId, UUID transactionId) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 3.3 Hybrid Evidence Collector (Orchestrator)

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/HybridEvidenceCollector.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidencePackage;
import com.funding.dispute.model.EvidenceSection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates auto-collection of evidence from all registered fetchers.
 *
 * On chargeback notification:
 * 1. Invokes every registered EvidenceFetcher in parallel
 * 2. Partial failures are tolerated — failed sections marked UNAVAILABLE
 * 3. Produces an EvidencePackage in PENDING_REVIEW status
 * 4. CS team reviews, edits, and submits via the dispute management interface
 */
@Service
public class HybridEvidenceCollector {

    private final List<EvidenceFetcher> fetchers;
    private final EvidencePackageRepository evidencePackageRepository;

    public HybridEvidenceCollector(List<EvidenceFetcher> fetchers,
                                    EvidencePackageRepository evidencePackageRepository) {
        this.fetchers = fetchers;
        this.evidencePackageRepository = evidencePackageRepository;
    }

    /**
     * Auto-collects all available evidence for a chargeback.
     * Each fetcher is invoked independently; failures in one fetcher
     * do not block others.
     *
     * @return the persisted EvidencePackage in PENDING_REVIEW status
     */
    public EvidencePackage autoCollect(UUID chargebackId, UUID playerId, UUID transactionId) {
        // 1. Invoke each EvidenceFetcher, catching exceptions per fetcher
        // 2. Build List<EvidenceSection> (COLLECTED or UNAVAILABLE per fetcher)
        // 3. Create and persist EvidencePackage with status PENDING_REVIEW
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/evidence/EvidencePackageRepository.java`

```java
package com.funding.dispute.evidence;

import com.funding.dispute.model.EvidencePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidencePackageRepository extends JpaRepository<EvidencePackage, UUID> {

    Optional<EvidencePackage> findByChargebackId(UUID chargebackId);
}
```

---

## 4. Implementation — Player Penalty Engine

### 4.1 Penalty Model

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/PenaltyType.java`

```java
package com.funding.dispute.model;

/**
 * Escalating penalty types applied based on chargeback history.
 * Ordered by severity.
 */
public enum PenaltyType {
    /** Monitoring only. Player added to watchlist. */
    FLAG,
    /** Card payments blocked. E-wallet and crypto still allowed. */
    RESTRICT,
    /** Account frozen. All deposits/withdrawals blocked. Manual review required. */
    FREEZE,
    /** Cards permanently banned. Cannot be reversed without CFO approval. */
    PERMANENT_CARD_BAN
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/model/PenaltyAction.java`

```java
package com.funding.dispute.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Result of penalty evaluation. Captures the penalty type,
 * the triggering criteria, and the applied timestamp.
 */
public record PenaltyAction(
    UUID playerId,
    PenaltyType type,
    int chargebackCount,
    String cumulativeAmount,
    String reason,
    Instant appliedAt
) {}
```

### 4.2 Penalty Engine Interface and Implementation

**File**: `payment-dispute/src/main/java/com/funding/dispute/penalty/PlayerPenaltyEngine.java`

```java
package com.funding.dispute.penalty;

import com.funding.common.model.Money;
import com.funding.dispute.model.PenaltyAction;

import java.util.UUID;

/**
 * Evaluates and applies escalating sanctions based on a player's chargeback history.
 *
 * Rules:
 *   1 CB            -> FLAG (monitoring only)
 *   2 CB            -> RESTRICT (block card payments; e-wallet/crypto allowed)
 *   3 CB            -> FREEZE (account frozen; manual review required)
 *   Cumulative >$1K -> PERMANENT_CARD_BAN (regardless of count)
 *
 * Cumulative threshold is strictly greater-than ($1,000.00 exactly does NOT trigger).
 */
public interface PlayerPenaltyEngine {

    PenaltyAction evaluate(UUID playerId, int chargebackCount, Money cumulativeAmount);
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/penalty/DefaultPlayerPenaltyEngine.java`

```java
package com.funding.dispute.penalty;

import com.funding.common.model.Money;
import com.funding.dispute.model.PenaltyAction;
import com.funding.dispute.model.PenaltyType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Default implementation of the player penalty engine.
 * Applies the highest-severity penalty matching the player's chargeback profile.
 *
 * Count-based:  1 -> FLAG, 2 -> RESTRICT, >=3 -> FREEZE
 * Amount-based: cumulative > $1,000 -> PERMANENT_CARD_BAN
 *
 * When both count-based and amount-based rules match, the higher severity wins.
 */
@Service
public class DefaultPlayerPenaltyEngine implements PlayerPenaltyEngine {

    private static final BigDecimal PERMANENT_BAN_THRESHOLD = new BigDecimal("1000.00");

    private final PlayerAccountService playerAccountService;
    private final PlayerPenaltyRepository penaltyRepository;
    private final PenaltyNotifier penaltyNotifier;

    public DefaultPlayerPenaltyEngine(PlayerAccountService playerAccountService,
                                       PlayerPenaltyRepository penaltyRepository,
                                       PenaltyNotifier penaltyNotifier) {
        this.playerAccountService = playerAccountService;
        this.penaltyRepository = penaltyRepository;
        this.penaltyNotifier = penaltyNotifier;
    }

    @Override
    public PenaltyAction evaluate(UUID playerId, int chargebackCount, Money cumulativeAmount) {
        // 1. Determine count-based penalty: 1->FLAG, 2->RESTRICT, >=3->FREEZE
        // 2. Check amount-based: cumulative > $1,000 -> PERMANENT_CARD_BAN
        // 3. Apply the higher-severity penalty
        // 4. Execute penalty side-effects via playerAccountService
        // 5. Persist penalty record
        // 6. Send notifications
        // 7. Return PenaltyAction
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 4.3 Supporting Interfaces (Stubs)

**File**: `payment-dispute/src/main/java/com/funding/dispute/penalty/PlayerAccountService.java`

```java
package com.funding.dispute.penalty;

import java.util.UUID;

/**
 * Interface for applying account-level restrictions as penalty side-effects.
 * Implementation delegates to the Player domain via API or Kafka command.
 */
public interface PlayerAccountService {

    /** Block card payment methods for the player. E-wallet/crypto remain available. */
    void blockPaymentMethod(UUID playerId, String paymentMethodType);

    /** Freeze the player's entire account. All deposits and withdrawals blocked. */
    void freezeAccount(UUID playerId);

    /** Permanently ban card payments. Requires CFO approval to reverse. */
    void permanentlyBlockCards(UUID playerId);

    /** Add player to the chargeback monitoring watchlist. */
    void addToWatchlist(UUID playerId);
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/penalty/PlayerPenaltyRepository.java`

```java
package com.funding.dispute.penalty;

import com.funding.dispute.model.PenaltyAction;
import com.funding.dispute.model.PenaltyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PlayerPenaltyRepository extends JpaRepository<PenaltyAction, UUID> {

    List<PenaltyAction> findByPlayerIdOrderByAppliedAtDesc(UUID playerId);

    boolean existsByPlayerIdAndType(UUID playerId, PenaltyType type);
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/penalty/PenaltyNotifier.java`

```java
package com.funding.dispute.penalty;

import com.funding.dispute.model.PenaltyAction;

import java.util.UUID;

/**
 * Sends notifications for penalty events.
 * Internal alerts go to CS/management; player-facing notifications
 * are sent for RESTRICT and FREEZE penalties.
 */
public interface PenaltyNotifier {

    /** Internal alert — all penalty types. */
    void notifyInternal(PenaltyAction action);

    /** Player-facing notification — RESTRICT, FREEZE, PERMANENT_CARD_BAN only. */
    void notifyPlayer(UUID playerId, PenaltyAction action);
}
```

---

## 5. Implementation — Chargeback Monitoring and Alerting

### 5.1 Chargeback Kafka Event Publisher

**File**: `payment-dispute/src/main/java/com/funding/dispute/event/ChargebackEventPublisher.java`

```java
package com.funding.dispute.event;

import com.funding.dispute.model.Chargeback;
import org.springframework.stereotype.Component;

/**
 * Publishes chargeback events to Kafka topic: funding.payment.chargeback
 * Consumed by: risk domain, data domain.
 *
 * Events are written via the transactional outbox pattern (see section-11)
 * to guarantee at-least-once delivery without dual-write issues.
 */
@Component
public class ChargebackEventPublisher {

    private static final String TOPIC = "funding.payment.chargeback";

    /**
     * Publishes a ChargebackEvent containing: tenantId, playerId, timestamp,
     * transactionId, disputedAmount, currency, chargebackState, reasonCode.
     */
    public void publish(Chargeback chargeback) {
        // Write to outbox table within the same DB transaction as state change
        // Outbox relay (section-11) publishes to Kafka asynchronously
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

### 5.2 Chargeback Rate Monitor

**File**: `payment-dispute/src/main/java/com/funding/dispute/monitoring/ChargebackRateMonitor.java`

```java
package com.funding.dispute.monitoring;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Monitors rolling 30-day chargeback rate as a Micrometer gauge.
 *
 * Metric: payment.chargeback.rate
 * Alert:  >= 1.0% rolling -> P1 severity, notify CFO
 *
 * Card scheme thresholds (for reference):
 *   Visa:       0.9% triggers VDMP (Visa Dispute Monitoring Program)
 *   Mastercard: 1.0% triggers ECP (Excessive Chargeback Program)
 */
@Component
public class ChargebackRateMonitor {

    private final ChargebackRateCalculator rateCalculator;
    private final AlertService alertService;

    public ChargebackRateMonitor(MeterRegistry meterRegistry,
                                  ChargebackRateCalculator rateCalculator,
                                  AlertService alertService) {
        this.rateCalculator = rateCalculator;
        this.alertService = alertService;

        // Register Micrometer gauge
        Gauge.builder("payment.chargeback.rate", rateCalculator, ChargebackRateCalculator::currentRate)
             .description("Rolling 30-day chargeback rate (count / total transactions)")
             .register(meterRegistry);
    }

    /**
     * Scheduled check — evaluates chargeback rate and fires P1 alert
     * if the rate meets or exceeds the 1.0% threshold.
     */
    @Scheduled(fixedDelayString = "${dispute.monitoring.check-interval:300000}") // 5 min default
    public void checkChargebackRate() {
        // 1. Calculate current rolling 30-day rate
        // 2. If rate >= 0.01 (1.0%), fire P1 alert to CFO
        // 3. If rate >= 0.009 (0.9%), fire P2 warning (approaching Visa VDMP threshold)
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/monitoring/ChargebackRateCalculator.java`

```java
package com.funding.dispute.monitoring;

import org.springframework.stereotype.Component;

/**
 * Calculates the rolling 30-day chargeback rate:
 *   rate = chargebackCount(30d) / totalTransactions(30d)
 */
@Component
public class ChargebackRateCalculator {

    /**
     * Returns the current rolling 30-day chargeback rate as a decimal.
     * E.g., 0.008 = 0.8%.
     */
    public double currentRate() {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

**File**: `payment-dispute/src/main/java/com/funding/dispute/monitoring/AlertService.java`

```java
package com.funding.dispute.monitoring;

/**
 * Abstraction for alert delivery (Slack, email, PagerDuty, etc.).
 */
public interface AlertService {

    void fireAlert(String alertName, AlertSeverity severity, String message);

    enum AlertSeverity {
        P0_IMMEDIATE,
        P1_15MIN,
        P2_1H,
        P3_4H
    }
}
```

### 5.3 Chargeback Notifier

**File**: `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackNotifier.java`

```java
package com.funding.dispute.lifecycle;

import java.util.UUID;

/**
 * Sends notifications related to chargeback lifecycle events.
 * Used by state machine actions to notify CS teams and management
 * of transitions, SLA breaches, and required reviews.
 */
public interface ChargebackNotifier {

    /** Notify CS team of a chargeback event requiring action. */
    void notifyCSTeam(UUID chargebackId, String message);

    /** Notify management of an SLA breach or escalation. */
    void notifyManagement(UUID chargebackId, String message);
}
```

---

## 6. Chargeback Service (Entry Point)

**File**: `payment-dispute/src/main/java/com/funding/dispute/service/ChargebackService.java`

```java
package com.funding.dispute.service;

import com.funding.dispute.event.ChargebackEventPublisher;
import com.funding.dispute.lifecycle.ChargebackLifecycleManager;
import com.funding.dispute.model.Chargeback;
import com.funding.dispute.penalty.PlayerPenaltyEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Top-level service for the payment-dispute module.
 * Coordinates between the lifecycle state machine, evidence collection,
 * penalty evaluation, and event publishing.
 */
@Service
public class ChargebackService {

    private final ChargebackLifecycleManager lifecycleManager;
    private final PlayerPenaltyEngine penaltyEngine;
    private final ChargebackEventPublisher eventPublisher;

    public ChargebackService(ChargebackLifecycleManager lifecycleManager,
                              PlayerPenaltyEngine penaltyEngine,
                              ChargebackEventPublisher eventPublisher) {
        this.lifecycleManager = lifecycleManager;
        this.penaltyEngine = penaltyEngine;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Handles an incoming chargeback notification from a PSP.
     * 1. Creates the chargeback and starts the lifecycle state machine
     * 2. Evaluates and applies player penalties
     * 3. Publishes ChargebackEvent to Kafka
     */
    @Transactional
    public Chargeback handleChargebackNotification(UUID tenantId, UUID playerId,
                                                    UUID transactionId,
                                                    String pspDisputeReference,
                                                    String reasonCode) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Advances the chargeback lifecycle when a PSP responds to representment
     * or arbitration.
     */
    @Transactional
    public Chargeback handlePspResponse(UUID chargebackId, String pspResponseCode) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

## 7. File Paths Summary

### Source Files

| Path | Purpose |
|------|---------|
| `payment-dispute/src/main/java/com/funding/dispute/model/ChargebackState.java` | State enum for lifecycle |
| `payment-dispute/src/main/java/com/funding/dispute/model/ChargebackEvent.java` | Event enum for lifecycle |
| `payment-dispute/src/main/java/com/funding/dispute/model/Chargeback.java` | Chargeback JPA entity |
| `payment-dispute/src/main/java/com/funding/dispute/model/EvidenceType.java` | Evidence type enum |
| `payment-dispute/src/main/java/com/funding/dispute/model/EvidenceSection.java` | Single evidence section record |
| `payment-dispute/src/main/java/com/funding/dispute/model/EvidencePackage.java` | Evidence package JPA entity |
| `payment-dispute/src/main/java/com/funding/dispute/model/PenaltyType.java` | Penalty type enum |
| `payment-dispute/src/main/java/com/funding/dispute/model/PenaltyAction.java` | Penalty evaluation result record |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackStateMachineConfig.java` | Spring Statemachine config |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/EvidenceDeadlineGuard.java` | 7-day deadline guard |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/AutoCollectEvidenceAction.java` | Auto-collect on notification |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/SlaTimeoutAction.java` | SLA timeout auto-escalation |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackLifecycleManager.java` | Lifecycle orchestrator |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackRepository.java` | Chargeback JPA repository |
| `payment-dispute/src/main/java/com/funding/dispute/lifecycle/ChargebackNotifier.java` | Lifecycle notification interface |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/EvidenceFetcher.java` | Common fetcher interface |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/TransactionDetailsFetcher.java` | Transaction data fetcher |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/LoginHistoryFetcher.java` | Login history fetcher |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/DeviceFingerprintFetcher.java` | Device fingerprint fetcher |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/GameSessionFetcher.java` | Game session fetcher |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/KycStatusFetcher.java` | KYC status fetcher |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/InteractionHistoryFetcher.java` | Interaction history fetcher |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/HybridEvidenceCollector.java` | Evidence collection orchestrator |
| `payment-dispute/src/main/java/com/funding/dispute/evidence/EvidencePackageRepository.java` | Evidence package JPA repository |
| `payment-dispute/src/main/java/com/funding/dispute/penalty/PlayerPenaltyEngine.java` | Penalty engine interface |
| `payment-dispute/src/main/java/com/funding/dispute/penalty/DefaultPlayerPenaltyEngine.java` | Penalty engine implementation |
| `payment-dispute/src/main/java/com/funding/dispute/penalty/PlayerAccountService.java` | Account restriction interface |
| `payment-dispute/src/main/java/com/funding/dispute/penalty/PlayerPenaltyRepository.java` | Penalty JPA repository |
| `payment-dispute/src/main/java/com/funding/dispute/penalty/PenaltyNotifier.java` | Penalty notification interface |
| `payment-dispute/src/main/java/com/funding/dispute/event/ChargebackEventPublisher.java` | Kafka event publisher |
| `payment-dispute/src/main/java/com/funding/dispute/monitoring/ChargebackRateMonitor.java` | Rate gauge + alert scheduler |
| `payment-dispute/src/main/java/com/funding/dispute/monitoring/ChargebackRateCalculator.java` | 30-day rolling rate calculator |
| `payment-dispute/src/main/java/com/funding/dispute/monitoring/AlertService.java` | Alert delivery abstraction |
| `payment-dispute/src/main/java/com/funding/dispute/service/ChargebackService.java` | Top-level service entry point |

### Test Files

| Path | TDD Ref |
|------|---------|
| `payment-dispute/src/test/java/com/funding/dispute/lifecycle/ChargebackLifecycleTest.java` | 10.1 |
| `payment-dispute/src/test/java/com/funding/dispute/evidence/EvidenceCollectorTest.java` | 10.2 |
| `payment-dispute/src/test/java/com/funding/dispute/penalty/PlayerPenaltyEngineTest.java` | 10.3 |

---

## 8. Dependencies

| Dependency | Source | Purpose |
|------------|--------|---------|
| **section-01-foundation** | Internal | `Money` value object, base entity classes, DB migration infrastructure, Kafka/Redis config, Testcontainers setup |
| Spring Boot Starter Web | External | REST controllers |
| Spring Boot Starter Data JPA | External | Persistence (`Chargeback`, `EvidencePackage` entities) |
| Spring Statemachine | External | Chargeback lifecycle state machine |
| Micrometer Core | External | `payment.chargeback.rate` gauge metric |
| Kafka (via section-11 outbox) | Internal | `funding.payment.chargeback` event publishing |
| JUnit 5 + Mockito | External | Test framework |
| AssertJ | External | Fluent test assertions |
