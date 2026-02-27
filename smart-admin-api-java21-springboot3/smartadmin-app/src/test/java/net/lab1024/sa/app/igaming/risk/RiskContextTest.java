package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import net.lab1024.sa.igaming.risk.domain.RiskContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RiskContext 測試")
class RiskContextTest {

  @Nested
  @DisplayName("addRuleScore 測試")
  class AddRuleScoreTest {

    @Test
    @DisplayName("分數限制在 0-100 範圍內")
    void score_clamped_to_0_100() {
      RiskContext ctx = new RiskContext();
      ctx.addRuleScore("test1", -10);
      ctx.addRuleScore("test2", 150);
      ctx.addRuleScore("test3", 50);

      assertThat(ctx.getRuleScores().get("test1")).isZero();
      assertThat(ctx.getRuleScores().get("test2")).isEqualTo(100);
      assertThat(ctx.getRuleScores().get("test3")).isEqualTo(50);
    }
  }

  @Nested
  @DisplayName("calculateTotalScore 測試")
  class CalculateTotalScoreTest {

    @Test
    @DisplayName("加權平均計算正確")
    void weighted_average_calculation() {
      RiskContext ctx = new RiskContext();
      ctx.addRuleScore("velocityCheck", 50);
      ctx.addRuleScore("amountThreshold", 0);

      Map<String, BigDecimal> weights =
          Map.of(
              "velocityCheck", new BigDecimal("0.20"),
              "amountThreshold", new BigDecimal("0.25"));

      // (50 * 0.20 + 0 * 0.25) / (0.20 + 0.25) = 10 / 0.45 = 22.22 -> 22
      int total = ctx.calculateTotalScore(weights);
      assertThat(total).isEqualTo(22);
    }

    @Test
    @DisplayName("無權重配置使用預設權重 1")
    void default_weight_when_missing() {
      RiskContext ctx = new RiskContext();
      ctx.addRuleScore("a", 60);
      ctx.addRuleScore("b", 40);

      // (60 * 1 + 40 * 1) / (1 + 1) = 50
      int total = ctx.calculateTotalScore(Map.of());
      assertThat(total).isEqualTo(50);
    }

    @Test
    @DisplayName("無分數返回 0")
    void no_scores_returns_zero() {
      RiskContext ctx = new RiskContext();
      int total = ctx.calculateTotalScore(Map.of("a", BigDecimal.ONE));
      assertThat(total).isZero();
    }

    @Test
    @DisplayName("單一元件分數等於自身")
    void single_component_equals_self() {
      RiskContext ctx = new RiskContext();
      ctx.addRuleScore("geoLocation", 80);

      int total = ctx.calculateTotalScore(Map.of("geoLocation", new BigDecimal("0.15")));
      assertThat(total).isEqualTo(80);
    }
  }
}
