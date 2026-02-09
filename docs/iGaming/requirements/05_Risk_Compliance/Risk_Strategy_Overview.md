# Risk Control Strategy Overview

> **Canonical Source**: [source/05_Risk_Control/05-01_Risk_Framework.md](../../source/05_Risk_Control/05-01_Risk_Framework.md)
> **Audience**: Executives, Compliance Officers, Business Analysts
> **Related Technical Doc**: [Risk_System_Architecture.md](../../architecture/05_Risk_Engine/Risk_System_Architecture.md)
> **Last Synced**: 2026-02-08
> **Source Version**: 4.0.0

---

## Executive Summary

Online gambling platforms lose over **$1.2 billion annually** to fraud, with **Bonus Abuse** representing **63.8%** of all fraud cases. From 2022 to 2024, iGaming fraud grew by **64%**, making risk control systems critical for platform survival.

This document outlines the business strategy, policies, and KPIs for the iGaming risk control framework.

---

## 1. Three-Layer Risk Architecture (Business View)

The iGaming platform employs a three-layer risk control architecture:

| Layer | Module | Responsibility | Key Metrics |
|-------|--------|----------------|-------------|
| **Layer 1** | Risk Control | Base validation (hedge detection, odds threshold, anomaly patterns) | RiskFactor: 0 or 1 |
| **Layer 2** | Finance Center | Status factor application (WIN/LOSS/DRAW) | StatusFactor: 0%, 50%, 100% |
| **Layer 3** | Activity System | Game weight application (Slots 100%, Baccarat 15%, etc.) | GameWeight: 5%-100% |

**Unified Turnover Calculation**:
```
ValidTurnover = BetAmount × RiskFactor × StatusFactor × GameWeight
```

---

## 2. Risk Categories & Business Rules

### 2.1 Fraud Types by Prevalence

| Fraud Type | Prevalence | Annual Loss | Primary Target |
|------------|------------|-------------|----------------|
| **Bonus Abuse** | 69.9% (Q1 2024) | Highest | New player bonuses |
| **Multi-Accounting** | 15-20% | High | Welcome offers |
| **Payment Fraud** | 10-15% | $207 per $100 chargeback | Deposit/withdrawal |
| **Account Takeover** | 5-10% | $4.81M average breach | Player accounts |

### 2.2 Detection Priorities

| Priority | Code | Scenarios | SLA | Auto Action |
|----------|------|-----------|-----|-------------|
| **URGENT** | Blacklist hit, fund aggregation, emulator detected | 1 hour | Account freeze |
| **HIGH** | Bot behavior, rapid withdrawal, high ML fraud probability | 2 hours | Auto block |
| **MEDIUM** | Suspicious IP, abnormal betting pattern | 24 hours | Manual review |
| **LOW** | Normal with minor anomalies | 48 hours | Allow with monitoring |

### 2.3 Priority Mapping Rules (v2.1.0)

| Dimension | Detection Item | LOW | MEDIUM | HIGH | URGENT |
|-----------|----------------|-----|--------|------|--------|
| **Device** | Accounts per device | 1-2 | 3-4 | 5+ | Blacklist |
| **Device** | Emulator/VM | No | - | - | Yes |
| **Device** | VPN/Proxy | No | - | Yes | - |
| **Payment** | Accounts per payment method | 1 | - | 2 | 3+ |
| **Payment** | Players per withdrawal account | 1 | - | - | 2+ |
| **Payment** | Card BIN vs IP location | Match | Mismatch | - | - |
| **Behavior** | ML fraud probability | < 0.5 | 0.5-0.8 | > 0.8 | - |
| **Behavior** | Bonus abuse pattern | Normal | - | Min bet + high rollover | Hedge betting |
| **Behavior** | Withdrawal rate (per hour) | < 3 | 3-5 | > 5 | - |
| **Graph** | Connected accounts (BFS-3) | < 3 | - | 3-4 | 5+ |
| **Graph** | Synchronized behavior | No | - | Yes | - |
| **Graph** | Fund flow aggregation | No | - | - | Yes |

**Key Design Decisions (v2.1.0)**:
- No score-based system (avoids 59 vs 60 boundary ambiguity)
- Each rule directly maps to a priority level
- Multiple rule triggers: highest priority wins

