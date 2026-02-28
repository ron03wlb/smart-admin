package net.lab1024.sa.app.igaming.wallet.consumer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.wallet.consumer.AgentSettlementConsumer;
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
 * AgentSettlementConsumer unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-28
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgentSettlementConsumer 單元測試")
class AgentSettlementConsumerTest {

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

  @InjectMocks private AgentSettlementConsumer agentSettlementConsumer;

  @Nested
  @DisplayName("onAgentEvent 事件消費")
  class OnAgentEventTest {

    @Test
    @DisplayName("COMMISSION_APPROVED — 呼叫 processAgentCommission")
    void commissionApproved_creditsWallet() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("agentId", 500L);
      payload.put("netAmount", "1234.5678");
      payload.put("recordId", 42L);

      DomainEvent event =
          DomainEvent.builder()
              .eventType("COMMISSION_APPROVED")
              .eventId("evt-001")
              .aggregateType("Commission")
              .aggregateId("42")
              .payload(payload)
              .build();

      agentSettlementConsumer.onAgentEvent(MAPPER.writeValueAsString(event));

      verify(walletService)
          .processAgentCommission(eq(500L), eq(new BigDecimal("1234.5678")), eq("commission:42"));
    }

    @Test
    @DisplayName("SETTLEMENT_COMPLETED — 僅記錄日誌，不呼叫 walletService")
    void settlementCompleted_logsOnly() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("tenantId", 1L);
      payload.put("settlementWeek", "2026-W09");
      payload.put("recordCount", 15);

      DomainEvent event =
          DomainEvent.builder()
              .eventType("SETTLEMENT_COMPLETED")
              .eventId("evt-002")
              .aggregateType("Settlement")
              .aggregateId("2026-W09")
              .payload(payload)
              .build();

      agentSettlementConsumer.onAgentEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("非相關事件 — 忽略")
    void nonMatchingEvent_ignored() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("AGENT_REGISTERED")
              .eventId("evt-003")
              .aggregateType("AffiliateAgent")
              .aggregateId("100")
              .build();

      agentSettlementConsumer.onAgentEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("COMMISSION_APPROVED 缺少 payload — 忽略")
    void commissionApproved_noPayload_ignored() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("COMMISSION_APPROVED")
              .eventId("evt-004")
              .aggregateType("Commission")
              .aggregateId("50")
              .build();

      agentSettlementConsumer.onAgentEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("COMMISSION_APPROVED 缺少必要欄位 — 忽略")
    void commissionApproved_missingFields_ignored() throws JsonProcessingException {
      ObjectNode payload = MAPPER.createObjectNode();
      // Missing agentId and netAmount

      DomainEvent event =
          DomainEvent.builder()
              .eventType("COMMISSION_APPROVED")
              .eventId("evt-005")
              .aggregateType("Commission")
              .aggregateId("60")
              .payload(payload)
              .build();

      agentSettlementConsumer.onAgentEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletService);
    }

    @Test
    @DisplayName("JSON 解析失敗 — 不拋出異常")
    void invalidJson_noException() {
      agentSettlementConsumer.onAgentEvent("invalid json {{{");

      verifyNoInteractions(walletService);
    }
  }
}
