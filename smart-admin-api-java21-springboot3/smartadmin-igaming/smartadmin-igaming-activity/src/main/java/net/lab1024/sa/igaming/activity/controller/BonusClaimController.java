package net.lab1024.sa.igaming.activity.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.domain.form.BonusClaimForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.BonusClaimResultVO;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.activity.service.BonusClaimService;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bonus Claim Controller — REST API endpoints for bonus claiming and wagering progress.
 *
 * <p>TODO: Replace @RequestParam playerId with session-based player identity (e.g. StpUtil or
 * PlayerContext) once iGaming player authentication module is implemented. Current MVP accepts
 * playerId as parameter for development/testing convenience.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class BonusClaimController {

  private final BonusClaimService bonusClaimService;

  @Operation(summary = "Claim a bonus")
  @PostMapping("/igaming/activity/bonus/claim")
  public ResponseDTO<BonusClaimResultVO> claimBonus(
      @RequestParam Long playerId, @RequestBody @Valid BonusClaimForm form) {
    Long tenantId = TenantContext.getTenantId();
    return bonusClaimService.claimBonus(playerId, form, tenantId);
  }

  @Operation(summary = "Query bonus records for a player")
  @PostMapping("/igaming/activity/bonus/records")
  public ResponseDTO<PageResult<PlayerBonusRecordVO>> queryBonusRecords(
      @RequestParam Long playerId, @RequestBody @Valid WageringProgressQueryForm form) {
    return bonusClaimService.queryBonusRecords(playerId, form);
  }

  @Operation(summary = "Get wagering progress for a bonus record")
  @GetMapping("/igaming/activity/bonus/progress/{recordId}")
  public ResponseDTO<WageringProgressVO> getWageringProgress(
      @RequestParam Long playerId, @PathVariable Long recordId) {
    return bonusClaimService
        .getWageringProgress(playerId, recordId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(ActivityErrorCode.BONUS_NOT_FOUND.getMsg()));
  }
}
