# Section 01 -- Foundation

> **Scope**: Project structure, Gradle multi-module build, Spring Boot application setup, database schema migrations, Redis Cluster and Kafka configuration, Money value object, base entity classes, and test infrastructure (Testcontainers).
> **Blocks**: All other sections depend on this one.
> **Dependencies**: None -- this is the root of the dependency graph.
> **Tech Stack**: Java 17+, Spring Boot 3.x, Gradle (Kotlin DSL), PostgreSQL, Redis Cluster, Kafka

---

## 1. Overview

This section establishes the complete project skeleton for the iGaming Funding Domain. Everything built here is consumed by every subsequent section. The deliverables are:

1. A Gradle multi-module project with 10 submodules plus a shared module.
2. The `Money` value object -- the foundational type for all monetary operations across the entire domain.
3. Base entity classes with JPA auditing.
4. Flyway database migration infrastructure with the initial wallet schema.
5. Spring Boot application configuration for PostgreSQL, Redis Cluster, and Kafka.
6. A reusable Testcontainers-based test infrastructure so all integration tests across every section can spin up real PostgreSQL, Redis, and Kafka instances.

---

## 2. Tests First (TDD)

All tests in this section should be written and verified to fail (red) before any implementation is provided.

### 2.1 Money Value Object Tests

**File**: `currency-engine/src/test/java/com/funding/currency/MoneyTest.java`

These tests verify the five invariants of the `Money` type that protect the entire domain from precision errors.

```java
package com.funding.currency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("BigDecimal created from String, NEVER from double")
    void shouldCreateFromString() {
        // Money internally must use new BigDecimal("10.5000"), not new BigDecimal(10.5)
        // Verify: Money.of("10.5", "USD").getAmount() equals new BigDecimal("10.5000")
        // Verify: no floating-point representation artifacts
    }

    @Test
    @DisplayName("Arithmetic always specifies ROUND_HALF_UP")
    void shouldUseRoundHalfUp() {
        // Given two Money values whose division produces a 5 in the rounding digit
        // When dividing (e.g., Money.of("10.0000", "USD").divide(3))
        // Then result rounds up: 3.3334 (not 3.3333)
    }

    @Test
    @DisplayName("Scale is always 4 decimal places for storage")
    void shouldAlwaysHaveScale4() {
        // Given Money created with fewer decimals: Money.of("100", "USD")
        // Then amount.scale() == 4 and toString shows "100.0000"
        // Given Money from intermediate calc with 8 decimals
        // Then final Money is rounded to scale 4
    }

    @Test
    @DisplayName("Money(100, USD) + Money(50, EUR) throws CurrencyMismatchException")
    void shouldRejectCrossCurrencyArithmetic() {
        // Given Money a = Money.of("100.0000", "USD")
        // And   Money b = Money.of("50.0000", "EUR")
        // When  a.add(b)
        // Then  throws CurrencyMismatchException
    }

    @Test
    @DisplayName("Addition produces correct result with scale 4")
    void shouldAddCorrectly() {
        // Money.of("100.1234", "USD").add(Money.of("50.5678", "USD"))
        // equals Money.of("150.6912", "USD")
    }

    @Test
    @DisplayName("Subtraction produces correct result with scale 4")
    void shouldSubtractCorrectly() {
        // Money.of("100.1234", "USD").subtract(Money.of("50.0001", "USD"))
        // equals Money.of("50.1233", "USD")
    }

    @Test
    @DisplayName("Multiplication rounds to scale 4 with ROUND_HALF_UP")
    void shouldMultiplyWithCorrectRounding() {
        // Money.of("33.3333", "USD").multiply("3")
        // equals Money.of("99.9999", "USD")
    }

    @Test
    @DisplayName("Zero money is valid and has correct currency")
    void shouldSupportZeroMoney() {
        // Money.zero("USD").getAmount() equals BigDecimal 0.0000
        // Money.zero("USD").getCurrency() equals Currency.getInstance("USD")
    }

    @Test
    @DisplayName("Negative amount throws IllegalArgumentException")
    void shouldRejectNegativeAmount() {
        // Money.of("-10.0000", "USD") throws IllegalArgumentException
        // (Money represents non-negative values; debits use explicit operations)
    }

    @Test
    @DisplayName("Comparison works correctly for same currency")
    void shouldCompareCorrectly() {
        // Money.of("100.0000", "USD").isGreaterThan(Money.of("99.9999", "USD")) == true
        // Money.of("100.0000", "USD").isGreaterThanOrEqual(Money.of("100.0000", "USD")) == true
        // Money.of("50.0000", "USD").isLessThan(Money.of("100.0000", "USD")) == true
    }
}
```

