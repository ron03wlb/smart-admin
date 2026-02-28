package net.lab1024.sa.igaming.wallet.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Agent settlement consumer — credits agent wallets when commissions are approved.
 *
 * <p>Listens to {@code igaming.agent.events} for COMMISSION_APPROVED events and calls {@link
 * WalletService#processAgentCommission} to credit the approved amount to the agent's CASH wallet.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentSettlementConsumer {

  private final WalletService walletService;

  @KafkaListener(
      topics = IgamingKafkaConst.Topic.AGENT_EVENTS,
      groupId = IgamingKafkaConst.Group.WALLET)
  public void onAgentEvent(String message) {
    DomainEvent event;
    try {
      event = JsonUtil.fromJson(message, DomainEvent.class);
    } catch (Exception e) {
      log.error("Failed to deserialize agent event: {}", message, e);
      return;
    }

    if (event == null) {
      return;
    }

    String eventType = event.getEventType();
    if (DomainEventTypeConst.SETTLEMENT_COMPLETED.equals(eventType)) {
      log.info(
          "[AGENT] Settlement completed: eventId={}, aggregateId={}",
          event.getEventId(),
          event.getAggregateId());
      return;
    }

    if (!DomainEventTypeConst.COMMISSION_APPROVED.equals(eventType)) {
      return;
    }

    try {
      processCommissionApproved(event);
    } catch (Exception e) {
      log.error(
          "Failed to process COMMISSION_APPROVED event: eventId={}, error={}",
          event.getEventId(),
          e.getMessage(),
          e);
    }
  }

  private void processCommissionApproved(DomainEvent event) {
    JsonNode payload = event.getPayload();
    if (payload == null) {
      log.warn("COMMISSION_APPROVED event has no payload: eventId={}", event.getEventId());
      return;
    }

    Long agentId = payload.path("agentId").asLong(0);
    String netAmountStr = payload.path("netAmount").asText("");
    Long recordId = payload.path("recordId").asLong(0);

    if (agentId == 0 || netAmountStr.isEmpty()) {
      log.warn("COMMISSION_APPROVED event missing required fields: eventId={}", event.getEventId());
      return;
    }

    BigDecimal netAmount;
    try {
      netAmount = new BigDecimal(netAmountStr);
    } catch (NumberFormatException e) {
      log.warn(
          "COMMISSION_APPROVED invalid netAmount: eventId={}, netAmount={}",
          event.getEventId(),
          netAmountStr);
      return;
    }

    String requestId = "commission:" + recordId;
    walletService.processAgentCommission(agentId, netAmount, requestId);
  }
}
