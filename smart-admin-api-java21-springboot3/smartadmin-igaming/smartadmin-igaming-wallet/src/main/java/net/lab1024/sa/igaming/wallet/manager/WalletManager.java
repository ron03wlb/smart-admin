package net.lab1024.sa.igaming.wallet.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wallet Manager — handles all transactional wallet operations.
 *
 * <p>All public methods MUST be annotated with {@code @Transactional(rollbackFor =
 * Throwable.class)} per SmartAdmin architecture rules (enforced by ArchUnit).
 *
 * <p>The Manager receives pre-assembled Entity objects from the Service layer and performs
 * persistence operations within a single transaction.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletManager {

  private final WalletDao walletDao;
  private final WalletTransactionDao walletTransactionDao;
  private final WalletLockDao walletLockDao;
  private final WalletBonusExtDao walletBonusExtDao;

  /**
   * Domain event publisher (optional - only available when Kafka is enabled).
   *
   * <p>When Kafka is disabled (smart.kafka.enabled=false), this field will be empty and wallet
   * events will not be published.
   */
  private final Optional<DomainEventPublisher> domainEventPublisher;

  /**
   * Create a new wallet.
   *
   * <p>Catches {@link DuplicateKeyException} from the UNIQUE constraint on (player_id, wallet_type)
   * to handle TOCTOU race conditions. The caller (Service) performs a SELECT check first, but
   * concurrent requests may pass that check simultaneously.
   *
   * @param wallet wallet entity to insert
   * @throws DuplicateKeyException re-thrown to let Service handle the business response
   */
  @Transactional(rollbackFor = Throwable.class)
  public void createWallet(WalletEntity wallet) {
    walletDao.insert(wallet);
  }

  /**
   * Credit (add funds to) a wallet within a single transaction.
   *
   * <p>Updates wallet balance via optimistic lock (@Version) and inserts the transaction record.
   * Checks updateById return value to detect version conflicts. Catches {@link
   * DuplicateKeyException} on requestId UNIQUE constraint as idempotency Layer 2 defense.
   *
   * @param wallet wallet entity with updated balance
   * @param transaction transaction record to insert
   * @return the persisted transaction (or existing one if idempotent duplicate)
   * @throws OptimisticLockingFailureException if wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public WalletTransactionEntity credit(WalletEntity wallet, WalletTransactionEntity transaction) {
    int rows = walletDao.updateById(wallet);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Wallet version conflict, please retry");
    }
    try {
      walletTransactionDao.insert(transaction);
    } catch (DuplicateKeyException e) {
      return walletTransactionDao.selectOne(
          Wrappers.<WalletTransactionEntity>lambdaQuery()
              .eq(WalletTransactionEntity::getRequestId, transaction.getRequestId()));
    }

    publishWalletEvent(DomainEventTypeConst.WALLET_CREDITED, wallet, transaction);
    return transaction;
  }

  /**
   * Debit (deduct funds from) a wallet within a single transaction.
   *
   * <p>Updates wallet balance via optimistic lock (@Version) and inserts the transaction record.
   * Checks updateById return value to detect version conflicts. Catches {@link
   * DuplicateKeyException} on requestId UNIQUE constraint as idempotency Layer 2 defense.
   *
   * @param wallet wallet entity with updated balance
   * @param transaction transaction record to insert
   * @return the persisted transaction (or existing one if idempotent duplicate)
   * @throws OptimisticLockingFailureException if wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public WalletTransactionEntity debit(WalletEntity wallet, WalletTransactionEntity transaction) {
    int rows = walletDao.updateById(wallet);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Wallet version conflict, please retry");
    }
    try {
      walletTransactionDao.insert(transaction);
    } catch (DuplicateKeyException e) {
      return walletTransactionDao.selectOne(
          Wrappers.<WalletTransactionEntity>lambdaQuery()
              .eq(WalletTransactionEntity::getRequestId, transaction.getRequestId()));
    }

    publishWalletEvent(DomainEventTypeConst.WALLET_DEBITED, wallet, transaction);
    return transaction;
  }

  /**
   * Lock (freeze) funds in a wallet within a single transaction.
   *
   * <p>Updates wallet lockedAmount and inserts the lock record. Checks updateById return value to
   * detect version conflicts.
   *
   * @param wallet wallet entity with updated lockedAmount
   * @param lockEntity lock record to insert
   * @throws OptimisticLockingFailureException if wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public void lockFunds(WalletEntity wallet, WalletLockEntity lockEntity) {
    int rows = walletDao.updateById(wallet);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Wallet version conflict, please retry");
    }
    walletLockDao.insert(lockEntity);
  }

  /**
   * Unlock (unfreeze) funds in a wallet within a single transaction.
   *
   * <p>Updates wallet lockedAmount and deletes the lock record. Checks updateById return value to
   * detect version conflicts.
   *
   * @param wallet wallet entity with updated lockedAmount
   * @param lockId lock record ID to delete
   * @throws OptimisticLockingFailureException if wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public void unlockFunds(WalletEntity wallet, Long lockId) {
    int rows = walletDao.updateById(wallet);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Wallet version conflict, please retry");
    }
    walletLockDao.deleteById(lockId);
  }

  /**
   * Credit to BONUS wallet and create the associated bonus extension record.
   *
   * <p>Atomically updates the BONUS wallet balance, inserts the WalletBonusExtEntity, and records
   * the transaction. Catches {@link DuplicateKeyException} on requestId UNIQUE constraint as
   * idempotency Layer 2 defense.
   *
   * @param bonusWallet BONUS wallet entity with updated balance
   * @param transaction transaction record to insert
   * @param bonusExt bonus extension record to insert
   * @return the persisted transaction (or existing one if idempotent duplicate)
   * @throws OptimisticLockingFailureException if wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public WalletTransactionEntity creditBonus(
      WalletEntity bonusWallet,
      WalletTransactionEntity transaction,
      WalletBonusExtEntity bonusExt) {
    int rows = walletDao.updateById(bonusWallet);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Wallet version conflict, please retry");
    }
    walletBonusExtDao.insert(bonusExt);
    try {
      walletTransactionDao.insert(transaction);
    } catch (DuplicateKeyException e) {
      return walletTransactionDao.selectOne(
          Wrappers.<WalletTransactionEntity>lambdaQuery()
              .eq(WalletTransactionEntity::getRequestId, transaction.getRequestId()));
    }

    publishWalletEvent(DomainEventTypeConst.BONUS_CREDITED, bonusWallet, transaction);
    return transaction;
  }

  /**
   * Debit from BONUS wallet using FIFO order (earliest expiry first).
   *
   * <p>Selects ACTIVE bonus extension records ordered by expiresAt ASC, and deducts from each
   * sequentially until the requested amount is fully consumed. Records whose balance reaches zero
   * are marked as COMPLETED.
   *
   * @param bonusWallet BONUS wallet entity with updated balance
   * @param transaction transaction record to insert
   * @param amount positive amount to debit
   * @return the persisted transaction (or existing one if idempotent duplicate)
   * @throws OptimisticLockingFailureException if wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public WalletTransactionEntity debitBonus(
      WalletEntity bonusWallet, WalletTransactionEntity transaction, BigDecimal amount) {
    int rows = walletDao.updateById(bonusWallet);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Wallet version conflict, please retry");
    }

    // FIFO deduction: earliest expiry first
    List<WalletBonusExtEntity> activeRecords =
        walletBonusExtDao.selectList(
            Wrappers.<WalletBonusExtEntity>lambdaQuery()
                .eq(WalletBonusExtEntity::getWalletId, bonusWallet.getWalletId())
                .eq(WalletBonusExtEntity::getStatus, BonusStatusEnum.ACTIVE.getValue())
                .gt(WalletBonusExtEntity::getBalance, BigDecimal.ZERO)
                .orderByAsc(WalletBonusExtEntity::getExpiresAt));

    BigDecimal remaining = amount;
    for (WalletBonusExtEntity ext : activeRecords) {
      if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
        break;
      }
      BigDecimal deduct = remaining.min(ext.getBalance());
      ext.setBalance(ext.getBalance().subtract(deduct));
      if (ext.getBalance().compareTo(BigDecimal.ZERO) == 0) {
        ext.setStatus(BonusStatusEnum.COMPLETED.getValue());
      }
      walletBonusExtDao.updateById(ext);
      remaining = remaining.subtract(deduct);
    }

    try {
      walletTransactionDao.insert(transaction);
    } catch (DuplicateKeyException e) {
      return walletTransactionDao.selectOne(
          Wrappers.<WalletTransactionEntity>lambdaQuery()
              .eq(WalletTransactionEntity::getRequestId, transaction.getRequestId()));
    }

    publishWalletEvent(DomainEventTypeConst.BONUS_DEBITED, bonusWallet, transaction);
    return transaction;
  }

  /**
   * Convert completed bonus balance from BONUS wallet to CASH wallet.
   *
   * <p>Deducts the bonus extension balance from the BONUS wallet and credits the same amount to the
   * CASH wallet. The bonus extension record is zeroed out. Both wallet updates use optimistic
   * locking.
   *
   * @param bonusExt bonus extension record whose wagering requirement has been met
   * @param bonusWallet BONUS wallet entity
   * @param cashWallet CASH wallet entity
   * @throws OptimisticLockingFailureException if either wallet version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public void convertBonusToCash(
      WalletBonusExtEntity bonusExt, WalletEntity bonusWallet, WalletEntity cashWallet) {
    BigDecimal convertAmount = bonusExt.getBalance();
    if (convertAmount.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("Bonus ext {} has zero balance, skipping conversion", bonusExt.getId());
      return;
    }

    // Deduct from BONUS wallet
    BigDecimal bonusBefore = bonusWallet.getBalance();
    bonusWallet.setBalance(bonusBefore.subtract(convertAmount));
    int bonusRows = walletDao.updateById(bonusWallet);
    if (bonusRows == 0) {
      throw new OptimisticLockingFailureException("BONUS wallet version conflict, please retry");
    }

    // Credit to CASH wallet
    BigDecimal cashBefore = cashWallet.getBalance();
    cashWallet.setBalance(cashBefore.add(convertAmount));
    int cashRows = walletDao.updateById(cashWallet);
    if (cashRows == 0) {
      throw new OptimisticLockingFailureException("CASH wallet version conflict, please retry");
    }

    // Zero out bonus ext balance
    bonusExt.setBalance(BigDecimal.ZERO);
    bonusExt.setStatus(BonusStatusEnum.COMPLETED.getValue());
    walletBonusExtDao.updateById(bonusExt);

    // Record debit transaction on BONUS wallet
    WalletTransactionEntity debitTx = new WalletTransactionEntity();
    debitTx.setWalletId(bonusWallet.getWalletId());
    debitTx.setPlayerId(bonusWallet.getPlayerId());
    debitTx.setTransactionType(
        net.lab1024.sa.igaming.common.constant.TransactionTypeEnum.ADJUSTMENT.getValue());
    debitTx.setAmount(convertAmount.negate());
    debitTx.setBalanceBefore(bonusBefore);
    debitTx.setBalanceAfter(bonusWallet.getBalance());
    debitTx.setRequestId("bonus_convert_debit:" + bonusExt.getId());
    debitTx.setReferenceType("BONUS_CONVERT");
    debitTx.setReferenceId(String.valueOf(bonusExt.getId()));
    debitTx.setDescription("Bonus conversion debit: ext " + bonusExt.getId());
    walletTransactionDao.insert(debitTx);

    // Record credit transaction on CASH wallet
    WalletTransactionEntity creditTx = new WalletTransactionEntity();
    creditTx.setWalletId(cashWallet.getWalletId());
    creditTx.setPlayerId(cashWallet.getPlayerId());
    creditTx.setTransactionType(
        net.lab1024.sa.igaming.common.constant.TransactionTypeEnum.BONUS.getValue());
    creditTx.setAmount(convertAmount);
    creditTx.setBalanceBefore(cashBefore);
    creditTx.setBalanceAfter(cashWallet.getBalance());
    creditTx.setRequestId("bonus_convert_credit:" + bonusExt.getId());
    creditTx.setReferenceType("BONUS_CONVERT");
    creditTx.setReferenceId(String.valueOf(bonusExt.getId()));
    creditTx.setDescription("Bonus conversion credit: ext " + bonusExt.getId());
    walletTransactionDao.insert(creditTx);

    publishWalletEvent(DomainEventTypeConst.BONUS_CONVERTED, cashWallet, creditTx);
    log.info(
        "Bonus converted: extId={}, amount={}, playerId={}",
        bonusExt.getId(),
        convertAmount,
        bonusWallet.getPlayerId());
  }

  /**
   * Freeze all wallets for a player (soft-delete).
   *
   * <p>Used when a player is suspended or self-excluded. All non-deleted wallets are marked as
   * deleted within a single transaction.
   *
   * @param playerId player ID
   * @return number of wallets frozen
   */
  @Transactional(rollbackFor = Throwable.class)
  public int freezePlayerWallets(Long playerId) {
    List<WalletEntity> wallets =
        walletDao.selectList(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, playerId)
                .eq(WalletEntity::getDeleted, false));
    for (WalletEntity wallet : wallets) {
      wallet.setDeleted(true);
      walletDao.updateById(wallet);
    }
    return wallets.size();
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishWalletEvent(
      String eventType, WalletEntity wallet, WalletTransactionEntity transaction) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("walletId", wallet.getWalletId());
    node.put("playerId", wallet.getPlayerId());
    node.put("transactionType", transaction.getTransactionType());
    node.put("amount", transaction.getAmount().toPlainString());
    node.put("balanceBefore", transaction.getBalanceBefore().toPlainString());
    node.put("balanceAfter", transaction.getBalanceAfter().toPlainString());
    node.put("requestId", transaction.getRequestId());

    domainEventPublisher.ifPresent(
        publisher ->
            publisher.publish(
                IgamingKafkaConst.Topic.WALLET_EVENTS,
                DomainEvent.builder()
                    .eventType(eventType)
                    .aggregateType("Wallet")
                    .aggregateId(String.valueOf(wallet.getWalletId()))
                    .payload(node)
                    .build()));
  }
}
