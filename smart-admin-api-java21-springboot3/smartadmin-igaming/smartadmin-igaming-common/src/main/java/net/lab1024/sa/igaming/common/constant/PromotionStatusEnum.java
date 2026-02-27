package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Promotion status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum PromotionStatusEnum implements BaseEnum {
  ACTIVE(1, "有效"),
  PAUSED(2, "暫停"),
  EXPIRED(3, "過期"),
  ;

  private final Integer value;
  private final String desc;

  public static PromotionStatusEnum of(Integer value) {
    if (value == null) {
      return null;
    }
    for (PromotionStatusEnum e : values()) {
      if (e.value.equals(value)) {
        return e;
      }
    }
    return null;
  }
}
