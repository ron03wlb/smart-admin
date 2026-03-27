# Section 06 -- Currency Engine

> **Module**: `currency-engine`
> **Plan ref**: claude-plan.md SS11 (Currency Engine)
> **TDD ref**: claude-plan-tdd.md SS11.1, SS11.2, SS11.3
> **Depends on**: section-01-foundation (Money value object base, Gradle config, DB migrations, test infra)
> **Blocks**: section-07 (payment-gateway), section-08 (payment-deposit), section-09 (payment-withdrawal)
> **Parallelizable with**: section-02 (wallet-core)

---

## Overview

The Currency Engine is the multi-currency and cryptocurrency backbone of the funding domain. It provides:

1. **CryptoAmount value object** -- native blockchain precision (BTC:8, ETH:18, USDT:6) stored as `NUMERIC` in PostgreSQL
2. **FX Rate Manager** -- tiered caching (fiat 5min TTL, crypto 30sec TTL) with rate freeze (15min lock)
3. **FX Risk Sharing Matrix** -- DB-configurable rules for who absorbs exchange rate fluctuation
4. **Blockchain Monitor** -- watches BTC/ETH/USDT deposits, enforces confirmation thresholds
5. **Wallet Address Generator** -- unique deposit address per player per transaction
6. **AML Screening Integration** -- risk scoring for wallet addresses, Enhanced Due Diligence triggers

---

## Part A -- TDD Test Stubs (Tests FIRST)

All tests are written before implementation. Naming convention: `{ClassName}Test.java` for unit tests, `{ClassName}IntegrationTest.java` for integration tests.

### A.1 Money Value Object Tests (4 tests)

> These tests validate the core `Money` type that all monetary operations depend on.
> The Money value object itself lives in section-01 (foundation), but these currency-specific
> behavioral tests belong here to verify arithmetic correctness in currency conversion contexts.

```java
package com.funding.currencyengine.precision;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.assertj.core.api.Assertions.*;

class MoneyTest {

    @Test
    @DisplayName("BigDecimal created from String -- NEVER from double")
    void shouldCreateBigDecimalFromStringOnly() {
        // Given: a monetary amount expressed as a string
        // When: Money is constructed
        // Then: internal BigDecimal matches exact string representation
        // Assert: new Money("100.1234", USD).amount() equals new BigDecimal("100.1234")
        // Anti-pattern guard: verify Money cannot be constructed from a double literal
    }

    @Test
    @DisplayName("Arithmetic always specifies ROUND_HALF_UP")
    void shouldUseRoundHalfUpForAllArithmetic() {
        // Given: two Money values whose sum/product produces >4 decimal places
        // When: arithmetic operations (add, subtract, multiply, divide) are performed
        // Then: result is rounded using RoundingMode.HALF_UP
        // Example: Money("10.00005") rounded to 4dp -> "10.0001" (HALF_UP)
        // Verify: divide(3) on Money("10.0000") yields "3.3333" not "3.3334"
    }

    @Test
    @DisplayName("Scale is always 4 for storage")
    void shouldEnforceScale4ForStorage() {
        // Given: Money created with varying decimal places (0, 2, 6, 8)
        // When: toStorageValue() or amount() is called
        // Then: scale is always exactly 4
        // Verify: Money("100") -> "100.0000", Money("100.123456") -> "100.1235"
    }

    @Test
    @DisplayName("Money(100, USD) + Money(50, EUR) throws CurrencyMismatchException")
    void shouldRejectCrossCurrencyArithmetic() {
        // Given: Money in USD and Money in EUR
        // When: add operation is attempted
        // Then: CurrencyMismatchException is thrown
        // Verify: exception message includes both currency codes
    }
}
```

### A.2 FX Rate Management Tests (5 tests)

```java
package com.funding.currencyengine.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FXRateManagerTest {

    @Mock
    private FXRateProvider externalRateProvider;

    @Mock
    private FXRateCache rateCache;

    @Mock
    private FXRiskSharingRepository riskSharingRepository;

    @InjectMocks
    private FXRateManagerImpl fxRateManager;

    @Test
    @DisplayName("Fiat rate cached with 5min TTL")
    void shouldCacheFiatRateWith5MinTTL() {
        // Given: an external FX provider returns USD/EUR rate
        // When: getRate(USD, EUR) is called twice within 5 minutes
        // Then: external provider is called only once; second call returns cached value
        // Verify: cache TTL is set to 300 seconds for fiat currency pair
        // Verify: after 5min expiry, next call hits external provider again
    }

    @Test
    @DisplayName("Crypto rate cached with 30sec TTL")
    void shouldCacheCryptoRateWith30SecTTL() {
        // Given: an external FX provider returns BTC/USD rate
        // When: getRate(BTC, USD) is called twice within 30 seconds
        // Then: external provider is called only once; second call returns cached value
        // Verify: cache TTL is set to 30 seconds for crypto currency pair
        // Verify: crypto TTL is strictly shorter than fiat TTL
    }

    @Test
    @DisplayName("Rate freeze locks rate for 15 minutes")
    void shouldFreezeRateFor15Minutes() {
        // Given: current BTC/USD rate is 65000.0000
        // When: freezeRate(BTC, USD, Duration.ofMinutes(15)) is called
        // Then: FrozenRate is returned with the locked rate
        // Verify: subsequent getRate() calls within 15min return the frozen rate
        //         even if live rate has changed
        // Verify: after 15min, frozen rate expires and live rate is returned
        // Verify: FrozenRate contains freezeId, frozenAt timestamp, expiresAt timestamp
    }

    @Test
    @DisplayName("FX risk sharing: fiat <0.5% fluctuation -> platform absorbs")
    void shouldAbsorbFiatFluctuationBelow05Percent() {
        // Given: frozen rate was 1.1000 USD/EUR, current rate is 1.1040 (0.36% change)
        // And: FX risk sharing matrix loaded from DB says fiat threshold = 0.5%
        // When: calculateRiskSharing(frozenRate, currentRate) is called
        // Then: result indicates PLATFORM_ABSORBS
        // Verify: player is charged/credited at the original frozen rate
        // Verify: platform bears the 0.36% difference
    }

    @Test
    @DisplayName("FX risk sharing: fiat >0.5% fluctuation -> 50/50 split")
    void shouldSplitFiatFluctuationAbove05Percent() {
        // Given: frozen rate was 1.1000 USD/EUR, current rate is 1.1200 (1.82% change)
        // And: FX risk sharing matrix loaded from DB says fiat threshold = 0.5%
        // When: calculateRiskSharing(frozenRate, currentRate) is called
        // Then: result indicates SPLIT_50_50
        // Verify: the 1.82% fluctuation is split: player bears 0.91%, platform bears 0.91%
        // Verify: effective rate applied to player reflects the 50/50 split
        // Verify: risk sharing parameters are DB-configurable (not hardcoded)
    }
}
```

