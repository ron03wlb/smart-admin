package net.lab1024.sa.igaming.integration.journey;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.vip.dao.PlayerVipHistoryDao;
import net.lab1024.sa.igaming.player.vip.dao.VipLevelConfigDao;
import net.lab1024.sa.igaming.player.vip.dao.VipRewardRecordDao;
import net.lab1024.sa.igaming.player.vip.domain.entity.PlayerVipHistoryEntity;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipLevelConfigEntity;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipRewardRecordEntity;
import net.lab1024.sa.igaming.player.vip.service.VipAutoUpgradeService;
import net.lab1024.sa.igaming.player.vip.service.VipLevelConfigService;
import net.lab1024.sa.igaming.player.vip.service.VipRewardDistributionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * VIP Auto-Upgrade Journey Integration Test.
 *
 * <p>Tests the complete VIP auto-upgrade flow from eligibility check to reward distribution.
 *
 * <p><b>Test Coverage:</b>
 *
 * <ul>
 *   <li>Single condition upgrade (turnover only)
 *   <li>Multi-condition upgrade (deposit + turnover + active days)
 *   <li>Auto-upgrade success (Bronze → Silver)
 *   <li>Multi-level jump upgrade (Bronze → Gold, skip Silver)
 *   <li>Manual upgrade (admin override)
 *   <li>Upgrade reward distribution (LEVEL_UP_BONUS)
 *   <li>Upgrade history verification
 *   <li>Ineligible player does not upgrade
 *   <li>Max level player does not upgrade
 *   <li>VIP level config validity period check
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@DisplayName("VIP Auto-Upgrade Journey Integration Test")
class VipAutoUpgradeJourneyIntegrationTest extends BaseIntegrationTest {

  @Autowired private VipLevelConfigService vipLevelConfigService;

  @Autowired private VipAutoUpgradeService vipAutoUpgradeService;

  @Autowired private VipRewardDistributionService vipRewardDistributionService;

  @Autowired private PlayerDao playerDao;

  @Autowired private VipLevelConfigDao vipLevelConfigDao;

  @Autowired private PlayerVipHistoryDao playerVipHistoryDao;

  @Autowired private VipRewardRecordDao vipRewardRecordDao;

  private Long testPlayer1Id; // Bronze VIP player
  private Long testPlayer2Id; // Silver VIP player
  private Long testPlayer3Id; // Supreme VIP player (max level)
  private Long testAdminId; // Admin for manual upgrades

  @BeforeEach
  void setUpTestData() {
    // Create test player 1 (Bronze VIP - Level 1)
    PlayerEntity player1 = new PlayerEntity();
    player1.setUsername("test_vip_player_bronze");
    player1.setPasswordHash("hash");
    player1.setEmailEncrypted("bronze@test.com");
    player1.setStatus(1); // ACTIVE
    player1.setVipLevel(1); // Bronze
    player1.setDeleted(false);
    playerDao.insert(player1);
    testPlayer1Id = player1.getPlayerId();

    // Create test player 2 (Silver VIP - Level 2)
    PlayerEntity player2 = new PlayerEntity();
    player2.setUsername("test_vip_player_silver");
    player2.setPasswordHash("hash");
    player2.setEmailEncrypted("silver@test.com");
    player2.setStatus(1); // ACTIVE
    player2.setVipLevel(2); // Silver
    player2.setDeleted(false);
    playerDao.insert(player2);
    testPlayer2Id = player2.getPlayerId();

    // Create test player 3 (Supreme VIP - Level 10, max level)
    PlayerEntity player3 = new PlayerEntity();
    player3.setUsername("test_vip_player_supreme");
    player3.setPasswordHash("hash");
    player3.setEmailEncrypted("supreme@test.com");
    player3.setStatus(1); // ACTIVE
    player3.setVipLevel(10); // Supreme (max level)
    player3.setDeleted(false);
    playerDao.insert(player3);
    testPlayer3Id = player3.getPlayerId();

    // Create test admin
    PlayerEntity admin = new PlayerEntity();
    admin.setUsername("test_admin_vip");
    admin.setPasswordHash("hash");
    admin.setEmailEncrypted("admin@test.com");
    admin.setStatus(1); // ACTIVE
    admin.setVipLevel(1);
    admin.setDeleted(false);
    playerDao.insert(admin);
    testAdminId = admin.getPlayerId();
  }

