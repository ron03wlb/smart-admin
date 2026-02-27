package net.lab1024.sa.igaming.player.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.dao.VipChangeLogDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.entity.VipChangeLogEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * VIP Level Manager — handles VIP level transitions with change logging.
 *
 * <p>All public methods MUST be annotated with {@code @Transactional(rollbackFor =
 * Throwable.class)} per SmartAdmin architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class VipLevelManager {

  private final PlayerDao playerDao;
  private final VipChangeLogDao vipChangeLogDao;

  /**
   * Update player VIP level with change logging.
   *
   * @param player player entity
   * @param newLevel new VIP level
   * @param reason change reason
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateVipLevel(PlayerEntity player, VipLevelEnum newLevel, String reason) {
    Integer oldLevel = player.getVipLevel();
    player.setVipLevel(newLevel.getValue());
    playerDao.updateById(player);

    VipChangeLogEntity logEntity = new VipChangeLogEntity();
    logEntity.setPlayerId(player.getPlayerId());
    logEntity.setOldLevel(oldLevel);
    logEntity.setNewLevel(newLevel.getValue());
    logEntity.setReason(reason);
    vipChangeLogDao.insert(logEntity);
  }
}
