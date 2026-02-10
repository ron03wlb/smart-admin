# Detection Model Implementation

> **Canonical Source**: [05-02-01_Detection_Model.md](../../source-archive/05_Risk_Control/05-02-01_Detection_Model.md)
> **Audience**: Architects, Backend Engineers, Risk Team Engineers
> **Business Requirements**: [Detection_Model_Spec.md](../../requirements/05_Risk_Compliance/Detection_Model_Spec.md)
> **Last Synced**: 2026-02-09
>
> **Technical Focus**: This document contains implementation details (TCC pattern, SAGA flow, rule engine integration) extracted from Requirements layer.

---

## 1. Executive Summary

SmartAdmin iGaming v2.1.0 introduces a Configuration-Driven Risk Control System with a five-layer architecture. This document covers the technical implementation details: system architecture, event-driven processing, rule engine integration, and data flow.

### Prerequisites

- [05-01 Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) -- Configuration-driven rule engine (Section 9)
- [01-05 Withdrawal Risk](../../source-archive/01_Player_Center/01-05_Withdrawal_Risk.md) -- SAGA Step 2.5 deferred risk check
- [02-04 Turnover Reconciliation](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) -- Layer 1 processing flow

---

## 2. Configuration-Driven Architecture

### 2.1 Concept

Configuration-Driven Risk Control decouples rule action types (BLOCK / FLAG / IGNORE) from application code into database configuration tables.

```
Configuration-Driven Risk Control =
    Async Active Risk (BLOCK generates HIGH priority proposal)
  + Async Passive Risk (FLAG generates MEDIUM priority proposal)
  + Log-Only (IGNORE)
         |                          |                    |
    Async post-bet analysis    Async post-bet analysis  Log only
    Generates Risk Proposal    Generates Risk Proposal
    (Priority: HIGH)           (Priority: MEDIUM)
```

### 2.2 Design Principles

- **Synchronous blocking** is limited to: blacklisted players, IP bans, account freezes (Layer 1 fast checks)
- **All BLOCK/FLAG rules** execute asynchronously after bet success (Layer 3 risk analysis)
- **BLOCK rules do not reject bets**; they generate high-priority Risk Proposals for human review
- **Fund interception** occurs at withdrawal time -- deferred check (Layer 5 -- SAGA Step 2.5)

### 2.3 Advantages

| Advantage | Technical Rationale |
|-----------|---------------------|
| Zero false kills | Bet already succeeded; risk only flags post-hoc |
| High availability | Risk engine failure does not block betting (Fail Open principle) |
| Low latency | Bet response time unaffected by risk analysis (async processing) |
| Flexibility | Operators adjust rule priority (HIGH/MEDIUM/LOW) via config |
| Compliance | Aligned with DraftKings/FanDuel/Bet365/UKGC best practices |

---

## 3. Five-Layer System Architecture

