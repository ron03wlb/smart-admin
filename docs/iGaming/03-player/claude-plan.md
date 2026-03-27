# Implementation Plan — 03-player (Player Domain)

> **Purpose**: Self-contained blueprint for implementing the iGaming Player Domain.
> **Approach**: Extend the existing SmartAdmin iGaming player module to cover the full spec.
> **Scale**: 100K–1M+ players. Hot paths must be designed for high throughput.

---

## 1. Project Context

### 1.1 What We're Building

The Player Domain is the central hub of the iGaming platform. It manages:
- **Player Lifecycle**: 7-state state machine (registration through closure)
- **KYC Verification**: 4-level identity verification with OCR + human review
- **VIP System**: 5-tier loyalty program with points and upgrade/downgrade rules
- **Player Segmentation**: RFM scoring with automated segment assignment and tagging
- **Responsible Gambling**: Self-exclusion, deposit/loss limits, session protection, affordability assessment, problem gambling detection
- **Customer Service**: 360° player view, multi-channel support, AI chatbot, ticketing with SLA

### 1.2 Why This Architecture

Three chapters (Ch1, Ch12, Ch15) are combined into one domain because they share the player entity as the central aggregate. Separating them would create circular dependencies (e.g., self-exclusion modifies player state; customer service reads player state; VIP influences CS routing). A single domain keeps the aggregate consistent and avoids cross-module transactions.

### 1.3 Existing Codebase

The SmartAdmin iGaming player module already has:
- `PlayerEntity` with encrypted PII fields (AES-256-GCM + blind index)
- `PlayerStateManager` with a subset of state transitions and Kafka event publishing
- `SelfExclusionRequestService` / `SelfExclusionEnforcementService` / `SelfExclusionReviewService`
- `VipLevelConfigService` / `VipAutoUpgradeService` / `VipRewardDistributionService`
- Basic controller, service, DAO, and form/VO patterns
- ArchitectureTest enforcement for all layers

This plan extends these existing classes and adds new modules for the missing capabilities.

### 1.4 Key Constraints

- **SmartAdmin Architecture**: Controller → Service → Manager → DAO (enforced by ArchitectureTest)
- **Transaction Rule**: `@Transactional(rollbackFor = Throwable.class)` in Manager layer ONLY
- **Option Type**: `io.vavr.control.Option` in Service layer (NOT `java.util.Optional`)
- **Injection**: Constructor injection via `@RequiredArgsConstructor` (no `@Autowired`)
- **MyBatis Plus**: `BaseMapper<Entity>`, `LambdaQueryWrapper`, XML mappers for complex queries
- **Database**: PostgreSQL with `t_` prefix tables, SMALLINT enums, `DECIMAL(19,4)` for money
- **Events**: Kafka domain events via `DomainEventPublisher`
- **Error codes**: `303xx` range for player module

---

## 2. Database Migration (V7)

### 2.1 Strategy

Clean break migration (`V7__player_domain_expansion.sql`). This migration:
1. Expands `t_player` status enum to support 7 states
2. Adds new tables for KYC workflow, RFM, tags, limits, affordability, customer service
3. Adds CHECK constraints and indexes for all new columns

### 2.2 Data Migration

**CRITICAL**: The existing `PlayerStatusEnum` uses: ACTIVE(1), LOCKED(2), SUSPENDED(3), PENDING_VERIFICATION(4), CLOSED(5), SELF_EXCLUDED(6). The new model **replaces** this entirely. The V7 migration must include data transformation:

```sql
-- Step 1: Map existing status values to new enum
UPDATE t_player SET status = CASE
  WHEN status = 1 THEN 2  -- ACTIVE(1) → ACTIVE(2)
  WHEN status = 2 THEN 6  -- LOCKED(2) → SUSPENDED(6) (LOCKED removed, map to SUSPENDED)
  WHEN status = 3 THEN 6  -- SUSPENDED(3) → SUSPENDED(6)
  WHEN status = 4 THEN 1  -- PENDING_VERIFICATION(4) → REGISTERED(1)
  WHEN status = 5 THEN 7  -- CLOSED(5) → CLOSED(7)
  WHEN status = 6 THEN 4  -- SELF_EXCLUDED(6) → SELF_EXCLUDED(4)
END;

-- Step 2: Drop old CHECK constraint, add new
ALTER TABLE t_player DROP CONSTRAINT ck_player_status;
ALTER TABLE t_player ADD CONSTRAINT ck_player_status
  CHECK (status IN (1, 2, 3, 4, 5, 6, 7));
```

**Note**: `LOCKED` and `PENDING_VERIFICATION` states are removed. `LOCKED` maps to `SUSPENDED` (closest equivalent). `PENDING_VERIFICATION` maps to `REGISTERED` (pre-activation state). All existing tests for removed states need rewrite.

### 2.3 Schema Changes

**t_player modifications**:
- Replace status CHECK constraint: REGISTERED(1), ACTIVE(2), DORMANT(3), SELF_EXCLUDED(4), COOLING_OFF(5), SUSPENDED(6), CLOSED(7)
- Add columns: `kyc_level` expanded to support L0-L3 (SMALLINT 0-3), `rfm_segment` (VARCHAR 20), `rfm_recency_score` / `rfm_frequency_score` / `rfm_monetary_score` (SMALLINT), `rfm_calculated_at` (TIMESTAMPTZ), `vip_points_balance` (BIGINT), `dormant_since` (TIMESTAMPTZ), `marketing_dnd` (BOOLEAN DEFAULT FALSE)

**New tables** (all with `tenant_id`, `create_time`, `update_time`, `deleted`):

