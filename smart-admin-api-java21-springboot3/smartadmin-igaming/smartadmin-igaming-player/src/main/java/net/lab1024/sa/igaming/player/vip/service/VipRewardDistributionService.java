package net.lab1024.sa.igaming.player.vip.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.vip.dao.VipRewardRecordDao;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipRewardRecordEntity;
import org.springframework.stereotype.Service;

/**
 * VIP Reward Distribution Service.
 *
 * <p>Manages VIP-related reward distribution including:
 *
 * <ul>
 *   <li>Level-up bonuses (issued when player upgrades VIP level)
 *   <li>Birthday bonuses (issued on player's birthday)
 *   <li>Monthly/weekly rebates (issued based on betting volume)
 * </ul>
 *
 * <p><b>Reward Flow:</b> Create PENDING reward record → Issue to wallet → Mark as ISSUED
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipRewardDistributionService {

  private final VipRewardRecordDao vipRewardRecordDao;

  /**
   * Issue level-up bonus to player.
   *
   * <p>Called automatically when player upgrades to a new VIP level. Creates a PENDING reward
   * record that will be processed by reward distribution job.
   *
   * @param playerId Player ID
   * @param newVipLevel New VIP level
   * @param bonusAmount Level-up bonus amount
   * @return ResponseDTO with reward record ID
   */
  public ResponseDTO<Long> issueLevelUpBonus(
      Long playerId, Integer newVipLevel, BigDecimal bonusAmount) {

    // Skip if no bonus amount
    if (bonusAmount == null || bonusAmount.compareTo(BigDecimal.ZERO) <= 0) {
      log.debug("No level-up bonus for VIP level {}", newVipLevel);
      return ResponseDTO.ok(null);
    }

    // Create PENDING reward record
    VipRewardRecordEntity reward =
        VipRewardRecordEntity.builder()
            .playerId(playerId)
            .vipLevel(newVipLevel)
            .rewardType("LEVEL_UP_BONUS")
            .rewardAmount(bonusAmount)
            .rewardDescription(
                String.format("VIP %d 升級獎金 - 恭喜升級至 VIP %d", newVipLevel, newVipLevel))
            .status("PENDING") // Awaiting wallet credit
            .issuedAt(null) // Will be set when credited to wallet
            .expiresAt(OffsetDateTime.now(ZoneId.systemDefault()).plusDays(30)) // 30 days expiry
            .issuedBy(null) // Auto-issued
            .build();

    vipRewardRecordDao.insert(reward);

    log.info(
        "Created level-up bonus reward for player {} (VIP {}, Amount: {})",
        playerId,
        newVipLevel,
        bonusAmount);

    return ResponseDTO.ok(reward.getRewardId());
  }

  /**
   * Issue birthday bonus to player.
   *
   * <p>Called by scheduled job on player's birthday. Creates a PENDING reward record.
   *
   * @param playerId Player ID
   * @param vipLevel Player's current VIP level
   * @param bonusAmount Birthday bonus amount
   * @return ResponseDTO with reward record ID
   */
  public ResponseDTO<Long> issueBirthdayBonus(
      Long playerId, Integer vipLevel, BigDecimal bonusAmount) {

    // Skip if no bonus amount
    if (bonusAmount == null || bonusAmount.compareTo(BigDecimal.ZERO) <= 0) {
      log.debug("No birthday bonus for VIP level {}", vipLevel);
      return ResponseDTO.ok(null);
    }

    // Create PENDING reward record
    VipRewardRecordEntity reward =
        VipRewardRecordEntity.builder()
            .playerId(playerId)
            .vipLevel(vipLevel)
            .rewardType("BIRTHDAY_BONUS")
            .rewardAmount(bonusAmount)
            .rewardDescription(String.format("VIP %d 生日禮金 - 祝您生日快樂！", vipLevel))
            .status("PENDING")
            .issuedAt(null)
            .expiresAt(OffsetDateTime.now(ZoneId.systemDefault()).plusDays(7)) // 7 days expiry
            .issuedBy(null)
            .build();

    vipRewardRecordDao.insert(reward);

    log.info(
        "Created birthday bonus reward for player {} (VIP {}, Amount: {})",
        playerId,
        vipLevel,
        bonusAmount);

    return ResponseDTO.ok(reward.getRewardId());
  }

  /**
   * Issue monthly rebate to player.
   *
   * <p>Called by scheduled job at the end of each month based on player's betting volume.
   *
   * @param playerId Player ID
   * @param vipLevel Player's current VIP level
   * @param rebateAmount Calculated rebate amount
   * @return ResponseDTO with reward record ID
   */
  public ResponseDTO<Long> issueMonthlyRebate(
      Long playerId, Integer vipLevel, BigDecimal rebateAmount) {

    // Skip if no rebate amount
    if (rebateAmount == null || rebateAmount.compareTo(BigDecimal.ZERO) <= 0) {
      log.debug("No monthly rebate for player {}", playerId);
      return ResponseDTO.ok(null);
    }

    // Create PENDING reward record
    VipRewardRecordEntity reward =
        VipRewardRecordEntity.builder()
            .playerId(playerId)
            .vipLevel(vipLevel)
            .rewardType("MONTHLY_REBATE")
            .rewardAmount(rebateAmount)
            .rewardDescription(String.format("VIP %d 月度返水", vipLevel))
            .status("PENDING")
            .issuedAt(null)
            .expiresAt(OffsetDateTime.now(ZoneId.systemDefault()).plusDays(30)) // 30 days expiry
            .issuedBy(null)
            .build();

    vipRewardRecordDao.insert(reward);

    log.info(
        "Created monthly rebate reward for player {} (VIP {}, Amount: {})",
        playerId,
        vipLevel,
        rebateAmount);

    return ResponseDTO.ok(reward.getRewardId());
  }

  /**
   * Mark reward as issued (credited to wallet).
   *
   * <p>Called after wallet service successfully credits the reward amount to player's wallet.
   *
   * @param rewardId Reward record ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> markRewardAsIssued(Long rewardId) {
    VipRewardRecordEntity reward = vipRewardRecordDao.selectById(rewardId);
    if (reward == null) {
      return ResponseDTO.userErrorParam("獎勵記錄不存在");
    }

    if (!"PENDING".equals(reward.getStatus())) {
      return ResponseDTO.userErrorParam("只能標記 PENDING 狀態的獎勵為已發放");
    }

    reward.setStatus("ISSUED");
    reward.setIssuedAt(OffsetDateTime.now(ZoneId.systemDefault()));
    vipRewardRecordDao.updateById(reward);

    log.info("Marked reward {} as ISSUED for player {}", rewardId, reward.getPlayerId());

    return ResponseDTO.ok();
  }

  /**
   * Cancel reward (admin action).
   *
   * <p>Allows administrators to cancel pending rewards before they are issued.
   *
   * @param rewardId Reward record ID
   * @param reason Cancellation reason
   * @param adminId Admin employee ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> cancelReward(Long rewardId, String reason, Long adminId) {
    VipRewardRecordEntity reward = vipRewardRecordDao.selectById(rewardId);
    if (reward == null) {
      return ResponseDTO.userErrorParam("獎勵記錄不存在");
    }

    if (!"PENDING".equals(reward.getStatus())) {
      return ResponseDTO.userErrorParam("只能取消 PENDING 狀態的獎勵");
    }

    reward.setStatus("CANCELLED");
    reward.setCancellationReason(reason);
    reward.setIssuedBy(adminId);
    vipRewardRecordDao.updateById(reward);

    log.info(
        "Cancelled reward {} for player {} by admin {} (Reason: {})",
        rewardId,
        reward.getPlayerId(),
        adminId,
        reason);

    return ResponseDTO.ok();
  }
}
