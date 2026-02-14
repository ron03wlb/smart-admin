package net.lab1024.sa.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SmartBigDecimalUtil 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>基本運算（加減乘除）
 *   <li>Amount 內部類（金額計算）
 *   <li>比較方法
 *   <li>百分比計算
 *   <li>max/min 方法
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("SmartBigDecimalUtil 單元測試")
class SmartBigDecimalUtilTest {

  // ==================== 基本運算測試 ====================

  @Nested
  @DisplayName("基本運算測試")
  class BasicOperationsTest {

    @Test
    @DisplayName("add：加法運算保留指定小數位")
    void add_shouldAddWithScale() {
      // Given
      BigDecimal num1 = new BigDecimal("10.123");
      BigDecimal num2 = new BigDecimal("5.456");

      // When
      BigDecimal result = SmartBigDecimalUtil.add(num1, num2, 2);

      // Then
      assertThat(result).isEqualByComparingTo("15.58");
    }

    @Test
    @DisplayName("add：可變參數累加")
    void add_shouldSumVarargs() {
      // Given
      BigDecimal num1 = new BigDecimal("10");
      BigDecimal num2 = new BigDecimal("20");
      BigDecimal num3 = new BigDecimal("30");

      // When
      BigDecimal result = SmartBigDecimalUtil.add(2, num1, num2, num3);

      // Then
      assertThat(result).isEqualByComparingTo("60.00");
    }

    @Test
    @DisplayName("add：null 值視為零")
    void add_shouldTreatNullAsZero() {
      // Given
      BigDecimal num1 = new BigDecimal("10");

      // When
      BigDecimal result = SmartBigDecimalUtil.add(2, num1, null, null);

      // Then
      assertThat(result).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("multiply：乘法運算")
    void multiply_shouldMultiplyWithScale() {
      // Given
      BigDecimal num1 = new BigDecimal("10.5");
      BigDecimal num2 = new BigDecimal("2");

      // When
      BigDecimal result = SmartBigDecimalUtil.multiply(num1, num2, 2);

      // Then
      assertThat(result).isEqualByComparingTo("21.00");
    }

    @Test
    @DisplayName("subtract：減法運算")
    void subtract_shouldSubtractWithScale() {
      // Given
      BigDecimal num1 = new BigDecimal("100.00");
      BigDecimal num2 = new BigDecimal("30.50");

      // When
      BigDecimal result = SmartBigDecimalUtil.subtract(num1, num2, 2);

      // Then
      assertThat(result).isEqualByComparingTo("69.50");
    }

    @Test
    @DisplayName("divide：除法運算")
    void divide_shouldDivideWithRounding() {
      // Given
      BigDecimal num1 = new BigDecimal("100");
      BigDecimal num2 = new BigDecimal("3");

      // When
      BigDecimal result = SmartBigDecimalUtil.divide(num1, num2, 2);

      // Then
      assertThat(result).isEqualByComparingTo("33.33");
    }
  }

  // ==================== Amount 內部類測試 ====================

  @Nested
  @DisplayName("Amount 金額計算測試")
  class AmountTest {

    @Test
    @DisplayName("Amount.add：金額加法保留2位")
    void amountAdd_shouldKeepTwoDecimals() {
      // Given
      BigDecimal num1 = new BigDecimal("99.99");
      BigDecimal num2 = new BigDecimal("0.015");

      // When
      BigDecimal result = SmartBigDecimalUtil.Amount.add(num1, num2);

      // Then
      assertThat(result).isEqualByComparingTo("100.01");
    }

    @Test
    @DisplayName("Amount.multiply：金額乘法")
    void amountMultiply_shouldKeepTwoDecimals() {
      // Given
      BigDecimal price = new BigDecimal("19.99");
      BigDecimal quantity = new BigDecimal("3");

      // When
      BigDecimal result = SmartBigDecimalUtil.Amount.multiply(price, quantity);

      // Then
      assertThat(result).isEqualByComparingTo("59.97");
    }

    @Test
    @DisplayName("Amount.subtract：金額減法")
    void amountSubtract_shouldKeepTwoDecimals() {
      // Given
      BigDecimal total = new BigDecimal("100.00");
      BigDecimal discount = new BigDecimal("15.50");

      // When
      BigDecimal result = SmartBigDecimalUtil.Amount.subtract(total, discount);

      // Then
      assertThat(result).isEqualByComparingTo("84.50");
    }

    @Test
    @DisplayName("Amount.divide：金額除法")
    void amountDivide_shouldKeepTwoDecimals() {
      // Given
      BigDecimal total = new BigDecimal("100.00");
      BigDecimal parts = new BigDecimal("3");

      // When
      BigDecimal result = SmartBigDecimalUtil.Amount.divide(total, parts);

      // Then
      assertThat(result).isEqualByComparingTo("33.33");
    }
  }

  // ==================== 比較方法測試 ====================

  @Nested
  @DisplayName("比較方法測試")
  class ComparisonTest {

    @Test
    @DisplayName("isGreaterThan：大於比較")
    void isGreaterThan_shouldCompareCorrectly() {
      // Given
      BigDecimal num1 = new BigDecimal("100");
      BigDecimal num2 = new BigDecimal("50");

      // Then
      assertThat(SmartBigDecimalUtil.isGreaterThan(num1, num2)).isTrue();
      assertThat(SmartBigDecimalUtil.isGreaterThan(num2, num1)).isFalse();
      assertThat(SmartBigDecimalUtil.isGreaterThan(num1, num1)).isFalse();
    }

