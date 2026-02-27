package net.lab1024.sa.igaming.game.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Game transaction manager — atomic game round + wallet operations.
 *
 * <p>Handles debit (bet), credit (win), and rollback within @Transactional boundaries. Calls
 * WalletManager directly (Manager-to-Manager) per SmartAdmin layered architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameTransactionManager {

  private static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

  private final GameRoundDao gameRoundDao;
  private final GameDao gameDao;
  private final GameWeightConfigDao gameWeightConfigDao;
  private final WalletDao walletDao;
  private final WalletManager walletManager;

  /**
   * Execute debit (bet placement).
   *
   * <p>Creates a game round and debits the player's CASH wallet atomically. DuplicateKeyException
   * on transactionId is caught for Layer 2 idempotency.
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<CallbackResponseVO> executeDebit(CallbackDebitForm form, Long tenantId) {
    // Find player's CASH wallet
    WalletEntity wallet = findCashWallet(form.getPlayerId());
    if (wallet == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    // Check available balance
    BigDecimal available = wallet.getBalance().subtract(wallet.getLockedAmount());
    if (available.compareTo(form.getAmount()) < 0) {
      return ResponseDTO.userErrorParam(GameErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    // Build wallet transaction and update balance
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.subtract(form.getAmount());

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.BET.getValue());
    transaction.setAmount(form.getAmount().negate());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(form.getTransactionId());
    transaction.setReferenceType("GAME_ROUND");
    transaction.setReferenceId(form.getRoundId());
    transaction.setDescription("Game bet: " + form.getGameCode());

    wallet.setBalance(balanceAfter);

    try {
      walletManager.debit(wallet, transaction);
    } catch (OptimisticLockingFailureException e) {
      return ResponseDTO.userErrorParam(GameErrorCode.DEBIT_FAILED.getMsg());
    }

    // Calculate weighted turnover (inline from removed GameWeightService)
    BigDecimal weightedTurnover =
        calculateWeightedTurnover(tenantId, form.getGameCode(), form.getAmount());

    // Create game round
    GameRoundEntity round = new GameRoundEntity();
    round.setTenantId(tenantId);
    round.setPlayerId(form.getPlayerId());
    round.setProviderCode(form.getProviderCode());
    round.setGpRoundId(form.getRoundId());
    round.setGameCode(form.getGameCode());
    round.setTransactionId(form.getTransactionId());
    round.setBetAmount(form.getAmount());
    round.setPayoutAmount(BigDecimal.ZERO);
    round.setWeightedTurnover(weightedTurnover);
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    round.setReconciliationStatus(ReconciliationStatusEnum.PENDING.getValue());
    round.setDeleted(false);

    try {
      gameRoundDao.insert(round);
    } catch (DuplicateKeyException e) {
      log.info("Idempotent debit Layer 2: transactionId={}", form.getTransactionId());
    }

    // Build response
    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(form.getTransactionId());
    response.setBalance(balanceAfter);
    response.setStatus(RoundStatusEnum.OPEN.getValue());
    return ResponseDTO.ok(response);
  }

  /**
   * Execute credit (win settlement).
   *
   * <p>Credits the player's CASH wallet and updates the round to SETTLED.
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<CallbackResponseVO> executeCredit(CallbackCreditForm form, Long tenantId) {
    // Find the game round by gpRoundId
    GameRoundEntity round =
        gameRoundDao.selectOne(
            Wrappers.<GameRoundEntity>lambdaQuery()
                .eq(GameRoundEntity::getGpRoundId, form.getRoundId())
                .eq(GameRoundEntity::getTenantId, tenantId));
    if (round == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROUND_NOT_FOUND.getMsg());
    }
    if (RoundStatusEnum.SETTLED.getValue().equals(round.getStatus())) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROUND_ALREADY_SETTLED.getMsg());
    }

    // Find player's CASH wallet
    WalletEntity wallet = findCashWallet(form.getPlayerId());
    if (wallet == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.CREDIT_FAILED.getMsg());
    }

    // Build wallet transaction and update balance
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(form.getPayoutAmount());

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.WIN.getValue());
    transaction.setAmount(form.getPayoutAmount());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(form.getTransactionId());
    transaction.setReferenceType("GAME_ROUND");
    transaction.setReferenceId(form.getRoundId());
    transaction.setDescription("Game win: round " + form.getRoundId());

    wallet.setBalance(balanceAfter);

    try {
      walletManager.credit(wallet, transaction);
    } catch (OptimisticLockingFailureException e) {
      return ResponseDTO.userErrorParam(GameErrorCode.CREDIT_FAILED.getMsg());
    }

    // Update round to SETTLED
    round.setPayoutAmount(form.getPayoutAmount());
    round.setStatus(RoundStatusEnum.SETTLED.getValue());
    gameRoundDao.updateById(round);

    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(form.getTransactionId());
    response.setBalance(balanceAfter);
    response.setStatus(RoundStatusEnum.SETTLED.getValue());
    return ResponseDTO.ok(response);
  }

  /**
   * Execute rollback (bet cancellation).
   *
   * <p>Refunds the player's CASH wallet and marks the round as CANCELLED.
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<CallbackResponseVO> executeRollback(CallbackRollbackForm form, Long tenantId) {
    // Find the original round by transactionId
    GameRoundEntity round = gameRoundDao.selectByTransactionId(form.getOriginalTransactionId());
    if (round == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROUND_NOT_FOUND.getMsg());
    }
    if (RoundStatusEnum.CANCELLED.getValue().equals(round.getStatus())) {
      // Already cancelled — idempotent
      CallbackResponseVO response = new CallbackResponseVO();
      response.setTransactionId(form.getOriginalTransactionId());
      response.setStatus(RoundStatusEnum.CANCELLED.getValue());
      return ResponseDTO.ok(response);
    }

    // Find player's CASH wallet
    WalletEntity wallet = findCashWallet(form.getPlayerId());
    if (wallet == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROLLBACK_FAILED.getMsg());
    }

    // Build wallet transaction (refund) and update balance
    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(round.getBetAmount());

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
    transaction.setAmount(round.getBetAmount());
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId("rollback:" + form.getOriginalTransactionId());
    transaction.setReferenceType("GAME_ROUND_ROLLBACK");
    transaction.setReferenceId(String.valueOf(round.getRoundId()));
    transaction.setDescription("Rollback bet: " + form.getOriginalTransactionId());

    wallet.setBalance(balanceAfter);

    try {
      walletManager.credit(wallet, transaction);
    } catch (OptimisticLockingFailureException e) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROLLBACK_FAILED.getMsg());
    }

    // Update round to CANCELLED
    round.setStatus(RoundStatusEnum.CANCELLED.getValue());
    gameRoundDao.updateById(round);

    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(form.getOriginalTransactionId());
    response.setBalance(balanceAfter);
    response.setStatus(RoundStatusEnum.CANCELLED.getValue());
    return ResponseDTO.ok(response);
  }

  private WalletEntity findCashWallet(Long playerId) {
    return walletDao.selectOne(
        Wrappers.<WalletEntity>lambdaQuery()
            .eq(WalletEntity::getPlayerId, playerId)
            .eq(WalletEntity::getWalletType, WalletTypeEnum.CASH.getValue())
            .eq(WalletEntity::getDeleted, false));
  }

  private BigDecimal calculateWeightedTurnover(
      Long tenantId, String gameCode, BigDecimal betAmount) {
    GameEntity game =
        gameDao.selectOne(
            Wrappers.<GameEntity>lambdaQuery()
                .eq(GameEntity::getGameCode, gameCode)
                .eq(GameEntity::getTenantId, tenantId)
                .eq(GameEntity::getDeleted, false));
    if (game == null) {
      return betAmount.multiply(DEFAULT_WEIGHT);
    }

    BigDecimal weight = gameWeightConfigDao.selectWeight(tenantId, game.getCategory());
    if (weight == null) {
      weight = DEFAULT_WEIGHT;
    }

    return betAmount.multiply(weight);
  }
}
