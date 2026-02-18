package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;

/**
 * Form for debiting (withdrawing) funds from a wallet.
 *
 * <p>Supported transaction types: WITHDRAW(2), BET(3), ADJUSTMENT(6).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletDebitForm {

  @Schema(description = "Wallet ID")
  @NotNull(message = "walletId cannot be null")
  private Long walletId;

  @Schema(description = "Debit amount")
  @NotNull(message = "amount cannot be null")
  @DecimalMin(value = "0.0001", message = "amount must be positive")
  @DecimalMax(value = "9999999999.9999", message = "amount exceeds maximum limit")
  @Digits(integer = 15, fraction = 4, message = "amount precision exceeded (max 4 decimal places)")
  private BigDecimal amount;

  @SchemaEnum(TransactionTypeEnum.class)
  @CheckEnum(
      message = "transactionType invalid",
      value = TransactionTypeEnum.class,
      required = true)
  private Integer transactionType;

  @Schema(description = "Idempotency key")
  @NotBlank(message = "requestId cannot be blank")
  @Size(max = 64)
  private String requestId;

  @Schema(description = "Reference type")
  @Size(max = 30)
  private String referenceType;

  @Schema(description = "Reference ID")
  @Size(max = 64)
  private String referenceId;

  @Schema(description = "Transaction description")
  @Size(max = 200)
  private String description;
}
