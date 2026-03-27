# Section 11 -- Shared Infrastructure

> **Module**: `shared/` (subpackages: `lock/`, `event/`, `config/`, `stub/`, `metrics/`)
> **Depends on**: section-01-foundation (base entities, Money VO, Gradle structure, Testcontainers setup)
> **Blocks**: section-12-integration-tests
> **Parallelizable with**: any other section after 01
> **Plan refs**: claude-plan.md SS12 Shared Infrastructure, SS14 Monitoring & Observability
> **TDD refs**: claude-plan-tdd.md SS12.1, SS12.2, SS12.3

---

## Scope

This section covers the five cross-cutting infrastructure components consumed by every other module in the funding domain:

1. **Distributed Lock Abstraction** -- Redisson-backed distributed locking with watchdog auto-renewal
2. **Transactional Outbox Pattern** -- At-least-once Kafka event delivery via DB-transactional outbox
3. **Governance Domain Stubs** -- Contract interfaces + Spring-profile-activated mock implementations
4. **DB-Configurable Parameter Reader** -- Three-layer override: Global -> Jurisdiction -> Brand (most-specific wins)
5. **Micrometer Metrics Setup** -- Standardized metric names, tags, and Prometheus export

---

## Part A -- Tests FIRST

All test classes follow the project naming convention: `{ClassName}Test.java` for unit tests, `{ClassName}IntegrationTest.java` for integration tests. Tests are written before any implementation code.

### A.1 Distributed Lock Tests (4 tests)

**File**: `shared/src/test/java/com/funding/shared/lock/DistributedLockServiceTest.java`

```java
package com.funding.shared.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock rLock;

    @InjectMocks
    private RedissonDistributedLockService lockService;

    /**
     * Test: Lock acquired within timeout -> action executes.
     *
     * Given a lock key with a 3-second wait timeout,
     * when the lock is acquired within that window,
     * then the supplied action executes and its result is returned.
     */
    @Test
    void lockAcquiredWithinTimeout_actionExecutes() {
        // Arrange: Redisson returns a lock that is immediately acquirable
        // Act: executeWithLock(key, 3s wait, 5s lease, () -> "result")
        // Assert: action's return value is returned; lock.unlock() is called
    }

    /**
     * Test: Lock not acquired within timeout -> fail fast.
     *
     * Given a lock key where another process holds the lock,
     * when tryLock exceeds the 3-second wait timeout,
     * then a LockAcquisitionException is thrown immediately (fail fast).
     * The supplied action is never invoked.
     */
    @Test
    void lockNotAcquiredWithinTimeout_failFast() {
        // Arrange: rLock.tryLock() returns false (timeout exceeded)
        // Act & Assert: assertThatThrownBy -> LockAcquisitionException
        // Assert: action supplier is never called
    }

    /**
     * Test: Lock auto-renewed by watchdog during long operations.
     *
     * Given a lock with a 5-second lease and watchdog enabled,
     * when the action takes longer than the lease time,
     * then the Redisson watchdog automatically extends the lease
     * and the action completes successfully without losing the lock.
     *
     * Implementation note: Redisson's watchdog is enabled when leaseTime
     * is set to -1. The lock is configured to use watchdog mode for
     * operations that may exceed the default lease.
     */
    @Test
    void lockAutoRenewedByWatchdog_duringLongOperations() {
        // Arrange: configure lock with watchdog (leaseTime = -1)
        // Act: execute action that simulates a long-running operation
        // Assert: action completes; lock was held throughout
    }

    /**
     * Test: Redis unavailable -> DB atomic update still prevents over-deduction.
     *
     * Given Redis is unavailable (RedissonClient throws exception),
     * when a wallet operation is attempted,
     * then the lock acquisition fails with RedisUnavailableException,
     * but the caller (TransactionProcessor) falls through to the DB
     * atomic conditional update (UPDATE ... WHERE balance >= amount)
     * which serves as the sole safety net.
     *
     * Note: There is NO advisory lock fallback. The DB guard is sufficient
     * to prevent over-deduction. This test verifies the lock service
     * propagates the failure cleanly so the caller can decide.
     */
    @Test
    void redisUnavailable_dbAtomicUpdatePreventsOverDeduction() {
        // Arrange: redissonClient.getLock() throws RedisConnectionException
        // Act & Assert: assertThatThrownBy -> RedisUnavailableException
        // Note: actual DB guard is tested in wallet-transaction (section-03)
    }
}
```

### A.2 Kafka Outbox Tests (5 tests)

**File**: `shared/src/test/java/com/funding/shared/event/OutboxEventServiceTest.java` (unit)
**File**: `shared/src/test/java/com/funding/shared/event/OutboxRelayIntegrationTest.java` (integration)