### 2.2 Wallet Entity and Schema Tests (Integration)

**File**: `wallet-core/src/test/java/com/funding/wallet/WalletEntityIntegrationTest.java`

These tests verify the database schema constraints that serve as the last-resort safety net against data corruption.

```java
package com.funding.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletEntityIntegrationTest {

    // Uses shared PostgreSQLContainer from test infrastructure (see section 2.5)

    @Test
    @DisplayName("Wallet entity creation initializes with zero balance")
    void shouldCreateWalletWithZeroBalance() {
        // Given a new Wallet for a player with walletType=CASH
        // When persisted via WalletRepository.save()
        // Then wallet.getBalance() == Money.zero(currency)
        // And  wallet.getLockedAmount() == Money.zero(currency)
        // And  wallet.getVersion() == 0L
        // And  wallet.getStatus() == ACTIVE
    }

    @Test
    @DisplayName("DB CHECK constraint rejects negative CASH balance on INSERT/UPDATE")
    void shouldRejectNegativeCashBalance() {
        // Given a CASH wallet with balance = 0
        // When UPDATE wallet SET balance = -1.0000 WHERE walletType = 'CASH'
        // Then DB throws constraint violation exception
    }

    @Test
    @DisplayName("DB CHECK constraint rejects negative BONUS balance on INSERT/UPDATE")
    void shouldRejectNegativeBonusBalance() {
        // Given a BONUS wallet with balance = 0
        // When UPDATE wallet SET balance = -1.0000 WHERE walletType = 'BONUS'
        // Then DB throws constraint violation exception
    }

    @Test
    @DisplayName("DB CHECK constraint allows negative CREDIT balance")
    void shouldAllowNegativeCreditBalance() {
        // Given a CREDIT wallet with balance = 0
        // When UPDATE wallet SET balance = -500.0000 WHERE walletType = 'CREDIT'
        // Then no exception -- CREDIT wallets can go negative by design
    }

    @Test
    @DisplayName("Unique index on (playerId, walletType, tenantId) prevents duplicates")
    void shouldPreventDuplicateWallets() {
        // Given a CASH wallet exists for (player1, CASH, tenant1)
        // When inserting another wallet with the same (player1, CASH, tenant1)
        // Then DB throws unique constraint violation
    }

    @Test
    @DisplayName("Wallet auditing fields are auto-populated")
    void shouldPopulateAuditFields() {
        // Given a new Wallet
        // When persisted
        // Then createdAt is not null
        // And  updatedAt is not null
        // When updated
        // Then updatedAt changes, createdAt remains the same
    }
}
```

### 2.3 Flyway Migration Test

**File**: `wallet-core/src/test/java/com/funding/wallet/FlywayMigrationIntegrationTest.java`

```java
package com.funding.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@Testcontainers
class FlywayMigrationIntegrationTest {

    @Test
    @DisplayName("All Flyway migrations execute successfully on a clean database")
    void shouldRunAllMigrationsSuccessfully() {
        // Given a fresh PostgreSQL container
        // When Spring context loads (Flyway auto-runs)
        // Then no migration errors
        // And  the 'wallets' table exists with correct columns
        // And  the CHECK constraint exists on balance
        // And  the unique index on (playerId, walletType, tenantId) exists
    }

    @Test
    @DisplayName("Outbox table is created by migration")
    void shouldCreateOutboxTable() {
        // Then the 'outbox_events' table exists
        // And  has columns: id, topic, key, payload, status, created_at
    }
}
```

