package net.lab1024.sa.app.igaming.wallet.consumer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.lang.reflect.Field;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.igaming.wallet.consumer.PlayerEventConsumer;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerEventConsumer unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerEventConsumer 單元測試")
class PlayerEventConsumerTest {

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

  @Mock private WalletManager walletManager;

  @InjectMocks private PlayerEventConsumer playerEventConsumer;

  @Nested
  @DisplayName("onPlayerEvent 事件消費")
  class OnPlayerEventTest {

    @Test
    @DisplayName("PLAYER_SUSPENDED — 呼叫 freezePlayerWallets")
    void playerSuspended_freezesWallets() throws JsonProcessingException {
      when(walletManager.freezePlayerWallets(100L)).thenReturn(2);

      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("playerId", 100L);

      DomainEvent event =
          DomainEvent.builder()
              .eventType("PLAYER_SUSPENDED")
              .eventId("evt-001")
              .aggregateType("Player")
              .aggregateId("100")
              .tenantId(1L)
              .payload(payload)
              .build();

      playerEventConsumer.onPlayerEvent(MAPPER.writeValueAsString(event));

      verify(walletManager).freezePlayerWallets(eq(100L));
    }

    @Test
    @DisplayName("PLAYER_SELF_EXCLUDED — 呼叫 freezePlayerWallets")
    void playerSelfExcluded_freezesWallets() throws JsonProcessingException {
      when(walletManager.freezePlayerWallets(200L)).thenReturn(1);

      ObjectNode payload = MAPPER.createObjectNode();
      payload.put("playerId", 200L);

      DomainEvent event =
          DomainEvent.builder()
              .eventType("PLAYER_SELF_EXCLUDED")
              .eventId("evt-002")
              .aggregateType("Player")
              .aggregateId("200")
              .tenantId(1L)
              .payload(payload)
              .build();

      playerEventConsumer.onPlayerEvent(MAPPER.writeValueAsString(event));

      verify(walletManager).freezePlayerWallets(eq(200L));
    }

    @Test
    @DisplayName("playerId 從 aggregateId 回退取得")
    void playerIdFromAggregateId_fallback() throws JsonProcessingException {
      when(walletManager.freezePlayerWallets(400L)).thenReturn(1);

      // No playerId in payload — falls back to aggregateId
      DomainEvent event =
          DomainEvent.builder()
              .eventType("PLAYER_SUSPENDED")
              .eventId("evt-004")
              .aggregateType("Player")
              .aggregateId("400")
              .tenantId(1L)
              .build();

      playerEventConsumer.onPlayerEvent(MAPPER.writeValueAsString(event));

      verify(walletManager).freezePlayerWallets(eq(400L));
    }

    @Test
    @DisplayName("非相關事件 — 忽略")
    void nonMatchingEvent_ignored() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("PLAYER_REGISTERED")
              .eventId("evt-005")
              .aggregateType("Player")
              .aggregateId("500")
              .build();

      playerEventConsumer.onPlayerEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletManager);
    }

    @Test
    @DisplayName("缺少 playerId — 忽略")
    void missingPlayerId_ignored() throws JsonProcessingException {
      // No payload and aggregateId is not numeric
      DomainEvent event =
          DomainEvent.builder()
              .eventType("PLAYER_SUSPENDED")
              .eventId("evt-006")
              .aggregateType("Player")
              .aggregateId("not-a-number")
              .tenantId(1L)
              .build();

      playerEventConsumer.onPlayerEvent(MAPPER.writeValueAsString(event));

      verify(walletManager, never()).freezePlayerWallets(anyLong());
    }

    @Test
    @DisplayName("tenantId 為 null — 跳過處理")
    void nullTenantId_skips() throws JsonProcessingException {
      DomainEvent event =
          DomainEvent.builder()
              .eventType("PLAYER_SUSPENDED")
              .eventId("evt-null-tenant")
              .aggregateType("Player")
              .aggregateId("100")
              .tenantId(null)
              .build();

      playerEventConsumer.onPlayerEvent(MAPPER.writeValueAsString(event));

      verifyNoInteractions(walletManager);
    }

    @Test
    @DisplayName("JSON 解析失敗 — 不拋出異常")
    void invalidJson_noException() {
      playerEventConsumer.onPlayerEvent("invalid json {{{");

      verifyNoInteractions(walletManager);
    }
  }
}
