package net.lab1024.sa.igaming.wallet.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletBonusCreditForm;
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
@Slf4j
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
  private final WalletBonusExtDao walletBonusExtDao;
  private final WalletManager walletManager;
  private final LockService lockService;
  private final IgamingProperties igamingProperties;

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
      return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_ALREADY_EXISTS.getMsg());
    }

    WalletEntity entity = new WalletEntity();
    entity.setPlayerId(form.getPlayerId());
    entity.setWalletType(form.getWalletType());
    entity.setCurrencyCode(
        form.getCurrencyCode() != null
            ? form.getCurrencyCode()
            : igamingProperties.getWallet().getDefaultCurrency());
    entity.setBalance(BigDecimal.ZERO);
    entity.setLockedAmount(BigDecimal.ZERO);
    entity.setDeleted(false);

    try {
      walletManager.createWallet(entity);
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_ALREADY_EXISTS.getMsg());
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
    // Validate transaction type direction (outside lock — pure parameter check)
    if (!CREDIT_TYPES.contains(form.getTransactionType())) {
      return ResponseDTO.userErrorParam(WalletErrorCode.INVALID_CREDIT_TYPE.getMsg());
    }

    String lockKey = "wallet:lock:" + form.getWalletId();
    return lockService.executeWithLock(
        lockKey,
        igamingProperties.getLock().getWaitMs(),
        igamingProperties.getLock().getLeaseMs(),
        () -> {
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
            return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
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
          transaction.setTenantId(wallet.getTenantId()); // Fix: set tenant_id from wallet

          // Update wallet balance
          wallet.setBalance(balanceAfter);

          // Execute in transaction (Manager handles @Transactional + optimistic lock + idempotency
          // L2)
          WalletTransactionEntity result = walletManager.credit(wallet, transaction);

          return ResponseDTO.ok(SmartBeanUtil.copy(result, WalletTransactionVO.class));
        });
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
    // Validate transaction type direction (outside lock — pure parameter check)
    if (!DEBIT_TYPES.contains(form.getTransactionType())) {
      return ResponseDTO.userErrorParam(WalletErrorCode.INVALID_DEBIT_TYPE.getMsg());
    }

    String lockKey = "wallet:lock:" + form.getWalletId();
    return lockService.executeWithLock(
        lockKey,
        igamingProperties.getLock().getWaitMs(),
        igamingProperties.getLock().getLeaseMs(),
        () -> {
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
            return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
          }

          // Check available balance
          BigDecimal available = wallet.getBalance().subtract(wallet.getLockedAmount());
          if (available.compareTo(form.getAmount()) < 0) {
            return ResponseDTO.userErrorParam(WalletErrorCode.INSUFFICIENT_BALANCE.getMsg());
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
          transaction.setTenantId(wallet.getTenantId()); // Fix: set tenant_id from wallet

          // Update wallet balance
          wallet.setBalance(balanceAfter);

          // Execute in transaction (Manager handles @Transactional + optimistic lock + idempotency
          // L2)
          WalletTransactionEntity result = walletManager.debit(wallet, transaction);

          return ResponseDTO.ok(SmartBeanUtil.copy(result, WalletTransactionVO.class));
        });
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
    String lockKey = "wallet:lock:" + form.getWalletId();
    return lockService.executeWithLock(
        lockKey,
        igamingProperties.getLock().getWaitMs(),
        igamingProperties.getLock().getLeaseMs(),
        () -> {
          // Load wallet
          WalletEntity wallet = walletDao.selectById(form.getWalletId());
          if (wallet == null || wallet.getDeleted()) {
            return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
          }

          // Check available balance
          BigDecimal available = wallet.getBalance().subtract(wallet.getLockedAmount());
          if (available.compareTo(form.getLockAmount()) < 0) {
            return ResponseDTO.userErrorParam(
                WalletErrorCode.INSUFFICIENT_BALANCE_FOR_LOCK.getMsg());
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
        });
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
    // Load lock record outside lock to get walletId for lock key
    WalletLockEntity lockEntity = walletLockDao.selectById(lockId);
    if (lockEntity == null) {
      return ResponseDTO.userErrorParam(WalletErrorCode.LOCK_NOT_FOUND.getMsg());
    }

    String lockKey = "wallet:lock:" + lockEntity.getWalletId();
    return lockService.executeWithLock(
        lockKey,
        igamingProperties.getLock().getWaitMs(),
        igamingProperties.getLock().getLeaseMs(),
        () -> {
          // Re-load wallet under lock for consistency
          WalletEntity wallet = walletDao.selectById(lockEntity.getWalletId());
          if (wallet == null || wallet.getDeleted()) {
            return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
          }

          // Guard: lockedAmount must not go negative
          BigDecimal newLockedAmount =
              wallet.getLockedAmount().subtract(lockEntity.getLockAmount());
          if (newLockedAmount.compareTo(BigDecimal.ZERO) < 0) {
            return ResponseDTO.userErrorParam(WalletErrorCode.LOCKED_AMOUNT_INCONSISTENCY.getMsg());
          }

          // Update wallet lockedAmount
          wallet.setLockedAmount(newLockedAmount);

          // Execute in transaction
          walletManager.unlockFunds(wallet, lockId);

          return ResponseDTO.ok();
        });
  }

  /**
   * Credit bonus funds to a player's BONUS wallet.
   *
   * <p>Locates the player's BONUS wallet, builds the bonus extension record, and delegates to
   * {@link WalletManager#creditBonus} for atomic execution. Idempotency: checks requestId before
   * processing.
   *
   * @param form bonus credit form with amount, bonusId, wagering requirements
   * @return transaction VO
   */
  public ResponseDTO<WalletTransactionVO> creditBonus(WalletBonusCreditForm form) {
    String lockKey = "wallet:bonus:lock:" + form.getPlayerId();
    return lockService.executeWithLock(
        lockKey,
        igamingProperties.getLock().getWaitMs(),
        igamingProperties.getLock().getLeaseMs(),
        () -> {
          // Idempotency Layer 1: check requestId
          WalletTransactionEntity existing =
              walletTransactionDao.selectOne(
                  Wrappers.<WalletTransactionEntity>lambdaQuery()
                      .eq(WalletTransactionEntity::getRequestId, form.getRequestId()));
          if (existing != null) {
            return ResponseDTO.ok(SmartBeanUtil.copy(existing, WalletTransactionVO.class));
          }

          // Find BONUS wallet for player
          WalletEntity bonusWallet =
              walletDao.selectOne(
                  Wrappers.<WalletEntity>lambdaQuery()
                      .eq(WalletEntity::getPlayerId, form.getPlayerId())
                      .eq(WalletEntity::getWalletType, WalletTypeEnum.BONUS.getValue())
                      .eq(WalletEntity::getDeleted, false));
          if (bonusWallet == null) {
            return ResponseDTO.userErrorParam(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
          }

          // Build bonus extension record
          WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
          bonusExt.setWalletId(bonusWallet.getWalletId());
          bonusExt.setBonusId(form.getBonusId());
          bonusExt.setBalance(form.getAmount());
          bonusExt.setWageringRequirement(form.getWageringRequirement());
          bonusExt.setWageredAmount(BigDecimal.ZERO);
          bonusExt.setExpiresAt(form.getExpiresAt());
          bonusExt.setGameRestriction(form.getGameRestriction());
          bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());

          // Build transaction record
          BigDecimal balanceBefore = bonusWallet.getBalance();
          BigDecimal balanceAfter = balanceBefore.add(form.getAmount());

          WalletTransactionEntity transaction = new WalletTransactionEntity();
          transaction.setWalletId(bonusWallet.getWalletId());
          transaction.setPlayerId(form.getPlayerId());
          transaction.setTransactionType(TransactionTypeEnum.BONUS.getValue());
          transaction.setAmount(form.getAmount());
          transaction.setBalanceBefore(balanceBefore);
          transaction.setBalanceAfter(balanceAfter);
          transaction.setRequestId(form.getRequestId());
          transaction.setReferenceType("BONUS_CREDIT");
          transaction.setReferenceId(String.valueOf(form.getBonusId()));
          transaction.setDescription(form.getDescription());
          transaction.setTenantId(bonusWallet.getTenantId()); // Fix: set tenant_id from wallet

          // Update wallet balance
          bonusWallet.setBalance(balanceAfter);

          // Execute in transaction (Manager handles @Transactional + optimistic lock + ext insert)
          WalletTransactionEntity result =
              walletManager.creditBonus(bonusWallet, transaction, bonusExt);

          return ResponseDTO.ok(SmartBeanUtil.copy(result, WalletTransactionVO.class));
        });
  }

  /**
   * Process agent commission — credit commission amount to the agent's CASH wallet.
   *
   * <p>Called by {@link net.lab1024.sa.igaming.wallet.consumer.AgentSettlementConsumer} when a
   * COMMISSION_APPROVED event is received.
   *
   * @param agentId agent ID (maps to playerId)
   * @param amount commission amount to credit
   * @param requestId idempotency key
   */
  public void processAgentCommission(Long agentId, BigDecimal amount, String requestId) {
    WalletEntity agentWallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, agentId)
                .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
                .eq(WalletEntity::getDeleted, false));

    if (agentWallet == null) {
      log.error("Agent CASH wallet not found: agentId={}", agentId);
      return;
    }

    WalletCreditForm form = new WalletCreditForm();
    form.setWalletId(agentWallet.getWalletId());
    form.setAmount(amount);
    form.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
    form.setRequestId(requestId);
    form.setReferenceType("COMMISSION");
    form.setReferenceId(String.valueOf(agentId));
    form.setDescription("Agent commission credit");

    ResponseDTO<WalletTransactionVO> result = credit(form);
    if (result.getOk()) {
      log.info(
          "Agent commission credited: agentId={}, amount={}, requestId={}",
          agentId,
          amount,
          requestId);
    } else {
      log.error(
          "Agent commission credit failed: agentId={}, amount={}, error={}",
          agentId,
          amount,
          result.getMsg());
    }
  }

  /**
   * Process bonus completion — load wallets and trigger BONUS to CASH conversion.
   *
   * <p>Called by {@link net.lab1024.sa.igaming.wallet.consumer.BonusCompletionConsumer} when a
   * WAGERING_COMPLETED event is received.
   *
   * @param bonusExtId bonus extension record ID
   * @param playerId player ID
   */
  public void processBonusCompletion(Long bonusExtId, Long playerId) {
    WalletBonusExtEntity bonusExt = walletBonusExtDao.selectById(bonusExtId);
    if (bonusExt == null || !BonusStatusEnum.COMPLETED.getValue().equals(bonusExt.getStatus())) {
      log.warn("BonusExt not found or not COMPLETED: bonusExtId={}", bonusExtId);
      return;
    }

    WalletEntity bonusWallet = walletDao.selectById(bonusExt.getWalletId());
    WalletEntity cashWallet =
        walletDao.selectOne(
            Wrappers.<WalletEntity>lambdaQuery()
                .eq(WalletEntity::getPlayerId, playerId)
                .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
                .eq(WalletEntity::getDeleted, false));

    if (bonusWallet == null || cashWallet == null) {
      log.error("Cannot find wallets for BONUS->CASH conversion: playerId={}", playerId);
      return;
    }

    walletManager.convertBonusToCash(bonusExt, bonusWallet, cashWallet);
    log.info(
        "BONUS->CASH conversion completed: playerId={}, bonusExtId={}, amount={}",
        playerId,
        bonusExtId,
        bonusExt.getBalance());
  }
}
