package net.lab1024.sa.igaming.activity.turnover.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverGameWeightRuleVO;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverGameWeightRuleService;
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
 * Turnover game weight rule controller — REST API endpoints for game weight rule management.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class TurnoverGameWeightRuleController {

  private final TurnoverGameWeightRuleService gameWeightRuleService;

  @Operation(summary = "Get game weight rule by ID")
  @GetMapping("/igaming/activity/turnover/game-weight/get/{ruleId}")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<TurnoverGameWeightRuleVO> getRule(@PathVariable Long ruleId) {
    return gameWeightRuleService
        .getRule(ruleId)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("規則不存在"));
  }

  @Operation(summary = "Query game weight rules with pagination")
  @PostMapping("/igaming/activity/turnover/game-weight/query")
  @SaCheckPermission("activity:turnover:query")
  public ResponseDTO<PageResult<TurnoverGameWeightRuleVO>> queryRules(
      @RequestBody @Valid TurnoverGameWeightRuleQueryForm form) {
    return gameWeightRuleService.queryRules(form);
  }

  @Operation(summary = "Add a new game weight rule")
  @PostMapping("/igaming/activity/turnover/game-weight/add")
  @SaCheckPermission("activity:turnover:add")
  public ResponseDTO<Void> addRule(
      @RequestBody @Valid TurnoverGameWeightRuleAddForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return gameWeightRuleService.addRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Update an existing game weight rule")
  @PutMapping("/igaming/activity/turnover/game-weight/update")
  @SaCheckPermission("activity:turnover:update")
  public ResponseDTO<Void> updateRule(
      @RequestBody @Valid TurnoverGameWeightRuleUpdateForm form,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return gameWeightRuleService.updateRule(form, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Delete a game weight rule (soft delete)")
  @DeleteMapping("/igaming/activity/turnover/game-weight/delete/{ruleId}")
  @SaCheckPermission("activity:turnover:delete")
  public ResponseDTO<Void> deleteRule(
      @PathVariable Long ruleId,
      @RequestParam Long operatorId,
      @RequestParam String operatorName,
      @RequestParam(required = false) String changeReason) {
    return gameWeightRuleService.deleteRule(ruleId, operatorId, operatorName, changeReason);
  }

  @Operation(summary = "Evict cache for game weight rules")
  @PostMapping("/igaming/activity/turnover/game-weight/evict-cache")
  @SaCheckPermission("activity:turnover:cache")
  public ResponseDTO<Void> evictCache() {
    return gameWeightRuleService.evictCache();
  }
}
