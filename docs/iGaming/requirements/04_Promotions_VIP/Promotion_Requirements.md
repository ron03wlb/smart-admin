# Promotion Requirements

> **Canonical Source**: [00-13_Promotion_Implementation.md](../../source-archive/00_Foundation/guides/00-13_Promotion_Implementation.md)
> **Audience**: Executives, Product Managers, Operations Teams
> **Related Architecture**: [Promotion_Implementation.md](../../architecture/04_Activity_Engine/Promotion_Implementation.md)
> **Last Synced**: 2026-02-08

---

## Business Value

The promotion system delivers critical business value by:
- **Player Acquisition**: First deposit bonuses convert registrations to active depositors with industry-standard 25-40% FTD conversion rates
- **Player Retention**: VIP tier system rewards loyalty, reducing churn by incentivizing continued play through progressive benefits
- **Revenue Protection**: Anti-duplicate claim mechanisms and wagering requirements prevent bonus abuse that accounts for 63.8% of iGaming fraud
- **Competitive Positioning**: Configurable bonus engine enables rapid deployment of market-competitive promotions without code changes
- **Regulatory Compliance**: Accurate wagering tracking supports AML turnover requirements and audit obligations

---

## 1. Overview

This document defines the business requirements for the iGaming platform promotion system, covering bonus distribution, wagering requirement tracking, and VIP tier management. These requirements ensure promotions are properly configured, securely distributed, and accurately tracked.

---

## 2. Bonus Distribution Engine Requirements

**Business Goal**: Build a configurable bonus engine that supports multiple bonus types, enforces distribution rules, and prevents abuse through anti-duplication mechanisms.

### Bonus Types

| Bonus Type | Description | Trigger |
|-----------|-------------|---------|
| First Deposit Bonus | Percentage match on player's first deposit | First successful deposit |
| Wagering Bonus | Reward for reaching wagering milestones | Cumulative bet threshold reached |
| Activity Bonus | Time-limited promotional bonuses | Campaign enrollment |
| Reload Bonus | Bonus on subsequent deposits | Qualifying deposit |
| Referral Bonus | Reward for successful player referrals | Referred player completes requirements |

### Functional Requirements

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| PROMO-BONUS-01 | Bonus type configuration | Critical | All bonus types (first deposit, wagering, activity, etc.) can be configured |
| PROMO-BONUS-02 | Distribution rule engine | Critical | Bonus rules execute correctly for all trigger conditions |
| PROMO-BONUS-03 | Anti-duplicate claim | Critical | A player cannot claim the same bonus more than once per eligibility window |
| PROMO-BONUS-04 | Bonus wallet balance | High | Bonus funds are credited to the correct wallet with accurate balance |

### Eligibility Rules

| Rule | Description |
|------|-------------|
| Player Status | Player must be active and KYC-verified |
| Deposit Minimum | Qualifying deposit must meet minimum threshold |
| Time Window | Claim must occur within promotion validity period |
| Wagering Status | No outstanding incomplete wagering from previous bonuses |
| Geographic Eligibility | Player's jurisdiction permits the promotion type |

### Compliance Checklist

- Bonus types are correctly configured and match business specifications
- Distribution rules execute accurately for all trigger scenarios
- Anti-duplicate claim mechanism prevents concurrent exploitation
- Bonus wallet balances are correct after distribution

### Known Risks

| Risk | Description | Mitigation |
|------|-------------|------------|
| Duplicate Claims | Concurrent requests allow double-claiming | Implement distributed locking on claim operations |
| Incomplete Wagering | New bonus issued while previous wagering is incomplete | Check outstanding wagering before new bonus distribution |
| Expired Bonuses | Unclaimed or expired bonuses remain in system | Schedule automated cleanup of expired bonuses |

---

## 3. Wagering Requirement Tracking

**Business Goal**: Accurately track player wagering progress against bonus requirements, ensuring fair calculation across all game types with real-time progress visibility.

### Wagering Calculation Rules

| Factor | Description |
|--------|-------------|
| Valid Bet Definition | Only settled bets count toward wagering; voided/cancelled bets are excluded |
| Game Weight | Different game categories contribute different percentages toward wagering |
| Accumulation Period | Wagering accumulates across days until requirement is met or expires |
| Cancellation Handling | Cancelled bets must be deducted from accumulated wagering progress |

