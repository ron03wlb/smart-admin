package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import java.time.LocalDate;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.wallet.controller.FinancialReportController;
import net.lab1024.sa.igaming.wallet.domain.form.FinancialReportQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.FinancialReportVO;
import net.lab1024.sa.igaming.wallet.service.FinancialReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FinancialReportController unit tests — Option handling with empty object fallback.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialReportController 單元測試")
class FinancialReportControllerTest {

  @Mock private FinancialReportService financialReportService;

  @InjectMocks private FinancialReportController financialReportController;

  @Test
  @DisplayName("generateReport 有資料 → ok(FinancialReportVO)")
  void generateReport_hasData() {
    LocalDate start = LocalDate.of(2026, 1, 1);
    LocalDate end = LocalDate.of(2026, 1, 31);
    FinancialReportQueryForm form = new FinancialReportQueryForm();
    form.setStartDate(start);
    form.setEndDate(end);

    FinancialReportVO vo = new FinancialReportVO();
    when(financialReportService.generateReport(start, end)).thenReturn(Option.some(vo));

    ResponseDTO<FinancialReportVO> result = financialReportController.generateReport(form);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(vo);
  }

  @Test
  @DisplayName("generateReport 無資料 → ok(空 FinancialReportVO)")
  void generateReport_noData() {
    LocalDate start = LocalDate.of(2026, 2, 1);
    LocalDate end = LocalDate.of(2026, 2, 28);
    FinancialReportQueryForm form = new FinancialReportQueryForm();
    form.setStartDate(start);
    form.setEndDate(end);

    when(financialReportService.generateReport(start, end)).thenReturn(Option.none());

    ResponseDTO<FinancialReportVO> result = financialReportController.generateReport(form);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();
  }
}
