# Fraud Detection Pattern Generator - Quick Reference

**Version**: 1.0.0
**Last Updated**: 2026-02-02
**Skill**: fraud-detection-pattern-generator (P1 - Extended/Domain)

---

## Command Quick Reference

### Basic Commands

| Command | Purpose | Duration |
|---------|---------|----------|
| Generate Detector | Create fraud detection Service | ~10 min |
| Risk Scoring | Implement real-time risk scoring | ~15 min |
| KYC Triggers | Auto-trigger KYC based on risk | ~8 min |
| Database Schema | Generate fraud tracking tables | ~5 min |
| Alert System | Setup fraud alert notifications | ~12 min |

### Rapid Development Workflow

| Step | Action | Time |
|------|--------|------|
| 1. Select Pattern | Choose detection pattern type | ~2 min |
| 2. Generate Service | Create SmartAdmin Service layer | ~8 min |
| 3. Database Schema | Generate tracking tables | ~5 min |
| 4. Risk Scoring | Integrate with risk score manager | ~7 min |
| 5. KYC Integration | Connect to KYC workflow | ~6 min |
| 6. Testing | Verify detection with mock data | ~15 min |

**Total**: ~45 minutes per fraud detection pattern

---

## Fraud Detection Pattern Matrix (Decision Guide)

### Pattern 1: Multi-Account Detection (Device Fingerprinting + IP)

**Trigger Keywords**: "multi-account", "device fingerprint", "duplicate registration"

**Use When**: Detect players creating multiple accounts for bonus abuse or arbitrage

**Detection Signals**:
- Same device fingerprint across accounts (SHA-256 hash)
- Same IP address for registration/login
- Coordinated deposit/withdrawal timing
- Shared payment methods
- Similar betting patterns

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class MultiAccountDetectionService {

    private final PlayerDao playerDao;
    private final DeviceFingerprintDao fingerprintDao;

    /**
     * Detect linked accounts by device fingerprint
     * Time: ~10 min to implement
     */
    public List<LinkedAccountGroup> detectByDeviceFingerprint(Long playerId) {
        // Get all device fingerprints for player
        List<DeviceFingerprint> fingerprints = fingerprintDao.selectList(
            new LambdaQueryWrapper<DeviceFingerprint>()
                .eq(DeviceFingerprint::getPlayerId, playerId)
        );

        List<LinkedAccountGroup> groups = new ArrayList<>();

        for (DeviceFingerprint fingerprint : fingerprints) {
            // Find other players using same device
            List<Long> linkedPlayerIds = fingerprintDao
                .findPlayersByFingerprint(fingerprint.getFingerprintHash());

            if (linkedPlayerIds.size() > 1) {
                LinkedAccountGroup group = LinkedAccountGroup.builder()
                    .detectionMethod("DEVICE_FINGERPRINT")
                    .playerId(playerId)
                    .linkedPlayerIds(linkedPlayerIds)
                    .riskScore(calculateLinkRiskScore(linkedPlayerIds))
                    .fingerprintHash(fingerprint.getFingerprintHash())
                    .build();

                groups.add(group);

                log.warn("[Fraud] Multi-account: player={}, links={}",
                    playerId, linkedPlayerIds.size() - 1);
            }
        }

        return groups;
    }

    /**
     * Calculate risk score (0-100)
     */
    private int calculateLinkRiskScore(List<Long> linkedPlayerIds) {
        int baseScore = 30;  // Base multi-account risk

        if (hasCoordinatedDeposits(linkedPlayerIds)) baseScore += 20;
        if (hasCoordinatedWithdrawals(linkedPlayerIds)) baseScore += 20;
        if (hasSimilarBettingPatterns(linkedPlayerIds)) baseScore += 15;
        if (hasSharedPaymentMethods(linkedPlayerIds)) baseScore += 15;

        return Math.min(baseScore, 100);
    }
}
```

**Database Schema**:
```sql
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    fingerprint_hash VARCHAR(64) NOT NULL,  -- SHA-256
    user_agent TEXT,
    screen_resolution VARCHAR(20),
    timezone VARCHAR(50),
    canvas_hash VARCHAR(64),
    webgl_hash VARCHAR(64),
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_fingerprint_hash ON device_fingerprints(fingerprint_hash);
CREATE INDEX idx_fingerprint_player ON device_fingerprints(player_id);
```

**Risk Score Calculation**:
- Base multi-account: 30 points
- Coordinated deposits: +20 points
- Coordinated withdrawals: +20 points
- Similar betting: +15 points
- Shared payment: +15 points
- **Total**: 0-100 points

**Time to Implement**: 10-15 minutes

---

### Pattern 2: Bonus Abuse Detection (Velocity + Pattern Matching)

**Trigger Keywords**: "bonus abuse", "wagering requirement", "arbitrage betting"

**Use When**: Prevent players from exploiting bonus promotions

**Detection Signals**:
- Rapid bonus claiming (>3 in 24 hours)
- Minimum deposit → bonus claim → immediate withdrawal
- Low-risk wagering (odds < 1.1 to clear requirements)
- Opposite betting (hedging to guarantee profit)
- Multiple active bonuses simultaneously

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class BonusAbuseDetectionService {

    private final BonusClaimDao bonusClaimDao;
    private final BettingHistoryDao bettingHistoryDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect rapid bonus claiming
     * Time: ~8 min to implement
     */
    public boolean detectRapidBonusClaiming(Long playerId) {
        int count24h = bonusClaimDao.countBonusesClaimed(
            playerId,
            Duration.ofHours(24)
        );

        if (count24h > 3) {
            log.warn("[Fraud] Rapid bonus claiming: player={}, count={}",
                playerId, count24h);

            riskScoreManager.addRiskPoints(playerId, 25, "RAPID_BONUS_CLAIMING");
            return true;
        }

        return false;
    }

    /**
     * Detect low-risk wagering (clearing bonus with minimal risk)
     * Time: ~12 min to implement
     */
    public boolean detectLowRiskWagering(Long playerId, Long bonusId) {
        List<Bet> bets = bettingHistoryDao.getBetsForBonus(playerId, bonusId);

        // Calculate average odds
        double avgOdds = bets.stream()
            .mapToDouble(Bet::getOdds)
            .average()
            .orElse(0.0);

        // Low-risk pattern: Near-certain outcomes
        if (avgOdds < 1.1) {
            log.warn("[Fraud] Low-risk wagering: player={}, avgOdds={}",
                playerId, avgOdds);

            riskScoreManager.addRiskPoints(playerId, 30, "LOW_RISK_WAGERING");
            return true;
        }

        // Opposite betting (hedging)
        if (hasOppositeBets(bets)) {
            log.warn("[Fraud] Opposite betting: player={}, bonusId={}",
                playerId, bonusId);

            riskScoreManager.addRiskPoints(playerId, 40, "OPPOSITE_BETTING");
            return true;
        }

        return false;
    }

    /**
     * Detect opposite betting (hedging strategy)
     */
    private boolean hasOppositeBets(List<Bet> bets) {
        Map<String, List<Bet>> betsByEvent = bets.stream()
            .collect(Collectors.groupingBy(Bet::getEventId));

        for (List<Bet> eventBets : betsByEvent.values()) {
            if (hasConflictingOutcomes(eventBets)) {
                return true;
            }
        }

        return false;
    }
}
```

