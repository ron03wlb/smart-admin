package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;

/**
 * Wallet transaction paginated query form.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WalletTransactionQueryForm extends PageParam {

  @Schema(description = "Wallet ID")
  @NotNull(message = "walletId cannot be null")
  private Long walletId;

  @SchemaEnum(TransactionTypeEnum.class)
  @CheckEnum(
      message = "transactionType invalid",
      value = TransactionTypeEnum.class,
      required = false)
  private Integer transactionType;

  @Schema(description = "Start time (inclusive)")
  private OffsetDateTime startTime;

  @Schema(description = "End time (exclusive)")
  private OffsetDateTime endTime;
}
