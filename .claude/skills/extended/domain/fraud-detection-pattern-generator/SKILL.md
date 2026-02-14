---
name: fraud-detection-pattern-generator
description: [P1 - Extended] Use when implementing iGaming fraud detection systems including multi-account detection, bonus abuse prevention, suspicious betting patterns, payment fraud, KYC verification automation, or real-time risk scoring. Triggers when user mentions "fraud", "risk control", "bonus abuse", "multi-account", "suspicious transactions", "KYC automation", "AML screening", or "iGaming compliance".
---

# Fraud Detection Pattern Generator

Generate complete fraud detection and risk control systems for iGaming platforms, following Malta Gaming Authority (MGA) and Curacao compliance requirements.

## Quick Start

**Most common usage:**
```
User: "Detect players with multiple accounts"
User: "Prevent bonus abuse and arbitrage betting"
User: "Implement real-time risk scoring for withdrawals"
User: "Flag suspicious betting patterns"
User: "Automate KYC verification triggers"
```

You will:
1. Identify fraud detection requirements from iGaming specs
2. Design detection algorithms and risk scoring models
3. Generate SmartAdmin Service/Manager layer implementations
4. Create database schemas for fraud tracking
5. Add real-time monitoring and alerting

## Trigger Keywords

This skill is automatically activated when the user's request contains:

**Primary Keywords** (High confidence):
- "fraud" / "fraud detection" - Fraud detection system implementation
- "risk control" - Risk management and control systems
- "bonus abuse" - Bonus abuse prevention patterns
- "multi-account" - Multi-account detection algorithms

**Secondary Keywords** (Medium confidence):
- "suspicious transactions" - Transaction fraud detection
- "KYC automation" - Automated KYC trigger rules
- "AML" - Anti-Money Laundering compliance
- "arbitrage betting" - Betting pattern fraud detection
- "device fingerprinting" - Multi-account detection
- "chargeback prevention" - Payment fraud detection

**Phrase Patterns**:
- "Detect [fraud type]" - Example: "Detect players with multiple accounts"
- "Prevent [abuse type]" - Example: "Prevent bonus abuse and arbitrage betting"
- "Implement [risk feature]" - Example: "Implement real-time risk scoring for withdrawals"

**Note**: This skill can also be manually invoked via `/fraud-detection-pattern-generator` command.

---

## Core Fraud Detection Patterns

### 1. Multi-Account Detection (Device Fingerprinting + IP Analysis)

Detect linked accounts using device fingerprints, IP addresses, coordinated behavior, and shared payment methods. Risk score calculated based on coordinated deposits/withdrawals, similar betting patterns, and shared payment methods.

**Key Risk Scoring**: Base 30 points for multi-account + 20 for coordinated deposits + 20 for coordinated withdrawals + 15 for similar betting + 15 for shared payments (max 100).

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#1-multi-account-detection-device-fingerprinting--ip-analysis) for complete Java implementation and database schema.

### 2. Bonus Abuse Detection (Velocity + Pattern Matching)

Detect rapid bonus claiming (velocity check: >3 bonuses in 24h), low-risk wagering (average odds <1.1), opposite betting (hedging strategy), and bonus arbitrage (multiple active bonuses).

**Key Thresholds**: >3 bonuses/24h = 25 risk points, avg odds <1.1 = 30 points, opposite betting = 40 points, multiple bonuses = 20 points.

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#2-bonus-abuse-detection-velocity--pattern-matching) for complete Java implementation and database schema.

### 3. Suspicious Betting Patterns (Arbitrage + Matched Betting)

Detect arbitrage betting (covering all outcomes for guaranteed profit), late betting (<30 seconds before event), and syndicate betting (coordinated large bets within 5-minute window).

**Key Risk Points**: Arbitrage = 40 points, late betting = 15 points, syndicate = 50 points.

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#3-suspicious-betting-patterns-arbitrage--matched-betting) for complete Java implementation.

### 4. Payment Fraud Detection (Card Testing + Chargebacks)

Detect card testing (>=3 failed attempts in 1 hour), high chargeback rate (>5% in 90 days), and stolen card usage (country mismatch between player location and billing address).

**Key Thresholds**: Card testing = 40 points + 24h payment block, chargeback rate >5% = 50 points, country mismatch = 30 points.

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#4-payment-fraud-detection-card-testing--chargebacks) for complete Java implementation.

### 5. Real-Time Risk Scoring System

Composite risk scoring with 4 components: Multi-account (30 max), Bonus abuse (30 max), Betting pattern (20 max), Payment fraud (20 max). Total 0-100 with risk levels: LOW (<30), MEDIUM (30-49), HIGH (50-69), CRITICAL (70+).

**Auto-actions**: Score >=70 = flag for manual review, Score >=90 = freeze account immediately.
**Caching**: Redis with 5-min TTL, 24-hour risk point accumulation window.

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#5-real-time-risk-scoring-system) for RiskScoreManager implementation and database schema.

