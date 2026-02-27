package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Promotion type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum PromotionTypeEnum implements BaseEnum {
  FIRST_DEPOSIT(1, "首存"),
  RELOAD(2, "續存"),
  WAGERING_REBATE(3, "流水回饋"),
  ACTIVITY(4, "活動"),
  FREE_SPIN(5, "免費旋轉"),
  ;

  private final Integer value;
  private final String desc;
}
