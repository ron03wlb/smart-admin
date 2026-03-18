package net.lab1024.sa.igaming.integration.turnover;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.activity.manager.WageringProgressManager;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverCalculationService;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.integration.turnover.domain.event.BetSettlementEvent;
import org.springframework.stereotype.Service;

/**
 * Turnover integration service — orchestrates turnover calculation and wagering progress update.
 *
 * <p>This service integrates the Activity module's turnover calculation engine (LiteFlow-based)
 * with the Wallet module's wagering progress tracking. It processes bet settlement events from the
 * Game module and coordinates the following flow:
 *
 * <p><b>Integration Flow:</b>
 *
 * <ol>
 *   <li>Receive bet settlement event from Kafka (ROUND_SETTLED)
 *   <li>Call TurnoverCalculationService (LiteFlow chain execution)
 *   <li>If turnover calculation succeeds → update wagering progress
 *   <li>If wagering requirement met → publish WAGERING_PROGRESS_UPDATED event
 * </ol>
 *
 * <p><b>Turnover Calculation (3-Layer Verification):</b>
 *
 * <ul>
 *   <li><b>Layer 1 (Risk Filter)</b>: Odds threshold + risk score adjustment
 *   <li><b>Layer 2 (Status Factor)</b>: Settlement status factor (WIN=100%, DRAW=0%, etc.)
 *   <li><b>Layer 3 (Game Weight)</b>: Game category weight (Slots=100%, Live Casino=15%, etc.)
 * </ul>
 *
 * <p><b>Formula:</b>
 *
 * <pre>
 * effectiveTurnoverBase = betAmount * riskFactor / 100
 * validTurnoverFinance = effectiveTurnoverBase * statusFactor / 100
 * activityValidTurnover = validTurnoverFinance * gameWeight / 100
 * </pre>
 *
 * <p><b>Dependencies:</b>
 *
 * <ul>
 *   <li>TurnoverCalculationService (Activity module, LiteFlow chain)
 *   <li>WageringProgressManager (Activity module, @Transactional)
 *   <li>DomainEventPublisher (Kafka integration)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnoverIntegrationService {

  private final TurnoverCalculationService turnoverCalculationService;
  private final WageringProgressManager wageringProgressManager;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Process bet settlement event and calculate turnover.
   *
   * <p>This method is called by {@link
   * net.lab1024.sa.igaming.integration.turnover.TurnoverEventConsumer} when a ROUND_SETTLED event
   * is received from Kafka. It orchestrates the complete turnover calculation and wagering progress
   * update flow.
   *
   * <p><b>Error Handling:</b>
   *
   * <ul>
   *   <li>If turnover calculation fails → log error but don't throw (avoid Kafka retry storm)
   *   <li>If wagering progress update fails → log error but don't throw
   *   <li>Both failures are logged for manual investigation
   * </ul>
   *
   * @param event bet settlement event from Kafka
   */
  public void processBetSettlement(BetSettlementEvent event) {
    log.info(
        "Processing bet settlement: betId={}, playerId={}, betAmount={}, settlementStatus={}",
        event.getBetId(),
        event.getPlayerId(),
        event.getBetAmount(),
        event.getSettlementStatus());

    // Step 1: Calculate turnover using LiteFlow chain
    ResponseDTO<TurnoverCalculationService.TurnoverCalculationResult> calculationResult =
        turnoverCalculationService.calculate(
            event.getBetId(),
            event.getPlayerId(),
            event.getTenantId(),
            event.getBetAmount(),
            event.getGameCategory(),
            event.getSettlementStatus(),
            event.getOddsValue(),
            event.getOddsType(),
            event.getRiskScore());

    if (!calculationResult.getOk()) {
      log.error(
          "Turnover calculation failed: betId={}, error={}",
          event.getBetId(),
          calculationResult.getMsg());
      return; // Don't throw - avoid Kafka retry storm
    }

    TurnoverCalculationService.TurnoverCalculationResult result = calculationResult.getData();

    log.info(
        "Turnover calculated: betId={}, effectiveTurnoverBase={}, validTurnoverFinance={}, activityValidTurnover={}, rejected={}",
        event.getBetId(),
        result.getEffectiveTurnoverBase(),
        result.getValidTurnoverFinance(),
        result.getActivityValidTurnover(),
        result.getRejected());

    // Step 2: Update wagering progress if turnover is valid (not rejected)
    if (result.getRejected() != null && result.getRejected()) {
      log.warn(
          "Bet rejected by turnover engine: betId={}, rejectedBy={}, skip wagering progress update",
          event.getBetId(),
          result.getRejectedBy());
      return;
    }

    BigDecimal activityValidTurnover = result.getActivityValidTurnover();
    if (activityValidTurnover == null || activityValidTurnover.compareTo(BigDecimal.ZERO) <= 0) {
      log.debug(
          "No valid turnover for bet: betId={}, activityValidTurnover={}, skip wagering progress update",
          event.getBetId(),
          activityValidTurnover);
      return;
    }

    // Step 3: Update wagering progress for all active bonuses
    try {
      int updatedRecords =
          wageringProgressManager.updateWageringProgress(
              event.getPlayerId(), event.getGameCode(), activityValidTurnover, event.getTenantId());

      log.info(
          "Wagering progress updated: betId={}, playerId={}, gameCode={}, activityValidTurnover={}, updatedRecords={}",
          event.getBetId(),
          event.getPlayerId(),
          event.getGameCode(),
          activityValidTurnover,
          updatedRecords);

      // Step 4: Publish wagering progress updated event (if any bonuses were updated)
      if (updatedRecords > 0) {
        publishWageringProgressUpdated(event, activityValidTurnover, updatedRecords);
      }
    } catch (Exception e) {
      log.error(
          "Wagering progress update failed: betId={}, playerId={}, error={}",
          event.getBetId(),
          event.getPlayerId(),
          e.getMessage(),
          e);
      // Don't throw - avoid Kafka retry storm
    }
  }

  /**
   * Publish wagering progress updated event to Kafka.
   *
   * <p>This event notifies downstream systems (e.g., wallet, bonus conversion) that wagering
   * progress has been updated. Subscribers can check if any bonuses are now completed.
   *
   * @param event original bet settlement event
   * @param activityValidTurnover calculated activity valid turnover
   * @param updatedRecords number of bonus records updated
   */
  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishWageringProgressUpdated(
      BetSettlementEvent event, BigDecimal activityValidTurnover, int updatedRecords) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", event.getPlayerId());
    payload.put("gameCode", event.getGameCode());
    payload.put("betId", event.getBetId());
    payload.put("betAmount", event.getBetAmount().toPlainString());
    payload.put("activityValidTurnover", activityValidTurnover.toPlainString());
    payload.put("updatedRecords", updatedRecords);

    DomainEvent domainEvent =
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.WAGERING_PROGRESS_UPDATED)
            .tenantId(event.getTenantId())
            .aggregateType("PLAYER_BONUS")
            .aggregateId(String.valueOf(event.getPlayerId()))
            .payload(payload)
            .build();

    domainEventPublisher.publish(IgamingKafkaConst.Topic.ACTIVITY_EVENTS, domainEvent);

    log.debug(
        "Published WAGERING_PROGRESS_UPDATED event: playerId={}, betId={}, activityValidTurnover={}",
        event.getPlayerId(),
        event.getBetId(),
        activityValidTurnover);
  }
}
