package net.lab1024.sa.igaming.integration.risk.component;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.integration.risk.domain.WithdrawalRiskContext;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;

/**
 * KYC level check component — validates KYC level meets withdrawal requirements.
 *
 * <p>This component checks if the player's KYC verification level is sufficient for the requested
 * withdrawal amount:
 *
 * <ul>
 *   <li>KYC L0 (Unverified): Cannot withdraw (score=100, blocked)
 *   <li>KYC L1 (Basic): Can withdraw up to $2,000/day (score=30 if amount > $2,000)
 *   <li>KYC L2 (Advanced): No withdrawal limit (score=0)
 * </ul>
 *
 * <p><b>Score Range:</b> 0-100
 *
 * <ul>
 *   <li>0: KYC verified and meets requirement
 *   <li>30: KYC L1 but exceeds $2,000 limit (needs manual review)
 *   <li>100: KYC L0 (unverified, automatic rejection)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-18
 */
@Slf4j
@LiteflowComponent("kycLevelCheck")
@RequiredArgsConstructor
public class KycLevelCheckCmp extends NodeComponent {

  private static final String COMPONENT_ID = "kycLevelCheck";
  private static final int KYC_L0 = 0; // Unverified
  private static final int KYC_L1 = 1; // Basic (name, DOB)
  private static final int KYC_L2 = 2; // Advanced (ID, address proof)
  private static final BigDecimal KYC_L1_DAILY_LIMIT = new BigDecimal("2000.00");

  private final PlayerDao playerDao;

  @Override
  public void process() throws Exception {
    WithdrawalRiskContext ctx = this.getContextBean(WithdrawalRiskContext.class);
    Long playerId = ctx.getPlayerId();
    BigDecimal withdrawalAmount = ctx.getWithdrawalAmount();

    // Load player to get KYC level
    PlayerEntity player =
        playerDao.selectOne(
            Wrappers.<PlayerEntity>lambdaQuery()
                .eq(PlayerEntity::getPlayerId, playerId)
                .eq(PlayerEntity::getDeleted, false));

    if (player == null) {
      log.error("KycLevelCheck: Player not found, playerId={}", playerId);
      ctx.addRuleScore(COMPONENT_ID, 100); // Treat as critical risk
      ctx.setBlocked(true);
      ctx.setBlockReason("Player not found");
      return;
    }

    int kycLevel = player.getKycLevel();
    int score = calculateScore(kycLevel, withdrawalAmount);

    log.debug(
        "KycLevelCheck: playerId={}, kycLevel={}, amount={}, score={}",
        playerId,
        kycLevel,
        withdrawalAmount,
        score);

    ctx.addRuleScore(COMPONENT_ID, score);
    ctx.setKycLevel(kycLevel);

    // Block if KYC L0 (unverified)
    if (kycLevel == KYC_L0) {
      ctx.setBlocked(true);
      ctx.setBlockReason("KYC verification required. Please complete KYC L1 verification.");
    }

    // Flag if KYC L1 exceeds daily limit (manual review required)
    if (kycLevel == KYC_L1 && withdrawalAmount.compareTo(KYC_L1_DAILY_LIMIT) > 0) {
      ctx.setFlagged(true);
      ctx.setFlagReason(
          "KYC L1 exceeds $2,000 daily limit. Please complete KYC L2 verification for higher"
              + " limits.");
    }
  }

  /**
   * Calculate risk score based on KYC level and withdrawal amount.
   *
   * <p>Score logic:
   *
   * <ul>
   *   <li>KYC L0: 100 (critical, auto-reject)
   *   <li>KYC L1 + amount > $2,000: 30 (medium, manual review)
   *   <li>KYC L1 + amount <= $2,000: 0 (low, auto-approve)
   *   <li>KYC L2: 0 (low, auto-approve)
   * </ul>
   *
   * @param kycLevel player's KYC level (0=L0, 1=L1, 2=L2)
   * @param amount withdrawal amount
   * @return risk score (0-100)
   */
  private int calculateScore(int kycLevel, BigDecimal amount) {
    if (kycLevel == KYC_L0) {
      return 100; // Critical: unverified, auto-reject
    } else if (kycLevel == KYC_L1) {
      if (amount.compareTo(KYC_L1_DAILY_LIMIT) > 0) {
        return 30; // Medium: exceeds L1 limit, manual review
      }
      return 0; // Low: within L1 limit, auto-approve
    } else if (kycLevel >= KYC_L2) {
      return 0; // Low: L2 verified, auto-approve
    }

    // Unknown KYC level — treat as unverified
    return 100;
  }
}
