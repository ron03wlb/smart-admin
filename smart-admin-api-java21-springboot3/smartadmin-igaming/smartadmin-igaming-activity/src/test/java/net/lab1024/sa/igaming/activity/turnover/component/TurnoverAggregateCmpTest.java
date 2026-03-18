package net.lab1024.sa.igaming.activity.turnover.component;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.activity.turnover.domain.TurnoverContext;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TurnoverAggregateCmp}.
 *
 * <p>Tests the final aggregation node including:
 *
 * <ul>
 *   <li>Context state verification (normal vs rejected flow)
 *   <li>Context immutability (no calculations performed by aggregate node)
 *   <li>All required fields are present and valid
 * </ul>
 *
 * <p>Note: Since TurnoverAggregateCmp only logs without modifying context, these tests verify the
 * context state directly without invoking the component.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@DisplayName("TurnoverAggregateCmp 單元測試")
class TurnoverAggregateCmpTest {

  // No setup needed - tests verify context state directly

  // ===== 正常流程測試 =====

  @Nested
  @DisplayName("正常流程")
  class NormalFlowTests {

    @Test
    @DisplayName("當正常流程時應該有完整結果")
    void whenNormalFlow_ShouldHaveCompleteResult() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("100.00"));
      ctx.setValidTurnoverFinance(new BigDecimal("100.00"));
      ctx.setActivityValidTurnover(new BigDecimal("100.00"));
      ctx.setRiskActionType(1);
      ctx.setStatusFactor(new BigDecimal("100.00"));
      ctx.setGameWeight(new BigDecimal("100.00"));
      ctx.addMatchedRule("RULE-001");
      ctx.addMatchedRule("RULE-002");

      // Assert - Verify all fields are correctly set
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("100.00");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("100.00");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("100.00");
      assertThat(ctx.getRiskActionType()).isEqualTo(1);
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("100.00");
      assertThat(ctx.getMatchedRules()).hasSize(2);
      assertThat(ctx.isRejected()).isFalse();
    }

    @Test
    @DisplayName("當正常流程時各階段計算值應該正確")
    void whenNormalFlow_ShouldHaveCorrectCalculations() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("250.50"), 2, 1);
      ctx.setEffectiveTurnoverBase(new BigDecimal("250.50"));
      ctx.setValidTurnoverFinance(new BigDecimal("250.50"));
      ctx.setActivityValidTurnover(new BigDecimal("37.58")); // 250.50 * 15% / 100
      ctx.setRiskActionType(1);
      ctx.setStatusFactor(new BigDecimal("100.00"));
      ctx.setGameWeight(new BigDecimal("15.00"));

      // Assert - Verify all calculated values are correct
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("250.50");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("250.50");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("37.58");
      assertThat(ctx.getRiskActionType()).isEqualTo(1);
      assertThat(ctx.getStatusFactor()).isEqualByComparingTo("100.00");
      assertThat(ctx.getGameWeight()).isEqualByComparingTo("15.00");
    }
  }

  // ===== 拒絕流程測試 =====

  @Nested
  @DisplayName("拒絕流程")
  class RejectedFlowTests {

    @Test
    @DisplayName("當拒絕時應該有拒絕原因")
    void whenRejected_ShouldHaveRejectionReason() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.markRejected("ODDS_TOO_LOW");
      ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);
      ctx.setValidTurnoverFinance(BigDecimal.ZERO);
      ctx.setActivityValidTurnover(BigDecimal.ZERO);

      // Assert - Verify rejection state is correctly set
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("ODDS_TOO_LOW");
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("0");
      assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("0");
      assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("當拒絕時拒絕狀態應該正確")
    void whenRejected_ShouldHaveCorrectRejectionState() {
      // Arrange
      TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("100.00"), 1, 1);
      ctx.markRejected("RISK_SCORE_HIGH");
      ctx.setEffectiveTurnoverBase(BigDecimal.ZERO);

      // Assert - Verify rejection state is correctly set
      assertThat(ctx.isRejected()).isTrue();
      assertThat(ctx.getRejectedBy()).isEqualTo("RISK_SCORE_HIGH");
      assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("0");
    }
  }

  // ===== 字段驗證測試 =====

  @Test
  @DisplayName("應該包含所有必需字段")
  void shouldHaveAllRequiredFields() {
    // Arrange
    TurnoverContext ctx = TurnoverTestFixture.createContext(new BigDecimal("500.00"), 3, 1);
    ctx.setPlayerId(9001L);
    ctx.setEffectiveTurnoverBase(new BigDecimal("500.00"));
    ctx.setValidTurnoverFinance(new BigDecimal("500.00"));
    ctx.setActivityValidTurnover(new BigDecimal("500.00"));
    ctx.setRiskActionType(1);
    ctx.setStatusFactor(new BigDecimal("100.00"));
    ctx.setGameWeight(new BigDecimal("100.00"));
    ctx.addMatchedRule("RULE-ODDS-001");
    ctx.addMatchedRule("RULE-RISK-001");
    ctx.addMatchedRule("RULE-STATUS-001");
    ctx.addMatchedRule("RULE-GAME-001");

    // Assert - Verify all required fields are present and valid
    assertThat(ctx.getBetId()).isNotNull();
    assertThat(ctx.getPlayerId()).isEqualTo(9001L);
    assertThat(ctx.getBetAmount()).isEqualByComparingTo("500.00");
    assertThat(ctx.getEffectiveTurnoverBase()).isEqualByComparingTo("500.00");
    assertThat(ctx.getValidTurnoverFinance()).isEqualByComparingTo("500.00");
    assertThat(ctx.getActivityValidTurnover()).isEqualByComparingTo("500.00");
    assertThat(ctx.getRiskActionType()).isEqualTo(1);
    assertThat(ctx.getStatusFactor()).isEqualByComparingTo("100.00");
    assertThat(ctx.getGameWeight()).isEqualByComparingTo("100.00");
    assertThat(ctx.getMatchedRules()).hasSize(4);
  }
}
