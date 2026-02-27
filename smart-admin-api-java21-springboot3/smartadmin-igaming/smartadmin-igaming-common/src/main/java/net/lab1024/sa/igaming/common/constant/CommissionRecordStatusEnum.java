package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Commission record approval status.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@AllArgsConstructor
@Getter
public enum CommissionRecordStatusEnum implements BaseEnum {
  PENDING(1, "待審核"),
  APPROVED(2, "已批准"),
  REJECTED(3, "已拒絕"),
  ;

  private final Integer value;
  private final String desc;
}
