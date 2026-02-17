package net.lab1024.sa.igaming.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;

/**
 * Wallet bonus extension view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletBonusExtVO {

  @Schema(description = "ID")
  private Long id;

  @Schema(description = "Wallet ID")
  private Long walletId;

  @Schema(description = "Bonus activity ID")
  private Long bonusId;

  @Schema(description = "Remaining bonus balance")
  private BigDecimal balance;

  @Schema(description = "Wagering requirement (valid turnover)")
  private BigDecimal wageringRequirement;

  @Schema(description = "Wagered amount (accumulated valid turnover)")
  private BigDecimal wageredAmount;

  @Schema(description = "Completion rate (%)")
  private BigDecimal completionRate;

  @Schema(description = "Expiry time")
  private OffsetDateTime expiresAt;

  @SchemaEnum(BonusStatusEnum.class)
  private Integer status;

  @Schema(description = "Status description")
  private String statusDesc;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;

  @Schema(description = "Updated time")
  private OffsetDateTime updateTime;
}
