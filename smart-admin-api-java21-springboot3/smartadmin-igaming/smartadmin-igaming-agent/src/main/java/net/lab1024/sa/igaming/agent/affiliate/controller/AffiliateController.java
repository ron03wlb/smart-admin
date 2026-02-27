package net.lab1024.sa.igaming.agent.affiliate.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionPlanEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.AffiliateRegisterForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionApprovalForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.AffiliateAgentVO;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateService;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Affiliate controller — agent registration, hierarchy, commission management.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.AGENT_AFFILIATE)
@RequiredArgsConstructor
public class AffiliateController {

  private final AffiliateService affiliateService;

  @Operation(summary = "Register a new affiliate agent")
  @PostMapping("/igaming/agent/affiliate/register")
  @SaCheckPermission("agent:affiliate:register")
  public ResponseDTO<AffiliateAgentVO> register(@RequestBody @Valid AffiliateRegisterForm form) {
    return affiliateService.registerAgent(form);
  }

  @Operation(summary = "Get agent by ID")
  @GetMapping("/igaming/agent/affiliate/{agentId}")
  @SaCheckPermission("agent:affiliate:query")
  public ResponseDTO<AffiliateAgentVO> getAgent(
      @PathVariable Long agentId, @RequestParam Long tenantId) {
    return affiliateService.getAgentById(agentId, tenantId);
  }

  @Operation(summary = "Get downline tree for an agent")
  @GetMapping("/igaming/agent/affiliate/downline/{agentId}")
  @SaCheckPermission("agent:affiliate:query")
  public ResponseDTO<List<AffiliateAgentVO>> getDownline(
      @PathVariable Long agentId, @RequestParam Long tenantId) {
    return affiliateService.getDownlineTree(agentId, tenantId);
  }

  @Operation(summary = "Get pending commission approvals")
  @GetMapping("/igaming/agent/affiliate/commissions/pending")
  @SaCheckPermission("agent:affiliate:commission")
  public ResponseDTO<List<CommissionRecordVO>> getPendingApprovals(@RequestParam Long tenantId) {
    return affiliateService.getPendingApprovals(tenantId);
  }

  @Operation(summary = "Approve a commission record")
  @PostMapping("/igaming/agent/affiliate/commission/approve")
  @SaCheckPermission("agent:affiliate:commission")
  public ResponseDTO<String> approveCommission(@RequestBody @Valid CommissionApprovalForm form) {
    return affiliateService.approveCommission(form);
  }

  @Operation(summary = "Reject a commission record")
  @PostMapping("/igaming/agent/affiliate/commission/reject")
  @SaCheckPermission("agent:affiliate:commission")
  public ResponseDTO<String> rejectCommission(@RequestBody @Valid CommissionApprovalForm form) {
    return affiliateService.rejectCommission(form);
  }

  @Operation(summary = "Get commission plan by ID")
  @GetMapping("/igaming/agent/affiliate/plan/{planId}")
  @SaCheckPermission("agent:affiliate:query")
  public ResponseDTO<AffiliateCommissionPlanEntity> getCommissionPlan(@PathVariable Long planId) {
    return affiliateService.getCommissionPlan(planId);
  }
}
