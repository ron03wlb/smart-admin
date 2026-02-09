# Player Lifecycle - Business Requirements

> **Canonical Source**: [docs/iGaming/source-archive/01_Player_Center/01-01_Player_Lifecycle.md](../../source-archive/01_Player_Center/01-01_Player_Lifecycle.md)
> **View**: Business Requirements (Product & Operations)
> **Technical Implementation**: [Player_Lifecycle_Implementation.md](../../architecture/01_Player_Service/Player_Lifecycle_Implementation.md)

---

## Document Purpose

This document defines the **business requirements** for Player Lifecycle Management in iGaming platforms. It covers:
- Player lifecycle stages and transitions
- KYC requirements by jurisdiction
- Player segmentation rules (RFM model)
- VIP tier progression rules
- Problem gambling indicators

**For technical implementation details** (state machine, API specs, database schema), see [Player_Lifecycle_Implementation.md](../../architecture/01_Player_Service/Player_Lifecycle_Implementation.md).

---

## 1. System Overview

### 1.1 Core Purpose

The Player Lifecycle Management (PLM) system is the foundation of the iGaming platform, responsible for managing players from registration, login, identity verification, status transitions, risk assessment, to account closure. The module supports **Multi-Tenant** architecture, ensuring complete data isolation between merchants while supporting Brand-level unified views.

### 1.2 Core Functions

**Identity & Registration Management**:
- **Multiple Registration Methods**: Account/Password, Phone/OTP, Quick Registration, Social Login (Google, Line, Telegram, Facebook)
- **Merchant Configuration**: Mandatory phone verification, custom registration fields, default and allowed currencies
- **Initialization Flow**: Create player record -> Initialize wallet -> Set default VIP tier (Bronze) -> Record device fingerprint

**Security & Risk Control Integration**:
- **Multi-Factor Authentication (MFA)**: Google Authenticator, SMS OTP, Email OTP
- **Security Controls**: Remote login alerts, brute force protection, device fingerprinting, duplicate account handling
- **Risk Assessment**: 0-100 risk scoring system integrating device, payment, behavior, and graph dimensions

**Lifecycle Management**:
- **Stage Identification**: New User, Active User, Dormant User, Churned User
- **Tagging System**: Automatic tags (High Value, Dormant, Arbitrage Suspect) + Manual tags (CS Notes)
- **Status Transitions**: 5-state state machine (ACTIVE, LOCKED, SUSPENDED, PENDING_VERIFICATION, CLOSED)

### 1.3 Design Principles

**Single Source of Truth (SSOT)**:
- Player account status state machine defined in this document, other modules reference only
- Risk scoring dimensions defined in Risk Control Framework
- Wallet creation logic defined in Unified Wallet Model

**Event-Driven Architecture**:
- Published Events: `player.registered`, `player.kyc.completed`, `player.status.changed`, `player.risk.flagged`
- Consumed Events: `wallet.deposit.completed`, `game.bet.placed`, `vip.tier.changed`

**Multi-Tenant Isolation**:
- Data Isolation: `tenant_id` column-level isolation + Row-Level Security (RLS)
- Configuration Isolation: Each tenant can customize registration fields, KYC levels, risk thresholds
- Resource Isolation: Independent risk rule sets, blacklists, whitelists

---

## 2. Lifecycle Stage Definition

### 2.1 Lifecycle Stage Matrix

**Overview**: Player lifecycle is divided into 4 main stages, dynamically calculated based on registration time and betting activity.

| Stage | Definition | Identification Criteria | Business Strategy | Average Duration | Conversion Target |
|-------|------------|------------------------|-------------------|------------------|-------------------|
| **New User** | Within 7 days of registration | Days since registration <= 7 | First deposit bonus, onboarding guide | 7 days | First deposit conversion 30-40% |
| **Active User** | Bet within last 30 days | Last bet date within 30 days | VIP upgrade, exclusive events | 30-90 days | D30 retention 40-50% |
| **Dormant User** | No bet for 30-90 days | Last bet 30-90 days ago | Reactivation bonus, EDM marketing | 60 days | Reactivation rate 20-30% |
| **Churned User** | No bet for 90+ days | Last bet 90+ days ago | Large return offers, CS outreach | Permanent | Return rate 5-10% |

