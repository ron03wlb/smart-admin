package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * VIP level enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum VipLevelEnum implements BaseEnum {
  BRONZE(1, "銅牌"),
  SILVER(2, "銀牌"),
  GOLD(3, "金牌"),
  PLATINUM(4, "白金"),
  DIAMOND(5, "鑽石"),
  ;

  private final Integer value;
  private final String desc;
}
