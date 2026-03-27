---
title: "Ch6: 風控與合規技術架構"
part: technical
module: risk-compliance
version: v2.2
created: 2026-03-24
---

# 第 6 章：風控與合規技術架構

## 6.1 模組概述

The Risk & Compliance module implements a five-layer detection pipeline for real-time fraud detection, AML compliance, and player protection. The system uses LiteFlow for synchronous rule execution and Apache Flink CEP for complex event processing on streaming data.

**Technology Stack:**
- **Go 1.22**: Core risk scoring engine and Layer 1-2 components (high-performance blocking and rule evaluation)
- **Python 3.12 FastAPI**: ML model serving for fraud classification and risk prediction
- **Apache Flink 1.18**: CEP (Complex Event Processing) for Layer 3 async rule detection
- **LiteFlow 2.11**: Rule chain engine for declarative risk rules (Layer 2)
- **Redis Cluster**: Blacklist lookups, sliding window counters, device fingerprint caching
- **PostgreSQL 15**: Risk decisions, proposals, audit logs (7-year retention)
- **Kafka 3.5**: Event streaming for CEP and audit trail
- **FingerprintJS Pro**: Device fingerprinting with 99.5% accuracy

**Key Characteristics:**
- Sub-10ms latency for Layer 1 (blacklist blocking)
- Sub-50ms latency for Layer 2 (synchronous rules)
- Configurable rule chains per tenant
- Independent rule firing (not score accumulation)
- Full audit trail for regulatory compliance
- 3-tier review permissions system

---

## 6.2 五層偵測管線

The detection pipeline processes requests through five sequential layers, with early exit on critical blocks:

```
┌─────────────────────────────────────────────────────────────────┐
│ Layer 1: Sync Blocking (<10ms) - Blacklist Check Only           │
│ Redis SET: IP/Device/Player/Payment blacklists                  │
│ Decision: BLOCK ONLY (no scoring)                               │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ├─ BLOCK → Immediate Denial
               │
               ├─ PASS ↓
└──────────────┬──────────────────────────────────────────────────┐
│ Layer 2: Sync Rules (<50ms) - LiteFlow Chain Execution          │
│ Parallel: Velocity, Amount, Device, Behavior, Geo               │
│ Decision: PASS / FLAG / MANUAL_REVIEW / BLOCK                   │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ├─ BLOCK → Immediate Denial
               │
               ├─ FLAG/REVIEW ↓
└──────────────┬──────────────────────────────────────────────────┐
│ Layer 3: Async Rules (Flink CEP) - Complex Patterns             │
│ Event Stream: Bets, Deposits, Withdrawals                       │
│ Decision: BLOCK / FLAG for Human Review                         │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ├─ BLOCK → Account Freeze
               │
               ├─ FLAG → Create Proposal ↓
└──────────────┬──────────────────────────────────────────────────┐
│ Layer 4: Human Review - Risk Proposal Handling                  │
│ Priority: URGENT (1h) / HIGH (2h) / MEDIUM (24h) / LOW (48h)   │
│ Decision: APPROVED / REJECTED / ESCALATED                       │
└──────────────┬──────────────────────────────────────────────────┘
               │
               ├─ APPROVED → Proceed
               │
               ├─ REJECTED → Action Denied ↓
└──────────────┬──────────────────────────────────────────────────┐
│ Layer 5: Withdrawal Delay Check - 30-day Lookback               │
│ Check pending proposals before withdrawal approval              │
│ Additional velocity checks on withdrawal patterns               │
└──────────────────────────────────────────────────────────────────┘
```

### Layer 1: 同步阻擋 (<10ms)

**Purpose**: Fast-fail on known-malicious actors before expensive processing.

**Implementation**:
- Redis SET data structures for O(1) lookups
- Go microservice (compiled binary, minimal GC pauses)
- Parallel checks: IP blacklist, device blacklist, player blacklist, payment method blacklist
- No scoring; decision is binary: BLOCK or PASS

**Redis Key Structure**:
```
blacklist:ip:{tenant_id} → SET of CIDR blocks
blacklist:device:{tenant_id} → SET of device fingerprints
blacklist:player:{tenant_id} → SET of player IDs
blacklist:payment:{tenant_id} → SET of payment method hashes
```

**Go Pseudocode**:
```go
func (ls *Layer1Service) CheckBlacklist(ctx context.Context, req *RiskRequest) Result {
    checks := []func() bool{
        func() bool { return ls.redis.SIsMember(ctx, "blacklist:ip:"+req.TenantID, req.IP).Val() },
        func() bool { return ls.redis.SIsMember(ctx, "blacklist:device:"+req.TenantID, req.DeviceID).Val() },
        func() bool { return ls.redis.SIsMember(ctx, "blacklist:player:"+req.TenantID, req.PlayerID).Val() },
        func() bool { return ls.redis.SIsMember(ctx, "blacklist:payment:"+req.TenantID, req.PaymentHash).Val() },
    }

    for _, check := range checks {
        if check() {
            return Result{Decision: BLOCK, Layer: 1, Reason: "Blacklist hit"}
        }
    }
    return Result{Decision: PASS, Layer: 1}
}
```

**SLA**: P99 < 10ms (typical: 2-5ms)

---

### Layer 2: 同步規則 (<50ms)

**Purpose**: Fast synchronous rule evaluation using LiteFlow rule engine.

**Components** (5 parallel executors where possible):

1. **VelocityCheckCmp** - Rate limiting
   - Redis sliding window (SortedSet)
   - Limits: 100 operations per minute per player
   - 5 operation types tracked: login, bet, deposit, withdrawal, KYC update
   - Returns: velocity_score (0-100), violation_count

2. **AmountThresholdCmp** - High-amount flagging
   - Static thresholds: bets > $10k, withdrawals > $50k, deposits > $100k
   - Configurable per tenant in rule parameters
   - Returns: amount_flag (true/false), flagged_amount, threshold

3. **DeviceFingerprintCmp** - Device anomaly detection
   - FingerprintJS integration: 99.5% accuracy
   - 65K+ data points across all players (browser, OS, hardware, canvas fingerprint, WebGL)
   - Detects: device spoofing, rapid device changes, stolen devices
   - Returns: device_anomaly_score (0-100), anomaly_type (spoofing|hardware_mismatch|new_device)

4. **BehaviorPatternCmp** - Betting pattern analysis
   - Features: bet frequency, bet sizing variance, game selection patterns, play duration
   - Detects: arbitrage betting, grinding patterns, unusual time-of-day activity
   - Returns: behavior_anomaly_score (0-100), pattern_type

5. **GeoLocationCmp** - Geo-restriction and anomaly detection
   - IP geolocation + device location (if available)
   - Rapid location changes (e.g., London to Tokyo in 10 minutes)
   - Restricted jurisdictions (UK, NJ, etc.)
   - Returns: geo_violation (true/false), risk_country (true/false), velocity_score

**LiteFlow EL Expression**:
```javascript
THEN(
    VelocityCheckCmp,
    AmountThresholdCmp,
    WHEN(
        DeviceFingerprintCmp,
        BehaviorPatternCmp,
        GeoLocationCmp
    )
);
```

**Execution**: Components 1-2 execute sequentially; components 3-5 execute in parallel WHEN block.

**Go Implementation** (LiteFlow wrapper):
```go
type Layer2Service struct {
    lf *liteflow.LiteFlow
    rules map[string]*RuleChain
}

func (ls *Layer2Service) ExecuteRules(ctx context.Context, req *RiskRequest) (*Layer2Result, error) {
    chain := ls.rules[req.TenantID+":"+req.RuleChainID]
    if chain == nil {
        return nil, fmt.Errorf("rule chain not found")
    }

    executionResult, err := chain.Execute(ctx, req)
    if err != nil {
        return nil, err
    }

    // Build result from component outputs
    result := &Layer2Result{
        VelocityScore: executionResult.Get("velocity_score"),
        AmountFlag: executionResult.Get("amount_flag"),
        DeviceAnomalyScore: executionResult.Get("device_anomaly_score"),
        BehaviorAnomalyScore: executionResult.Get("behavior_anomaly_score"),
        GeoViolation: executionResult.Get("geo_violation"),
    }

    // Determine decision
    if result.DeviceAnomalyScore > 80 || result.GeoViolation {
        result.Decision = MANUAL_REVIEW
    } else if result.AmountFlag && result.VelocityScore > 60 {
        result.Decision = MANUAL_REVIEW
    } else {
        result.Decision = PASS
    }

    return result, nil
}
```