```java
package com.funding.shared.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @InjectMocks
    private OutboxEventService outboxEventService;

    /**
     * Test: Event written to outbox in same DB transaction as business operation.
     *
     * Given a business operation wrapped in @Transactional,
     * when the operation completes and an event is saved to the outbox,
     * then both the business state change and the outbox record
     * are committed atomically in the same DB transaction.
     *
     * If the transaction rolls back, neither the business change
     * nor the outbox event persists.
     */
    @Test
    void eventWrittenToOutbox_inSameDbTransaction() {
        // Arrange: create an OutboxEvent with topic, key, payload, status=PENDING
        // Act: outboxEventService.saveEvent(event) within transactional context
        // Assert: outboxEventRepository.save() called with status PENDING
        // Assert: event's transactionId matches the business operation
    }

    /**
     * Test: Outbox relay publishes events to correct Kafka topic.
     *
     * Given PENDING events in the outbox table with different topic values,
     * when the outbox relay polls and processes them,
     * then each event is published to the Kafka topic specified in the
     * event's "topic" field (e.g., funding.wallet.debit, funding.payment.deposit).
     */
    @Test
    void outboxRelay_publishesToCorrectKafkaTopic() {
        // Arrange: two OutboxEvent records, one for "funding.wallet.debit",
        //          one for "funding.payment.deposit"
        // Act: relay.processOutbox()
        // Assert: kafkaTemplate.send() called with matching topics
    }

    /**
     * Test: Published events marked as PUBLISHED in outbox.
     *
     * Given a PENDING outbox event that is successfully published to Kafka,
     * when the relay confirms successful delivery,
     * then the event's status is updated from PENDING to PUBLISHED
     * and the publishedAt timestamp is set.
     */
    @Test
    void publishedEvents_markedAsPublished() {
        // Arrange: PENDING OutboxEvent in repository
        // Act: relay processes and Kafka confirms delivery
        // Assert: event.getStatus() == OutboxEventStatus.PUBLISHED
        // Assert: event.getPublishedAt() is not null
    }

    /**
     * Test: Failed publish -> retry with exponential backoff.
     *
     * Given a PENDING outbox event where Kafka publish fails,
     * when the relay encounters the failure,
     * then the event's retryCount is incremented,
     * nextRetryAt is set using exponential backoff (base * 2^retryCount),
     * and the event remains in PENDING status for future polling.
     */
    @Test
    void failedPublish_retryWithExponentialBackoff() {
        // Arrange: OutboxEvent with retryCount=0; kafkaTemplate.send() throws
        // Act: relay.processOutbox()
        // Assert: event.getRetryCount() == 1
        // Assert: event.getNextRetryAt() == now + baseDelay * 2^1
        // Assert: event.getStatus() == PENDING
    }

    /**
     * Test: Max retries exhausted -> event moves to DLQ.
     *
     * Given a PENDING outbox event that has reached the maximum retry count,
     * when the relay attempts to publish and it fails again,
     * then the event's status is changed to DEAD_LETTERED,
     * a DLQ record is created, and an alert metric is incremented.
     */
    @Test
    void maxRetriesExhausted_eventMovesToDlq() {
        // Arrange: OutboxEvent with retryCount == maxRetries; publish fails
        // Act: relay.processOutbox()
        // Assert: event.getStatus() == OutboxEventStatus.DEAD_LETTERED
        // Assert: dlqRepository.save() called
    }
}
```

**Integration test** (uses Testcontainers for PostgreSQL + Kafka):

```java
package com.funding.shared.event;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class OutboxRelayIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static KafkaContainer kafka = new KafkaContainer(/* Confluent image */);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    /**
     * End-to-end: save event in transaction, relay picks it up,
     * event arrives on Kafka topic, status becomes PUBLISHED.
     */
    @Test
    void endToEnd_outboxEventDeliveredToKafka() {
        // Arrange: insert business record + outbox event in one transaction
        // Act: trigger relay polling
        // Assert: Kafka consumer receives the event on the correct topic
        // Assert: outbox record status == PUBLISHED
    }
}
```

### A.3 Governance Stubs Tests (2 tests)

**File**: `shared/src/test/java/com/funding/shared/stub/GovernanceClientStubTest.java`

```java
package com.funding.shared.stub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

class GovernanceClientStubTest {

    /**
     * Test: Stub returns configurable test data.
     *
     * Given the GovernanceClientStub is instantiated,
     * when resolveTenant(), hasPermission(), and getAgentCreditQuota() are called,
     * then each returns pre-configured test data that can be customized
     * per test scenario (e.g., different tenant configs, permission grants).
     */
    @Test
    void stubReturnsConfigurableTestData() {
        // Arrange: create GovernanceClientStub with custom test fixtures
        // Act: call resolveTenant("tenant-001")
        // Assert: returns TenantContext with expected test values
        // Act: call hasPermission("user-001", "funding.withdraw")
        // Assert: returns true (default stub behavior)
        // Act: call getAgentCreditQuota("agent-001")
        // Assert: returns CreditQuota with expected test limits
    }

    /**
     * Test: Stub profile ("stub") activates via Spring profile.
     *
     * Given the Spring application context with profile "stub" active,
     * when the GovernanceClient bean is resolved,
     * then it is an instance of GovernanceClientStub (not the real HTTP client).
     *
     * Given the Spring application context WITHOUT profile "stub",
     * when the GovernanceClient bean is resolved,
     * then it is an instance of GovernanceHttpClient (the real client).
     */
    @Test
    void stubProfileActivatesViaSpringProfile() {
        // This is best tested with two nested @SpringBootTest configurations:
        // Config 1: @ActiveProfiles("stub") -> bean instanceof GovernanceClientStub
        // Config 2: @ActiveProfiles("production") -> bean instanceof GovernanceHttpClient
    }
}

// Separate Spring integration test for profile activation:
@SpringBootTest
@ActiveProfiles("stub")
class GovernanceClientStubProfileIntegrationTest {

    @Autowired
    private GovernanceClient governanceClient;

    @Test
    void whenStubProfileActive_stubBeanInjected() {
        assertThat(governanceClient).isInstanceOf(GovernanceClientStub.class);
    }
}
```

