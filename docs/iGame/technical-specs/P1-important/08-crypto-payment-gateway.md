# P1-08: Crypto Payment Gateway

**Version**: 1.0.0
**Status**: Draft
**Last Updated**: 2026-01-23
**Owner**: iGaming Platform Team
**Related Documents**: [P0-01 (Ledger)](../P0-critical/01-double-entry-ledger-schema.md), [P0-03 (Wallet)](../P0-critical/03-seamless-wallet-implementation.md), [P1-05 (Saga)](05-distributed-transaction-patterns.md), [P1-07 (Multi-Tenant)](07-multi-tenant-isolation.md)

---

## Table of Contents

1. [Background & Strategic Context](#1-background--strategic-context)
2. [Architecture Overview](#2-architecture-overview)
3. [HD Wallet Implementation (BIP32/BIP44)](#3-hd-wallet-implementation-bip32bip44)
4. [Exchange Rate Management](#4-exchange-rate-management)
5. [Cold Wallet Security Architecture](#5-cold-wallet-security-architecture)
6. [Database Schema](#6-database-schema)
7. [Implementation Details (SmartAdmin)](#7-implementation-details-smartadmin)
8. [Integration Points](#8-integration-points)
9. [Testing Strategy](#9-testing-strategy)
10. [Operations & Monitoring](#10-operations--monitoring)
11. [Appendices](#11-appendices)

---

## 1. Background & Strategic Context

### 1.1 Strategic Importance

**From igame_str.md (First Principles)**:
- **Zero Friction (摩擦)**: Crypto deposits complete in minutes vs days for fiat
- **Global Reach**: No geographic restrictions (50+ countries)
- **High-Value Players**: Crypto users spend 3× more than fiat players
- **Code Leverage**: 1 HD Wallet implementation → infinite addresses → zero marginal cost

**Business Metrics**:
- **Target**: 40% of deposits via crypto (Bitcoin 60%, Ethereum 30%, others 10%)
- **Average Crypto Deposit**: $500 vs $150 fiat
- **Confirmation Time**: BTC 60 min (6 confirmations), ETH 3 min (12 confirmations)
- **Cost Savings**: 0.1% crypto fees vs 2.5-3.5% card processing fees

### 1.2 Technical Challenges

1. **Security**:
   - Hot wallet (online) vs cold wallet (offline) segregation
   - Private key protection (HSM, multi-signature)
   - Address reuse prevention (privacy)

2. **Exchange Rate Volatility**:
   - Real-time USD conversion
   - Slippage protection (±2% tolerance)
   - Localized rates per region

3. **Blockchain Complexity**:
   - Different confirmation requirements (BTC 6, ETH 12)
   - Fee estimation (dynamic gas prices)
   - Orphaned blocks, chain reorganization

4. **Multi-Tenant Isolation**:
   - Segregated wallets per merchant
   - Independent cold wallet per tenant (regulatory requirement)

### 1.3 Related Documents

- **P0-01 (Ledger)**: All crypto deposits/withdrawals recorded with exchange rate snapshot
- **P0-03 (Wallet)**: Multi-currency wallet supports BTC/ETH alongside USD/EUR
- **P1-05 (Saga)**: Crypto deposit/withdrawal sagas handle async blockchain confirmation
- **P1-07 (Multi-Tenant)**: Each merchant has isolated HD wallet hierarchy

---

## 2. Architecture Overview

### 2.1 System Components

```
┌─────────────────────────────────────────────────────────────────┐
│                      Crypto Payment Gateway                       │
└─────────────────────────────────────────────────────────────────┘
         │
         ├─► HD Wallet Manager (BIP32/BIP44)
         │   ├─ Address Generation (infinite addresses)
         │   ├─ Private Key Derivation (hierarchical)
         │   └─ Multi-Tenant Isolation (m/44'/0'/{tenant_index}')
         │
         ├─► Exchange Rate Service
         │   ├─ Real-Time Feed (CoinGecko API, 1s polling)
         │   ├─ Rate Caching (Redis, 10s TTL)
         │   └─ Slippage Protection (±2% tolerance)
         │
         ├─► Hot Wallet Service (Online)
         │   ├─ Immediate Deposits (<$1,000)
         │   ├─ Immediate Withdrawals (<$500)
         │   └─ Auto-Sweep to Cold Wallet (daily, >$5,000 threshold)
         │
         ├─► Cold Wallet Service (Offline)
         │   ├─ Manual Approval (>$1,000 deposits, >$500 withdrawals)
         │   ├─ Multi-Signature (2-of-3)
         │   └─ HSM Integration (AWS CloudHSM)
         │
         ├─► Blockchain Monitor
         │   ├─ Address Watching (Bitcoin Core RPC, Ethereum Geth)
         │   ├─ Confirmation Tracking (6 for BTC, 12 for ETH)
         │   └─ Orphaned Block Detection
         │
         └─► Transaction Reconciliation
             ├─ Daily Balance Verification (on-chain vs internal ledger)
             ├─ Missing Deposit Detection (scan last 1000 blocks)
             └─ Duplicate Deposit Prevention (idempotency)
```

### 2.2 Technology Stack

| Component | Technology | Rationale |
|-----------|-----------|-----------|
| Bitcoin Integration | Bitcoin Core 27.0 (RPC) | Full node, SPV too risky |
| Ethereum Integration | Geth 1.14.x (JSON-RPC) | Official client, most stable |
| HD Wallet Library | Web3j 4.12.0 (Java) | BIP32/BIP44 support |
| Exchange Rate Feed | CoinGecko API v3 | Free tier 50 calls/min |
| Private Key Storage | AWS CloudHSM (FIPS 140-2 Level 3) | Regulatory compliance |
| Multi-Signature | BitcoinJ 0.16.3 (P2SH) | Multi-sig transaction building |

### 2.3 Security Boundaries

```
┌─────────────────┐
│   Hot Wallet    │ <── Online, immediate operations
│   (Online)      │     Max: $10,000 total
└─────────────────┘
        │
        │ Daily Sweep (>$5,000)
        ▼
┌─────────────────┐
│  Cold Wallet    │ <── Offline, manual approval
│  (Offline)      │     Multi-signature (2-of-3)
└─────────────────┘     HSM-protected keys
```

**Security Thresholds**:
- Hot Wallet Max: $10,000 total across all cryptocurrencies
- Auto-Approval Deposit: <$1,000
- Auto-Approval Withdrawal: <$500
- Cold Wallet Transfer: Requires 2-of-3 multi-signature approval

---

## 3. HD Wallet Implementation (BIP32/BIP44)

### 3.1 BIP44 Derivation Path

**Standard**: BIP44 (Multi-Account Hierarchy for Deterministic Wallets)

```
m / purpose' / coin_type' / account' / change / address_index

Example paths:
- Bitcoin Tenant #1:  m/44'/0'/0'/0/0  (first deposit address)
- Bitcoin Tenant #1:  m/44'/0'/0'/0/1  (second deposit address)
- Bitcoin Tenant #2:  m/44'/0'/1'/0/0  (first deposit address for different merchant)
- Ethereum Tenant #1: m/44'/60'/0'/0/0 (ETH uses coin_type 60)
```

**Multi-Tenant Isolation**:
- `account'` level = Tenant ID mapping
- Each tenant gets independent HD wallet hierarchy
- **Critical**: Tenants cannot derive each other's addresses

### 3.2 Master Seed Generation

**Initial Setup (One-Time)**:
```java
// Master seed generation (24-word mnemonic)
SecureRandom secureRandom = new SecureRandom();
byte[] entropy = new byte[32];  // 256 bits
secureRandom.nextBytes(entropy);

MnemonicCode mnemonicCode = new MnemonicCode();
List<String> mnemonicWords = mnemonicCode.toMnemonic(entropy);
// Example: "abandon ability able about above absent absorb abstract absurd abuse access accident..."

// Convert mnemonic to seed
byte[] seed = MnemonicCode.toSeed(mnemonicWords, "optional_passphrase");

// CRITICAL: Store mnemonic in AWS Secrets Manager
// NEVER store in database or code
SecretsManagerClient secretsClient = SecretsManagerClient.create();
secretsClient.putSecretValue(
    PutSecretValueRequest.builder()
        .secretId("igaming/master-seed-mnemonic")
        .secretString(String.join(" ", mnemonicWords))
        .build()
);
```

**Security Measures**:
1. Master seed stored in AWS Secrets Manager (encrypted at rest)
2. Access requires IAM role with MFA
3. Seed rotation every 90 days (gradual migration)
4. Backup mnemonic split across 3 physical locations (Shamir's Secret Sharing)

### 3.3 Address Generation Service

**Interface**:
```java
package net.lab1024.sa.base.module.support.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Bip32ECKeyPair;
import org.web3j.crypto.MnemonicUtils;

import java.math.BigInteger;

@Service
@RequiredArgsConstructor
public class HdWalletService {

    private final CryptoConfigDao cryptoConfigDao;
    private final CryptoAddressDao cryptoAddressDao;
    private final RedissonClient redisson;

    /**
     * Generate new deposit address for player
     *
     * @param playerId Player ID
     * @param currencyCode BTC or ETH
     * @return Deposit address (Bitcoin: starts with 1/3/bc1, Ethereum: 0x...)
     */
    public String generateDepositAddress(Long playerId, String currencyCode) {
        String tenantId = TenantContextHolder.getTenantId();

        // 1. Get tenant's HD wallet configuration
        CryptoConfig config = cryptoConfigDao.selectOne(
            new LambdaQueryWrapper<CryptoConfig>()
                .eq(CryptoConfig::getTenantId, tenantId)
                .eq(CryptoConfig::getCurrencyCode, currencyCode)
        );

        // 2. Increment address index (atomic operation)
        String addressIndexKey = String.format("crypto:address_index:%s:%s", tenantId, currencyCode);
        RAtomicLong addressIndex = redisson.getAtomicLong(addressIndexKey);
        long nextIndex = addressIndex.incrementAndGet();

        // 3. Derive address from master seed
        String address = deriveAddress(config.getAccountIndex(), nextIndex, currencyCode);

        // 4. Store address in database
        CryptoAddress cryptoAddress = new CryptoAddress();
        cryptoAddress.setTenantId(tenantId);
        cryptoAddress.setPlayerId(playerId);
        cryptoAddress.setCurrencyCode(currencyCode);
        cryptoAddress.setAddress(address);
        cryptoAddress.setAddressIndex(nextIndex);
        cryptoAddress.setDerivationPath(buildDerivationPath(config.getAccountIndex(), nextIndex, currencyCode));
        cryptoAddress.setStatus(AddressStatus.ACTIVE);

        cryptoAddressDao.insert(cryptoAddress);

        log.info("Generated deposit address: player={}, currency={}, address={}, derivationPath={}",
            playerId, currencyCode, address, cryptoAddress.getDerivationPath());

        return address;
    }

    private String deriveAddress(int accountIndex, long addressIndex, String currencyCode) {
        // Load master seed from AWS Secrets Manager
        String mnemonic = loadMasterSeedMnemonic();
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, "");

        // BIP44 derivation path
        int[] path = buildDerivationPathArray(accountIndex, addressIndex, currencyCode);

        // Derive key pair
        Bip32ECKeyPair masterKeyPair = Bip32ECKeyPair.generateKeyPair(seed);
        Bip32ECKeyPair derivedKeyPair = Bip32ECKeyPair.deriveKeyPair(masterKeyPair, path);

        // Generate address based on currency
        if ("BTC".equals(currencyCode)) {
            return generateBitcoinAddress(derivedKeyPair);
        } else if ("ETH".equals(currencyCode)) {
            return generateEthereumAddress(derivedKeyPair);
        }

        throw new IllegalArgumentException("Unsupported currency: " + currencyCode);
    }

    private String generateBitcoinAddress(Bip32ECKeyPair keyPair) {
        // P2PKH address (starts with 1)
        byte[] publicKey = keyPair.getPublicKey().toByteArray();
        byte[] publicKeyHash = Hash.sha256hash160(publicKey);

        byte[] addressBytes = new byte[1 + publicKeyHash.length];
        addressBytes[0] = 0x00;  // Mainnet prefix
        System.arraycopy(publicKeyHash, 0, addressBytes, 1, publicKeyHash.length);

        return Base58.encodeChecked(addressBytes);
    }

    private String generateEthereumAddress(Bip32ECKeyPair keyPair) {
        // Ethereum address = last 20 bytes of Keccak-256(public key)
        BigInteger publicKey = keyPair.getPublicKey();
        byte[] publicKeyBytes = Numeric.toBytesPadded(publicKey, 64);

        byte[] hash = Hash.sha3(publicKeyBytes);
        byte[] addressBytes = Arrays.copyOfRange(hash, hash.length - 20, hash.length);

        return "0x" + Numeric.toHexStringNoPrefix(addressBytes);
    }

    private int[] buildDerivationPathArray(int accountIndex, long addressIndex, String currencyCode) {
        int coinType = "BTC".equals(currencyCode) ? 0 : 60;  // BTC=0, ETH=60

        return new int[] {
            44 | Bip32ECKeyPair.HARDENED_BIT,         // purpose'
            coinType | Bip32ECKeyPair.HARDENED_BIT,   // coin_type'
            accountIndex | Bip32ECKeyPair.HARDENED_BIT, // account'
            0,                                         // change (0 = external/deposit)
            (int) addressIndex                        // address_index
        };
    }

    private String buildDerivationPath(int accountIndex, long addressIndex, String currencyCode) {
        int coinType = "BTC".equals(currencyCode) ? 0 : 60;
        return String.format("m/44'/%d'/%d'/0/%d", coinType, accountIndex, addressIndex);
    }
}
```

### 3.4 Address Reuse Prevention

**Privacy Concern**: Reusing addresses allows blockchain analysis to link transactions

**Solution**:
1. **One address per deposit**: Generate new address for each deposit request
2. **Address expiration**: Mark unused addresses as expired after 7 days
3. **Address retirement**: Mark addresses as used after first deposit

```java
@Scheduled(cron = "0 0 2 * * ?")  // Daily 2 AM
public void expireUnusedAddresses() {
    LocalDateTime expirationThreshold = LocalDateTime.now().minusDays(7);

    cryptoAddressDao.update(
        new LambdaUpdateWrapper<CryptoAddress>()
            .set(CryptoAddress::getStatus, AddressStatus.EXPIRED)
            .eq(CryptoAddress::getStatus, AddressStatus.ACTIVE)
            .isNull(CryptoAddress::getFirstDepositAt)
            .lt(CryptoAddress::getCreatedAt, expirationThreshold)
    );
}
```

---

## 4. Exchange Rate Management

### 4.1 Real-Time Exchange Rate Feed

**Provider**: CoinGecko API v3 (Free tier: 50 calls/min)

```java
package net.lab1024.sa.base.module.support.crypto;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    private final RedissonClient redisson;
    private final ExchangeRateHistoryDao exchangeRateHistoryDao;

    private static final String COINGECKO_API_URL =
        "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=usd,eur,gbp";

    /**
     * Fetch latest exchange rates (runs every 10 seconds)
     */
    @Scheduled(fixedDelay = 10000)  // 10 seconds
    public void updateExchangeRates() {
        try {
            // 1. Fetch from CoinGecko API
            Map<String, Map<String, Double>> response = restTemplate.getForObject(
                COINGECKO_API_URL,
                Map.class
            );

            // 2. Extract rates
            BigDecimal btcUsd = BigDecimal.valueOf(response.get("bitcoin").get("usd"));
            BigDecimal ethUsd = BigDecimal.valueOf(response.get("ethereum").get("usd"));
            BigDecimal btcEur = BigDecimal.valueOf(response.get("bitcoin").get("eur"));
            BigDecimal ethEur = BigDecimal.valueOf(response.get("ethereum").get("eur"));

            // 3. Cache in Redis (10s TTL)
            RBucket<BigDecimal> btcUsdBucket = redisson.getBucket("exchange_rate:BTC:USD");
            btcUsdBucket.set(btcUsd, 10, TimeUnit.SECONDS);

            RBucket<BigDecimal> ethUsdBucket = redisson.getBucket("exchange_rate:ETH:USD");
            ethUsdBucket.set(ethUsd, 10, TimeUnit.SECONDS);

            // Similar for EUR, GBP...

            // 4. Store historical snapshot (for audit trail)
            ExchangeRateHistory history = new ExchangeRateHistory();
            history.setCurrencyPair("BTC/USD");
            history.setRate(btcUsd);
            history.setProvider("CoinGecko");
            history.setFetchedAt(LocalDateTime.now());
            exchangeRateHistoryDao.insert(history);

            log.debug("Exchange rates updated: BTC/USD={}, ETH/USD={}", btcUsd, ethUsd);

        } catch (Exception e) {
            log.error("Failed to update exchange rates", e);
            // Fallback to cached rates (stale data acceptable for <60s)
        }
    }

    /**
     * Get current exchange rate with slippage protection
     *
     * @param cryptoCurrency BTC, ETH
     * @param fiatCurrency USD, EUR
     * @return Exchange rate (e.g., 1 BTC = 42000 USD)
     */
    public BigDecimal getExchangeRate(String cryptoCurrency, String fiatCurrency) {
        String cacheKey = String.format("exchange_rate:%s:%s", cryptoCurrency, fiatCurrency);
        RBucket<BigDecimal> bucket = redisson.getBucket(cacheKey);

        BigDecimal rate = bucket.get();
        if (rate == null) {
            throw new ServiceException("Exchange rate not available for " + cryptoCurrency + "/" + fiatCurrency);
        }

        return rate;
    }

    /**
     * Convert crypto amount to fiat with slippage tolerance
     *
     * @param cryptoAmount Amount in crypto (e.g., 0.5 BTC)
     * @param cryptoCurrency BTC, ETH
     * @param fiatCurrency USD, EUR
     * @param slippageTolerance Percentage tolerance (e.g., 2.0 = ±2%)
     * @return Fiat amount with slippage range
     */
    public FiatConversionResult convertCryptoToFiat(
        BigDecimal cryptoAmount,
        String cryptoCurrency,
        String fiatCurrency,
        BigDecimal slippageTolerance
    ) {
        BigDecimal rate = getExchangeRate(cryptoCurrency, fiatCurrency);
        BigDecimal fiatAmount = cryptoAmount.multiply(rate);

        // Calculate slippage range
        BigDecimal slippageMultiplier = slippageTolerance.divide(BigDecimal.valueOf(100));
        BigDecimal minFiat = fiatAmount.multiply(BigDecimal.ONE.subtract(slippageMultiplier));
        BigDecimal maxFiat = fiatAmount.multiply(BigDecimal.ONE.add(slippageMultiplier));

        return FiatConversionResult.builder()
            .fiatAmount(fiatAmount)
            .minFiatAmount(minFiat)
            .maxFiatAmount(maxFiat)
            .exchangeRate(rate)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
```

### 4.2 Slippage Protection

**Problem**: Exchange rate changes between player initiating deposit and blockchain confirmation

**Solution**: Lock exchange rate at transaction initiation

```java
@Service
@RequiredArgsConstructor
public class CryptoDepositService {

    private final ExchangeRateService exchangeRateService;
    private final CryptoTransactionDao cryptoTransactionDao;

    @Transactional
    public CryptoDepositResponse initiateDeposit(CryptoDepositForm form) {
        String tenantId = TenantContextHolder.getTenantId();
        Long playerId = RequestUtils.getPlayerId();

        // 1. Lock exchange rate at initiation
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
            form.getCryptoCurrency(),
            "USD"
        );

        // 2. Calculate expected fiat amount
        BigDecimal expectedFiatAmount = form.getCryptoAmount().multiply(exchangeRate);

        // 3. Create transaction record with locked rate
        CryptoTransaction transaction = new CryptoTransaction();
        transaction.setTenantId(tenantId);
        transaction.setPlayerId(playerId);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setCryptoCurrency(form.getCryptoCurrency());
        transaction.setCryptoAmount(form.getCryptoAmount());
        transaction.setExchangeRate(exchangeRate);  // LOCKED RATE
        transaction.setFiatCurrency("USD");
        transaction.setExpectedFiatAmount(expectedFiatAmount);
        transaction.setStatus(CryptoTransactionStatus.PENDING);

        cryptoTransactionDao.insert(transaction);

        // 4. Generate deposit address
        String depositAddress = hdWalletService.generateDepositAddress(
            playerId,
            form.getCryptoCurrency()
        );

        return CryptoDepositResponse.builder()
            .transactionId(transaction.getId())
            .depositAddress(depositAddress)
            .cryptoAmount(form.getCryptoAmount())
            .expectedFiatAmount(expectedFiatAmount)
            .exchangeRate(exchangeRate)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();
    }

    /**
     * Process blockchain confirmation (called by blockchain monitor)
     */
    @Transactional
    public void processBlockchainConfirmation(String txHash, int confirmations) {
        CryptoTransaction transaction = cryptoTransactionDao.selectOne(
            new LambdaQueryWrapper<CryptoTransaction>()
                .eq(CryptoTransaction::getBlockchainTxHash, txHash)
        );

        // Use LOCKED exchange rate from transaction record
        // NOT the current exchange rate
        BigDecimal fiatAmount = transaction.getCryptoAmount()
            .multiply(transaction.getExchangeRate());

        // Credit player wallet (P0-03 integration)
        walletService.deposit(
            transaction.getPlayerId(),
            fiatAmount,
            "USD",
            "Crypto deposit: " + transaction.getCryptoCurrency()
        );
    }
}
```

### 4.3 Exchange Rate History Audit

**Regulatory Requirement**: MGA requires 7-year retention of exchange rates used

```sql
CREATE TABLE exchange_rate_history (
    id                  BIGSERIAL PRIMARY KEY,
    currency_pair       VARCHAR(10) NOT NULL,  -- BTC/USD, ETH/EUR
    rate                DECIMAL(20, 8) NOT NULL,
    provider            VARCHAR(50) NOT NULL,  -- CoinGecko, Coinbase
    fetched_at          TIMESTAMP NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_currency_pair_fetched (currency_pair, fetched_at DESC)
);

-- Partition by month for efficient archival
CREATE TABLE exchange_rate_history_2026_01 PARTITION OF exchange_rate_history
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
```

---

## 5. Cold Wallet Security Architecture

### 5.1 Hot Wallet vs Cold Wallet Segregation

**Hot Wallet (Online)**:
- **Purpose**: Immediate deposits (<$1,000), immediate withdrawals (<$500)
- **Max Balance**: $10,000 total across all cryptocurrencies
- **Key Storage**: AWS CloudHSM (FIPS 140-2 Level 3)
- **Auto-Sweep**: Daily to cold wallet when balance > $5,000

**Cold Wallet (Offline)**:
- **Purpose**: Large deposits (>$1,000), large withdrawals (>$500)
- **Key Storage**: Hardware wallet (Ledger Nano X) in bank safe deposit box
- **Approval**: 2-of-3 multi-signature (CFO, CTO, Security Officer)
- **Access**: Air-gapped computer, never connected to internet

```
┌──────────────────────────────────────────────────────────────┐
│                        Hot Wallet                            │
│  - Online, automated operations                              │
│  - Max balance: $10,000                                      │
│  - Private keys in AWS CloudHSM                              │
└──────────────────────────────────────────────────────────────┘
                    │
                    │ Daily Sweep (if balance > $5,000)
                    ▼
┌──────────────────────────────────────────────────────────────┐
│                       Cold Wallet                            │
│  - Offline, manual approval                                  │
│  - Multi-signature 2-of-3                                    │
│  - Hardware wallet in bank safe                              │
└──────────────────────────────────────────────────────────────┘
```

### 5.2 Multi-Signature Implementation

**Bitcoin P2SH (Pay-to-Script-Hash)**:

```java
package net.lab1024.sa.base.module.support.crypto;

import org.bitcoinj.core.*;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptBuilder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ColdWalletService {

    /**
     * Create 2-of-3 multi-signature address
     *
     * @param publicKeys Public keys of CFO, CTO, Security Officer
     * @return P2SH address (starts with 3)
     */
    public String createMultiSigAddress(List<ECKey> publicKeys) {
        if (publicKeys.size() != 3) {
            throw new IllegalArgumentException("Requires exactly 3 public keys");
        }

        // Create redeem script: 2-of-3 multi-sig
        Script redeemScript = ScriptBuilder.createMultiSigOutputScript(2, publicKeys);

        // Generate P2SH address from redeem script
        Address p2shAddress = Address.fromP2SHScript(
            NetworkParameters.fromID(NetworkParameters.ID_MAINNET),
            redeemScript
        );

        log.info("Created multi-sig address: {}", p2shAddress);
        return p2shAddress.toString();
    }

    /**
     * Sign withdrawal transaction (requires 2-of-3 signatures)
     *
     * @param unsignedTx Unsigned transaction
     * @param privateKey Signer's private key (from hardware wallet)
     * @return Partially signed transaction
     */
    public Transaction signWithdrawalTransaction(Transaction unsignedTx, ECKey privateKey) {
        // Sign all inputs
        for (int i = 0; i < unsignedTx.getInputs().size(); i++) {
            TransactionInput input = unsignedTx.getInput(i);
            Script redeemScript = getRedeemScript(input.getConnectedOutput().getScriptPubKey());

            TransactionSignature signature = unsignedTx.calculateSignature(
                i,
                privateKey,
                redeemScript,
                Transaction.SigHash.ALL,
                false
            );

            // Add signature to script
            ScriptBuilder scriptBuilder = new ScriptBuilder();
            scriptBuilder.data(signature.encodeToBitcoin());
            scriptBuilder.data(privateKey.getPubKey());
            scriptBuilder.data(redeemScript.getProgram());

            input.setScriptSig(scriptBuilder.build());
        }

        log.info("Signed transaction: {} (1-of-2 signatures)", unsignedTx.getTxId());
        return unsignedTx;
    }

    /**
     * Verify transaction has 2-of-3 signatures before broadcast
     */
    public boolean verifyMultiSigTransaction(Transaction tx) {
        for (TransactionInput input : tx.getInputs()) {
            Script scriptSig = input.getScriptSig();
            int signatureCount = countSignatures(scriptSig);

            if (signatureCount < 2) {
                log.error("Insufficient signatures: {} (requires 2)", signatureCount);
                return false;
            }
        }
        return true;
    }
}
```

### 5.3 Daily Sweep from Hot to Cold Wallet

```java
@Scheduled(cron = "0 0 3 * * ?")  // Daily 3 AM
@Transactional
public void sweepHotToColdWallet() {
    List<CryptoConfig> configs = cryptoConfigDao.selectList(
        new LambdaQueryWrapper<CryptoConfig>()
            .eq(CryptoConfig::getWalletType, WalletType.HOT)
    );

    for (CryptoConfig config : configs) {
        BigDecimal hotWalletBalance = getHotWalletBalance(config.getCurrencyCode());
        BigDecimal sweepThreshold = BigDecimal.valueOf(5000);  // $5,000 USD equivalent

        if (hotWalletBalance.compareTo(sweepThreshold) > 0) {
            // Calculate amount to sweep (leave $1,000 for immediate operations)
            BigDecimal sweepAmount = hotWalletBalance.subtract(BigDecimal.valueOf(1000));

            // Create manual approval task
            ColdWalletTransferTask task = new ColdWalletTransferTask();
            task.setTenantId(config.getTenantId());
            task.setCurrencyCode(config.getCurrencyCode());
            task.setAmount(sweepAmount);
            task.setFromAddress(config.getHotWalletAddress());
            task.setToAddress(config.getColdWalletAddress());
            task.setStatus(TransferTaskStatus.PENDING_APPROVAL);
            task.setRequiredSignatures(2);
            task.setReceivedSignatures(0);

            coldWalletTransferTaskDao.insert(task);

            // Send notification to approvers (CFO, CTO, Security Officer)
            notificationService.sendMultiSigApprovalRequest(task);

            log.warn("Hot wallet sweep initiated: currency={}, amount={}, taskId={}",
                config.getCurrencyCode(), sweepAmount, task.getId());
        }
    }
}
```

### 5.4 Regulatory Compliance

**Malta Gaming Authority (MGA) Requirements**:
1. **Segregated Cold Wallet per Tenant**: Each merchant must have independent cold wallet
2. **Multi-Signature Mandatory**: Minimum 2-of-3 for withdrawals >$500
3. **Audit Trail**: All private key access logged with timestamp, user, purpose
4. **Annual Penetration Test**: Third-party security audit of wallet infrastructure

**Implementation**:
```java
@Component
public class ColdWalletAuditLogger {

    @Around("execution(* ColdWalletService.*(..))")
    public Object logColdWalletAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();

        ColdWalletAuditLog auditLog = new ColdWalletAuditLog();
        auditLog.setTenantId(TenantContextHolder.getTenantId());
        auditLog.setUserId(RequestUtils.getUserId());
        auditLog.setMethod(method);
        auditLog.setParameters(JSON.toJSONString(args));
        auditLog.setAccessedAt(LocalDateTime.now());

        coldWalletAuditLogDao.insert(auditLog);

        return joinPoint.proceed();
    }
}
```

---

## 6. Database Schema

### 6.1 Crypto Configuration

```sql
CREATE TABLE crypto_config (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    currency_code           VARCHAR(10) NOT NULL,   -- BTC, ETH

    -- HD Wallet Configuration
    account_index           INT NOT NULL,            -- BIP44 account' index
    wallet_type             VARCHAR(10) NOT NULL,    -- HOT, COLD

    -- Hot Wallet
    hot_wallet_address      VARCHAR(100),
    hot_wallet_max_balance_usd  DECIMAL(20, 2) DEFAULT 10000,

    -- Cold Wallet
    cold_wallet_address     VARCHAR(100),
    cold_wallet_type        VARCHAR(20),             -- MULTI_SIG, SINGLE_SIG
    multi_sig_threshold     INT,                     -- 2 (for 2-of-3)
    multi_sig_total         INT,                     -- 3 (for 2-of-3)

    -- Transaction Limits
    auto_approve_deposit_max_usd    DECIMAL(20, 2) DEFAULT 1000,
    auto_approve_withdrawal_max_usd DECIMAL(20, 2) DEFAULT 500,

    -- Blockchain Configuration
    confirmation_required   INT NOT NULL,            -- BTC=6, ETH=12

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_crypto_config_tenant_currency UNIQUE (tenant_id, currency_code)
);

CREATE INDEX idx_crypto_config_tenant ON crypto_config(tenant_id);
```

### 6.2 Crypto Addresses

```sql
CREATE TABLE crypto_addresses (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    player_id               BIGINT NOT NULL,
    currency_code           VARCHAR(10) NOT NULL,

    address                 VARCHAR(100) NOT NULL,
    derivation_path         VARCHAR(100) NOT NULL,   -- m/44'/0'/0'/0/123
    address_index           BIGINT NOT NULL,

    status                  VARCHAR(20) NOT NULL,    -- ACTIVE, USED, EXPIRED
    first_deposit_at        TIMESTAMP,

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_crypto_addresses_address UNIQUE (address),
    CONSTRAINT fk_crypto_addresses_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_crypto_addresses_tenant_player ON crypto_addresses(tenant_id, player_id);
CREATE INDEX idx_crypto_addresses_status ON crypto_addresses(status);
```

### 6.3 Crypto Transactions

```sql
CREATE TABLE crypto_transactions (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    player_id               BIGINT NOT NULL,
    transaction_type        VARCHAR(20) NOT NULL,    -- DEPOSIT, WITHDRAWAL

    -- Crypto Details
    crypto_currency         VARCHAR(10) NOT NULL,
    crypto_amount           DECIMAL(30, 18) NOT NULL,
    from_address            VARCHAR(100),
    to_address              VARCHAR(100),

    -- Blockchain Details
    blockchain_tx_hash      VARCHAR(100),
    block_number            BIGINT,
    confirmations           INT NOT NULL DEFAULT 0,

    -- Fiat Conversion (locked at initiation)
    fiat_currency           VARCHAR(10) NOT NULL,
    exchange_rate           DECIMAL(20, 8) NOT NULL, -- Locked rate
    expected_fiat_amount    DECIMAL(20, 2) NOT NULL,
    actual_fiat_amount      DECIMAL(20, 2),

    -- Status & Timestamps
    status                  VARCHAR(20) NOT NULL,    -- PENDING, CONFIRMED, COMPLETED, FAILED
    initiated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    confirmed_at            TIMESTAMP,
    completed_at            TIMESTAMP,

    -- Integration
    wallet_transaction_id   BIGINT,                  -- Link to P0-03 wallet_transactions
    saga_id                 UUID,                    -- Link to P1-05 saga_instances

    CONSTRAINT fk_crypto_transactions_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_crypto_transactions_tenant_player ON crypto_transactions(tenant_id, player_id);
CREATE INDEX idx_crypto_transactions_blockchain_hash ON crypto_transactions(blockchain_tx_hash);
CREATE INDEX idx_crypto_transactions_status ON crypto_transactions(status);
```

### 6.4 Cold Wallet Transfer Tasks

```sql
CREATE TABLE cold_wallet_transfer_tasks (
    id                      BIGSERIAL PRIMARY KEY,
    tenant_id               VARCHAR(100) NOT NULL,
    currency_code           VARCHAR(10) NOT NULL,

    amount                  DECIMAL(30, 18) NOT NULL,
    from_address            VARCHAR(100) NOT NULL,
    to_address              VARCHAR(100) NOT NULL,

    -- Multi-Signature
    required_signatures     INT NOT NULL DEFAULT 2,
    received_signatures     INT NOT NULL DEFAULT 0,
    signer_1_user_id        BIGINT,
    signer_1_signed_at      TIMESTAMP,
    signer_2_user_id        BIGINT,
    signer_2_signed_at      TIMESTAMP,

    -- Transaction Details
    unsigned_tx_hex         TEXT,
    partially_signed_tx_hex TEXT,
    final_tx_hex            TEXT,
    blockchain_tx_hash      VARCHAR(100),

    status                  VARCHAR(20) NOT NULL,    -- PENDING_APPROVAL, PARTIALLY_SIGNED, COMPLETED, REJECTED

    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at             TIMESTAMP,
    broadcast_at            TIMESTAMP
);

CREATE INDEX idx_cold_wallet_tasks_tenant_status ON cold_wallet_transfer_tasks(tenant_id, status);
```

---

## 7. Implementation Details (SmartAdmin)

### 7.1 Layered Architecture

```
CryptoController (sa-admin/src/.../crypto/)
    ↓
CryptoService
    ↓
CryptoManager (@Transactional, @Cacheable)
    ↓
CryptoDao (MyBatis-Plus BaseMapper)
```

### 7.2 Controller Layer

```java
package net.lab1024.sa.admin.module.business.crypto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.domain.PageResult;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Tag(name = AdminSwaggerTagConst.Business.CRYPTO_PAYMENT)
@RestController
@RequestMapping("/api/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoService cryptoService;

    @Operation(summary = "Initiate crypto deposit")
    @PostMapping("/deposit/initiate")
    public ResponseDTO<CryptoDepositResponse> initiateDeposit(@Valid @RequestBody CryptoDepositForm form) {
        return ResponseDTO.ok(cryptoService.initiateDeposit(form));
    }

    @Operation(summary = "Get deposit address")
    @GetMapping("/deposit/address/{currencyCode}")
    public ResponseDTO<String> getDepositAddress(@PathVariable String currencyCode) {
        return ResponseDTO.ok(cryptoService.getDepositAddress(currencyCode));
    }

    @Operation(summary = "Initiate crypto withdrawal")
    @PostMapping("/withdrawal/initiate")
    public ResponseDTO<CryptoWithdrawalResponse> initiateWithdrawal(@Valid @RequestBody CryptoWithdrawalForm form) {
        return ResponseDTO.ok(cryptoService.initiateWithdrawal(form));
    }

    @Operation(summary = "Query transaction history")
    @PostMapping("/transaction/query")
    public ResponseDTO<PageResult<CryptoTransactionVO>> queryTransactions(
        @Valid @RequestBody CryptoTransactionQueryForm form
    ) {
        return ResponseDTO.ok(cryptoService.queryTransactions(form));
    }

    @Operation(summary = "Get exchange rate")
    @GetMapping("/exchange-rate/{cryptoCurrency}/{fiatCurrency}")
    public ResponseDTO<ExchangeRateVO> getExchangeRate(
        @PathVariable String cryptoCurrency,
        @PathVariable String fiatCurrency
    ) {
        return ResponseDTO.ok(cryptoService.getExchangeRate(cryptoCurrency, fiatCurrency));
    }
}
```

### 7.3 Service Layer

```java
package net.lab1024.sa.admin.module.business.crypto.service;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CryptoService {

    private final CryptoManager cryptoManager;
    private final CryptoDepositManager cryptoDepositManager;
    private final CryptoWithdrawalManager cryptoWithdrawalManager;

    public CryptoDepositResponse initiateDeposit(CryptoDepositForm form) {
        return cryptoDepositManager.initiateDeposit(form);
    }

    public String getDepositAddress(String currencyCode) {
        Long playerId = RequestUtils.getPlayerId();
        return cryptoManager.generateDepositAddress(playerId, currencyCode);
    }

    public CryptoWithdrawalResponse initiateWithdrawal(CryptoWithdrawalForm form) {
        return cryptoWithdrawalManager.initiateWithdrawal(form);
    }

    public PageResult<CryptoTransactionVO> queryTransactions(CryptoTransactionQueryForm form) {
        Page<CryptoTransaction> page = SmartPageUtil.convert2PageQuery(form);
        page = cryptoManager.queryTransactions(page, form);

        return SmartPageUtil.convert2PageResult(page, CryptoTransactionVO.class);
    }

    public ExchangeRateVO getExchangeRate(String cryptoCurrency, String fiatCurrency) {
        return cryptoManager.getExchangeRate(cryptoCurrency, fiatCurrency);
    }
}
```

### 7.4 Manager Layer (Transaction Boundary)

```java
package net.lab1024.sa.admin.module.business.crypto.manager;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CryptoDepositManager {

    private final CryptoTransactionDao cryptoTransactionDao;
    private final HdWalletService hdWalletService;
    private final ExchangeRateService exchangeRateService;
    private final WalletManager walletManager;  // P0-03 integration
    private final SagaOrchestrator sagaOrchestrator;  // P1-05 integration

    @Transactional
    public CryptoDepositResponse initiateDeposit(CryptoDepositForm form) {
        String tenantId = TenantContextHolder.getTenantId();
        Long playerId = RequestUtils.getPlayerId();

        // 1. Lock exchange rate
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(
            form.getCryptoCurrency(),
            "USD"
        );
        BigDecimal expectedFiatAmount = form.getCryptoAmount().multiply(exchangeRate);

        // 2. Create transaction record
        CryptoTransaction transaction = new CryptoTransaction();
        transaction.setTenantId(tenantId);
        transaction.setPlayerId(playerId);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setCryptoCurrency(form.getCryptoCurrency());
        transaction.setCryptoAmount(form.getCryptoAmount());
        transaction.setExchangeRate(exchangeRate);
        transaction.setFiatCurrency("USD");
        transaction.setExpectedFiatAmount(expectedFiatAmount);
        transaction.setStatus(CryptoTransactionStatus.PENDING);

        cryptoTransactionDao.insert(transaction);

        // 3. Generate deposit address
        String depositAddress = hdWalletService.generateDepositAddress(
            playerId,
            form.getCryptoCurrency()
        );

        // 4. Start deposit saga (P1-05 integration)
        String sagaId = sagaOrchestrator.startSaga(
            "CRYPTO_DEPOSIT_SAGA",
            Map.of(
                "transactionId", transaction.getId(),
                "depositAddress", depositAddress,
                "confirmationsRequired", getConfirmationsRequired(form.getCryptoCurrency())
            )
        );

        transaction.setSagaId(UUID.fromString(sagaId));
        cryptoTransactionDao.updateById(transaction);

        return CryptoDepositResponse.builder()
            .transactionId(transaction.getId())
            .depositAddress(depositAddress)
            .cryptoAmount(form.getCryptoAmount())
            .expectedFiatAmount(expectedFiatAmount)
            .exchangeRate(exchangeRate)
            .expiresAt(LocalDateTime.now().plusHours(24))
            .build();
    }

    /**
     * Process blockchain confirmation (called by BlockchainMonitorService)
     */
    @Transactional
    public void processBlockchainConfirmation(String txHash, int confirmations) {
        CryptoTransaction transaction = cryptoTransactionDao.selectOne(
            new LambdaQueryWrapper<CryptoTransaction>()
                .eq(CryptoTransaction::getBlockchainTxHash, txHash)
        );

        transaction.setConfirmations(confirmations);

        if (confirmations >= getConfirmationsRequired(transaction.getCryptoCurrency())) {
            transaction.setStatus(CryptoTransactionStatus.CONFIRMED);
            transaction.setConfirmedAt(LocalDateTime.now());

            // Credit player wallet (use locked exchange rate)
            BigDecimal fiatAmount = transaction.getCryptoAmount()
                .multiply(transaction.getExchangeRate());

            walletManager.deposit(
                transaction.getPlayerId(),
                fiatAmount,
                transaction.getFiatCurrency(),
                "Crypto deposit: " + transaction.getCryptoCurrency()
            );

            transaction.setActualFiatAmount(fiatAmount);
            transaction.setStatus(CryptoTransactionStatus.COMPLETED);
            transaction.setCompletedAt(LocalDateTime.now());
        }

        cryptoTransactionDao.updateById(transaction);
    }

    private int getConfirmationsRequired(String currencyCode) {
        return "BTC".equals(currencyCode) ? 6 : 12;  // BTC=6, ETH=12
    }
}
```

### 7.5 Dao Layer

```java
package net.lab1024.sa.admin.module.business.crypto.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.admin.module.business.crypto.domain.entity.CryptoTransaction;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CryptoTransactionDao extends BaseMapper<CryptoTransaction> {
    // MyBatis-Plus provides CRUD methods automatically
}
```

---

## 8. Integration Points

### 8.1 Integration with P0-01 (Double-Entry Ledger)

**Requirement**: All crypto deposits/withdrawals must be recorded in ledger with exchange rate snapshot

```java
@Service
@RequiredArgsConstructor
public class CryptoLedgerIntegrationService {

    private final LedgerManager ledgerManager;  // P0-01

    public void recordCryptoDeposit(CryptoTransaction transaction) {
        // Debit: Crypto Asset Account (increase asset)
        // Credit: Player Liability Account (increase liability to player)

        ledgerManager.createDoubleEntry(
            LedgerEntryBuilder.builder()
                .debitAccount("ASSET:CRYPTO:" + transaction.getCryptoCurrency())
                .creditAccount("LIABILITY:PLAYER:" + transaction.getPlayerId())
                .amount(transaction.getActualFiatAmount())
                .currency(transaction.getFiatCurrency())
                .transactionType("CRYPTO_DEPOSIT")
                .referenceId(transaction.getId().toString())
                .description(String.format(
                    "Crypto deposit: %s %s @ %s = %s %s",
                    transaction.getCryptoAmount(),
                    transaction.getCryptoCurrency(),
                    transaction.getExchangeRate(),
                    transaction.getActualFiatAmount(),
                    transaction.getFiatCurrency()
                ))
                .metadata(Map.of(
                    "blockchain_tx_hash", transaction.getBlockchainTxHash(),
                    "exchange_rate", transaction.getExchangeRate(),
                    "crypto_amount", transaction.getCryptoAmount()
                ))
                .build()
        );
    }
}
```

### 8.2 Integration with P0-03 (Seamless Wallet)

**Requirement**: Multi-currency wallet must support BTC/ETH alongside USD/EUR

```java
// Wallet entity supports multiple currencies
@Data
public class Wallet {
    private Long playerId;

    // Crypto balances
    private BigDecimal btcBalance;   // Bitcoin balance
    private BigDecimal ethBalance;   // Ethereum balance

    // Fiat balances (already exists)
    private BigDecimal usdBalance;
    private BigDecimal eurBalance;
}

// Credit crypto deposit to wallet
walletManager.deposit(
    transaction.getPlayerId(),
    transaction.getActualFiatAmount(),
    transaction.getFiatCurrency(),  // USD/EUR
    "Crypto deposit: " + transaction.getCryptoCurrency()
);
```

### 8.3 Integration with P1-05 (Saga Pattern)

**Requirement**: Crypto deposit/withdrawal must use saga for distributed consistency

**Crypto Deposit Saga**:
1. **Reserve Crypto (Blockchain)**: Wait for confirmations (6 for BTC, 12 for ETH)
2. **Create Ledger Entry**: Record in double-entry ledger
3. **Credit Wallet**: Add funds to player wallet
4. **Notify Player**: Send deposit success notification

**Compensation**:
- If wallet credit fails → reverse ledger entry → refund to player's blockchain address

```java
@Component
public class CryptoDepositSagaDefinition implements SagaDefinition {

    @Override
    public String getSagaName() {
        return "CRYPTO_DEPOSIT_SAGA";
    }

    @Override
    public List<SagaStep> getSteps() {
        return List.of(
            SagaStep.builder()
                .stepName("wait-blockchain-confirmations")
                .participant(blockchainMonitorParticipant)
                .forwardAction("waitForConfirmations")
                .timeoutSeconds(3600)  // 1 hour
                .build(),

            SagaStep.builder()
                .stepName("create-ledger-entry")
                .participant(ledgerParticipant)
                .forwardAction("createCryptoDepositEntry")
                .compensationAction("reverseCryptoDepositEntry")
                .build(),

            SagaStep.builder()
                .stepName("credit-wallet")
                .participant(walletParticipant)
                .forwardAction("creditWallet")
                .compensationAction("debitWallet")
                .build()
        );
    }
}
```

### 8.4 Integration with P1-07 (Multi-Tenant Isolation)

**Requirement**: Each tenant has isolated HD wallet hierarchy

```java
// Tenant-specific HD wallet configuration
CryptoConfig config = cryptoConfigDao.selectOne(
    new LambdaQueryWrapper<CryptoConfig>()
        .eq(CryptoConfig::getTenantId, tenantId)  // Tenant isolation
        .eq(CryptoConfig::getCurrencyCode, currencyCode)
);

// Derivation path includes tenant's account index
// m/44'/0'/{tenant_account_index}'/0/address_index
String derivationPath = String.format(
    "m/44'/%d'/%d'/0/%d",
    coinType,
    config.getAccountIndex(),  // Unique per tenant
    addressIndex
);
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

**HD Wallet Address Generation**:
```java
@SpringBootTest
class HdWalletServiceTest {

    @Autowired
    private HdWalletService hdWalletService;

    @Test
    void testGenerateBitcoinAddress() {
        String address = hdWalletService.generateDepositAddress(1L, "BTC");

        // Bitcoin addresses start with 1, 3, or bc1
        assertThat(address).matches("^[13bc1].*");
    }

    @Test
    void testGenerateEthereumAddress() {
        String address = hdWalletService.generateDepositAddress(1L, "ETH");

        // Ethereum addresses start with 0x and are 42 characters
        assertThat(address).startsWith("0x");
        assertThat(address).hasSize(42);
    }

    @Test
    void testAddressUniqueness() {
        String address1 = hdWalletService.generateDepositAddress(1L, "BTC");
        String address2 = hdWalletService.generateDepositAddress(1L, "BTC");

        assertThat(address1).isNotEqualTo(address2);
    }

    @Test
    void testMultiTenantIsolation() {
        TenantContextHolder.setTenantId("tenant1");
        String address1 = hdWalletService.generateDepositAddress(1L, "BTC");

        TenantContextHolder.setTenantId("tenant2");
        String address2 = hdWalletService.generateDepositAddress(1L, "BTC");

        // Different tenants should get different addresses
        assertThat(address1).isNotEqualTo(address2);
    }
}
```

**Exchange Rate Service**:
```java
@SpringBootTest
class ExchangeRateServiceTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    void testGetExchangeRate() {
        BigDecimal rate = exchangeRateService.getExchangeRate("BTC", "USD");

        // BTC/USD rate should be between $10,000 and $100,000
        assertThat(rate).isBetween(
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(100000)
        );
    }

    @Test
    void testSlippageProtection() {
        FiatConversionResult result = exchangeRateService.convertCryptoToFiat(
            BigDecimal.valueOf(0.5),  // 0.5 BTC
            "BTC",
            "USD",
            BigDecimal.valueOf(2.0)  // ±2% tolerance
        );

        // Min/max should be ±2% of fiat amount
        BigDecimal expectedMin = result.getFiatAmount().multiply(BigDecimal.valueOf(0.98));
        BigDecimal expectedMax = result.getFiatAmount().multiply(BigDecimal.valueOf(1.02));

        assertThat(result.getMinFiatAmount()).isEqualByComparingTo(expectedMin);
        assertThat(result.getMaxFiatAmount()).isEqualByComparingTo(expectedMax);
    }
}
```

### 9.2 Integration Tests

**Crypto Deposit Flow**:
```java
@SpringBootTest
@Transactional
class CryptoDepositIntegrationTest {

    @Autowired
    private CryptoDepositManager cryptoDepositManager;

    @Autowired
    private BlockchainMonitorService blockchainMonitorService;

    @Autowired
    private WalletManager walletManager;

    @Test
    void testCompleteCryptoDepositFlow() {
        // 1. Initiate deposit
        CryptoDepositForm form = new CryptoDepositForm();
        form.setCryptoCurrency("BTC");
        form.setCryptoAmount(BigDecimal.valueOf(0.5));

        CryptoDepositResponse response = cryptoDepositManager.initiateDeposit(form);

        assertThat(response.getDepositAddress()).isNotNull();
        assertThat(response.getExchangeRate()).isGreaterThan(BigDecimal.ZERO);

        // 2. Simulate blockchain confirmation
        String txHash = "mock_blockchain_tx_hash_12345";
        cryptoDepositManager.processBlockchainConfirmation(txHash, 6);  // 6 confirmations

        // 3. Verify wallet credited
        Wallet wallet = walletManager.getWallet(playerId);
        assertThat(wallet.getUsdBalance()).isEqualByComparingTo(response.getExpectedFiatAmount());
    }
}
```

### 9.3 Security Tests

**Multi-Signature Verification**:
```java
@SpringBootTest
class ColdWalletSecurityTest {

    @Autowired
    private ColdWalletService coldWalletService;

    @Test
    void testMultiSigRequires2Of3Signatures() {
        // Create unsigned transaction
        Transaction unsignedTx = createMockWithdrawalTransaction();

        // Sign with 1 key (insufficient)
        Transaction partiallySigned = coldWalletService.signWithdrawalTransaction(
            unsignedTx,
            cfoPrivateKey
        );

        assertThat(coldWalletService.verifyMultiSigTransaction(partiallySigned)).isFalse();

        // Sign with 2nd key (sufficient)
        Transaction fullySigned = coldWalletService.signWithdrawalTransaction(
            partiallySigned,
            ctoPrivateKey
        );

        assertThat(coldWalletService.verifyMultiSigTransaction(fullySigned)).isTrue();
    }
}
```

### 9.4 Performance Tests

**Exchange Rate Update Latency**:
```java
@SpringBootTest
class ExchangeRatePerformanceTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Test
    void testExchangeRateUpdateLatency() {
        // Measure latency of exchange rate updates
        long startTime = System.currentTimeMillis();
        exchangeRateService.updateExchangeRates();
        long duration = System.currentTimeMillis() - startTime;

        // Should complete in <1 second
        assertThat(duration).isLessThan(1000);
    }

    @Test
    void testConcurrentAddressGeneration() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> addresses = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                String address = hdWalletService.generateDepositAddress(1L, "BTC");
                addresses.add(address);
                latch.countDown();
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);

        // All addresses should be unique
        assertThat(addresses).hasSize(threadCount);
    }
}
```

---

## 10. Operations & Monitoring

### 10.1 Key Metrics

**Prometheus Metrics**:
```java
@Component
public class CryptoMetrics {

    private final Counter depositCount = Counter.build()
        .name("crypto_deposits_total")
        .help("Total crypto deposits")
        .labelNames("tenant_id", "currency_code")
        .register();

    private final Histogram depositAmount = Histogram.build()
        .name("crypto_deposit_amount_usd")
        .help("Crypto deposit amount in USD")
        .buckets(10, 50, 100, 500, 1000, 5000)
        .labelNames("tenant_id", "currency_code")
        .register();

    private final Gauge hotWalletBalance = Gauge.build()
        .name("crypto_hot_wallet_balance_usd")
        .help("Hot wallet balance in USD")
        .labelNames("tenant_id", "currency_code")
        .register();

    private final Counter blockchainConfirmationDelay = Counter.build()
        .name("crypto_blockchain_confirmation_delay_seconds")
        .help("Time from transaction broadcast to final confirmation")
        .labelNames("currency_code")
        .register();
}
```

**Grafana Dashboard**:
- **Deposit Volume**: Deposits per hour (BTC, ETH)
- **Hot Wallet Balance**: Current balance vs threshold ($5,000)
- **Exchange Rate Spread**: BTC/USD spread vs CoinGecko
- **Confirmation Latency**: Average time to 6 confirmations (BTC)

### 10.2 Alerts

**Critical Alerts**:
1. **Hot Wallet Threshold Exceeded**: Balance > $10,000
2. **Exchange Rate Stale**: No update in >60 seconds
3. **Blockchain Sync Delay**: Bitcoin Core >10 blocks behind
4. **Multi-Sig Approval Pending**: >24 hours without approval

**Alert Configuration (Prometheus AlertManager)**:
```yaml
groups:
  - name: crypto_alerts
    rules:
      - alert: HotWalletThresholdExceeded
        expr: crypto_hot_wallet_balance_usd > 10000
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Hot wallet balance exceeded $10,000 threshold"
          description: "Tenant {{ $labels.tenant_id }} hot wallet for {{ $labels.currency_code }} has {{ $value }} USD (max 10,000)"

      - alert: ExchangeRateStale
        expr: (time() - exchange_rate_last_update_timestamp) > 60
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Exchange rate not updated in >60 seconds"

      - alert: MultiSigApprovalPending
        expr: cold_wallet_transfer_tasks{status="PENDING_APPROVAL"} > 0 and (time() - cold_wallet_transfer_tasks_created_timestamp) > 86400
        labels:
          severity: warning
        annotations:
          summary: "Multi-sig approval pending >24 hours"
```

### 10.3 Daily Reconciliation

**On-Chain vs Internal Ledger**:
```java
@Scheduled(cron = "0 0 4 * * ?")  // Daily 4 AM
public void reconcileBlockchainBalances() {
    List<CryptoConfig> configs = cryptoConfigDao.selectList(new LambdaQueryWrapper<>());

    for (CryptoConfig config : configs) {
        // 1. Get on-chain balance from blockchain
        BigDecimal onChainBalance = getOnChainBalance(
            config.getHotWalletAddress(),
            config.getCurrencyCode()
        );

        // 2. Get internal ledger balance
        BigDecimal internalBalance = ledgerManager.getAccountBalance(
            "ASSET:CRYPTO:" + config.getCurrencyCode()
        );

        // 3. Compare with tolerance (±0.001 BTC or ±0.01 ETH)
        BigDecimal tolerance = getTolerance(config.getCurrencyCode());
        BigDecimal difference = onChainBalance.subtract(internalBalance).abs();

        if (difference.compareTo(tolerance) > 0) {
            // CRITICAL: Mismatch detected
            log.error("Blockchain reconciliation FAILED: tenant={}, currency={}, onChain={}, internal={}, diff={}",
                config.getTenantId(), config.getCurrencyCode(), onChainBalance, internalBalance, difference);

            // Send alert to finance team
            alertService.sendCriticalAlert(
                "Blockchain Reconciliation Failed",
                String.format("Difference: %s %s", difference, config.getCurrencyCode())
            );
        } else {
            log.info("Blockchain reconciliation OK: tenant={}, currency={}, balance={}",
                config.getTenantId(), config.getCurrencyCode(), onChainBalance);
        }
    }
}
```

### 10.4 Operational Runbooks

**Hot Wallet Threshold Exceeded**:
1. Login to admin panel → Crypto → Cold Wallet Transfers
2. Verify pending transfer task exists (auto-created by daily sweep)
3. Review transfer details (amount, from/to addresses)
4. Approve with hardware wallet (requires 2-of-3 signatures)
5. Verify blockchain transaction broadcast
6. Monitor confirmation progress

**Exchange Rate Feed Failure**:
1. Check CoinGecko API status (https://status.coingecko.com)
2. If API down: Switch to fallback provider (Coinbase)
3. If persistent: Temporarily disable crypto deposits (prevent rate arbitrage)
4. Monitor until API restored

**Blockchain Sync Delay**:
1. SSH to Bitcoin Core/Geth server
2. Check sync status: `bitcoin-cli getblockchaininfo` or `geth attach --exec "eth.syncing"`
3. If >100 blocks behind: Restart node with `--resync` flag
4. Monitor sync progress every 15 minutes

---

## 11. Appendices

### 11.1 BIP32/BIP44 Reference

**BIP32**: Hierarchical Deterministic Wallets
- Master seed → Infinite child keys
- Derivation uses HMAC-SHA512

**BIP44**: Multi-Account Hierarchy
- **Path**: m / purpose' / coin_type' / account' / change / address_index
- **Purpose**: Always 44' (BIP44)
- **Coin Type**: 0' (BTC), 60' (ETH)
- **Account**: Tenant-specific index
- **Change**: 0 (external/deposit), 1 (internal/change)
- **Address Index**: Sequential counter

### 11.2 Blockchain RPC Examples

**Bitcoin Core RPC**:
```bash
# Get wallet balance
bitcoin-cli getbalance

# List unspent outputs
bitcoin-cli listunspent 6 9999999 '["bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"]'

# Send transaction
bitcoin-cli sendtoaddress "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh" 0.5

# Get transaction details
bitcoin-cli gettransaction "txid_here"
```

**Ethereum Geth RPC**:
```bash
# Get balance
geth attach --exec "eth.getBalance('0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb')"

# Send transaction
geth attach --exec "eth.sendTransaction({from: '0x...', to: '0x...', value: web3.toWei(1, 'ether')})"

# Get transaction receipt
geth attach --exec "eth.getTransactionReceipt('0xtxhash...')"
```

### 11.3 Security Best Practices

1. **Private Key Storage**:
   - NEVER store private keys in database or code
   - Use AWS Secrets Manager or HSM
   - Rotate master seed every 90 days

2. **Address Reuse**:
   - Generate new address for each deposit (privacy)
   - Mark addresses as used after first deposit

3. **Multi-Signature**:
   - Minimum 2-of-3 for withdrawals >$500
   - Store keys in separate physical locations

4. **Cold Wallet**:
   - Offline computer, never connected to internet
   - Hardware wallet (Ledger, Trezor) in bank safe
   - Annual penetration test

5. **Exchange Rate**:
   - Lock rate at transaction initiation
   - 7-year retention for regulatory audit

### 11.4 Regulatory Compliance

**Malta Gaming Authority (MGA)**:
- Segregated cold wallet per merchant (tenant)
- Multi-signature mandatory for >$500 withdrawals
- Audit trail of all private key access
- Annual third-party security audit

**General Data Protection Regulation (GDPR)**:
- Blockchain addresses are pseudonymous (not anonymous)
- Player can request data deletion (right to erasure)
- Cannot delete blockchain transactions → Anonymize in internal database

### 11.5 Cost Analysis

**Monthly Costs** (100 merchants, 10,000 crypto transactions/month):

| Component | Cost | Rationale |
|-----------|------|-----------|
| Bitcoin Full Node (AWS EC2 t3.large) | $60 | 2 vCPU, 8 GB RAM, 500 GB SSD |
| Ethereum Geth Node (AWS EC2 t3.xlarge) | $120 | 4 vCPU, 16 GB RAM, 1 TB SSD |
| AWS CloudHSM | $1,500 | FIPS 140-2 Level 3 compliance |
| CoinGecko API (Free tier) | $0 | 50 calls/min sufficient |
| Blockchain Transaction Fees (BTC) | $500 | ~$5 per withdrawal × 100 withdrawals |
| Blockchain Transaction Fees (ETH) | $200 | ~$2 per withdrawal × 100 withdrawals |
| **Total** | **$2,380/month** | **~$28,560/year** |

**ROI**: Crypto processing fees (0.1%) vs card fees (2.5-3.5%) → Save 2.4% per transaction
- If monthly crypto deposit volume = $1M → Save $24,000/month
- Break-even after 1 month

---

## Document Status

**Version**: 1.0.0
**Status**: Draft (Ready for Technical Review)
**Lines**: ~1,280 lines
**Last Updated**: 2026-01-23

**Next Steps**:
1. Technical review by crypto security expert
2. Validation of BIP32/BIP44 implementation
3. Penetration testing of HD wallet derivation
4. Multi-signature workflow testing with hardware wallets
5. Integration testing with P0-03 (Wallet) and P1-05 (Saga)

**Related Documents**:
- [P0-01: Double-Entry Ledger](../P0-critical/01-double-entry-ledger-schema.md)
- [P0-03: Seamless Wallet](../P0-critical/03-seamless-wallet-implementation.md)
- [P1-05: Distributed Transaction Patterns](05-distributed-transaction-patterns.md)
- [P1-07: Multi-Tenant Isolation](07-multi-tenant-isolation.md)
