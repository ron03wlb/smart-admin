# KYC/AML Compliance Requirements

> **Canonical Source**: [source/05_Risk_Control/05-03_KYC_AML.md](../../source/05_Risk_Control/05-03_KYC_AML.md)
> **Audience**: Executives, Compliance Officers, Product Managers
> **Related Doc**: [KYC_Verification_API.md](../../architecture/05_Risk_Engine/KYC_Verification_API.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## Executive Summary

SmartAdmin iGaming platform implements a comprehensive KYC/AML compliance system to meet global multi-jurisdictional regulatory requirements. The system adopts a **tiered verification approach** (L0/L1/L2/L3) and a **Risk-Based Approach** for AML monitoring, balancing user experience with compliance costs.

### Core Capabilities

| Feature | Description | Business Value |
|---------|-------------|----------------|
| **Tiered Verification** | L0 (Registration) -> L1 (Identity) -> L2 (Address) -> L3 (SOF/SOW) | Reduces first-time registration friction with progressive trust building |
| **Automated Review** | AI OCR + Facial Recognition, 90% auto-approval rate | Reduces labor costs, improves review efficiency |
| **Risk-Based Approach** | Enhanced Due Diligence (EDD) for high-risk customers | Focuses resources on high-risk scenarios |
| **Multi-Jurisdiction Support** | UK UKGC, Malta MGA, Gibraltar, Curacao | One system adapts to multiple markets |
| **Audit Logs** | 7-year retention period, meets AML regulatory requirements | Compliance audits, historical traceability |

---

## Regulatory Penalty Case Studies

**2023 European regulatory fines totaled GBP 348 million / USD 443 million**. Major cases include:

- **Entain (Ladbrokes Coral)**: GBP 17M fine - Failed to conduct proper Source of Funds (SOF) checks
- **Betfred**: GBP 3.25M fine - Allowed a player to deposit GBP 210,000 over 8 months without triggering AML alerts
- **Caesars Entertainment**: USD 15M ransom payment - Internal systems breached due to insufficient employee security awareness

### Key Lessons

- Robust Customer Due Diligence (CDD) procedures are critical
- Real-time transaction monitoring is essential
- VIP customers require appropriate SOF/SOW checks
- Employee security awareness training cannot be overlooked

> **VIP No Exemption Principle**: VIP players must undergo **exactly the same** risk control checks as regular players. VIP status only affects review queue priority, and **never** affects rule trigger thresholds or CDD/EDD requirements.
> **Reference Case**: Entain GBP 17M fine (2022) - VIP customer due diligence failure.

---

## 1. KYC Tiered Verification System

### 1.1 Four-Level Certification Standards

SmartAdmin adopts a **progressive KYC system** that dynamically adjusts verification requirements based on player behavior:

| Level | Verification Requirements | Withdrawal Limit | Use Case | Automation Level |
|-------|--------------------------|------------------|----------|------------------|
| **L0 (Registration)** | Phone/Email verification | No withdrawals | Initial registration, trial mode | 100% automated |
| **L1 (Identity)** | Upload ID (Passport/ID Card), AI OCR + Facial comparison | <= $2,000/day | Small players, daily withdrawals | 90% automated |
| **L2 (Address)** | Upload utility bill/bank statement, address matching | <= $10,000/day | Medium players, VIP level upgrades | 70% automated |
| **L3 (Source of Wealth)** | Provide SOF (Source of Funds), SOW (Source of Wealth) documents | Unlimited | Large transactions (> EUR 10,000), PEPs (Politically Exposed Persons) | 0% automated (manual review) |

### 1.2 Upgrade Trigger Rules

**L1 Upgrade Triggers**:
- UKGC: GBP 2,000 cumulative deposits OR first withdrawal request
- General: Cumulative deposit >= 2,000 currency units

**L2 Upgrade Triggers**:
- Cumulative deposit >= 5,000 currency units
- VIP level >= 3

**L3 EDD Triggers**:
- Single transaction >= 10,000 currency units
- Cumulative deposit >= 50,000 currency units
- Identified as PEP
- Risk score >= 80

### 1.3 Key Performance Indicators (KPI)

- **Auto-approval rate**: Target >= 90% (L1), >= 70% (L2)
- **Manual review SLA**: 95% cases completed within 24 hours
- **False positive rate**: < 2% (legitimate players incorrectly rejected)
- **Average review time**: L1 < 5 minutes, L2 < 30 minutes

### 1.4 Third-Party Provider Options

| Provider | Function | Cost | Recommendation |
|----------|----------|------|----------------|
| **Sumsub** | Full KYC flow (OCR + Face + Liveness) | $0.5-2.0/verification | Small operators (< 10K players) |
| **Jumio** | Identity verification + AML screening | $1.0-3.0/verification | Medium operators (10K-100K) |
| **AWS Rekognition** | Facial comparison + Liveness detection | $0.001/image | All sizes |
| **Tesseract OCR** | Open-source OCR (self-hosted) | Free | Large operators (> 100K) |
| **Cifas** | UK National Fraud Database | GBP 5,000/year | UK-licensed operators |

**Recommended Solutions**:
- **Small operators (< 10K players)**: Sumsub all-in-one (fast deployment)
- **Medium operators (10K-100K)**: Jumio + AWS Rekognition (cost-optimized)
- **Large operators (> 100K)**: Self-built OCR + AWS Rekognition (lowest cost)

---

## 2. AML Anti-Money Laundering Requirements

### 2.1 Money Laundering Three Stages

| Stage | Description | Detection Focus |
|-------|-------------|-----------------|
| **Placement** | Illegal cash deposited into gambling accounts via small frequent deposits to avoid CDD thresholds | Multi-account detection, deposit frequency monitoring |
| **Layering** | Multiple low-risk bets (sports arbitrage, low-stake slots), cross-account transfers | Abnormal betting patterns, effective wager rate < 30% |
| **Integration** | Withdrawal to bank accounts, funds appear as legitimate gambling winnings | Large withdrawal monitoring, fund flow aggregation |

### 2.2 Customer Due Diligence (CDD) Requirements

**Standard CDD Process**:
1. **Customer Identification**: Collect basic information (name, DOB, address, nationality)
2. **Identity Verification**: Government-issued ID + address proof
3. **Ongoing Monitoring**: Transaction behavior anomaly detection, periodic re-verification (every 12 months)

**Trigger Conditions by Jurisdiction**:

| Jurisdiction | CDD Threshold | Notes |
|--------------|---------------|-------|
| **EU** | Single transaction >= EUR 2,000 | EU AML 5th Directive |
| **UK (UKGC)** | Cumulative deposit >= GBP 2,000 OR first withdrawal | Most stringent |
| **Malta (MGA)** | Single transaction >= EUR 2,000 | EU-aligned |

### 2.3 Enhanced Due Diligence (EDD) Requirements

**Applicable Subjects**:
- **Politically Exposed Persons (PEPs)**: Current/former government officials, legislators, military officers
- **High-Risk Region Customers**: FATF blacklist countries (North Korea, Iran, Myanmar)
- **Large Transaction Customers**: Single deposit > EUR 10,000 OR cumulative deposit > EUR 50,000

**Additional Requirements**:
- Source of Wealth (SOW): Salary slips, property documents, inheritance documents
- Source of Funds (SOF): Bank statements, investment portfolio reports
- Purpose of Transaction: Declaration of gambling motivation
- Senior Management Approval: Requires MLRO (Money Laundering Reporting Officer) sign-off

### 2.4 Suspicious Activity Report (SAR) Requirements

**Mandatory Reporting Scenarios**:
- Player refuses to provide KYC documents or provides forged documents
- Fund flow aggregation: Multiple accounts withdrawing to the same bank account
- Abnormal betting patterns: Sports arbitrage, effective wager rate < 20%
- Large cash transactions: Single deposit > EUR 10,000 without reasonable explanation
- Shared account usage: Frequent device/IP changes on login

**Reporting Deadlines by Jurisdiction**:

| Jurisdiction | Deadline | Reporting Authority |
|--------------|----------|---------------------|
| **UK (UKGC)** | **7 working days** from suspicion formed | NCA (National Crime Agency) |
| **Malta (MGA)** | 15 days | FIAU (Financial Intelligence Analysis Unit) |
| **Gibraltar** | 7 days | GFIU (Gibraltar Financial Intelligence Unit) |
| **Curacao** | 30 days | Gaming Control Board |

> **Important Correction (2026-02-07)**: Per UKGC AML Guidance 2023 and POCA 2002 Section 330, SAR must be submitted within **7 working days** of suspicion forming, not 14 days.

### 2.5 Transaction Monitoring Rules

| Indicator | Threshold | Action | False Positive Rate |
|-----------|-----------|--------|---------------------|
| **Deposit Frequency** | > 10 transactions/hour | FLAG (manual review) | 8% |
| **Small Frequent Deposits** | > 20 deposits < EUR 50/day | FLAG (suspicious placement) | 12% |
| **Two-Sided Betting** | Sports hedge > EUR 1,000 | BLOCK (money laundering risk) | 3% |
| **Fund Aggregation** | 3+ accounts -> same bank account | BLOCK + SAR | 1% |
| **Effective Wager Rate** | < 20% (consecutive 7 days) | FLAG (low-risk games) | 15% |
| **Rapid Withdrawal** | Withdrawal within 24h of deposit | FLAG (test account) | 20% |

**Effective Wager Rate Formula**:
```
Effective Wager Rate = (Actual Risk Wager Amount / Total Wager Amount) x 100%

Actual Risk Wager Calculation:
- Sports arbitrage betting: 0% (fully hedged)
- Slot low stake (< 10% minimum bet): 30%
- Live games normal betting: 100%
```

---

## 3. Multi-Jurisdiction Requirements

### 3.1 Regulatory Authority Comparison

| Jurisdiction | Regulator | CDD Threshold | SAR Deadline | Special Requirements | Strictness |
|--------------|-----------|---------------|--------------|----------------------|------------|
| **UK** | UKGC | GBP 2,000 cumulative | 7 working days | Credit card gambling banned, GamStop mandatory | Most strict |
| **Malta** | MGA | EUR 2,000 single | 15 days | EU AML Directive compliant, 10-year license | High |
| **Gibraltar** | GRA | GBP 2,000 | 7 days | Dedicated AML Code of Practice, audit requirements | High |
| **Curacao** | Gaming Control Board | USD 2,500 | 30 days | Crypto allowed, less stringent oversight | Moderate |
| **Philippines** | PAGCOR | PHP 100,000 (~USD 2,000) | 5 working days | AMLC supervision, local server required | Moderate-High |

### 3.2 UK UKGC Special Requirements (Most Stringent)

**2025 New Regulations**:
- **Instant KYC Verification** (from January 2025): 72-hour grace period removed, **must verify before deposits**
- **Affordability Assessment**: GBP 125-500 display warning, GBP 500-2,000 player self-declaration, > GBP 2,000 third-party verification
- **Gambling Levy**: From April 6, 2025, tiered by GGR (0.1%-1.1%)
- **Credit Card Ban** (from April 2020): No credit card gambling
- **Mandatory SOF Checks**: At GBP 2,000 cumulative deposit
- **GamStop Self-Exclusion**: Must integrate national self-exclusion system
- **Affordability Checks**: Net loss > GBP 2,000/90 days requires financial capability assessment
- **No VIP Exemptions**: All players (including VIP) must undergo same AML checks

**Penalty Cases**:
- **Entain (2022)**: GBP 17M - Failed SOF checks on VIP players
- **Betfred (2021)**: GBP 3.25M - Failed to trigger AML alerts (player deposited GBP 210,000 over 8 months)

### 3.3 EU AML Fifth Directive (5AMLD)

**Core Changes** (effective 2020):
- Virtual currency exchanges brought under regulation
- Anonymous prepaid card limit reduced to EUR 150
- High-risk third country list updated (23 countries)
- Beneficial ownership transparency requirements

**Impact on iGaming Platforms**:
- Cryptocurrency deposits require KYC (even small amounts)
- Prepaid card payments require additional verification
- Players from high-risk countries automatically trigger EDD

---

## 4. Politically Exposed Persons (PEP) Handling

### 4.1 PEP Definition and Classification

Per FATF guidelines and EU 4AMLD/5AMLD:

| Category | Definition | Risk Level |
|----------|------------|------------|
| **Foreign PEP** | Senior foreign government officials, legislators, judges, military officers, state enterprise executives | Highest |
| **Domestic PEP** | Senior domestic government officials and equivalent positions | High |
| **International Organization PEP** | Senior positions in international organizations (UN, EU, IMF, etc.) | High |
| **PEP Associates** | PEP family members, close business associates | Medium-High |

**Family Member Definition**:
- Spouse/cohabiting partner
- Children and their spouses/partners
- Parents

**Close Business Associate Definition**:
- Person who co-beneficially owns a legal entity with a PEP
- Person with close business relationship with a PEP
- Sole beneficial owner of a legal entity where PEP actually benefits

### 4.2 PEP Screening Process

1. Screen against PEP lists (World-Check, Dow Jones) at registration and periodic re-screening
2. **Exact match** (>= 95% confidence): Automatically trigger EDD, account restrictions
3. **Fuzzy match** (70-95%): Manual review queue (24h SLA)
4. **No match**: Normal process
5. If confirmed PEP: Request SOF + SOW documents, MLRO approval required
6. Ongoing monitoring: Monthly transaction review, annual re-screening

### 4.3 PEP EDD Requirements

Required documents for PEP accounts:
- **SOF (Source of Funds)**: Bank statements showing fund origin
- **SOW (Source of Wealth)**: Documentation of overall wealth accumulation
- **PEP Declaration**: Signed declaration of PEP status
- **Purpose Statement**: Account usage explanation

---

## 5. Counter-Terrorism Financing (CTF)

### 5.1 CTF Regulatory Framework

| Authority | List Type | Update Frequency |
|-----------|-----------|------------------|
| **UN** | UN Security Council Sanctions | Immediate |
| **OFAC** | SDN List (US) | Daily |
| **EU** | EU Consolidated Sanctions | Weekly |
| **UK** | UK Sanctions List | Daily |
| **FATF** | High-Risk Jurisdictions | Quarterly |

### 5.2 Sanctions Screening Requirements

When a player matches a sanctions list:
1. **Immediate account freeze** - prohibit all transactions
2. **Notify MLRO** within 30 minutes
3. **MLRO confirms match** - rule out namesakes
4. If confirmed: **Report to regulatory authority** within 24 hours
5. Maintain freeze pending regulatory decision
6. Execute confiscation or unfreeze per regulatory instruction

### 5.3 High-Risk Jurisdictions

**FATF Blacklist (High-Risk)**:
- North Korea (KP), Iran (IR), Myanmar (MM)

**FATF Greylist (Increased Monitoring)**:
- Syria, Yemen, Afghanistan, Albania, Burkina Faso, Cameroon, Congo (DR), Ghana, Haiti, Jamaica, Jordan, Mali, Mozambique, Nicaragua, Nigeria, Panama, Philippines, Senegal, South Sudan, Tanzania, Togo, Uganda, UAE, Vietnam

Players from these jurisdictions automatically trigger EDD.

---

## 6. Smurfing (Structuring) Detection

### 6.1 Definition

Smurfing (also called Structuring) is a money laundering technique that splits large transactions into multiple small transactions to evade reporting thresholds.

| Characteristic | Description |
|----------------|-------------|
| **Purpose** | Evade CDD/EDD trigger thresholds |
| **Method** | Split transactions below thresholds |
| **Common Thresholds** | EUR 2,000 (MGA) / GBP 2,000 (UKGC) |

### 6.2 Detection Rules

| Rule | Condition | Risk Level | Action |
|------|-----------|------------|--------|
| **High-frequency near-threshold** | 24h: >= 3 deposits of EUR 1,500-1,999 | Critical | SAR + Freeze |
| **Cumulative exceeded** | 7-day cumulative deposit > EUR 10,000 (multiple small amounts) | High | Trigger EDD |
| **Split pattern** | Multiple small test deposits before large deposit | High | FLAG |
| **Cross-account split** | Linked accounts total exceeds threshold | Critical | SAR |

---

## 7. MLRO Role and Responsibilities

### 7.1 MLRO Definition

Money Laundering Reporting Officer (MLRO) is a key compliance role required by regulators.

| Regulator | MLRO Requirement |
|-----------|------------------|
| **UKGC** | Must designate, reports to NCA |
| **MGA** | Must designate, reports to FIAU |
| **Gibraltar** | Must designate, reports to GFIU |

### 7.2 MLRO Core Responsibilities

**Daily Responsibilities**:
- Review and decide whether to submit SAR
- Maintain AML policies and procedures
- Oversee transaction monitoring systems
- Handle internal whistleblowing

**Periodic Responsibilities**:
- Monthly: AML alert statistics report
- Quarterly: Report AML status to Board
- Annual: AML policy review and update
- Annual: Employee AML training program

**Emergency Responsibilities**:
- Urgent SAR submission (terrorism financing, etc.)
- Regulatory investigation cooperation
- Asset freeze order execution

**Authority Requirements**:
- Independent reporting line (direct to Board)
- Access to all AML-related information
- Authority to freeze suspicious accounts
- Authority to reject high-risk customers

---

## 8. Compliance Checklist

### 8.1 Pre-Launch Requirements

- [ ] KYC tiered system implemented (L0/L1/L2/L3)
- [ ] Automated review process (OCR + Facial comparison + Liveness detection)
- [ ] AML transaction monitoring rules (at least 6 core rules)
- [ ] SAR reporting process (with deadline monitoring)
- [ ] Audit log Hash Chain (7-year retention)
- [ ] Third-party integration (Sumsub/Jumio or self-built)
- [ ] Blacklist integration (Cifas/PEPs/FATF)
- [ ] Employee AML training (2 times per year)
- [ ] MLRO designated (Money Laundering Reporting Officer)
- [ ] Compliance policy documentation (AML Policy, KYC Manual)

### 8.2 Periodic Maintenance

- [ ] Quarterly audit (Hash Chain verification, SAR timeliness rate)
- [ ] Monthly blacklist update (Cifas, PEPs)
- [ ] Semi-annual risk model tuning (reduce false positive rate)
- [ ] Annual regulatory requirement update (UKGC/MGA policy changes)

---

## 9. Best Practices

### 9.1 Balancing Compliance and User Experience

| Scenario | Traditional Approach (High Friction) | SmartAdmin Optimization |
|----------|--------------------------------------|-------------------------|
| **First Registration** | Immediately require ID upload | L0 (phone only) -> Allow trial -> L1 on first withdrawal |
| **KYC Review** | 1-3 day manual review | 90% auto-approval (< 5 min) -> Only 10% manual review |
| **Large Transactions** | Immediate freeze + SOF request | Soft prompt (advance notice) + 48-hour buffer |
| **VIP Players** | KYC exemption (violation) | Equal treatment + Dedicated support for document preparation |

### 9.2 Common Pitfalls to Avoid

| Mistake | Risk | Correct Approach |
|---------|------|------------------|
| VIP exemption from KYC | Entain fined GBP 17M for this | All players undergo same AML process |
| No alerts for small frequent deposits | Structuring evades regulation | Set > 20 deposits < EUR 50/day to trigger FLAG |
| Delayed SAR submission | Regulatory fines + license revocation | Automated process + SLA monitoring (100% within deadline) |
| Unencrypted/modifiable audit logs | Compliance audit failure | Hash Chain + HMAC tamper-proofing + 7-year retention |

### 9.3 Cost Optimization

**Cost Estimate (100K players/year)**:
- Full outsourcing: $150,000 - $200,000/year
- Hybrid approach (recommended): $30,000 - $50,000/year
- Fully self-built: $5,000 - $10,000/year (excluding labor costs)

---

## Related Documentation

### Business References
- [05-01 Risk Framework](../../source/05_Risk_Control/05-01_Risk_Framework.md) - AML monitoring, risk scoring model
- [05-02 Fraud Detection](../../source/05_Risk_Control/05-02_Fraud_Detection.md) - Fund flow aggregation detection
- [01-01 Player Lifecycle](../../source/01_Player_Center/01-01_Player_Lifecycle.md) - KYC trigger logic, registration flow

### Compliance Framework
- [06-08 UKGC Compliance](../../source/06_Platform_Governance/06-08_UKGC_Compliance.md) - UK license specific requirements (2025 new regulations)
- [06-09 MGA Compliance](../../source/06_Platform_Governance/06-09_MGA_Compliance.md) - Malta license requirements
- [15 Responsible Gambling](../../source/15_Responsible_Gambling/) - Complete responsible gambling module

### External References
- FATF Guidance: [www.fatf-gafi.org](https://www.fatf-gafi.org)
- UKGC AML Guide: [www.gamblingcommission.gov.uk](https://www.gamblingcommission.gov.uk)

---

**Document Maintenance**: Quarterly update of regulatory requirements and penalty cases
**Last Review**: 2026-02-08 (Compliance Team)
