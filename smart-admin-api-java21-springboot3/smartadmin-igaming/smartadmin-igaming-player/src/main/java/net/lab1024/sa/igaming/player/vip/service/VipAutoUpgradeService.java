package net.lab1024.sa.igaming.player.vip.service;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.vip.dao.PlayerVipHistoryDao;
import net.lab1024.sa.igaming.player.vip.domain.entity.PlayerVipHistoryEntity;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipLevelConfigEntity;
import org.springframework.stereotype.Service;

/**
 * VIP Auto-Upgrade Service.
 *
 * <p>Automatic VIP level upgrade engine that:
 *
 * <ul>
 *   <li>Checks if player is eligible for VIP upgrade based on stats
 *   <li>Performs automatic VIP level upgrade
 *   <li>Records upgrade history with player stats at upgrade time
 *   <li>Triggers reward distribution for level-up bonus
 * </ul>
 *
 * <p><b>Upgrade Logic:</b> Calculates the highest eligible VIP level based on player's cumulative
 * deposit, cumulative turnover, and active days. If player qualifies for multiple levels, upgrades
 * directly to the highest eligible level.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipAutoUpgradeService {

  private final PlayerDao playerDao;
  private final VipLevelConfigService vipLevelConfigService;
  private final PlayerVipHistoryDao playerVipHistoryDao;
  private final VipRewardDistributionService vipRewardDistributionService;

  /**
   * Check and execute VIP auto-upgrade for a player.
   *
   * <p>This method is typically called:
   *
   * <ul>
   *   <li>After player makes a deposit (triggers stat recalculation)
   *   <li>After player completes a bet (accumulates turnover)
   *   <li>By scheduled job (daily batch check for all active players)
   * </ul>
   *
   * @param playerId Player ID
   * @param totalDeposit Player's cumulative total deposit
   * @param totalTurnover Player's cumulative valid turnover
   * @param activeDays Player's active days count
   * @return ResponseDTO with upgrade result (contains new VIP level if upgraded)
   */
  public ResponseDTO<Integer> checkAndUpgradePlayer(
      Long playerId, BigDecimal totalDeposit, BigDecimal totalTurnover, Integer activeDays) {

    // Get player current VIP level
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null) {
      return ResponseDTO.userErrorParam("玩家不存在");
    }

    Integer currentVipLevel = player.getVipLevel();

    // Calculate next eligible level
    Option<VipLevelConfigEntity> nextLevelOpt =
        vipLevelConfigService.calculateNextEligibleLevel(
            currentVipLevel, totalDeposit, totalTurnover, activeDays);

    if (nextLevelOpt.isEmpty()) {
      log.debug(
          "Player {} is not eligible for VIP upgrade (current level: {})",
          playerId,
          currentVipLevel);
      return ResponseDTO.ok(currentVipLevel); // No upgrade
    }

    VipLevelConfigEntity nextLevel = nextLevelOpt.get();
    Integer newVipLevel = nextLevel.getVipLevel();

    // Perform upgrade
    player.setVipLevel(newVipLevel);
    playerDao.updateById(player);

    // Record upgrade history
    PlayerVipHistoryEntity history =
        PlayerVipHistoryEntity.builder()
            .playerId(playerId)
            .oldVipLevel(currentVipLevel)
            .newVipLevel(newVipLevel)
            .upgradeType("AUTO_UPGRADE")
            .totalDepositAtUpgrade(totalDeposit)
            .totalTurnoverAtUpgrade(totalTurnover)
            .activeDaysAtUpgrade(activeDays)
            .upgradeReason(
                String.format(
                    "Auto upgraded from VIP %d to VIP %d (Deposit: %s, Turnover: %s, Days: %d)",
                    currentVipLevel, newVipLevel, totalDeposit, totalTurnover, activeDays))
            .upgradedAt(OffsetDateTime.now(ZoneId.systemDefault()))
            .upgradedBy(null) // NULL for auto upgrades
            .build();

    playerVipHistoryDao.insert(history);

    // Issue level-up bonus reward
    vipRewardDistributionService.issueLevelUpBonus(
        playerId, newVipLevel, nextLevel.getLevelUpBonus());

    log.info(
        "Player {} auto-upgraded from VIP {} to VIP {} (Deposit: {}, Turnover: {}, Days: {})",
        playerId,
        currentVipLevel,
        newVipLevel,
        totalDeposit,
        totalTurnover,
        activeDays);

    return ResponseDTO.ok(newVipLevel);
  }

  /**
   * Manual VIP upgrade by admin.
   *
   * <p>Allows administrators to manually upgrade a player's VIP level without checking
   * requirements. Used for VIP promotions, special cases, or customer service escalations.
   *
   * @param playerId Player ID
   * @param newVipLevel Target VIP level
   * @param reason Reason for manual upgrade
   * @param adminId Admin employee ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> manualUpgrade(
      Long playerId, Integer newVipLevel, String reason, Long adminId) {

    // Validate player exists
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null) {
      return ResponseDTO.userErrorParam("玩家不存在");
    }

    Integer currentVipLevel = player.getVipLevel();

    // Validate target VIP level exists
    Option<VipLevelConfigEntity> targetLevelOpt =
        vipLevelConfigService.getVipLevelConfig(newVipLevel);
    if (targetLevelOpt.isEmpty()) {
      return ResponseDTO.userErrorParam("目標 VIP 等級配置不存在");
    }

    // Validate upgrade direction (must be higher)
    if (newVipLevel <= currentVipLevel) {
      return ResponseDTO.userErrorParam("目標 VIP 等級必須高於當前等級");
    }

    VipLevelConfigEntity targetLevel = targetLevelOpt.get();

    // Perform manual upgrade
    player.setVipLevel(newVipLevel);
    playerDao.updateById(player);

    // Record upgrade history
    PlayerVipHistoryEntity history =
        PlayerVipHistoryEntity.builder()
            .playerId(playerId)
            .oldVipLevel(currentVipLevel)
            .newVipLevel(newVipLevel)
            .upgradeType("MANUAL_UPGRADE")
            .totalDepositAtUpgrade(null) // Not applicable for manual upgrade
            .totalTurnoverAtUpgrade(null)
            .activeDaysAtUpgrade(null)
            .upgradeReason(reason)
            .upgradedAt(OffsetDateTime.now(ZoneId.systemDefault()))
            .upgradedBy(adminId)
            .build();

    playerVipHistoryDao.insert(history);

    // Issue level-up bonus reward
    vipRewardDistributionService.issueLevelUpBonus(
        playerId, newVipLevel, targetLevel.getLevelUpBonus());

    log.info(
        "Player {} manually upgraded from VIP {} to VIP {} by admin {} (Reason: {})",
        playerId,
        currentVipLevel,
        newVipLevel,
        adminId,
        reason);

    return ResponseDTO.ok();
  }
}
