package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.dao.PspDao;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspDepositResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PspEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.form.DepositRequestForm;
import net.lab1024.sa.igaming.wallet.payment.domain.vo.DepositResponseVO;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentManager;
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
 * PaymentService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 單元測試")
class PaymentServiceTest {

  @Mock private PaymentOrderDao paymentOrderDao;
  @Mock private PspDao pspDao;
  @Mock private WalletDao walletDao;
  @Mock private PaymentManager paymentManager;
  @Mock private PspAdapterFactory pspAdapterFactory;

  @InjectMocks private PaymentService paymentService;

  // ==================== createDeposit ====================

  @Nested
  @DisplayName("createDeposit 存款建立")
  class CreateDepositTest {

    @Test
    @DisplayName("成功建立存款訂單")
    void createDeposit_success() {
      DepositRequestForm form = buildDepositForm();

      // No existing order (not idempotent duplicate)
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      // Wallet exists
      WalletEntity wallet = buildWallet();
      when(walletDao.selectById(1L)).thenReturn(wallet);

      // PSP config exists
      PspEntity psp = buildPspEntity();
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(psp);

      // PSP adapter returns success
      PaymentProviderAdapter mockAdapter = mock(PaymentProviderAdapter.class);
      when(pspAdapterFactory.getAdapter("mock")).thenReturn(Option.some(mockAdapter));
      when(mockAdapter.deposit(any()))
          .thenReturn(
              Either.right(
                  PspDepositResponse.builder()
                      .pspTransactionId("mock_dep_123")
                      .redirectUrl("https://mock-psp.local/pay/123")
                      .status("PENDING")
                      .build()));

      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getRedirectUrl()).contains("mock-psp.local");
      verify(paymentManager).createOrder(any(PaymentOrderEntity.class));
      verify(paymentManager).updateOrderPspResponse(any(PaymentOrderEntity.class));
    }

