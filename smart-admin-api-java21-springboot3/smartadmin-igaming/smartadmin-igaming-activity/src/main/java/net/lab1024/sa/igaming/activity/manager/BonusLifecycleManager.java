package net.lab1024.sa.igaming.activity.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bonus lifecycle manager — handles bonus expiration and forfeiture.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BonusLifecycleManager {

  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final WalletBonusExtDao walletBonusExtDao;
  private final WalletDao walletDao;
  private final WalletManager walletManager;

  /**
   * Expire active bonuses that have passed their expiry date.
   *
   * @param batchSize maximum records to process per batch
   * @return number of expired records
   */
  public int expireActiveBonuses(int batchSize) {
    List<PlayerBonusRecordEntity> expiredRecords =
        playerBonusRecordDao.selectExpiredActive(batchSize);
    if (expiredRecords.isEmpty()) {
      return 0;
    }

    int count = 0;
    for (PlayerBonusRecordEntity record : expiredRecords) {
      try {
        expireSingleBonus(record);
        count++;
      } catch (Exception e) {
        log.error("Failed to expire bonus recordId={}", record.getRecordId(), e);
      }
    }

    log.info("Expired {} bonus records", count);
    return count;
  }

  /**
   * Forfeit an active bonus (manual operation).
   *
   * @param recordId bonus record ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public void forfeitBonus(Long recordId) {
    PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
    if (record == null || !BonusRecordStatusEnum.ACTIVE.getValue().equals(record.getStatus())) {
      return;
    }

    record.setStatus(BonusRecordStatusEnum.FORFEITED.getValue());
    playerBonusRecordDao.updateById(record);

    deductBonusWallet(record, BonusStatusEnum.FORFEITED);
    log.info("Bonus forfeited: recordId={}", recordId);
  }

  @Transactional(rollbackFor = Throwable.class)
  public void expireSingleBonus(PlayerBonusRecordEntity record) {
    record.setStatus(BonusRecordStatusEnum.EXPIRED.getValue());
    playerBonusRecordDao.updateById(record);

    deductBonusWallet(record, BonusStatusEnum.EXPIRED);
  }

  private void deductBonusWallet(PlayerBonusRecordEntity record, BonusStatusEnum newStatus) {
    if (record.getWalletBonusExtId() == null) {
      return;
    }

    WalletBonusExtEntity ext = walletBonusExtDao.selectById(record.getWalletBonusExtId());
    if (ext == null || ext.getBalance().compareTo(BigDecimal.ZERO) <= 0) {
      if (ext != null) {
        ext.setStatus(newStatus.getValue());
        walletBonusExtDao.updateById(ext);
      }
      return;
    }

    // Update ext status
    BigDecimal deductAmount = ext.getBalance();
    ext.setStatus(newStatus.getValue());
    ext.setBalance(BigDecimal.ZERO);
    walletBonusExtDao.updateById(ext);

    // Deduct from BONUS wallet
    WalletEntity bonusWallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, record.getPlayerId())
                .eq(WalletEntity::getWalletType, WalletTypeEnum.BONUS.getValue())
                .eq(WalletEntity::getDeleted, false));
    if (bonusWallet == null) {
      log.warn(
          "BONUS wallet not found for playerId={}, recordId={}",
          record.getPlayerId(),
          record.getRecordId());
      return;
    }

    BigDecimal balanceBefore = bonusWallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.subtract(deductAmount).max(BigDecimal.ZERO);

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(bonusWallet.getWalletId());
    transaction.setPlayerId(record.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
    transaction.setAmount(deductAmount.negate());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(
        "bonus_"
            + newStatus.name().toLowerCase(java.util.Locale.ROOT)
            + ":"
            + record.getRecordId());
    transaction.setReferenceType("BONUS_" + newStatus.name());
    transaction.setReferenceId(String.valueOf(record.getRecordId()));
    transaction.setDescription(
        "Bonus "
            + newStatus.name().toLowerCase(java.util.Locale.ROOT)
            + ": record "
            + record.getRecordId());

    bonusWallet.setBalance(balanceAfter);
    walletManager.debit(bonusWallet, transaction);
  }
}
