package net.lab1024.sa.igaming.activity.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.service.PromotionCacheAdminService;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Promotion cache admin controller — admin endpoints for promotion cache management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.ACTIVITY)
@RequiredArgsConstructor
public class PromotionCacheAdminController {

  private final PromotionCacheAdminService promotionCacheAdminService;

  @Operation(summary = "Get cached active promotion rules (admin)")
  @GetMapping("/igaming/admin/activity/cache/rules")
  @SaCheckPermission("activity:cache:admin")
  public ResponseDTO<List<PromotionRuleVO>> getActiveRules() {
    return promotionCacheAdminService.getActiveRules();
  }

  @Operation(summary = "Get cached promotion rule by code (admin)")
  @GetMapping("/igaming/admin/activity/cache/rule/{code}")
  @SaCheckPermission("activity:cache:admin")
  public ResponseDTO<PromotionRuleVO> getRuleByCode(@PathVariable String code) {
    return promotionCacheAdminService.getRuleByCode(code);
  }

  @Operation(summary = "Evict promotion cache (admin)")
  @PostMapping("/igaming/admin/activity/cache/evict")
  @SaCheckPermission("activity:cache:admin")
  public ResponseDTO<String> evictCache() {
    return promotionCacheAdminService.evictCache();
  }
}
