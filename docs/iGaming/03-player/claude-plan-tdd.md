# TDD Plan — 03-player (Player Domain)

> Companion to `claude-plan.md`. Defines tests to write BEFORE implementing each section.
> **Framework**: JUnit 5 + ArchUnit + Testcontainers (integration). Existing patterns from codebase research.
> **Convention**: Unit tests in `src/test/java/`, integration tests marked `@Tag("integration")`.

---

## 2. Database Migration (V7)

### Migration Validation Tests
- Test: V7 migration applies cleanly on a fresh database (Testcontainers PostgreSQL)
- Test: V7 migration applies correctly after V1-V6 (sequential migration)
- Test: Data transformation maps all old `PlayerStatusEnum` values to new values correctly
- Test: CHECK constraints reject invalid status values (0, 8, -1)
- Test: Unique constraint on `t_deposit_limit(tenant_id, player_id, period)` prevents duplicates
- Test: Unique constraint on `t_loss_limit(tenant_id, player_id, period)` prevents duplicates
- Test: `t_vip_points_transaction.event_id` unique constraint prevents duplicate inserts
- Test: Table partitioning on `t_vip_points_transaction` works correctly for cross-month queries

---

## 3. Module: player-lifecycle

### 3.1 State Machine Tests
- Test: Each valid transition in the transition map succeeds (11 transitions)
- Test: Invalid transitions are rejected with `INVALID_STATUS_TRANSITION` error
- Test: CAS update succeeds when version matches
- Test: CAS update fails when version is stale (concurrent modification)
- Test: Audit log is inserted for every successful transition (same transaction)
- Test: Kafka `PLAYER_STATUS_CHANGED` event is published after transition
- Test: Self-exclusion transition takes precedence when concurrent with risk flag (CAS ordering)
- Test: REGISTERED → ACTIVE requires KYC L1 for UKGC jurisdiction
- Test: REGISTERED → ACTIVE allowed without KYC L1 for non-UKGC jurisdictions

### 3.2 Registration Tests
- Test: Email registration creates player with status=REGISTERED, VIP=Bronze
- Test: Email registration creates CASH wallet via 02-funding service call
- Test: Phone registration uses SMS OTP with 5min TTL
- Test: Social OAuth registration requires DOB supplement
- Test: One-click registration creates player with minimal data
- Test: Duplicate email (blind index match) is rejected with `EMAIL_ALREADY_EXISTS`
- Test: Duplicate phone (blind index match) is rejected with `PHONE_ALREADY_EXISTS`
- Test: Same device fingerprint + different email allows registration but adds tag
- Test: 3+ registrations from same IP in 24h triggers CAPTCHA requirement
- Test: Registration records source (landing page, promo code, agent code, IP, device fingerprint)
- Test: Failed wallet creation marks player as REGISTRATION_INCOMPLETE
- Test: REGISTRATION_INCOMPLETE player cannot proceed (login, deposit blocked)
- Test: Retry job attempts wallet creation for incomplete registrations

### 3.3 Dormancy Tests
- Test: Player with last_login > 90 days transitions to DORMANT
- Test: Player with last_login < 90 days remains ACTIVE
- Test: DORMANT player with 365+ days inactivity + zero balance transitions to CLOSED
- Test: DORMANT player with 365+ days inactivity + non-zero balance remains DORMANT

### 3.4 Permission Matrix Tests
- Test: REGISTERED player can browse games but cannot deposit
- Test: ACTIVE player can perform all operations (subject to KYC limits)
- Test: DORMANT player can login and deposit but withdrawal requires re-verification
- Test: SELF_EXCLUDED player is blocked from all operations including login
- Test: COOLING_OFF player can view profile but cannot bet or deposit
- Test: SUSPENDED player can view balance but cannot transact
- Test: CLOSED player has no access

---

## 4. Module: player-kyc

### 4.1 Adapter Tests
- Test: StubKycVerificationAdapter returns configurable APPROVED result
- Test: StubKycVerificationAdapter returns configurable REJECTED result
- Test: StubKycVerificationAdapter returns configurable RETRY result
- Test: Adapter interface contract: submitDocument returns providerReference
- Test: Adapter interface contract: checkStatus returns valid KycVerificationStatus

### 4.2 KYC Workflow Tests
- Test: L0 verification auto-approves on OTP success (no adapter call)
- Test: L1 submission creates verification record with status=PENDING
- Test: L1 submission calls adapter.submitDocument with correct request
- Test: Webhook processing updates verification status to APPROVED
- Test: Webhook processing updates player kyc_level on APPROVED
- Test: Webhook processing publishes KYC_LEVEL_CHANGED event
- Test: REJECTED webhook preserves current kyc_level
- Test: RETRY webhook preserves current kyc_level, allows resubmission

