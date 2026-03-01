package net.lab1024.sa.igaming.agent.affiliate.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionAdjustmentForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionCalculateForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateCommissionAdminService;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Affiliate commission admin controller — admin operations for commission management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.AGENT_AFFILIATE)
@RequiredArgsConstructor
public class AffiliateCommissionAdminController {

  private final AffiliateCommissionAdminService affiliateCommissionAdminService;

  @Operation(summary = "Calculate and issue commission for an agent (admin)")
  @PostMapping("/igaming/admin/agent/commission/calculate")
  @SaCheckPermission("agent:commission:admin")
  public ResponseDTO<CommissionRecordVO> calculate(
      @RequestBody @Valid CommissionCalculateForm form) {
    return affiliateCommissionAdminService.calculateCommission(form);
  }

  @Operation(summary = "Create a commission adjustment (admin)")
  @PostMapping("/igaming/admin/agent/commission/adjustment")
  @SaCheckPermission("agent:commission:admin")
  public ResponseDTO<String> createAdjustment(@RequestBody @Valid CommissionAdjustmentForm form) {
    return affiliateCommissionAdminService.createAdjustment(form);
  }

  @Operation(summary = "Get commission record detail by ID")
  @GetMapping("/igaming/admin/agent/commission/record/{recordId}")
  @SaCheckPermission("agent:commission:admin")
  public ResponseDTO<CommissionRecordVO> getRecord(@PathVariable Long recordId) {
    return affiliateCommissionAdminService.getCommissionRecord(recordId);
  }

  @Operation(summary = "List pending commission approvals")
  @GetMapping("/igaming/admin/agent/commission/pending")
  @SaCheckPermission("agent:commission:admin")
  public ResponseDTO<List<CommissionRecordVO>> listPending() {
    return affiliateCommissionAdminService.listPendingApprovals();
  }
}
