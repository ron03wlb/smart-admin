package net.lab1024.sa.igaming.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * Payment order status enumeration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@AllArgsConstructor
@Getter
public enum PaymentOrderStatusEnum implements BaseEnum {
  PENDING(1, "待處理"),
  PROCESSING(2, "處理中"),
  SUCCESS(3, "成功"),
  FAILED(4, "失敗"),
  REJECTED(5, "拒絕"),
  REFUNDED(6, "已退款"),
  CANCELLED(7, "已取消"),
  ;

  private final Integer value;
  private final String desc;
}
