# Player Protection Requirements (玩家保護業務需求)

> **Canonical Source**: [15-07_Player_Protection_API.md](../../source/15_Responsible_Gambling/15-07_Player_Protection_API.md)
> **Audience**: Executives, Compliance Officers
> **Related Doc**: [Player_Protection_API.md](../../architecture/05_Risk_Engine/Player_Protection_API.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

The Player Protection module provides a unified set of responsible gambling tools that enable players to manage their own gambling activity. These tools are mandatory under multiple regulatory frameworks (UKGC, MGA, Brazil SPA) and serve as the platform's primary mechanism for player harm prevention.

---

## 2. Business Objectives

| Objective | Description | Priority |
|-----------|-------------|----------|
| Regulatory Compliance | Meet responsible gambling requirements across all licensed jurisdictions | P0 |
| Player Empowerment | Allow players to set and manage their own protection limits | P0 |
| Harm Prevention | Proactively detect and intervene in problem gambling behaviours | P0 |
| Audit Readiness | Maintain full audit trail of all protection actions for regulatory reporting | P1 |
| Operator Intervention | Enable operators to exclude players when warranted | P1 |

---

## 3. Player-Facing Protection Tools

### 3.1 Self-Exclusion

Players must be able to voluntarily exclude themselves from all gambling activity for a defined period.

| Requirement | Details |
|-------------|---------|
| Duration Options | 24 hours, 7 days, 30 days, 6 months, 1 year, 5 years, Permanent |
| Activation | Immediate upon player request; no operator approval required |
| Scope | All gambling activity across the platform (all game types) |
| Revocation | Player may request revocation only after the exclusion period ends; permanent exclusions cannot be revoked |
| Confirmation | Player must confirm understanding before activation |
| Gamstop Sync (UKGC) | Self-exclusion must synchronise with Gamstop for UK-licensed operations |

### 3.2 Cooling-Off Period

A lighter-weight alternative to self-exclusion for players who want a short break.

| Requirement | Details |
|-------------|---------|
| Duration Options | Configurable (e.g., 1 day, 3 days, 7 days) |
| Activation | Immediate upon player request |
| Scope | All gambling activity |
| Auto-Expiry | Cooling-off period ends automatically at the scheduled time |
| Overlap Rule | Cannot activate cooling-off while self-exclusion is active |

### 3.3 Deposit Limits

Players must be able to set maximum deposit amounts across daily, weekly, and monthly periods.

| Requirement | Details |
|-------------|---------|
| Periods | Daily, Weekly, Monthly |
| Hierarchy Rule | Daily limit must not exceed weekly limit; weekly limit must not exceed monthly limit |
| Decrease | Takes effect immediately |
| Increase | Subject to a mandatory cooling-off period (minimum 24 hours) before taking effect |
| Pending Changes | Players must be able to view and cancel pending limit increases |
| Reset Timing | Limits reset at the start of each respective period (midnight UTC for daily, Monday for weekly, 1st of month for monthly) |

### 3.4 Loss Limits

Players must be able to set maximum net loss amounts across configurable periods.

| Requirement | Details |
|-------------|---------|
| Periods | Daily, Weekly, Monthly (monthly is optional) |
| Calculation | Net loss = total wagered - total won within the period |
| Decrease | Takes effect immediately |
| Increase | Subject to the same cooling-off rules as deposit limits |
| Enforcement | When a loss limit is reached, all further wagering must be blocked until the period resets |

### 3.5 Session Limits

Players must be able to control their session duration and receive periodic reminders.

| Requirement | Details |
|-------------|---------|
| Duration Limit | Maximum session length in minutes (player-configurable) |
| Idle Timeout | Automatic session termination after a period of inactivity |
| Reality Checks | Periodic notifications showing session duration, net result, and option to continue or quit |
| Reality Check Interval | Configurable (default: 30 minutes) |

---

## 4. Limit Usage Visibility

Players must have real-time visibility into their current limit utilisation.

| Data Point | Details |
|------------|---------|
| Current Usage | Amount used within the current period |
| Remaining Amount | Difference between limit and usage |
| Usage Percentage | Percentage of limit consumed |
| Reset Time | When the current period ends and usage resets |
| Session Duration | Current session length relative to session limit |
| Next Reality Check | Scheduled time for the next reality check notification |

---

## 5. Activity History

Players must be able to review their complete protection activity history.

| Requirement | Details |
|-------------|---------|
| Record Types | Exclusion events, limit changes, cooling-off periods, reality check responses |
| Date Filtering | Filter by start date, end date |
| Type Filtering | Filter by activity type |
| Pagination | Standard paginated access |
| Retention | Minimum 5 years (regulatory requirement) |

---

## 6. Operator Protection Tools

### 6.1 Operator-Initiated Exclusion

Operators must be able to exclude players when problem gambling indicators are identified.

| Requirement | Details |
|-------------|---------|
| Authority | Authorised staff with appropriate permissions |
| Duration Options | Same as player self-exclusion |
| Reason Capture | Mandatory reason field for audit trail |
| Player Notification | Optional notification to the affected player |
| Audit Trail | Full record of who excluded whom, when, and why |

### 6.2 Exclusion Management

Operators must be able to view and manage all player exclusions.

| Requirement | Details |
|-------------|---------|
| Status Filter | Active, Completed, Pending Revocation |
| Type Filter | Self-exclusion, Operator-initiated, Gamstop |
| Player Details | Masked personal data with full details available to authorised staff |
| Gamstop Sync Status | Whether the exclusion has been synchronised with Gamstop |

### 6.3 Gamstop Integration (UKGC Requirement)

For UK-licensed operations, the platform must integrate with the Gamstop national self-exclusion scheme.

| Requirement | Details |
|-------------|---------|
| Registration Check | All new UK player registrations must be checked against Gamstop |
| Periodic Sync | Regular synchronisation to detect new Gamstop exclusions |
| Data Required | First name, last name, date of birth, postcode |
| Failure Handling | Gamstop sync failures must be logged, alerted, and retried |

---

## 7. Compliance Reporting

### 7.1 Report Types

| Report | Frequency | Content |
|--------|-----------|---------|
| Self-Exclusion Summary | Monthly, Quarterly, Annual | New, active, completed, revoked exclusions |
| Deposit Limit Summary | Monthly | Players with limits, limit breaches, average limit value |
| Loss Limit Summary | Monthly | Players with limits, limit breaches |
| Reality Check Summary | Monthly | Total checks, continue rate, average response time |
| Gamstop Sync Report | Monthly | Checks performed, matches found, sync failures |

### 7.2 Report Requirements

| Requirement | Details |
|-------------|---------|
| Export Formats | JSON, CSV, PDF |
| Download | Reports must be downloadable via a secure link |
| Retention | Reports must be retained for the period required by each jurisdiction |
| Timeliness | Reports must be generated within 24 hours of the reporting period end |

---

## 8. Error Scenarios

| Error Code | Business Scenario | Expected Behaviour |
|------------|-------------------|-------------------|
| RG_001 | Player attempts to gamble while excluded | Block activity, display exclusion end date |
| RG_002 | Player attempts to gamble during cooling-off | Block activity, display cooling-off end date |
| RG_003 | Deposit exceeds the current deposit limit | Block deposit, display remaining allowance |
| RG_004 | Wagering would exceed the loss limit | Block wager, display remaining loss allowance |
| RG_005 | Player requests revocation of permanent exclusion | Deny request, explain permanent nature |
| RG_006 | Player requests revocation before exclusion period ends | Deny request, display remaining exclusion time |
| RG_007 | Player attempts to cancel a non-existent pending limit change | Inform player that no pending changes exist |
| RG_008 | Player attempts to gamble during a mandatory break | Block activity, display break end time |
| RG_009 | Gamstop synchronisation fails | Log failure, alert operations, retry automatically |
| RG_010 | Invalid limit hierarchy (e.g., daily limit exceeds weekly) | Reject change, explain hierarchy rule |

---

## 9. Limit Change Policies

### 9.1 Immediate vs Delayed Changes

| Change Type | Effective Timing | Rationale |
|-------------|-----------------|-----------|
| Decrease any limit | Immediate | Protects player; no reason to delay |
| Increase any limit | After mandatory cooling-off (min 24h) | Prevents impulsive limit increases during gambling sessions |
| Set new limit (from none) | Immediate | New protection measure benefits the player |
| Remove limit entirely | After mandatory cooling-off (min 24h) | Treated as an increase (removing protection) |

### 9.2 Pending Change Rules

- Players may cancel a pending increase at any time before it takes effect
- Only one pending change per limit type is allowed at a time
- A new pending change replaces any existing pending change for the same limit type

---

## 10. Cross-References

| Topic | Document |
|-------|----------|
| Self-Exclusion Details | source/15_Responsible_Gambling/15-01_Self_Exclusion.md |
| Deposit Limits Details | source/15_Responsible_Gambling/15-02_Deposit_Limits.md |
| Loss Limits Details | source/15_Responsible_Gambling/15-06_Loss_Limits.md |
| Session Management | source/15_Responsible_Gambling/15-04_Session_Management.md |
| Reality Checks | source/15_Responsible_Gambling/15-05_Reality_Checks.md |
| API Design Standards | source/09_Technical_Infrastructure/09-03-04_Domain_APIs.md |
| Technical API Specification | architecture/05_Risk_Engine/Player_Protection_API.md |

### Technical Implementation

→ **[Player Protection API](../../architecture/05_Risk_Engine/Player_Protection_API.md)** - Self-exclusion workflows, deposit/loss limit enforcement, session timeout mechanisms, reality check intervals, cooling-off period management, and responsible gambling tool APIs
