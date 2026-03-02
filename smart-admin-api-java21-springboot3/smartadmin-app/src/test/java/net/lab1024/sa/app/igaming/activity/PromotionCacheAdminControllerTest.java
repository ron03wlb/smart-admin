package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.controller.PromotionCacheAdminController;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.service.PromotionCacheAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PromotionCacheAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionCacheAdminController 單元測試")
class PromotionCacheAdminControllerTest {

  @Mock private PromotionCacheAdminService promotionCacheAdminService;
  @InjectMocks private PromotionCacheAdminController controller;

  @Test
  @DisplayName("getActiveRules — 委託 Service 成功")
  void getActiveRules_success() {
    PromotionRuleVO vo = new PromotionRuleVO();
    vo.setRuleId(1L);
    when(promotionCacheAdminService.getActiveRules()).thenReturn(ResponseDTO.ok(List.of(vo)));

    ResponseDTO<List<PromotionRuleVO>> result = controller.getActiveRules();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }

  @Test
  @DisplayName("getRuleByCode — 委託 Service 成功")
  void getRuleByCode_success() {
    PromotionRuleVO vo = new PromotionRuleVO();
    vo.setRuleId(1L);
    vo.setPromotionCode("WELCOME100");
    when(promotionCacheAdminService.getRuleByCode("WELCOME100")).thenReturn(ResponseDTO.ok(vo));

    ResponseDTO<PromotionRuleVO> result = controller.getRuleByCode("WELCOME100");

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getPromotionCode()).isEqualTo("WELCOME100");
  }

  @Test
  @DisplayName("evictCache — 委託 Service 成功")
  void evictCache_success() {
    when(promotionCacheAdminService.evictCache()).thenReturn(ResponseDTO.ok());

    ResponseDTO<String> result = controller.evictCache();

    assertThat(result.getOk()).isTrue();
  }
}
