package net.lab1024.sa.igaming.wallet.payment.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;

/**
 * Payment order paginated query form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PaymentOrderQueryForm extends PageParam {

  @Schema(description = "Player ID")
  private Long playerId;

  @SchemaEnum(PaymentOrderTypeEnum.class)
  @CheckEnum(message = "orderType invalid", value = PaymentOrderTypeEnum.class, required = false)
  private Integer orderType;

  @SchemaEnum(PaymentOrderStatusEnum.class)
  @CheckEnum(message = "status invalid", value = PaymentOrderStatusEnum.class, required = false)
  private Integer status;

  @Schema(description = "PSP code")
  private String pspCode;
}
