package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Settlement phase enumeration — 4-phase weekly settlement workflow.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum SettlementPhaseEnum implements BaseEnum {
  FREEZE_CALCULATE(1, "凍結計算"),
  COLLECTION(2, "收款"),
  VERIFICATION(3, "驗證"),
  REPORT(4, "報表"),
  ;

  private final Integer value;
  private final String desc;
}
