package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Reconciliation exception type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum ReconciliationExceptionTypeEnum implements BaseEnum {
  AMOUNT_MISMATCH(1, "金額不符"),
  MISSING_ON_PLATFORM(2, "平台缺失"),
  MISSING_ON_PSP(3, "PSP 缺失"),
  STATUS_MISMATCH(4, "狀態不符"),
  ;

  private final Integer value;
  private final String desc;
}
