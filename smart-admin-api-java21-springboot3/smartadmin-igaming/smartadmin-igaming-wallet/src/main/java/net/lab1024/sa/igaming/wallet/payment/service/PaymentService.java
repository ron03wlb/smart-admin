package net.lab1024.sa.igaming.wallet.payment.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.common.code.PaymentErrorCode;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.dao.PspDao;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositRequest;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PspEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.form.DepositRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.form.PaymentOrderQueryForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.DepositResponseVO;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentManager;
import net.lab1024.sa.igaming.wallet.payment.psp.PaymentProviderAdapter;
import net.lab1024.sa.igaming.wallet.payment.psp.PspAdapterFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Payment Service — business logic layer for payment operations.
 *
 * <p>Handles deposit/withdrawal creation, PSP interaction, and callback processing. Delegates
 * transactional operations to {@link PaymentManager}.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentOrderDao paymentOrderDao;
  private final PspDao pspDao;
  private final WalletDao walletDao;
  private final PaymentManager paymentManager;
  private final PspAdapterFactory pspAdapterFactory;

  /**
   * Get payment order by ID.
   *
   * @param paymentOrderId payment order ID
   * @return Option<PaymentOrderVO> — Some if exists, None otherwise
   */
  public Option<PaymentOrderVO> getPaymentOrder(Long paymentOrderId) {
    return Option.of(paymentOrderDao.selectById(paymentOrderId))
        .map(entity -> SmartBeanUtil.copy(entity, PaymentOrderVO.class));
  }

  /**
   * Query payment orders with pagination.
   *
   * @param queryForm query parameters
   * @return paginated payment order list
   */
  public ResponseDTO<PageResult<PaymentOrderVO>> queryPaymentOrders(
      PaymentOrderQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<PaymentOrderVO> list = paymentOrderDao.queryPage(page, queryForm);
    PageResult<PaymentOrderVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * Create a deposit order and initiate PSP deposit.
   *
   * <p>Flow: idempotency check → load wallet → load PSP → create order → call PSP → update order.
   *
   * @param form deposit request form
   * @return deposit response with redirect URL
   */
  public ResponseDTO<DepositResponseVO> createDeposit(DepositRequestForm form) {
    // Idempotency Layer 1: check requestId
    PaymentOrderEntity existing =
        paymentOrderDao.selectOne(
            Wrappers.<PaymentOrderEntity>lambdaQuery()
                .eq(PaymentOrderEntity::getRequestId, form.getRequestId()));
    if (existing != null) {
      DepositResponseVO vo = new DepositResponseVO();
      vo.setOrderNo(existing.getOrderNo());
      vo.setRedirectUrl(existing.getRedirectUrl());
      vo.setStatus(existing.getStatus());
      return ResponseDTO.ok(vo);
    }

    // Load wallet
    WalletEntity wallet = walletDao.selectById(form.getWalletId());
    if (wallet == null || wallet.getDeleted()) {
      return ResponseDTO.userErrorParam(
          net.lab1024.sa.igaming.common.code.WalletErrorCode.WALLET_NOT_FOUND.getMsg());
    }

    // Resolve PSP
    String pspCode = form.getPspCode() != null ? form.getPspCode() : resolveDefaultPsp();
    PspEntity pspConfig = findEnabledPsp(pspCode);
    if (pspConfig == null) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.PSP_NOT_FOUND.getMsg());
    }

    // Validate amount range
    if (pspConfig.getMinDeposit() != null
        && form.getAmount().compareTo(pspConfig.getMinDeposit()) < 0) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.DEPOSIT_AMOUNT_OUT_OF_RANGE.getMsg());
    }
    if (pspConfig.getMaxDeposit() != null
        && form.getAmount().compareTo(pspConfig.getMaxDeposit()) > 0) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.DEPOSIT_AMOUNT_OUT_OF_RANGE.getMsg());
    }

    // Create order (PENDING)
    String orderNo = generateOrderNo();
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setOrderNo(orderNo);
    order.setPlayerId(wallet.getPlayerId());
    order.setWalletId(wallet.getWalletId());
    order.setOrderType(PaymentOrderTypeEnum.DEPOSIT.getValue());
    order.setAmount(form.getAmount());
    order.setCurrencyCode(
        form.getCurrencyCode() != null ? form.getCurrencyCode() : wallet.getCurrencyCode());
    order.setStatus(PaymentOrderStatusEnum.PENDING.getValue());
    order.setPspCode(pspCode);
    order.setRequestId(form.getRequestId());
    order.setDescription(form.getDescription());

    try {
      paymentManager.createOrder(order);
    } catch (DuplicateKeyException e) {
      // Idempotency Layer 2: concurrent insert caught by UNIQUE constraint
      return ResponseDTO.userErrorParam(PaymentErrorCode.DUPLICATE_REQUEST.getMsg());
    }

    // Call PSP adapter
    Option<PaymentProviderAdapter> adapterOpt = pspAdapterFactory.getAdapter(pspCode);
    if (adapterOpt.isEmpty()) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.PSP_UNAVAILABLE.getMsg());
    }

    PspDepositRequest pspRequest =
        PspDepositRequest.builder()
            .orderId(orderNo)
            .amount(form.getAmount())
            .currency(order.getCurrencyCode())
            .paymentMethod(form.getPaymentMethod())
            .build();

    Either<String, PspDepositResponse> pspResult = adapterOpt.get().deposit(pspRequest);

    if (pspResult.isLeft()) {
      // PSP rejected — mark order FAILED
      paymentManager.failOrder(order, PaymentOrderStatusEnum.FAILED);
      return ResponseDTO.userErrorParam(PaymentErrorCode.PSP_UNAVAILABLE.getMsg());
    }

    // Update order with PSP response
    PspDepositResponse pspResponse = pspResult.get();
    order.setPspTransactionId(pspResponse.getPspTransactionId());
    order.setRedirectUrl(pspResponse.getRedirectUrl());
    order.setStatus(PaymentOrderStatusEnum.PROCESSING.getValue());
    paymentManager.updateOrderPspResponse(order);

    // Build response
    DepositResponseVO vo = new DepositResponseVO();
    vo.setOrderNo(orderNo);
    vo.setRedirectUrl(pspResponse.getRedirectUrl());
    vo.setStatus(order.getStatus());
    return ResponseDTO.ok(vo);
  }

  /**
   * Process PSP webhook callback for deposit.
   *
   * <p>Idempotency: CAS update (status=PENDING → SUCCESS). If CAS fails, the callback is a
   * duplicate.
   *
   * @param orderNo platform order number
   * @param pspTransactionId PSP transaction ID
   * @param callbackPayload raw callback payload for audit
   * @return success or error response
   */
  public ResponseDTO<String> processDepositCallback(
      String orderNo, String pspTransactionId, String callbackPayload) {
    // Load order
    PaymentOrderEntity order =
        paymentOrderDao.selectOne(
            Wrappers.<PaymentOrderEntity>lambdaQuery().eq(PaymentOrderEntity::getOrderNo, orderNo));
    if (order == null) {
      return ResponseDTO.userErrorParam(PaymentErrorCode.PAYMENT_ORDER_NOT_FOUND.getMsg());
    }

    // Idempotency: only process PENDING or PROCESSING orders
    if (!order.getStatus().equals(PaymentOrderStatusEnum.PENDING.getValue())
        && !order.getStatus().equals(PaymentOrderStatusEnum.PROCESSING.getValue())) {
      return ResponseDTO.ok("Already processed");
    }

    // Store callback payload for audit
    order.setCallbackPayload(callbackPayload);
    order.setPspTransactionId(pspTransactionId);

    // Load wallet for credit
    WalletEntity wallet = walletDao.selectById(order.getWalletId());
    if (wallet == null || wallet.getDeleted()) {
      return ResponseDTO.userErrorParam(
          net.lab1024.sa.igaming.common.code.WalletErrorCode.WALLET_NOT_FOUND.getMsg());
    }

    // Credit wallet + update order status in single transaction
    String creditRequestId = "deposit_credit_" + order.getOrderNo();
    paymentManager.completeDeposit(order, wallet, creditRequestId);

    return ResponseDTO.ok("Deposit completed");
  }

  private PspEntity findEnabledPsp(String pspCode) {
    return pspDao.selectOne(
        Wrappers.<PspEntity>lambdaQuery()
            .eq(PspEntity::getPspCode, pspCode)
            .eq(PspEntity::getEnabled, true));
  }

  private String resolveDefaultPsp() {
    // Phase 1.5: simple default to "mock". Phase 2 will implement smart routing.
    return "mock";
  }

  private String generateOrderNo() {
    return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
  }
}
