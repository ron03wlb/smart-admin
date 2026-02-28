package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.igaming.common.constant.ReconciliationExceptionTypeEnum;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentReconciliationDao;
import net.lab1024.sa.igaming.wallet.payment.dao.ReconciliationExceptionDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentReconciliationEntity;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.ReconciliationExceptionEntity;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentReconciliationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PaymentReconciliationManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationManager 單元測試")
@SuppressWarnings("unchecked")
class PaymentReconciliationManagerTest {

  @Mock private PaymentOrderDao paymentOrderDao;
  @Mock private PaymentReconciliationDao reconciliationDao;
  @Mock private ReconciliationExceptionDao exceptionDao;
  @InjectMocks private PaymentReconciliationManager manager;

  @Nested
  @DisplayName("verifyTransaction 實時驗證")
  class VerifyTransactionTest {

    @Test
    @DisplayName("金額匹配 — 狀態更新為 VERIFIED")
    void verifyTransaction_matched_statusVerified() {
      PaymentOrderEntity order = buildOrder("ORD-001", "100.0000");
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

      manager.verifyTransaction("ORD-001", new BigDecimal("100.0000"), new BigDecimal("100.0000"));

      assertThat(order.getReconciliationStatus())
          .isEqualTo(ReconciliationStatusEnum.VERIFIED.getValue());
      verify(paymentOrderDao).updateById(order);
      verify(exceptionDao, never()).insert(any(ReconciliationExceptionEntity.class));
    }

    @Test
    @DisplayName("金額不符 — 狀態更新為 MISMATCH + 建立異常記錄")
    void verifyTransaction_mismatch_createException() {
      PaymentOrderEntity order = buildOrder("ORD-002", "100.0000");
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

      manager.verifyTransaction("ORD-002", new BigDecimal("100.0000"), new BigDecimal("95.0000"));

      assertThat(order.getReconciliationStatus())
          .isEqualTo(ReconciliationStatusEnum.MISMATCH.getValue());
      verify(paymentOrderDao).updateById(order);

      ArgumentCaptor<ReconciliationExceptionEntity> captor =
          ArgumentCaptor.forClass(ReconciliationExceptionEntity.class);
      verify(exceptionDao).insert(captor.capture());
      ReconciliationExceptionEntity exception = captor.getValue();
      assertThat(exception.getOrderNo()).isEqualTo("ORD-002");
      assertThat(exception.getExceptionType())
          .isEqualTo(ReconciliationExceptionTypeEnum.AMOUNT_MISMATCH.getValue());
      assertThat(exception.getDifferenceAmount()).isEqualByComparingTo("5.0000");
    }

    @Test
    @DisplayName("訂單不存在 — 不做任何操作")
    void verifyTransaction_orderNotFound_noOp() {
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      manager.verifyTransaction("ORD-999", BigDecimal.TEN, BigDecimal.TEN);

      verify(paymentOrderDao, never()).updateById(any(PaymentOrderEntity.class));
      verify(exceptionDao, never()).insert(any(ReconciliationExceptionEntity.class));
    }
  }

  @Nested
  @DisplayName("dailyBatchReconciliation 每日批次")
  class DailyBatchTest {

    @Test
    @DisplayName("有訂單 — 按 PSP 分組生成對帳記錄")
    void dailyBatch_generatesSummaryPerPsp() {
      PaymentOrderEntity order1 = buildOrder("ORD-010", "50.0000");
      order1.setPspCode("PSP_A");
      order1.setReconciliationStatus(ReconciliationStatusEnum.VERIFIED.getValue());

      PaymentOrderEntity order2 = buildOrder("ORD-011", "100.0000");
      order2.setPspCode("PSP_A");
      order2.setReconciliationStatus(ReconciliationStatusEnum.MISMATCH.getValue());

      when(paymentOrderDao.selectList(any(LambdaQueryWrapper.class)))
          .thenReturn(List.of(order1, order2));

      manager.dailyBatchReconciliation(LocalDate.of(2026, 2, 26));

      ArgumentCaptor<PaymentReconciliationEntity> captor =
          ArgumentCaptor.forClass(PaymentReconciliationEntity.class);
      verify(reconciliationDao).insert(captor.capture());
      PaymentReconciliationEntity recon = captor.getValue();
      assertThat(recon.getPspCode()).isEqualTo("PSP_A");
      assertThat(recon.getTotalPlatformTransactions()).isEqualTo(2);
      assertThat(recon.getMatchedCount()).isEqualTo(1);
      assertThat(recon.getMismatchCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("無訂單 — 不建立對帳記錄")
    void dailyBatch_noOrders_noReconciliation() {
      when(paymentOrderDao.selectList(any(LambdaQueryWrapper.class)))
          .thenReturn(Collections.emptyList());

      manager.dailyBatchReconciliation(LocalDate.of(2026, 2, 26));

      verify(reconciliationDao, never()).insert(any(PaymentReconciliationEntity.class));
    }
  }

  @Nested
  @DisplayName("markCompensated 標記補償完成")
  class MarkCompensatedTest {

    @Test
    @DisplayName("成功標記 COMPENSATED")
    void markCompensated_success() {
      PaymentOrderEntity order = buildOrder("ORD-020", "200.0000");
      when(paymentOrderDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

      manager.markCompensated("ORD-020");

      assertThat(order.getReconciliationStatus())
          .isEqualTo(ReconciliationStatusEnum.COMPENSATED.getValue());
      verify(paymentOrderDao).updateById(order);
    }
  }

  // ==================== helpers ====================

  private PaymentOrderEntity buildOrder(String orderNo, String amount) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setOrderNo(orderNo);
    order.setAmount(new BigDecimal(amount));
    order.setReconciliationStatus(ReconciliationStatusEnum.PENDING.getValue());
    return order;
  }
}