### A.4 DB-Configurable Parameter Reader Tests (additional coverage)

These tests are not explicitly enumerated in the TDD plan SS12 but are implied by the section scope.

**File**: `shared/src/test/java/com/funding/shared/config/ConfigurableParameterReaderTest.java`

```java
package com.funding.shared.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ConfigurableParameterReaderTest {

    /**
     * Test: Three-layer override resolution -- most-specific wins.
     *
     * Given parameter "funding.wallet.idempotency_ttl_hours":
     *   - Global config: 24
     *   - Jurisdiction config (MGA): 48
     *   - Brand config (BrandX under MGA): 12
     *
     * When resolving for BrandX under MGA,
     * Then returns 12 (Brand overrides Jurisdiction overrides Global).
     *
     * When resolving for a different brand under MGA (no brand override),
     * Then returns 48 (Jurisdiction override).
     *
     * When resolving for a jurisdiction with no overrides,
     * Then returns 24 (Global fallback).
     */
    @Test
    void threeLayerOverride_mostSpecificWins() {
        // Arrange: set up parameter repository with all three layers
        // Act & Assert: verify resolution at each specificity level
    }

    /**
     * Test: Domain-meaningful key prefixes used consistently.
     *
     * Given parameter keys follow the convention:
     *   funding.wallet.* for wallet domain parameters
     *   funding.payment.* for payment domain parameters
     *
     * When querying a key that does not exist at any layer,
     * Then throws ParameterNotFoundException (no silent null returns).
     */
    @Test
    void domainPrefixKeys_notFoundThrows() {
        // Arrange: empty parameter repository
        // Act & Assert: resolveParameter("funding.wallet.nonexistent") throws
    }

    /**
     * Test: Parameter values are cached and cache is invalidated on update.
     */
    @Test
    void parameterValues_cachedAndInvalidatedOnUpdate() {
        // Arrange: set global value, read (populates cache)
        // Act: update the value in DB
        // Assert: after cache invalidation, new value is returned
    }
}
```

### A.5 Micrometer Metrics Setup Tests (additional coverage)

**File**: `shared/src/test/java/com/funding/shared/metrics/FundingMetricsRegistryTest.java`

```java
package com.funding.shared.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FundingMetricsRegistryTest {

    /**
     * Test: Standard metric names registered with expected tags.
     *
     * Given the FundingMetricsRegistry is initialized with a MeterRegistry,
     * when wallet and payment metrics are recorded,
     * then metric names follow the conventions:
     *   wallet.transaction.duration (histogram, tags: scenarioType, tenantId)
     *   wallet.lock.contention (counter, tags: lockKey)
     *   payment.deposit.success_rate (counter, tags: pspId, tenantId)
     *   payment.psp.circuit_breaker.state (gauge, tags: pspId)
     */
    @Test
    void standardMetricNames_registeredWithTags() {
        // Arrange: SimpleMeterRegistry
        // Act: FundingMetricsRegistry.recordTransactionDuration(...)
        // Assert: meter with name "wallet.transaction.duration" exists
        // Assert: tag "scenarioType" is present
    }

    /**
     * Test: Prometheus-compatible metric export.
     *
     * Given metrics are recorded via Micrometer,
     * when the Prometheus scrape endpoint is queried,
     * then metrics are exported in Prometheus text format
     * with correct naming (dots converted to underscores).
     */
    @Test
    void prometheusCompatible_metricsExported() {
        // Arrange: PrometheusMeterRegistry
        // Act: record a metric
        // Assert: registry.scrape() contains expected metric name
    }
}
```

---

## Part B -- Implementation Details

### B.1 Distributed Lock Abstraction (Redisson)

**Purpose**: Serialize concurrent access to player wallets at the application level. The DB `UPDATE ... WHERE balance >= amount` guard is the ultimate safety net; the distributed lock is a performance optimization that prevents unnecessary DB contention.

#### Interface

**File**: `shared/src/main/java/com/funding/shared/lock/DistributedLock.java`

```java
package com.funding.shared.lock;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Abstraction for distributed locking. Primary implementation uses Redisson.
 * Callers must handle RedisUnavailableException by falling through to
 * DB-level atomic guards.
 */
public interface DistributedLock {

    /**
     * Execute the given action while holding a distributed lock.
     *
     * @param lockKey   unique key identifying the locked resource (e.g., "wallet:{playerId}")
     * @param waitTime  maximum time to wait for lock acquisition (default: 3s)
     * @param leaseTime lock auto-release time; use Duration.ofSeconds(-1) for watchdog mode
     * @param action    the business logic to execute while holding the lock
     * @return the action's result
     * @throws LockAcquisitionException if lock cannot be acquired within waitTime
     * @throws RedisUnavailableException if Redis connection fails
     */
    <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action);
}
```

#### Implementation

**File**: `shared/src/main/java/com/funding/shared/lock/RedissonDistributedLockService.java`