**SLA**: P99 < 50ms (typical: 20-40ms)

---

### Layer 3: 非同步規則 (Flink CEP)

**Purpose**: Detect complex multi-event patterns over sliding time windows using Flink Continuous Event Processing.

**Architecture**:
- Kafka source: bet_events, deposit_events, withdrawal_events topics
- Independent rule firing: each pattern detection triggers its own BLOCK or FLAG action
- NOT score accumulation—each rule fires independently based on pattern matches
- ADR-012: "Flag mode, not real-time block"—Layer 3 creates proposals; Layer 4 decides

**Patterns Detected**:

1. **Arbitrage Detection** (5-min window)
   ```java
   Pattern<BetEvent, ?> arbitragePattern = Pattern
       .<BetEvent>begin("first_bet")
       .where(event -> event.getBetType().equals("SPORTS"))
       .followedBy("opposite_bet")
       .where((event, ctx) -> {
           BetEvent first = ctx.getEventsForPattern("first_bet").iterator().next();
           return event.getPlayerId().equals(first.getPlayerId())
               && !event.getSelection().equals(first.getSelection())
               && event.getOdds() * first.getOdds() > 1.95; // Guaranteed profit
       })
       .within(Time.minutes(5));
   ```

2. **Rapid Deposit-Withdrawal** (30-min window)
   ```java
   Pattern<TransactionEvent, ?> rapidExitPattern = Pattern
       .<TransactionEvent>begin("deposit")
       .where(e -> e.getType() == DEPOSIT && e.getAmount() > 500)
       .followedBy("withdrawal")
       .where((e, ctx) -> {
           TransactionEvent dep = ctx.getEventsForPattern("deposit").iterator().next();
           return e.getPlayerId().equals(dep.getPlayerId())
               && e.getAmount() >= dep.getAmount() * 0.9
               && e.getTimestamp() - dep.getTimestamp() < Duration.ofMinutes(30).toMillis();
       })
       .within(Time.minutes(30));
   ```

3. **Self-Exclusion Evasion** (cross-tenant, 7-day window)
   - Player excluded in GAMSTOP database
   - Same device/IP/payment attempting to create new account
   - Action: Immediate BLOCK with escalation

4. **Multi-Account Ring** (device fingerprint clustering)
   - 5+ accounts sharing same fingerprint + payment method
   - Betting patterns correlated (same teams, games, odds)
   - Action: FLAG all accounts for review, investigate for bonus abuse

5. **Layering Pattern** (7-day window)
   - Multiple small deposits (< $500 each) to same player
   - Followed by single large withdrawal
   - Classic money laundering indicator
   - Action: FLAG for SAR review

**Flink Job Configuration** (YAML):
```yaml
jobName: "Risk-CEP-Pipeline"
parallelism: 8
source:
  kafka:
    topic: ["bet_events", "deposit_events", "withdrawal_events"]
    groupId: "risk-cep-group"
    bootstrapServers: "kafka-0.kafka.default:9092,kafka-1.kafka.default:9092"
sink:
  kafka:
    topic: "risk_proposals"
    bootstrapServers: "kafka-0.kafka.default:9092"
stateBackend: "rocksdb"
checkpointInterval: "60000" # 1 min
```

**CEP Output** (flagged events published to Kafka):
```json
{
  "proposal_id": "cep_20260324_001234",
  "tenant_id": "tenant_abc",
  "player_id": "player_xyz",
  "trigger_rule": "arbitrage_detection",
  "priority": "HIGH",
  "status": "OPEN",
  "evidence": {
    "pattern_type": "arbitrage_3bet_window",
    "bets": [
      {"bet_id": "bet_001", "selection": "Team A", "odds": 2.0},
      {"bet_id": "bet_002", "selection": "Team B", "odds": 2.1},
      {"bet_id": "bet_003", "selection": "Draw", "odds": 3.0}
    ],
    "matched_time_window": "2026-03-24T14:05:00Z",
    "roi": "5.2%"
  },
  "created_at": "2026-03-24T14:05:12Z"
}
```

**SLA**: Latency 100-500ms (CEP processes on 1-min checkpoints)

---

### Layer 4: 人工審核

**Purpose**: Human-in-the-loop decision making for flagged proposals with clear prioritization and role-based permissions.

**Risk Proposal Lifecycle**:
1. Layer 2/3 creates proposal → status: OPEN
2. Reviewer picks up proposal → status: REVIEWING
3. Reviewer makes decision (APPROVED/REJECTED/ESCALATED) → status: terminal
4. Decision logged with full evidence trail

**Proposal Priority & SLA**:
| Priority | Definition | SLA | Auto-escalate |
|----------|-----------|-----|---------------|
| URGENT | Suspected account takeover, large fraud | 1 hour | Yes |
| HIGH | Arbitrage, device anomaly, geo violation | 2 hours | Yes |
| MEDIUM | Amount threshold flag, velocity warning | 24 hours | No |
| LOW | Suspicious pattern, low confidence | 48 hours | No |

**Role-Based Permissions** (3-tier):

**L1 Reviewer** (view + comment):
- View proposal details, evidence, player history
- Add internal comments/notes
- Cannot approve/reject
- Typical: junior analysts, data scientists

**L2 Reviewer** (approve/reject):
- View + comment permissions
- Approve proposal (player can proceed)
- Reject proposal (player action denied; account flagged)
- Typical: senior analysts, compliance officers

**L3 Escalation** (override + escalate):
- L2 permissions
- Override L2 decisions
- Escalate to legal/compliance team
- Manual account freeze/unfreeze
- Typical: compliance manager, legal counsel

**Database Schema**:
```sql
CREATE TABLE t_risk_proposal (
    proposal_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    player_id VARCHAR(50) NOT NULL,
    trigger_rule VARCHAR(100) NOT NULL,
    priority VARCHAR(20), -- URGENT, HIGH, MEDIUM, LOW
    status VARCHAR(20), -- OPEN, REVIEWING, APPROVED, REJECTED, ESCALATED
    reviewer_id VARCHAR(50),
    decision VARCHAR(20), -- APPROVED, REJECTED, ESCALATED
    decision_reason TEXT,
    evidence JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    decided_at TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES t_tenant(tenant_id),
    INDEX idx_status_priority (status, priority),
    INDEX idx_player_id (player_id)
);

CREATE TABLE t_risk_proposal_audit (
    audit_id BIGSERIAL PRIMARY KEY,
    proposal_id VARCHAR(36),
    reviewer_id VARCHAR(50),
    action VARCHAR(50), -- VIEW, COMMENT, APPROVE, REJECT, ESCALATE
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (proposal_id) REFERENCES t_risk_proposal(proposal_id)
);
```

**Withdrawal Decision Logic**:
```go
func (svc *ProposalService) CanApproveWithdrawal(ctx context.Context, playerID string) (bool, error) {
    // Check for any OPEN or REVIEWING proposals
    openProposals, err := svc.db.FindByPlayerAndStatus(ctx, playerID, []string{"OPEN", "REVIEWING"})
    if err != nil {
        return false, err
    }

    // If URGENT or HIGH priority proposals exist, withdrawal blocked
    for _, p := range openProposals {
        if p.Priority == "URGENT" || p.Priority == "HIGH" {
            return false, fmt.Errorf("pending high-priority proposal blocks withdrawal")
        }
    }

    // Find highest priority open proposal
    if len(openProposals) > 0 {
        sort.Slice(openProposals, func(i, j int) bool {
            return priorityOrder[openProposals[i].Priority] < priorityOrder[openProposals[j].Priority]
        })
        return false, fmt.Errorf("pending proposal %s requires review", openProposals[0].ProposalID)
    }

    return true, nil
}
```

---

### Layer 5: 提款延遲檢查

**Purpose**: Additional validation before withdrawal approval using 30-day lookback.

**Checks**:
1. **Pending Proposal Check**: Scan Layer 4 for any REVIEWING proposals
2. **Withdrawal Velocity**: Check withdrawal frequency in past 30 days
3. **Deposit-Withdrawal Ratio**: Ensure deposits exceed withdrawals (prevent layering)
4. **Account Age**: New accounts (< 7 days) may require additional verification

