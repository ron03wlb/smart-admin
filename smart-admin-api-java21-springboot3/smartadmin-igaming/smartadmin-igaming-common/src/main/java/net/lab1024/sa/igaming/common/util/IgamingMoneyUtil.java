package net.lab1024.sa.igaming.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * iGaming financial precision utility
 *
 * <p>Enforces DECIMAL(19,4) scale with RoundingMode.HALF_EVEN (Banker's rounding) for all monetary
 * calculations in the iGaming platform.
 *
 * <p>Differences from {@link net.lab1024.sa.common.core.util.SmartBigDecimalUtil}:
 *
 * <ul>
 *   <li>Scale: 4 decimals (vs 2 in SmartBigDecimalUtil)
 *   <li>Rounding: HALF_EVEN / Banker's rounding (vs HALF_UP)
 *   <li>Null-safe: null treated as ZERO
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-02-16
 */
public final class IgamingMoneyUtil {

  /** Standard scale for iGaming monetary amounts: DECIMAL(19,4) */
  public static final int SCALE = 4;

  /** Banker's rounding — industry standard for regulated gaming */
  public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

  /** Zero with proper scale */
  public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

  public static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

  private IgamingMoneyUtil() {}

  // ==================== Arithmetic ====================

  /**
   * Addition: a + b (null-safe)
   *
   * @param a first operand (null treated as ZERO)
   * @param b second operand (null treated as ZERO)
   * @return sum with scale=4 and HALF_EVEN rounding
   */
  public static BigDecimal add(BigDecimal a, BigDecimal b) {
    return setScale(nullToZero(a).add(nullToZero(b)));
  }

  /**
   * Subtraction: a - b (null-safe)
   *
   * @param a minuend (null treated as ZERO)
   * @param b subtrahend (null treated as ZERO)
   * @return difference with scale=4 and HALF_EVEN rounding
   */
  public static BigDecimal subtract(BigDecimal a, BigDecimal b) {
    return setScale(nullToZero(a).subtract(nullToZero(b)));
  }

  /**
   * Multiplication: a × b (null-safe)
   *
   * @param a first factor (null treated as ZERO)
   * @param b second factor (null treated as ZERO)
   * @return product with scale=4 and HALF_EVEN rounding
   */
  public static BigDecimal multiply(BigDecimal a, BigDecimal b) {
    return setScale(nullToZero(a).multiply(nullToZero(b)));
  }

  /**
   * Division: a ÷ b (null-safe for numerator)
   *
   * @param a dividend (null treated as ZERO)
   * @param b divisor (must not be null or zero)
   * @return quotient with scale=4 and HALF_EVEN rounding
   * @throws ArithmeticException if b is zero
   */
  public static BigDecimal divide(BigDecimal a, BigDecimal b) {
    return nullToZero(a).divide(b, SCALE, ROUNDING);
  }

  // ==================== Comparison ====================

  public static boolean isGreaterThan(BigDecimal a, BigDecimal b) {
    return nullToZero(a).compareTo(nullToZero(b)) > 0;
  }

  public static boolean isGreaterOrEqual(BigDecimal a, BigDecimal b) {
    return nullToZero(a).compareTo(nullToZero(b)) >= 0;
  }

  public static boolean isLessThan(BigDecimal a, BigDecimal b) {
    return nullToZero(a).compareTo(nullToZero(b)) < 0;
  }

  public static boolean isLessOrEqual(BigDecimal a, BigDecimal b) {
    return nullToZero(a).compareTo(nullToZero(b)) <= 0;
  }

  public static boolean equals(BigDecimal a, BigDecimal b) {
    return nullToZero(a).compareTo(nullToZero(b)) == 0;
  }

  // ==================== Validation ====================

  public static boolean isZero(BigDecimal v) {
    return nullToZero(v).compareTo(BigDecimal.ZERO) == 0;
  }

  public static boolean isPositive(BigDecimal v) {
    return nullToZero(v).compareTo(BigDecimal.ZERO) > 0;
  }

  public static boolean isNegative(BigDecimal v) {
    return nullToZero(v).compareTo(BigDecimal.ZERO) < 0;
  }

  /**
   * Validate that the value is non-negative
   *
   * @param v the value to check
   * @param fieldName field name for error message
   * @throws IllegalArgumentException if the value is negative
   */
  public static void requireNonNegative(BigDecimal v, String fieldName) {
    if (isNegative(v)) {
      throw new IllegalArgumentException(fieldName + " must not be negative, got: " + v);
    }
  }

  // ==================== Utility ====================

  /**
   * Normalize to scale=4 with HALF_EVEN rounding
   *
   * @param v the value to normalize
   * @return value with scale=4
   */
  public static BigDecimal setScale(BigDecimal v) {
    return v.setScale(SCALE, ROUNDING);
  }

  /**
   * Convert null to ZERO
   *
   * @param v the value (may be null)
   * @return the value, or ZERO if null
   */
  public static BigDecimal nullToZero(BigDecimal v) {
    return v != null ? v : ZERO;
  }

  /**
   * Calculate percentage: (numerator / denominator) × 100, with 2 decimal places
   *
   * @param numerator the numerator (null treated as ZERO)
   * @param denominator the denominator
   * @return percentage with 2 decimal places, or ZERO if denominator is zero
   */
  public static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    if (denominator == null || isZero(denominator)) {
      return BigDecimal.ZERO.setScale(2, ROUNDING);
    }
    return nullToZero(numerator)
        .divide(denominator, SCALE + 2, ROUNDING)
        .multiply(ONE_HUNDRED)
        .setScale(2, ROUNDING);
  }
}
