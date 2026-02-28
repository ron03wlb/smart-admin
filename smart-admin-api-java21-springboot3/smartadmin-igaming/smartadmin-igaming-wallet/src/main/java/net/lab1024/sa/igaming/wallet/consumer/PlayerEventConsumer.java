package net.lab1024.sa.igaming.wallet.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Player event consumer — freezes wallets when a player is suspended or self-excluded.
 *
 * <p>Listens to {@code igaming.player.events} for PLAYER_SUSPENDED and PLAYER_SELF_EXCLUDED events
 * and delegates to {@link WalletManager#freezePlayerWallets} to mark all player wallets as deleted
 * (soft freeze).
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerEventConsumer {

  private final WalletManager walletManager;

  @KafkaListener(
      topics = IgamingKafkaConst.Topic.PLAYER_EVENTS,
      groupId = IgamingKafkaConst.Group.WALLET)
  public void onPlayerEvent(String message) {
    DomainEvent event;
    try {
      event = JsonUtil.fromJson(message, DomainEvent.class);
    } catch (Exception e) {
      log.error("Failed to deserialize player event: {}", message, e);
      return;
    }

    if (event == null) {
      return;
    }

    String eventType = event.getEventType();
    if (!DomainEventTypeConst.PLAYER_SUSPENDED.equals(eventType)
        && !DomainEventTypeConst.PLAYER_SELF_EXCLUDED.equals(eventType)) {
      return;
    }

    try {
      freezePlayerWallets(event);
    } catch (Exception e) {
      log.error(
          "Failed to freeze wallets for player event: eventId={}, error={}",
          event.getEventId(),
          e.getMessage(),
          e);
    }
  }

  private void freezePlayerWallets(DomainEvent event) {
    JsonNode payload = event.getPayload();
    Long playerId = payload != null ? payload.path("playerId").asLong(0) : 0L;
    if (playerId == 0 && event.getAggregateId() != null) {
      try {
        playerId = Long.valueOf(event.getAggregateId());
      } catch (NumberFormatException e) {
        log.warn("Cannot parse aggregateId as playerId: {}", event.getAggregateId());
        return;
      }
    }

    if (playerId == 0) {
      log.warn("Player event missing playerId: eventId={}", event.getEventId());
      return;
    }

    int walletCount = walletManager.freezePlayerWallets(playerId);
    log.info(
        "[FREEZE] Player wallets frozen: playerId={}, walletCount={}, reason={}",
        playerId,
        walletCount,
        event.getEventType());
  }
}
