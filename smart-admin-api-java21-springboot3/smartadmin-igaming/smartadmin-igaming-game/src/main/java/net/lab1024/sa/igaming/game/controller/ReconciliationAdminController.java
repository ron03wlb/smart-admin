package net.lab1024.sa.igaming.game.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.game.domain.form.ReconciliationTriggerForm;
import net.lab1024.sa.igaming.game.domain.vo.ReconciliationVO;
import net.lab1024.sa.igaming.game.service.ReconciliationAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reconciliation Admin Controller — admin endpoints for reconciliation management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.GAME)
@RequiredArgsConstructor
public class ReconciliationAdminController {

  private final ReconciliationAdminService reconciliationAdminService;

  @SaCheckPermission("game:reconciliation:admin")
  @Operation(summary = "Trigger daily batch reconciliation")
  @PostMapping("/igaming/admin/reconciliation/trigger")
  public ResponseDTO<String> trigger(@RequestBody @Valid ReconciliationTriggerForm form) {
    return reconciliationAdminService.triggerDailyReconciliation(form);
  }

  @SaCheckPermission("game:reconciliation:admin")
  @Operation(summary = "Poll for missing settlements")
  @PostMapping("/igaming/admin/reconciliation/poll")
  public ResponseDTO<Integer> poll() {
    return reconciliationAdminService.triggerMissingSettlementPoll();
  }

  @SaCheckPermission("game:reconciliation:admin")
  @Operation(summary = "Query reconciliation status by date")
  @GetMapping("/igaming/admin/reconciliation/status")
  public ResponseDTO<List<ReconciliationVO>> status(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return reconciliationAdminService.queryReconciliationStatus(date);
  }
}
