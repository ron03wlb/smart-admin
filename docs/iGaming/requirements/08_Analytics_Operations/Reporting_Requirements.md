# Reporting & BI Business Requirements

> **Canonical Source**: [source-archive/08_Analytics_BI/08-01_Reporting_BI.md](../../source-archive/08_Analytics_BI/08-01_Reporting_BI.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [Reporting Architecture](../../architecture/08_Analytics_Service/Reporting_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This requirements document delivers strategic value by:
- **Revenue Visibility**: Defines GGR/NGR calculations with hourly refresh enabling real-time P&L monitoring across game types and player segments
- **Risk Mitigation**: Specifies 4 suspicious betting detection types (Hedging, Arbitrage, Abnormal Win Rate, Turnover Anomaly) with real-time 5-minute delay alerting
- **Regulatory Compliance**: Documents MGA regulatory report requirements (monthly GGR, RTP verification, Responsible Gaming metrics) and AML/KYC reporting (SAR filing, SOF verification)
- **Operational Efficiency**: Establishes tiered export rules (sync <100k rows, async >100k rows) with role-based access control across 6 user categories

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Real-time Dashboard Freshness | < 5 minutes delay | Data lag from event to dashboard |
| Real-time Dashboard Availability | 99.9% uptime | Monthly availability percentage |
| T+1 Report Accuracy | 100% | Reconciliation with source systems |
| T+1 Report Availability | By 03:00 AM daily | Report generation completion time |
| Monthly Compliance Report SLA | Available by 2nd of month | Report delivery date |
| Export Task Success Rate | > 99% | Successful exports / Total export requests |

---

## 1. Business Overview

The Reporting & BI system provides **data insights** and **decision support** for different roles across the iGaming platform. The system must support multi-dimensional analysis, near real-time monitoring, compliance reporting, and self-service analytics.

### 1.1 Core Capabilities

| Capability | Description |
|-----------|-------------|
| **Multi-dimensional Analysis** | Player behavior, game performance, financial metrics, risk events |
| **Timeliness** | T+0 real-time (5-minute delay), T+1 daily, T+7 weekly, T+30 monthly |
| **Visualization** | Dashboards, self-service analytics, Excel/PDF exports |
| **Compliance** | MGA/Curacao regulatory reports, audit trails |
| **Extensibility** | Custom reports, data export capabilities |

---

## 2. Financial Reports

### 2.1 GGR/NGR Report (Gross/Net Gaming Revenue)

**Purpose**: Core revenue metrics, required for regulatory compliance.

**Business Formulas**:
- **GGR** = Total Bets - Total Wins
- **NGR** = GGR - Bonuses - Chargebacks - Refunds

**Key Indicators**:

| Indicator | Breakdown |
|-----------|-----------|
| GGR by Game Type | Slots, Live Casino, Sports |
| NGR by Player Segment | VIP, Regular, New |
| GGR Margin | GGR / Total Bets |
| NGR Margin | NGR / GGR |

**Refresh Frequency**: Hourly (T+0)

---

### 2.2 Deposit & Withdrawal Report

**Purpose**: Fund flow monitoring, PSP reconciliation.

**Key Indicators**:

| Indicator | Breakdown |
|-----------|-----------|
| Total Deposits | By PSP, currency, region |
| Total Withdrawals | By approval status, risk level |
| Net Deposit | Deposit - Withdrawal |
| PSP Success Rate | Success rate, average fee |
| Deposit/Withdrawal Ratio | Health indicator |

**Refresh Frequency**: Real-time (5-minute delay)

---

### 2.3 Commission Report

**Purpose**: Agent commission calculation and disbursement tracking.

**Refresh Frequency**: Daily (T+1)

---

## 3. Operational Reports

### 3.1 Player Activity Report

**Key Indicators**:

| Indicator | Definition |
|-----------|------------|
| **DAU** | Daily Active Users |
| **MAU** | Monthly Active Users |
| **Stickiness** | DAU / MAU ratio |
| **New Registrations** | Daily new player count |
| **Churn Rate** | Percentage of players who stopped playing |

**Refresh Frequency**: Daily (T+1)

---

### 3.2 Game Performance Report

**Key Indicators**:

| Indicator | Description |
|-----------|-------------|
| Total Bets / Wins | Per game breakdown |
| Actual RTP | Actual Return to Player percentage |
| Average Bet Size | Mean bet amount per game |
| Top Performing Games | Ranked by GGR |
| Game Popularity | By active player count |

**Refresh Frequency**: Hourly (T+0)

---

### 3.3 Campaign Performance Report

**Key Indicators**:

| Indicator | Description |
|-----------|-------------|
| Bonus Issued / Redeemed | Total bonus amounts and redemption rates |
| Wagering Completion Rate | Percentage of players completing bonus requirements |
| CPA | Cost Per Acquisition |
| ROI | Return on Investment for campaign |
| Bonus Abuse Detection | Rate of detected bonus abuse |

**Refresh Frequency**: Daily (T+1)

---

## 4. Risk Control Reports

### 4.1 Suspicious Betting Report

**Trigger Conditions**:

| Detection Type | Description |
|---------------|-------------|
| Hedging | Simultaneous opposite bets detected |
| Arbitrage | Cross-platform value exploitation |
| Abnormal Win Rate | Win rate exceeding 55% |
| Turnover Anomaly | Large wagering completed in short time |

**Refresh Frequency**: Real-time (5-minute delay)

---

### 4.2 Multi-Account Detection Report

**Detection Dimensions**:

| Dimension | Description |
|-----------|-------------|
| Device Fingerprint | Same device used by multiple accounts |
| IP Address | Same IP for multiple accounts |
| Payment Method | Same payment instrument across accounts |
| Behavioral Similarity | ML model-based betting pattern analysis |

**Refresh Frequency**: Daily (T+1)

---

### 4.3 Withdrawal Risk Report

**Key Indicators**:

| Indicator | Description |
|-----------|-------------|
| High Risk Withdrawals | Amounts exceeding $10,000 |
| KYC Unverified Withdrawals | Withdrawals without identity verification |
| First Deposit Bonus Abuse | Deposit-and-immediate-withdrawal pattern |
| Average Approval Time | Average time for withdrawal review |
| Rejection Rate by Reason | Categorized rejection analysis |

**Refresh Frequency**: Real-time (5-minute delay)

---

## 5. Compliance Reports

### 5.1 MGA Regulatory Report (Malta Gaming Authority)

**Required Content**:

| Requirement | Description |
|-------------|-------------|
| Total GGR | Breakdown by game type |
| Total Bets / Total Wins | Aggregate figures |
| Player Count | By jurisdiction |
| RTP Verification | Actual RTP vs declared RTP |
| Responsible Gaming Metrics | Self-exclusion count, deposit limit settings |

**Submission Frequency**: Monthly (T+30)

---

### 5.2 AML/KYC Report (Anti-Money Laundering / Know Your Customer)

**Key Indicators**:

| Indicator | Description |
|-----------|-------------|
| High-Value Transactions | Single transactions exceeding $10,000 |
| Suspicious Activity Reports (SAR) | Filed SAR count |
| KYC Verification Rate | Percentage of verified players |
| EDD Required Cases | Enhanced Due Diligence cases |
| SOF Verification Rate | Source of Funds verification completion |

**Refresh Frequency**: Daily (T+1)

---

## 6. Report Export Requirements

### 6.1 Supported Formats

| Format | Use Case | Maximum Capacity |
|--------|----------|-----------------|
| **Excel (XLSX)** | Multi-sheet dimensional reports | 1,048,576 rows |
| **PDF** | Archival and print-friendly reports | Unlimited pages |
| **CSV** | Bulk data export for ETL | Billions of rows (streaming) |

### 6.2 Export Rules

| Rule | Description |
|------|-------------|
| Small exports (< 100k rows) | Synchronous download |
| Large exports (> 100k rows) | Asynchronous with email/in-app notification |
| Download link validity | 24 hours |
| Concurrent export limit | Maximum 5 per tenant |

---

## 7. Access Control Requirements

| Role | Accessible Reports | Restrictions |
|------|-------------------|--------------|
| **C-Level Executive** | All platform reports | Cross-tenant view |
| **Operations Manager** | Operational + Financial reports | Own tenant only |
| **Risk Analyst** | Risk control reports | All tenants |
| **Agent** | Commission + downstream player reports | Own hierarchy only |
| **Finance** | Financial + commission reports | Own tenant only |
| **BI Analyst** | Self-service query access | Read-only, no PII access |

---

## 8. Business SLAs

| Report Category | Freshness SLA | Availability SLA | Accuracy |
|----------------|---------------|-------------------|----------|
| Real-time Dashboard | < 5 minutes delay | 99.9% | 95%+ (approximate) |
| T+1 Daily Reports | Available by 03:00 AM | 99.5% | 100% (exact) |
| Weekly Reports | Available by Monday 06:00 AM | 99.5% | 100% (exact) |
| Monthly Compliance Reports | Available by 2nd of month | 99% | 100% (auditable) |

---

## 9. Rate Limiting Requirements

| Limit Type | Threshold |
|-----------|-----------|
| Per user per minute | 10 queries |
| Per tenant per minute | 100 queries |
| Export task concurrency | Maximum 5 |

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Data Team & BI Team
