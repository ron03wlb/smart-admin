package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.web.web.json.serializer.LongJsonSerializer;
import net.lab1024.sa.common.web.web.json.serializer.TenantTimezoneSerializer;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.domain.vo.PlayerAuthVO;
import net.lab1024.sa.igaming.player.domain.vo.PlayerVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Player DTO serialization tests — verifies PII masking fields, enum integer values, token
 * handling, and OffsetDateTime formatting in JSON output.
 *
 * @author iGaming Team
 * @since 2026-03-09
 */
@DisplayName("Player DTO 序列化測試")
class PlayerDtoSerializationTest {

  private static ObjectMapper objectMapper;

  @BeforeAll
  static void setupMapper() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Long.class, LongJsonSerializer.INSTANCE);
    module.addSerializer(Long.TYPE, LongJsonSerializer.INSTANCE);
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
    TenantContext.setTimezone("Asia/Taipei");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Nested
  @DisplayName("PlayerVO 序列化")
  class PlayerVOTest {

    @Test
    @DisplayName("PII 欄位 (email/phone) 以脫敏字串序列化")
    void piiFieldsMasked() throws JsonProcessingException {
      PlayerVO vo = new PlayerVO();
      vo.setPlayerId(100L);
      vo.setUsername("testplayer");
      vo.setEmail("j***@example.com");
      vo.setPhone("138****5678");

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("email").asText()).isEqualTo("j***@example.com");
      assertThat(node.get("phone").asText()).isEqualTo("138****5678");
    }

    @Test
    @DisplayName("Enum 欄位序列化為 Integer")
    void enumFieldsAsIntegers() throws JsonProcessingException {
      PlayerVO vo = new PlayerVO();
      vo.setStatus(PlayerStatusEnum.ACTIVE.getValue());
      vo.setKycLevel(KycLevelEnum.L1.getValue());
      vo.setVipLevel(VipLevelEnum.GOLD.getValue());

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("status").isInt()).isTrue();
      assertThat(node.get("status").asInt()).isEqualTo(PlayerStatusEnum.ACTIVE.getValue());
      assertThat(node.get("kycLevel").asInt()).isEqualTo(KycLevelEnum.L1.getValue());
      assertThat(node.get("vipLevel").asInt()).isEqualTo(VipLevelEnum.GOLD.getValue());
    }

    @Test
    @DisplayName("lastLoginTime 轉換為租戶時區")
    void lastLoginTimeTimezoneConversion() throws JsonProcessingException {
      PlayerVO vo = new PlayerVO();
      vo.setLastLoginTime(OffsetDateTime.of(2026, 3, 9, 12, 0, 0, 0, ZoneOffset.UTC));

      JsonNode node = objectMapper.valueToTree(vo);

      String lastLogin = node.get("lastLoginTime").asText();
      assertThat(lastLogin).contains("+08:00");
      assertThat(lastLogin).startsWith("2026-03-09T20:00:00");
    }

    @Test
    @DisplayName("null PII 欄位序列化為 JSON null")
    void nullPiiFields() throws JsonProcessingException {
      PlayerVO vo = new PlayerVO();
      vo.setPlayerId(100L);

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("email").isNull()).isTrue();
      assertThat(node.get("phone").isNull()).isTrue();
    }
  }

  @Nested
  @DisplayName("PlayerAuthVO 序列化")
  class PlayerAuthVOTest {

    @Test
    @DisplayName("tokenValue 以字串序列化")
    void tokenValueAsString() throws JsonProcessingException {
      PlayerAuthVO vo = new PlayerAuthVO();
      vo.setPlayerId(100L);
      vo.setUsername("testplayer");
      vo.setTokenValue("sa-token-abc123def456");
      vo.setVipLevel(VipLevelEnum.BRONZE.getValue());

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("tokenValue").isTextual()).isTrue();
      assertThat(node.get("tokenValue").asText()).isEqualTo("sa-token-abc123def456");
      assertThat(node.get("vipLevel").isInt()).isTrue();
    }
  }
}
