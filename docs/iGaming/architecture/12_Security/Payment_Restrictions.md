# Payment Restrictions Architecture

> **Business Requirements**: [Payment Security Requirements](../../requirements/12_Security_Compliance/Payment_Security_Requirements.md)
> **Canonical Source**: [source-archive/12_System_Security/12-06](../../source-archive/12_System_Security/12-06_Payment_Restrictions.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Backend Developers, Compliance Engineers

---

## 1. Credit Card Bans by Jurisdiction

| Region | Effective Date | Scope | Status |
|--------|---------------|-------|--------|
| **UK** | 2020-04 | All gambling | Complete ban |
| **Australia** | 2026-04 | Online gambling | Upcoming |
| **Sweden** | 2025+ | Online brands | Expanded ban |
| **Germany** | 2021 | All gambling | Complete ban |

## 2. Payment Restriction Service

```java
@Service
@RequiredArgsConstructor
public class PaymentRestrictionService {

    private final JurisdictionRouterService jurisdictionService;

    /**
     * Validate payment method against jurisdiction rules
     */
    public PaymentValidationResult validatePaymentMethod(
            Long playerId,
            String paymentMethod) {

        JurisdictionConfig config = jurisdictionService.getPlayerJurisdiction(playerId);

        // Credit card check
        if (isCreditCard(paymentMethod) && !config.getCreditCardsAllowed()) {
            return PaymentValidationResult.blocked(
                "CREDIT_CARD_NOT_ALLOWED",
                "Credit cards are not allowed for deposits in your region"
            );
        }

        // Cryptocurrency check
        if (isCrypto(paymentMethod) && !config.getCryptoAllowed()) {
            return PaymentValidationResult.blocked(
                "CRYPTO_NOT_ALLOWED",
                "Cryptocurrency deposits are not allowed in your region"
            );
        }

        // Whitelist check
        if (!config.getAllowedPaymentMethods().isEmpty() &&
            !config.getAllowedPaymentMethods().contains(paymentMethod)) {
            return PaymentValidationResult.blocked(
                "PAYMENT_METHOD_NOT_ALLOWED",
                "This payment method is not supported"
            );
        }

        return PaymentValidationResult.allowed();
    }

    private boolean isCreditCard(String paymentMethod) {
        return Set.of(
            "CREDIT_CARD",
            "VISA_CREDIT",
            "MASTERCARD_CREDIT",
            "AMEX"
        ).contains(paymentMethod.toUpperCase());
    }

    private boolean isCrypto(String paymentMethod) {
        return Set.of(
            "BITCOIN",
            "ETHEREUM",
            "USDT",
            "CRYPTO"
        ).contains(paymentMethod.toUpperCase());
    }
}
```

## 3. Payment Method Whitelists

### UK Allowed Payment Methods

| Type | Allowed | Notes |
|------|---------|-------|
| Debit Card | Yes | Visa/Mastercard debit |
| Bank Transfer | Yes | Bank transfer |
| e-Wallet | Yes | PayPal, Skrill, Neteller |
| Prepaid Card | Yes | Paysafecard |
| Credit Card | **No** | **Prohibited** |

### Brazil Required Payment Methods

| Type | Requirement | Notes |
|------|------------|-------|
| **PIX** | **Mandatory** | Brazilian instant payment |
| Bank Transfer | Recommended | Bank transfer |
| Boleto | Recommended | Cash payment |
| Credit Card | Allowed | Currently permitted |

## 4. Cryptocurrency AML Service

```java
@Service
public class CryptoAMLService {

    /**
     * Cryptocurrency AML check
     */
    public CryptoAMLResult performAMLCheck(CryptoDepositRequest request) {
        // 1. Wallet address risk scoring
        WalletRiskScore walletScore = cryptoAnalysisClient.scoreWallet(
            request.getWalletAddress(),
            request.getCryptoType()
        );

        if (walletScore.getRiskLevel() == RiskLevel.HIGH) {
            return CryptoAMLResult.blocked("HIGH_RISK_WALLET");
        }

        // 2. Transaction tracing
        TransactionTrace trace = cryptoAnalysisClient.traceTransaction(
            request.getTxHash()
        );

        if (trace.hasBlacklistedAddress()) {
            return CryptoAMLResult.blocked("BLACKLISTED_ADDRESS");
        }

        // 3. Audit logging
        cryptoAMLLogDao.insert(CryptoAMLLog.of(request, walletScore, trace));

        return CryptoAMLResult.passed();
    }
}
```

## 5. Cryptocurrency Regulatory Landscape

| Region | Attitude | Notes |
|--------|----------|-------|
| UK | Cautious | Full AML required |
| Malta | Allowed | Regulatory framework required |
| Curacao | Allowed | More relaxed |

## 6. Transaction Screening Flow

```mermaid
flowchart TD
    A[Deposit / Withdrawal Request] --> B{Jurisdiction<br/>Check}
    B -->|Blocked Region| C[Reject: REGION_BLOCKED]
    B -->|Allowed| D{Payment Method<br/>Allowed?}
    D -->|Not Allowed| E[Reject: METHOD_BLOCKED]
    D -->|Allowed| F{Velocity<br/>Check}
    F -->|Exceeded| G[Reject: VELOCITY_EXCEEDED]
    F -->|Pass| H{AML<br/>Screening}
    H -->|Flagged| I[Hold for Manual Review]
    H -->|Clear| J{Amount<br/>Threshold?}
    J -->|Above Threshold| K[Enhanced Due Diligence]
    J -->|Below Threshold| L[Approve Transaction]
    K --> L

    style C fill:#FF5252,color:#fff
    style E fill:#FF5252,color:#fff
    style G fill:#FF9800,color:#fff
    style I fill:#FFC107
    style L fill:#4CAF50,color:#fff
```

## 7. Velocity Check Implementation

```java
@Service
@RequiredArgsConstructor
public class VelocityCheckService {

    private final RedissonClient redissonClient;
    private final VelocityRuleDao velocityRuleDao;

    /**
     * Check deposit/withdrawal velocity against configurable thresholds.
     * Uses Redis sliding window counters per player per action.
     */
    public VelocityCheckResult checkVelocity(Long playerId, String action, BigDecimal amount) {
        List<VelocityRule> rules = velocityRuleDao.selectByAction(action);

        for (VelocityRule rule : rules) {
            String key = String.format("velocity:%s:%d:%s", action, playerId, rule.getWindow());
            RAtomicLong counter = redissonClient.getAtomicLong(key + ":count");
            RAtomicLong totalAmount = redissonClient.getAtomicLong(key + ":amount");

            // Check transaction count limit
            if (counter.get() >= rule.getMaxCount()) {
                return VelocityCheckResult.exceeded(
                    "MAX_COUNT_EXCEEDED",
                    String.format("Max %d transactions per %s", rule.getMaxCount(), rule.getWindow())
                );
            }

            // Check cumulative amount limit
            if (totalAmount.get() + amount.longValue() > rule.getMaxAmount().longValue()) {
                return VelocityCheckResult.exceeded(
                    "MAX_AMOUNT_EXCEEDED",
                    String.format("Max %s per %s", rule.getMaxAmount(), rule.getWindow())
                );
            }

            // Increment counters with TTL matching the window
            counter.incrementAndGet();
            counter.expire(Duration.ofSeconds(rule.getWindowSeconds()));
            totalAmount.addAndGet(amount.longValue());
            totalAmount.expire(Duration.ofSeconds(rule.getWindowSeconds()));
        }

        return VelocityCheckResult.passed();
    }
}
```

### Velocity Rule Configuration

| Window | Max Count | Max Amount (USD) | Action | Notes |
|--------|-----------|-----------------|--------|-------|
| 1 hour | 5 | 2,000 | Deposit | Standard player |
| 24 hours | 15 | 10,000 | Deposit | Standard player |
| 7 days | 50 | 50,000 | Deposit | Standard player |
| 1 hour | 3 | 5,000 | Withdrawal | Standard player |
| 24 hours | 5 | 20,000 | Withdrawal | Standard player |
| 1 hour | 20 | 50,000 | Deposit | VIP player |
| 24 hours | 50 | 200,000 | Deposit | VIP player |

## 8. Jurisdiction-Based Payment Rules

```sql
CREATE TABLE t_jurisdiction_payment_rule (
    id              BIGSERIAL PRIMARY KEY,
    jurisdiction    VARCHAR(10) NOT NULL,
    payment_method  VARCHAR(50) NOT NULL,
    allowed         BOOLEAN NOT NULL DEFAULT TRUE,
    min_amount      DECIMAL(18,2),
    max_amount      DECIMAL(18,2),
    requires_kyc    VARCHAR(20) DEFAULT 'BASIC',
    effective_from  TIMESTAMP NOT NULL,
    effective_to    TIMESTAMP,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(jurisdiction, payment_method, effective_from)
);

-- UK: Block credit cards, allow debit
INSERT INTO t_jurisdiction_payment_rule (jurisdiction, payment_method, allowed, effective_from)
VALUES ('UK', 'CREDIT_CARD', FALSE, '2020-04-14');
INSERT INTO t_jurisdiction_payment_rule (jurisdiction, payment_method, allowed, min_amount, max_amount, effective_from)
VALUES ('UK', 'DEBIT_CARD', TRUE, 5.00, 10000.00, '2020-04-14');

-- Brazil: PIX mandatory, low minimum
INSERT INTO t_jurisdiction_payment_rule (jurisdiction, payment_method, allowed, min_amount, max_amount, effective_from)
VALUES ('BR', 'PIX', TRUE, 1.00, 50000.00, '2024-01-01');
```

---

<!-- End of Document -->