**Go Implementation**:
```go
func (svc *WithdrawalService) ValidateWithdrawalL5(ctx context.Context, req *WithdrawalRequest) (bool, string) {
    // Check pending proposals
    canApprove, err := svc.proposalSvc.CanApproveWithdrawal(ctx, req.PlayerID)
    if err != nil {
        return false, fmt.Sprintf("proposal_block: %v", err)
    }
    if !canApprove {
        return false, "pending_high_priority_proposal"
    }

    // Check 30-day withdrawal velocity
    withdrawals30d, err := svc.db.GetWithdrawals(ctx, req.PlayerID, time.Now().AddDate(0, 0, -30))
    if len(withdrawals30d) > 10 {
        return false, "excessive_withdrawal_frequency"
    }

    // Check deposit-withdrawal ratio
    deposits30d, err := svc.db.GetDeposits(ctx, req.PlayerID, time.Now().AddDate(0, 0, -30))
    depositTotal := sumAmounts(deposits30d)
    withdrawalTotal := sumAmounts(withdrawals30d) + req.Amount

    if withdrawalTotal > depositTotal * 1.5 { // Allow 50% more than deposited
        return false, "withdrawal_exceeds_deposits_ratio"
    }

    // Check account age
    account, err := svc.db.GetAccount(ctx, req.PlayerID)
    if time.Since(account.CreatedAt) < 7*24*time.Hour {
        // New account—require additional verification
        return false, "new_account_withdrawal_restriction"
    }

    return true, ""
}
```

---

## 6.3 資料模型

**Core Tables**:

### t_risk_rule
Rules engine configuration, loaded into LiteFlow at startup.
```sql
CREATE TABLE t_risk_rule (
    rule_id VARCHAR(50) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    rule_code VARCHAR(100) NOT NULL, -- VELOCITY_CHECK, AMOUNT_FLAG, etc.
    rule_type VARCHAR(50), -- VELOCITY, AMOUNT, DEVICE, BEHAVIOR, GEO
    liteflow_chain_id VARCHAR(100),
    parameters JSONB, -- {"threshold": 100, "window_minutes": 1, ...}
    enabled BOOLEAN DEFAULT TRUE,
    priority INT DEFAULT 100,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE (tenant_id, rule_code),
    INDEX idx_tenant_enabled (tenant_id, enabled)
);
```

### t_risk_proposal
Risk proposals for human review (detailed above).

### t_risk_score
Real-time risk dimension scores for analytics and reporting.
```sql
CREATE TABLE t_risk_score (
    event_id VARCHAR(50) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    player_id VARCHAR(50) NOT NULL,
    action_type VARCHAR(50), -- BET, DEPOSIT, WITHDRAWAL
    overall_score INT CHECK (overall_score >= 0 AND overall_score <= 100),
    dimensions JSONB, -- {"financial": 25, "behavioral": 20, "device": 15, ...}
    decision VARCHAR(20), -- AUTO_APPROVE, MANUAL_REVIEW, AUTO_REJECT
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_action (player_id, action_type, created_at DESC),
    INDEX idx_overall_score (overall_score)
);
```

### t_blacklist
Blacklist entries for Layer 1 fast-fail checks.
```sql
CREATE TABLE t_blacklist (
    blacklist_id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50), -- IP, DEVICE, PLAYER, PAYMENT
    value VARCHAR(255) NOT NULL,
    reason TEXT,
    tenant_id VARCHAR(50),
    added_by VARCHAR(50),
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    UNIQUE (type, value, tenant_id),
    INDEX idx_type_active (type, is_active),
    INDEX idx_expires (expires_at)
);
```

### t_liteflow_chain
LiteFlow rule chain definitions (versioned).
```sql
CREATE TABLE t_liteflow_chain (
    chain_id VARCHAR(100) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    chain_name VARCHAR(100),
    version INT DEFAULT 1,
    el_data TEXT NOT NULL, -- EL expression (LiteFlow syntax)
    application_name VARCHAR(50),
    enabled BOOLEAN DEFAULT TRUE,
    deployed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_tenant_name (tenant_id, chain_name)
);
```

---

## 6.4 七維風險評分

**Risk Score Dimensions** (weighted average):

| Dimension | Weight | Source | Components |
|-----------|--------|--------|------------|
| **Financial** | 25% | Transaction patterns | Velocity, amount spikes, deposit-withdrawal ratio |
| **Behavioral** | 20% | Betting patterns | Bet frequency, odds preference, sport selection variance |
| **Device** | 15% | Fingerprint anomalies | Hardware mismatch, OS changes, browser inconsistencies |
| **Geographic** | 15% | Location patterns | Rapid location changes, restricted jurisdictions |
| **Identity** | 10% | KYC verification | Document inconsistencies, address verification failures |
| **Social** | 10% | Multi-account relations | Shared devices, payments, IPs with other accounts |
| **Temporal** | 5% | Time patterns | Unusual activity hours, weekend patterns, seasonal |

**Calculation**:
```
Overall Score = (
    Financial_Score × 0.25 +
    Behavioral_Score × 0.20 +
    Device_Score × 0.15 +
    Geographic_Score × 0.15 +
    Identity_Score × 0.10 +
    Social_Score × 0.10 +
    Temporal_Score × 0.05
)
Capped to [0, 100]
```

**Risk Score Boundaries (SSOT - Source of Truth)**:

| Range | Action | SLA | Description |
|-------|--------|-----|-------------|
| **[0, 30)** | AUTO_APPROVE | Instant | Low risk; log only; automatic approval |
| **[30, 70)** | MANUAL_REVIEW | Layer 4 SLA | Medium risk; create risk proposal for review |
| **[70, 100]** | AUTO_REJECT | Instant | High risk; immediate action denial; account flag |

**Python FastAPI ML Integration** (for score calculation):
```python
from fastapi import FastAPI
import joblib
import numpy as np

app = FastAPI()
model = joblib.load("models/rf_fraud_classifier.pkl")

@app.post("/calculate-dimensions")
async def calculate_dimensions(player_features: dict) -> dict:
    """
    Input: {
        "financial_velocity": 0-100,
        "bet_frequency": 0-100,
        "device_anomaly": 0-100,
        "location_velocity": 0-100,
        "kyc_completeness": 0-100,
        "multi_account_correlation": 0-100,
        "unusual_hours": 0-100
    }

    Returns: {"financial": 25, "behavioral": 20, ...}
    """
    features = np.array([
        player_features["financial_velocity"],
        player_features["bet_frequency"],
        player_features["device_anomaly"],
        player_features["location_velocity"],
        player_features["kyc_completeness"],
        player_features["multi_account_correlation"],
        player_features["unusual_hours"]
    ])

    dimensions = {
        "financial": min(100, player_features["financial_velocity"]),
        "behavioral": min(100, player_features["bet_frequency"]),
        "device": min(100, player_features["device_anomaly"]),
        "geographic": min(100, player_features["location_velocity"]),
        "identity": min(100, 100 - player_features["kyc_completeness"]),
        "social": min(100, player_features["multi_account_correlation"]),
        "temporal": min(100, player_features["unusual_hours"])
    }

    return dimensions

@app.post("/predict-fraud")
async def predict_fraud(player_id: str, dimensions: dict) -> dict:
    """
    ML model prediction with AUC 0.729
    """
    features = np.array([
        dimensions["financial"],
        dimensions["behavioral"],
        dimensions["device"],
        dimensions["geographic"],
        dimensions["identity"],
        dimensions["social"],
        dimensions["temporal"]
    ]).reshape(1, -1)

    probability = model.predict_proba(features)[0][1]

    return {
        "fraud_probability": probability,
        "recommended_action": "AUTO_REJECT" if probability > 0.7 else "MANUAL_REVIEW" if probability > 0.3 else "AUTO_APPROVE"
    }
```

---

## 6.5 詐欺偵測模式

### Multi-Account Detection (Device Fingerprinting)

**Algorithm**:
1. Extract FingerprintJS fingerprint from player session
2. Query t_player_fingerprint table for matching fingerprints
3. Cross-reference with t_account to find linked accounts
4. If 5+ accounts share fingerprint: FLAG all for review

