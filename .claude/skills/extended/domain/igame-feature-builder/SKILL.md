---
name: igaming-feature-builder
description: "[P1 - Extended] Implement iGaming domain features (VIP system, Wallet API, Bonus engine, Fraud detection, Risk control) with financial precision, compliance requirements, and audit trails. Follows SmartAdmin architecture patterns."
---

# iGaming Feature Builder

**Priority**: P1 | **Category**: Domain

Implement iGaming-specific business features including VIP systems, wallet operations, bonus engines, and fraud detection with financial precision and compliance.

## Trigger Keywords

**Primary Keywords** (High confidence):
- "VIP tier" / "VIP system"
- "wallet deposit" / "wallet withdrawal"
- "bonus distribution"
- "fraud detection" / "risk control"

**Secondary Keywords** (Medium confidence):
- "financial operations" / "compliance features" / "player management"
- "audit trail" / "wagering requirements"
- "bonus abuse" / "multi-account" / "KYC automation" / "AML"
- "suspicious transactions" / "arbitrage betting"

## Core Capabilities

### 1. VIP System
- Tier calculation based on lifetime points
- Point accumulation with row-level locking in Manager layer
- `@Transactional(isolation = SERIALIZABLE)` for critical operations

### 2. Wallet API
- Deposit/Withdrawal with BigDecimal precision (2 decimal places, RoundingMode.DOWN)
- KYC verification before withdrawal (REQUIRED)
- AML suspicious activity check
- Daily/single transaction limits
- Row-level locking (`selectForUpdate`)

### 3. Bonus Engine
- Rule evaluation and eligibility checks
- Bonus amount calculation (percentage-based with max cap)
- Wagering requirement tracking

### 4. Fraud Detection (6 Patterns)

| Pattern | Risk Scoring |
|---------|-------------|
| Multi-Account Detection (device fingerprint + IP) | Base 30 + coordinated behavior up to 100 |
| Bonus Abuse Detection (velocity + pattern) | >3 bonuses/24h = 25pts, low-risk wagering = 30pts |
| Suspicious Betting (arbitrage + matched) | Arbitrage = 40pts, syndicate = 50pts |
| Payment Fraud (card testing + chargebacks) | Card testing = 40pts, chargeback >5% = 50pts |
| Real-Time Risk Scoring | Composite 0-100 (LOW/MEDIUM/HIGH/CRITICAL) |
| KYC Progressive Automation | Score 50-69 = Tier 2, Score 70+ = Tier 3 |

**Auto-actions**: Score >=70 = manual review, Score >=90 = freeze account.

### 5. Audit Trail (MANDATORY)
Every financial operation must log: operation, params, operatorId, ipAddress, timestamp.

## Validation Checklist

**Financial Operations:**
- [ ] BigDecimal for money/points (NEVER float/double)
- [ ] @Transactional(isolation = SERIALIZABLE) in Manager layer
- [ ] Row-level locking (selectForUpdate)
- [ ] Idempotency for duplicate requests

**Compliance:**
- [ ] KYC verification before withdrawal
- [ ] AML suspicious activity check
- [ ] Audit log for every financial operation
- [ ] Transaction limits enforced

## Related Rules

- [Architecture Rules](../../../.agent/rules/foundation/F04-architecture-rules.md) - Controller -> Service -> Manager -> Dao
- [Manager Layer Rules](../../../.agent/rules/foundation/F03-manager-layer.md) - @Transactional in Manager only
- [Naming Conventions](../../../.agent/rules/foundation/F01-naming-conventions.md) - WalletEntity, VipService, WalletManager

## Related Skills

- **[igaming-pm-analyst](../igame-pm-analyst/SKILL.md)** - Requirements analysis (PM perspective)
- **[liteflow-rule-builder](../liteflow-rule-builder/SKILL.md)** - Business workflow rules

---
**Version**: 2.0.0 (Merged from igame-feature-builder + fraud-detection-pattern-generator)
**Last Updated**: 2026-03-07
