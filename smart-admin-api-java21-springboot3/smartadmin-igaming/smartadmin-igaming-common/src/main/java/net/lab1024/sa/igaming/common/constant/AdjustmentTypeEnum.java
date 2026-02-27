package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Commission adjustment type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum AdjustmentTypeEnum implements BaseEnum {
  LATE_ARRIVAL(1, "遲到入帳"),
  ROLLBACK(2, "回滾"),
  MANUAL(3, "手動調整"),
  ;

  private final Integer value;
  private final String desc;
}
