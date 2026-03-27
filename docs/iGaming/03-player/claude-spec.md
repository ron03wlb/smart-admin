# Complete Specification — 03-player (Player Domain)

> Synthesized from: spec.md (original requirements), claude-research.md (codebase + web research), claude-interview.md (stakeholder decisions)

---

## 1. Overview

The Player Domain (`03-player`) is the central module for all player-related operations in the iGaming platform. It covers three chapters:

- **Ch1 Player Management**: Lifecycle, registration, KYC, VIP, RFM segmentation, tagging
- **Ch15 Responsible Gambling**: Self-exclusion, deposit/loss limits, session protection, affordability assessment
- **Ch12 Customer Service**: 360° player view, multi-channel support, AI chatbot, ticketing/SLA

**Implementation approach**: Extend existing codebase (PlayerEntity, PlayerStateManager, SelfExclusion services, VIP services) to cover the full spec requirements.

**Scale target**: Large-scale operator (100K–1M+ players). Hot paths must be designed for high throughput.

---

## 2. Architecture Decisions (from Interview)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Implementation scope | Extend existing code | Build on existing Player/SelfExclusion/VIP code |
| KYC integration | Abstract adapter pattern | Provider-agnostic `KycVerificationAdapter` interface |
| AI Chatbot | Full integration design | LLM/NLP integration with intent routing and degradation |
| State machine migration | Clean break (new V7 migration) | No legacy data constraints |
| Self-exclusion external | Full adapter + GAMSTOP stub | `SelfExclusionGateway` interface with mock/real modes |
| Problem gambling detection | Hybrid real-time + batch | Critical indicators via Kafka; daily patterns via batch |
| 360° view aggregation | API Gateway aggregation | Query-time calls to other domain services |
| Affordability tiers | Full 3-tier as per spec | Basic/Enhanced/Full including Open Banking adapter |

---

## 3. Module Breakdown

### 3.1 player-lifecycle (Complexity: Medium)

**Scope**: §5.1 State Machine, §5.2 Registration, §5.10 Multi-Jurisdiction, §5.11 Data Retention

**State Machine** — 7 states:
- `REGISTERED → ACTIVE → DORMANT → SELF_EXCLUDED → COOLING_OFF → SUSPENDED → CLOSED`
- Transition rules enforced via `PlayerStateManager` (existing, to be expanded)
- Optimistic locking (CAS) via `@Version` column for concurrent safety
- Priority: Self-exclusion (regulatory) > AML/Risk > Operational transitions

**Registration** — 4 methods:
- Email, Phone, Social OAuth (Google/Facebook/Line), One-click (phone only)
- Initialization: status=REGISTERED, CASH wallet created (calls 02-funding), VIP=Bronze, bind tenant (immutable)
- Anti-duplicate: email/phone uniqueness via blind index, device fingerprint flagging (calls 05-risk-compliance API), IP rate limiting (≥3/24h → CAPTCHA)
- Age verification: min 18 (21 for some US states), cross-validated with KYC L1 document

**Data Retention**: Per-category retention policies with GDPR compliance (crypto-shredding delegated to 05-risk-compliance)

### 3.2 player-kyc (Complexity: High)

**Scope**: §5.3 KYC 4-Level, §3.1 SSOT

**4-Level KYC**:
- L0: Email/Phone OTP → instant auto-approve
- L1: Name + Gov ID + DOB → OCR auto (<30s) or manual fallback (≤24h)
- L2: L1 + Address proof (3mo) + ID photo → OCR auto (<1min) or manual (≤48h)
- L3: L2 + Bank verification + Source of Funds → 1-3 business days

**Integration**: Abstract adapter pattern (`KycVerificationAdapter` interface)
- Webhook-based async verification flow
- `KycVerificationStatus`: PENDING → IN_PROGRESS → APPROVED / REJECTED / RETRY
- Provider-agnostic: initial implementation with stub/mock, production adapter (Sumsub) later

**Multi-Jurisdiction Config**:
- UKGC: L1 before first deposit, L2 within 72h or freeze
- MGA: L1 deferred to first withdrawal, L2 within 90 days
- PAGCOR: Philippine gov ID accepted, GCash verification
- Curacao: L1 minimum, L2 on large withdrawals (>$2,000)

