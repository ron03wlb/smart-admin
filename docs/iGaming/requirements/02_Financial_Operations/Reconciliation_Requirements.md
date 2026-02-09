# Reconciliation Requirements

> **Canonical Source**: [02-03_Reconciliation_System.md](../../source/02_Finance_Center/02-03_Reconciliation_System.md)
> **Audience**: Executives, Compliance Officers, Finance Team, Operations Managers
> **Related Doc**: [Reconciliation_Technical.md](../../architecture/02_Finance_Service/Reconciliation_Technical.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (PostgreSQL/S3 Glacier storage, UTC timezone conversion algorithms, 3DS verification implementation, blockchain confirmation counts) moved to Architecture layer. This document focuses on business requirements only.

---

## 1. Business Overview

Reconciliation is the last line of defense for platform fund security and data accuracy. The system compares "Internal Ledger" with "External Statements" and generates financial reports.

---

## 2. Core Functional Requirements

### 2.1 Three-Way Matching

The system must compare data from three sources:

1. **Platform Orders**: Player deposit/withdrawal records
2. **PSP Reports**: Actual fund movement records from payment providers
3. **Bank Statements**: Final fund landing records (when available)

### 2.2 Three-Tier Reconciliation Architecture

| Tier | Execution Frequency | Data Sources | Matching Method | Latency | Primary Purpose | Automation Level |
|------|---------------------|--------------|-----------------|---------|-----------------|------------------|
| **Tier 1: Real-time** | Within 30 seconds of transaction | Platform <-> PSP API | Active query verification | < 1 min | Prevent fake callback attacks | 100% Automated |
| **Tier 2: Batch** | Hourly | Platform <-> PSP API | Batch query comparison | 1 hour | Detect delayed/dropped orders | 95% Automated (exceptions manual) |
| **Tier 3: T+1 Daily** | Daily at 02:00 AM | Platform <-> PSP <-> Bank | Complete three-way matching | 1 day | Financial reports + Audit | 80% Automated (discrepancies manual) |

### 2.3 Tolerance Configuration

| Tolerance Type | Value | Description |
|----------------|-------|-------------|
| Amount Tolerance | 2% or $1 (whichever is smaller) | Acceptable difference range |
| Time Tolerance | 10 minutes | Fuzzy matching time window |
| Auto-Approve Threshold | < $10 difference within tolerance | Automatic pass without review |

---

## 3. Discrepancy Handling Requirements

### 3.1 Discrepancy Types

| Type | Definition | Risk Level | Response |
|------|------------|------------|----------|
| **Over (Long)** | External has money, platform has no order | MEDIUM | Manual supplement entry |
| **Short** | Platform has successful order, external has no money | CRITICAL | Immediate alert + freeze player account |
| **Amount Mismatch** | Platform order $100, actual deposit $90 | VARIES | Configure tolerance range |

### 3.2 Discrepancy Handling Matrix

| Discrepancy Type | Amount Range | Risk Level | Response Time | Approval Authority | Automation | Typical Cause |
|------------------|--------------|------------|---------------|-------------------|------------|---------------|
| **Over Payment** | < $10 | LOW | 24 hours | Finance Specialist | 80% Auto | Test transactions, dropped orders |
| | $10-$1000 | MEDIUM | 4 hours | Finance Manager | 50% Auto | Player mis-transfer, dropped orders |
| | >= $1000 | HIGH | 1 hour | CFO | 0% (Full manual) | Large dropped order, abnormal transfer |
| **Short Payment** | Any amount | CRITICAL | Immediate | CTO + CFO | 0% (Full manual) | Fake callback, fraud attack |
| **Amount Mismatch** | < $1 (within tolerance) | LOW | Auto-pass | System Auto | 100% Auto | Exchange rate fluctuation, precision error |
| | $1-$10 | LOW | 24 hours | System Auto | 100% Auto | Fee deduction, small variance |
| | $10-$100 | MEDIUM | 4 hours | Finance Manager | 0% (Full manual) | Rate error, fee config error |
| | >= $100 | HIGH | 1 hour | CFO | 0% (Full manual) | System error, PSP calculation error |

### 3.3 Escalation Path

| Level | Handler | Decision Authority | SLA | Escalation Condition |
|-------|---------|-------------------|-----|---------------------|
| **L1** | System Auto | Risk Flag + Reconciliation Exception -> Auto-suspend | Immediate | Auto-triggered |
| **L2** | Finance Manager | Conflicts < $10,000 | 4h | L1 unresolved |
| **L3** | CTO + CFO | >= $10,000 or involving SAR | 1h | L2 unresolved |
| **L4** | Board | Regulatory reports or major funds | 24h | L3 requires board notification |

---

## 4. Dispute Resolution Procedures

### 4.1 Over Payment (Long) Handling

**Step 1: Verify Data Source**
- Check if PSP transaction is a test environment transaction
- If test data entered production -> Ignore

**Step 2: Query Player Account**
- Search player by PSP report email/phone
- Check for pending orders with matching amount

**Step 3: Supplement Order**
- Auto-supplement conditions: Amount < $100 AND player is trusted
- Otherwise: Submit for manual review with supporting documents

### 4.2 Short Payment Handling

**CRITICAL ALERT**: Possible fake callback attack!

**Step 1: Immediate Freeze**
- Freeze player account immediately
- Update player status to 'frozen'

**Step 2: Investigation**
1. Check PSP callback logs (IP, timestamp, signature)
2. Contact PSP customer service to confirm payment receipt
3. If PSP confirms no payment -> Rollback player balance

**Step 3: Resolution**
- Confirmed fraud: Rollback balance + Risk alert + Consider police report (if > $10,000)
- PSP data issue: Wait for PSP data sync (24-48 hours) then recheck

### 4.3 Amount Mismatch Handling

**Tolerance Check Process**:
1. Calculate difference: `diff = |platform - psp|`
2. Calculate percentage: `diff_pct = diff / platform × 100%`
3. Apply tolerance rules per matrix above

---

## 5. Daily Closing Process

### 5.1 Automated Schedule

- **Execution Time**: 02:00 AM daily
- **Target Data**: Previous day's PSP reports (CSV/API)
- **Matching Method**: Key-Value comparison (Order ID)

### 5.2 Daily Report Contents

| Report Item | Description |
|-------------|-------------|
| Total Deposits | Sum of all deposits for the day |
| Total Withdrawals | Sum of all withdrawals for the day |
| Fees | Total transaction fees |
| Discrepancy Count | Number of unmatched records |
| Discrepancy Amount | Total unmatched amount |

---

## 6. Agent Settlement Reconciliation

For credit network model operations:

### 6.1 Core Formula

```
Net Payable = (Total Win/Loss - Commission) + Adjustment (Late Settlement)
```

### 6.2 Input Data

1. **System Report**: System-generated weekly statement
2. **Manual Input / Bank Statement**: Actual remittance proof (USDT TxID or bank statement)

### 6.3 Write-off Logic

| Condition | Action |
|-----------|--------|
| Match (difference < $10) | Auto-execute Credit Reset, restore agent credit limit |
| Mismatch | Generate Outstanding Debt ticket, freeze agent credit until difference is settled |

---

## 7. Alert Notification Rules

| Discrepancy Type | Alert Level | Notification Channel | Recipients | Frequency | Continuous Alert Condition |
|------------------|-------------|---------------------|------------|-----------|---------------------------|
| Short Payment | P0 | SMS + Email + Slack | CTO, CFO, Risk Team | Immediate + Every 30 min | Unprocessed |
| Large Over (>=$10k) | P1 | Email + Slack | CFO, Finance Manager | Immediate + Every 2 hours | Unapproved |
| Medium Over ($1k-$10k) | P2 | Email | Finance Manager | Immediate + Daily summary | 24h unprocessed |
| Small Over (<$1k) | P3 | Email | Finance Specialist | Daily summary | No continuous |
| Large Amount Mismatch (>=$100) | P2 | Email + Slack | Finance Manager | Immediate + Every 4 hours | Unapproved |
| Small Amount Mismatch (<$100) | P3 | Email | Finance Specialist | Daily summary | No continuous |
| PSP API Failure | P1 | SMS + Slack | DevOps, Backend Team | Immediate + Every 15 min | Service not restored |

---

## 8. Approval and Permissions

### 8.1 Manual Adjustment Process

1. Finance staff identifies discrepancy, submits adjustment request
2. Fill in adjustment reason (required, minimum 20 characters)
3. Upload supporting documents (bank screenshot, PSP email reply, etc.)
4. **Mandatory Approval**: Adjustment amount > $0 requires Finance Manager approval

### 8.2 Role Permission Matrix (RBAC)

| Operation | Finance Specialist | Finance Manager | CTO | Super Admin |
|-----------|-------------------|-----------------|-----|-------------|
| View Reconciliation Report | Yes | Yes | Yes | Yes |
| Submit Adjustment Request | Yes | Yes | No | Yes |
| Approve Adjustment (< $1000) | No | Yes | Yes | Yes |
| Approve Adjustment (>= $1000) | No | No | Yes | Yes |
| Modify Reconciliation Rules | No | No | Yes | Yes |

---

## 9. Compliance Requirements

### 9.1 Audit Trail Requirements

- All manual adjustments must go through dual approval (submitter != approver)
- Adjustment amount >= $1000 must include supporting documents (bank screenshot, PSP email, etc.)
- Short payment events must be investigated and reported within 24 hours
- Audit logs must be retained for 7 years (tax compliance requirement)
- Monthly reconciliation reports must be submitted to external auditors (if applicable)
- Discrepancy handling SOP must be reviewed and updated annually (Compliance department responsibility)

### 9.2 Multi-Regulatory Data Retention Requirements

| Regulator | Reconciliation Data Retention | Game Audit Logs | Key Requirements |
|-----------|------------------------------|-----------------|------------------|
| **UKGC** | 5 years | 5 years | Complete transaction history, replay verification |
| **MGA** | 10 years | 10 years | Immutable timestamps, audit traceability |
| **PAGCOR** | 5 years | 5 years | Player self-service query |
| **Curacao** | 3 years | 3 years | Minimum requirement |
| **Tax Compliance** | 7 years | - | Financial reports and transaction vouchers |

**SmartAdmin Standard**: Adopt **10-year** unified retention period to meet all regulatory requirements.

### 9.3 Data Archival Strategy

The system MUST implement tiered storage based on data age:

| Data Tier | Scope | Access Requirement |
|-----------|-------|-------------------|
| Hot Data | Last 90 days | Immediate access (< 100ms) |
| Warm Data | 90 days - 2 years | Near real-time access (< 1s) |
| Cold Data | 2 years - 10 years | Acceptable restoration delay (minutes) |

→ **[Technical Storage Implementation](../../architecture/02_Finance_Service/Reconciliation_Technical.md#data-archival)** - PostgreSQL/MySQL configuration, S3 Glacier/Azure Archive setup, partitioning strategies

### 9.4 Monthly Compliance Report

| Report Item | Description | Regulatory Requirement |
|-------------|-------------|----------------------|
| Reconciliation Completion Rate | Monthly transactions vs reconciled transactions | UKGC/MGA require 99%+ |
| Discrepancy Resolution Time | Average processing time | P0 < 1h, P1 < 4h |
| Manual Adjustment Records | Audit logs for all adjustments | Dual approval + Supporting documents |
| PSP Reconciliation Rate | Reconciliation success rate by PSP | Identify problem channels |

---

## 10. Multi-Currency Exchange Rate Reconciliation

### 10.1 Core Principle

Record exchange rate snapshot at transaction time, compare with PSP actual settlement rate.

### 10.2 Exchange Rate Reconciliation Rules

| Variance Range | Handling | Description |
|----------------|----------|-------------|
| < 0.5% | Auto-pass | Normal exchange rate fluctuation |
| 0.5% - 1% | Record alert | Large fluctuation, record but don't block |
| 1% - 3% | Manual review | Abnormal fluctuation, verify PSP report |
| > 3% | Suspend transaction | Extreme fluctuation, possible error |

---

## 11. PSP Settlement Cycle Requirements

### 11.1 Settlement Cycle Configuration

| PSP Type | Settlement Cycle | Reconciliation Strategy |
|----------|-----------------|------------------------|
| **Instant Settlement** (PayPal, Crypto) | T+0 | Layer 1 Real-time reconciliation |
| **Next Day Settlement** (Alipay, some bank cards) | T+1 | Layer 3 Daily reconciliation |
| **Multi-Day Settlement** (Credit cards, Stripe) | T+2 ~ T+7 | Delayed reconciliation task |
| **Weekly Settlement** (some bank transfers) | T+7 | Weekly reconciliation task |

### 11.2 Delayed Reconciliation Requirements

- Mark as pending reconciliation when PSP settlement cycle > T+1
- Record expected reconciliation date
- Daily delayed reconciliation task at 03:00
- Maximum wait period: T+10 before marking as exception for manual handling

---

## 12. Refund and Chargeback Reconciliation

### 12.1 Refund Types

| Type | Initiator | Timeframe | Process |
|------|-----------|-----------|---------|
| **Active Refund** | Operator | 1-30 days after transaction | Operator initiates -> PSP confirms -> Player receives |
| **Chargeback** | Player's bank | 1-120 days after transaction | Bank notifies -> PSP deducts -> Operator disputes |
| **Pre-authorization Cancel** | Operator | Within 7 days of authorization | Operator initiates -> PSP cancels |

### 12.2 Chargeback Alert Rules

| Alert Condition | Priority | Response Time |
|-----------------|----------|---------------|
| Single Chargeback > $1,000 | P0 | Immediate |
| Player >= 3 Chargebacks in 30 days | P1 | 4h |
| Daily Chargeback Rate > 0.5% | P1 | 4h |
| Monthly Chargeback Rate > 1% | P0 | Immediate |

---

## 13. Negative Balance Reconciliation

### 13.1 Negative Balance Scenarios

| Scenario | Cause | Typical Amount | Risk Level |
|----------|-------|----------------|------------|
| **Chargeback** | Bank refund when insufficient balance | $10 - $10,000 | HIGH |
| **Game Rollback** | Void/Cancel when insufficient balance | $1 - $500 | MEDIUM |
| **System Error** | Rollback after duplicate credit | Variable | HIGH |
| **Bonus Clawback** | Bonus revoked for rule violation | $10 - $5,000 | MEDIUM |
| **Exchange Rate Adjustment** | Settlement vs transaction rate difference | $1 - $100 | LOW |

### 13.2 Auto-Processing Rules

| Condition | Handling | Description |
|-----------|----------|-------------|
| Negative balance < $10 | Auto-recover | Deduct from next deposit |
| Negative balance $10 - $100 | Notify + Wait | 7-day auto-recovery window |
| Negative balance > $100 | Freeze account | Must recover before unfreezing |
| Negative balance > $1,000 | Freeze + Risk review | Possible fraud |

### 13.3 Bad Debt Recognition Rules

| Time Condition | Amount Condition | Action |
|----------------|------------------|--------|
| > 30 days persistent | Single < $50 | Auto-recognize after 30 days |
| > 60 days persistent | Single $50-$500 | Manual review recognition |
| > 60 days persistent | Single > $500 | Legal evaluation for collection potential |
| > 90 days + VIP player | Any | Extended to 90 days |
| Under dispute | Any | Do not recognize |
| Under legal pursuit | Any | Do not recognize |

---

## 14. AML/KYC Integration Requirements

### 14.1 Integration Points

| Integration Point | Reconciliation Responsibility | AML/KYC Responsibility | Data Flow |
|-------------------|------------------------------|----------------------|-----------|
| **Large Transaction Flagging** | Flag > $10,000 transactions | Generate CTR report | Reconciliation -> AML |
| **Suspicious Transaction Report** | Identify abnormal patterns | Generate STR report | Reconciliation -> AML |
| **KYC Status Association** | Verify KYC status | Provide KYC level | KYC -> Reconciliation |
| **Sanctions Screening** | Transaction party info | List matching | Reconciliation -> AML |

### 14.2 KYC Level and Transaction Limits

| KYC Level | Single Limit | Daily Limit | Monthly Limit | Reconciliation Handling |
|-----------|--------------|-------------|---------------|------------------------|
| **NONE** | $100 | $500 | $2,000 | Auto-reject if exceeded |
| **BASIC** | $1,000 | $5,000 | $20,000 | Manual review if exceeded |
| **ENHANCED** | $10,000 | $50,000 | $200,000 | Auto-flag large transactions |
| **FULL** | $50,000 | $200,000 | Unlimited | CTR recording only |

---

## 15. Cross-Timezone Reconciliation Requirements

### 15.1 Timezone Unification Requirements

**Core Principle**: All reconciliation MUST use a unified baseline timezone to ensure accurate comparisons across different data sources.

**Data Source Considerations**:
- Platform orders (baseline timezone)
- PSP reports (may use different timezones)
- Bank statements (local timezone)
- Game provider reports (configured per provider)

### 15.2 Time Tolerance Requirements

| Setting | Value | Purpose |
|---------|-------|---------|
| Standard Window | Daily reconciliation period | Defines reconciliation day boundary |
| Tolerance Range | +/- 30 minutes | Handles cross-day boundary transactions |
| Maximum Delay | 48 hours | Covers weekends and holidays |

→ **[Timezone Conversion Implementation](../../architecture/02_Finance_Service/Reconciliation_Technical.md#timezone-handling)** - UTC conversion algorithms, timezone offset configuration, cross-day identification logic

---

## 16. Fund Segregation Verification

> **Industry Basis**: UKGC LCCP 4.2.1, MGA Rule 44
> **Penalty Case**: William Hill 6.2M GBP (2018) - Improper fund segregation

### 16.1 Verification Formula

```
Trust Account Balance >= Sum(Player Wallet Balances) + In-Transit Deposits - In-Transit Withdrawals + Safety Buffer
```

### 16.2 Verification Schedule

**Execution Time**: Daily at 03:00 UTC (after T+1 reconciliation completes)

### 16.3 Variance Handling Matrix

| Variance Type | Range | Risk Level | Handling | SLA |
|---------------|-------|------------|----------|-----|
| **Positive** (Bank > Expected) | < 5% | LOW | Record as safety buffer | 24h |
| | 5-10% | MEDIUM | Investigate GGR settlement delay | 4h |
| | > 10% | HIGH | Possible wrong credit, immediate investigation | 1h |
| **Negative** (Bank < Expected) | < 1% | MEDIUM | Check in-transit fund delays | 4h |
| | 1-5% | HIGH | Suspend large withdrawals (> $10,000) | 1h |
| | > 5% | **CRITICAL** | Freeze all withdrawals, notify regulators | Immediate |

---

## 17. Risk Control and Reconciliation Coordination

> **Industry Basis**: UKGC AML Guidance 2023, ISO 27001 5.3 (Separation of Duties)
> **Penalty Case**: Entain 17M GBP (2023) - VIP exemption from AML checks

### 17.1 Priority Definition

**Core Principle**: Risk control checks take priority over financial reconciliation; SAR investigation takes priority over all operations.

| Scenario | Risk Decision | Reconciliation Decision | Final Handling | Priority Rule |
|----------|---------------|------------------------|----------------|---------------|
| SAR under investigation + Any reconciliation operation | SAR Priority | Suspend | Wait for SAR result | **SAR > All** |
| Player frozen + Reconciliation discrepancy | Freeze funds | Need reconciliation | Complete risk investigation first | **Risk > Reconciliation** |
| Short payment + Normal player | No risk | Rollback balance | Execute reconciliation rollback | **Reconciliation Priority** |
| Large withdrawal + Reconciliation incomplete | Needs review | Unconfirmed | Delay withdrawal | **Risk = Reconciliation** |
| AML alert + Over payment supplement | AML review | Pending supplement | Complete AML review first | **Risk > Reconciliation** |

### 17.2 VIP No-Exemption Principle

> **CRITICAL**: VIP players must go through **exactly the same** risk control/reconciliation rules as regular players.

**VIP Status Impact Scope**:
- Can influence: Review queue priority (VIP processed first)
- Can influence: Customer service response SLA (VIP faster response)
- Cannot influence: Risk control rule trigger thresholds
- Cannot influence: AML check process
- Cannot influence: Discrepancy handling logic

---

## 18. Multi-Account Reconciliation

> **Industry Basis**: UKGC AML Guidance, MGA Rule 5.3.3 (Related Account Tracing)

### 18.1 Related Account Identification Sources

| Data Source | Identification Signal | Link Strength | Reconciliation Impact |
|-------------|----------------------|---------------|----------------------|
| **Neo4j Graph** | Same device fingerprint | HIGH | Merge statistics |
| **Neo4j Graph** | Same IP + concurrent activity | MEDIUM | Flag for review |
| **Neo4j Graph** | Payment card association | HIGH | Merge statistics |
| **Risk System** | Fund aggregation detection | HIGH | Reverse tracing |
| **AML Alert** | Multi-account same-direction betting | HIGH | Flag both sides |

### 18.2 Fund Tracing Rules

| Scenario | Tracing Scope | Handling | Reconciliation Flag |
|----------|---------------|----------|-------------------|
| **Confirmed Multi-Account** | All transactions from all linked accounts | Merge GGR statistics | `MULTI_ACCOUNT_LINKED` |
| **Fund Aggregation** | Loser account -> Winner account | Reverse trace fund flow | `FUND_AGGREGATION` |
| **Arbitrage Betting** | Hedging accounts on both sides | Flag both | `ARBITRAGE_PAIR` |
| **Money Laundering Suspect** | Entire linked network | Freeze all + SAR | `AML_SUSPECTED` |

---

## 19. Regulatory Reporting Integration

> **Industry Basis**: UKGC Reporting Requirements, MGA Reporting Requirements, Gambling Levy Act 2025

### 19.1 Report Types and Schedules

| Report Type | Regulator | Frequency | Source | Format | SLA |
|-------------|-----------|-----------|--------|--------|-----|
| **Monthly GGR** | UKGC/MGA | Monthly | T+1 reconciliation results | JSON/CSV | 15th of following month |
| **Gambling Levy** | UKGC | Monthly | GGR calculation table | UKGC Portal | 28th of following month |
| **Player Protection** | UKGC | Monthly | Self-exclusion + reconciliation freezes | PDF | 15th of following month |
| **SAR Summary** | NCA/FIAU | Monthly | AML alerts + reconciliation exceptions | XML | 10th of following month |
| **Jackpot Report** | All | Immediate | Jackpot reconciliation table | Webhook | Within 24h |
| **Annual Report** | UKGC/MGA | Annual | Full year reconciliation summary | PDF | 90 days after year end |

### 19.2 Data Consistency Requirement

**Verification Formula**:
```
|Regulatory Report GGR - Reconciliation Report GGR| / Reconciliation Report GGR < 0.01%
```

### 19.3 Gambling Levy Calculation (UKGC 2025)

| Annual GGR Range | Levy Rate | Calculation Basis |
|------------------|-----------|-------------------|
| 0 - 1M GBP | 0.1% | Reconciliation GGR |
| 1M - 50M GBP | 0.25% | Reconciliation GGR |
| 50M - 250M GBP | 0.5% | Reconciliation GGR |
| > 250M GBP | 1.1% | Reconciliation GGR |

---

## 20. Performance SLA Requirements

### 20.1 Tier-Level SLA

| Tier | Operation | Target Latency | Warning Threshold | Critical Threshold | Alert Priority |
|------|-----------|----------------|-------------------|-------------------|----------------|
| **L1 Real-time** | Single transaction verification | < 500ms | > 800ms | > 1s | P0 |
| **L1 Real-time** | PSP callback processing | < 30s | > 45s | > 60s | P0 |
| **L2 Batch** | Hourly reconciliation batch | < 5min | > 10min | > 15min | P1 |
| **L2 Batch** | PSP batch query (1000 records) | < 30s | > 45s | > 60s | P1 |
| **L3 Daily** | T+1 full reconciliation | < 30min | > 45min | > 1h | P2 |
| **L3 Daily** | Financial report generation | < 5min | > 10min | > 15min | P2 |

### 20.2 Match Rate SLA

| Metric | Target | Warning | Critical |
|--------|--------|---------|----------|
| **Daily Match Rate** | >= 99.5% | < 99% | < 95% |
| **First-Pass Match Rate** | >= 95% | < 90% | < 80% |
| **Discrepancy Resolution Time** | < 4h | > 8h | > 24h |
| **P0 Discrepancy Response** | < 15min | > 30min | > 1h |

### 20.3 Throughput SLA

| Scenario | Design Capacity | Warning Threshold | Critical Threshold |
|----------|-----------------|-------------------|-------------------|
| **Transactions Per Second** | 1,000 TPS | > 800 TPS | > 950 TPS |
| **Hourly Batch Volume** | 50,000 records | > 40,000 records | > 48,000 records |
| **Daily Total Volume** | 500,000 records | > 400,000 records | > 480,000 records |

---

## 21. Payment Method Specific Requirements

### 21.1 Payment Method Classification

Different payment methods have different settlement cycles and reconciliation requirements:

| Category | Payment Methods | Settlement Cycle | Reconciliation Granularity | Special Considerations |
|----------|-----------------|------------------|---------------------------|----------------------|
| **Card Payment** | Visa/MC | T+1~T+3 | Transaction level | Chargeback handling required |
| **E-Wallet** | PayPal/Skrill | T+0~T+1 | Transaction level | Instant notification, currency conversion |
| **Bank Transfer** | SEPA/Faster Payments | T+1~T+2 | Batch level | Bank reference matching |
| **Cryptocurrency** | BTC/ETH/USDT | Instant after confirmation | Block level | Confirmation requirements vary |
| **Prepaid Card** | Paysafecard | T+1 | Transaction level | PIN verification |
| **Carrier Billing** | Boku/Payforit | T+7~T+30 | Monthly | High refund rate (15-25%) |

### 21.2 Card Payment Business Rules

**Chargeback Handling**:
- Process stages: Initial notification → Evidence collection (7-14 days) → Representment → Final ruling (45-120 days)
- All chargebacks MUST be tracked and reconciled
- Authorization codes MUST be matched during reconciliation

### 21.3 Cryptocurrency Business Rules

**Confirmation Requirements**:
- Bitcoin transactions require minimum confirmations before finalization
- Ethereum/USDT transactions require minimum confirmations before finalization
- All cryptocurrency transactions MUST be verified against blockchain records

### 21.4 Carrier Billing Business Rules

**Refund Provisions**:
- Settlement cycle: T+30 (monthly)
- Expected refund rate: 15-25% (industry average)
- Provision formula: `provision = revenue × historical_refund_rate × 1.2`

→ **[Payment Method Technical Specifications](../../architecture/02_Finance_Service/Reconciliation_Technical.md#payment-methods)** - 3DS verification implementation, blockchain confirmation counts, MNO report parsing, on-chain verification logic

---

## Related Documents

### Business Logic Reference
- [02-02 Payment Gateway Integration](../../source/02_Finance_Center/02-02_Payment_Gateway_Integration.md) - PSP transaction data source
- [02-04 Turnover and Game Reconciliation Analysis](../../source/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - Game reconciliation process
- [02-06 Wallet Architecture](../../source/02_Finance_Center/02-06_Wallet_Architecture.md) - Balance adjustment logic

### Technical Architecture Reference
- [Reconciliation_Technical.md](../../architecture/02_Finance_Service/Reconciliation_Technical.md) - Technical implementation details

---

**Document Version**: 1.0.0
**Source Version**: 6.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Finance Team

**Change Log**:
- v1.0.0 (2026-02-08): Initial split from canonical source, extracted business requirements view
