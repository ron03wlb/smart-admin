package net.lab1024.sa.igaming.wallet.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;

/**
 * Wallet paginated query form.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WalletQueryForm extends PageParam {

  @Schema(description = "Player ID")
  private Long playerId;

  @SchemaEnum(WalletTypeEnum.class)
  @CheckEnum(message = "walletType invalid", value = WalletTypeEnum.class, required = false)
  private Integer walletType;

  @Schema(description = "Currency code")
  private String currencyCode;
}
