package net.lab1024.sa.igaming.risk.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.risk.domain.RiskEvent;
import net.lab1024.sa.igaming.risk.service.RiskEvaluationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Risk event consumer — consumes wallet, game, and player events for risk evaluation.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEventConsumer {

  private final RiskEvaluationService riskEvaluationService;

  @KafkaListener(
      topics = {
        IgamingKafkaConst.Topic.GAME_EVENTS,
        IgamingKafkaConst.Topic.WALLET_EVENTS,
        IgamingKafkaConst.Topic.PLAYER_EVENTS
      },
      groupId = IgamingKafkaConst.Group.RISK)
  public void onEvent(String message) {
    DomainEvent domainEvent;
    try {
      domainEvent = JsonUtil.fromJson(message, DomainEvent.class);
    } catch (Exception e) {
      log.error("Failed to deserialize event: {}", message, e);
      return;
    }

    if (domainEvent == null) {
      log.error("Failed to deserialize event (null result): {}", message);
      return;
    }

    String eventType = domainEvent.getEventType();
    if (!isRiskRelevant(eventType)) {
      return;
    }

    try {
      if (domainEvent.getTenantId() != null) {
        TenantContext.setTenantId(domainEvent.getTenantId());
      }
      RiskEvent riskEvent = toRiskEvent(domainEvent);
      riskEvaluationService.evaluateAndDispatch(riskEvent);
    } catch (Exception e) {
      log.error(
          "Risk evaluation failed: eventType={}, aggregateId={}",
          eventType,
          domainEvent.getAggregateId(),
          e);
    } finally {
      TenantContext.clear();
    }
  }

  private boolean isRiskRelevant(String eventType) {
    return DomainEventTypeConst.BET_PLACED.equals(eventType)
        || DomainEventTypeConst.WITHDRAWAL_REQUESTED.equals(eventType)
        || DomainEventTypeConst.PLAYER_LOGIN.equals(eventType);
  }

  private RiskEvent toRiskEvent(DomainEvent domainEvent) {
    RiskEvent event = new RiskEvent();
    event.setEventType(domainEvent.getEventType());
    event.setEventId(domainEvent.getEventId());
    event.setTenantId(domainEvent.getTenantId());

    if (domainEvent.getPayload() != null) {
      var payload = domainEvent.getPayload();
      event.setPlayerId(payload.has("playerId") ? payload.get("playerId").asLong() : null);
      event.setAmount(payload.has("amount") ? payload.decimalValue() : null);
      event.setDeviceId(payload.has("deviceId") ? payload.get("deviceId").asText() : null);
      event.setIpAddress(payload.has("ipAddress") ? payload.get("ipAddress").asText() : null);
      event.setCountryCode(payload.has("countryCode") ? payload.get("countryCode").asText() : null);
      event.setGameType(payload.has("gameType") ? payload.get("gameType").asText() : null);
      event.setGameCode(payload.has("gameCode") ? payload.get("gameCode").asText() : null);
      if (payload.has("amount")) {
        event.setAmount(new java.math.BigDecimal(payload.get("amount").asText()));
      }
      if (payload.has("playerBalance")) {
        event.setPlayerBalance(new java.math.BigDecimal(payload.get("playerBalance").asText()));
      }
    }

    // Fallback: use aggregateId as playerId if not in payload
    if (event.getPlayerId() == null && domainEvent.getAggregateId() != null) {
      try {
        event.setPlayerId(Long.valueOf(domainEvent.getAggregateId()));
      } catch (NumberFormatException e) {
        log.warn("Cannot parse aggregateId as playerId: {}", domainEvent.getAggregateId());
      }
    }

    return event;
  }
}