### 2.4 Test Infrastructure Verification

**File**: `shared/src/test/java/com/funding/test/TestInfrastructureTest.java`

```java
package com.funding.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

class TestInfrastructureTest {

    @Test
    @DisplayName("PostgreSQL Testcontainer starts and accepts connections")
    void shouldStartPostgres() {
        // Verify container.isRunning() == true
        // Verify JDBC connection can be established
    }

    @Test
    @DisplayName("Redis Testcontainer starts and accepts commands")
    void shouldStartRedis() {
        // Verify container.isRunning() == true
        // Verify SET/GET roundtrip works
    }

    @Test
    @DisplayName("Kafka Testcontainer starts and accepts produce/consume")
    void shouldStartKafka() {
        // Verify container.isRunning() == true
        // Verify produce a message to a test topic and consume it back
    }
}
```

### 2.5 Testing Conventions

| Convention | Detail |
|-----------|--------|
| Unit test naming | `{ClassName}Test.java` |
| Integration test naming | `{ClassName}IntegrationTest.java` |
| Unit test framework | JUnit 5 + Mockito + AssertJ |
| Integration test framework | Testcontainers + Spring Boot Test |
| Coverage target | 90%+ for wallet-core and wallet-transaction |
| Concurrency tests | Java `CountDownLatch` / `CyclicBarrier` |
| Contract tests | Spring Cloud Contract or Pact (later sections) |

---

## 3. Project Structure

### 3.1 Gradle Multi-Module Layout

The project root is `02-funding/`. Each submodule is a Gradle sub-project.

```
02-funding/
├── build.gradle.kts              # Root build -- shared plugin/dependency config
├── settings.gradle.kts           # Declares all submodules
├── gradle.properties             # Shared versions
│
├── wallet-core/                  # Wallet CRUD, balance engine, deduction priority
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/wallet/
│       │   ├── model/            # Wallet, WalletType, WalletStatus entities
│       │   ├── service/          # WalletService, BalanceCalculator, DeductionEngine
│       │   └── repository/       # WalletRepository, BalanceSnapshotRepository
│       └── test/java/com/funding/wallet/
│
├── wallet-transaction/           # Transaction processing, round lifecycle
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/transaction/
│       │   ├── model/            # Transaction, Round, TransactionScenario
│       │   ├── service/          # TransactionProcessor, RoundLifecycleManager
│       │   ├── scenario/         # Nine scenario handlers (Strategy pattern)
│       │   └── statemachine/     # Round state machine configuration
│       └── test/
│
├── seamless-wallet-api/          # GP-facing API layer
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/seamless/
│       │   ├── controller/       # Five endpoint controllers
│       │   ├── security/         # HMAC token validator
│       │   ├── idempotency/      # IdempotencyStore (Redis-backed)
│       │   └── adapter/          # GP-specific request/response adapters
│       └── test/
│
├── wallet-reconciliation/        # Three-layer reconciliation
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/reconciliation/
│       │   ├── service/          # RealtimeReconciler, HourlyReconciler, DailyReconciler
│       │   ├── detector/         # OrphanRoundDetector, DiscrepancyResolver
│       │   └── scheduler/        # Scheduled recon jobs
│       └── test/
│
├── payment-gateway/              # PSP orchestration layer
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/gateway/
│       │   ├── router/           # SmartRouter, RoutingScoreCalculator
│       │   ├── circuitbreaker/   # PSP health monitor, circuit breaker
│       │   ├── adapter/          # Per-PSP adapter implementations
│       │   └── model/            # PaymentOrder, PSPResponse, RoutingDecision
│       └── test/
│
├── payment-deposit/              # Deposit flow
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/deposit/
│       │   ├── service/          # DepositService, CashierFrictionEvaluator
│       │   ├── callback/         # PSP callback handlers
│       │   └── limit/            # DepositLimitChecker
│       └── test/
│
├── payment-withdrawal/           # Withdrawal flow
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/withdrawal/
│       │   ├── service/          # WithdrawalService, WageringVerifier
│       │   ├── approval/         # Multi-tier approval workflow
│       │   └── scheduler/        # Holiday SLA adjuster
│       └── test/
│
├── payment-dispute/              # Chargeback management
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/dispute/
│       │   ├── service/          # ChargebackService, EvidenceCollector
│       │   ├── lifecycle/        # Chargeback state machine
│       │   └── penalty/          # PlayerPenaltyEngine
│       └── test/
│
├── currency-engine/              # Multi-currency + crypto
│   ├── build.gradle.kts
│   └── src/
│       ├── main/java/com/funding/currency/
│       │   ├── service/          # CurrencyConverter, FXRateManager
│       │   ├── crypto/           # BlockchainMonitor, WalletAddressGenerator
│       │   └── precision/        # Money, CryptoAmount, PrecisionGuard
│       └── test/
│
└── shared/                       # Cross-module shared code
    ├── build.gradle.kts
    └── src/
        ├── main/java/com/funding/shared/
        │   ├── event/            # Kafka event publishers, outbox
        │   ├── lock/             # Distributed lock abstraction
        │   ├── config/           # DB-configurable parameter reader
        │   └── stub/             # Governance domain test stubs
        └── test/java/com/funding/test/
            └── TestContainerConfig.java  # Shared Testcontainers setup
```

