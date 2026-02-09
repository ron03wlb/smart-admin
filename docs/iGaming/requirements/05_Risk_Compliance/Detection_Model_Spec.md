# Detection Model Specification

> **Canonical Source**: [05-02-01_Detection_Model.md](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md)
> **Audience**: Executives, Risk Operations, Compliance Officers, Product Managers
> **Related Doc**: [Detection_Model_Implementation.md](../../architecture/05_Risk_Engine/Detection_Model_Implementation.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (TCC pattern, SAGA flow implementation) moved to Architecture layer. This document focuses on business rules and operational policies.

---

## 1. Executive Summary

SmartAdmin iGaming v2.1.0 introduces a Configuration-Driven Risk Control System that allows operations teams to configure how each risk rule is handled -- without requiring code changes or new deployments.

### Core Value Proposition

| Feature | Description | Business Value |
|---------|-------------|----------------|
| Configuration-Driven | Rule action types (BLOCK / FLAG / IGNORE) managed via database settings | Adjust risk strategies without code changes |
| Human-Centric Review | Anomalies generate risk proposals for human review | Simplified decision logic; avoids over-automation |
| Equal Treatment | All players undergo risk checks (no VIP exemptions) | Regulatory compliance; fairness principle |
| Operator Choice | Operators decide their own risk tolerance levels | Supports multi-market strategies; improves client satisfaction |

---

## 2. Evolution from Traditional Risk Control

| Dimension | Traditional Risk (v2.0.0) | Configuration-Driven Risk (v2.1.0) |
|-----------|--------------------------|--------------------------------------|
| Rule Classification | Hard-coded P0/P1/P2 (30%/50%/20%) | Configuration-driven (operator chooses BLOCK/FLAG per rule) |
| VIP Exemption | VIP Level 3+ exempt from risk checks (non-compliant) | All players treated equally (no exemptions) |
| Anomaly Handling | Complex automated threshold logic | Human review as primary decision mechanism |
| Strategy Adjustment | Requires code changes + deployment | Database configuration update only |
| Flexibility | Fixed percentages, not adjustable | Each rule independently configurable |

---

## 3. Risk Rule Action Types

Every risk rule can be configured with one of three action types:

| Action Type | Behaviour | Use Case |
|-------------|-----------|----------|
| **BLOCK** | Generates a high-priority risk proposal after the bet succeeds (async) | Serious fraud patterns -- bot detection, same-match hedging, same-IP arbitrage |
| **FLAG** | Generates a medium-priority risk proposal after the bet succeeds (async) | Suspicious but lower-risk patterns -- cross-match hedging, low-odds turnover manipulation |
| **IGNORE** | Logs the event only; no proposal generated | Experimental rules, data collection rules |

**Important**: BLOCK rules do not reject bets. All bets are processed first; risk analysis runs asynchronously afterward. Fund interception occurs at the withdrawal stage.

---

## 4. Detection Layers

The system operates across five layers, each with specific business rules:

### Layer 1: Synchronous Blocking (Immediate, under 10ms)

Only the most severe cases are blocked in real time:

| Scenario | Description |
|----------|-------------|
| Blacklisted Player | Confirmed fraudster; bet is immediately rejected |
| IP Blocked | Known attack source |
| Account Frozen | Under manual review |
| Self-Exclusion List | Regulatory requirement (UKGC/MGA) |

### Layer 2: Transaction Processing

Bets are processed using a two-phase transaction pattern. The player sees their bet result immediately. Risk analysis does not delay or affect the betting experience.

→ **[TCC Pattern Implementation](../../architecture/05_Risk_Engine/Detection_Model_Implementation.md#transaction-processing)** - Try-Confirm-Cancel technical details

### Layer 3: Asynchronous Risk Analysis (within approx. 5 seconds)

Rules are evaluated after the bet succeeds. Detected patterns generate risk proposals:

**BLOCK Rules (High Priority)**:

| Rule | Description |
|------|-------------|
| Bot Detection | Behavioural pattern analysis identifies automated betting |
| Same-Match Hedging | Opposite bets on the same match by the same player |
| Same-IP Arbitrage | Correlated accounts from the same IP placing offsetting bets |
| Abnormal Odds Detection | Statistical anomaly in odds selection patterns |
| Turnover Manipulation | Artificial turnover generation to meet withdrawal requirements |

**FLAG Rules (Medium Priority)**:

| Rule | Description |
|------|-------------|
| Cross-Match Hedging | Opposite bets across different matches (lower risk) |
| Low-Odds Turnover | Bets at odds below 1.5 (requires human judgement) |
| Abnormal Betting Pattern | Unusual pattern that may be a false positive |
| High-Frequency Betting | More than 10 bets per minute (requires trend observation) |

### Layer 4: Human Review and Disposition

Risk proposals are reviewed by human operators. Three possible outcomes:

| Decision | Action |
|----------|--------|
| Approved | No action taken; player continues to be monitored |
| Rejected | Account frozen; suspicious funds marked; risk profile updated |
| Partial | Partial account freeze applied |

### Layer 5: Withdrawal Deferred Check

When a player requests a withdrawal, the system queries historical risk proposals (30-day window):

→ **[SAGA Implementation](../../architecture/05_Risk_Engine/Detection_Model_Implementation.md#withdrawal-deferred-check)** - SAGA Step 2.5 technical flow

| Condition | Outcome |
|-----------|---------|
| Suspicious amount = 0 | Withdrawal proceeds normally |
| Suspicious amount > 0 | Funds frozen; manual review proposal generated |

---

## 5. Key Design Principles

| Principle | Description | Business Benefit |
|-----------|-------------|------------------|
| Minimal Synchronous Blocking | Only blacklist / IP block / account freeze are checked synchronously | Near-zero latency impact on betting |
| Bet-First Completion | Bets always succeed before risk analysis runs | No false rejections; zero innocent player impact |
| Async Risk Analysis | All BLOCK/FLAG rules run asynchronously post-bet | 5-second analysis window; no betting delays |
| Human-Primary Review | Automation generates proposals; humans make final decisions | Avoids ML model false positives |
| Post-Hoc Fund Interception | Suspicious funds intercepted at withdrawal time | Compliant with industry best practices |

---

## 6. Risk Thresholds and Policies

### Risk Score Model

The system does not use a composite risk score for per-bet decisions. Instead, each rule independently triggers proposals based on its configured action type.

### Withdrawal Risk Aggregation

| Time Window | Scope |
|-------------|-------|
| 30 days | All unresolved risk proposals within the window |
| Calculation | Sum of suspicious amounts from all matched proposals |
| Threshold | Any suspicious amount > 0 triggers review |

### Industry Benchmarks

| Operator / Regulator | Practice |
|----------------------|----------|
| DraftKings / FanDuel (US) | Only blacklist is synchronous; everything else is async |
| Bet365 (UK) | Hedging detection analysed within 5 minutes post-bet |
| UKGC Compliance Framework | Recommends post-hoc risk control + withdrawal interception |

---

## 7. Comparison with Previous Architecture

| Metric | Previous (v2.1.0 sync) | Current (v3.0.0 async) |
|--------|------------------------|------------------------|
| Risk Trigger Timing | During bet request (synchronous) | After bet success (asynchronous) |
| BLOCK Rule Handling | Rejects the bet | Generates high-priority proposal |
| FLAG Rule Handling | Allows bet + generates proposal | Generates medium-priority proposal |
| Blacklist Check | Mixed with other rules | Independent synchronous check layer |
| Fund Interception Timing | At bet time (blocking) | At withdrawal time (deferred) |
| False Positive Impact | 5--10% of normal players rejected | Zero false rejections (bet already succeeded) |
| System Availability | Single point of failure (risk down = bets fail) | High availability (risk down does not affect bets) |

---

## 8. Acceptance Criteria

- All players undergo identical risk checks regardless of VIP status
- BLOCK/FLAG action types are configurable per rule without code deployment
- Bets are never rejected by asynchronous risk rules (zero false kills)
- Risk proposals are generated within approximately 5 seconds of bet completion
- Withdrawal checks query all unresolved proposals within a 30-day window
- All configuration changes are recorded in an audit log

---

## 9. Related Documents

- [05-02-02 Rule Configuration](../../source-archive/05_Risk_Control/05-02-02_Rule_Configuration.md) -- Risk proposal service and deferred checks
- [05-02-03 ML Integration](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) -- Multi-dimensional risk rules
- [05-02-04 Operations Tools](../../source-archive/05_Risk_Control/05-02-04_Operations_Tools.md) -- SmartAdmin architecture mapping and monitoring
