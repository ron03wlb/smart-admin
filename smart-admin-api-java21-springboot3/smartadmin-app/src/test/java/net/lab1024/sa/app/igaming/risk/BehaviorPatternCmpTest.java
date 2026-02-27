package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BehaviorPatternCmp 測試")
class BehaviorPatternCmpTest {

  @Test
  @DisplayName("all-in (bet/balance >= 0.90) 得分 45")
  void all_in_score_45() {
    int score = calculateScore(new BigDecimal("950"), new BigDecimal("1000"));
    assertThat(score).isEqualTo(45);
  }

  @Test
  @DisplayName("高比例 (bet/balance >= 0.80) 得分 25")
  void high_ratio_score_25() {
    int score = calculateScore(new BigDecimal("850"), new BigDecimal("1000"));
    assertThat(score).isEqualTo(25);
  }

  @Test
  @DisplayName("正常比例 得分 0")
  void normal_ratio_score_0() {
    int score = calculateScore(new BigDecimal("100"), new BigDecimal("1000"));
    assertThat(score).isZero();
  }

  @Test
  @DisplayName("餘額為零 得分 0")
  void zero_balance_score_0() {
    int score = calculateScore(new BigDecimal("100"), BigDecimal.ZERO);
    assertThat(score).isZero();
  }

  @Test
  @DisplayName("金額或餘額為 null 得分 0")
  void null_values_score_0() {
    assertThat(calculateScore(null, new BigDecimal("1000"))).isZero();
    assertThat(calculateScore(new BigDecimal("100"), null)).isZero();
  }

  /** Mirrors BehaviorPatternCmp.process() logic. */
  private int calculateScore(BigDecimal amount, BigDecimal balance) {
    if (amount == null || balance == null || balance.compareTo(BigDecimal.ZERO) <= 0) {
      return 0;
    }
    BigDecimal ratio = amount.divide(balance, 4, java.math.RoundingMode.HALF_UP);
    if (ratio.compareTo(new BigDecimal("0.90")) >= 0) {
      return 45;
    } else if (ratio.compareTo(new BigDecimal("0.80")) >= 0) {
      return 25;
    }
    return 0;
  }
}