  @Test
  @DisplayName("Test 1: Player reaches upgrade condition (single condition - turnover only)")
  void testPlayerReachesUpgradeConditionSingleCondition() {
    // Given: Bronze player (Level 1) meets Silver requirement (turnover only)
    // VIP Level 2 (Silver) requirements (from V015 seed):
    // - Deposit: $1,000
    // - Turnover: $5,000
    // - Active Days: 7 days

    // Player has:
    // - Deposit: $2,000 (exceeds requirement)
    // - Turnover: $6,000 (exceeds requirement)
    // - Active Days: 10 (exceeds requirement)
    BigDecimal totalDeposit = new BigDecimal("2000.00");
    BigDecimal totalTurnover = new BigDecimal("6000.00");
    Integer activeDays = 10;

    // When: Check eligibility for Level 2 (Silver)
    boolean eligible =
        vipLevelConfigService.isEligibleForLevel(2, totalDeposit, totalTurnover, activeDays);

    // Then: Player should be eligible for Silver
    assertThat(eligible).isTrue();
  }

  @Test
  @DisplayName(
      "Test 2: Player reaches upgrade condition (multi-condition - deposit + turnover + days)")
  void testPlayerReachesUpgradeConditionMultiCondition() {
    // Given: Bronze player (Level 1) meets Gold requirement (Level 3)
    // VIP Level 3 (Gold) requirements (from V015 seed):
    // - Deposit: $5,000
    // - Turnover: $25,000
    // - Active Days: 15 days

    // Player has exact requirements:
    BigDecimal totalDeposit = new BigDecimal("5000.00");
    BigDecimal totalTurnover = new BigDecimal("25000.00");
    Integer activeDays = 15;

    // When: Check eligibility for Level 3 (Gold)
    boolean eligible =
        vipLevelConfigService.isEligibleForLevel(3, totalDeposit, totalTurnover, activeDays);

    // Then: Player should be eligible for Gold
    assertThat(eligible).isTrue();
  }

  @Test
  @DisplayName("Test 3: Auto-upgrade success (Bronze → Silver)")
  void testAutoUpgradeSuccess() {
    // Given: Bronze player (Level 1) meets Silver requirements
    BigDecimal totalDeposit = new BigDecimal("2000.00");
    BigDecimal totalTurnover = new BigDecimal("6000.00");
    Integer activeDays = 10;

    // When: Check and upgrade player
    ResponseDTO<Integer> response =
        vipAutoUpgradeService.checkAndUpgradePlayer(
            testPlayer1Id, totalDeposit, totalTurnover, activeDays);

    // Then: Response should be successful
    assertThat(response.getOk()).isTrue();
    assertThat(response.getData()).isEqualTo(2); // Upgraded to Silver (Level 2)

    // Verify player VIP level updated
    PlayerEntity player = playerDao.selectById(testPlayer1Id);
    assertThat(player.getVipLevel()).isEqualTo(2);

    // Verify upgrade history created
    List<PlayerVipHistoryEntity> history =
        playerVipHistoryDao.selectList(
            new LambdaQueryWrapper<PlayerVipHistoryEntity>()
                .eq(PlayerVipHistoryEntity::getPlayerId, testPlayer1Id));
    assertThat(history).hasSize(1);
    assertThat(history.get(0).getOldVipLevel()).isEqualTo(1);
    assertThat(history.get(0).getNewVipLevel()).isEqualTo(2);
    assertThat(history.get(0).getUpgradeType()).isEqualTo("AUTO_UPGRADE");
    assertThat(history.get(0).getTotalDepositAtUpgrade()).isEqualByComparingTo(totalDeposit);
    assertThat(history.get(0).getTotalTurnoverAtUpgrade()).isEqualByComparingTo(totalTurnover);
    assertThat(history.get(0).getActiveDaysAtUpgrade()).isEqualTo(activeDays);

    // Verify level-up bonus reward created (PENDING status)
    List<VipRewardRecordEntity> rewards =
        vipRewardRecordDao.selectList(
            new LambdaQueryWrapper<VipRewardRecordEntity>()
                .eq(VipRewardRecordEntity::getPlayerId, testPlayer1Id)
                .eq(VipRewardRecordEntity::getRewardType, "LEVEL_UP_BONUS"));
    assertThat(rewards).hasSize(1);
    assertThat(rewards.get(0).getVipLevel()).isEqualTo(2);
    assertThat(rewards.get(0).getStatus()).isEqualTo("PENDING");
    assertThat(rewards.get(0).getRewardAmount())
        .isEqualByComparingTo(new BigDecimal("50.00")); // Silver bonus ($50)
  }

