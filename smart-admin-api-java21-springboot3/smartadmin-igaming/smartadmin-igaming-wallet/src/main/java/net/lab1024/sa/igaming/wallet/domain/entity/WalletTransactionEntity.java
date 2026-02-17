package net.lab1024.sa.igaming.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Wallet transaction entity — immutable append-only transaction log.
 *
 * <p>This table has no update_time column since transactions are never modified.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet_transaction")
public class WalletTransactionEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long transactionId;

  private Long walletId;

  private Long playerId;

  /** Transaction type: see {@link net.lab1024.sa.igaming.common.constant.TransactionTypeEnum}. */
  private Integer transactionType;

  private BigDecimal amount;

  private BigDecimal balanceBefore;

  private BigDecimal balanceAfter;

  /** Idempotency key — UNIQUE constraint in DB (Layer 2 defense). */
  private String requestId;

  private String referenceType;

  private String referenceId;

  private String description;

  /** Override: t_wallet_transaction has no update_time column (immutable). */
  @TableField(exist = false)
  private OffsetDateTime updateTime;
}
