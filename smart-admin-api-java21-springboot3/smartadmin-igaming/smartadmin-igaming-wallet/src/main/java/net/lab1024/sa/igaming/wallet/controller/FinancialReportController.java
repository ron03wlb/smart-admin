package net.lab1024.sa.igaming.wallet.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.wallet.domain.form.FinancialReportQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.FinancialReportVO;
import net.lab1024.sa.igaming.wallet.service.FinancialReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Financial Report Controller — GGR/NGR/RTP summary API.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.REPORT)
@RequiredArgsConstructor
public class FinancialReportController {

  private final FinancialReportService financialReportService;

  @Operation(summary = "Generate financial summary report (GGR/NGR/RTP)")
  @PostMapping("/igaming/wallet/report/summary")
  @SaCheckPermission("wallet:report:query")
  public ResponseDTO<FinancialReportVO> generateReport(
      @RequestBody @Valid FinancialReportQueryForm form) {
    return financialReportService
        .generateReport(form.getStartDate(), form.getEndDate())
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.ok(new FinancialReportVO()));
  }
}