  @Test
  @DisplayName("Test 4: Multi-level jump upgrade (Bronze → Gold, skip Silver)")
  void testMultiLevelJumpUpgrade() {
    // Given: Bronze player (Level 1) meets Gold requirements (Level 3)
    // This should skip Silver (Level 2) entirely
    BigDecimal totalDeposit = new BigDecimal("6000.00");
    BigDecimal totalTurnover = new BigDecimal("30000.00");
    Integer activeDays = 20;

    // When: Check and upgrade player
    ResponseDTO<Integer> response =
        vipAutoUpgradeService.checkAndUpgradePlayer(
            testPlayer1Id, totalDeposit, totalTurnover, activeDays);

    // Then: Response should be successful
    assertThat(response.getOk()).isTrue();
    assertThat(response.getData()).isEqualTo(3); // Jumped to Gold (Level 3), skipped Silver

    // Verify player VIP level updated to Gold
    PlayerEntity player = playerDao.selectById(testPlayer1Id);
    assertThat(player.getVipLevel()).isEqualTo(3);

    // Verify upgrade history shows jump from 1 → 3
    List<PlayerVipHistoryEntity> history =
        playerVipHistoryDao.selectList(
            new LambdaQueryWrapper<PlayerVipHistoryEntity>()
                .eq(PlayerVipHistoryEntity::getPlayerId, testPlayer1Id));
    assertThat(history).hasSize(1);
    assertThat(history.get(0).getOldVipLevel()).isEqualTo(1); // Bronze
    assertThat(history.get(0).getNewVipLevel()).isEqualTo(3); // Gold
    assertThat(history.get(0).getUpgradeType()).isEqualTo("AUTO_UPGRADE");

    // Verify level-up bonus for Gold (not Silver)
    List<VipRewardRecordEntity> rewards =
        vipRewardRecordDao.selectList(
            new LambdaQueryWrapper<VipRewardRecordEntity>()
                .eq(VipRewardRecordEntity::getPlayerId, testPlayer1Id)
                .eq(VipRewardRecordEntity::getRewardType, "LEVEL_UP_BONUS"));
    assertThat(rewards).hasSize(1);
    assertThat(rewards.get(0).getVipLevel()).isEqualTo(3); // Gold bonus
    assertThat(rewards.get(0).getRewardAmount())
        .isEqualByComparingTo(new BigDecimal("100.00")); // Gold bonus ($100)
  }

  @Test
  @DisplayName("Test 5: Manual upgrade (admin force upgrade)")
  void testManualUpgrade() {
    // Given: Bronze player (Level 1) to be manually upgraded to Platinum (Level 4)
    // Admin override for special promotion or customer service escalation
    String reason = "Special VIP promotion - Customer appreciation month";

    // When: Manual upgrade by admin
    ResponseDTO<Void> response =
        vipAutoUpgradeService.manualUpgrade(testPlayer1Id, 4, reason, testAdminId);

    // Then: Response should be successful
    assertThat(response.getOk()).isTrue();

    // Verify player VIP level updated to Platinum
    PlayerEntity player = playerDao.selectById(testPlayer1Id);
    assertThat(player.getVipLevel()).isEqualTo(4);

    // Verify upgrade history created with MANUAL_UPGRADE type
    List<PlayerVipHistoryEntity> history =
        playerVipHistoryDao.selectList(
            new LambdaQueryWrapper<PlayerVipHistoryEntity>()
                .eq(PlayerVipHistoryEntity::getPlayerId, testPlayer1Id));
    assertThat(history).hasSize(1);
    assertThat(history.get(0).getOldVipLevel()).isEqualTo(1); // Bronze
    assertThat(history.get(0).getNewVipLevel()).isEqualTo(4); // Platinum
    assertThat(history.get(0).getUpgradeType()).isEqualTo("MANUAL_UPGRADE");
    assertThat(history.get(0).getUpgradeReason()).isEqualTo(reason);
    assertThat(history.get(0).getUpgradedBy()).isEqualTo(testAdminId);

    // Verify level-up bonus for Platinum
    List<VipRewardRecordEntity> rewards =
        vipRewardRecordDao.selectList(
            new LambdaQueryWrapper<VipRewardRecordEntity>()
                .eq(VipRewardRecordEntity::getPlayerId, testPlayer1Id)
                .eq(VipRewardRecordEntity::getRewardType, "LEVEL_UP_BONUS"));
    assertThat(rewards).hasSize(1);
    assertThat(rewards.get(0).getVipLevel()).isEqualTo(4); // Platinum bonus
    assertThat(rewards.get(0).getRewardAmount())
        .isEqualByComparingTo(new BigDecimal("300.00")); // Platinum bonus ($300)
  }

