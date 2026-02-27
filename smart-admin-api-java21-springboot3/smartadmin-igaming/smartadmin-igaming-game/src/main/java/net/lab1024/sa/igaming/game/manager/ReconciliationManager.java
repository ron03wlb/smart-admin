package net.lab1024.sa.igaming.game.manager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.adapter.GPAdapterFactory;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.dao.ReconciliationDao;
import net.lab1024.sa.igaming.game.domain.dto.GameRoundDetail;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.entity.ReconciliationEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciliation manager — three-layer reconciliation logic.
 *
 * <p>Layer 1: Real-time transaction verification. Layer 2: Periodic poll for missing settlements.
 * Layer 3: Daily batch reconciliation.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconciliationManager {

  private final GameRoundDao gameRoundDao;
  private final ReconciliationDao reconciliationDao;
  private final GameProviderDao gameProviderDao;
  private final GPAdapterFactory gpAdapterFactory;

  /**
   * Layer 1: Real-time transaction verification.
   *
   * @param transactionId transaction to verify
   * @param expectedAmount platform-side amount
   * @param actualAmount GP-reported amount
   */
  @Transactional(rollbackFor = Throwable.class)
  public void verifyTransaction(
      String transactionId, BigDecimal expectedAmount, BigDecimal actualAmount) {
    GameRoundEntity round = gameRoundDao.selectByTransactionId(transactionId);
    if (round == null) {
      log.warn("Reconciliation: round not found for transactionId={}", transactionId);
      return;
    }

    if (expectedAmount.compareTo(actualAmount) == 0) {
      round.setReconciliationStatus(ReconciliationStatusEnum.VERIFIED.getValue());
      log.info("Reconciliation VERIFIED: transactionId={}", transactionId);
    } else {
      round.setReconciliationStatus(ReconciliationStatusEnum.MISMATCH.getValue());
      log.warn(
          "Reconciliation MISMATCH: transactionId={}, expected={}, actual={}",
          transactionId,
          expectedAmount,
          actualAmount);
    }
    gameRoundDao.updateById(round);
  }

  /**
   * Layer 2: Poll for missing settlements (unsettled rounds older than 10 minutes).
   *
   * <p>Queries GP for round status and compensates if settled on GP side.
   */
  @Transactional(rollbackFor = Throwable.class)
  public int pollMissingSettlements() {
    List<GameRoundEntity> unsettled = gameRoundDao.selectUnsettledRoundsOlderThan(10);
    int compensatedCount = 0;

    for (GameRoundEntity round : unsettled) {
      gpAdapterFactory
          .getAdapter(round.getProviderCode())
          .peek(
              adapter -> {
                adapter
                    .queryRound(round.getGpRoundId())
                    .onSuccess(detail -> handleGpRoundDetail(round, detail))
                    .onFailure(
                        e ->
                            log.warn(
                                "Failed to query GP round {}: {}",
                                round.getGpRoundId(),
                                e.getMessage()));
              });
      compensatedCount++;
    }

    log.info("Polled {} unsettled rounds for missing settlements", unsettled.size());
    return compensatedCount;
  }

  /**
   * Layer 3: Daily batch reconciliation for a specific date.
   *
   * @param date date to reconcile
   */
  @Transactional(rollbackFor = Throwable.class)
  public void dailyBatchReconciliation(LocalDate date) {
    List<GameProviderEntity> providers = gameProviderDao.selectEnabledProviders();

    for (GameProviderEntity provider : providers) {
      List<GameRoundEntity> rounds =
          gameRoundDao.selectByProviderAndDate(provider.getProviderCode(), date);

      int matched = 0;
      int mismatch = 0;
      int missing = 0;

      for (GameRoundEntity round : rounds) {
        Integer reconStatus = round.getReconciliationStatus();
        if (ReconciliationStatusEnum.VERIFIED.getValue().equals(reconStatus)) {
          matched++;
        } else if (ReconciliationStatusEnum.MISMATCH.getValue().equals(reconStatus)) {
          mismatch++;
        } else {
          missing++;
        }
      }

      ReconciliationEntity recon = new ReconciliationEntity();
      recon.setTenantId(provider.getTenantId());
      recon.setReconciliationDate(date);
      recon.setProviderCode(provider.getProviderCode());
      recon.setTotalGpTransactions(rounds.size());
      recon.setTotalPlatformTransactions(rounds.size());
      recon.setMatchedCount(matched);
      recon.setMismatchCount(mismatch);
      recon.setMissingCount(missing);
      recon.setExtraCount(0);
      recon.setStatus(
          mismatch > 0 || missing > 0
              ? ReconciliationStatusEnum.MISMATCH.getValue()
              : ReconciliationStatusEnum.VERIFIED.getValue());
      recon.setDeleted(false);
      reconciliationDao.insert(recon);

      log.info(
          "Daily reconciliation for {} on {}: matched={}, mismatch={}, missing={}",
          provider.getProviderCode(),
          date,
          matched,
          mismatch,
          missing);
    }
  }

  private void handleGpRoundDetail(GameRoundEntity round, GameRoundDetail detail) {
    if ("COMPLETED".equalsIgnoreCase(detail.getStatus())
        && RoundStatusEnum.OPEN.getValue().equals(round.getStatus())) {
      round.setPayoutAmount(detail.getPayoutAmount());
      round.setStatus(RoundStatusEnum.SETTLED.getValue());
      round.setReconciliationStatus(ReconciliationStatusEnum.COMPENSATED.getValue());
      gameRoundDao.updateById(round);
      log.info("Compensated missing settlement for round {}", round.getGpRoundId());
    }
  }
}
