# 02-funding Synthesized Specification — iGaming Funding Domain

> **Version**: 1.0
> **Date**: 2026-03-27
> **Sources**: spec.md (original requirements), claude-research.md (industry best practices), claude-interview.md (stakeholder decisions)
> **Domain**: Wallet System + Payment System

---

## 1. Domain Overview

The Funding Domain is the **core financial engine** of the iGaming platform. It manages two tightly coupled sub-systems:

- **Wallet System**: Three wallet types (CASH, BONUS, CREDIT), balance calculation, transaction lifecycle, Seamless Wallet protocol for game provider integration, and reconciliation.
- **Payment System**: Multi-PSP smart routing, deposit/withdrawal flows, chargeback management, multi-currency support including cryptocurrency, and 3D Secure/SCA compliance.

All other domains depend on this domain for financial operations: game providers call the Seamless Wallet API for bets/wins, the player domain queries balances, the risk domain monitors transaction events, and the data domain consumes transaction logs.

### 1.1 Dependency: 01-governance-agent

The funding domain depends on the governance domain for:
- `tenant_id` on all API requests (multi-tenant isolation)
- RBAC permission checks
- Agent credit quota management
- Approval workflow for large transactions

**Strategy (Stakeholder Decision)**: Design against defined interface contracts. The funding domain builds its own test stubs/mocks for independent development. Stubs are swapped for real governance APIs at integration time.

---

## 2. Wallet System Requirements

### 2.1 Three Wallet Types

| Wallet | Type | Nature | Withdrawal | Initial |
|--------|------|--------|-----------|---------|
| CASH | Player funds | Self-owned | Free (per KYC limits) | 0 |
| BONUS | Promotional | Conditional | After wagering requirement met | 0 |
| CREDIT | Agent credit | Liability | Never (only winnings extractable) | 0 (agent-set) |