```mermaid
graph TD
    A[Bet Request<br/>Player Bet Request] --> B{Layer 1: Synchronous Blacklist Check<br/>Response Time: <10ms}

    B -->|Redis Cache Lookup| C{Check Result}

    C -->|Hit: Blacklist/Frozen/IP Blocked| D[Reject Bet<br/>Return: Explicit Rejection Reason]

    C -->|Pass| E[Layer 2: TCC Transaction Processing]

    E --> E1[Try Phase: Freeze Resources<br/>Bonus + Cash + Credit]
    E1 --> E2[Confirm Phase:<br/>Deduct Funds + Write Transaction Logs]
    E2 --> E3[Write outbox_event]
    E3 --> E4[Commit DB Transaction]

    E4 --> F[Bet Success<br/>Player Sees Bet Result]

    F -.->|Async Event| G[Layer 3: Async Risk Analysis<br/>Event-Driven]

    G --> G1[Kafka Consumer<br/>Topic: wallet.debited<br/>Payload: player_id, bet_id,<br/>amount, game_type]

    G1 --> G2[Risk Engine Executes All Rules<br/>Load t_risk_rule_config<br/>Execute enabled rules<br/>Return matched_rules]

    G2 --> G3{Decision Routing<br/>Based on action_type}

    G3 -->|action_type = BLOCK<br/>and matched| G4[Generate Risk Proposal<br/>Priority: HIGH<br/>Mark matched_rules<br/>Calculate suspicious_amount]

    G3 -->|action_type = FLAG<br/>and matched| G5[Generate Risk Proposal<br/>Priority: MEDIUM<br/>Mark flagged_rules]

    G3 -->|action_type = IGNORE| G6[Log Only]

    G4 --> H[Layer 4: Human Review and Disposition]
    G5 --> H

    H --> H1[Reviewer Checks Proposal Details]
    H1 --> H2{Review Decision}

    H2 -->|APPROVED| H3[No Action<br/>Continue Monitoring]

    H2 -->|REJECTED| H4[Freeze Account + Mark Funds<br/>Update t_player_risk_profile<br/>Write account_freeze_log<br/>Write suspicious_fund_marker]

    H2 -->|PARTIAL| H5[Partial Freeze]

    F -.->|Player Requests Withdrawal| I[Layer 5: Withdrawal Deferred Check<br/>SAGA Step 2.5]

    I --> I1[Query Historical Risk Proposals<br/>Time Window: 30 Days]

    I1 --> I2[Calculate Suspicious Amount Sum]

    I2 --> I3{Decision}

    I3 -->|Suspicious Amount = 0| I4[Continue Withdrawal]

    I3 -->|Suspicious Amount > 0| I5[Freeze Amount<br/>Generate Manual Review Proposal<br/>Route to Review Queue]

    style A fill:#e1f5ff
    style B fill:#fff4e1
    style C fill:#fff4e1
    style D fill:#ffe1e1
    style E fill:#e1ffe1
    style F fill:#e1ffe1
    style G fill:#f0e1ff
    style G3 fill:#f0e1ff
    style H fill:#ffe1f0
    style I fill:#e1f0ff

    classDef syncLayer fill:#fff4e1,stroke:#ff9800,stroke-width:3px
    classDef asyncLayer fill:#f0e1ff,stroke:#9c27b0,stroke-width:3px
    classDef transactionLayer fill:#e1ffe1,stroke:#4caf50,stroke-width:3px
    classDef reviewLayer fill:#ffe1f0,stroke:#e91e63,stroke-width:3px
    classDef withdrawalLayer fill:#e1f0ff,stroke:#2196f3,stroke-width:3px

    class B,C syncLayer
    class E,E1,E2,E3,E4,F transactionLayer
    class G,G1,G2,G3,G4,G5,G6 asyncLayer
    class H,H1,H2,H3,H4,H5 reviewLayer
    class I,I1,I2,I3,I4,I5 withdrawalLayer
```

---

## 4. Layer Details

### 4.1 Layer 1: Synchronous Blacklist Check (< 10ms)

- **Data source**: Redis cache
- **Checked conditions**: Blacklisted players, IP bans, frozen accounts, self-exclusion lists (UKGC/MGA)
- **Fail Open**: If the risk system is down, bets are allowed through by default

### 4.2 Layer 2: TCC Transaction Processing

```
Try Phase   --> Freeze resources (Bonus + Cash + Credit)
Confirm Phase --> Actual deduction + write transaction logs
                  Write outbox_event
                  Commit DB transaction
```

The player sees their bet result immediately after Layer 2 completes.

### 4.3 Layer 3: Async Risk Analysis (Event-Driven, ~5s)

**Event Pipeline**:

```
outbox_event --> Kafka Topic: wallet.debited --> Risk Analysis Consumer

Kafka Payload:
{
  "player_id": Long,
  "bet_id": Long,
  "amount": BigDecimal,
  "game_type": String
}
```

**Rule Execution Flow**:

```
1. Load enabled rules from t_risk_rule_config
2. Execute each rule against the bet context
3. Collect matched_rules
4. Route by action_type:
   - BLOCK + matched --> Risk Proposal (Priority: HIGH)
   - FLAG + matched  --> Risk Proposal (Priority: MEDIUM)
   - IGNORE          --> Log only
```

### 4.4 Layer 4: Human Review and Disposition

Review outcomes and their system actions:

| Decision | System Actions |
|----------|---------------|
| APPROVED | No action; continue monitoring |
| REJECTED | Freeze account; mark suspicious funds; update `t_player_risk_profile`; write `account_freeze_log`; write `suspicious_fund_marker` |
| PARTIAL | Partial freeze applied |

### 4.5 Layer 5: Withdrawal Deferred Check (SAGA Step 2.5)