### 2.2 Lifecycle Conversion Funnel

```
Registered Users (100%)
    | First Deposit Conversion 30-40%
First Deposit Users (30-40%)
    | Active Retention 40-50%
Active Users (15-20%)
    | VIP Conversion 10-15%
VIP Players (1.5-3%)
```

**Key Performance Indicators (KPIs)**:
- **First Deposit Conversion**: Percentage depositing within 7 days of registration (Target: 30-40%)
- **D7 Retention**: Percentage still active on day 7 after registration (Target: 20-30%)
- **D30 Retention**: Percentage still active on day 30 after registration (Target: 15-20%)
- **Churn Rate**: Percentage with no bet for 90 days (Target: <40%)
- **Monthly Active Users (MAU)**: Players with bets in last 30 days
- **Lifetime Value (LTV)**: Total net revenue from player across entire lifecycle

### 2.3 RFM Model Integration

**RFM Segmentation Matrix** (Recency, Frequency, Monetary):

| Segment | R (Recency) | F (Frequency) | M (Monetary) | Characteristics | Strategy |
|---------|-------------|---------------|--------------|-----------------|----------|
| **Champions** | 5 | 5 | 5 | Recent bet, high frequency, high amount | VIP exclusive events, 1-on-1 service, birthday gifts |
| **Potential Loyalists** | 4-5 | 3-4 | 3-4 | Recently active, medium frequency and amount | VIP upgrade incentives, exclusive bonuses, increase frequency |
| **At Risk** | 2-3 | 3-5 | 3-5 | Previously high value, but recently inactive | Reactivation campaigns, exclusive return bonuses, CS calls |
| **Lost** | 1 | 1-2 | 1-3 | Long-term inactive, low frequency | Large return offers, reactivation ads |

**RFM Scoring Logic**:

| Dimension | Score 5 | Score 4 | Score 3 | Score 2 | Score 1 |
|-----------|---------|---------|---------|---------|---------|
| **R (Days since last bet)** | <= 7 days | 8-30 days | 31-60 days | 61-90 days | > 90 days |
| **F (Bet count in 90 days)** | >= 100 | 50-99 | 20-49 | 5-19 | < 5 |
| **M (Total bet in 90 days)** | >= $10,000 | $5,000-9,999 | $1,000-4,999 | $100-999 | < $100 |

### 2.4 Player Tagging System

**Value Tags**:
- `VIP_WHALE` - Whale player (Monthly deposit > $50K)
- `HIGH_ROLLER` - High roller (Monthly deposit $10K-$50K)
- `REGULAR` - Regular player (Monthly deposit $1K-$10K)
- `CASUAL` - Casual player (Monthly deposit < $1K)

**Risk Tags** (Refer to Risk Framework for details):
- `BONUS_HUNTER` - Bonus hunter (Only plays high RTP games, withdraws immediately after wagering)
- `ARBITRAGE` - Arbitrager (Hedges bets across multiple platforms)
- `HEDGER` - Hedger (Multi-account hedging on same platform)
- `MULTI_ACCOUNT` - Multi-account linked (Same device fingerprint/IP/payment method)

**Behavior Tags**:
- `SLOT_LOVER` - Slot enthusiast (90% bets on slots)
- `LIVE_CASINO_FAN` - Live casino fan
- `SPORTS_BETTOR` - Sports bettor
- `NIGHT_OWL` - Night owl (Betting concentrated 22:00-06:00)

