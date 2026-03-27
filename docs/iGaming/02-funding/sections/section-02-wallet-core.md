# Section 02 -- Wallet Core (`wallet-core`)

> **Depends on**: section-01-foundation (Money value object, base entities, Gradle config, Testcontainers setup)
> **Blocks**: section-03-wallet-transaction, section-04-seamless-api, section-05-wallet-reconciliation
> **Parallelizable with**: section-06-currency-engine, section-07-payment-gateway
> **Tech stack**: Java 17+, Spring Boot 3.x, Gradle, PostgreSQL, Redis Cluster

---

## Overview

This section implements the foundational wallet layer of the Funding Domain. It covers:

1. **Three wallet types** -- CASH, BONUS, CREDIT -- with entity modeling, DB constraints, and repository access.
2. **Playable balance calculation** -- a hot-path service (<50ms SLA) with versioned Redis caching and read-replica fallback.
3. **Deduction priority engine** -- a 4-layer override system that determines which wallet(s) to debit for a given bet, with full audit trail.

All monetary values use the `Money` value object from section-01-foundation (`BigDecimal` with scale 4, `ROUND_HALF_UP`, no naked numerics).

---

## Module Structure

```
wallet-core/
├── src/main/java/com/funding/wallet/core/
│   ├── model/
│   │   ├── Wallet.java                    # Wallet JPA entity
│   │   ├── WalletType.java                # Enum: CASH, BONUS, CREDIT
│   │   ├── WalletStatus.java              # Enum: ACTIVE, SUSPENDED, CLOSED
│   │   ├── DeductionStep.java             # Value object: walletType, amount, appliedLayer, ruleId
│   │   ├── DeductionContext.java           # Context for deduction resolution
│   │   ├── DeductionLayer.java            # Enum: PLAYER(1), GP_CONTRACT(2), GAME_LEVEL(3), SYSTEM_DEFAULT(4)
│   │   ├── DeductionRule.java             # Entity: persisted priority rule
│   │   └── GameContext.java               # Context for balance calculation (GP config, game type)
│   ├── repository/
│   │   ├── WalletRepository.java          # Spring Data JPA repository
│   │   └── DeductionRuleRepository.java   # Rule lookup repository
│   ├── service/
│   │   ├── BalanceCalculator.java         # Playable balance computation + versioned caching
│   │   ├── DeductionEngine.java           # 4-layer priority resolution
│   │   └── WalletService.java             # Wallet CRUD operations
│   └── cache/
│       └── VersionedBalanceCache.java     # Redis versioned cache key management
├── src/main/resources/
│   └── db/migration/
│       └── V2__wallet_core_schema.sql     # Flyway migration for wallet tables
└── src/test/java/com/funding/wallet/core/
    ├── model/
    │   └── WalletEntityTest.java          # Unit tests for entity creation
    ├── repository/
    │   └── WalletRepositoryIntegrationTest.java  # DB constraint + index tests
    ├── service/
    │   ├── BalanceCalculatorTest.java     # Unit tests for balance logic
    │   ├── BalanceCalculatorIntegrationTest.java  # Cache + DB integration
    │   ├── DeductionEngineTest.java       # Unit tests for priority resolution
    │   └── DeductionEngineIntegrationTest.java   # Full priority chain tests
    └── cache/
        └── VersionedBalanceCacheIntegrationTest.java  # Redis versioned cache tests
```

---

## Part 1: Tests (TDD -- Write These First)

All tests derive from the TDD plan (claude-plan-tdd.md, sections 3.1, 3.2, 3.3). Tests are written and must pass BEFORE implementation proceeds.

### 1.1 Data Model Tests

#### `WalletEntityTest.java` (Unit)

```java
package com.funding.wallet.core.model;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class WalletEntityTest {

    /**
     * TDD 3.1: Wallet entity creation initializes all three wallet types with zero balance.
     *
     * Given a new player onboarding,
     * When the system creates wallets for the player,
     * Then exactly three wallets (CASH, BONUS, CREDIT) are created,
     * And each has balance = 0, lockedAmount = 0, status = ACTIVE.
     */
    @Test
    void shouldInitializeAllThreeWalletTypesWithZeroBalance() {
        // Arrange: playerId, tenantId, currency
        // Act: create wallets via factory or service
        // Assert: 3 wallets exist, each with Money(0), ACTIVE status
        fail("Not yet implemented");
    }
}
```

#### `WalletRepositoryIntegrationTest.java` (Integration -- Testcontainers PostgreSQL)

```java
package com.funding.wallet.core.repository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WalletRepositoryIntegrationTest {

    // Inject: WalletRepository, TestEntityManager
    // Testcontainers: PostgreSQL container via shared base class from section-01

    /**
     * TDD 3.1: DB CHECK constraint rejects negative CASH balance on INSERT/UPDATE.
     *
     * Given a CASH wallet with balance = 100,
     * When an UPDATE sets balance = -1,
     * Then the DB throws a constraint violation exception.
     */
    @Test
    void shouldRejectNegativeCashBalanceViaDbCheckConstraint() {
        // Arrange: persist a CASH wallet with balance 100
        // Act: attempt native UPDATE to set balance = -1
        // Assert: ConstraintViolationException thrown
        fail("Not yet implemented");
    }

    /**
     * TDD 3.1: DB CHECK constraint rejects negative BONUS balance on INSERT/UPDATE.
     *
     * Given a BONUS wallet with balance = 50,
     * When an UPDATE sets balance = -1,
     * Then the DB throws a constraint violation exception.
     */
    @Test
    void shouldRejectNegativeBonusBalanceViaDbCheckConstraint() {
        // Arrange: persist a BONUS wallet with balance 50
        // Act: attempt native UPDATE to set balance = -1
        // Assert: ConstraintViolationException thrown
        fail("Not yet implemented");
    }

    /**
     * TDD 3.1: DB CHECK constraint allows negative CREDIT balance.
     *
     * Given a CREDIT wallet with balance = 0,
     * When an UPDATE sets balance = -500,
     * Then the update succeeds (CREDIT wallets may carry negative balance).
     */
    @Test
    void shouldAllowNegativeCreditBalance() {
        // Arrange: persist a CREDIT wallet with balance 0
        // Act: native UPDATE to set balance = -500
        // Assert: update succeeds, wallet.balance == -500
        fail("Not yet implemented");
    }

    /**
     * TDD 3.1: Unique index on (playerId, walletType, tenantId) prevents duplicates.
     *
     * Given an existing CASH wallet for player P1 in tenant T1,
     * When inserting another CASH wallet for the same player and tenant,
     * Then the DB throws a unique constraint violation.
     */
    @Test
    void shouldPreventDuplicateWalletForSamePlayerTypeTenant() {
        // Arrange: persist CASH wallet for (playerA, CASH, tenantA)
        // Act: attempt to persist another CASH wallet for (playerA, CASH, tenantA)
        // Assert: DataIntegrityViolationException thrown
        fail("Not yet implemented");
    }
}
```

