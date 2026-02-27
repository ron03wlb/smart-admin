package net.lab1024.sa.igaming.activity.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.activity.manager.WageringProgressManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Wagering event consumer — listens for BET_PLACED events and updates wagering progress.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WageringEventConsumer {

  private static final String EVENT_TYPE_BET_PLACED = "BET_PLACED";

  private final WageringProgressManager wageringProgressManager;

  /**
   * Consume game events and update wagering progress for BET_PLACED events.
   *
   * @param message raw Kafka message (JSON serialized DomainEvent)
   */
  @KafkaListener(
      topics = IgamingKafkaConst.Topic.GAME_EVENTS,
      groupId = IgamingKafkaConst.Group.ACTIVITY)
  public void onGameEvent(String message) {
    DomainEvent event;
    try {
      event = JsonUtil.fromJson(message, DomainEvent.class);
    } catch (Exception e) {
      log.error("Failed to deserialize game event: {}", message, e);
      return;
    }
    if (event == null) {
      log.error("Failed to deserialize game event (null result): {}", message);
      return;
    }

    if (!EVENT_TYPE_BET_PLACED.equals(event.getEventType())) {
      return;
    }

    try {
      processBetPlaced(event);
    } catch (Exception e) {
      log.error(
          "Failed to process BET_PLACED event: eventId={}, error={}",
          event.getEventId(),
          e.getMessage(),
          e);
    }
  }

  private void processBetPlaced(DomainEvent event) {
    JsonNode payload = event.getPayload();
    if (payload == null) {
      log.warn("BET_PLACED event has no payload: eventId={}", event.getEventId());
      return;
    }

    Long playerId = payload.path("playerId").asLong(0);
    String gameCode = payload.path("gameCode").asText(null);
    BigDecimal amount = new BigDecimal(payload.path("amount").asText("0"));
    Long tenantId = event.getTenantId();

    if (playerId == 0 || gameCode == null || tenantId == null) {
      log.warn("BET_PLACED event missing required fields: eventId={}", event.getEventId());
      return;
    }

    int updated =
        wageringProgressManager.updateWageringProgress(playerId, gameCode, amount, tenantId);
    if (updated > 0) {
      log.info(
          "Wagering updated: playerId={}, gameCode={}, amount={}, recordsUpdated={}",
          playerId,
          gameCode,
          amount,
          updated);
    }
  }
}