### 4.3 Multi-Jurisdiction Tests
- Test: UKGC jurisdiction blocks first deposit if KYC L1 not completed
- Test: UKGC jurisdiction freezes account if L2 not completed within 72h
- Test: MGA jurisdiction allows deposit before KYC L1
- Test: MGA jurisdiction requires L2 within 90 days
- Test: PAGCOR jurisdiction accepts Philippine government ID
- Test: Curacao jurisdiction triggers L2 on withdrawal > $2,000

---

## 5. Module: player-vip

### 5.1 VIP Tier Tests
- Test: Monthly evaluation upgrades player from Bronze to Silver at $10K threshold
- Test: Instant upgrade triggered mid-month when threshold reached
- Test: Downgrade occurs after 2 consecutive months below maintenance threshold
- Test: Protection period prevents downgrade (Diamond 3mo, Platinum 2mo, others 1mo)
- Test: DORMANT player is not evaluated for downgrade
- Test: SUSPENDED player freezes VIP evaluation
- Test: Manual upgrade requires reason and creates audit record

### 5.2 VIP Points Tests
- Test: Bet settlement earns correct points (turnover × gameRate × vipMultiplier)
- Test: Slots earn 100% rate, Table Games 50%, Sports 75%
- Test: VIP multiplier applied correctly per tier (1x–5x)
- Test: Points older than 365 days are expired by daily job
- Test: Redemption fails when balance insufficient
- Test: Redemption at 100 points = $1 conversion rate
- Test: Redis balance counter increments atomically on earn
- Test: Balance flush job syncs Redis to database
- Test: Duplicate event_id does not earn double points (idempotency)

---

## 6. Module: player-segmentation

### 6.1 RFM Tests
- Test: Recency score 5 for activity within 7 days
- Test: Recency score 1 for activity > 60 days ago
- Test: Frequency score 5 for ≥100 bets in 30 days
- Test: Monetary score 5 for ≥$10K turnover in 30 days
- Test: Champions segment assigned for R5F5M5
- Test: Hibernating segment assigned for R1F1M1
- Test: New Players segment assigned for R5F1M1
- Test: Segment change publishes RFM_SEGMENT_CHANGED event
- Test: 2+ level segment drop publishes RFM_ANOMALY_DETECTED event
- Test: Daily job processes players in batches of 10K

### 6.2 Tag Tests
- Test: System auto-tag created when risk score ≥ 70 (HIGH_RISK)
- Test: Manual tag requires operator_id and reason
- Test: SANCTION_HIT tag triggers immediate account freeze
- Test: HIGH_RISK + WHALE combination routes to manual review
- Test: Duplicate tag insertion is idempotent (upsert)
- Test: Tag query by name and tenant_id uses composite index

---

## 7. Module: responsible-gambling

### 7.1 Self-Exclusion Tests
- Test: Temporary exclusion (24h) auto-expires
- Test: Medium exclusion (6mo) requires cooldown period for reactivation
- Test: Permanent exclusion is irrevocable
- Test: Exclusion triggers token revocation (all GP tokens)
- Test: Exclusion freezes all wallets (CASH/BONUS/CREDIT)
- Test: Exclusion sets marketing DND flag
- Test: GAMSTOP gateway mock mode returns configurable result
- Test: GAMSTOP timeout 0-24h: account held, awaiting review
- Test: GAMSTOP timeout 24h: escalated to compliance manager
- Test: GAMSTOP timeout 48h: account upgraded to SUSPENDED
- Test: GAMSTOP timeout 72h: forced freeze + regulatory report
- Test: GAMSTOP match NEVER auto-releases on timeout
- Test: Self-exclusion enforcement task persists across service restarts
- Test: Force rollback triggered after 5min unsettled GP rounds

### 7.2 Deposit/Loss Limit Tests
- Test: Lower deposit limit takes effect immediately
- Test: Raise deposit limit creates pending increase with 24-72h delay
- Test: Player can cancel pending increase during cooldown
- Test: Layer validation: daily ≤ weekly ≤ monthly (rejects invalid combos)
- Test: Deposit at 100% of limit is blocked
- Test: Loss limit at 80% triggers warning event
- Test: Loss limit at 100% blocks further activity
- Test: Atomic enforcement: check + deduct in same transaction (Manager layer)
- Test: Unique constraint prevents duplicate limit per (tenant, player, period)

### 7.3 Session Protection Tests
- Test: Time-out blocks all activity for selected duration
- Test: Time-out auto-recovers when duration expires
- Test: UKGC forced rest: 10 deposits in 24h triggers 60min rest
- Test: Germany forced rest: 60min continuous play triggers 5min break
- Test: Reality check fires at configured interval (15/30/60min)
- Test: Reality check pauses auto-spin
- Test: 10+ rapid dismissals (<5s each) suggest increased frequency
- Test: Idle logout triggers at 30min (default)
- Test: Active game round excluded from idle timer