---

## 3. KYC/AML Compliance Requirements

### 3.1 Jurisdiction Requirements

| Jurisdiction | KYC Trigger | CDD Threshold | Key Requirements |
|--------------|-------------|---------------|------------------|
| **UK (UKGC)** | Registration | £2,000 | Mandatory SAR, no credit card gambling |
| **Malta (MGA)** | Registration | €2,000 | EU AML Directive compliant, 10-year license |
| **Gibraltar** | Registration | £1,000 | Dedicated AML Code of Practice |
| **Curacao** | Registration | Varies | Allows crypto, lower international recognition |

### 3.2 KYC Process Flow

1. **Customer Identification**: Collect basic information (name, DOB, address)
2. **Identity Verification**: Government-issued ID + proof of address
3. **Ongoing Monitoring**: Transaction patterns, behavior changes

### 3.3 Enhanced Due Diligence (EDD)

Required for:
- Politically Exposed Persons (PEPs)
- High-risk jurisdiction customers
- Large transaction customers

EDD includes:
- Source of Wealth (SOW) verification
- Source of Funds (SOF) verification
- Enhanced transaction monitoring

---

## 4. Regulatory Penalty Case Studies

### 4.1 2023 European Penalty Summary

**Total fines**: £348 million / $443 million

| Operator | Fine | Key Failures |
|----------|------|--------------|
| **William Hill** | £19.2M | AML oversight failures, allowed £23,000 loss in 20 minutes |
| **Entain** | £17M | Customer deposited £230,000 over 18 months without affordability check |
| **Betway** | £11.6M | Customer moved £8M, lost £4M with no SOF check for 4 years |

### 4.2 Key Lessons

1. Robust customer due diligence procedures are essential
2. Real-time transaction monitoring is non-negotiable
3. Multi-brand accounts require unified management
4. VIP customers need appropriate SOF checks and problem gambling intervention
5. Employee security awareness training cannot be neglected

---

## 5. Risk Control KPIs

### 5.1 Core Fraud Metrics

| Metric | Formula | Industry Benchmark | Target |
|--------|---------|-------------------|--------|
| **Fraud Rate** | Confirmed fraud / Total transactions | < 1% | < 0.5% |
| **Value Detection Rate (VDR)** | Blocked fraud amount / Total fraud amount | > 80% | > 90% |
| **Recall Rate** | Rejected fraud / Total fraud | > 85% | > 94% |

**Note**: JPMorgan AI research found that selecting correct fraud KPIs improves protection by at least **20%**.

### 5.2 Operational Efficiency Metrics

| Metric | Best Practice | Our Target |
|--------|---------------|-----------|
| **False Positive Rate** | < 5% | < 3% |
| **Approval Rate** | > 95% | > 97% |
| **Manual Review Rate** | < 10% | < 5% |
| **Mean Time to Detect (MTTD)** | < 1 hour | < 30 minutes |

**Important**: False positive costs can be **75x** actual fraud costs - controlling false positives is critical for revenue.

### 5.3 Gaming-Specific KPIs

| Category | KPIs |
|----------|------|
| **Revenue** | GGR (Gross Gaming Revenue), NGR (Net Gaming Revenue) |
| **Player Health** | Retention rate, Promotion abuse rate |
| **Compliance** | SOF review completion rate, STR submission timeliness (100%) |

---

## 6. Name List Management

### 6.1 Blacklist Sources

- Internal confirmed fraud cases
- Regulatory authority lists
- Industry shared databases
- Third-party anti-fraud providers

**UK Cifas (National Fraud Database)**:
- 1,100+ member enterprises (including Bet Victor)
- 24/7 real-time online access
- Hundreds of thousands of new fraud records annually

### 6.2 Greylist Management

For suspicious but unconfirmed users:
- Enhanced monitoring periods: 30/60/90 days
- Track betting pattern changes
- Cumulative risk assessment

### 6.3 VIP Management Considerations

**888 Case Warning**: Penalized for allowing an NHS employee (known £1,400 monthly salary) to set £1,300 monthly deposit limit.

**VIP Best Practices**:
- Regular Source of Funds (SOF) reviews
- Dedicated account managers
- Higher transaction limits with maintained monitoring
- Problem gambling intervention mechanisms

