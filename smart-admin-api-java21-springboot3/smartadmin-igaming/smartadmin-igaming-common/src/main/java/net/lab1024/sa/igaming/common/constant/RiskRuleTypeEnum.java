package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Risk rule type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum RiskRuleTypeEnum implements BaseEnum {
  VELOCITY(1, "速率限制"),
  AMOUNT(2, "金額閾值"),
  DEVICE(3, "設備指紋"),
  BEHAVIOR(4, "行為模式"),
  GEO(5, "地理限制"),
  ;

  private final Integer value;
  private final String desc;
}
