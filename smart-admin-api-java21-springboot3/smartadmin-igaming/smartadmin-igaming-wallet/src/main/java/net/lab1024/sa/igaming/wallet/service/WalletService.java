package net.lab1024.sa.igaming.wallet.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreateForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreditForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletDebitForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletLockForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletQueryForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletTransactionQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletLockVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Wallet Service — business logic layer with Vavr Option pattern.
 *
 * <p>Handles wallet CRUD operations, credit/debit transactions, and fund locking. Delegates
 * transactional operations to {@link WalletManager}.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
public class WalletService {

  /** Valid transaction types for credit (add funds) operations. */
  private static final Set<Integer> CREDIT_TYPES =
      Set.of(
          TransactionTypeEnum.DEPOSIT.getValue(),
          TransactionTypeEnum.WIN.getValue(),
          TransactionTypeEnum.BONUS.getValue(),
          TransactionTypeEnum.ADJUSTMENT.getValue());

  /** Valid transaction types for debit (deduct funds) operations. */
  private static final Set<Integer> DEBIT_TYPES =
      Set.of(
          TransactionTypeEnum.WITHDRAW.getValue(),
          TransactionTypeEnum.BET.getValue(),
          TransactionTypeEnum.ADJUSTMENT.getValue());

  private final WalletDao walletDao;
  private final WalletTransactionDao walletTransactionDao;
  private final WalletLockDao walletLockDao;
  private final WalletManager walletManager;

  /**
   * Get wallet by ID.
   *
   * @param walletId wallet ID
   * @return Option<WalletVO> — Some if exists and not deleted, None otherwise
   */
  public Option<WalletVO> getWallet(Long walletId) {
    return Option.of(walletDao.selectById(walletId))
        .filter(entity -> !entity.getDeleted())
        .map(
            entity -> {
              WalletVO vo = SmartBeanUtil.copy(entity, WalletVO.class);
              vo.setAvailableBalance(entity.getBalance().subtract(entity.getLockedAmount()));
              return vo;
            });
  }

  /**
   * Query wallets with pagination.
   *
   * @param queryForm query parameters
   * @return paginated wallet list
   */
  public ResponseDTO<PageResult<WalletVO>> queryWallets(WalletQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<WalletVO> list = walletDao.queryPage(page, queryForm);
    PageResult<WalletVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * Query wallet transactions with pagination.
   *
   * @param queryForm query parameters (walletId required)
   * @return paginated transaction list
   */
  public ResponseDTO<PageResult<WalletTransactionVO>> queryTransactions(
      WalletTransactionQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<WalletTransactionVO> list = walletTransactionDao.queryPage(page, queryForm);
    PageResult<WalletTransactionVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * Create a new wallet for a player.
   *
   * <p>TOCTOU defense: the UNIQUE constraint on (player_id, wallet_type) acts as Layer 2 defense.
   * If a concurrent request inserts between our SELECT check and INSERT, {@link
   * DuplicateKeyException} is caught and a user-friendly error is returned.
   *
   * @param form create wallet form
   * @return created wallet VO
   */
  public ResponseDTO<WalletVO> createWallet(WalletCreateForm form) {
    // Check uniqueness: one wallet per (playerId, walletType)
    WalletEntity existing =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, form.getPlayerId())
                .eq(WalletEntity::getWalletType, form.getWalletType())
                .eq(WalletEntity::getDeleted, false));
    if (existing != null) {
      return ResponseDTO.userErrorParam("Wallet already exists for this player and type");
    }

    WalletEntity entity = new WalletEntity();
    entity.setPlayerId(form.getPlayerId());
    entity.setWalletType(form.getWalletType());
    entity.setCurrencyCode(form.getCurrencyCode() != null ? form.getCurrencyCode() : "USD");
    entity.setBalance(BigDecimal.ZERO);
    entity.setLockedAmount(BigDecimal.ZERO);
    entity.setDeleted(false);

    try {
      walletManager.createWallet(entity);
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam("Wallet already exists for this player and type");
    }

    WalletVO vo = SmartBeanUtil.copy(entity, WalletVO.class);
    vo.setAvailableBalance(BigDecimal.ZERO);
    return ResponseDTO.ok(vo);
  }

  /**
   * Credit (add) funds to a wallet.
   *
   * <p>Idempotency: checks requestId before processing. If the same requestId already exists,
   * returns the previous transaction result. Manager layer provides Layer 2 idempotency via UNIQUE
   * constraint catch.
   *
   * @param form credit form with amount, transactionType, and requestId
   * @return transaction VO
   */
  public ResponseDTO<WalletTransactionVO> credit(WalletCreditForm form) {
    // Validate transaction type direction
    if (!CREDIT_TYPES.contains(form.getTransactionType())) {
      return ResponseDTO.userErrorParam("Invalid transaction type for credit operation");
    }

    // Idempotency Layer 1: check requestId
    WalletTransactionEntity existing =
        walletTransactionDao.selectOne(
            Wrappers.<WalletTransactionEntity>lambdaQuery()
                .eq(WalletTransactionEntity::getRequestId, form.getRequestId()));
    if (existing != null) {
      return ResponseDTO.ok(SmartBeanUtil.copy(existing, WalletTransactionVO.class));
    }

    // Load wallet
    WalletEntity wallet = walletDao.selectById(form.getWalletId());
    if (wallet == null || wallet.getDeleted()) {
      return ResponseDTO.userErrorParam("Wallet does not exist");
    }

    // Build transaction record
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(form.getAmount());

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(form.getTransactionType());
    transaction.setAmount(form.getAmount());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(form.getRequestId());
    transaction.setReferenceType(form.getReferenceType());
    transaction.setReferenceId(form.getReferenceId());
    transaction.setDescription(form.getDescription());

    // Update wallet balance
    wallet.setBalance(balanceAfter);

    // Execute in transaction (Manager handles @Transactional + optimistic lock + idempotency L2)
    WalletTransactionEntity result = walletManager.credit(wallet, transaction);

    return ResponseDTO.ok(SmartBeanUtil.copy(result, WalletTransactionVO.class));
  }

