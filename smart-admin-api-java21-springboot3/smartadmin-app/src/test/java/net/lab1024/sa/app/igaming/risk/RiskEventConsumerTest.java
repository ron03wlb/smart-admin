package net.lab1024.sa.app.igaming.risk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.risk.consumer.RiskEventConsumer;
import net.lab1024.sa.igaming.risk.domain.RiskEvent;
import net.lab1024.sa.igaming.risk.service.RiskEvaluationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskEventConsumer 測試")
class RiskEventConsumerTest {

  @Mock private RiskEvaluationService riskEvaluationService;
  @InjectMocks private RiskEventConsumer riskEventConsumer;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Nested
  @DisplayName("onEvent 測試")
  class OnEventTest {

    @Test
    @DisplayName("BET_PLACED 事件觸發風控評估")
    void bet_placed_triggers_evaluation() {
      DomainEvent event = buildDomainEvent("BET_PLACED", "100");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService).evaluateAndDispatch(any(RiskEvent.class));
    }

    @Test
    @DisplayName("WITHDRAWAL_REQUESTED 事件觸發風控評估")
    void withdrawal_requested_triggers_evaluation() {
      DomainEvent event = buildDomainEvent("WITHDRAWAL_REQUESTED", "200");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService).evaluateAndDispatch(any(RiskEvent.class));
    }

    @Test
    @DisplayName("PLAYER_LOGIN 事件觸發風控評估")
    void player_login_triggers_evaluation() {
      DomainEvent event = buildDomainEvent("PLAYER_LOGIN", "300");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService).evaluateAndDispatch(any(RiskEvent.class));
    }

    @Test
    @DisplayName("非風控相關事件不觸發評估")
    void irrelevant_event_skipped() {
      DomainEvent event = buildDomainEvent("ROUND_SETTLED", "100");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService, never()).evaluateAndDispatch(any());
    }

    @Test
    @DisplayName("無效 JSON 不觸發評估")
    void invalid_json_skipped() {
      riskEventConsumer.onEvent("not-valid-json");

      verify(riskEvaluationService, never()).evaluateAndDispatch(any());
    }

    @Test
    @DisplayName("評估服務異常不影響消費者")
    void evaluation_failure_handled() {
      DomainEvent event = buildDomainEvent("BET_PLACED", "100");
      String json = JsonUtil.toJson(event);

      org.mockito.Mockito.doThrow(new RuntimeException("eval failed"))
          .when(riskEvaluationService)
          .evaluateAndDispatch(any(RiskEvent.class));

      // Should not throw
      riskEventConsumer.onEvent(json);
    }
  }

  private DomainEvent buildDomainEvent(String eventType, String aggregateId) {
    DomainEvent event = new DomainEvent();
    event.setEventType(eventType);
    event.setEventId("evt-001");
    event.setAggregateId(aggregateId);
    event.setTenantId(1L);

    ObjectNode payload = objectMapper.createObjectNode();
    payload.put("playerId", Long.parseLong(aggregateId));
    payload.put("amount", "100.00");
    payload.put("playerBalance", "5000.00");
    event.setPayload(payload);

    return event;
  }
}
