package net.lab1024.sa.igaming.game.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Game round entity — seamless wallet core table.
 *
 * <p>Each round represents a single game bet/settle cycle. The {@code transactionId} field provides
 * idempotency via a unique constraint.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_game_round")
public class GameRoundEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long roundId;

  private Long playerId;

  private String providerCode;

  private String gpRoundId;

  private String gameCode;

  /** Unique transaction ID for idempotency. */
  private String transactionId;

  private BigDecimal betAmount;

  private BigDecimal payoutAmount;

  private BigDecimal weightedTurnover;

  /** Round status. See {@link net.lab1024.sa.igaming.common.constant.RoundStatusEnum}. */
  private Integer status;

  /**
   * Reconciliation status. See {@link
   * net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum}.
   */
  private Integer reconciliationStatus;

  private Boolean deleted;

  @Version private Integer version;
}
