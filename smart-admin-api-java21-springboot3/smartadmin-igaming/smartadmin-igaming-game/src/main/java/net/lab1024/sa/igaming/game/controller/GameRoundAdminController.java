package net.lab1024.sa.igaming.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.game.domain.form.ResettlementForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.service.GameRoundAdminService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Game Round Admin Controller — admin endpoints for game round management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.GAME)
@RequiredArgsConstructor
public class GameRoundAdminController {

  private final GameRoundAdminService gameRoundAdminService;

  @SaCheckPermission("game:round:resettle")
  @Operation(summary = "Resettle game round")
  @PostMapping("/igaming/admin/game/round/resettle")
  public ResponseDTO<CallbackResponseVO> resettle(@RequestBody @Valid ResettlementForm form) {
    return gameRoundAdminService.resettle(form);
  }
}