**Schema**:
```sql
CREATE TABLE t_player_fingerprint (
    fingerprint_id VARCHAR(100) PRIMARY KEY,
    player_id VARCHAR(50) NOT NULL,
    device_hash VARCHAR(256) NOT NULL,
    fingerprint_data JSONB,
    confidence NUMERIC(3,2), -- 0.95 = 95% confidence
    first_seen TIMESTAMP,
    last_seen TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES t_account(player_id),
    INDEX idx_device_hash (device_hash),
    INDEX idx_player_id (player_id)
);

CREATE TABLE t_multi_account_detection (
    detection_id BIGSERIAL PRIMARY KEY,
    device_hash VARCHAR(256),
    account_count INT,
    player_ids TEXT[], -- Array of linked player IDs
    detected_at TIMESTAMP,
    proposal_ids TEXT[], -- Link to risk proposals
    INDEX idx_device_hash (device_hash)
);
```

**Implementation**:
```go
func (svc *FraudService) DetectMultiAccounts(ctx context.Context, deviceHash string) ([]*Account, error) {
    // Find all accounts with same device fingerprint
    accounts, err := svc.db.GetAccountsByDeviceHash(ctx, deviceHash)
    if err != nil {
        return nil, err
    }

    if len(accounts) >= 5 {
        // Create FLAG proposals for all accounts
        for _, acc := range accounts {
            proposal := &RiskProposal{
                PlayerID: acc.PlayerID,
                TriggerRule: "multi_account_detection_device",
                Priority: "HIGH",
                Status: "OPEN",
                Evidence: map[string]interface{}{
                    "linked_accounts": len(accounts),
                    "device_hash": deviceHash,
                    "linked_player_ids": extractPlayerIDs(accounts),
                },
            }
            svc.db.CreateProposal(ctx, proposal)
        }
    }

    return accounts, nil
}
```

### Self-Exclusion Evasion Detection

**Data Sources**:
- GAMSTOP (UK GamCare, Betwise, Bet Blocker, Gamban)
- CRUKS (Cruising, UK Self-Exclusion Scheme)
- Internal t_self_exclusion table

**Detection Logic**:
```sql
SELECT ap.player_id, ap.device_hash, ap.ip_address, ap.payment_hash
FROM t_account_profile ap
WHERE ap.device_hash IN (
    SELECT device_hash FROM t_self_exclusion_evasion_index
)
   OR ap.ip_address IN (SELECT ip FROM t_self_exclusion_evasion_index)
   OR ap.payment_hash IN (SELECT payment_hash FROM t_self_exclusion_evasion_index)
LIMIT 100; -- Batch process
```

**Action**: Immediate BLOCK + escalation to compliance team.

### ML Model (Python - Random Forest)

**Model Details**:
- Algorithm: Random Forest Classifier
- AUC: 0.729 (good discrimination)
- Features: 50 transaction and behavioral features
- Retraining: Monthly with new labeled fraud/non-fraud data
- Serving: FastAPI endpoint, called from Go risk service with 50ms timeout

**Feature Engineering** (sample):
```python
def engineer_features(player_id: str, lookback_days: int = 30) -> dict:
    features = {}

    # Transaction velocity
    transactions = get_transactions(player_id, lookback_days)
    features["transaction_count"] = len(transactions)
    features["transaction_rate_per_day"] = len(transactions) / lookback_days

    # Amount distribution
    amounts = [t.amount for t in transactions]
    features["amount_mean"] = np.mean(amounts)
    features["amount_std"] = np.std(amounts)
    features["amount_max"] = max(amounts)
    features["amount_min"] = min(amounts)

    # Device diversity
    devices = get_unique_devices(player_id, lookback_days)
    features["device_count"] = len(devices)
    features["device_changes_per_day"] = (len(devices) - 1) / lookback_days

    # Geo patterns
    locations = get_unique_locations(player_id, lookback_days)
    features["location_count"] = len(locations)
    features["location_change_velocity"] = max_distance_between_locations(locations) / lookback_days

    # Betting patterns
    bets = get_bets(player_id, lookback_days)
    features["total_bets"] = len(bets)
    features["avg_odds"] = np.mean([b.odds for b in bets])
    features["odds_variance"] = np.std([b.odds for b in bets])

    return features

# Model training (monthly batch job)
def retrain_model():
    labeled_data = get_monthly_labeled_data()  # From fraud investigation outcomes
    X = pd.DataFrame([engineer_features(p) for p in labeled_data["player_ids"]])
    y = labeled_data["fraud_labels"]  # 0=non-fraud, 1=fraud

    model = RandomForestClassifier(n_estimators=100, max_depth=10)
    model.fit(X, y)

    # Evaluation
    y_pred = model.predict(X)
    auc = roc_auc_score(y, model.predict_proba(X)[:, 1])
    print(f"Model AUC: {auc}")

    joblib.dump(model, "models/rf_fraud_classifier.pkl")
```

---

## 6.6 AML 合規

**Regulatory Requirements**:
- FinCEN regulations (US)
- FCA handbook (UK)
- OASIS (Austria)
- Local jurisdiction regulations per player location

### SAR (Suspicious Activity Report) Triggers

Automatic SAR filing (via third-party AML vendor):

1. **Single Transaction > $10,000**
   - Immediate escalation to compliance
   - Evidence: transaction details, player KYC, source of funds

2. **Structured Deposits** (deposits just under $10k threshold)
   - Multiple deposits < $9,999 within 24-hour window
   - Pattern detection via SQL:
   ```sql
   SELECT player_id, COUNT(*) as deposit_count, SUM(amount) as total
   FROM t_transaction
   WHERE action = 'DEPOSIT'
     AND amount < 10000
     AND created_at >= NOW() - INTERVAL '24 hours'
   GROUP BY player_id
   HAVING COUNT(*) >= 3
     AND SUM(amount) >= 20000;
   ```

3. **Rapid Deposit-Withdrawal** (no play)
   - Deposit > $5,000
   - Withdrawal of 90%+ within 24 hours
   - No significant betting activity
   - Action: SAR + account review

4. **Source of Funds Mismatch**
   - Player claims income < $50k/year
   - Deposits > $500k in 30 days
   - Address/occupation inconsistencies
   - Action: Enhanced Due Diligence (EDD)

### CDD/EDD Process

**Customer Due Diligence (CDD)** - Standard (L1 KYC):
- All players at account opening
- Collect: Name, DOB, address, phone, email
- Verify: Government ID (passport/license)
- Perform: Enhanced ID verification (Jumio or similar)

**Enhanced Due Diligence (EDD)**:
- Triggered by: High-risk triggers, SAR patterns, PEP match
- Additional: Source of funds declaration, beneficial owner disclosure, business purpose
- Risk classification: HIGH, MEDIUM, LOW
- Ongoing monitoring: Monthly review for HIGH-risk players

**Ongoing Monitoring** (Flink CEP):
```java
// Monitor for SAR triggers in real-time
Pattern<TransactionEvent, ?> sarPattern = Pattern
    .<TransactionEvent>begin("large_transaction")
    .where(e -> e.getAmount() > 10000 && e.getType() == DEPOSIT)
    .within(Time.minutes(5));

Pattern<TransactionEvent, ?> structuredPattern = Pattern
    .<TransactionEvent>begin("deposit1")
    .where(e -> e.getAmount() < 10000)
    .followedBy("deposit2")
    .where((e, ctx) -> {
        TransactionEvent dep1 = ctx.getEventsForPattern("deposit1").iterator().next();
        return e.getPlayerId().equals(dep1.getPlayerId())
            && e.getAmount() < 10000;
    })
    // ... more deposits, total > $20k
    .within(Time.hours(24));
```

**Outcome**:
- Automatic SAR generation + filing with FinCEN
- Account flagged for compliance review
- Withdrawal restrictions until review complete

---

## 6.7 UKGC 可負擔性評估

**Regulation**: UKGC's safer gambling rules require affordability checks for depositing players.

**Three-Tier Assessment**:

### Tier 1: Basic Warning (£125-500 in 30 days)
- Automated warning message at deposit confirmation
- Message: "Please ensure you can afford this deposit."
- No action required; tracking only

### Tier 2: Enhanced (£500-2,000 in 30 days)
- Self-declaration form (player confirms affordability)
- Questions: "Can you afford this deposit?", "Has gambling affected your finances?"
- If answered NO → Deposit blocked, escalated to support
- Evidence: Form responses stored in t_affordability_assessment

