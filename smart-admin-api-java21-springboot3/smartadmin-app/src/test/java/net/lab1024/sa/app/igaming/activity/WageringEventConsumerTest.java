package net.lab1024.sa.app.igaming.activity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.activity.consumer.WageringEventConsumer;
import net.lab1024.sa.igaming.activity.manager.WageringProgressManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WageringEventConsumer unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WageringEventConsumer 單元測試")
class WageringEventConsumerTest {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  /** Initialize JsonUtil.staticMapper for unit tests (normally done by Spring). */
  @BeforeAll
  static void initJsonUtil() throws Exception {
    Field field = JsonUtil.class.getDeclaredField("staticMapper");
    field.setAccessible(true);
    if (field.get(null) == null) {
      field.set(null, MAPPER);
    }
  }

  @Mock private WageringProgressManager wageringProgressManager;
  @InjectMocks private WageringEventConsumer wageringEventConsumer;

  @Nested
  @DisplayName("onGameEvent 事件消費")
  class OnGameEventTest {

    @Test
    @DisplayName("BET_PLACED 事件 — 成功更新流水")
    void betPlaced_updatesWagering() throws JsonProcessingException {
      ObjectNode payload = JsonNodeFactory.instance.objectNode();
      payload.put("playerId", 1L);
      payload.put("gameCode", "slot-001");
      payload.put("amount", "50.00");

      DomainEvent event =
          DomainEvent.builder()
              .eventType("BET_PLACED")
              .aggregateType("GameRound")
              .aggregateId("1")
              .tenantId(100L)
              .payload(payload)
              .build();

      when(wageringProgressManager.updateWageringProgress(
              eq(1L), eq("slot-001"), any(BigDecimal.class), eq(100L)))
          .thenReturn(1);

      wageringEventConsumer.onGameEvent(MAPPER.writeValueAsString(event));

      verify(wageringProgressManager)
          .updateWageringProgress(eq(1L), eq("slot-001"), any(BigDecimal.class), eq(100L));
    }

    @Test
    @DisplayName("非 BET_PLACED 事件 — 忽略")
    void nonBetPlaced_ignored() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("ROUND_SETTLED")
              .aggregateType("GameRound")
              .aggregateId("1")
              .build();

      wageringEventConsumer.onGameEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(wageringProgressManager);
    }

    @Test
    @DisplayName("JSON 解析失敗 — 不拋出異常")
    void invalidJson_noException() {
      wageringEventConsumer.onGameEvent("invalid json {{{");

      verifyNoInteractions(wageringProgressManager);
    }

    @Test
    @DisplayName("BET_PLACED 缺少必要欄位 — 不更新")
    void betPlaced_missingFields_noUpdate() throws JsonProcessingException {
      ObjectNode payload = JsonNodeFactory.instance.objectNode();
      // Missing playerId, gameCode

      DomainEvent event =
          DomainEvent.builder()
              .eventType("BET_PLACED")
              .aggregateType("GameRound")
              .aggregateId("1")
              .tenantId(100L)
              .payload(payload)
              .build();

      wageringEventConsumer.onGameEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(wageringProgressManager);
    }

    @Test
    @DisplayName("BET_PLACED 無 payload — 不更新")
    void betPlaced_nullPayload_noUpdate() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("BET_PLACED")
              .aggregateType("GameRound")
              .aggregateId("1")
              .tenantId(100L)
              .build();

      wageringEventConsumer.onGameEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(wageringProgressManager);
    }

    @Test
    @DisplayName("BET_PLACED tenantId 為 null — 跳過處理")
    void betPlaced_nullTenantId_skips() throws JsonProcessingException {
      ObjectNode payload = JsonNodeFactory.instance.objectNode();
      payload.put("playerId", 1L);
      payload.put("gameCode", "slot-001");
      payload.put("amount", "50.00");

      DomainEvent event =
          DomainEvent.builder()
              .eventType("BET_PLACED")
              .aggregateType("GameRound")
              .aggregateId("1")
              .tenantId(null)
              .payload(payload)
              .build();

      wageringEventConsumer.onGameEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(wageringProgressManager);
    }
  }
}
