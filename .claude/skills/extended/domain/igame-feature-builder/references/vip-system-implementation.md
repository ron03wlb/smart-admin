# VIP System Implementation Guide

**Version**: 1.0.0
**Last Updated**: 2026-01-24

Complete implementation guide for VIP tier system with automatic upgrades/downgrades, personalized benefits, and event-driven architecture.

---

## Table of Contents

1. [VIP Tier Matrix](#vip-tier-matrix)
2. [Database Schema](#database-schema)
3. [Tier Calculation Logic](#tier-calculation-logic)
4. [Upgrade/Downgrade Rules](#upgradedowngrade-rules)
5. [Benefits Distribution](#benefits-distribution)
6. [Event-Driven Architecture](#event-driven-architecture)
7. [Testing Strategy](#testing-strategy)

---

## VIP Tier Matrix

### Standard 5-Tier System

| Tier | Monthly GGR (USD) | Total Deposits (30d) | Min Sessions | Retention Period | Benefits |
|------|-------------------|----------------------|--------------|------------------|----------|
| **Bronze** | $0 - $499 | $0 - $999 | 0 | N/A (default) | 5% cashback, 25% reload bonus |
| **Silver** | $500 - $1,999 | $1,000 - $4,999 | 10 | 30 days | 7% cashback, 35% reload bonus |
| **Gold** | $2,000 - $4,999 | $5,000 - $14,999 | 20 | 60 days | 10% cashback, 50% reload bonus |
| **Platinum** | $5,000 - $14,999 | $15,000 - $49,999 | 30 | 90 days | 15% cashback, 75% reload bonus |
| **Diamond** | $15,000+ | $50,000+ | 40 | 180 days | 20% cashback, 100% reload bonus |

**Upgrade Condition:** Player must meet ALL THREE criteria (GGR + Deposits + Sessions).

**Downgrade Condition:** Player fails ANY ONE criterion AND grace period expires.

**Grace Period:** Protection mechanism to prevent temporary inactivity from causing downgrades.

---

## Database Schema

### Entity: VipTierEntity

```java
package net.lab1024.sa.admin.module.business.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_vip_tier")
public class VipTierEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;

    @TableField("vip_tier")
    private String tier; // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND

    private LocalDateTime effectiveDate;

    private LocalDateTime expiryDate; // Null if no grace period active

    @TableField("grace_period_days")
    private Integer gracePeriodDays;

    private Boolean inGracePeriod;

    // Metrics at time of tier assignment
    private BigDecimal monthlyGgr;
    private BigDecimal totalDeposits;
    private Integer sessionCount;

    // Metadata
    private String upgradeReason; // e.g., "Met Gold tier criteria on deposit"
    private String downgradeReason; // e.g., "Failed to maintain Silver GGR threshold"

    // Audit fields
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long operatorId; // Null for automatic, operator ID for manual override

    @Version
    private Integer version; // Optimistic locking
}
```

### Entity: VipTierConfigEntity

```java
package net.lab1024.sa.admin.module.business.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("t_vip_tier_config")
public class VipTierConfigEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId; // Multi-tenant support

    private String tier;

    // Thresholds
    private BigDecimal minMonthlyGgr;
    private BigDecimal maxMonthlyGgr; // Null for top tier
    private BigDecimal minDeposits30d;
    private BigDecimal maxDeposits30d; // Null for top tier
    private Integer minSessions;

    // Retention
    private Integer gracePeriodDays;

    // Benefits
    private BigDecimal cashbackRate; // e.g., 0.10 for 10%
    private BigDecimal reloadBonusRate;
    private BigDecimal dailyWithdrawalLimit;
    private BigDecimal monthlyWithdrawalLimit;

    // Metadata
    private String colorCode; // e.g., #FFD700 for Gold
    private Boolean active;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Version
    private Integer version;
}
```

### Entity: PlayerStatsEntity

```java
package net.lab1024.sa.admin.module.business.vip.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("t_player_stats_daily")
public class PlayerStatsEntity {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long playerId;
    private LocalDate statDate;

    // Daily stats
    private BigDecimal dailyGgr; // Bets - Wins
    private BigDecimal dailyDeposits;
    private BigDecimal dailyWithdrawals;
    private Integer dailySessions;

    // Rolling 30-day aggregates (updated daily)
    private BigDecimal rollingGgr30d;
    private BigDecimal rollingDeposits30d;
    private Integer rollingSessions30d;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableIndex(value = {"idx_player_date"}, columnList = "player_id, stat_date")
    @TableIndex(value = {"idx_stat_date"}, columnList = "stat_date")
}
```

---

## Tier Calculation Logic

### Manager: VipTierEvaluationManager

```java
package net.lab1024.sa.admin.module.business.vip.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.vip.dao.VipTierDao;
import net.lab1024.sa.admin.module.business.vip.dao.VipTierConfigDao;
import net.lab1024.sa.admin.module.business.vip.dao.PlayerStatsDao;
import net.lab1024.sa.admin.module.business.vip.domain.entity.VipTierEntity;
import net.lab1024.sa.admin.module.business.vip.domain.entity.VipTierConfigEntity;
import net.lab1024.sa.admin.module.business.vip.domain.entity.PlayerStatsEntity;
import net.lab1024.sa.admin.module.business.vip.domain.enums.VipTier;
import net.lab1024.sa.foundation.domain.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VipTierEvaluationManager {
    private final VipTierDao vipTierDao;
    private final VipTierConfigDao configDao;
    private final PlayerStatsDao statsDao;

    /**
     * Evaluate player's VIP tier eligibility based on current stats.
     * Called by: Kafka consumer (player-activity-events), scheduled job (daily evaluation)
     */
    @Transactional(rollbackFor = Throwable.class, isolation = Isolation.SERIALIZABLE)
    public void evaluateTierEligibility(Long playerId, LocalDate evaluationDate) {
        // 1. Fetch player's rolling 30-day stats
        PlayerStatsEntity stats = statsDao.getLatestStatsForPlayer(playerId);
        if (stats == null) {
            // New player - assign Bronze tier
            assignTier(playerId, VipTier.BRONZE, BigDecimal.ZERO, BigDecimal.ZERO, 0,
                "New player default tier", null);
            return;
        }

        // 2. Calculate eligible tier based on stats
        VipTier eligibleTier = calculateEligibleTier(
            stats.getRollingGgr30d(),
            stats.getRollingDeposits30d(),
            stats.getRollingSessions30d()
        );

        // 3. Get current tier
        VipTierEntity currentTier = vipTierDao.getCurrentTier(playerId);
        if (currentTier == null) {
            // First-time tier assignment
            assignTier(playerId, eligibleTier,
                stats.getRollingGgr30d(),
                stats.getRollingDeposits30d(),
                stats.getRollingSessions30d(),
                String.format("Initial tier assignment: %s", eligibleTier),
                null);
            return;
        }

        // 4. Compare eligible vs current tier
        VipTier currentVipTier = VipTier.valueOf(currentTier.getTier());

        if (eligibleTier.ordinal() > currentVipTier.ordinal()) {
            // Upgrade scenario
            upgradeTier(playerId, eligibleTier, stats, "Automatic upgrade based on activity");
        } else if (eligibleTier.ordinal() < currentVipTier.ordinal()) {
            // Downgrade scenario - check grace period
            handlePotentialDowngrade(playerId, currentTier, eligibleTier, evaluationDate);
        } else {
            // Same tier - cancel grace period if active
            if (Boolean.TRUE.equals(currentTier.getInGracePeriod())) {
                cancelGracePeriod(currentTier);
            }
        }
    }

    /**
     * Calculate eligible tier based on player stats.
     * Must meet ALL THREE criteria (GGR, Deposits, Sessions).
     */
    private VipTier calculateEligibleTier(
        BigDecimal rollingGgr30d,
        BigDecimal rollingDeposits30d,
        Integer rollingSessions30d
    ) {
        // Fetch tier configs (sorted by min thresholds DESC)
        List<VipTierConfigEntity> configs = configDao.getAllActiveConfigs();

        for (VipTierConfigEntity config : configs) {
            if (meetsAllCriteria(rollingGgr30d, rollingDeposits30d, rollingSessions30d, config)) {
                return VipTier.valueOf(config.getTier());
            }
        }

        // Default to Bronze if no tiers met
        return VipTier.BRONZE;
    }

    /**
     * Check if player meets ALL criteria for a tier.
     */
    private boolean meetsAllCriteria(
        BigDecimal ggr,
        BigDecimal deposits,
        Integer sessions,
        VipTierConfigEntity config
    ) {
        // Check GGR
        if (ggr.compareTo(config.getMinMonthlyGgr()) < 0) {
            return false;
        }
        if (config.getMaxMonthlyGgr() != null && ggr.compareTo(config.getMaxMonthlyGgr()) > 0) {
            return false;
        }

        // Check Deposits
        if (deposits.compareTo(config.getMinDeposits30d()) < 0) {
            return false;
        }
        if (config.getMaxDeposits30d() != null && deposits.compareTo(config.getMaxDeposits30d()) > 0) {
            return false;
        }

        // Check Sessions
        if (sessions < config.getMinSessions()) {
            return false;
        }

        return true;
    }

    /**
     * Upgrade player to higher tier (immediate).
     */
    private void upgradeTier(Long playerId, VipTier newTier, PlayerStatsEntity stats, String reason) {
        // Expire current tier
        VipTierEntity currentTier = vipTierDao.getCurrentTier(playerId);
        currentTier.setExpiryDate(LocalDateTime.now());
        vipTierDao.updateById(currentTier);

        // Assign new tier
        assignTier(playerId, newTier,
            stats.getRollingGgr30d(),
            stats.getRollingDeposits30d(),
            stats.getRollingSessions30d(),
            reason,
            null);

        // Publish upgrade event (for notifications, benefits activation)
        publishTierChangeEvent(playerId, currentTier.getTier(), newTier.name(), "UPGRADE");
    }

    /**
     * Handle potential downgrade - initiate or continue grace period.
     */
    private void handlePotentialDowngrade(
        Long playerId,
        VipTierEntity currentTier,
        VipTier eligibleTier,
        LocalDate evaluationDate
    ) {
        if (Boolean.TRUE.equals(currentTier.getInGracePeriod())) {
            // Already in grace period - check if expired
            if (LocalDateTime.now().isAfter(currentTier.getExpiryDate())) {
                // Grace period expired - downgrade
                downgradeTier(playerId, eligibleTier, "Grace period expired without recovery");
            }
            // Else: Still in grace period, wait
        } else {
            // Initiate grace period
            VipTierConfigEntity config = configDao.getConfigForTier(currentTier.getTier());
            LocalDateTime graceExpiryDate = LocalDateTime.now().plusDays(config.getGracePeriodDays());

            currentTier.setInGracePeriod(true);
            currentTier.setExpiryDate(graceExpiryDate);
            currentTier.setGracePeriodDays(config.getGracePeriodDays());
            vipTierDao.updateById(currentTier);

            // Send grace period notification
            publishGracePeriodNotification(playerId, currentTier.getTier(), eligibleTier.name(), graceExpiryDate);
        }
    }

    /**
     * Downgrade player to lower tier (after grace period expires).
     */
    private void downgradeTier(Long playerId, VipTier newTier, String reason) {
        // Expire current tier
        VipTierEntity currentTier = vipTierDao.getCurrentTier(playerId);
        currentTier.setExpiryDate(LocalDateTime.now());
        vipTierDao.updateById(currentTier);

        // Assign new tier (lower)
        PlayerStatsEntity stats = statsDao.getLatestStatsForPlayer(playerId);
        assignTier(playerId, newTier,
            stats.getRollingGgr30d(),
            stats.getRollingDeposits30d(),
            stats.getRollingSessions30d(),
            reason,
            null);

        // Publish downgrade event
        publishTierChangeEvent(playerId, currentTier.getTier(), newTier.name(), "DOWNGRADE");
    }

    /**
     * Cancel grace period when player recovers activity.
     */
    private void cancelGracePeriod(VipTierEntity tier) {
        tier.setInGracePeriod(false);
        tier.setExpiryDate(null);
        vipTierDao.updateById(tier);
    }

    /**
     * Assign VIP tier to player.
     */
    private void assignTier(
        Long playerId,
        VipTier tier,
        BigDecimal ggr,
        BigDecimal deposits,
        Integer sessions,
        String reason,
        Long operatorId
    ) {
        VipTierConfigEntity config = configDao.getConfigForTier(tier.name());

        VipTierEntity entity = new VipTierEntity();
        entity.setPlayerId(playerId);
        entity.setTier(tier.name());
        entity.setEffectiveDate(LocalDateTime.now());
        entity.setExpiryDate(null); // No grace period for new assignments
        entity.setGracePeriodDays(config.getGracePeriodDays());
        entity.setInGracePeriod(false);
        entity.setMonthlyGgr(ggr);
        entity.setTotalDeposits(deposits);
        entity.setSessionCount(sessions);
        entity.setUpgradeReason(reason);
        entity.setOperatorId(operatorId);

        vipTierDao.insert(entity);
    }

    // Event publishing methods (Kafka integration)
    private void publishTierChangeEvent(Long playerId, String oldTier, String newTier, String changeType) {
        // Implement Kafka event publishing
    }

    private void publishGracePeriodNotification(Long playerId, String currentTier, String targetTier, LocalDateTime expiryDate) {
        // Implement notification service
    }
}
```

---

## Upgrade/Downgrade Rules

### Upgrade Flow

1. **Trigger:** Player activity event (deposit, bet, session)
2. **Evaluation:** Calculate rolling 30-day stats
3. **Comparison:** Check if meets higher tier criteria (ALL THREE)
4. **Action:** Immediate upgrade
5. **Notification:** Send upgrade email + in-app message
6. **Benefits:** Activate new tier benefits (cashback rate, bonus rate)

**Example:**
- Player (Silver tier) deposits $10,000 on Jan 15
- Rolling 30-day deposits: $10,000 (was $3,000)
- Rolling 30-day GGR: $4,000 (meets Gold threshold)
- Sessions: 25 (meets Gold threshold)
- **Result:** Upgraded to Gold tier immediately

---

### Downgrade Flow

1. **Trigger:** Daily evaluation job (00:00 UTC)
2. **Evaluation:** Calculate rolling 30-day stats
3. **Comparison:** Check if fails current tier criteria (ANY ONE)
4. **Grace Period:**
   - First failure: Initiate grace period (30-180 days based on tier)
   - Send notification: "Your VIP tier is at risk"
5. **Recovery:**
   - If player recovers activity: Cancel grace period
   - If player doesn't recover: Downgrade after grace period expires
6. **Action:** Downgrade to eligible tier
7. **Notification:** Send downgrade email + in-app message

**Example:**
- Player (Platinum tier) inactive for 60 days
- Rolling 30-day GGR: $1,000 (below Platinum threshold of $5,000)
- **Day 1:** Initiate 90-day grace period
- **Day 83:** Send warning notification ("7 days until downgrade")
- **Day 90:** If still below threshold, downgrade to Gold tier

---

## Benefits Distribution

### Cashback Benefit

**Trigger:** Daily/weekly/monthly (configurable)

**Calculation:**
```java
@Service
@RequiredArgsConstructor
public class VipCashbackManager {
    private final VipTierDao vipTierDao;
    private final VipTierConfigDao configDao;
    private final PlayerStatsDao statsDao;
    private final WalletTransactionManager walletManager;

    @Transactional(rollbackFor = Throwable.class)
    public void distributeDailyCashback(Long playerId, LocalDate cashbackDate) {
        // 1. Get current VIP tier
        VipTierEntity tier = vipTierDao.getCurrentTier(playerId);
        VipTierConfigEntity config = configDao.getConfigForTier(tier.getTier());

        // 2. Calculate losses for period
        PlayerStatsEntity stats = statsDao.getStatsForDate(playerId, cashbackDate);
        BigDecimal dailyLosses = stats.getDailyGgr().negate(); // GGR is negative for losses
        if (dailyLosses.compareTo(BigDecimal.ZERO) <= 0) {
            return; // No losses = no cashback
        }

        // 3. Calculate cashback amount
        BigDecimal cashbackAmount = dailyLosses.multiply(config.getCashbackRate())
            .setScale(2, RoundingMode.HALF_UP);

        // 4. Credit to wallet
        walletManager.creditCashback(playerId, cashbackAmount, "USD",
            String.format("Daily VIP cashback (%s tier)", tier.getTier()));
    }
}
```

---

## Event-Driven Architecture

### Kafka Topics

1. **player-deposit-events:** Trigger VIP tier evaluation on deposit
2. **player-bet-events:** Update session count, GGR stats
3. **vip-tier-change-events:** Notify downstream systems (benefits, reporting)
4. **vip-grace-period-events:** Schedule notifications

### Event Schemas

```java
// Deposit Event
public class PlayerDepositEvent {
    private Long playerId;
    private Long transactionId;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime timestamp;
}

// VIP Tier Change Event
public class VipTierChangeEvent {
    private Long playerId;
    private String oldTier;
    private String newTier;
    private String changeType; // UPGRADE, DOWNGRADE, MANUAL_OVERRIDE
    private String reason;
    private LocalDateTime timestamp;
}
```

---

## Testing Strategy

### Unit Tests

```java
@ExtendWith(MockitoExtension.class)
class VipTierEvaluationManagerTest {

    @Mock
    private VipTierDao vipTierDao;
    @Mock
    private VipTierConfigDao configDao;
    @Mock
    private PlayerStatsDao statsDao;

    @InjectMocks
    private VipTierEvaluationManager manager;

    @Test
    void testUpgradeToGoldTier() {
        // Given: Player meets Gold tier criteria
        PlayerStatsEntity stats = new PlayerStatsEntity();
        stats.setRollingGgr30d(new BigDecimal("3000")); // Gold threshold: $2K-$5K
        stats.setRollingDeposits30d(new BigDecimal("6000")); // Gold threshold: $5K-$15K
        stats.setRollingSessions30d(25); // Gold threshold: 20 sessions

        when(statsDao.getLatestStatsForPlayer(1L)).thenReturn(stats);

        VipTierEntity currentTier = new VipTierEntity();
        currentTier.setTier("SILVER");
        when(vipTierDao.getCurrentTier(1L)).thenReturn(currentTier);

        // When: Evaluate tier eligibility
        manager.evaluateTierEligibility(1L, LocalDate.now());

        // Then: Player upgraded to Gold
        verify(vipTierDao).insert(argThat(tier ->
            tier.getTier().equals("GOLD") &&
            tier.getPlayerId().equals(1L)
        ));
    }

    @Test
    void testGracePeriodInitiation() {
        // Given: Player fails Platinum criteria
        PlayerStatsEntity stats = new PlayerStatsEntity();
        stats.setRollingGgr30d(new BigDecimal("3000")); // Below Platinum threshold
        stats.setRollingDeposits30d(new BigDecimal("10000"));
        stats.setRollingSessions30d(35);

        when(statsDao.getLatestStatsForPlayer(1L)).thenReturn(stats);

        VipTierEntity currentTier = new VipTierEntity();
        currentTier.setTier("PLATINUM");
        currentTier.setInGracePeriod(false);
        when(vipTierDao.getCurrentTier(1L)).thenReturn(currentTier);

        VipTierConfigEntity config = new VipTierConfigEntity();
        config.setGracePeriodDays(90);
        when(configDao.getConfigForTier("PLATINUM")).thenReturn(config);

        // When: Evaluate tier eligibility
        manager.evaluateTierEligibility(1L, LocalDate.now());

        // Then: Grace period initiated
        verify(vipTierDao).updateById(argThat(tier ->
            Boolean.TRUE.equals(tier.getInGracePeriod()) &&
            tier.getGracePeriodDays().equals(90)
        ));
    }
}
```

### Integration Tests

```java
@SpringBootTest
@Testcontainers
class VipTierIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
        .withDatabaseName("smartadmin_test")
        .withUsername("test")
        .withPassword("test");

    @Autowired
    private VipTierEvaluationManager manager;

    @Autowired
    private VipTierDao vipTierDao;

    @Autowired
    private PlayerStatsDao statsDao;

    @Test
    @Transactional
    void testFullUpgradeFlow() {
        // Setup: Create player with Bronze tier
        Long playerId = 1L;
        assignBronzeTier(playerId);

        // Simulate 30 days of activity meeting Gold criteria
        for (int i = 0; i < 30; i++) {
            PlayerStatsEntity stats = new PlayerStatsEntity();
            stats.setPlayerId(playerId);
            stats.setStatDate(LocalDate.now().minusDays(30 - i));
            stats.setDailyGgr(new BigDecimal("100")); // Total: $3,000
            stats.setDailyDeposits(new BigDecimal("200")); // Total: $6,000
            stats.setDailySessions(1); // Total: 30 sessions
            statsDao.insert(stats);
        }

        // Aggregate rolling stats
        statsDao.updateRollingStats(playerId);

        // Execute: Evaluate tier
        manager.evaluateTierEligibility(playerId, LocalDate.now());

        // Verify: Player upgraded to Gold
        VipTierEntity currentTier = vipTierDao.getCurrentTier(playerId);
        assertThat(currentTier.getTier()).isEqualTo("GOLD");
        assertThat(currentTier.getMonthlyGgr()).isEqualByComparingTo(new BigDecimal("3000"));
    }
}
```

---

## Best Practices

1. **Always use BigDecimal for financial calculations (GGR, deposits, cashback)**
2. **Use SERIALIZABLE isolation for tier updates to prevent race conditions**
3. **Event-driven evaluation (Kafka) instead of synchronous calls**
4. **Grace period as protection mechanism (prevent churn from temporary inactivity)**
5. **Audit trail for all tier changes (manual override requires operator ID)**
6. **Multi-tenant support (tenant-specific tier configs)**
7. **Notification strategy: 7 days before grace period expires**

---

**Related Documents:**
- [P1-11: VIP System Design (Source Spec)](/Users/zhangxuanrong/Documents/Workspace/Java/smart-admin/docs/iGame/technical-specs/P1-important/11-vip-system-design.md)
- [Wallet API Patterns](wallet-api-patterns.md)
- [Bonus Engine Guide](bonus-engine-guide.md)
