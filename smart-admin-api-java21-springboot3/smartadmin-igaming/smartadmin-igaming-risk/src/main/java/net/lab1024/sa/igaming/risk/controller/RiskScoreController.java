package net.lab1024.sa.igaming.risk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.risk.domain.form.RiskScoreQueryForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskScoreVO;
import net.lab1024.sa.igaming.risk.service.RiskScoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk score query controller.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.RISK)
@RequiredArgsConstructor
public class RiskScoreController {

  private final RiskScoreService riskScoreService;

  @Operation(summary = "Query risk scores with pagination")
  @PostMapping("/igaming/risk/score/query")
  @SaCheckPermission("risk:score:query")
  public ResponseDTO<PageResult<RiskScoreVO>> queryPage(
      @RequestBody @Valid RiskScoreQueryForm form) {
    return riskScoreService.queryPage(form);
  }

  @Operation(summary = "Get risk score by player ID")
  @GetMapping("/igaming/risk/score/player/{playerId}")
  @SaCheckPermission("risk:score:query")
  public ResponseDTO<RiskScoreVO> getByPlayerId(
      @PathVariable Long playerId, @RequestParam Long tenantId) {
    return riskScoreService.getByPlayerId(playerId, tenantId);
  }
}