---

### 1.2 Playable Balance Calculation Tests

#### `BalanceCalculatorTest.java` (Unit -- Mockito)

```java
package com.funding.wallet.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BalanceCalculatorTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private VersionedBalanceCache balanceCache;

    @InjectMocks
    private BalanceCalculator balanceCalculator;

    /**
     * TDD 3.2: Standard mode = CASH + BONUS - locked - inProgress.
     *
     * Given CASH.balance = 1000, BONUS.balance = 500, locked = 200, inProgress = 100,
     * When calculating playable balance in standard mode,
     * Then result = 1000 + 500 - 200 - 100 = 1200.
     */
    @Test
    void shouldCalculateStandardModeBalance() {
        // Arrange: mock wallet repo to return CASH(1000), BONUS(500)
        //          mock locked=200, inProgress=100
        //          GameContext with standard mode (GP accepts BONUS)
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: result == Money(1200.0000, currency)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.2: Credit mode = creditLimit - usedAmount.
     *
     * Given a CREDIT wallet with creditLimit = 5000, usedAmount = 1200,
     * When calculating playable balance in credit mode,
     * Then result = 5000 - 1200 = 3800.
     */
    @Test
    void shouldCalculateCreditModeBalance() {
        // Arrange: mock CREDIT wallet, creditLimit=5000, usedAmount=1200
        //          GameContext with credit mode
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: result == Money(3800.0000, currency)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.2: GP that rejects BONUS -> playable balance excludes BONUS wallet.
     *
     * Given CASH.balance = 1000, BONUS.balance = 500,
     * When GP contract specifies rejectBonus = true,
     * Then playable balance = CASH only = 1000 (BONUS excluded).
     */
    @Test
    void shouldExcludeBonusWalletWhenGpRejectsBonus() {
        // Arrange: mock wallets CASH(1000), BONUS(500)
        //          GameContext with gpRejectsBonus = true
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: result == Money(1000.0000, currency) -- BONUS excluded
        fail("Not yet implemented");
    }

    /**
     * TDD 3.2: Precision: result always has exactly 4 decimal places.
     *
     * Given CASH.balance = 33.3333, BONUS.balance = 66.6667,
     * When calculating playable balance,
     * Then result has exactly 4 decimal places (scale = 4).
     */
    @Test
    void shouldAlwaysReturnFourDecimalPlaces() {
        // Arrange: mock wallets with fractional balances
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: result.getAmount().scale() == 4
        fail("Not yet implemented");
    }

    /**
     * TDD 3.2: Balance calculation with zero balance across all wallets returns Money(0).
     *
     * Given all wallets have balance = 0,
     * When calculating playable balance,
     * Then result = Money(0.0000, currency).
     */
    @Test
    void shouldReturnZeroMoneyWhenAllWalletsEmpty() {
        // Arrange: mock wallets all with balance 0
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: result == Money(0.0000, currency)
        fail("Not yet implemented");
    }
}
```

#### `BalanceCalculatorIntegrationTest.java` (Integration -- Testcontainers PostgreSQL + Redis)

```java
package com.funding.wallet.core.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
class BalanceCalculatorIntegrationTest {

    // Inject: BalanceCalculator, WalletRepository, RedisTemplate
    // Testcontainers: PostgreSQL + Redis containers via shared base class

    /**
     * TDD 3.2: Versioned cache key returns cached value when version matches.
     *
     * Given a wallet with version=5 and playable balance = 1000,
     * And the cache contains key "balance:{playerId}:v5" = 1000,
     * When calculatePlayableBalance is called,
     * Then the cached value is returned without DB query.
     */
    @Test
    void shouldReturnCachedValueWhenVersionMatches() {
        // Arrange: persist wallet with version=5
        //          pre-populate Redis key "balance:{playerId}:v5" = 1000
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: result == 1000, no DB query executed (verify via query count)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.2: Cache miss triggers DB read replica query.
     *
     * Given a wallet with version=5,
     * And no cache entry for "balance:{playerId}:v5",
     * When calculatePlayableBalance is called,
     * Then the DB is queried (read replica) and result is cached.
     */
    @Test
    void shouldQueryDbOnCacheMissAndPopulateCache() {
        // Arrange: persist wallet, ensure no Redis key exists
        // Act: calculatePlayableBalance(playerId, ctx)
        // Assert: DB was queried, result cached at "balance:{playerId}:v{version}"
        fail("Not yet implemented");
    }

    /**
     * TDD 3.2: Balance mutation increments version, old cache key naturally expires.
     *
     * Given a cached balance at version=5,
     * When a balance mutation occurs (version becomes 6),
     * Then the old key "balance:{playerId}:v5" is orphaned (TTL 2s expiry),
     * And subsequent reads use "balance:{playerId}:v6" (cache miss -> DB -> cache).
     */
    @Test
    void shouldIncrementVersionOnMutationAndExpireOldCacheKey() {
        // Arrange: wallet at version=5 with cached balance
        // Act: mutate balance (simulated), version -> 6
        // Assert: next read uses v6 key, misses cache, queries DB, caches at v6
        //         v5 key still exists but will expire via TTL
        fail("Not yet implemented");
    }
}
```