**Lifecycle Tags**:
- `FIRST_DEPOSIT_PENDING` - Pending first deposit
- `ACTIVE_7D` - Active within 7 days
- `DORMANT_30D` - Dormant for 30 days
- `CHURNED_90D` - Churned for 90 days

---

## 3. Account Status Definition

### 3.1 Five-Status Definition Table

**Overview**: Player account status uses a state machine pattern to ensure clear, traceable status transitions with degradation protection mechanisms.

| Status Code | Name | Description | Trigger Condition | Business Impact | Auto-Recovery | Player Visibility |
|-------------|------|-------------|-------------------|-----------------|---------------|-------------------|
| `ACTIVE` | Active | Default status | Initial state | No restrictions | N/A | Normal |
| `LOCKED` | Locked | Security lock | 5 consecutive login failures | Login blocked for 30 minutes | Yes (after 30 min) | Shows countdown |
| `SUSPENDED` | Suspended | Risk freeze | Risk score >= 70 | Deposit/Withdrawal/Betting blocked | No (requires manual review) | Shows appeal entry |
| `PENDING_VERIFICATION` | Pending Verification | KYC upgrade required | Withdrawal triggers KYC upgrade | Withdrawal limit restricted | Yes (after KYC completion) | Shows pending tasks |
| `CLOSED` | Closed | Permanently closed | Self-exclusion OR AML violation | Permanently closed | No (irreversible) | Shows closure reason |

### 3.2 Status Transition Rules

**ACTIVE Status Transitions**:
- ACTIVE -> LOCKED: 5 consecutive login failures within 5 minutes
- ACTIVE -> SUSPENDED: Risk score >= 70 (HIGH or CRITICAL level)
- ACTIVE -> PENDING_VERIFICATION: Withdrawal amount exceeds KYC limit
- ACTIVE -> CLOSED: Self-exclusion or AML violation

**LOCKED Status Transitions**:
- LOCKED -> ACTIVE: Automatic unlock after 30 minutes
- LOCKED -> SUSPENDED: Password reset fails 3 times

**SUSPENDED Status Transitions**:
- SUSPENDED -> ACTIVE: Manual review approval
- SUSPENDED -> CLOSED: Fraud confirmed

**PENDING_VERIFICATION Status Transitions**:
- PENDING_VERIFICATION -> ACTIVE: KYC document review approved
- PENDING_VERIFICATION -> SUSPENDED: KYC document forgery detected
- PENDING_VERIFICATION -> CLOSED: Identity verification fails 3 times

**CLOSED Status**:
- Terminal state - no transitions allowed

### 3.3 Status Transition Monitoring

**Key Monitoring Metrics**:

| Metric | Formula | Normal Range | Alert Threshold | Action |
|--------|---------|--------------|-----------------|--------|
| **Lock Rate** | LOCKED / ACTIVE | < 2% | > 5% | Check for brute force attacks |
| **Suspension Rate** | SUSPENDED / ACTIVE | < 1% | > 3% | Review risk rules severity |
| **Pending Verification Rate** | PENDING_VERIFICATION / ACTIVE | < 5% | > 10% | Review KYC limit settings |
| **Closure Rate** | CLOSED / TOTAL | < 0.5% | > 2% | Check for abnormal batch closures |
| **Average Lock Duration** | AVG(unlocked_at - locked_at) | < 30 min | > 2 hours | Check auto-unlock mechanism |

---

## 4. KYC Requirements by Jurisdiction

### 4.1 KYC Level System

**Tiered Verification System**:

| KYC Level | Verification Content | Single Withdrawal Limit | Cumulative Withdrawal Limit | Upgrade Requirement | Review SLA |
|-----------|---------------------|-------------------------|----------------------------|---------------------|------------|
| **L0** (Registration) | Phone OR Email | $500 | $1,000 / month | OTP verification only | Instant |
| **L1** (Identity Verification) | Document (Passport/ID) + Face | $5,000 | $50,000 / month | Upload document + face match | 1-4 hours |
| **L2** (Address Verification) | Utility Bill / Bank Statement | $50,000 | Unlimited | Upload address proof | 4-24 hours |

