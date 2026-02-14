# 12-06 Payment Restrictions (支付限制合規)

**版本**: 1.0.0
**創建日期**: 2026-02-07
**狀態**: ✅ 完整

---

## 概述

本文檔說明各司法管轄區的支付限制要求，包括信用卡禁令和支付方式白名單。

---

## 信用卡禁令

### 按地區要求

| 地區 | 生效日期 | 範圍 | 說明 |
|------|---------|------|------|
| **UK** | 2020-04 | 所有博彩 | 完全禁止 |
| **Australia** | 2026-04 | 線上博彩 | 即將生效 |
| **Sweden** | 2025+ | 線上品牌 | 擴展禁令 |
| **Germany** | 2021 | 所有博彩 | 完全禁止 |

### 技術實現

```java
@Service
@RequiredArgsConstructor
public class PaymentRestrictionService {

    private final JurisdictionRouterService jurisdictionService;

    /**
     * 檢查支付方式是否允許
     */
    public PaymentValidationResult validatePaymentMethod(
            Long playerId,
            String paymentMethod) {

        JurisdictionConfig config = jurisdictionService.getPlayerJurisdiction(playerId);

        // 信用卡檢查
        if (isCreditCard(paymentMethod) && !config.getCreditCardsAllowed()) {
            return PaymentValidationResult.blocked(
                "CREDIT_CARD_NOT_ALLOWED",
                "您所在地區不允許使用信用卡存款"
            );
        }

        // 加密貨幣檢查
        if (isCrypto(paymentMethod) && !config.getCryptoAllowed()) {
            return PaymentValidationResult.blocked(
                "CRYPTO_NOT_ALLOWED",
                "您所在地區不允許使用加密貨幣存款"
            );
        }

        // 白名單檢查
        if (!config.getAllowedPaymentMethods().isEmpty() &&
            !config.getAllowedPaymentMethods().contains(paymentMethod)) {
            return PaymentValidationResult.blocked(
                "PAYMENT_METHOD_NOT_ALLOWED",
                "不支援此支付方式"
            );
        }

        return PaymentValidationResult.allowed();
    }

    /**
     * 判斷是否為信用卡
     */
    private boolean isCreditCard(String paymentMethod) {
        return Set.of(
            "CREDIT_CARD",
            "VISA_CREDIT",
            "MASTERCARD_CREDIT",
            "AMEX"
        ).contains(paymentMethod.toUpperCase());
    }

    /**
     * 判斷是否為加密貨幣
     */
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

---

## 支付方式白名單

### UK 允許的支付方式

| 類型 | 允許 | 說明 |
|------|------|------|
| Debit Card | ✅ | Visa/Mastercard 借記卡 |
| Bank Transfer | ✅ | 銀行轉帳 |
| e-Wallet | ✅ | PayPal, Skrill, Neteller |
| Prepaid Card | ✅ | Paysafecard |
| Credit Card | ❌ | **禁止** |

### Brazil 要求的支付方式

| 類型 | 要求 | 說明 |
|------|------|------|
| **PIX** | **必須** | 巴西即時支付 |
| Bank Transfer | 建議 | 銀行轉帳 |
| Boleto | 建議 | 現金支付 |
| Credit Card | 允許 | 目前允許 |

---

## 加密貨幣合規

### 監管考量

| 地區 | 態度 | 說明 |
|------|------|------|
| UK | 謹慎 | 需完整 AML |
| Malta | 允許 | 需監管框架 |
| Curacao | 允許 | 較寬鬆 |

### 加密貨幣 AML 要求

```java
@Service
public class CryptoAMLService {

    /**
     * 加密貨幣 AML 檢查
     */
    public CryptoAMLResult performAMLCheck(CryptoDepositRequest request) {
        // 1. 錢包地址風險評分
        WalletRiskScore walletScore = cryptoAnalysisClient.scoreWallet(
            request.getWalletAddress(),
            request.getCryptoType()
        );

        if (walletScore.getRiskLevel() == RiskLevel.HIGH) {
            return CryptoAMLResult.blocked("HIGH_RISK_WALLET");
        }

        // 2. 交易追蹤
        TransactionTrace trace = cryptoAnalysisClient.traceTransaction(
            request.getTxHash()
        );

        if (trace.hasBlacklistedAddress()) {
            return CryptoAMLResult.blocked("BLACKLISTED_ADDRESS");
        }

        // 3. 記錄
        cryptoAMLLogDao.insert(CryptoAMLLog.of(request, walletScore, trace));

        return CryptoAMLResult.passed();
    }
}
```

---

## 相關文檔

- [02-02_Payment_Gateway_Integration.md](../02_Finance_Center/02-02_Payment_Gateway_Integration.md) - 支付閘道
- [06-07_Multi_Jurisdiction_Framework.md](../06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) - 多牌照框架
- [05-03_KYC_AML.md](../05_Risk_Control/05-03_KYC_AML.md) - KYC/AML

---

**返回**: [系統安全](README.md) | [iGaming 首頁](../README.md)
