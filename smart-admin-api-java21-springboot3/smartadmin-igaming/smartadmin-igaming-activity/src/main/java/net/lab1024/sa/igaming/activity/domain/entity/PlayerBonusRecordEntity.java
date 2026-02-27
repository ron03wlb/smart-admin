package net.lab1024.sa.igaming.activity.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Player bonus record entity — tracks individual bonus claims and wagering progress.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_player_bonus_record")
public class PlayerBonusRecordEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long recordId;

  private Long playerId;

  private Long ruleId;

  /** Idempotency key for claim deduplication. */
  private String claimId;

  private BigDecimal bonusAmount;

  private BigDecimal wageringRequired;

  private BigDecimal wageringCompleted;

  /** See {@link net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum}. */
  private Integer status;

  private OffsetDateTime claimedAt;

  private OffsetDateTime completedAt;

  private OffsetDateTime expiredAt;

  /** Reference to t_wallet_bonus_ext.id. */
  private Long walletBonusExtId;

  private Boolean deleted;

  @Version private Integer version;
}
