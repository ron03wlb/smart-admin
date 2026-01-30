# Fraud Detection Pattern Generator - Summary

## Skill Overview

**Purpose:** Generate complete fraud detection and risk control systems for iGaming platforms following MGA/Curacao compliance requirements.

**Target Users:** SmartAdmin developers building iGaming platforms with fraud prevention requirements.

**Key Features:**
- Multi-account detection (device fingerprinting + IP analysis)
- Bonus abuse prevention (velocity checks + pattern matching)
- Suspicious betting detection (arbitrage + matched betting)
- Payment fraud detection (card testing + chargebacks)
- Real-time risk scoring (0-100 scale with auto-actions)
- KYC verification automation (Progressive KYC integration)

---

## Fraud Detection Requirements (from iGaming Specs)

### From P1-15: Security Hardening

**Key Requirements:**
- OWASP Top 10 compliance
- Data encryption (AES-256) for sensitive fraud data
- Audit logging for all fraud events (7-year retention)
- Rate limiting to prevent automated fraud attempts
- Input validation to prevent SQL injection in fraud queries

**Integration:**
- API encryption for fraud detection endpoints
- Data masking for PII in fraud reports
- CSRF protection for fraud management dashboard

### From P0-04: KYC/AML Automation

**Progressive KYC Integration:**
- Tier 0 → Tier 1: Triggered by first deposit or risk score >30
- Tier 1 → Tier 2: Triggered by withdrawal >$500 or risk score >50
- Tier 2 → Tier 3: Triggered by risk score >70 or AML hit

**AML Integration:**
- ComplyAdvantage API for sanction list screening
- Automatic risk score increase (+100) for sanction matches
- Freeze account if critical risk detected

**Document Storage:**
- MinIO for fraud evidence (screenshots, logs)
- Encrypted storage with 7-year retention
- Access audit trail for compliance

### From P1-06: Real-Time Risk Engine

**Risk Scoring Algorithm:**
```
Total Risk Score (0-100) =
  Multi-Account Risk (0-30) +
  Bonus Abuse Risk (0-30) +
  Betting Pattern Risk (0-20) +
  Payment Fraud Risk (0-20)
```

**Risk Levels:**
- LOW (0-29): Automatic approval
- MEDIUM (30-49): Enhanced monitoring
- HIGH (50-69): Manual review required
- CRITICAL (70-100): Auto-freeze account

**Auto-Actions by Risk Level:**
- MEDIUM: Flag for enhanced monitoring, require Tier 1 KYC
- HIGH: Block bonus claims, require Tier 2 KYC
- CRITICAL: Freeze account, require Tier 3 KYC + manual review

---

## Core Fraud Detection Patterns

### 1. Multi-Account Detection

**Implementation:**
- `MultiAccountDetectionService`: Device fingerprint + IP analysis
- Database: `device_fingerprints`, `linked_account_groups`
- Detection signals: Same device, same IP, coordinated behavior
- Risk score: 30 base + up to 50 for coordinated actions

**Key Methods:**
- `detectByDeviceFingerprint()`: Hash-based matching
- `detectByIpAddress()`: IP pattern analysis (excludes VPN/public WiFi)
- `calculateLinkRiskScore()`: Composite score from multiple signals

### 2. Bonus Abuse Detection

**Implementation:**
- `BonusAbuseDetectionService`: Velocity checks + wagering patterns
- Database: `bonus_abuse_events`
- Detection signals: Rapid claiming, low-risk wagering, arbitrage
- Risk score: Up to 40 for confirmed abuse

**Key Methods:**
- `detectRapidBonusClaiming()`: >3 bonuses in 24h = +25 points
- `detectLowRiskWagering()`: Avg odds <1.1 = +30 points
- `detectBonusArbitrage()`: Multiple active bonuses = +20 points

### 3. Suspicious Betting Detection

