# Risk Proposal Workflow Requirements

> **Canonical Source**: [source/05_Risk_Control/05-05_Risk_Proposal_Workflow.md](../../source/05_Risk_Control/05-05_Risk_Proposal_Workflow.md)
> **Audience**: Executives, Compliance Officers, Risk Team Managers
> **Related Doc**: [Risk_Proposal_Implementation.md](../../architecture/05_Risk_Engine/Risk_Proposal_Implementation.md)
> **Last Synced**: 2026-02-08

---

## 1. Executive Summary

SmartAdmin iGaming v2.1.0 introduces a **human-centric** risk proposal review workflow. The design simplifies automated decision logic and routes all suspicious cases to a manual review queue, where reviewers make approve, reject, or partial approve decisions based on specific circumstances.

### 1.1 Core Design Principles

| Principle | Description | Business Value |
|-----------|-------------|----------------|
| **Human-First** | All cases with suspicious amount > 0 generate a manual review proposal | Avoids over-automation, reduces false positive risk |
| **No Complex Thresholds** | Eliminates $500/$5,000 automatic routing rules | Simplifies decision logic, easier to maintain |
| **Reviewer Autonomy** | Reviewers can manually escalate complex cases | Flexible response to various scenarios |
| **Transparent Decisions** | All review actions are fully logged | Meets compliance audit requirements |

### 1.2 Key Changes from v2.0.0

| Dimension | Legacy Workflow (v2.0.0) | v2.1.0 Simplified Workflow |
|-----------|--------------------------|---------------------------|
| **Routing Rules** | Complex threshold-based routing ($500/$5,000) | Unified routing to review queue |
| **Auto-Approval** | Auto-approve under $500 | No auto-approval; all cases reviewed manually |
| **Review Tiers** | Mandatory L1/L2 routing | Reviewer-initiated escalation |
| **SLA Management** | Fixed L1=24h, L2=48h | Priority-based SLA (see Section 2.2) |
| **Decision Complexity** | Multi-layer automated decisions | Simplified to human decisions |

---

## 2. Review Queue Management Requirements

### 2.1 Priority Classification

All risk proposals MUST be assigned a priority based on the suspicious amount:

| Priority | Amount Threshold | Description |
|----------|-----------------|-------------|
| **URGENT** | > $10,000 | Highest priority, requires immediate attention |
| **HIGH** | > $5,000 | High priority |
| **MEDIUM** | > $1,000 | Standard priority |
| **LOW** | <= $1,000 | Low priority |

**Dynamic Priority Upgrade**: When a proposal exceeds its SLA deadline, the system MUST automatically upgrade its priority (LOW -> MEDIUM -> HIGH -> URGENT).

### 2.2 SLA Requirements (v3.0.0 Priority-Based)

Each priority level has its own SLA and timeout behavior:

| Priority | SLA Deadline | Timeout Action | Applicable Scenario |
|----------|-------------|----------------|---------------------|
| **URGENT** | 1 hour | Auto-reject withdrawal | Blacklist / IP ban cases |
| **HIGH** | 2 hours | Auto-reject withdrawal | Bot detection cases |
| **MEDIUM** | 24 hours | Auto-reject withdrawal | Abnormal betting patterns |
| **LOW** | 48 hours | Auto-approve (release) | Data collection cases |

**Key Business Rules**:
- URGENT/HIGH/MEDIUM proposals that exceed SLA are automatically rejected (safety-first)
- LOW proposals that exceed SLA are automatically approved (low risk, avoid blocking legitimate players)
- Warning notifications MUST be sent when a proposal reaches 75% of its SLA deadline
- SLA monitoring MUST run every 5 minutes

### 2.3 Queue Display and Task Assignment

- The review queue MUST be sorted by priority (descending) then creation time (ascending)
- Reviewers MUST actively claim tasks (no automatic assignment)
- Approaching-SLA proposals MUST be highlighted in the queue

