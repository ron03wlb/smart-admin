package net.lab1024.sa.app.igaming;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.common.util.IgamingMoneyUtil;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for iGaming financial precision utility.
 *
 * @since 2026-02-17
 */
class IgamingMoneyUtilTest {

  // ==================== Arithmetic ====================

  @Test
  void addWithPrecision() {
    BigDecimal result = IgamingMoneyUtil.add(new BigDecimal("100.1234"), new BigDecimal("50.5678"));
    assertEquals(new BigDecimal("150.6912"), result);
    assertEquals(4, result.scale());
  }

  @Test
  void addNullSafety() {
    BigDecimal result = IgamingMoneyUtil.add(null, new BigDecimal("50.0000"));
    assertEquals(new BigDecimal("50.0000"), result);

    result = IgamingMoneyUtil.add(new BigDecimal("50.0000"), null);
    assertEquals(new BigDecimal("50.0000"), result);

    result = IgamingMoneyUtil.add(null, null);
    assertEquals(0, result.compareTo(BigDecimal.ZERO));
  }

  @Test
  void subtractWithPrecision() {
    BigDecimal result =
        IgamingMoneyUtil.subtract(new BigDecimal("100.5000"), new BigDecimal("30.2500"));
    assertEquals(new BigDecimal("70.2500"), result);
  }

  @Test
  void multiplyWithPrecision() {
    BigDecimal result =
        IgamingMoneyUtil.multiply(new BigDecimal("10.0000"), new BigDecimal("3.5000"));
    assertEquals(new BigDecimal("35.0000"), result);
  }

  @Test
  void divideWithBankersRounding() {
    // 10 / 3 = 3.3333... with HALF_EVEN
    BigDecimal result = IgamingMoneyUtil.divide(new BigDecimal("10"), new BigDecimal("3"));
    assertEquals(new BigDecimal("3.3333"), result);
  }

  @Test
  void divideByZeroThrows() {
    assertThrows(
        ArithmeticException.class,
        () -> IgamingMoneyUtil.divide(new BigDecimal("10"), BigDecimal.ZERO));
  }

  // ==================== Comparison ====================

  @Test
  void comparisonOperations() {
    BigDecimal a = new BigDecimal("100.0000");
    BigDecimal b = new BigDecimal("50.0000");

    assertTrue(IgamingMoneyUtil.isGreaterThan(a, b));
    assertTrue(IgamingMoneyUtil.isGreaterOrEqual(a, b));
    assertFalse(IgamingMoneyUtil.isLessThan(a, b));
    assertFalse(IgamingMoneyUtil.isLessOrEqual(a, b));
    assertFalse(IgamingMoneyUtil.equals(a, b));

    assertTrue(IgamingMoneyUtil.equals(a, new BigDecimal("100.0000")));
    assertTrue(IgamingMoneyUtil.isGreaterOrEqual(a, a));
    assertTrue(IgamingMoneyUtil.isLessOrEqual(a, a));
  }

  @Test
  void comparisonWithNull() {
    BigDecimal positive = new BigDecimal("10.0000");

    assertTrue(IgamingMoneyUtil.isGreaterThan(positive, null));
    assertFalse(IgamingMoneyUtil.isGreaterThan(null, positive));
    assertTrue(IgamingMoneyUtil.equals(null, null));
  }

  // ==================== Validation ====================

  @Test
  void isZeroChecks() {
    assertTrue(IgamingMoneyUtil.isZero(BigDecimal.ZERO));
    assertTrue(IgamingMoneyUtil.isZero(new BigDecimal("0.0000")));
    assertTrue(IgamingMoneyUtil.isZero(null));
    assertFalse(IgamingMoneyUtil.isZero(new BigDecimal("0.0001")));
  }

  @Test
  void isPositiveChecks() {
    assertTrue(IgamingMoneyUtil.isPositive(new BigDecimal("0.0001")));
    assertFalse(IgamingMoneyUtil.isPositive(BigDecimal.ZERO));
    assertFalse(IgamingMoneyUtil.isPositive(new BigDecimal("-1")));
  }

  @Test
  void isNegativeChecks() {
    assertTrue(IgamingMoneyUtil.isNegative(new BigDecimal("-0.0001")));
    assertFalse(IgamingMoneyUtil.isNegative(BigDecimal.ZERO));
    assertFalse(IgamingMoneyUtil.isNegative(new BigDecimal("1")));
  }

  @Test
  void requireNonNegativePassesForZeroAndPositive() {
    assertDoesNotThrow(() -> IgamingMoneyUtil.requireNonNegative(BigDecimal.ZERO, "balance"));
    assertDoesNotThrow(() -> IgamingMoneyUtil.requireNonNegative(new BigDecimal("100"), "balance"));
  }

  @Test
  void requireNonNegativeThrowsForNegative() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> IgamingMoneyUtil.requireNonNegative(new BigDecimal("-1"), "balance"));
    assertTrue(ex.getMessage().contains("balance"));
  }

  // ==================== Utility ====================

  @Test
  void percentCalculation() {
    BigDecimal result = IgamingMoneyUtil.percent(new BigDecimal("75"), new BigDecimal("100"));
    assertEquals(new BigDecimal("75.00"), result);
  }

  @Test
  void percentWithZeroDenominator() {
    BigDecimal result = IgamingMoneyUtil.percent(new BigDecimal("75"), BigDecimal.ZERO);
    assertEquals(0, result.compareTo(BigDecimal.ZERO));
  }

  @Test
  void percentWithNullDenominator() {
    BigDecimal result = IgamingMoneyUtil.percent(new BigDecimal("75"), null);
    assertEquals(0, result.compareTo(BigDecimal.ZERO));
  }

  @Test
  void setScaleEnforces4Decimals() {
    BigDecimal result = IgamingMoneyUtil.setScale(new BigDecimal("123.456789"));
    assertEquals(4, result.scale());
  }
}
