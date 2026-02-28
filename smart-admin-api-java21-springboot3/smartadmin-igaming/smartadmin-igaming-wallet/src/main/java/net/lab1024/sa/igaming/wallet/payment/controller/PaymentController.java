package net.lab1024.sa.igaming.wallet.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.annotation.NoNeedLogin;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PaymentErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.wallet.payment.domain.form.DepositRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.form.PaymentOrderQueryForm;
import net.lab1024.sa.igaming.wallet.payment.domain.form.WithdrawRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.DepositResponseVO;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import net.lab1024.sa.igaming.wallet.payment.psp.PspAdapterFactory;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment Controller — REST API endpoints for payment operations.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.PAYMENT)
@RequiredArgsConstructor
public class PaymentController {

  private final PaymentService paymentService;
  private final PspAdapterFactory pspAdapterFactory;

  @Operation(summary = "Create deposit order")
  @PostMapping("/igaming/payment/deposit")
  @SaCheckPermission("payment:deposit:operate")
  public ResponseDTO<DepositResponseVO> createDeposit(@RequestBody @Valid DepositRequestForm form) {
    return paymentService.createDeposit(form);
  }

  @Operation(summary = "Create withdrawal order")
  @PostMapping("/igaming/payment/withdraw")
  @SaCheckPermission("payment:withdraw:operate")
  public ResponseDTO<PaymentOrderVO> createWithdrawal(
      @RequestBody @Valid WithdrawRequestForm form) {
    return paymentService.createWithdrawal(form);
  }

  @Operation(summary = "Get payment order by ID")
  @GetMapping("/igaming/payment/get/{paymentOrderId}")
  @SaCheckPermission("payment:order:query")
  public ResponseDTO<PaymentOrderVO> getPaymentOrder(@PathVariable Long paymentOrderId) {
    return paymentService
        .getPaymentOrder(paymentOrderId)
        .map(ResponseDTO::ok)
        .getOrElse(
            () -> ResponseDTO.userErrorParam(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Query payment orders with pagination")
  @PostMapping("/igaming/payment/query")
  @SaCheckPermission("payment:order:query")
  public ResponseDTO<PageResult<PaymentOrderVO>> queryPaymentOrders(
      @RequestBody @Valid PaymentOrderQueryForm queryForm) {
    return paymentService.queryPaymentOrders(queryForm);
  }

  @NoNeedLogin
  @Operation(summary = "PSP webhook callback handler")
  @PostMapping("/igaming/payment/callback/{pspCode}")
  public ResponseDTO<String> handleCallback(
      @PathVariable String pspCode,
      @RequestHeader(value = "X-PSP-Signature", required = false) String signature,
      @RequestHeader(value = "X-PSP-Timestamp", required = false) Long timestamp,
      @RequestBody String payload) {
    // Verify PSP adapter exists
    if (pspAdapterFactory.getAdapter(pspCode).isEmpty()) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.PSP_NOT_FOUND.getMsg());
    }

    // Verify signature via adapter
    boolean signatureValid =
        pspAdapterFactory
            .getAdapter(pspCode)
            .get()
            .verifyCallback(
                payload, signature != null ? signature : "", timestamp != null ? timestamp : 0L);
    if (!signatureValid) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.CALLBACK_SIGNATURE_INVALID.getMsg());
    }

    // Delegate to service for processing
    return paymentService.processCallback(pspCode, payload);
  }
}
