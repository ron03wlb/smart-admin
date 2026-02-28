package net.lab1024.sa.app.igaming.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.code.PaymentErrorCode;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.code.RiskErrorCode;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import net.lab1024.sa.igaming.common.constant.AdjustmentTypeEnum;
import net.lab1024.sa.igaming.common.constant.AgentStatusEnum;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.CommissionPlanTypeEnum;
import net.lab1024.sa.igaming.common.constant.CommissionRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.GameCategoryEnum;
import net.lab1024.sa.igaming.common.constant.HealthStatusEnum;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum;
import net.lab1024.sa.igaming.common.constant.LockReasonEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.common.constant.PaymentVerifyStatusEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import net.lab1024.sa.igaming.common.constant.PromotionTypeEnum;
import net.lab1024.sa.igaming.common.constant.ReconciliationExceptionTypeEnum;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.common.constant.RiskDecisionEnum;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.common.constant.RiskRuleTypeEnum;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.common.constant.SettlementPeriodEnum;
import net.lab1024.sa.igaming.common.constant.SettlementPhaseEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Validates all iGaming enums for completeness and consistency.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@DisplayName("iGaming 枚舉完整性測試")
class IgamingEnumTest {

  /** All BaseEnum implementations in igaming-common. */
  @SuppressWarnings("unchecked")
  private static final Class<? extends BaseEnum>[] BASE_ENUMS =
      new Class[] {
        WalletTypeEnum.class,
        BonusStatusEnum.class,
        BonusRecordStatusEnum.class,
        LockReasonEnum.class,
        TransactionTypeEnum.class,
        AdjustmentTypeEnum.class,
        PaymentOrderTypeEnum.class,
        PaymentOrderStatusEnum.class,
        PaymentVerifyStatusEnum.class,
        PlayerStatusEnum.class,
        KycLevelEnum.class,
        KycDocumentTypeEnum.class,
        KycVerificationStatusEnum.class,
        VipLevelEnum.class,
        GameCategoryEnum.class,
        RoundStatusEnum.class,
        ReconciliationStatusEnum.class,
        ReconciliationExceptionTypeEnum.class,
        HealthStatusEnum.class,
        PromotionTypeEnum.class,
        PromotionStatusEnum.class,
        RiskRuleTypeEnum.class,
        RiskLevelEnum.class,
        RiskDecisionEnum.class,
        RiskProposalStatusEnum.class,
        AgentStatusEnum.class,
        CommissionPlanTypeEnum.class,
        CommissionRecordStatusEnum.class,
        SettlementPeriodEnum.class,
        SettlementPhaseEnum.class,
      };

  @Nested
  @DisplayName("BaseEnum 完整性")
  class BaseEnumCompletenessTest {

    @Test
    @DisplayName("所有 BaseEnum 的 getValue() 不為 null")
    void allBaseEnumValuesShouldNotBeNull() {
      for (Class<? extends BaseEnum> enumClass : BASE_ENUMS) {
        BaseEnum[] constants = enumClass.getEnumConstants();
        assertThat(constants)
            .as("%s should have at least one constant", enumClass.getSimpleName())
            .isNotEmpty();
        for (BaseEnum constant : constants) {
          assertThat(constant.getValue())
              .as("%s.%s getValue() should not be null", enumClass.getSimpleName(), constant)
              .isNotNull();
        }
      }
    }

    @Test
    @DisplayName("所有 BaseEnum 的 getDesc() 不為空")
    void allBaseEnumDescsShouldNotBeBlank() {
      for (Class<? extends BaseEnum> enumClass : BASE_ENUMS) {
        for (BaseEnum constant : enumClass.getEnumConstants()) {
          assertThat(constant.getDesc())
              .as("%s.%s getDesc() should not be blank", enumClass.getSimpleName(), constant)
              .isNotBlank();
        }
      }
    }

    @Test
    @DisplayName("同一枚舉內 value 不重複")
    void baseEnumValuesWithinSameEnumShouldBeUnique() {
      for (Class<? extends BaseEnum> enumClass : BASE_ENUMS) {
        Set<Object> values = new HashSet<>();
        for (BaseEnum constant : enumClass.getEnumConstants()) {
          boolean added = values.add(constant.getValue());
          if (!added) {
            fail(
                "Duplicate value %s in %s.%s",
                constant.getValue(), enumClass.getSimpleName(), constant);
          }
        }
      }
    }
  }

