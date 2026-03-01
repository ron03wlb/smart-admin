package net.lab1024.sa.igaming.risk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalAssignForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.igaming.risk.service.RiskProposalAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk proposal admin controller — admin operations for proposal management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.RISK)
@RequiredArgsConstructor
public class RiskProposalAdminController {

  private final RiskProposalAdminService riskProposalAdminService;

  @Operation(summary = "Get proposal by ID")
  @GetMapping("/igaming/admin/risk/proposal/{proposalId}")
  @SaCheckPermission("risk:proposal:admin")
  public ResponseDTO<RiskProposalVO> getById(@PathVariable Long proposalId) {
    return riskProposalAdminService.getProposalById(proposalId);
  }

  @Operation(summary = "Assign proposal to reviewer")
  @PostMapping("/igaming/admin/risk/proposal/assign")
  @SaCheckPermission("risk:proposal:admin")
  public ResponseDTO<String> assign(@RequestBody @Valid RiskProposalAssignForm form) {
    return riskProposalAdminService.assignProposal(form);
  }

  @Operation(summary = "Manually escalate a proposal")
  @PostMapping("/igaming/admin/risk/proposal/escalate/{proposalId}")
  @SaCheckPermission("risk:proposal:admin")
  public ResponseDTO<String> escalate(@PathVariable Long proposalId) {
    return riskProposalAdminService.escalateProposal(proposalId);
  }

  @Operation(summary = "Trigger batch escalation of overdue proposals")
  @PostMapping("/igaming/admin/risk/escalation/trigger")
  @SaCheckPermission("risk:proposal:admin")
  public ResponseDTO<Integer> triggerEscalation() {
    return riskProposalAdminService.triggerOverdueEscalation();
  }
}
