package net.lab1024.sa.igaming.activity.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bonus distribution manager — atomic bonus creation + wallet credit.
 *
 * <p>Creates a PlayerBonusRecord, WalletBonusExt, and credits the BONUS wallet within a single
 * transaction. DuplicateKeyException on claim_id provides Layer 2 idempotency.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BonusDistributionManager {

  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final WalletBonusExtDao walletBonusExtDao;
  private final WalletDao walletDao;
  private final WalletManager walletManager;

  /**
   * Distribute bonus to a player.
   *
   * @param playerId player ID
   * @param rule promotion rule
   * @param claimId idempotency key
   * @param tenantId tenant ID
   * @return the created bonus record
   * @throws DuplicateKeyException if claim_id already exists (idempotent)
   * @throws OptimisticLockingFailureException if wallet version conflict
   */
  @Transactional(rollbackFor = Throwable.class)
  public PlayerBonusRecordEntity distributeBonus(
      Long playerId, PromotionRuleEntity rule, String claimId, Long tenantId) {

    // Atomic max-claims check inside @Transactional (prevents TOCTOU race condition)
    if (rule.getMaxClaimsPerPlayer() != null) {
      int claimCount = playerBonusRecordDao.countClaimsByPlayerAndRule(playerId, rule.getRuleId());
      if (claimCount >= rule.getMaxClaimsPerPlayer()) {
        throw new IllegalStateException(ActivityErrorCode.MAX_CLAIMS_REACHED.getMsg());
      }
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    BigDecimal bonusAmount = rule.getMaxBonus();
    BigDecimal wageringRequired =
        bonusAmount.multiply(
            rule.getWageringMultiplier() != null ? rule.getWageringMultiplier() : BigDecimal.ONE);

    // Create bonus record
    PlayerBonusRecordEntity record = new PlayerBonusRecordEntity();
    record.setPlayerId(playerId);
    record.setRuleId(rule.getRuleId());
    record.setClaimId(claimId);
    record.setBonusAmount(bonusAmount);
    record.setWageringRequired(wageringRequired);
    record.setWageringCompleted(BigDecimal.ZERO);
    record.setStatus(BonusRecordStatusEnum.ACTIVE.getValue());
    record.setClaimedAt(now);
    record.setExpiredAt(
        now.plusDays(rule.getBonusExpiryDays() != null ? rule.getBonusExpiryDays() : 30));
    record.setDeleted(false);

    try {
      playerBonusRecordDao.insert(record);
    } catch (DuplicateKeyException e) {
      log.info("Idempotent bonus claim Layer 2: claimId={}", claimId);
      throw e;
    }

    // Find BONUS wallet
    WalletEntity bonusWallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, playerId)
                .eq(WalletEntity::getWalletType, WalletTypeEnum.BONUS.getValue())
                .eq(WalletEntity::getDeleted, false));
    if (bonusWallet == null) {
      throw new IllegalStateException("BONUS wallet not found for player: " + playerId);
    }

    // Create WalletBonusExt record
    WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
    bonusExt.setWalletId(bonusWallet.getWalletId());
    bonusExt.setBonusId(record.getRecordId());
    bonusExt.setBalance(bonusAmount);
    bonusExt.setWageringRequirement(wageringRequired);
    bonusExt.setWageredAmount(BigDecimal.ZERO);
    bonusExt.setExpiresAt(record.getExpiredAt());
    bonusExt.setGameRestriction(rule.getGameRestriction());
    bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
    walletBonusExtDao.insert(bonusExt);

    // Update bonus record with ext reference
    record.setWalletBonusExtId(bonusExt.getId());
    playerBonusRecordDao.updateById(record);

    // Credit BONUS wallet
    BigDecimal balanceBefore = bonusWallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(bonusAmount);

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(bonusWallet.getWalletId());
    transaction.setPlayerId(playerId);
    transaction.setTransactionType(TransactionTypeEnum.BONUS.getValue());
    transaction.setAmount(bonusAmount);
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId("bonus:" + claimId);
    transaction.setReferenceType("BONUS_CLAIM");
    transaction.setReferenceId(String.valueOf(record.getRecordId()));
    transaction.setDescription("Bonus claim: " + rule.getPromotionCode());

    bonusWallet.setBalance(balanceAfter);
    walletManager.credit(bonusWallet, transaction);

    return record;
  }
}