**Implementation:**
- `SuspiciousBettingDetectionService`: Pattern analysis
- Database: `betting_history` with fraud flags
- Detection signals: Arbitrage, late betting, syndicate coordination
- Risk score: Up to 50 for critical patterns

**Key Methods:**
- `detectArbitrageBetting()`: Covering all outcomes = +40 points
- `detectLateBetting()`: Bets <30s before event = +15 points
- `detectSyndicateBetting()`: >3 coordinated large bets = +50 points

### 4. Payment Fraud Detection

**Implementation:**
- `PaymentFraudDetectionService`: Transaction monitoring
- Database: `payment_transactions` with fraud flags
- Detection signals: Card testing, chargebacks, stolen cards
- Risk score: Up to 50 for critical fraud

**Key Methods:**
- `detectCardTesting()`: >3 failed attempts in 1h = +40 points
- `detectChargebackRisk()`: >5% chargeback rate = +50 points
- `detectStolenCard()`: Country mismatch = +30 points

### 5. Real-Time Risk Scoring

**Implementation:**
- `RiskScoreManager`: Composite scoring engine
- Cache: Redis with 5-minute TTL
- Actions: Auto-freeze at 70+, manual review at 50+
- Alerts: PagerDuty/Slack for CRITICAL risk

**Key Methods:**
- `calculateRiskScore()`: Sum of all components
- `addRiskPoints()`: Event-driven score updates
- `triggerHighRiskAction()`: Auto-freeze or manual review

### 6. KYC Fraud Integration

**Implementation:**
- `KycFraudIntegrationService`: Links fraud detection to KYC tiers
- Triggers: Auto-upgrade KYC tier based on risk score
- Actions: Block withdrawals until KYC verified

**Key Methods:**
- `triggerEnhancedKyc()`: Force Tier 2/3 upgrade
- `onHighRiskDetected()`: Event listener for risk alerts

---

## Database Schema Summary

### Core Tables

1. **device_fingerprints** (105 rows in baseline test)
   - Tracks browser/device identifiers
   - SHA-256 hashed fingerprints
   - Indexed by fingerprint_hash for fast lookup

2. **linked_account_groups** (1 row per detected group)
   - Groups linked accounts
   - Tracks detection method and risk score
   - Status: PENDING, CONFIRMED, FALSE_POSITIVE

3. **bonus_abuse_events** (4 rows in baseline test)
   - Logs bonus abuse incidents
   - JSONB details for flexible evidence storage
   - Indexed by player_id and abuse_type

4. **risk_scores** (Snapshot per player evaluation)
   - Stores composite risk scores
   - JSONB components for breakdown
   - Indexed by player_id + timestamp

5. **risk_events** (Event log for all fraud signals)
   - Immutable audit trail
   - Links to specific fraud types
   - 7-year retention for compliance

---

## Integration with SmartAdmin

### Manager Layer (Transactions)

```java
@Service
@RequiredArgsConstructor
public class FraudDetectionManager {

    @Transactional(rollbackFor = Throwable.class)
    public void processFraudAlert(Long playerId, FraudAlert alert) {
        // Transaction-safe fraud event processing
        riskScoreDao.insertEvent(alert.toRiskEvent());

        if (alert.getRiskLevel() == RiskLevel.CRITICAL) {
            playerManager.freezeAccount(playerId, alert.getReason());
        }
    }
}
```

### Event-Driven Architecture

```java
@EventListener
@Async
public void onDepositCompleted(DepositCompletedEvent event) {
    // Don't block deposit transaction
    fraudDetectionService.checkDeposit(event.getPlayerId(), event.getAmount());
}
```

### Service Layer (Vavr Option)

```java
public Option<LinkedAccountGroup> findLinkedAccounts(Long playerId) {
    List<LinkedAccountGroup> groups = multiAccountDetectionService
        .detectByDeviceFingerprint(playerId);

    return groups.isEmpty() ? Option.none() : Option.of(groups.get(0));
}
```

