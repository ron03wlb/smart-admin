# iGaming Platform Overview

> **Canonical Source**: [00-01_Quickstart.md](../../source-archive/00_Foundation/00-01_Quickstart.md)
> **Audience**: Executives, Product Managers, Business Stakeholders
> **Related Doc**: [System_Overview.md](../../architecture/00_Overview/System_Overview.md)
> **Last Synced**: 2026-02-08

---

## 1. Platform Vision

The iGaming platform is a comprehensive online gaming solution designed to serve multiple brands (tenants) within a single unified system. It addresses five core business domains that together cover 80% of the system's design considerations:

1. **Wallet and Bettable Balance** -- Ensuring players always know exactly how much they can wager
2. **Turnover Calculation (Valid Bets)** -- Determining which wagers count towards promotional and compliance requirements
3. **Token Verification and API Security** -- Safeguarding interactions between the platform and game providers
4. **Multi-Tenant Architecture and Data Isolation** -- Supporting multiple brands with complete data separation
5. **Risk Control Rule Engine** -- Real-time detection of abnormal behaviour to prevent fraud and money laundering

---

## 2. Core Business Concepts

### 2.1 Wallet and Bettable Balance

The most fundamental formula in the platform determines how much a player can wager:

| Component | Description |
|-----------|-------------|
| Cash Balance | The player's deposited funds |
| Locked Amount | Funds reserved or frozen by the system |
| In-Progress Bets | Wagers currently being settled |
| **Bettable Balance** | **Cash Balance - Locked Amount - In-Progress Bets** |

**Business Impact**:
- Incorrect calculations lead to either over-betting (platform losses) or blocked betting (poor customer experience)
- Correct calculations ensure both fund safety and player satisfaction

**Key Scenarios**:
- When a player places a bet: check bettable balance, lock funds, deduct
- When a game settles: release lock, settle win/loss, update balance

### 2.2 Turnover Calculation (Valid Bets)

Different game types contribute differently to turnover requirements:

| Game Type | Turnover Weight | Rationale |
|-----------|----------------|-----------|
| Slots | 100% | Pure luck, full contribution |
| Baccarat | 95% | Some skill involved; tie exception |
| Sports Betting | Actual risk amount only | High hedging risk; only settled risk counts |

**Business Significance**:
- **Promotional Requirements**: e.g., "Deposit 100, get 50 bonus, requires 20x turnover" means the player must generate (100+50) x 20 = 3,000 in valid bets
- **VIP Level Calculation**: Monthly turnover thresholds determine VIP tier upgrades
- **Anti-Money Laundering (AML)**: Players must achieve minimum 1x turnover after depositing before withdrawing

### 2.3 Multi-Tenant Architecture

The platform supports a four-tier hierarchy for multi-brand operations:

| Level | Entity | Purpose |
|-------|--------|---------|
| Level 1 | Platform | Top-level system operator |
| Level 2 | Brand / Tenant | Independent brand with isolated data |
| Level 3 | Agent | Distribution and referral layer |
| Level 4 | Player | End user |

**Business Benefits**:
- **Data Security**: Brand A's player data is never visible to Brand B
- **Business Flexibility**: Each brand can have unique games, promotions, and configurations
- **Regulatory Compliance**: Different brands can satisfy different jurisdictional requirements

### 2.4 Risk Control Rule Engine

The risk engine operates across multiple checkpoints throughout the player journey:

| Checkpoint | Action | Outcomes |
|------------|--------|----------|
| Deposit | Frequency and amount limit checks | Pass / Reject |
| Withdrawal | KYC verification, AML checks, turnover validation | Pass / Pending Review / Reject |
| Betting | Real-time hedging detection | Pass / Reject |
| Promotion Claim | Eligibility checks | Pass / Reject |

**Risk Scoring Model**:

| Risk Level | Score Range | Action |
|------------|-------------|--------|
| Low Risk | 0 -- 30 | Automatic approval |
| Medium Risk | 31 -- 70 | Manual review required |
| High Risk | 71 -- 100 | Automatic rejection |

