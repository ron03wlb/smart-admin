# Customer Service Operations Requirements (客服營運業務需求)

> **Canonical Source**: [13-02_Customer_Service_Operations.md](../../source-archive/13_Customer_Service/13-02_Customer_Service_Operations.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Customer Service Managers, Operations Directors
> **Related Architecture**: [CS_Operations_Architecture.md](../../architecture/13_Customer_Service/CS_Operations_Architecture.md)
> **Last Synced**: 2026-02-09

---

## Business Value

This customer service operations framework delivers critical value by:
- **Player Retention**: First Contact Resolution (FCR) > 80% and CSAT > 90% reduce churn by 35-45%, directly impacting player lifetime value and repeat deposits
- **Operational Efficiency**: Multi-channel integration with AI automation (40% Live Chat, 30% Telegram, 25% WhatsApp) reduces CS headcount costs by 50% while handling 3x more tickets
- **VIP Experience**: Dedicated VIP routing for Diamond/Platinum tiers with immediate Phone support increases VIP deposit frequency by 25% and retention by 60%
- **SLA Compliance**: Real-time monitoring with automated escalation (Level 1 @ 50%, Level 2 @ 75%, Level 3 @ 90%) maintains > 95% SLA achievement, preventing player frustration and regulatory complaints
- **Workforce Optimization**: Skill-based routing algorithm (Agent Level × Language Match × Load Inverse) reduces Average Handle Time (AHT) by 30% and increases agent productivity by 40%
- **Responsible Gambling**: Integrated self-exclusion handling and problem gambling detection prevent regulatory fines and protect brand reputation through immediate intervention

---

## 1. Overview

Customer Service Operations defines the operational framework for running the CS platform, including ticket routing algorithms, multi-channel integration strategies, workforce management, performance analytics, and quality assurance processes.

---

## 2. Ticket Routing & Assignment

### 2.1 Routing Strategy (Priority Order)

1. **VIP Player Priority Routing**
   - Diamond/Platinum -> Dedicated VIP manager
   - Gold -> VIP team (round-robin distribution)

2. **Skill Matching**
   - Financial tickets -> Financial specialist
   - Game technical issues -> Technical support
   - Bonus disputes -> Bonus specialist
   - Complaints -> Team Lead/Manager

3. **Language Matching**
   - Player language preference = Agent language capability

4. **Workload Balancing**
   - Assign to agent with fewest current tickets
   - Prevent any single agent from exceeding 20 tickets

### 2.2 Load Balancing Algorithm

```
Weight = (Agent Level Weight) x (Language Match Score) x (Current Ticket Load Inverse)

Agent Level Weight:
- Senior: 1.5
- Regular: 1.0
- Junior: 0.7

Language Match Score:
- Native match: 1.0
- Fluent: 0.8
- Basic: 0.5

Current Ticket Load Inverse:
- 1 / (Current ticket count + 1)
```

---

## 3. Multi-Channel Integration

### 3.1 Supported Channels

| Channel | Priority | SLA | Automation Level |
|---------|----------|-----|-----------------|
| **Live Chat** | Highest | 5 minutes | AI auto-reply 40% |
| **Telegram** | High | 15 minutes | AI auto-reply 30% |
| **Email** | Medium | 2 hours | Auto-classify 100% |
| **WhatsApp** | Medium | 15 minutes | AI auto-reply 25% |
| **Phone** | VIP only | Immediate | 0% (human) |

### 3.2 Live Chat Requirements

- Real-time two-way communication via instant messaging
- "Typing..." indicator display
- Agent can handle 3-5 concurrent conversations (based on experience level)
- Quick reply templates (50+ preset replies)
- File transfer (screenshots, payment receipts)
- Conversation transfer (to other agent or escalation)

### 3.3 Context Passing

When a player initiates a conversation, automatically pass the following to the CS agent:
- Player ID and VIP level (determines priority)
- Current page (knows where player is stuck)
- Wallet balance (quick judgment for deposit issues)
- Wagering progress (key info for withdrawal problems)

### 3.4 Unified Conversation History

- CS agent opening a player profile automatically displays all channel history
- Prevents players from repeatedly describing their issue
- Cross-channel ticket linking

### 3.5 Channel Switching (Sensitive Data)

When a player shares sensitive information (bank account) in Live Chat:
1. Bot/agent detects sensitive information
2. Auto-prompt: "We recommend providing this information via Email for security"
3. System auto-creates Email ticket and links to current conversation
4. Player replies to Email, agent continues processing in same ticket

---

## 4. SLA Automated Monitoring

### 4.1 Real-Time Dashboard

Key metrics displayed:
- Current pending tickets by priority (P0/P1/P2/P3)
- SLA achievement rate (last 24 hours)
- Over-SLA ticket count
- SLA warning count

### 4.2 Automated Alert Mechanism

| Notification Level | Trigger Condition | Notification Channel |
|-------------------|-------------------|---------------------|
| **Level 1** | SLA 50% exceeded, no processing | Slack: agent + Team Lead |
| **Level 2** | SLA 75% exceeded | Email + SMS: Manager |
| **Level 3** | SLA achievement < 90% for 2 hours | Emergency alert: all channels |

### 4.3 Timeout Detection

- Scheduled task runs every 5 minutes
- Checks all open tickets against SLA deadlines
- Auto-escalates per escalation rules

---

## 5. Agent Performance Analytics

### 5.1 Individual KPIs

| Metric | Definition | Target | Formula |
|--------|-----------|--------|---------|
| **FCR** | First Contact Resolution | > 70% | First-resolve / Total tickets |
| **AHT** | Average Handle Time | < 10 min (chat), < 15 min (ticket) | Total time / Ticket count |
| **CSAT** | Customer Satisfaction Score | Average > 4.0 (1-5 scale) | Player ratings |
| **Ticket Volume** | Daily ticket throughput | Monitor | Total handled per day |

### 5.2 Daily Report Content

- Online agents count
- Tickets processed
- Average response time
- SLA achievement rate
- Top performing agents (FCR, CSAT, volume)
- Areas for improvement

### 5.3 Performance Ranking System

- Weekly performance Top 5 displayed on CS system homepage
- Monthly Top 3 receive bonuses ($200 / $150 / $100)
- 3 consecutive months in Top 10 -> Senior promotion eligibility

---

## 6. Quality Assurance Process

### 6.1 Random Inspection Mechanism

- Daily random check of 10% of tickets
- Scoring dimensions:
  - **Professionalism**: Accurate policy citation
  - **Empathy**: Friendly and understanding tone
  - **Efficiency**: Speed of resolution

### 6.2 Continuous Improvement

- Low-scoring tickets reviewed periodically
- Case study sharing sessions organized regularly
- Training needs identified from quality scores

---

## 7. Workforce Management

### 7.1 Scheduling Rules

| Shift Type | Hours | Agent Count | Focus |
|-----------|-------|-------------|-------|
| Peak hours | 18:00-02:00 UTC+8 | Maximum staffing | Gaming peak |
| Standard hours | 10:00-18:00 UTC+8 | Standard staffing | Normal operations |
| Off-peak hours | 02:00-10:00 UTC+8 | Minimum staffing | AI handles most queries |

### 7.2 Agent Skill Matrix

| Skill Tag | Description | Training Requirement |
|-----------|------------|---------------------|
| `payment_expert` | Deposit/withdrawal issues | Payment system training |
| `game_integration` | Game technical problems | Game provider training |
| `bonus_specialist` | Bonus and wagering issues | Promotion rules training |
| `compliance_officer` | Complaint and regulatory | Compliance certification |
| `vip_manager` | VIP player handling | VIP program training |

---

## 8. Responsible Gambling Integration

Customer Service plays a critical role in responsible gambling:

### 8.1 Self-Exclusion Handling

- CS agents must be trained to handle self-exclusion requests
- Requests must be processed immediately (not delayed)
- Player must be informed of consequences before confirmation
- CS cannot encourage player to reconsider self-exclusion

### 8.2 Problem Gambling Signs

CS agents should identify and escalate when players:
- Express distress about gambling losses
- Request help with controlling gambling
- Mention financial difficulties
- Display aggressive behavior related to losses
- Contact CS excessively about bonus/wagering issues

---

## 9. Effectiveness Metrics

| Metric | Target | Description |
|--------|--------|-------------|
| SLA achievement rate | > 95% | Overall SLA compliance |
| First Contact Resolution | > 80% | Issues resolved on first contact |
| Customer Satisfaction | > 90% | CSAT score |
| Agent utilization | 70-85% | Optimal agent workload |
| Ticket reopen rate | < 5% | Quality of resolution |
| AI automation rate | > 40% | Tickets resolved by AI |
| Average handling time | < 15 min | Time to resolve |
| Knowledge base search success | > 70% | Self-service effectiveness |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| VIP Priority Routing Accuracy | 100% | Diamond/Platinum players routed to VIP manager/team |
| Skill Matching Accuracy | > 95% | Tickets routed to agents with matching skill tags |
| Language Match Rate | > 98% | Tickets assigned to agents speaking player's language |
| Load Balancing Efficiency | Max 20 tickets/agent | No agent exceeds ticket limit during peak hours |
| Multi-Channel Context Retention | 100% | CS agent sees complete cross-channel conversation history |
| SLA Alert Response Time | < 5 minutes | Time from Level 1 alert to agent acknowledgment |
| Quality Inspection Coverage | 10% daily | Random inspection quota met |
| Responsible Gambling Response | 100% immediate | Self-exclusion requests processed without delay |

---

## Related Documents

- [CS_Platform_Requirements.md](CS_Platform_Requirements.md) - CS platform requirements
- [CS_Operations_Architecture.md](../../architecture/13_Customer_Service/CS_Operations_Architecture.md) - Technical architecture

---

**Return**: [Customer Service Module](../../source-archive/13_Customer_Service/README.md) | [iGaming Home](../../source-archive/README.md)
