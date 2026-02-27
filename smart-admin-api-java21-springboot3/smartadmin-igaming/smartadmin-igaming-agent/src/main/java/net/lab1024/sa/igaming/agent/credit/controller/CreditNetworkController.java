package net.lab1024.sa.igaming.agent.credit.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditAllocateForm;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditRecallForm;
import net.lab1024.sa.igaming.agent.credit.domain.form.SettlementTriggerForm;
import net.lab1024.sa.igaming.agent.credit.domain.vo.AgentCreditVO;
import net.lab1024.sa.igaming.agent.credit.domain.vo.SettlementRecordVO;
import net.lab1024.sa.igaming.agent.credit.service.CreditNetworkService;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Credit network controller — credit allocation, recall, settlement.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.AGENT_CREDIT)
@RequiredArgsConstructor
public class CreditNetworkController {

  private final CreditNetworkService creditNetworkService;

  @Operation(summary = "Get agent credit info")
  @GetMapping("/igaming/agent/credit/{agentId}")
  @SaCheckPermission("agent:credit:query")
  public ResponseDTO<AgentCreditVO> getAgentCredit(
      @PathVariable Long agentId, @RequestParam Long tenantId) {
    return creditNetworkService.getAgentCredit(agentId, tenantId);
  }

  @Operation(summary = "Get downline credits for a parent agent")
  @GetMapping("/igaming/agent/credit/downline/{parentId}")
  @SaCheckPermission("agent:credit:query")
  public ResponseDTO<List<AgentCreditVO>> getDownlineCredits(
      @PathVariable Long parentId, @RequestParam Long tenantId) {
    return creditNetworkService.getDownlineCredits(parentId, tenantId);
  }

  @Operation(summary = "Allocate credit from parent to child")
  @PostMapping("/igaming/agent/credit/allocate")
  @SaCheckPermission("agent:credit:allocate")
  public ResponseDTO<String> allocateCredit(@RequestBody @Valid CreditAllocateForm form) {
    return creditNetworkService.allocateCredit(form, "system");
  }

  @Operation(summary = "Recall credit from child agent")
  @PostMapping("/igaming/agent/credit/recall")
  @SaCheckPermission("agent:credit:recall")
  public ResponseDTO<String> recallCredit(@RequestBody @Valid CreditRecallForm form) {
    return creditNetworkService.reclaimCredit(form, "system");
  }

  @Operation(summary = "Trigger weekly settlement")
  @PostMapping("/igaming/agent/settlement/trigger")
  @SaCheckPermission("agent:settlement:trigger")
  public ResponseDTO<List<SettlementRecordVO>> triggerSettlement(
      @RequestBody @Valid SettlementTriggerForm form) {
    return creditNetworkService.triggerSettlement(form);
  }

  @Operation(summary = "Verify settlement payment")
  @PostMapping("/igaming/agent/settlement/verify")
  @SaCheckPermission("agent:settlement:verify")
  public ResponseDTO<String> verifyPayment(
      @RequestParam Long settlementRecordId, @RequestParam String txnId) {
    return creditNetworkService.verifyPayment(settlementRecordId, txnId);
  }
}
