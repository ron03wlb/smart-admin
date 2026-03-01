package net.lab1024.sa.igaming.game.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
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
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
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
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final GameRoundDao gameRoundDao;
  private final GameDao gameDao;
  private final GameWeightConfigDao gameWeightConfigDao;
  private final WalletDao walletDao;
  private final WalletBonusExtDao walletBonusExtDao;
  private final WalletTransactionDao walletTransactionDao;
  private final WalletManager walletManager;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Execute debit (bet placement) with multi-wallet priority: BONUS → CASH.
   *
   * <p>When a player has eligible BONUS balance for the game, it is deducted first (FIFO by
   * expiry). Any remaining amount is deducted from the CASH wallet. Each wallet debit produces its
   * own transaction record with a distinct requestId for idempotency.
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<CallbackResponseVO> executeDebit(CallbackDebitForm form, Long tenantId) {
    // 1. Find player's CASH wallet (required)
    WalletEntity cashWallet = findCashWallet(form.getPlayerId());
    if (cashWallet == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    // 2. Find BONUS wallet (optional)
    WalletEntity bonusWallet = findBonusWallet(form.getPlayerId());

    // 3. Calculate eligible bonus balance for this game
    BigDecimal eligibleBonus = BigDecimal.ZERO;
    if (bonusWallet != null) {
      eligibleBonus = calculateEligibleBonusForGame(bonusWallet.getWalletId(), form.getGameCode());
    }

    // 4. Determine debit allocation: BONUS first, then CASH
    BigDecimal betAmount = form.getAmount();
    BigDecimal bonusDebit = eligibleBonus.min(betAmount);
    BigDecimal cashDebit = betAmount.subtract(bonusDebit);

    // 5. Check CASH available balance covers the cash portion
    BigDecimal cashAvailable = cashWallet.getBalance().subtract(cashWallet.getLockedAmount());
    if (cashAvailable.compareTo(cashDebit) < 0) {
      return ResponseDTO.userErrorParam(GameErrorCode.INSUFFICIENT_BALANCE.getMsg());
    }

    // 6. Debit BONUS wallet if applicable
    BigDecimal bonusBalanceAfter = BigDecimal.ZERO;
    if (bonusDebit.compareTo(BigDecimal.ZERO) > 0 && bonusWallet != null) {
      BigDecimal bonusBefore = bonusWallet.getBalance();
      bonusBalanceAfter = bonusBefore.subtract(bonusDebit);

      WalletTransactionEntity bonusTx = new WalletTransactionEntity();
      bonusTx.setWalletId(bonusWallet.getWalletId());
      bonusTx.setPlayerId(bonusWallet.getPlayerId());
      bonusTx.setTransactionType(TransactionTypeEnum.BET.getValue());
      bonusTx.setAmount(bonusDebit.negate());
      bonusTx.setBalanceBefore(bonusBefore);
      bonusTx.setBalanceAfter(bonusBalanceAfter);
      bonusTx.setRequestId(form.getTransactionId() + ":bonus");
      bonusTx.setReferenceType("GAME_ROUND");
      bonusTx.setReferenceId(form.getRoundId());
      bonusTx.setDescription("Game bet (bonus): " + form.getGameCode());

      bonusWallet.setBalance(bonusBalanceAfter);

      try {
        walletManager.debitBonus(bonusWallet, bonusTx, bonusDebit);
      } catch (OptimisticLockingFailureException e) {
        return ResponseDTO.userErrorParam(GameErrorCode.DEBIT_FAILED.getMsg());
      }
    }

    // 7. Debit CASH wallet if applicable
    BigDecimal cashBalanceAfter = cashWallet.getBalance();
    if (cashDebit.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal cashBefore = cashWallet.getBalance();
      cashBalanceAfter = cashBefore.subtract(cashDebit);

      WalletTransactionEntity cashTx = new WalletTransactionEntity();
      cashTx.setWalletId(cashWallet.getWalletId());
      cashTx.setPlayerId(cashWallet.getPlayerId());
      cashTx.setTransactionType(TransactionTypeEnum.BET.getValue());
      cashTx.setAmount(cashDebit.negate());
      cashTx.setBalanceBefore(cashBefore);
      cashTx.setBalanceAfter(cashBalanceAfter);
      cashTx.setRequestId(
          bonusDebit.compareTo(BigDecimal.ZERO) > 0
              ? form.getTransactionId() + ":cash"
              : form.getTransactionId());
      cashTx.setReferenceType("GAME_ROUND");
      cashTx.setReferenceId(form.getRoundId());
      cashTx.setDescription("Game bet (cash): " + form.getGameCode());

      cashWallet.setBalance(cashBalanceAfter);

      try {
        walletManager.debit(cashWallet, cashTx);
      } catch (OptimisticLockingFailureException e) {
        return ResponseDTO.userErrorParam(GameErrorCode.DEBIT_FAILED.getMsg());
      }
    }

    // 8. Calculate weighted turnover
    BigDecimal weightedTurnover =
        calculateWeightedTurnover(tenantId, form.getGameCode(), form.getAmount());

    // 9. Create game round
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

    // 10. Publish BET_PLACED event with bonus/cash breakdown
    publishGameEvent(
        DomainEventTypeConst.BET_PLACED,
        form.getPlayerId(),
        tenantId,
        buildBetPlacedPayload(form, weightedTurnover, cashBalanceAfter, bonusDebit, cashDebit));

    // 11. Build response
    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(form.getTransactionId());
    response.setBalance(cashBalanceAfter);
    response.setBonusBalance(bonusBalanceAfter);
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

    // Publish ROUND_SETTLED event
    publishGameEvent(
        DomainEventTypeConst.ROUND_SETTLED,
        form.getPlayerId(),
        tenantId,
        buildRoundSettledPayload(form, balanceAfter));

    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(form.getTransactionId());
    response.setBalance(balanceAfter);
    response.setStatus(RoundStatusEnum.SETTLED.getValue());
    return ResponseDTO.ok(response);
  }

  /**
   * Execute rollback (bet cancellation) with dual-wallet refund support.
   *
   * <p>Looks up original debit transactions by requestId pattern to determine which wallets were
   * debited (BONUS, CASH, or both), then refunds each accordingly.
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

    // Find original debit transactions to determine refund allocation
    String origTxId = form.getOriginalTransactionId();
    WalletTransactionEntity bonusOrigTx = findTransactionByRequestId(origTxId + ":bonus");
    WalletTransactionEntity cashOrigTx = findTransactionByRequestId(origTxId + ":cash");
    WalletTransactionEntity singleOrigTx =
        (bonusOrigTx == null && cashOrigTx == null) ? findTransactionByRequestId(origTxId) : null;

    // Refund BONUS wallet if there was a bonus debit
    BigDecimal bonusBalanceAfter = BigDecimal.ZERO;
    if (bonusOrigTx != null) {
      BigDecimal bonusRefund = bonusOrigTx.getAmount().abs();
      WalletEntity bonusWallet = findBonusWallet(form.getPlayerId());
      if (bonusWallet != null) {
        BigDecimal bonusBefore = bonusWallet.getBalance();
        bonusBalanceAfter = bonusBefore.add(bonusRefund);

        WalletTransactionEntity bonusRefundTx = new WalletTransactionEntity();
        bonusRefundTx.setWalletId(bonusWallet.getWalletId());
        bonusRefundTx.setPlayerId(bonusWallet.getPlayerId());
        bonusRefundTx.setTransactionType(TransactionTypeEnum.ROLLBACK.getValue());
        bonusRefundTx.setAmount(bonusRefund);
        bonusRefundTx.setBalanceBefore(bonusBefore);
        bonusRefundTx.setBalanceAfter(bonusBalanceAfter);
        bonusRefundTx.setRequestId("rollback:" + origTxId + ":bonus");
        bonusRefundTx.setReferenceType("GAME_ROUND_ROLLBACK");
        bonusRefundTx.setReferenceId(String.valueOf(round.getRoundId()));
        bonusRefundTx.setDescription("Rollback bonus bet: " + origTxId);

        bonusWallet.setBalance(bonusBalanceAfter);

        try {
          walletManager.credit(bonusWallet, bonusRefundTx);
        } catch (OptimisticLockingFailureException e) {
          return ResponseDTO.userErrorParam(GameErrorCode.ROLLBACK_FAILED.getMsg());
        }
      }
    }

    // Refund CASH wallet
    WalletEntity cashWallet = findCashWallet(form.getPlayerId());
    if (cashWallet == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROLLBACK_FAILED.getMsg());
    }

    BigDecimal cashRefund;
    if (cashOrigTx != null) {
      cashRefund = cashOrigTx.getAmount().abs();
    } else if (singleOrigTx != null) {
      cashRefund = singleOrigTx.getAmount().abs();
    } else if (bonusOrigTx != null) {
      // Only bonus was debited, no cash refund needed
      cashRefund = BigDecimal.ZERO;
    } else {
      // Fallback: refund full bet amount to CASH (legacy rounds without transaction records)
      cashRefund = round.getBetAmount();
    }

    BigDecimal cashBalanceAfter = cashWallet.getBalance();
    if (cashRefund.compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal cashBefore = cashWallet.getBalance();
      cashBalanceAfter = cashBefore.add(cashRefund);

      WalletTransactionEntity cashRefundTx = new WalletTransactionEntity();
      cashRefundTx.setWalletId(cashWallet.getWalletId());
      cashRefundTx.setPlayerId(cashWallet.getPlayerId());
      cashRefundTx.setTransactionType(TransactionTypeEnum.ROLLBACK.getValue());
      cashRefundTx.setAmount(cashRefund);
      cashRefundTx.setBalanceBefore(cashBefore);
      cashRefundTx.setBalanceAfter(cashBalanceAfter);
      cashRefundTx.setRequestId(
          cashOrigTx != null ? "rollback:" + origTxId + ":cash" : "rollback:" + origTxId);
      cashRefundTx.setReferenceType("GAME_ROUND_ROLLBACK");
      cashRefundTx.setReferenceId(String.valueOf(round.getRoundId()));
      cashRefundTx.setDescription("Rollback cash bet: " + origTxId);

      cashWallet.setBalance(cashBalanceAfter);

      try {
        walletManager.credit(cashWallet, cashRefundTx);
      } catch (OptimisticLockingFailureException e) {
        return ResponseDTO.userErrorParam(GameErrorCode.ROLLBACK_FAILED.getMsg());
      }
    }

    // Update round to CANCELLED
    round.setStatus(RoundStatusEnum.CANCELLED.getValue());
    gameRoundDao.updateById(round);

    // Publish BET_CANCELLED event
    publishGameEvent(
        DomainEventTypeConst.BET_CANCELLED,
        form.getPlayerId(),
        tenantId,
        buildBetCancelledPayload(form, round, cashBalanceAfter));

    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(form.getOriginalTransactionId());
    response.setBalance(cashBalanceAfter);
    response.setBonusBalance(bonusBalanceAfter);
    response.setStatus(RoundStatusEnum.CANCELLED.getValue());
    return ResponseDTO.ok(response);
  }

  /**
   * Execute timeout (OPEN -> TIMEOUT).
   *
   * <p>Called by MissingSettlementPollJob for rounds that have been OPEN longer than the configured
   * threshold (typically 2 hours). No wallet mutation — just a status change.
   *
   * @param roundId the round ID to mark as timed out
   */
  @Transactional(rollbackFor = Throwable.class)
  public void executeTimeout(Long roundId) {
    GameRoundEntity round = gameRoundDao.selectById(roundId);
    if (round == null || !RoundStatusEnum.OPEN.getValue().equals(round.getStatus())) {
      return;
    }
    round.setStatus(RoundStatusEnum.TIMEOUT.getValue());
    gameRoundDao.updateById(round);

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("roundId", roundId);
    node.put("playerId", round.getPlayerId());
    node.put("gpRoundId", round.getGpRoundId());
    publishGameEvent(
        DomainEventTypeConst.ROUND_TIMEOUT, round.getPlayerId(), round.getTenantId(), node);

    log.info("Round timed out: roundId={}, gpRoundId={}", roundId, round.getGpRoundId());
  }

  /**
   * Execute resettlement (SETTLED -> ADJUSTED).
   *
   * <p>Admin-triggered operation that adjusts the payout amount for an already-settled round. If
   * the new payout is higher, credits the difference; if lower, debits the difference.
   *
   * @param roundId the round ID to adjust
   * @param newPayoutAmount the corrected payout amount
   * @param requestId idempotency key for the wallet transaction
   * @param tenantId tenant ID
   * @return callback response with updated balance
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<CallbackResponseVO> executeResettlement(
      Long roundId, BigDecimal newPayoutAmount, String requestId, Long tenantId) {
    GameRoundEntity round = gameRoundDao.selectById(roundId);
    if (round == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROUND_NOT_FOUND.getMsg());
    }
    if (!RoundStatusEnum.SETTLED.getValue().equals(round.getStatus())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_STATE_TRANSITION.getMsg());
    }

    BigDecimal delta = newPayoutAmount.subtract(round.getPayoutAmount());
    if (delta.compareTo(BigDecimal.ZERO) == 0) {
      CallbackResponseVO response = new CallbackResponseVO();
      response.setTransactionId(round.getTransactionId());
      response.setStatus(RoundStatusEnum.ADJUSTED.getValue());
      return ResponseDTO.ok(response);
    }

    WalletEntity wallet = findCashWallet(round.getPlayerId());
    if (wallet == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.PLAYER_WALLET_NOT_FOUND.getMsg());
    }

    BigDecimal balanceBefore = wallet.getBalance();
    BigDecimal balanceAfter = balanceBefore.add(delta);

    WalletTransactionEntity transaction = new WalletTransactionEntity();
    transaction.setWalletId(wallet.getWalletId());
    transaction.setPlayerId(wallet.getPlayerId());
    transaction.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
    transaction.setAmount(delta);
    transaction.setBalanceBefore(balanceBefore);
    transaction.setBalanceAfter(balanceAfter);
    transaction.setRequestId(requestId);
    transaction.setReferenceType("RESETTLEMENT");
    transaction.setReferenceId(String.valueOf(roundId));
    transaction.setDescription("Resettlement: round " + round.getGpRoundId());

    wallet.setBalance(balanceAfter);

    try {
      if (delta.compareTo(BigDecimal.ZERO) > 0) {
        walletManager.credit(wallet, transaction);
      } else {
        walletManager.debit(wallet, transaction);
      }
    } catch (OptimisticLockingFailureException e) {
      return ResponseDTO.userErrorParam(GameErrorCode.DEBIT_FAILED.getMsg());
    }

    round.setPayoutAmount(newPayoutAmount);
    round.setStatus(RoundStatusEnum.ADJUSTED.getValue());
    gameRoundDao.updateById(round);

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("roundId", roundId);
    node.put("playerId", round.getPlayerId());
    node.put("oldPayout", round.getPayoutAmount().toPlainString());
    node.put("newPayout", newPayoutAmount.toPlainString());
    node.put("delta", delta.toPlainString());
    node.put("balanceAfter", balanceAfter.toPlainString());
    publishGameEvent(DomainEventTypeConst.ROUND_ADJUSTED, round.getPlayerId(), tenantId, node);

    log.info("Round resettled: roundId={}, delta={}, newBalance={}", roundId, delta, balanceAfter);

    CallbackResponseVO response = new CallbackResponseVO();
    response.setTransactionId(round.getTransactionId());
    response.setBalance(balanceAfter);
    response.setStatus(RoundStatusEnum.ADJUSTED.getValue());
    return ResponseDTO.ok(response);
  }

  /**
   * Mark round as pending review.
   *
   * <p>Triggered when the GP is unavailable during reconciliation and the round status cannot be
   * determined. Operations team must manually investigate within 24h SLA.
   *
   * @param roundId the round ID
   * @param reason the reason for manual review
   */
  @Transactional(rollbackFor = Throwable.class)
  public void markPendingReview(Long roundId, String reason) {
    GameRoundEntity round = gameRoundDao.selectById(roundId);
    if (round == null) {
      return;
    }
    round.setStatus(RoundStatusEnum.PENDING_REVIEW.getValue());
    gameRoundDao.updateById(round);

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("roundId", roundId);
    node.put("playerId", round.getPlayerId());
    node.put("gpRoundId", round.getGpRoundId());
    node.put("reason", reason);
    publishGameEvent(
        DomainEventTypeConst.ROUND_PENDING_REVIEW, round.getPlayerId(), round.getTenantId(), node);

    log.info("Round marked for review: roundId={}, reason={}", roundId, reason);
  }

  /**
   * Load player's CASH wallet with DB pessimistic lock (Layer 2).
   *
   * <p>Uses SELECT FOR UPDATE to prevent concurrent modifications at the database level. The row
   * lock is held until the enclosing @Transactional method commits or rolls back.
   */
  private WalletEntity findCashWallet(Long playerId) {
    return walletDao.selectForUpdate(playerId, WalletTypeEnum.CASH.getValue());
  }

  private WalletEntity findBonusWallet(Long playerId) {
    return walletDao.selectForUpdate(playerId, WalletTypeEnum.BONUS.getValue());
  }

  private WalletTransactionEntity findTransactionByRequestId(String requestId) {
    return walletTransactionDao.selectOne(
        Wrappers.<WalletTransactionEntity>lambdaQuery()
            .eq(WalletTransactionEntity::getRequestId, requestId));
  }

  /**
   * Calculate eligible bonus balance for a specific game (within transaction, using locked data).
   */
  private BigDecimal calculateEligibleBonusForGame(Long bonusWalletId, String gameCode) {
    List<WalletBonusExtEntity> activeRecords =
        walletBonusExtDao.selectList(
            Wrappers.<WalletBonusExtEntity>lambdaQuery()
                .eq(WalletBonusExtEntity::getWalletId, bonusWalletId)
                .eq(WalletBonusExtEntity::getStatus, BonusStatusEnum.ACTIVE.getValue())
                .gt(WalletBonusExtEntity::getBalance, BigDecimal.ZERO));

    BigDecimal eligible = BigDecimal.ZERO;
    for (WalletBonusExtEntity ext : activeRecords) {
      if (isGameEligible(ext.getGameRestriction(), gameCode)) {
        eligible = eligible.add(ext.getBalance());
      }
    }
    return eligible;
  }

  private boolean isGameEligible(String gameRestriction, String gameCode) {
    if (gameRestriction == null || gameRestriction.isBlank()) {
      return true;
    }
    if (gameCode == null || gameCode.isBlank()) {
      return true;
    }
    try {
      JsonNode node = OBJECT_MAPPER.readTree(gameRestriction);
      JsonNode allowedGames = node.get("allowedGames");
      if (allowedGames != null && allowedGames.isArray()) {
        for (JsonNode game : allowedGames) {
          if (gameCode.equals(game.asText())) {
            return true;
          }
        }
        return false;
      }
      return true;
    } catch (Exception e) {
      log.warn("Failed to parse gameRestriction: {}", gameRestriction, e);
      return false;
    }
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishGameEvent(String eventType, Long playerId, Long tenantId, JsonNode payload) {
    domainEventPublisher.publish(
        IgamingKafkaConst.Topic.GAME_EVENTS,
        DomainEvent.builder()
            .eventType(eventType)
            .aggregateType("GameRound")
            .aggregateId(String.valueOf(playerId))
            .tenantId(tenantId)
            .payload(payload)
            .build());
  }

  private JsonNode buildBetPlacedPayload(
      CallbackDebitForm form,
      BigDecimal weightedTurnover,
      BigDecimal cashBalanceAfter,
      BigDecimal bonusDebit,
      BigDecimal cashDebit) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("playerId", form.getPlayerId());
    node.put("transactionId", form.getTransactionId());
    node.put("roundId", form.getRoundId());
    node.put("gameCode", form.getGameCode());
    node.put("amount", form.getAmount().toPlainString());
    node.put("bonusDebit", bonusDebit.toPlainString());
    node.put("cashDebit", cashDebit.toPlainString());
    node.put("weightedTurnover", weightedTurnover.toPlainString());
    node.put("cashBalanceAfter", cashBalanceAfter.toPlainString());
    return node;
  }

  private JsonNode buildRoundSettledPayload(CallbackCreditForm form, BigDecimal balanceAfter) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("playerId", form.getPlayerId());
    node.put("transactionId", form.getTransactionId());
    node.put("roundId", form.getRoundId());
    node.put("payoutAmount", form.getPayoutAmount().toPlainString());
    node.put("balanceAfter", balanceAfter.toPlainString());
    return node;
  }

  private JsonNode buildBetCancelledPayload(
      CallbackRollbackForm form, GameRoundEntity round, BigDecimal balanceAfter) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("playerId", form.getPlayerId());
    node.put("originalTransactionId", form.getOriginalTransactionId());
    node.put("refundAmount", round.getBetAmount().toPlainString());
    node.put("balanceAfter", balanceAfter.toPlainString());
    return node;
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