### Tier 3: Full Assessment (>£2,000 in 30 days)
- Open Banking API integration (Plaid or TrueLayer)
- Credit bureau check (Experian, Equifax)
- Automated analysis: Income vs. deposit ratio, credit score, payment history
- Decline if: Income < £10k annually and deposit requested
- Decline if: Multiple recent missed payments

**Implementation** (Go + Python):

```go
func (svc *AffordabilityService) CheckAffordability(ctx context.Context, playerID string, depositAmount float64) (*AffordabilityDecision, error) {
    // Get 30-day deposit rolling total
    total30d, err := svc.db.Get30DayDeposits(ctx, playerID)
    if err != nil {
        return nil, err
    }

    total := total30d + depositAmount

    if total < 125 {
        return &AffordabilityDecision{Tier: "NONE", Decision: "APPROVE"}, nil
    }

    if total < 500 {
        return &AffordabilityDecision{Tier: "BASIC", Decision: "WARN"}, nil
    }

    if total < 2000 {
        // Tier 2: Self-declaration required
        assessment, err := svc.db.GetLatestAffordabilityAssessment(ctx, playerID)
        if assessment == nil || time.Since(assessment.CreatedAt) > 24*time.Hour {
            return &AffordabilityDecision{Tier: "ENHANCED", Decision: "REQUIRE_FORM"}, nil
        }

        if assessment.CanAfford == false {
            return &AffordabilityDecision{Tier: "ENHANCED", Decision: "REJECT"}, nil
        }
    }

    // Tier 3: Full assessment via Open Banking + credit bureau
    openBankingResult, err := svc.openBankingSvc.CheckIncome(ctx, playerID)
    creditResult, err := svc.creditBureau.GetScore(ctx, playerID)

    if openBankingResult.MonthlyIncome < 833 && depositAmount > 500 {
        return &AffordabilityDecision{Tier: "FULL", Decision: "REJECT", Reason: "insufficient_income"}, nil
    }

    if creditResult.Score < 600 {
        return &AffordabilityDecision{Tier: "FULL", Decision: "REJECT", Reason: "credit_score_too_low"}, nil
    }

    return &AffordabilityDecision{Tier: "FULL", Decision: "APPROVE"}, nil
}
```

**Schema**:
```sql
CREATE TABLE t_affordability_assessment (
    assessment_id BIGSERIAL PRIMARY KEY,
    player_id VARCHAR(50) NOT NULL,
    tier VARCHAR(20), -- BASIC, ENHANCED, FULL
    assessment_date TIMESTAMP,
    can_afford BOOLEAN,
    form_responses JSONB,
    open_banking_income NUMERIC(10, 2),
    credit_score INT,
    decision VARCHAR(20), -- APPROVE, REJECT
    created_at TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES t_account(player_id),
    INDEX idx_player_date (player_id, assessment_date DESC)
);
```

---

## 6.8 審計與報表

**Audit Logging** (all risk decisions):

```sql
CREATE TABLE t_risk_audit_trail (
    audit_id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    player_id VARCHAR(50),
    action_type VARCHAR(100), -- PROPOSAL_CREATED, PROPOSAL_APPROVED, SAR_FILED, etc.
    actor_id VARCHAR(50), -- User ID (for human actions) or "system"
    actor_type VARCHAR(20), -- USER, SYSTEM
    details JSONB, -- Full context
    evidence JSONB, -- Supporting data
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_action (player_id, action_type),
    INDEX idx_audit_date (created_at)
);
```

**Retention Policy**:
- 7 years: AML-required retention (SAR, CDD, EDD documents)
- 2 years: Risk proposals and audit trail (after account closure)
- Encrypted at rest (AES-256)

**Audit Trail Entry Example**:
```json
{
  "audit_id": 12345,
  "tenant_id": "tenant_abc",
  "player_id": "player_xyz",
  "action_type": "PROPOSAL_APPROVED",
  "actor_id": "user_analytics_01",
  "actor_type": "USER",
  "details": {
    "proposal_id": "cep_20260324_001234",
    "decision": "APPROVED",
    "reason": "Legitimate arbitrage opportunity - known pattern in sports betting"
  },
  "evidence": {
    "rule_triggered": "arbitrage_detection",
    "bets_analyzed": 3,
    "roi_calculated": "5.2%",
    "comment": "Player has clean history; similar pattern previously approved"
  },
  "created_at": "2026-03-24T15:30:45Z"
}
```

**Monthly Risk Reports** (auto-generated):
- Total proposals created: 1,234
- Approved: 890 (72%)
- Rejected: 234 (19%)
- Escalated: 110 (9%)
- Average SLA compliance: 98%
- False positive rate: 3.2%
- SAR filings: 45 (100% of triggers filed)

---

## 6.9 監控指標

**Performance SLAs**:
| Layer | Metric | Target | Typical |
|-------|--------|--------|---------|
| L1 | P99 latency | < 10ms | 2-5ms |
| L2 | P99 latency | < 50ms | 20-40ms |
| L3 | Pattern match latency | < 500ms | 100-300ms |
| L4 | SLA compliance | > 95% | 97% |
| L5 | Validation latency | < 100ms | 30-50ms |

**Quality Metrics**:
- **False Positive Rate**: < 5% (proposal rejected by human review)
- **False Negative Rate**: < 1% (fraud not flagged by any layer)
- **Fraud Detection Rate**: > 95% (of confirmed fraud cases)
- **AML SAR Filing Rate**: 100% (all triggers filed)

**Alerting**:
```
IF P99(Layer1_Latency) > 10ms → Page on-call
IF FalsePositiveRate > 5% in 24h → Escalate to ML team
IF SLA_Compliance < 95% in 7d → Review proposal workflow
IF Fraud_Undetected > 1% → Retrain ML model
```

**Monitoring Stack**:
- Prometheus: Metrics collection
- Grafana: Dashboards (real-time + historical)
- AlertManager: Page on-call for critical breaches
- ELK Stack: Log aggregation (Flink CEP logs, Go service logs)

---

## 6.10 串謀偵測 Flink CEP (Collusion Detection)

> **業務規則來源**: Ch6 需求 §6.10 串謀偵測

### Baccarat Hedging Detection (5-min window)

同桌多帳戶分別押莊/閒，通過 IP/設備/支付關聯分析識別。

```java
/**
 * Detect baccarat hedging: two accounts at same table betting opposite sides.
 * Cross-reference with device/IP/payment correlation from t_player_fingerprint.
 */
Pattern<BetEvent, ?> baccaratHedgePattern = Pattern
    .<BetEvent>begin("banker_bet")
    .where(e -> e.getGameType().equals("LIVE")
        && e.getSubType().equals("BACCARAT")
        && e.getSelection().equals("BANKER"))
    .followedBy("player_bet")
    .where((e, ctx) -> {
        BetEvent bankerBet = ctx.getEventsForPattern("banker_bet").iterator().next();
        return e.getGameType().equals("LIVE")
            && e.getSubType().equals("BACCARAT")
            && e.getSelection().equals("PLAYER")
            && e.getTableId().equals(bankerBet.getTableId())
            && !e.getPlayerId().equals(bankerBet.getPlayerId())
            && correlationService.areCorrelated(
                e.getPlayerId(), bankerBet.getPlayerId()); // IP/device/payment check
    })
    .within(Time.minutes(5));

// Action: CRITICAL → immediate freeze of both accounts
PatternStream<BetEvent> hedgeStream = CEP.pattern(betStream, baccaratHedgePattern);
hedgeStream.select(new PatternSelectFunction<BetEvent, RiskProposal>() {
    @Override
    public RiskProposal select(Map<String, List<BetEvent>> pattern) {
        BetEvent banker = pattern.get("banker_bet").get(0);
        BetEvent player = pattern.get("player_bet").get(0);
        return RiskProposal.builder()
            .triggerRule("baccarat_hedge_collusion")
            .priority("URGENT")
            .playerIds(List.of(banker.getPlayerId(), player.getPlayerId()))
            .evidence(Map.of(
                "table_id", banker.getTableId(),
                "banker_bet", banker.getBetId(),
                "player_bet", player.getBetId(),
                "correlation_type", correlationService.getCorrelationType(
                    banker.getPlayerId(), player.getPlayerId())
            ))
            .build();
    }
});
```

