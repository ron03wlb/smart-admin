package net.lab1024.sa.igaming.wallet.service;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.vo.FinancialReportVO;
import org.springframework.stereotype.Service;

/**
 * Financial report service — calculates GGR, NGR, and RTP from wallet transactions.
 *
 * <p>GGR (Gross Gaming Revenue) = Total Bets - Total Wins. NGR (Net Gaming Revenue) = GGR - Bonus
 * Cost. RTP (Return to Player) = Total Wins / Total Bets × 100.
 *
 * <p>All calculations are based on {@code t_wallet_transaction} aggregation. The wallet module
 * cannot directly access game module DAOs (ArchUnit enforced).
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@Service
@RequiredArgsConstructor
public class FinancialReportService {

  private final WalletTransactionDao walletTransactionDao;

  /**
   * Generate a financial summary report for the given date range.
   *
   * @param startDate start date (inclusive)
   * @param endDate end date (inclusive)
   * @return Option containing the report, or None if no transactions found
   */
  public Option<FinancialReportVO> generateReport(LocalDate startDate, LocalDate endDate) {
    OffsetDateTime startTime = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
    OffsetDateTime endTime = endDate.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

    BigDecimal totalBets =
        walletTransactionDao.sumAmountByType(
            TransactionTypeEnum.BET.getValue(), startTime, endTime);
    BigDecimal totalWins =
        walletTransactionDao.sumAmountByType(
            TransactionTypeEnum.WIN.getValue(), startTime, endTime);
    BigDecimal bonusCost =
        walletTransactionDao.sumAmountByType(
            TransactionTypeEnum.BONUS.getValue(), startTime, endTime);
    BigDecimal totalDeposits =
        walletTransactionDao.sumAmountByType(
            TransactionTypeEnum.DEPOSIT.getValue(), startTime, endTime);
    BigDecimal totalWithdrawals =
        walletTransactionDao.sumAmountByType(
            TransactionTypeEnum.WITHDRAW.getValue(), startTime, endTime);

    // If no bets at all, return None
    if (totalBets.compareTo(BigDecimal.ZERO) == 0 && totalWins.compareTo(BigDecimal.ZERO) == 0) {
      return Option.none();
    }

    BigDecimal ggr = totalBets.subtract(totalWins);
    BigDecimal ngr = ggr.subtract(bonusCost);
    BigDecimal rtp =
        totalBets.compareTo(BigDecimal.ZERO) > 0
            ? totalWins.multiply(BigDecimal.valueOf(100)).divide(totalBets, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

    FinancialReportVO vo = new FinancialReportVO();
    vo.setTotalBets(totalBets);
    vo.setTotalWins(totalWins);
    vo.setBonusCost(bonusCost);
    vo.setGgr(ggr);
    vo.setNgr(ngr);
    vo.setRtp(rtp);
    vo.setTotalDeposits(totalDeposits);
    vo.setTotalWithdrawals(totalWithdrawals);

    return Option.some(vo);
  }
}
