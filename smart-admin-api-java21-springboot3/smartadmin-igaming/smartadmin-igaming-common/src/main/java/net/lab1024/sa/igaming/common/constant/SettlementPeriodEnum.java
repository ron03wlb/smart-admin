package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Settlement period enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum SettlementPeriodEnum implements BaseEnum {
  WEEKLY(1, "週結"),
  MONTHLY(2, "月結"),
  ;

  private final Integer value;
  private final String desc;
}