### 3.2 Root Build Configuration

**File**: `settings.gradle.kts`

```kotlin
rootProject.name = "02-funding"

include(
    "wallet-core",
    "wallet-transaction",
    "seamless-wallet-api",
    "wallet-reconciliation",
    "payment-gateway",
    "payment-deposit",
    "payment-withdrawal",
    "payment-dispute",
    "currency-engine",
    "shared"
)
```

**File**: `gradle.properties`

```properties
# Shared version catalog
springBootVersion=3.2.x
javaVersion=17
testcontainersVersion=1.19.x
redissonVersion=3.25.x
flywayVersion=10.x
assertjVersion=3.25.x
```

**File**: `build.gradle.kts` (root)

```kotlin
plugins {
    java
    id("org.springframework.boot") version "${springBootVersion}" apply false
    id("io.spring.dependency-management") version "1.1.x" apply false
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    group = "com.funding"
    version = "0.0.1-SNAPSHOT"

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        // Common test dependencies for all modules
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.testcontainers:junit-jupiter")
        testImplementation("org.assertj:assertj-core")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
```

### 3.3 Submodule Build Files (Key Dependency Signatures)

**File**: `shared/build.gradle.kts`

```kotlin
plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.redisson:redisson-spring-boot-starter:${redissonVersion}")

    // Test infrastructure -- exposed to other modules
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:kafka")
    testImplementation("com.redis.testcontainers:testcontainers-redis:1.6.x")
}
```

**File**: `wallet-core/build.gradle.kts`

```kotlin
plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":currency-engine"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation(project(":shared", "testArtifacts"))
    testImplementation("org.testcontainers:postgresql")
}
```

**File**: `currency-engine/build.gradle.kts`

```kotlin
plugins {
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

dependencies {
    implementation(project(":shared"))

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":shared", "testArtifacts"))
}
```

---

## 4. Money Value Object

The `Money` class is the single most important type in this domain. Every monetary column, every calculation, every API response uses it. Getting this wrong causes cascading precision errors.

### 4.1 Design Rules (Non-Negotiable)

