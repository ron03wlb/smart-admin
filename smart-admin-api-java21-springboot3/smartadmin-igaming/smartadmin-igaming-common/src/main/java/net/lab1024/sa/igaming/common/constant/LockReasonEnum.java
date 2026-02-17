package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Wallet lock reason enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@AllArgsConstructor
@Getter
public enum LockReasonEnum implements BaseEnum {
  BET_PENDING(1, "下注待結算"),
  WITHDRAWAL_PENDING(2, "提款待審核"),
  FRAUD_HOLD(3, "風控凍結"),
  ;

  private final Integer value;
  private final String desc;
}
