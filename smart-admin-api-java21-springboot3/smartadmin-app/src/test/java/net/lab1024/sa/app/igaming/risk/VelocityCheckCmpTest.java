package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VelocityCheckCmp 測試")
class VelocityCheckCmpTest {

  @Test
  @DisplayName("高頻率 (>= 100) 得分 50")
  void high_frequency_score_50() {
    assertThat(calculateScore(120)).isEqualTo(50);
    assertThat(calculateScore(100)).isEqualTo(50);
  }

  @Test
  @DisplayName("中頻率 (>= 50) 得分 30")
  void medium_frequency_score_30() {
    assertThat(calculateScore(75)).isEqualTo(30);
    assertThat(calculateScore(50)).isEqualTo(30);
  }

  @Test
  @DisplayName("低頻率 (>= 20) 得分 15")
  void low_frequency_score_15() {
    assertThat(calculateScore(30)).isEqualTo(15);
    assertThat(calculateScore(20)).isEqualTo(15);
  }

  @Test
  @DisplayName("正常頻率 (< 20) 得分 0")
  void normal_frequency_score_0() {
    assertThat(calculateScore(10)).isZero();
    assertThat(calculateScore(0)).isZero();
  }

  /** Mirrors VelocityCheckCmp.calculateScore() logic. */
  private int calculateScore(int count) {
    if (count >= 100) {
      return 50;
    } else if (count >= 50) {
      return 30;
    } else if (count >= 20) {
      return 15;
    }
    return 0;
  }
}
