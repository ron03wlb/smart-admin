package net.lab1024.sa.igaming.wallet.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

/**
 * Financial report value object — GGR/NGR/RTP summary.
 *
 * <p>GGR = Total Bets - Total Wins. NGR = GGR - Bonus Cost. RTP = Total Wins / Total Bets × 100.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Data
public class FinancialReportVO {

  @Schema(description = "Total bet amount (absolute value)")
  private BigDecimal totalBets;

  @Schema(description = "Total win/payout amount")
  private BigDecimal totalWins;

  @Schema(description = "Total bonus cost")
  private BigDecimal bonusCost;

  @Schema(description = "Gross Gaming Revenue (GGR = totalBets - totalWins)")
  private BigDecimal ggr;

  @Schema(description = "Net Gaming Revenue (NGR = GGR - bonusCost)")
  private BigDecimal ngr;

  @Schema(description = "Return to Player percentage (RTP = totalWins / totalBets * 100)")
  private BigDecimal rtp;

  @Schema(description = "Total deposit amount")
  private BigDecimal totalDeposits;

  @Schema(description = "Total withdrawal amount (absolute value)")
  private BigDecimal totalWithdrawals;
}
