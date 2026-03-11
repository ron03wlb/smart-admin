package net.lab1024.sa.igaming.wallet.payment.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Manager — handles all transactional payment operations.
 *
 * <p>All public methods MUST be annotated with {@code @Transactional(rollbackFor =
 * Throwable.class)} per SmartAdmin architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PaymentManager {

  private final PaymentOrderDao paymentOrderDao;
  private final WalletLockDao walletLockDao;
  private final WalletManager walletManager;

  /**
   * Create a payment order.
   *
   * @param order payment order entity to insert
   * @throws DuplicateKeyException re-thrown for idempotency Layer 2
   */
  @Transactional(rollbackFor = Throwable.class)
  public void createOrder(PaymentOrderEntity order) {
    paymentOrderDao.insert(order);
  }

  /**
   * Update order with PSP response (pspTransactionId + redirectUrl).
   *
   * @param order payment order entity with updated fields
   * @throws OptimisticLockingFailureException if version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateOrderPspResponse(PaymentOrderEntity order) {
    int rows = paymentOrderDao.updateById(order);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Payment order version conflict");
    }
  }

  /**
   * Complete a deposit: update order status to SUCCESS and credit wallet balance.
   *
   * @param order payment order entity (status will be set to SUCCESS)
   * @param wallet wallet entity to credit
   * @param creditRequestId idempotency key for the wallet credit transaction
   * @return the wallet transaction entity
   * @throws OptimisticLockingFailureException if version conflict detected
   */
  @Transactional(rollbackFor = Throwable.class)
  public WalletTransactionEntity completeDeposit(
      PaymentOrderEntity order, WalletEntity wallet, String creditRequestId) {
    // Update order status
    order.setStatus(PaymentOrderStatusEnum.SUCCESS.getValue());
    int rows = paymentOrderDao.updateById(order);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Payment order version conflict");
    }

    // Credit wallet balance
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(order.getAmount());
    wallet.setBalance(balanceAfter);

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
    transaction.setAmount(order.getAmount());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(creditRequestId);
    transaction.setReferenceType("PAYMENT_ORDER");
    transaction.setReferenceId(order.getOrderNo());
    transaction.setDescription("Deposit via " + order.getPspCode());
    transaction.setTenantId(wallet.getTenantId()); // FIX: Set tenant_id from wallet

    return walletManager.credit(wallet, transaction);
  }

  /**
   * Complete a withdrawal: update order status to SUCCESS and debit wallet balance + unlock funds.
   *
   * @param order payment order entity (status will be set to SUCCESS)
   * @param wallet wallet entity to debit
   * @param debitRequestId idempotency key for the wallet debit transaction
   * @param lockId lock record ID to unlock
   * @return the wallet transaction entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public WalletTransactionEntity completeWithdrawal(
      PaymentOrderEntity order, WalletEntity wallet, String debitRequestId, Long lockId) {
    // Update order status
    order.setStatus(PaymentOrderStatusEnum.SUCCESS.getValue());
    int rows = paymentOrderDao.updateById(order);
    if (rows == 0) {
      throw new OptimisticLockingFailureException("Payment order version conflict");
    }

    // Debit wallet balance
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.subtract(order.getAmount());
    wallet.setBalance(balanceAfter);

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.WITHDRAW.getValue());
    transaction.setAmount(order.getAmount().negate());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(debitRequestId);
    transaction.setReferenceType("PAYMENT_ORDER");
    transaction.setReferenceId(order.getOrderNo());
    transaction.setDescription("Withdrawal via " + order.getPspCode());
    transaction.setTenantId(wallet.getTenantId()); // FIX: Set tenant_id from wallet

    WalletTransactionEntity result = walletManager.debit(wallet, transaction);

    // Unlock frozen funds — update lockedAmount on entity before unlockFunds
    if (lockId != null) {
      WalletLockEntity lockEntity = walletLockDao.selectById(lockId);
      if (lockEntity != null) {
        wallet.setLockedAmount(wallet.getLockedAmount().subtract(lockEntity.getLockAmount()));
        walletManager.unlockFunds(wallet, lockId);
      }
    }

    return result;
  }

  /**
   * Fail a payment order and update status.
   *
   * @param order payment order entity
   * @param failStatus target fail status (FAILED or REJECTED)
   */
  @Transactional(rollbackFor = Throwable.class)
  public void failOrder(PaymentOrderEntity order, PaymentOrderStatusEnum failStatus) {
    order.setStatus(failStatus.getValue());
    paymentOrderDao.updateById(order);
  }

  /**
   * Update order status atomically using CAS pattern.
   *
   * @param orderId payment order ID
   * @param expectedStatus expected current status
   * @param newStatus new target status
   * @return number of rows updated (0 if CAS failed)
   */
  @Transactional(rollbackFor = Throwable.class)
  public int casUpdateStatus(Long orderId, int expectedStatus, int newStatus) {
    return paymentOrderDao.update(
        null,
        Wrappers.<PaymentOrderEntity>lambdaUpdate()
            .set(PaymentOrderEntity::getStatus, newStatus)
            .eq(PaymentOrderEntity::getPaymentOrderId, orderId)
            .eq(PaymentOrderEntity::getStatus, expectedStatus));
  }
}
