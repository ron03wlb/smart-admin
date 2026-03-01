package net.lab1024.sa.igaming.game.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.code.GameErrorCode;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.ResettlementForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.manager.GameTransactionManager;
import org.springframework.stereotype.Service;

/**
 * Game round admin service — orchestrates admin resettlement operations.
 *
 * <p>Performs pre-checks (tenant, round existence, round status) outside the distributed lock, then
 * acquires a wallet-level lock before delegating to GameTransactionManager for the atomic
 * resettlement.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoundAdminService {

  private static final String WALLET_LOCK_PREFIX = "wallet:lock:";

  private final GameRoundDao gameRoundDao;
  private final GameTransactionManager gameTransactionManager;
  private final LockService lockService;
  private final IgamingProperties igamingProperties;

  /**
   * Resettle a game round with a corrected payout amount.
   *
   * @param form resettlement form
   * @return callback response with updated balance
   */
  public ResponseDTO<CallbackResponseVO> resettle(ResettlementForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    // Pre-check: round existence and status (no lock)
    GameRoundEntity round = gameRoundDao.selectById(form.getRoundId());
    if (round == null) {
      return ResponseDTO.userErrorParam(GameErrorCode.ROUND_NOT_FOUND.getMsg());
    }
    if (!RoundStatusEnum.SETTLED.getValue().equals(round.getStatus())) {
      return ResponseDTO.userErrorParam(GameErrorCode.INVALID_STATE_TRANSITION.getMsg());
    }

    // Acquire distributed lock then delegate to Manager
    try {
      return lockService.executeWithLock(
          WALLET_LOCK_PREFIX + round.getPlayerId(),
          igamingProperties.getLock().getWaitMs(),
          igamingProperties.getLock().getLeaseMs(),
          () ->
              gameTransactionManager.executeResettlement(
                  form.getRoundId(), form.getNewPayoutAmount(), form.getRequestId(), tenantId));
    } catch (IllegalStateException e) {
      log.warn("Lock acquisition failed for resettlement: roundId={}", form.getRoundId());
      return ResponseDTO.userErrorParam(GameErrorCode.LOCK_ACQUISITION_FAILED.getMsg());
    }
  }
}
