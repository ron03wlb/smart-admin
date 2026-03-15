package net.lab1024.sa.igaming.activity.turnover.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverStatusFactorRuleVO;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverStatusFactorRuleService;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Turnover status factor rule controller — REST API endpoints for status factor rule management.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class TurnoverStatusFactorRuleController {

  private final TurnoverStatusFactorRuleService statusFactorRuleService;

  @Operation(summary = "Get status factor rule by ID")
  @GetMapping("/igaming/activity/turnover/status-factor/get/{ruleId}")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<TurnoverStatusFactorRuleVO> getRule(@PathVariable Long ruleId) {
    return statusFactorRuleService
        .getRule(ruleId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("規則不存在"));
  }

  @Operation(summary = "Query status factor rules with pagination")
  @PostMapping("/igaming/activity/turnover/status-factor/query")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<PageResult<TurnoverStatusFactorRuleVO>> queryRules(
      @RequestBody @Valid TurnoverStatusFactorRuleQueryForm form) {
    return statusFactorRuleService.queryRules(form);
  }

  @Operation(summary = "Add a new status factor rule")
  @PostMapping("/igaming/activity/turnover/status-factor/add")
  @SaCheckPermission("activity:turnover:add")
  public ResponseDTO<Void> addRule(
      @RequestBody @Valid TurnoverStatusFactorRuleAddForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return statusFactorRuleService.addRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Update an existing status factor rule")
  @PutMapping("/igaming/activity/turnover/status-factor/update")
  @SaCheckPermission("activity:turnover:update")
  public ResponseDTO<Void> updateRule(
      @RequestBody @Valid TurnoverStatusFactorRuleUpdateForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return statusFactorRuleService.updateRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Delete a status factor rule (soft delete)")
  @DeleteMapping("/igaming/activity/turnover/status-factor/delete/{ruleId}")
  @SaCheckPermission("activity:turnover:delete")
  public ResponseDTO<Void> deleteRule(
      @PathVariable Long ruleId,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return statusFactorRuleService.deleteRule(ruleId, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Evict cache for status factor rules")
  @PostMapping("/igaming/activity/turnover/status-factor/evict-cache")
  @SaCheckPermission("activity:turnover:cache")
  public ResponseDTO<Void> evictCache() {
    return statusFactorRuleService.evictCache();
  }
}