---

### 1.3 Deduction Priority Engine Tests

#### `DeductionEngineTest.java` (Unit -- Mockito)

```java
package com.funding.wallet.core.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeductionEngineTest {

    @Mock
    private DeductionRuleRepository ruleRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private DeductionEngine deductionEngine;

    /**
     * TDD 3.3: Default priority is BONUS -> CASH -> CREDIT.
     *
     * Given no overrides at any layer,
     * When resolving deduction order,
     * Then the order is [BONUS, CASH, CREDIT] (system default = Layer 4).
     */
    @Test
    void shouldApplyDefaultPriorityBonusCashCredit() {
        // Arrange: no rules at Layer 1-3, system default active
        //          BONUS balance = 200, CASH balance = 800
        //          amount = 100
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: single step from BONUS(100), appliedLayer = SYSTEM_DEFAULT(4)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Layer 1 (player) override takes precedence over all others.
     *
     * Given a player-level rule: priority = [CASH, BONUS, CREDIT],
     * And a GP contract rule: priority = [BONUS, CASH],
     * When resolving deduction order,
     * Then Layer 1 (player) wins: deduction starts from CASH.
     */
    @Test
    void shouldApplyPlayerOverrideAboveAllOtherLayers() {
        // Arrange: Layer 1 rule for player -> [CASH, BONUS, CREDIT]
        //          Layer 2 rule for GP     -> [BONUS, CASH]
        //          CASH balance = 500, BONUS balance = 300
        //          amount = 100
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: step from CASH(100), appliedLayer = PLAYER(1)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Layer 2 (GP contract) override applies when no Layer 1 exists.
     *
     * Given no player-level rule,
     * But GP contract rule: priority = [CASH, CREDIT] (GP rejects BONUS),
     * When resolving deduction order,
     * Then Layer 2 applies: BONUS is excluded, deduction from CASH.
     */
    @Test
    void shouldApplyGpContractOverrideWhenNoPlayerRule() {
        // Arrange: no Layer 1 rule
        //          Layer 2 rule for GP -> [CASH, CREDIT]
        //          CASH balance = 500
        //          amount = 100
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: step from CASH(100), appliedLayer = GP_CONTRACT(2)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Layer 3 (game level) override for Live Casino = CASH only.
     *
     * Given no player-level or GP-level rule,
     * But game-level rule: Live Casino = [CASH] only,
     * When resolving deduction for a Live Casino game,
     * Then Layer 3 applies: only CASH is used.
     */
    @Test
    void shouldApplyGameLevelOverrideForLiveCasino() {
        // Arrange: no Layer 1, no Layer 2
        //          Layer 3 rule: gameCategory=LIVE_CASINO -> [CASH]
        //          CASH balance = 1000
        //          amount = 100
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: step from CASH(100), appliedLayer = GAME_LEVEL(3)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Layer 4 (system default) applies when no overrides exist.
     *
     * Given no rules at Layer 1, 2, or 3,
     * When resolving deduction order,
     * Then Layer 4 system default applies: BONUS -> CASH -> CREDIT.
     */
    @Test
    void shouldFallbackToSystemDefaultWhenNoOverrides() {
        // Arrange: no rules at any layer
        //          BONUS balance = 50, CASH balance = 200
        //          amount = 30
        // Act: resolveDeductionOrder(playerId, Money(30), ctx)
        // Assert: step from BONUS(30), appliedLayer = SYSTEM_DEFAULT(4)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Multi-wallet deduction: BONUS insufficient -> remainder from CASH.
     *
     * Given default priority (BONUS -> CASH -> CREDIT),
     * And BONUS.balance = 30, CASH.balance = 200,
     * When deducting 100,
     * Then step 1: BONUS(30), step 2: CASH(70).
     */
    @Test
    void shouldSpillOverToNextWalletWhenInsufficient() {
        // Arrange: BONUS balance = 30, CASH balance = 200
        //          amount = 100, default priority
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: 2 steps -> [BONUS(30, SYSTEM_DEFAULT), CASH(70, SYSTEM_DEFAULT)]
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Each DeductionStep records the applied layer for audit.
     *
     * Given any deduction resolution,
     * When steps are generated,
     * Then every DeductionStep has a non-null appliedLayer and ruleId.
     */
    @Test
    void shouldRecordAppliedLayerAndRuleIdOnEveryStep() {
        // Arrange: any valid deduction scenario
        // Act: resolveDeductionOrder(playerId, amount, ctx)
        // Assert: all steps have appliedLayer != null, ruleId != null
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: All deduction steps sum to exactly the requested amount.
     *
     * Given a multi-wallet deduction of 100,
     * When steps are generated,
     * Then sum of all step amounts == 100 exactly (no rounding drift).
     */
    @Test
    void shouldSumDeductionStepsToExactRequestedAmount() {
        // Arrange: BONUS balance = 33.3333, CASH balance = 200
        //          amount = 100
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: steps.stream().map(DeductionStep::amount).reduce(Money::add) == Money(100)
        fail("Not yet implemented");
    }

    /**
     * TDD 3.3: Empty wallet -> next wallet in priority order.
     *
     * Given BONUS.balance = 0, CASH.balance = 500,
     * When deducting 100 with default priority,
     * Then BONUS is skipped (zero balance), CASH is used entirely.
     */
    @Test
    void shouldSkipEmptyWalletAndProceedToNext() {
        // Arrange: BONUS balance = 0, CASH balance = 500
        //          amount = 100, default priority
        // Act: resolveDeductionOrder(playerId, Money(100), ctx)
        // Assert: single step CASH(100), BONUS skipped
        fail("Not yet implemented");
    }
}
```

