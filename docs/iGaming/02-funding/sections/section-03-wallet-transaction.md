# Section 03 -- Wallet Transaction Processing

> **Module**: `wallet-transaction`
> **Dependencies**: section-01-foundation, section-02-wallet-core
> **Blocks**: section-04-seamless-api, section-05-wallet-reconciliation
> **Plan ref**: claude-plan.md SS4
> **TDD ref**: claude-plan-tdd.md SS4

---

## Overview

This section implements the transaction processing engine -- the most performance-critical component in the funding domain. Every GP bet, win, rollback, and adjustment flows through this module. It contains three major subsystems:

1. **Transaction Processor** -- The critical path that acquires a Redisson distributed lock, resolves deduction order, executes atomic DB updates, and writes to the transactional outbox, all within a single database transaction.
2. **Nine Scenario Handlers** -- A Strategy pattern implementation where each of the nine known transaction scenarios (insufficient balance, concurrent bets, duplicate/retry, out-of-order, rollback, resettlement, free spin, bonus bet, jackpot) has a dedicated handler.
3. **Round Lifecycle State Machine** -- A Spring Statemachine-driven state machine modeling round states (OPEN, CLOSED, CANCELLED, TIMEOUT, PENDING_REVIEW, ADJUSTED) with guarded transitions, and an orphan round detector that escalates stale rounds.

### Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Concurrency control | Redisson lock + `UPDATE ... WHERE balance >= amount` | Double-layer: Redis serializes at app level; DB conditional update is the safety net if Redis lock expires prematurely |
| Version column | Reserved for optimistic reads and audit only -- NOT on the critical write path | Avoids redundant conflict with Redisson lock on the hot path |
| Lock timeout | 3s wait, 5s TTL | Fail-fast under contention; prevents thread pool starvation |
| Event delivery | Transactional outbox in same DB transaction | Guarantees at-least-once delivery to Kafka without 2PC |
| Out-of-order parking | DB `parked_transactions` table + Redis fast lookup | DB persistence survives Redis restart; Redis provides sub-ms lookup |
| Round state machine | Spring Statemachine | Declarative transitions, guards enforce invariants, listeners provide audit trail |

---

## 1. Tests (TDD -- Write These First)

All tests are written before implementation. Unit tests use JUnit 5 + Mockito. Integration tests use Testcontainers (PostgreSQL + Redis + Kafka). Concurrency tests use `CountDownLatch`/`CyclicBarrier`.