```java
package com.funding.shared.lock;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Service
public class RedissonDistributedLockService implements DistributedLock {

    private static final Duration DEFAULT_WAIT_TIME = Duration.ofSeconds(3);
    private static final Duration DEFAULT_LEASE_TIME = Duration.ofSeconds(5);
    // leaseTime = -1 enables Redisson watchdog (auto-renewal every 10s of 30s lease)

    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;

    // Constructor injection

    @Override
    public <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime,
                                  Supplier<T> action) {
        // 1. Obtain RLock from RedissonClient
        //    RLock lock = redissonClient.getLock(lockKey);
        //
        // 2. Try to acquire lock within waitTime
        //    boolean acquired = lock.tryLock(waitTime.toMillis(), leaseTime.toMillis(), TimeUnit.MILLISECONDS);
        //
        // 3. If not acquired: throw LockAcquisitionException (fail fast)
        //
        // 4. If acquired: execute action in try-finally block
        //    try { return action.get(); } finally { lock.unlock(); }
        //
        // 5. Catch RedisConnectionException -> wrap as RedisUnavailableException
        //
        // 6. Record metrics:
        //    - wallet.lock.acquisition_time (timer)
        //    - wallet.lock.contention (counter, incremented on timeout)
        //    - wallet.lock.redis_unavailable (counter, incremented on connection failure)
        throw new UnsupportedOperationException("Stub -- implement");
    }
}
```

#### Exception Types

**File**: `shared/src/main/java/com/funding/shared/lock/LockAcquisitionException.java`

```java
package com.funding.shared.lock;

public class LockAcquisitionException extends RuntimeException {
    private final String lockKey;
    private final long waitTimeMs;

    public LockAcquisitionException(String lockKey, long waitTimeMs) {
        super("Failed to acquire lock '%s' within %d ms".formatted(lockKey, waitTimeMs));
        this.lockKey = lockKey;
        this.waitTimeMs = waitTimeMs;
    }

    // Getters
}
```

**File**: `shared/src/main/java/com/funding/shared/lock/RedisUnavailableException.java`

```java
package com.funding.shared.lock;

public class RedisUnavailableException extends RuntimeException {
    public RedisUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### Key Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| No advisory lock fallback | DB atomic `WHERE balance >= amount` is the sole fallback | Advisory locks add complexity without clear benefit; the DB guard already prevents over-deduction |
| Watchdog mode for long ops | leaseTime = -1 activates Redisson watchdog | Prevents lock expiry during legitimate long-running operations (e.g., multi-wallet deductions) |
| Fail fast on timeout | 3-second default wait | Under high contention, queuing requests degrades throughput; better to reject and let client retry |
| Metric instrumentation | Lock acquisition time, contention rate, Redis availability | Critical for capacity planning and incident detection |

---

### B.2 Transactional Outbox Pattern for Kafka

**Purpose**: Guarantee at-least-once event delivery to Kafka without coupling DB transaction commit to Kafka broker availability. Events are written to an outbox table inside the same DB transaction as the business operation. A separate relay process polls the outbox and publishes to Kafka.

#### Outbox Entity

**File**: `shared/src/main/java/com/funding/shared/event/OutboxEvent.java`

```java
package com.funding.shared.event;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_next_retry", columnList = "status, nextRetryAt")
})
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String topic;           // e.g., "funding.wallet.debit"

    @Column(nullable = false)
    private String aggregateKey;    // Kafka partition key (e.g., playerId)

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;         // Serialized event JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status; // PENDING, PUBLISHED, DEAD_LETTERED

    @Column(nullable = false)
    private int retryCount;

    @Column
    private Instant nextRetryAt;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant publishedAt;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String transactionId;   // Correlation with business operation

    // No-arg constructor (JPA), all-args constructor, getters, setters
}
```

#### Outbox Status Enum

**File**: `shared/src/main/java/com/funding/shared/event/OutboxEventStatus.java`

```java
package com.funding.shared.event;

