package net.lab1024.sa.igaming.risk.manager;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Risk proposal manager — creates and manages risk review work orders.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskProposalManager {

  /** Standard SLA: 24 hours for medium priority. */
  private static final long STANDARD_SLA_HOURS = 24;

  /** Urgent SLA: 30 minutes for critical priority. */
  private static final long URGENT_SLA_MINUTES = 30;

  private final RiskProposalDao riskProposalDao;

  /**
   * Create a standard review proposal (score 30-69).
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @param assessmentId linked risk assessment ID
   * @param riskLevel risk level
   * @return created proposal entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public RiskProposalEntity createReviewProposal(
      Long playerId, Long tenantId, Long assessmentId, RiskLevelEnum riskLevel) {
    RiskProposalEntity proposal = new RiskProposalEntity();
    proposal.setPlayerId(playerId);
    proposal.setTenantId(tenantId);
    proposal.setAssessmentId(assessmentId);
    proposal.setStatus(RiskProposalStatusEnum.PENDING.getValue());
    proposal.setPriority(riskLevel.getValue());
    proposal.setSlaDeadline(OffsetDateTime.now(ZoneOffset.UTC).plusHours(STANDARD_SLA_HOURS));

    riskProposalDao.insert(proposal);
    log.info(
        "Created review proposal: playerId={}, assessmentId={}, priority={}",
        playerId,
        assessmentId,
        riskLevel.getDesc());
    return proposal;
  }

  /**
   * Create an urgent proposal (score >= 70).
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @param assessmentId linked risk assessment ID
   * @return created proposal entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public RiskProposalEntity createUrgentProposal(Long playerId, Long tenantId, Long assessmentId) {
    RiskProposalEntity proposal = new RiskProposalEntity();
    proposal.setPlayerId(playerId);
    proposal.setTenantId(tenantId);
    proposal.setAssessmentId(assessmentId);
    proposal.setStatus(RiskProposalStatusEnum.PENDING.getValue());
    proposal.setPriority(RiskLevelEnum.CRITICAL.getValue());
    proposal.setSlaDeadline(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(URGENT_SLA_MINUTES));

    riskProposalDao.insert(proposal);
    log.warn("Created URGENT proposal: playerId={}, assessmentId={}", playerId, assessmentId);
    return proposal;
  }

  /**
   * Escalate all overdue pending/assigned proposals.
   *
   * @return number of escalated proposals
   */
  @Transactional(rollbackFor = Throwable.class)
  public int escalateOverdueProposals() {
    List<RiskProposalEntity> overdue = riskProposalDao.findOverduePending();
    for (RiskProposalEntity proposal : overdue) {
      proposal.setStatus(RiskProposalStatusEnum.ESCALATED.getValue());
      proposal.setPriority(RiskLevelEnum.CRITICAL.getValue());
      riskProposalDao.updateById(proposal);
      log.warn(
          "Escalated overdue proposal: proposalId={}, playerId={}",
          proposal.getProposalId(),
          proposal.getPlayerId());
    }
    return overdue.size();
  }
}
