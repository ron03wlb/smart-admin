package net.lab1024.sa.igaming.wallet.payment.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PaymentErrorCode;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.wallet.payment.domain.form.DepositRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.form.PaymentOrderQueryForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.DepositResponseVO;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @Operation(summary = "Create deposit order")
  @PostMapping("/igaming/payment/deposit")
  public ResponseDTO<DepositResponseVO> createDeposit(@RequestBody @Valid DepositRequestForm form) {
    return paymentService.createDeposit(form);
  }

  @Operation(summary = "Get payment order by ID")
  @GetMapping("/igaming/payment/get/{paymentOrderId}")
  public ResponseDTO<PaymentOrderVO> getPaymentOrder(@PathVariable Long paymentOrderId) {
    return paymentService
        .getPaymentOrder(paymentOrderId)
        .map(ResponseDTO::ok)
        .getOrElse(
            () -> ResponseDTO.userErrorParam(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND.getMsg()));
  }

  @Operation(summary = "Query payment orders with pagination")
  @PostMapping("/igaming/payment/query")
  public ResponseDTO<PageResult<PaymentOrderVO>> queryPaymentOrders(
      @RequestBody @Valid PaymentOrderQueryForm queryForm) {
    return paymentService.queryPaymentOrders(queryForm);
  }
}
