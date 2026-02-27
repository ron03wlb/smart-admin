package net.lab1024.sa.igaming.game.domain.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * Game round VO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class GameRoundVO {

  private Long roundId;
  private Long playerId;
  private String providerCode;
  private String gpRoundId;
  private String gameCode;
  private String transactionId;
  private BigDecimal betAmount;
  private BigDecimal payoutAmount;
  private BigDecimal weightedTurnover;
  private Integer status;
  private Integer reconciliationStatus;
  private OffsetDateTime createTime;
}
