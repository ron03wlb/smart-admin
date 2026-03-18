package net.lab1024.sa.igaming.integration.turnover;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.integration.turnover.domain.event.BetSettlementEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Turnover event consumer — consumes bet settlement events from Kafka.
 *
 * <p>This consumer listens to the {@code game-events} topic and processes ROUND_SETTLED events.
 * When a bet is settled in the Game module, this consumer triggers the turnover calculation and
 * wagering progress update flow.
 *
 * <p><b>Kafka Configuration:</b>
 *
 * <ul>
 *   <li>Topic: {@code game-events}
 *   <li>Group ID: {@code igaming-integration-turnover-consumer}
 *   <li>Event Type Filter: {@code ROUND_SETTLED}
 *   <li>Concurrency: 3 threads (configurable via application.yml)
 *   <li>Acknowledgment Mode: MANUAL (explicit commit after processing)
 * </ul>
 *
 * <p><b>Error Handling:</b>
 *
 * <ul>
 *   <li>Parse errors → log error and acknowledge (avoid blocking queue)
 *   <li>Business logic errors → handled in TurnoverIntegrationService (no exception thrown)
 *   <li>Idempotency: Turnover calculation is idempotent (same betId = same result)
 * </ul>
 *
 * <p><b>Multi-Tenant:</b> TenantContext is set from event.tenantId before processing.
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TurnoverEventConsumer {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final TurnoverIntegrationService turnoverIntegrationService;

  /**
   * Consume ROUND_SETTLED events from game-events topic.
   *
   * <p>This method is invoked by Spring Kafka when a new message arrives. It deserializes the
   * DomainEvent, extracts bet settlement details, and delegates to TurnoverIntegrationService for
   * processing.
   *
   * <p><b>Processing Steps:</b>
   *
   * <ol>
   *   <li>Deserialize DomainEvent from Kafka message
   *   <li>Filter: only process ROUND_SETTLED events
   *   <li>Set TenantContext from event.tenantId
   *   <li>Extract bet settlement details from event.payload
   *   <li>Call TurnoverIntegrationService.processBetSettlement()
   *   <li>Acknowledge Kafka message (manual commit)
   *   <li>Clear TenantContext
   * </ol>
   *
   * @param record Kafka consumer record
   * @param acknowledgment manual acknowledgment handle
   */
  @KafkaListener(
      topics = IgamingKafkaConst.Topic.GAME_EVENTS,
      groupId = "igaming-integration-turnover-consumer",
      concurrency = "3")
  public void consumeGameEvent(
      ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
    String messageValue = record.value();

    try {
      // Deserialize DomainEvent
      DomainEvent event = OBJECT_MAPPER.readValue(messageValue, DomainEvent.class);

      // Filter: only process ROUND_SETTLED events
      if (!DomainEventTypeConst.ROUND_SETTLED.equals(event.getEventType())) {
        acknowledgment.acknowledge();
        return;
      }

      log.info(
          "Received ROUND_SETTLED event: eventId={}, tenantId={}, aggregateId={}",
          event.getEventId(),
          event.getTenantId(),
          event.getAggregateId());

      // Set TenantContext (required for MyBatis Plus tenant interceptor)
      TenantContext.setTenantId(event.getTenantId());

      // Extract bet settlement details from payload
      BetSettlementEvent betEvent = extractBetSettlementEvent(event);

      // Process bet settlement (turnover calculation + wagering progress update)
      turnoverIntegrationService.processBetSettlement(betEvent);

      // Acknowledge Kafka message (manual commit)
      acknowledgment.acknowledge();

      log.debug(
          "ROUND_SETTLED event processed successfully: eventId={}, betId={}",
          event.getEventId(),
          betEvent.getBetId());

    } catch (Exception e) {
      log.error(
          "Failed to process ROUND_SETTLED event: offset={}, partition={}, error={}",
          record.offset(),
          record.partition(),
          e.getMessage(),
          e);
      // Acknowledge anyway to avoid blocking the queue (dead letter queue should be configured)
      acknowledgment.acknowledge();
    } finally {
      // Clear TenantContext to avoid thread-local pollution
      TenantContext.clear();
    }
  }

  /**
   * Extract bet settlement details from DomainEvent payload.
   *
   * <p>Payload structure (JSON):
   *
   * <pre>
   * {
   *   "roundId": 12345,
   *   "playerId": 1001,
   *   "betId": "BET-20260318-001",
   *   "gameCode": "slot-001",
   *   "providerCode": "pragmatic-play",
   *   "gameCategory": 1,
   *   "betAmount": "100.00",
   *   "payoutAmount": "195.00",
   *   "settlementStatus": 1,
   *   "oddsValue": "1.95",
   *   "oddsType": 1,
   *   "riskScore": 0,
   *   "settledAt": "2026-03-18T10:30:00Z"
   * }
   * </pre>
   *
   * @param event domain event from Kafka
   * @return bet settlement event DTO
   */
  private BetSettlementEvent extractBetSettlementEvent(DomainEvent event) {
    JsonNode payload = event.getPayload();

    BetSettlementEvent betEvent = new BetSettlementEvent();
    betEvent.setRoundId(payload.has("roundId") ? payload.get("roundId").asLong() : null);
    betEvent.setPlayerId(payload.get("playerId").asLong());
    betEvent.setTenantId(event.getTenantId());
    betEvent.setBetId(payload.get("betId").asText());
    betEvent.setGameCode(payload.get("gameCode").asText());
    betEvent.setProviderCode(payload.get("providerCode").asText());
    betEvent.setGameCategory(payload.get("gameCategory").asInt());
    betEvent.setBetAmount(new BigDecimal(payload.get("betAmount").asText()));
    betEvent.setPayoutAmount(
        payload.has("payoutAmount")
            ? new BigDecimal(payload.get("payoutAmount").asText())
            : BigDecimal.ZERO);
    betEvent.setSettlementStatus(payload.get("settlementStatus").asInt());
    betEvent.setOddsValue(
        payload.has("oddsValue") ? new BigDecimal(payload.get("oddsValue").asText()) : null);
    betEvent.setOddsType(payload.has("oddsType") ? payload.get("oddsType").asInt() : null);
    betEvent.setRiskScore(payload.has("riskScore") ? payload.get("riskScore").asInt() : null);
    betEvent.setSettledAt(
        payload.has("settledAt")
            ? payload.get("settledAt").asText()
            : OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

    return betEvent;
  }
}
