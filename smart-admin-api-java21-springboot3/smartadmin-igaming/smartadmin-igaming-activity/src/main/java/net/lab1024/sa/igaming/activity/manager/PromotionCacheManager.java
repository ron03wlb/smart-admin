package net.lab1024.sa.igaming.activity.manager;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * Promotion cache manager — handles promotion rule caching.
 *
 * <p>{@code @Cacheable} / {@code @CacheEvict} must reside in Manager layer per SmartAdmin rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Component
@RequiredArgsConstructor
public class PromotionCacheManager {

  private final PromotionRuleDao promotionRuleDao;

  @Cacheable(value = "promotion:active", key = "'tenant:' + #tenantId")
  public List<PromotionRuleEntity> getActiveRules(Long tenantId) {
    return promotionRuleDao.selectActiveRules(tenantId);
  }

  @Cacheable(value = "promotion:code", key = "'tenant:' + #tenantId + ':code:' + #code")
  public PromotionRuleEntity getRuleByCode(Long tenantId, String code) {
    return promotionRuleDao.selectByCode(tenantId, code);
  }

  @CacheEvict(
      value = {"promotion:active", "promotion:code"},
      allEntries = true)
  public void evictCache(Long tenantId) {
    // Cache eviction only
  }
}