### Poker Soft Play / Chip Dumping (30-min session window)

```java
/**
 * Detect coordinated fold/raise patterns between correlated players at same table.
 * Trigger: Win/Loss ratio between two players > 80:20 over 10+ hands.
 */
Pattern<PokerHandEvent, ?> chipDumpPattern = Pattern
    .<PokerHandEvent>begin("hand_result")
    .where(e -> e.getGameType().equals("POKER"))
    .timesOrMore(10) // At least 10 hands
    .within(Time.minutes(30));

// Process function calculates pairwise win/loss ratios
chipDumpStream.process(new PatternProcessFunction<PokerHandEvent, RiskProposal>() {
    @Override
    public void processMatch(Map<String, List<PokerHandEvent>> match,
                             Context ctx, Collector<RiskProposal> out) {
        List<PokerHandEvent> hands = match.get("hand_result");

        // Group by table, then compute pairwise win/loss ratios
        Map<String, Map<String, WinLossStats>> pairStats = computePairwiseStats(hands);

        for (var entry : pairStats.entrySet()) {
            for (var pair : entry.getValue().entrySet()) {
                WinLossStats stats = pair.getValue();
                if (stats.getWinRatio() > 0.80 && stats.getHandCount() >= 10) {
                    out.collect(RiskProposal.builder()
                        .triggerRule("poker_chip_dumping")
                        .priority("URGENT")
                        .playerIds(List.of(stats.getWinnerId(), stats.getLoserId()))
                        .evidence(Map.of(
                            "table_id", entry.getKey(),
                            "hands_analyzed", stats.getHandCount(),
                            "win_ratio", stats.getWinRatio(),
                            "total_transferred", stats.getNetTransfer()
                        ))
                        .build());
                }
            }
        }
    }
});
```

### Result Sharing Detection (action timing analysis)

```java
/**
 * Detect suspiciously synchronized action timing at live dealer tables.
 * Trigger: 2+ players with action time difference < 1 second consistently.
 */
Pattern<LiveActionEvent, ?> syncActionPattern = Pattern
    .<LiveActionEvent>begin("action1")
    .where(e -> e.getGameType().equals("LIVE"))
    .followedBy("action2")
    .where((e, ctx) -> {
        LiveActionEvent a1 = ctx.getEventsForPattern("action1").iterator().next();
        return e.getTableId().equals(a1.getTableId())
            && !e.getPlayerId().equals(a1.getPlayerId())
            && Math.abs(e.getTimestamp() - a1.getTimestamp()) < 1000; // < 1s
    })
    .timesOrMore(5) // 5+ synchronized actions
    .within(Time.minutes(15));

// Action: HIGH → suspend withdrawals + human review
```

### Collusion CorrelationService

```java
@Service
public class CollusionCorrelationService {

    /**
     * Check if two players are correlated via device, IP, or payment method.
     * Uses t_player_fingerprint and t_multi_account_detection.
     */
    public boolean areCorrelated(String playerA, String playerB) {
        // Check shared device fingerprint
        boolean sharedDevice = fingerprintRepo.haveSharedDevice(playerA, playerB);
        // Check shared IP (persistent, not one-time)
        boolean sharedIP = sessionRepo.havePersistentSharedIP(playerA, playerB, 3); // 3+ sessions
        // Check shared payment method
        boolean sharedPayment = paymentRepo.haveSharedPaymentMethod(playerA, playerB);

        return sharedDevice || (sharedIP && sharedPayment);
    }

    public String getCorrelationType(String playerA, String playerB) {
        List<String> types = new ArrayList<>();
        if (fingerprintRepo.haveSharedDevice(playerA, playerB)) types.add("DEVICE");
        if (sessionRepo.havePersistentSharedIP(playerA, playerB, 3)) types.add("IP");
        if (paymentRepo.haveSharedPaymentMethod(playerA, playerB)) types.add("PAYMENT");
        return String.join("+", types);
    }
}
```

---

## 6.11 Bot 偵測服務 (Bot Detection)

> **業務規則來源**: Ch6 需求 §6.11 Bot 偵測規範 (6 維度)

### BotDetectionService (Go)

```go
package risk

import (
    "context"
    "math"
    "time"
)

// BotDetectionResult aggregates 6-dimension analysis.
type BotDetectionResult struct {
    MouseTrajectory   DimensionScore `json:"mouse_trajectory"`
    ClickFrequency    DimensionScore `json:"click_frequency"`
    APICallPattern    DimensionScore `json:"api_call_pattern"`
    DecisionTime      DimensionScore `json:"decision_time"`
    SessionDuration   DimensionScore `json:"session_duration"`
    AntiAutomation    DimensionScore `json:"anti_automation"`
    OverallDecision   string         `json:"overall_decision"` // PASS, FLAG, HIGH, BLOCK
    TriggeredDimensions []string     `json:"triggered_dimensions"`
}

type DimensionScore struct {
    Score     float64 `json:"score"`
    Level     string  `json:"level"` // PASS, FLAG, HIGH, BLOCK
    Detail    string  `json:"detail"`
}

type BotDetectionService struct {
    redis     *redis.Client
    sessionDB SessionRepository
}

// Analyze performs 6-dimension bot detection.
// Called from Layer 2 LiteFlow chain as BotDetectionCmp.
func (svc *BotDetectionService) Analyze(ctx context.Context, playerID string,
    sessionData *SessionBehaviorData) (*BotDetectionResult, error) {

    result := &BotDetectionResult{}
    var triggered []string

    // Dimension 1: Mouse Trajectory (straight line ratio > 90% = non-human)
    if sessionData.MouseEvents != nil {
        straightRatio := calculateStraightLineRatio(sessionData.MouseEvents)
        if straightRatio > 0.90 {
            result.MouseTrajectory = DimensionScore{
                Score: straightRatio * 100, Level: "FLAG",
                Detail: fmt.Sprintf("Straight line ratio: %.1f%%", straightRatio*100),
            }
            triggered = append(triggered, "MOUSE_TRAJECTORY")
        } else {
            result.MouseTrajectory = DimensionScore{Score: straightRatio * 100, Level: "PASS"}
        }
    }

    // Dimension 2: Click Frequency (stddev < 50ms = machine precision)
    if len(sessionData.ClickIntervals) > 10 {
        stddev := calculateStdDev(sessionData.ClickIntervals)
        if stddev < 50 { // < 50ms stddev → machine-like regularity
            result.ClickFrequency = DimensionScore{
                Score: 100 - stddev, Level: "HIGH",
                Detail: fmt.Sprintf("Click interval stddev: %.1fms", stddev),
            }
            triggered = append(triggered, "CLICK_FREQUENCY")
        }
    }

    // Dimension 3: API Call Pattern (non-browser UA / missing cookies)
    ua := sessionData.UserAgent
    if isNonBrowserUA(ua) || !sessionData.HasCookieSupport {
        result.APICallPattern = DimensionScore{
            Score: 100, Level: "BLOCK",
            Detail: fmt.Sprintf("UA: %s, Cookies: %v", ua, sessionData.HasCookieSupport),
        }
        triggered = append(triggered, "API_CALL_PATTERN")
    }

    // Dimension 4: Bet Decision Time (sustained < 200ms = no reading time)
    if len(sessionData.BetDecisionTimes) > 5 {
        avgDecision := average(sessionData.BetDecisionTimes)
        if avgDecision < 200 { // < 200ms average
            result.DecisionTime = DimensionScore{
                Score: (200 - avgDecision) / 2, Level: "FLAG",
                Detail: fmt.Sprintf("Avg decision time: %.0fms", avgDecision),
            }
            triggered = append(triggered, "DECISION_TIME")
        }
    }

    // Dimension 5: Session Duration (> 12 hours continuous = non-human)
    sessionDuration := time.Since(sessionData.SessionStart)
    if sessionDuration > 12*time.Hour && sessionData.NoBreaksDetected {
        result.SessionDuration = DimensionScore{
            Score: float64(sessionDuration.Hours()) / 24 * 100, Level: "FLAG",
            Detail: fmt.Sprintf("Continuous session: %.1f hours", sessionDuration.Hours()),
        }
        triggered = append(triggered, "SESSION_DURATION")
    }

    // Dimension 6: Anti-Automation Tool Detection (WebDriver/Selenium/Puppeteer)
    if sessionData.WebDriverDetected || sessionData.SeleniumDetected ||
        sessionData.PuppeteerDetected {
        result.AntiAutomation = DimensionScore{
            Score: 100, Level: "BLOCK",
            Detail: fmt.Sprintf("Detected: WebDriver=%v, Selenium=%v, Puppeteer=%v",
                sessionData.WebDriverDetected, sessionData.SeleniumDetected,
                sessionData.PuppeteerDetected),
        }
        triggered = append(triggered, "ANTI_AUTOMATION")
    }

    result.TriggeredDimensions = triggered

    // Overall decision: take highest severity
    result.OverallDecision = resolveOverallDecision(result)

    return result, nil
}

// resolveOverallDecision applies escalation rules from requirements §6.11:
// - Any BLOCK → BLOCK (immediate session kill + review queue)
// - Any HIGH → HIGH (CAPTCHA challenge + human review)
// - 3+ FLAG → HIGH (accumulated flags escalate)
// - Any FLAG → FLAG (monitor 7 days)
// - Otherwise → PASS
func resolveOverallDecision(r *BotDetectionResult) string {
    dimensions := []DimensionScore{
        r.MouseTrajectory, r.ClickFrequency, r.APICallPattern,
        r.DecisionTime, r.SessionDuration, r.AntiAutomation,
    }

    flagCount := 0
    for _, d := range dimensions {
        switch d.Level {
        case "BLOCK":
            return "BLOCK"
        case "HIGH":
            return "HIGH"
        case "FLAG":
            flagCount++
        }
    }

    if flagCount >= 3 {
        return "HIGH"
    }
    if flagCount > 0 {
        return "FLAG"
    }
    return "PASS"
}
```

