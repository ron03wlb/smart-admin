package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;

/**
 * Form for creating a new wallet.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletCreateForm {

  @Schema(description = "Player ID")
  @NotNull(message = "playerId cannot be null")
  private Long playerId;

  @SchemaEnum(WalletTypeEnum.class)
  @CheckEnum(message = "walletType invalid", value = WalletTypeEnum.class, required = true)
  private Integer walletType;

  @Schema(description = "Currency code, default USD")
  @Size(min = 3, max = 3, message = "currencyCode must be 3 characters")
  @Pattern(regexp = "^[A-Z]{3}$", message = "currencyCode must be uppercase ISO 4217 format")
  private String currencyCode;
}