### 1.1 Transaction Processor -- Critical Path Tests

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/processor/TransactionProcessorTest.java`

```java
package com.funding.wallet.transaction.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionProcessorTest {

    @Mock
    private DistributedLockService lockService;
    @Mock
    private DeductionEngine deductionEngine;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private TransactionProcessor processor;

    @Test
    void successfulDebit_acquiresLock_updatesDb_releasesLock() {
        // GIVEN a valid debit request for player with sufficient balance
        // WHEN TransactionProcessor.processDebit() is called
        // THEN Redisson lock is acquired on player:{playerId}
        //  AND DeductionEngine resolves deduction steps
        //  AND each step executes UPDATE wallet SET balance = balance - :amount WHERE id = :id AND balance >= :amount
        //  AND transaction record + audit trail written in same DB transaction
        //  AND event written to outbox table in same DB transaction
        //  AND lock is released in finally block
    }

    @Test
    void concurrentDebits_forSamePlayer_areSerialized() {
        // GIVEN two concurrent debit requests for the same player
        // WHEN both call processDebit() simultaneously
        // THEN only one acquires the lock at a time
        //  AND the second waits (up to 3s) for the first to complete
        //  AND both eventually succeed if balance is sufficient
        //  AND final balance reflects both deductions correctly
    }

    @Test
    void multiWalletDeduction_executesInSingleDbTransaction() {
        // GIVEN a debit that spans BONUS + CASH wallets (BONUS insufficient alone)
        // WHEN processDebit() executes the deduction steps
        // THEN all UPDATE statements run within @Transactional boundary
        //  AND commit happens only after ALL steps succeed
    }

    @Test
    void partialDeductionFailure_rollsBackEntireTransaction() {
        // GIVEN a multi-wallet deduction where step 2 (CASH) returns 0 rows updated
        // WHEN processDebit() detects the failed step
        // THEN the entire DB transaction is rolled back
        //  AND no wallet balance is modified (BONUS step also reverted)
        //  AND InsufficientBalanceException is thrown
    }

    @Test
    void dbBalanceGuard_preventsOverDeduction_evenWithoutRedissonLock() {
        // GIVEN a debit request where Redis lock was not acquired (Redis unavailable)
        // WHEN the UPDATE ... WHERE balance >= :amount executes
        // THEN if balance < amount, zero rows updated, transaction fails safely
        //  AND no over-deduction occurs
    }

    @Test
    void lockTimeout_exceedsThreeSeconds_failsFast() {
        // GIVEN Redisson lock is held by another thread
        // WHEN a second thread waits > 3s to acquire the lock
        // THEN LockAcquisitionTimeoutException is thrown
        //  AND the request is not processed
        //  AND appropriate error response returned to caller
    }

    @Test
    void outboxEvent_writtenInSameTransactionAsBalanceUpdate() {
        // GIVEN a successful debit
        // WHEN balance update commits
        // THEN outbox table contains the event record in same transaction
        //  AND if balance update fails, no orphan event exists in outbox
    }

    @Test
    void stressTest_tenConcurrentDebits_zeroOverDeduction() {
        // GIVEN a player with balance = 100.00
        // WHEN 10 concurrent threads each attempt to debit 15.00
        // THEN at most 6 succeed (6 * 15 = 90, 7th would exceed)
        //  AND final balance >= 0
        //  AND sum of all successful debits + final balance == 100.00
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/processor/TransactionProcessorIntegrationTest.java`

```java
package com.funding.wallet.transaction.processor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@Testcontainers
class TransactionProcessorIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Test
    void concurrentDebits_withRealRedisAndPostgres_serializeCorrectly() {
        // GIVEN player with balance 1000.00 in real PostgreSQL
        // WHEN 10 threads simultaneously debit 150.00 using real Redisson lock
        // THEN exactly 6 succeed, final balance == 100.00
        // USE CyclicBarrier to synchronize thread start
    }

    @Test
    void outboxEvent_persistedAtomically_withBalanceUpdate() {
        // GIVEN a successful debit in real PostgreSQL
        // THEN outbox table has exactly one event for this transaction
        //  AND event contains correct amount, playerId, transactionId
    }
}
```

### 1.2 Nine Scenario Handler Tests

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/InsufficientBalanceHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InsufficientBalanceHandlerTest {

    @Test
    void scenarioA_insufficientBalance_returnsErrorWithAvailableBreakdown() {
        // GIVEN player has CASH=50, BONUS=30, requested debit=100
        // WHEN InsufficientBalanceHandler.execute() is called
        // THEN result is INSUFFICIENT_BALANCE error
        //  AND response includes breakdown: {cash: 50.0000, bonus: 30.0000, total: 80.0000}
    }

    @Test
    void scenarioA_gpRejectsBonus_excludesBonusFromAvailabilityCheck() {
        // GIVEN player has CASH=50, BONUS=30, GP contract rejects BONUS
        // WHEN checking availability for debit=60
        // THEN available balance reported as 50 (BONUS excluded)
        //  AND result is INSUFFICIENT_BALANCE
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/ConcurrentBetHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcurrentBetHandlerTest {

    @Test
    void scenarioB_concurrentBets_serializeCorrectlyUnderRedissonLock() {
        // GIVEN two bet requests for the same player arriving simultaneously
        // WHEN ConcurrentBetHandler processes them
        // THEN Redisson lock serializes execution
        //  AND both succeed if balance allows
        //  AND no race condition on balance
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/IdempotentRetryHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IdempotentRetryHandlerTest {

    @Test
    void scenarioC_duplicateTransactionId_returnsCachedResponse() {
        // GIVEN a transactionId that was already processed successfully
        // WHEN IdempotentRetryHandler checks the idempotency store
        // THEN cached response is returned immediately
        //  AND no reprocessing occurs
        //  AND response is identical to original
    }

    @Test
    void scenarioC_duplicateTransactionIdWithDifferentBody_returnsErrorAndWarns() {
        // GIVEN a transactionId that was processed, but new request has different body (amount differs)
        // WHEN IdempotentRetryHandler detects fingerprint mismatch
        // THEN error response is returned
        //  AND warning log is emitted with both fingerprints
        //  AND original transaction is NOT modified
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/OutOfOrderHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutOfOrderHandlerTest {

    @Test
    void scenarioD_winBeforeBet_parkedInDbAndRedis() {
        // GIVEN a Win transaction arrives but no Bet exists for this roundId
        // WHEN OutOfOrderHandler.execute() is called
        // THEN transaction is persisted to parked_transactions table
        //  AND Redis key parking:{roundId} is set with 30min TTL
        //  AND response indicates PARKED status
    }

    @Test
    void scenarioD_parkedWin_survivesRedisRestart() {
        // GIVEN a Win is parked in both DB and Redis
        // WHEN Redis restarts (key lost)
        // THEN background job can still find parked transaction in DB
        //  AND rehydrates Redis key from DB record
        //  AND processing resumes normally when Bet arrives
    }

    @Test
    void scenarioD_parkedWin_expiresAfterThirtyMinutes() {
        // GIVEN a Win was parked 30 minutes ago and Bet never arrived
        // WHEN the expiry check runs
        // THEN parked transaction is marked as EXPIRED
        //  AND error log emitted with roundId, playerId, amount
        //  AND GP notification sent (e.g., via webhook or Kafka event)
    }

    @Test
    void scenarioD_parkedWinDequeue_fails_escalatesToCs() {
        // GIVEN a parked Win is dequeued when Bet arrives
        // WHEN processing the dequeued Win fails (e.g., balance issue after Bet)
        // THEN error is logged with full context
        //  AND CS escalation ticket is created
        //  AND parked transaction marked as DEQUEUE_FAILED
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/RollbackHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RollbackHandlerTest {

    @Test
    void scenarioE_rollbackWithMissingOriginal_returnsSuccess() {
        // GIVEN a rollback request referencing a transactionId that does not exist
        // WHEN RollbackHandler.execute() is called
        // THEN result is SUCCESS (idempotent behavior)
        //  AND no balance changes occur
    }

    @Test
    void scenarioE_rollback_restoresBalanceAndUpdatesRoundState() {
        // GIVEN a rollback request for an existing bet transaction
        // WHEN RollbackHandler processes the rollback
        // THEN each wallet balance is restored by the original deduction amounts
        //  AND round state transitions from OPEN to CANCELLED
        //  AND rollback audit record is created
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/ResettlementHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResettlementHandlerTest {

    @Test
    void scenarioF_resettlement_allowsControlledNegativeBalance() {
        // GIVEN an original Win of 200 was credited, now GP requests resettlement to Win=50
        // WHEN ResettlementHandler calculates difference = -150
        //  AND player's current CASH balance is 100
        // THEN balance becomes -50 (controlled negative allowed for resettlement)
        //  AND round state transitions to ADJUSTED
    }

    @Test
    void scenarioF_negativeBalance_triggersAccountLockWithinFiveSeconds() {
        // GIVEN resettlement causes negative CASH balance
        // WHEN the negative balance is detected
        // THEN account lock event is published
        //  AND account is locked within 5 seconds (INV-6)
        //  AND CS notification is sent
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/FreeSpinHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreeSpinHandlerTest {

    @Test
    void scenarioG_freeSpin_betZeroAccepted_winCreditsCash() {
        // GIVEN a free spin transaction with Bet amount = 0
        // WHEN FreeSpinHandler.execute() processes it
        // THEN Bet=0 is accepted (no deduction needed)
        //  AND Win > 0 credits the CASH wallet (not BONUS)
        //  AND round lifecycle records both Bet and Win
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/BonusBetHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BonusBetHandlerTest {

    @Test
    void scenarioH_deductionFollowsPriorityOrder_bonusThenCash() {
        // GIVEN player has BONUS=40, CASH=60, default priority BONUS -> CASH
        // WHEN BonusBetHandler processes a debit of 70
        // THEN BONUS wallet debited 40, CASH wallet debited 30
        //  AND deduction steps record applied priority layer
    }
}
```

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/scenario/JackpotHandlerTest.java`

```java
package com.funding.wallet.transaction.scenario;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JackpotHandlerTest {

    @Test
    void scenarioI_largeWinWithManualApprovalGp_createsApprovalTask() {
        // GIVEN a Win > $10K from a GP with manual-approval contract
        // WHEN JackpotHandler.execute() is called
        // THEN an approval task is created (not auto-credited)
        //  AND the round remains in OPEN state until approval
        //  AND CS/Finance team is notified
    }

    @Test
    void scenarioI_largeWinWithAutoCreditGp_creditsAndFreezesForReview() {
        // GIVEN a Win > $10K from a GP with auto-credit contract
        // WHEN JackpotHandler.execute() is called
        // THEN Win amount is credited to player's CASH wallet
        //  AND the credited amount is frozen (withdrawal blocked) pending review
        //  AND risk review task is created
    }
}
```

### 1.3 Round Lifecycle State Machine Tests

**File**: `wallet-transaction/src/test/java/com/funding/wallet/transaction/statemachine/RoundStateMachineTest.java`

```java
package com.funding.wallet.transaction.statemachine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoundStateMachineTest {

    @Test
    void openToClosed_onWinLossSettlement() {
        // GIVEN a round in OPEN state with all bets settled
        // WHEN Win/Loss settlement event is received
        // THEN round transitions to CLOSED
        //  AND guard validates all bets have corresponding settlements
        //  AND action publishes ROUND_CLOSED event
    }

    @Test
    void openToCancelled_onRollback() {
        // GIVEN a round in OPEN state
        // WHEN Rollback event is received
        // THEN round transitions to CANCELLED
        //  AND all bet deductions are reversed
    }

    @Test
    void openToTimeout_afterTwoHoursWithoutActivity() {
        // GIVEN a round in OPEN state with lastActivity > 2 hours ago
        // WHEN orphan detector scheduled job runs
        // THEN round transitions to TIMEOUT
    }

    @Test
    void timeoutToClosed_whenGpQueryReturnsResult() {
        // GIVEN a round in TIMEOUT state
        // WHEN GP API query returns a definitive result (win/loss)
        // THEN round transitions to CLOSED
        //  AND settlement is applied based on GP response
    }

    @Test
    void timeoutToPendingReview_whenGpQueryFails() {
        // GIVEN a round in TIMEOUT state
        // WHEN GP API query fails or returns inconclusive
        // THEN round transitions to PENDING_REVIEW
        //  AND CS ticket is created for manual resolution
    }

    @Test
    void closedToAdjusted_onResettlement() {
        // GIVEN a round in CLOSED state
        // WHEN resettlement request arrives from GP
        // THEN round transitions to ADJUSTED
        //  AND balance difference is applied
    }

    @Test
    void invalidTransition_isRejected() {
        // GIVEN a round in CLOSED state
        // WHEN an attempt to transition to OPEN is made
        // THEN InvalidTransitionException is thrown
        //  AND round state remains CLOSED
        //  AND warning log emitted
    }

    @Test
    void orphanDetector_findsRoundsOpenLongerThanTwoHours() {
        // GIVEN 3 rounds: one opened 1h ago, one opened 3h ago, one opened 5h ago
        // WHEN OrphanRoundDetector.scan() runs
        // THEN returns 2 rounds (the 3h and 5h ones)
    }

    @Test
    void highValueOrphan_createsPriorityTicket() {
        // GIVEN an orphan round with total bet amount > $1,000
        // WHEN OrphanRoundDetector processes it
        // THEN a high-priority CS ticket is created with 24h SLA
        //  AND ticket includes roundId, playerId, amount, GP info
    }
}
```

---

## 2. Implementation

### 2.1 Domain Model

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/Transaction.java`

```java
package com.funding.wallet.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_transaction_round_id", columnList = "roundId"),
    @Index(name = "idx_transaction_player_id", columnList = "playerId"),
    @Index(name = "idx_transaction_created_at", columnList = "createdAt")
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String transactionId;  // GP-provided idempotency key

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    private UUID roundId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;  // BET, WIN, ROLLBACK, ADJUST, FREE_SPIN

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;  // COMPLETED, FAILED, PARKED, PENDING_APPROVAL

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionScenario scenario;  // A through I

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String gpId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant completedAt;

    @Version
    private Long version;  // optimistic lock for reads/audit only -- NOT on critical write path

    // Getters, setters, builder pattern omitted -- generate via Lombok @Builder or records
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/TransactionType.java`

```java
package com.funding.wallet.transaction.model;

public enum TransactionType {
    BET,
    WIN,
    ROLLBACK,
    ADJUST,
    FREE_SPIN
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/TransactionStatus.java`

```java
package com.funding.wallet.transaction.model;

public enum TransactionStatus {
    COMPLETED,
    FAILED,
    PARKED,
    PENDING_APPROVAL,
    EXPIRED
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/TransactionScenario.java`

```java
package com.funding.wallet.transaction.model;

public enum TransactionScenario {
    A_INSUFFICIENT_BALANCE,
    B_CONCURRENT_BET,
    C_DUPLICATE_RETRY,
    D_OUT_OF_ORDER,
    E_ROLLBACK,
    F_RESETTLEMENT,
    G_FREE_SPIN,
    H_BONUS_BET,
    I_JACKPOT
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/Round.java`

```java
package com.funding.wallet.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rounds", indexes = {
    @Index(name = "idx_round_player_id", columnList = "playerId"),
    @Index(name = "idx_round_state_created", columnList = "state, createdAt")
})
public class Round {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String roundId;  // GP-provided round identifier

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private RoundState state;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalBetAmount;

    @Column(precision = 19, scale = 4)
    private BigDecimal totalWinAmount;

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String gpId;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant lastActivityAt;

    private Instant closedAt;

    @Version
    private Long version;

    // Getters, setters, builder pattern omitted
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/RoundState.java`

```java
package com.funding.wallet.transaction.model;

public enum RoundState {
    OPEN,
    CLOSED,
    CANCELLED,
    TIMEOUT,
    PENDING_REVIEW,
    ADJUSTED
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/ParkedTransaction.java`

```java
package com.funding.wallet.transaction.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "parked_transactions", indexes = {
    @Index(name = "idx_parked_round_id", columnList = "roundId"),
    @Index(name = "idx_parked_status_expires", columnList = "status, expiresAt")
})
public class ParkedTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String roundId;

    @Column(nullable = false)
    private UUID playerId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ParkedStatus status;  // WAITING, DEQUEUED, EXPIRED, DEQUEUE_FAILED

    @Lob
    @Column(nullable = false)
    private String rawPayload;  // original GP request JSON, preserved for replay

    @Column(nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private String gpId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;  // createdAt + 30 minutes

    private int retryCount;

    // Getters, setters, builder pattern omitted
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/ParkedStatus.java`

```java
package com.funding.wallet.transaction.model;

public enum ParkedStatus {
    WAITING,
    DEQUEUED,
    EXPIRED,
    DEQUEUE_FAILED
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/TransactionContext.java`

```java
package com.funding.wallet.transaction.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Immutable context object passed to scenario handlers.
 * Contains all information needed to process a transaction.
 */
public record TransactionContext(
    String transactionId,
    UUID playerId,
    String roundId,
    TransactionType type,
    BigDecimal amount,
    String currency,
    UUID tenantId,
    String gpId,
    String rawPayload,
    String requestFingerprint  // SHA-256 of canonical request body for idempotency validation
) {}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/TransactionResult.java`

```java
package com.funding.wallet.transaction.model;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Result returned by scenario handlers and the transaction processor.
 */
public record TransactionResult(
    TransactionStatus status,
    BigDecimal updatedBalance,
    Map<String, BigDecimal> balanceBreakdown,  // e.g., {cash: 100, bonus: 50}
    String errorCode,
    String errorMessage
) {
    public static TransactionResult success(BigDecimal balance, Map<String, BigDecimal> breakdown) {
        return new TransactionResult(TransactionStatus.COMPLETED, balance, breakdown, null, null);
    }

    public static TransactionResult failed(String errorCode, String errorMessage, Map<String, BigDecimal> breakdown) {
        return new TransactionResult(TransactionStatus.FAILED, null, breakdown, errorCode, errorMessage);
    }

    public static TransactionResult parked() {
        return new TransactionResult(TransactionStatus.PARKED, null, null, null, null);
    }
}
```

### 2.2 Database Migration

**File**: `wallet-transaction/src/main/resources/db/migration/V3__create_transaction_tables.sql`

```sql
-- Transactions table
CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  VARCHAR(255) NOT NULL UNIQUE,
    player_id       UUID NOT NULL,
    round_id        UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(30) NOT NULL,
    scenario        VARCHAR(30) NOT NULL,
    tenant_id       UUID NOT NULL,
    gp_id           VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    version         BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_transaction_round_id ON transactions(round_id);
CREATE INDEX idx_transaction_player_id ON transactions(player_id);
CREATE INDEX idx_transaction_created_at ON transactions(created_at);

-- Rounds table
CREATE TABLE rounds (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id          VARCHAR(255) NOT NULL UNIQUE,
    player_id         UUID NOT NULL,
    state             VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    total_bet_amount  NUMERIC(19,4) DEFAULT 0,
    total_win_amount  NUMERIC(19,4) DEFAULT 0,
    tenant_id         UUID NOT NULL,
    gp_id             VARCHAR(50) NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_activity_at  TIMESTAMPTZ,
    closed_at         TIMESTAMPTZ,
    version           BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_round_player_id ON rounds(player_id);
CREATE INDEX idx_round_state_created ON rounds(state, created_at);

-- Parked transactions for out-of-order scenario (D)
CREATE TABLE parked_transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  VARCHAR(255) NOT NULL,
    round_id        VARCHAR(255) NOT NULL,
    player_id       UUID NOT NULL,
    type            VARCHAR(20) NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    raw_payload     TEXT NOT NULL,
    tenant_id       UUID NOT NULL,
    gp_id           VARCHAR(50) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,
    retry_count     INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_parked_round_id ON parked_transactions(round_id);
CREATE INDEX idx_parked_status_expires ON parked_transactions(status, expires_at);

-- Deduction audit trail
CREATE TABLE deduction_audit (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  VARCHAR(255) NOT NULL REFERENCES transactions(transaction_id),
    wallet_id       UUID NOT NULL,
    wallet_type     VARCHAR(10) NOT NULL,
    amount          NUMERIC(19,4) NOT NULL,
    applied_layer   INT NOT NULL,       -- 1=Player, 2=GP, 3=Game, 4=System
    rule_id         VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_deduction_audit_txn ON deduction_audit(transaction_id);

-- Transactional outbox for Kafka event delivery
CREATE TABLE transaction_outbox (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type  VARCHAR(50) NOT NULL,  -- e.g., 'Transaction', 'Round'
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,
    retry_count     INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_status ON transaction_outbox(status) WHERE status = 'PENDING';
```

### 2.3 Repository Layer

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/repository/TransactionRepository.java`

```java
package com.funding.wallet.transaction.repository;

import com.funding.wallet.transaction.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByTransactionId(String transactionId);

    boolean existsByTransactionId(String transactionId);
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/repository/RoundRepository.java`

```java
package com.funding.wallet.transaction.repository;

import com.funding.wallet.transaction.model.Round;
import com.funding.wallet.transaction.model.RoundState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoundRepository extends JpaRepository<Round, UUID> {

    Optional<Round> findByRoundId(String roundId);

    @Query("SELECT r FROM Round r WHERE r.state = :state AND r.createdAt < :cutoff")
    List<Round> findOrphanRounds(RoundState state, Instant cutoff);
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/repository/ParkedTransactionRepository.java`

```java
package com.funding.wallet.transaction.repository;

import com.funding.wallet.transaction.model.ParkedStatus;
import com.funding.wallet.transaction.model.ParkedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParkedTransactionRepository extends JpaRepository<ParkedTransaction, UUID> {

    List<ParkedTransaction> findByRoundIdAndStatus(String roundId, ParkedStatus status);

    List<ParkedTransaction> findByStatusAndExpiresAtBefore(ParkedStatus status, Instant now);

    Optional<ParkedTransaction> findByTransactionId(String transactionId);
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/repository/OutboxRepository.java`

```java
package com.funding.wallet.transaction.repository;

import com.funding.wallet.transaction.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents();

    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PUBLISHED', e.publishedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
    void markAsPublished(UUID id);
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/model/OutboxEvent.java`

```java
package com.funding.wallet.transaction.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_outbox")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String aggregateType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 20)
    private String status;  // PENDING, PUBLISHED, FAILED

    @Column(nullable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int retryCount;

    // Getters, setters, builder pattern omitted
}
```

### 2.4 Transaction Processor (Critical Path)

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/processor/TransactionProcessor.java`

```java
package com.funding.wallet.transaction.processor;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;

/**
 * The critical-path transaction processor. Every GP bet/win/rollback flows through here.
 *
 * Core Debit flow:
 *   1. Acquire Redisson distributed lock on player:{playerId} (wait: 3s, TTL: 5s)
 *   2. Resolve deduction order via DeductionEngine -> List<DeductionStep>
 *   3. Execute ALL deduction steps in a SINGLE DB transaction:
 *      - UPDATE wallet SET balance = balance - :amount WHERE id = :id AND balance >= :amount
 *      - NO version check on critical path (Redisson serializes; balance >= amount is the DB safety net)
 *      - If ANY step returns 0 rows -> rollback entire transaction
 *   4. Write transaction record + deduction audit trail (same DB transaction)
 *   5. Write event to transactional outbox table (same DB transaction)
 *   6. Release lock in finally block
 *
 * Credit flow (Win):
 *   - Similar but UPDATE wallet SET balance = balance + :amount
 *   - No balance guard needed for credit
 *
 * Delegates to scenario handlers when special cases are detected.
 */
public interface TransactionProcessor {

    /**
     * Process a debit (bet) transaction.
     * @param ctx immutable transaction context
     * @return result with updated balance or error
     */
    TransactionResult processDebit(TransactionContext ctx);

    /**
     * Process a credit (win) transaction.
     * @param ctx immutable transaction context
     * @return result with updated balance
     */
    TransactionResult processCredit(TransactionContext ctx);

    /**
     * Process a rollback transaction.
     * @param ctx immutable transaction context containing the original transactionId
     * @return result (always success for idempotent rollback)
     */
    TransactionResult processRollback(TransactionContext ctx);

    /**
     * Process an adjustment (resettlement) transaction.
     * @param ctx immutable transaction context
     * @return result with updated balance
     */
    TransactionResult processAdjust(TransactionContext ctx);
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/processor/TransactionProcessorImpl.java`

```java
package com.funding.wallet.transaction.processor;

import com.funding.wallet.core.service.DeductionEngine;
import com.funding.wallet.transaction.model.*;
import com.funding.wallet.transaction.repository.*;
import com.funding.wallet.transaction.scenario.TransactionScenarioHandler;
import com.funding.shared.lock.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TransactionProcessorImpl implements TransactionProcessor {

    private final DistributedLockService lockService;
    private final DeductionEngine deductionEngine;
    private final WalletUpdateService walletUpdateService;
    private final TransactionRepository transactionRepository;
    private final OutboxRepository outboxRepository;
    private final List<TransactionScenarioHandler> scenarioHandlers;
    private final ScenarioResolver scenarioResolver;

    // Constructor injection (all dependencies)

    public TransactionProcessorImpl(
            DistributedLockService lockService,
            DeductionEngine deductionEngine,
            WalletUpdateService walletUpdateService,
            TransactionRepository transactionRepository,
            OutboxRepository outboxRepository,
            List<TransactionScenarioHandler> scenarioHandlers,
            ScenarioResolver scenarioResolver) {
        this.lockService = lockService;
        this.deductionEngine = deductionEngine;
        this.walletUpdateService = walletUpdateService;
        this.transactionRepository = transactionRepository;
        this.outboxRepository = outboxRepository;
        this.scenarioHandlers = scenarioHandlers;
        this.scenarioResolver = scenarioResolver;
    }

    @Override
    public TransactionResult processDebit(TransactionContext ctx) {
        // 1. Resolve scenario (check for special cases first)
        // 2. If special scenario detected, delegate to handler
        // 3. Otherwise: standard debit path
        //    a. Acquire Redisson lock: lockService.lock("player:" + ctx.playerId(), 3, 5, SECONDS)
        //    b. Inside lock: deductionEngine.resolveDeductionOrder(playerId, amount, deductionCtx)
        //    c. executeAtomicDeduction(deductionSteps, ctx) -- @Transactional
        //    d. Release lock in finally block
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult processCredit(TransactionContext ctx) {
        // 1. Resolve scenario (check for jackpot, free spin, etc.)
        // 2. Acquire lock, credit wallet, write outbox event
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult processRollback(TransactionContext ctx) {
        // Delegate to RollbackHandler
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult processAdjust(TransactionContext ctx) {
        // Delegate to ResettlementHandler
        throw new UnsupportedOperationException("TODO: implement");
    }

    /**
     * Executes all deduction steps in a SINGLE DB transaction.
     * If any step fails (0 rows updated), the entire transaction rolls back.
     * Also writes transaction record, deduction audit, and outbox event.
     */
    @Transactional
    protected TransactionResult executeAtomicDeduction(
            List<?> deductionSteps, /* List<DeductionStep> from wallet-core */
            TransactionContext ctx) {
        // For each step:
        //   int updated = walletUpdateService.conditionalDebit(step.walletId(), step.amount());
        //   if (updated == 0) throw new InsufficientBalanceException(...)
        //
        // Save transaction record
        // Save deduction audit trail
        // Save outbox event
        // Return TransactionResult.success(...)
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/processor/WalletUpdateService.java`

```java
package com.funding.wallet.transaction.processor;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Encapsulates the atomic wallet balance updates.
 * Uses conditional UPDATE to prevent over-deduction at the DB level.
 */
@Service
public class WalletUpdateService {

    /**
     * Atomic conditional debit: UPDATE wallet SET balance = balance - :amount WHERE id = :id AND balance >= :amount
     * @return number of rows updated (0 = insufficient balance, 1 = success)
     */
    public int conditionalDebit(UUID walletId, BigDecimal amount) {
        // Execute native query: UPDATE wallet SET balance = balance - :amount WHERE id = :id AND balance >= :amount
        throw new UnsupportedOperationException("TODO: implement with native query");
    }

    /**
     * Credit: UPDATE wallet SET balance = balance + :amount WHERE id = :id
     * No balance guard needed for credits.
     * @return number of rows updated
     */
    public int credit(UUID walletId, BigDecimal amount) {
        throw new UnsupportedOperationException("TODO: implement with native query");
    }

    /**
     * Controlled debit for resettlement: allows negative balance.
     * UPDATE wallet SET balance = balance - :amount WHERE id = :id
     * No balance >= amount guard (resettlement can go negative).
     */
    public int controlledDebit(UUID walletId, BigDecimal amount) {
        throw new UnsupportedOperationException("TODO: implement with native query");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/processor/ScenarioResolver.java`

```java
package com.funding.wallet.transaction.processor;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionScenario;
import com.funding.wallet.transaction.scenario.TransactionScenarioHandler;

import java.util.List;
import java.util.Optional;

/**
 * Determines which scenario handler should process a given transaction.
 * Iterates through registered handlers and returns the first one whose canHandle() returns true.
 * If no special scenario applies, returns empty (standard path).
 */
public interface ScenarioResolver {

    Optional<TransactionScenarioHandler> resolve(TransactionContext ctx);
}
```

### 2.5 Nine Scenario Handlers (Strategy Pattern)

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/TransactionScenarioHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;

/**
 * Strategy interface for the nine transaction scenarios (A through I).
 * Each handler is responsible for detecting and processing its specific scenario.
 */
public interface TransactionScenarioHandler {

    /**
     * Determine if this handler can process the given transaction context.
     * Handlers are evaluated in priority order; first match wins.
     */
    boolean canHandle(TransactionContext ctx);

    /**
     * Execute the scenario-specific logic.
     * @param ctx immutable transaction context
     * @return result with status, balance, or error information
     */
    TransactionResult execute(TransactionContext ctx);
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/InsufficientBalanceHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario A: Insufficient balance.
 * Checks multi-wallet availability; skips BONUS if GP rejects it.
 * Returns structured error with available balance breakdown.
 */
@Component
@Order(10)
public class InsufficientBalanceHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Check if total available balance across applicable wallets < requested amount
        // Must consider GP contract rules (e.g., GP rejects BONUS)
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. Calculate available balance per wallet type (respecting GP rules)
        // 2. Return INSUFFICIENT_BALANCE error with breakdown: {cash: X, bonus: Y, total: Z}
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/ConcurrentBetHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario B: Concurrent bets for the same player.
 * Serialize via Redisson lock per player.
 * This handler is generally transparent -- the lock acquisition in TransactionProcessor
 * handles serialization. This handler adds retry with exponential backoff on lock contention.
 */
@Component
@Order(20)
public class ConcurrentBetHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect if lock contention is present (e.g., lock wait exceeded soft threshold)
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // Retry with exponential backoff on lock contention
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/IdempotentRetryHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario C: Duplicate transactionId (timeout/retry from GP).
 * Checks idempotency store first; if cached, returns immediately.
 * If transactionId matches but request fingerprint differs, returns error + warning log.
 */
@Component
@Order(5)  // High priority -- check idempotency before any processing
public class IdempotentRetryHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Check if transactionId already exists in idempotency store or DB
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. Retrieve cached response for this transactionId
        // 2. Validate request fingerprint matches original
        //    - Match: return cached response
        //    - Mismatch: return error, log warning with both fingerprints
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/OutOfOrderHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import com.funding.wallet.transaction.repository.ParkedTransactionRepository;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario D: Out-of-order (Win arrives before Bet).
 * Parks the transaction in DB (parked_transactions table) + Redis fast lookup (parking:{roundId}, TTL 30min).
 * Background job retries every 5s.
 * If Bet never arrives within 30min: expire with error log + GP notification.
 * CS can manually force-process parked transactions.
 */
@Component
@Order(15)
public class OutOfOrderHandler implements TransactionScenarioHandler {

    private static final int PARKING_TTL_MINUTES = 30;
    private static final int RETRY_INTERVAL_SECONDS = 5;

    private final ParkedTransactionRepository parkedRepository;
    // + RedisTemplate for fast lookup key

    public OutOfOrderHandler(ParkedTransactionRepository parkedRepository) {
        this.parkedRepository = parkedRepository;
    }

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect: transaction type is WIN/CREDIT and no BET exists for this roundId
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. Persist to parked_transactions table (DB durability)
        // 2. Set Redis key parking:{roundId} with 30min TTL (fast lookup)
        // 3. Return TransactionResult.parked()
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/RollbackHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario E: Rollback.
 * If original bet not found: return Success (idempotent behavior).
 * If found: reverse deduction, restore balance, update round state to CANCELLED.
 */
@Component
@Order(30)
public class RollbackHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect: transaction type is ROLLBACK
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. Look up original transaction by referenced transactionId
        // 2. Not found: return Success (idempotent)
        // 3. Found: reverse each deduction step, restore wallet balances
        // 4. Update round state: OPEN -> CANCELLED
        // 5. Write rollback audit record
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/ResettlementHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario F: Resettlement.
 * Calculates difference between new and original settlement.
 * Allows controlled negative balance (uses controlledDebit, not conditionalDebit).
 * Triggers negative balance protection flow if balance goes negative.
 */
@Component
@Order(35)
public class ResettlementHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect: transaction type is ADJUST and original settlement exists
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. Find original settlement transaction
        // 2. Calculate difference (new amount - original amount)
        // 3. If difference < 0: controlledDebit (allows negative)
        // 4. If difference > 0: credit
        // 5. Transition round state: CLOSED -> ADJUSTED
        // 6. If resulting balance < 0: publish NEGATIVE_BALANCE event -> account lock within 5s
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/FreeSpinHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario G: Free spin.
 * Bet=0 is legal (no deduction needed).
 * Win > 0 credits CASH wallet (not BONUS).
 */
@Component
@Order(40)
public class FreeSpinHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect: transaction type is FREE_SPIN or bet amount == 0 with free spin flag
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. If BET: accept with amount=0, no deduction
        // 2. If WIN: credit CASH wallet with win amount
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/BonusBetHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Scenario H: Bonus bet (standard deduction with priority).
 * Follows the 4-layer deduction priority from wallet-core DeductionEngine.
 * Default: BONUS -> CASH -> CREDIT.
 * Overrides apply per Layer 1-3 rules.
 */
@Component
@Order(50)
public class BonusBetHandler implements TransactionScenarioHandler {

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect: standard bet with BONUS wallet participation
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // Delegate to TransactionProcessor standard debit path with DeductionEngine
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scenario/JackpotHandler.java`

```java
package com.funding.wallet.transaction.scenario;

import com.funding.wallet.transaction.model.TransactionContext;
import com.funding.wallet.transaction.model.TransactionResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Scenario I: Jackpot / Large win (Win > $10K).
 * Checks GP contract:
 *   - manual-approval GP: creates approval task, does NOT auto-credit
 *   - auto-credit GP: credits immediately, freezes for risk review
 */
@Component
@Order(25)
public class JackpotHandler implements TransactionScenarioHandler {

    private static final BigDecimal JACKPOT_THRESHOLD = new BigDecimal("10000.0000");

    @Override
    public boolean canHandle(TransactionContext ctx) {
        // Detect: WIN transaction with amount > $10K
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Override
    public TransactionResult execute(TransactionContext ctx) {
        // 1. Look up GP contract: manual-approval vs auto-credit
        // 2. Manual: create approval task, return PENDING_APPROVAL status
        // 3. Auto: credit CASH wallet, freeze credited amount, create risk review task
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

### 2.6 Round Lifecycle State Machine

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/statemachine/RoundStateMachineConfig.java`

```java
package com.funding.wallet.transaction.statemachine;

import com.funding.wallet.transaction.model.RoundState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

/**
 * Spring Statemachine configuration for the Round lifecycle.
 *
 * States: OPEN, CLOSED, CANCELLED, TIMEOUT, PENDING_REVIEW, ADJUSTED
 *
 * Transitions:
 *   OPEN -> CLOSED       (on: SETTLE)       guard: all bets have settlements
 *   OPEN -> CANCELLED    (on: ROLLBACK)
 *   OPEN -> TIMEOUT      (on: TIMEOUT)      guard: lastActivity > 2 hours
 *   TIMEOUT -> CLOSED    (on: GP_RESULT)    guard: GP query returned result
 *   TIMEOUT -> PENDING_REVIEW (on: GP_QUERY_FAILED)
 *   CLOSED -> ADJUSTED   (on: RESETTLE)
 *
 * Each transition has:
 *   - Guard: validates preconditions
 *   - Action: executes business logic (balance updates, event publishing)
 *   - Listener: logs state transition for audit
 */
@Configuration
@EnableStateMachineFactory
public class RoundStateMachineConfig extends StateMachineConfigurerAdapter<RoundState, RoundEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<RoundState, RoundEvent> states) throws Exception {
        states.withStates()
                .initial(RoundState.OPEN)
                .states(EnumSet.allOf(RoundState.class));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<RoundState, RoundEvent> transitions) throws Exception {
        transitions
                .withExternal()
                    .source(RoundState.OPEN).target(RoundState.CLOSED)
                    .event(RoundEvent.SETTLE)
                    // .guard(allBetsSettledGuard())
                    // .action(settleAction())
                    .and()
                .withExternal()
                    .source(RoundState.OPEN).target(RoundState.CANCELLED)
                    .event(RoundEvent.ROLLBACK)
                    // .action(rollbackAction())
                    .and()
                .withExternal()
                    .source(RoundState.OPEN).target(RoundState.TIMEOUT)
                    .event(RoundEvent.TIMEOUT)
                    // .guard(inactivityGuard())
                    .and()
                .withExternal()
                    .source(RoundState.TIMEOUT).target(RoundState.CLOSED)
                    .event(RoundEvent.GP_RESULT)
                    // .guard(gpResultAvailableGuard())
                    // .action(applyGpResultAction())
                    .and()
                .withExternal()
                    .source(RoundState.TIMEOUT).target(RoundState.PENDING_REVIEW)
                    .event(RoundEvent.GP_QUERY_FAILED)
                    // .action(createCsTicketAction())
                    .and()
                .withExternal()
                    .source(RoundState.CLOSED).target(RoundState.ADJUSTED)
                    .event(RoundEvent.RESETTLE)
                    // .action(resettleAction())
                    ;
    }

    // Guard beans and Action beans defined below as @Bean methods
    // TODO: implement guard and action beans
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/statemachine/RoundEvent.java`

```java
package com.funding.wallet.transaction.statemachine;

/**
 * Events that trigger transitions in the Round lifecycle state machine.
 */
public enum RoundEvent {
    SETTLE,           // Win/Loss settlement received
    ROLLBACK,         // Rollback received
    TIMEOUT,          // Inactivity timeout (> 2 hours)
    GP_RESULT,        // GP API query returned a result
    GP_QUERY_FAILED,  // GP API query failed or was inconclusive
    RESETTLE          // Resettlement request from GP
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/statemachine/RoundLifecycleManager.java`

```java
package com.funding.wallet.transaction.statemachine;

import com.funding.wallet.transaction.model.Round;
import com.funding.wallet.transaction.model.RoundState;

/**
 * Manages round lifecycle transitions using the Spring Statemachine.
 * Provides a clean API for the transaction processor to trigger state changes.
 */
public interface RoundLifecycleManager {

    /**
     * Get or create a round for the given roundId.
     */
    Round getOrCreateRound(String roundId, java.util.UUID playerId, java.util.UUID tenantId, String gpId);

    /**
     * Send an event to the round's state machine, triggering a transition.
     * @throws InvalidTransitionException if the transition is not allowed
     */
    void sendEvent(String roundId, RoundEvent event);

    /**
     * Get the current state of a round.
     */
    RoundState getCurrentState(String roundId);
}
```

### 2.7 Orphan Round Detector

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/detector/OrphanRoundDetector.java`

```java
package com.funding.wallet.transaction.detector;

import com.funding.wallet.transaction.model.Round;
import com.funding.wallet.transaction.model.RoundState;
import com.funding.wallet.transaction.repository.RoundRepository;
import com.funding.wallet.transaction.statemachine.RoundEvent;
import com.funding.wallet.transaction.statemachine.RoundLifecycleManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Scheduled job that runs every 15 minutes to detect orphan rounds.
 * An orphan round is one in OPEN state for > 2 hours without activity.
 *
 * For each orphan:
 *   1. Query GP API for round status
 *   2. GP responds with result -> auto-close (TIMEOUT -> CLOSED via GP_RESULT event)
 *   3. GP API unavailable -> transition to PENDING_REVIEW
 *   4. Amount > $1,000 -> create high-priority CS ticket with 24h SLA
 */
@Component
public class OrphanRoundDetector {

    private static final int ORPHAN_THRESHOLD_HOURS = 2;
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("1000.0000");

    private final RoundRepository roundRepository;
    private final RoundLifecycleManager roundLifecycleManager;
    // + GpApiClient for querying round status
    // + CsTicketService for creating priority tickets

    public OrphanRoundDetector(RoundRepository roundRepository, RoundLifecycleManager roundLifecycleManager) {
        this.roundRepository = roundRepository;
        this.roundLifecycleManager = roundLifecycleManager;
    }

    @Scheduled(fixedRate = 900_000)  // every 15 minutes
    public void detectOrphanRounds() {
        Instant cutoff = Instant.now().minus(ORPHAN_THRESHOLD_HOURS, ChronoUnit.HOURS);
        List<Round> orphans = roundRepository.findOrphanRounds(RoundState.OPEN, cutoff);

        for (Round orphan : orphans) {
            processOrphan(orphan);
        }
    }

    private void processOrphan(Round orphan) {
        // 1. Transition OPEN -> TIMEOUT
        // 2. Query GP API for round result
        // 3. If result available: send GP_RESULT event (TIMEOUT -> CLOSED)
        // 4. If GP unavailable: send GP_QUERY_FAILED event (TIMEOUT -> PENDING_REVIEW)
        // 5. If totalBetAmount > $1,000: create high-priority CS ticket
        throw new UnsupportedOperationException("TODO: implement");
    }
}
```

### 2.8 Parked Transaction Processor (Background Job)

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/scheduler/ParkedTransactionProcessor.java`

```java
package com.funding.wallet.transaction.scheduler;

import com.funding.wallet.transaction.model.ParkedStatus;
import com.funding.wallet.transaction.model.ParkedTransaction;
import com.funding.wallet.transaction.repository.ParkedTransactionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Background job that:
 *   1. Retries parked transactions every 5 seconds (checks if corresponding Bet has arrived)
 *   2. Expires parked transactions older than 30 minutes
 *
 * DB is the source of truth; Redis parking:{roundId} is a fast-lookup optimization.
 */
@Component
public class ParkedTransactionProcessor {

    private final ParkedTransactionRepository parkedRepository;
    // + TransactionProcessor for replaying dequeued transactions
    // + GpNotificationService for expiry notifications
    // + CsTicketService for escalation on dequeue failure

    public ParkedTransactionProcessor(ParkedTransactionRepository parkedRepository) {
        this.parkedRepository = parkedRepository;
    }

    @Scheduled(fixedRate = 5_000)  // every 5 seconds
    public void retryParkedTransactions() {
        // 1. Find all WAITING parked transactions
        // 2. For each: check if corresponding Bet now exists
        // 3. If Bet exists: dequeue and process (mark DEQUEUED)
        // 4. If processing fails: mark DEQUEUE_FAILED, create CS ticket
        throw new UnsupportedOperationException("TODO: implement");
    }

    @Scheduled(fixedRate = 60_000)  // every minute
    public void expireStaleParkedTransactions() {
        List<ParkedTransaction> expired = parkedRepository
                .findByStatusAndExpiresAtBefore(ParkedStatus.WAITING, Instant.now());

        for (ParkedTransaction p : expired) {
            // 1. Mark as EXPIRED
            // 2. Log error with full context
            // 3. Send GP notification
            throw new UnsupportedOperationException("TODO: implement per-record");
        }
    }
}
```

### 2.9 Exception Types

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/exception/InsufficientBalanceException.java`

```java
package com.funding.wallet.transaction.exception;

import java.math.BigDecimal;
import java.util.Map;

public class InsufficientBalanceException extends RuntimeException {

    private final Map<String, BigDecimal> availableBreakdown;

    public InsufficientBalanceException(String message, Map<String, BigDecimal> availableBreakdown) {
        super(message);
        this.availableBreakdown = availableBreakdown;
    }

    public Map<String, BigDecimal> getAvailableBreakdown() {
        return availableBreakdown;
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/exception/LockAcquisitionTimeoutException.java`

```java
package com.funding.wallet.transaction.exception;

public class LockAcquisitionTimeoutException extends RuntimeException {

    public LockAcquisitionTimeoutException(String playerId) {
        super("Failed to acquire lock for player:" + playerId + " within 3s timeout");
    }
}
```

**File**: `wallet-transaction/src/main/java/com/funding/wallet/transaction/exception/InvalidTransitionException.java`

```java
package com.funding.wallet.transaction.exception;

import com.funding.wallet.transaction.model.RoundState;
import com.funding.wallet.transaction.statemachine.RoundEvent;

public class InvalidTransitionException extends RuntimeException {

    public InvalidTransitionException(RoundState currentState, RoundEvent event) {
        super("Invalid transition: cannot apply event " + event + " to round in state " + currentState);
    }
}
```

---

## 3. Configuration

**File**: `wallet-transaction/src/main/resources/application.yml` (transaction-specific properties)

```yaml
funding:
  transaction:
    lock:
      wait-time: 3s        # Redisson lock wait timeout -- fail fast
      lease-time: 5s        # Redisson lock TTL
      key-prefix: "player:" # Lock key format: player:{playerId}
    parking:
      ttl: 30m              # Parked transaction expiry
      retry-interval: 5s    # How often to check if Bet arrived
    orphan:
      scan-interval: 15m    # How often to scan for orphan rounds
      threshold: 2h         # Round OPEN longer than this = orphan
      high-value-amount: 1000.00  # Creates priority CS ticket
    jackpot:
      threshold: 10000.00   # Win amount requiring special handling

spring:
  statemachine:
    redis:
      enabled: true         # Persist state machine state in Redis
```

---

## 4. Dependency Summary

### From section-01-foundation (required)

- `Money` value object (BigDecimal wrapper with currency and 4-decimal precision)
- Base entity classes (`BaseEntity` with `id`, `createdAt`, `version`)
- Flyway migration infrastructure
- Testcontainers base configuration
- Kafka configuration and topic definitions

### From section-02-wallet-core (required)

- `WalletRepository` -- lookup wallet by playerId and walletType
- `DeductionEngine.resolveDeductionOrder(playerId, amount, ctx)` -- returns `List<DeductionStep>`
- `DeductionStep` -- `walletType`, `walletId`, `amount`, `appliedLayer`, `ruleId`
- `BalanceCalculator.getPlayableBalance(playerId)` -- for availability checks
- Wallet entity and three wallet types (CASH, BONUS, CREDIT)

### From section-11-shared-infrastructure (expected)

- `DistributedLockService` -- Redisson lock abstraction (`lock`, `unlock`, `tryLock`)
- Kafka outbox relay (polls `transaction_outbox`, publishes, marks published)
- DB-configurable parameter reader (for GP contract settings like manual-approval threshold)

---

## 5. File Index

### Test Files
| File | Purpose |
|------|---------|
| `processor/TransactionProcessorTest.java` | 8 unit tests for the critical debit path |
| `processor/TransactionProcessorIntegrationTest.java` | Integration tests with real PostgreSQL + Redis |
| `scenario/InsufficientBalanceHandlerTest.java` | Scenario A: 2 tests |
| `scenario/ConcurrentBetHandlerTest.java` | Scenario B: 1 test |
| `scenario/IdempotentRetryHandlerTest.java` | Scenario C: 2 tests |
| `scenario/OutOfOrderHandlerTest.java` | Scenario D: 4 tests |
| `scenario/RollbackHandlerTest.java` | Scenario E: 2 tests |
| `scenario/ResettlementHandlerTest.java` | Scenario F: 2 tests |
| `scenario/FreeSpinHandlerTest.java` | Scenario G: 1 test |
| `scenario/BonusBetHandlerTest.java` | Scenario H: 1 test |
| `scenario/JackpotHandlerTest.java` | Scenario I: 2 tests |
| `statemachine/RoundStateMachineTest.java` | 9 tests for round lifecycle transitions + orphan detection |

### Implementation Files
| File | Purpose |
|------|---------|
| `model/Transaction.java` | Transaction entity (JPA) |
| `model/Round.java` | Round entity (JPA) |
| `model/ParkedTransaction.java` | Parked (out-of-order) transaction entity |
| `model/OutboxEvent.java` | Transactional outbox event entity |
| `model/TransactionContext.java` | Immutable context record passed to handlers |
| `model/TransactionResult.java` | Result record returned by handlers |
| `model/*.java` (enums) | TransactionType, TransactionStatus, TransactionScenario, RoundState, ParkedStatus |
| `repository/TransactionRepository.java` | Transaction CRUD |
| `repository/RoundRepository.java` | Round CRUD + orphan query |
| `repository/ParkedTransactionRepository.java` | Parked transaction CRUD + expiry query |
| `repository/OutboxRepository.java` | Outbox event CRUD + pending query |
| `processor/TransactionProcessor.java` | Critical path interface |
| `processor/TransactionProcessorImpl.java` | Critical path implementation stub |
| `processor/WalletUpdateService.java` | Atomic conditional UPDATE operations |
| `processor/ScenarioResolver.java` | Determines which scenario handler to use |
| `scenario/TransactionScenarioHandler.java` | Strategy interface for nine handlers |
| `scenario/InsufficientBalanceHandler.java` | Scenario A |
| `scenario/ConcurrentBetHandler.java` | Scenario B |
| `scenario/IdempotentRetryHandler.java` | Scenario C |
| `scenario/OutOfOrderHandler.java` | Scenario D |
| `scenario/RollbackHandler.java` | Scenario E |
| `scenario/ResettlementHandler.java` | Scenario F |
| `scenario/FreeSpinHandler.java` | Scenario G |
| `scenario/BonusBetHandler.java` | Scenario H |
| `scenario/JackpotHandler.java` | Scenario I |
| `statemachine/RoundStateMachineConfig.java` | Spring Statemachine configuration |
| `statemachine/RoundEvent.java` | State machine events enum |
| `statemachine/RoundLifecycleManager.java` | State machine facade interface |
| `detector/OrphanRoundDetector.java` | Scheduled orphan round scanner |
| `scheduler/ParkedTransactionProcessor.java` | Background job for parked transaction retry/expiry |
| `exception/*.java` | Domain exceptions |
| `V3__create_transaction_tables.sql` | Flyway migration for all tables |
| `application.yml` | Transaction-specific configuration |
