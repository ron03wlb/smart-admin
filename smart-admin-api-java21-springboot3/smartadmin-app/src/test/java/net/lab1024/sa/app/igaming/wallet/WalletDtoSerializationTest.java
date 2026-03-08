package net.lab1024.sa.app.igaming.wallet;

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
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Wallet DTO serialization tests — verifies BigDecimal precision, OffsetDateTime timezone
 * conversion, and Long safe-integer handling match production Jackson configuration.
 *
 * @author iGaming Team
 * @since 2026-03-09
 */
@DisplayName("Wallet DTO 序列化測試")
class WalletDtoSerializationTest {

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
    TenantContext.setTimezone("Asia/Taipei");
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Nested
  @DisplayName("WalletVO 序列化")
  class WalletVOTest {

    @Test
    @DisplayName("BigDecimal balance 序列化為字串")
    void bigDecimalSerializedAsString() throws JsonProcessingException {
      WalletVO vo = new WalletVO();
      vo.setBalance(new BigDecimal("12345.6789"));
      vo.setLockedAmount(new BigDecimal("100.0000"));
      vo.setAvailableBalance(new BigDecimal("12245.6789"));

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("balance").isTextual()).isTrue();
      assertThat(node.get("balance").asText()).isEqualTo("12345.6789");
      assertThat(node.get("lockedAmount").asText()).isEqualTo("100.0000");
      assertThat(node.get("availableBalance").asText()).isEqualTo("12245.6789");
    }

    @Test
    @DisplayName("OffsetDateTime 轉換為租戶時區 (UTC → Asia/Taipei +08:00)")
    void offsetDateTimeConvertsToTenantTimezone() throws JsonProcessingException {
      WalletVO vo = new WalletVO();
      vo.setCreateTime(OffsetDateTime.of(2026, 3, 9, 10, 0, 0, 0, ZoneOffset.UTC));

      JsonNode node = objectMapper.valueToTree(vo);

      String createTime = node.get("createTime").asText();
      assertThat(createTime).contains("+08:00");
      assertThat(createTime).startsWith("2026-03-09T18:00:00");
    }

    @Test
    @DisplayName("Long 在 JS 安全範圍內序列化為數字")
    void longWithinSafeRange() throws JsonProcessingException {
      WalletVO vo = new WalletVO();
      vo.setWalletId(12345L);
      vo.setPlayerId(67890L);

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("walletId").isNumber()).isTrue();
      assertThat(node.get("walletId").asLong()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("null 欄位正確處理")
    void nullFieldsHandled() throws JsonProcessingException {
      WalletVO vo = new WalletVO();

      String json = objectMapper.writeValueAsString(vo);
      JsonNode node = objectMapper.readTree(json);

      assertThat(node.get("balance").isNull()).isTrue();
      assertThat(node.get("createTime").isNull()).isTrue();
    }
  }

  @Nested
  @DisplayName("WalletTransactionVO 序列化")
  class WalletTransactionVOTest {

    @Test
    @DisplayName("金額欄位保持 4 位小數精度")
    void amountFieldsPrecision() throws JsonProcessingException {
      WalletTransactionVO vo = new WalletTransactionVO();
      vo.setAmount(new BigDecimal("500.0001"));
      vo.setBalanceBefore(new BigDecimal("1000.0000"));
      vo.setBalanceAfter(new BigDecimal("1500.0001"));

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("amount").asText()).isEqualTo("500.0001");
      assertThat(node.get("balanceBefore").asText()).isEqualTo("1000.0000");
      assertThat(node.get("balanceAfter").asText()).isEqualTo("1500.0001");
    }

    @Test
    @DisplayName("BigDecimal 尾零保留 (1000.0000 ≠ 1000)")
    void bigDecimalTrailingZerosPreserved() throws JsonProcessingException {
      WalletTransactionVO vo = new WalletTransactionVO();
      vo.setAmount(new BigDecimal("1000.0000"));

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("amount").asText()).isEqualTo("1000.0000");
    }

    @Test
    @DisplayName("OffsetDateTime 使用 ISO-8601 格式")
    void createTimeIso8601Format() throws JsonProcessingException {
      WalletTransactionVO vo = new WalletTransactionVO();
      vo.setCreateTime(OffsetDateTime.of(2026, 1, 15, 8, 30, 0, 0, ZoneOffset.UTC));

      JsonNode node = objectMapper.valueToTree(vo);

      String createTime = node.get("createTime").asText();
      assertThat(createTime)
          .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+\\-]\\d{2}:\\d{2}");
    }
  }
}
