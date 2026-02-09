# Fraud Detection Requirements

## Document Information

| Property | Value |
|----------|-------|
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-08 |
| **Canonical Source** | [05-02 Fraud Detection](../../source/05_Risk_Control/05-02_Fraud_Detection.md) |
| **View Type** | Business Requirements |
| **Target Audience** | Product Managers, Compliance Officers, Risk Analysts |
| **Related Doc** | [Fraud_Detection_System.md](../../architecture/05_Risk_Engine/Fraud_Detection_System.md) |

---

## Executive Summary

SmartAdmin iGaming v2.1.0 introduces a "Configuration-Driven Risk Control System" that allows operators to configure the handling method for each risk control rule (real-time blocking BLOCK vs. deferred checking FLAG), enabling flexible risk control strategy management.

### Core Features

| Feature | Description | Business Value |
|---------|-------------|----------------|
| **Configuration-Driven** | action_type determined by database configuration (BLOCK/FLAG/IGNORE) | Adjust risk strategies without code changes |
| **Human-First** | Generate risk proposals for anomalies, manual review processing | Simplify decision logic, avoid over-automation |
| **Equal Treatment** | All players go through risk control (no VIP exemptions) | Compliance requirement, fairness principle |
| **Operator Choice** | Operators decide their own risk control intensity | Improve customer satisfaction, support multi-market strategies |

---

## 1. Fraud Types and Prevalence

### 1.1 Primary Fraud Categories

| Fraud Type | Risk Level | Regulatory Requirement |
|------------|------------|------------------------|
| **Bonus Abuse** | HIGH | UKGC: Each player limited to one account |
| **Self-Exclusion Evasion** | CRITICAL | UKGC LCCP 17.1.1: Must block excluded players from creating new accounts |
| **Collusion** | HIGH | Poker/P2P games prohibit multi-accounts |
| **Money Laundering Structuring** | CRITICAL | AML regulations: Prohibit transaction splitting |
| **Bot/Automation** | HIGH | Industry standard: Prohibit automated betting |
| **Arbitrage** | MEDIUM | Same-match hedging, cross-IP arbitrage |
| **Turnover Manipulation** | MEDIUM | Low-odds washing behavior |

### 1.2 Detection Scenarios by Layer

**Layer 1: Synchronous Blocking (Very Few, <10ms)**

These scenarios result in immediate rejection at the betting request stage using high-speed cache lookups:

