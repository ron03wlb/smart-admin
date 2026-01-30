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

## Core Fraud Detection Patterns

### 1. Multi-Account Detection (Device Fingerprinting + IP Analysis)

**Detection Signals:**
- Same device fingerprint across multiple accounts
- Same IP address for registration/login
- Similar betting patterns and game preferences
- Coordinated deposit/withdrawal timing
- Shared payment methods (same credit card)

**Implementation Pattern:**

```java
/**
 * Multi-Account Detection Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MultiAccountDetectionService {

    private final PlayerDao playerDao;
    private final DeviceFingerprintDao fingerprintDao;
    private final LoginHistoryDao loginHistoryDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect linked accounts by device fingerprint
     */
    public List<LinkedAccountGroup> detectByDeviceFingerprint(Long playerId) {
        Player player = playerDao.selectById(playerId);

        // Get all device fingerprints for this player
        List<DeviceFingerprint> playerFingerprints = fingerprintDao.selectList(
            new LambdaQueryWrapper<DeviceFingerprint>()
                .eq(DeviceFingerprint::getPlayerId, playerId)
        );

        List<LinkedAccountGroup> groups = new ArrayList<>();

        for (DeviceFingerprint fingerprint : playerFingerprints) {
            // Find other players using same device
            List<Long> linkedPlayerIds = fingerprintDao.findPlayersByFingerprint(
                fingerprint.getFingerprintHash()
            );

            if (linkedPlayerIds.size() > 1) {
                LinkedAccountGroup group = new LinkedAccountGroup();
                group.setDetectionMethod("DEVICE_FINGERPRINT");
                group.setPlayerId(playerId);
                group.setLinkedPlayerIds(linkedPlayerIds);
                group.setRiskScore(calculateLinkRiskScore(linkedPlayerIds));
                group.setFingerprintHash(fingerprint.getFingerprintHash());
                groups.add(group);

                log.warn("[Fraud Detection] Multi-account detected: player={}, linkedAccounts={}, method=DEVICE",
                    playerId, linkedPlayerIds.size() - 1);
            }
        }

        return groups;
    }

    /**
     * Detect linked accounts by IP address
     */
    public List<LinkedAccountGroup> detectByIpAddress(Long playerId) {
        // Get recent login IPs (last 30 days)
        List<String> playerIps = loginHistoryDao.getRecentIps(playerId, 30);

        List<LinkedAccountGroup> groups = new ArrayList<>();

        for (String ip : playerIps) {
            // Find other players using same IP
            List<Long> linkedPlayerIds = loginHistoryDao.findPlayersByIp(ip, 30);

            if (linkedPlayerIds.size() > 1) {
                // Filter out obvious false positives (same household, same office)
                if (isHighRiskIpPattern(ip, linkedPlayerIds)) {
                    LinkedAccountGroup group = new LinkedAccountGroup();
                    group.setDetectionMethod("IP_ADDRESS");
                    group.setPlayerId(playerId);
                    group.setLinkedPlayerIds(linkedPlayerIds);
                    group.setRiskScore(calculateIpRiskScore(ip, linkedPlayerIds));
                    group.setIpAddress(ip);
                    groups.add(group);

                    log.warn("[Fraud Detection] Multi-account detected: player={}, linkedAccounts={}, method=IP, ip={}",
                        playerId, linkedPlayerIds.size() - 1, maskIp(ip));
                }
            }
        }

        return groups;
    }

    /**
     * Calculate risk score for linked accounts
     */
    private int calculateLinkRiskScore(List<Long> linkedPlayerIds) {
        int baseScore = 30;  // Base score for multi-account

        // Check for coordinated behavior
        if (hasCoordinatedDeposits(linkedPlayerIds)) {
            baseScore += 20;
        }

        if (hasCoordinatedWithdrawals(linkedPlayerIds)) {
            baseScore += 20;
        }

        if (hasSimilarBettingPatterns(linkedPlayerIds)) {
            baseScore += 15;
        }

        if (hasSharedPaymentMethods(linkedPlayerIds)) {
            baseScore += 15;
        }

        return Math.min(baseScore, 100);  // Cap at 100
    }

    /**
     * High-risk IP patterns (coordinated actions)
     */
    private boolean isHighRiskIpPattern(String ip, List<Long> playerIds) {
        // Public WiFi / VPN / Proxy: Lower risk (many false positives)
        if (isPublicIp(ip) || isVpnIp(ip)) {
            return false;
        }

        // Residential IP with >5 accounts: High risk
        if (playerIds.size() > 5) {
            return true;
        }

        // Residential IP with coordinated timing: High risk
        return hasCoordinatedRegistration(playerIds);
    }
}
```

