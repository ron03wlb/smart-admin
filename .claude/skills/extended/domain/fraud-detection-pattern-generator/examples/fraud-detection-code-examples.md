# Fraud Detection Code Examples

> Extracted from SKILL.md to keep the main file focused on AI agent instructions.
> This file contains complete Java implementation patterns, SQL schemas, and test scenarios.

---

## 1. Multi-Account Detection (Device Fingerprinting + IP Analysis)

### Detection Signals
- Same device fingerprint across multiple accounts
- Same IP address for registration/login
- Similar betting patterns and game preferences
- Coordinated deposit/withdrawal timing
- Shared payment methods (same credit card)

### Implementation

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

        List<DeviceFingerprint> playerFingerprints = fingerprintDao.selectList(
            new LambdaQueryWrapper<DeviceFingerprint>()
                .eq(DeviceFingerprint::getPlayerId, playerId)
        );

        List<LinkedAccountGroup> groups = new ArrayList<>();

        for (DeviceFingerprint fingerprint : playerFingerprints) {
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
        List<String> playerIps = loginHistoryDao.getRecentIps(playerId, 30);

        List<LinkedAccountGroup> groups = new ArrayList<>();

        for (String ip : playerIps) {
            List<Long> linkedPlayerIds = loginHistoryDao.findPlayersByIp(ip, 30);

            if (linkedPlayerIds.size() > 1) {
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

    private int calculateLinkRiskScore(List<Long> linkedPlayerIds) {
        int baseScore = 30;
        if (hasCoordinatedDeposits(linkedPlayerIds)) baseScore += 20;
        if (hasCoordinatedWithdrawals(linkedPlayerIds)) baseScore += 20;
        if (hasSimilarBettingPatterns(linkedPlayerIds)) baseScore += 15;
        if (hasSharedPaymentMethods(linkedPlayerIds)) baseScore += 15;
        return Math.min(baseScore, 100);
    }

    private boolean isHighRiskIpPattern(String ip, List<Long> playerIds) {
        if (isPublicIp(ip) || isVpnIp(ip)) return false;
        if (playerIds.size() > 5) return true;
        return hasCoordinatedRegistration(playerIds);
    }
}
```

### Database Schema

```sql
-- Device fingerprint tracking
CREATE TABLE device_fingerprints (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    fingerprint_hash VARCHAR(64) NOT NULL,
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
    detection_method VARCHAR(50) NOT NULL,
    risk_score INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
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

## 2. Bonus Abuse Detection (Velocity + Pattern Matching)

### Detection Signals
- Rapid account creation and bonus claiming
- Minimum deposit to unlock bonus, immediate withdrawal attempt
- Betting patterns designed to clear wagering requirements with minimal risk
- Bonus arbitrage across multiple promotions

### Implementation

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

    public boolean detectRapidBonusClaiming(Long playerId) {
        int bonusCount24h = bonusClaimDao.countBonusesClaimed(playerId, Duration.ofHours(24));
        if (bonusCount24h > 3) {
            log.warn("[Fraud Detection] Rapid bonus claiming: player={}, count={}", playerId, bonusCount24h);
            riskScoreManager.addRiskPoints(playerId, 25, "RAPID_BONUS_CLAIMING");
            return true;
        }
        return false;
    }

    public boolean detectLowRiskWagering(Long playerId, Long bonusId) {
        List<Bet> bets = bettingHistoryDao.getBetsForBonus(playerId, bonusId);
        double avgOdds = bets.stream().mapToDouble(Bet::getOdds).average().orElse(0.0);

        if (avgOdds < 1.1) {
            log.warn("[Fraud Detection] Low-risk wagering detected: player={}, avgOdds={}", playerId, avgOdds);
            riskScoreManager.addRiskPoints(playerId, 30, "LOW_RISK_WAGERING");
            return true;
        }

        if (hasOppositeBets(bets)) {
            log.warn("[Fraud Detection] Opposite betting detected: player={}, bonusId={}", playerId, bonusId);
            riskScoreManager.addRiskPoints(playerId, 40, "OPPOSITE_BETTING");
            return true;
        }
        return false;
    }

    public boolean detectBonusArbitrage(Long playerId) {
        List<BonusClaim> activeClaims = bonusClaimDao.getActiveClaims(playerId);
        if (activeClaims.size() > 2) {
            log.warn("[Fraud Detection] Multiple active bonuses: player={}, count={}", playerId, activeClaims.size());
            riskScoreManager.addRiskPoints(playerId, 20, "MULTIPLE_BONUSES");
            return true;
        }
        return false;
    }

    private boolean hasOppositeBets(List<Bet> bets) {
        Map<String, List<Bet>> betsByEvent = bets.stream()
            .collect(Collectors.groupingBy(Bet::getEventId));
        for (List<Bet> eventBets : betsByEvent.values()) {
            if (hasConflictingOutcomes(eventBets)) return true;
        }
        return false;
    }
}
```

### Database Schema

```sql
CREATE TABLE bonus_abuse_events (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    bonus_id BIGINT,
    abuse_type VARCHAR(50) NOT NULL,
    risk_score INTEGER NOT NULL,
    details JSONB,
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bonus_abuse_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_bonus_abuse_player ON bonus_abuse_events(player_id);
CREATE INDEX idx_bonus_abuse_type ON bonus_abuse_events(abuse_type);
```

---

## 3. Suspicious Betting Patterns (Arbitrage + Matched Betting)

### Detection Signals
- Arbitrage betting: Exploiting odds differences across bookmakers
- Matched betting: Risk-free profit using free bets
- Syndicate betting: Coordinated large bets from multiple accounts
- Late betting: Placing bets seconds before event ends

### Implementation

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

    public boolean detectArbitrageBetting(Long playerId) {
        List<Bet> recentBets = bettingHistoryDao.getRecentBets(playerId, Duration.ofDays(7));
        Map<String, List<Bet>> betsByEvent = recentBets.stream()
            .collect(Collectors.groupingBy(Bet::getEventId));

        for (Map.Entry<String, List<Bet>> entry : betsByEvent.entrySet()) {
            if (coversAllOutcomes(entry.getValue())) {
                log.warn("[Fraud Detection] Arbitrage betting detected: player={}, event={}",
                    playerId, entry.getKey());
                riskScoreManager.addRiskPoints(playerId, 40, "ARBITRAGE_BETTING");
                return true;
            }
        }
        return false;
    }

    public boolean detectLateBetting(Long playerId, Bet bet) {
        Duration timeBeforeEvent = Duration.between(bet.getPlacedAt(), bet.getEventStartTime());
        if (timeBeforeEvent.getSeconds() < 30) {
            log.warn("[Fraud Detection] Late betting detected: player={}, timeBeforeEvent={}s",
                playerId, timeBeforeEvent.getSeconds());
            riskScoreManager.addRiskPoints(playerId, 15, "LATE_BETTING");
            return true;
        }
        return false;
    }

    public boolean detectSyndicateBetting(Long playerId, Bet bet) {
        List<Bet> similarBets = bettingHistoryDao.findSimilarBets(
            bet.getEventId(), bet.getOutcome(), bet.getPlacedAt(), Duration.ofMinutes(5));

        if (similarBets.size() > 3 && isLargeBet(bet)) {
            log.warn("[Fraud Detection] Syndicate betting detected: player={}, similarBets={}",
                playerId, similarBets.size());
            riskScoreManager.addRiskPoints(playerId, 50, "SYNDICATE_BETTING");
            return true;
        }
        return false;
    }

    private boolean coversAllOutcomes(List<Bet> bets) {
        Set<String> outcomes = bets.stream().map(Bet::getOutcome).collect(Collectors.toSet());
        return outcomes.size() >= 2 && calculateGuaranteedProfit(bets) > 0;
    }

    private double calculateGuaranteedProfit(List<Bet> bets) {
        double totalStake = bets.stream().mapToDouble(Bet::getStake).sum();
        double minReturn = bets.stream()
            .mapToDouble(b -> b.getStake() * b.getOdds()).min().orElse(0.0);
        return minReturn - totalStake;
    }
}
```

---

## 4. Payment Fraud Detection (Card Testing + Chargebacks)

### Detection Signals
- Multiple failed payment attempts (card testing)
- High chargeback rate
- Stolen credit card usage
- Mismatched billing address and player location

### Implementation

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

    public boolean detectCardTesting(Long playerId) {
        int failedAttempts = transactionDao.countFailedAttempts(playerId, Duration.ofHours(1));
        if (failedAttempts >= 3) {
            log.warn("[Fraud Detection] Card testing detected: player={}, failedAttempts={}",
                playerId, failedAttempts);
            riskScoreManager.addRiskPoints(playerId, 40, "CARD_TESTING");
            blockPayments(playerId, Duration.ofHours(24));
            return true;
        }
        return false;
    }

    public boolean detectChargebackRisk(Long playerId) {
        int totalTransactions = transactionDao.countTransactions(playerId, Duration.ofDays(90));
        int chargebacks = transactionDao.countChargebacks(playerId, Duration.ofDays(90));
        if (totalTransactions > 0) {
            double chargebackRate = (double) chargebacks / totalTransactions;
            if (chargebackRate > 0.05) {
                log.warn("[Fraud Detection] High chargeback risk: player={}, rate={}%",
                    playerId, chargebackRate * 100);
                riskScoreManager.addRiskPoints(playerId, 50, "HIGH_CHARGEBACK_RATE");
                return true;
            }
        }
        return false;
    }

    public boolean detectStolenCard(Long playerId, PaymentTransaction transaction) {
        Player player = playerDao.selectById(playerId);
        if (!player.getCountry().equals(transaction.getBillingCountry())) {
            log.warn("[Fraud Detection] Country mismatch: player={}, playerCountry={}, cardCountry={}",
                playerId, player.getCountry(), transaction.getBillingCountry());
            riskScoreManager.addRiskPoints(playerId, 30, "COUNTRY_MISMATCH");
            return true;
        }
        return false;
    }
}
```

---

## 5. Real-Time Risk Scoring System

### Implementation

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

    public RiskScore calculateRiskScore(Long playerId) {
        RiskScore score = new RiskScore();
        score.setPlayerId(playerId);
        score.setTimestamp(LocalDateTime.now());

        score.addComponent("MULTI_ACCOUNT", detectMultiAccountRisk(playerId));
        score.addComponent("BONUS_ABUSE", detectBonusAbuseRisk(playerId));
        score.addComponent("BETTING_PATTERN", detectBettingPatternRisk(playerId));
        score.addComponent("PAYMENT_FRAUD", detectPaymentFraudRisk(playerId));

        int totalScore = score.getTotalScore();
        score.setRiskLevel(getRiskLevel(totalScore));
        cacheRiskScore(playerId, score);

        if (totalScore >= 70) {
            alertService.sendHighRiskAlert(playerId, score);
        }
        return score;
    }

    public void addRiskPoints(Long playerId, int points, String reason) {
        String key = "risk:player:" + playerId;
        RAtomicLong riskScore = redisson.getAtomicLong(key);
        long newScore = riskScore.addAndGet(points);
        riskScore.expire(Duration.ofHours(24));

        RiskEvent event = RiskEvent.builder()
            .playerId(playerId).points(points).reason(reason)
            .timestamp(LocalDateTime.now()).build();
        riskScoreDao.insertEvent(event);

        log.info("[Risk Scoring] Added points: player={}, points={}, reason={}, totalScore={}",
            playerId, points, reason, newScore);

        if (newScore >= 70) triggerHighRiskAction(playerId, newScore);
    }

    private RiskLevel getRiskLevel(int score) {
        if (score < 30) return RiskLevel.LOW;
        if (score < 50) return RiskLevel.MEDIUM;
        if (score < 70) return RiskLevel.HIGH;
        return RiskLevel.CRITICAL;
    }

    private void triggerHighRiskAction(Long playerId, long score) {
        if (score >= 90) {
            playerManager.freezeAccount(playerId, "HIGH_RISK_SCORE");
            alertService.sendUrgentAlert("CRITICAL_RISK", playerId, score);
        } else if (score >= 70) {
            playerManager.flagForReview(playerId, "HIGH_RISK_SCORE");
            alertService.sendAlert("HIGH_RISK", playerId, score);
        }
    }
}
```

### Database Schema

```sql
CREATE TABLE risk_scores (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    total_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    components JSONB NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_risk_player FOREIGN KEY (player_id) REFERENCES players(id)
);

CREATE INDEX idx_risk_player_timestamp ON risk_scores(player_id, timestamp DESC);

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

## 6. KYC Verification Automation (Progressive KYC)

### Implementation

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

    public void triggerEnhancedKyc(Long playerId, String reason) {
        Player player = playerDao.selectById(playerId);
        if (player.getKycTier() < KycTier.TIER_2_ENHANCED.getLevel()) {
            kycService.forceUpgrade(playerId, KycTier.TIER_2_ENHANCED, reason);
            log.info("[Fraud Detection] Forced KYC upgrade: player={}, tier={}, reason={}",
                playerId, KycTier.TIER_2_ENHANCED, reason);
        }
        withdrawalService.blockWithdrawals(playerId, "PENDING_ENHANCED_KYC");
    }

    @EventListener
    public void onHighRiskDetected(HighRiskEvent event) {
        Long playerId = event.getPlayerId();
        int riskScore = event.getRiskScore();

        if (riskScore >= 50 && riskScore < 70) {
            triggerEnhancedKyc(playerId, "HIGH_RISK_SCORE");
        }
        if (riskScore >= 70) {
            kycService.forceUpgrade(playerId, KycTier.TIER_3_FULL, "CRITICAL_RISK_SCORE");
            playerManager.flagForManualReview(playerId, "CRITICAL_RISK");
        }
    }
}
```

---

## 7. RED Phase Baseline Test

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

**Version**: 1.0.0
**Extracted From**: SKILL.md
**Last Updated**: 2026-02-06