| Rule | Detail | Rationale |
|------|--------|-----------|
| String constructor only | `new BigDecimal("10.50")` -- NEVER `new BigDecimal(10.50)` | `double` constructor introduces IEEE 754 artifacts (e.g., `10.50` becomes `10.4999999...`) |
| Explicit rounding | Every arithmetic operation specifies `RoundingMode.HALF_UP` | Prevents `ArithmeticException` on non-terminating decimals and ensures deterministic rounding |
| Scale = 4 for storage | Final `Money` values always have `scale(4)` | Matches DB column `DECIMAL(19,4)`; sufficient for 4-decimal-place currencies |
| Scale = 8 for intermediates | Intermediate calculations may use up to 8 decimal places | Reduces cumulative rounding error across multi-step calculations; rounded to 4 at point of storage |
| Immutable | `@Value` (Lombok) or manual final fields + no setters | Prevents mutation bugs in concurrent code |
| Currency enforcement | Arithmetic between different currencies throws `CurrencyMismatchException` | Catches unit errors at compile-adjacent time |

### 4.2 Class Signature

**File**: `currency-engine/src/main/java/com/funding/currency/Money.java`

```java
package com.funding.currency;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * Immutable value object representing a monetary amount with currency.
 * This is the foundational type for ALL monetary operations in the funding domain.
 *
 * <p>Invariants:
 * <ul>
 *   <li>Amount is always non-negative</li>
 *   <li>Amount always has scale 4 (e.g., "100.0000")</li>
 *   <li>BigDecimal is always constructed from String, never from double</li>
 *   <li>All arithmetic uses RoundingMode.HALF_UP</li>
 *   <li>Cross-currency arithmetic throws CurrencyMismatchException</li>
 * </ul>
 */
public final class Money implements Comparable<Money> {

    private static final int STORAGE_SCALE = 4;
    private static final int INTERMEDIATE_SCALE = 8;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal amount;
    private final Currency currency;

    // --- Factory methods ---

    public static Money of(String amount, String currencyCode);
    public static Money of(BigDecimal amount, Currency currency);
    public static Money zero(String currencyCode);

    // --- Arithmetic (all return new Money with scale 4, ROUND_HALF_UP) ---

    public Money add(Money other);           // throws CurrencyMismatchException
    public Money subtract(Money other);      // throws CurrencyMismatchException
    public Money multiply(String factor);    // String factor to avoid double
    public Money divide(String divisor);     // String divisor to avoid double
    public Money negate();                   // For reversal operations

    // --- Comparison ---

    public boolean isGreaterThan(Money other);
    public boolean isGreaterThanOrEqual(Money other);
    public boolean isLessThan(Money other);
    public boolean isZero();
    public boolean isPositive();

    @Override
    public int compareTo(Money other);

    // --- Getters ---

    public BigDecimal getAmount();
    public Currency getCurrency();

    // --- equals/hashCode based on amount AND currency ---
    // --- toString returns e.g. "100.0000 USD" ---
}
```

**File**: `currency-engine/src/main/java/com/funding/currency/CurrencyMismatchException.java`

```java
package com.funding.currency;

/**
 * Thrown when arithmetic is attempted between Money objects of different currencies.
 */
public class CurrencyMismatchException extends RuntimeException {
    public CurrencyMismatchException(Currency a, Currency b);
}
```

### 4.3 JPA Converter (for entity mapping)

**File**: `currency-engine/src/main/java/com/funding/currency/MoneyAttributeConverter.java`

```java
package com.funding.currency;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.math.BigDecimal;

/**
 * JPA converter for persisting Money.amount to DECIMAL(19,4) columns.
 * The currency is stored in a separate column on the entity.
 */
@Converter(autoApply = false)
public class MoneyAttributeConverter implements AttributeConverter<BigDecimal, BigDecimal> {
    // Ensures scale=4 on write, reconstructs with scale=4 on read
}
```

---

## 5. Base Entity Classes

### 5.1 Auditable Base Entity

**File**: `shared/src/main/java/com/funding/shared/entity/BaseEntity.java`

```java
package com.funding.shared.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Base entity with auto-generated UUID primary key and JPA auditing fields.
 * All domain entities extend this.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant createdAt;

    @Column(nullable = false)
    @LastModifiedDate
    private Instant updatedAt;

    // Getters (no setters for id and createdAt -- immutable after creation)
    public UUID getId();
    public Instant getCreatedAt();
    public Instant getUpdatedAt();
}
```

