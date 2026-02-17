package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Wallet type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@AllArgsConstructor
@Getter
public enum WalletTypeEnum implements BaseEnum {
  CASH(1, "現金錢包"),
  BONUS(2, "紅利錢包"),
  CREDIT(3, "信用錢包"),
  ;

  private final Integer value;
  private final String desc;
}