### Bot Detection Actions

```go
func (svc *BotDetectionService) ExecuteAction(ctx context.Context,
    playerID string, result *BotDetectionResult) error {

    switch result.OverallDecision {
    case "BLOCK":
        // Immediate session termination
        svc.sessionMgr.KillSession(ctx, playerID)
        // Add to review queue
        svc.proposalSvc.CreateProposal(ctx, &RiskProposal{
            PlayerID:    playerID,
            TriggerRule: "bot_detection_block",
            Priority:    "HIGH",
            Evidence:    toJSON(result),
        })
        // Increment 7-day flag counter
        svc.redis.Incr(ctx, fmt.Sprintf("bot:flags:%s", playerID))
        svc.redis.Expire(ctx, fmt.Sprintf("bot:flags:%s", playerID), 7*24*time.Hour)

    case "HIGH":
        // Trigger CAPTCHA challenge
        svc.captchaSvc.TriggerChallenge(ctx, playerID)
        // Create review proposal
        svc.proposalSvc.CreateProposal(ctx, &RiskProposal{
            PlayerID:    playerID,
            TriggerRule: "bot_detection_high",
            Priority:    "MEDIUM",
            Evidence:    toJSON(result),
        })

    case "FLAG":
        // Monitor for 7 days; accumulate 3+ flags → escalate to HIGH
        flagKey := fmt.Sprintf("bot:flags:%s", playerID)
        count, _ := svc.redis.Incr(ctx, flagKey).Result()
        svc.redis.Expire(ctx, flagKey, 7*24*time.Hour)

        if count >= 3 {
            // Escalate
            svc.ExecuteAction(ctx, playerID, &BotDetectionResult{
                OverallDecision: "HIGH",
                TriggeredDimensions: result.TriggeredDimensions,
            })
        }
    }

    return nil
}
```

---

## 6.12 風控規則治理服務 (Config-Driven Governance)

> **業務規則來源**: Ch6 需求 §6.12 風控規則治理

### Maker-Checker + Canary + Rollback 工作流

```
┌────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│ 1. Maker   │───>│ 2. Checker   │───>│ 3. Canary    │───>│ 4. Full      │
│ 提交變更   │    │ 審批 (雙人)  │    │ 10% 流量     │    │ 全量生效     │
│            │    │              │    │ 24h 觀察     │    │              │
└────────────┘    └──────┬───────┘    └──────┬───────┘    └──────────────┘
                         │                    │
                    REJECT ↓              ANOMALY ↓
                  ┌──────────────┐    ┌──────────────┐
                  │ 變更駁回     │    │ 5. Rollback  │
                  │ 記入審計     │    │ 一鍵回滾     │
                  └──────────────┘    └──────────────┘
```

### t_rule_change_request

```sql
CREATE TABLE t_rule_change_request (
    change_id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    rule_id VARCHAR(50) NOT NULL REFERENCES t_risk_rule(rule_id),
    change_type ENUM('CREATE', 'UPDATE', 'DELETE', 'TOGGLE') NOT NULL,
    old_parameters JSONB,           -- Snapshot before change
    new_parameters JSONB,           -- Proposed new values
    impact_assessment TEXT,         -- Maker's impact analysis
    status ENUM('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'CANARY', 'ACTIVE', 'ROLLED_BACK', 'REJECTED') DEFAULT 'DRAFT',
    maker_id VARCHAR(50) NOT NULL,  -- Who proposed
    checker_id VARCHAR(50),         -- Who approved
    checker2_id VARCHAR(50),        -- Second approver (for CRITICAL rules)
    canary_started_at TIMESTAMP,
    canary_traffic_pct INT DEFAULT 10,
    canary_metrics JSONB,           -- Observed metrics during canary
    activated_at TIMESTAMP,
    rolled_back_at TIMESTAMP,
    rolled_back_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_rule (rule_id)
);
```

### RuleGovernanceService

