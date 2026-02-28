package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.activity.controller.PromotionRuleController;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.service.PromotionRuleService;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PromotionRuleController unit tests — Option handling for rule lookup.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromotionRuleController 單元測試")
class PromotionRuleControllerTest {

  @Mock private PromotionRuleService promotionRuleService;

  @InjectMocks private PromotionRuleController promotionRuleController;

  @Test
  @DisplayName("getRule 存在 → ok(PromotionRuleVO)")
  void getRule_found() {
    PromotionRuleVO vo = new PromotionRuleVO();
    when(promotionRuleService.getRule(1L)).thenReturn(Option.some(vo));

    ResponseDTO<PromotionRuleVO> result = promotionRuleController.getRule(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(vo);
  }

  @Test
  @DisplayName("getRule 不存在 → PROMOTION_NOT_FOUND")
  void getRule_notFound() {
    when(promotionRuleService.getRule(999L)).thenReturn(Option.none());

    ResponseDTO<PromotionRuleVO> result = promotionRuleController.getRule(999L);

    assertThat(result.getOk()).isFalse();
    assertThat(result.getMsg()).isEqualTo(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
  }
}