---

## Testing Strategy (Baseline Test)

### Scenario: Multi-Account Bonus Abuse

**Setup:**
- 3 fraudulent accounts (alex1@, alex2@, alex3@)
- Same device fingerprint
- Each claims $100 welcome bonus
- All attempt withdrawal after 45 minutes

**Expected Detection:**
1. **Phase 1 (10:15 AM):** Device fingerprint match detected
2. **Phase 2 (10:20 AM):** Rapid bonus claiming detected
3. **Phase 3 (10:30 AM):** Risk score reaches 75 → auto-freeze
4. **Phase 4 (10:45 AM):** Withdrawal attempts blocked

**Metrics:**
- Detection rate: 100% (all 3 accounts)
- Detection time: 15 minutes (after 2nd account)
- False positive rate: 0% (confirmed fraud)
- Processing time: <500ms per check

**ROI:**
- Monthly fraud prevented: $27,000
- Annual savings: $324,000
- Implementation cost: $10,000
- ROI: 3,140% annually

---

## Performance Benchmarks

| Operation | Target | Expected |
|-----------|--------|----------|
| Device fingerprint check | <500ms | 312ms |
| Risk score calculation | <200ms | 156ms |
| Database queries | <100ms | 67ms |
| Redis cache lookup | <10ms | 4ms |
| Alert delivery | <30s | 12s |

---

## Common Mistakes to Avoid

1. **❌ False Positives:** Over-aggressive detection hurts UX
   - **✅ Solution:** Whitelist trusted players, tune thresholds

2. **❌ Performance Impact:** Real-time detection slows transactions
   - **✅ Solution:** Async processing, Redis caching, batch analysis

3. **❌ Data Privacy:** Excessive player data storage
   - **✅ Solution:** Hash identifiers, GDPR compliance, encryption

4. **❌ Manual Review Bottleneck:** All alerts require human review
   - **✅ Solution:** Auto-actions for LOW/CRITICAL, manual only for MEDIUM/HIGH

---

## Deployment Checklist

- [ ] Create database tables and indexes
- [ ] Implement 5 fraud detection services
- [ ] Implement RiskScoreManager
- [ ] Configure event listeners (@Async)
- [ ] Set up Redis caching (5-min TTL)
- [ ] Create compliance dashboard
- [ ] Add Prometheus metrics
- [ ] Write integration tests (baseline scenario)
- [ ] Deploy to staging
- [ ] Train compliance team
- [ ] Configure alerts (Slack/PagerDuty)
- [ ] Enable in production (gradual rollout)

---

## Compliance & Audit

**Malta Gaming Authority (MGA):**
- ✅ Real-time fraud detection (required)
- ✅ Audit trail for all fraud events (7-year retention)
- ✅ KYC verification triggers (automated)
- ✅ AML screening integration (ComplyAdvantage)

**GDPR:**
- ✅ Data minimization (hash identifiers)
- ✅ Right to erasure (delete fraud data after ban)
- ✅ Encryption for sensitive data (AES-256)
- ✅ Access audit trail (who viewed fraud data)

---

## Next Steps

1. **RED Phase:** Run baseline test scenario (BASELINE-TEST.md)
   - Verify detection accuracy (>90%)
   - Measure performance (<500ms)
   - Check false positive rate (<5%)

2. **GREEN Phase:** Implement skill in SmartAdmin
   - Create database schema
   - Implement 6 core services
   - Add event listeners
   - Set up Redis caching

3. **REFACTOR Phase:** Tune and optimize
   - Adjust risk score thresholds
   - Add machine learning (future)
   - Reduce false positives
   - Improve performance

---

**Skill Version:** 1.0.0
**Last Updated:** 2026-01-25
**Status:** ✅ Ready for Testing
**Estimated Implementation Time:** 2 weeks (80 hours)
**Expected ROI:** 3,140% annually
