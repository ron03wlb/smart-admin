# Activity Risk Control Requirements

> **Canonical Source**: [04-03_Activity_Risk_Control.md](../../source-archive/04_Activity_Center/04-03_Activity_Risk_Control.md)
> **Audience**: Executives, Product Managers, Compliance Officers
> **Related Doc**: [Activity_Risk_System.md](../../architecture/04_Activity_Engine/Activity_Risk_System.md)
> **Last Synced**: 2026-02-08

---

## 1. Overview

Bonus abuse accounts for **63.8%** of all iGaming fraud, with fraud rates rising **64%** between 2022 and 2024. Risk control must be deeply integrated with the activity (promotion) system to protect revenue while maintaining a positive player experience.

---

## 2. Player Lifecycle Activity Strategy

### 2.1 Lifecycle Stage Framework

| Stage | Objective | Core Activity Types | Key Metrics |
|-------|-----------|---------------------|-------------|
| **Acquisition** | Convert registrations | First deposit bonus, no-deposit bonus | FTD conversion rate, CAC |
| **Activation** | First experience | Task system, onboarding tutorial | Day-1 retention, games tried |
| **Retention** | Long-term engagement | Daily check-in, VIP, consecutive login | MAU, churn rate, LTV |
| **Reactivation** | Win back lapsed players | Exclusive comeback gift, time-limited offer | Reactivation cost, re-deposit rate |

### 2.2 First Deposit Bonus Standards

**Industry parameter ranges:**

| Parameter | Low | Average | High |
|-----------|-----|---------|------|
| Match percentage | 50% | 100% | 200%+ |
| Maximum amount | $100 | $500 | $2,000+ |
| Wagering multiplier | 15x | 35x | 50x |
| Completion deadline | 7 days | 14 days | 30 days |

**Multi-deposit welcome package (recommended):**

- 1st deposit: 100% up to $500 + 50 free spins
- 2nd deposit: 50% up to $300 + 30 free spins
- 3rd deposit: 25% up to $200 + 20 free spins

### 2.3 Cashback / Rebate System

| Type | Calculation Basis | Typical Rate | Target Audience |
|------|-------------------|--------------|-----------------|
| Loss-based Cashback | Net losses | 5-25% | Casual players |
| Turnover-based Rebate | Total wager | 0.2-0.8% | High-frequency players |
| VIP Rakeback | Wager (tiered) | 10-25% | Top-tier VIP |

**Cashback formulas:**

| Type | Formula | Example |
|------|---------|---------|
| Loss-based | (Total Bets - Total Payouts) x Rate | 10% x $1,000 net loss = $100 |
| Turnover-based | Total Wager x Rate | 0.5% x $10,000 wager = $50 |

### 2.4 VIP Tier System

**Standard five-tier structure:**

| Tier | Points Threshold | Core Benefits |
|------|-----------------|---------------|
| Bronze | 0 | Base rebate, standard support |
| Silver | 1,000 | 10% rebate boost, birthday bonus |
| Gold | 5,000 | 15% rebate, fast withdrawal, exclusive rewards |
| Platinum | 20,000 | 20% rebate, VIP manager, exclusive events |
| Diamond | 50,000 | 25% rebate, luxury gifts, travel rewards |

**Advanced VIP policies:**

- Level matching: Match competitor VIP tiers for new sign-ups
- No negative carryover: Unmet wager from previous month does not affect current month
- Dedicated manager: Platinum and above receive 24/7 personal service

---

## 3. Regional Market Localization Requirements

### 3.1 Southeast Asia (Thailand, Vietnam, Indonesia, Philippines, Malaysia)

**Core strategy**: Mobile-first + Festival-driven + Gamification

**Key festival calendar:**

| Festival | Period | Activity Design |
|----------|--------|-----------------|
| Lunar New Year / Tet | Jan-Feb | Red envelope rewards, 888 lucky numbers, dragon-themed slots |
| Songkran | Apr 13-15 | "Cool down" rewards, water-themed games, refreshed jackpots |
| Eid al-Fitr | Post-Ramadan | Celebration rewards, family reunion theme |
| Mid-Autumn Festival | Aug-Sep | Mooncake theme, lantern activities |

**Payment and technical requirements:**

- Payment: GCash (Philippines), PromptPay (Thailand), e-wallets primary
- Games: Fishing games highly popular; live dealer is core expectation
- Technical: App must be under 5 MB, portrait-mode design, 75%+ revenue from mobile