### A.3 Cryptocurrency Tests (7 tests)

```java
package com.funding.currencyengine.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptocurrencyServiceTest {

    @Mock
    private WalletAddressGenerator walletAddressGenerator;

    @Mock
    private BlockchainMonitor blockchainMonitor;

    @Mock
    private AmlScreeningClient amlScreeningClient;

    @Mock
    private FXRateManagerImpl fxRateManager;

    @InjectMocks
    private CryptocurrencyService cryptocurrencyService;

    @Test
    @DisplayName("Unique deposit address generated per player per transaction")
    void shouldGenerateUniqueDepositAddressPerPlayerPerTransaction() {
        // Given: playerId = "P001", crypto = BTC
        // When: generateDepositAddress(playerId, BTC) is called twice
        // Then: two different addresses are returned
        // Verify: each address is associated with (playerId, transactionId) in DB
        // Verify: address format is valid for the specified cryptocurrency
    }

    @Test
    @DisplayName("BTC deposit with <3 confirmations -> status PENDING, not credited")
    void shouldSetPendingStatusForBtcUnderThreeConfirmations() {
        // Given: a BTC deposit detected on-chain with 2 confirmations
        // When: blockchainMonitor processes the incoming transaction
        // Then: deposit status is PENDING
        // Verify: player wallet balance is NOT updated
        // Verify: deposit record exists in DB with confirmations=2, status=PENDING
        // Verify: monitoring continues for additional confirmations
    }

    @Test
    @DisplayName("BTC deposit with >=3 confirmations -> proceed to AML check")
    void shouldProceedToAmlCheckForBtcWithThreeOrMoreConfirmations() {
        // Given: a BTC deposit detected on-chain with 3 confirmations
        // When: blockchainMonitor processes the confirmation update
        // Then: AML screening is triggered for the sender wallet address
        // Verify: deposit status transitions from PENDING to AML_CHECK
        // Verify: amlScreeningClient.screenAddress() is called with the sender address
        // Verify: if AML passes, deposit proceeds to credit flow
    }

    @Test
    @DisplayName("High-risk wallet address -> deposit rejected")
    void shouldRejectDepositFromHighRiskWalletAddress() {
        // Given: a BTC deposit with >=3 confirmations from a flagged address
        // And: AML screening returns HIGH_RISK (mixer/darknet/sanctioned)
        // When: AML check is performed
        // Then: deposit is REJECTED
        // Verify: player wallet balance is NOT updated
        // Verify: deposit record updated with status=REJECTED, reason=HIGH_RISK_ADDRESS
        // Verify: compliance alert is generated
        // Verify: rejected deposit details are logged for regulatory reporting
    }

    @Test
    @DisplayName("CryptoAmount(ETH, 18 decimals) stored without truncation")
    void shouldStoreCryptoAmountWithoutTruncation() {
        // Given: CryptoAmount of 1.123456789012345678 ETH (18 decimal places)
        // When: stored in PostgreSQL NUMERIC column
        // Then: all 18 decimal places are preserved
        // Verify: retrieved value exactly equals the stored value
        // Verify: no rounding or truncation occurred
        // Verify: CryptoCurrency.ETH.getPrecision() returns 18
    }

    @Test
    @DisplayName("CryptoAmount -> Money conversion preserves value within 0.0001 precision")
    void shouldConvertCryptoAmountToMoneyWithinPrecisionTolerance() {
        // Given: CryptoAmount of 0.00150000 BTC
        // And: current BTC/USD rate is 65000.0000
        // When: convertToMoney(cryptoAmount, USD) is called
        // Then: result is Money("97.5000", USD) -- 0.0015 * 65000
        // Verify: |convertedMoney - expectedMoney| <= 0.0001
        // Verify: conversion uses the FX rate from FXRateManager
        // Verify: converted Money has scale=4
    }

    @Test
    @DisplayName("Deposit >$10K equivalent -> triggers EDD")
    void shouldTriggerEddForLargeDeposits() {
        // Given: a crypto deposit whose fiat equivalent exceeds $10,000
        // Example: 0.16 BTC at BTC/USD = 65000 -> $10,400
        // When: the deposit passes AML screening
        // Then: Enhanced Due Diligence (EDD) workflow is triggered
        // Verify: deposit status is set to EDD_REVIEW
        // Verify: EDD notification is sent to compliance team
        // Verify: deposit is NOT credited until EDD is completed
        // Verify: threshold ($10K) is DB-configurable
    }
}
```

