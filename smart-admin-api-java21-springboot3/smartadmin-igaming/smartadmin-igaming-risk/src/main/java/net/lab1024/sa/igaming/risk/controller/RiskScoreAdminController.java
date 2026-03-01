package net.lab1024.sa.igaming.risk.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.risk.domain.form.RiskResetAutoLockForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskAssessmentVO;
import net.lab1024.sa.igaming.risk.service.RiskScoreAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Risk score admin controller — admin operations for score management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.RISK)
@RequiredArgsConstructor
public class RiskScoreAdminController {

  private final RiskScoreAdminService riskScoreAdminService;

  @Operation(summary = "Reset auto-lock on a player's risk score profile")
  @PostMapping("/igaming/admin/risk/score/resetlock")
  @SaCheckPermission("risk:score:admin")
  public ResponseDTO<String> resetLock(@RequestBody @Valid RiskResetAutoLockForm form) {
    return riskScoreAdminService.resetAutoLock(form);
  }

  @Operation(summary = "Query a player's risk assessment history")
  @GetMapping("/igaming/admin/risk/assessment/player/{playerId}")
  @SaCheckPermission("risk:score:admin")
  public ResponseDTO<List<RiskAssessmentVO>> queryAssessments(@PathVariable Long playerId) {
    return riskScoreAdminService.queryPlayerAssessments(playerId);
  }
}
