package net.lab1024.sa.igaming.wallet.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Wallet audit consumer — logs all wallet events for audit trail.
 *
 * <p>Consumes events from {@code igaming.wallet.events} in the AUDIT consumer group. Currently
 * persists audit records via structured logging; future versions may write to an audit table.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WalletAuditConsumer {

  @KafkaListener(
      topics = IgamingKafkaConst.Topic.WALLET_EVENTS,
      groupId = IgamingKafkaConst.Group.AUDIT)
  public void onWalletEvent(String message) {
    DomainEvent event;
    try {
      event = JsonUtil.fromJson(message, DomainEvent.class);
    } catch (Exception e) {
      log.error("Failed to deserialize wallet audit event: {}", message, e);
      return;
    }

    if (event == null) {
      log.error("Failed to deserialize wallet audit event (null result): {}", message);
      return;
    }

    log.info(
        "[AUDIT] wallet event: type={}, aggregateId={}, eventId={}, tenantId={}, traceId={}",
        event.getEventType(),
        event.getAggregateId(),
        event.getEventId(),
        event.getTenantId(),
        event.getTraceId());
  }
}