### A.4 CryptoAmount Value Object Unit Tests

```java
package com.funding.currencyengine.precision;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.*;

class CryptoAmountTest {

    @ParameterizedTest
    @CsvSource({
        "BTC, 8",
        "ETH, 18",
        "USDT, 6"
    })
    @DisplayName("CryptoAmount respects native precision per currency")
    void shouldRespectNativePrecisionPerCurrency(String currency, int expectedPrecision) {
        // Given: a CryptoCurrency enum value
        // When: CryptoAmount is created for that currency
        // Then: the precision metadata matches the native blockchain precision
        // Verify: BTC allows up to 8 decimal places
        // Verify: ETH allows up to 18 decimal places
        // Verify: USDT allows up to 6 decimal places
    }

    @Test
    @DisplayName("CryptoAmount rejects values exceeding native precision")
    void shouldRejectValuesExceedingNativePrecision() {
        // Given: an amount string with more decimals than BTC allows (>8)
        // When: CryptoAmount(BTC, "0.123456789") is constructed
        // Then: IllegalArgumentException is thrown
        // Verify: error message indicates precision overflow
    }

    @Test
    @DisplayName("CryptoAmount created from String only -- never double")
    void shouldCreateFromStringOnly() {
        // Given: a precise crypto amount as a string
        // When: CryptoAmount is constructed
        // Then: no precision is lost
        // Verify: factory method only accepts String, not double
    }
}
```

### A.5 FX Rate Cache Integration Test

```java
package com.funding.currencyengine.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class FXRateCacheIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private FXRateManager fxRateManager;

    @Test
    @DisplayName("Fiat rate cache expires after 5 minutes in Redis")
    void shouldExpireFiatRateCacheAfter5Minutes() {
        // Given: a fiat rate is fetched and cached in Redis
        // When: 5 minutes pass (use Redis TTL inspection)
        // Then: cache key has expired
        // Verify: next fetch hits external provider
    }

    @Test
    @DisplayName("Crypto rate cache expires after 30 seconds in Redis")
    void shouldExpireCryptoRateCacheAfter30Seconds() {
        // Given: a crypto rate is fetched and cached in Redis
        // When: 30 seconds pass (use Redis TTL inspection)
        // Then: cache key has expired
        // Verify: next fetch hits external provider
    }

    @Test
    @DisplayName("Frozen rate persists in Redis for exactly 15 minutes")
    void shouldPersistFrozenRateInRedisFor15Minutes() {
        // Given: a rate is frozen
        // When: the frozen rate key is inspected in Redis
        // Then: TTL is approximately 900 seconds (15 minutes)
        // Verify: frozen rate is retrievable within the window
    }
}
```

### A.6 Blockchain Monitor Integration Test

```java
package com.funding.currencyengine.crypto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class BlockchainMonitorIntegrationTest {

    @Test
    @DisplayName("Deposit lifecycle: detect -> confirm -> AML -> credit")
    void shouldProcessFullDepositLifecycle() {
        // Given: a mocked blockchain node reporting a BTC incoming transaction
        // When: BlockchainMonitor detects the transaction
        // And: confirmations reach threshold (>=3)
        // And: AML screening passes
        // Then: deposit is credited to player's CASH wallet
        // Verify: crypto_transactions table has both CryptoAmount and converted Money
        // Verify: status transitions: DETECTED -> PENDING -> AML_CHECK -> CREDITED
    }

    @Test
    @DisplayName("Deposit to unrecognized address is ignored")
    void shouldIgnoreDepositsToUnrecognizedAddresses() {
        // Given: a blockchain transaction to an address not in our system
        // When: BlockchainMonitor processes it
        // Then: no deposit record is created
        // Verify: no wallet balance changes
    }
}
```

---

## Part B -- Implementation Details

### B.1 CryptoAmount Value Object

The `CryptoAmount` type handles native blockchain precision, separate from the fiat `Money` type. Conversion from `CryptoAmount` to `Money` happens exactly once at the FX conversion point.

```java
package com.funding.currencyengine.precision;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value object for cryptocurrency amounts with native blockchain precision.
 * <p>
 * BTC: 8 decimal places (satoshi)
 * ETH: 18 decimal places (wei)
 * USDT: 6 decimal places
 * <p>
 * IMPORTANT: Always construct from String to avoid floating-point precision loss.
 * Conversion to Money (fiat) occurs exactly once -- at the deposit-time FX conversion point.
 */
public final class CryptoAmount {

    private final BigDecimal amount;
    private final CryptoCurrency currency;

    private CryptoAmount(BigDecimal amount, CryptoCurrency currency) {
        // Validate: amount scale does not exceed currency.getPrecision()
        // Validate: amount is non-negative for deposits
        // Store as-is (no rescaling -- preserve native precision)
        throw new UnsupportedOperationException("Stub -- implement in TDD cycle");
    }

    /** Factory method: ONLY accepts String to prevent double precision loss. */
    public static CryptoAmount of(String amount, CryptoCurrency currency) {
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Convert to fiat Money using the provided exchange rate.
     * This is the SINGLE conversion point -- called once at deposit/withdrawal time.
     *
     * @param fiatCurrency target fiat currency (e.g., USD)
     * @param exchangeRate rate from FXRateManager (potentially frozen)
     * @return Money with scale=4, rounded HALF_UP
     */
    public Money toMoney(Currency fiatCurrency, BigDecimal exchangeRate) {
        // amount * exchangeRate, then round to scale=4 with ROUND_HALF_UP
        throw new UnsupportedOperationException("Stub");
    }

    public BigDecimal getAmount() { throw new UnsupportedOperationException("Stub"); }
    public CryptoCurrency getCurrency() { throw new UnsupportedOperationException("Stub"); }

    // equals, hashCode, toString
}
```

