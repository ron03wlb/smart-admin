package net.lab1024.sa.igaming.risk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalQueryForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalReviewForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.igaming.risk.service.RiskProposalService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk proposal review controller.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.RISK)
@RequiredArgsConstructor
public class RiskProposalController {

  private final RiskProposalService riskProposalService;

  @Operation(summary = "Query risk proposals with pagination")
  @PostMapping("/igaming/risk/proposal/query")
  @SaCheckPermission("risk:proposal:query")
  public ResponseDTO<PageResult<RiskProposalVO>> queryPage(
      @RequestBody @Valid RiskProposalQueryForm form) {
    return riskProposalService.queryPage(form);
  }

  @Operation(summary = "Approve a risk proposal")
  @PostMapping("/igaming/risk/proposal/approve")
  @SaCheckPermission("risk:proposal:review")
  public ResponseDTO<String> approve(@RequestBody @Valid RiskProposalReviewForm form) {
    return riskProposalService.approve(form);
  }

  @Operation(summary = "Reject a risk proposal")
  @PostMapping("/igaming/risk/proposal/reject")
  @SaCheckPermission("risk:proposal:review")
  public ResponseDTO<String> reject(@RequestBody @Valid RiskProposalReviewForm form) {
    return riskProposalService.reject(form);
  }
}
