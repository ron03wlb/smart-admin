package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Bonus record status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum BonusRecordStatusEnum implements BaseEnum {
  PENDING(1, "待啟用"),
  ACTIVE(2, "進行中"),
  COMPLETED(3, "已完成"),
  EXPIRED(4, "已過期"),
  FORFEITED(5, "已沒收"),
  ;

  private final Integer value;
  private final String desc;
}
