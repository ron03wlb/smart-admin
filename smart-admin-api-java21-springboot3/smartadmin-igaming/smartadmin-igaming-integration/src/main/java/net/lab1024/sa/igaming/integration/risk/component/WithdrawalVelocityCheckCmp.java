package net.lab1024.sa.igaming.integration.risk.component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.integration.risk.domain.WithdrawalRiskContext;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;

/**
 * Withdrawal velocity check component — validates withdrawal frequency limits.
 *
 * <p>This LiteFlow component implements velocity check to detect high-frequency withdrawal
 * attempts. Excessive withdrawals within a short timeframe may indicate fraud or bonus abuse.
 *
 * <p><b>Velocity Limits (24-hour window):</b>
 *
 * <ul>
 *   <li>0-3 withdrawals: Normal (Score 0, no flag)
 *   <li>4-5 withdrawals: Medium risk (Score 30, flag for manual review)
 *   <li>6+ withdrawals: High risk (Score 50, flag for manual review)
 * </ul>
 *
 * <p><b>Business Rules:</b>
 *
 * <ul>
 *   <li>Query all PENDING and SUCCESS withdrawals in past 24 hours (include current request)
 *   <li>If count >= 4, set {@code flagged=true} (manual approval required)
 *   <li>Component ID: "withdrawalVelocityCheck"
 * </ul>
 *
 * <p><b>Context Dependencies:</b>
 *
 * <ul>
 *   <li>Input: {@code playerId}, {@code tenantId}
 *   <li>Output: {@code ruleScores["withdrawalVelocityCheck"]}, {@code flagged}, {@code flagReason}
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@LiteflowComponent("withdrawalVelocityCheck")
@RequiredArgsConstructor
public class WithdrawalVelocityCheckCmp extends NodeComponent {

  private static final String COMPONENT_ID = "withdrawalVelocityCheck";

  /** Velocity check time window (24 hours). */
  private static final int VELOCITY_WINDOW_HOURS = 24;

  /** Normal threshold (0-3 withdrawals). */
  private static final int NORMAL_THRESHOLD = 3;

  /** Medium risk threshold (4-5 withdrawals). */
  private static final int MEDIUM_RISK_THRESHOLD = 5;

  private final PaymentOrderDao paymentOrderDao;

  /**
   * Process withdrawal velocity check.
   *
   * <p>This method queries recent withdrawals (PENDING + SUCCESS) in past 24 hours and calculates
   * velocity risk score.
   *
   * <p><b>Steps:</b>
   *
   * <ol>
   *   <li>Query withdrawal count in past 24 hours
   *   <li>Calculate risk score based on count
   *   <li>If count >= 4, flag for manual review
   * </ol>
   *
   * @throws Exception if processing fails
   */
  @Override
  public void process() throws Exception {
    WithdrawalRiskContext ctx = this.getContextBean(WithdrawalRiskContext.class);

    Long playerId = ctx.getPlayerId();
    Long tenantId = ctx.getTenantId();

    log.debug("[VELOCITY_CHECK] Processing player={}, tenantId={}", playerId, tenantId);

    // Step 1: Query withdrawal count in past 24 hours
    int withdrawalCount = queryRecentWithdrawalCount(playerId, tenantId);

    log.debug(
        "[VELOCITY_CHECK] Player={}, 24h withdrawal count={} (threshold={})",
        playerId,
        withdrawalCount,
        NORMAL_THRESHOLD);

    // Step 2: Calculate risk score
    int score = calculateScore(withdrawalCount);

    // Step 3: Add score to context
    ctx.addRuleScore(COMPONENT_ID, score);

    log.debug("[VELOCITY_CHECK] Player={}, score={}", playerId, score);

    // Step 4: Flag for manual review if count >= 4
    if (withdrawalCount > NORMAL_THRESHOLD) {
      ctx.setFlagged(true);
      ctx.setFlagReason(
          String.format(
              "High withdrawal frequency detected. %d withdrawals in past 24 hours (threshold:"
                  + " %d)",
              withdrawalCount, NORMAL_THRESHOLD));

      log.warn(
          "[VELOCITY_CHECK] Player={} FLAGGED for manual review. Withdrawal count={} in 24h"
              + " (threshold={})",
          playerId,
          withdrawalCount,
          NORMAL_THRESHOLD);
    }
  }

  /**
   * Query recent withdrawal count in past 24 hours.
   *
   * <p>Includes both PENDING and SUCCESS withdrawals to count all active withdrawal attempts.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @return withdrawal count
   */
  private int queryRecentWithdrawalCount(Long playerId, Long tenantId) {
    OffsetDateTime startTime = OffsetDateTime.now(ZoneOffset.UTC).minusHours(VELOCITY_WINDOW_HOURS);

    long count =
        paymentOrderDao.selectCount(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .eq(PaymentOrderEntity::getPlayerId, playerId)
                .eq(PaymentOrderEntity::getTenantId, tenantId)
                .eq(PaymentOrderEntity::getOrderType, PaymentOrderTypeEnum.WITHDRAWAL.getValue())
                .in(
                    PaymentOrderEntity::getStatus,
                    PaymentOrderStatusEnum.PENDING.getValue(),
                    PaymentOrderStatusEnum.SUCCESS.getValue())
                .ge(PaymentOrderEntity::getCreateTime, startTime));

    return (int) count;
  }

  /**
   * Calculate risk score based on withdrawal count.
   *
   * <p><b>Scoring Logic:</b>
   *
   * <ul>
   *   <li>0-3 withdrawals: Score 0 (Normal velocity)
   *   <li>4-5 withdrawals: Score 30 (Medium risk, flag for review)
   *   <li>6+ withdrawals: Score 50 (High risk, flag for review)
   * </ul>
   *
   * @param withdrawalCount withdrawal count in past 24 hours
   * @return risk score (0-50)
   */
  private int calculateScore(int withdrawalCount) {
    if (withdrawalCount <= NORMAL_THRESHOLD) {
      return 0; // Normal velocity (0-3 withdrawals)
    } else if (withdrawalCount <= MEDIUM_RISK_THRESHOLD) {
      return 30; // Medium risk (4-5 withdrawals)
    } else {
      return 50; // High risk (6+ withdrawals)
    }
  }
}