    @Test
    @DisplayName("isGreaterOrEqual：大於等於比較")
    void isGreaterOrEqual_shouldCompareCorrectly() {
      // Given
      BigDecimal num1 = new BigDecimal("100");
      BigDecimal num2 = new BigDecimal("50");

      // Then
      assertThat(SmartBigDecimalUtil.isGreaterOrEqual(num1, num2)).isTrue();
      assertThat(SmartBigDecimalUtil.isGreaterOrEqual(num1, num1)).isTrue();
      assertThat(SmartBigDecimalUtil.isGreaterOrEqual(num2, num1)).isFalse();
    }

    @Test
    @DisplayName("isLessThan：小於比較")
    void isLessThan_shouldCompareCorrectly() {
      // Given
      BigDecimal num1 = new BigDecimal("50");
      BigDecimal num2 = new BigDecimal("100");

      // Then
      assertThat(SmartBigDecimalUtil.isLessThan(num1, num2)).isTrue();
      assertThat(SmartBigDecimalUtil.isLessThan(num2, num1)).isFalse();
      assertThat(SmartBigDecimalUtil.isLessThan(num1, num1)).isFalse();
    }

    @Test
    @DisplayName("isLessOrEqual：小於等於比較")
    void isLessOrEqual_shouldCompareCorrectly() {
      // Given
      BigDecimal num1 = new BigDecimal("50");
      BigDecimal num2 = new BigDecimal("100");

      // Then
      assertThat(SmartBigDecimalUtil.isLessOrEqual(num1, num2)).isTrue();
      assertThat(SmartBigDecimalUtil.isLessOrEqual(num1, num1)).isTrue();
      assertThat(SmartBigDecimalUtil.isLessOrEqual(num2, num1)).isFalse();
    }

    @Test
    @DisplayName("equals：相等比較")
    void equals_shouldCompareCorrectly() {
      // Given
      BigDecimal num1 = new BigDecimal("100.00");
      BigDecimal num2 = new BigDecimal("100");
      BigDecimal num3 = new BigDecimal("99");

      // Then
      assertThat(SmartBigDecimalUtil.equals(num1, num2)).isTrue();
      assertThat(SmartBigDecimalUtil.equals(num1, num3)).isFalse();
    }
  }

  // ==================== 百分比計算測試 ====================

  @Nested
  @DisplayName("百分比計算測試")
  class PercentTest {

    @Test
    @DisplayName("percent：百分比計算")
    void percent_shouldCalculatePercentage() {
      // Given
      BigDecimal num1 = new BigDecimal("25");
      BigDecimal num2 = new BigDecimal("100");

      // When
      BigDecimal result = SmartBigDecimalUtil.percent(num1, num2, 2);

      // Then
      assertThat(result).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("percent：分母為零返回零")
    void percent_shouldReturnZeroWhenDivideByZero() {
      // Given
      BigDecimal num1 = new BigDecimal("25");
      BigDecimal num2 = BigDecimal.ZERO;

      // When
      BigDecimal result = SmartBigDecimalUtil.percent(num1, num2, 2);

      // Then
      assertThat(result).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("percent：Integer 重載方法")
    void percent_shouldWorkWithIntegers() {
      // When
      BigDecimal result = SmartBigDecimalUtil.percent(50, 200, 2);

      // Then
      assertThat(result).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("percent：null 參數返回零")
    void percent_shouldReturnZeroForNull() {
      // When
      BigDecimal result = SmartBigDecimalUtil.percent(null, 100, 2);

      // Then
      assertThat(result).isEqualByComparingTo("0");
    }
  }

  // ==================== max/min 測試 ====================

  @Nested
  @DisplayName("max/min 測試")
  class MaxMinTest {

    @Test
    @DisplayName("max：返回較大值")
    void max_shouldReturnLarger() {
      // Given
      BigDecimal num1 = new BigDecimal("100");
      BigDecimal num2 = new BigDecimal("200");

      // Then
      assertThat(SmartBigDecimalUtil.max(num1, num2)).isEqualByComparingTo("200");
      assertThat(SmartBigDecimalUtil.max(num2, num1)).isEqualByComparingTo("200");
    }

    @Test
    @DisplayName("min：返回較小值")
    void min_shouldReturnSmaller() {
      // Given
      BigDecimal num1 = new BigDecimal("100");
      BigDecimal num2 = new BigDecimal("200");

      // Then
      assertThat(SmartBigDecimalUtil.min(num1, num2)).isEqualByComparingTo("100");
      assertThat(SmartBigDecimalUtil.min(num2, num1)).isEqualByComparingTo("100");
    }
  }

  // ==================== setScale 測試 ====================

  @Nested
  @DisplayName("setScale 測試")
  class SetScaleTest {

    @Test
    @DisplayName("setScale：四捨五入")
    void setScale_shouldRoundHalfUp() {
      // Given
      BigDecimal num = new BigDecimal("10.125");

      // When
      BigDecimal result = SmartBigDecimalUtil.setScale(num, 2);

      // Then
      assertThat(result).isEqualByComparingTo("10.13");
    }
  }
}