  /**
   * Debit (deduct) funds from a wallet.
   *
   * <p>Validates available balance (balance - lockedAmount) before deducting. Idempotency: checks
   * requestId before processing. Manager layer provides Layer 2 idempotency via UNIQUE constraint
   * catch.
   *
   * @param form debit form with amount, transactionType, and requestId
   * @return transaction VO
   */
  public ResponseDTO<WalletTransactionVO> debit(WalletDebitForm form) {
    // Validate transaction type direction
    if (!DEBIT_TYPES.contains(form.getTransactionType())) {
      return ResponseDTO.userErrorParam("Invalid transaction type for debit operation");
    }

    // Idempotency Layer 1: check requestId
    WalletTransactionEntity existing =
        walletTransactionDao.selectOne(
            Wrappers.<WalletTransactionEntity>lambdaQuery()
                .eq(WalletTransactionEntity::getRequestId, form.getRequestId()));
    if (existing != null) {
      return ResponseDTO.ok(SmartBeanUtil.copy(existing, WalletTransactionVO.class));
    }

    // Load wallet
    WalletEntity wallet = walletDao.selectById(form.getWalletId());
    if (wallet == null || wallet.getDeleted()) {
      return ResponseDTO.userErrorParam("Wallet does not exist");
    }

    // Check available balance
    BigDecimal available = wallet.getBalance().subtract(wallet.getLockedAmount());
    if (available.compareTo(form.getAmount()) < 0) {
      return ResponseDTO.userErrorParam("Insufficient available balance");
    }

    // Build transaction record
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.subtract(form.getAmount());

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(form.getTransactionType());
    transaction.setAmount(form.getAmount().negate());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(form.getRequestId());
    transaction.setReferenceType(form.getReferenceType());
    transaction.setReferenceId(form.getReferenceId());
    transaction.setDescription(form.getDescription());

    // Update wallet balance
    wallet.setBalance(balanceAfter);

    // Execute in transaction (Manager handles @Transactional + optimistic lock + idempotency L2)
    WalletTransactionEntity result = walletManager.debit(wallet, transaction);

    return ResponseDTO.ok(SmartBeanUtil.copy(result, WalletTransactionVO.class));
  }

  /**
   * Lock (freeze) funds in a wallet.
   *
   * <p>Validates available balance before locking.
   *
   * @param form lock form with lockAmount, lockReason, and referenceId
   * @return lock VO
   */
  public ResponseDTO<WalletLockVO> lockFunds(WalletLockForm form) {
    // Load wallet
    WalletEntity wallet = walletDao.selectById(form.getWalletId());
    if (wallet == null || wallet.getDeleted()) {
      return ResponseDTO.userErrorParam("Wallet does not exist");
    }

    // Check available balance
    BigDecimal available = wallet.getBalance().subtract(wallet.getLockedAmount());
    if (available.compareTo(form.getLockAmount()) < 0) {
      return ResponseDTO.userErrorParam("Insufficient available balance for locking");
    }

    // Build lock entity
    WalletLockEntity lockEntity = new WalletLockEntity();
    lockEntity.setWalletId(wallet.getWalletId());
    lockEntity.setLockAmount(form.getLockAmount());
    lockEntity.setLockReason(form.getLockReason());
    lockEntity.setReferenceId(form.getReferenceId());
    lockEntity.setExpiresAt(form.getExpiresAt());

    // Update wallet lockedAmount
    wallet.setLockedAmount(wallet.getLockedAmount().add(form.getLockAmount()));

    // Execute in transaction
    walletManager.lockFunds(wallet, lockEntity);

    return ResponseDTO.ok(SmartBeanUtil.copy(lockEntity, WalletLockVO.class));
  }

  /**
   * Unlock (unfreeze) funds in a wallet.
   *
   * <p>Validates that lockedAmount will not go negative after unlock.
   *
   * @param lockId lock record ID
   * @return success message
   */
  public ResponseDTO<String> unlockFunds(Long lockId) {
    // Load lock record
    WalletLockEntity lockEntity = walletLockDao.selectById(lockId);
    if (lockEntity == null) {
      return ResponseDTO.userErrorParam("Lock record does not exist");
    }

    // Load wallet
    WalletEntity wallet = walletDao.selectById(lockEntity.getWalletId());
    if (wallet == null || wallet.getDeleted()) {
      return ResponseDTO.userErrorParam("Wallet does not exist");
    }

    // Guard: lockedAmount must not go negative
    BigDecimal newLockedAmount = wallet.getLockedAmount().subtract(lockEntity.getLockAmount());
    if (newLockedAmount.compareTo(BigDecimal.ZERO) < 0) {
      return ResponseDTO.userErrorParam("Locked amount inconsistency detected");
    }

    // Update wallet lockedAmount
    wallet.setLockedAmount(newLockedAmount);

    // Execute in transaction
    walletManager.unlockFunds(wallet, lockId);

    return ResponseDTO.ok();
  }
}