---

## Part 2: Implementation Stubs

All implementation files below are stubs with method signatures, annotations, and Javadoc. The actual logic is to be filled in after tests are green-to-red.

### 2.1 Enums

#### `WalletType.java`

```java
package com.funding.wallet.core.model;

/**
 * The three wallet types supported per player.
 *
 * <p>CASH: Real money deposited by the player. Cannot go negative (DB CHECK enforced).
 * <p>BONUS: Promotional funds subject to wagering requirements. Cannot go negative.
 * <p>CREDIT: Credit line extended to VIP/agent players. MAY carry negative balance.
 */
public enum WalletType {
    CASH,
    BONUS,
    CREDIT
}
```

#### `WalletStatus.java`

```java
package com.funding.wallet.core.model;

/**
 * Wallet lifecycle status.
 */
public enum WalletStatus {
    ACTIVE,
    SUSPENDED,
    CLOSED
}
```

#### `DeductionLayer.java`

```java
package com.funding.wallet.core.model;

/**
 * The 4-layer override hierarchy for deduction priority resolution.
 *
 * <p>Evaluation order: Layer 1 (highest precedence) -> Layer 4 (lowest / system default).
 * The first layer that defines a rule for the given context wins.
 */
public enum DeductionLayer {

    /** Layer 1: Player-level override (e.g., VIP prefers CASH first). */
    PLAYER(1),

    /** Layer 2: GP contract override (e.g., GP rejects BONUS). */
    GP_CONTRACT(2),

    /** Layer 3: Game-level override (e.g., Live Casino = CASH only). */
    GAME_LEVEL(3),

    /** Layer 4: System default (BONUS -> CASH -> CREDIT). */
    SYSTEM_DEFAULT(4);

    private final int priority;

    DeductionLayer(int priority) {
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
```

---

### 2.2 Wallet Entity

#### `Wallet.java`

```java
package com.funding.wallet.core.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Wallet entity representing a single wallet for a player within a tenant.
 *
 * <p>Each player has exactly three wallets (CASH, BONUS, CREDIT) per tenant,
 * enforced by the unique index on (playerId, walletType, tenantId).
 *
 * <p>DB CHECK constraint: {@code balance >= 0 OR walletType = 'CREDIT'}
 * prevents negative CASH/BONUS balances at the database level as a last-resort guard.
 * The constraint checks {@code balance} alone (not {@code balance + lockedAmount})
 * because the invariant is that the actual balance must never go negative for non-CREDIT wallets.
 *
 * @see WalletType
 * @see WalletStatus
 */
@Entity
@Table(
    name = "wallet",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_wallet_player_type_tenant",
        columnNames = {"player_id", "wallet_type", "tenant_id"}
    )
)
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false, length = 10)
    private WalletType walletType;

    /** ISO 4217 currency code (e.g., "USD", "EUR"). */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Current balance. Scale 4, precision 19. */
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /** Amount currently locked (e.g., pending withdrawal). */
    @Column(name = "locked_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal lockedAmount;

    /**
     * Optimistic lock version. Incremented on every balance mutation.
     * Used for versioned cache keys and audit -- NOT for the write-path
     * concurrency control (Redisson lock handles that).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    private WalletStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
        // JPA required no-arg constructor
    }

    // --- Factory / Builder ---

    /**
     * Creates a new wallet with zero balance and ACTIVE status.
     *
     * @param playerId   the owning player's ID
     * @param tenantId   the tenant this wallet belongs to
     * @param walletType one of CASH, BONUS, CREDIT
     * @param currency   ISO 4217 currency code
     * @return a new Wallet instance ready for persistence
     */
    public static Wallet create(UUID playerId, UUID tenantId, WalletType walletType, String currency) {
        // TODO: implement
        throw new UnsupportedOperationException("Not yet implemented");
    }

    // --- Getters ---

    public UUID getId() { return id; }
    public UUID getPlayerId() { return playerId; }
    public UUID getTenantId() { return tenantId; }
    public WalletType getWalletType() { return walletType; }
    public String getCurrency() { return currency; }
    public BigDecimal getBalance() { return balance; }
    public BigDecimal getLockedAmount() { return lockedAmount; }
    public Long getVersion() { return version; }
    public WalletStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // --- Lifecycle callbacks ---

    @PrePersist
    protected void onCreate() {
        // TODO: set createdAt, updatedAt, default balance/lockedAmount to 0
    }

    @PreUpdate
    protected void onUpdate() {
        // TODO: set updatedAt
    }
}
```

---

### 2.3 Value Objects

#### `DeductionStep.java`

```java
package com.funding.wallet.core.model;

import java.util.UUID;

/**
 * Immutable value object representing a single step in a multi-wallet deduction.
 *
 * <p>Each step records:
 * <ul>
 *   <li>{@code walletType} -- which wallet to debit</li>
 *   <li>{@code amount} -- how much to debit from this wallet (Money value object)</li>
 *   <li>{@code appliedLayer} -- which override layer determined this step (1-4)</li>
 *   <li>{@code ruleId} -- the specific rule ID for audit trail (nullable for SYSTEM_DEFAULT)</li>
 * </ul>
 *
 * <p>Invariant: The sum of all DeductionStep amounts in a deduction MUST equal
 * the total requested deduction amount exactly (no rounding drift).
 *
 * @param walletType   the wallet type to debit
 * @param amount       the Money amount to debit from this wallet
 * @param appliedLayer the deduction layer that produced this step
 * @param ruleId       the specific rule ID (for audit); null for system default
 */
public record DeductionStep(
    WalletType walletType,
    Money amount,               // Money from section-01-foundation
    DeductionLayer appliedLayer,
    UUID ruleId
) {
    /**
     * Compact constructor with validation.
     */
    public DeductionStep {
        if (walletType == null) throw new IllegalArgumentException("walletType must not be null");
        if (amount == null) throw new IllegalArgumentException("amount must not be null");
        if (appliedLayer == null) throw new IllegalArgumentException("appliedLayer must not be null");
        // ruleId may be null for SYSTEM_DEFAULT layer
    }
}
```