### 7.4 Problem Gambling Detection Tests
- Test: ≥5 deposits in 24h sliding window → MEDIUM alert
- Test: Deposit amount > 3x 30-day average → HIGH alert
- Test: 3 consecutive bet increases after losses → HIGH (loss chasing)
- Test: Session > 4h → MEDIUM alert (batch)
- Test: Late-night frequency > 200% average → MEDIUM (batch)
- Test: Rapid small deposits pattern → HIGH (batch)
- Test: HIGH alert publishes PROBLEM_GAMBLING_HIGH_RISK event
- Test: Alert write is transactional via ProblemGamblingAlertManager

### 7.5 Affordability Tests
- Test: Basic tier triggered at net loss GBP 125-500
- Test: Enhanced tier triggered at net loss GBP 500-2,000
- Test: Full tier triggered at net loss > GBP 2,000
- Test: Rolling 30-day net deposit ≥ GBP 150 triggers vulnerability check
- Test: PASSED result: no intervention, set re-evaluation date
- Test: FAILED (Enhanced): auto-set deposit limit to disposable income × 10%
- Test: DECLINED: apply jurisdiction minimum defaults
- Test: TIMEOUT (>7 days): suspend deposits, allow play + withdrawals
- Test: Timeout allows current game round to complete before blocking new bets
- Test: Bonus wagering timer paused during assessment timeout

---

## 8. Module: customer-service

### 8.1 360° View Tests
- Test: Aggregation calls all 6 data sources in parallel
- Test: Partial data returned when one downstream service is unavailable
- Test: Circuit breaker opens after 50% failure rate
- Test: Timeout of 3s per downstream call
- Test: PII masking applied for standard CS agent role
- Test: Full PII visible for compliance officer role

### 8.2 Multi-Channel Tests
- Test: Message from LiveChat creates CsMessage with correct channel type
- Test: Message from Telegram creates CsMessage with Telegram metadata
- Test: Context auto-transfer attaches player ID, VIP level, balance
- Test: Agent capacity limit prevents assignment beyond max (configurable 3-5)

### 8.3 Chatbot Tests
- Test: DEPOSIT_ISSUE intent classified with confidence > 0.8 → auto-handle
- Test: COMPLAINT intent classified → immediate human transfer
- Test: Confidence < 0.7 → human transfer
- Test: 3+ unresolved auto-replies → human transfer
- Test: Diamond VIP → auto-route to VIP manager
- Test: Circuit breaker degrades to human-only when AI P99 > 3s
- Test: Recovery requires 3 consecutive health checks

### 8.4 Ticketing Tests
- Test: Ticket creation calculates SLA deadlines based on type + VIP level
- Test: Diamond VIP gets 5min first-response SLA
- Test: Bronze VIP gets 2h first-response SLA
- Test: SLA at 50% → Team Lead notified
- Test: SLA at 100% → Manager notified
- Test: 3+ re-opens → assigned to Senior agent
- Test: Auto-close: Pending Customer + 72h → closed
- Test: Auto-close: Resolved + 24h → closed

### 8.5 Routing Tests
- Test: Diamond player routed to VIP manager (priority 1)
- Test: Payment issue routed to payment-skilled agent (priority 2)
- Test: Language match applied when skill match is equal
- Test: Load balancing routes to agent with fewest tickets
- Test: Agent at max capacity (20 tickets) is skipped

---

## 9-10. Cross-Domain Events & API Endpoints

### Event Publishing Tests
- Test: Each state transition publishes correct event type to correct topic
- Test: Event payload contains required fields (playerId, oldStatus, newStatus, etc.)

### API Endpoint Tests (Integration)
- Test: All endpoints require authentication (except webhooks)
- Test: Permission-protected endpoints return 403 without correct permission
- Test: Pagination works correctly on query endpoints
- Test: Request validation rejects invalid forms (missing required fields)
- Test: Webhook signature verification rejects invalid signatures

---

## 15-17. Infrastructure Tests

### Kafka Consumer Idempotency Tests
- Test: Processing same event_id twice does not create duplicate records
- Test: Concurrent processing of same event_id — one succeeds, one skipped

### Distributed Locking Tests
- Test: Job acquires Redisson lock before execution
- Test: Second instance of same job skips execution when lock held
- Test: Lock released after job completion

### Security Tests
- Test: Webhook with timestamp > 5min old is rejected
- Test: Webhook with invalid HMAC signature is rejected
- Test: Rate limiter blocks > 100 webhook requests/minute
- Test: Chatbot message > 2,000 characters is truncated/rejected
