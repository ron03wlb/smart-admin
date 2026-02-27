package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Wallet transaction type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@AllArgsConstructor
@Getter
public enum TransactionTypeEnum implements BaseEnum {
  DEPOSIT(1, "存款"),
  WITHDRAW(2, "提款"),
  BET(3, "下注"),
  WIN(4, "派彩"),
  BONUS(5, "紅利"),
  ADJUSTMENT(6, "調整"),
  ROLLBACK(7, "回滾"),
  ;

  private final Integer value;
  private final String desc;
}
