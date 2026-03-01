package net.lab1024.sa.app.igaming.risk;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.risk.consumer.RiskEventConsumer;
import net.lab1024.sa.igaming.risk.domain.RiskEvent;
import net.lab1024.sa.igaming.risk.service.RiskEvaluationService;
import org.junit.jupiter.api.BeforeAll;
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

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  @BeforeAll
  static void initJsonUtil() throws Exception {
    Field field = JsonUtil.class.getDeclaredField("staticMapper");
    field.setAccessible(true);
    if (field.get(null) == null) {
      field.set(null, MAPPER);
    }
  }

  @Mock private RiskEvaluationService riskEvaluationService;
  @InjectMocks private RiskEventConsumer riskEventConsumer;

  @Nested
  @DisplayName("onEvent 測試")
  class OnEventTest {

    @Test
    @DisplayName("BET_PLACED 事件觸發風控評估")
    void bet_placed_triggers_evaluation() {
      DomainEvent event = buildDomainEvent(DomainEventTypeConst.BET_PLACED, "100");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService).evaluateAndDispatch(any(RiskEvent.class));
    }

    @Test
    @DisplayName("WITHDRAWAL_REQUESTED 事件觸發風控評估")
    void withdrawal_requested_triggers_evaluation() {
      DomainEvent event = buildDomainEvent(DomainEventTypeConst.WITHDRAWAL_REQUESTED, "200");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService).evaluateAndDispatch(any(RiskEvent.class));
    }

    @Test
    @DisplayName("PLAYER_LOGIN 事件觸發風控評估")
    void player_login_triggers_evaluation() {
      DomainEvent event = buildDomainEvent(DomainEventTypeConst.PLAYER_LOGIN, "300");
      String json = JsonUtil.toJson(event);

      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService).evaluateAndDispatch(any(RiskEvent.class));
    }

    @Test
    @DisplayName("非風控相關事件不觸發評估")
    void irrelevant_event_skipped() {
      DomainEvent event = buildDomainEvent(DomainEventTypeConst.ROUND_SETTLED, "100");
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
    @DisplayName("tenantId 為 null — 跳過處理")
    void nullTenantId_skips() {
      DomainEvent event = new DomainEvent();
      event.setEventType(DomainEventTypeConst.BET_PLACED);
      event.setEventId("evt-null-tenant");
      event.setAggregateId("100");
      event.setTenantId(null);

      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("playerId", 100L);
      payload.put("amount", "100.00");
      payload.put("playerBalance", "5000.00");
      event.setPayload(payload);

      String json = JsonUtil.toJson(event);
      riskEventConsumer.onEvent(json);

      verify(riskEvaluationService, never()).evaluateAndDispatch(any());
    }

    @Test
    @DisplayName("評估服務異常不影響消費者")
    void evaluation_failure_handled() {
      DomainEvent event = buildDomainEvent(DomainEventTypeConst.BET_PLACED, "100");
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

    ObjectNode payload = MAPPER.createObjectNode();
    payload.put("playerId", Long.parseLong(aggregateId));
    payload.put("amount", "100.00");
    payload.put("playerBalance", "5000.00");
    event.setPayload(payload);

    return event;
  }
}
