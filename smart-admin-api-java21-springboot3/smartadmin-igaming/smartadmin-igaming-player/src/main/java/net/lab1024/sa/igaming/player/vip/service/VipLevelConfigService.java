package net.lab1024.sa.igaming.player.vip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.vip.dao.VipLevelConfigDao;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipLevelConfigEntity;
import org.springframework.stereotype.Service;

/**
 * VIP Level Configuration Service.
 *
 * <p>Manages VIP level configurations including:
 *
 * <ul>
 *   <li>Query active VIP level configs
 *   <li>Check upgrade eligibility based on player stats
 *   <li>Calculate next VIP level for a player
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VipLevelConfigService {

  private final VipLevelConfigDao vipLevelConfigDao;

  /**
   * Get all active VIP level configurations.
   *
   * @return List of active VIP configs sorted by vip_level ASC
   */
  public List<VipLevelConfigEntity> getAllActiveVipLevels() {
    LambdaQueryWrapper<VipLevelConfigEntity> wrapper =
        new LambdaQueryWrapper<VipLevelConfigEntity>()
            .eq(VipLevelConfigEntity::getStatus, 1) // ACTIVE
            .and(
                w ->
                    w.isNull(VipLevelConfigEntity::getEffectiveTo)
                        .or()
                        .ge(VipLevelConfigEntity::getEffectiveTo, LocalDate.now()))
            .orderByAsc(VipLevelConfigEntity::getVipLevel);

    return vipLevelConfigDao.selectList(wrapper);
  }

  /**
   * Get VIP level configuration by level number.
   *
   * @param vipLevel VIP level number (1-10)
   * @return Option containing VIP config if found
   */
  public Option<VipLevelConfigEntity> getVipLevelConfig(Integer vipLevel) {
    LambdaQueryWrapper<VipLevelConfigEntity> wrapper =
        new LambdaQueryWrapper<VipLevelConfigEntity>()
            .eq(VipLevelConfigEntity::getVipLevel, vipLevel)
            .eq(VipLevelConfigEntity::getStatus, 1) // ACTIVE
            .and(
                w ->
                    w.isNull(VipLevelConfigEntity::getEffectiveTo)
                        .or()
                        .ge(VipLevelConfigEntity::getEffectiveTo, LocalDate.now()))
            .orderByDesc(VipLevelConfigEntity::getEffectiveFrom)
            .last("LIMIT 1");

    return Option.of(vipLevelConfigDao.selectOne(wrapper));
  }

  /**
   * Calculate next eligible VIP level for a player.
   *
   * <p>Checks all VIP levels higher than current level to find the highest eligible level based on
   * player stats.
   *
   * @param currentVipLevel Player's current VIP level
   * @param totalDeposit Player's cumulative total deposit
   * @param totalTurnover Player's cumulative valid turnover
   * @param activeDays Player's active days count
   * @return Option containing next eligible VIP level (returns None if no upgrade available)
   */
  public Option<VipLevelConfigEntity> calculateNextEligibleLevel(
      Integer currentVipLevel,
      BigDecimal totalDeposit,
      BigDecimal totalTurnover,
      Integer activeDays) {

    // Get all VIP levels higher than current level
    List<VipLevelConfigEntity> higherLevels = getAllActiveVipLevels();
    higherLevels.removeIf(config -> config.getVipLevel() <= currentVipLevel);

    // Find the highest eligible level
    Option<VipLevelConfigEntity> highestEligible = Option.none();

    for (VipLevelConfigEntity config : higherLevels) {
      boolean eligible =
          totalDeposit.compareTo(config.getUpgradeDepositRequirement()) >= 0
              && totalTurnover.compareTo(config.getUpgradeTurnoverRequirement()) >= 0
              && activeDays >= config.getUpgradeActiveDaysRequirement();

      if (eligible) {
        // Keep track of the highest eligible level
        highestEligible =
            Option.of(
                highestEligible
                    .map(current -> config.getVipLevel() > current.getVipLevel() ? config : current)
                    .getOrElse(config));
      }
    }

    return highestEligible;
  }

  /**
   * Check if player is eligible for specific VIP level.
   *
   * @param vipLevel Target VIP level
   * @param totalDeposit Player's cumulative total deposit
   * @param totalTurnover Player's cumulative valid turnover
   * @param activeDays Player's active days count
   * @return true if player meets all requirements
   */
  public boolean isEligibleForLevel(
      Integer vipLevel, BigDecimal totalDeposit, BigDecimal totalTurnover, Integer activeDays) {

    return getVipLevelConfig(vipLevel)
        .map(
            config ->
                totalDeposit.compareTo(config.getUpgradeDepositRequirement()) >= 0
                    && totalTurnover.compareTo(config.getUpgradeTurnoverRequirement()) >= 0
                    && activeDays >= config.getUpgradeActiveDaysRequirement())
        .getOrElse(false);
  }

  /**
   * Get VIP level configuration with benefits details.
   *
   * @param vipLevel VIP level number
   * @return ResponseDTO with VIP config
   */
  public ResponseDTO<VipLevelConfigEntity> getVipLevelDetails(Integer vipLevel) {
    return getVipLevelConfig(vipLevel)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("VIP 等級配置不存在"));
  }
}
