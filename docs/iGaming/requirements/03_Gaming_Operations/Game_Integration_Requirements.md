# Game Integration Requirements

> **Canonical Source**: [source-archive/00_Foundation/guides/00-12_Game_Integration_Implementation.md](../../source-archive/00_Foundation/guides/00-12_Game_Integration_Implementation.md)
> **Audience**: Executives, Product Managers
> **Related Doc**: [Game_Integration_Security.md](../../architecture/03_Game_Integration/Game_Integration_Security.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (Token generation Java code, HMAC-SHA256 signature algorithm, Base64 encoding, Redis anti-replay blacklist, Three-layer idempotency defense, Stalled transaction recovery jobs, Rate limiting Redisson implementation) moved to Architecture layer. This document focuses on business requirements only.

---

## 1. Overview

This document defines the business requirements for integrating new Game Providers (GPs) into the iGaming platform. It covers the core functional areas that must be supported, the partnership standards, and the acceptance criteria for each integration milestone.

---

## 2. Core Integration Capabilities

The platform must support the following capabilities when onboarding a new Game Provider:

| Capability | Description | Business Priority |
|-----------|-------------|-------------------|
| Seamless Wallet API | Unified wallet allowing players to use a single balance across all games from any GP | Critical |
| Token-Based Authentication | Secure session validation between the platform and GP systems | Critical |
| Idempotent Transaction Processing | Guarantee that duplicate requests (e.g., network retries) do not result in duplicate debits or credits | Critical |
| Transaction Cancellation | Ability to reverse a completed bet transaction upon GP request | High |
| Error Recovery | Automated detection and resolution of stalled or inconsistent transactions | High |
| Real-Time Balance Query | GP can query a player's current playable balance at any time | Critical |

---

## 3. Seamless Wallet Functional Requirements

### 3.1 Supported Operations

The Seamless Wallet integration must support four primary operations:

| Operation | Business Rule | Expected Outcome |
|-----------|--------------|------------------|
| **Debit (Bet Placement)** | Deduct the bet amount from the player's wallet when a bet is placed | Player balance decreases; transaction is recorded |
| **Credit (Win Payout)** | Add the win amount to the player's wallet when a round resolves | Player balance increases; transaction is recorded |
| **Cancel (Transaction Reversal)** | Reverse a previously completed debit or credit on GP request | Original transaction is reversed; balance is restored |
| **Balance Inquiry** | Return the player's current playable balance | GP receives up-to-date balance for display |

### 3.2 Transaction Integrity Rules

| Rule | Description |
|------|-------------|
| Single-use request identifiers | Every transaction request from a GP must carry a unique request ID |
| Idempotent responses | Repeated requests with the same request ID must return the same result without re-processing |
| Atomic balance updates | Each debit or credit must fully succeed or fully fail; partial updates are not permitted |
| Audit trail | Every transaction must be recorded with timestamp, player ID, GP ID, round ID, and amount |

---

## 4. Security Requirements

### 4.1 Token Authentication Standards

| Requirement | Specification |
|------------|---------------|
| Token composition | Must include player ID, tenant ID, timestamp, and cryptographic signature |
| Signature algorithm | Industry-standard cryptographic signature |
| Token validity window | Maximum 5 minutes (configurable) |
| Anti-replay protection | Each token must be single-use; used tokens must be rejected |
| Encoding | Secure encoding for transport |

→ **[Token Authentication Implementation](../../architecture/03_Game_Integration/Game_Integration_Security.md#token-authentication)** - HMAC-SHA256 algorithm, Base64 encoding, Java generateToken/validateToken methods

### 4.2 GP Onboarding Security Checklist

| Item | Requirement |
|------|------------|
| IP whitelisting | GP callback endpoints must be IP-whitelisted |
| HTTPS enforcement | All API communications must use TLS 1.2 or higher |
| API key rotation | GP API keys must support periodic rotation without downtime |
| Rate limiting | API endpoints must enforce per-GP rate limits to prevent abuse |

→ **[Security Implementation Details](../../architecture/03_Game_Integration/Game_Integration_Security.md#security-checklist)** - IP whitelisting configuration, TLS 1.2+ setup, rate limiting (Redisson), Redis anti-replay blacklist

---

## 5. Error Recovery Requirements

### 5.1 Stalled Transaction Policy

| Scenario | Business Rule | Maximum Resolution Time |
|----------|--------------|------------------------|
| Network timeout during debit | System must auto-detect and recover stalled transactions | 10 minutes |
| GP callback failure | System must retry or query GP for authoritative status | 10 minutes |
| Inconsistent state (debit recorded, GP says failed) | System must refund the player automatically | 10 minutes |
| Unknown transaction at GP | System must treat as failed and refund the player | 10 minutes |

### 5.2 Recovery Verification

After every automated recovery, the system must:
- Record the recovery action in the audit log
- Send an alert notification if the transaction status was FAILED or NOT_FOUND
- Ensure the player's balance is consistent with the resolved state

→ **[Stalled Transaction Auto-Recovery](../../architecture/03_Game_Integration/Game_Integration_Security.md#auto-recovery)** - Scheduled job configuration, SQL queries, GP status query API integration, refund automation

---

## 6. Planned Capabilities (Phase 5+)

### 6.1 Seamless Wallet API Advanced Features

| Feature | Status | Description |
|---------|--------|-------------|
| Free Spin Support | Planned | GP-initiated free spins with platform-funded bonus balance |
| Jackpot Contribution | Planned | Progressive jackpot contribution deducted per bet |
| Multi-Currency Support | Planned | Single wallet supporting bets in multiple currencies with real-time conversion |

### 6.2 Turnover Calculation Logic

| Feature | Status | Description |
|---------|--------|-------------|
| Three-Layer Validation | Planned | Layer 1 (Risk), Layer 2 (Finance), Layer 3 (Activity) turnover validation |
| Game Weight Configuration | Planned | Different game types contribute at different rates to wagering requirements |
| Free Spin Turnover Exclusion | Planned | Free spin bets typically excluded from wagering requirement calculations |

---

## 7. Acceptance Criteria

### 7.1 Integration Verification Checklist

| Criterion | Validation Method |
|-----------|------------------|
| Token authentication works correctly (signature, expiry, anti-replay) | Automated test suite |
| Idempotency verified (duplicate requests return identical results) | Load test with duplicate request injection |
| Concurrency verified (1,000 TPS without duplicate debits) | Concurrent load test |
| Cancel transaction correctly refunds player | Functional test |
| Error recovery mechanism operates within SLA | Chaos engineering test (network partition simulation) |
| Stalled transactions auto-recover | Scheduled task validation |
| All game transactions recorded in audit log | Audit log inspection |

### 7.2 Common Integration Pitfalls

| Pitfall | Mitigation |
|---------|-----------|
| Token replay attacks | Mandatory single-use token enforcement with server-side tracking |
| Idempotency layer failure | Multi-layer defense mechanism must be operational |
| Lock timeout too short | Lock hold time must exceed maximum business execution time |
| Stalled transaction accumulation | Automated recovery task must be monitored for continuous operation |

→ **[Idempotency Implementation](../../architecture/03_Game_Integration/Game_Integration_Security.md#idempotency-defense)** - Three-layer defense (Redis cache + DB unique constraint + Distributed lock), timeout configuration, fallback mechanisms

---

## Related Documents

### Business References
- [Turnover Business Rules](./Turnover_Business_Rules.md) - Wagering and turnover requirements

### Technical Implementation

→ **[Game Integration Implementation](../../architecture/03_Game_Integration/Game_Integration_Implementation.md)** - Complete game provider integration patterns, API specifications, token generation/validation algorithms, error handling, and testing strategies

**Additional Technical References**:
- [Turnover Calculation Logic (Architecture)](../../architecture/03_Game_Integration/Turnover_Calculation_Logic.md) - Turnover calculation technical design

---

**Document Version**: 4.0.0
**Last Updated**: 2026-02-08
**Maintainers**: Product Team & Game Integration Team