### 3.2 Latin America (Brazil, Mexico, Argentina, Colombia)

**Core strategy**: Football integration + PIX payments + Low entry barriers

**Football dominance**: 81% of Brazilian bettors prefer football betting. Activities must deeply integrate local leagues (Liga MX, Copa Libertadores) and European leagues.

**Key festivals:**

| Festival | Period | Activity Design |
|----------|--------|-----------------|
| Carnival | Feb-Mar | Party theme, samba-themed slots, extended promotions |
| Day of the Dead | Nov 1-2 | Skull/marigold themed slots |
| World Cup / Copa America | Periodic | Region-wide super events, national team support |

**Brazil payment**: PIX accounts for 81-90% of iGaming transactions -- instant, free, 24/7. Credit cards are **banned** for gambling transactions.

### 3.3 Europe (UK, Germany, Spain, Italy)

**Core strategy**: Compliance-first + Responsible gambling integration

**UK January 2026 regulations (major changes):**

| Regulation | Requirement |
|------------|-------------|
| Wagering multiplier cap | Maximum 10x (down from 50x+) |
| Mixed product promotions | Banned (cannot combine betting + casino in single offer) |
| Slot stake limits | GBP 5/spin (age 25+), GBP 2/spin (age 18-24) |
| Financial vulnerability check | Triggered at GBP 500 net deposits within 30 days |

**Germany restrictions:**

| Restriction | Detail |
|-------------|--------|
| Slot maximum stake | EUR 1 per spin |
| Advertising ban | 6 AM - 9 PM (TV and internet) |
| Bonus advertising | Public bonus promotions prohibited |

**GDPR marketing requirements:**

- Explicit opt-in consent required for marketing communications
- Separate consent by product type (betting vs. casino)
- Easy unsubscribe mechanism must be provided

### 3.4 Chinese / Chinese-Speaking Markets

**Core strategy**: Lucky numbers + Red envelope mechanics + Social sharing

**Key festivals:**

| Festival | Activity Design |
|----------|-----------------|
| Chinese New Year | Red envelope rewards, 888 jackpots, dragon/phoenix theme |
| Mid-Autumn Festival | Mooncake-themed slots, reunion rewards |
| Singles' Day (11.11) | Shopping festival cross-promotions |
| National Day Golden Week | 7-day consecutive events |

**Cultural symbolism rules:**

| Element | Favorable | Prohibited |
|---------|-----------|------------|
| Numbers | 8 (prosperity), 88, 888, 9 (longevity), 6 (smooth) | 4 (death) -- never use in bonus amounts |
| Colors | Red (primary), Gold (wealth) | White/black combination (funeral association) |

**Game preferences**: Baccarat is dominant (95% of Macau tables), Sic Bo, Mahjong, Dragon Tiger.

---

## 4. Risk Control and Anti-Fraud Policies

### 4.1 Common Bonus Abuse Methods

| Abuse Type | Method | Detection Approach |
|------------|--------|-------------------|
| Multi-account | Multiple identities to claim welcome bonus repeatedly | Device fingerprint, IP correlation, behavioral analysis |
| Bonus hunter | Systematically targets low-wagering platforms | Betting pattern analysis, rapid withdrawal monitoring |
| Arbitrage betting | Cross-platform hedging of all possible outcomes | Abnormal odds betting, multi-platform data sharing |
| Chip dumping | Deliberately losing to accomplice accounts in P2P games | Co-table frequency analysis, win/loss pattern tracking |

### 4.2 Multi-Layer Risk Assessment Framework

| Layer | Assessment Target | Methods |
|-------|-------------------|---------|
| Device layer | Device authenticity | Device fingerprint, emulator detection, GPS spoofing detection |
| Identity layer | Player identity | KYC verification status, document authenticity, biometrics |
| Behavior layer | Player patterns | Betting patterns, deposit/withdrawal behavior, game preferences |
| Network layer | Player associations | Known fraudster linkage, shared attribute detection |
| Composite | Final decision | Real-time risk scoring combining all layers |

### 4.3 KYC Tiered Verification Strategy

| Stage | Trigger | Verification Content |
|-------|---------|---------------------|
| Light KYC | Registration | Email/phone verification, basic identity |
| Enhanced KYC | First deposit | Document verification, liveness detection |
| Full KYC | First withdrawal | Source of funds, biometric re-verification |
| Ongoing monitoring | Full lifecycle | Behavioral anomaly detection |

