package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.controller.GameRoundAdminController;
import net.lab1024.sa.igaming.game.domain.form.GameRoundPendingReviewForm;
import net.lab1024.sa.igaming.game.domain.form.ResettlementForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.service.GameRoundAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameRoundAdminController unit tests — delegation to service.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameRoundAdminController 單元測試")
class GameRoundAdminControllerTest {

  @Mock private GameRoundAdminService gameRoundAdminService;
  @InjectMocks private GameRoundAdminController gameRoundAdminController;

  @Test
  @DisplayName("resettle → 成功委託 service 並返回結果")
  void resettle_success_returnsOk() {
    ResettlementForm form = new ResettlementForm();
    form.setRoundId(1L);
    form.setNewPayoutAmount(new BigDecimal("30.00"));
    form.setRequestId("req-001");

    CallbackResponseVO responseVO = new CallbackResponseVO();
    responseVO.setTransactionId("tx-001");
    responseVO.setBalance(new BigDecimal("120.00"));
    responseVO.setStatus(RoundStatusEnum.ADJUSTED.getValue());
    when(gameRoundAdminService.resettle(form)).thenReturn(ResponseDTO.ok(responseVO));

    ResponseDTO<CallbackResponseVO> result = gameRoundAdminController.resettle(form);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
    verify(gameRoundAdminService).resettle(form);
  }

  @Test
  @DisplayName("resettle → service 返回錯誤時透傳")
  void resettle_serviceError_passThrough() {
    ResettlementForm form = new ResettlementForm();
    form.setRoundId(999L);
    form.setNewPayoutAmount(new BigDecimal("30.00"));
    form.setRequestId("req-002");

    when(gameRoundAdminService.resettle(form))
        .thenReturn(ResponseDTO.userErrorParam("Game round does not exist"));

    ResponseDTO<CallbackResponseVO> result = gameRoundAdminController.resettle(form);

    assertThat(result.getOk()).isFalse();
    verify(gameRoundAdminService).resettle(form);
  }

  @Test
  @DisplayName("markPendingReview → 成功委託 service")
  void markPendingReview_success() {
    GameRoundPendingReviewForm form = new GameRoundPendingReviewForm();
    form.setRoundId(1L);
    form.setReason("Suspicious activity");

    when(gameRoundAdminService.markPendingReview(form)).thenReturn(ResponseDTO.ok());

    ResponseDTO<String> result = gameRoundAdminController.markPendingReview(form);

    assertThat(result.getOk()).isTrue();
    verify(gameRoundAdminService).markPendingReview(form);
  }
}