#### `DeductionContext.java`

```java
package com.funding.wallet.core.model;

import java.util.UUID;

/**
 * Context object passed to the DeductionEngine to resolve deduction priority.
 *
 * <p>Contains all the identifiers needed to look up override rules at each layer:
 * player, GP, game category, and tenant.
 *
 * @param playerId     the player whose wallets are being debited
 * @param gpId         the game provider initiating the transaction
 * @param gameCategory the category of the game (e.g., SLOTS, LIVE_CASINO, TABLE_GAMES)
 * @param tenantId     the tenant context for multi-tenant rule isolation
 */
public record DeductionContext(
    UUID playerId,
    UUID gpId,
    String gameCategory,
    UUID tenantId
) {}
```

#### `GameContext.java`

```java
package com.funding.wallet.core.model;

import java.util.UUID;

/**
 * Context for balance calculation. Determines which wallets participate
 * in the playable balance based on GP configuration and game type.
 *
 * @param gpId             the game provider requesting the balance
 * @param gpRejectsBonus   true if the GP contract excludes BONUS wallet
 * @param creditModeActive true if the player operates in credit mode
 * @param tenantId         the tenant context
 */
public record GameContext(
    UUID gpId,
    boolean gpRejectsBonus,
    boolean creditModeActive,
    UUID tenantId
) {}
```

---

### 2.4 DeductionRule Entity

#### `DeductionRule.java`

```java
package com.funding.wallet.core.model;

import jakarta.persistence.*;
import java.util.UUID;

/**
 * Persisted deduction priority rule. Rules are stored in the DB-configurable
 * parameter system and resolved using the 4-layer override hierarchy.
 *
 * <p>Rules are looked up by the DeductionEngine in layer order (1 -> 4).
 * The first matching rule wins.
 *
 * <p>The {@code walletPriority} field is a comma-separated ordered list
 * of WalletType values (e.g., "CASH,BONUS,CREDIT" or "CASH" for CASH-only).
 */
@Entity
@Table(name = "deduction_rule")
public class DeductionRule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "layer", nullable = false, length = 20)
    private DeductionLayer layer;

    /** Nullable. Set for PLAYER layer rules. */
    @Column(name = "player_id")
    private UUID playerId;

    /** Nullable. Set for GP_CONTRACT layer rules. */
    @Column(name = "gp_id")
    private UUID gpId;

    /** Nullable. Set for GAME_LEVEL layer rules (e.g., "LIVE_CASINO"). */
    @Column(name = "game_category", length = 50)
    private String gameCategory;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Comma-separated ordered wallet types.
     * Example: "BONUS,CASH,CREDIT" (system default) or "CASH" (CASH only).
     */
    @Column(name = "wallet_priority", nullable = false, length = 50)
    private String walletPriority;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected DeductionRule() {}

    // --- Getters ---

    public UUID getId() { return id; }
    public DeductionLayer getLayer() { return layer; }
    public UUID getPlayerId() { return playerId; }
    public UUID getGpId() { return gpId; }
    public String getGameCategory() { return gameCategory; }
    public UUID getTenantId() { return tenantId; }
    public String getWalletPriority() { return walletPriority; }
    public boolean isActive() { return active; }

    /**
     * Parses the walletPriority string into an ordered list of WalletType.
     *
     * @return ordered list of WalletType for deduction
     */
    public java.util.List<WalletType> getParsedPriority() {
        // TODO: implement -- split by comma, map to WalletType enum
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.5 Repositories

#### `WalletRepository.java`

```java
package com.funding.wallet.core.repository;

import com.funding.wallet.core.model.Wallet;
import com.funding.wallet.core.model.WalletType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Wallet entity CRUD and query operations.
 *
 * <p>Read-intensive queries (balance lookups) should use {@code @QueryHint} for read-replica
 * routing when the datasource supports it.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    /**
     * Find all wallets for a player within a tenant.
     *
     * @param playerId the player's ID
     * @param tenantId the tenant's ID
     * @return list of wallets (up to 3: CASH, BONUS, CREDIT)
     */
    List<Wallet> findByPlayerIdAndTenantId(UUID playerId, UUID tenantId);

    /**
     * Find a specific wallet by player, type, and tenant.
     *
     * @param playerId   the player's ID
     * @param walletType the wallet type
     * @param tenantId   the tenant's ID
     * @return the wallet if it exists
     */
    Optional<Wallet> findByPlayerIdAndWalletTypeAndTenantId(
        UUID playerId, WalletType walletType, UUID tenantId
    );

    /**
     * Atomic conditional balance deduction. Returns the number of rows updated (0 or 1).
     * Used by the transaction processor as the DB-level safety net.
     *
     * <p>This is the DB guard in the double-layer concurrency control:
     * Redisson lock serializes at app level; this WHERE clause prevents over-deduction at DB level.
     *
     * @param walletId the wallet ID
     * @param amount   the amount to deduct
     * @return 1 if successful, 0 if insufficient balance
     */
    @Query(value = """
        UPDATE wallet
        SET balance = balance - :amount,
            updated_at = NOW(),
            version = version + 1
        WHERE id = :walletId
          AND balance >= :amount
        """, nativeQuery = true)
    int deductBalanceAtomic(@Param("walletId") UUID walletId, @Param("amount") java.math.BigDecimal amount);

    /**
     * Credit (add) to a wallet balance. No conditional check needed for credits.
     *
     * @param walletId the wallet ID
     * @param amount   the amount to credit
     * @return 1 if successful
     */
    @Query(value = """
        UPDATE wallet
        SET balance = balance + :amount,
            updated_at = NOW(),
            version = version + 1
        WHERE id = :walletId
        """, nativeQuery = true)
    int creditBalance(@Param("walletId") UUID walletId, @Param("amount") java.math.BigDecimal amount);
}
```

#### `DeductionRuleRepository.java`

```java
package com.funding.wallet.core.repository;