### 4.2 L1 (Identity Verification) - Accepted Documents

| Document Type | Required Information | Common Rejection Reasons |
|---------------|---------------------|-------------------------|
| **Passport** | Name, DOB, Passport Number, Photo | Expired, blurry photo, glare |
| **National ID** | Name, DOB, ID Number, Photo | Single side only, screen capture |
| **Driver's License** | Name, DOB, License Number, Photo | Provisional license not accepted |

### 4.3 L2 (Address Verification) - Accepted Documents

| Document Type | Required Information | Validity Period | Common Rejection Reasons |
|---------------|---------------------|-----------------|-------------------------|
| **Utility Bill** | Name, Address, Issue Date | Within 3 months | Name mismatch, expired |
| **Bank Statement** | Name, Address, Statement Date | Within 3 months | Partial address shown |
| **Lease Agreement** | Name, Address, Signing Date | Within 6 months | Informal agreement |

### 4.4 Third-Party KYC Providers

**Recommended Providers**:

| Provider | Country Coverage | OCR Accuracy | Liveness Accuracy | Monthly Fee | API Call Cost |
|----------|-----------------|--------------|-------------------|-------------|---------------|
| **Sumsub** | 240+ | 98.5% | 99.2% | $500 | $0.50 / call |
| **Jumio** | 200+ | 97.8% | 98.9% | $800 | $0.70 / call |
| **Onfido** | 195+ | 98.1% | 99.0% | $600 | $0.60 / call |

### 4.5 Review Decision Criteria

**Automatic Approval Criteria**:
- OCR confidence score >= 95%
- Face match score >= 98%
- No blacklist matches
- Age >= 21

**Manual Review Required**:
- OCR confidence score < 95%
- Rare document type
- Age 18-20 (risk age bracket)
- Face match score < 98%

**Automatic Rejection**:
- Blacklisted player
- Age < 18 (underage)

**Manual Review SLA**:

| Priority | Player Type | Review SLA | Reviewer Assignment |
|----------|-------------|------------|---------------------|
| **P0 - Urgent** | VIP Diamond | 30 minutes | Dedicated reviewer (24/7) |
| **P1 - High** | VIP Platinum/Gold | 2 hours | Priority queue |
| **P2 - Normal** | Other players | 4 hours | Normal queue |
| **P3 - Low** | L2 upgrade (non-mandatory) | 24 hours | Batch review |

---

## 5. Player Segmentation Rules

### 5.1 Value-Based Segmentation

**Deposit-Based Tiers**:

| Tier | Monthly Deposit | Characteristics | Service Level |
|------|-----------------|-----------------|---------------|
| **Whale** | > $50,000 | Ultra-high value, personal service | Dedicated VIP manager, 24/7 priority |
| **High Roller** | $10,000 - $50,000 | High value, frequent large bets | VIP service, priority support |
| **Regular** | $1,000 - $10,000 | Moderate value, consistent activity | Standard VIP benefits |
| **Casual** | < $1,000 | Low value, occasional activity | Basic service |

### 5.2 Behavior-Based Segmentation

**Gaming Preference Segments**:
- **Slot Enthusiast**: 90%+ of bets on slots
- **Live Casino Fan**: Primarily plays live dealer games
- **Sports Bettor**: Primarily bets on sports
- **Mixed Player**: Balanced activity across game types

**Activity Pattern Segments**:
- **Peak Hour Player**: Active during 18:00-22:00
- **Night Owl**: Active during 22:00-06:00
- **Weekend Warrior**: Primarily active on weekends
- **Daily Player**: Consistent daily activity

---

## 6. VIP Tier Progression Rules

### 6.1 VIP Tier Structure