### 3.3 player-vip (Complexity: Medium)

**Scope**: §5.4 VIP 5-Tier, §5.5 VIP Points

**5-Tier System**: Bronze → Silver → Gold → Platinum → Diamond
- Upgrade: monthly valid turnover threshold, instant upgrade on reaching threshold
- Downgrade: 2 consecutive months below maintenance threshold, with protection periods
- Special: DORMANT doesn't trigger downgrade, SUSPENDED freezes evaluation

**VIP Points**:
- Earn: $1 valid turnover = 1 base point × VIP multiplier
- Game rates: Slots 100%, Table Games 50%, Sports 75%
- Expiry: 365 days rolling
- Redemption: 100 points = $1 cash bonus, free spins, VIP shop

### 3.4 player-segmentation (Complexity: Medium)

**Scope**: §5.6 RFM, §5.7 Tag System

**RFM Segmentation**:
- R/F/M scores (1-5) with defined thresholds
- 6 segments: Champions, Loyal, Potential, At Risk, Hibernating, New Players
- Daily recalculation at 02:00 UTC
- Segment changes trigger marketing automation

**Tag System**:
- System auto-tags: HIGH_RISK, BONUS_ABUSER, MULTI_ACCOUNT, WHALE, PROBLEM_GAMBLER, CHURN_RISK, PEP, SANCTION_HIT
- Manual tags: VIP manager/CS supervisor with audit trail
- Priority: SANCTION_HIT highest (instant freeze), conflicting tags → risk manager decision

### 3.5 responsible-gambling (Complexity: Very High)

**Scope**: §6.1–§6.6, SSOT §3.2–§3.4

**Self-Exclusion**:
- Types: Temporary (24h–6wk), Medium (6mo–1yr), Permanent (lifetime, irrevocable)
- `SelfExclusionGateway` interface with jurisdiction adapters: GAMSTOP (stub+real mode), CRUKS, Spelpaus, ROFUS
- Token/Session revocation (v2.2): revoke GP tokens → notify GPs → wait 5min settlement → force rollback → freeze wallets → block agent credit
- GAMSTOP timeout handling (v2.2 GAP-7): 24h hold → 48h SUSPENDED → 72h forced freeze + regulatory report
- Anti-circumvention: device fingerprint + payment method + IP + re-registration detection (target: <5% evasion)

**Deposit/Loss Limits**:
- Types: daily/weekly/monthly
- Rules: lower = instant, raise = 24-72h delay + cooldown, layer validation (daily ≤ weekly ≤ monthly)
- Loss limit: zero tolerance for system-caused breaches; 80% warning, 100% hard stop

**Session Protection**:
- Time-Out: 24h–6wk options, auto-recover
- Forced rest: UKGC (10 deposits/24h → 60min), Germany (60min play → 5min), Sweden (session limit options)
- Reality Check: configurable intervals (15/30/60min), pause auto-spin, show session time + net P&L
- Idle logout: default 30min (configurable 15-60min)

**Problem Gambling Detection** (§3.3):
- 6 indicators with hybrid architecture:
  - Real-time via Kafka: deposit frequency (≥5/day), rapid deposit amount (>3x monthly avg), loss chasing (3 consecutive raise bets)
  - Batch (daily): continuous play (>4h), late-night frequency (00:00-06:00 >200% avg), lending signals (rapid small deposits)
- Actions: popup reminder → cooldown suggestion → self-exclusion option → CS intervention

**Affordability Assessment** (§3.4):
- Full 3-tier UKGC 2025 framework:
  - Basic (GBP 125-500 net loss): warning message
  - Enhanced (GBP 500-2,000): self-declaration (income, housing, dependents)
  - Full (>GBP 2,000): third-party verification (Open Banking + credit agency)
- Rolling 30-day net deposit ≥ GBP 150 trigger (Feb 2025 rule)
- Timeout handling (v2.2 GAP-6): allow current round → block new bets → suspend deposits → allow withdrawals
- Open Banking adapter for Full tier (abstract interface, provider-agnostic)