### B.2 CryptoCurrency Enum

```java
package com.funding.currencyengine.precision;

/**
 * Supported cryptocurrencies with their native precision metadata.
 * Precision = number of decimal places on the native blockchain.
 */
public enum CryptoCurrency {

    BTC(8, "Bitcoin", 3),       // 8 decimals (satoshi), 3 confirmation threshold
    ETH(18, "Ethereum", 12),    // 18 decimals (wei), 12 confirmation threshold
    USDT(6, "Tether", 12);      // 6 decimals, 12 confirmation threshold (ERC-20)

    private final int precision;
    private final String displayName;
    private final int confirmationThreshold;

    CryptoCurrency(int precision, String displayName, int confirmationThreshold) {
        // Store fields
        throw new UnsupportedOperationException("Stub");
    }

    public int getPrecision() { throw new UnsupportedOperationException("Stub"); }
    public String getDisplayName() { throw new UnsupportedOperationException("Stub"); }
    public int getConfirmationThreshold() { throw new UnsupportedOperationException("Stub"); }
}
```

### B.3 FX Rate Manager

```java
package com.funding.currencyengine.service;

import java.time.Duration;

/**
 * Manages exchange rates for both fiat and crypto currencies.
 *
 * Cache strategy:
 *   - Fiat pairs: Redis cache with 5-minute TTL (rates change slowly)
 *   - Crypto pairs: Redis cache with 30-second TTL (rates are volatile)
 *
 * Rate freeze:
 *   - Locks a rate for 15 minutes after deposit/withdrawal confirmation
 *   - Frozen rate stored in Redis with explicit TTL
 *   - Frozen rate takes priority over live rate during its validity window
 *
 * Rate source: External FX provider API (abstracted behind FXRateProvider interface)
 */
public interface FXRateManager {

    /**
     * Get the current exchange rate between two currencies.
     * Returns frozen rate if one exists and is valid; otherwise returns cached or live rate.
     */
    ExchangeRate getRate(Currency from, Currency to);

    /**
     * Freeze the current rate for the specified duration (default: 15 minutes).
     * Used at deposit/withdrawal confirmation time to protect against slippage.
     *
     * @return FrozenRate containing the locked rate, freezeId, and expiry
     */
    FrozenRate freezeRate(Currency from, Currency to, Duration freezeWindow);
}
```

```java
package com.funding.currencyengine.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class FXRateManagerImpl implements FXRateManager {

    private static final Duration FIAT_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration CRYPTO_CACHE_TTL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_FREEZE_WINDOW = Duration.ofMinutes(15);

    private final FXRateProvider externalRateProvider;
    private final FXRateCache rateCache;         // Redis-backed cache
    private final FXRiskSharingRepository riskSharingRepository;  // DB-configurable matrix

    public FXRateManagerImpl(FXRateProvider externalRateProvider,
                             FXRateCache rateCache,
                             FXRiskSharingRepository riskSharingRepository) {
        // Constructor injection
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public ExchangeRate getRate(Currency from, Currency to) {
        // 1. Check for frozen rate in cache -> return if valid
        // 2. Check for cached live rate -> return if not expired
        // 3. Fetch from external provider
        // 4. Cache with appropriate TTL (FIAT_CACHE_TTL or CRYPTO_CACHE_TTL)
        // 5. Return rate
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public FrozenRate freezeRate(Currency from, Currency to, Duration freezeWindow) {
        // 1. Get current live rate
        // 2. Create FrozenRate with freezeId, frozenAt, expiresAt
        // 3. Store in Redis with TTL = freezeWindow
        // 4. Return FrozenRate
        throw new UnsupportedOperationException("Stub");
    }

    /**
     * Determine TTL based on whether the currency pair involves crypto.
     */
    Duration determineCacheTTL(Currency from, Currency to) {
        // If either currency is crypto -> CRYPTO_CACHE_TTL (30s)
        // Otherwise -> FIAT_CACHE_TTL (5min)
        throw new UnsupportedOperationException("Stub");
    }
}
```

### B.4 FX Rate Provider (External API abstraction)

```java
package com.funding.currencyengine.service;

/**
 * Abstraction over external FX rate provider APIs.
 * Implementations may include: CoinGecko, CoinMarketCap, ECB, Xe, etc.
 */
public interface FXRateProvider {

    /**
     * Fetch the live exchange rate from the external provider.
     * This is an I/O call -- always cache the result via FXRateManager.
     */
    ExchangeRate fetchLiveRate(Currency from, Currency to);
}
```

### B.5 FX Rate Cache (Redis-backed)

```java
package com.funding.currencyengine.service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-backed cache for exchange rates and frozen rates.
 * Key patterns:
 *   - Live rate:   "fx:rate:{from}:{to}"       TTL = 5min (fiat) / 30s (crypto)
 *   - Frozen rate: "fx:frozen:{freezeId}"       TTL = 15min (default)
 *   - Frozen lookup: "fx:frozen:lookup:{from}:{to}" -> freezeId (TTL = 15min)
 */
public interface FXRateCache {

    void cacheRate(Currency from, Currency to, ExchangeRate rate, Duration ttl);
    Optional<ExchangeRate> getCachedRate(Currency from, Currency to);

    void cacheFrozenRate(FrozenRate frozenRate);
    Optional<FrozenRate> getActiveFrozenRate(Currency from, Currency to);
}
```