public enum OutboxEventStatus {
    PENDING,
    PUBLISHED,
    DEAD_LETTERED
}
```

#### Outbox Repository

**File**: `shared/src/main/java/com/funding/shared/event/OutboxEventRepository.java`

```java
package com.funding.shared.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' " +
           "AND (e.nextRetryAt IS NULL OR e.nextRetryAt <= :now) " +
           "ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(Instant now, org.springframework.data.domain.Pageable pageable);
}
```

#### Outbox Event Service (used inside business transactions)

**File**: `shared/src/main/java/com/funding/shared/event/OutboxEventService.java`

```java
package com.funding.shared.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    // Constructor injection

    /**
     * Save an event to the outbox table. MUST be called within an existing
     * @Transactional context so that the event is committed atomically
     * with the business operation.
     *
     * Usage inside a transactional service method:
     *   walletRepository.save(wallet);
     *   outboxEventService.saveEvent(new OutboxEvent("funding.wallet.debit", playerId, payload));
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveEvent(OutboxEvent event) {
        // event.setStatus(OutboxEventStatus.PENDING);
        // event.setCreatedAt(Instant.now());
        // event.setRetryCount(0);
        // return outboxEventRepository.save(event);
        throw new UnsupportedOperationException("Stub -- implement");
    }
}
```

#### Outbox Relay (polling scheduler)

**File**: `shared/src/main/java/com/funding/shared/event/OutboxRelay.java`

```java
package com.funding.shared.event;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class OutboxRelay {

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRIES = 5;
    private static final Duration BASE_BACKOFF = Duration.ofSeconds(2);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    // Constructor injection

    /**
     * Polls the outbox table for PENDING events and publishes to Kafka.
     * Runs every 500ms (configurable).
     *
     * For each event:
     * 1. Send to Kafka topic specified in event.topic with key event.aggregateKey
     * 2. On success: mark PUBLISHED, set publishedAt
     * 3. On failure:
     *    a. If retryCount < MAX_RETRIES: increment retryCount, set nextRetryAt
     *       using exponential backoff (BASE_BACKOFF * 2^retryCount)
     *    b. If retryCount >= MAX_RETRIES: mark DEAD_LETTERED, save to DLQ table
     *
     * Alternative: Debezium CDC can replace polling for lower latency.
     */
    @Scheduled(fixedDelayString = "${funding.outbox.relay.poll-interval-ms:500}")
    public void processOutbox() {
        // var events = outboxEventRepository.findPendingEvents(Instant.now(), PageRequest.of(0, BATCH_SIZE));
        // for (var event : events) { publishEvent(event); }
        throw new UnsupportedOperationException("Stub -- implement");
    }
}
```

#### Event Topics Reference

All events include common fields: `tenantId`, `playerId`, `timestamp`, `transactionId`, `amount`, `currency`, plus event-specific fields.

| Topic | Event Class | Consumers |
|-------|-------------|-----------|
| `funding.wallet.debit` | WalletDebitEvent | risk, data |
| `funding.wallet.credit` | WalletCreditEvent | risk, data |
| `funding.wallet.rollback` | WalletRollbackEvent | risk, data |
| `funding.wallet.negative-balance` | NegativeBalanceAlert | risk (priority) |
| `funding.payment.deposit` | DepositCompletedEvent | player, data |
| `funding.payment.withdrawal` | WithdrawalCompletedEvent | player, data |
| `funding.payment.chargeback` | ChargebackEvent | risk, data |
| `funding.psp.circuit-breaker` | PSPStateChangeEvent | data (monitoring) |

---

### B.3 Governance Domain Stubs

**Purpose**: Enable independent development and testing of the funding domain without a running Governance domain instance. A contract interface defines the API boundary; a stub implementation provides configurable test data; the real HTTP client replaces it at integration time via Spring profile switching.

#### Contract Interface

**File**: `shared/src/main/java/com/funding/shared/stub/GovernanceClient.java`

```java
package com.funding.shared.stub;

/**
 * Contract interface for the Governance domain.
 * Two implementations:
 *   - GovernanceClientStub (@Profile("stub")) -- returns test data
 *   - GovernanceHttpClient (@Profile("!stub")) -- real HTTP calls
 */
public interface GovernanceClient {

    /**
     * Resolve tenant configuration (multi-tenancy context, jurisdiction, brand).
     */
    TenantContext resolveTenant(String tenantId);

    /**
     * Check if user has a specific permission (RBAC).
     */
    boolean hasPermission(String userId, String permission);

    /**
     * Get agent credit quota for credit wallet operations.
     */
    CreditQuota getAgentCreditQuota(String agentId);
}
```

#### DTOs

**File**: `shared/src/main/java/com/funding/shared/stub/TenantContext.java`

```java
package com.funding.shared.stub;

public record TenantContext(
    String tenantId,
    String jurisdiction,   // e.g., "MGA", "UKGC", "CURACAO"
    String brand,
    String settlementCurrency,
    boolean kycRequired
) {}
```

**File**: `shared/src/main/java/com/funding/shared/stub/CreditQuota.java`

```java
package com.funding.shared.stub;

import java.math.BigDecimal;

public record CreditQuota(
    String agentId,
    BigDecimal creditLimit,
    BigDecimal usedAmount,
    BigDecimal availableQuota
) {}
```

#### Stub Implementation

**File**: `shared/src/main/java/com/funding/shared/stub/GovernanceClientStub.java`

```java
package com.funding.shared.stub;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("stub")
public class GovernanceClientStub implements GovernanceClient {

    // Configurable test fixtures -- tests can inject custom values
    private final Map<String, TenantContext> tenantFixtures = new ConcurrentHashMap<>();
    private final Map<String, Boolean> permissionFixtures = new ConcurrentHashMap<>();
    private final Map<String, CreditQuota> quotaFixtures = new ConcurrentHashMap<>();

    // Default test data
    private static final TenantContext DEFAULT_TENANT = new TenantContext(
        "default-tenant", "MGA", "default-brand", "USD", true
    );

    @Override
    public TenantContext resolveTenant(String tenantId) {
        return tenantFixtures.getOrDefault(tenantId, DEFAULT_TENANT);
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        return permissionFixtures.getOrDefault(userId + ":" + permission, true);
    }

    @Override
    public CreditQuota getAgentCreditQuota(String agentId) {
        return quotaFixtures.getOrDefault(agentId, new CreditQuota(
            agentId, new BigDecimal("10000"), BigDecimal.ZERO, new BigDecimal("10000")
        ));
    }

    // Methods for test fixture configuration
    public void setTenantFixture(String tenantId, TenantContext context) {
        tenantFixtures.put(tenantId, context);
    }

    public void setPermissionFixture(String userId, String permission, boolean granted) {
        permissionFixtures.put(userId + ":" + permission, granted);
    }

    public void setQuotaFixture(String agentId, CreditQuota quota) {
        quotaFixtures.put(agentId, quota);
    }

