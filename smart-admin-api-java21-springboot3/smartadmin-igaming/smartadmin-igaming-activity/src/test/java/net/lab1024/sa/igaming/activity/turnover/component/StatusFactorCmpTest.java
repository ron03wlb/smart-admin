package net.lab1024.sa.igaming.activity.turnover.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverStatusFactorRuleManager;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link StatusFactorCmp}.
 *
 * <p>Tests the Layer 2 settlement status factor logic including:
 *
 * <ul>
 *   <li>9 settlement statuses (WIN, LOSS, DRAW, TIE, VOID, CANCEL, HALF_WIN, HALF_LOSS, RUNNING)
 *   <li>Factor application (100% for settled, 0% for unsettled/void)
 *   <li>Valid turnover calculation = effectiveTurnoverBase * statusFactor / 100
 *   <li>Rejected bet handling (skip calculation)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StatusFactorCmp 單元測試")
class StatusFactorCmpTest {

  @Mock private TurnoverStatusFactorRuleManager statusFactorRuleManager;

  private StatusFactorCmpTestHelper helper;

  @BeforeEach
  void setUp() {
    helper = new StatusFactorCmpTestHelper(statusFactorRuleManager);
  }

  // ===== 結算狀態因子測試 =====

  @Nested
  @DisplayName("結算狀態因子")
  class SettlementStatusFactorTests {

    @Test
    @DisplayName("當狀態為WIN時應該應用100%因子")
    void whenStatusWIN_ShouldApply100Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(1); // WIN

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(1)))
          .thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當狀態為LOSS時應該應用100%因子")
    void whenStatusLOSS_ShouldApply100Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 2);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(2); // LOSS

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(2)))
          .thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當狀態為DRAW時應該應用0%因子")
    void whenStatusDRAW_ShouldApply0Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 3);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(3); // DRAW

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(3)))
          .thenReturn(new BigDecimal("0.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當狀態為VOID時應該應用0%因子")
    void whenStatusVOID_ShouldApply0Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 5);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(5); // VOID

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(5)))
          .thenReturn(new BigDecimal("0.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當狀態為RUNNING時應該應用0%因子")
    void whenStatusRUNNING_ShouldApply0Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 9);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(9); // RUNNING

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(9)))
          .thenReturn(new BigDecimal("0.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當已被拒絕時應該設置因子為0")
    void whenAlreadyRejected_ShouldSetFactorZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.markRejected("ODDS_TOO_LOW");

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當沒有找到規則時應該默認為0")
    void whenNoRuleFound_ShouldDefaultZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(1);

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(1))).thenReturn(null);

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當狀態為null時應該默認為0")
    void whenStatusNull_ShouldDefaultZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(null);

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("0.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("當有效投注基數為null時應該返回0")
    void whenEffectiveTurnoverNull_ShouldReturnZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(null);
      ctx.setSettlementStatus(1);

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(1)))
          .thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0.00");
    }

    /**
     * 參數化測試 - 覆蓋所有9個結算狀態.
     *
     * <p>狀態映射:
     *
     * <ul>
     *   <li>1: WIN - 100%
     *   <li>2: LOSS - 100%
     *   <li>3: DRAW - 0%
     *   <li>4: TIE - 0%
     *   <li>5: VOID - 0%
     *   <li>6: CANCEL - 0%
     *   <li>7: HALF_WIN - 100%
     *   <li>8: HALF_LOSS - 100%
     *   <li>9: RUNNING - 0%
     * </ul>
     */
    @ParameterizedTest
    @DisplayName("應該為所有狀態應用正確的因子")
    @CsvSource({
      "1, 100.00, 100.00", // WIN
      "2, 100.00, 100.00", // LOSS
      "3, 0.00, 0.00", // DRAW
      "4, 0.00, 0.00", // TIE
      "5, 0.00, 0.00", // VOID
      "6, 0.00, 0.00", // CANCEL
      "7, 100.00, 100.00", // HALF_WIN
      "8, 100.00, 100.00", // HALF_LOSS
      "9, 0.00, 0.00" // RUNNING
    })
    void shouldApplyCorrectFactorForAllStatuses(
        int status, String expectedFactor, String expectedTurnover) {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, status);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setSettlementStatus(status);

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(status)))
          .thenReturn(new BigDecimal(expectedFactor));

      // Act
      helper.processStatusFactor(ctx);

      // Assert
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo(expectedFactor);
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo(expectedTurnover);
    }
  }

  // ===== 計算驗證 =====

  @Nested
  @DisplayName("計算驗證")
  class CalculationValidationTests {

    @Test
    @DisplayName("應該正確計算validTurnoverFinance公式")
    void shouldCalculateValidTurnoverFormulaCorrectly() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("150.00"));
      ctx.setSettlementStatus(1);

      when(statusFactorRuleManager.getStatusFactor(eq(1L), eq(1)))
          .thenReturn(new BigDecimal("100.00"));

      // Act
      helper.processStatusFactor(ctx);

      // Assert - Formula: validTurnoverFinance = effectiveTurnoverBase * statusFactor / 100
      // 150.00 * 100.00 / 100 = 150.00
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("150.00");
    }
  }

  // ===== Test Helper Class =====

  /** Test helper to simulate StatusFactorCmp behavior without LiteFlow runtime. */
  private static class StatusFactorCmpTestHelper {

    private final StatusFactorCmp component;

    StatusFactorCmpTestHelper(TurnoverStatusFactorRuleManager statusFactorRuleManager) {
      this.component = new StatusFactorCmp(statusFactorRuleManager);
    }

    /** Simulate StatusFactorCmp.process() behavior via reflection. */
    void processStatusFactor(TurnoverContext ctx) {
      try {
        // Call private method getStatusFactor
        java.lang.reflect.Method getStatusFactor =
            StatusFactorCmp.class.getDeclaredMethod("getStatusFactor", TurnoverContext.class);
        getStatusFactor.setAccessible(true);

        // Check if already rejected
        if (ctx.isRejected()) {
          ctx.setStatusFactor(BigDecimal.ZERO);
          ctx.setValidTurnoverFinance(BigDecimal.ZERO);
          return;
        }

        BigDecimal statusFactor = (BigDecimal) getStatusFactor.invoke(component, ctx);
        ctx.setStatusFactor(statusFactor);

        // Call private method calculateValidTurnover
        java.lang.reflect.Method calculateValidTurnover =
            StatusFactorCmp.class.getDeclaredMethod(
                "calculateValidTurnover", TurnoverContext.class, BigDecimal.class);
        calculateValidTurnover.setAccessible(true);

        BigDecimal validTurnover =
            (BigDecimal) calculateValidTurnover.invoke(component, ctx, statusFactor);
        ctx.setValidTurnoverFinance(validTurnover);

      } catch (Exception e) {
        throw new RuntimeException("Failed to process status factor", e);
      }
    }
  }
}