### 5.2 Versioned Base Entity (for optimistic locking)

**File**: `shared/src/main/java/com/funding/shared/entity/VersionedEntity.java`

```java
package com.funding.shared.entity;

import jakarta.persistence.*;

/**
 * Extends BaseEntity with an optimistic lock version column.
 * Used by entities that participate in concurrent reads (e.g., Wallet).
 * Note: The version column is for optimistic reads and audit, NOT for the
 * critical write path which uses Redisson + DB conditional updates.
 */
@MappedSuperclass
public abstract class VersionedEntity extends BaseEntity {

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public Long getVersion();
}
```

### 5.3 Wallet Entity Signature

**File**: `wallet-core/src/main/java/com/funding/wallet/model/Wallet.java`

```java
package com.funding.wallet.model;

import com.funding.shared.entity.VersionedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

/**
 * Represents a single wallet for a player. Each player has up to 3 wallets
 * per tenant: CASH, BONUS, CREDIT.
 *
 * <p>DB constraints:
 * <ul>
 *   <li>CHECK (balance >= 0 OR wallet_type = 'CREDIT')</li>
 *   <li>UNIQUE (player_id, wallet_type, tenant_id)</li>
 * </ul>
 */
@Entity
@Table(name = "wallets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"player_id", "wallet_type", "tenant_id"})
})
public class Wallet extends VersionedEntity {

    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, updatable = false)
    private WalletType walletType;

    @Column(nullable = false, length = 3)
    private String currency;  // ISO 4217

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    @Column(name = "locked_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    // Protected no-arg constructor for JPA
    // Factory method: Wallet.create(playerId, tenantId, walletType, currency)
    //   -> initializes balance=0.0000, lockedAmount=0.0000, status=ACTIVE
}
```

**File**: `wallet-core/src/main/java/com/funding/wallet/model/WalletType.java`

```java
package com.funding.wallet.model;

public enum WalletType {
    CASH,
    BONUS,
    CREDIT
}
```

**File**: `wallet-core/src/main/java/com/funding/wallet/model/WalletStatus.java`

```java
package com.funding.wallet.model;

public enum WalletStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}
```

### 5.4 JPA Auditing Configuration

**File**: `shared/src/main/java/com/funding/shared/config/JpaAuditingConfig.java`

```java
package com.funding.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
    // Enables @CreatedDate and @LastModifiedDate on BaseEntity
}
```

---

## 6. Database Schema Migrations (Flyway)

### 6.1 Migration File Structure

Flyway migrations live in the module that owns the table. All migration files follow the naming convention `V{version}__{description}.sql`.

```
wallet-core/src/main/resources/db/migration/
    V1__create_wallets_table.sql
    V2__create_outbox_events_table.sql
```

### 6.2 V1 -- Wallets Table

**File**: `wallet-core/src/main/resources/db/migration/V1__create_wallets_table.sql`

```sql
-- V1: Core wallets table with CHECK constraint and unique index
-- This is the financial heart of the platform.

CREATE TABLE wallets (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id       UUID            NOT NULL,
    tenant_id       UUID            NOT NULL,
    wallet_type     VARCHAR(10)     NOT NULL,       -- CASH, BONUS, CREDIT
    currency        VARCHAR(3)      NOT NULL,       -- ISO 4217
    balance         DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    locked_amount   DECIMAL(19, 4)  NOT NULL DEFAULT 0.0000,
    version         BIGINT          NOT NULL DEFAULT 0,
    status          VARCHAR(15)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, SUSPENDED, CLOSED
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- Last-resort safety net: CASH and BONUS wallets must never go negative.
    -- CREDIT wallets are allowed to go negative by design.
    CONSTRAINT chk_wallet_balance_non_negative
        CHECK (balance >= 0 OR wallet_type = 'CREDIT'),

    -- One wallet per type per player per tenant.
    CONSTRAINT uq_wallet_player_type_tenant
        UNIQUE (player_id, wallet_type, tenant_id)
);

-- Index for fast player wallet lookup (the hot path query)
CREATE INDEX idx_wallets_player_id ON wallets (player_id);

-- Index for tenant-scoped queries (admin/reporting)
CREATE INDEX idx_wallets_tenant_id ON wallets (tenant_id);

-- Comment for documentation
COMMENT ON CONSTRAINT chk_wallet_balance_non_negative ON wallets IS
    'Prevents negative balance for CASH/BONUS wallets at the DB level. '
    'This is the safety net beneath the application-level Redisson lock '
    'and the UPDATE WHERE balance >= amount guard.';
```

