package net.lab1024.sa.igaming.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;

/**
 * Wallet transaction view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
public class WalletTransactionVO {

  @Schema(description = "Transaction ID")
  private Long transactionId;

  @Schema(description = "Wallet ID")
  private Long walletId;

  @Schema(description = "Player ID")
  private Long playerId;

  @SchemaEnum(TransactionTypeEnum.class)
  private Integer transactionType;

  @Schema(description = "Transaction type description")
  private String transactionTypeDesc;

  @Schema(description = "Transaction amount")
  private BigDecimal amount;

  @Schema(description = "Balance before transaction")
  private BigDecimal balanceBefore;

  @Schema(description = "Balance after transaction")
  private BigDecimal balanceAfter;

  @Schema(description = "Idempotency request ID")
  private String requestId;

  @Schema(description = "Reference type (DEPOSIT_ORDER, ROUND, BONUS, etc.)")
  private String referenceType;

  @Schema(description = "Reference ID")
  private String referenceId;

  @Schema(description = "Description")
  private String description;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;
}
