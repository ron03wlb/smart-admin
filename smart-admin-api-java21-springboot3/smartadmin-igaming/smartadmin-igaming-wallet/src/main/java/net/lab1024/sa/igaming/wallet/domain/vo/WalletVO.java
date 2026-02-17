package net.lab1024.sa.igaming.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;

/**
 * Wallet view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletVO {

  @Schema(description = "Wallet ID")
  private Long walletId;

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Currency code (ISO 4217)")
  private String currencyCode;

  @SchemaEnum(WalletTypeEnum.class)
  private Integer walletType;

  @Schema(description = "Wallet type description")
  private String walletTypeDesc;

  @Schema(description = "Balance")
  private BigDecimal balance;

  @Schema(description = "Locked amount")
  private BigDecimal lockedAmount;

  @Schema(description = "Available balance (balance - lockedAmount)")
  private BigDecimal availableBalance;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;

  @Schema(description = "Updated time")
  private OffsetDateTime updateTime;
}