All three wallets initialize at 0 when a player account is created. Each wallet holds a single currency (the merchant's settlement fiat currency).

### 2.2 Playable Balance

**Formula (SSOT)**: `Playable Balance = (CASH + BONUS) - Locked - InProgress`
**Credit mode**: `Playable Balance = CreditLimit - UsedCredit`
**Accuracy**: 99.99% required.

### 2.3 Deduction Priority (SSOT — this domain owns)

**Default**: BONUS → CASH → CREDIT

**Four-layer override** (highest to lowest):
1. Player level (e.g., Diamond VIP prefers CASH)
2. GP contract (e.g., GP doesn't accept BONUS)
3. Game level (e.g., Live Casino = CASH only)
4. System default

Every transaction records which deduction layer was applied (audit trail).

**Known Conflict Resolution**: Ch2 §2.7 Scenario H incorrectly says "CASH first". Stakeholder ruling: §2.4 is authoritative. BONUS → CASH is the default unless overridden by Layer 1-3.

### 2.4 Transaction Models

- **Transaction-Based** (recommended): Each request independent, globally unique transactionId
- **Round-Based** (most common): RoundId groups Bet/Win/Rollback within a game round

### 2.5 Round Lifecycle State Machine

```
[*] → OPEN (Bet success)
OPEN → OPEN (additional Bet)
OPEN → CLOSED (Win/Loss settlement)
OPEN → CANCELLED (Rollback)
OPEN → TIMEOUT (2h no settlement)
TIMEOUT → CLOSED (GP query confirms)
TIMEOUT → PENDING_REVIEW (GP query fails)
PENDING_REVIEW → CLOSED (manual confirm)
PENDING_REVIEW → CANCELLED (manual cancel)
CLOSED → ADJUSTED (Resettlement)
```

**Orphan round detection**: Every 15min scan; 2h timeout threshold; query GP API first; escalate to manual if GP unavailable. SLA: $1,000+ rounds resolved within 24h.

### 2.6 Nine Transaction Scenarios

| # | Scenario | Key Challenge | Decision |
|---|----------|--------------|----------|
| A | Insufficient balance | Multi-wallet check | Skip BONUS if game doesn't support it |
| B | Concurrent bets | Same player, multiple games | Serialize via distributed lock |
| C | Timeout/retry | Idempotency | Same transactionId → return cached result; **TTL 24h** (extended from spec's 1h per stakeholder decision) |
| D | Out-of-order | Win before Bet | **Park Win for 30min**, retry when Bet arrives |
| E | Rollback/refund | Reverse transaction | Missing original → return Success (idempotent) |
| F | Resettlement | Difference adjustment | Allow controlled negative balance |
| G | Free spin | Bet=0 | Validate free spin quota; Win>0 → credit CASH |
| H | Bonus wallet bet | Dual-wallet deduction | Follow §2.4 priority: BONUS → CASH |
| I | Jackpot/large win | Risk review | Win>$10K → manual approval or auto-credit+freeze (per GP contract) |

### 2.7 Seamless Wallet Protocol (SSOT — this domain owns)

**Five endpoints**:

| # | Endpoint | Mutates Balance | Key Requirements |
|---|----------|----------------|-----------------|
| 1 | GetBalance | No (read-only) | <50ms response; lenient token validation |
| 2 | Debit (Bet) | Yes (decrease) | Strict token; idempotent; concurrent-safe |
| 3 | Credit (Win) | Yes (increase) | Strict token; idempotent |
| 4 | Rollback | Yes (restore) | Strict token; idempotent |
| 5 | Adjust | Yes (±) | Strict token; idempotent; resettlement |

**Atomicity**: All wallet updates are ACID transactions.
**Idempotency**: All endpoints require `transactionId` (globally unique); duplicates return cached response; **cache TTL: 24 hours** (stakeholder decision, extended from 1h).
**Token**: HMAC-SHA256, 5-minute validity, one-time use (anti-replay). Token expiry on Win → GP uses Resettlement endpoint or CS ticket to re-credit.

### 2.8 Bonus Wallet Rules

- Wagering requirement: `bonusAmount × multiplier` (typically 20x-40x)
- Valid bet: `BetAmount × RiskFactor(0/1) × GameWeight` (Standard Principal Method)
- Game weights: Slots 100%, Sports 50%, Baccarat configurable (default 10%), etc. — **owned by gaming domain (Ch5 §5.3)**, this domain reads config
- Conversion to CASH: Auto-convert when wagering met; cap = original bonus amount; excess profit does not convert; expired = forfeit (24h warning)

### 2.9 Credit Wallet Rules

- Agent sets credit limit (depends on governance domain)
- Settlement: Agent-defined cycle (typically weekly Mon 12:00 UTC)
- Net P&L settlement: player wins → agent pays; player loses → player pays
- Cannot withdraw; not eligible for promotions

### 2.10 Negative Balance Policy

- Player-initiated bets: **NEVER** allow negative balance
- System-forced only (GP Rollback excess, Resettlement clawback): controlled negative
- Lock account within ≤5 seconds of negative detection
- Compensation order: BONUS → CASH → manual intervention
- Alert thresholds: single account > -$100; platform-wide > -$10,000
- Only RISK_MANAGER role can unlock

### 2.11 Three-Layer Reconciliation

| Layer | Frequency | Source | Purpose |
|-------|-----------|--------|---------|
| Real-time | Instant | GP Debit/Credit | Live balance tracking |
| Reconciliation | Hourly | Platform vs GP reports | Discrepancy detection |
| Analytics | Daily 02:00 UTC | Data warehouse | Daily summaries |

**Discrepancy rules**: <$1 auto-correct; <5min time diff auto-match; >$100 manual alert; >10 missing/hr → check GP API; 3+ consecutive hours failed → pause game.

**Authority (Stakeholder Decision)**: GP is always authoritative — both for amount disputes AND transaction existence disputes. If GP says a transaction doesn't exist, platform reverses it.

### 2.12 Currency & Precision

- All amounts: **4 decimal places** (0.0001)
- No cumulative rounding error beyond 0.0001
- One wallet = one currency = merchant's settlement fiat
- **Deposit-time conversion**: Any currency → instant FX → settlement fiat wallet
- FX rate updates: Fiat every 5min; Crypto every 30sec
- FX rate freeze: 15min after deposit/withdrawal confirmation

### 2.13 FX Risk Sharing Matrix (SSOT — this domain owns)

| Currency Type | Platform Absorbs | Beyond Threshold |
|--------------|-----------------|------------------|
| Fiat | ≤0.5% fluctuation | 50/50 split platform:player |
| Crypto | ≤2% fluctuation | Player bears (UI confirmation required) |
| Agent settlement | 0% | Agent bears 100% (settlement day rate) |

All values DB-configurable (param_keys: `ch2.fx.fiat_absorption_pct`, `ch2.fx.crypto_absorption_pct`).

---

## 3. Payment System Requirements

### 3.1 Payment Methods

**Fiat**: Bank transfer (Wire/ACH/SEPA), Cards (VISA/MC/AMEX), E-wallets (LinePay/GCash/PayPal/Skrill/Momo), QR (Alipay/WeChat Pay/PIX)
**Crypto** (Phase 1 — stakeholder decision): USDT (TRC20/ERC20), BTC (≥3 confirmations), ETH (≥12 confirmations)

### 3.2 Jurisdiction Payment Restrictions

Dynamic whitelist per player jurisdiction. No hardcoding.
- UK (UKGC): Credit cards banned (2020-04)
- Australia: Credit cards banned (2026-04 — prepare ahead)
- Germany: All credit cards banned (2021)
- Sweden: Expanded ban (2025+)

### 3.3 PSP Smart Routing (5+ PSPs at launch — stakeholder decision)

**Single-pass routing** (stakeholder decision): Jurisdiction compliance is a scoring factor (ineligible PSP = score 0), not a separate pre-filter. All logic in one pass.

**Scoring weights**:
| Factor | Weight |
|--------|--------|
| Success rate (rolling window) | 50% |
| Processing fee | 30% |
| Settlement speed | 15% |
| VIP priority | 5% |

**PSP health states**:
| State | Success Rate | Action |
|-------|-------------|--------|
| Healthy | ≥80% | Normal routing |
| Degraded | 50%-80% | Reduce weight, split to backup |
| Unavailable | <50% | Full failover; 3 consecutive health checks to restore |

**Circuit breaker**: Closed → Open (threshold exceeded) → Half-Open (probe after cooldown) → Closed. Switchover target: <200ms.

### 3.4 Deposit Flow

Standard: Player initiates → Order created (Pending) → PSP page → Payment → PSP callback → Verify → Credit wallet → Success → Notify

**Cashier friction tiers** (v2.2):
| Risk Level | Criteria | Max Steps |
|-----------|----------|-----------|
| Low | KYC L2+, 3+ deposits, risk<30 | ≤3 |
| Medium | KYC L1, risk 30-70, new payment method | ≤5 |
| High | KYC L0, risk≥70, jurisdiction requirement | ≤7 |

### 3.5 Withdrawal Flow

**Approval tiers**:
| Amount | Approver | SLA |
|--------|----------|-----|
| <$100 | CS Agent | Instant |
| $100-$1K | CS Supervisor | 1h |
| $1K-$10K | CFO | 4h |
| >$10K | CFO + CEO | 24h |

**Wagering verification** (on withdrawal request):
1. Lock amount instantly (<50ms)
2. Sync query wagering progress (Ch5 §5.3 Layer 3, SLA <200ms)
3. Wagering met → auto-convert BONUS → CASH → proceed to withdrawal
4. Not met → reject + show remaining wagering
5. **Degradation**: Query timeout >500ms → queue as `WAGERING_CHECK_PENDING`; **NEVER auto-approve**; 2h timeout → escalate to CS

**Holiday SLA**: Bank transfers exclude non-business days; e-wallets/crypto are 24/7.

### 3.6 Chargeback Lifecycle

```
NOTIFICATION → EVIDENCE_COLLECTION (7d) → REPRESENTMENT
  → WON | LOST | PRE_ARBITRATION → ARBITRATION → WON | LOST
LOST → PLAYER_ACTION
```

**Evidence collection (Stakeholder Decision — Hybrid)**: System auto-gathers transaction logs, login history, device fingerprint, play history, IP records. CS team reviews, edits, and submits to PSP within 7-day window. System tracks deadlines and SLA.

**Player penalty escalation**: 1 CB → FLAG; 2 CB → restrict to e-wallet/crypto only; 3 CB → freeze + manual review; cumulative >$1K → permanent card ban.

**Monitoring**: CB rate ≥0.5% yellow; ≥0.8% orange; ≥1.0% red (notify CFO); >10/day → instant alert.

### 3.7 3D Secure / SCA + Crypto AML

- EU (PSD2): 3DS 2.0 mandatory for all card transactions
- UK: SCA for >GBP 30 or cumulative >GBP 100
- **Crypto AML**: All crypto deposits require wallet address risk scoring; high-risk addresses (mixers/darknet) → reject; single deposit >$10K equivalent → Enhanced Due Diligence

---

## 4. Cross-Domain Interfaces

### 4.1 Inbound (this domain consumes)

| Source | Interface | Purpose | SLA |
|--------|-----------|---------|-----|
| 01-governance | tenant_id + RBAC | Tenant isolation, permissions | Every request |
| 01-governance | Agent credit quota API | Credit wallet limit management | Real-time |
| 03-player | KYC level + withdrawal limits | Withdrawal verification | <50ms |
| 04-gaming | Wagering progress query | Withdrawal wagering check | <200ms |
| 04-gaming | Game weight config | Wagering contribution calculation | Config read |
| 05-risk | Risk score | Cashier friction tier, transaction risk | <100ms |

### 4.2 Outbound (this domain provides)

| Consumer | Interface | Purpose |
|----------|-----------|---------|
| 04-gaming (GPs) | Seamless Wallet 5 endpoints | Bet/Win/Rollback/Adjust |
| 04-gaming | GetBalance | In-game balance display |
| 03-player | Balance query | Account overview |
| 05-risk | **Kafka event stream** (stakeholder decision) | Real-time transaction monitoring |
| 07-data | Transaction logs (Kafka) | Reporting and analytics |

### 4.3 Cross-Module Boundary Scenarios (from Appendix E)

| ID | Scenario | Decision |
|----|----------|----------|
| BS-01 | Token expired × wagering | Wagering counts; Credit via Resettlement |
| BS-02 | Bonus expired × unsettled bet | Forfeit unused BONUS; unsettled Win → CASH |
| BS-03 | GP maintenance × round timeout | Normal timeout flow; re-query GP after maintenance |
| BS-04 | Self-exclusion × agent credit settlement | Settle existing; block new bets |
| BS-05 | Withdrawal × wagering verification | Sync blocking query; never auto-approve on failure |
| BS-07 | KYC upgrade × in-progress TX | Current TX completes; new TX uses new KYC limits |
| BS-08 | Account freeze × unsettled bet | Unsettled bets settle normally; new bets blocked |

---

## 5. Non-Functional Requirements

### 5.1 Scale Targets (Stakeholder Decision)

- **Seamless Wallet peak**: 5,000-20,000 concurrent requests/sec
- Platform peak TPS: 120 (design capacity 300)
- GetBalance response: <50ms
- Balance check: <100ms
- Concurrent lock handling: <200ms

### 5.2 Architecture Decisions (from Interview + Research)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Concurrency | Redisson distributed lock + DB atomic conditional update | Double-layer: app-level serialization + DB-level safety net for 5K-20K req/sec |
| Events | Kafka topics for async events + sync API for pre-TX risk | Hybrid: risk domain needs blocking pre-TX check AND async monitoring |
| Idempotency store | Redis Cluster with 24h TTL | Industry standard; supports GP late-retry scenarios |
| Currency precision | BigDecimal (String constructor) + DB DECIMAL(19,4) | Never float/double; Money value object pattern (JSR 354 or custom) |
| Crypto | Phase 1 full support | BTC/ETH/USDT deposit and withdrawal at launch |
| GP dependencies | Contract-first + local test stubs | Independent development; swap stubs for real APIs at integration |

### 5.3 Monitoring KPIs

**Wallet**:
- Playable balance accuracy: 99.99%
- Transaction success rate: ≥99.95%
- Idempotency coverage: 100%
- Daily reconciliation match: ≥99.9%
- Orphan round resolution (24h): ≥95%
- Zero over-deduction: 100% (any = P0)
- Out-of-order frequency: <0.1% (alert >0.5%)
- Cumulative negative balance: $0 target (alert <-$10K)

**Payment**:
- Deposit success rate: ≥95%
- Drop rate: <1% (alert >3%)
- Credit success rate: >95% (alert <90%)
- Average credit latency: <30min (alert >2h)
- Reconciliation accuracy: ≥99.9%
- Routing optimality: ≥85%
- Zero fund loss: 100% (any = P0)
- PSP API success rate: >99% (alert <95%)

---

## 6. Sub-Module Architecture

| Sub-Module | Scope | Complexity |
|-----------|-------|-----------|
| wallet-core | Three wallets CRUD, playable balance, deduction priority engine | High |
| wallet-transaction | Nine scenarios, round lifecycle state machine | Very High |
| seamless-wallet-api | 5 endpoints, token validation, idempotency | High |
| wallet-reconciliation | Three-layer recon, orphan detection | Medium |
| payment-gateway | PSP integration, smart routing, circuit breaker | High |
| payment-deposit | Deposit flow, cashier friction tiers, limit checks | Medium |
| payment-withdrawal | Withdrawal approval, wagering verification, holiday SLA | Medium |
| payment-dispute | Chargeback lifecycle, evidence auto-collection, player penalties | Medium |
| currency-engine | Multi-currency FX, precision control, crypto integration | High |
