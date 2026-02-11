# Game Integration Standards

> **Canonical Source**: [03-01_Game_Integration_Standard.md](../../source-archive/03_Game_Center/03-01_Game_Integration_Standard.md)
> **Audience**: Executives, Product Managers, Operations Leads, Compliance Officers
> **Related Doc**: [Game Integration Protocols (Architecture)](../../architecture/03_Game_Integration/Game_Integration_Protocols.md)
> **Last Synced**: 2026-02-09
>
> **Refinement Note**: Technical details (HMAC-SHA256, TLS, Type A/B/C classifications, HTTP status codes) moved to Architecture layer. This document focuses on business requirements only.

---

## Business Value

This standards document delivers strategic value by:
- **Player Experience**: Defines Seamless Wallet architecture enabling players to access any integrated game without manual fund transfers
- **Platform Protection**: Establishes RTP automatic suspension thresholds (RTP >200% AND Net Loss >$10K triggers auto-disable) to prevent catastrophic losses from GP bugs
- **Operational Efficiency**: Specifies new game onboarding in <5 business days with standardized certification requirements
- **Revenue Assurance**: Documents jackpot handling to correctly attribute network jackpots (GP-funded) vs normal wins (merchant-funded), preventing merchant insolvency

---

## Acceptance Criteria

- [ ] All GP integrations satisfy communication standards (RESTful API, encrypted transport, IP whitelist, cryptographic signatures)
- [ ] GetBalance, Transaction (Bet/Win), and CheckToken endpoints function correctly for all integrated providers
- [ ] Duplicate requests with same transaction ID never result in duplicate charges (idempotency test)
- [ ] API timeout handling marks transactions as "Pending" and queries final status (no direct rollback)
- [ ] Jackpot transactions are correctly identified and frozen pending GP verification
- [ ] RTP monitoring triggers warning alert at RTP >120% AND Net Loss >$5K (5-min window)
- [ ] RTP monitoring auto-disables game at RTP >200% AND Net Loss >$10K (5-min window)
- [ ] Game provider integration achieves API uptime ≥99.9%
- [ ] Transaction processing latency <200ms P95
- [ ] Game launch success rate >99.5%

---

## 1. Overview

The platform integrates with external Game Providers (GP) using a **Seamless Wallet (Single Wallet)** architecture. Players can access any integrated game without manual fund transfers between wallets. This document defines the partnership standards, certification requirements, and SLA expectations for all game provider integrations.

---

## 2. Provider Partnership Standards

### 2.1 Communication Protocol Requirements

All game provider integrations must satisfy the following communication standards:

| Requirement | Standard |
|-------------|----------|
| API Format | Industry-standard RESTful API with JSON payloads |
| Transport Security | Encrypted transport mandatory |
| Network Access Control | IP whitelist enforcement |
| Request Authentication | Cryptographic signature verification |