```java
@Service
public class RuleGovernanceService {

    /**
     * Step 1: Maker submits change request with impact assessment.
     */
    public RuleChangeRequest submitChange(String makerId, String ruleId,
                                          JsonNode newParameters, String impactAssessment) {
        RiskRule currentRule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new RuleNotFoundException(ruleId));

        RuleChangeRequest request = RuleChangeRequest.builder()
            .changeId(UUID.randomUUID().toString())
            .tenantId(currentRule.getTenantId())
            .ruleId(ruleId)
            .changeType(ChangeType.UPDATE)
            .oldParameters(currentRule.getParameters())
            .newParameters(newParameters)
            .impactAssessment(impactAssessment)
            .status(ChangeStatus.PENDING_APPROVAL)
            .makerId(makerId)
            .build();

        changeRequestRepository.save(request);

        // Notify checkers
        notificationService.notifyRoleGroup("RISK_CHECKER",
            "Rule change pending approval: " + ruleId);

        auditLog.record("RULE_CHANGE_SUBMITTED", makerId, ruleId, newParameters);

        return request;
    }

    /**
     * Step 2: Checker approves (Maker-Checker dual approval).
     * Maker cannot approve own change.
     */
    public void approveChange(String checkerId, String changeId) {
        RuleChangeRequest request = changeRequestRepository.findById(changeId)
            .orElseThrow();

        // Maker cannot be checker
        if (request.getMakerId().equals(checkerId)) {
            throw new GovernanceException("Maker cannot approve own change");
        }

        // Check if second approval needed (for CRITICAL rules)
        RiskRule rule = ruleRepository.findById(request.getRuleId()).orElseThrow();
        boolean needsDoubleApproval = rule.getPriority() < 10; // High-priority rules

        if (request.getCheckerId() == null) {
            request.setCheckerId(checkerId);
            if (needsDoubleApproval) {
                // Needs second checker
                notificationService.notifyRoleGroup("RISK_CHECKER",
                    "Rule change needs second approval: " + changeId);
                changeRequestRepository.save(request);
                return;
            }
        } else if (needsDoubleApproval && request.getChecker2Id() == null) {
            if (request.getCheckerId().equals(checkerId)) {
                throw new GovernanceException("Same checker cannot approve twice");
            }
            request.setChecker2Id(checkerId);
        }

        // All approvals collected → start canary
        request.setStatus(ChangeStatus.CANARY);
        request.setCanaryStartedAt(Instant.now());
        changeRequestRepository.save(request);

        // Deploy canary (10% traffic)
        deployCanary(request);

        auditLog.record("RULE_CHANGE_APPROVED", checkerId, changeId, null);
    }

    /**
     * Step 3: Deploy canary — 10% traffic uses new rule parameters.
     */
    private void deployCanary(RuleChangeRequest request) {
        // Store canary config in Redis for LiteFlow to pick up
        String canaryKey = String.format("rule:canary:%s:%s",
            request.getTenantId(), request.getRuleId());

        CanaryConfig config = CanaryConfig.builder()
            .changeId(request.getChangeId())
            .newParameters(request.getNewParameters())
            .trafficPct(request.getCanaryTrafficPct()) // default 10
            .startedAt(Instant.now())
            .build();

        redisTemplate.opsForValue().set(canaryKey, JsonUtil.toJson(config),
            Duration.ofHours(25)); // Slightly > 24h observation
    }

    /**
     * Step 3.5: LiteFlow reads canary config to decide which params to use.
     * Called by each LiteFlow component before evaluation.
     */
    public JsonNode getEffectiveParameters(String tenantId, String ruleId,
                                           String requestHash) {
        String canaryKey = String.format("rule:canary:%s:%s", tenantId, ruleId);
        String canaryJson = redisTemplate.opsForValue().get(canaryKey);

        if (canaryJson != null) {
            CanaryConfig config = JsonUtil.fromJson(canaryJson, CanaryConfig.class);
            // Deterministic routing: hash(requestId) % 100 < trafficPct → canary
            int bucket = Math.abs(requestHash.hashCode() % 100);
            if (bucket < config.getTrafficPct()) {
                return config.getNewParameters(); // Use canary parameters
            }
        }

        // Default: use current production parameters
        return ruleRepository.findById(ruleId).orElseThrow().getParameters();
    }

    /**
     * Step 4: Promote canary to full after 24h observation (or auto if metrics OK).
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void evaluateCanaryMetrics() {
        List<RuleChangeRequest> canaries = changeRequestRepository
            .findByStatus(ChangeStatus.CANARY);

        for (RuleChangeRequest canary : canaries) {
            Duration elapsed = Duration.between(canary.getCanaryStartedAt(), Instant.now());

            if (elapsed.toHours() >= 24) {
                CanaryMetrics metrics = collectCanaryMetrics(canary);
                canary.setCanaryMetrics(JsonUtil.toJson(metrics));

                if (metrics.getFalsePositiveRate() <= 0.05 &&
                    metrics.getLatencyP99() <= getBaselineLatency(canary.getRuleId()) * 1.1) {
                    // Metrics acceptable → promote to full
                    promoteToFull(canary);
                } else {
                    // Metrics degraded → auto-rollback
                    rollback(canary.getChangeId(), "SYSTEM",
                        "Canary metrics degraded: FPR=" + metrics.getFalsePositiveRate());
                }
            }
        }
    }

    private void promoteToFull(RuleChangeRequest request) {
        // Update actual rule parameters
        RiskRule rule = ruleRepository.findById(request.getRuleId()).orElseThrow();
        rule.setParameters(request.getNewParameters());
        rule.setVersion(rule.getVersion() + 1);
        ruleRepository.save(rule);

        // Clean up canary
        String canaryKey = String.format("rule:canary:%s:%s",
            request.getTenantId(), request.getRuleId());
        redisTemplate.delete(canaryKey);

        request.setStatus(ChangeStatus.ACTIVE);
        request.setActivatedAt(Instant.now());
        changeRequestRepository.save(request);

        auditLog.record("RULE_CHANGE_ACTIVATED", "SYSTEM", request.getChangeId(), null);
    }

    /**
     * Step 5: One-click rollback to previous version.
     * Any approved checker can trigger.
     */
    public void rollback(String changeId, String rollbackBy, String reason) {
        RuleChangeRequest request = changeRequestRepository.findById(changeId)
            .orElseThrow();

        // Restore old parameters
        RiskRule rule = ruleRepository.findById(request.getRuleId()).orElseThrow();
        rule.setParameters(request.getOldParameters());
        rule.setVersion(rule.getVersion() + 1);
        ruleRepository.save(rule);

        // Clean up canary if still active
        String canaryKey = String.format("rule:canary:%s:%s",
            request.getTenantId(), request.getRuleId());
        redisTemplate.delete(canaryKey);

        // Update status
        request.setStatus(ChangeStatus.ROLLED_BACK);
        request.setRolledBackAt(Instant.now());
        request.setRolledBackBy(rollbackBy);
        changeRequestRepository.save(request);

        auditLog.record("RULE_CHANGE_ROLLED_BACK", rollbackBy, changeId,
            Map.of("reason", reason));

        notificationService.notifyRoleGroup("RISK_CHECKER",
            "Rule change rolled back: " + changeId + " reason: " + reason);
    }
}
```

### Governance API Endpoints

```java
@RestController
@RequestMapping("/api/v1/risk/governance")
public class RuleGovernanceController {

    @PostMapping("/changes")
    public ResponseEntity<RuleChangeRequest> submitChange(
            @RequestBody SubmitChangeRequest req) {
        return ResponseEntity.ok(governanceService.submitChange(
            req.getMakerId(), req.getRuleId(), req.getNewParameters(),
            req.getImpactAssessment()));
    }

    @PostMapping("/changes/{changeId}/approve")
    public ResponseEntity<Void> approveChange(
            @PathVariable String changeId,
            @RequestParam String checkerId) {
        governanceService.approveChange(checkerId, changeId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/changes/{changeId}/rollback")
    public ResponseEntity<Void> rollback(
            @PathVariable String changeId,
            @RequestParam String rollbackBy,
            @RequestParam String reason) {
        governanceService.rollback(changeId, rollbackBy, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/changes")
    public ResponseEntity<Page<RuleChangeRequest>> listChanges(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(governanceService.listChanges(status,
            PageRequest.of(page, size)));
    }
}
```

---

## 6.13 跨模組邊界情境 (Cross-Module Boundary Scenarios)

The following boundary scenarios describe interactions between Risk & Compliance and other modules when edge cases occur. For complete boundary scenario specifications, refer to [Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md).

| BS ID | Scenario | Decision |
|-------|----------|----------|
| BS-04 | 自我排除 × 代理信用額度 | Wait for active bet settlement, then freeze agent credit (block withdrawals, different from self-exclusion lock); new credit requests rejected |
| BS-08 | 帳戶凍結 × 未結算投注 | Same as BS-04: wait for settlement then freeze (block withdrawals); unlike self-exclusion, deposits may be permitted for dispute resolution |
| BS-09 | 代理層級變更 × 結算 | Commission recalculated daily using weighted split; no retroactive adjustment to past settlement periods |
| BS-11 | GDPR 刪除 × 紅利 | Immediately forfeit BONUS wallet; maintain CASH withdrawal access for 7 days; full crypto-destruction on Day 37 per GDPR Article 17 |

**Cross-references**: [§BS-04](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-04-自我排除--代理信用額度), [§BS-08](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-08-帳戶凍結--未結算投注), [§BS-09](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-09-代理層級變更--結算), [§BS-11](./Technical_Cross_Module_Boundary_Scenarios_跨模組邊界情境技術規格.md#bs-11-gdpr-刪除--紅利)

---

## 6.14 對應業務文檔

This technical implementation document maps to:
- **Requirements**: `/requirements/06_Risk_Compliance_風控與合規.md`
- **API Specs**: `/api-specs/risk_compliance_api_v2.0.yaml`
- **Data Dictionary**: `/data-models/risk_compliance_schema.sql`
- **Deployment**: `/infrastructure/risk-compliance-helm-chart/`
- **ADRs**:
  - ADR-012: "Risk Layer 3 implements flag-based review, not real-time blocking"
  - ADR-015: "FingerprintJS for device fingerprinting (99.5% accuracy)"
  - ADR-018: "7-year audit log retention for AML compliance"

---

**v2.1 新增/變更清單**:
- §6.10 串謀偵測 Flink CEP: 百家樂對沖、撲克串謀/籌碼傾倒、行動同步偵測、CorrelationService
- §6.11 Bot 偵測服務: 6 維度 Go 實作 (滑鼠軌跡/點擊頻率/API 模式/決策時間/Session 時長/反自動化)
- §6.12 風控規則治理: Maker-Checker 雙人審批 + Canary 10% 流量 24h 觀察 + 一鍵回滾 + t_rule_change_request schema

**Document Version**: 2.1
**Last Updated**: 2026-03-24
**Status**: Production Ready
