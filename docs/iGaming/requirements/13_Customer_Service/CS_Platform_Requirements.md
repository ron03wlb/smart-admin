# Customer Service Platform Requirements (客服平台業務需求)

> **Canonical Source**: [13-01_CS_Platform_Design.md](../../source-archive/13_Customer_Service/13-01_CS_Platform_Design.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Customer Service Managers
> **Related Doc**: [CS_Platform_Architecture.md](../../architecture/13_Customer_Service/CS_Platform_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This customer service platform delivers critical value by:
- **Player Retention & Satisfaction**: Player 360-degree view with unified financial/betting/risk data enables personalized service, driving CSAT > 90% and reducing churn by 40% through faster issue resolution (AFR < 5 minutes)
- **Operational Cost Reduction**: AI chatbot with intent classification (90% accuracy, 40% auto-resolution rate) reduces CS headcount costs by 50% while handling 3x more ticket volume (145+ FAQ articles covering 80% of common queries)
- **VIP Revenue Protection**: Tiered SLA management (Diamond: 5 min / 2 hrs, Platinum: 15 min / 4 hrs) with automated escalation prevents VIP churn, protecting 70-80% of platform revenue from top 5% players
- **Quality Assurance**: Knowledge base with version control, approval workflow, and multi-language support (6 languages) ensures consistent CS responses, reducing ticket reopen rate < 5% and improving First Contact Resolution (FCR) > 80%
- **Risk Mitigation**: Real-time risk tags (BONUS_HUNTER, ARBITRAGE, MULTI_ACCOUNT) and AML status integration enable CS agents to identify fraud early, preventing $200K-$500K monthly fraud losses

---

## 1. Overview

The Customer Service Platform is a core operational support system for iGaming platforms, providing omnichannel ticket management, AI-assisted responses, Player 360-degree view, and SLA automation.

### 1.1 Core Objectives

| Objective | Target | Description |
|-----------|--------|-------------|
| Reduce response time | AFR < 5 minutes | Average First Response time |
| Improve resolution rate | FCR > 80% | First Contact Resolution |
| Enhance satisfaction | CSAT > 90% | Customer Satisfaction Score |
| Optimize cost | Automation > 40% | FAQ and simple query automation |

---

## 2. Player 360-Degree View

### 2.1 Data Categories

The CS agent needs a unified view integrating multiple data sources to quickly locate issues and provide personalized service.

#### A. Identity & Basic Information
- Player ID, username, registration time, KYC status
- Country, language preference, timezone
- Contact methods (Email, phone, Telegram ID)
- Device fingerprint, last login time

#### B. Financial Data
- Wallet balance (cash balance, bonus balance, locked amount)
- Deposit records (total deposits, last deposit time, primary payment method)
- Withdrawal records (total withdrawals, pending withdrawals, withdrawal methods)
- Transaction anomaly markers (chargebacks, refunds, duplicate transactions)

#### C. Betting & Gaming Behavior
- Total bets, total wins/losses, game preferences (slots, live casino, sports)
- Recent 10 bet records
- Active game providers (Pragmatic Play, Evolution, etc.)
- Abnormal betting pattern markers (hedging, arbitrage)

#### D. Player Segmentation & Tags
- **Lifecycle stage**: New / Active / Dormant / Lost
- **RFM segmentation**: Champions / At Risk / Lost
- **Value tags**: VIP_WHALE / HIGH_ROLLER / REGULAR
- **Risk tags**: BONUS_HUNTER / ARBITRAGE / MULTI_ACCOUNT

#### E. CS-Specific Tags
- `VIP_CONCIERGE` - Requires VIP manager takeover
- `COMPLAINT_ESCALATED` - Complaint escalated to management
- `SELF_EXCLUSION_REQUESTED` - Player requested self-exclusion
- `PAYMENT_ISSUE_HISTORY` - Historical payment issue record

#### F. Ticket History
- Recent 20 tickets (title, status, assigned agent, resolution time)
- High-frequency issue types (deposit failure, game lag, bonus inquiry)
- Complaint records (severity, compensation, resolution)
- NPS score (player satisfaction rating)

#### G. Risk & Compliance Information
- Real-time risk score (0-100)
- AML status: Pending / Passed / Suspicious transaction flag
- Multi-account associations (device fingerprint, IP, payment method)
- Restrictions: withdrawal limits, bonus disabled, betting restrictions

### 2.2 Action Panel

High-frequency operations CS agents can perform (requires permission control):

| Action | Description | Permission Required |
|--------|-------------|-------------------|
| **Manual Credit** | Add funds to player account (requires reason) | Senior+ |
| **Kickout** | Force player logout (for anomalous accounts) | Agent+ |
| **Reset Password** | Send password reset email | Agent+ |
| **Unlock** | Remove login failure lock | Agent+ |

---

## 3. Ticket System

### 3.1 Ticket Types

| Category | Sub-Type | Priority | SLA (First Response) | Avg. Handle Time |
|----------|----------|----------|---------------------|------------------|
| **Financial** | Deposit failure | Urgent | 5 minutes | 15 minutes |
| | Withdrawal delay | Urgent | 5 minutes | 30 minutes |
| | Balance error | High | 15 minutes | 1 hour |
| **Gaming** | Game lag | Medium | 30 minutes | 2 hours |
| | Bet dispute | High | 15 minutes | 1 hour |
| **Account** | Login issue | High | 15 minutes | 30 minutes |
| | Password reset | Medium | 30 minutes | 5 minutes |
| **Bonus** | Bonus not credited | Medium | 30 minutes | 1 hour |
| | Wagering calculation query | Low | 2 hours | 30 minutes |
| **Complaint** | Service complaint | Urgent | 5 minutes | 4 hours |
| | Fraud accusation | Urgent | 5 minutes | 24 hours |

### 3.2 Ticket Lifecycle

```
New -> Assigned -> In Progress
                     |
              Pending Customer (waiting for player reply)
                     |
              Pending Internal (waiting for internal processing)
                     |
              Resolved -> Closed
                     |
              Reopened
```

### 3.3 Auto-Close Rules

- Status = `Pending Customer` and 72 hours no reply -> Auto-close
- Status = `Resolved` and 24 hours no player objection -> Auto-close

### 3.4 Game Error Handling SOP

1. CS agent calls game provider transaction status check with Round ID
2. If provider returns `Status: Settled, Win: 0` -> Reply to player "No win"
3. If provider returns `Record Not Found` -> Escalate ticket to technical team for "dropped transaction" investigation

---

## 4. Knowledge Base

### 4.1 Content Categories

#### A. FAQ (145+ articles)
- **Deposits & Withdrawals** (50+ articles)
- **Bonuses & Promotions** (40+ articles)
- **Game Issues** (30+ articles)
- **Account Security** (25+ articles)

#### B. Game Guides
- Slot rules (100+ games)
- Live casino rules (Baccarat, Roulette, Sic Bo)
- Sports betting guides (handicap, over/under, parlay)

#### C. Compliance Policies
- Privacy policy (GDPR)
- Responsible gambling (self-exclusion, deposit limits, cooling-off)
- AML (KYC requirements, source of funds)
- Terms & conditions

#### D. Internal Operations Manual (CS-only)
- Financial issue handling procedures
- Game dispute processing
- Escalation & compensation standards
- Fraud detection procedures

### 4.2 Version Control
- Each article retains full version history
- Show last update time and updater
- Support version comparison (diff view)
- Approval workflow: Create -> Peer Review -> Team Lead Approval -> Publish

### 4.3 Multi-Language Support

| Language | Status |
|----------|--------|
| English | Primary version |
| Traditional Chinese | Translation |
| Simplified Chinese | Translation |
| Thai | Translation |
| Vietnamese | Translation |
| Indonesian | Translation |

### 4.4 Permission Control

| Role | Visible Scope | Edit | Approval |
|------|--------------|------|----------|
| **Player** | FAQ + Game Guides + Compliance | No | No |
| **CS Agent** | All (including internal manual) | Draft only | No |
| **Team Lead** | All | Draft + Edit | Approve |
| **Manager** | All | Full | Final approve |

---

## 5. AI Chatbot

### 5.1 Intent Classification

| Intent | Example | Confidence Threshold | Handling |
|--------|---------|---------------------|----------|
| `DEPOSIT_ISSUE` | "My deposit didn't arrive" | > 0.8 | Auto-reply + create ticket |
| `WITHDRAWAL_QUERY` | "How long for withdrawal?" | > 0.9 | Return FAQ article |
| `BONUS_INQUIRY` | "Where's my bonus?" | > 0.85 | Check Bonus Wallet + reply |
| `GAME_MALFUNCTION` | "Game is stuck" | > 0.75 | Create ticket + transfer to human |
| `PASSWORD_RESET` | "Forgot my password" | > 0.95 | Send reset link |
| `KYC_VERIFICATION` | "How to complete KYC?" | > 0.9 | Return KYC guide |
| `COMPLAINT` | "I want to complain" | > 0.7 | Immediately transfer to human |

### 5.2 Auto-Transfer to Human Conditions

- Intent confidence < 0.7
- Player explicitly requests human agent
- Sensitive issues (complaints, fraud accusations, account anomalies)
- 3 consecutive auto-replies failed to resolve
- VIP Diamond player (auto-transfer to VIP manager)

### 5.3 Quality Metrics

| Metric | Target |
|--------|--------|
| Intent classification accuracy | > 90% |
| Auto-reply resolution rate | > 40% |
| Player satisfaction (AI replies) | > 75% |

---

## 6. SLA Management

### 6.1 SLA Levels by VIP Tier

| VIP Level | First Response SLA | Resolution SLA | Escalation Condition |
|-----------|-------------------|----------------|---------------------|
| Diamond | 5 minutes | 2 hours | Exceeds SLA 50% -> Manager |
| Platinum | 15 minutes | 4 hours | Exceeds SLA 50% -> Senior |
| Gold | 30 minutes | 8 hours | Exceeds SLA 75% -> Senior |
| Silver/Bronze | 2 hours | 24 hours | Exceeds SLA 100% -> Team Lead |

### 6.2 Escalation Rules

| Condition | Escalation Action |
|-----------|------------------|
| Exceeds SLA 50%, no processing record | Escalate to Team Lead |
| Exceeds SLA 100%, no solution | Escalate to Manager |
| Player complaint + Diamond VIP | Immediately escalate to VIP Manager |
| Ticket reopened 3+ times | Escalate to Senior agent |

### 6.3 Alert Channels

- SLA 50% exceeded: Slack notification to agent + Team Lead
- SLA 75% exceeded: Email + SMS to Manager
- SLA achievement rate < 90% (2 consecutive hours): Emergency alert

---

## 7. Effectiveness Metrics (KPIs)

| Metric | Definition | Target | Formula |
|--------|-----------|--------|---------|
| **FCR** | First Contact Resolution | > 80% | First-resolve tickets / Total tickets |
| **AHT** | Average Handling Time | < 15 min | Total handling time / Ticket count |
| **CSAT** | Customer Satisfaction | > 90% | Satisfied tickets / Rated tickets |
| **SLA Achievement** | SLA compliance rate | > 95% | Within-SLA tickets / Total tickets |
| **Reopen Rate** | Ticket reopen rate | < 5% | Reopened tickets / Closed tickets |

---

## 8. Quality Assurance

### 8.1 Random Inspection
- Daily random check 10% of tickets
- Scoring dimensions: Professionalism, Empathy, Efficiency

### 8.2 Performance Ranking
- Weekly performance ranking displayed on CS system homepage
- Monthly Top 3 receive bonuses ($200 / $150 / $100)
- 3 consecutive months in Top 10 qualifies for Senior promotion

---

## Related Documents

- [CS_Operations_Requirements.md](CS_Operations_Requirements.md) - CS operations requirements
- [CS_Platform_Architecture.md](../../architecture/13_Customer_Service/CS_Platform_Architecture.md) - Technical architecture

---

**Return**: [Customer Service Module](../../source-archive/13_Customer_Service/README.md) | [iGaming Home](../../source-archive/README.md)
