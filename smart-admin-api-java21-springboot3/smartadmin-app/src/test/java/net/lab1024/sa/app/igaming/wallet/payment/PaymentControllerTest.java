package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PaymentErrorCode;
import net.lab1024.sa.igaming.wallet.payment.controller.PaymentController;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import net.lab1024.sa.igaming.wallet.payment.psp.PaymentProviderAdapter;
import net.lab1024.sa.igaming.wallet.payment.psp.PspAdapterFactory;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PaymentController unit tests — PSP callback validation and Option handling.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController 單元測試")
class PaymentControllerTest {

  @Mock private PaymentService paymentService;
  @Mock private PspAdapterFactory pspAdapterFactory;
  @Mock private PaymentProviderAdapter mockAdapter;

  @InjectMocks private PaymentController paymentController;

  @Nested
  @DisplayName("handleCallback")
  class HandleCallbackTests {

    @Test
    @DisplayName("PSP 存在 + 簽名通過 → 委派 processCallback")
    void handleCallback_success() {
      when(pspAdapterFactory.getAdapter("test_psp")).thenReturn(Option.some(mockAdapter));
      when(mockAdapter.verifyCallback("{}", "sig123", 1000L)).thenReturn(true);
      when(paymentService.processCallback("test_psp", "{}")).thenReturn(ResponseDTO.ok("OK"));

      ResponseDTO<String> result =
          paymentController.handleCallback("test_psp", "sig123", 1000L, "{}");

      assertThat(result.getOk()).isTrue();
      verify(paymentService).processCallback("test_psp", "{}");
    }

    @Test
    @DisplayName("PSP 不存在 → PSP_NOT_FOUND")
    void handleCallback_pspNotFound() {
      when(pspAdapterFactory.getAdapter("unknown")).thenReturn(Option.none());

      ResponseDTO<String> result = paymentController.handleCallback("unknown", "sig", 1000L, "{}");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PaymentErrorCode.PSP_NOT_FOUND.getMsg());
      verifyNoInteractions(paymentService);
    }

    @Test
    @DisplayName("簽名驗證失敗 → CALLBACK_SIGNATURE_INVALID")
    void handleCallback_invalidSignature() {
      when(pspAdapterFactory.getAdapter("test_psp")).thenReturn(Option.some(mockAdapter));
      when(mockAdapter.verifyCallback("{}", "bad_sig", 1000L)).thenReturn(false);

      ResponseDTO<String> result =
          paymentController.handleCallback("test_psp", "bad_sig", 1000L, "{}");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PaymentErrorCode.CALLBACK_SIGNATURE_INVALID.getMsg());
      verifyNoInteractions(paymentService);
    }
  }

  @Nested
  @DisplayName("getPaymentOrder")
  class GetPaymentOrderTests {

    @Test
    @DisplayName("訂單存在 → ok(PaymentOrderVO)")
    void getPaymentOrder_found() {
      PaymentOrderVO vo = new PaymentOrderVO();
      when(paymentService.getPaymentOrder(1L)).thenReturn(Option.some(vo));

      ResponseDTO<PaymentOrderVO> result = paymentController.getPaymentOrder(1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEqualTo(vo);
    }

    @Test
    @DisplayName("訂單不存在 → PAYMENT_ORDER_NOT_FOUND")
    void getPaymentOrder_notFound() {
      when(paymentService.getPaymentOrder(999L)).thenReturn(Option.none());

      ResponseDTO<PaymentOrderVO> result = paymentController.getPaymentOrder(999L);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND.getMsg());
    }
  }
}
