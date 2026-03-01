package net.lab1024.sa.igaming.risk.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalAssignForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import org.springframework.stereotype.Service;

/**
 * Risk proposal admin service — admin operations for proposal management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskProposalAdminService {

  private final RiskProposalDao riskProposalDao;
  private final RiskProposalManager riskProposalManager;

  /**
   * Get a single proposal by ID.
   *
   * @param proposalId proposal ID
   * @return proposal VO
   */
  public ResponseDTO<RiskProposalVO> getProposalById(Long proposalId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    return Option.of(riskProposalDao.selectById(proposalId))
        .map(e -> SmartBeanUtil.copy(e, RiskProposalVO.class))
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("Proposal does not exist"));
  }

  /**
   * Assign a proposal to a reviewer.
   *
   * @param form assign form with proposalId and assignee
   * @return success response
   */
  public ResponseDTO<String> assignProposal(RiskProposalAssignForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    RiskProposalEntity proposal = riskProposalDao.selectById(form.getProposalId());
    if (proposal == null) {
      return ResponseDTO.userErrorParam("Proposal does not exist");
    }
    if (!RiskProposalStatusEnum.PENDING.getValue().equals(proposal.getStatus())) {
      return ResponseDTO.userErrorParam("Only PENDING proposals can be assigned");
    }

    proposal.setAssignee(form.getAssignee());
    proposal.setStatus(RiskProposalStatusEnum.ASSIGNED.getValue());
    riskProposalDao.updateById(proposal);

    log.info(
        "Proposal assigned: proposalId={}, assignee={}, tenantId={}",
        form.getProposalId(),
        form.getAssignee(),
        tenantId);
    return ResponseDTO.ok();
  }

  /**
   * Manually escalate a single proposal.
   *
   * @param proposalId proposal ID to escalate
   * @return success response
   */
  public ResponseDTO<String> escalateProposal(Long proposalId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    RiskProposalEntity proposal = riskProposalDao.selectById(proposalId);
    if (proposal == null) {
      return ResponseDTO.userErrorParam("Proposal does not exist");
    }
    Integer status = proposal.getStatus();
    if (!RiskProposalStatusEnum.PENDING.getValue().equals(status)
        && !RiskProposalStatusEnum.ASSIGNED.getValue().equals(status)) {
      return ResponseDTO.userErrorParam("Only PENDING or ASSIGNED proposals can be escalated");
    }

    proposal.setStatus(RiskProposalStatusEnum.ESCALATED.getValue());
    proposal.setPriority(RiskLevelEnum.CRITICAL.getValue());
    riskProposalDao.updateById(proposal);

    log.warn("Proposal manually escalated: proposalId={}, tenantId={}", proposalId, tenantId);
    return ResponseDTO.ok();
  }

  /**
   * Trigger batch escalation of all overdue proposals.
   *
   * @return number of escalated proposals
   */
  public ResponseDTO<Integer> triggerOverdueEscalation() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    int escalatedCount = riskProposalManager.escalateOverdueProposals();
    log.info("Overdue escalation triggered: escalated={}, tenantId={}", escalatedCount, tenantId);
    return ResponseDTO.ok(escalatedCount);
  }
}
