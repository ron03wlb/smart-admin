package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Game round status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum RoundStatusEnum implements BaseEnum {
  OPEN(1, "進行中"),
  SETTLED(2, "已結算"),
  CANCELLED(3, "已取消"),
  VOIDED(4, "已作廢"),
  TIMEOUT(5, "逾時"),
  ADJUSTED(6, "已調整"),
  PENDING_REVIEW(7, "待審核"),
  ;

  private final Integer value;
  private final String desc;
}
