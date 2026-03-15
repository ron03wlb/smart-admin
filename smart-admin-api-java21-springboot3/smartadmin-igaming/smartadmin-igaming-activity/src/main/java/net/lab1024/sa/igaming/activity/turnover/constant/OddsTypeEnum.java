package net.lab1024.sa.igaming.activity.turnover.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Odds type enumeration.
 *
 * <p>Represents different odds formats used in sports betting. Different regions prefer different
 * odds formats, and threshold rules must convert all odds to a standard format for comparison.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@AllArgsConstructor
@Getter
public enum OddsTypeEnum implements BaseEnum {
  EUR(1, "歐洲盤", "European (Decimal)"),
  HK(2, "香港盤", "Hong Kong"),
  MY(3, "馬來盤", "Malay"),
  ID(4, "印尼盤", "Indonesian"),
  ;

  private final Integer value;
  private final String desc;
  private final String englishDesc;

  /**
   * Get English description for API responses.
   *
   * @return English description of the odds type
   */
  public String getEnglishDesc() {
    return englishDesc;
  }
}
