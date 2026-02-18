package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.LockReasonEnum;

/**
 * Form for locking (freezing) funds in a wallet.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletLockForm {

  @Schema(description = "Wallet ID")
  @NotNull(message = "walletId cannot be null")
  private Long walletId;

  @Schema(description = "Lock amount")
  @NotNull(message = "lockAmount cannot be null")
  @DecimalMin(value = "0.0001", message = "lockAmount must be positive")
  @DecimalMax(value = "9999999999.9999", message = "lockAmount exceeds maximum limit")
  @Digits(
      integer = 15,
      fraction = 4,
      message = "lockAmount precision exceeded (max 4 decimal places)")
  private BigDecimal lockAmount;

  @SchemaEnum(LockReasonEnum.class)
  @CheckEnum(message = "lockReason invalid", value = LockReasonEnum.class, required = true)
  private Integer lockReason;

  @Schema(description = "Reference ID (order/bet ID)")
  @NotBlank(message = "referenceId cannot be blank")
  @Size(max = 64)
  private String referenceId;

  @Schema(description = "Lock expiry time")
  private OffsetDateTime expiresAt;
}
