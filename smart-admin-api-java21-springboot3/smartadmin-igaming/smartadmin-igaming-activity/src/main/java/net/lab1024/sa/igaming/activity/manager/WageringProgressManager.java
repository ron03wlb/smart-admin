package net.lab1024.sa.igaming.activity.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wagering progress manager — updates wagering progress for active bonuses.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WageringProgressManager {

  private static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final WalletBonusExtDao walletBonusExtDao;
  private final GameDao gameDao;
  private final GameWeightConfigDao gameWeightConfigDao;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Update wagering progress for all active bonuses of a player.
   *
   * @param playerId player ID
   * @param gameCode game code
   * @param betAmount bet amount
   * @param tenantId tenant ID
   * @return number of records updated
   */
  @Transactional(rollbackFor = Throwable.class)
  public int updateWageringProgress(
      Long playerId, String gameCode, BigDecimal betAmount, Long tenantId) {

    List<PlayerBonusRecordEntity> activeRecords =
        playerBonusRecordDao.selectActiveByPlayerId(playerId);
    if (activeRecords.isEmpty()) {
      return 0;
    }

    // Calculate effective bet with game weight
    BigDecimal effectiveBet = calculateEffectiveBet(tenantId, gameCode, betAmount);

    int updated = 0;
    for (PlayerBonusRecordEntity record : activeRecords) {
      BigDecimal newCompleted = record.getWageringCompleted().add(effectiveBet);
      record.setWageringCompleted(newCompleted);

      // Update WalletBonusExt wageredAmount
      if (record.getWalletBonusExtId() != null) {
        WalletBonusExtEntity ext = walletBonusExtDao.selectById(record.getWalletBonusExtId());
        if (ext != null) {
          ext.setWageredAmount(ext.getWageredAmount().add(effectiveBet));
          walletBonusExtDao.updateById(ext);
        }
      }

      // Check if wagering requirement is met
      if (newCompleted.compareTo(record.getWageringRequired()) >= 0) {
        completeBonus(record);
      } else {
        playerBonusRecordDao.updateById(record);
      }
      updated++;
    }

    return updated;
  }

  /**
   * Mark a bonus record as completed.
   *
   * @param record bonus record
   */
  @Transactional(rollbackFor = Throwable.class)
  public void completeBonus(PlayerBonusRecordEntity record) {
    record.setStatus(BonusRecordStatusEnum.COMPLETED.getValue());
    record.setCompletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    playerBonusRecordDao.updateById(record);

    if (record.getWalletBonusExtId() != null) {
      WalletBonusExtEntity ext = walletBonusExtDao.selectById(record.getWalletBonusExtId());
      if (ext != null) {
        ext.setStatus(BonusStatusEnum.COMPLETED.getValue());
        walletBonusExtDao.updateById(ext);
      }
    }

    log.info(
        "Bonus completed: recordId={}, playerId={}", record.getRecordId(), record.getPlayerId());

    publishWageringCompleted(record);
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishWageringCompleted(PlayerBonusRecordEntity record) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("bonusExtId", record.getWalletBonusExtId());
    payload.put("playerId", record.getPlayerId());
    payload.put("recordId", record.getRecordId());
    domainEventPublisher.publish(
        IgamingKafkaConst.Topic.ACTIVITY_EVENTS,
        DomainEvent.builder()
            .eventType("WAGERING_COMPLETED")
            .tenantId(record.getTenantId())
            .aggregateType("BONUS_RECORD")
            .aggregateId(String.valueOf(record.getRecordId()))
            .payload(payload)
            .build());
  }

  private BigDecimal calculateEffectiveBet(Long tenantId, String gameCode, BigDecimal betAmount) {
    GameEntity game =
        gameDao.selectOne(
            Wrappers.<GameEntity>lambdaQuery()
                .eq(GameEntity::getGameCode, gameCode)
                .eq(GameEntity::getTenantId, tenantId)
                .eq(GameEntity::getDeleted, false));
    if (game == null) {
      return betAmount.multiply(DEFAULT_WEIGHT);
    }
    BigDecimal weight = gameWeightConfigDao.selectWeight(tenantId, game.getCategory());
    return betAmount.multiply(weight != null ? weight : DEFAULT_WEIGHT);
  }
}