    @Test
    @DisplayName("冪等重複 requestId 返回已有訂單")
    void createDeposit_idempotent_returns_existing() {
      DepositRequestForm form = buildDepositForm();

      PaymentOrderEntity existing = new PaymentOrderEntity();
      existing.setOrderNo("PAY_EXISTING");
      existing.setRedirectUrl("https://mock-psp.local/pay/existing");
      existing.setStatus(PaymentOrderStatusEnum.PROCESSING.getValue());

      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getOrderNo()).isEqualTo("PAY_EXISTING");
      verify(paymentManager, never()).createOrder(any());
    }

    @Test
    @DisplayName("錢包不存在返回錯誤")
    void createDeposit_walletNotFound() {
      DepositRequestForm form = buildDepositForm();

      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(walletDao.selectById(1L)).thenReturn(null);

      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isFalse();
      verify(paymentManager, never()).createOrder(any());
    }

    @Test
    @DisplayName("PSP 不存在返回錯誤")
    void createDeposit_pspNotFound() {
      DepositRequestForm form = buildDepositForm();

      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(walletDao.selectById(1L)).thenReturn(buildWallet());
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isFalse();
      verify(paymentManager, never()).createOrder(any());
    }

    @Test
    @DisplayName("PSP 拒絕返回錯誤")
    void createDeposit_pspDeclined() {
      DepositRequestForm form = buildDepositForm();

      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(walletDao.selectById(1L)).thenReturn(buildWallet());
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildPspEntity());

      PaymentProviderAdapter mockAdapter = mock(PaymentProviderAdapter.class);
      when(pspAdapterFactory.getAdapter("mock")).thenReturn(Option.some(mockAdapter));
      when(mockAdapter.deposit(any())).thenReturn(Either.left("DECLINED"));

      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isFalse();
      verify(paymentManager).failOrder(any(), eq(PaymentOrderStatusEnum.FAILED));
    }

    @Test
    @DisplayName("金額超出 PSP 範圍返回錯誤")
    void createDeposit_amountOutOfRange() {
      DepositRequestForm form = buildDepositForm();
      form.setAmount(new BigDecimal("99999.0000")); // exceeds max 10000

      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(walletDao.selectById(1L)).thenReturn(buildWallet());
      when(pspDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildPspEntity());

      ResponseDTO<DepositResponseVO> result = paymentService.createDeposit(form);

      assertThat(result.getOk()).isFalse();
      verify(paymentManager, never()).createOrder(any());
    }
  }

  // ==================== processDepositCallback ====================

  @Nested
  @DisplayName("processDepositCallback 存款回調")
  class ProcessDepositCallbackTest {

    @Test
    @DisplayName("成功處理回調並入賬")
    void processDepositCallback_success() {
      PaymentOrderEntity order = buildPendingOrder();
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

      WalletEntity wallet = buildWallet();
      when(walletDao.selectById(1L)).thenReturn(wallet);

      ResponseDTO<String> result =
          paymentService.processDepositCallback("PAY_123", "psp_txn_456", "{}");

      assertThat(result.getOk()).isTrue();
      verify(paymentManager).completeDeposit(any(), any(), any());
    }

    @Test
    @DisplayName("訂單不存在返回錯誤")
    void processDepositCallback_orderNotFound() {
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      ResponseDTO<String> result =
          paymentService.processDepositCallback("PAY_999", "psp_txn_456", "{}");

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("重複回調 (已處理) 返回成功")
    void processDepositCallback_alreadyProcessed() {
      PaymentOrderEntity order = buildPendingOrder();
      order.setStatus(PaymentOrderStatusEnum.SUCCESS.getValue());
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

      ResponseDTO<String> result =
          paymentService.processDepositCallback("PAY_123", "psp_txn_456", "{}");

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).contains("Already processed");
      verify(paymentManager, never()).completeDeposit(any(), any(), any());
    }
  }

  // ==================== getPaymentOrder ====================

  @Nested
  @DisplayName("getPaymentOrder 查詢訂單")
  class GetPaymentOrderTest {

    @Test
    @DisplayName("訂單存在返回 VO")
    void getPaymentOrder_exists() {
      PaymentOrderEntity order = buildPendingOrder();
      when(paymentOrderDao.selectById(1L)).thenReturn(order);

      Option<net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO> result =
          paymentService.getPaymentOrder(1L);

      assertThat(result.isDefined()).isTrue();
    }

    @Test
    @DisplayName("訂單不存在返回 None")
    void getPaymentOrder_notFound() {
      when(paymentOrderDao.selectById(999L)).thenReturn(null);

      Option<net.lab1024.sa.igaming.wallet.payment.domain.vo.PaymentOrderVO> result =
          paymentService.getPaymentOrder(999L);

      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ==================== Helpers ====================

  private DepositRequestForm buildDepositForm() {
    DepositRequestForm form = new DepositRequestForm();
    form.setWalletId(1L);
    form.setAmount(new BigDecimal("100.0000"));
    form.setCurrencyCode("USD");
    form.setPspCode("mock");
    form.setRequestId("req_deposit_001");
    return form;
  }

  private WalletEntity buildWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(100L);
    wallet.setCurrencyCode("USD");
    wallet.setWalletType(1);
    wallet.setBalance(new BigDecimal("1000.0000"));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    wallet.setVersion(0);
    return wallet;
  }

  private PspEntity buildPspEntity() {
    PspEntity psp = new PspEntity();
    psp.setPspId(1L);
    psp.setPspCode("mock");
    psp.setPspName("Mock PSP");
    psp.setEnabled(true);
    psp.setPriority(999);
    psp.setMinDeposit(new BigDecimal("10.0000"));
    psp.setMaxDeposit(new BigDecimal("10000.0000"));
    return psp;
  }

  private PaymentOrderEntity buildPendingOrder() {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setPaymentOrderId(1L);
    order.setOrderNo("PAY_123");
    order.setPlayerId(100L);
    order.setWalletId(1L);
    order.setOrderType(1);
    order.setAmount(new BigDecimal("100.0000"));
    order.setCurrencyCode("USD");
    order.setStatus(PaymentOrderStatusEnum.PENDING.getValue());
    order.setPspCode("mock");
    order.setRequestId("req_deposit_001");
    order.setVersion(0);
    return order;
  }
}
