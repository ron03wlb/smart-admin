package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PromotionCacheManager unit tests — promotion rule caching delegation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionCacheManager 單元測試")
class PromotionCacheManagerTest {

  @Mock private PromotionRuleDao promotionRuleDao;

  @InjectMocks private PromotionCacheManager promotionCacheManager;

  @Test
  @DisplayName("getActiveRules → 委派至 promotionRuleDao.selectActiveRules")
  void getActiveRules_delegatesToDao() {
    PromotionRuleEntity rule = new PromotionRuleEntity();
    rule.setPromotionCode("WELCOME_BONUS");
    when(promotionRuleDao.selectActiveRules(1L)).thenReturn(List.of(rule));

    List<PromotionRuleEntity> result = promotionCacheManager.getActiveRules(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPromotionCode()).isEqualTo("WELCOME_BONUS");
    verify(promotionRuleDao).selectActiveRules(1L);
  }

  @Test
  @DisplayName("getRuleByCode → 委派至 promotionRuleDao.selectByCode")
  void getRuleByCode_delegatesToDao() {
    PromotionRuleEntity rule = new PromotionRuleEntity();
    rule.setPromotionCode("DEPOSIT_50");
    when(promotionRuleDao.selectByCode(1L, "DEPOSIT_50")).thenReturn(rule);

    PromotionRuleEntity result = promotionCacheManager.getRuleByCode(1L, "DEPOSIT_50");

    assertThat(result).isNotNull();
    assertThat(result.getPromotionCode()).isEqualTo("DEPOSIT_50");
    verify(promotionRuleDao).selectByCode(1L, "DEPOSIT_50");
  }

  @Test
  @DisplayName("evictCache → 無業務邏輯，不拋異常")
  void evictCache_noBusinessLogic() {
    promotionCacheManager.evictCache(1L);
    // No business logic — cache eviction handled by Spring @CacheEvict
  }
}
