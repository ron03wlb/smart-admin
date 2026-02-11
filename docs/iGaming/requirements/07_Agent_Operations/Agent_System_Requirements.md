# Agent System Business Requirements

> **Canonical Source**: [source-archive/07_Agent_Center/07-03_Agent_System.md](../../source-archive/07_Agent_Center/07-03_Agent_System.md)
> **View Type**: Business Requirements
> **Target Audience**: Product Managers, Compliance Officers
> **Related Doc**: [Agent System Architecture](../../architecture/07_Agent_Service/Agent_System_Architecture.md)
> **Last Synced**: 2026-02-09

---

## 1. Business Overview

The Agent (Affiliate) System is the core customer acquisition engine for the iGaming platform. It supports **unlimited hierarchy levels** and tenant-isolated agent structures. The system accommodates both **credit/position taking** and **pure commission** business models.

---

## 2. Agent Hierarchy & Relationships

### 2.1 Binding Rules

| Rule | Description |
|------|-------------|
| **Binding Method** | Via referral link or invitation code |
| **Binding Duration** | Permanent, or configurable protection period (e.g., 30 days) |
| **Protection Period** | During this period, the player cannot be reassigned to another agent |

### 2.2 Hierarchy Structure

**Master Agent (Total Agent) > Level 1 Agent > Level 2 Agent > ... > Player**

The system supports two operating models:

| Model | Description | Revenue Source |
|-------|-------------|----------------|
| **Position Taking (Credit)** | Agent shares win/loss risk with platform | Position-based profit/loss sharing |
| **Pure Commission** | Agent earns commission only, no risk exposure | Commission on player activity |

---

## 3. Commission Calculation

### 3.1 Commission Plan Types

| Plan Type | Basis | Example |
|-----------|-------|---------|
| **Revenue Share** | Net Win = Bet - Win - Bonus - Fee | Net Win < $100k (30%), $100k-$500k (40%), > $500k (50%) |
| **Turnover Rebate** | Valid Bet Amount | Baccarat 0.8%, Slots 1.0% |
| **CPA (Cost Per Acquisition)** | Per qualifying new depositing player | $100 per player with first deposit >= $50 |

### 3.2 Tiered Revenue Share Schedule

| Net Win Range | Commission Rate |
|--------------|----------------|
| < $100,000 | 30% |
| $100,000 - $500,000 | 40% |
| > $500,000 | 50% |

### 3.3 Settlement Cycles

| Cycle | Execution Time | Use Case |
|-------|---------------|----------|
| **Daily** | Every day at 02:00 | High-volume agents |
| **Weekly** | Every Monday | Standard agents |
| **Monthly** | 1st of each month | Master agents |

---

## 4. Adjustment Wallet & Late Settlement

The Adjustment Wallet resolves issues with cross-period bets and historical corrections.

### 4.1 Core Principles

- Historical settled reports are **immutable** and cannot be modified
- Adjustments are recorded as separate entries in the current period
- Both manual and system-automated adjustments are supported

### 4.2 Adjustment Scenarios

| Scenario | Description | Handling |
|----------|-------------|----------|
| **Late Arrival** | Previous period bet settled in current period | Add ADJUSTMENT entry in current period |
| **Bet Rollback** | Game provider cancels a bet | Add negative ADJUSTMENT in current period |
| **Manual Adjustment** | Finance team corrects an error | Add MANUAL ADJUSTMENT with approval |

### 4.3 Commission Calculation Formula

**Current Period Payable = Current Period Commission + Previous Period Carryover + Adjustments (Manual/System)**

---

## 5. Negative Carryover Rules

### 5.1 Definition

When the current month's Net Win is negative (players win big), the agent's commission becomes negative. This negative value carries forward to the next month.

### 5.2 Formula

**Carryover_Next = Min(0, Current_Commission + Carryover_Previous)**

### 5.3 Example

| Period | Commission | Carryover In | Net Amount | Carryover Out |
|--------|-----------|-------------|------------|---------------|
| January | +$8,000 | -$10,000 | -$2,000 (not paid) | -$2,000 |
| February | +$5,000 | -$2,000 | +$3,000 (paid) | $0 |