### Game Weight Examples

| Game Category | Wagering Contribution |
|--------------|----------------------|
| Slots | 100% |
| Table Games | 50% |
| Live Casino | 25% |
| Sports Betting | Varies by market type |

### Functional Requirements

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| PROMO-WAG-01 | Valid bet calculation | Critical | Only settled, non-voided bets contribute to wagering |
| PROMO-WAG-02 | Real-time progress updates | High | Wagering progress reflects within 30 seconds of bet settlement |
| PROMO-WAG-03 | Completion notifications | High | Player receives notification when wagering requirement is met |
| PROMO-WAG-04 | Historical wagering audit trail | Medium | All wagering records are traceable for compliance review |

### Compliance Checklist

- Valid bet calculations are accurate across all game types
- Wagering progress updates in real time
- Completion notifications are sent promptly
- Historical wagering records are fully traceable

### Known Risks

| Risk | Description | Mitigation |
|------|-------------|------------|
| Game Weight Errors | Incorrect weight assignment for game categories | Maintain centralized weight configuration with approval workflow |
| Cancelled Bet Handling | Cancelled bets not properly deducted | Implement deduction logic triggered on bet cancellation events |
| Cross-Day Accumulation | Progress resets incorrectly at day boundary | Use cumulative tracking without daily reset boundaries |

---

## 4. VIP Tier System Requirements

**Business Goal**: Implement a multi-tier VIP program that rewards loyal players with progressive benefits, clear upgrade criteria, and fair demotion policies.

### VIP Tier Structure

| Tier | Upgrade Criteria | Key Benefits |
|------|-----------------|--------------|
| Bronze | Default entry level | Basic promotions access |
| Silver | Points or deposit threshold | Enhanced bonus percentages |
| Gold | Higher points or deposit threshold | Priority withdrawals, personal manager |
| Platinum | Top-tier achievement | Exclusive events, maximum bonus rates |
| Diamond | Invitation only | Bespoke benefits, highest limits |

### Functional Requirements

| ID | Requirement | Priority | Acceptance Criteria |
|----|-------------|----------|---------------------|
| PROMO-VIP-01 | VIP tier calculation | Critical | Tier assignment is accurate based on configured criteria |
| PROMO-VIP-02 | Upgrade trigger accuracy | Critical | Upgrade occurs immediately when criteria are met |
| PROMO-VIP-03 | Demotion mechanism | High | Demotion follows grace period and notification policies |
| PROMO-VIP-04 | Exclusive benefits activation | High | Tier-specific benefits are active immediately on tier change |

### Demotion Policy

| Policy Element | Description |
|---------------|-------------|
| Retention Conditions | Minimum activity required to maintain current tier |
| Grace Period | Buffer period before demotion takes effect |
| Benefit Removal | Tier-specific benefits are removed upon confirmed demotion |
| Notification | Player is notified of impending demotion during grace period |

### Compliance Checklist

- VIP tiers are correctly calculated based on defined criteria
- Upgrades trigger accurately and immediately
- Demotion mechanism operates with proper grace period
- Exclusive benefits activate and deactivate on tier changes

### Known Risks

| Risk | Description | Mitigation |
|------|-------------|------------|
| Demotion Rules | Unclear retention conditions cause player confusion | Define and publish clear retention criteria |
| Benefit Deactivation | Demoted player retains elevated benefits | Implement benefit removal triggered by demotion event |
| Points Expiration | Expired points not cleaned up | Schedule periodic points expiration processing |

---

## 5. Reference Documents

| Area | Reference |
|------|-----------|
| Activity Bonus System | 04-04 Activity Bonus |
| Bonus Calculation Engine | 04-02 Bonus Calculation Engine |
| Turnover Calculation | 03-04 Turnover Calculation |
| VIP & Loyalty | 01-06 VIP Loyalty |
| Wallet Architecture | 02-06 Wallet Architecture |

### Technical Implementation

→ **[Promotion Implementation Architecture](../../architecture/04_Activity_Engine/Promotion_Implementation.md)** - Promotion rule engine, VIP tier calculation algorithms, benefit activation workflows, grace period management, and points expiration scheduling

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Source Version**: 4.0.0
