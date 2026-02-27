package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Reconciliation status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum ReconciliationStatusEnum implements BaseEnum {
  PENDING(1, "待核對"),
  VERIFIED(2, "已驗證"),
  MISMATCH(3, "不符"),
  COMPENSATED(4, "已補償"),
  RECONCILED(5, "已調和"),
  ;

  private final Integer value;
  private final String desc;
}
