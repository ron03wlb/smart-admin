package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Payment order type enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum PaymentOrderTypeEnum implements BaseEnum {
  DEPOSIT(1, "存款"),
  WITHDRAWAL(2, "提款"),
  ;

  private final Integer value;
  private final String desc;
}
