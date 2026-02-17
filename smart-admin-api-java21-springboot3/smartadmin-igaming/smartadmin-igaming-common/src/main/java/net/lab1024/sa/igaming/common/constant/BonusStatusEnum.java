package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Bonus status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@AllArgsConstructor
@Getter
public enum BonusStatusEnum implements BaseEnum {
  ACTIVE(1, "有效"),
  EXPIRED(2, "過期"),
  COMPLETED(3, "完成"),
  FORFEITED(4, "沒收"),
  ;

  private final Integer value;
  private final String desc;
}
