package net.lab1024.sa.igaming.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.LockReasonEnum;

/**
 * Wallet lock view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletLockVO {

  @Schema(description = "Lock ID")
  private Long lockId;

  @Schema(description = "Wallet ID")
  private Long walletId;

  @Schema(description = "Lock amount")
  private BigDecimal lockAmount;

  @SchemaEnum(LockReasonEnum.class)
  private Integer lockReason;

  @Schema(description = "Lock reason description")
  private String lockReasonDesc;

  @Schema(description = "Reference ID (order/bet ID)")
  private String referenceId;

  @Schema(description = "Lock expiry time")
  private OffsetDateTime expiresAt;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