```
1. Query historical Risk Proposals (time window: 30 days)
2. Calculate sum of suspicious_amount across all proposals
3. Decision:
   - suspicious_amount = 0 --> Continue withdrawal
   - suspicious_amount > 0 --> Freeze amount + generate manual review proposal
```

---

## 5. Applicable Risk Scenarios

### 5.1 Layer 1 -- Synchronous Block Scenarios

| Scenario | Check Method |
|----------|-------------|
| Blacklisted Player | Redis SET lookup |
| IP Blocked | Redis SET lookup |
| Account Frozen | Redis hash field check |
| Self-Exclusion (UKGC/MGA) | Redis SET lookup |

### 5.2 Layer 3 -- Async BLOCK Rules

| Rule | Detection Method |
|------|-----------------|
| Bot Detection | Behavioural feature analysis (bet timing, pattern regularity) |
| Same-Match Hedging | Historical bet query for same match, opposite outcomes |
| Same-IP Arbitrage | Correlation analysis across accounts sharing IP |
| Abnormal Odds Detection | Statistical analysis of odds selection distribution |
| Turnover Manipulation | Turnover velocity calculation against deposit ratio |

### 5.3 Layer 3 -- Async FLAG Rules

| Rule | Detection Method |
|------|-----------------|
| Cross-Match Hedging | Cross-match bet correlation (lower risk) |
| Low-Odds Turnover (< 1.5) | Odds threshold check |
| Abnormal Betting Pattern | Pattern deviation from player baseline |
| High-Frequency Betting (> 10/min) | Sliding window count |

---

## 6. Database Schema (PostgreSQL)

### detection_models
Stores risk detection model configurations and rules.

```sql
CREATE TABLE detection_models (
    model_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    model_name VARCHAR(100) NOT NULL,
    model_type VARCHAR(50) NOT NULL, -- 'BLACKLIST', 'BOT_DETECTION', 'HEDGING', 'ARBITRAGE', 'TURNOVER_MANIPULATION'
    action_type VARCHAR(20) NOT NULL, -- 'BLOCK', 'FLAG', 'IGNORE'
    priority VARCHAR(10) NOT NULL, -- 'HIGH', 'MEDIUM', 'LOW'
    layer INT NOT NULL, -- 1 (Sync), 3 (Async), 5 (Withdrawal)

    -- Rule configuration
    rule_definition JSONB NOT NULL, -- Model-specific detection logic
    thresholds JSONB, -- Configurable thresholds (e.g., { "max_frequency": 10, "odds_min": 1.5 })
    detection_method TEXT, -- Natural language description
    enabled_game_types TEXT[], -- ['SPORTS', 'SLOTS', 'BACCARAT', ...] or NULL (all games)

    -- Model metadata
    accuracy_rate DECIMAL(5,4), -- 0.0000 to 1.0000 (e.g., 0.9523 = 95.23%)
    false_positive_rate DECIMAL(5,4),
    training_data_size BIGINT,
    last_trained_at TIMESTAMPTZ,
    model_version VARCHAR(20),

    -- Status and lifecycle
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL REFERENCES admins(admin_id),
    updated_by UUID REFERENCES admins(admin_id),

    -- Audit trail
    change_history JSONB, -- Array of { timestamp, changed_by, changes[] }

    CONSTRAINT valid_layer CHECK (layer IN (1, 3, 5)),
    CONSTRAINT valid_action_type CHECK (action_type IN ('BLOCK', 'FLAG', 'IGNORE')),
    CONSTRAINT valid_priority CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW'))
);

CREATE INDEX idx_detection_models_tenant ON detection_models(tenant_id) WHERE is_enabled = TRUE;
CREATE INDEX idx_detection_models_layer_action ON detection_models(layer, action_type) WHERE is_enabled = TRUE;
CREATE INDEX idx_detection_models_type ON detection_models(model_type) WHERE is_enabled = TRUE;
CREATE INDEX idx_detection_models_effective ON detection_models(effective_from, effective_to) WHERE is_enabled = TRUE;

COMMENT ON TABLE detection_models IS 'Risk detection model configurations for five-layer risk control system';
COMMENT ON COLUMN detection_models.layer IS '1=Synchronous blacklist check, 3=Async risk analysis, 5=Withdrawal deferred check';
COMMENT ON COLUMN detection_models.action_type IS 'BLOCK=Generate HIGH priority proposal, FLAG=Generate MEDIUM priority proposal, IGNORE=Log only';
COMMENT ON COLUMN detection_models.rule_definition IS 'JSONB configuration for rule-specific logic (SQL queries, ML model params, threshold conditions)';
```

