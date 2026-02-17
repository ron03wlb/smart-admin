package net.lab1024.sa.igaming.wallet.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Wallet entity — master wallet table.
 *
 * <p>Each player has one wallet per type (CASH/BONUS/CREDIT) per tenant.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_wallet")
public class WalletEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long walletId;

  private Long playerId;

  private String currencyCode;

  /**
   * Wallet type: 1=CASH, 2=BONUS, 3=CREDIT. See {@link
   * net.lab1024.sa.igaming.common.constant.WalletTypeEnum}.
   */
  private Integer walletType;

  /** Balance with DECIMAL(19,4) precision. */
  private BigDecimal balance;

  /** Locked amount (= SUM of t_wallet_lock.lock_amount). */
  private BigDecimal lockedAmount;

  /** Optimistic lock version. */
  @Version private Integer version;

  private Boolean deleted;
}
