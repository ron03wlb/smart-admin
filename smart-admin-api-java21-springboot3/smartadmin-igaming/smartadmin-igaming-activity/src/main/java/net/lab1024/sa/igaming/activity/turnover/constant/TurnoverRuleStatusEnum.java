package net.lab1024.sa.igaming.activity.turnover.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Turnover rule status enumeration.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@AllArgsConstructor
@Getter
public enum TurnoverRuleStatusEnum implements BaseEnum {
  ENABLED(1, "啟用"),
  DISABLED(2, "禁用"),
  EXPIRED(3, "已過期"),
  ;

  private final Integer value;
  private final String desc;
}
