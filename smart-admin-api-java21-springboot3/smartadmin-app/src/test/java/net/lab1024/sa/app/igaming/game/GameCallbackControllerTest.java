package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.game.controller.GameCallbackController;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.service.GameCallbackService;
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
 * GameCallbackController unit tests — external GP callback delegation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameCallbackController 單元測試")
class GameCallbackControllerTest {

  @Mock private GameCallbackService gameCallbackService;

  @InjectMocks private GameCallbackController gameCallbackController;

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
  @DisplayName("debit → 委派至 gameCallbackService.processDebit")
  void debit_delegatesToService() {
    CallbackDebitForm form = new CallbackDebitForm();
    ResponseDTO<CallbackResponseVO> expected = ResponseDTO.ok(new CallbackResponseVO());
    when(gameCallbackService.processDebit(form)).thenReturn(expected);

    ResponseDTO<CallbackResponseVO> result = gameCallbackController.debit(form);

    assertThat(result).isEqualTo(expected);
    verify(gameCallbackService).processDebit(form);
  }

  @Test
  @DisplayName("credit → 委派至 gameCallbackService.processCredit")
  void credit_delegatesToService() {
    CallbackCreditForm form = new CallbackCreditForm();
    ResponseDTO<CallbackResponseVO> expected = ResponseDTO.ok(new CallbackResponseVO());
    when(gameCallbackService.processCredit(form)).thenReturn(expected);

    ResponseDTO<CallbackResponseVO> result = gameCallbackController.credit(form);

    assertThat(result).isEqualTo(expected);
    verify(gameCallbackService).processCredit(form);
  }
}