### B.6 FX Risk Sharing Matrix

```java
package com.funding.currencyengine.service;

import java.math.BigDecimal;

/**
 * DB-configurable FX risk sharing rules.
 *
 * Fiat rules (SSOT, all DB-configurable):
 *   - Fluctuation <= 0.5%  -> PLATFORM_ABSORBS (player gets frozen rate)
 *   - Fluctuation >  0.5%  -> SPLIT_50_50 (player and platform each bear half)
 *
 * Crypto rules:
 *   - Fluctuation <= 2%    -> PLATFORM_ABSORBS
 *   - Fluctuation >  2%    -> PLAYER_BEARS (with UI confirmation)
 *
 * Agent settlement:
 *   - Agent bears 100% (settlement day rate)
 */
public interface FXRiskSharingEvaluator {

    /**
     * Calculate the risk sharing outcome based on the frozen and current rates.
     *
     * @param frozenRate the rate locked at transaction confirmation time
     * @param currentRate the live rate at settlement time
     * @param currencyType FIAT or CRYPTO
     * @return RiskSharingResult indicating who absorbs and the effective rate
     */
    RiskSharingResult evaluate(BigDecimal frozenRate, BigDecimal currentRate, CurrencyType currencyType);
}
```

```java
package com.funding.currencyengine.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

@Service
public class FXRiskSharingEvaluatorImpl implements FXRiskSharingEvaluator {

    private final FXRiskSharingRepository riskSharingRepository;

    public FXRiskSharingEvaluatorImpl(FXRiskSharingRepository riskSharingRepository) {
        // Inject repository for DB-configurable thresholds
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public RiskSharingResult evaluate(BigDecimal frozenRate, BigDecimal currentRate,
                                      CurrencyType currencyType) {
        // 1. Calculate fluctuation percentage: |currentRate - frozenRate| / frozenRate * 100
        // 2. Load threshold from DB via riskSharingRepository.findByCurrencyType(currencyType)
        // 3. If fluctuation <= threshold -> PLATFORM_ABSORBS, effective rate = frozenRate
        // 4. If fluctuation > threshold:
        //    - FIAT:   SPLIT_50_50, effective rate = midpoint between frozen and current
        //    - CRYPTO: PLAYER_BEARS, effective rate = currentRate (with UI confirmation flag)
        // 5. Return RiskSharingResult with outcome, effectiveRate, platformCost, playerCost
        throw new UnsupportedOperationException("Stub");
    }
}
```

### B.7 FX Risk Sharing Repository (DB-configurable)

```java
package com.funding.currencyengine.service;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repository for FX risk sharing configuration.
 * These thresholds are DB-configurable -- never hardcoded.
 */
public interface FXRiskSharingRepository extends JpaRepository<FXRiskSharingConfig, Long> {

    Optional<FXRiskSharingConfig> findByCurrencyType(CurrencyType currencyType);
}
```

### B.8 Exchange Rate / Frozen Rate / Risk Sharing DTOs

```java
package com.funding.currencyengine.service;

import java.math.BigDecimal;
import java.time.Instant;

/** Immutable exchange rate value. */
public record ExchangeRate(
    Currency from,
    Currency to,
    BigDecimal rate,
    Instant fetchedAt,
    String source          // e.g., "coinGecko", "ecb", "frozen:abc123"
) {}

/** A rate frozen for a specific time window. */
public record FrozenRate(
    String freezeId,       // UUID
    Currency from,
    Currency to,
    BigDecimal rate,
    Instant frozenAt,
    Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}

/** Result of FX risk sharing evaluation. */
public record RiskSharingResult(
    RiskSharingOutcome outcome,    // PLATFORM_ABSORBS, SPLIT_50_50, PLAYER_BEARS
    BigDecimal effectiveRate,      // The rate actually applied to the transaction
    BigDecimal platformCost,       // Amount platform absorbs (in target currency)
    BigDecimal playerCost,         // Amount player absorbs (in target currency)
    BigDecimal fluctuationPercent  // Actual fluctuation percentage
) {}

/** Risk sharing outcome types. */
public enum RiskSharingOutcome {
    PLATFORM_ABSORBS,
    SPLIT_50_50,
    PLAYER_BEARS
}

/** Currency classification for risk sharing rule selection. */
public enum CurrencyType {
    FIAT,
    CRYPTO
}
```

### B.9 FX Risk Sharing Config Entity

```java
package com.funding.currencyengine.service;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * DB-configurable FX risk sharing parameters.
 * Table: fx_risk_sharing_config
 */
@Entity
@Table(name = "fx_risk_sharing_config")
public class FXRiskSharingConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_type", nullable = false, unique = true)
    private CurrencyType currencyType;

    /** Fluctuation threshold percentage below which platform absorbs. */
    @Column(name = "absorption_threshold_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal absorptionThresholdPercent;  // e.g., 0.50 for fiat, 2.00 for crypto

    /** Split ratio for fluctuation above threshold. 50 means 50/50. */
    @Column(name = "split_ratio_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal splitRatioPercent;            // e.g., 50.00

    // Getters (no setters -- update via repository)
    public CurrencyType getCurrencyType() { throw new UnsupportedOperationException("Stub"); }
    public BigDecimal getAbsorptionThresholdPercent() { throw new UnsupportedOperationException("Stub"); }
    public BigDecimal getSplitRatioPercent() { throw new UnsupportedOperationException("Stub"); }
}
```

### B.10 Blockchain Monitor

