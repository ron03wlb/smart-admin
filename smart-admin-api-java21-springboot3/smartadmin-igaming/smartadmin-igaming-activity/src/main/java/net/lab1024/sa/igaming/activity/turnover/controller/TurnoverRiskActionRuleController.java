package net.lab1024.sa.igaming.activity.turnover.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverRiskActionRuleVO;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverRiskActionRuleService;
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
 * Turnover risk action rule controller — REST API endpoints for risk action rule management.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class TurnoverRiskActionRuleController {

  private final TurnoverRiskActionRuleService riskActionRuleService;

  @Operation(summary = "Get risk action rule by ID")
  @GetMapping("/igaming/activity/turnover/risk-action/get/{ruleId}")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<TurnoverRiskActionRuleVO> getRule(@PathVariable Long ruleId) {
    return riskActionRuleService
        .getRule(ruleId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("規則不存在"));
  }

  @Operation(summary = "Query risk action rules with pagination")
  @PostMapping("/igaming/activity/turnover/risk-action/query")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<PageResult<TurnoverRiskActionRuleVO>> queryRules(
      @RequestBody @Valid TurnoverRiskActionRuleQueryForm form) {
    return riskActionRuleService.queryRules(form);
  }

  @Operation(summary = "Add a new risk action rule")
  @PostMapping("/igaming/activity/turnover/risk-action/add")
  @SaCheckPermission("activity:turnover:add")
  public ResponseDTO<Void> addRule(
      @RequestBody @Valid TurnoverRiskActionRuleAddForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return riskActionRuleService.addRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Update an existing risk action rule")
  @PutMapping("/igaming/activity/turnover/risk-action/update")
  @SaCheckPermission("activity:turnover:update")
  public ResponseDTO<Void> updateRule(
      @RequestBody @Valid TurnoverRiskActionRuleUpdateForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return riskActionRuleService.updateRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Delete a risk action rule (soft delete)")
  @DeleteMapping("/igaming/activity/turnover/risk-action/delete/{ruleId}")
  @SaCheckPermission("activity:turnover:delete")
  public ResponseDTO<Void> deleteRule(
      @PathVariable Long ruleId,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return riskActionRuleService.deleteRule(ruleId, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Evict cache for risk action rules")
  @PostMapping("/igaming/activity/turnover/risk-action/evict-cache")
  @SaCheckPermission("activity:turnover:cache")
  public ResponseDTO<Void> evictCache() {
    return riskActionRuleService.evictCache();
  }
}
