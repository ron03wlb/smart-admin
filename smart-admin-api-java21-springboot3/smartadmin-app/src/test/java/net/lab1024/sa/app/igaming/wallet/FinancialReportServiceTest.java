package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.vo.FinancialReportVO;
import net.lab1024.sa.igaming.wallet.service.FinancialReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FinancialReportService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialReportService 單元測試")
class FinancialReportServiceTest {

  @Mock private WalletTransactionDao walletTransactionDao;
  @InjectMocks private FinancialReportService financialReportService;

  private static final LocalDate START = LocalDate.of(2026, 2, 1);
  private static final LocalDate END = LocalDate.of(2026, 2, 28);

  @Nested
  @DisplayName("GGR 計算")
  class GgrTest {

    @Test
    @DisplayName("正確公式 — GGR = totalBets - totalWins")
    void calculateGGR_correctFormula() {
      stubTransactions("1000.00", "700.00", "50.00", "5000.00", "300.00");

      Option<FinancialReportVO> result = financialReportService.generateReport(START, END);

      assertThat(result.isDefined()).isTrue();
      FinancialReportVO vo = result.get();
      assertThat(vo.getTotalBets()).isEqualByComparingTo("1000.00");
      assertThat(vo.getTotalWins()).isEqualByComparingTo("700.00");
      assertThat(vo.getGgr()).isEqualByComparingTo("300.00"); // 1000 - 700
    }
  }

  @Nested
  @DisplayName("NGR 計算")
  class NgrTest {

    @Test
    @DisplayName("正確公式 — NGR = GGR - bonusCost")
    void calculateNGR_subtractsBonus() {
      stubTransactions("1000.00", "700.00", "50.00", "5000.00", "300.00");

      Option<FinancialReportVO> result = financialReportService.generateReport(START, END);

      assertThat(result.isDefined()).isTrue();
      FinancialReportVO vo = result.get();
      assertThat(vo.getNgr()).isEqualByComparingTo("250.00"); // 300 - 50
    }
  }

  @Nested
  @DisplayName("RTP 計算")
  class RtpTest {

    @Test
    @DisplayName("正確公式 — RTP = totalWins / totalBets * 100")
    void calculateRTP_correctFormula() {
      stubTransactions("1000.00", "700.00", "50.00", "5000.00", "300.00");

      Option<FinancialReportVO> result = financialReportService.generateReport(START, END);

      assertThat(result.isDefined()).isTrue();
      FinancialReportVO vo = result.get();
      assertThat(vo.getRtp()).isEqualByComparingTo("70.00"); // 700/1000 * 100
    }

    @Test
    @DisplayName("零投注 — 返回 None")
    void calculateRTP_zeroBetsReturnsNone() {
      stubTransactions("0", "0", "0", "0", "0");

      Option<FinancialReportVO> result = financialReportService.generateReport(START, END);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  @Nested
  @DisplayName("存取款匯總")
  class DepositWithdrawalTest {

    @Test
    @DisplayName("存取款金額正確")
    void depositWithdrawal_correct() {
      stubTransactions("1000.00", "700.00", "50.00", "5000.00", "300.00");

      Option<FinancialReportVO> result = financialReportService.generateReport(START, END);

      assertThat(result.isDefined()).isTrue();
      FinancialReportVO vo = result.get();
      assertThat(vo.getTotalDeposits()).isEqualByComparingTo("5000.00");
      assertThat(vo.getTotalWithdrawals()).isEqualByComparingTo("300.00");
    }
  }

  // ==================== helpers ====================

  private void stubTransactions(
      String bets, String wins, String bonus, String deposits, String withdrawals) {
    when(walletTransactionDao.sumAmountByType(
            eq(TransactionTypeEnum.BET.getValue()),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class)))
        .thenReturn(new BigDecimal(bets));
    when(walletTransactionDao.sumAmountByType(
            eq(TransactionTypeEnum.WIN.getValue()),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class)))
        .thenReturn(new BigDecimal(wins));
    when(walletTransactionDao.sumAmountByType(
            eq(TransactionTypeEnum.BONUS.getValue()),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class)))
        .thenReturn(new BigDecimal(bonus));
    when(walletTransactionDao.sumAmountByType(
            eq(TransactionTypeEnum.DEPOSIT.getValue()),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class)))
        .thenReturn(new BigDecimal(deposits));
    when(walletTransactionDao.sumAmountByType(
            eq(TransactionTypeEnum.WITHDRAW.getValue()),
            any(OffsetDateTime.class),
            any(OffsetDateTime.class)))
        .thenReturn(new BigDecimal(withdrawals));
  }
}