### 6. KYC Verification Automation (Progressive KYC)

Auto-trigger KYC upgrades based on risk score: Score 50-69 = Tier 2 Enhanced KYC + block withdrawals. Score 70+ = Tier 3 Full KYC + manual review.

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#6-kyc-verification-automation-progressive-kyc) for KycFraudIntegrationService implementation.

---

## Common Mistakes

### False Positives (Over-Detection)

**Problem:** Legitimate players flagged as fraudulent.
**Solution:** Add whitelisting for trusted players, machine learning for threshold tuning, manual review queue for borderline cases.

```java
if (playerService.isTrusted(playerId)) {
    log.info("[Fraud Detection] Skipping detection for trusted player: {}", playerId);
    return false;
}
```

### Performance Impact

**Problem:** Real-time fraud detection slows down transactions.
**Solution:** Async processing with event listeners, Redis caching for risk scores, batch processing for historical analysis.

```java
@EventListener
@Async  // Don't block transaction
public void onWithdrawalRequest(WithdrawalRequestedEvent event) {
    fraudDetectionService.checkWithdrawal(event.getPlayerId(), event.getAmount());
}
```

### Data Privacy Violations

**Problem:** Storing excessive player data for fraud detection.
**Solution:** Hash sensitive identifiers, GDPR compliance (delete after 7 years), encrypt stored fraud evidence.

---

## Rationalization Table

| Excuse | Reality |
|--------|---------|
| "Fraud detection is overkill for small platform" | Fraud scales with platform size. Implement from day 1. |
| "Manual review is cheaper than automation" | Manual review doesn't scale. Automate detection, manual review only high-risk. |
| "Too many false positives hurt UX" | Tune thresholds. False negatives cost more than false positives. |
| "Real-time detection is too slow" | Use async processing and caching. Detection <100ms is achievable. |
| "We'll add fraud detection later" | Fraud patterns established early are hard to eradicate. Implement now. |

---

## Integration with SmartAdmin

**Manager Layer (Transactions):**
```java
@Service
@RequiredArgsConstructor
public class FraudDetectionManager {
    @Transactional(rollbackFor = Throwable.class)
    public void processFraudAlert(Long playerId, FraudAlert alert) {
        // Process alert in transaction
    }
}
```

**Event-Driven Architecture:**
```java
@EventListener
public void onDepositCompleted(DepositCompletedEvent event) {
    // Trigger fraud checks
}
```

---

## RED Phase Baseline Test

See [fraud-detection-code-examples.md](examples/fraud-detection-code-examples.md#7-red-phase-baseline-test) for complete test scenario (multi-account detection for bonus abuse) including expected detection logic, database queries, and success criteria.

---

## Related Rules

### Mandatory Requirements

- **[Architecture Rules](../../../.agent/rules/foundation/F04-architecture-rules.md)** - Layered architecture (Controller -> Service -> Manager -> Dao), fraud detection in Service layer, @Transactional in Manager only, constructor injection
- **[Manager Layer Rules](../../../.agent/rules/foundation/F03-manager-layer.md)** - RiskScoreManager @Transactional(rollbackFor = Throwable.class), distributed lock coordination
- **[Naming Conventions](../../../.agent/rules/foundation/F01-naming-conventions.md)** - LinkedAccountEntity, RiskScoreEntity, MultiAccountDetectionService, RiskScoreManager

### Reference Guidelines

- **[Exception Handling](../../../.agent/rules/technology/patterns/04-exception-logging.md)** - Risk control exception handling, high-risk event logging
- **[Concurrency Safety](../../../.agent/rules/technology/patterns/05-concurrency-safety.md)** - Concurrent risk score accumulation (Lua script), distributed lock

### Related Skills

- **[igame-feature-builder](../igame-feature-builder/SKILL.md)** - VIP system, wallet API implementation (risk control integration points)
- **[igame-pm-analyst](../igame-pm-analyst/SKILL.md)** - Risk control requirements analysis and PRD generation
- **[smartadmin-integration-test](../../foundation/full-stack/smartadmin-integration-test/SKILL.md)** - Risk control logic integration testing

---

## References

**iGaming Compliance:**
- [P0-04: KYC/AML Automation](docs/iGame/technical-specs/P0-critical/04-kyc-aml-automation.md)
- [P1-06: Real-Time Risk Engine](docs/iGame/technical-specs/P1-important/06-real-time-risk-engine.md)
- [P1-15: Security Hardening](docs/iGame/technical-specs/P1-important/15-security-hardening.md)

**SmartAdmin Patterns:**
- [Manager Layer Transactions](.agent/foundation/09-manager-layer.md)
- [Event-Driven Architecture](.claude/shared/knowledge/smartadmin-patterns.md)
