package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Risk level enumeration — severity classification of risk score.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum RiskLevelEnum implements BaseEnum {
  LOW(1, "低風險"),
  MEDIUM(2, "中風險"),
  HIGH(3, "高風險"),
  CRITICAL(4, "極高風險"),
  ;

  private final Integer value;
  private final String desc;
}
