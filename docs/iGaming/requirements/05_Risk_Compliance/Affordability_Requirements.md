# Affordability Assessment Requirements (可負擔性評估業務需求)

> **Canonical Source**: [15-08_Affordability_Assessment.md](../../source/15_Responsible_Gambling/15-08_Affordability_Assessment.md)
> **Audience**: Executives, Compliance Officers, Product Managers
> **Related Doc**: [Affordability_Implementation.md](../../architecture/05_Risk_Engine/Affordability_Implementation.md)
> **Last Synced**: 2026-02-08

---

## 1. Business Context

Affordability Assessment is a core regulatory requirement under UKGC 2025 rules. The system evaluates a player's financial capacity, ensuring gambling activity remains within affordable limits. Failure to comply exposes the operator to license sanctions, fines, and reputational damage.

---

## 2. Regulatory Mandate Summary

| Regulator | Regulation | Effective Date | Primary Mandate |
|-----------|-----------|----------------|-----------------|
| **UKGC** | 2025 New Rules | 2025-01 | Mandatory financial affordability assessments |
| **Netherlands** | KOA Remote Gambling | 2024 | Annual affordability self-declaration |
| **Germany** | GlueStV 2021 | 2021 | Hard cap of EUR 1,000 per month |

---

## 3. UK 2025 Rules -- Assessment Triggers

### 3.1 Annual Net Loss Triggers

UKGC lowered the mandatory financial assessment thresholds significantly:

| Trigger Condition | Pre-2025 Threshold | 2025 Threshold |
|-------------------|-------------------|----------------|
| Annual net loss | GBP 2,000/year | GBP 500/year |
| Deposit velocity | None | GBP 500 in multiple deposits within 24 hours |
| High-risk flagging | Operator discretion | Mandatory rules |

### 3.2 Assessment Tiers

Each tier carries specific obligations for the operator:

| Tier | Trigger Condition | Required Action |
|------|-------------------|-----------------|
| **Basic** | Net loss GBP 125 -- GBP 500 | Display warning message to player |
| **Enhanced** | Net loss GBP 500 -- GBP 2,000 | Player self-declaration form |
| **Full** | Net loss exceeding GBP 2,000 | Third-party data verification |

### 3.3 Self-Declaration Data Points (Enhanced Tier)

Players must declare the following:

| Data Point | Purpose |
|------------|---------|
| Annual income range | Establish baseline earnings |
| Housing status (own / rent / family) | Assess fixed living costs |
| Household size | Factor dependents into affordability |
| Monthly disposable income | Determine gambling budget headroom |

### 3.4 Third-Party Verification Sources (Full Tier)

| Source | Data Provided |
|--------|--------------|
| Open Banking | Real-time bank account balances and transaction history |
| Credit Reference Agency (Experian, Equifax) | Credit file, income verification, financial commitments |
| Public financial records | Insolvency, county court judgments |

---

## 4. Monthly Net Deposit Trigger (UKGC 2025-02-28)

Effective 28 February 2025, a separate and parallel trigger applies based on monthly net deposits.

### 4.1 Rule Definition

| Attribute | Value |
|-----------|-------|
| Calculation period | Rolling 30 days |
| Formula | Total Deposits minus Total Withdrawals |
| Threshold | GBP 150 or more |
| Effective date | 2025-02-28 |

This rule operates **independently** of the annual net loss rules and both may trigger simultaneously.

### 4.2 Trigger Comparison Matrix

| Rule Type | Calculation Period | Formula | Thresholds | Effective |
|-----------|--------------------|---------|------------|-----------|
| Annual net loss | Rolling 12 months | Loss amount minus winnings | GBP 125 / GBP 500 / GBP 2,000 | 2025-01 |
| Monthly net deposit | Rolling 30 days | Deposits minus withdrawals | GBP 150 | 2025-02-28 |

### 4.3 Relationship Between Triggers

Player activity is evaluated against both trigger types:

- Annual net loss at or above GBP 125 triggers Basic assessment (warning)
- Annual net loss at or above GBP 500 triggers Enhanced assessment (self-declaration)
- Annual net loss at or above GBP 2,000 triggers Full assessment (third-party verification)
- Monthly net deposit at or above GBP 150 triggers Financial Vulnerability Basic Check (may escalate based on combined risk indicators)

---

## 5. Financial Vulnerability Detection

The system must detect behavioural indicators of financial vulnerability in real time.

### 5.1 Vulnerability Indicators

| Indicator | Definition | Severity |
|-----------|-----------|----------|
| **Chasing losses** | Player increases stake by more than 50% immediately after a loss, occurring 3 or more times in 20 bets | HIGH |
| **Deposit velocity** | 5 or more deposits within 24 hours totalling more than GBP 500 | MEDIUM |
| **Unusual pattern** | Deviations from established betting pattern (time-of-day, stake size, game type) | MEDIUM |

### 5.2 Severity-Based Response Policy

| Severity | Required Platform Action |
|----------|------------------------|
| **HIGH** | Trigger full affordability assessment; send care message to player |
| **MEDIUM** | Display in-session warning message about responsible gambling |
| **LOW** | Log indicator for monitoring; no immediate player-facing action |

---

## 6. Recommended Limit Calculation Policy

When a player completes a self-declaration, the platform calculates a recommended monthly deposit limit.

### 6.1 Calculation Rules

| Rule | Description |
|------|------------|
| Base calculation | 10% of declared monthly disposable income |
| Household adjustment | Reduce by 20% if household size exceeds 2 |
| Minimum floor | GBP 50 per month |
| Maximum cap | GBP 2,000 per month |

### 6.2 Limit Application Policy

| Assessment Result | Limit Application | Player Choice |
|-------------------|-------------------|---------------|
| PASSED (Enhanced) | Recommended limit presented | Player may accept or set a lower custom limit |
| PASSED (Full) | Limit applied automatically | Player may request lower limit but not higher |
| FAILED (Full) | Limit forced immediately | No player override; account may be restricted |

---

## 7. Assessment Validity and Renewal

| Assessment Type | Validity Period | Renewal Process |
|----------------|----------------|-----------------|
| Enhanced (self-declaration) | 3 months | Player completes new declaration |
| Full (third-party verification) | 6 months | New third-party check required |

If a player's existing assessment covers the required tier and has not expired, no new assessment is triggered.

---

## 8. Compliance Reporting Requirements

### 8.1 Monthly Reports

| Report | Frequency | Content |
|--------|-----------|---------|
| Assessment Statistics | Monthly | Total triggered, pass rate, limit distribution by tier |
| Vulnerability Detection | Monthly | Detection counts by indicator type, resolution outcomes |
| Limit Enforcement | Monthly | Forced limit count, player response (accepted / appealed) |

### 8.2 Key Performance Indicators

| KPI | Description |
|-----|------------|
| Assessment trigger rate | Number of assessments triggered per active player cohort |
| Assessment pass rate | Ratio of PASSED to total assessments |
| Average recommended limit | Mean monthly limit recommended across all assessments |
| Vulnerability detection rate | Number of vulnerability indicators detected per active player |

---

## 9. Related Business Requirements

| Document | Relationship |
|----------|-------------|
| Deposit Limits | Affordability limits feed into the deposit limit system |
| Loss Limits | Net loss calculations share data with loss limit enforcement |
| KYC / AML | Full assessments may share data with KYC verification |
| UKGC Compliance | Affordability is a subset of overall UKGC licence compliance |

### Technical Implementation

→ **[Affordability Assessment Implementation](../../architecture/05_Risk_Engine/Affordability_Implementation.md)** - Light/enhanced/full assessment algorithms, bank API integration, income verification workflows, limit enforcement mechanisms, and real-time monitoring dashboards

---

**Navigation**: [Risk and Compliance Requirements](../05_Risk_Compliance/) | [iGaming Home](../../README.md)