---

## 3. Review Decision Requirements

### 3.1 Decision Types

The system MUST support four types of review decisions:

| Decision | Description | Post-Decision Action | Status Transition |
|----------|-------------|---------------------|-------------------|
| **Approve** | Suspicious amount is legitimate, full release | Unfreeze amount, continue withdrawal | PENDING_REVIEW -> APPROVED |
| **Reject** | Confirmed violation, deny withdrawal | Keep frozen, execute deduction | PENDING_REVIEW -> REJECTED |
| **Partial Approve** | Part of the amount is legitimate | Release approved portion, freeze remainder | PENDING_REVIEW -> PARTIAL_APPROVED |
| **Escalate** | Case too complex for current reviewer | Route to senior analyst | PENDING_REVIEW -> ESCALATED |

### 3.2 Decision Business Rules

**Approve**:
- Full suspicious amount is released back to the player's wallet
- Withdrawal flow continues normally
- Review notes are optional

**Reject**:
- Review notes are MANDATORY (rejection reason must be documented)
- Frozen amount remains locked and a deduction is processed
- Player receives a rejection notification

**Partial Approve**:
- Approved amount MUST be greater than zero and less than the total suspicious amount
- Review notes are MANDATORY
- Approved portion is released; rejected portion triggers deduction
- Player receives a partial approval notification with breakdown

**Escalate**:
- Escalation reason is MANDATORY
- Priority is automatically set to URGENT
- Senior analyst team is notified
- The escalated proposal re-enters the review cycle

---

## 4. Compensation Requirements

### 4.1 Compensation Types

The system MUST support three types of financial compensation:

| Type | Scenario | Fund Direction | Example |
|------|----------|---------------|---------|
| **Refund** | False positive case, return frozen funds | System -> Player Wallet | Player complaint confirmed as no issue |
| **Adjustment** | Turnover calculation error, compensate difference | System -> Player Wallet | Mistaken turnover deduction, refund difference |
| **Deduction** | Confirmed violation, seize suspicious amount | Player Wallet -> System | Rejected proposal, frozen funds deducted |

### 4.2 Compensation Execution Rules

- Every compensation MUST generate a unique compensation record with a tracking number
- Compensation records MUST track: player ID, type, amount, reason, status, and execution timestamp
- All wallet balance updates from compensation MUST be atomic (transactional)

### 4.3 Compensation Failure Handling

- Failed compensations MUST be retried using exponential backoff (1s, 2s, 4s)
- Maximum retry count: 3 attempts
- After 3 failed retries, the status MUST be set to PERMANENTLY_FAILED
- Permanently failed compensations MUST trigger an alert for manual intervention
- Retry scheduler runs every 10 minutes

---

## 5. Reviewer Permission Requirements

### 5.1 Three-Tier Permission Structure

| Role | Permission | Responsibilities | Scope |
|------|-----------|-----------------|-------|
| **Reviewer** | `risk:proposal:review` | Process all proposals (approve/reject/partial approve/escalate) | All proposals |
| **Senior Analyst** | `risk:proposal:escalate_review` | Handle escalated cases, provide consultation | ESCALATED proposals |
| **Compliance Manager** | `risk:proposal:final_decision` | Final decision authority, policy configuration | All proposals + policy config |

### 5.2 Key Permission Rules (v2.1.0)

- Reviewers CAN process all proposals (no L1/L2 automatic routing)
- Reviewers CAN manually escalate complex cases
- NO automatic task assignment; reviewers claim tasks proactively
- All operations require appropriate Sa-Token permission annotation

### 5.3 Reviewer Workflow

The standard reviewer workflow is:

1. **Log in** -> View review queue (sorted by priority)
2. **Claim task** -> Actively select and claim a pending proposal
3. **Review details** -> Examine bet history, risk rule matches, player profile
4. **Make decision**:
   - Simple cases -> Directly approve / reject / partial approve
   - Complex cases -> Manually escalate to senior analyst
