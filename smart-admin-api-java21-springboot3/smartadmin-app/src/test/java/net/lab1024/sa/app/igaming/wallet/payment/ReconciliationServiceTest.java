package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.PaymentOrderTypeEnum;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.dto.PspQueryResponse;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentManager;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentReconciliationManager;
import net.lab1024.sa.igaming.wallet.payment.psp.PaymentProviderAdapter;
import net.lab1024.sa.igaming.wallet.payment.psp.PspAdapterFactory;
import net.lab1024.sa.igaming.wallet.payment.service.PaymentService;
import net.lab1024.sa.igaming.wallet.payment.service.ReconciliationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReconciliationService unit tests — Layer 2 PSP polling reconciliation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationService 單元測試")
class ReconciliationServiceTest {

  @Mock private PaymentOrderDao paymentOrderDao;
  @Mock private PaymentManager paymentManager;
  @Mock private PaymentReconciliationManager reconciliationManager;
  @Mock private PspAdapterFactory pspAdapterFactory;
  @Mock private PaymentService paymentService;

  @InjectMocks private ReconciliationService reconciliationService;

  @Test
  @DisplayName("PSP 回報 SUCCESS — 存款訂單 → processDepositCallback + markCompensated")
  void reconcilePendingOrders_successDeposit() {
    PaymentOrderEntity order = buildOrder("ORD001", PaymentOrderTypeEnum.DEPOSIT, "PSP_TX_1");
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    mockAdapterWithStatus(order.getPspCode(), order.getPspTransactionId(), "SUCCESS");

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(1);
    verify(paymentService)
        .processDepositCallback(eq("ORD001"), eq("PSP_TX_1"), eq("reconciliation"));
    verify(reconciliationManager).markCompensated("ORD001");
  }

  @Test
  @DisplayName("PSP 回報 SUCCESS — 提款訂單 → processWithdrawalCallback(true) + markCompensated")
  void reconcilePendingOrders_successWithdrawal() {
    PaymentOrderEntity order = buildOrder("ORD002", PaymentOrderTypeEnum.WITHDRAWAL, "PSP_TX_2");
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    mockAdapterWithStatus(order.getPspCode(), order.getPspTransactionId(), "SUCCESS");

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(1);
    verify(paymentService)
        .processWithdrawalCallback(eq("ORD002"), eq("PSP_TX_2"), eq("reconciliation"), eq(true));
    verify(reconciliationManager).markCompensated("ORD002");
  }

  @Test
  @DisplayName("PSP 回報 FAILED — 存款訂單 → failOrder + markCompensated")
  void reconcilePendingOrders_failedDeposit() {
    PaymentOrderEntity order = buildOrder("ORD003", PaymentOrderTypeEnum.DEPOSIT, "PSP_TX_3");
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    mockAdapterWithStatus(order.getPspCode(), order.getPspTransactionId(), "FAILED");

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(1);
    verify(paymentManager).failOrder(eq(order), eq(PaymentOrderStatusEnum.FAILED));
    verify(reconciliationManager).markCompensated("ORD003");
  }

  @Test
  @DisplayName("PSP 回報 REJECTED — 提款訂單 → processWithdrawalCallback(false) + markCompensated")
  void reconcilePendingOrders_rejectedWithdrawal() {
    PaymentOrderEntity order = buildOrder("ORD004", PaymentOrderTypeEnum.WITHDRAWAL, "PSP_TX_4");
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    mockAdapterWithStatus(order.getPspCode(), order.getPspTransactionId(), "REJECTED");

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(1);
    verify(paymentService)
        .processWithdrawalCallback(eq("ORD004"), eq("PSP_TX_4"), eq("reconciliation"), eq(false));
    verify(reconciliationManager).markCompensated("ORD004");
  }

  @Test
  @DisplayName("PSP 回報 PENDING — 不處理")
  void reconcilePendingOrders_stillPending() {
    PaymentOrderEntity order = buildOrder("ORD005", PaymentOrderTypeEnum.DEPOSIT, "PSP_TX_5");
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    mockAdapterWithStatus(order.getPspCode(), order.getPspTransactionId(), "PENDING");

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(0);
    verifyNoInteractions(paymentManager);
    verify(reconciliationManager, never()).markCompensated(any());
  }

  @Test
  @DisplayName("PSP 適配器不存在 → 跳過，返回 0")
  void reconcilePendingOrders_noAdapter() {
    PaymentOrderEntity order = buildOrder("ORD006", PaymentOrderTypeEnum.DEPOSIT, "PSP_TX_6");
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    when(pspAdapterFactory.getAdapter(order.getPspCode())).thenReturn(Option.none());

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(0);
    verifyNoInteractions(paymentManager, paymentService);
  }

  @Test
  @DisplayName("訂單無 PSP 交易 ID → 跳過，返回 0")
  void reconcilePendingOrders_noPspTxId() {
    PaymentOrderEntity order = buildOrder("ORD007", PaymentOrderTypeEnum.DEPOSIT, null);
    when(paymentOrderDao.selectList(any())).thenReturn(List.of(order));
    when(pspAdapterFactory.getAdapter(order.getPspCode()))
        .thenReturn(Option.of(mock(PaymentProviderAdapter.class)));

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(0);
    verifyNoInteractions(paymentManager, paymentService);
  }

  @Test
  @DisplayName("無過期訂單 → 返回 0")
  void reconcilePendingOrders_emptyList() {
    when(paymentOrderDao.selectList(any())).thenReturn(Collections.emptyList());

    int result = reconciliationService.reconcilePendingOrders(30);

    assertThat(result).isEqualTo(0);
    verifyNoInteractions(paymentManager, paymentService, reconciliationManager);
  }

  // --- Helpers ---

  private PaymentOrderEntity buildOrder(String orderNo, PaymentOrderTypeEnum type, String pspTxId) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setOrderNo(orderNo);
    order.setOrderType(type.getValue());
    order.setStatus(PaymentOrderStatusEnum.PENDING.getValue());
    order.setPspCode("TEST_PSP");
    order.setPspTransactionId(pspTxId);
    order.setAmount(new BigDecimal("100.00"));
    return order;
  }

  private void mockAdapterWithStatus(String pspCode, String pspTxId, String status) {
    PaymentProviderAdapter adapter = mock(PaymentProviderAdapter.class);
    when(pspAdapterFactory.getAdapter(pspCode)).thenReturn(Option.of(adapter));
    PspQueryResponse response =
        PspQueryResponse.builder().pspTransactionId(pspTxId).status(status).build();
    when(adapter.queryStatus(pspTxId)).thenReturn(Option.of(response));
  }
}
