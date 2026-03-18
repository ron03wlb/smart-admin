package net.lab1024.sa.igaming.activity.turnover.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverOddsThresholdRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverOddsThresholdRuleManager;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverRiskActionRuleManager;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link RiskFilterCmp}.
 *
 * <p>Tests the Layer 1 risk filtering logic including:
 *
 * <ul>
 *   <li>Odds threshold validation with 6 comparison operators
 *   <li>Risk score mapping to 4 risk levels (LOW, MEDIUM, HIGH, CRITICAL)
 *   <li>Risk action application (PASS, FLAG, BLOCK)
 *   <li>Effective turnover calculation
 * </ul>
 *
 * <p><b>Note:</b> This is a unit test using mocked managers. For end-to-end LiteFlow chain
 * execution, see {@code TurnoverCalculationIntegrationTest}.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskFilterCmp 單元測試")
class RiskFilterCmpTest {

  @Mock private TurnoverOddsThresholdRuleManager oddsThresholdRuleManager;
  @Mock private TurnoverRiskActionRuleManager riskActionRuleManager;

  private RiskFilterCmpTestHelper helper;

  @BeforeEach
  void setUp() {
    helper = new RiskFilterCmpTestHelper(oddsThresholdRuleManager, riskActionRuleManager);
  }

  // ===== 測試維度A: 賠率閾值驗證 =====

  @Nested
  @DisplayName("賠率閾值驗證")
  class OddsThresholdValidationTests {

