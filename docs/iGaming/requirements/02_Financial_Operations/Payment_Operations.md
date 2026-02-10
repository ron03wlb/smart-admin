# Payment Operations

> **Canonical Source**: [source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md](../../source-archive/02_Finance_Center/02-02_Payment_Gateway_Integration.md)
> **Audience**: Executives, Product Managers, Compliance Officers
> **Related Doc**: [Payment_Gateway_Technical.md](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (PSP webhook implementation, signature verification algorithms, smart routing code, scheduled reconciliation jobs, connection pool configuration, Prometheus metrics) moved to Architecture layer. This document focuses on business rules only.

---

## Business Value

This payment operations system delivers value by:
- **Payment success rate optimization**: Smart routing algorithm dynamically selects optimal PSPs based on success rate (50% weight), transaction fees (30% weight), and settlement speed (15% weight), reducing drop rate from industry average 3-5% to target <1%
- **Cost efficiency**: Multi-PSP competition and dynamic channel switching reduce per-transaction fees by 15-30% compared to single-provider setups, especially for high-volume VIP players
- **Regulatory compliance**: Ensures PCI-DSS Level 1 compliance (no card storage), PSD2 3D Secure mandates (EU), and AML wallet address risk scoring for cryptocurrency payments

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Drop Rate | <1% | (Credits / Total Success) x 100%; alert triggered at >3% |
| Credit Success Rate | >95% | (Successful Credits / Credit Attempts) x 100%; alert at <90% |
| Average Credit Delay | <30 min | Time from PSP success callback to platform balance credit; alert at >2 hours |
| PSP API Success Rate | >99% | (Successful Queries / Total Queries) x 100%; alert at <95% |
| Pending Backlog | <10 orders | Orders pending >2 hours; alert at >50 orders |
| Manual Review Rate | <5% | (Manual Reviews / Total Credits) x 100%; alert at >15% |

---

## 1. Overview

The payment operations module is responsible for all interactions with external payment service providers (PSPs), ensuring secure, stable, and automated fund inflows and outflows. The system must support multiple payment methods and dynamic routing capabilities.

---

## 2. Supported Payment Methods

### 2.1 Fiat Currency Payments

| Payment Type | Examples | Business Considerations |
|-------------|----------|------------------------|
| **Bank Transfer** | Wire Transfer, ACH, SEPA | T+1 to T+3 settlement |
| **Credit/Debit Card** | VISA, Mastercard, AMEX | Subject to jurisdiction restrictions |
| **E-Wallet** | LinePay, Momo, GCash, PayPal, Skrill | Regional availability varies |

### 2.2 Cryptocurrency Payments

| Currency | Networks | Special Requirements |
|----------|----------|---------------------|
| USDT | TRC20, ERC20 | Real-time exchange rate conversion required |
| BTC | Bitcoin Network | Wallet address risk scoring for AML |
| ETH | Ethereum | Gas fee considerations |

**Business Rule**: Platform primary accounts are typically in fiat currency. Crypto payments require integration with exchange rate APIs (e.g., Binance, Oanda) for real-time conversion.

---

## 3. Jurisdiction-Specific Restrictions

### 3.1 Credit Card Ban Compliance

| Jurisdiction | Effective Date | Scope | Impact |
|-------------|---------------|-------|--------|
| **United Kingdom** | April 2020 | All gambling | Credit cards blocked |
| **Australia** | April 2026 | Online gambling | Preparation required |
| **Sweden** | 2025+ | Online brands | Expanding ban |
| **Germany** | 2021 | All gambling | Complete prohibition |

**Business Requirement**: Payment method whitelist must be dynamically loaded based on player jurisdiction.

### 3.2 Regional PSP Matrix

| Region | Preferred PSP | Primary Payment Methods | Notes |
|--------|--------------|------------------------|-------|
| United States | Stripe, Nuvei | Credit Card, ACH | PCI-DSS Level 1 required |
| European Union | Adyen, Trustly | SEPA, iDEAL, Sofort | PSD2 strong authentication |
| China | Alipay, WeChat Pay | QR Code payments | Merchant qualification required |
| Philippines | GCash, PayMaya | E-Wallet | High cash usage market |
| Brazil | MercadoPago, PagSeguro | Boleto, PIX | PIX real-time transfer dominant |
| Japan | PayPay, Line Pay, Rakuten Pay | QR Code, E-Wallet | Mobile-first market |

---

## 4. Payment Routing Rules

### 4.1 Smart Routing Principles

**Dynamic Switching Criteria**:
- When a payment channel success rate falls below threshold (e.g., 80%), automatically switch to backup channel
- Route VIP players through dedicated high-speed channels
- Prioritize local PSPs to reduce cross-border fees and improve success rates

### 4.2 Routing Weight Factors

| Factor | Weight | Business Rationale |
|--------|--------|-------------------|
| **Success Rate** | 50% | Primary metric affecting player experience and platform losses |
| **Transaction Fee** | 30% | Cost control; significant impact on high-value transactions |
| **Settlement Speed** | 15% | User experience; faster crediting improves satisfaction |
| **VIP Priority** | 5% | Differentiated service for high-value players |
| **Currency Match** | 3% | Avoid FX losses and additional fees |

→ **[Smart Routing Algorithm](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md#smart-routing-algorithm)** - Score calculation formula, weighted ranking implementation, real-time PSP selection logic

### 4.3 VIP Channel Benefits

| VIP Level | Channel Benefit | Fee Discount | Priority |
|-----------|----------------|--------------|----------|
| VIP 5 | Dedicated account manager | Priority processing | Highest |
| VIP 4 | Fee reduction -0.5% | Fast track | High |
| VIP 3 | Expedited settlement (<5 min) | Standard | Medium |
| VIP 1-2 | Standard PSPs | None | Normal |

---

## 5. Deposit Business Rules

### 5.1 Deposit Flow Summary

1. Player initiates deposit -> System creates order (Pending) -> Redirect to PSP payment page
2. Player completes payment -> PSP sends callback -> System verifies authenticity
3. Verification passed -> Credit player balance -> Update order status (Success) -> Send notification

→ **[PSP Webhook Integration](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md#psp-webhook-integration)** - Signature verification (HMAC-SHA256), callback processing, database updates

### 5.2 Amount Limits

| Limit Type | Configuration Level | Enforcement |
|-----------|---------------------|-------------|
| Per-transaction minimum | Merchant configurable | Real-time |
| Per-transaction maximum | Merchant configurable | Real-time |
| Daily limit | Player level based | Cumulative check |
| Monthly limit | VIP level based | Cumulative check |

---

## 6. Withdrawal Business Rules

### 6.1 Payout Methods

**Automated Payout (API)**:
- Small withdrawals (e.g., < $500): Automatic processing
- Large withdrawals: Manual review required before API trigger

**Manual Payout**:
- Finance personnel review bank details in back office
- Manual transfer and completion marking

### 6.2 Approval Thresholds

| Amount Range | Approval Level | SLA | Evidence Required |
|-------------|---------------|-----|-------------------|
| < $100 | CS Agent | Immediate | Player request |
| $100 - $1,000 | CS Manager | 1 hour | Bank verification |
| $1,000 - $10,000 | CFO | 4 hours | Complete bank statement |
| > $10,000 | CFO + CEO | 24 hours | Full documentation + video call |

---

## 7. Failed Transaction Handling

### 7.1 Reconciliation Requirements

**Automatic Reconciliation**:
- Frequency: Every 15 minutes
- Target: Transactions pending > 30 minutes
- Action: Query PSP status and reconcile

→ **[Auto Reconciliation Implementation](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md#auto-reconciliation)** - Scheduled job configuration, PSP API integration, SQL queries, status synchronization logic

### 7.2 Player Appeal Process

1. Player uploads payment proof (bank transfer screenshot)
2. CS queries PSP API to verify
3. If PSP confirms receipt but platform not credited -> Manual credit + audit log

### 7.3 Severity Classification

| Scenario | Priority | SLA | Notification Recipients |
|----------|----------|-----|------------------------|
| High-value NOT_FOUND (>$1000) | P0 Critical | 15 minutes | Finance + CTO + Security |
| Drop rate > 3% | P1 High | 1 hour | Finance + CS Manager |
| Single drop (auto-credited) | P2 Medium | 24 hours | Daily summary |
| PSP PENDING status | P3 Low | Monitor only | None |

---

## 8. SLA Requirements

### 8.1 Performance Targets

| Metric | Definition | Target | Alert Threshold |
|--------|-----------|--------|-----------------|
| **Drop Rate** | (Credits / Total Success) x 100% | < 1% | > 3% |
| **Credit Success Rate** | (Successful Credits / Credit Attempts) x 100% | > 95% | < 90% |
| **Average Credit Delay** | Time from PSP success to platform credit | < 30 min | > 2 hours |
| **Pending Backlog** | Orders pending > 2 hours | < 10 | > 50 |
| **Manual Review Rate** | (Manual / Total Credits) x 100% | < 5% | > 15% |
| **PSP API Success Rate** | (Successful Queries / Total Queries) x 100% | > 99% | < 95% |

→ **[Performance Monitoring](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md#performance-monitoring)** - Prometheus metrics configuration, Grafana dashboards, alert rules (PagerDuty, Slack)

### 8.2 PSP Health Status Thresholds

| Status | Success Rate | Routing Decision | Recovery Criteria |
|--------|-------------|------------------|-------------------|
| Healthy | >= 80% | Normal routing | N/A |
| Degraded | 50-80% | Lower priority (Score x 0.7) | 3 consecutive successes |
| Unavailable | < 50% | Excluded, use backup | Manual verification |
| Down | No response | Skip, use next in queue | Emergency alert + manual |

---

## 9. Compliance Requirements

### 9.1 Security Standards

| Requirement | Standard | Business Rule |
|------------|----------|---------------|
| Card data storage | PCI-DSS Level 1 | Token-only storage; no full card numbers |
| Payment page | PCI-DSS | Use PSP hosted payment page |
| Data transmission | Encrypted | All API requests must be encrypted |
| API key management | Secure vault | Keys stored in secure credential vault |

→ **[Security Implementation](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md#security-implementation)** - TLS 1.2+ configuration, HashiCorp Vault integration, API key rotation policy

### 9.2 3D Secure Requirements

| Regulation | Requirement | Impact |
|-----------|-------------|--------|
| PSD2 (EU) | Mandatory 3DS 2.0 | All EU card transactions |
| SCA Exemptions | Low-value (<30 EUR), Recurring | Reduced friction for eligible transactions |

→ **[3DS Integration](../../architecture/02_Finance_Service/Payment_Gateway_Technical.md#3ds-integration)** - 3DS 2.0 flow implementation, SCA exemption logic, PSD2 compliance validation

### 9.3 AML Compliance

- Cryptocurrency wallet address risk scoring required
- Suspicious transaction reporting
- Cross-reference with sanctions lists

---

## 10. Configuration Change Governance

### 10.1 High-Risk Operations

- Adding/modifying PSP merchant credentials
- Adjusting routing weights
- Changing transaction limits

### 10.2 Change Approval Flow

1. Technical team submits change request
2. System calculates impact scope (estimated affected transaction volume)
3. CFO/CTO review and approval
4. Scheduled deployment with operations team notification

---

## 11. Related Documentation

### Business References
- [Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) - Deposit crediting logic
- [Withdrawal Risk Control](../../source-archive/01_Player_Center/01-05_Withdrawal_Risk.md) - Withdrawal process and risk control
- [Reconciliation System](../../source-archive/02_Finance_Center/02-03_Reconciliation_System.md) - PSP reconciliation process

### Compliance References
- [Payment Restrictions](../../source-archive/12_System_Security/12-06_Payment_Restrictions.md) - Credit card ban and crypto compliance
- [Multi-Jurisdiction Framework](../../source-archive/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md) - Multi-license payment configuration
- [UKGC Compliance](../../source-archive/06_Platform_Governance/06-08_UKGC_Compliance.md) - UK credit card ban details

### Technical Implementation

→ **[Payment Gateway API Architecture](../../architecture/02_Finance_Service/Payment_Gateway_API.md)** - PSP adapter implementation, webhook processing, smart routing algorithm, reconciliation automation, HikariCP tuning, and Prometheus monitoring

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Finance Team
