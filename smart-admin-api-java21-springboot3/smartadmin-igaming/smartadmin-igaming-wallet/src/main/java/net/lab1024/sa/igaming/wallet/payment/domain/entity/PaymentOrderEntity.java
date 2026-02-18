package net.lab1024.sa.igaming.wallet.payment.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Payment order entity — unified deposit/withdrawal order.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_payment_order")
public class PaymentOrderEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long paymentOrderId;

  private String orderNo;

  private Long playerId;

  private Long walletId;

  /**
   * Order type: 1=DEPOSIT, 2=WITHDRAWAL. See {@link
   * net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum}.
   */
  private Integer orderType;

  /** Order amount with DECIMAL(19,4) precision. */
  private BigDecimal amount;

  private String currencyCode;

  /** Order status. See {@link net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum}. */
  private Integer status;

  private String pspCode;

  private String pspTransactionId;

  private String redirectUrl;

  private String callbackPayload;

  private String requestId;

  private String description;

  /** Optimistic lock version. */
  @Version private Integer version;
}
