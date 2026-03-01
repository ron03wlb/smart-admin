package net.lab1024.sa.igaming.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.game.adapter.GpSignatureVerifier;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.manager.GameTransactionManager;
import org.springframework.stereotype.Service;

/**
 * Game callback service — orchestrates GP callback processing.
 *
 * <p>Handles signature verification, Layer 1 idempotency checks, distributed lock acquisition
 * (Layer 1 concurrency control), and delegates to GameTransactionManager for atomic operations.
 *
 * <p>3-Layer Concurrency Control:
 *
 * <ul>
 *   <li>Layer 1: Redisson distributed lock (this class) — cross-instance protection
 *   <li>Layer 2: SELECT FOR UPDATE (GameTransactionManager) — DB pessimistic lock
 *   <li>Layer 3: @Version optimistic lock (WalletEntity) — final safety net
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameCallbackService {

  private static final String WALLET_LOCK_PREFIX = "wallet:lock:";
  private static final String ROUND_LOCK_PREFIX = "round:lock:";

  private final GpSignatureVerifier gpSignatureVerifier;
  private final GameRoundDao gameRoundDao;
  private final GameTransactionManager gameTransactionManager;
  private final LockService lockService;
  private final IgamingProperties igamingProperties;

  /**
   * Process debit callback (bet placement).
   *
   * @param form debit form from GP
   * @return callback response with transactionId and balance
   */
  public ResponseDTO<CallbackResponseVO> processDebit(CallbackDebitForm form) {
    // Verify GP signature
    String payload = form.getProviderCode() + form.getTransactionId() + form.getAmount();
    if (!gpSignatureVerifier.verify(
        form.getProviderCode(), payload, form.getSignature(), form.getTimestamp())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_SIGNATURE.getMsg());
    }

    // Idempotency Layer 1: check if transactionId already processed (NO LOCK)
    GameRoundEntity existing = gameRoundDao.selectByTransactionId(form.getTransactionId());
    if (existing != null) {
      log.info("Idempotent debit Layer 1: transactionId={}", form.getTransactionId());
      CallbackResponseVO response = new CallbackResponseVO();
      response.setTransactionId(existing.getTransactionId());
      response.setStatus(existing.getStatus());
      return ResponseDTO.ok(response);
    }

    Long tenantId = TenantContext.getTenantId();

    // Acquire distributed locks (Layer 1 concurrency) then delegate to Manager
    try {
      return lockService.executeWithLock(
          WALLET_LOCK_PREFIX + form.getPlayerId(),
          igamingProperties.getLock().getWaitMs(),
          igamingProperties.getLock().getLeaseMs(),
          () ->
              lockService.executeWithLock(
                  ROUND_LOCK_PREFIX + form.getPlayerId() + ":" + form.getRoundId(),
                  igamingProperties.getLock().getWaitMs(),
                  igamingProperties.getLock().getLeaseMs(),
                  () -> gameTransactionManager.executeDebit(form, tenantId)));
    } catch (IllegalStateException e) {
      log.warn("Lock acquisition failed for debit: playerId={}", form.getPlayerId());
      return ResponseDTO.userErrorParam(GameErrorCode.LOCK_ACQUISITION_FAILED.getMsg());
    }
  }

  /**
   * Process credit callback (win settlement).
   *
   * @param form credit form from GP
   * @return callback response with transactionId and balance
   */
  public ResponseDTO<CallbackResponseVO> processCredit(CallbackCreditForm form) {
    // Verify GP signature
    String payload = form.getProviderCode() + form.getTransactionId() + form.getPayoutAmount();
    if (!gpSignatureVerifier.verify(
        form.getProviderCode(), payload, form.getSignature(), form.getTimestamp())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_SIGNATURE.getMsg());
    }

    Long tenantId = TenantContext.getTenantId();

    // Acquire distributed locks then delegate to Manager
    try {
      return lockService.executeWithLock(
          WALLET_LOCK_PREFIX + form.getPlayerId(),
          igamingProperties.getLock().getWaitMs(),
          igamingProperties.getLock().getLeaseMs(),
          () ->
              lockService.executeWithLock(
                  ROUND_LOCK_PREFIX + form.getPlayerId() + ":" + form.getRoundId(),
                  igamingProperties.getLock().getWaitMs(),
                  igamingProperties.getLock().getLeaseMs(),
                  () -> gameTransactionManager.executeCredit(form, tenantId)));
    } catch (IllegalStateException e) {
      log.warn("Lock acquisition failed for credit: playerId={}", form.getPlayerId());
      return ResponseDTO.userErrorParam(GameErrorCode.LOCK_ACQUISITION_FAILED.getMsg());
    }
  }

  /**
   * Process rollback callback (bet cancellation).
   *
   * @param form rollback form from GP
   * @return callback response with transactionId and balance
   */
  public ResponseDTO<CallbackResponseVO> processRollback(CallbackRollbackForm form) {
    // Verify GP signature
    String payload = form.getProviderCode() + form.getOriginalTransactionId();
    if (!gpSignatureVerifier.verify(
        form.getProviderCode(), payload, form.getSignature(), form.getTimestamp())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_SIGNATURE.getMsg());
    }

    Long tenantId = TenantContext.getTenantId();

    // Acquire player-level distributed lock then delegate to Manager
    try {
      return lockService.executeWithLock(
          WALLET_LOCK_PREFIX + form.getPlayerId(),
          igamingProperties.getLock().getWaitMs(),
          igamingProperties.getLock().getLeaseMs(),
          () -> gameTransactionManager.executeRollback(form, tenantId));
    } catch (IllegalStateException e) {
      log.warn("Lock acquisition failed for rollback: playerId={}", form.getPlayerId());
      return ResponseDTO.userErrorParam(GameErrorCode.LOCK_ACQUISITION_FAILED.getMsg());
    }
  }

  /**
   * Query round status (read-only, no lock required).
   *
   * @param gpRoundId GP round identifier
   * @return callback response with round status
   */
  public ResponseDTO<CallbackResponseVO> queryRound(String gpRoundId) {
    Long tenantId = TenantContext.getTenantId();
    GameRoundEntity round =
        gameRoundDao.selectOne(
            Wrappers.<GameRoundEntity>lambdaQuery()
                .eq(GameRoundEntity::getGpRoundId, gpRoundId)
                .eq(GameRoundEntity::getTenantId, tenantId));
    if (round == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROUND_NOT_FOUND.getMsg());
    }
    CallbackResponseVO vo = new CallbackResponseVO();
    vo.setTransactionId(round.getTransactionId());
    vo.setStatus(round.getStatus());
    return ResponseDTO.ok(vo);
  }
}