  @Test
  @DisplayName("Test 6: Upgrade reward distribution (LEVEL_UP_BONUS PENDING status)")
  void testUpgradeRewardDistribution() {
    // Given: Player upgraded to Silver
    BigDecimal totalDeposit = new BigDecimal("2000.00");
    BigDecimal totalTurnover = new BigDecimal("6000.00");
    Integer activeDays = 10;
    vipAutoUpgradeService.checkAndUpgradePlayer(
        testPlayer1Id, totalDeposit, totalTurnover, activeDays);

    // When: Query reward record
    List<VipRewardRecordEntity> rewards =
        vipRewardRecordDao.selectList(
            new LambdaQueryWrapper<VipRewardRecordEntity>()
                .eq(VipRewardRecordEntity::getPlayerId, testPlayer1Id)
                .eq(VipRewardRecordEntity::getRewardType, "LEVEL_UP_BONUS"));

    // Then: Reward should be in PENDING status
    assertThat(rewards).hasSize(1);
    VipRewardRecordEntity reward = rewards.get(0);
    assertThat(reward.getStatus()).isEqualTo("PENDING");
    assertThat(reward.getIssuedAt()).isNull(); // Not issued yet
    assertThat(reward.getExpiresAt()).isNotNull(); // Has expiration (30 days)
    assertThat(reward.getIssuedBy()).isNull(); // Auto-issued

    // Then: Mark reward as issued (simulate wallet service crediting)
    ResponseDTO<Void> markResponse =
        vipRewardDistributionService.markRewardAsIssued(reward.getRewardId());
    assertThat(markResponse.getOk()).isTrue();

    // Verify reward status changed to ISSUED
    VipRewardRecordEntity updatedReward = vipRewardRecordDao.selectById(reward.getRewardId());
    assertThat(updatedReward.getStatus()).isEqualTo("ISSUED");
    assertThat(updatedReward.getIssuedAt()).isNotNull(); // Now has issue timestamp
  }

  @Test
  @DisplayName("Test 7: Upgrade history verification")
  void testUpgradeHistoryVerification() {
    // Given: Player upgraded from Bronze to Silver
    BigDecimal totalDeposit = new BigDecimal("2000.00");
    BigDecimal totalTurnover = new BigDecimal("6000.00");
    Integer activeDays = 10;
    vipAutoUpgradeService.checkAndUpgradePlayer(
        testPlayer1Id, totalDeposit, totalTurnover, activeDays);

    // When: Query upgrade history
    List<PlayerVipHistoryEntity> history =
        playerVipHistoryDao.selectList(
            new LambdaQueryWrapper<PlayerVipHistoryEntity>()
                .eq(PlayerVipHistoryEntity::getPlayerId, testPlayer1Id)
                .orderByDesc(PlayerVipHistoryEntity::getUpgradedAt));

    // Then: History should contain complete snapshot
    assertThat(history).hasSize(1);
    PlayerVipHistoryEntity record = history.get(0);
    assertThat(record.getOldVipLevel()).isEqualTo(1); // Bronze
    assertThat(record.getNewVipLevel()).isEqualTo(2); // Silver
    assertThat(record.getUpgradeType()).isEqualTo("AUTO_UPGRADE");
    assertThat(record.getTotalDepositAtUpgrade()).isEqualByComparingTo(totalDeposit);
    assertThat(record.getTotalTurnoverAtUpgrade()).isEqualByComparingTo(totalTurnover);
    assertThat(record.getActiveDaysAtUpgrade()).isEqualTo(activeDays);
    assertThat(record.getUpgradedBy()).isNull(); // Auto-upgrade (NULL upgradedBy)
    assertThat(record.getUpgradedAt()).isNotNull();
    assertThat(record.getUpgradeReason()).contains("Auto upgraded from VIP 1 to VIP 2");
  }