import com.funding.wallet.core.model.DeductionLayer;
import com.funding.wallet.core.model.DeductionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for deduction priority rules.
 *
 * <p>Rules are looked up in layer order (1 -> 4) by the DeductionEngine.
 * Each finder method corresponds to one layer of the override hierarchy.
 */
@Repository
public interface DeductionRuleRepository extends JpaRepository<DeductionRule, UUID> {

    /** Layer 1: Player-level override. */
    Optional<DeductionRule> findByLayerAndPlayerIdAndTenantIdAndActiveTrue(
        DeductionLayer layer, UUID playerId, UUID tenantId
    );

    /** Layer 2: GP contract override. */
    Optional<DeductionRule> findByLayerAndGpIdAndTenantIdAndActiveTrue(
        DeductionLayer layer, UUID gpId, UUID tenantId
    );

    /** Layer 3: Game-level override. */
    Optional<DeductionRule> findByLayerAndGameCategoryAndTenantIdAndActiveTrue(
        DeductionLayer layer, String gameCategory, UUID tenantId
    );

    /** Layer 4: System default (no player/GP/game filter). */
    Optional<DeductionRule> findByLayerAndTenantIdAndActiveTrue(
        DeductionLayer layer, UUID tenantId
    );
}
```

---

### 2.6 Services

#### `BalanceCalculator.java`

```java
package com.funding.wallet.core.service;

import com.funding.wallet.core.model.GameContext;
import com.funding.wallet.core.model.Wallet;
import com.funding.wallet.core.cache.VersionedBalanceCache;
import com.funding.wallet.core.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Computes playable balance for a player. This is a <b>hot path</b> called
 * on every GetBalance request with a <50ms SLA target.
 *
 * <h3>Calculation modes:</h3>
 * <ul>
 *   <li><b>Standard mode</b>: {@code CASH.balance + BONUS.balance - totalLocked - totalInProgress}</li>
 *   <li><b>Credit mode</b>: {@code CREDIT.limit - CREDIT.usedAmount}</li>
 * </ul>
 *
 * <h3>GP BONUS rejection:</h3>
 * If the GP contract rejects BONUS ({@code GameContext.gpRejectsBonus() == true}),
 * the BONUS wallet is excluded from the calculation.
 *
 * <h3>Caching strategy (versioned cache key):</h3>
 * <ul>
 *   <li>Cache key format: {@code balance:{playerId}:v{walletVersion}}</li>
 *   <li>On cache hit with matching version: return cached value (no DB query)</li>
 *   <li>On cache miss: query DB (read replica), cache result with 2s TTL</li>
 *   <li>On balance mutation: wallet version auto-increments; old cache key
 *       is orphaned and expires naturally via TTL -- no explicit invalidation needed</li>
 * </ul>
 *
 * <h3>Precision:</h3>
 * Result is always a Money value object with exactly 4 decimal places (scale 4, ROUND_HALF_UP).
 */
@Service
public class BalanceCalculator {

    private final WalletRepository walletRepository;
    private final VersionedBalanceCache balanceCache;

    public BalanceCalculator(WalletRepository walletRepository, VersionedBalanceCache balanceCache) {
        this.walletRepository = walletRepository;
        this.balanceCache = balanceCache;
    }

