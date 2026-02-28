package net.lab1024.sa.igaming.game.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.annotation.NoNeedLogin;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.service.GameCallbackService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Game Callback Controller — REST API endpoints for GP seamless wallet callbacks.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.GAME)
@RequiredArgsConstructor
public class GameCallbackController {

  private final GameCallbackService gameCallbackService;

  @NoNeedLogin
  @Operation(summary = "GP debit callback (bet placement)")
  @PostMapping("/igaming/game/callback/debit")
  public ResponseDTO<CallbackResponseVO> debit(
      @RequestBody @Valid CallbackDebitForm form,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId) {
    return gameCallbackService.processDebit(form, tenantId);
  }

  @NoNeedLogin
  @Operation(summary = "GP credit callback (win settlement)")
  @PostMapping("/igaming/game/callback/credit")
  public ResponseDTO<CallbackResponseVO> credit(
      @RequestBody @Valid CallbackCreditForm form,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId) {
    return gameCallbackService.processCredit(form, tenantId);
  }

  @NoNeedLogin
  @Operation(summary = "GP rollback callback (bet cancellation)")
  @PostMapping("/igaming/game/callback/rollback")
  public ResponseDTO<CallbackResponseVO> rollback(
      @RequestBody @Valid CallbackRollbackForm form,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId) {
    return gameCallbackService.processRollback(form, tenantId);
  }

  @NoNeedLogin
  @Operation(summary = "Query round status")
  @GetMapping("/igaming/game/callback/query")
  public ResponseDTO<CallbackResponseVO> query(
      @RequestParam String gpRoundId,
      @RequestHeader(value = "X-Tenant-Id", defaultValue = "1") Long tenantId) {
    return gameCallbackService.queryRound(gpRoundId, tenantId);
  }
}
