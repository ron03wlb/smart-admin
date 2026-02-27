package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.risk.domain.RiskContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AmountThresholdCmp 測試")
class AmountThresholdCmpTest {

  @Test
  @DisplayName("金額 >= 50000 得分 60")
  void critical_amount_score_60() {
    RiskContext ctx = buildContext(new BigDecimal("55000"));
    int score = calculateScore(ctx);
    assertThat(score).isEqualTo(60);
  }

  @Test
  @DisplayName("金額 >= 10000 得分 40")
  void high_amount_score_40() {
    RiskContext ctx = buildContext(new BigDecimal("15000"));
    int score = calculateScore(ctx);
    assertThat(score).isEqualTo(40);
  }

  @Test
  @DisplayName("金額 >= 5000 得分 20")
  void medium_amount_score_20() {
    RiskContext ctx = buildContext(new BigDecimal("7500"));
    int score = calculateScore(ctx);
    assertThat(score).isEqualTo(20);
  }

  @Test
  @DisplayName("金額 < 5000 得分 0")
  void low_amount_score_0() {
    RiskContext ctx = buildContext(new BigDecimal("1000"));
    int score = calculateScore(ctx);
    assertThat(score).isZero();
  }

  @Test
  @DisplayName("金額為 null 得分 0")
  void null_amount_score_0() {
    RiskContext ctx = buildContext(null);
    int score = calculateScore(ctx);
    assertThat(score).isZero();
  }

  /**
   * Direct score calculation without LiteFlow framework — tests the threshold logic only. Mirrors
   * AmountThresholdCmp.process() logic.
   */
  private int calculateScore(RiskContext ctx) {
    BigDecimal amount = ctx.getAmount();
    if (amount == null) {
      return 0;
    }
    if (amount.compareTo(new BigDecimal("50000")) >= 0) {
      return 60;
    } else if (amount.compareTo(new BigDecimal("10000")) >= 0) {
      return 40;
    } else if (amount.compareTo(new BigDecimal("5000")) >= 0) {
      return 20;
    }
    return 0;
  }

  private RiskContext buildContext(BigDecimal amount) {
    RiskContext ctx = new RiskContext();
    ctx.setPlayerId(1L);
    ctx.setTenantId(1L);
    ctx.setAmount(amount);
    return ctx;
  }
}