| Table | Purpose | Key Columns |
|-------|---------|-------------|
| `t_kyc_verification` | KYC verification workflow | `player_id`, `level` (0-3), `status` (PENDING/IN_PROGRESS/APPROVED/REJECTED/RETRY), `provider_ref`, `provider_response` (JSONB), `reviewer_id`, `reviewed_at` |
| `t_kyc_document_type_config` | Accepted documents per jurisdiction | `jurisdiction`, `kyc_level`, `document_types` (JSONB), `max_age_days` |
| `t_player_tag` | Player tags (system + manual) | `player_id`, `tag_name`, `tag_category`, `source` (SYSTEM/MANUAL), `operator_id`, `reason`, `expires_at` |
| `t_deposit_limit` | Deposit limits | `player_id`, `period` (DAILY/WEEKLY/MONTHLY), `amount` DECIMAL(19,4), `currency`, `effective_from`, `pending_increase_amount`, `pending_increase_at`. **UNIQUE(tenant_id, player_id, period)** |
| `t_loss_limit` | Loss limits | Same structure as deposit limit. **UNIQUE(tenant_id, player_id, period)** |
| `t_session_config` | Session protection settings | `player_id`, `reality_check_interval_minutes`, `session_limit_minutes`, `idle_timeout_minutes` |
| `t_time_out` | Time-out (cooldown) periods | `player_id`, `type` (COOLING/FORCED), `starts_at`, `ends_at`, `auto_recover` |
| `t_affordability_assessment` | Affordability checks | `player_id`, `tier` (BASIC/ENHANCED/FULL), `status`, `trigger_type`, `trigger_amount`, `assessment_data` (JSONB), `result`, `expires_at`, `reviewed_by` |
| `t_problem_gambling_alert` | Problem gambling detections | `player_id`, `indicator_type`, `risk_level` (LOW/MEDIUM/HIGH), `indicator_value`, `threshold_value`, `action_taken` |
| `t_self_exclusion_registry_check` | External registry checks | `player_id`, `registry` (GAMSTOP/CRUKS/etc), `check_status`, `match_result`, `check_time`, `timeout_escalation_level` |
| `t_cs_ticket` | Customer service tickets | `ticket_id`, `player_id`, `channel`, `type`, `priority`, `status`, `assigned_to`, `sla_first_response_at`, `sla_resolution_at`, `first_responded_at`, `resolved_at` |
| `t_cs_ticket_message` | Ticket messages | `ticket_id`, `sender_type` (PLAYER/AGENT/BOT), `sender_id`, `content`, `channel` |
| `t_cs_ticket_escalation` | Escalation history | `ticket_id`, `from_agent`, `to_agent`, `reason`, `escalation_level` |
| `t_cs_chatbot_session` | AI chatbot sessions | `session_id`, `player_id`, `channel`, `intent`, `confidence`, `resolution`, `escalated_to_human` |
| `t_cs_knowledge_article` | Knowledge base articles | `article_id`, `category`, `title`, `content`, `language`, `status`, `author_id`, `approved_by` |
| `t_vip_points_transaction` | VIP points ledger (append-only, **partitioned by month**) | `player_id`, `type` (EARN/REDEEM/EXPIRE), `amount`, `source` (BET/REDEMPTION/EXPIRY), `reference_id`, `event_id` (UUID, **UNIQUE** — idempotency key) |
| `t_rfm_history` | RFM score history | `player_id`, `calculated_date`, `recency_score`, `frequency_score`, `monetary_score`, `segment` |

### 2.4 Index Strategy

For high-volume tables, partial indexes with `WHERE deleted = FALSE`:
- `t_player`: composite index on `(tenant_id, status)`, `(tenant_id, rfm_segment)`, `(tenant_id, vip_level)`
- `t_player_tag`: composite on `(player_id, tag_name)` unique, index on `(tag_name, tenant_id)`
- `t_cs_ticket`: index on `(assigned_to, status)`, `(player_id, create_time DESC)`
- `t_affordability_assessment`: index on `(player_id, status, expires_at)`
- `t_problem_gambling_alert`: index on `(player_id, create_time DESC)`

---

## 3. Module: player-lifecycle

### 3.1 State Machine Expansion

**Replace** `PlayerStateManager`'s `VALID_TRANSITIONS` map entirely (not extend). The existing transitions for `LOCKED` and `PENDING_VERIFICATION` are removed. The new transition map defines all valid transitions:

| From | To | Trigger |
|------|----|---------|
| REGISTERED | ACTIVE | First deposit (jurisdiction-dependent: UKGC requires KYC L1 completion before first deposit) |
| ACTIVE | DORMANT | 90 days inactivity |
| DORMANT | ACTIVE | Re-login or deposit |
| ACTIVE | SELF_EXCLUDED | Player self-exclusion request |
| SELF_EXCLUDED | COOLING_OFF | Exclusion period expires |
| COOLING_OFF | ACTIVE | Cooldown expires + player confirms |
| ACTIVE | SUSPENDED | Risk/admin trigger |
| SUSPENDED | ACTIVE | Review passes |
| SUSPENDED | CLOSED | Permanent ban |
| DORMANT | CLOSED | 365 days + zero balance |
| ACTIVE | CLOSED | Player requests closure |

Each transition must:
1. Validate via CAS (`WHERE status = #{expected} AND version = #{version}`)
2. Insert audit log (`t_player_audit_log`) in same transaction
3. Publish Kafka event (`PLAYER_STATUS_CHANGED`)

**Concurrent transition resolution**: When multiple triggers fire simultaneously (e.g., risk flag + self-exclusion), the first to succeed via CAS wins. The loser reloads the player's new state and re-evaluates whether the transition is still valid from the new state. Self-exclusion (regulatory) always takes precedence conceptually, but the CAS mechanism handles ordering naturally.

### 3.2 Registration Enhancement

Extend `PlayerRegistrationManager` to support all 4 registration methods:

**Email Registration**: Validate email uniqueness via blind index → send OTP (10min TTL) → verify → create player (status=REGISTERED) → create CASH wallet (call 02-funding) → set VIP=Bronze → record registration source (landing page, promo code, agent code) → record IP + device fingerprint.

**Phone Registration**: Same flow but with SMS OTP (5min TTL). Phone blind index uniqueness check.

**Social OAuth**: Redirect to Google/Facebook/Line OAuth → receive token → verify → create player with OAuth-provided data → require DOB supplement form → same initialization.

**One-Click**: Phone only → SMS OTP → create player with minimal data → subsequent profile completion flow.

**Registration Saga / Compensation**: If wallet creation (02-funding call) fails after player record is persisted, mark player status as `REGISTRATION_INCOMPLETE` (treated as a sub-state of REGISTERED). An async retry queue (`PlayerRegistrationRetryJob`) periodically attempts wallet creation for incomplete registrations. After 3 failed attempts, alert ops team. Player cannot proceed until wallet is created.

