package net.lab1024.sa.igaming.activity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleAddForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleQueryForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleUpdateForm;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.service.PromotionRuleService;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Promotion Rule Controller — REST API endpoints for promotion management.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class PromotionRuleController {

  private final PromotionRuleService promotionRuleService;

  @Operation(summary = "Get promotion rule by ID")
  @GetMapping("/igaming/activity/promotion/get/{ruleId}")
  @SaCheckPermission("activity:promotion:query")
  public ResponseDTO<PromotionRuleVO> getRule(@PathVariable Long ruleId) {
    return promotionRuleService
        .getRule(ruleId)
        .map(ResponseDTO::ok)
        .getOrElse(
            () -> ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Query promotion rules with pagination")
  @PostMapping("/igaming/activity/promotion/query")
  @SaCheckPermission("activity:promotion:query")
  public ResponseDTO<PageResult<PromotionRuleVO>> queryRules(
      @RequestBody @Valid PromotionRuleQueryForm form) {
    return promotionRuleService.queryRules(form);
  }

  @Operation(summary = "Add a new promotion rule")
  @PostMapping("/igaming/activity/promotion/add")
  @SaCheckPermission("activity:promotion:add")
  public ResponseDTO<PromotionRuleVO> addRule(@RequestBody @Valid PromotionRuleAddForm form) {
    return promotionRuleService.addRule(form);
  }

  @Operation(summary = "Update promotion rule")
  @PutMapping("/igaming/activity/promotion/update")
  @SaCheckPermission("activity:promotion:update")
  public ResponseDTO<Void> updateRule(@RequestBody @Valid PromotionRuleUpdateForm form) {
    return promotionRuleService.updateRule(form);
  }

  @Operation(summary = "Toggle promotion rule status")
  @PostMapping("/igaming/activity/promotion/toggle-status/{ruleId}")
  @SaCheckPermission("activity:promotion:update")
  public ResponseDTO<Void> toggleStatus(@PathVariable Long ruleId, @RequestParam Integer status) {
    return promotionRuleService.toggleStatus(ruleId, status);
  }
}