→ **[Fast Cache Implementation](../../architecture/05_Risk_Engine/Fraud_Detection_System.md#layer1-cache)**

- **Blacklisted Players** (confirmed fraudsters)
- **IP Blocking** (known attack sources)
- **Account Frozen** (under manual review)
- **Regulatory Self-Exclusion Lists** (UKGC/MGA requirement)

**Layer 3: Asynchronous BLOCK Rules (Majority, ~5 seconds)**

These rules execute asynchronously after bet success, generating high-priority Risk Proposals:

- **Bot Detection** - Behavioral pattern analysis
- **Same Match Hedging** - Requires historical bet queries
- **Same IP Arbitrage** - Requires correlation analysis
- **Abnormal Odds Detection** - Requires statistical analysis
- **Turnover Manipulation** - Requires turnover calculation

**Layer 3: Asynchronous FLAG Rules (Medium Priority, ~5 seconds)**

These rules execute asynchronously after bet success, generating medium-priority Risk Proposals:

- **Cross Match Hedging** - Lower risk
- **Low Odds Washing** (<1.5 odds) - Requires manual judgment
- **Abnormal Betting Pattern** - Potential false positives
- **High Frequency Betting** (>10 bets/min) - Requires trend observation

---

## 2. Detection Rules and Thresholds

### 2.1 Risk Rule Configuration

| Rule Code | Rule Name | Action Type | Game Types | Description |
|-----------|-----------|-------------|------------|-------------|
| BLACKLIST_PLAYER | Blacklisted Player | BLOCK | All | Player on confirmed fraud blacklist |
| BOT_DETECTION | Bot Detection | BLOCK | All | Automated/bot behavior detected |
| IP_BLOCKED | IP Blocked | BLOCK | All | IP on known attack source list |
| SAME_MATCH_HEDGE | Same Match Hedging | BLOCK | SPORTS | Opposite bets on same match |
| CROSS_MATCH_HEDGE | Cross Match Hedging | FLAG | SPORTS | Hedging across different matches |
| LOW_ODDS_WAGERING | Low Odds Washing | FLAG | SPORTS, LIVE | Odds <1.5, potential turnover manipulation |
| ABNORMAL_PATTERN | Abnormal Pattern | FLAG | All | Unusual betting patterns |
| HIGH_FREQUENCY | High Frequency Betting | FLAG | All | >10 bets per minute |

### 2.2 Priority Thresholds

| Priority | Trigger Condition | Handling Method |
|----------|-------------------|-----------------|
| **URGENT** | Amount > $10,000 OR Blacklist/IP Blocked | Immediate block |
| **HIGH** | Amount > $5,000 OR Bot detection | Block + Manual review |
| **MEDIUM** | Amount > $1,000 OR Abnormal betting | Manual review |
| **LOW** | Amount ≤ $1,000 | Normal monitoring |

### 2.3 Risk Scoring Factors

| Factor | Weight | Cap | Description |
|--------|--------|-----|-------------|
| Risk Flag Count | +5/occurrence | 30 points | Historical risk flag count |
| Block Count | +10/occurrence | 40 points | Actual block count |
| Abnormal Win Rate | +20 | 20 points | Win rate >60% or <30% |
| Recent Proposals | +3/proposal | 20 points | Risk proposals in last 30 days |

### 2.4 Risk Level Mapping

| Score Range | Risk Level | Handling Recommendation |
|-------------|------------|------------------------|
| 0-49 | LOW | Normal monitoring |
| 50-79 | MEDIUM | Enhanced monitoring |
| 80-100 | HIGH | Manual review |
| Blacklist | BLACKLIST | Reject betting |

---

## 3. Priority Levels and SLAs

### 3.1 Review SLAs

| Priority | Response Time | Resolution Time | Escalation |
|----------|---------------|-----------------|------------|
| URGENT | <15 minutes | <1 hour | Immediate manager escalation |
| HIGH | <1 hour | <4 hours | Escalate after 2 hours |
| MEDIUM | <4 hours | <24 hours | Escalate after 8 hours |
| LOW | <24 hours | <72 hours | Normal queue |

### 3.2 Monitoring Thresholds

| Metric | Alert Threshold | Description |
|--------|-----------------|-------------|
| `risk_proposal_pending_count` | >100 | Pending review backlog |
| `risk_query_duration_ms (P95)` | >200ms | Query performance degradation |
| `risk_cache_hit_rate` | <90% | Cache hit rate decline |
| `risk_event_publish_errors` | >10/min | Risk event delivery failures (exceeding threshold indicates system degradation) |

→ **[Event Publishing Architecture](../../architecture/05_Risk_Engine/Fraud_Detection_System.md#event-publishing)**
| `multi_account_detection_rate` | >5% | Abnormally high multi-account detection |
| `self_exclusion_bypass_attempts` | >10/day | High bypass attempt rate |

---

## 4. Compliance Requirements

### 4.1 VIP Player Risk Control Notice

> **Compliance Requirement**: Per UKGC LCCP and Entain GBP 17M penalty case lessons, **VIP players must go through exactly the same risk control rules as regular players**.
>
> VIP Level's only impact is:
> - VIP proposal priority in manual review queue
> - Dedicated customer service assists with KYC/SOF document preparation
> - **Does NOT affect** risk rule trigger conditions
> - **Does NOT affect** AML thresholds
> - **Does NOT affect** affordability assessment triggers

### 4.2 Self-Exclusion Requirements (UKGC LCCP 17.1.1)

Per UKGC LCCP 17.1.1:

> "Licensees must have effective procedures to prevent any individual who has made a self-exclusion request from gambling."

**Registration Check Requirements**:
- Name fuzzy matching (Levenshtein distance <=2)
- Date of birth matching
- Address fuzzy matching
- Phone number matching (including variants)
- Email domain matching
- Gamstop API query (UK market)

**Login Check Requirements**:
- Device fingerprint matching
- IP address matching (recent usage)
- Behavioral pattern matching

**Detection Result Handling**:

| Match Type | Action |
|------------|--------|
| Potential Match (Similarity >=70%) | Suspend account, generate high-priority review proposal, 24-hour manual review |
| Confirmed Match | Immediately freeze account, cancel all pending bets, refund cash balance, forfeit bonus balance (per T&C), notify player, record violation event |

### 4.3 Multi-Account Detection Requirements

| Link Type | Risk Level | Handling |
|-----------|------------|----------|
| Same IP + Same Device | CRITICAL | Immediate freeze, manual review |
| Same IP + Different Device | MEDIUM | Household verification process |
| Different IP + Same Device | HIGH | Account merge investigation |
| Same Phone Number | CRITICAL | Not allowed (blocked at registration) |

### 4.4 Household Account Verification

**Legitimate Household Scenarios**:
- Spouses/partners each have accounts
- Adult children living with parents
- Roommates sharing network

**Verification Process**:
1. System detects same-IP multi-accounts -> Trigger verification
2. Send verification email to all account holders
3. Each account independently completes KYC verification
4. Manual review KYC documents (confirm different identities)
5. Mark as "Verified Household Accounts"

**Household Account Restrictions**:
- Prohibited from participating in same event/tournament
- No inter-account transfers/gifts
- Bonus activities independent (no sharing)
- P2P games automatically avoid matchups

---

## 5. Case Studies and Penalties

### 5.1 Entain GBP 17M Penalty (Reference: 05-03 KYC/AML Section 11)

**Violation**: VIP players received preferential treatment in risk control, bypassing standard AML checks.

**Lessons Learned**:
- All players must receive equal risk control treatment
- VIP status cannot exempt from compliance requirements
- Inadequate source of funds documentation for high-value customers

### 5.2 888 Holdings Penalty

**Violation**: Failed to implement effective self-exclusion procedures, allowing excluded players to continue gambling.

**Lessons Learned**:
- Multi-channel exclusion must be synchronized
- Device fingerprinting essential for exclusion enforcement
- Regular Gamstop integration audits required

### 5.3 Betfred GBP 3.25M Penalty

**Violation**: Inadequate AML controls and customer interaction procedures.

**Lessons Learned**:
- Automated risk scoring must trigger manual review
- Documentation of risk decisions required
- Clear escalation procedures must be implemented

---

## 6. Business Value Metrics

### 6.1 Key Performance Indicators

| KPI | Target | Description |
|-----|--------|-------------|
| Detection Rate | >=95% | Multi-account behavior detection rate |
| False Positive Rate | <=1% | Incorrect fraud detection rate |
| Detection Latency - Registration | <100ms | Real-time registration blocking |
| Detection Latency - Login | <50ms | Real-time login checks |
| Review SLA Compliance | >=95% | Meeting review time SLAs |
| Bonus Abuse Prevention | >=90% | Prevented bonus abuse rate |

### 6.2 Reporting Requirements

**Daily Reports**:
- New multi-account cases
- Account freeze/merge count
- Household account verifications
- Self-exclusion bypass attempts
- Gamstop query statistics

**Weekly Reports**:
- Detection rate trends
- False positive analysis
- Device fingerprint stability
- Correlation graph complexity analysis

**Monthly Reports**:
- Regulatory compliance report
- Multi-account loss estimation
- System effectiveness evaluation

---

## 7. Comparison with Traditional Risk Control

| Dimension | Traditional (v2.0.0) | Configuration-Driven (v2.1.0) |
|-----------|---------------------|-------------------------------|
| **Rule Classification** | Hardcoded P0/P1/P2 (30%/50%/20%) | Configuration-driven (operator chooses BLOCK/FLAG) |
| **VIP Exemption** | VIP Level >=3 exempt (violation) | All players equal (no exemptions) |
| **Anomaly Handling** | Complex automated threshold logic | Human review primary |
| **Strategy Adjustment** | Requires code change + deployment | Update configuration table only |
| **Flexibility** | Fixed percentages, non-adjustable | Each rule independently configurable |
| **False Positive Risk** | 5-10% normal players rejected | Zero false positives (bet already succeeded) |
| **System Availability** | Single point failure | High availability (risk failure doesn't affect betting) |

---

## 8. Industry References

- **DraftKings/FanDuel (US Market)**: Only blacklist blocks synchronously, all others asynchronous
- **Bet365 (UK Market)**: Hedge detection analyzed within 5 minutes after bet success
- **UKGC Compliance Architecture**: Recommends post-hoc risk control + withdrawal-time interception

---

## Related Documents

- [05-01 Risk Framework](../../source/05_Risk_Control/05-01_Risk_Framework.md) - Risk system overall architecture
- [01-05 Withdrawal Risk](../../source/01_Player_Center/01-05_Withdrawal_Risk.md) - Withdrawal SAGA process
- [05-03 KYC/AML](../../source/05_Risk_Control/05-03_KYC_AML.md) - Identity verification and AML
- [15-01 Self Exclusion](../../source/15_Responsible_Gambling/15-01_Self_Exclusion.md) - Self-exclusion system
- [06-08 UKGC Compliance](../../source/06_Platform_Governance/06-08_UKGC_Compliance.md) - UK regulatory compliance

---

## Change Log

### v1.0.0 (2026-02-08)

**Initial Version**:
- Extracted business requirements from source documents
- Fraud types and prevalence statistics
- Detection rules and thresholds
- Priority levels and SLAs
- Compliance requirements (UKGC, Gamstop)
- Case studies and penalties
- Removed all code blocks and technical diagrams
