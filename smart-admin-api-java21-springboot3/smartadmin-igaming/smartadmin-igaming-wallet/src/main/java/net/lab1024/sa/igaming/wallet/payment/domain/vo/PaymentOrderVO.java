package net.lab1024.sa.igaming.wallet.payment.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;

/**
 * Payment order view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PaymentOrderVO {

  @Schema(description = "Payment order ID")
  private Long paymentOrderId;

  @Schema(description = "Platform order number")
  private String orderNo;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Wallet ID")
  private Long walletId;

  @SchemaEnum(PaymentOrderTypeEnum.class)
  private Integer orderType;

  @Schema(description = "Order amount")
  private BigDecimal amount;

  @Schema(description = "Currency code")
  private String currencyCode;

  @SchemaEnum(PaymentOrderStatusEnum.class)
  private Integer status;

  @Schema(description = "PSP code")
  private String pspCode;

  @Schema(description = "PSP transaction ID")
  private String pspTransactionId;

  @Schema(description = "Description")
  private String description;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;

  @Schema(description = "Updated time")
  private OffsetDateTime updateTime;
}