    public void resetFixtures() {
        tenantFixtures.clear();
        permissionFixtures.clear();
        quotaFixtures.clear();
    }
}
```

#### Real Client Placeholder

**File**: `shared/src/main/java/com/funding/shared/stub/GovernanceHttpClient.java`

```java
package com.funding.shared.stub;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("!stub")
public class GovernanceHttpClient implements GovernanceClient {

    private final RestClient restClient;

    // Constructor: RestClient configured with governance domain base URL

    @Override
    public TenantContext resolveTenant(String tenantId) {
        // GET /api/governance/tenants/{tenantId}
        throw new UnsupportedOperationException("Stub -- implement at integration time");
    }

    @Override
    public boolean hasPermission(String userId, String permission) {
        // GET /api/governance/permissions/check?userId={}&permission={}
        throw new UnsupportedOperationException("Stub -- implement at integration time");
    }

    @Override
    public CreditQuota getAgentCreditQuota(String agentId) {
        // GET /api/governance/agents/{agentId}/credit-quota
        throw new UnsupportedOperationException("Stub -- implement at integration time");
    }
}
```

---

### B.4 DB-Configurable Parameter Reader

**Purpose**: Externalize all magic numbers, thresholds, rates, and limits into a database-backed configuration system with three-layer override resolution: **Global** (fallback) -> **Jurisdiction** (e.g., MGA, UKGC) -> **Brand** (most specific, wins).

#### Entity

**File**: `shared/src/main/java/com/funding/shared/config/ConfigurableParameter.java`

```java
package com.funding.shared.config;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "configurable_parameters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"paramKey", "scope", "scopeId"})
})
public class ConfigurableParameter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String paramKey;        // e.g., "funding.wallet.idempotency_ttl_hours"

    @Column(nullable = false)
    private String paramValue;      // stored as String, caller parses to target type

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParameterScope scope;   // GLOBAL, JURISDICTION, BRAND

    @Column
    private String scopeId;         // null for GLOBAL, jurisdiction code for JURISDICTION, brandId for BRAND

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private String updatedBy;

    // No-arg constructor, all-args constructor, getters, setters
}
```

#### Scope Enum

**File**: `shared/src/main/java/com/funding/shared/config/ParameterScope.java`

```java
package com.funding.shared.config;

