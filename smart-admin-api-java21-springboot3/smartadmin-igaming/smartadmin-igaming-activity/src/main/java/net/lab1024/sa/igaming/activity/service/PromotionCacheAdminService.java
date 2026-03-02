package net.lab1024.sa.igaming.activity.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import org.springframework.stereotype.Service;

/**
 * Promotion cache admin service — admin operations for promotion cache management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionCacheAdminService {

  private final PromotionCacheManager promotionCacheManager;

  /**
   * Get cached active promotion rules for the current tenant.
   *
   * @return list of promotion rule VOs from cache
   */
  public ResponseDTO<List<PromotionRuleVO>> getActiveRules() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    List<PromotionRuleEntity> entities = promotionCacheManager.getActiveRules(tenantId);
    List<PromotionRuleVO> voList = SmartBeanUtil.copyList(entities, PromotionRuleVO.class);
    return ResponseDTO.ok(voList);
  }

  /**
   * Get a cached promotion rule by code for the current tenant.
   *
   * @param code promotion code
   * @return promotion rule VO from cache, or error if not found
   */
  public ResponseDTO<PromotionRuleVO> getRuleByCode(String code) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    PromotionRuleEntity entity = promotionCacheManager.getRuleByCode(tenantId, code);
    if (entity == null) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
    }
    PromotionRuleVO vo = SmartBeanUtil.copy(entity, PromotionRuleVO.class);
    return ResponseDTO.ok(vo);
  }

  /**
   * Evict promotion cache for the current tenant.
   *
   * @return success response
   */
  public ResponseDTO<String> evictCache() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    promotionCacheManager.evictCache(tenantId);
    log.info("Promotion cache evicted by admin: tenantId={}", tenantId);
    return ResponseDTO.ok();
  }
}
