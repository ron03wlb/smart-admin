package net.lab1024.sa.igaming.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
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
 * <p>Handles signature verification, Layer 1 idempotency checks, and delegates to
 * GameTransactionManager for atomic operations.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameCallbackService {

  private final GpSignatureVerifier gpSignatureVerifier;
  private final GameRoundDao gameRoundDao;
  private final GameTransactionManager gameTransactionManager;

  /**
   * Process debit callback (bet placement).
   *
   * @param form debit form from GP
   * @param tenantId current tenant
   * @return callback response with transactionId and balance
   */
  public ResponseDTO<CallbackResponseVO> processDebit(CallbackDebitForm form, Long tenantId) {
    // Verify GP signature
    String payload = form.getProviderCode() + form.getTransactionId() + form.getAmount();
    if (!gpSignatureVerifier.verify(
        form.getProviderCode(), payload, form.getSignature(), form.getTimestamp())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_SIGNATURE.getMsg());
    }

    // Idempotency Layer 1: check if transactionId already processed
    GameRoundEntity existing = gameRoundDao.selectByTransactionId(form.getTransactionId());
    if (existing != null) {
      log.info("Idempotent debit Layer 1: transactionId={}", form.getTransactionId());
      CallbackResponseVO response = new CallbackResponseVO();
      response.setTransactionId(existing.getTransactionId());
      response.setStatus(existing.getStatus());
      return ResponseDTO.ok(response);
    }

    return gameTransactionManager.executeDebit(form, tenantId);
  }

  /**
   * Process credit callback (win settlement).
   *
   * @param form credit form from GP
   * @param tenantId current tenant
   * @return callback response with transactionId and balance
   */
  public ResponseDTO<CallbackResponseVO> processCredit(CallbackCreditForm form, Long tenantId) {
    // Verify GP signature
    String payload = form.getProviderCode() + form.getTransactionId() + form.getPayoutAmount();
    if (!gpSignatureVerifier.verify(
        form.getProviderCode(), payload, form.getSignature(), form.getTimestamp())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_SIGNATURE.getMsg());
    }

    return gameTransactionManager.executeCredit(form, tenantId);
  }

  /**
   * Process rollback callback (bet cancellation).
   *
   * @param form rollback form from GP
   * @param tenantId current tenant
   * @return callback response with transactionId and balance
   */
  public ResponseDTO<CallbackResponseVO> processRollback(CallbackRollbackForm form, Long tenantId) {
    // Verify GP signature
    String payload = form.getProviderCode() + form.getOriginalTransactionId();
    if (!gpSignatureVerifier.verify(
        form.getProviderCode(), payload, form.getSignature(), form.getTimestamp())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_SIGNATURE.getMsg());
    }

    return gameTransactionManager.executeRollback(form, tenantId);
  }
}
