package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Settlement payment verification status.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum PaymentVerifyStatusEnum implements BaseEnum {
  PENDING(1, "待付"),
  VERIFIED(2, "已驗證"),
  OVERDUE(3, "逾期"),
  ;

  private final Integer value;
  private final String desc;
}