### detection_results
Tracks risk detection execution results and matched rules.

```sql
CREATE TABLE detection_results (
    result_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(tenant_id),
    player_id UUID NOT NULL REFERENCES players(player_id),
    bet_id UUID REFERENCES bets(bet_id),
    withdrawal_id UUID REFERENCES withdrawals(withdrawal_id),
    model_id UUID NOT NULL REFERENCES detection_models(model_id),

    -- Detection context
    layer INT NOT NULL, -- 1, 3, or 5
    event_type VARCHAR(50) NOT NULL, -- 'BET_PLACED', 'WITHDRAWAL_REQUEST', 'BLACKLIST_CHECK'
    event_timestamp TIMESTAMPTZ NOT NULL,
    game_type VARCHAR(50),
    amount DECIMAL(18,4),

    -- Detection outcome
    matched BOOLEAN NOT NULL DEFAULT FALSE,
    action_type VARCHAR(20) NOT NULL, -- 'BLOCK', 'FLAG', 'IGNORE'
    risk_score DECIMAL(5,4), -- 0.0000 to 1.0000 (higher = riskier)
    confidence_level DECIMAL(5,4), -- Model confidence (0.0000 to 1.0000)

    -- Matched rule details
    matched_rules JSONB, -- Array of matched rule names with scores
    detection_reason TEXT, -- Human-readable explanation
    suspicious_amount DECIMAL(18,4), -- Amount flagged for review (if applicable)

    -- Risk proposal linkage
    risk_proposal_id UUID REFERENCES risk_proposals(proposal_id),
    proposal_priority VARCHAR(10), -- 'HIGH' (BLOCK), 'MEDIUM' (FLAG), NULL (IGNORE)

    -- Processing metadata
    processing_time_ms INT, -- Execution time in milliseconds
    model_version VARCHAR(20),
    rule_snapshot JSONB, -- Snapshot of detection_models at execution time

    -- Audit trail
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    review_decision VARCHAR(20), -- 'APPROVED', 'REJECTED', 'PARTIAL', NULL (pending)
    reviewed_by UUID REFERENCES admins(admin_id),

    CONSTRAINT valid_layer CHECK (layer IN (1, 3, 5)),
    CONSTRAINT valid_action_type CHECK (action_type IN ('BLOCK', 'FLAG', 'IGNORE')),
    CONSTRAINT valid_review_decision CHECK (review_decision IS NULL OR review_decision IN ('APPROVED', 'REJECTED', 'PARTIAL'))
);

CREATE INDEX idx_detection_results_player ON detection_results(player_id, detected_at DESC);
CREATE INDEX idx_detection_results_bet ON detection_results(bet_id) WHERE bet_id IS NOT NULL;
CREATE INDEX idx_detection_results_withdrawal ON detection_results(withdrawal_id) WHERE withdrawal_id IS NOT NULL;
CREATE INDEX idx_detection_results_matched ON detection_results(matched, action_type, detected_at DESC) WHERE matched = TRUE;
CREATE INDEX idx_detection_results_proposal ON detection_results(risk_proposal_id) WHERE risk_proposal_id IS NOT NULL;
CREATE INDEX idx_detection_results_review_pending ON detection_results(detected_at DESC) WHERE review_decision IS NULL AND matched = TRUE;
CREATE INDEX idx_detection_results_layer_event ON detection_results(layer, event_type, detected_at DESC);

COMMENT ON TABLE detection_results IS 'Risk detection execution results for all layers (sync, async, withdrawal)';
COMMENT ON COLUMN detection_results.matched IS 'TRUE if detection rule was triggered, FALSE otherwise';
COMMENT ON COLUMN detection_results.suspicious_amount IS 'Amount flagged for review or freeze (used in Layer 5 withdrawal check)';
COMMENT ON COLUMN detection_results.rule_snapshot IS 'Immutable snapshot of detection_models configuration at execution time';
```

### Query Examples