---

## 7. Vendor Solution Comparison

| Vendor | Core Capability | Best For | Key Metric |
|--------|-----------------|----------|------------|
| **GeoComply** | Geolocation verification | US sports betting | 1.2B monthly verifications, 350+ checks per transaction |
| **Iovation/TransUnion** | Device fingerprinting | Cross-industry intelligence | 25M daily transactions, 300K fraud blocks |
| **Sift** | ML fraud detection | Global coverage | 1T+ events/year, 90% US iGaming revenue protected |
| **Kambi** | Sports betting risk | European operators | €17B+ global liquidity, 30% GGR from AI pricing |
| **Sportradar UFDS** | Match-fixing detection | Sports integrity | 600+ operators, 300B odds changes analyzed |

### 7.1 Build vs Buy Decision Matrix

| Factor | Build Advantage | Buy Advantage |
|--------|-----------------|---------------|
| **Cost** | Lower long-term (high initial) | Quick time-to-market |
| **Customization** | Fully customizable | Depends on vendor support |
| **Data Security** | Full control | May share with third party |
| **Technical Barrier** | Requires ML/DevOps team | Low barrier |
| **Update Speed** | Depends on internal resources | Vendor continuous updates |

**Recommendations**:
- **Startups**: Purchase mature solutions (Sift + Iovation) for quick launch
- **Mature Platforms**: Build core rule engine, integrate third-party ML and device fingerprinting

---

## 8. Configuration-Driven Risk Control (v2.1.0)

### 8.1 Business Rationale

SmartAdmin v2.1.0 introduces configuration-driven risk control, allowing operators to configure each rule's processing method (real-time block vs delayed review) without code changes.

**Core Principles**:
- **Configuration-Driven**: Each rule's action type configured in database (BLOCK/FLAG/IGNORE)
- **Human-Centric**: Anomalies generate risk proposals for human review
- **Equal Treatment**: All players go through risk control (no VIP exemption)
- **Operator Choice**: Operators decide risk strategy without code changes

### 8.2 Regional Strategy Configuration

| Region | Regulation Level | Recommended Strategy |
|--------|------------------|---------------------|
| **UK/Malta** | Strict | More BLOCK rules, fewer FLAG rules |
| **Philippines/Brazil** | Relaxed | More FLAG rules, better UX |

### 8.3 Business Value

| Advantage | Description | Business Value |
|-----------|-------------|----------------|
| **Flexibility** | No code changes needed | Rapid market response |
| **Operator Autonomy** | Self-determined risk strategy | Higher customer satisfaction |
| **A/B Testing** | Test different strategies per market | Data-driven decisions |
| **Audit Trail** | All config changes logged | Compliance requirement |
| **Risk Reduction** | Avoid hard-coded errors | System stability |

---

## 9. Certification & Standards

### 9.1 GLI-19 Standard

Covers interactive gaming system requirements:
- Player software security
- Encryption protocols
- Location detection
- Tamper-proof requirements

### 9.2 eCOGRA Certification

- Covers 45+ jurisdictions
- **Safe and Fair Seal**: Operator level
- **Certified Software Seal**: Software developer level

---

## 10. Success Factors

1. **Cross-Module Collaboration**: Risk control must deeply integrate with Finance, Activity, CS systems
2. **Data-Driven Decisions**: Regularly review fraud detection rate, false positive rate KPIs
3. **Compliance First**: All decisions must comply with GDPR, AML regulations
4. **Performance & Security Balance**: Maintain low latency without sacrificing security

---

## Related Documents

### Core Dependencies
- [02-03 Reconciliation Requirements](../02_Financial_Operations/Reconciliation_Requirements.md) - Balance verification
- [05_Risk_Compliance/KYC_AML_Requirements](KYC_AML_Requirements.md) - KYC procedures

### Compliance
- [UKGC_Requirements](UKGC_Requirements.md) - UK regulatory requirements
- [MGA_Requirements](MGA_Requirements.md) - Malta regulatory requirements

### Technical Implementation

→ **[Risk System Architecture](../../architecture/05_Risk_Engine/Risk_System_Architecture.md)** - Five-layer risk control architecture, real-time detection engine, rule configuration system, ML model integration, and cross-module orchestration

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Risk & Compliance Team
