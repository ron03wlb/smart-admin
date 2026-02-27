package net.lab1024.sa.igaming.risk.manager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.risk.dao.RiskAssessmentDao;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Risk score manager — handles transactional persistence of risk assessment and profile scores.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskScoreManager {

  private static final BigDecimal OLD_WEIGHT = new BigDecimal("0.70");
  private static final BigDecimal NEW_WEIGHT = new BigDecimal("0.30");
  private static final BigDecimal AUTO_LOCK_THRESHOLD = new BigDecimal("80");

  private final RiskAssessmentDao riskAssessmentDao;
  private final RiskScoreDao riskScoreDao;

  /**
   * Save a per-transaction risk assessment record.
   *
   * @param assessment assessment entity to insert
   */
  @Transactional(rollbackFor = Throwable.class)
  public void saveTransactionScore(RiskAssessmentEntity assessment) {
    riskAssessmentDao.insert(assessment);
    log.info(
        "Saved risk assessment: playerId={}, score={}, decision={}",
        assessment.getPlayerId(),
        assessment.getRiskScore(),
        assessment.getDecision());
  }

  /**
   * Update the player's cumulative risk profile using weighted moving average.
   *
   * <p>Formula: newScore = oldScore * 0.70 + transactionScore * 0.30. If cumulative score >= 80,
   * the player is automatically locked.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @param transactionScore score from the current assessment (0-100)
   * @return the updated risk score entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public RiskScoreEntity updateProfileScore(Long playerId, Long tenantId, int transactionScore) {
    RiskScoreEntity profile = riskScoreDao.findByPlayerIdAndTenantId(playerId, tenantId);

    if (profile == null) {
      profile = createNewProfile(playerId, tenantId, transactionScore);
      riskScoreDao.insert(profile);
      log.info(
          "Created new risk profile: playerId={}, initialScore={}", playerId, transactionScore);
    } else {
      BigDecimal oldScore = profile.getCumulativeScore();
      BigDecimal newScore =
          oldScore
              .multiply(OLD_WEIGHT)
              .add(BigDecimal.valueOf(transactionScore).multiply(NEW_WEIGHT));

      profile.setCumulativeScore(newScore);
      profile.setRiskLevel(determineRiskLevel(newScore).getValue());
      profile.setTotalAssessments(profile.getTotalAssessments() + 1);
      profile.setLastAssessmentTime(OffsetDateTime.now(ZoneOffset.UTC));

      if (newScore.compareTo(AUTO_LOCK_THRESHOLD) >= 0 && !profile.getAutoLocked()) {
        profile.setAutoLocked(true);
        log.warn("Auto-locked player due to high risk: playerId={}, score={}", playerId, newScore);
      }

      riskScoreDao.updateById(profile);
      log.info(
          "Updated risk profile: playerId={}, oldScore={}, newScore={}",
          playerId,
          oldScore,
          newScore);
    }

    return profile;
  }

  private RiskScoreEntity createNewProfile(Long playerId, Long tenantId, int transactionScore) {
    RiskScoreEntity profile = new RiskScoreEntity();
    profile.setPlayerId(playerId);
    profile.setTenantId(tenantId);
    profile.setCumulativeScore(BigDecimal.valueOf(transactionScore));
    profile.setRiskLevel(determineRiskLevel(BigDecimal.valueOf(transactionScore)).getValue());
    profile.setTotalAssessments(1);
    profile.setLastAssessmentTime(OffsetDateTime.now(ZoneOffset.UTC));
    profile.setAutoLocked(transactionScore >= AUTO_LOCK_THRESHOLD.intValue());
    return profile;
  }

  private RiskLevelEnum determineRiskLevel(BigDecimal score) {
    int s = score.intValue();
    if (s >= 70) {
      return RiskLevelEnum.CRITICAL;
    } else if (s >= 50) {
      return RiskLevelEnum.HIGH;
    } else if (s >= 30) {
      return RiskLevelEnum.MEDIUM;
    }
    return RiskLevelEnum.LOW;
  }
}
