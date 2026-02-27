package net.lab1024.sa.igaming.risk.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import net.lab1024.sa.support.job.core.SmartJob;
import org.springframework.stereotype.Component;

/**
 * Risk proposal SLA monitoring job — escalates overdue proposals.
 *
 * <p>Recommended schedule: every 5 minutes.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskProposalSLAJob implements SmartJob {

  private final RiskProposalManager riskProposalManager;

  @Override
  public String run(String param) {
    int escalated = riskProposalManager.escalateOverdueProposals();
    String result = "Risk SLA check: escalated " + escalated + " overdue proposals";
    log.info(result);
    return result;
  }
}