**Database Schema**:
```sql
CREATE TABLE bonus_abuse_events (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    bonus_id BIGINT,
    abuse_type VARCHAR(50) NOT NULL,  -- RAPID_CLAIMING, LOW_RISK_WAGERING
    risk_score INTEGER NOT NULL,
    details JSONB,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bonus_abuse_player ON bonus_abuse_events(player_id);
CREATE INDEX idx_bonus_abuse_type ON bonus_abuse_events(abuse_type);
```

**Risk Score Calculation**:
- Rapid claiming (>3/24h): 25 points
- Low-risk wagering (<1.1 odds): 30 points
- Opposite betting: 40 points
- Multiple active bonuses: 20 points

**Time to Implement**: 12-18 minutes

---

### Pattern 3: Suspicious Betting Patterns (Arbitrage + Matched Betting)

**Trigger Keywords**: "arbitrage", "matched betting", "syndicate betting", "late betting"

**Use When**: Detect betting strategies that exploit odds or insider information

**Detection Signals**:
- Arbitrage betting (betting on all outcomes to guarantee profit)
- Matched betting (risk-free profit using free bets)
- Syndicate betting (coordinated large bets from multiple accounts)
- Late betting (bets placed <30 seconds before event)

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class SuspiciousBettingDetectionService {

    private final BettingHistoryDao bettingHistoryDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect arbitrage betting
     * Time: ~15 min to implement
     */
    public boolean detectArbitrageBetting(Long playerId) {
        List<Bet> recentBets = bettingHistoryDao.getRecentBets(
            playerId,
            Duration.ofDays(7)
        );

        // Group by event
        Map<String, List<Bet>> betsByEvent = recentBets.stream()
            .collect(Collectors.groupingBy(Bet::getEventId));

        for (Map.Entry<String, List<Bet>> entry : betsByEvent.entrySet()) {
            List<Bet> eventBets = entry.getValue();

            // Arbitrage: Betting on all outcomes
            if (coversAllOutcomes(eventBets)) {
                log.warn("[Fraud] Arbitrage detected: player={}, event={}",
                    playerId, entry.getKey());

                riskScoreManager.addRiskPoints(playerId, 40, "ARBITRAGE_BETTING");
                return true;
            }
        }

        return false;
    }

    /**
     * Detect late betting (last-minute bets)
     * Time: ~8 min to implement
     */
    public boolean detectLateBetting(Long playerId, Bet bet) {
        Duration timeBeforeEvent = Duration.between(
            bet.getPlacedAt(),
            bet.getEventStartTime()
        );

        // Suspicious if <30 seconds before event
        if (timeBeforeEvent.getSeconds() < 30) {
            log.warn("[Fraud] Late betting: player={}, seconds={}",
                playerId, timeBeforeEvent.getSeconds());

            riskScoreManager.addRiskPoints(playerId, 15, "LATE_BETTING");
            return true;
        }

        return false;
    }

    /**
     * Detect syndicate betting (coordinated large bets)
     * Time: ~12 min to implement
     */
    public boolean detectSyndicateBetting(Long playerId, Bet bet) {
        // Find similar bets from other players
        List<Bet> similarBets = bettingHistoryDao.findSimilarBets(
            bet.getEventId(),
            bet.getOutcome(),
            bet.getPlacedAt(),
            Duration.ofMinutes(5)
        );

        if (similarBets.size() > 3 && isLargeBet(bet)) {
            log.warn("[Fraud] Syndicate betting: player={}, similarBets={}",
                playerId, similarBets.size());

            riskScoreManager.addRiskPoints(playerId, 50, "SYNDICATE_BETTING");
            return true;
        }

        return false;
    }

    /**
     * Check if bets cover all outcomes (arbitrage)
     */
    private boolean coversAllOutcomes(List<Bet> bets) {
        Set<String> outcomes = bets.stream()
            .map(Bet::getOutcome)
            .collect(Collectors.toSet());

        // For binary (win/lose): need 2 bets
        // For 3-way (win/draw/lose): need 3 bets
        return outcomes.size() >= 2 && calculateGuaranteedProfit(bets) > 0;
    }

    /**
     * Calculate guaranteed profit
     */
    private double calculateGuaranteedProfit(List<Bet> bets) {
        double totalStake = bets.stream().mapToDouble(Bet::getStake).sum();
        double minReturn = bets.stream()
            .mapToDouble(b -> b.getStake() * b.getOdds())
            .min()
            .orElse(0.0);

        return minReturn - totalStake;
    }
}
```

**Risk Score Calculation**:
- Arbitrage betting: 40 points
- Late betting (<30s): 15 points
- Syndicate betting (>3 similar): 50 points

**Time to Implement**: 15-20 minutes

---

### Pattern 4: Payment Fraud Detection (Card Testing + Chargebacks)

**Trigger Keywords**: "card testing", "chargeback", "stolen card", "payment fraud"

**Use When**: Detect stolen credit cards and fraudulent payment activity

**Detection Signals**:
- Multiple failed payment attempts (card testing)
- High chargeback rate (>5%)
- Stolen credit card (mismatched country)
- Rapid successive payments

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class PaymentFraudDetectionService {

    private final PaymentTransactionDao transactionDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect card testing (multiple failed attempts)
     * Time: ~10 min to implement
     */
    public boolean detectCardTesting(Long playerId) {
        int failedAttempts = transactionDao.countFailedAttempts(
            playerId,
            Duration.ofHours(1)
        );

        if (failedAttempts >= 3) {
            log.warn("[Fraud] Card testing: player={}, failures={}",
                playerId, failedAttempts);

            riskScoreManager.addRiskPoints(playerId, 40, "CARD_TESTING");

            // Auto-block payments for 24h
            blockPayments(playerId, Duration.ofHours(24));
            return true;
        }

        return false;
    }

    /**
     * Detect high chargeback risk
     * Time: ~8 min to implement
     */
    public boolean detectChargebackRisk(Long playerId) {
        int total = transactionDao.countTransactions(playerId, Duration.ofDays(90));
        int chargebacks = transactionDao.countChargebacks(playerId, Duration.ofDays(90));

        if (total > 0) {
            double chargebackRate = (double) chargebacks / total;

            if (chargebackRate > 0.05) {  // >5%
                log.warn("[Fraud] High chargeback: player={}, rate={}%",
                    playerId, chargebackRate * 100);

                riskScoreManager.addRiskPoints(playerId, 50, "HIGH_CHARGEBACK_RATE");
                return true;
            }
        }

        return false;
    }

    /**
     * Detect stolen card (country mismatch)
     * Time: ~6 min to implement
     */
    public boolean detectStolenCard(Long playerId, PaymentTransaction transaction) {
        Player player = playerDao.selectById(playerId);
        String playerCountry = player.getCountry();
        String cardCountry = transaction.getBillingCountry();

        if (!playerCountry.equals(cardCountry)) {
            log.warn("[Fraud] Country mismatch: player={}, playerCountry={}, cardCountry={}",
                playerId, playerCountry, cardCountry);

            riskScoreManager.addRiskPoints(playerId, 30, "COUNTRY_MISMATCH");
            return true;
        }

        return false;
    }
}
```