5. **Document reasoning** -> Fill in review notes (mandatory for reject/partial approve)
6. **Submit decision** -> System automatically executes compensation logic

---

## 6. Workflow Integration Requirements

### 6.1 BPMN Process Requirements

The risk proposal review MUST be managed through a formal workflow engine (Camunda BPMN) with:

- **Start Event**: Triggered when a risk proposal is created
- **Reviewer Task**: Human task assigned to the risk reviewer group
- **Decision Gateway**: Routes based on decision type (approve/reject/partial approve/escalate)
- **Compensation Tasks**: Service tasks that execute the appropriate compensation
- **Escalation Loop**: Escalated proposals return to the review cycle for senior analyst handling
- **End Event**: Marks review completion

### 6.2 Workflow Business Rules

- Escalated proposals loop back to the decision gateway after senior analyst review
- Each decision path triggers the appropriate compensation service task
- Workflow variables MUST include: proposalId, decisionType, approvedAmount (for partial approve), reviewNotes

---

## 7. Monitoring and Compliance Requirements

### 7.1 Key Performance Indicators

**Review Efficiency KPIs**:

| KPI | Description | Alert Threshold |
|-----|-------------|-----------------|
| Review response time (P95) | 95th percentile of time from creation to decision | > 1 hour |
| Proposal approval rate | Percentage of proposals approved | < 70% or > 95% (abnormal) |
| SLA violation count | Number of proposals exceeding SLA deadline | > 10 per day |
| Pending proposal count | Number of proposals awaiting review | > 100 |

**Compensation KPIs**:

| KPI | Description | Alert Threshold |
|-----|-------------|-----------------|
| Compensation execution failures | Number of failed compensation operations | > 5 per hour |
| Compensation retry count | Number of retries needed | > 20 per day |
| Total compensation amount | Cumulative dollar amount of compensations | (tracking only) |

### 7.2 Alert Requirements

The following conditions MUST trigger operational alerts:

1. **Review latency**: P95 response time exceeds 1 hour (sustained for 10 minutes)
2. **Queue backlog**: Pending proposals exceed 100 (sustained for 30 minutes)
3. **SLA violations**: More than 10 SLA violations in a single day
4. **Compensation failures**: Compensation failure rate exceeds 5 per hour (sustained for 5 minutes)

### 7.3 Dashboard Requirements

A dedicated Grafana dashboard MUST display:
- Review response time distribution (P50/P95/P99)
- Proposal approval rate gauge (with color thresholds: red < 70%, yellow 70-80%, green 80-95%, orange > 95%)
- Pending proposal count (real-time stat)
- SLA violation trend (hourly rate)

---

## 8. Compliance Requirements

### 8.1 Audit Trail

- Every review decision MUST be recorded with: reviewer identity, timestamp, decision type, review notes, approved/rejected amounts
- Every compensation execution MUST be recorded with: type, amount, reason, execution status
- Audit records MUST be immutable and retained per regulatory requirements

### 8.2 Regulatory Alignment

- The workflow supports compliance requirements for KYC/AML review processes
- All suspicious activity is documented and traceable
- Escalation paths ensure complex cases receive appropriate expertise

---

## 9. Change Log

### v2.1.0 Simplification Changes

- Eliminated complex threshold-based routing ($500/$5,000 auto-routing)
- Eliminated mandatory L1/L2 tier routing
- Introduced unified SLA with priority-based differentiation
- Implemented reviewer-initiated escalation (no auto-assignment)
- Established human-first review principle (all suspicious amounts > 0 create proposals)

### v3.0.0 Priority-Based SLA

- Introduced per-priority SLA deadlines (URGENT=1h, HIGH=2h, MEDIUM=24h, LOW=48h)
- Added automatic timeout actions per priority level
- Added SLA warning notifications at 75% threshold