### 5.4 Reset Mechanisms

| Reset Type | Condition | Description |
|-----------|-----------|-------------|
| **Threshold Reset** | Negative value < -$1,000,000 (configurable) | Platform absorbs 50% to prevent agent loss |
| **Time Reset** | January 1st annually (optional) | Used to incentivize new year promotions |
| **Active Activity** | Agent inactive for 3+ consecutive months | Reset benefit suspended if no new active players |

---

## 6. Commission Approval Process

### 6.1 Approval Workflow

1. System automatically calculates commission report (status: Pending)
2. Finance team reviews commission data (excludes arbitrage and anomalies)
3. Supervisor approves
4. Commission released to agent wallet

### 6.2 Approval Roles

| Role | Responsibility |
|------|---------------|
| **System** | Auto-calculate and generate pending report |
| **Finance Staff** | Review data accuracy, flag anomalies |
| **Finance Supervisor** | Final approval |
| **System** | Transfer to agent wallet after approval |

---

## 7. Agent Portal

The agent portal is a separate login entry from the player frontend.

### 7.1 Dashboard

| Widget | Description |
|--------|-------------|
| **Today's New Members** | Count of newly registered players under this agent |
| **Today's Commission** | Estimated commission for the current day |
| **Active Players** | Number of active players (bet in last 30 days) |

### 7.2 Promotional Tools

| Tool | Description |
|------|-------------|
| **Referral Link Generator** | Create unique referral links for campaigns |
| **Banner Downloads** | Download promotional banner materials |
| **QR Code** | Auto-generated QR code for referral link |

### 7.3 Reports Available

| Report | Description |
|--------|-------------|
| **Downstream Player Report** | List of all players under this agent |
| **Win/Loss Report** | Player win/loss summary by period |
| **Commission History** | Historical commission records with status |

---

## 8. Risk Control

### 8.1 Same IP Detection

When an agent and their downstream players use the same IP address, the account is flagged as abnormal (possible self-commission fraud).

| Detection | Rule | Action |
|-----------|------|--------|
| Same IP | Agent login IP matches player login IP | Flag agent account |
| High frequency | Agent creates multiple players from same IP in short time | Lock agent account |
| Commission spike | Abnormal commission increase coinciding with same-IP activity | Trigger investigation |

### 8.2 Commission Amount Anomaly

| Indicator | Threshold | Action |
|-----------|-----------|--------|
| Single agent commission | > $100,000 per period | Flag for review |
| Negative carryover | > $100,000 negative | Requires manual audit |
| Consecutive negative months | 3+ months | Flag downstream player anomaly |

---

## 9. Business KPIs

| KPI | Target | Measurement |
|-----|--------|-------------|
| **Agent Acquisition Cost** | < $200/agent | Marketing spend / new agents |
| **Player per Agent** | > 50 active players | Average active players per agent |
| **Commission Payout Ratio** | 25-40% of GGR | Total commission / GGR |
| **On-time Settlement Rate** | > 95% | Settled on time / total settlements |
| **Agent Retention** | > 80% annual | Active agents / total agents |

---

## Success Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Agent Acquisition Cost | < $200 per agent | Total marketing spend / new agent registrations |
| Players per Agent | > 50 active players | Average active players per agent (bet in last 30 days) |
| Commission Payout Ratio | 25-40% of GGR | Total commission paid / Gross Gaming Revenue |
| On-time Settlement Rate | > 95% | Settlements completed on schedule / total settlements |
| Agent Retention | > 80% annually | Active agents (logged in within 90 days) / total registered agents |
| Same-IP Fraud Detection Rate | 100% detection | All agent-player IP matches flagged automatically |
| Commission Calculation Accuracy | 100% | Zero commission calculation errors after system approval |
| Negative Carryover Resolution | Within 6 months | Average time to clear negative carryover balance |

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-09
**Maintenance Team**: Product Team