### 4.4 Responsible Gambling Integration

The activity system must respect all player-set protection mechanisms:

| Protection | Requirement |
|------------|-------------|
| Deposit limits | Verify bonus activation will not exceed player's set limits |
| Self-exclusion | Players on exclusion list must be blocked from all promotions |
| Cooling-off period | All marketing communications paused during cooling-off |
| Problem gambling identification | Automatically stop promotion delivery when risk indicators triggered |

---

## 5. Bonus Abuse Detection Policies

### 5.1 Matched Betting Detection Indicators

| Indicator | Description | Risk Weight |
|-----------|-------------|-------------|
| Pre-match timing | Bet placed within 15 minutes of match start | 20 points |
| High odds / low risk | Selecting high-odds but statistically safe outcomes | 30 points |
| Near max bet | Bet amount approaches maximum allowed | 15 points |
| Rapid turnover | 35x wagering completed in under 12 hours | 25 points |
| No recreational betting | Absence of casual / exploratory bets | 20 points |

**Threshold**: Risk score >= 70 triggers investigation.

### 5.2 Bonus Hunter Detection Criteria

| Pattern Category | Indicators | Weight |
|-----------------|------------|--------|
| Deposit pattern | Minimum deposit only, instant bonus activation, no follow-up deposits | 20% |
| Betting pattern | Highest RTP games only, uniform bet amounts, fewer than 3 games played | 30% |
| Withdrawal pattern | Immediate withdrawal after wager completion, account goes dormant | 30% |
| Account activity | Registration coincides with promotion launch, no return deposits | 20% |

**Risk Score Formula**: Score = (Deposit x 0.2) + (Betting x 0.3) + (Withdrawal x 0.3) + (Activity x 0.2)

### 5.3 Instant Blocking Rules (BLOCK)

| Rule | Condition | Action | Code |
|------|-----------|--------|------|
| Multi-account strong link | Same device + same bank account | Block bonus activation | `BLOCK_MULTI_ACCOUNT` |
| Known bonus hunter | Risk score >= 90 | Block bonus activation | `BLOCK_BONUS_HUNTER` |
| Blacklisted bank | Bank account on blacklist | Block deposit and bonus | `BLOCK_BLACKLISTED_BANK` |

### 5.4 Delayed Detection Rules (FLAG)

| Rule | Condition | Action | Code |
|------|-----------|--------|------|
| Rapid turnover completion | Completion time < 50% of expected | Manual review on withdrawal | `FLAG_RAPID_TURNOVER` |
| Matched betting suspicion | 3+ suspicious indicators | Flag for review | `FLAG_MATCHED_BETTING` |
| Bonus hunter suspicion | Risk score 60-89 | Restrict future bonuses | `FLAG_BONUS_HUNTER_SUSPECT` |

### 5.5 Bonus Forfeiture Rules

**Automatic forfeiture (no review required):**

- Confirmed multi-account violation
- Conclusive matched betting evidence
- Violation of activity terms (game restrictions, etc.)

**Forfeiture after review:**

- Confirmed bonus hunter behavior
- AML investigation related
- Abnormal betting patterns

**Fund handling on forfeiture:**

| Fund Type | Action |
|-----------|--------|
| Bonus balance | Full forfeiture |
| Cash balance | Retained (unless AML-related) |
| Pending bets (bonus-funded) | Cancel and forfeit |
| Pending bets (cash-funded) | Settle normally |

**Player notification requirements:**

- Send forfeiture notification email
- State specific violation reason
- Provide appeal channel
- Retain full audit record

---

## 6. Monitoring Metrics

| Metric | Formula | Alert Threshold | Purpose |
|--------|---------|-----------------|---------|
| `bonus_abuse_rate` | Forfeitures / Claims | >5% | Overall abuse level |
| `matched_betting_detected` | Detected cases / day | >10 | Matched betting prevalence |
| `bonus_hunter_score_avg` | Average risk score | >50 | Bonus hunter risk trend |
| `turnover_completion_time_avg` | Average completion time | <24h | Abnormally fast completion |
| `bonus_roi` | Incremental NGR / Bonus cost | <1.0 | Bonus investment return |

---

## 7. Data-Driven Activity Optimization

### 7.1 Core KPI Framework

**Acquisition metrics:**

