package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Commission plan type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum CommissionPlanTypeEnum implements BaseEnum {
  REVENUE_SHARE(1, "營收分成"),
  TURNOVER_REBATE(2, "流水返佣"),
  CPA(3, "CPA"),
  ;

  private final Integer value;
  private final String desc;
}