```java
package com.funding.currencyengine.crypto;

import com.funding.currencyengine.precision.CryptoCurrency;

/**
 * Monitors blockchain networks for incoming deposits.
 *
 * Supported chains: BTC, ETH (including ERC-20 USDT)
 *
 * Confirmation thresholds:
 *   - BTC:  >=3 confirmations  -> proceed to AML check
 *   - ETH:  >=12 confirmations -> proceed to AML check
 *   - USDT: >=12 confirmations -> proceed to AML check (ERC-20 on Ethereum)
 *
 * Below threshold: deposit status = PENDING, wallet balance NOT updated.
 * At/above threshold: triggers AML screening flow.
 */
public interface BlockchainMonitor {

    /**
     * Start monitoring for deposits to the given address.
     * Called after WalletAddressGenerator creates a new address.
     */
    void watchAddress(String address, CryptoCurrency currency, String playerId, String depositId);

    /**
     * Process a confirmation update from the blockchain node/service.
     * Invoked by a listener (webhook or polling).
     *
     * @param txHash blockchain transaction hash
     * @param confirmations current number of confirmations
     */
    void handleConfirmationUpdate(String txHash, int confirmations);
}
```

```java
package com.funding.currencyengine.crypto;

import com.funding.currencyengine.precision.CryptoCurrency;
import com.funding.currencyengine.precision.CryptoAmount;
import com.funding.currencyengine.service.FXRateManager;
import org.springframework.stereotype.Service;

@Service
public class BlockchainMonitorImpl implements BlockchainMonitor {

    private final CryptoDepositRepository cryptoDepositRepository;
    private final AmlScreeningClient amlScreeningClient;
    private final FXRateManager fxRateManager;
    private final EddTriggerService eddTriggerService;
    private final WalletCreditService walletCreditService;

    public BlockchainMonitorImpl(CryptoDepositRepository cryptoDepositRepository,
                                 AmlScreeningClient amlScreeningClient,
                                 FXRateManager fxRateManager,
                                 EddTriggerService eddTriggerService,
                                 WalletCreditService walletCreditService) {
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public void watchAddress(String address, CryptoCurrency currency,
                             String playerId, String depositId) {
        // 1. Register address in monitoring table
        // 2. Start monitoring via blockchain node RPC or third-party service
        throw new UnsupportedOperationException("Stub");
    }

    @Override
    public void handleConfirmationUpdate(String txHash, int confirmations) {
        // 1. Look up deposit by txHash
        // 2. Update confirmation count
        // 3. If confirmations < currency.getConfirmationThreshold() -> status = PENDING, return
        // 4. If confirmations >= threshold AND status is still PENDING:
        //    a. Transition to AML_CHECK
        //    b. Call amlScreeningClient.screenAddress(senderAddress)
        //    c. If HIGH_RISK -> status = REJECTED, log for compliance, return
        //    d. If PASS:
        //       i.  Convert CryptoAmount to Money via fxRateManager
        //       ii. Check if fiat equivalent > $10K -> trigger EDD
        //       iii. If EDD required -> status = EDD_REVIEW, return
        //       iv. Credit player's CASH wallet
        //       v.  Status = CREDITED
        //       vi. Store both CryptoAmount and converted Money in crypto_transactions
        throw new UnsupportedOperationException("Stub");
    }
}
```

### B.11 Wallet Address Generator

```java
package com.funding.currencyengine.crypto;

import com.funding.currencyengine.precision.CryptoCurrency;

/**
 * Generates unique deposit addresses per player per transaction.
 * Each address maps to exactly one (playerId, depositId) pair.
 *
 * Implementation options:
 *   - HD wallet derivation (BIP-32/44) for BTC
 *   - CREATE2 or nonce-based for ETH/ERC-20
 *   - Third-party custody API (e.g., Fireblocks, BitGo)
 */
public interface WalletAddressGenerator {

    /**
     * Generate a unique deposit address for the given player and transaction.
     *
     * @param playerId the player receiving the deposit
     * @param depositId unique identifier for this deposit transaction
     * @param currency the cryptocurrency type
     * @return a unique, valid blockchain address for the specified currency
     */
    String generateDepositAddress(String playerId, String depositId, CryptoCurrency currency);
}
```

### B.12 AML Screening Client

```java
package com.funding.currencyengine.crypto;

/**
 * Integration point for AML (Anti-Money Laundering) screening.
 * Screens wallet addresses against risk databases.
 *
 * Provider options: Elliptic, TRM Labs, Chainalysis
 *
 * Risk levels:
 *   - LOW:       proceed normally
 *   - MEDIUM:    proceed with enhanced monitoring
 *   - HIGH:      reject deposit (mixers, darknet, sanctioned wallets)
 *
 * This is an integration point -- the actual provider is injected at runtime.
 */
public interface AmlScreeningClient {

    /**
     * Screen a wallet address for AML risk.
     *
     * @param address the blockchain wallet address to screen
     * @param currency the cryptocurrency type (for chain-specific screening)
     * @return screening result with risk level and details
     */
    AmlScreeningResult screenAddress(String address, CryptoCurrency currency);
}
```

```java
package com.funding.currencyengine.crypto;

/** Result of AML screening for a wallet address. */
public record AmlScreeningResult(
    AmlRiskLevel riskLevel,
    String address,
    String provider,       // e.g., "elliptic", "chainalysis"
    String details,        // Human-readable risk description
    boolean sanctioned     // true if address is on a sanctions list
) {}

public enum AmlRiskLevel {
    LOW,
    MEDIUM,
    HIGH
}
```

### B.13 EDD Trigger Service