**Anti-Duplicate Logic** (in registration service):
- Same email (blind index match) → reject with `EMAIL_ALREADY_EXISTS`
- Same phone (blind index match) → reject with `PHONE_ALREADY_EXISTS`
- Same device fingerprint + different email → allow but add `MULTI_DEVICE_FLAG` tag, notify risk service
- Same IP with ≥3 registrations in 24h → require CAPTCHA, escalate to manual review

### 3.3 Dormancy Detection

A scheduled job (`PlayerDormancyJob`) runs daily:
1. Query players with status=ACTIVE and `last_login_time < NOW() - INTERVAL '90 days'`
2. Transition to DORMANT, set `dormant_since`
3. Query DORMANT players with `dormant_since < NOW() - INTERVAL '365 days'` and zero wallet balance → transition to CLOSED

### 3.4 Permission Matrix

Each state defines allowed/restricted operations. Implement via a `PlayerPermissionService` that checks `(playerStatus, operationType) → boolean`. The permission matrix from the spec is encoded as a static configuration.

---

## 4. Module: player-kyc

### 4.1 Abstract Adapter Pattern

Define `KycVerificationAdapter` interface:

```java
public interface KycVerificationAdapter {
    KycSubmissionResult submitDocument(KycSubmissionRequest request);
    KycVerificationStatus checkStatus(String providerReference);
    void handleWebhook(String payload, String signature);
}
```

`KycSubmissionRequest` contains: `playerId`, `kycLevel`, `documentType`, `documentImages` (list of byte arrays or URLs), `playerData` (name, DOB, address).

`KycSubmissionResult` contains: `providerReference`, `initialStatus`, `estimatedCompletionTime`.

`KycVerificationStatus` enum: `PENDING`, `IN_PROGRESS`, `APPROVED`, `REJECTED`, `RETRY`.

Implementations:
- `StubKycVerificationAdapter` — returns configurable results (for dev/test)
- `SumsubKycVerificationAdapter` — (future) implements Sumsub REST API

### 4.2 KYC Workflow

`KycVerificationService` orchestrates:
1. Player submits documents via form
2. Service validates document type against jurisdiction config (`t_kyc_document_type_config`)
3. Creates `t_kyc_verification` record (status=PENDING)
4. Calls `KycVerificationAdapter.submitDocument()`
5. Updates record with provider reference
6. Webhook endpoint receives async result → `KycApprovalManager.processVerificationResult()` (in Manager, transactional)
7. If APPROVED: update player `kyc_level`, publish `KYC_LEVEL_CHANGED` event
8. If REJECTED/RETRY: update verification status, notify player

**Auto-Approval Rules**:
- L0: Email/Phone OTP success → instant, no adapter call needed
- L1-L3: Delegate to adapter, process result via webhook

### 4.3 Multi-Jurisdiction Configuration

`KycJurisdictionConfigService` reads from `t_kyc_document_type_config` and applies the three-layer override (Jurisdiction → Brand → Global per Ch0 §0.10).

Key per-jurisdiction rules:
- UKGC: L1 must complete before first deposit; L2 within 72h or freeze
- MGA: L1 deferred to first withdrawal; L2 within 90 days
- PAGCOR: Philippine gov ID + GCash accepted
- Curacao: L1 minimum; L2 on large withdrawals

Implement as a `KycTriggerEvaluator` that checks deposit/withdrawal events against jurisdiction rules and auto-triggers KYC upgrade requests when thresholds are met.

---

## 5. Module: player-vip

### 5.1 VIP Tier Management

Extend existing `VipAutoUpgradeService`:

**Monthly Evaluation Job** (`VipMonthlyEvaluationJob`, scheduled 1st of month 00:00 UTC):
1. Query all ACTIVE players' monthly valid turnover
2. Check upgrade thresholds (Bronze→Silver $10K, Silver→Gold $50K, etc.)
3. Check maintenance thresholds for current tier
4. Apply protection periods before downgrade (Diamond 3mo, Platinum 2mo, others 1mo)
5. Log all changes to `t_vip_change_log`

