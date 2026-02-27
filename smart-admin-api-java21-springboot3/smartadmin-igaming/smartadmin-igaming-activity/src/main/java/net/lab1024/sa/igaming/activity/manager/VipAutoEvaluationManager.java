package net.lab1024.sa.igaming.activity.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.VipLevelManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * VIP auto-evaluation manager — evaluates VIP level based on cumulative turnover.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VipAutoEvaluationManager {

  private static final int BATCH_SIZE = 200;
  private static final BigDecimal SILVER_THRESHOLD = new BigDecimal("1000");
  private static final BigDecimal GOLD_THRESHOLD = new BigDecimal("10000");
  private static final BigDecimal PLATINUM_THRESHOLD = new BigDecimal("50000");
  private static final BigDecimal DIAMOND_THRESHOLD = new BigDecimal("200000");

  private final GameRoundDao gameRoundDao;
  private final PlayerDao playerDao;
  private final VipLevelManager vipLevelManager;

  /**
   * Evaluate all active players and update VIP levels.
   *
   * @return number of players evaluated
   */
  public int evaluateAllActivePlayers() {
    int evaluated = 0;
    int pageNum = 1;

    while (true) {
      Page<PlayerEntity> page = new Page<>(pageNum, BATCH_SIZE);
      playerDao.selectPage(
          page,
          Wrappers.<PlayerEntity>lambdaQuery()
              .eq(PlayerEntity::getDeleted, false)
              .eq(PlayerEntity::getStatus, PlayerStatusEnum.ACTIVE.getValue()));

      List<PlayerEntity> batch = page.getRecords();
      if (batch.isEmpty()) {
        break;
      }

      for (PlayerEntity player : batch) {
        try {
          evaluateVipLevel(player.getPlayerId(), player.getTenantId());
          evaluated++;
        } catch (Exception e) {
          log.error("VIP evaluation failed for playerId={}", player.getPlayerId(), e);
        }
      }

      if (!page.hasNext()) {
        break;
      }
      pageNum++;
    }
    return evaluated;
  }

  /**
   * Evaluate and update VIP level for a player based on cumulative weighted turnover.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public void evaluateVipLevel(Long playerId, Long tenantId) {
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null || player.getDeleted()) {
      return;
    }

    BigDecimal totalTurnover = gameRoundDao.sumWeightedTurnoverByPlayer(playerId, tenantId);
    if (totalTurnover == null) {
      totalTurnover = BigDecimal.ZERO;
    }

    VipLevelEnum targetLevel = determineVipLevel(totalTurnover);
    Integer currentLevel = player.getVipLevel();

    if (!targetLevel.getValue().equals(currentLevel)) {
      vipLevelManager.updateVipLevel(
          player, targetLevel, "Auto-evaluation: turnover=" + totalTurnover);
      log.info(
          "VIP level changed: playerId={}, from={}, to={}, turnover={}",
          playerId,
          currentLevel,
          targetLevel.getValue(),
          totalTurnover);
    }
  }

  private VipLevelEnum determineVipLevel(BigDecimal totalTurnover) {
    if (totalTurnover.compareTo(DIAMOND_THRESHOLD) >= 0) {
      return VipLevelEnum.DIAMOND;
    } else if (totalTurnover.compareTo(PLATINUM_THRESHOLD) >= 0) {
      return VipLevelEnum.PLATINUM;
    } else if (totalTurnover.compareTo(GOLD_THRESHOLD) >= 0) {
      return VipLevelEnum.GOLD;
    } else if (totalTurnover.compareTo(SILVER_THRESHOLD) >= 0) {
      return VipLevelEnum.SILVER;
    } else {
      return VipLevelEnum.BRONZE;
    }
  }
}