**Database Schema:**

```sql
-- Device fingerprint tracking
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    fingerprint_hash VARCHAR(64) NOT NULL,  -- SHA-256 hash
    user_agent TEXT,
    screen_resolution VARCHAR(20),
    timezone VARCHAR(50),
    language VARCHAR(10),
    canvas_hash VARCHAR(64),
    webgl_hash VARCHAR(64),
    first_seen_at TIMESTAMP NOT NULL,
    last_seen_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_device_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_device_fingerprint_hash ON device_fingerprints(fingerprint_hash);
CREATE INDEX idx_device_player_id ON device_fingerprints(player_id);

-- Linked account groups
CREATE TABLE linked_account_groups (
    id BIGSERIAL PRIMARY KEY,
    detection_method VARCHAR(50) NOT NULL,  -- DEVICE, IP, PAYMENT, BEHAVIOR
    risk_score INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING, CONFIRMED, FALSE_POSITIVE
    reviewed_by BIGINT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE linked_account_members (
    group_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    PRIMARY KEY (group_id, player_id),

    CONSTRAINT fk_linked_group FOREIGN KEY (group_id) REFERENCES linked_account_groups(id),
    CONSTRAINT fk_linked_player FOREIGN KEY (player_id) REFERENCES players(id)
);
```

---

### 2. Bonus Abuse Detection (Velocity + Pattern Matching)

**Detection Signals:**
- Rapid account creation and bonus claiming
- Minimum deposit to unlock bonus, immediate withdrawal attempt
- Betting patterns designed to clear wagering requirements with minimal risk
- Bonus arbitrage across multiple promotions

**Implementation Pattern:**

```java
/**
 * Bonus Abuse Detection Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BonusAbuseDetectionService {

    private final BonusClaimDao bonusClaimDao;
    private final PlayerDao playerDao;
    private final BettingHistoryDao bettingHistoryDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect rapid bonus claiming (velocity check)
     */
    public boolean detectRapidBonusClaiming(Long playerId) {
        // Count bonuses claimed in last 24 hours
        int bonusCount24h = bonusClaimDao.countBonusesClaimed(playerId, Duration.ofHours(24));

        if (bonusCount24h > 3) {
            log.warn("[Fraud Detection] Rapid bonus claiming: player={}, count={}",
                playerId, bonusCount24h);

            riskScoreManager.addRiskPoints(playerId, 25, "RAPID_BONUS_CLAIMING");
            return true;
        }

        return false;
    }

    /**
     * Detect low-risk wagering (clearing bonus with minimal risk)
     */
    public boolean detectLowRiskWagering(Long playerId, Long bonusId) {
        List<Bet> bets = bettingHistoryDao.getBetsForBonus(playerId, bonusId);

        // Calculate average odds
        double avgOdds = bets.stream()
            .mapToDouble(Bet::getOdds)
            .average()
            .orElse(0.0);

        // Low-risk pattern: Betting on near-certain outcomes (odds < 1.1)
        if (avgOdds < 1.1) {
            log.warn("[Fraud Detection] Low-risk wagering detected: player={}, avgOdds={}",
                playerId, avgOdds);

            riskScoreManager.addRiskPoints(playerId, 30, "LOW_RISK_WAGERING");
            return true;
        }

        // Opposite betting pattern: Hedging bets to guarantee win
        if (hasOppositeBets(bets)) {
            log.warn("[Fraud Detection] Opposite betting detected: player={}, bonusId={}",
                playerId, bonusId);

            riskScoreManager.addRiskPoints(playerId, 40, "OPPOSITE_BETTING");
            return true;
        }

        return false;
    }

    /**
     * Detect bonus arbitrage (exploiting multiple promotions)
     */
    public boolean detectBonusArbitrage(Long playerId) {
        // Check if player is claiming multiple incompatible bonuses
        List<BonusClaim> activeClaims = bonusClaimDao.getActiveClaims(playerId);

        if (activeClaims.size() > 2) {
            log.warn("[Fraud Detection] Multiple active bonuses: player={}, count={}",
                playerId, activeClaims.size());

            riskScoreManager.addRiskPoints(playerId, 20, "MULTIPLE_BONUSES");
            return true;
        }

        return false;
    }

    /**
     * Detect opposite betting (hedging strategy)
     */
    private boolean hasOppositeBets(List<Bet> bets) {
        // Group bets by game/event
        Map<String, List<Bet>> betsByEvent = bets.stream()
            .collect(Collectors.groupingBy(Bet::getEventId));

        for (List<Bet> eventBets : betsByEvent.values()) {
            // Check for opposite outcomes (e.g., Team A win + Team B win)
            if (hasConflictingOutcomes(eventBets)) {
                return true;
            }
        }

        return false;
    }
}
```