    @Test
    @DisplayName("當賠率高於閾值且使用>=運算符時應該通過")
    void whenOddsAboveThreshold_GE_Operator_ShouldPass() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.95"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setRuleCode("OT-TEST-1");
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator(">=");

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(rule));

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setRuleCode("RA-TEST-1");
      riskRule.setActionType(1); // PASS
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isFalse();
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
      assertThat(ctx.getMatchedRules()).contains("OT-TEST-1", "RA-TEST-1");
    }

    @Test
    @DisplayName("當賠率低於閾值且使用>=運算符時應該拒絕")
    void whenOddsBelowThreshold_GE_Operator_ShouldReject() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.30"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setRuleCode("OT-TEST-2");
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator(">=");

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(rule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("ODDS_TOO_LOW");
      assertThat(ctx.getMatchedRules()).contains("OT-TEST-2");
    }

    @Test
    @DisplayName("當賠率等於閾值且使用>運算符時應該拒絕")
    void whenOddsEqual_GT_Operator_ShouldReject() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.50"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setRuleCode("OT-TEST-3");
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator(">");

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(rule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("ODDS_TOO_LOW");
    }

    @Test
    @DisplayName("當賠率高於閾值且使用<運算符時應該拒絕")
    void whenOddsAbove_LT_Operator_ShouldReject() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("2.00"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setRuleCode("OT-TEST-4");
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator("<");

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(rule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("ODDS_TOO_LOW");
    }

    @Test
    @DisplayName("當賠率低於閾值且使用<=運算符時應該通過")
    void whenOddsBelow_LE_Operator_ShouldPass() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.20"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setRuleCode("OT-TEST-5");
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator("<=");

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(rule));

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setRuleCode("RA-TEST-2");
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(any(), any())).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isFalse();
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當賠率等於閾值且使用=運算符時應該通過")
    void whenOddsEqual_EQ_Operator_ShouldPass() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.50"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setRuleCode("OT-TEST-6");
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator("=");

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(rule));

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setRuleCode("RA-TEST-3");
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(any(), any())).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isFalse();
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當沒有配置規則時應該默認允許")
    void whenNoRuleConfigured_ShouldDefaultAllow() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.20"));
      ctx.setOddsType(1);

      when(oddsThresholdRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setRuleCode("RA-TEST-4");
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(any(), any())).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isFalse();
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當賠率值為null時應該拒絕")
    void whenOddsValueNull_ShouldReject() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(null);
      ctx.setOddsType(1);

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("ODDS_TOO_LOW");
    }
  }

  // ===== 測試維度B: 風險評分映射 =====

  @Nested
  @DisplayName("風險評分映射")
  class RiskScoreMappingTests {

    @Test
    @DisplayName("風險評分0應該映射到LOW級別")
    void whenRiskScore0_ShouldMapToLOW() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(0);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setRuleCode("RA-LOW");
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.getRiskActionType()).isEqualTo(1);
      assertThat(ctx.getTurnoverFactor()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("風險評分29應該映射到LOW級別")
    void whenRiskScore29_ShouldMapToLOW() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(29);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert - riskLevel=1 should be used
      assertThat(ctx.getRiskActionType()).isEqualTo(1);
    }

    @Test
    @DisplayName("風險評分30應該映射到MEDIUM級別")
    void whenRiskScore30_ShouldMapToMEDIUM() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(30);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(2); // FLAG
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(2))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert - riskLevel=2 should be used
      assertThat(ctx.getRiskActionType()).isEqualTo(2);
    }

    @Test
    @DisplayName("風險評分49應該映射到MEDIUM級別")
    void whenRiskScore49_ShouldMapToMEDIUM() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(49);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(2);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(2))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.getRiskActionType()).isEqualTo(2);
    }

    @Test
    @DisplayName("風險評分50應該映射到HIGH級別")
    void whenRiskScore50_ShouldMapToHIGH() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(50);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(2);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(3))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert - riskLevel=3 should be used
      assertThat(ctx.getRiskActionType()).isEqualTo(2);
    }

    @Test
    @DisplayName("風險評分70應該映射到CRITICAL級別")
    void whenRiskScore70_ShouldMapToCRITICAL() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(70);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(3); // BLOCK
      riskRule.setTurnoverFactor(new BigDecimal("0.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(4))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert - riskLevel=4 should be used
      assertThat(ctx.getRiskActionType()).isEqualTo(3);
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("RISK_SCORE_HIGH");
    }

    @Test
    @DisplayName("風險評分100應該映射到CRITICAL級別")
    void whenRiskScore100_ShouldMapToCRITICAL() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(100);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(3);
      riskRule.setTurnoverFactor(new BigDecimal("0.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(4))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("RISK_SCORE_HIGH");
    }
  }

  // ===== 測試維度C: 風險動作處理 =====

  @Nested
  @DisplayName("風險動作處理")
  class RiskActionHandlingTests {

    @Test
    @DisplayName("當動作類型為PASS時應該應用100%因子")
    void whenActionTypePASS_ShouldApply100Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(0);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(1))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.getRiskActionType()).isEqualTo(1);
      assertThat(ctx.getTurnoverFactor()).isEqualByComparingTo("100.00");
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當動作類型為FLAG時應該應用100%因子")
    void whenActionTypeFLAG_ShouldApply100Percent() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(30);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(2); // FLAG
      riskRule.setTurnoverFactor(new BigDecimal("100.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(2))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.getRiskActionType()).isEqualTo(2);
      assertThat(ctx.isRejected()).isFalse();
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當動作類型為BLOCK時應該拒絕")
    void whenActionTypeBLOCK_ShouldReject() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(70);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(3); // BLOCK
      riskRule.setTurnoverFactor(new BigDecimal("0.00"));

      when(riskActionRuleManager.getActiveRule(eq(1L), eq(4))).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.getRiskActionType()).isEqualTo(3);
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("RISK_SCORE_HIGH");
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("當沒有風險動作規則時應該默認PASS")
    void whenNoRiskActionRule_ShouldDefaultPASS() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(0);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());
      when(riskActionRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      // Act
      helper.processRiskFilter(ctx);

      // Assert - should default to PASS with 100%
      assertThat(ctx.getRiskActionType()).isEqualTo(1);
      assertThat(ctx.getTurnoverFactor()).isEqualByComparingTo("100.00");
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
    }
  }

  // ===== 測試維度D: 有效投注額計算 =====

  @Nested
  @DisplayName("有效投注額計算")
  class EffectiveTurnoverCalculationTests {

    @Test
    @DisplayName("當投注額100且因子50%時應該計算為50")
    void whenBetAmount100_Factor50_ShouldCalculate50() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setRiskScore(0);

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.none());

      TurnoverRiskActionRuleEntity riskRule = new TurnoverRiskActionRuleEntity();
      riskRule.setActionType(1);
      riskRule.setTurnoverFactor(new BigDecimal("50.00"));

      when(riskActionRuleManager.getActiveRule(any(), any())).thenReturn(Option.of(riskRule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert - 100 * 50 / 100 = 50
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("當已被拒絕時應該設置為0")
    void whenRejected_ShouldSetZero() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setOddsValue(new BigDecimal("1.20"));
      ctx.setOddsType(1);

      TurnoverOddsThresholdRuleEntity rule = new TurnoverOddsThresholdRuleEntity();
      rule.setThresholdValue(new BigDecimal("1.50"));
      rule.setComparisonOperator(">=");

      when(oddsThresholdRuleManager.getActiveRule(any(), any())).thenReturn(Option.of(rule));

      // Act
      helper.processRiskFilter(ctx);

      // Assert
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getEffectiveTurnoverBase())
          .isEqualByComparingTo("0"); // Set to zero when rejected
    }
  }

  // ===== Test Helper Class =====

  /**
   * Test helper to simulate RiskFilterCmp behavior without LiteFlow runtime.
   *
   * <p>This helper replicates the component logic for unit testing by directly calling the
   * component methods with a mocked context.
   */
  private static class RiskFilterCmpTestHelper {

    private final RiskFilterCmp component;

    RiskFilterCmpTestHelper(
        TurnoverOddsThresholdRuleManager oddsThresholdRuleManager,
        TurnoverRiskActionRuleManager riskActionRuleManager) {
      this.component = new RiskFilterCmp(oddsThresholdRuleManager, riskActionRuleManager);
    }

    /**
     * Simulate RiskFilterCmp.process() behavior.
     *
     * <p>Since we cannot call process() directly (requires LiteFlow runtime), we replicate the
     * logic by calling private methods via reflection or using integration tests.
     *
     * <p>For unit tests, we test the component behavior by verifying the TurnoverContext state
     * changes after processing.
     */
    void processRiskFilter(TurnoverContext ctx) {
      try {
        // Use reflection to call private methods
        java.lang.reflect.Method validateOddsThreshold =
            RiskFilterCmp.class.getDeclaredMethod("validateOddsThreshold", TurnoverContext.class);
        validateOddsThreshold.setAccessible(true);

        boolean oddsValid = (Boolean) validateOddsThreshold.invoke(component, ctx);

        if (!oddsValid) {
          ctx.markRejected("ODDS_TOO_LOW");
          return;
        }

        java.lang.reflect.Method applyRiskAction =
            RiskFilterCmp.class.getDeclaredMethod("applyRiskAction", TurnoverContext.class);
        applyRiskAction.setAccessible(true);
        applyRiskAction.invoke(component, ctx);

        java.lang.reflect.Method calculateEffectiveTurnover =
            RiskFilterCmp.class.getDeclaredMethod(
                "calculateEffectiveTurnover", TurnoverContext.class);
        calculateEffectiveTurnover.setAccessible(true);
        calculateEffectiveTurnover.invoke(component, ctx);

      } catch (Exception e) {
        throw new RuntimeException("Failed to process risk filter", e);
      }
    }
  }
}