**Risk Score Calculation**:
- Card testing (≥3 failures): 40 points
- High chargeback rate (>5%): 50 points
- Country mismatch: 30 points

**Time to Implement**: 10-12 minutes

---

### Pattern 5: Real-Time Risk Scoring System

**Trigger Keywords**: "risk score", "risk level", "fraud alert"

**Use When**: Aggregate multiple fraud signals into unified risk score

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class RiskScoreManager {

    private final RiskScoreDao riskScoreDao;
    private final RedissonClient redisson;
    private final AlertService alertService;

    /**
     * Calculate composite risk score (0-100)
     * Time: ~15 min to implement
     */
    public RiskScore calculateRiskScore(Long playerId) {
        RiskScore score = new RiskScore();
        score.setPlayerId(playerId);
        score.setTimestamp(LocalDateTime.now());

        // Multi-account risk (30 points max)
        score.addComponent("MULTI_ACCOUNT", detectMultiAccountRisk(playerId));

        // Bonus abuse risk (30 points max)
        score.addComponent("BONUS_ABUSE", detectBonusAbuseRisk(playerId));

        // Betting pattern risk (20 points max)
        score.addComponent("BETTING_PATTERN", detectBettingPatternRisk(playerId));

        // Payment fraud risk (20 points max)
        score.addComponent("PAYMENT_FRAUD", detectPaymentFraudRisk(playerId));

        // Calculate total (0-100)
        int totalScore = score.getTotalScore();
        score.setRiskLevel(getRiskLevel(totalScore));

        // Cache in Redis (5 min TTL)
        cacheRiskScore(playerId, score);

        // Alert if high risk
        if (totalScore >= 70) {
            alertService.sendHighRiskAlert(playerId, score);
        }

        return score;
    }

    /**
     * Add risk points for specific event
     */
    public void addRiskPoints(Long playerId, int points, String reason) {
        String key = "risk:player:" + playerId;
        RAtomicLong riskScore = redisson.getAtomicLong(key);

        long newScore = riskScore.addAndGet(points);
        riskScore.expire(Duration.ofHours(24));

        // Log event
        RiskEvent event = RiskEvent.builder()
            .playerId(playerId)
            .points(points)
            .reason(reason)
            .timestamp(LocalDateTime.now())
            .build();

        riskScoreDao.insertEvent(event);

        log.info("[Risk] Added points: player={}, +{}, reason={}, total={}",
            playerId, points, reason, newScore);

        // Auto-action if exceeds threshold
        if (newScore >= 70) {
            triggerHighRiskAction(playerId, newScore);
        }
    }

    /**
     * Get risk level based on score
     */
    private RiskLevel getRiskLevel(int score) {
        if (score < 30) return RiskLevel.LOW;
        if (score < 50) return RiskLevel.MEDIUM;
        if (score < 70) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    /**
     * Trigger action for high-risk player
     */
    private void triggerHighRiskAction(Long playerId, long score) {
        if (score >= 90) {
            // Critical: Freeze account
            playerManager.freezeAccount(playerId, "HIGH_RISK_SCORE");
            alertService.sendUrgentAlert("CRITICAL_RISK", playerId, score);
        } else if (score >= 70) {
            // High: Manual review
            playerManager.flagForReview(playerId, "HIGH_RISK_SCORE");
            alertService.sendAlert("HIGH_RISK", playerId, score);
        }
    }
}
```

**Database Schema**:
```sql
CREATE TABLE risk_scores (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    total_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,  -- LOW, MEDIUM, HIGH, CRITICAL
    components JSONB NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE risk_events (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL,
    reason TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_player_timestamp ON risk_scores(player_id, timestamp DESC);
CREATE INDEX idx_risk_events_player ON risk_events(player_id, timestamp DESC);
```

**Risk Levels**:
- 0-29: LOW (no action)
- 30-49: MEDIUM (monitor)
- 50-69: HIGH (require enhanced KYC)
- 70+: CRITICAL (freeze account, manual review)

**Time to Implement**: 15-20 minutes

---

### Pattern 6: KYC/AML Integration (Progressive KYC)

**Trigger Keywords**: "KYC trigger", "AML screening", "progressive KYC", "enhanced verification"

**Use When**: Auto-trigger KYC verification based on fraud risk score

**Implementation**:
```java
@Service
@RequiredArgsConstructor
public class KycFraudIntegrationService {

    private final KycService kycService;
    private final RiskScoreManager riskScoreManager;
    private final WithdrawalService withdrawalService;

    /**
     * Trigger enhanced KYC for high-risk players
     * Time: ~8 min to implement
     */
    public void triggerEnhancedKyc(Long playerId, String reason) {
        Player player = playerDao.selectById(playerId);

        // Force upgrade to Tier 2 (Enhanced KYC)
        if (player.getKycTier() < KycTier.TIER_2_ENHANCED.getLevel()) {
            kycService.forceUpgrade(playerId, KycTier.TIER_2_ENHANCED, reason);

            log.info("[Fraud] Forced KYC upgrade: player={}, tier={}, reason={}",
                playerId, KycTier.TIER_2_ENHANCED, reason);
        }

        // Block withdrawals until KYC completed
        withdrawalService.blockWithdrawals(playerId, "PENDING_ENHANCED_KYC");
    }

    /**
     * Auto-trigger KYC based on risk score
     */
    @EventListener
    public void onHighRiskDetected(HighRiskEvent event) {
        Long playerId = event.getPlayerId();
        int riskScore = event.getRiskScore();

        // Risk 50-69: Tier 2 KYC
        if (riskScore >= 50 && riskScore < 70) {
            triggerEnhancedKyc(playerId, "HIGH_RISK_SCORE");
        }

        // Risk 70+: Tier 3 KYC + manual review
        if (riskScore >= 70) {
            kycService.forceUpgrade(playerId, KycTier.TIER_3_FULL, "CRITICAL_RISK_SCORE");
            playerManager.flagForManualReview(playerId, "CRITICAL_RISK");
        }
    }
}
```

**KYC Tier Mapping**:
| Risk Score | KYC Tier | Actions |
|------------|----------|---------|
| 0-49 | Tier 1 (Basic) | Email verification only |
| 50-69 | Tier 2 (Enhanced) | ID document + selfie |
| 70+ | Tier 3 (Full) | Proof of address + source of funds |

**Time to Implement**: 8-10 minutes

---

## Common Errors and Quick Fixes

### Error 1: False Positives (Over-Detection)

**Symptom**: Legitimate players flagged as fraudulent

**Causes**:
- Public WiFi / VPN usage (multiple accounts from same IP)
- Shared household (family members on same device)
- Legitimate arbitrage strategies (legal in some jurisdictions)

**Quick Fix**:
```java
// Whitelist trusted players
if (playerService.isTrusted(playerId)) {
    log.info("[Fraud] Skipping detection for trusted player: {}", playerId);
    return false;
}

// Filter out public IPs
if (isPublicIp(ip) || isVpnIp(ip)) {
    return false;  // Lower risk
}
```

**Time to Fix**: 5-10 minutes

---

### Error 2: Performance Impact

**Symptom**: Real-time fraud detection slows down transactions

**Causes**:
- Synchronous fraud checks blocking transaction flow
- Heavy database queries in critical path
- No caching for frequently calculated risk scores

**Quick Fix**:
```java
// Use async event listeners
@EventListener
@Async  // Don't block transaction
public void onWithdrawalRequest(WithdrawalRequestedEvent event) {
    fraudDetectionService.checkWithdrawal(event.getPlayerId(), event.getAmount());
}

// Cache risk scores in Redis (5 min TTL)
String key = "risk:player:" + playerId;
RBucket<RiskScore> bucket = redisson.getBucket(key);
RiskScore cached = bucket.get();

if (cached != null && !cached.isExpired()) {
    return cached;  // Use cached score
}
```

**Time to Fix**: 10-15 minutes

---

### Error 3: Data Privacy Violations (GDPR)

**Symptom**: Storing excessive player data for fraud detection

**Causes**:
- Storing raw device fingerprints (PII)
- Retaining fraud data indefinitely
- Not encrypting sensitive fraud evidence

**Quick Fix**:
```java
// Hash device fingerprint before storing
String hashedFingerprint = DigestUtils.sha256Hex(rawFingerprint);

// GDPR: Auto-delete fraud data after 7 years
@Scheduled(cron = "0 0 2 * * ?")  // Daily at 2 AM
public void cleanupOldFraudData() {
    LocalDateTime cutoff = LocalDateTime.now().minusYears(7);
    riskScoreDao.deleteOldRecords(cutoff);
}

// Encrypt sensitive data
String encryptedCardLast4 = encryptionService.encrypt(cardLast4);
```

**Time to Fix**: 15-20 minutes

---

### Error 4: Inadequate Alert System

**Symptom**: High-risk fraud not notified to risk team in time

**Causes**:
- No real-time alerting for critical risks
- Alerts only logged, not sent to monitoring systems
- No escalation for repeated fraud attempts

**Quick Fix**:
```java
@Service
@RequiredArgsConstructor
public class AlertService {

    private final EmailService emailService;
    private final SlackService slackService;
    private final SmsService smsService;

    /**
     * Send urgent alert for critical risks
     */
    public void sendUrgentAlert(String alertType, Long playerId, long riskScore) {
        String message = String.format(
            "[URGENT] %s detected: Player %d, Risk Score: %d",
            alertType, playerId, riskScore
        );

        // Multi-channel alerting
        emailService.sendToRiskTeam(message);
        slackService.sendToChannel("#risk-alerts", message);
        smsService.sendToOnCallManager(message);

        log.error("[Alert] Urgent: {}", message);
    }
}
```

**Time to Fix**: 10-15 minutes

---

## Time Estimates (Production Data)

| Pattern | Implementation | Testing | Total | Complexity |
|---------|---------------|---------|-------|------------|
| Multi-Account Detection | 10-15 min | 8 min | 18-23 min | Medium |
| Bonus Abuse Detection | 12-18 min | 10 min | 22-28 min | Medium |
| Suspicious Betting | 15-20 min | 12 min | 27-32 min | High |
| Payment Fraud | 10-12 min | 8 min | 18-20 min | Low |
| Risk Scoring System | 15-20 min | 15 min | 30-35 min | High |
| KYC/AML Integration | 8-10 min | 5 min | 13-15 min | Low |

**Full Fraud Detection System**: 2-3 hours (all 6 patterns)

---

## Compliance Requirements (iGaming)

### Malta Gaming Authority (MGA)

**Requirements**:
- ✅ Multi-account detection (mandatory)
- ✅ Bonus abuse prevention (mandatory)
- ✅ Progressive KYC (Tier 1 → Tier 2 → Tier 3)
- ✅ AML screening for withdrawals >€2,000
- ✅ Suspicious transaction reporting (STR) within 24 hours

### Curacao License

**Requirements**:
- ✅ Device fingerprinting (recommended)
- ✅ Real-time risk scoring (mandatory)
- ✅ KYC verification for withdrawals >$1,000
- ✅ Payment fraud detection (card testing prevention)

---

## Validation Checklist

Before deploying fraud detection system:

- [ ] Multi-account detection implemented and tested
- [ ] Bonus abuse detection active
- [ ] Real-time risk scoring integrated with Redis
- [ ] KYC auto-trigger configured (risk score thresholds)
- [ ] Alert system connected to monitoring (Slack, email, SMS)
- [ ] Database indexes created for fraud tables
- [ ] GDPR compliance: Hash PII, 7-year retention policy
- [ ] Performance tested: Detection <100ms per transaction
- [ ] False positive rate monitored and tuned

---

**See Also**:
- [Example: Multi-Account Detection](../examples/example-1-multi-account-detection.md) - Real-world implementation
- [iGame Feature Builder](../../igame-feature-builder/) - VIP system, Wallet API, Bonus engine
- [LiteFlow Rule Builder](../../liteflow-rule-builder/) - Rule engine for fraud workflows
- [SmartAdmin Patterns](./../../../.claude/shared/knowledge/smartadmin-patterns.md) - ResponseDTO, Transaction management
