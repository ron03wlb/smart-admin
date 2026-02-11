# ML Risk Detection Requirements (機器學習風控業務需求)

> **Canonical Source**: [05-02-03_ML_Integration.md](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md)
> **Audience**: Executives, Product Managers
> **Related Architecture**: [ML_Integration_Architecture.md](../../architecture/05_Risk_Engine/ML_Integration_Architecture.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

Machine learning models augment the rule-based risk control system by detecting complex fraud patterns that static rules cannot capture. This document defines the business requirements, expected outcomes, and governance policies for ML-based risk detection.

---

## Acceptance Criteria

- [ ] All five ML models (Abnormal Betting, Multi-Account, Bonus Abuse, AML Risk Scoring, Fraudulent Transaction) deployed and operational with accuracy meeting or exceeding targets specified in Section 3.1
- [ ] Real-time risk scoring API responds within latency targets: ≤50ms for betting detection, ≤100ms for multi-account/transaction detection, ≤200ms for bonus abuse, ≤500ms for AML scoring
- [ ] Player risk profiling calculates total scores correctly using factor weights and caps (Section 5.2-5.3), assigns risk levels (LOW/MEDIUM/HIGH/BLACKLIST) accurately
- [ ] VIP players undergo identical risk controls as non-VIP players (no exemptions or reduced thresholds) as mandated in Section 5.4
- [ ] Model governance dashboard displays performance metrics (F1 score, false positive rate, latency, PSI) with automated alerts when thresholds breached (Section 7.1)
- [ ] All new models and updates pass 14-day A/B testing with p-value <0.05 before full deployment, with automated rollback if treatment underperforms control (Section 7.2)
- [ ] Game-type dimensional rules correctly filter risk rules to applicable game types (SPORTS, LIVE, SLOTS, etc.) per Section 4
- [ ] Blacklist enforcement immediately blocks all betting activity and restricts account access to read-only when player added to blacklist (Section 6.2)
- [ ] Fraud detection rate improves from 70% (rule-based) to ≥95% (with ML), false positive rate reduces from 15% to <5%, as specified in Section 8

---

## 2. Business Objectives

| Objective | Description | Priority |
|-----------|-------------|----------|
| Enhanced Fraud Detection | Identify sophisticated fraud patterns beyond rule-based detection | P0 |
| Real-Time Risk Scoring | Provide sub-second risk assessments for every betting transaction | P0 |
| Adaptive Learning | Models must adapt to evolving fraud tactics without manual rule updates | P1 |
| Regulatory Compliance | All ML decisions must be explainable and auditable for regulatory review | P0 |
| Operational Efficiency | Reduce false positive rates to minimise manual review workload | P1 |

---

## 3. ML Model Use Cases

### 3.1 Model Portfolio

| Model | Business Purpose | Accuracy Target | Latency Target |
|-------|-----------------|-----------------|----------------|
| Abnormal Betting Detection | Identify suspicious betting patterns (e.g., timing anomalies, amount clustering, correlated accounts) | 95% or higher | Under 50ms |
| Multi-Account Detection | Detect device fingerprint and behavioural similarities across accounts to identify linked accounts | 90% or higher | Under 100ms |
| Bonus Abuse Detection | Identify players systematically exploiting promotional offers through coordinated or repeated abuse patterns | 85% or higher | Under 200ms |
| AML Risk Scoring | Assign anti-money-laundering risk grades based on transaction patterns, account behaviour, and external data | 80% or higher | Under 500ms |
| Fraudulent Transaction Detection | Identify payment fraud in deposits and withdrawals using transaction pattern analysis | 92% or higher | Under 100ms |

### 3.2 Use Case Descriptions

**Abnormal Betting Detection**
- Detects anomalous wagering patterns such as sudden stake increases, rapid bet placement, and hedging across correlated markets
- Triggers further investigation or automatic flagging based on confidence score

**Multi-Account Detection**
- Uses device fingerprinting, behavioural biometrics, and network analysis to cluster accounts
- Identifies shared devices, similar betting patterns, and suspicious registration correlations

**Bonus Abuse Detection**
- Monitors promotional offer redemption patterns across accounts
- Detects systematic exploitation such as minimum-play bonus extraction and coordinated signup bonuses

**AML Risk Scoring**
- Continuous risk scoring based on transaction velocity, structuring patterns, and source-of-funds indicators
- Risk grades feed into the compliance team's enhanced due diligence workflow

**Fraudulent Transaction Detection**
- Real-time scoring of payment transactions for chargeback risk and stolen credentials
- Integrates with payment gateway decision engines

---

## 4. Game-Type Dimensional Rules

### 4.1 Game-Type Applicability

Risk detection rules (both ML and rule-based) must support game-type filtering so that rules designed for specific gambling verticals are only applied to relevant transactions.

| Configuration | Business Meaning |
|---------------|-----------------|
| Rule with no game type filter | Applies universally to all game types |
| Rule with specific game types | Applies only to the listed game types |
| Rule with excluded games | Applies to all games except those specifically excluded |

### 4.2 Supported Game Types

| Game Type | Description |
|-----------|-------------|
| SPORTS | Sports betting (pre-match and live) |
| LIVE | Live casino (baccarat, roulette, blackjack) |
| SLOTS | Slot machines and video slots |
| LOTTERY | Lottery and number games |
| POKER | Poker (cash games and tournaments) |
| FISHING | Fishing arcade games |
| ESPORTS | Esports betting |

### 4.3 Game-Specific Rule Examples

| Rule | Applicable Game Types | Action |
|------|----------------------|--------|
| Same-Match Hedging | SPORTS only | Block |
| Cross-Match Hedging | SPORTS only | Flag |
| Low-Odds Wagering | SPORTS, LIVE | Flag |
| Blacklist Player | All (universal) | Block |
| Bot Detection | All (universal) | Block |

### 4.4 Game Exclusion Policy

Individual games or game providers may be excluded from specific rules. For example, a low-odds wagering rule may exclude specific Evolution Gaming titles where the betting pattern is normal for the game type.

---

## 5. Player Risk Profiling

### 5.1 Risk Levels

| Risk Level | Score Range | Operational Response |
|------------|-----------|---------------------|
| LOW | 0 - 49 | Standard monitoring; no additional action |
| MEDIUM | 50 - 79 | Enhanced monitoring; periodic manual review |
| HIGH | 80 - 100 | Mandatory manual review; restricted operations |
| BLACKLIST | N/A | All betting activity blocked; account frozen |

### 5.2 Risk Scoring Factors

| Factor | Weight per Occurrence | Maximum Contribution | Description |
|--------|-----------------------|---------------------|-------------|
| Risk Flag Count | +5 per flag | 30 points | Number of times the player has been flagged by any risk rule |
| Block Count | +10 per block | 40 points | Number of times the player's activity has been actively blocked |
| Abnormal Win Rate | +20 (one-time) | 20 points | Win rate above 60% or below 30% (statistical anomaly) |
| Recent Proposals | +3 per proposal | 20 points | Number of risk proposals generated in the past 30 days |

### 5.3 Total Score Calculation

- Total Score = sum of all factor contributions, capped at 0 (minimum) and 100 (maximum)
- Risk level is derived directly from the score using the thresholds in Section 5.1

### 5.4 VIP Player Compliance Policy

**Critical Regulatory Requirement**: VIP players must undergo the exact same risk controls as all other players. No exemptions, reduced thresholds, or bypasses are permitted for VIP status.

VIP level affects only:
- Priority ordering in the manual review queue (VIP proposals are reviewed first)
- Dedicated customer service assistance for preparing KYC/SOF documentation

VIP level does NOT affect:
- Risk rule trigger conditions
- AML thresholds
- Affordability assessment triggers

Regulatory precedent: Entain was fined 17 million GBP for failing to apply adequate risk controls to VIP players.

---

## 6. Blacklist Management

### 6.1 Blacklist Entry Criteria

| Criterion | Description |
|-----------|-------------|
| Confirmed Fraud | Player confirmed as fraudulent through investigation |
| AML Violation | Player involved in money laundering activity |
| Multiple Account Abuse | Player operating multiple accounts to circumvent controls |
| Regulatory Order | Blacklisting ordered by a regulator or law enforcement |
| Operator Decision | Operator-initiated blacklisting based on risk assessment |

### 6.2 Blacklist Consequences

- All betting activity is immediately and permanently blocked
- Pending withdrawals are reviewed and may be suspended
- Account access is restricted to read-only (balance viewing only)
- The blacklist reason and operator are recorded for audit purposes

---

## 7. Model Governance

### 7.1 Performance Monitoring

| Metric | Threshold | Action When Breached |
|--------|-----------|---------------------|
| Accuracy (F1 Score) | Must not drop more than 5% from baseline | Trigger model retraining |
| False Positive Rate | Must remain below the agreed ceiling per model | Investigate and tune |
| Latency | Must remain within the target specified per model | Optimise or scale infrastructure |
| Data Drift (PSI) | PSI must remain below 0.2 | Re-evaluate model with new data |

### 7.2 A/B Testing Policy

All new models and significant model updates must pass A/B testing before full deployment.

| Requirement | Details |
|-------------|---------|
| Minimum Test Duration | 14 days |
| Statistical Significance | p-value below 0.05 (95% confidence) |
| Traffic Split | Configurable; default 10% treatment, 90% control |
| Rollback Criteria | If treatment performs worse than control at any point with statistical significance, rollback immediately |
| Assignment Method | Consistent hashing by player ID to ensure the same player always sees the same model version |

### 7.3 Model Drift Monitoring

| Drift Type | Detection Method | Response |
|------------|-----------------|----------|
| Data Drift | Feature distribution changes (KS Test, PSI) | Investigate feature pipeline changes |
| Concept Drift | Prediction distribution shift, accuracy decline | Schedule model retraining |
| Missing Values | Missing value ratio increase | Investigate data source issues |
| New Categories | Appearance of previously unseen categorical values | Update feature encoding |

---

## 8. Expected Outcomes

| Metric | Current (Rule-Based Only) | Target (With ML) |
|--------|--------------------------|-------------------|
| Fraud Detection Rate | 70% | 95% or higher |
| False Positive Rate | 15% | Under 5% |
| Mean Detection Latency | 200ms | Under 100ms |
| Manual Review Volume | 100% of flagged cases | 30% reduction |
| Bonus Abuse Losses | Baseline | 40% reduction |

---

## Acceptance Criteria

- [ ] Abnormal Betting Detection model achieves ≥95% accuracy with latency under 50ms
- [ ] Multi-Account Detection model identifies linked accounts with ≥90% accuracy under 100ms
- [ ] Bonus Abuse Detection model flags systematic exploitation patterns with ≥85% accuracy
- [ ] AML Risk Scoring assigns risk grades within 500ms with ≥80% accuracy against known money laundering patterns
- [ ] Fraudulent Transaction Detection achieves ≥92% accuracy for payment fraud with latency under 100ms
- [ ] All ML models pass A/B testing requirements (14 days, p-value < 0.05, 95% confidence)
- [ ] Model drift monitoring detects PSI > 0.2 and triggers retraining alerts automatically
- [ ] VIP players receive identical risk controls as non-VIP players (no exemptions permitted)
- [ ] Game-type dimensional filtering correctly applies rules only to specified game categories

---

## 9. Cross-References

| Topic | Document |
|-------|----------|
| Detection Model Architecture | source/05_Risk_Control/05-02-01_Detection_Model.md |
| Rule Configuration | source/05_Risk_Control/05-02-02_Rule_Configuration.md |
| Operations Tools | source/05_Risk_Control/05-02-04_Operations_Tools.md |
| Technical ML Architecture | architecture/05_Risk_Engine/ML_Integration_Architecture.md |
| KYC/AML Compliance | source/05_Risk_Control/05-03_KYC_AML.md |

### Technical Implementation

→ **[ML Integration Architecture](../../architecture/05_Risk_Engine/ML_Integration_Architecture.md)** - Machine learning model deployment, feature engineering pipelines, real-time prediction API, model retraining workflows, and fraud detection algorithms
