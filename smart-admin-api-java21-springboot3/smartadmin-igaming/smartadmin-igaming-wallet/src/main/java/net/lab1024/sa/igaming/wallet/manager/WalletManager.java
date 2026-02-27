package net.lab1024.sa.igaming.wallet.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
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
@Service
@RequiredArgsConstructor
public class WalletManager {

  private final WalletDao walletDao;
  private final WalletTransactionDao walletTransactionDao;
  private final WalletLockDao walletLockDao;
  private final DomainEventPublisher domainEventPublisher;

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

    publishWalletEvent("WALLET_CREDITED", wallet, transaction);
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

    publishWalletEvent("WALLET_DEBITED", wallet, transaction);
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

    domainEventPublisher.publish(
        IgamingKafkaConst.Topic.WALLET_EVENTS,
        DomainEvent.builder()
            .eventType(eventType)
            .aggregateType("Wallet")
            .aggregateId(String.valueOf(wallet.getWalletId()))
            .payload(node)
            .build());
  }
}