    /**
     * Calculate the playable balance for a player given the game context.
     *
     * @param playerId the player's ID
     * @param ctx      game context determining which wallets participate
     * @return the playable balance as a Money value object (4 decimal places)
     */
    public Money calculatePlayableBalance(UUID playerId, GameContext ctx) {
        // TODO: implement
        // 1. Determine wallet version from the repository
        // 2. Check versioned cache: "balance:{playerId}:v{version}"
        // 3. On hit: return cached Money
        // 4. On miss: query DB (read replica), compute balance per mode:
        //    - Standard: CASH + BONUS(if !gpRejectsBonus) - locked - inProgress
        //    - Credit: creditLimit - usedAmount
        // 5. Cache result with 2s TTL
        // 6. Return Money with scale 4
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

#### `DeductionEngine.java`

```java
package com.funding.wallet.core.service;

import com.funding.wallet.core.model.*;
import com.funding.wallet.core.repository.DeductionRuleRepository;
import com.funding.wallet.core.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Resolves the deduction order for a given bet amount across a player's wallets.
 *
 * <h3>4-Layer Override Hierarchy:</h3>
 * <ol>
 *   <li><b>Layer 1 -- Player</b>: Player-specific rule (e.g., VIP prefers CASH first)</li>
 *   <li><b>Layer 2 -- GP Contract</b>: GP-specific rule (e.g., GP rejects BONUS)</li>
 *   <li><b>Layer 3 -- Game Level</b>: Game-category rule (e.g., Live Casino = CASH only)</li>
 *   <li><b>Layer 4 -- System Default</b>: BONUS -> CASH -> CREDIT</li>
 * </ol>
 *
 * <p>The engine evaluates layers top-down. The <b>first layer that has an active rule</b>
 * for the given context wins. If no override exists at Layers 1-3, Layer 4 applies.
 *
 * <h3>Multi-wallet spillover:</h3>
 * If the first wallet in the priority order has insufficient balance, the remainder
 * spills over to the next wallet. Each partial deduction becomes a separate
 * {@link DeductionStep}.
 *
 * <h3>Audit trail:</h3>
 * Every {@link DeductionStep} records:
 * <ul>
 *   <li>{@code appliedLayer} -- which layer determined the priority</li>
 *   <li>{@code ruleId} -- the specific rule ID (null for SYSTEM_DEFAULT)</li>
 * </ul>
 *
 * <h3>Invariant:</h3>
 * The sum of all returned DeductionStep amounts MUST equal the requested amount exactly.
 */
@Service
public class DeductionEngine {

    private final DeductionRuleRepository ruleRepository;
    private final WalletRepository walletRepository;

    public DeductionEngine(DeductionRuleRepository ruleRepository, WalletRepository walletRepository) {
        this.ruleRepository = ruleRepository;
        this.walletRepository = walletRepository;
    }

    /**
     * Resolve the deduction order for a given amount.
     *
     * @param playerId the player's ID
     * @param amount   the total amount to deduct
     * @param ctx      deduction context (player, GP, game category, tenant)
     * @return ordered list of DeductionStep; sum of amounts == requested amount
     * @throws InsufficientBalanceException if total available balance across all
     *         eligible wallets is less than the requested amount
     */
    public List<DeductionStep> resolveDeductionOrder(UUID playerId, Money amount, DeductionContext ctx) {
        // TODO: implement
        // 1. Resolve priority: check Layer 1 -> 2 -> 3 -> 4 (first match wins)
        // 2. Get ordered wallet types from the winning rule
        // 3. For each wallet type in order:
        //    a. Look up wallet balance
        //    b. Deduct min(remaining, walletBalance)
        //    c. Create DeductionStep with walletType, amount, appliedLayer, ruleId
        //    d. Reduce remaining amount
        //    e. If remaining == 0, stop
        // 4. If remaining > 0 after all wallets, throw InsufficientBalanceException
        // 5. Assert: sum of steps == requested amount
        // 6. Return steps
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Resolves the winning deduction rule by evaluating layers 1 through 4.
     *
     * @param ctx the deduction context
     * @return the resolved DeductionRule (Layer 4 system default is always present)
     */
    DeductionRule resolveRule(DeductionContext ctx) {
        // TODO: implement
        // Layer 1: findByLayerAndPlayerIdAndTenantIdAndActiveTrue(PLAYER, playerId, tenantId)
        // Layer 2: findByLayerAndGpIdAndTenantIdAndActiveTrue(GP_CONTRACT, gpId, tenantId)
        // Layer 3: findByLayerAndGameCategoryAndTenantIdAndActiveTrue(GAME_LEVEL, gameCategory, tenantId)
        // Layer 4: findByLayerAndTenantIdAndActiveTrue(SYSTEM_DEFAULT, tenantId)
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

#### `WalletService.java`

```java
package com.funding.wallet.core.service;

import com.funding.wallet.core.model.Wallet;
import com.funding.wallet.core.model.WalletType;
import com.funding.wallet.core.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * CRUD operations for wallets. Handles wallet creation during player onboarding
 * and wallet status management.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    /**
     * Creates all three wallets (CASH, BONUS, CREDIT) for a new player.
     * Each wallet is initialized with zero balance and ACTIVE status.
     *
     * @param playerId the new player's ID
     * @param tenantId the tenant this player belongs to
     * @param currency the ISO 4217 currency code for these wallets
     * @return the list of 3 created wallets
     */
    @Transactional
    public List<Wallet> createWalletsForPlayer(UUID playerId, UUID tenantId, String currency) {
        // TODO: implement
        // For each WalletType: Wallet.create(playerId, tenantId, type, currency)
        // Persist all three
        // Return list
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Retrieves all wallets for a player within a tenant.
     *
     * @param playerId the player's ID
     * @param tenantId the tenant's ID
     * @return list of wallets (up to 3)
     */
    @Transactional(readOnly = true)
    public List<Wallet> getPlayerWallets(UUID playerId, UUID tenantId) {
        return walletRepository.findByPlayerIdAndTenantId(playerId, tenantId);
    }
}
```

---

### 2.7 Cache Layer

#### `VersionedBalanceCache.java`

```java
package com.funding.wallet.core.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Redis-backed versioned balance cache.
 *
 * <h3>Cache key format:</h3>
 * {@code balance:{playerId}:v{walletVersion}}
 *
 * <h3>Why versioned keys:</h3>
 * Eliminates the classic cache-aside write-invalidation race condition.
 * When a balance mutation occurs, the wallet's version column auto-increments.
 * The old cache key (with old version) is never invalidated explicitly -- it
 * simply becomes unreachable (no one asks for it) and expires via TTL (2 seconds).
 *
 * <h3>TTL:</h3>
 * 2 seconds. Short enough that stale reads are bounded; long enough to absorb
 * burst GetBalance requests during peak gameplay.
 */
@Component
public class VersionedBalanceCache {

    private static final Duration CACHE_TTL = Duration.ofSeconds(2);
    private static final String KEY_PREFIX = "balance:";

    private final RedisTemplate<String, String> redisTemplate;

    public VersionedBalanceCache(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Build the versioned cache key.
     *
     * @param playerId the player's ID
     * @param version  the wallet version
     * @return the cache key string
     */
    public String buildKey(UUID playerId, long version) {
        return KEY_PREFIX + playerId + ":v" + version;
    }

    /**
     * Get cached balance if present and version matches.
     *
     * @param playerId the player's ID
     * @param version  the current wallet version
     * @return the cached balance string, or empty if miss
     */
    public Optional<String> get(UUID playerId, long version) {
        // TODO: implement -- redisTemplate.opsForValue().get(buildKey(playerId, version))
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Cache the balance with the versioned key and TTL.
     *
     * @param playerId the player's ID
     * @param version  the wallet version at time of calculation
     * @param balance  the serialized balance value
     */
    public void put(UUID playerId, long version, String balance) {
        // TODO: implement -- redisTemplate.opsForValue().set(key, balance, CACHE_TTL)
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
```

---

### 2.8 Database Migration

#### `V2__wallet_core_schema.sql`

```sql
-- Flyway migration: V2 -- Wallet Core schema
-- Depends on: V1 (foundation tables from section-01)

-- =============================================================================
-- WALLET TABLE
-- =============================================================================
CREATE TABLE wallet (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id    UUID         NOT NULL,
    tenant_id    UUID         NOT NULL,
    wallet_type  VARCHAR(10)  NOT NULL,     -- CASH, BONUS, CREDIT
    currency     VARCHAR(3)   NOT NULL,     -- ISO 4217
    balance      DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    locked_amount DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    version      BIGINT       NOT NULL DEFAULT 0,
    status       VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    -- CRITICAL: Last-resort negative balance prevention for CASH and BONUS.
    -- CREDIT wallets MAY carry negative balance (credit line usage).
    -- Checks balance alone (not balance + lockedAmount).
    CONSTRAINT chk_wallet_balance_non_negative
        CHECK (balance >= 0 OR wallet_type = 'CREDIT'),

    -- Wallet type must be one of the three allowed values.
    CONSTRAINT chk_wallet_type_valid
        CHECK (wallet_type IN ('CASH', 'BONUS', 'CREDIT')),

    -- Status must be one of the allowed values.
    CONSTRAINT chk_wallet_status_valid
        CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

-- Unique constraint: one wallet per (player, type, tenant).
CREATE UNIQUE INDEX uk_wallet_player_type_tenant
    ON wallet (player_id, wallet_type, tenant_id);

-- Index for fast lookup by player within a tenant.
CREATE INDEX idx_wallet_player_tenant
    ON wallet (player_id, tenant_id);

-- =============================================================================
-- DEDUCTION RULE TABLE
-- =============================================================================
CREATE TABLE deduction_rule (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    layer           VARCHAR(20)  NOT NULL,   -- PLAYER, GP_CONTRACT, GAME_LEVEL, SYSTEM_DEFAULT
    player_id       UUID,                    -- Set for PLAYER layer
    gp_id           UUID,                    -- Set for GP_CONTRACT layer
    game_category   VARCHAR(50),             -- Set for GAME_LEVEL layer (e.g., LIVE_CASINO)
    tenant_id       UUID         NOT NULL,
    wallet_priority VARCHAR(50)  NOT NULL,   -- Comma-separated: "BONUS,CASH,CREDIT"
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_deduction_layer_valid
        CHECK (layer IN ('PLAYER', 'GP_CONTRACT', 'GAME_LEVEL', 'SYSTEM_DEFAULT'))
);

-- Index for Layer 1 lookup: player-level override.
CREATE INDEX idx_deduction_rule_player
    ON deduction_rule (layer, player_id, tenant_id) WHERE active = TRUE;

-- Index for Layer 2 lookup: GP contract override.
CREATE INDEX idx_deduction_rule_gp
    ON deduction_rule (layer, gp_id, tenant_id) WHERE active = TRUE;

-- Index for Layer 3 lookup: game-level override.
CREATE INDEX idx_deduction_rule_game
    ON deduction_rule (layer, game_category, tenant_id) WHERE active = TRUE;

-- Index for Layer 4 lookup: system default.
CREATE INDEX idx_deduction_rule_system
    ON deduction_rule (layer, tenant_id) WHERE active = TRUE AND layer = 'SYSTEM_DEFAULT';

-- =============================================================================
-- SEED DATA: System default deduction rule (Layer 4)
-- One per tenant. The actual tenant_id must be replaced during deployment.
-- =============================================================================
-- INSERT INTO deduction_rule (layer, tenant_id, wallet_priority)
-- VALUES ('SYSTEM_DEFAULT', '<tenant-uuid>', 'BONUS,CASH,CREDIT');
```

---

## Part 3: Dependency Summary

### Depends On (section-01-foundation)

This section assumes the following are already in place from section-01:

| Artifact | Purpose |
|----------|---------|
| `Money` value object | All monetary values (`BigDecimal`, scale 4, `ROUND_HALF_UP`) |
| Base entity classes | `@MappedSuperclass` with audit fields if applicable |
| Gradle multi-module config | `wallet-core` subproject configured |
| Flyway setup | Migration runner configured, `V1__*` migrations applied |
| Testcontainers base class | Shared PostgreSQL + Redis container setup for integration tests |
| `application-test.yml` | Test profile with Testcontainers datasource |

### Blocks (downstream sections)

| Section | What it needs from wallet-core |
|---------|-------------------------------|
| section-03-wallet-transaction | `WalletRepository.deductBalanceAtomic()`, `DeductionEngine.resolveDeductionOrder()`, `Wallet` entity |
| section-04-seamless-api | `BalanceCalculator.calculatePlayableBalance()`, `WalletService.getPlayerWallets()` |
| section-05-wallet-reconciliation | `WalletRepository` for balance queries during recon |
| section-09-payment-withdrawal | `WalletRepository` for balance locking |

---

## Checklist

- [ ] All TDD tests written and failing (red phase)
- [ ] `Wallet` entity with JPA annotations and factory method
- [ ] `WalletType`, `WalletStatus`, `DeductionLayer` enums
- [ ] `DeductionStep`, `DeductionContext`, `GameContext` value objects
- [ ] `DeductionRule` entity with `getParsedPriority()`
- [ ] `WalletRepository` with `deductBalanceAtomic()` and `creditBalance()` native queries
- [ ] `DeductionRuleRepository` with per-layer finders
- [ ] `BalanceCalculator` with versioned cache strategy
- [ ] `DeductionEngine` with 4-layer override resolution
- [ ] `WalletService` with player onboarding wallet creation
- [ ] `VersionedBalanceCache` with Redis key management
- [ ] `V2__wallet_core_schema.sql` Flyway migration with CHECK constraint and indexes
- [ ] All tests passing (green phase)
- [ ] Code reviewed for Money usage (no naked BigDecimal/double)