```java
package com.funding.currencyengine.crypto;

import java.math.BigDecimal;

/**
 * Triggers Enhanced Due Diligence (EDD) for deposits exceeding the threshold.
 * Threshold: $10,000 USD equivalent (DB-configurable).
 */
public interface EddTriggerService {

    /** Default threshold in USD. DB-configurable. */
    BigDecimal DEFAULT_EDD_THRESHOLD_USD = new BigDecimal("10000.0000");

    /**
     * Check if the deposit amount triggers EDD.
     *
     * @param fiatEquivalentUsd the deposit amount converted to USD
     * @return true if EDD is required
     */
    boolean requiresEdd(BigDecimal fiatEquivalentUsd);

    /**
     * Initiate the EDD workflow for a large deposit.
     *
     * @param depositId the deposit being reviewed
     * @param playerId the player making the deposit
     * @param fiatEquivalentUsd the deposit amount in USD
     */
    void triggerEddReview(String depositId, String playerId, BigDecimal fiatEquivalentUsd);
}
```

### B.14 Crypto Deposit Entity and Repository

```java
package com.funding.currencyengine.crypto;

import com.funding.currencyengine.precision.CryptoCurrency;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Tracks crypto deposit lifecycle from detection to credit.
 * Stores BOTH the original CryptoAmount AND the converted Money for audit.
 *
 * Status flow: DETECTED -> PENDING -> AML_CHECK -> CREDITED
 *                                              \-> REJECTED (high-risk)
 *                                   -> EDD_REVIEW -> CREDITED | REJECTED
 */
@Entity
@Table(name = "crypto_deposits")
public class CryptoDeposit {

    @Id
    @Column(name = "deposit_id", nullable = false, length = 36)
    private String depositId;

    @Column(name = "player_id", nullable = false)
    private String playerId;

    @Column(name = "deposit_address", nullable = false)
    private String depositAddress;

    @Column(name = "sender_address")
    private String senderAddress;

    @Column(name = "tx_hash")
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "crypto_currency", nullable = false)
    private CryptoCurrency cryptoCurrency;

    /** Original crypto amount -- NUMERIC (no fixed scale) for native precision. */
    @Column(name = "crypto_amount", columnDefinition = "NUMERIC", nullable = false)
    private BigDecimal cryptoAmount;

    /** Converted fiat amount -- DECIMAL(19,4) standard Money storage. */
    @Column(name = "fiat_amount", precision = 19, scale = 4)
    private BigDecimal fiatAmount;

    @Column(name = "fiat_currency", length = 3)
    private String fiatCurrency;

    /** Exchange rate used for conversion (frozen rate). */
    @Column(name = "exchange_rate", precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "confirmations", nullable = false)
    private int confirmations;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CryptoDepositStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Getters only -- updates via repository methods
}

public enum CryptoDepositStatus {
    DETECTED,
    PENDING,
    AML_CHECK,
    EDD_REVIEW,
    CREDITED,
    REJECTED
}
```

```java
package com.funding.currencyengine.crypto;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CryptoDepositRepository extends JpaRepository<CryptoDeposit, String> {

    Optional<CryptoDeposit> findByTxHash(String txHash);
    Optional<CryptoDeposit> findByDepositAddress(String depositAddress);
}
```

### B.15 Wallet Credit Service (Cross-module integration point)

```java
package com.funding.currencyengine.crypto;

import com.funding.currencyengine.precision.Money;

/**
 * Integration point to credit a player's CASH wallet after crypto deposit clears.
 * This delegates to the wallet-core module's WalletService.
 * Defined here as an interface to avoid circular dependency.
 */
public interface WalletCreditService {

    /**
     * Credit the player's CASH wallet with the converted fiat amount.
     *
     * @param playerId the player to credit
     * @param amount the fiat amount to credit
     * @param depositId reference to the originating crypto deposit
     */
    void creditCashWallet(String playerId, Money amount, String depositId);
}
```

---

## Part C -- Database Migrations

### C.1 Flyway Migration: Crypto Deposits Table

```sql
-- V6_001__create_crypto_deposits.sql

CREATE TABLE crypto_deposits (
    deposit_id       VARCHAR(36)   PRIMARY KEY,
    player_id        VARCHAR(36)   NOT NULL,
    deposit_address  VARCHAR(128)  NOT NULL,
    sender_address   VARCHAR(128),
    tx_hash          VARCHAR(128)  UNIQUE,
    crypto_currency  VARCHAR(10)   NOT NULL,
    crypto_amount    NUMERIC       NOT NULL,        -- Native precision, no fixed scale
    fiat_amount      DECIMAL(19,4),                 -- Post-conversion Money
    fiat_currency    VARCHAR(3),
    exchange_rate    DECIMAL(19,8),                  -- Rate used for conversion
    confirmations    INTEGER       NOT NULL DEFAULT 0,
    status           VARCHAR(20)   NOT NULL DEFAULT 'DETECTED',
    rejection_reason TEXT,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_crypto_currency CHECK (crypto_currency IN ('BTC', 'ETH', 'USDT')),
    CONSTRAINT chk_status CHECK (status IN ('DETECTED','PENDING','AML_CHECK','EDD_REVIEW','CREDITED','REJECTED'))
);

CREATE INDEX idx_crypto_deposits_player ON crypto_deposits(player_id);
CREATE INDEX idx_crypto_deposits_address ON crypto_deposits(deposit_address);
CREATE INDEX idx_crypto_deposits_status ON crypto_deposits(status);
```

### C.2 Flyway Migration: FX Risk Sharing Config Table

