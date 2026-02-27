package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Player account status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum PlayerStatusEnum implements BaseEnum {
  ACTIVE(1, "活躍"),
  LOCKED(2, "鎖定"),
  SUSPENDED(3, "停權"),
  PENDING_VERIFICATION(4, "待驗證"),
  CLOSED(5, "關閉"),
  SELF_EXCLUDED(6, "自我排除"),
  ;

  private final Integer value;
  private final String desc;
}