| Metric | Formula | Purpose |
|--------|---------|---------|
| Player Acquisition Rate (PAR) | New players / Unique visitors x 100 | Conversion efficiency |
| First Deposit Conversion (FTD) | First depositors / Registrations x 100 | Activation efficiency |
| Customer Acquisition Cost (CAC) | Total marketing spend / New customers | Cost effectiveness |

**Revenue metrics:**

| Metric | Formula | Notes |
|--------|---------|-------|
| Gross Gaming Revenue (GGR) | Total bets - Total payouts | Top-line revenue |
| Net Gaming Revenue (NGR) | GGR - Bonuses - Taxes | True profit |
| Player Lifetime Value (LTV) | Predicted total revenue | Top 1% of players = 40% of GGR |

**Activity-specific metrics:**

| Metric | Description |
|--------|-------------|
| Bonus clearance rate | Percentage of bonuses with wagering completed |
| Bonus ratio | Bonus spend / Total deposits |
| Bonus player share | Active players using bonuses / Total active players |
| Bonus ROI | (Incremental NGR - Bonus cost) / Bonus cost |

### 7.2 A/B Testing Framework

**Testable elements:**

- Bonus amount and structure
- Wagering multiplier
- Promotional copy and CTA
- Landing page design
- Email subject line and send time
- Bonus unlock mechanism

**Testing best practices:**

1. Define clear, measurable objectives
2. Test one variable at a time
3. Ensure sufficient sample size for statistical significance
4. Run long enough to cover cyclical variations
5. Track both short-term (conversion) and long-term (LTV) metrics

---

## 8. Dynamic Configuration and Approval Policies

### 8.1 Dynamic Configuration Requirements

- **No hardcoding**: Rules such as "Deposit > 100" or "Bonus = 50%" must never be hardcoded. All variables must come from back-office configuration.
- **Configurable scope:**
  - Trigger conditions: Deposit amount, wagering multiplier, eligible games, valid period
  - Reward parameters: Bonus percentage, maximum cap, target wallet type
  - Audience targeting: Applicable countries, VIP tiers, exclusion lists

### 8.2 Approval Workflow Requirements

**Activity publication approval:**

| Role | Responsibility |
|------|----------------|
| Maker (Operations) | Create activity draft, configure all parameters |
| Checker (Ops Manager / Finance) | Review activity cost and terms |
| Action | Approved -> Activity status transitions to Ready/Active |

**Sensitive change approval:**

| Change Type | Risk Level | Required Approver |
|-------------|------------|-------------------|
| Increase budget | High | CFO or above |
| Lower wagering requirement | High | CFO or above |
| Extend promotion period | Medium | Operations Manager |
| Update eligible games | Low | Checker (standard) |

---

## 9. Implementation Priority

### Phase 1: Core Foundation (Months 1-3)

1. Rule engine framework and basic activity templates
2. Multi-tenant activity isolation
3. Unified turnover tracking service
4. Basic KYC and risk control integration

### Phase 2: Feature Expansion (Months 3-6)

1. Event-driven real-time triggers
2. VIP tier system
3. Automated cashback / rebate
4. Leaderboard and tournament functionality

### Phase 3: Intelligent Optimization (Months 6-12)

1. A/B testing platform
2. AI-driven player segmentation
3. Personalized activity recommendations
4. Predictive analytics and LTV modeling

### Key Success Factors

- Start with templates for common activity types
- Event-first design: Record all player actions as events from day one
- Schema validation for all configurations
- Idempotent operations as a distributed processing safeguard
- Comprehensive audit with immutable logs for compliance
- Regional compliance: Responsible gambling in European markets is mandatory, not optional

---

## 10. Success Metrics

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| Bonus Abuse Rate | <5% | Forfeitures / Total Claims × 100% |
| Bonus ROI | ≥1.0 | Incremental NGR / Bonus Cost |
| Matched Betting Detection Rate | ≥90% | Detected cases / Actual matched betting cases |
| First Deposit Conversion (FTD) | ≥25% | First depositors / Registrations × 100% |
| VIP Tier Upgrade Accuracy | 100% | Upgrades occurring immediately when criteria met |
| Multi-Account Detection Rate | ≥95% | Blocked multi-account attempts / Total attempts |
| Responsible Gambling Compliance | 100% | All self-exclusion players blocked from promotions |

---

## Related Documentation

→ **[Activity Risk System - Technical Implementation](../../architecture/04_Activity_Engine/Activity_Risk_System.md)** - Activity rule engine, risk scoring algorithms, bonus abuse detection patterns, configuration-driven architecture, and A/B testing framework