→ **[Technical Implementation Details](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#1-integration-architecture-overview)** - HTTPS/TLS configuration, HMAC-SHA256 signature algorithm

### 2.2 Core API Contract

The platform exposes the following interfaces for game providers to invoke:

| API Endpoint | Purpose | Key Requirement |
|--------------|---------|-----------------|
| **GetBalance** | Query player current balance | Real-time balance accuracy |
| **Transaction (Bet/Win)** | Process wagers and payouts | Consistency and duplicate prevention guaranteed |
| **CheckToken** | Validate player login token | Session validity confirmation |

**Transaction Requirements**:
- Bet and Win must be processed consistently with support for rollback
- Duplicate requests with the same transaction ID must never result in duplicate charges

→ **[Transaction Guarantees](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#22-transaction-betwin)** - Atomicity implementation, idempotency mechanisms

### 2.3 Game Launch Flow

The game launch process follows a defined sequence:
1. Frontend requests a launch URL from the platform backend
2. Backend obtains a token and URL from the game provider
3. Frontend embeds the game via iframe or opens a new window

**Required Launch Parameters**: player token, language, currency, and lobby return URL.

---

## 3. Seamless Wallet Edge Case Handling

The following scenarios must have documented resolution procedures for every provider integration:

| Scenario | Description | Required Resolution |
|----------|-------------|---------------------|
| **API Timeout** | Platform debit succeeds but GP response times out | Mark as "Pending" and query final status via QueryStatus. Direct rollback is strictly prohibited. |
| **Rollback / Cancel** | GP initiates bet cancellation (system error or event cancellation) | Refund if balance is sufficient; allow negative balance if funds were already withdrawn, flag for manual recovery. |
| **Race Condition** | Player sends concurrent bet requests | Use optimistic locking to prevent balance from going negative. |
| **Idempotency** | GP retries sending the same webhook | Return success for duplicate transaction IDs without re-processing. |

---

## 4. Provider Adaptation Requirements

The platform must support integration with multiple game providers using different API styles and data formats.

### Data Normalization Requirements

- All provider game types must be mapped to platform standard categories: Live, Slot, Sport
- All currency units must be unified (e.g., if provider uses cents, platform converts to base currency units)

→ **[Provider Adaptor Layer](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#4-provider-adaptor-layer)** - Type A/B/C technical classifications, middleware adaptation strategies

---

## 5. Dynamic Configuration and Approval Workflows

### 5.1 Emergency Provider Maintenance

When a game provider experiences critical errors requiring immediate disconnection:

| Step | Actor | Action |
|------|-------|--------|
| 1 | Operations Engineer | Initiates "GP Maintenance" request |
| 2 | CTO | Approves the request |
| 3 | System | Hides all entry points for that provider platform-wide |

### 5.2 Bet Limit Adjustments

- Bet limits (minimum/maximum) are configured per currency and per merchant
- All bet limit changes require approval from the Risk Management department

---

## 6. Jackpot Handling Requirements

### 6.1 Risk Scenario

Network Jackpots (e.g., accumulated prize pools of USD 10M) are funded by the Game Provider, not the merchant. The platform must distinguish between normal wins (merchant-funded) and jackpot wins (GP-funded) to prevent merchant insolvency.

### 6.2 Win Type Classification

| Win Type | Funding Source | Balance Impact |
|----------|---------------|----------------|
| **Normal Win** | Merchant | Directly updates player balance |
| **Jackpot Win (Network)** | Game Provider | Does NOT deduct from merchant quota |
| **Jackpot Win (Local)** | Varies by agreement | Per provider contract terms |

### 6.3 Jackpot Verification Process

| Step | Action | Responsible Party |
|------|--------|-------------------|
| 1 | Identify jackpot transaction | System (automatic) |
| 2 | Freeze funds in Jackpot Wallet | System (automatic) |
| 3 | Alert risk control and finance teams | System (automatic notification) |
| 4 | Await GP official Jackpot Verification Report | Game Provider |
| 5 | Confirm GP fund transfer to platform | Finance Team |
| 6 | Unfreeze and credit to player balance | Finance Team (manual release) |

---

## 7. RTP Automatic Suspension - Business Rules

To protect against GP bugs (e.g., guaranteed-win glitches) or incorrect odds causing rapid platform losses, the integration layer implements real-time automatic game suspension.

### 7.1 Monitoring Metrics

The system monitors each game provider and game ID using a 5-minute sliding window:

| Metric | Definition |
|--------|------------|
| Total Bet | Sum of all wagers in the window |
| Total Win | Sum of all payouts in the window |
| RTP (Return To Player) | Total Win / Total Bet x 100% |
| Net Loss | Total Win - Total Bet |

### 7.2 Automatic Suspension Thresholds

| Alert Level | Trigger Condition (5-min window) | Automated Action | Recovery Method |
|-------------|----------------------------------|-------------------|-----------------|
| **Warning** | RTP > 120% AND Net Loss > $5,000 | Send alert to risk control team (Slack/Telegram) | Automatic (if next period normalizes) |
| **Critical** | RTP > 200% AND Net Loss > $10,000 | Auto-disable the game/provider | Manual: requires CTO/Risk Director confirmation to unlock |

→ **[Suspension Implementation](../../architecture/03_Game_Integration/Game_Integration_Protocols.md#6-rtp-circuit-breaker-implementation)** - See architecture layer for technical implementation details

### 7.3 Manual Recovery Process

After a critical automatic suspension event:

| Step | Action | Responsible Party |
|------|--------|-------------------|
| 1 | Analyze game logs to determine if bug or player luck | Risk Control Team |
| 2 | Contact GP to confirm known vulnerabilities | Integration Team |
| 3a | If false positive: Admin resets and resumes via back-office | Operations |
| 3b | If confirmed bug: Maintain block until GP provides fix and patch notes | Integration Team |

### 7.4 Player Experience During Circuit Break

**Players currently in-game**:
- Next bet request returns a service unavailable response
- Frontend displays: "Game under temporary maintenance, please try again later"
- Balance automatically syncs back to main wallet

**Players in the lobby**:
- Affected game icon is greyed out with a "Maintenance" label
- Clicking displays a maintenance announcement

---

## 8. SLA Expectations

### 8.1 Provider Integration SLAs

| Metric | Target |
|--------|--------|
| API Uptime | 99.9% |
| Transaction Processing Latency | < 200ms P95 |
| Game Launch Success Rate | > 99.5% |
| Webhook Delivery Guarantee | At-least-once with duplicate prevention |
| New Game Onboarding Time | < 5 business days (after GP certification) |

### 8.2 Certification Requirements

Before a game provider goes live, the following must be verified:
- API contract compliance (all core endpoints functional)
- Edge case handling (timeout, rollback, duplicate prevention)
- Currency and game type normalization
- Jackpot transaction handling (if applicable)
- Circuit breaker integration testing

→ **[Technical Certification Checklist](../../architecture/03_Game_Integration/Game_Integration_Protocols.md)** - Detailed testing procedures and validation criteria

---

## Related Documents

### Core Dependencies
- [Wallet Architecture](../../source-archive/02_Finance_Center/02-06_Wallet_Architecture.md) - Game wallet transfer logic
- [Seamless Wallet Analysis](../../source-archive/03_Game_Center/03-03_Seamless_Wallet_Analysis.md) - GP edge case handling

### Business Integration
- [Turnover and Reconciliation](../../source-archive/02_Finance_Center/02-04_Turnover_and_Game_Reconciliation_Analysis.md) - Game reconciliation and turnover calculation
- [Game Lobby Management](../../source-archive/03_Game_Center/03-02_Game_Lobby_Management.md) - Game metadata sync and lobby configuration
- [Risk Framework](../../source-archive/05_Risk_Control/05-01_Risk_Framework.md) - Game risk detection and circuit breaker

---

**Document Version**: 1.0.0
**Last Updated**: 2026-02-08
**Maintainer**: Game Integration Team
