package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.web.web.json.serializer.LongJsonSerializer;
import net.lab1024.sa.common.web.web.json.serializer.TenantTimezoneSerializer;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.domain.vo.GameRoundVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Game DTO serialization tests — verifies betAmount/payoutAmount/weightedTurnover precision, round
 * status enum integer, and OffsetDateTime formatting in JSON output.
 *
 * @author iGaming Team
 * @since 2026-03-09
 */
@DisplayName("Game DTO 序列化測試")
class GameDtoSerializationTest {

  private static ObjectMapper objectMapper;

  @BeforeAll
  static void setupMapper() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Long.class, LongJsonSerializer.INSTANCE);
    module.addSerializer(Long.TYPE, LongJsonSerializer.INSTANCE);
    module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
    module.addSerializer(OffsetDateTime.class, new TenantTimezoneSerializer());

    objectMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addModule(module)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();
  }

  @BeforeEach
  void setUp() {
    TenantContext.setTimezone("UTC");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Nested
  @DisplayName("GameRoundVO 序列化")
  class GameRoundVOTest {

    @Test
    @DisplayName("betAmount/payoutAmount 保持 4 位精度")
    void amountFieldsPrecision() throws JsonProcessingException {
      GameRoundVO vo = buildGameRoundVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("betAmount").isTextual()).isTrue();
      assertThat(node.get("betAmount").asText()).isEqualTo("100.0000");
      assertThat(node.get("payoutAmount").asText()).isEqualTo("250.5000");
      assertThat(node.get("weightedTurnover").asText()).isEqualTo("80.0000");
    }

    @Test
    @DisplayName("status 序列化為 Integer (非字串)")
    void statusAsInteger() throws JsonProcessingException {
      GameRoundVO vo = buildGameRoundVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("status").isInt()).isTrue();
      assertThat(node.get("status").asInt()).isEqualTo(RoundStatusEnum.SETTLED.getValue());
    }

    @Test
    @DisplayName("reconciliationStatus 序列化為 Integer")
    void reconciliationStatusAsInteger() throws JsonProcessingException {
      GameRoundVO vo = buildGameRoundVO();
      vo.setReconciliationStatus(2);

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("reconciliationStatus").isInt()).isTrue();
      assertThat(node.get("reconciliationStatus").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("OffsetDateTime 以 ISO-8601 格式序列化")
    void createTimeIso8601() throws JsonProcessingException {
      GameRoundVO vo = new GameRoundVO();
      vo.setCreateTime(OffsetDateTime.of(2026, 3, 9, 14, 30, 0, 0, ZoneOffset.UTC));

      JsonNode node = objectMapper.valueToTree(vo);

      String createTime = node.get("createTime").asText();
      assertThat(createTime).startsWith("2026-03-09T14:30:00");
      assertThat(createTime).contains("Z");
    }

    @Test
    @DisplayName("BigDecimal 零值保持精度 (0.0000)")
    void zeroAmountPreserved() throws JsonProcessingException {
      GameRoundVO vo = new GameRoundVO();
      vo.setBetAmount(new BigDecimal("0.0000"));
      vo.setPayoutAmount(BigDecimal.ZERO);

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("betAmount").asText()).isEqualTo("0.0000");
      assertThat(node.get("payoutAmount").asText()).isEqualTo("0");
    }
  }

  private GameRoundVO buildGameRoundVO() {
    GameRoundVO vo = new GameRoundVO();
    vo.setRoundId(1L);
    vo.setPlayerId(100L);
    vo.setProviderCode("PROVIDER_A");
    vo.setGpRoundId("GP-001");
    vo.setGameCode("GAME_A");
    vo.setTransactionId("txn-001");
    vo.setBetAmount(new BigDecimal("100.0000"));
    vo.setPayoutAmount(new BigDecimal("250.5000"));
    vo.setWeightedTurnover(new BigDecimal("80.0000"));
    vo.setStatus(RoundStatusEnum.SETTLED.getValue());
    vo.setReconciliationStatus(1);
    vo.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }
}
