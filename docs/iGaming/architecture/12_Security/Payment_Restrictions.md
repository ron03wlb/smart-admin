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
