package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.web.web.json.serializer.LongJsonSerializer;
import net.lab1024.sa.common.web.web.json.serializer.TenantTimezoneSerializer;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.credit.domain.vo.AgentCreditVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Agent DTO serialization tests — verifies commission amount precision, credit computed fields,
 * LocalDate date format, and OffsetDateTime timezone handling.
 *
 * @author iGaming Team
 * @since 2026-03-09
 */
@DisplayName("Agent DTO 序列化測試")
class AgentDtoSerializationTest {

  private static ObjectMapper objectMapper;

  @BeforeAll
  static void setupMapper() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(Long.class, LongJsonSerializer.INSTANCE);
    module.addSerializer(Long.TYPE, LongJsonSerializer.INSTANCE);
    module.addSerializer(BigDecimal.class, ToStringSerializer.instance);
    module.addSerializer(OffsetDateTime.class, new TenantTimezoneSerializer());
    module.addSerializer(
        LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));

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
  @DisplayName("CommissionRecordVO 序列化")
  class CommissionRecordVOTest {

    @Test
    @DisplayName("佣金金額保持 4 位精度")
    void commissionAmountPrecision() throws JsonProcessingException {
      CommissionRecordVO vo = buildCommissionVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("grossAmount").isTextual()).isTrue();
      assertThat(node.get("grossAmount").asText()).isEqualTo("10000.0000");
      assertThat(node.get("adjustmentAmount").asText()).isEqualTo("-500.0000");
      assertThat(node.get("carryoverAmount").asText()).isEqualTo("200.0000");
      assertThat(node.get("netAmount").asText()).isEqualTo("9700.0000");
    }

    @Test
    @DisplayName("settlementDate 以 yyyy-MM-dd 格式序列化")
    void settlementDateFormat() throws JsonProcessingException {
      CommissionRecordVO vo = buildCommissionVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("settlementDate").isTextual()).isTrue();
      assertThat(node.get("settlementDate").asText()).isEqualTo("2026-03-09");
    }

    @Test
    @DisplayName("approvedAt OffsetDateTime 轉換為租戶時區")
    void approvedAtTimezoneConversion() throws JsonProcessingException {
      CommissionRecordVO vo = buildCommissionVO();
      vo.setApprovedAt(OffsetDateTime.of(2026, 3, 9, 10, 0, 0, 0, ZoneOffset.UTC));

      JsonNode node = objectMapper.valueToTree(vo);

      String approvedAt = node.get("approvedAt").asText();
      assertThat(approvedAt).contains("+08:00");
      assertThat(approvedAt).startsWith("2026-03-09T18:00:00");
    }

    @Test
    @DisplayName("status 序列化為 Integer")
    void statusAsInteger() throws JsonProcessingException {
      CommissionRecordVO vo = buildCommissionVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("status").isInt()).isTrue();
      assertThat(node.get("status").asInt()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("AgentCreditVO 序列化")
  class AgentCreditVOTest {

    @Test
    @DisplayName("信用額度計算欄位精度正確")
    void creditComputedFieldsPrecision() throws JsonProcessingException {
      AgentCreditVO vo = buildAgentCreditVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("creditLimit").asText()).isEqualTo("50000.0000");
      assertThat(node.get("usedCredit").asText()).isEqualTo("10000.0000");
      assertThat(node.get("allocatedToChildren").asText()).isEqualTo("5000.0000");
      assertThat(node.get("availableCredit").asText()).isEqualTo("35000.0000");
    }

    @Test
    @DisplayName("positionPercent 百分比精度")
    void positionPercentPrecision() throws JsonProcessingException {
      AgentCreditVO vo = buildAgentCreditVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("positionPercent").asText()).isEqualTo("50.0000");
      assertThat(node.get("maxPosition").asText()).isEqualTo("100.0000");
    }

    @Test
    @DisplayName("frozenAt 為 null 時序列化為 JSON null")
    void nullTimestampFields() throws JsonProcessingException {
      AgentCreditVO vo = buildAgentCreditVO();

      JsonNode node = objectMapper.valueToTree(vo);

      assertThat(node.get("frozenAt").isNull()).isTrue();
    }
  }

  private CommissionRecordVO buildCommissionVO() {
    CommissionRecordVO vo = new CommissionRecordVO();
    vo.setRecordId(1L);
    vo.setAgentId(100L);
    vo.setPlanId(1L);
    vo.setSettlementDate(LocalDate.of(2026, 3, 9));
    vo.setGrossAmount(new BigDecimal("10000.0000"));
    vo.setAdjustmentAmount(new BigDecimal("-500.0000"));
    vo.setCarryoverAmount(new BigDecimal("200.0000"));
    vo.setNetAmount(new BigDecimal("9700.0000"));
    vo.setStatus(1);
    vo.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }

  private AgentCreditVO buildAgentCreditVO() {
    AgentCreditVO vo = new AgentCreditVO();
    vo.setAgentCreditId(1L);
    vo.setAgentId(100L);
    vo.setParentId(50L);
    vo.setCreditLimit(new BigDecimal("50000.0000"));
    vo.setUsedCredit(new BigDecimal("10000.0000"));
    vo.setAllocatedToChildren(new BigDecimal("5000.0000"));
    vo.setAvailableCredit(new BigDecimal("35000.0000"));
    vo.setPositionPercent(new BigDecimal("50.0000"));
    vo.setMaxPosition(new BigDecimal("100.0000"));
    vo.setStatus(1);
    vo.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }
}
