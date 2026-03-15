package net.lab1024.sa.igaming.activity.turnover.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverOddsThresholdRuleVO;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverOddsThresholdRuleService;
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
 * Turnover odds threshold rule controller — REST API endpoints for odds threshold rule management.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class TurnoverOddsThresholdRuleController {

  private final TurnoverOddsThresholdRuleService oddsThresholdRuleService;

  @Operation(summary = "Get odds threshold rule by ID")
  @GetMapping("/igaming/activity/turnover/odds-threshold/get/{ruleId}")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<TurnoverOddsThresholdRuleVO> getRule(@PathVariable Long ruleId) {
    return oddsThresholdRuleService
        .getRule(ruleId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("規則不存在"));
  }

  @Operation(summary = "Query odds threshold rules with pagination")
  @PostMapping("/igaming/activity/turnover/odds-threshold/query")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<PageResult<TurnoverOddsThresholdRuleVO>> queryRules(
      @RequestBody @Valid TurnoverOddsThresholdRuleQueryForm form) {
    return oddsThresholdRuleService.queryRules(form);
  }

  @Operation(summary = "Add a new odds threshold rule")
  @PostMapping("/igaming/activity/turnover/odds-threshold/add")
  @SaCheckPermission("activity:turnover:add")
  public ResponseDTO<Void> addRule(
      @RequestBody @Valid TurnoverOddsThresholdRuleAddForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return oddsThresholdRuleService.addRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Update an existing odds threshold rule")
  @PutMapping("/igaming/activity/turnover/odds-threshold/update")
  @SaCheckPermission("activity:turnover:update")
  public ResponseDTO<Void> updateRule(
      @RequestBody @Valid TurnoverOddsThresholdRuleUpdateForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return oddsThresholdRuleService.updateRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Delete an odds threshold rule (soft delete)")
  @DeleteMapping("/igaming/activity/turnover/odds-threshold/delete/{ruleId}")
  @SaCheckPermission("activity:turnover:delete")
  public ResponseDTO<Void> deleteRule(
      @PathVariable Long ruleId,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return oddsThresholdRuleService.deleteRule(ruleId, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Evict cache for odds threshold rules")
  @PostMapping("/igaming/activity/turnover/odds-threshold/evict-cache")
  @SaCheckPermission("activity:turnover:cache")
  public ResponseDTO<Void> evictCache() {
    return oddsThresholdRuleService.evictCache();
  }
}
