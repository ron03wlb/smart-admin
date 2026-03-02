package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import net.lab1024.sa.igaming.activity.service.PromotionCacheAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PromotionCacheAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionCacheAdminService 單元測試")
class PromotionCacheAdminServiceTest {

  @Mock private PromotionCacheManager promotionCacheManager;
  @InjectMocks private PromotionCacheAdminService service;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("getActiveRules — 成功返回快取活動規則列表")
  void getActiveRules_success() {
    PromotionRuleEntity entity = buildEntity();
    when(promotionCacheManager.getActiveRules(1L)).thenReturn(List.of(entity));

    ResponseDTO<List<PromotionRuleVO>> result = service.getActiveRules();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
    assertThat(result.getData().get(0).getPromotionCode()).isEqualTo("WELCOME100");
  }

  @Test
  @DisplayName("getActiveRules — Tenant 為空返回錯誤")
  void getActiveRules_tenantNull() {
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

    ResponseDTO<List<PromotionRuleVO>> result = service.getActiveRules();

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("getRuleByCode — 成功返回快取規則")
  void getRuleByCode_success() {
    PromotionRuleEntity entity = buildEntity();
    when(promotionCacheManager.getRuleByCode(1L, "WELCOME100")).thenReturn(entity);

    ResponseDTO<PromotionRuleVO> result = service.getRuleByCode("WELCOME100");

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getPromotionCode()).isEqualTo("WELCOME100");
    assertThat(result.getData().getPromotionName()).isEqualTo("Welcome Bonus");
  }

  @Test
  @DisplayName("getRuleByCode — 規則不存在返回錯誤")
  void getRuleByCode_notFound() {
    when(promotionCacheManager.getRuleByCode(1L, "INVALID")).thenReturn(null);

    ResponseDTO<PromotionRuleVO> result = service.getRuleByCode("INVALID");

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("evictCache — 成功清除快取")
  void evictCache_success() {
    ResponseDTO<String> result = service.evictCache();

    assertThat(result.getOk()).isTrue();
    verify(promotionCacheManager).evictCache(1L);
  }

  private PromotionRuleEntity buildEntity() {
    PromotionRuleEntity entity = new PromotionRuleEntity();
    entity.setRuleId(1L);
    entity.setPromotionCode("WELCOME100");
    entity.setPromotionName("Welcome Bonus");
    entity.setPromotionType(1);
    entity.setStatus(1);
    entity.setMaxBonus(new BigDecimal("100.00"));
    entity.setWageringMultiplier(new BigDecimal("20.00"));
    return entity;
  }
}