### 3.6 customer-service (Complexity: High)

**Scope**: §7.1–§7.7

**360° Player View**:
- API Gateway aggregation pattern (query-time calls to other domains)
- Data: identity, financial, betting behavior, segments/tags, CS history, risk data
- Operator panel: manual top-up (senior+), force logout, password reset, account unlock

**Multi-Channel Support**:
- 5 channels: Live Chat, Telegram, Email, WhatsApp, Phone (VIP only)
- SLA per channel and VIP tier
- Unified conversation view across channels

**AI Chatbot**:
- Intent classification (target ≥90% accuracy)
- 6 intent types with confidence thresholds
- Auto-escalation: confidence <0.7, player request, sensitive topics, 3+ unresolved, Diamond VIP
- Degradation strategy: Normal → Degraded (P99>3s) → Unavailable → Recovery (3 health checks)
- Full LLM/NLP integration design

**Ticketing & SLA**:
- Type-based SLA (Urgent 5min → Low 2h first response)
- VIP differentiated SLA (Diamond 5min/2h → Bronze 2h/24h)
- Escalation: 50% SLA → Team Lead, 100% → Manager, Diamond complaint → VIP Manager
- Violation penalties: KPI records, configurable compensation ($5 default), monthly caps
- Auto-close: Pending Customer +72h, Resolved +24h

**Knowledge Base**: 145+ articles, 6 languages, role-based access

**Routing**: VIP → Skill match → Language → Load balance (max 20 tickets/person)

---

## 4. Cross-Domain Integration Points

| Integration | Direction | Mechanism |
|------------|-----------|-----------|
| tenant_id binding | 01 → 03 | SmartAdminBaseEntity auto-fill |
| RBAC permissions | 01 → 03 | Sa-Token @SaCheckPermission |
| CASH wallet creation | 03 → 02 | Service call on registration |
| KYC status query | 04 ← 03 | Service API |
| VIP level query | 04 ← 03 | Service API |
| Risk score consumption | 05 → 03 | Tag system consumes risk scores |
| Device fingerprint | 05 ← 03 | API call for anti-duplicate/anti-circumvention |
| Self-exclusion → GP | 03 → 04 | Domain event (Kafka) |
| Self-exclusion → Wallet | 03 → 02 | Domain event (Kafka) |
| 360° view aggregation | All → 03 | API Gateway pattern (query-time) |
| Marketing restriction | 03 → 04 | DND flag |

---

## 5. Technical Patterns (from Codebase Research)

- **Architecture**: Controller → Service → Manager → DAO (ArchitectureTest enforced)
- **Entity**: `SmartAdminBaseEntity` + `@TableName` + `@Version` + PII encryption (AES-256-GCM + blind index)
- **Service**: Vavr `Option`, NO `@Transactional`
- **Manager**: `@Transactional(rollbackFor = Throwable.class)`, domain event publishing
- **State transitions**: CAS update with `WHERE status = #{expected} AND version = #{version}`
- **Error codes**: `303xx` range for Player module
- **Database**: `t_` prefix, SMALLINT enums, DECIMAL(19,4) money, TIMESTAMPTZ, partial indexes
- **Events**: Kafka via `DomainEventPublisher`, topics in `IgamingKafkaConst`

---

## 6. Success Metrics

### Player Management
- Registration completion ≥ 85%
- KYC L1 auto-pass ≥ 70%, auto processing < 30s, manual < 24h
- VIP retention (Gold+) ≥ 60%/year

### Responsible Gambling
- Self-exclusion evasion < 5%
- GAMSTOP sync > 99.9%
- System-caused limit breach: 0 (zero tolerance)
- Affordability assessment coverage: 100% of qualifying players

### Customer Service
- FCR > 80%, AHT < 15min, CSAT > 90%
- SLA achievement > 95%
- AI intent accuracy > 90%, auto-resolution > 40%

### Lifecycle Funnel
- Visit→Register: 15-25%, Register→First Deposit: 30-40%, FTD→D7 Active: 50-60%
