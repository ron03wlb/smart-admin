# Section 12 — End-to-End Integration Tests & Key Invariant Verification

> **Module**: `funding-integration-tests` (dedicated test-only module)
> **Depends on**: ALL previous sections (01 through 11)
> **Blocked by**: Every module must have its stubs/interfaces defined before these tests can compile
> **Stack**: Java 17+ / JUnit 5 / Spring Boot 3.x Test / Testcontainers (PostgreSQL + Redis Cluster + Kafka) / Awaitility / CyclicBarrier

---

## Table of Contents

1. [Overview](#1-overview)
2. [Test Infrastructure Setup](#2-test-infrastructure-setup)
3. [Key Invariant Tests (INV-1 through INV-10)](#3-key-invariant-tests-inv-1-through-inv-10)
4. [Cross-Module Scenario Tests](#4-cross-module-scenario-tests)
5. [Performance & Concurrency Stress Tests](#5-performance--concurrency-stress-tests)
6. [Dependencies & Module References](#6-dependencies--module-references)
7. [Implementation Checklist](#7-implementation-checklist)

---

## 1. Overview

This section defines the final verification layer for the entire Funding Domain. While individual modules have their own unit and integration tests, this section covers tests that **span multiple modules** and verify **system-wide invariants** that no single module can guarantee alone.

Three categories of tests:

| Category | Purpose | Count |
|----------|---------|-------|
| Key Invariants (INV-1 to INV-10) | Must-hold properties verified across all API paths | 10 test classes |
| Cross-Module Scenarios | End-to-end flows through deposit, wallet, withdrawal, reconciliation | 4 scenario test classes |
| Performance / Concurrency Stress | Race conditions, throughput, precision under load | 3 stress test classes |

**Naming convention**: `*IntegrationTest.java` for all tests in this section.

**Scale context**: The platform targets 5,000 to 20,000 concurrent Seamless Wallet requests/sec. Stress tests must demonstrate correctness at concurrency levels of at least 10+ concurrent threads per player.

---

## 2. Test Infrastructure Setup

### 2.1 Shared Testcontainers Base Class

All integration tests in this module extend a common base that provisions real infrastructure via Testcontainers. This avoids container startup per test class.

**File**: `src/test/java/com/funding/integration/BaseIntegrationTest.java`

```java
package com.funding.integration;

import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for all cross-module integration tests.
 *
 * Provisions:
 * - PostgreSQL 15 (real DB with schema migrations via Flyway)
 * - Redis 7 (single-node for tests; sufficient for Redisson locking)
 * - Kafka (Confluent) for event verification
 *
 * All containers are shared across test classes via static lifecycle.
 * Spring profile "integration-test" activates governance stubs and
 * disables external service calls (risk API, wagering API, blockchain monitor).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles({"integration-test", "stub"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("funding_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final GenericContainer<?> REDIS =
        new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static final KafkaContainer KAFKA =
        new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> true);

        // Redis (Redisson config)
        registry.add("spring.redis.host", REDIS::getHost);
        registry.add("spring.redis.port", () -> REDIS.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
```

### 2.2 Test Data Builders

**File**: `src/test/java/com/funding/integration/TestDataFactory.java`

```java
package com.funding.integration;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Factory for constructing test entities used across all integration tests.
 *
 * Provides builders for:
 * - Player wallet setup (CASH + BONUS + CREDIT with configurable balances)
 * - Tenant/jurisdiction configuration
 * - GP adapter configuration (token secrets, rate limits)
 * - PSP configuration (routing weights, circuit breaker thresholds)
 * - Deposit/withdrawal request payloads
 * - Seamless wallet API request payloads (Debit, Credit, Rollback, Adjust)
 */
public final class TestDataFactory {

    private TestDataFactory() {}

    /** Create a player with three wallets initialized to the given balances. */
    public static PlayerTestData createPlayerWithBalances(
            UUID tenantId,
            BigDecimal cashBalance,
            BigDecimal bonusBalance,
            BigDecimal creditLimit) {
        // TODO: Implement — insert wallet rows via repository, return player context
        throw new UnsupportedOperationException("Stub");
    }

    /** Create a valid HMAC-SHA256 token for seamless wallet API calls. */
    public static String createValidToken(UUID playerId, String gpSecret) {
        // TODO: Implement — generate token per §5.2 spec
        throw new UnsupportedOperationException("Stub");
    }

    /** Create a deposit request payload for the given amount and payment method. */
    public static DepositRequestTestData createDepositRequest(
            UUID playerId, UUID tenantId, BigDecimal amount, String paymentMethod) {
        // TODO: Implement
        throw new UnsupportedOperationException("Stub");
    }

    /** Create a withdrawal request payload for the given amount. */
    public static WithdrawalRequestTestData createWithdrawalRequest(
            UUID playerId, UUID tenantId, BigDecimal amount) {
        // TODO: Implement
        throw new UnsupportedOperationException("Stub");
    }

    /** Create a seamless wallet debit (bet) request. */
    public static SeamlessDebitRequestTestData createDebitRequest(
            UUID playerId, UUID roundId, BigDecimal amount, String gpId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Stub");
    }

    /** Create a seamless wallet credit (win) request. */
    public static SeamlessCreditRequestTestData createCreditRequest(
            UUID playerId, UUID roundId, BigDecimal amount, String gpId) {
        // TODO: Implement
        throw new UnsupportedOperationException("Stub");
    }
}
```

### 2.3 Kafka Test Consumer

**File**: `src/test/java/com/funding/integration/KafkaTestConsumer.java`

```java
package com.funding.integration;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory Kafka consumer for integration test assertions.
 *
 * Listens to all funding.* topics and stores received events.
 * Provides Awaitility-friendly poll methods:
 *   - awaitEvent(topic, transactionId, timeout)
 *   - getEventsForTopic(topic)
 *   - assertNoEventPublished(topic, duration)
 */
public class KafkaTestConsumer {

    private final CopyOnWriteArrayList<ReceivedEvent> events = new CopyOnWriteArrayList<>();

    /** Block until an event with the given transactionId appears on the topic. */
    public ReceivedEvent awaitEvent(String topic, String transactionId, Duration timeout) {
        // TODO: Implement with Awaitility
        throw new UnsupportedOperationException("Stub");
    }

    /** Return all events received on the given topic so far. */
    public List<ReceivedEvent> getEventsForTopic(String topic) {
        // TODO: Implement
        throw new UnsupportedOperationException("Stub");
    }

    /** Assert that no event is published to the topic within the given duration. */
    public void assertNoEventPublished(String topic, Duration duration) {
        // TODO: Implement
        throw new UnsupportedOperationException("Stub");
    }
}
```

### 2.4 Gradle Module Configuration

**File**: `funding-integration-tests/build.gradle`

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.x'
    id 'io.spring.dependency-management' version '1.1.x'
}

dependencies {
    // All funding modules as test dependencies
    testImplementation project(':wallet-core')
    testImplementation project(':wallet-transaction')
    testImplementation project(':seamless-wallet-api')
    testImplementation project(':wallet-reconciliation')
    testImplementation project(':payment-gateway')
    testImplementation project(':payment-deposit')
    testImplementation project(':payment-withdrawal')
    testImplementation project(':payment-dispute')
    testImplementation project(':currency-engine')
    testImplementation project(':shared')

    // Test stack
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'org.awaitility:awaitility:4.2.0'
    testImplementation 'org.redisson:redisson-spring-boot-starter:3.25.0'
}

// Tag for CI: integration tests run after unit tests pass
tasks.named('test') {
    useJUnitPlatform {
        includeTags 'integration'
    }
    // Increase heap for concurrent stress tests
    jvmArgs '-Xmx1g'
}
```

---

## 3. Key Invariant Tests (INV-1 through INV-10)

These 10 invariants are the must-hold properties from the plan (claude-plan.md section 15). Each is verified as an independent test class that exercises the invariant across all relevant API paths.

---

### 3.1 INV-1: No Player-Initiated Operation Results in Negative CASH/BONUS Balance

**Rule**: Playable balance must remain >= 0 for CASH and BONUS wallets after any player-initiated operation. The DB CHECK constraint `(balance >= 0 OR walletType = 'CREDIT')` is the last resort, but application-layer guards must prevent it from ever firing.

**File**: `src/test/java/com/funding/integration/invariant/Inv1NonNegativeBalanceIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-1: No player-initiated operation results in negative CASH/BONUS balance.
 *
 * Verifies across all API paths that can deduct money:
 * - Seamless Wallet Debit (bet)
 * - Withdrawal lock
 * - Multi-wallet deduction with partial balances
 * - Concurrent debit attempts
 *
 * DB CHECK constraint is the safety net, but the Redisson lock + WHERE balance >= amount
 * guard should prevent the constraint from ever being violated.
 */
@Tag("integration")
class Inv1NonNegativeBalanceIntegrationTest extends BaseIntegrationTest {

    @Test
    void debit_exactBalance_shouldSucceedWithZeroRemaining() {
        // GIVEN: Player with CASH=100.0000, BONUS=0.0000
        // WHEN:  Seamless Debit for 100.0000
        // THEN:  CASH balance == 0.0000
        //        Transaction succeeds
        //        No DB constraint violation
    }

    @Test
    void debit_exceedsBalance_shouldRejectWithoutModifyingBalance() {
        // GIVEN: Player with CASH=50.0000, BONUS=30.0000
        // WHEN:  Seamless Debit for 100.0000 (exceeds total 80.0000)
        // THEN:  HTTP error response with available balance breakdown
        //        CASH still 50.0000, BONUS still 30.0000
    }

    @Test
    void multiWalletDeduction_partialBonusInsufficientCash_shouldReject() {
        // GIVEN: Player with BONUS=40.0000, CASH=20.0000
        //        Default deduction order: BONUS -> CASH
        // WHEN:  Debit for 70.0000 (BONUS covers 40, CASH only has 20)
        // THEN:  Entire transaction rolled back
        //        BONUS still 40.0000, CASH still 20.0000
        //        No partial deduction applied
    }

    @Test
    void withdrawal_exceedsCashBalance_shouldReject() {
        // GIVEN: Player with CASH=500.0000
        // WHEN:  Withdrawal request for 600.0000
        // THEN:  Rejected at locking stage
        //        CASH still 500.0000, no locked amount
    }

    @Test
    void concurrentDebits_shouldNeverResultInNegativeBalance() {
        // GIVEN: Player with CASH=100.0000
        // WHEN:  5 concurrent debit requests of 30.0000 each
        // THEN:  At most 3 succeed (3 * 30 = 90 <= 100)
        //        Final CASH balance >= 0
        //        Query DB directly: SELECT balance FROM wallet WHERE player_id = ? AND wallet_type = 'CASH'
        //        Assert balance >= 0
    }

    @Test
    void dbCheckConstraint_directSqlAttemptToSetNegative_shouldFail() {
        // GIVEN: Player with CASH=10.0000
        // WHEN:  Direct SQL: UPDATE wallet SET balance = -1 WHERE wallet_type = 'CASH' AND player_id = ?
        // THEN:  PostgreSQL CHECK constraint violation exception
        //        This verifies the DB safety net is properly configured
    }
}
```

---

### 3.2 INV-2: Every Transaction Has a Unique transactionId (DB Constraint)

**Rule**: The `transactionId` column has a DB unique constraint. Combined with the idempotency store, this guarantees no duplicate processing. The idempotency key is composed as `{tenantId}:{endpoint}:{gpId}:{transactionId}`.

**File**: `src/test/java/com/funding/integration/invariant/Inv2UniqueTransactionIdIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-2: Every transaction has a unique transactionId (DB constraint enforced).
 *
 * Verifies:
 * - DB unique constraint on transactions table
 * - Idempotency store returns cached response on duplicate transactionId
 * - Cross-tenant isolation (same transactionId, different tenant = different transactions)
 */
@Tag("integration")
class Inv2UniqueTransactionIdIntegrationTest extends BaseIntegrationTest {

    @Test
    void duplicateTransactionId_shouldReturnCachedResponseWithoutReprocessing() {
        // GIVEN: Player with CASH=1000.0000
        //        First debit with transactionId="TX-001" for 50.0000 succeeds
        // WHEN:  Second debit with same transactionId="TX-001", same body
        // THEN:  Returns same response as first call
        //        CASH balance debited only once (950.0000, not 900.0000)
        //        Only one row in transactions table with transactionId="TX-001"
    }

    @Test
    void duplicateTransactionId_differentBody_shouldReturnErrorAndLogWarning() {
        // GIVEN: First debit with transactionId="TX-002" for 50.0000 succeeds
        // WHEN:  Second debit with transactionId="TX-002" but amount=75.0000
        // THEN:  Error response (fingerprint mismatch)
        //        Balance NOT modified by second request
        //        Warning log entry for fingerprint mismatch
    }

    @Test
    void sameTransactionId_differentTenant_shouldBothSucceed() {
        // GIVEN: Tenant-A player with CASH=500.0000
        //        Tenant-B player with CASH=500.0000
        // WHEN:  Debit with transactionId="TX-003" for Tenant-A
        //        Debit with transactionId="TX-003" for Tenant-B
        // THEN:  Both succeed (idempotency key includes tenantId)
        //        Two separate rows in transactions table
    }

    @Test
    void dbUniqueConstraint_directInsertDuplicate_shouldFail() {
        // GIVEN: A transaction row with transactionId="TX-004" exists
        // WHEN:  Direct SQL INSERT with same transactionId + same tenant
        // THEN:  DB unique constraint violation exception
    }
}
```

---

### 3.3 INV-3: Every Deduction Records Which Priority Layer Was Applied

**Rule**: Every `DeductionStep` in a multi-wallet deduction must record the `appliedLayer` (1-4) and `ruleId` for audit. This enables reconstructing why a particular deduction order was chosen after the fact.

**File**: `src/test/java/com/funding/integration/invariant/Inv3DeductionAuditTrailIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-3: Every deduction records which priority layer was applied.
 *
 * Four-layer priority system:
 *   Layer 1: Player-level override (e.g., VIP prefers CASH)
 *   Layer 2: GP contract rule (e.g., GP rejects BONUS)
 *   Layer 3: Game-level rule (e.g., Live Casino = CASH only)
 *   Layer 4: System default (BONUS -> CASH -> CREDIT)
 *
 * Each DeductionStep must have: walletType, amount, appliedLayer, ruleId.
 */
@Tag("integration")
class Inv3DeductionAuditTrailIntegrationTest extends BaseIntegrationTest {

    @Test
    void systemDefault_deduction_shouldRecordLayer4() {
        // GIVEN: Player with BONUS=50.0000, CASH=100.0000
        //        No player/GP/game-level overrides configured
        // WHEN:  Debit for 70.0000 (default order: BONUS -> CASH)
        // THEN:  Two DeductionStep records in DB:
        //          Step 1: walletType=BONUS, amount=50.0000, appliedLayer=4, ruleId=<system-default-id>
        //          Step 2: walletType=CASH, amount=20.0000, appliedLayer=4, ruleId=<system-default-id>
    }

    @Test
    void gpContractRejectsBonus_shouldRecordLayer2() {
        // GIVEN: Player with BONUS=50.0000, CASH=100.0000
        //        GP contract rule: "rejects BONUS wallet"
        // WHEN:  Debit for 70.0000 via this GP
        // THEN:  Single DeductionStep:
        //          Step 1: walletType=CASH, amount=70.0000, appliedLayer=2, ruleId=<gp-rule-id>
        //        BONUS wallet untouched
    }

    @Test
    void playerOverride_shouldRecordLayer1AndTakePrecedence() {
        // GIVEN: Player with BONUS=50.0000, CASH=100.0000
        //        Player-level rule: "prefer CASH first"
        //        GP contract rule also exists (would be Layer 2)
        // WHEN:  Debit for 70.0000
        // THEN:  DeductionSteps use Layer 1 (player override wins):
        //          Step 1: walletType=CASH, amount=70.0000, appliedLayer=1, ruleId=<player-rule-id>
    }

    @Test
    void gameLevelOverride_liveCasinoCashOnly_shouldRecordLayer3() {
        // GIVEN: Player with BONUS=50.0000, CASH=100.0000
        //        Game-level rule: "Live Casino = CASH only"
        // WHEN:  Debit for 30.0000 with game context = Live Casino
        // THEN:  Single DeductionStep:
        //          walletType=CASH, amount=30.0000, appliedLayer=3
    }

    @Test
    void allDeductionSteps_shouldSumToExactRequestedAmount() {
        // GIVEN: Player with BONUS=25.5000, CASH=74.5000
        // WHEN:  Debit for 100.0000
        // THEN:  Sum of all DeductionStep amounts == 100.0000 exactly
        //        Query: SELECT SUM(amount) FROM deduction_steps WHERE transaction_id = ?
    }
}
```

---

### 3.4 INV-4: 10,000 Sequential Transactions Accumulate <= 0.0001 Rounding Error

**Rule**: All monetary arithmetic uses `BigDecimal` with `ROUND_HALF_UP`, stored at 4 decimal places (`DECIMAL(19,4)`). After 10,000 sequential operations, cumulative rounding error must not exceed 0.0001.

**File**: `src/test/java/com/funding/integration/invariant/Inv4PrecisionAccumulationIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-4: 10,000 sequential transactions accumulate <= 0.0001 rounding error.
 *
 * The Money value object enforces:
 * - BigDecimal created from String (NEVER from double)
 * - Arithmetic always specifies ROUND_HALF_UP
 * - Scale is always 4 for storage, up to 8 for intermediate calculations
 *
 * This test runs a long sequence of small transactions and verifies that the
 * final DB balance matches the mathematically expected value within tolerance.
 */
@Tag("integration")
class Inv4PrecisionAccumulationIntegrationTest extends BaseIntegrationTest {

    @Test
    void tenThousandSequentialDebits_shouldAccumulateLessThanPointZeroZeroZeroOneError() {
        // GIVEN: Player with CASH=1000000.0000 (large enough for 10K debits)
        // WHEN:  Execute 10,000 sequential debit transactions of 3.3333 each
        //        (3.3333 chosen because it produces rounding scenarios)
        // THEN:  Expected deducted: 10000 * 3.3333 = 33333.0000
        //        Actual DB balance: 1000000.0000 - 33333.0000 = 966667.0000
        //        abs(actual - expected) <= 0.0001
    }

    @Test
    void tenThousandAlternatingDebitCredit_shouldPreserveBalance() {
        // GIVEN: Player with CASH=10000.0000
        // WHEN:  10,000 cycles of: debit 1.1111 then credit 1.1111
        // THEN:  Final balance == 10000.0000 (exact, no drift)
        //        abs(finalBalance - initialBalance) <= 0.0001
    }

    @Test
    void multiWalletDeductionPrecision_shouldNotDriftAcrossWallets() {
        // GIVEN: Player with BONUS=5000.0000, CASH=5000.0000
        // WHEN:  1,000 sequential debits of 7.7777 each (spans BONUS then CASH)
        //        Total expected: 7777.7000
        // THEN:  BONUS.balance + CASH.balance == 10000.0000 - 7777.7000 = 2222.3000
        //        abs(actual_sum - expected_sum) <= 0.0001
    }
}
```

---

### 3.5 INV-5: CREDIT Wallet Balance Cannot Be Withdrawn via Any API Path

**Rule**: The CREDIT wallet is for game play only. No withdrawal, no cashout, no transfer to CASH. Enforced at both the API layer and DB constraint level.

**File**: `src/test/java/com/funding/integration/invariant/Inv5CreditNotWithdrawableIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-5: CREDIT wallet balance cannot be withdrawn via any API path.
 *
 * The CREDIT wallet exists for credit-mode play (creditLimit - usedAmount).
 * Players must NEVER be able to extract CREDIT balance as real money.
 */
@Tag("integration")
class Inv5CreditNotWithdrawableIntegrationTest extends BaseIntegrationTest {

    @Test
    void withdrawal_fromCreditWallet_shouldBeRejected() {
        // GIVEN: Player with CASH=0.0000, CREDIT limit=5000.0000, usedAmount=0
        // WHEN:  Withdrawal request for 1000.0000
        // THEN:  Rejected — withdrawal only considers CASH balance
        //        CREDIT wallet unchanged
    }

    @Test
    void withdrawal_shouldOnlyConsiderCashBalance() {
        // GIVEN: Player with CASH=200.0000, BONUS=300.0000, CREDIT limit=5000.0000
        // WHEN:  Withdrawal request for 200.0000
        // THEN:  Succeeds using CASH wallet only
        //        CREDIT wallet completely uninvolved
    }

    @Test
    void seamlessDebit_creditModePlayer_winShouldNotBeCashWithdrawable() {
        // GIVEN: Player in credit mode, CREDIT limit=1000.0000
        // WHEN:  GP credit (win) of 500.0000 — credits reduce usedAmount
        // THEN:  Win reduces CREDIT.usedAmount, does NOT add to CASH
        //        Subsequent withdrawal attempt for 500.0000 fails (CASH=0)
    }

    @Test
    void playableBalance_creditMode_shouldNotIncludeCashOrBonus() {
        // GIVEN: Player with CASH=100.0000, CREDIT limit=5000.0000, usedAmount=1000.0000
        //        Game context = credit mode
        // WHEN:  GetBalance request
        // THEN:  Playable balance = 5000 - 1000 = 4000.0000
        //        CASH balance is NOT included in credit-mode playable balance
    }
}
```

---

### 3.6 INV-6: Negative Balance Detected -> Account Locked Within <= 5 Seconds

**Rule**: If any CASH or BONUS wallet goes negative (only possible via resettlement — Scenario F), the system must detect and lock the account within 5 seconds. Monitored via `wallet.negative_balance.total` metric.

**File**: `src/test/java/com/funding/integration/invariant/Inv6NegativeBalanceLockIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-6: Negative balance detected -> account locked within <= 5 seconds.
 *
 * Only resettlement (Scenario F) can create a controlled negative balance.
 * When this happens:
 * 1. A NegativeBalanceAlert Kafka event is published
 * 2. The account status transitions to SUSPENDED
 * 3. All subsequent operations for this player are rejected
 *
 * Uses Awaitility for timing assertions.
 */
@Tag("integration")
class Inv6NegativeBalanceLockIntegrationTest extends BaseIntegrationTest {

    @Test
    void resettlement_causesNegativeBalance_shouldLockAccountWithinFiveSeconds() {
        // GIVEN: Player with CASH=10.0000
        //        Original bet was 50.0000 (processed normally)
        //        Resettlement adjusts to 80.0000 (difference = 30.0000 more deducted)
        // WHEN:  Resettlement executes, CASH goes to -20.0000
        // THEN:  Within 5 seconds (Awaitility):
        //          - Wallet status == SUSPENDED
        //          - NegativeBalanceAlert event published to funding.wallet.negative-balance topic
        //          - Subsequent debit requests for this player return error
    }

    @Test
    void lockedAccount_shouldRejectAllSubsequentOperations() {
        // GIVEN: Player account already locked due to negative balance
        // WHEN:  Seamless Debit request
        // THEN:  Rejected with account-suspended error
        // WHEN:  Seamless Credit (win) request
        // THEN:  Also rejected (even credits blocked on suspended account)
        // WHEN:  Withdrawal request
        // THEN:  Also rejected
    }

    @Test
    void negativeBalance_shouldPublishKafkaAlertEvent() {
        // GIVEN: Resettlement causes negative balance
        // WHEN:  Event processing completes
        // THEN:  KafkaTestConsumer receives event on funding.wallet.negative-balance topic
        //        Event contains: playerId, tenantId, walletType, negativeAmount, transactionId
    }
}
```

---

### 3.7 INV-7: Wagering Service Down -> Withdrawal NEVER Auto-Approved

**Rule**: This is a CRITICAL invariant. If the wagering verification service (gaming domain) is unavailable (timeout > 500ms), the withdrawal must be queued as `WAGERING_CHECK_PENDING` and NEVER auto-approved. After 2 hours unresolved, it escalates to CS.

**File**: `src/test/java/com/funding/integration/invariant/Inv7WageringDownNeverAutoApproveIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-7: Wagering service down -> withdrawal NEVER auto-approved.
 *
 * The wagering verification is a synchronous call to the gaming domain:
 *   GET /api/v1/wagering/{playerId}/status (SLA: < 200ms)
 *
 * Degradation behavior:
 * - Timeout > 500ms -> UNAVAILABLE
 * - Queue withdrawal as WAGERING_CHECK_PENDING
 * - Background job retries every 5 minutes
 * - After 2 hours unresolved -> escalate to CS
 * - CRITICAL: NEVER auto-approve
 *
 * This test uses WireMock or a configurable stub to simulate wagering service failures.
 */
@Tag("integration")
class Inv7WageringDownNeverAutoApproveIntegrationTest extends BaseIntegrationTest {

    @Test
    void wageringServiceTimeout_shouldQueueAsPendingNotAutoApprove() {
        // GIVEN: Player with CASH=1000.0000, wagering requirement exists
        //        Wagering service stub configured to delay > 500ms (timeout)
        // WHEN:  Withdrawal request for 500.0000
        // THEN:  Withdrawal status == WAGERING_CHECK_PENDING
        //        Amount remains locked (500.0000)
        //        Withdrawal is NOT approved
        //        No PSP call made
        //        No wallet debit executed
    }

    @Test
    void wageringServiceDown_shouldNeverProgressToApproval() {
        // GIVEN: Wagering service stub returns HTTP 503 (service unavailable)
        // WHEN:  Withdrawal request for 500.0000
        // THEN:  Status == WAGERING_CHECK_PENDING
        //        Wait 5 seconds — status still WAGERING_CHECK_PENDING (not approved)
        //        Verify in DB: approval_status IS NULL (never set to APPROVED)
    }

    @Test
    void wageringServiceRecovers_pendingWithdrawal_shouldResumeNormally() {
        // GIVEN: Withdrawal in WAGERING_CHECK_PENDING state
        //        Wagering service stub now returns COMPLETE
        // WHEN:  Background retry job executes
        // THEN:  Withdrawal proceeds through normal approval workflow
        //        Eventually reaches PSP and debits wallet
    }

    @Test
    void wageringCheckPending_afterTwoHours_shouldEscalateToCS() {
        // GIVEN: Withdrawal in WAGERING_CHECK_PENDING state since > 2 hours ago
        //        (Simulate by inserting with timestamp 2+ hours in the past)
        // WHEN:  Escalation scheduler runs
        // THEN:  CS ticket created with withdrawal details
        //        Withdrawal status updated to ESCALATED
        //        Amount still locked (never auto-released)
    }
}
```

---

### 3.8 INV-8: Reconciliation Follows GP as Authority

**Rule**: For both amount mismatches and existence disputes, the GP record is always authoritative. Missing on platform -> create from GP record. Missing on GP -> reverse the platform transaction. This is a stakeholder decision.

**File**: `src/test/java/com/funding/integration/invariant/Inv8GpAuthoritativeReconciliationIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-8: Reconciliation follows GP as authority for both amount and existence.
 *
 * Hourly reconciliation compares platform vs GP records:
 * - Amount mismatch < $1 -> auto-correct to GP amount
 * - Amount mismatch > $100 -> manual alert
 * - Missing on platform -> create transaction from GP record
 * - Missing on GP -> reverse the platform transaction
 *
 * Reversal circuit breaker: > N reversals per GP per hour -> pause + escalate.
 */
@Tag("integration")
class Inv8GpAuthoritativeReconciliationIntegrationTest extends BaseIntegrationTest {

    @Test
    void amountMismatch_belowOneDollar_shouldAutoCorrectToGpAmount() {
        // GIVEN: Platform has transaction TX-100 with amount=99.5000
        //        GP report has TX-100 with amount=100.0000 (diff = 0.50)
        // WHEN:  Hourly reconciliation runs
        // THEN:  Platform amount corrected to 100.0000
        //        Wallet balance adjusted by +0.5000
        //        Reconciliation log records the auto-correction
    }

    @Test
    void amountMismatch_aboveOneHundred_shouldCreateManualAlert() {
        // GIVEN: Platform has transaction TX-101 with amount=500.0000
        //        GP report has TX-101 with amount=750.0000 (diff = 250.00)
        // WHEN:  Hourly reconciliation runs
        // THEN:  No auto-correction applied
        //        Alert generated with discrepancy details
        //        Reconciliation status = PENDING_MANUAL_REVIEW
    }

    @Test
    void missingOnPlatform_shouldCreateTransactionFromGpRecord() {
        // GIVEN: GP report contains transaction TX-102 (debit, 30.0000)
        //        Platform has no record of TX-102
        // WHEN:  Hourly reconciliation runs
        // THEN:  New transaction created from GP data
        //        Wallet balance debited by 30.0000
        //        Transaction flagged as "reconciliation-created"
    }

    @Test
    void missingOnGp_shouldReversePlatformTransaction() {
        // GIVEN: Platform has transaction TX-103 (debit, 40.0000)
        //        GP report does NOT contain TX-103
        // WHEN:  Hourly reconciliation runs
        // THEN:  Platform transaction reversed
        //        Wallet balance credited back 40.0000
        //        Reversal preserves GP report snapshot for audit
    }

    @Test
    void reversalCircuitBreaker_exceedsThreshold_shouldPauseAndEscalate() {
        // GIVEN: GP "GP-X" reconciliation triggers 11 reversals (threshold=10)
        // WHEN:  Hourly reconciliation processes the 11th reversal
        // THEN:  Auto-reversal paused for GP-X
        //        Remaining discrepancies queued for manual review
        //        Escalation alert sent
    }
}
```

---

### 3.9 INV-9: Crypto Deposit Credited Only After Confirmation Threshold Met

**Rule**: BTC requires >= 3 confirmations, ETH requires >= 12 confirmations before the deposit is credited to the wallet. Until the threshold is met, the deposit is in PENDING status and the player sees "pending" in their balance.

**File**: `src/test/java/com/funding/integration/invariant/Inv9CryptoConfirmationThresholdIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-9: Crypto deposit credited only after confirmation threshold met.
 *
 * Thresholds (from currency-engine spec):
 * - BTC: >= 3 confirmations
 * - ETH: >= 12 confirmations
 *
 * Pre-credit flow: confirmations met -> AML check -> convert to fiat -> credit CASH
 * High-risk wallet address -> deposit rejected regardless of confirmations.
 * Single deposit > $10K equivalent -> triggers Enhanced Due Diligence (EDD).
 */
@Tag("integration")
class Inv9CryptoConfirmationThresholdIntegrationTest extends BaseIntegrationTest {

    @Test
    void btcDeposit_belowThreeConfirmations_shouldNotCreditWallet() {
        // GIVEN: Player has CASH=0.0000
        //        BTC deposit of 0.01 BTC detected on blockchain
        //        Current confirmations: 2
        // WHEN:  BlockchainMonitor processes the transaction
        // THEN:  Deposit status == PENDING
        //        CASH balance still 0.0000
        //        No credit transaction in transactions table
    }

    @Test
    void btcDeposit_atThreeConfirmations_shouldProceedToAmlAndCredit() {
        // GIVEN: Player has CASH=0.0000
        //        BTC deposit with 3 confirmations
        //        AML check returns clean (no risk flags)
        // WHEN:  BlockchainMonitor processes the transaction
        // THEN:  Deposit status == COMPLETED
        //        CASH balance == converted fiat amount (FX rate * 0.01 BTC)
        //        DepositCompletedEvent published to Kafka
    }

    @Test
    void ethDeposit_belowTwelveConfirmations_shouldNotCreditWallet() {
        // GIVEN: Player has CASH=0.0000
        //        ETH deposit of 1.0 ETH detected
        //        Current confirmations: 11
        // WHEN:  BlockchainMonitor processes the transaction
        // THEN:  Deposit status == PENDING
        //        CASH balance still 0.0000
    }

    @Test
    void cryptoDeposit_highRiskAddress_shouldRejectRegardlessOfConfirmations() {
        // GIVEN: BTC deposit from address flagged as high-risk (mixer/darknet)
        //        Confirmations: 10 (well above threshold)
        // WHEN:  AML screening runs
        // THEN:  Deposit status == REJECTED
        //        CASH balance unchanged
        //        Risk alert published
    }

    @Test
    void cryptoDeposit_aboveTenThousand_shouldTriggerEDD() {
        // GIVEN: BTC deposit equivalent to $15,000
        //        Confirmations met, AML clean
        // WHEN:  Deposit processing runs
        // THEN:  Enhanced Due Diligence triggered before credit
        //        Deposit status == PENDING_EDD until EDD completes
    }
}
```

---

### 3.10 INV-10: Transient Errors (5xx) Never Cached in Idempotency Store

**Rule**: The idempotency store (Redis, TTL 24h) only caches successful responses and deterministic errors (insufficient balance, invalid player). Transient errors (timeout, 5xx, lock contention) must NEVER be cached, so retries can succeed on the next attempt.

**File**: `src/test/java/com/funding/integration/invariant/Inv10TransientErrorNotCachedIntegrationTest.java`

```java
package com.funding.integration.invariant;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * INV-10: Transient errors (5xx) never cached in idempotency store.
 *
 * Idempotency store rules (from seamless-wallet-api spec):
 * - Cache successful responses: YES
 * - Cache deterministic errors (insufficient balance): YES
 * - Cache transient errors (timeout, 5xx, lock contention): NEVER
 * - TTL: 24 hours
 * - Key: {tenantId}:{endpoint}:{gpId}:{transactionId}
 */
@Tag("integration")
class Inv10TransientErrorNotCachedIntegrationTest extends BaseIntegrationTest {

    @Test
    void transientTimeout_shouldNotBeCached_retrySucceeds() {
        // GIVEN: Player with CASH=500.0000
        //        First debit with transactionId="TX-200" triggers a simulated timeout
        //        (e.g., Redisson lock acquisition exceeds 3s wait)
        // WHEN:  First request returns 5xx timeout error
        //        Second request with same transactionId="TX-200"
        // THEN:  Second request is processed normally (not returned from cache)
        //        Debit succeeds, CASH=450.0000
        //        Idempotency store now contains the successful response
    }

    @Test
    void serverError_shouldNotBeCached_retrySucceeds() {
        // GIVEN: Player with CASH=500.0000
        //        First debit triggers simulated internal server error (500)
        // WHEN:  Retry with same transactionId
        // THEN:  Retry is reprocessed (not returned from cache)
        //        Succeeds normally
    }

    @Test
    void deterministicError_shouldBeCached_retryReturnsCachedError() {
        // GIVEN: Player with CASH=10.0000
        //        Debit for 100.0000 with transactionId="TX-201"
        // WHEN:  First request returns INSUFFICIENT_BALANCE error
        //        Player deposits more money (CASH=200.0000)
        //        Retry with same transactionId="TX-201"
        // THEN:  Returns cached INSUFFICIENT_BALANCE error
        //        Does NOT reprocess (the transactionId is "used up")
        //        Balance remains 200.0000 (not debited)
    }

    @Test
    void lockContention_shouldNotBeCached() {
        // GIVEN: Player with CASH=500.0000
        //        Simulate lock contention (another thread holds the Redisson lock)
        //        First debit with transactionId="TX-202" fails with lock timeout
        // WHEN:  Lock is released, retry with same transactionId
        // THEN:  Retry succeeds (lock contention was transient)
        //        Result now cached in idempotency store
    }

    @Test
    void successfulResponse_shouldBeCachedForTwentyFourHours() {
        // GIVEN: Successful debit with transactionId="TX-203"
        // WHEN:  Query Redis for idempotency key
        // THEN:  Key exists with TTL approximately 24 hours
        //        Duplicate request returns cached response
    }
}
```

---

## 4. Cross-Module Scenario Tests

These tests verify complete business flows that traverse multiple modules in sequence. Each scenario represents a real user journey through the system.

---

### 4.1 Full Deposit Flow: Limit Check -> Friction -> PSP Route -> 3DS -> Callback -> Wallet Credit

**File**: `src/test/java/com/funding/integration/scenario/FullDepositFlowIntegrationTest.java`

```java
package com.funding.integration.scenario;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end deposit flow spanning:
 *   payment-deposit -> currency-engine -> payment-gateway -> seamless-wallet-api -> wallet-core
 *
 * Full path:
 *   Player initiates deposit
 *     -> DepositLimitChecker: verify single/daily/monthly limits
 *     -> CashierFrictionEvaluator: determine risk tier (LOW/MEDIUM/HIGH)
 *     -> SmartRouter: select optimal PSP (weighted scoring)
 *     -> PSPAdapter: redirect to PSP payment page
 *     -> (Player completes payment)
 *     -> PSP sends callback
 *     -> CallbackHandler: verify signature, parse result
 *     -> CurrencyEngine: convert to settlement currency (if different)
 *     -> WalletCore: credit CASH wallet
 *     -> Kafka: publish DepositCompletedEvent
 */
@Tag("integration")
class FullDepositFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void successfulDeposit_endToEnd_shouldCreditWalletAndPublishEvent() {
        // GIVEN: Player (KYC L2, 5 past deposits, risk score 20)
        //        CASH balance = 0.0000
        //        Deposit amount = 100.00 USD
        //        PSP stub configured to return success callback
        // WHEN:  POST /api/v1/payment/deposit
        // THEN:  1. Limit check passes (within single/daily/monthly limits)
        //        2. Friction tier = LOW (KYC L2+, 3+ deposits, risk < 30)
        //        3. SmartRouter selects highest-scoring PSP
        //        4. PSP callback processed successfully
        //        5. CASH balance == 100.0000
        //        6. DepositCompletedEvent on funding.payment.deposit topic
        //        7. Transaction record in DB with status COMPLETED
    }

    @Test
    void deposit_exceedsDailyLimit_shouldReject() {
        // GIVEN: Player has already deposited $9,500 today
        //        Daily limit = $10,000
        // WHEN:  New deposit for $600
        // THEN:  Rejected at limit check stage
        //        No PSP call made
        //        No wallet modification
    }

    @Test
    void deposit_withThreeDSChallenge_euCard_shouldCompleteAfterAuthentication() {
        // GIVEN: Player deposits with EU-issued card
        //        PSP returns 3DS challenge URL
        // WHEN:  3DS callback returns AUTHENTICATED
        // THEN:  Payment proceeds
        //        CASH wallet credited
        //        Transaction records 3DS authentication status
    }

    @Test
    void deposit_threeDSFailed_shouldReject() {
        // GIVEN: Player deposits with EU card
        //        3DS callback returns FAILED
        // WHEN:  ThreeDSHandler processes callback
        // THEN:  Deposit rejected
        //        CASH wallet unchanged
        //        Rejection reason logged
    }

    @Test
    void deposit_threeDSAttempted_issuerNotEnrolled_shouldProceed() {
        // GIVEN: Player deposits with card whose issuer is not 3DS enrolled
        //        3DS callback returns ATTEMPTED
        // WHEN:  ThreeDSHandler processes callback
        // THEN:  Deposit proceeds (liability shift to issuer)
        //        CASH wallet credited
    }

    @Test
    void deposit_highFriction_kycL0_shouldRequireExtraVerification() {
        // GIVEN: Player with KYC L0, risk score 75
        // WHEN:  Deposit request
        // THEN:  Friction tier = HIGH (up to 7 steps)
        //        Response includes required verification steps
    }

    @Test
    void deposit_allPSPsUnavailable_shouldReturnExplicitError() {
        // GIVEN: All PSP circuit breakers are OPEN
        // WHEN:  Deposit request
        // THEN:  SmartRouter returns NO_ELIGIBLE_PSP error
        //        Player receives user-friendly error message
        //        No partial processing
    }

    @Test
    void deposit_foreignCurrency_shouldConvertToSettlementCurrency() {
        // GIVEN: Player deposits 100 EUR, settlement currency is USD
        //        FX rate: 1 EUR = 1.10 USD
        // WHEN:  Deposit completes
        // THEN:  CASH wallet credited 110.0000 USD
        //        FX rate frozen for 15 minutes
        //        Both original and converted amounts stored in transaction record
    }
}
```

---

### 4.2 Full Withdrawal Flow: Lock -> Wagering -> Approval -> PSP -> Wallet Debit

**File**: `src/test/java/com/funding/integration/scenario/FullWithdrawalFlowIntegrationTest.java`

```java
package com.funding.integration.scenario;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end withdrawal flow spanning:
 *   payment-withdrawal -> wallet-core -> payment-gateway -> wallet-transaction
 *
 * Full path:
 *   Player requests withdrawal
 *     -> Lock withdrawal amount instantly (< 50ms)
 *     -> WageringVerifier: sync query gaming domain (< 200ms SLA)
 *     -> ApprovalWorkflow: route based on amount tier
 *     -> Approver approves
 *     -> SmartRouter: select PSP
 *     -> PSP confirms
 *     -> Unlock amount -> debit CASH wallet
 *     -> Kafka: publish WithdrawalCompletedEvent
 */
@Tag("integration")
class FullWithdrawalFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void successfulWithdrawal_endToEnd_shouldDebitWalletAndPublishEvent() {
        // GIVEN: Player with CASH=1000.0000
        //        Wagering stub returns COMPLETE
        //        Amount = 50.0000 (< $100 tier = CS Agent auto-approve)
        //        PSP stub returns success
        // WHEN:  POST /api/v1/payment/withdrawal
        // THEN:  1. Amount locked within 50ms
        //        2. Wagering verified as COMPLETE
        //        3. BONUS auto-converted to CASH (if applicable)
        //        4. CS Agent auto-approves (< $100)
        //        5. PSP processes withdrawal
        //        6. CASH balance == 950.0000
        //        7. WithdrawalCompletedEvent on funding.payment.withdrawal topic
    }

    @Test
    void withdrawal_wageringNotMet_shouldRejectWithRemainingAmount() {
        // GIVEN: Player with CASH=500.0000
        //        Wagering stub returns INCOMPLETE(remaining=200.0000, progress=75%)
        // WHEN:  Withdrawal request for 300.0000
        // THEN:  Rejected
        //        Response includes remaining wagering amount: 200.0000
        //        Locked amount released
        //        CASH balance unchanged at 500.0000
    }

    @Test
    void withdrawal_multiTierApproval_cfoRequired() {
        // GIVEN: Player with CASH=5000.0000
        //        Wagering complete
        //        Amount = 5000.0000 ($1K-$10K tier -> CFO approval, 4h SLA)
        // WHEN:  Withdrawal request
        // THEN:  Status == PENDING_APPROVAL
        //        Approval routed to CFO
        //        Amount locked (playable balance = 0)
        //        No PSP call until approved
    }

    @Test
    void withdrawal_dualApproval_overTenK() {
        // GIVEN: Player with CASH=15000.0000
        //        Amount = 15000.0000 (> $10K -> CFO + CEO dual, 24h SLA)
        // WHEN:  Withdrawal request -> CFO approves -> CEO approves
        // THEN:  After both approvals: PSP call made, wallet debited
        //        WithdrawalCompletedEvent includes both approval records
    }

    @Test
    void withdrawal_amountLock_shouldReducePlayableBalance() {
        // GIVEN: Player with CASH=1000.0000
        //        Withdrawal request for 700.0000 (amount locked)
        // WHEN:  GetBalance request from GP (seamless wallet API)
        // THEN:  Playable balance = 1000 - 700 = 300.0000
        //        Locked amount visible in balance breakdown
    }

    @Test
    void withdrawal_pspFailure_shouldUnlockAmountAndNotifyPlayer() {
        // GIVEN: Withdrawal approved, PSP stub returns failure
        // WHEN:  PSP call fails
        // THEN:  Locked amount released
        //        CASH balance restored to original
        //        Player notified of failure
        //        Retry via fallback PSP (if soft decline)
    }
}
```

---

### 4.3 GP Seamless Wallet Round: GetBalance -> Debit -> Credit -> Round Close

**File**: `src/test/java/com/funding/integration/scenario/SeamlessWalletRoundIntegrationTest.java`

```java
package com.funding.integration.scenario;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * End-to-end GP seamless wallet round spanning:
 *   seamless-wallet-api -> wallet-transaction -> wallet-core
 *
 * A complete game round:
 *   1. GP calls GetBalance -> returns playable balance
 *   2. GP calls Debit (bet) -> deducts from wallet(s) per priority
 *   3. GP calls Credit (win) -> credits CASH wallet
 *   4. Round closes (state machine: OPEN -> CLOSED)
 *
 * Covers: token validation, idempotency, deduction priority, round lifecycle,
 *         out-of-order handling, rollback, and multi-wallet flows.
 */
@Tag("integration")
class SeamlessWalletRoundIntegrationTest extends BaseIntegrationTest {

    @Test
    void completeRound_betThenWin_shouldTransitionOpenToClose() {
        // GIVEN: Player with CASH=500.0000, BONUS=200.0000
        //        Valid GP token
        // WHEN:  1. GetBalance -> returns 700.0000 (CASH + BONUS)
        //        2. Debit (bet) 50.0000 -> deducts from BONUS first (default priority)
        //        3. Credit (win) 120.0000 -> credits CASH wallet
        // THEN:  Final CASH = 500.0000 + 120.0000 = 620.0000
        //        Final BONUS = 200.0000 - 50.0000 = 150.0000
        //        Round state == CLOSED
        //        All responses include updated balance
        //        WalletDebitEvent + WalletCreditEvent published to Kafka
    }

    @Test
    void completeRound_betThenLoss_shouldCloseRound() {
        // GIVEN: Player with CASH=300.0000
        // WHEN:  Debit (bet) 30.0000 -> Credit (win) 0.0000
        // THEN:  CASH = 270.0000
        //        Round state == CLOSED
    }

    @Test
    void outOfOrder_creditBeforeDebit_shouldParkAndProcess() {
        // GIVEN: Player with CASH=500.0000
        // WHEN:  1. Credit (win) 100.0000 arrives BEFORE Debit (bet)
        //        2. Credit is parked in DB (parked_transactions table) + Redis (parking:{roundId})
        //        3. Debit (bet) 50.0000 arrives
        // THEN:  Debit processes normally
        //        Parked credit is dequeued and processed
        //        Final CASH = 500 - 50 + 100 = 550.0000
        //        Round state == CLOSED
    }

    @Test
    void rollback_shouldReverseDebitAndCancelRound() {
        // GIVEN: Player with CASH=500.0000
        //        Debit (bet) 50.0000 processed (CASH=450.0000, round=OPEN)
        // WHEN:  Rollback for the debit transaction
        // THEN:  CASH = 500.0000 (restored)
        //        Round state == CANCELLED
        //        WalletRollbackEvent published
    }

    @Test
    void rollback_originalNotFound_shouldReturnSuccessIdempotent() {
        // GIVEN: Rollback request references a transactionId that does not exist
        // WHEN:  POST /api/v1/seamless/rollback
        // THEN:  Returns Success (idempotent — treat as already rolled back)
        //        No balance change
    }

    @Test
    void multiWalletDeduction_bonusThenCash_shouldDeductInOrder() {
        // GIVEN: Player with BONUS=30.0000, CASH=100.0000
        //        Default priority: BONUS -> CASH
        // WHEN:  Debit 50.0000
        // THEN:  BONUS = 0.0000 (depleted)
        //        CASH = 100.0000 - 20.0000 = 80.0000 (remainder)
        //        DeductionSteps recorded with correct amounts and layers
    }

    @Test
    void gpRejectsBonus_shouldExcludeBonusFromDeduction() {
        // GIVEN: Player with BONUS=100.0000, CASH=200.0000
        //        GP contract: rejects BONUS wallet
        // WHEN:  Debit 50.0000
        // THEN:  BONUS untouched (100.0000)
        //        CASH = 150.0000
        //        Deduction recorded with Layer 2 (GP contract)
    }

    @Test
    void tokenExpired_onDebit_shouldRejectStrictly() {
        // GIVEN: GP token created 6 minutes ago (expired, TTL = 5 min)
        // WHEN:  Debit request with expired token
        // THEN:  Rejected (STRICT mode for mutations)
    }

    @Test
    void tokenExpired_onGetBalance_shouldAllowGracePeriod() {
        // GIVEN: GP token created 5 minutes 20 seconds ago
        //        (expired by 20s, but within 30s grace for GetBalance)
        // WHEN:  GetBalance request
        // THEN:  Accepted (LENIENT mode, 30s grace)
    }

    @Test
    void orphanRound_openForOverTwoHours_shouldBeDetected() {
        // GIVEN: Round opened 3 hours ago, no settlement received
        // WHEN:  Orphan round detector runs (every 15 minutes)
        // THEN:  GP API queried for round status
        //        If GP returns result -> round auto-closed
        //        If GP API fails -> round marked PENDING_REVIEW
    }

    @Test
    void highValueOrphan_overOneThousand_shouldCreatePriorityTicket() {
        // GIVEN: Orphan round with bet amount > $1,000
        // WHEN:  Orphan detector processes it
        // THEN:  Priority CS ticket created with 24h SLA
    }

    @Test
    void resettlement_shouldAdjustBalanceAndAllowControlledNegative() {
        // GIVEN: Original bet was 50.0000, player CASH=100.0000
        //        Round is CLOSED
        // WHEN:  Adjust (resettlement) changes bet to 200.0000 (additional 150.0000 deducted)
        // THEN:  CASH = 100 - 150 = -50.0000 (controlled negative via Scenario F)
        //        Round state == ADJUSTED
        //        Negative balance protection triggered (INV-6)
    }
}
```

---

### 4.4 Concurrent Stress: 10+ Concurrent Debits for Same Player -> Zero Over-Deduction

**File**: `src/test/java/com/funding/integration/scenario/ConcurrentDebitStressIntegrationTest.java`

```java
package com.funding.integration.scenario;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Concurrent stress tests for the transaction processor.
 *
 * The critical path uses double-layer concurrency control:
 *   Layer 1: Redisson distributed lock on player:{playerId} (serialize at app level)
 *   Layer 2: DB atomic conditional update WHERE balance >= amount (safety net)
 *
 * These tests use CyclicBarrier / CountDownLatch to synchronize concurrent threads
 * and verify that no over-deduction occurs under contention.
 *
 * Target: 5,000 to 20,000 concurrent Seamless Wallet requests/sec platform-wide.
 */
@Tag("integration")
class ConcurrentDebitStressIntegrationTest extends BaseIntegrationTest {

    @Test
    void tenConcurrentDebits_samePlayer_shouldNeverOverDeduct() {
        // GIVEN: Player with CASH=100.0000
        //        10 threads, each attempting to debit 20.0000
        //        CyclicBarrier ensures all threads start simultaneously
        // WHEN:  All 10 threads submit debit requests concurrently
        // THEN:  At most 5 succeed (5 * 20 = 100)
        //        Final CASH balance >= 0 (NEVER negative)
        //        Sum of successful debits <= 100.0000
        //        Failed requests return INSUFFICIENT_BALANCE
        //        No partial debits applied
    }

    @Test
    void fiftyConcurrentDebits_samePlayer_smallAmounts_shouldSerializeCorrectly() {
        // GIVEN: Player with CASH=1000.0000
        //        50 threads, each attempting to debit 1.0000
        //        CyclicBarrier for simultaneous start
        // WHEN:  All 50 threads submit concurrently
        // THEN:  All 50 succeed (50 * 1 = 50 <= 1000)
        //        Final CASH balance == 950.0000
        //        All 50 transactions recorded in DB
        //        No duplicates, no gaps in audit trail
    }

    @Test
    void concurrentDebitAndCredit_samePlayer_shouldMaintainConsistency() {
        // GIVEN: Player with CASH=500.0000
        //        Thread group A: 5 debits of 50.0000 each
        //        Thread group B: 5 credits of 50.0000 each
        //        All 10 threads start simultaneously
        // WHEN:  Concurrent execution
        // THEN:  Final CASH balance == 500.0000 (debits and credits cancel out)
        //        All 10 transactions recorded
        //        Order of execution may vary, but final state is deterministic
    }

    @Test
    void concurrentDebits_differentPlayers_shouldNotInterfere() {
        // GIVEN: Player-A with CASH=100.0000
        //        Player-B with CASH=100.0000
        //        5 threads debit Player-A for 30.0000
        //        5 threads debit Player-B for 30.0000
        //        All 10 threads start simultaneously
        // WHEN:  Concurrent execution
        // THEN:  Player-A: at most 3 debits succeed (3 * 30 = 90 <= 100)
        //        Player-B: at most 3 debits succeed (3 * 30 = 90 <= 100)
        //        Locks are per-player, so Player-A contention does not block Player-B
    }

    @Test
    void redisUnavailable_dbGuardShouldPreventOverDeduction() {
        // GIVEN: Player with CASH=100.0000
        //        Redis is stopped/unavailable (Redisson lock cannot be acquired)
        // WHEN:  10 concurrent debit requests of 20.0000 each
        // THEN:  DB atomic conditional update (WHERE balance >= amount) prevents over-deduction
        //        Final CASH balance >= 0
        //        Some or all requests may fail due to lock unavailability
        //        But ZERO over-deductions occur
    }
}
```

---

## 5. Performance & Concurrency Stress Tests

These tests go beyond correctness to measure throughput and latency under sustained load.

---

### 5.1 Throughput Stress Test

**File**: `src/test/java/com/funding/integration/stress/ThroughputStressIntegrationTest.java`

```java
package com.funding.integration.stress;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Throughput stress tests to validate the platform can handle
 * 5,000 to 20,000 concurrent Seamless Wallet requests/sec.
 *
 * NOTE: These tests verify correctness under load in a test environment.
 * Actual throughput numbers will differ from production. The goal is to
 * verify zero data corruption at high concurrency, not to benchmark hardware.
 */
@Tag("integration")
@Tag("stress")
class ThroughputStressIntegrationTest extends BaseIntegrationTest {

    @Test
    void sustainedLoad_oneThousandRequestsPerSecond_shouldMaintainCorrectness() {
        // GIVEN: 100 players, each with CASH=100000.0000
        //        Executor pool with 100 threads
        // WHEN:  Submit 1,000 debit requests/sec for 10 seconds (10,000 total)
        //        Each debit is 1.0000 to a random player
        // THEN:  All requests complete (success or controlled failure)
        //        Sum of all player balances == (100 * 100000) - (count_success * 1)
        //        No unaccounted money loss or gain
        //        p99 latency recorded (not asserted, just measured)
    }

    @Test
    void getBalance_hotPath_shouldCompleteWithinFiftyMs() {
        // GIVEN: Player with cached balance (versioned cache hit expected)
        // WHEN:  100 sequential GetBalance requests
        // THEN:  Average latency < 50ms (SLA from plan)
        //        All return consistent balance
    }

    @Test
    void mixedWorkload_debitsCreditsAndGetBalance_shouldRemainConsistent() {
        // GIVEN: 50 players
        //        Workload mix: 40% GetBalance, 30% Debit, 30% Credit
        // WHEN:  Run for 30 seconds at 200 requests/sec
        // THEN:  Final balances are mathematically consistent
        //        Zero over-deductions
        //        All transactions have audit trail
    }
}
```

---

### 5.2 Idempotency Under Load

**File**: `src/test/java/com/funding/integration/stress/IdempotencyStressIntegrationTest.java`

```java
package com.funding.integration.stress;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies idempotency guarantees hold under concurrent duplicate requests.
 *
 * Real-world scenario: GP retries after timeout, potentially sending the same
 * transactionId multiple times concurrently.
 */
@Tag("integration")
@Tag("stress")
class IdempotencyStressIntegrationTest extends BaseIntegrationTest {

    @Test
    void twentyConcurrentDuplicateRequests_shouldProcessExactlyOnce() {
        // GIVEN: Player with CASH=1000.0000
        //        Single transactionId="TX-STRESS-001", amount=50.0000
        // WHEN:  20 threads submit the same debit request simultaneously
        // THEN:  Exactly one debit applied (CASH=950.0000)
        //        All 20 threads receive the same response
        //        Single transaction row in DB
        //        Single event published to Kafka
    }

    @Test
    void rapidFireDuplicates_shouldAllReturnCachedResponse() {
        // GIVEN: First request succeeds and is cached
        // WHEN:  100 rapid-fire duplicate requests (same transactionId)
        // THEN:  All return cached response
        //        No additional DB writes
        //        Balance unchanged after first debit
    }
}
```

---

### 5.3 Rate Limiting Stress Test

**File**: `src/test/java/com/funding/integration/stress/RateLimitingStressIntegrationTest.java`

```java
package com.funding.integration.stress;

import com.funding.integration.BaseIntegrationTest;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Verifies per-GP rate limiting under burst traffic.
 *
 * Each GP has a configurable rate limit (default: 2,000 req/sec).
 * Exceeding the limit returns HTTP 429 with Retry-After header.
 * This prevents a misbehaving GP from consuming platform capacity.
 */
@Tag("integration")
@Tag("stress")
class RateLimitingStressIntegrationTest extends BaseIntegrationTest {

    @Test
    void gpExceedsRateLimit_shouldReturnHttp429() {
        // GIVEN: GP "GP-BURST" configured with rate limit = 50 req/sec (low for testing)
        // WHEN:  Submit 100 requests in 1 second from GP-BURST
        // THEN:  First ~50 requests succeed
        //        Remaining requests return HTTP 429 with Retry-After header
        //        Rate limit counter resets in next second window
    }

    @Test
    void differentGPs_shouldHaveIndependentRateLimits() {
        // GIVEN: GP-A rate limit = 50/sec, GP-B rate limit = 50/sec
        // WHEN:  GP-A sends 40 requests, GP-B sends 40 requests (same second)
        // THEN:  All 80 requests succeed (each GP within its own limit)
        //        GP limits are independent
    }
}
```

---

## 6. Dependencies & Module References

This section depends on ALL previous sections. Every module must have its interfaces and stubs defined before these tests can compile.

| Dependency | Module | What This Section Uses |
|------------|--------|----------------------|
| Section 01 | foundation | Base entities, Money value object, DB migrations, Testcontainers setup |
| Section 02 | wallet-core | WalletService, BalanceCalculator, DeductionEngine, Wallet entity |
| Section 03 | wallet-transaction | TransactionProcessor, ScenarioHandlers, RoundLifecycleManager |
| Section 04 | seamless-wallet-api | API endpoints, TokenValidator, IdempotencyStore, GPAdapter |
| Section 05 | wallet-reconciliation | RealtimeReconciler, HourlyReconciler, DiscrepancyResolver |
| Section 06 | currency-engine | CurrencyConverter, FXRateManager, BlockchainMonitor, CryptoAmount |
| Section 07 | payment-gateway | SmartRouter, CircuitBreaker, PSPAdapter, RetryStrategy |
| Section 08 | payment-deposit | DepositService, CashierFrictionEvaluator, DepositLimitChecker, ThreeDSHandler |
| Section 09 | payment-withdrawal | WithdrawalService, WageringVerifier, ApprovalWorkflow |
| Section 10 | payment-dispute | ChargebackService (only referenced for full-system invariant checks) |
| Section 11 | shared | DistributedLock, OutboxEvent, GovernanceClient stubs, ConfigReader |

**External service stubs used in these tests** (via Spring profile `stub`):
- `GovernanceClient` stub (Section 11) -- returns configurable tenant/permission data
- `WageringVerifier` stub -- configurable to return COMPLETE / INCOMPLETE / timeout
- `RiskScoreClient` stub -- returns configurable risk scores for friction tier tests
- `BlockchainMonitor` stub -- simulates confirmation count progression
- `PSPAdapter` stubs -- simulate success/failure/timeout callbacks

---

## 7. Implementation Checklist

### Phase 1: Test Infrastructure
- [ ] Create `funding-integration-tests` Gradle module with dependencies on all other modules
- [ ] Implement `BaseIntegrationTest` with Testcontainers (PostgreSQL + Redis + Kafka)
- [ ] Implement `TestDataFactory` with player/wallet/token/request builders
- [ ] Implement `KafkaTestConsumer` with Awaitility-based event assertions
- [ ] Configure Spring profile `integration-test` with governance stubs and external service mocks
- [ ] Verify all containers start and Spring context loads

### Phase 2: Key Invariant Tests (INV-1 through INV-10)
- [ ] INV-1: `Inv1NonNegativeBalanceIntegrationTest` -- non-negative CASH/BONUS balance
- [ ] INV-2: `Inv2UniqueTransactionIdIntegrationTest` -- unique transactionId constraint
- [ ] INV-3: `Inv3DeductionAuditTrailIntegrationTest` -- deduction layer audit trail
- [ ] INV-4: `Inv4PrecisionAccumulationIntegrationTest` -- 10K transaction precision
- [ ] INV-5: `Inv5CreditNotWithdrawableIntegrationTest` -- CREDIT wallet isolation
- [ ] INV-6: `Inv6NegativeBalanceLockIntegrationTest` -- negative balance lock within 5s
- [ ] INV-7: `Inv7WageringDownNeverAutoApproveIntegrationTest` -- wagering-down safety
- [ ] INV-8: `Inv8GpAuthoritativeReconciliationIntegrationTest` -- GP-authoritative recon
- [ ] INV-9: `Inv9CryptoConfirmationThresholdIntegrationTest` -- crypto confirmation threshold
- [ ] INV-10: `Inv10TransientErrorNotCachedIntegrationTest` -- transient error not cached

### Phase 3: Cross-Module Scenario Tests
- [ ] `FullDepositFlowIntegrationTest` -- limit -> friction -> PSP -> 3DS -> callback -> credit
- [ ] `FullWithdrawalFlowIntegrationTest` -- lock -> wagering -> approval -> PSP -> debit
- [ ] `SeamlessWalletRoundIntegrationTest` -- GetBalance -> Debit -> Credit -> round close
- [ ] `ConcurrentDebitStressIntegrationTest` -- 10+ concurrent debits, zero over-deduction

### Phase 4: Performance & Stress Tests
- [ ] `ThroughputStressIntegrationTest` -- sustained load correctness
- [ ] `IdempotencyStressIntegrationTest` -- concurrent duplicate handling
- [ ] `RateLimitingStressIntegrationTest` -- per-GP rate limit enforcement

### Verification Criteria
- [ ] All 10 invariant tests pass with real PostgreSQL + Redis + Kafka (no mocks for infrastructure)
- [ ] All scenario tests exercise the full call chain (API -> service -> repository -> DB)
- [ ] Concurrent stress tests run with CyclicBarrier synchronization (not sequential with sleeps)
- [ ] Stress tests tagged separately (`@Tag("stress")`) for optional CI execution
- [ ] Coverage: all 10 must-hold properties from claude-plan.md section 15 verified
- [ ] No test depends on execution order (each test sets up and tears down its own data)
