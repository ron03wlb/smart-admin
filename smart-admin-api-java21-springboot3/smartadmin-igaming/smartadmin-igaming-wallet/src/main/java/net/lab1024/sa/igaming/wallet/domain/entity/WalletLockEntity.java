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
 * Wallet lock entity — lock detail record (dual-track design).
 *
 * <p>Dual-track consistency: t_wallet.locked_amount = SUM(t_wallet_lock.lock_amount).
 *
 * <p>This table has no update_time column since locks are created and deleted, not updated.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet_lock")
public class WalletLockEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long lockId;

  private Long walletId;

  private BigDecimal lockAmount;

  /** Lock reason: see {@link net.lab1024.sa.igaming.common.constant.LockReasonEnum}. */
  private Integer lockReason;

  private String referenceId;

  private OffsetDateTime expiresAt;

  /** Override: t_wallet_lock has no update_time column. */
  @TableField(exist = false)
  private OffsetDateTime updateTime;
}