| VIP Tier | Point Threshold | Monthly Requirement | Benefits | Downgrade Protection |
|----------|-----------------|---------------------|----------|---------------------|
| **Bronze** | 0 | Default | Basic cashback 0.1% | N/A |
| **Silver** | 1,000 | 500 pts/month | Cashback 0.3%, Weekly bonus | 1 month grace |
| **Gold** | 10,000 | 2,000 pts/month | Cashback 0.5%, Dedicated CS | 2 months grace |
| **Platinum** | 50,000 | 10,000 pts/month | Cashback 0.8%, VIP events | 3 months grace |
| **Diamond** | 200,000 | 30,000 pts/month | Cashback 1.2%, Personal manager | 6 months grace |

### 6.2 Point Earning Rules

**Betting Points**:
- Slots: 1 point per $10 wagered
- Live Casino: 1 point per $20 wagered
- Sports: 1 point per $25 wagered
- Table Games: 1 point per $20 wagered

**Bonus Points**:
- First deposit: 2x points
- Birthday month: 2x points
- Special promotions: Variable multiplier

### 6.3 Tier Progression Logic

**Upgrade Criteria**:
- Accumulate required points (no time limit)
- Automatic upgrade upon reaching threshold

**Downgrade Criteria**:
- Fail to meet monthly requirement after grace period
- Drop to next lower tier (never below Bronze)

**Grace Period Benefits**:
- Maintain current tier benefits during grace period
- Warning notification before downgrade
- One-time save opportunity (special promotion)

---

## 7. Problem Gambling Indicators

### 7.1 Risk Indicators

**Spending Patterns**:
- Daily deposit exceeds 3x average
- Multiple deposits within 1 hour
- Deposit immediately after large loss
- Chasing losses (increasing bets after losses)

**Time Patterns**:
- Session exceeds 4 hours continuously
- Active during late night hours (02:00-06:00)
- Gaming during work hours consistently
- Increased frequency over time

**Behavioral Indicators**:
- Multiple failed deposit attempts
- Contacting support for limit increases
- Complaints about losses
- Requesting to close then reopen account

### 7.2 Intervention Triggers

| Risk Level | Indicators | Automated Action | Manual Action |
|------------|------------|------------------|---------------|
| **Low** | 1-2 indicators | In-game reminder | None |
| **Medium** | 3-4 indicators | Reality check popup | CS reach out |
| **High** | 5+ indicators | Mandatory cool-off | VIP manager call |
| **Critical** | Pattern matches addiction | Forced self-exclusion | Responsible gambling team |

### 7.3 Player Protection Tools

**Self-Imposed Limits**:
- Deposit limits (daily/weekly/monthly)
- Loss limits (daily/weekly/monthly)
- Wager limits (per bet/daily)
- Session time limits

**Cool-Off Options**:
- 24-hour cool-off
- 7-day cool-off
- 30-day cool-off
- Self-exclusion (6 months / permanent)

### 7.4 Regulatory Requirements

**UKGC (UK Gambling Commission)**:
- Mandatory affordability checks for losses > GBP 1,000/month
- Source of funds verification required
- Marketing opt-out easily accessible

**MGA (Malta Gaming Authority)**:
- Self-exclusion network integration required
- Reality checks every 60 minutes
- Session history readily available

---

## 8. Related Documents

### Business Documents
- VIP & Loyalty Program Requirements
- Responsible Gambling Policy
- AML/KYC Policy

### Technical Documents

→ **[Player Lifecycle Implementation](../../architecture/01_Player_Service/Player_Lifecycle_Implementation.md)** - Complete player journey state machines, API specifications, database schemas, and KYC/AML integration patterns

**Additional References**:
- Risk Control Framework
- Unified Wallet Model

---

**Document Version**: 1.0.0
**Created Date**: 2026-02-08
**Last Updated**: 2026-02-08
**Maintainers**: Player Center Team & Product Team

**Change History**:
- 1.0.0 (2026-02-08): Initial version - Split from source document (business requirements view)
