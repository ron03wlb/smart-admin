# Affordability & Player Protection API Requirements (可負擔性評估及玩家保護 API 業務需求)

> **Canonical Source**: [15-07_Player_Protection_API.md](../../source-archive/15_Responsible_Gambling/15-07_Player_Protection_API.md), [15-08_Affordability_Assessment.md](../../source-archive/15_Responsible_Gambling/15-08_Affordability_Assessment.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Architecture**: [Player_Protection_API.md](../../architecture/15_Responsible_Gambling/Player_Protection_API.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This requirements document delivers strategic value by:
- **Regulatory Compliance**: Implements UKGC 2025 mandatory affordability assessment requirements with lowered thresholds (GBP 500/year vs. previous GBP 2,000/year)
- **Player Protection**: Establishes three-tier assessment framework (Basic, Enhanced, Full) with escalating interventions based on net loss levels
- **Sustainable Revenue**: Targets <5% GGR ratio from high-risk players, avoiding regulatory penalties and reputational damage from harm-dependent revenue
- **Unified API**: Provides single Player Protection API for all responsible gambling tools (deposit limits, loss limits, self-exclusion, session limits) reducing integration complexity

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Assessment Trigger Compliance | 100% | Players meeting thresholds assessed / Total players at threshold |
| Assessment Pass Rate | Monitor | PASSED assessments / Total assessments |
| High-Risk Player GGR Ratio | < 5% | GGR from high-risk players / Total GGR |
| Monthly Net Deposit Trigger | 100% | Triggers at GBP 150/30-day threshold |
| Assessment Validity Tracking | 100% | Re-assessments triggered on expiry (3/6 months) |
| Vulnerability Detection Rate | Monitor | Behavioral indicators detected / Total active players |

---

## 1. Overview

Affordability Assessment is a core requirement of the UK Gambling Commission's 2025 regulations, designed to evaluate a player's financial capacity and ensure gambling activity does not exceed their affordable range. The Player Protection API provides a unified interface for all responsible gambling tools.

---

## 2. Regulatory Requirements

### 2.1 Affordability Assessment

| Regulator | Clause | Effective Date | Key Requirements |
|-----------|--------|---------------|-----------------|
| **UKGC** | 2025 New Rules | 2025-01 | Mandatory financial assessment |
| **Netherlands** | KOA Remote Gambling | 2024 | Annual affordability declaration |
| **Germany** | GlüStV 2021 | 2021 | EUR 1,000/month cap |

### 2.2 UK 2025 Trigger Thresholds

UKGC has lowered the mandatory financial assessment thresholds:

| Trigger Condition | Old Rule (Pre-2024) | New Rule (2025) |
|-------------------|--------------------|--------------------|
| Net loss threshold | GBP 2,000/year | GBP 500/year |
| Deposit velocity | None | GBP 500/24h with multiple deposits |
| High-risk flag | Operator discretion | Mandatory rules |

---

## 3. Assessment Levels

### 3.1 Three-Tier Assessment Framework

| Level | Trigger Condition | Required Action |
|-------|------------------|-----------------|
| **Basic** | Net loss GBP 125-500 | Display warning message |
| **Enhanced** | Net loss GBP 500-2,000 | Player self-declaration |
| **Full** | Net loss > GBP 2,000 | Third-party data verification |

### 3.2 Assessment Content

#### Player Self-Declaration (Enhanced Level)
- Annual income range
- Housing status (own/rent)
- Household size
- Monthly disposable income

#### Third-Party Data Verification (Full Level)
- Open Banking data
- Credit reference agency data
- Public financial records

---

## 4. Monthly Net Deposit Trigger Rule (UKGC 2025-02-28)

### 4.1 Trigger Conditions

This rule operates **in parallel** with the annual net loss rules:

| Rule Type | Calculation Period | Formula | Threshold | Effective Date |
|-----------|-------------------|---------|-----------|---------------|
| Annual net loss | 12-month rolling | Loss - Wins | GBP 125/500/2,000 | 2025-01 |
| **Monthly net deposit** | 30-day rolling | Deposits - Withdrawals | **>= GBP 150** | **2025-02-28** |

### 4.2 Monthly Net Deposit Calculation

```
Monthly Net Deposit = Sum of Deposits (30 days) - Sum of Withdrawals (30 days)
```

### 4.3 Combined Assessment Flow

```
Player Activity
    |
    +-- Annual net loss >= GBP 125 --> Basic assessment (display warning)
    +-- Annual net loss >= GBP 500 --> Enhanced assessment (self-declaration)
    +-- Annual net loss >= GBP 2,000 --> Full assessment (third-party verification)
    |
    +-- Monthly net deposit >= GBP 150 --> Financial vulnerability basic check
                                            |
                                            +-- Combined with risk indicators to determine upgrade
```

---

## 5. Assessment Result Handling

### 5.1 Recommended Limit Calculation

- Recommended gambling expenditure should not exceed 10% of disposable income
- Adjustment for household size > 2: reduce by 20%
- Minimum recommended limit: GBP 50
- Maximum recommended limit: GBP 2,000

### 5.2 Assessment Results

| Result | Description | Action |
|--------|------------|--------|
| **PASSED** | Player's gambling is within affordable range | Recommend limit, player can accept/decline |
| **FAILED** | Player's gambling exceeds affordable range | Force-apply deposit limit |
| **PENDING** | Awaiting player declaration or third-party verification | Block limit increases until completed |
| **EXPIRED** | Assessment validity expired | Trigger re-assessment |

### 5.3 Assessment Validity

| Assessment Type | Validity Period |
|----------------|-----------------|
| Enhanced (self-declaration) | 3 months |
| Full (third-party verified) | 6 months |

---

## 6. Financial Vulnerability Indicators

### 6.1 Behavioral Detection

| Behavior Pattern | Risk Level | Detection Rule |
|-----------------|-----------|---------------|
| **Chasing losses** | High | After losing, bet amount increases > 50% in next 3+ instances within 20 bets |
| **Deposit velocity** | Medium | 5+ deposits within 24 hours totaling > GBP 500 |
| **Unusual pattern** | Medium | Significant deviation from normal betting pattern |

### 6.2 Indicator Handling

| Severity | Action |
|----------|--------|
| **High** | Trigger full assessment + send care message |
| **Medium** | Display warning message |
| **Low** | Record only, no action |

---

## 7. Player Protection API Requirements

### 7.1 Player-Facing Endpoints

| Endpoint | Description |
|----------|-------------|
| Get protection settings overview | All current protection settings in one view |
| Set/update deposit limits | Daily/weekly/monthly limits |
| Set/update loss limits | Daily/weekly/monthly loss limits |
| Request self-exclusion | With duration and reason |
| Request exclusion revocation | After exclusion period ends |
| Start cooling-off period | With duration |
| Set session limits | Duration and idle timeout |
| Get limit usage | Current usage vs. limits |
| Get activity history | Paginated protection activity log |

### 7.2 Admin-Facing Endpoints

| Endpoint | Description |
|----------|-------------|
| List excluded players | Filterable by status, type |
| View player exclusion details | Full exclusion history |
| Operator-exclude player | With reason and notification |
| Generate compliance reports | Monthly, quarterly, annual |
| Export reports | Multiple formats |
| Gamstop sync | Manual sync trigger |
| Gamstop check | Manual player check |

### 7.3 Error Codes

| Code | Description |
|------|-------------|
| RG_001 | Player is excluded |
| RG_002 | Already in cooling-off period |
| RG_003 | Deposit limit exceeded |
| RG_004 | Loss limit exceeded |
| RG_005 | Cannot revoke permanent exclusion |
| RG_006 | Exclusion period not ended |
| RG_007 | No pending limit changes |
| RG_008 | Mandatory break in progress |
| RG_009 | Gamstop sync failed |
| RG_010 | Invalid limit settings (hierarchy error) |

---

## 8. Compliance Reporting

### 8.1 Report Types

| Report | Frequency | Content |
|--------|-----------|---------|
| Assessment statistics | Monthly | Trigger count, pass rate, limit distribution |
| Vulnerability detection | Monthly | Detection count, handling results |
| Limit application | Monthly | Forced limit count, player response |

### 8.2 Monthly Report Summary Fields

| Field | Description |
|-------|-------------|
| Total self-exclusions | New, active, completed, revoked |
| Deposit limit adoption | Players with limits, breaches, average limit |
| Loss limit adoption | Players with limits, breaches |
| Reality check data | Total checks, continue rate, average response time |
| Gamstop sync status | Checks performed, matches, sync failures |

---

## 9. Effectiveness Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| Assessment trigger rate | Monitor | Assessments triggered per jurisdiction |
| Assessment pass rate | Monitor | PASSED / TOTAL assessments |
| Average recommended limit | Monitor | Average suggested monthly limit |
| Vulnerability detection count | Monitor | Behavioral indicators detected |
| High-risk player GGR ratio | < 5% | Avoid dependence on high-risk players |

---

## 10. Testing Scenarios

| Scenario | Expected Result |
|----------|----------------|
| Player net loss reaches GBP 125 | Basic assessment warning displayed |
| Player net loss reaches GBP 500 | Enhanced assessment form required |
| Player net loss reaches GBP 2,000 | Full assessment with third-party verification |
| Monthly net deposit >= GBP 150 | Financial vulnerability check triggered |
| Player submits self-declaration | Recommended limit calculated and shown |
| Assessment expires (3/6 months) | Re-assessment triggered |
| Chasing losses detected | High severity indicator + full assessment |

---

## Related Documents

- [Deposit_Limits_Requirements.md](Deposit_Limits_Requirements.md) - Deposit limits
- [Self_Exclusion_Requirements.md](Self_Exclusion_Requirements.md) - Self-exclusion
- [Session_Protection_Requirements.md](Session_Protection_Requirements.md) - Session protection
- [Player_Protection_API.md](../../architecture/15_Responsible_Gambling/Player_Protection_API.md) - Technical architecture

---

**Return**: [Responsible Gambling Module](../../source-archive/15_Responsible_Gambling/README.md) | [iGaming Home](../../source-archive/README.md)
