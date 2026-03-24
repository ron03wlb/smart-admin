package net.lab1024.sa.igaming.integration.bonus;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.integration.bonus.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.stereotype.Service;

/**
 * Bonus conversion integration service — orchestrates bonus unlocking and conversion flow.
 *
 * <p>This service provides high-level APIs for bonus conversion operations, integrating the
 * Activity module (wagering progress) and Wallet module (bonus to cash conversion). It adds extra
 * business logic such as validation, notification, and audit on top of the existing domain
 * services.
 *
 * <p><b>Architecture Note:</b> The actual bonus conversion is triggered automatically via Kafka
 * event flow:
 *
 * <ol>
 *   <li>Game bet settled → TurnoverIntegrationService calculates turnover
 *   <li>WageringProgressManager updates wagering progress
 *   <li>If wagering completed → WageringProgressManager publishes WAGERING_COMPLETED event
 *   <li>BonusCompletionConsumer (Wallet module) consumes event → calls
 *       WalletService.processBonusCompletion()
 *   <li>WalletManager.convertBonusToCash() transfers BONUS → CASH
 * </ol>
 *
 * <p>This integration service provides:
 *
 * <ul>
 *   <li>Query wagering progress API (for frontend display)
 *   <li>Manual bonus conversion API (for compensation/correction scenarios)
 *   <li>Bonus conversion status notification (Kafka event publishing)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BonusConversionIntegrationService {

  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final PromotionRuleDao promotionRuleDao;
  private final WalletService walletService;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Query wagering progress for a player's active or completed bonuses.
   *
   * <p>This method retrieves all active and completed bonuses for a player and calculates the
   * wagering progress for each one. It is typically called by the frontend to display wagering
   * progress to the player.
   *
   * <p><b>Why Include COMPLETED Status?</b> When wagering reaches 100%, the bonus status changes
   * from ACTIVE to COMPLETED. However, the actual bonus-to-cash conversion may happen
   * asynchronously (via Kafka event). During this brief window, players should still see the
   * completed bonus in their wagering progress list (showing 100% progress, ready for conversion).
   *
   * @param playerId player ID
   * @return list of wagering progress VOs
   */
  public ResponseDTO<List<WageringProgressVO>> getWageringProgress(Long playerId) {
    List<PlayerBonusRecordEntity> activeRecords =
        playerBonusRecordDao.selectList(
            Wrappers.<PlayerBonusRecordEntity>lambdaQuery()
                .eq(PlayerBonusRecordEntity::getPlayerId, playerId)
                .in(
                    PlayerBonusRecordEntity::getStatus,
                    BonusRecordStatusEnum.ACTIVE.getValue(),
                    BonusRecordStatusEnum.COMPLETED.getValue())
                .orderByDesc(PlayerBonusRecordEntity::getCreateTime));

    List<WageringProgressVO> progressList =
        activeRecords.stream().map(this::toWageringProgressVO).collect(Collectors.toList());

    return ResponseDTO.ok(progressList);
  }

  /**
   * Query wagering progress for a specific bonus record.
   *
   * <p>This method retrieves a single bonus record and returns its wagering progress. It can be
   * used to check if a specific bonus is ready for conversion.
   *
   * @param recordId bonus record ID
   * @return wagering progress VO, or empty Option if not found
   */
  public Option<WageringProgressVO> getWageringProgressByRecordId(Long recordId) {
    PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
    if (record == null || !BonusRecordStatusEnum.ACTIVE.getValue().equals(record.getStatus())) {
      return Option.none();
    }
    return Option.some(toWageringProgressVO(record));
  }

  /**
   * Manually trigger bonus conversion (compensation/correction scenario).
   *
   * <p>This method is typically used by customer support or administrators to manually convert a
   * bonus to cash in exceptional scenarios (e.g., wagering completed but conversion didn't trigger
   * automatically due to system error).
   *
   * <p><b>Prerequisites:</b>
   *
   * <ul>
   *   <li>Bonus record must be in ACTIVE or COMPLETED status
   *   <li>Wagering requirement must be fully met (wageringCompleted >= wageringRequired)
   *   <li>Bonus must not have been converted already
   * </ul>
   *
   * @param recordId bonus record ID
   * @param operatorId operator ID (for audit trail)
   * @return success or error response
   */
  public ResponseDTO<Void> manualConvertBonus(Long recordId, Long operatorId) {
    PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
    if (record == null) {
      return ResponseDTO.userErrorParam("Bonus record not found: " + recordId);
    }

    // Validate bonus status (must be ACTIVE or COMPLETED)
    if (!BonusRecordStatusEnum.ACTIVE.getValue().equals(record.getStatus())
        && !BonusRecordStatusEnum.COMPLETED.getValue().equals(record.getStatus())) {
      return ResponseDTO.userErrorParam(
          "Bonus record is not in ACTIVE or COMPLETED status: status="
              + record.getStatus()
              + ", recordId="
              + recordId);
    }

    // Validate wagering requirement met
    if (record.getWageringCompleted().compareTo(record.getWageringRequired()) < 0) {
      return ResponseDTO.userErrorParam(
          "Wagering requirement not met: completed="
              + record.getWageringCompleted()
              + ", required="
              + record.getWageringRequired()
              + ", recordId="
              + recordId);
    }

    // Call WalletService to perform conversion
    try {
      walletService.processBonusCompletion(record.getWalletBonusExtId(), record.getPlayerId());

      log.info(
          "Manual bonus conversion triggered: recordId={}, playerId={}, operatorId={}",
          recordId,
          record.getPlayerId(),
          operatorId);

      // Publish bonus conversion notification event
      publishBonusConvertedEvent(record, operatorId);

      return ResponseDTO.ok();
    } catch (Exception e) {
      log.error(
          "Manual bonus conversion failed: recordId={}, playerId={}, error={}",
          recordId,
          record.getPlayerId(),
          e.getMessage(),
          e);
      return ResponseDTO.error(
          net.lab1024.sa.common.core.domain.code.SystemErrorCode.SYSTEM_ERROR,
          "Bonus conversion failed: " + e.getMessage());
    }
  }

  /**
   * Publish bonus converted event to Kafka.
   *
   * <p>This event notifies downstream systems (e.g., notification service, analytics) that a bonus
   * has been successfully converted to cash.
   *
   * @param record bonus record entity
   * @param operatorId operator ID (null for automatic conversion, non-null for manual conversion)
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishBonusConvertedEvent(PlayerBonusRecordEntity record, Long operatorId) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("recordId", record.getRecordId());
    payload.put("playerId", record.getPlayerId());
    payload.put("bonusAmount", record.getBonusAmount().toPlainString());
    payload.put("wageringRequired", record.getWageringRequired().toPlainString());
    payload.put("wageringCompleted", record.getWageringCompleted().toPlainString());

    // Query promotionCode from PromotionRuleEntity via ruleId
    PromotionRuleEntity promotionRule = promotionRuleDao.selectById(record.getRuleId());
    String promotionCode = promotionRule != null ? promotionRule.getPromotionCode() : null;
    payload.put("promotionCode", promotionCode);

    payload.put("isManualConversion", operatorId != null);
    if (operatorId != null) {
      payload.put("operatorId", operatorId);
    }

    DomainEvent event =
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.BONUS_CONVERTED)
            .tenantId(record.getTenantId())
            .aggregateType("PLAYER_BONUS")
            .aggregateId(String.valueOf(record.getRecordId()))
            .payload(payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.ACTIVITY_EVENTS, event);

    log.debug(
        "Published BONUS_CONVERTED event: recordId={}, playerId={}, isManual={}",
        record.getRecordId(),
        record.getPlayerId(),
        operatorId != null);
  }

  /**
   * Convert PlayerBonusRecordEntity to WageringProgressVO.
   *
   * @param record bonus record entity
   * @return wagering progress VO
   */
  private WageringProgressVO toWageringProgressVO(PlayerBonusRecordEntity record) {
    WageringProgressVO vo = new WageringProgressVO();
    vo.setRecordId(record.getRecordId());
    vo.setPlayerId(record.getPlayerId());
    vo.setBonusAmount(record.getBonusAmount());
    vo.setWageringRequired(record.getWageringRequired());
    vo.setWageringCompleted(record.getWageringCompleted());

    BigDecimal remaining = record.getWageringRequired().subtract(record.getWageringCompleted());
    vo.setWageringRemaining(remaining.max(BigDecimal.ZERO));

    BigDecimal progressPct =
        record.getWageringRequired().compareTo(BigDecimal.ZERO) > 0
            ? record
                .getWageringCompleted()
                .multiply(new BigDecimal("100"))
                .divide(record.getWageringRequired(), 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;
    vo.setProgressPercentage(progressPct.min(new BigDecimal("100")));

    vo.setStatus(record.getStatus());
    vo.setExpiresAt(record.getExpiredAt());

    // Query promotionCode from PromotionRuleEntity via ruleId
    PromotionRuleEntity promotionRule = promotionRuleDao.selectById(record.getRuleId());
    String promotionCode = promotionRule != null ? promotionRule.getPromotionCode() : null;
    vo.setPromotionCode(promotionCode);

    boolean isReady = record.getWageringCompleted().compareTo(record.getWageringRequired()) >= 0;
    vo.setIsReadyForConversion(isReady);

    return vo;
  }
}