### 6.3 V2 -- Outbox Events Table

**File**: `wallet-core/src/main/resources/db/migration/V2__create_outbox_events_table.sql`

```sql
-- V2: Transactional outbox for guaranteed Kafka event delivery.
-- Events are written to this table in the SAME DB transaction as the
-- business operation, then published to Kafka by a relay process.

CREATE TABLE outbox_events (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    topic           VARCHAR(255)    NOT NULL,       -- Kafka topic name
    event_key       VARCHAR(255)    NOT NULL,       -- Kafka message key (e.g., playerId)
    payload         JSONB           NOT NULL,       -- Serialized event payload
    status          VARCHAR(15)     NOT NULL DEFAULT 'PENDING',  -- PENDING, PUBLISHED, FAILED
    retry_count     INT             NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMPTZ,

    CONSTRAINT chk_outbox_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED'))
);

-- Index for the relay process: poll for unpublished events
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at)
    WHERE status = 'PENDING';
```

---

## 7. Application Configuration

### 7.1 Spring Boot Application Properties

**File**: `seamless-wallet-api/src/main/resources/application.yml`

This is the deployable Spring Boot application that brings all modules together.

```yaml
spring:
  application:
    name: funding-service

  # --- PostgreSQL ---
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:funding}
    username: ${DB_USER:funding_user}
    password: ${DB_PASSWORD:changeme}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 3000      # 3s -- fail fast
      idle-timeout: 600000          # 10min
      max-lifetime: 1800000         # 30min

  jpa:
    hibernate:
      ddl-auto: validate             # Flyway manages schema; Hibernate only validates
    open-in-view: false              # Prevent lazy-loading in controllers
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        default_schema: public

  # --- Flyway ---
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false

  # --- Redis Cluster ---
  data:
    redis:
      cluster:
        nodes: ${REDIS_NODES:localhost:7000,localhost:7001,localhost:7002}
        max-redirects: 3
      timeout: 2000                  # 2s connection timeout
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2

  # --- Kafka ---
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all                      # Strongest durability guarantee
      retries: 3
      properties:
        enable.idempotence: true     # Exactly-once producer semantics
    consumer:
      group-id: funding-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.funding.*"

# --- Redisson (distributed lock) ---
redisson:
  config: |
    clusterServersConfig:
      nodeAddresses:
        - "redis://${REDIS_NODE_1:localhost:7000}"
        - "redis://${REDIS_NODE_2:localhost:7001}"
        - "redis://${REDIS_NODE_3:localhost:7002}"
      scanInterval: 2000

# --- Server ---
server:
  port: ${SERVER_PORT:8080}

# --- Management / Actuator ---
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: funding-service
```

### 7.2 Test Profile Configuration

**File**: `shared/src/test/resources/application-test.yml`

```yaml
spring:
  # Testcontainers will override these via @DynamicPropertySource
  datasource:
    url: jdbc:tc:postgresql:15:///funding_test
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate
  kafka:
    bootstrap-servers: ${spring.embedded.kafka.brokers:localhost:9092}
```

---

## 8. Test Infrastructure (Testcontainers)

This is the shared test support module that all other modules depend on for integration tests.

### 8.1 Shared Container Configuration

**File**: `shared/src/test/java/com/funding/test/TestContainerConfig.java`