public enum ParameterScope {
    GLOBAL,         // Lowest priority -- system-wide default
    JURISDICTION,   // Middle priority -- per-jurisdiction override
    BRAND           // Highest priority -- per-brand override
}
```

#### Reader Service

**File**: `shared/src/main/java/com/funding/shared/config/ConfigurableParameterReader.java`

```java
package com.funding.shared.config;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ConfigurableParameterReader {

    private final ConfigurableParameterRepository repository;

    // Constructor injection

    /**
     * Resolve a parameter value using three-layer override:
     *   1. BRAND (scopeId = brandId) -- highest priority
     *   2. JURISDICTION (scopeId = jurisdictionCode) -- middle priority
     *   3. GLOBAL (scopeId = null) -- fallback
     *
     * @param key            parameter key, e.g., "funding.wallet.idempotency_ttl_hours"
     * @param jurisdiction   jurisdiction code, e.g., "MGA"
     * @param brandId        brand identifier
     * @return resolved parameter value as String
     * @throws ParameterNotFoundException if key does not exist at any layer
     */
    @Cacheable(value = "configParams", key = "#key + ':' + #jurisdiction + ':' + #brandId")
    public String resolveParameter(String key, String jurisdiction, String brandId) {
        // 1. Try BRAND scope
        // Optional<ConfigurableParameter> brandParam =
        //     repository.findByParamKeyAndScopeAndScopeId(key, ParameterScope.BRAND, brandId);
        // if (brandParam.isPresent()) return brandParam.get().getParamValue();
        //
        // 2. Try JURISDICTION scope
        // 3. Try GLOBAL scope
        // 4. Throw ParameterNotFoundException
        throw new UnsupportedOperationException("Stub -- implement");
    }

    /** Convenience: resolve as int */
    public int resolveAsInt(String key, String jurisdiction, String brandId) {
        return Integer.parseInt(resolveParameter(key, jurisdiction, brandId));
    }

    /** Convenience: resolve as BigDecimal */
    public BigDecimal resolveAsBigDecimal(String key, String jurisdiction, String brandId) {
        return new BigDecimal(resolveParameter(key, jurisdiction, brandId));
    }

    /** Convenience: resolve with global-only fallback (no jurisdiction/brand context) */
    public String resolveGlobal(String key) {
        return resolveParameter(key, null, null);
    }

    @CacheEvict(value = "configParams", allEntries = true)
    public void evictCache() {
        // Called after parameter updates via admin API
    }
}
```

#### Repository

**File**: `shared/src/main/java/com/funding/shared/config/ConfigurableParameterRepository.java`

```java
package com.funding.shared.config;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConfigurableParameterRepository extends JpaRepository<ConfigurableParameter, Long> {

    Optional<ConfigurableParameter> findByParamKeyAndScopeAndScopeId(
        String paramKey, ParameterScope scope, String scopeId);
}
```

#### Domain Parameter Keys Reference

All keys use domain-meaningful prefixes (not chapter numbers):

| Key | Default | Description |
|-----|---------|-------------|
| `funding.wallet.fx.fiat_absorption_pct` | 0.5 | FX risk: platform absorbs fiat variance below this % |
| `funding.wallet.fx.crypto_absorption_pct` | 2.0 | FX risk: platform absorbs crypto variance below this % |
| `funding.wallet.idempotency_ttl_hours` | 24 | Idempotency key TTL |
| `funding.wallet.parking_ttl_minutes` | 30 | Out-of-order Win parking expiry |
| `funding.wallet.orphan_scan_interval_minutes` | 15 | Orphan round scan frequency |
| `funding.wallet.orphan_timeout_hours` | 2 | Orphan round timeout threshold |
| `funding.payment.psp.circuit_breaker_failure_threshold` | 5 | CB: consecutive failures to trip |
| `funding.payment.psp.circuit_breaker_cooldown_seconds` | 60 | CB: cooldown before half-open |
| `funding.payment.psp.routing_weight_success` | 0.5 | Router: success rate weight |
| `funding.payment.psp.routing_weight_fee` | 0.3 | Router: fee weight |
| `funding.payment.psp.routing_weight_speed` | 0.15 | Router: speed weight |
| `funding.payment.psp.routing_weight_vip` | 0.05 | Router: VIP bonus weight |
| `funding.payment.withdrawal.approval_tier_1` | 100 | Withdrawal: tier 1 threshold |
| `funding.payment.withdrawal.approval_tier_2` | 1000 | Withdrawal: tier 2 threshold |
| `funding.payment.withdrawal.approval_tier_3` | 10000 | Withdrawal: tier 3 threshold |
| `funding.wallet.gp_rate_limit_per_sec` | 2000 | Per-GP API rate limit |
| `funding.payment.recon.max_reversals_per_gp_per_hour` | 10 | Reconciliation: reversal circuit breaker |

---

### B.5 Micrometer Metrics Setup

**Purpose**: Standardized metric registration and recording across all funding domain modules. All metrics are Prometheus-compatible and follow consistent naming and tagging conventions.

#### Metrics Registry

**File**: `shared/src/main/java/com/funding/shared/metrics/FundingMetricsRegistry.java`

```java
package com.funding.shared.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class FundingMetricsRegistry {

    private final MeterRegistry registry;

    public FundingMetricsRegistry(MeterRegistry registry) {
        this.registry = registry;
    }

    // --- Wallet Metrics ---

    /** Record wallet transaction processing duration */
    public void recordTransactionDuration(String scenarioType, String tenantId, Duration duration) {
        Timer.builder("wallet.transaction.duration")
            .tag("scenarioType", scenarioType)
            .tag("tenantId", tenantId)
            .register(registry)
            .record(duration);
    }

    /** Increment wallet transaction success/failure counter */
    public void recordTransactionResult(String scenarioType, boolean success) {
        Counter.builder("wallet.transaction.success_rate")
            .tag("scenarioType", scenarioType)
            .tag("result", success ? "success" : "failure")
            .register(registry)
            .increment();
    }

    /** Record lock contention event (timeout waiting for lock) */
    public void recordLockContention(String lockKey) {
        Counter.builder("wallet.lock.contention")
            .tag("lockKey", lockKey)
            .register(registry)
            .increment();
    }

    /** Record lock acquisition time */
    public void recordLockAcquisitionTime(String lockKey, Duration duration) {
        Timer.builder("wallet.lock.acquisition_time")
            .tag("lockKey", lockKey)
            .register(registry)
            .record(duration);
    }

    /** Set current orphan round count (gauge) */
    public void setOrphanRoundCount(double count) {
        // Use a gauge backed by an AtomicDouble or similar
        // Gauge.builder("wallet.orphan_rounds.count", () -> count).register(registry);
    }

    /** Set negative balance total (gauge) */
    public void setNegativeBalanceTotal(double total) {
        // Gauge.builder("wallet.negative_balance.total", () -> total).register(registry);
    }

    // --- Payment Metrics ---

    /** Record deposit result per PSP */
    public void recordDepositResult(String pspId, String tenantId, boolean success) {
        Counter.builder("payment.deposit.success_rate")
            .tag("pspId", pspId)
            .tag("tenantId", tenantId)
            .tag("result", success ? "success" : "failure")
            .register(registry)
            .increment();
    }

    /** Set PSP circuit breaker state (0=closed, 1=half-open, 2=open) */
    public void setPspCircuitBreakerState(String pspId, int state) {
        // Gauge: payment.psp.circuit_breaker.state
    }

    /** Record withdrawal approval SLA time */
    public void recordWithdrawalApprovalSla(Duration duration) {
        Timer.builder("payment.withdrawal.approval_sla")
            .register(registry)
            .record(duration);
    }

    /** Record chargeback event for rolling rate calculation */
    public void recordChargeback(String tenantId) {
        Counter.builder("payment.chargeback.rate")
            .tag("tenantId", tenantId)
            .register(registry)
            .increment();
    }

    // --- Outbox Metrics ---

    /** Record outbox relay publish result */
    public void recordOutboxPublish(String topic, boolean success) {
        Counter.builder("outbox.publish.result")
            .tag("topic", topic)
            .tag("result", success ? "success" : "failure")
            .register(registry)
            .increment();
    }

    /** Record outbox DLQ event */
    public void recordOutboxDlq(String topic) {
        Counter.builder("outbox.dlq.count")
            .tag("topic", topic)
            .register(registry)
            .increment();
    }

    // --- Redis Metrics ---

    /** Record Redis unavailability event */
    public void recordRedisUnavailable() {
        Counter.builder("wallet.lock.redis_unavailable")
            .register(registry)
            .increment();
    }
}
```

#### Spring Boot Configuration

**File**: `shared/src/main/resources/application-metrics.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, info, metrics
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: funding-domain
    distribution:
      percentiles-histogram:
        wallet.transaction.duration: true
        wallet.lock.acquisition_time: true
        payment.withdrawal.approval_sla: true
      slo:
        wallet.transaction.duration: 10ms, 50ms, 100ms, 500ms, 1s
