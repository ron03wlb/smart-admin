package net.lab1024.sa.igaming.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Wallet bonus extension entity — per-activity bonus detail.
 *
 * <p>Each bonus activity creates a separate record tracking its own balance and wagering progress.
 * Consistency rule: t_wallet.balance(BONUS) = SUM(t_wallet_bonus_ext.balance WHERE status=ACTIVE).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet_bonus_ext")
public class WalletBonusExtEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long walletId;

  private Long bonusId;

  /** Remaining bonus balance for this activity. */
  private BigDecimal balance;

  /** Required valid turnover to unlock bonus. */
  private BigDecimal wageringRequirement;

  /** Accumulated valid turnover. */
  private BigDecimal wageredAmount;

  private OffsetDateTime expiresAt;

  /** Game restriction rules as JSONB string. */
  private String gameRestriction;

  /** Status: see {@link net.lab1024.sa.igaming.common.constant.BonusStatusEnum}. */
  private Integer status;
}