  @Nested
  @DisplayName("ErrorCode 格式驗證")
  class ErrorCodeFormatTest {

    @Test
    @DisplayName("所有 ErrorCode 值在 30000-39999 範圍內")
    void allErrorCodesShouldBeInIgamingRange() {
      assertCodesInRange(WalletErrorCode.values(), "WalletErrorCode");
      assertCodesInRange(PaymentErrorCode.values(), "PaymentErrorCode");
      assertCodesInRange(PlayerErrorCode.values(), "PlayerErrorCode");
      assertCodesInRange(GameErrorCode.values(), "GameErrorCode");
      assertCodesInRange(ActivityErrorCode.values(), "ActivityErrorCode");
      assertCodesInRange(RiskErrorCode.values(), "RiskErrorCode");
      assertCodesInRange(AgentErrorCode.values(), "AgentErrorCode");
    }

    @Test
    @DisplayName("所有 ErrorCode 的 msg 不為空")
    void allErrorCodeMsgsShouldNotBeBlank() {
      assertMsgsNotBlank(WalletErrorCode.values(), "WalletErrorCode");
      assertMsgsNotBlank(PaymentErrorCode.values(), "PaymentErrorCode");
      assertMsgsNotBlank(PlayerErrorCode.values(), "PlayerErrorCode");
      assertMsgsNotBlank(GameErrorCode.values(), "GameErrorCode");
      assertMsgsNotBlank(ActivityErrorCode.values(), "ActivityErrorCode");
      assertMsgsNotBlank(RiskErrorCode.values(), "RiskErrorCode");
      assertMsgsNotBlank(AgentErrorCode.values(), "AgentErrorCode");
    }

    private <E extends Enum<E>> void assertCodesInRange(E[] values, String enumName) {
      for (E value : values) {
        int code = getCode(value);
        assertThat(code)
            .as(
                "%s.%s code=%d should be in iGaming range [30000, 39999]",
                enumName, value.name(), code)
            .isBetween(30000, 39999);
      }
    }

    private <E extends Enum<E>> void assertMsgsNotBlank(E[] values, String enumName) {
      for (E value : values) {
        String msg = getMsg(value);
        assertThat(msg).as("%s.%s msg should not be blank", enumName, value.name()).isNotBlank();
      }
    }

    private int getCode(Object enumValue) {
      try {
        return (int) enumValue.getClass().getMethod("getCode").invoke(enumValue);
      } catch (Exception e) {
        throw new RuntimeException("Failed to get code from " + enumValue, e);
      }
    }

    private String getMsg(Object enumValue) {
      try {
        return (String) enumValue.getClass().getMethod("getMsg").invoke(enumValue);
      } catch (Exception e) {
        throw new RuntimeException("Failed to get msg from " + enumValue, e);
      }
    }
  }

  @Nested
  @DisplayName("ErrorCode 計數驗證")
  class ErrorCodeCountTest {

    @Test
    @DisplayName("7 個 ErrorCode 枚舉存在")
    void sevenErrorCodeEnumsShouldExist() {
      Map<String, Integer> counts = new HashMap<>();
      counts.put("WalletErrorCode", WalletErrorCode.values().length);
      counts.put("PaymentErrorCode", PaymentErrorCode.values().length);
      counts.put("PlayerErrorCode", PlayerErrorCode.values().length);
      counts.put("GameErrorCode", GameErrorCode.values().length);
      counts.put("ActivityErrorCode", ActivityErrorCode.values().length);
      counts.put("RiskErrorCode", RiskErrorCode.values().length);
      counts.put("AgentErrorCode", AgentErrorCode.values().length);

      assertThat(counts).hasSize(7);
      counts.forEach(
          (name, count) ->
              assertThat(count).as("%s should have at least 1 constant", name).isGreaterThan(0));
    }

    @Test
    @DisplayName("30 個 BaseEnum 實現存在")
    void thirtyBaseEnumImplementationsShouldExist() {
      assertThat(BASE_ENUMS).hasSize(30);
    }
  }
}