```

#### Alert Rules Reference (for ops team)

| Alert | Condition | Severity |
|-------|-----------|----------|
| Over-deduction detected | Any transaction results in unintended negative balance | P0 -- immediate |
| Fund loss detected | Reconciliation shows unaccounted money | P0 -- immediate |
| All PSPs unavailable | Every circuit breaker is OPEN | P0 -- immediate |
| Negative balance exceeds threshold | Platform total < -$10,000 | P1 -- 15min |
| Chargeback rate red | >= 1.0% rolling | P1 -- notify CFO |
| Orphan rounds accumulating | > 50 unresolved orphans | P2 -- 1h |
| Reconciliation match dropping | Daily match < 99.5% | P2 -- 4h |

---

## Part C -- File Path Summary

### Source Files

```
shared/
├── src/main/java/com/funding/shared/
│   ├── lock/
│   │   ├── DistributedLock.java                    (interface)
│   │   ├── RedissonDistributedLockService.java     (implementation)
│   │   ├── LockAcquisitionException.java           (exception)
│   │   └── RedisUnavailableException.java          (exception)
│   ├── event/
│   │   ├── OutboxEvent.java                        (JPA entity)
│   │   ├── OutboxEventStatus.java                  (enum)
│   │   ├── OutboxEventRepository.java              (Spring Data)
│   │   ├── OutboxEventService.java                 (transactional save)
│   │   └── OutboxRelay.java                        (polling publisher)
│   ├── stub/
│   │   ├── GovernanceClient.java                   (contract interface)
│   │   ├── GovernanceClientStub.java               (@Profile("stub"))
│   │   ├── GovernanceHttpClient.java               (@Profile("!stub"))
│   │   ├── TenantContext.java                      (record DTO)
│   │   └── CreditQuota.java                        (record DTO)
│   ├── config/
│   │   ├── ConfigurableParameter.java              (JPA entity)
│   │   ├── ParameterScope.java                     (enum)
│   │   ├── ConfigurableParameterRepository.java    (Spring Data)
│   │   └── ConfigurableParameterReader.java        (3-layer resolution)
│   └── metrics/
│       └── FundingMetricsRegistry.java             (metric definitions)
├── src/main/resources/
│   └── application-metrics.yml                     (Prometheus config)
└── src/test/java/com/funding/shared/
    ├── lock/
    │   └── DistributedLockServiceTest.java          (4 tests)
    ├── event/
    │   ├── OutboxEventServiceTest.java              (5 unit tests)
    │   └── OutboxRelayIntegrationTest.java          (1 integration test)
    ├── stub/
    │   └── GovernanceClientStubTest.java            (2 tests + profile test)
    ├── config/
    │   └── ConfigurableParameterReaderTest.java     (3 tests)
    └── metrics/
        └── FundingMetricsRegistryTest.java          (2 tests)
```

### Database Migration

**File**: `shared/src/main/resources/db/migration/V11_001__outbox_events.sql`

```sql
CREATE TABLE outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic           VARCHAR(255) NOT NULL,
    aggregate_key   VARCHAR(255) NOT NULL,
    payload         JSONB NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count     INT NOT NULL DEFAULT 0,
    next_retry_at   TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    published_at    TIMESTAMP WITH TIME ZONE,
    tenant_id       VARCHAR(100) NOT NULL,
    transaction_id  VARCHAR(255) NOT NULL
);

CREATE INDEX idx_outbox_status_next_retry ON outbox_events (status, next_retry_at)
    WHERE status = 'PENDING';
```

**File**: `shared/src/main/resources/db/migration/V11_002__configurable_parameters.sql`

```sql
CREATE TABLE configurable_parameters (
    id              BIGSERIAL PRIMARY KEY,
    param_key       VARCHAR(255) NOT NULL,
    param_value     VARCHAR(1000) NOT NULL,
    scope           VARCHAR(20) NOT NULL,
    scope_id        VARCHAR(100),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100),
    UNIQUE (param_key, scope, scope_id)
);

CREATE INDEX idx_config_param_key ON configurable_parameters (param_key);
```

---

## Part D -- Dependencies

| Dependency | From Section | What Is Needed |
|------------|--------------|----------------|
| **section-01-foundation** | Base entity classes, Money value object, Gradle multi-module structure, Testcontainers base configuration, PostgreSQL/Redis/Kafka connection setup | All five components in this section depend on the foundation module being in place |

### Gradle Dependencies (for `shared/build.gradle`)

```groovy
dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-cache'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Redisson (distributed lock)
    implementation 'org.redisson:redisson-spring-boot-starter:3.27.0'

    // Kafka
    implementation 'org.springframework.kafka:spring-kafka'

    // Micrometer + Prometheus
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // Database
    runtimeOnly 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:postgresql'
    testImplementation 'org.testcontainers:kafka'
    testImplementation 'org.redisson:redisson-spring-boot-starter:3.27.0'
}
```
