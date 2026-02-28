package net.lab1024.sa.app.igaming.wallet.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.wallet.consumer.WalletAuditConsumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WalletAuditConsumer unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletAuditConsumer 單元測試")
class WalletAuditConsumerTest {

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

  @InjectMocks private WalletAuditConsumer walletAuditConsumer;

  @Nested
  @DisplayName("onWalletEvent 事件消費")
  class OnWalletEventTest {

    @Test
    @DisplayName("有效錢包事件 — 記錄審計日誌（不拋異常）")
    void validEvent_logsAudit() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("CREDIT_COMPLETED")
              .aggregateType("Wallet")
              .aggregateId("100")
              .eventId("evt-001")
              .tenantId(1L)
              .traceId("trace-001")
              .build();

      assertDoesNotThrow(() -> walletAuditConsumer.onWalletEvent(MAPPER.writeValueAsString(event)));
    }

    @Test
    @DisplayName("DEBIT_COMPLETED 事件 — 記錄審計日誌")
    void debitEvent_logsAudit() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("walletId", 1L);
      payload.put("amount", "50.00");

      DomainEvent event =
          DomainEvent.builder()
              .eventType("DEBIT_COMPLETED")
              .aggregateType("Wallet")
              .aggregateId("200")
              .eventId("evt-002")
              .tenantId(2L)
              .traceId("trace-002")
              .payload(payload)
              .build();

      assertDoesNotThrow(() -> walletAuditConsumer.onWalletEvent(MAPPER.writeValueAsString(event)));
    }

    @Test
    @DisplayName("無效 JSON — 不拋出異常")
    void invalidJson_noException() {
      assertDoesNotThrow(() -> walletAuditConsumer.onWalletEvent("invalid json {{{"));
    }

    @Test
    @DisplayName("空字串 — 不拋出異常")
    void emptyString_noException() {
      assertDoesNotThrow(() -> walletAuditConsumer.onWalletEvent(""));
    }
  }
}
