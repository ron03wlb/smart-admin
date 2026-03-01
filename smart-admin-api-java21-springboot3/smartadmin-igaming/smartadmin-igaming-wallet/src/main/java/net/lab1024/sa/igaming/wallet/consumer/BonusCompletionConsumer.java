package net.lab1024.sa.igaming.wallet.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Bonus completion consumer — triggers BONUS to CASH conversion when wagering requirement is met.
 *
 * <p>Listens to {@code igaming.activity.events} for WAGERING_COMPLETED events and calls {@link
 * WalletService#processBonusCompletion} to transfer bonus balance to the player's CASH wallet.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BonusCompletionConsumer {

  private final WalletService walletService;

  @KafkaListener(
      topics = IgamingKafkaConst.Topic.ACTIVITY_EVENTS,
      groupId = IgamingKafkaConst.Group.WALLET)
  public void onActivityEvent(String message) {
    DomainEvent event;
    try {
      event = JsonUtil.fromJson(message, DomainEvent.class);
    } catch (Exception e) {
      log.error("Failed to deserialize activity event: {}", message, e);
      return;
    }

    if (event == null || !DomainEventTypeConst.WAGERING_COMPLETED.equals(event.getEventType())) {
      return;
    }

    if (event.getTenantId() == null) {
      log.warn(
          "Event missing tenantId, skipping: eventId={}, eventType={}",
          event.getEventId(),
          event.getEventType());
      return;
    }

    try {
      TenantContext.setTenantId(event.getTenantId());
      processWageringCompleted(event);
    } catch (Exception e) {
      log.error(
          "Failed to process WAGERING_COMPLETED event: eventId={}, error={}",
          event.getEventId(),
          e.getMessage(),
          e);
    } finally {
      TenantContext.clear();
    }
  }

  private void processWageringCompleted(DomainEvent event) {
    JsonNode payload = event.getPayload();
    if (payload == null) {
      log.warn("WAGERING_COMPLETED event has no payload: eventId={}", event.getEventId());
      return;
    }

    Long bonusExtId = payload.path("bonusExtId").asLong(0);
    Long playerId = payload.path("playerId").asLong(0);
    if (bonusExtId == 0 || playerId == 0) {
      log.warn("WAGERING_COMPLETED event missing required fields: eventId={}", event.getEventId());
      return;
    }

    walletService.processBonusCompletion(bonusExtId, playerId);
  }
}