**Instant Upgrade Check**: On every bet settlement, check if cumulative monthly turnover crosses an upgrade threshold. If so, upgrade immediately (don't wait for monthly job).

**Special Rules**: DORMANT players skip evaluation (no downgrade). SUSPENDED players freeze evaluation. Manual upgrades require reason + audit record.

### 5.2 VIP Points Engine

New `VipPointsService`:

**Balance Strategy**: Use Redis atomic counter (`INCRBY`) for real-time VIP points balance. A periodic flush job (`VipPointsBalanceFlushJob`, every 5min) syncs Redis counters to `t_player.vip_points_balance`. This avoids row-level lock contention on `t_player` during high-volume bet settlements.

**Earning**: On bet settlement event (Kafka consumer), calculate `basePoints = validTurnover × gameRate × vipMultiplier`. Game rates: Slots 100%, Table 50%, Sports 75%. VIP multipliers: Bronze 1x, Silver 1.5x, Gold 2x, Platinum 3x, Diamond 5x. Insert into `t_vip_points_transaction` (type=EARN).

**Expiry**: Daily job scans `t_vip_points_transaction` for EARN records older than 365 days. Creates EXPIRE transaction to offset. Updates `t_player.vip_points_balance`.

**Redemption**: API endpoint for point redemption. Validates balance sufficiency. Inserts REDEEM transaction. Options: cash bonus (100pt = $1), free spins, VIP shop items.

---

## 6. Module: player-segmentation

### 6.1 RFM Scoring Engine

New `RfmCalculationService` with scheduled daily job at 02:00 UTC:

**Score Calculation** per player:
- R (Recency): days since last activity → score 1-5 based on thresholds (≤7d=5, 8-14d=4, 15-30d=3, 31-60d=2, >60d=1)
- F (Frequency): 30-day bet count → score 1-5 (≥100=5, 50-99=4, 20-49=3, 5-19=2, <5=1)
- M (Monetary): 30-day valid turnover → score 1-5 (≥$10K=5, $5K-$9,999=4, $1K-$4,999=3, $100-$999=2, <$100=1)

**Segment Assignment** based on composite RFM scores:
- Champions: R≥4,F≥4,M≥4
- Loyal: R≥3,F≥3,M≥3 (not Champions)
- Potential: R≥4,F≤2,M≤2
- At Risk: R≤2,F≥2,M≥2
- Hibernating: R=1,F≤2,M≤1
- New Players: R=5,F=1,M=1

**Output**: Update `t_player` RFM columns, insert `t_rfm_history` record, publish `RFM_SEGMENT_CHANGED` event if segment changed.

**Anomaly Detection**: If segment drops ≥2 levels (e.g., Champions→At Risk), publish `RFM_ANOMALY_DETECTED` event.

### 6.2 Tag System

New `PlayerTagService`:

**System Auto-Tags**: Driven by domain events from other modules:
- `HIGH_RISK`: When risk score ≥70 (consumed from 05-risk-compliance)
- `BONUS_ABUSER`: When bonus abuse detected (from 04-gaming)
- `MULTI_ACCOUNT`: When multi-account detected (from 05-risk-compliance)
- `WHALE`: When monthly turnover >$100K
- `PROBLEM_GAMBLER`: When problem gambling indicators trigger
- `CHURN_RISK`: When RFM drops to At Risk
- `PEP`: When PEP screening hits (from 05-risk-compliance)
- `SANCTION_HIT`: When sanction list match (from 05-risk-compliance)

**Manual Tags**: Via CS/VIP manager UI. Must include: operator ID, reason text. Stored with audit trail.

**Priority Resolution**: `SANCTION_HIT` → instant freeze (no other evaluation needed). `HIGH_RISK` + `WHALE` → route to manual review. Contradictory tags → flag for risk manager.

**Tag Queries**: Support efficient queries like "find all players with tag X in tenant Y" via composite index on `t_player_tag(tag_name, tenant_id)`.

---

## 7. Module: responsible-gambling

### 7.1 Self-Exclusion (Existing + Expansion)

**Extend existing services**:

`SelfExclusionGateway` interface (new):
```java
public interface SelfExclusionGateway {
    RegistryCheckResult checkPlayer(PlayerIdentity identity);
    void reportExclusion(Long playerId, SelfExclusionType type, Duration duration);
}
```

Implementations:
- `GamstopGateway` — GAMSTOP REST API (POST, API key + IP whitelist). Fields: first_name, last_name, date_of_birth, email, postcode. Response: allowed/blocked. Configurable: real mode (calls API) or mock mode (returns configurable result).
- `CruksGateway` / `SpelpausGateway` / `RofusGateway` — stub interfaces for future implementation.
- `CompositeGateway` — routes to correct registry based on player's jurisdiction.

**Token/Session Revocation Flow** (in `SelfExclusionEnforcementService`, enhanced for v2.2):
1. Revoke all active GP tokens (broadcast to connected GPs) → immediate
2. Notify all GPs player is excluded (GP must reject subsequent requests) → ≤30s
3. Wait for in-progress round settlement (GP has 5min) → ≤5min
4. Unsettled after 5min → force rollback + refund stakes → automatic
5. Freeze all wallets (CASH/BONUS/CREDIT) → immediate
6. Block agent credit channel → immediate
7. Set marketing DND flag → immediate
8. Publish `PLAYER_SELF_EXCLUDED` domain event

**GAMSTOP Timeout Handling** (v2.2 GAP-7):
State machine in `SelfExclusionRegistryCheckManager`:
- 0-24h: Account HOLD, await manual review
- 24h: Escalate to compliance manager + alert (NEVER auto-release)
- 48h: Escalate to MLRO + upgrade to SUSPENDED
- 72h: Force freeze + regulatory incident report

### 7.2 Deposit & Loss Limits

New `DepositLimitService` and `LossLimitService`:

**Setting a limit**: Player selects period (daily/weekly/monthly) and amount. Validate layer consistency (daily ≤ weekly ≤ monthly). If lowering → immediate effect. If raising → create pending increase with 24-72h delay + cooldown period (player can cancel).

**Enforcement**: On deposit attempt, `DepositLimitEnforcementService` checks current period usage against limit. On bet settlement, `LossLimitEnforcementService` calculates net loss (`total wagered - total paid`). At 80% → publish warning event. At 100% → block further activity + display message.

**Zero tolerance**: The system must NEVER allow a deposit/loss that exceeds the limit, even by rounding. Enforcement must be atomic — check and deduct in the same transaction. **Note**: The atomic check-and-deduct operation is performed in `DepositLimitManager` / `LossLimitManager` (Manager layer, `@Transactional`). The corresponding Service classes orchestrate the flow but delegate transactional operations to Managers.

### 7.3 Session Protection

New `SessionProtectionService`:

**Time-Out**: Player selects duration (24h–6wk). Creates `t_time_out` record. While active: all activity blocked. Auto-recovers when `ends_at` is reached.

**Forced Rest** (jurisdiction-specific):
- UKGC: Track deposit count per 24h window. At 10 deposits → trigger 60min forced rest.
- Germany: Track continuous play time. At 60min → 5min forced break.
- Sweden: Offer session limit options (15/30/60/120min).

**Reality Check**: Configurable interval (15/30/60min). On trigger: pause auto-spin, wait for current round, display overlay (session duration + net P&L). Player options: continue, stop, set limit, view history. Track rapid-dismiss count: 10+ dismissals in <5s each → suggest increasing frequency.

**Idle Logout**: Default 30min. Configurable 15-60min. Active game rounds excluded from idle timer.

### 7.4 Problem Gambling Detection

New `ProblemGamblingDetectionService` with hybrid architecture:

**Real-Time Indicators** (Kafka consumers):
1. **Deposit frequency**: Count deposits in sliding 24h window. ≥5 → MEDIUM risk → popup reminder.
2. **Deposit amount spike**: Compare to 30-day rolling average. >3x → HIGH → suggest cooldown.
3. **Loss chasing**: Track consecutive bet increases after losses. 3+ → HIGH → offer self-exclusion.

**Batch Indicators** (scheduled job, daily):
4. **Continuous play**: Calculate max session length from activity logs. >4h → MEDIUM → mandatory rest reminder.
5. **Late-night frequency**: Count 00:00-06:00 bets, compare to daily average. >200% → MEDIUM → log only.
6. **Lending signals**: Detect pattern of multiple rapid small deposits. → HIGH → flag + CS intervention.

**Alert Flow**: Each indicator writes to `t_problem_gambling_alert` via `ProblemGamblingAlertManager` (transactional — alert insert + event publish in same transaction). HIGH risk alerts publish `PROBLEM_GAMBLING_HIGH_RISK` event → triggers UI intervention (popup, self-exclusion option) + CS notification.

### 7.5 Affordability Assessment

New `AffordabilityAssessmentService`:

**3-Tier Framework**:

| Tier | Trigger | Check Method | Integration |
|------|---------|-------------|-------------|
| Basic | Net loss GBP 125-500 | Warning message | Internal only |
| Enhanced | Net loss GBP 500-2,000 | Self-declaration form (income, housing, dependents, disposable income) | Form submission + manual review |
| Full | Net loss >GBP 2,000 | Third-party verification | `AffordabilityCheckAdapter` interface (Open Banking + credit agency) |

**Rolling 30-day trigger**: Net deposits ≥ GBP 150 → financial vulnerability check (public records: bankruptcy, CCJs, IVAs).

**Suggested Limit Calculation**: `maxSafeSpend = disposableIncome × 10%`. Household >2 → reduce 20%. Floor GBP 50, cap GBP 2,000.

**Result State Machine**:
- PASSED → no intervention, record result, set re-evaluation date
- FAILED (Basic) → mandatory affordability warning + suggest limit
- FAILED (Enhanced) → auto-set deposit limit to `disposableIncome × 10%`
- FAILED (Full) → strict limits, >GBP 2,000 requires MLRO approval
- DECLINED → apply jurisdiction minimum defaults + 24h manual review
- TIMEOUT (>7 days) → suspend deposits, allow play + withdrawals

**Timeout Handling** (v2.2 GAP-6): Allow current round to complete naturally → block new bets → suspend deposits → allow withdrawals → pause bonus wagering timer → resume on assessment completion.

**`AffordabilityCheckAdapter` interface** (for Full tier):
```java
public interface AffordabilityCheckAdapter {
    AffordabilityCheckResult checkFinancialVulnerability(PlayerFinancialData data);
    AffordabilityCheckResult checkCreditAgency(PlayerIdentity identity);
    AffordabilityCheckResult checkOpenBanking(String consentToken);
}
```

### 7.6 Marketing Restrictions

`MarketingRestrictionService`: During exclusion/cooldown:
- Set `marketing_dnd = TRUE` on player
- DND flag checked by batch marketing jobs (skip player)
- Violation detection: If excluded player receives marketing → publish `MARKETING_VIOLATION` alert

---

## 8. Module: customer-service

### 8.1 360° Player View

New `PlayerViewAggregationService`:

**API Gateway Aggregation Pattern**: The `/igaming/cs/player-view/{playerId}` endpoint calls multiple domain services in parallel (using `CompletableFuture`/virtual threads) and aggregates results:

1. Player core data (local — PlayerService)
2. Wallet balances (call 02-funding WalletService)
3. Recent bets + game preferences (call 04-gaming)
4. Risk score + AML status (call 05-risk-compliance)
5. Tags and segments (local — PlayerTagService, RfmCalculationService)
6. CS ticket history (local — TicketService)

**Response**: `PlayerViewVO` containing all 6 categories. Each sub-section is optional (if a downstream service is unavailable, return partial data with error flags).

**Resilience Configuration**: Each downstream call wrapped with Resilience4j circuit breaker:
- Timeout: 3 seconds per call
- Retry: up to 2 retries with 500ms backoff
- Circuit breaker: opens at 50% failure rate (sliding window of 10 calls), half-open after 30s
- Fallback: return null section with `unavailable: true` flag in response

**Permission Control**: Operation panel actions require role-based permissions:
- Manual top-up → `cs:player:topup` (Senior CS+)
- Force logout → `cs:player:logout` (CS+)
- Password reset → `cs:player:password-reset` (CS+)
- Account unlock → `cs:player:unlock` (CS+)

### 8.2 Multi-Channel Support

New `ChannelMessageService`:

**Unified Message Model**: All channels produce `CsMessage` objects with: `channelType`, `senderId`, `senderType`, `content`, `metadata` (channel-specific: e.g., Telegram chat_id).

**Channel Adapters**: `ChannelAdapter` interface with implementations for LiveChat (WebSocket), Telegram (Bot API), Email (SMTP/IMAP), WhatsApp (Business API), Phone (VoIP integration stub).

**Context Auto-Transfer**: When a ticket is created, automatically attach: player ID, VIP level, current page (if available from LiveChat), wallet balance, active bet status.

**Agent Capacity**: Track active conversations per agent (max 3-5 configurable). Load balancer routes new conversations to least-loaded qualified agent.

### 8.3 AI Chatbot

New `ChatbotService` and `IntentClassificationService`:

**Intent Classification** (via `IntentClassificationAdapter` — abstract, LLM-agnostic):
```java
public interface IntentClassificationAdapter {
    IntentResult classify(String playerMessage, ConversationContext context);
}
```

`IntentResult` contains: `intent` (enum), `confidence` (0.0-1.0), `entities` (extracted data).

**6 Intent Types with Confidence Thresholds**:
| Intent | Threshold | Action |
|--------|-----------|--------|
| DEPOSIT_ISSUE | >0.8 | Auto-handle + create ticket |
| WITHDRAWAL_QUERY | >0.9 | FAQ reply |
| BONUS_QUERY | >0.85 | Check wallet + reply |
| GAME_ERROR | >0.75 | Create ticket + escalate |
| PASSWORD_RESET | >0.95 | Send reset link |
| COMPLAINT | >0.7 | Immediate human transfer |

**Auto-Escalation Rules**:
- Confidence <0.7 → human transfer
- Player explicitly requests human
- Sensitive topic (complaint, fraud, account anomaly)
- 3+ unresolved auto-replies
- Diamond VIP → auto-route to VIP manager

**Degradation Strategy** (`ChatbotCircuitBreaker`):
- Normal: AI processes messages
- Degraded (P99 >3s or error rate >5%): Skip AI, route directly to human
- Unavailable (AI completely down): Pre-built quick replies + immediate human queue
- Recovery: 3 consecutive health checks pass → restore 25% → 50% → 100%

Implement degradation via Resilience4j circuit breaker wrapping the `IntentClassificationAdapter`.

### 8.4 Ticketing & SLA

New `TicketService`, `SlaEnforcementService`:

**Ticket Lifecycle**: OPEN → ASSIGNED → IN_PROGRESS → PENDING_CUSTOMER → RESOLVED → CLOSED

**SLA Calculation**: On ticket creation, `SlaEnforcementService` determines first-response and resolution deadlines based on ticket type + player VIP level. Stores deadlines in `t_cs_ticket.sla_first_response_at` and `sla_resolution_at`.

**Escalation Engine** (`SlaEscalationJob`, runs every minute, query: `WHERE sla_first_response_at < NOW() AND first_responded_at IS NULL`):
- SLA at 50% → notify Team Lead
- SLA at 100% → notify Manager
- Diamond complaint → instant notify VIP Manager
- 3+ re-opens → assign to Senior

**Violation Tracking**: On SLA breach, create `t_cs_ticket_escalation` record. Track per-agent violations for KPI reporting.

**Compensation**: Configurable per violation type. Default $5 for resolution timeout. Requires Maker-Checker approval. Monthly cap $50 per player.

**Auto-Close**: Pending Customer + 72h no reply → close. Resolved + 24h no objection → close.

### 8.5 Routing Engine

`TicketRoutingService` applies routing rules in priority order:
1. **VIP routing**: Diamond/Platinum → assigned VIP manager
2. **Skill matching**: Payment issue → payment-skilled agent; Game issue → game-skilled agent
3. **Language matching**: Player language preference → agent language capability
4. **Load balancing**: Route to agent with fewest open tickets (max 20)

### 8.6 Knowledge Base

`KnowledgeBaseService` manages `t_cs_knowledge_article`:
- CRUD operations with role-based access (player: read public; CS: draft; Team Lead: edit+approve; Manager: final approve)
- Multi-language support (EN, zh-TW, zh-CN, TH, VI, ID)
- Full-text search (PostgreSQL `tsvector` with language-specific configurations)
- Article versioning (content changes tracked)

---

## 9. Cross-Domain Events

### 9.1 Events This Domain Publishes

| Event | Topic | Trigger | Consumers |
|-------|-------|---------|-----------|
| `PLAYER_STATUS_CHANGED` | `player-events` | Any state transition | All domains |
| `PLAYER_REGISTERED` | `player-events` | Registration complete | 02-funding (create wallet) |
| `KYC_LEVEL_CHANGED` | `player-events` | KYC approved | 04-gaming (unlock features) |
| `VIP_LEVEL_CHANGED` | `player-events` | VIP upgrade/downgrade | 04-gaming (adjust benefits) |
| `PLAYER_SELF_EXCLUDED` | `player-events` | Self-exclusion activated | 02-funding (freeze wallet), 04-gaming (revoke tokens) |
| `RFM_SEGMENT_CHANGED` | `player-events` | Daily RFM recalculation | Marketing automation |
| `PROBLEM_GAMBLING_HIGH_RISK` | `player-events` | Detection threshold crossed | CS notification |
| `MARKETING_VIOLATION` | `player-events` | Excluded player received marketing | Compliance alert |

### 9.2 Events This Domain Consumes

| Event | Source | Handler |
|-------|--------|---------|
| `BET_SETTLED` | 04-gaming | VipPointsService (earn points), ProblemGamblingDetectionService (loss chasing) |
| `RISK_SCORE_UPDATED` | 05-risk-compliance | PlayerTagService (update HIGH_RISK tag) |
| `DEVICE_FINGERPRINT_MATCH` | 05-risk-compliance | PlayerTagService (MULTI_ACCOUNT tag) |
| `BONUS_ABUSE_DETECTED` | 04-gaming | PlayerTagService (BONUS_ABUSER tag) |
| `DEPOSIT_COMPLETED` | 02-funding | DepositLimitEnforcementService, ProblemGamblingDetectionService |

---

## 10. API Endpoints

### 10.1 Player Lifecycle
- `POST /igaming/player/register` — Player registration
- `POST /igaming/player/login` — Player login
- `GET /igaming/player/get/{playerId}` — Get player info
- `POST /igaming/player/query` — Query players (paginated)
- `PUT /igaming/player/update` — Update player profile
- `POST /igaming/player/transition` — Admin state transition (with permission)

### 10.2 KYC
- `POST /igaming/kyc/submit` — Submit KYC documents
- `GET /igaming/kyc/status/{playerId}` — Check KYC status
- `POST /igaming/kyc/webhook/{provider}` — Webhook endpoint (no auth, signature verified)
- `POST /igaming/kyc/admin/review` — Admin review KYC (approve/reject)

### 10.3 VIP
- `GET /igaming/vip/info/{playerId}` — Get VIP info + points
- `POST /igaming/vip/points/redeem` — Redeem VIP points
- `GET /igaming/vip/points/history` — Points transaction history
- `POST /igaming/vip/admin/upgrade` — Manual VIP upgrade (with reason)

### 10.4 Responsible Gambling
- `POST /igaming/rg/self-exclude` — Request self-exclusion
- `GET /igaming/rg/self-exclude/status/{playerId}` — Check exclusion status
- `POST /igaming/rg/limit/deposit` — Set deposit limit
- `POST /igaming/rg/limit/loss` — Set loss limit
- `POST /igaming/rg/session/config` — Set session protection config
- `POST /igaming/rg/timeout` — Request time-out
- `GET /igaming/rg/affordability/status/{playerId}` — Affordability status
- `POST /igaming/rg/affordability/declaration` — Submit affordability self-declaration

### 10.5 Customer Service
- `GET /igaming/cs/player-view/{playerId}` — 360° player view
- `POST /igaming/cs/ticket/create` — Create ticket
- `POST /igaming/cs/ticket/query` — Query tickets (paginated)
- `PUT /igaming/cs/ticket/assign` — Assign ticket
- `PUT /igaming/cs/ticket/resolve` — Resolve ticket
- `POST /igaming/cs/ticket/message` — Add message to ticket
- `POST /igaming/cs/chatbot/message` — Send message to chatbot
- `POST /igaming/cs/kb/article` — Create/update KB article
- `POST /igaming/cs/kb/search` — Search knowledge base

---

## 11. Directory Structure

```
smartadmin-igaming-player/src/main/java/net/lab1024/sa/igaming/player/
├── controller/
│   ├── PlayerController.java          (existing, extend)
│   ├── PlayerAuthController.java      (existing)
│   ├── KycAdminController.java        (existing, extend)
│   ├── VipController.java             (new)
│   ├── ResponsibleGamblingController.java  (new)
│   └── CustomerServiceController.java      (new)
├── service/
│   ├── PlayerService.java             (existing, extend)
│   ├── PlayerAuthService.java         (existing)
│   ├── KycVerificationService.java    (existing, extend)
│   ├── PlayerPermissionService.java   (new)
│   └── PlayerViewAggregationService.java   (new)
├── manager/
│   ├── PlayerStateManager.java        (existing, extend state map)
│   ├── PlayerRegistrationManager.java (existing, extend registration methods)
│   ├── VipLevelManager.java           (existing)
│   ├── KycApprovalManager.java        (existing, extend)
│   ├── DepositLimitManager.java       (new)
│   ├── LossLimitManager.java          (new)
│   └── AffordabilityAssessmentManager.java  (new)
├── dao/
│   ├── PlayerDao.java                 (existing)
│   ├── KycDocumentDao.java            (existing)
│   ├── PlayerAuditLogDao.java         (existing)
│   ├── VipChangeLogDao.java           (existing)
│   ├── PlayerTagDao.java              (new)
│   ├── DepositLimitDao.java           (new)
│   ├── LossLimitDao.java              (new)
│   ├── AffordabilityAssessmentDao.java     (new)
│   ├── ProblemGamblingAlertDao.java         (new)
│   ├── SelfExclusionRegistryCheckDao.java  (new)
│   ├── CsTicketDao.java                    (new)
│   ├── CsTicketMessageDao.java             (new)
│   ├── CsChatbotSessionDao.java            (new)
│   ├── KnowledgeArticleDao.java            (new)
│   ├── VipPointsTransactionDao.java        (new)
│   └── RfmHistoryDao.java                  (new)
├── domain/
│   ├── entity/         (existing entities + new entities for each new table)
│   ├── form/           (existing forms + new forms for new features)
│   └── vo/             (existing VOs + new VOs for new features)
├── selfexclusion/
│   ├── service/        (existing, extend)
│   ├── gateway/        (new — SelfExclusionGateway interface + implementations)
│   └── domain/entity/  (existing)
├── vip/
│   ├── service/        (existing, extend with VipPointsService)
│   └── domain/entity/  (existing + VipPointsTransactionEntity)
├── segmentation/       (new subdomain)
│   ├── service/        (RfmCalculationService, PlayerTagService)
│   └── domain/entity/  (PlayerTagEntity, RfmHistoryEntity)
├── responsiblegambling/ (new subdomain)
│   ├── service/        (DepositLimitService, LossLimitService, SessionProtectionService,
│   │                    ProblemGamblingDetectionService, AffordabilityAssessmentService,
│   │                    MarketingRestrictionService)
│   ├── adapter/        (AffordabilityCheckAdapter interface + stubs)
│   └── domain/entity/  (DepositLimitEntity, LossLimitEntity, TimeOutEntity,
│                        AffordabilityAssessmentEntity, ProblemGamblingAlertEntity)
├── customerservice/    (new subdomain)
│   ├── service/        (TicketService, SlaEnforcementService, TicketRoutingService,
│   │                    ChannelMessageService, ChatbotService, IntentClassificationService,
│   │                    KnowledgeBaseService)
│   ├── adapter/        (IntentClassificationAdapter, ChannelAdapter interfaces)
│   └── domain/entity/  (CsTicketEntity, CsTicketMessageEntity, CsTicketEscalationEntity,
│                        CsChatbotSessionEntity, KnowledgeArticleEntity)
├── job/                (new — scheduled jobs)
│   ├── PlayerDormancyJob.java
│   ├── VipMonthlyEvaluationJob.java
│   ├── VipPointsExpiryJob.java
│   ├── RfmDailyCalculationJob.java
│   ├── ProblemGamblingBatchJob.java
│   ├── SlaEscalationJob.java
│   └── AffordabilityTriggerJob.java
├── consumer/           (new — Kafka event consumers)
│   ├── BetSettledConsumer.java
│   ├── DepositCompletedConsumer.java
│   ├── RiskScoreUpdatedConsumer.java
│   └── DeviceFingerprintMatchConsumer.java
└── typehandler/        (existing)
```

---

## 12. Implementation Order

Suggested phasing by dependency and risk:

### Phase 1: Foundation (player-lifecycle + player-kyc)
1. V7 database migration
2. Expand PlayerStateManager (7 states + full transition map)
3. Expand PlayerRegistrationManager (4 registration methods)
4. KycVerificationAdapter interface + StubAdapter
5. KYC workflow with multi-jurisdiction config
6. PlayerPermissionService
7. PlayerDormancyJob

### Phase 2: VIP & Segmentation (player-vip + player-segmentation)
8. VipPointsService + VipPointsExpiryJob
9. VipMonthlyEvaluationJob (enhanced with instant upgrade)
10. RfmCalculationService + RfmDailyCalculationJob
11. PlayerTagService (system + manual)

### Phase 3: Responsible Gambling (responsible-gambling)
12. DepositLimitService + LossLimitService
13. SessionProtectionService (reality check, time-out, forced rest)
14. SelfExclusionGateway + GamstopGateway (mock mode)
15. Self-exclusion enforcement enhancement (v2.2 token revocation)
16. ProblemGamblingDetectionService (hybrid architecture)
17. AffordabilityAssessmentService (3-tier + adapter interface)
18. MarketingRestrictionService

### Phase 4: Customer Service (customer-service)
19. CsTicketService + SLA enforcement + escalation
20. TicketRoutingService
21. PlayerViewAggregationService (360° view)
22. ChannelMessageService + adapters
23. ChatbotService + IntentClassificationAdapter
24. KnowledgeBaseService

### Phase 5: Integration & Kafka Consumers
25. Kafka consumers (BetSettled, DepositCompleted, RiskScoreUpdated, DeviceFingerprintMatch)
26. Domain event publishing for all new state changes
27. End-to-end integration testing

---

## 13. Error Code Allocation

Extending the existing `303xx` range:

| Range | Domain |
|-------|--------|
| 30300-30309 | Player not found / resource errors |
| 30310-30319 | Duplicate errors (existing) |
| 30320-30329 | Auth / PII errors (existing) |
| 30330-30339 | KYC errors (existing) |
| 30340-30349 | Validation errors (existing) |
| 30350-30359 | VIP errors (new) |
| 30360-30369 | Limit errors (new — deposit/loss limit) |
| 30370-30379 | Self-exclusion errors (new) |
| 30380-30389 | Affordability errors (new) |
| 30390-30399 | Session protection errors (new) |
| 30400-30419 | Customer service / ticket errors (new) — **Note: extends into `304xx` range** |
| 30420-30429 | Chatbot errors (new) |
| 30430-30439 | Tag errors (new) |

**Range Extension Justification**: The Player Domain's 6 submodules exceed the capacity of a single `303xx` range (100 codes). We explicitly claim `304xx` for player-adjacent features (customer service, chatbot, tags). This is documented here to prevent collision with future `304xx` module allocations. The Game module's existing `304xx` codes should be reviewed for overlap.

---

## 14. Performance Considerations (100K-1M+ Players)

### Hot Paths
- **Deposit limit check**: Must complete in <10ms. Use Redis cache for current period usage, invalidated on deposit events.
- **Self-exclusion check**: On every login. Use Redis cache with TTL, invalidated on exclusion events.
- **Reality check timer**: Client-side timer with server-side validation on game events.
- **Problem gambling real-time indicators**: Kafka consumers with in-memory sliding windows (e.g., Caffeine cache for deposit counts).

### Batch Job Scaling
- **RFM daily calculation**: Partition by tenant_id, process in parallel batches of 10K players. Aggregation queries run against **read replica** to avoid impacting OLTP. Pre-compute daily aggregates via materialized view or summary table.
- **VIP monthly evaluation**: Same partitioning strategy.
- **Dormancy check**: Single query with batch update.

### Database Optimization
- Partial indexes (`WHERE deleted = FALSE`) on all high-cardinality tables
- Read replicas for 360° view aggregation and RFM calculation (query-intensive, no writes)
- `t_vip_points_transaction` and `t_problem_gambling_alert` are append-only — **partitioned by month** (Phase 1 requirement at 100K+ players)

---

## 15. Kafka Consumer Idempotency

All event-driven write operations must be idempotent. Each consumed event carries a unique `event_id` (UUID).

**Pattern**: Before processing, check `event_id` existence in the target table's `event_id` column (unique constraint). If exists, skip. If not, insert atomically in the same transaction.

**Affected consumers**:
- `BetSettledConsumer` → `t_vip_points_transaction.event_id`
- `DepositCompletedConsumer` → deduplication via `t_deposit_event_log.event_id` (new table)
- `RiskScoreUpdatedConsumer` → `t_player_tag` upsert (idempotent by nature — tag either exists or doesn't)
- `DeviceFingerprintMatchConsumer` → same upsert pattern

---

## 16. Distributed Locking for Scheduled Jobs

All scheduled jobs acquire a Redisson `RLock` before execution. This prevents double processing in multi-instance deployments.

**Pattern**:
```java
RLock lock = redissonClient.getLock("job:player-dormancy");
if (lock.tryLock(0, 30, TimeUnit.MINUTES)) { /* execute */ }
```

**Job Lock Names**:
| Job | Lock Name | Max Duration |
|-----|-----------|-------------|
| PlayerDormancyJob | `job:player-dormancy` | 30 min |
| VipMonthlyEvaluationJob | `job:vip-monthly-eval` | 60 min |
| VipPointsExpiryJob | `job:vip-points-expiry` | 30 min |
| VipPointsBalanceFlushJob | `job:vip-points-flush` | 5 min |
| RfmDailyCalculationJob | `job:rfm-daily-calc` | 60 min |
| ProblemGamblingBatchJob | `job:problem-gambling-batch` | 30 min |
| SlaEscalationJob | `job:sla-escalation` | 2 min |
| AffordabilityTriggerJob | `job:affordability-trigger` | 30 min |
| PlayerRegistrationRetryJob | `job:registration-retry` | 10 min |

---

## 17. Security Considerations

### 17.1 Webhook Security
KYC webhook endpoints (`POST /igaming/kyc/webhook/{provider}`):
- **Signature verification**: HMAC-SHA256 signature in request header
- **Timestamp validation**: Reject webhooks >5 minutes old (prevent replay attacks)
- **Nonce tracking**: Store processed `X-Trace-ID` / nonce in Redis (TTL 10min) to prevent duplicate processing
- **Rate limiting**: Max 100 requests/minute per provider IP
- **IP whitelisting**: Configure allowed IPs per KYC provider in application config

### 17.2 360° View Data Masking
`PlayerViewAggregationService` applies role-based data masking:
- Standard CS agent: PII masked (email: j***@example.com, phone: 138****1234), bank details hidden
- Senior CS / Compliance: Full PII visible (requires `cs:player:view-pii` permission)
- KYC documents: Never exposed in 360° view — separate endpoint with `compliance:kyc:view` permission

### 17.3 Chatbot Input Sanitization
Before forwarding player messages to `IntentClassificationAdapter`:
- Content length limit: 2,000 characters
- Strip HTML/script tags
- The adapter implementation is responsible for prompt injection mitigation at the LLM level

### 17.4 Self-Exclusion Enforcement Recovery
The 5-minute GP settlement wait (Section 7.1 step 3) is implemented via:
- `t_self_exclusion_enforcement_task` table with status (PENDING_SETTLEMENT/COMPLETED/FORCE_ROLLBACK)
- `SelfExclusionSettlementCheckJob` runs every 30s, checks pending tasks
- If task age >5min and still PENDING → trigger force rollback
- Survives service restarts because state is persisted in database