```sql
-- V6_002__create_fx_risk_sharing_config.sql

CREATE TABLE fx_risk_sharing_config (
    id                            BIGSERIAL     PRIMARY KEY,
    currency_type                 VARCHAR(10)   NOT NULL UNIQUE,
    absorption_threshold_percent  DECIMAL(5,2)  NOT NULL,
    split_ratio_percent           DECIMAL(5,2)  NOT NULL,

    CONSTRAINT chk_currency_type CHECK (currency_type IN ('FIAT', 'CRYPTO'))
);

-- Seed default values (SSOT from plan)
INSERT INTO fx_risk_sharing_config (currency_type, absorption_threshold_percent, split_ratio_percent)
VALUES
    ('FIAT',   0.50, 50.00),   -- Fiat: absorb <=0.5%, split 50/50 above
    ('CRYPTO', 2.00,  0.00);   -- Crypto: absorb <=2%, player bears 100% above
```

### C.3 Flyway Migration: Deposit Address Mapping Table

```sql
-- V6_003__create_deposit_address_mapping.sql

CREATE TABLE deposit_address_mapping (
    address          VARCHAR(128)  PRIMARY KEY,
    player_id        VARCHAR(36)   NOT NULL,
    deposit_id       VARCHAR(36)   NOT NULL,
    crypto_currency  VARCHAR(10)   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_addr_crypto_currency CHECK (crypto_currency IN ('BTC', 'ETH', 'USDT'))
);

CREATE INDEX idx_addr_mapping_player ON deposit_address_mapping(player_id);
```

---

## Part D -- File Paths

```
currency-engine/
├── precision/
│   ├── CryptoAmount.java                   # Value object: native crypto precision
│   ├── CryptoCurrency.java                 # Enum: BTC(8), ETH(18), USDT(6)
│   └── MoneyTest.java                      # (test) Money arithmetic in currency context
│
├── service/
│   ├── FXRateManager.java                  # Interface: rate lookup + freeze
│   ├── FXRateManagerImpl.java              # Impl: caching (5min/30sec), freezing (15min)
│   ├── FXRateProvider.java                 # Interface: external FX API abstraction
│   ├── FXRateCache.java                    # Interface: Redis-backed rate cache
│   ├── FXRiskSharingEvaluator.java         # Interface: risk sharing evaluation
│   ├── FXRiskSharingEvaluatorImpl.java     # Impl: DB-configurable thresholds
│   ├── FXRiskSharingRepository.java        # JPA repository for config table
│   ├── FXRiskSharingConfig.java            # Entity: fx_risk_sharing_config
│   ├── ExchangeRate.java                   # Record: rate with metadata
│   ├── FrozenRate.java                     # Record: frozen rate with TTL
│   ├── RiskSharingResult.java              # Record: evaluation result
│   ├── RiskSharingOutcome.java             # Enum: PLATFORM_ABSORBS / SPLIT_50_50 / PLAYER_BEARS
│   ├── CurrencyType.java                   # Enum: FIAT / CRYPTO
│   ├── FXRateManagerTest.java              # (test) 5 tests: caching, freezing, risk sharing
│   └── FXRateCacheIntegrationTest.java     # (test) Redis TTL verification
│
├── crypto/
│   ├── BlockchainMonitor.java              # Interface: address watching + confirmation handling
│   ├── BlockchainMonitorImpl.java          # Impl: confirmation threshold + AML + EDD flow
│   ├── WalletAddressGenerator.java         # Interface: unique address generation
│   ├── AmlScreeningClient.java             # Interface: AML screening integration point
│   ├── AmlScreeningResult.java             # Record: risk level + details
│   ├── AmlRiskLevel.java                   # Enum: LOW / MEDIUM / HIGH
│   ├── EddTriggerService.java              # Interface: Enhanced Due Diligence trigger
│   ├── WalletCreditService.java            # Interface: cross-module wallet credit
│   ├── CryptoDeposit.java                  # Entity: crypto_deposits table
│   ├── CryptoDepositStatus.java            # Enum: deposit lifecycle states
│   ├── CryptoDepositRepository.java        # JPA repository
│   ├── CryptocurrencyServiceTest.java      # (test) 7 tests: address gen, confirmations, AML, EDD
│   ├── CryptoAmountTest.java               # (test) Precision per currency, validation
│   └── BlockchainMonitorIntegrationTest.java  # (test) Full lifecycle integration
│
└── db/migration/
    ├── V6_001__create_crypto_deposits.sql
    ├── V6_002__create_fx_risk_sharing_config.sql
    └── V6_003__create_deposit_address_mapping.sql
```

---

## Part E -- Dependencies

| Dependency | Source | What This Section Needs |
|------------|--------|------------------------|
| **section-01-foundation** | Money value object | `Money` class, `BigDecimal` conventions, `RoundingMode.HALF_UP`, scale=4 |
| **section-01-foundation** | Gradle config | Spring Boot 3.x parent, JPA, Redis, Testcontainers dependencies |
| **section-01-foundation** | DB migrations | Flyway setup, PostgreSQL Testcontainer base config |
| **section-01-foundation** | Test infrastructure | Testcontainers base class, AssertJ, Mockito configuration |
| **section-01-foundation** | Base entity | `@MappedSuperclass` with `createdAt`/`updatedAt` audit fields |

No dependency on section-02 through section-05. This section is parallelizable with section-02 (wallet-core) after section-01 completes.

---

## Appendix: Key Invariants Relevant to This Section

| Invariant | Description | Enforced By |
|-----------|-------------|-------------|
| **INV-4** | 10,000 sequential transactions accumulate <=0.0001 rounding error | Money scale=4, ROUND_HALF_UP, BigDecimal from String only |
| **INV-9** | Crypto deposit credited only after confirmation threshold met | BlockchainMonitorImpl confirmation check before AML/credit flow |