```java
package com.funding.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers configuration for integration tests.
 *
 * <p>Usage: Extend or compose with this class in integration tests.
 * Containers are started once per JVM (singleton pattern) and reused
 * across all test classes to keep test suite fast.
 *
 * <p>Containers provided:
 * <ul>
 *   <li>PostgreSQL 15 -- funding database</li>
 *   <li>Redis 7 (standalone, simulating cluster in test) -- caching and locks</li>
 *   <li>Kafka (Confluent) -- event streaming</li>
 * </ul>
 */
public abstract class TestContainerConfig {

    static final PostgreSQLContainer<?> POSTGRES;
    static final GenericContainer<?> REDIS;
    static final KafkaContainer KAFKA;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("funding_test")
                .withUsername("test")
                .withPassword("test");

        REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379);

        KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

        // Start all containers -- singleton pattern ensures one start per JVM
        POSTGRES.start();
        REDIS.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        // Redis (standalone in tests; production uses cluster)
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // Kafka
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }
}
```

### 8.2 Base Integration Test Class

**File**: `shared/src/test/java/com/funding/test/BaseIntegrationTest.java`

```java
package com.funding.test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for all integration tests.
 * Starts Testcontainers, activates the "test" profile, and injects
 * container connection properties into the Spring context.
 *
 * <p>Subclasses simply extend this and write tests:
 * <pre>
 * class MyServiceIntegrationTest extends BaseIntegrationTest {
 *     @Autowired MyService service;
 *
 *     @Test void shouldDoSomething() { ... }
 * }
 * </pre>
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseIntegrationTest extends TestContainerConfig {
    // All container setup is inherited.
    // Submodule integration tests extend this class.
}
```

---

## 9. Spring Boot Application Entry Point

**File**: `seamless-wallet-api/src/main/java/com/funding/FundingApplication.java`

```java
package com.funding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Funding Domain service.
 * This module aggregates all submodules into a single deployable.
 */
@SpringBootApplication(scanBasePackages = "com.funding")
@EnableScheduling  // For reconciliation jobs, orphan detection, outbox relay
public class FundingApplication {

    public static void main(String[] args) {
        SpringApplication.run(FundingApplication.class, args);
    }
}
```

---

## 10. Dependencies on Other Sections

This section is the root and has no dependencies. However, the following sections directly consume what is built here:

| What This Section Provides | Consumed By |
|---------------------------|-------------|
| `Money` value object | ALL sections -- every monetary calculation |
| `BaseEntity` / `VersionedEntity` | ALL sections -- every JPA entity |
| `Wallet` entity + Flyway migrations | section-02 (wallet-core), section-03 (wallet-transaction) |
| `TestContainerConfig` / `BaseIntegrationTest` | ALL sections -- every integration test |
| Gradle multi-module structure | ALL sections -- module boundaries |
| `application.yml` (DB/Redis/Kafka config) | ALL sections -- infrastructure access |
| Outbox events table | section-11 (shared-infrastructure) for Kafka outbox relay |
| `WalletType` / `WalletStatus` enums | section-02, section-03, section-04 |

---

## 11. Checklist for Implementation

Use this checklist to verify the foundation is complete before moving to section-02.

- [ ] `./gradlew build` succeeds with no compilation errors across all modules
- [ ] `./gradlew test` runs all foundation tests green
- [ ] `MoneyTest` -- all 10 tests pass (String construction, ROUND_HALF_UP, scale 4, currency mismatch, arithmetic, zero, negative rejection, comparison)
- [ ] `WalletEntityIntegrationTest` -- all 6 tests pass (zero balance init, CHECK constraint for CASH, CHECK constraint for BONUS, CREDIT allows negative, unique index, audit fields)
- [ ] `FlywayMigrationIntegrationTest` -- migrations run on fresh PostgreSQL
- [ ] `TestInfrastructureTest` -- PostgreSQL, Redis, and Kafka containers start and accept connections
- [ ] JPA auditing populates `createdAt` and `updatedAt` automatically
- [ ] Redis connection works in test profile
- [ ] Kafka produce/consume roundtrip works in test profile