  @Test
  @DisplayName("Test 8: Ineligible player does not upgrade")
  void testIneligiblePlayerDoesNotUpgrade() {
    // Given: Bronze player with insufficient stats
    // Silver requirements: $1,000 deposit, $5,000 turnover, 7 days
    BigDecimal totalDeposit = new BigDecimal("500.00"); // Below requirement
    BigDecimal totalTurnover = new BigDecimal("2000.00"); // Below requirement
    Integer activeDays = 3; // Below requirement

    // When: Check and attempt upgrade
    ResponseDTO<Integer> response =
        vipAutoUpgradeService.checkAndUpgradePlayer(
            testPlayer1Id, totalDeposit, totalTurnover, activeDays);

    // Then: Response should indicate no upgrade
    assertThat(response.getOk()).isTrue();
    assertThat(response.getData()).isEqualTo(1); // Still Bronze (Level 1)

    // Verify player VIP level unchanged
    PlayerEntity player = playerDao.selectById(testPlayer1Id);
    assertThat(player.getVipLevel()).isEqualTo(1); // Still Bronze

    // Verify no upgrade history created
    List<PlayerVipHistoryEntity> history =
        playerVipHistoryDao.selectList(
            new LambdaQueryWrapper<PlayerVipHistoryEntity>()
                .eq(PlayerVipHistoryEntity::getPlayerId, testPlayer1Id));
    assertThat(history).isEmpty();

    // Verify no reward created
    List<VipRewardRecordEntity> rewards =
        vipRewardRecordDao.selectList(
            new LambdaQueryWrapper<VipRewardRecordEntity>()
                .eq(VipRewardRecordEntity::getPlayerId, testPlayer1Id));
    assertThat(rewards).isEmpty();
  }

  @Test
  @DisplayName("Test 9: Max level player does not upgrade")
  void testMaxLevelPlayerDoesNotUpgrade() {
    // Given: Supreme player (Level 10, max level) with excessive stats
    BigDecimal totalDeposit = new BigDecimal("10000000.00"); // $10M
    BigDecimal totalTurnover = new BigDecimal("50000000.00"); // $50M
    Integer activeDays = 365;

    // When: Check and attempt upgrade
    ResponseDTO<Integer> response =
        vipAutoUpgradeService.checkAndUpgradePlayer(
            testPlayer3Id, totalDeposit, totalTurnover, activeDays);

    // Then: Response should indicate no upgrade (already max level)
    assertThat(response.getOk()).isTrue();
    assertThat(response.getData()).isEqualTo(10); // Still Supreme (Level 10)

    // Verify player VIP level unchanged
    PlayerEntity player = playerDao.selectById(testPlayer3Id);
    assertThat(player.getVipLevel()).isEqualTo(10); // Still Supreme

    // Verify no upgrade history created
    List<PlayerVipHistoryEntity> history =
        playerVipHistoryDao.selectList(
            new LambdaQueryWrapper<PlayerVipHistoryEntity>()
                .eq(PlayerVipHistoryEntity::getPlayerId, testPlayer3Id));
    assertThat(history).isEmpty();
  }

  @Test
  @DisplayName("Test 10: VIP level config validity period check")
  void testVipLevelConfigValidityPeriod() {
    // Given: Query all active VIP levels
    List<VipLevelConfigEntity> activeConfigs = vipLevelConfigService.getAllActiveVipLevels();

    // Then: Should return all 10 VIP levels (from V015 seed data)
    assertThat(activeConfigs).hasSize(10);

    // Verify validity period checks
    // All configs in V015 have effectiveFrom = '2026-01-01' and effectiveTo = NULL
    // Today's date should be within validity period
    LocalDate today = LocalDate.now(ZoneId.systemDefault());
    for (VipLevelConfigEntity config : activeConfigs) {
      assertThat(config.getEffectiveFrom()).isBeforeOrEqualTo(today);
      // effectiveTo is NULL (permanent), so should be valid
      assertThat(config.getEffectiveTo()).isNull();
      assertThat(config.getStatus()).isEqualTo(1); // ENABLED
    }

    // Verify levels are in ascending order
    for (int i = 0; i < activeConfigs.size(); i++) {
      assertThat(activeConfigs.get(i).getVipLevel()).isEqualTo(i + 1);
    }
  }
}