**Database Schema:**

```sql
-- Bonus abuse tracking
CREATE TABLE bonus_abuse_events (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    bonus_id BIGINT,
    abuse_type VARCHAR(50) NOT NULL,  -- RAPID_CLAIMING, LOW_RISK_WAGERING, ARBITRAGE
    risk_score INTEGER NOT NULL,
    details JSONB,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bonus_abuse_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_bonus_abuse_player ON bonus_abuse_events(player_id);
CREATE INDEX idx_bonus_abuse_type ON bonus_abuse_events(abuse_type);
```

---

### 3. Suspicious Betting Patterns (Arbitrage + Matched Betting)

**Detection Signals:**
- Arbitrage betting: Exploiting odds differences across bookmakers
- Matched betting: Risk-free profit using free bets
- Syndicate betting: Coordinated large bets from multiple accounts
- Late betting: Placing bets seconds before event ends

**Implementation Pattern:**

```java
/**
 * Suspicious Betting Detection Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SuspiciousBettingDetectionService {

    private final BettingHistoryDao bettingHistoryDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect arbitrage betting patterns
     */
    public boolean detectArbitrageBetting(Long playerId) {
        // Get recent bets (last 7 days)
        List<Bet> recentBets = bettingHistoryDao.getRecentBets(playerId, Duration.ofDays(7));

        // Group by event
        Map<String, List<Bet>> betsByEvent = recentBets.stream()
            .collect(Collectors.groupingBy(Bet::getEventId));

        for (Map.Entry<String, List<Bet>> entry : betsByEvent.entrySet()) {
            List<Bet> eventBets = entry.getValue();

            // Arbitrage pattern: Betting on all outcomes to guarantee profit
            if (coversAllOutcomes(eventBets)) {
                log.warn("[Fraud Detection] Arbitrage betting detected: player={}, event={}",
                    playerId, entry.getKey());

                riskScoreManager.addRiskPoints(playerId, 40, "ARBITRAGE_BETTING");
                return true;
            }
        }

        return false;
    }

    /**
     * Detect late betting (last-minute bets)
     */
    public boolean detectLateBetting(Long playerId, Bet bet) {
        Duration timeBeforeEvent = Duration.between(bet.getPlacedAt(), bet.getEventStartTime());

        // Suspicious if bet placed <30 seconds before event
        if (timeBeforeEvent.getSeconds() < 30) {
            log.warn("[Fraud Detection] Late betting detected: player={}, timeBeforeEvent={}s",
                playerId, timeBeforeEvent.getSeconds());

            riskScoreManager.addRiskPoints(playerId, 15, "LATE_BETTING");
            return true;
        }

        return false;
    }

    /**
     * Detect syndicate betting (coordinated large bets)
     */
    public boolean detectSyndicateBetting(Long playerId, Bet bet) {
        // Check if multiple players placed similar large bets on same event
        List<Bet> similarBets = bettingHistoryDao.findSimilarBets(
            bet.getEventId(),
            bet.getOutcome(),
            bet.getPlacedAt(),
            Duration.ofMinutes(5)  // Within 5 minutes
        );

        if (similarBets.size() > 3 && isLargeBet(bet)) {
            log.warn("[Fraud Detection] Syndicate betting detected: player={}, similarBets={}",
                playerId, similarBets.size());

            riskScoreManager.addRiskPoints(playerId, 50, "SYNDICATE_BETTING");
            return true;
        }

        return false;
    }

    /**
     * Check if bets cover all possible outcomes (arbitrage)
     */
    private boolean coversAllOutcomes(List<Bet> bets) {
        Set<String> outcomes = bets.stream()
            .map(Bet::getOutcome)
            .collect(Collectors.toSet());

        // For binary outcome (win/lose), need 2 bets
        // For 3-way (win/draw/lose), need 3 bets
        return outcomes.size() >= 2 && calculateGuaranteedProfit(bets) > 0;
    }

    /**
     * Calculate guaranteed profit from arbitrage
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

---

### 4. Payment Fraud Detection (Card Testing + Chargebacks)

**Detection Signals:**
- Multiple failed payment attempts (card testing)
- High chargeback rate
- Stolen credit card usage
- Mismatched billing address and player location

**Implementation Pattern:**

```java
/**
 * Payment Fraud Detection Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFraudDetectionService {

    private final PaymentTransactionDao transactionDao;
    private final RiskScoreManager riskScoreManager;

    /**
     * Detect card testing (multiple failed attempts)
     */
    public boolean detectCardTesting(Long playerId) {
        // Count failed payment attempts in last hour
        int failedAttempts = transactionDao.countFailedAttempts(
            playerId,
            Duration.ofHours(1)
        );

        if (failedAttempts >= 3) {
            log.warn("[Fraud Detection] Card testing detected: player={}, failedAttempts={}",
                playerId, failedAttempts);

            riskScoreManager.addRiskPoints(playerId, 40, "CARD_TESTING");

            // Auto-block player from making more payment attempts
            blockPayments(playerId, Duration.ofHours(24));
            return true;
        }

        return false;
    }

    /**
     * Detect high chargeback risk
     */
    public boolean detectChargebackRisk(Long playerId) {
        // Calculate chargeback rate (last 90 days)
        int totalTransactions = transactionDao.countTransactions(playerId, Duration.ofDays(90));
        int chargebacks = transactionDao.countChargebacks(playerId, Duration.ofDays(90));

        if (totalTransactions > 0) {
            double chargebackRate = (double) chargebacks / totalTransactions;

            if (chargebackRate > 0.05) {  // >5% chargeback rate
                log.warn("[Fraud Detection] High chargeback risk: player={}, rate={}%",
                    playerId, chargebackRate * 100);

                riskScoreManager.addRiskPoints(playerId, 50, "HIGH_CHARGEBACK_RATE");
                return true;
            }
        }

        return false;
    }

    /**
     * Detect stolen credit card usage (mismatched location)
     */
    public boolean detectStolenCard(Long playerId, PaymentTransaction transaction) {
        // Get player's country from KYC
        Player player = playerDao.selectById(playerId);
        String playerCountry = player.getCountry();

        // Get card's billing country
        String cardCountry = transaction.getBillingCountry();

        // High risk if countries don't match
        if (!playerCountry.equals(cardCountry)) {
            log.warn("[Fraud Detection] Country mismatch: player={}, playerCountry={}, cardCountry={}",
                playerId, playerCountry, cardCountry);

            riskScoreManager.addRiskPoints(playerId, 30, "COUNTRY_MISMATCH");
            return true;
        }

        return false;
    }
}
```

---

### 5. Real-Time Risk Scoring System

**Risk Score Calculation:**

```java
/**
 * Real-Time Risk Scoring Manager
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoreManager {

    private final RiskScoreDao riskScoreDao;
    private final RedissonClient redisson;
    private final AlertService alertService;

    /**
     * Calculate composite risk score
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

        // Calculate total score (0-100)
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

        log.info("[Risk Scoring] Added points: player={}, points={}, reason={}, totalScore={}",
            playerId, points, reason, newScore);

        // Auto-action if score exceeds threshold
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
            // Critical: Freeze account immediately
            playerManager.freezeAccount(playerId, "HIGH_RISK_SCORE");
            alertService.sendUrgentAlert("CRITICAL_RISK", playerId, score);
        } else if (score >= 70) {
            // High: Manual review required
            playerManager.flagForReview(playerId, "HIGH_RISK_SCORE");
            alertService.sendAlert("HIGH_RISK", playerId, score);
        }
    }
}
```

**Database Schema:**

```sql
-- Risk score tracking
CREATE TABLE risk_scores (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    total_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,  -- LOW, MEDIUM, HIGH, CRITICAL
    components JSONB NOT NULL,  -- {"MULTI_ACCOUNT": 30, "BONUS_ABUSE": 20, ...}
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_risk_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_risk_player_timestamp ON risk_scores(player_id, timestamp DESC);

-- Risk events
CREATE TABLE risk_events (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    points INTEGER NOT NULL,
    reason TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_risk_events_player ON risk_events(player_id, timestamp DESC);
```

---

### 6. KYC Verification Automation (Progressive KYC)

**Progressive KYC Integration with Fraud Detection:**

```java
/**
 * KYC Fraud Integration Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KycFraudIntegrationService {

    private final KycService kycService;
    private final RiskScoreManager riskScoreManager;
    private final WithdrawalService withdrawalService;

    /**
     * Trigger enhanced KYC for high-risk players
     */
    public void triggerEnhancedKyc(Long playerId, String reason) {
        Player player = playerDao.selectById(playerId);

        // Force upgrade to Tier 2 (Enhanced KYC) if not already
        if (player.getKycTier() < KycTier.TIER_2_ENHANCED.getLevel()) {
            kycService.forceUpgrade(playerId, KycTier.TIER_2_ENHANCED, reason);

            log.info("[Fraud Detection] Forced KYC upgrade: player={}, tier={}, reason={}",
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

        // Risk score 50-69: Require Tier 2 KYC
        if (riskScore >= 50 && riskScore < 70) {
            triggerEnhancedKyc(playerId, "HIGH_RISK_SCORE");
        }

        // Risk score 70+: Require Tier 3 KYC + manual review
        if (riskScore >= 70) {
            kycService.forceUpgrade(playerId, KycTier.TIER_3_FULL, "CRITICAL_RISK_SCORE");
            playerManager.flagForManualReview(playerId, "CRITICAL_RISK");
        }
    }
}
```

---

## Common Mistakes

### ❌ False Positives (Over-Detection)

**Problem:** Legitimate players flagged as fraudulent

**Solution:**
- Add whitelisting for known good players
- Machine learning to reduce false positives
- Manual review queue for borderline cases

```java
// Whitelist trusted players
if (playerService.isTrusted(playerId)) {
    log.info("[Fraud Detection] Skipping detection for trusted player: {}", playerId);
    return false;
}
```

### ❌ Performance Impact

**Problem:** Real-time fraud detection slows down transactions

**Solution:**
- Async processing with event listeners
- Redis caching for risk scores
- Batch processing for historical analysis

```java
@EventListener
@Async  // Don't block transaction
public void onWithdrawalRequest(WithdrawalRequestedEvent event) {
    fraudDetectionService.checkWithdrawal(event.getPlayerId(), event.getAmount());
}
```

### ❌ Data Privacy Violations

**Problem:** Storing excessive player data for fraud detection

**Solution:**
- Hash sensitive identifiers (IP, device fingerprint)
- GDPR compliance: Delete fraud data after 7 years
- Encrypt stored fraud evidence

```java
// Hash device fingerprint before storing
String hashedFingerprint = DigestUtils.sha256Hex(rawFingerprint);
```

---

## Rationalization Table

| Excuse | Reality |
|--------|---------|
| "Fraud detection is overkill for small platform" | Fraud scales with platform size. Implement from day 1. |
| "Manual review is cheaper than automation" | Manual review doesn't scale. Automate detection, manual review only high-risk. |
| "Too many false positives hurt UX" | Tune thresholds. False negatives (missing fraud) cost more than false positives. |
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

**Test Scenario:** Multi-account detection for bonus abuse

**Expected Detection Logic:**
1. Player registers 3 accounts using same device fingerprint
2. All 3 accounts claim welcome bonus
3. System detects device fingerprint match
4. System flags accounts as linked
5. System blocks bonus claims on linked accounts
6. Compliance team receives alert for manual review

**Database Queries:**
```sql
-- Find linked accounts by device fingerprint
SELECT player_id, COUNT(*) as account_count
FROM device_fingerprints
WHERE fingerprint_hash = 'abc123...'
GROUP BY player_id
HAVING COUNT(*) > 1;

-- Find players with multiple bonus claims
SELECT player_id, COUNT(*) as bonus_count
FROM bonus_claims
WHERE claimed_at > NOW() - INTERVAL '24 hours'
GROUP BY player_id
HAVING COUNT(*) > 2;
```

**Expected Outcome:**
- Detection rate: >90% of multi-account fraud
- False positive rate: <5%
- Processing time: <500ms per check
- Alert delivery: <30 seconds

---

## References

**iGaming Compliance:**
- [P0-04: KYC/AML Automation](docs/iGame/technical-specs/P0-critical/04-kyc-aml-automation.md)
- [P1-06: Real-Time Risk Engine](docs/iGame/technical-specs/P1-important/06-real-time-risk-engine.md)
- [P1-15: Security Hardening](docs/iGame/technical-specs/P1-important/15-security-hardening.md)

**SmartAdmin Patterns:**
- [Manager Layer Transactions](.agent/foundation/09-manager-layer.md)
- [Event-Driven Architecture](.claude/shared/knowledge/smartadmin-patterns.md)
