package net.lab1024.sa.app.igaming.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.HashMap;
import java.util.Map;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.code.PaymentErrorCode;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.code.RiskErrorCode;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validates that all iGaming error codes are globally unique across modules.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@DisplayName("iGaming ErrorCode 唯一性測試")
class ErrorCodeUniquenessTest {

  @Test
  @DisplayName("所有 ErrorCode 值全局唯一 — 無衝突")
  void allErrorCodesShouldBeGloballyUnique() {
    Map<Integer, String> codeMap = new HashMap<>();

    collectCodes(codeMap, WalletErrorCode.values(), "WalletErrorCode");
    collectCodes(codeMap, PaymentErrorCode.values(), "PaymentErrorCode");
    collectCodes(codeMap, PlayerErrorCode.values(), "PlayerErrorCode");
    collectCodes(codeMap, GameErrorCode.values(), "GameErrorCode");
    collectCodes(codeMap, ActivityErrorCode.values(), "ActivityErrorCode");
    collectCodes(codeMap, RiskErrorCode.values(), "RiskErrorCode");
    collectCodes(codeMap, AgentErrorCode.values(), "AgentErrorCode");

    // If we reached here without assertion failure, all codes are unique
    assertThat(codeMap).isNotEmpty();
  }

  @Test
  @DisplayName("ErrorCode 範圍不重疊")
  void errorCodeRangesShouldNotOverlap() {
    assertRangeDoesNotOverlap("Wallet", 30100, 30199, WalletErrorCode.values());
    assertRangeDoesNotOverlap("Payment", 30200, 30299, PaymentErrorCode.values());
    assertRangeDoesNotOverlap("Player", 30300, 30399, PlayerErrorCode.values());
    assertRangeDoesNotOverlap("Game", 30400, 30499, GameErrorCode.values());
    assertRangeDoesNotOverlap("Activity", 30500, 30599, ActivityErrorCode.values());
    assertRangeDoesNotOverlap("Agent", 30600, 30699, AgentErrorCode.values());
    assertRangeDoesNotOverlap("Risk", 30700, 30799, RiskErrorCode.values());
  }

  private <E extends Enum<E>> void collectCodes(
      Map<Integer, String> codeMap, E[] values, String enumName) {
    for (E value : values) {
      int code = getCode(value);
      String label = enumName + "." + value.name();
      String existing = codeMap.put(code, label);
      if (existing != null) {
        fail("Duplicate error code %d: %s conflicts with %s", code, label, existing);
      }
    }
  }

  private <E extends Enum<E>> void assertRangeDoesNotOverlap(
      String moduleName, int min, int max, E[] values) {
    for (E value : values) {
      int code = getCode(value);
      assertThat(code)
          .as("%s.%s code=%d should be within [%d, %d]", moduleName, value.name(), code, min, max)
          .isBetween(min, max);
    }
  }

  private int getCode(Object enumValue) {
    try {
      return (int) enumValue.getClass().getMethod("getCode").invoke(enumValue);
    } catch (Exception e) {
      throw new RuntimeException("Failed to get code from " + enumValue, e);
    }
  }
}