**Business Value**:
- Detects professional players, hedge arbitrage, and money laundering behaviour
- Prevents bonus abuse and abnormal withdrawals
- Satisfies AML regulatory requirements

---

## 3. End-to-End Player Journey

The complete player lifecycle connects all five core concepts:

| Step | Activity | Core Concept Involved |
|------|----------|-----------------------|
| 1 | Player Registration | Multi-Tenant (assigned to a Tenant) |
| 2 | KYC Verification | Risk Control |
| 3 | First Deposit | Wallet |
| 4 | Risk Check on Deposit | Risk Control |
| 5 | Claim Bonus | Wallet (Promotional Wallet) |
| 6 | Enter Game | Token Verification |
| 7 | Place Bets / Play | Wallet + Turnover |
| 8 | Accumulate Turnover | Turnover Calculation |
| 9 | Turnover Requirement Met | Bonus converts to cash |
| 10 | Request Withdrawal | Risk Control (withdrawal check) |
| 11 | Withdrawal Approved | Wallet (funds released) |

---

## 4. Dual Wallet Design

The platform uses two distinct wallet types to balance promotional generosity with fraud prevention:

| Wallet Type | Source of Funds | Withdrawal Policy |
|-------------|-----------------|-------------------|
| Cash Wallet | Player's real money deposits | Withdrawable at any time (subject to turnover) |
| Promotional Wallet | Platform-granted bonuses | Must complete turnover requirement before converting to cash |

This design prevents bonus abuse where players would claim promotions and immediately withdraw without engaging with the platform.

---

## 5. Role-Based Reading Paths

### Product Managers
| Priority | Topic | Focus |
|----------|-------|-------|
| P0 | Player Lifecycle | Registration, KYC, retention |
| P0 | Promotion System | Bonus rules, turnover requirements |
| P1 | VIP System | Tier management, loyalty programmes |
| P1 | Agent System | Commission structures, referrals |

### Business Executives
| Priority | Topic | Focus |
|----------|-------|-------|
| P0 | Platform Overview | Market positioning, feature highlights |
| P0 | Multi-Tenant Strategy | Brand management, data isolation |
| P1 | Risk & Compliance | AML, responsible gambling, regulatory |
| P1 | Analytics & BI | Revenue reporting, player metrics |

---

## 6. Frequently Asked Questions

### Why are there two wallet types (Cash + Promotional)?
The cash wallet holds real deposits that can be withdrawn freely. The promotional wallet holds platform bonuses that require turnover completion before becoming withdrawable. This prevents immediate bonus-and-withdraw abuse.

### Why is turnover calculation so complex?
Different game types carry different risk profiles. Slots are pure chance (100% turnover), while table games and sports betting allow strategic play that could be exploited. Weighted turnover prevents players from using low-risk games to rapidly complete requirements.

### Does multi-tenant architecture affect performance?
Testing confirms the performance overhead is less than 5%. The benefits in data security, business flexibility, and regulatory compliance far outweigh this minimal cost.

---

## 7. Success Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Bettable Balance Accuracy | 100% | Reconciliation between wallet service and game provider records |
| Multi-Tenant Performance Overhead | <5% | Load testing comparison vs single-tenant baseline |
| Risk Control Automation Rate | ≥80% low-risk auto-approved | Ratio of automated decisions to total risk assessments |
| Turnover Calculation Consistency | 100% | Cross-validation between game logs and turnover aggregation |
| Player Journey Completion Rate | ≥70% registration-to-first-bet | Funnel analytics from registration through first wager |

---

## 8. Version History

| Version | Date | Changes |
|---------|------|---------|
| 2.0.0 | 2026-02-03 | v2 restructured -- redesigned navigation, focused on 5 core concepts |
| 1.0.0 | 2026-01-27 | Initial version |

---

## Related Documentation

→ **[System Overview - Technical Implementation](../../architecture/00_Overview/System_Overview.md)** - Detailed system architecture, module dependencies, data flow diagrams, and technical infrastructure specifications
