package net.lab1024.sa.app.igaming.wallet.consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.wallet.consumer.BonusCompletionConsumer;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BonusCompletionConsumer unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusCompletionConsumer 單元測試")
class BonusCompletionConsumerTest {

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

  @Mock private WalletService walletService;

  @InjectMocks private BonusCompletionConsumer bonusCompletionConsumer;

  @Nested
  @DisplayName("onActivityEvent 事件消費")
  class OnActivityEventTest {

    @Test
    @DisplayName("WAGERING_COMPLETED — 呼叫 processBonusCompletion")
    void wageringCompleted_triggerConversion() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("bonusExtId", 10L);
      payload.put("playerId", 100L);

      DomainEvent event =
          DomainEvent.builder()
              .eventType("WAGERING_COMPLETED")
              .eventId("evt-001")
              .aggregateType("Activity")
              .aggregateId("10")
              .tenantId(1L)
              .payload(payload)
              .build();

      bonusCompletionConsumer.onActivityEvent(MAPPER.writeValueAsString(event));

      verify(walletService).processBonusCompletion(eq(10L), eq(100L));
    }

    @Test
    @DisplayName("非 WAGERING_COMPLETED 事件 — 忽略")
    void nonWageringCompleted_ignored() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("BONUS_CLAIMED")
              .eventId("evt-002")
              .aggregateType("Activity")
              .aggregateId("20")
              .build();

      bonusCompletionConsumer.onActivityEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("WAGERING_COMPLETED 缺少 payload — 忽略")
    void wageringCompleted_noPayload_ignored() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("WAGERING_COMPLETED")
              .eventId("evt-003")
              .aggregateType("Activity")
              .aggregateId("30")
              .tenantId(1L)
              .build();

      bonusCompletionConsumer.onActivityEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("WAGERING_COMPLETED 缺少必要欄位 — 忽略")
    void wageringCompleted_missingFields_ignored() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      // Missing bonusExtId and playerId

      DomainEvent event =
          DomainEvent.builder()
              .eventType("WAGERING_COMPLETED")
              .eventId("evt-004")
              .aggregateType("Activity")
              .aggregateId("40")
              .tenantId(1L)
              .payload(payload)
              .build();

      bonusCompletionConsumer.onActivityEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("tenantId 為 null — 跳過處理")
    void nullTenantId_skips() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("bonusExtId", 10L);
      payload.put("playerId", 100L);

      DomainEvent event =
          DomainEvent.builder()
              .eventType("WAGERING_COMPLETED")
              .eventId("evt-null-tenant")
              .aggregateType("Activity")
              .aggregateId("10")
              .tenantId(null)
              .payload(payload)
              .build();

      bonusCompletionConsumer.onActivityEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("JSON 解析失敗 — 不拋出異常")
    void invalidJson_noException() {
      bonusCompletionConsumer.onActivityEvent("invalid json {{{");

      verifyNoInteractions(walletService);
    }
  }
}
