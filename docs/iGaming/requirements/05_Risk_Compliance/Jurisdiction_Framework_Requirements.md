# Multi-Jurisdiction Framework Requirements (多牌照合規業務需求)

> **Canonical Source**: [06-07_Multi_Jurisdiction_Framework.md](../../source/06_Platform_Governance/06-07_Multi_Jurisdiction_Framework.md)
> **Audience**: Executives, Compliance Officers
> **Related Doc**: [Jurisdiction_Routing_Architecture.md](../../architecture/06_Platform_Core/Jurisdiction_Routing_Architecture.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

The Multi-Jurisdiction Framework enables the iGaming platform to operate under multiple gambling licences simultaneously, applying jurisdiction-specific compliance rules based on the player's geographic location and registered licence. This is a foundational requirement for any operator seeking to serve players across different regulatory markets.

---

## 2. Business Objectives

| Objective | Description | Priority |
|-----------|-------------|----------|
| Multi-Licence Operations | Support simultaneous operations under multiple gambling licences | P0 |
| Dynamic Compliance | Automatically apply the correct regulatory rules per player jurisdiction | P0 |
| Geographic Enforcement | Prevent players from accessing the platform from restricted regions | P0 |
| Regulatory Reporting | Generate jurisdiction-specific compliance reports | P1 |
| Scalable Expansion | Enable rapid onboarding of new jurisdictions without code changes | P1 |

---

## 3. Supported Licences

| Licence | Regulatory Body | Region | Strictness Level |
|---------|----------------|--------|-----------------|
| UKGC | UK Gambling Commission | United Kingdom | Very High |
| MGA | Malta Gaming Authority | European Union | High |
| PAGCOR | Philippine Amusement and Gaming Corporation | Philippines | Medium |
| Curacao | Curacao eGaming | Offshore | Low |
| Brazil SPA | Secretariat of Prizes and Bets | Brazil | High |

---

## 4. Jurisdiction Configuration Requirements

### 4.1 Geographic Controls

| Requirement | Details |
|-------------|---------|
| Country Allow-List | Each licence defines which countries are permitted |
| Country Block-List | Each licence defines which countries are explicitly blocked |
| Sub-National Restrictions | Support for state/province-level restrictions (e.g., US states) |
| Default Behaviour | If a player's country matches no licence, access is denied |

### 4.2 KYC Requirements by Jurisdiction

| Requirement | UKGC | MGA | PAGCOR | Curacao | Brazil SPA |
|-------------|------|-----|--------|---------|------------|
| Immediate KYC before deposit | Yes | No | No | No | Yes |
| KYC Grace Period | None | 72 hours | 30 days | None | None |
| Enhanced Due Diligence | Required | Case-by-case | No | No | Required |
| Source of Funds | Required (high value) | Required (high value) | No | No | Required |

### 4.3 Responsible Gambling Requirements

| Requirement | UKGC | MGA | PAGCOR | Curacao | Brazil SPA |
|-------------|------|-----|--------|---------|------------|
| Self-Exclusion | Mandatory | Mandatory | Optional | Optional | Mandatory |
| Deposit Limits | Mandatory | Mandatory | Optional | Optional | Mandatory |
| Mandatory Cooling-Off | Yes | No | No | No | Yes |
| Reality Check Interval | 30 minutes | 60 minutes | None | None | 30 minutes |
| Affordability Check | Required | No | No | No | Required |

### 4.4 Game Restrictions

| Requirement | Details |
|-------------|---------|
| Allowed Game Types | Each licence defines which game types are permitted |
| Blocked Game Types | Each licence defines which game types are prohibited |
| Maximum Bet Amount | Jurisdiction-specific maximum wager limits |
| Maximum Win Amount | Jurisdiction-specific maximum payout limits |

### 4.5 Payment Restrictions

| Requirement | UKGC | MGA | PAGCOR | Curacao | Brazil SPA |
|-------------|------|-----|--------|---------|------------|
| Credit Cards Allowed | No (banned since April 2020) | Yes | Yes | Yes | No |
| Cryptocurrency Allowed | No | No | Yes | Yes | No |
| Allowed Payment Methods | Bank transfer, debit card, e-wallets | All standard methods | All methods | All methods | PIX, bank transfer |

### 4.6 Tax and Financial Requirements

| Requirement | Details |
|-------------|---------|
| GGR Tax Rate | Jurisdiction-specific gross gaming revenue tax rate |
| Withholding Tax | Whether winnings are subject to withholding tax |
| Withholding Tax Rate | The applicable withholding tax rate on player winnings |
| Reporting Frequency | How often financial reports must be submitted to the regulator |

---

## 5. Player Journey Rules

### 5.1 Registration

| Step | Policy |
|------|--------|
| Geo-Detection | Player's country is determined from IP address at registration |
| Licence Assignment | Player is assigned to the appropriate licence based on their country |
| Age Verification | Minimum age varies by jurisdiction (18 for UKGC/MGA, 21 for PAGCOR) |
| Gamstop Check (UKGC) | All UK registrations must be checked against the Gamstop national exclusion database |
| KYC Timing | Depends on jurisdiction (immediate for UKGC, grace period for MGA) |

### 5.2 Deposits

| Step | Policy |
|------|--------|
| Payment Method Validation | Only payment methods permitted by the player's jurisdiction are offered |
| Credit Card Block | Credit cards are blocked in jurisdictions that prohibit them (UKGC, Brazil) |
| Affordability Check | In jurisdictions that require it, players must pass an affordability assessment before large deposits |
| Deposit Limit Enforcement | Deposits are checked against the player's configured limits |

### 5.3 Game Launch

| Step | Policy |
|------|--------|
| Game Type Validation | The game type must be permitted in the player's jurisdiction |
| Self-Exclusion Check | Players with active self-exclusion cannot launch any game |
| Cooling-Off Check | Players in a cooling-off period cannot launch any game |
| Bet Limits | Maximum bet amounts are enforced per jurisdiction configuration |

---

## 6. Geographic Enforcement (Geo-Fencing)

### 6.1 Access Control Requirements

| Requirement | Details |
|-------------|---------|
| IP-Based Detection | Player location is determined using GeoIP services |
| VPN/Proxy Detection | VPN and proxy usage must be detected and blocked |
| Continuous Monitoring | Active player sessions must be periodically re-validated for location compliance |
| Session Termination | If a player's location changes to a restricted region during a session, the session must be terminated |
| Player Notification | Players must be notified when access is denied or a session is terminated due to geo-fencing |

### 6.2 Monitoring Frequency

| Check Type | Frequency |
|------------|-----------|
| Registration | On registration attempt |
| Login | On every login |
| Active Session | Every 5 minutes during active sessions |
| Deposit | Before each deposit |
| Game Launch | Before each game launch |

---

## 7. Compliance Reporting

### 7.1 Report Requirements by Jurisdiction

| Report Type | UKGC | MGA | PAGCOR | Curacao | Brazil SPA |
|-------------|------|-----|--------|---------|------------|
| Monthly Activity Report | Required | Required | Required | Optional | Required |
| Quarterly Financial Report | Required | Required | Optional | Optional | Required |
| Annual Audit Report | Required | Required | Required | Optional | Required |
| Incident Report | Within 5 days | Within 72 hours | Within 30 days | None | Within 48 hours |
| Self-Exclusion Report | Monthly | Quarterly | None | None | Monthly |

### 7.2 Dashboard Requirements

| Feature | Details |
|---------|---------|
| Licence Status Cards | At-a-glance view of each licence's compliance status |
| Active Player Count | Number of active players per jurisdiction |
| Next Report Due | Countdown to the next required regulatory submission |
| Compliance Alerts | Real-time alerts for compliance violations or upcoming deadlines |
| Alert Severity Levels | CRITICAL (immediate action) and WARNING (attention needed) |

---

## 8. External System Integration

| Integration | Applicable Jurisdictions | Purpose |
|-------------|------------------------|---------|
| Gamstop | UKGC | National self-exclusion scheme for UK players |
| GeoIP Service | All | Player location detection and geo-fencing |
| Tax Authority Systems | Per jurisdiction | Tax reporting and withholding |
| Regulator Portals | Per jurisdiction | Regulatory report submission |

---

## 9. New Jurisdiction Onboarding

### 9.1 Onboarding Checklist

| Step | Description |
|------|-------------|
| 1. Regulatory Analysis | Document all regulatory requirements for the new jurisdiction |
| 2. Configuration Setup | Create jurisdiction configuration with all required parameters |
| 3. Payment Integration | Configure permitted payment methods for the jurisdiction |
| 4. Game Configuration | Define allowed and blocked game types |
| 5. KYC/AML Setup | Configure KYC requirements and AML thresholds |
| 6. Responsible Gambling | Enable required responsible gambling tools |
| 7. Reporting Setup | Configure reporting templates and submission schedules |
| 8. Geo-Fencing | Set up country allow/block lists |
| 9. Testing | End-to-end testing of all jurisdiction-specific flows |
| 10. Go-Live | Activate the jurisdiction configuration |

### 9.2 Configuration-Driven Design Principle

New jurisdictions must be onboardable through configuration changes only, without requiring code modifications. All jurisdiction-specific behaviours must be driven by the jurisdiction configuration data model.

---

## 10. Cross-References

| Topic | Document |
|-------|----------|
| UKGC Compliance Details | source/06_Platform_Governance/06-08_UKGC_Compliance.md |
| MGA Compliance Details | source/06_Platform_Governance/06-09_MGA_Compliance.md |
| Brazil SPA Compliance Details | source/06_Platform_Governance/06-10_Brazil_SPA_Compliance.md |
| PAGCOR & Curacao Details | source/06_Platform_Governance/06-11_PAGCOR_Curacao.md |
| Self-Exclusion | source/15_Responsible_Gambling/15-01_Self_Exclusion.md |
| Technical Architecture | architecture/06_Platform_Core/Jurisdiction_Routing_Architecture.md |