**Analyze detection model effectiveness:**
```sql
SELECT
    dm.model_name,
    dm.model_type,
    dm.action_type,
    COUNT(*) AS total_executions,
    COUNT(*) FILTER (WHERE dr.matched = TRUE) AS matches,
    COUNT(*) FILTER (WHERE dr.review_decision = 'REJECTED') AS confirmed_fraud,
    (COUNT(*) FILTER (WHERE dr.review_decision = 'REJECTED')::DECIMAL / NULLIF(COUNT(*) FILTER (WHERE dr.matched = TRUE), 0))::DECIMAL(5,4) AS precision,
    AVG(dr.processing_time_ms) AS avg_processing_time_ms
FROM detection_models dm
INNER JOIN detection_results dr ON dm.model_id = dr.model_id
WHERE dr.detected_at >= NOW() - INTERVAL '30 days'
GROUP BY dm.model_id, dm.model_name, dm.model_type, dm.action_type
ORDER BY confirmed_fraud DESC;
```

**Calculate suspicious amount for withdrawal deferred check (Layer 5):**
```sql
SELECT
    player_id,
    SUM(suspicious_amount) AS total_suspicious_amount,
    COUNT(*) FILTER (WHERE action_type = 'BLOCK') AS high_priority_flags,
    COUNT(*) FILTER (WHERE action_type = 'FLAG') AS medium_priority_flags,
    MAX(detected_at) AS latest_detection
FROM detection_results
WHERE player_id = 'player-uuid-001'
    AND matched = TRUE
    AND review_decision IS NULL -- Pending review
    AND detected_at >= NOW() - INTERVAL '30 days' -- 30-day window
GROUP BY player_id;
```

**Audit Layer 1 synchronous blocks:**
```sql
SELECT
    DATE_TRUNC('hour', detected_at) AS hour,
    model_type,
    COUNT(*) AS block_count,
    COUNT(DISTINCT player_id) AS unique_players,
    AVG(processing_time_ms) AS avg_processing_time_ms
FROM detection_results
WHERE layer = 1
    AND matched = TRUE
    AND detected_at >= NOW() - INTERVAL '24 hours'
GROUP BY hour, model_type
ORDER BY hour DESC, block_count DESC;
```

---

## 7. Architecture Comparison (v2.1.0 vs v3.0.0)

| Aspect | v2.1.0 (Synchronous) | v3.0.0 (Async Event-Driven) |
|--------|----------------------|------------------------------|
| Risk Trigger Timing | During bet request (sync) | After bet success (async) |
| BLOCK Rule Handling | Reject bet | Generate HIGH priority proposal |
| FLAG Rule Handling | Allow bet + generate proposal | Generate MEDIUM priority proposal |
| Blacklist Check | Mixed with other rules | Independent synchronous check layer |
| Fund Interception | At bet time (blocking) | At withdrawal time (deferred) |
| False Positive Rate | 5--10% normal players rejected | Zero false rejections |
| Availability | SPOF (risk down = bets fail) | HA (risk down does not affect bets) |

---

## 7. Key Design Decisions

### 7.1 Minimal Synchronous Blocking (Layer 1)
- Only hard rules checked: blacklist, IP ban, account freeze
- Redis cache for < 10ms response time
- Fail Open: risk system failure defaults to allow

### 7.2 Bet-First Completion (Layer 2)
- TCC transaction model ensures bet success
- Player sees result immediately
- Risk analysis does not impact betting experience

### 7.3 Async Risk Analysis (Layer 3)
- Kafka + event-driven architecture
- All rules analysed within ~5 seconds
- BLOCK/FLAG generate Risk Proposals; bets are never rejected

### 7.4 Human-Primary Review (Layer 4)
- Automation generates proposals only
- Final decisions made by human reviewers
- Avoids ML model false positives

### 7.5 Post-Hoc Fund Interception (Layer 5)
- Withdrawal triggers historical proposal query (30-day window)
- Suspicious amounts intercepted at withdrawal stage
- Aligned with DraftKings/FanDuel/Bet365/UKGC industry practices

---

## 8. Related Documents

- [05-02-02 Rule Configuration](../../source-archive/05_Risk_Control/05-02-02_Rule_Configuration.md) -- Risk proposal service and deferred checks
- [05-02-03 ML Integration](../../source-archive/05_Risk_Control/05-02-03_ML_Integration.md) -- Multi-dimensional risk rules
- [05-02-04 Operations Tools](../../source-archive/05_Risk_Control/05-02-04_Operations_Tools.md) -- SmartAdmin architecture mapping and monitoring
