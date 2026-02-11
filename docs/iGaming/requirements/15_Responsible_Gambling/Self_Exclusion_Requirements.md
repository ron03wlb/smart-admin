# Self-Exclusion Requirements (自我排除業務需求)

> **Canonical Source**: [15-01_Self_Exclusion.md](../../source-archive/15_Responsible_Gambling/15-01_Self_Exclusion.md), [15-09_Self_Exclusion_Reconciliation.md](../../source-archive/15_Responsible_Gambling/15-09_Self_Exclusion_Reconciliation.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [Self_Exclusion_Architecture.md](../../architecture/15_Responsible_Gambling/Self_Exclusion_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This requirements document delivers strategic value by:
- **Regulatory Compliance**: Implements mandatory Gamstop (UK), CRUKS (NL), Spelpaus (SE), ROFUS (DK) integrations with jurisdiction-specific duration requirements (6 months - lifetime)
- **Player Harm Prevention**: Establishes the most stringent protection tool with irrevocable permanent exclusion option and cross-platform enforcement
- **Operational Integrity**: Defines three-layer reconciliation architecture (real-time, daily batch, weekly audit) ensuring <24h discrepancy resolution
- **Anti-Circumvention**: Specifies device fingerprinting, payment matching, and national database sync to prevent exclusion bypass via new accounts

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Gamstop Sync Success Rate | > 99.9% | Successful syncs / Total sync attempts |
| Type A Discrepancy Resolution Time | < 24 hours | Time from detection to resolution |
| Daily Reconciliation Completion | 100% | Days with completed batch / Total days |
| Post-Exclusion Re-registration Attempt Rate | < 5% | Blocked re-registration attempts / Total exclusions |
| UKGC Breach Report Deadline Compliance | 100% | Reports within 24h / Total breaches |
| Exclusion Activation Accuracy | 100% | Correctly executed exclusions / Total requests |

---

## 1. Overview

Self-Exclusion is the most stringent player protection tool, allowing players to voluntarily prohibit themselves from accessing the gambling platform. This document defines the business requirements for self-exclusion, revocation, and cross-platform synchronization with national exclusion databases.

---

## 2. Regulatory Requirements

### 2.1 Jurisdiction-Specific Rules

| Regulator | Clause | Min Duration | Max Duration | Special Requirements |
|-----------|--------|-------------|-------------|---------------------|
| **UKGC** | LCCP SR 3.5.1 | 6 months | 5 years+ | Must integrate with Gamstop |
| **MGA** | Player Protection Directive | 6 months | Lifetime | National exclusion registry |
| **PAGCOR** | Responsible Gaming Guidelines | 6 months | 1 year | Operator-managed |
| **Brazil SPA** | Portaria SPA | 3 months | 5 years | National registry launching 2026 |
| **Netherlands** | KOA Remote Gambling Act | 6 months | Lifetime | Must integrate CRUKS |
| **Sweden** | Gambling Act | 1 month | Lifetime | Must integrate Spelpaus |
| **Denmark** | Gambling Authority | 6 months | Lifetime | Must integrate ROFUS |

### 2.2 Compliance Reporting

| Report Type | Frequency | Regulator | Content |
|-------------|-----------|-----------|---------|
| Self-exclusion statistics | Monthly | UKGC, MGA | New/active/completed/revoked counts |
| Exclusion database sync status | Monthly | UKGC | Sync success rate, discrepancies |
| Incident reports | As needed | UKGC (24h), MGA (48h) | Breach details, remediation actions |

---

## 3. Exclusion Types

### 3.1 Temporary Exclusion (Player-Initiated)

| Duration Option | Use Case | Release Conditions |
|----------------|----------|-------------------|
| 24 hours | Short-term cooling off | Auto-release |
| 7 days | Short-term control | Auto-release |
| 30 days | Monthly break | Auto-release |
| 6 months | Medium-term exclusion | Application + 24h cooling-off |
| 1 year | Long-term exclusion | Application + 7-day cooling-off |
| 5 years | Long-term exclusion | Application + 7-day cooling-off |

### 3.2 Permanent Exclusion

- **Irrevocable**: Lifetime ban from the platform
- **Non-transferable**: Cannot be transferred to other accounts
- **Cross-platform**: Synced to all licensed operators via Gamstop (UK)

### 3.3 Operator-Initiated Exclusion

Operators may proactively exclude players when the risk control system identifies high-risk behaviors:
- Suspected problem gambling behavior
- Abnormal deposit patterns
- Customer service request for assistance

---

## 4. Business Process Flows

### 4.1 Player Self-Exclusion Flow

1. Player submits exclusion request with chosen duration
2. System displays confirmation dialog showing consequences:
   - Account access will be blocked
   - All active sessions and games will be terminated
   - Open bets will be settled
   - Incomplete bonuses may be forfeited
   - Marketing communications will be stopped
3. For durations >= 6 months: 24-hour cooling-off confirmation window
4. For durations <= 30 days: Immediate activation
5. Upon activation, execute exclusion actions:
   - Close all active sessions and games
   - Settle all open bets/bonuses
   - Sync to national exclusion database (UK: Gamstop)
   - Send confirmation notification to player
6. Record audit log of all actions taken

### 4.2 Exclusion Release Flow

1. Exclusion period expires
2. Cooling-off period begins:
   - 6-month exclusion: 1-day cooling-off
   - 1-5 year exclusion: 7-day cooling-off
3. Player confirms release request
4. Account restored after cooling-off period
5. **Permanent exclusion: No release possible**

### 4.3 During Exclusion Period

**Allowed operations**:
- View account balance
- Request withdrawal of remaining funds
- Contact customer support

**Prohibited operations**:
- Login to gaming platform
- Deposit funds
- Place bets or play games
- Receive marketing communications

### 4.4 Handling of Open Bets During Exclusion

| Bet Type | Handling Rule |
|----------|-------------|
| Games in progress | Wait for round to complete, settle normally |
| Pre-scheduled bets | Cancel and refund |
| Sports bets | Per license rules: may wait for result or settle early |
| Bonuses (wagering met) | Allow withdrawal |
| Bonuses (wagering not met) | Forfeit |

---

## 5. Cross-Platform Synchronization Requirements

### 5.1 National Exclusion Databases

| System | Region | Sync Mode | Sync Frequency |
|--------|--------|-----------|----------------|
| **Gamstop** | UK | Real-time + Daily Batch | Real-time + Daily 03:00 UTC |
| **MGA Self-Ban** | Malta | Daily Batch | Daily via SFTP |
| **CRUKS** | Netherlands | Real-time | Real-time query |
| **Spelpaus** | Sweden | Real-time | Real-time query |
| **ROFUS** | Denmark | Real-time | Real-time query |

### 5.2 Gamstop Integration Requirements (UK)

1. **Registration Check**: Must check every new player registration against Gamstop
2. **Login Check**: Periodic re-verification of active players
3. **Exclusion Registration**: Must register player exclusions with Gamstop
4. **Sync Reconciliation**: Daily batch comparison to detect discrepancies

### 5.3 Reconciliation Rules

#### Three-Layer Reconciliation Architecture

| Layer | Timing | Mechanism | Purpose |
|-------|--------|-----------|---------|
| **Layer 1** | Player operation time | Real-time query | Immediate blocking |
| **Layer 2** | Daily 03:00 UTC | Full list comparison | Gap detection |
| **Layer 3** | Weekly (Monday) | Statistical report + audit | Compliance evidence |

#### Discrepancy Types and Handling

| Type | Description | Risk Level | Handling |
|------|-------------|-----------|---------|
| **Type A** | Excluded in Gamstop, active locally | Critical | Immediate freeze + P0 alert |
| **Type B** | Excluded locally (Gamstop type), not in Gamstop | Warning | Verify exclusion source |
| **Type C** | End date mismatch | Info | Update to match Gamstop |
| **Type D** | Connection failure | Warning | Retry 3 times + alert |

#### Breach Reporting Requirements

| Regulator | Report Deadline | Report Method | Required Content |
|-----------|----------------|---------------|-----------------|
| **UKGC** | 24 hours | Key Event Report | Player details, breach timeline, remediation |
| **MGA** | 48 hours | Incident Report | Impact scope, root cause analysis |

---

## 6. Anti-Circumvention Measures

Players must not be able to bypass exclusion by creating new accounts. Required measures:
- Device fingerprint identification
- Payment information matching
- Cross-platform sync via Gamstop (UK)
- Facial recognition verification (advanced)

---

## 7. Notification Requirements

| Event | Channel | Content |
|-------|---------|---------|
| Exclusion activated | Email + Push | Exclusion confirmation, duration, consequences |
| Exclusion period ending | Email | Reminder of upcoming expiry, cooling-off info |
| Revocation pending | Email | Confirmation of release request, effective date |
| Revocation effective | Email + Push | Account restored notification |
| Gamstop sync failure | Internal alert | P0 alert to compliance team |

---

## 8. Effectiveness Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| Self-exclusion rate | Monitor trend | Monthly new exclusions / active players |
| Post-exclusion re-registration attempt rate | < 5% | Exclusion mechanism effectiveness |
| Gamstop sync success rate | > 99.9% | Sync reliability |
| Discrepancy resolution time | < 24 hours | Time to resolve Type A discrepancies |
| Daily reconciliation completion | 100% | All daily batches completed |

---

## 9. Testing Scenarios

| Scenario | Expected Result |
|----------|----------------|
| Player requests 24-hour exclusion | Immediate activation, auto-release after 24 hours |
| Player requests 6-month exclusion | 24-hour confirmation window displayed |
| Login attempt during exclusion | Exclusion message shown, login blocked |
| Permanent exclusion release request | Rejected, permanent exclusion message shown |
| Gamstop sync failure | Failure recorded, retry scheduled |
| Gamstop shows excluded, local shows active | Immediate freeze, P0 alert, UKGC report within 24h |

---

## Related Documents

- [Deposit_Limits_Requirements.md](Deposit_Limits_Requirements.md) - Deposit limits
- [Session_Protection_Requirements.md](Session_Protection_Requirements.md) - Cooling-off period
- [Self_Exclusion_Architecture.md](../../architecture/15_Responsible_Gambling/Self_Exclusion_Architecture.md) - Technical architecture

---

**Return**: [Responsible Gambling Module](../../source-archive/15_Responsible_Gambling/README.md) | [iGaming Home](../../source-archive/README.md)
