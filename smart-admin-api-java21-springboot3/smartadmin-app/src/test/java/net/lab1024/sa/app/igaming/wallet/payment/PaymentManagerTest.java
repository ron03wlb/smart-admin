package net.lab1024.sa.app.igaming.wallet.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.common.constant.PaymentOrderStatusEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.payment.dao.PaymentOrderDao;
import net.lab1024.sa.igaming.wallet.payment.domain.entity.PaymentOrderEntity;
import net.lab1024.sa.igaming.wallet.payment.manager.PaymentManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * PaymentManager unit tests — transactional payment operations.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentManager 單元測試")
class PaymentManagerTest {

  @Mock private PaymentOrderDao paymentOrderDao;
  @Mock private WalletLockDao walletLockDao;
  @Mock private WalletManager walletManager;

  @InjectMocks private PaymentManager paymentManager;

  @Nested
  @DisplayName("createOrder")
  class CreateOrderTests {

    @Test
    @DisplayName("插入成功 → 驗證 paymentOrderDao.insert")
    void createOrder_success() {
      PaymentOrderEntity order = buildOrder("ORD001");

      paymentManager.createOrder(order);

      verify(paymentOrderDao).insert(order);
    }

    @Test
    @DisplayName("重複 requestId → DuplicateKeyException 拋出")
    void createOrder_duplicate() {
      PaymentOrderEntity order = buildOrder("ORD002");
      doThrow(new DuplicateKeyException("Duplicate key"))
          .when(paymentOrderDao)
          .insert(any(PaymentOrderEntity.class));

      assertThatThrownBy(() -> paymentManager.createOrder(order))
          .isInstanceOf(DuplicateKeyException.class);
    }
  }

  @Nested
  @DisplayName("updateOrderPspResponse")
  class UpdateOrderPspResponseTests {

    @Test
    @DisplayName("更新成功 → 驗證 updateById")
    void updateOrderPspResponse_success() {
      PaymentOrderEntity order = buildOrder("ORD003");
      when(paymentOrderDao.updateById(order)).thenReturn(1);

      paymentManager.updateOrderPspResponse(order);

      verify(paymentOrderDao).updateById(order);
    }

    @Test
    @DisplayName("版本衝突 rows=0 → OptimisticLockingFailureException")
    void updateOrderPspResponse_versionConflict() {
      PaymentOrderEntity order = buildOrder("ORD004");
      when(paymentOrderDao.updateById(order)).thenReturn(0);

      assertThatThrownBy(() -> paymentManager.updateOrderPspResponse(order))
          .isInstanceOf(OptimisticLockingFailureException.class);
    }
  }

  @Nested
  @DisplayName("completeDeposit")
  class CompleteDepositTests {

    @Test
    @DisplayName("存款完成 → 訂單 SUCCESS + 錢包 credit")
    void completeDeposit_success() {
      PaymentOrderEntity order = buildOrder("ORD005");
      order.setAmount(new BigDecimal("200.00"));
      order.setPspCode("TEST_PSP");
      when(paymentOrderDao.updateById(order)).thenReturn(1);

      WalletEntity wallet = buildWallet(new BigDecimal("500.00"));
      WalletTransactionEntity mockTx = new WalletTransactionEntity();
      when(walletManager.credit(eq(wallet), any(WalletTransactionEntity.class))).thenReturn(mockTx);

      WalletTransactionEntity result = paymentManager.completeDeposit(order, wallet, "dep_req_001");

      assertThat(result).isEqualTo(mockTx);
      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getValue());
      assertThat(wallet.getBalance()).isEqualByComparingTo("700.00");

      ArgumentCaptor<WalletTransactionEntity> txCaptor =
          ArgumentCaptor.forClass(WalletTransactionEntity.class);
      verify(walletManager).credit(eq(wallet), txCaptor.capture());

      WalletTransactionEntity capturedTx = txCaptor.getValue();
      assertThat(capturedTx.getTransactionType()).isEqualTo(TransactionTypeEnum.DEPOSIT.getValue());
      assertThat(capturedTx.getAmount()).isEqualByComparingTo("200.00");
      assertThat(capturedTx.getBalanceBefore()).isEqualByComparingTo("500.00");
      assertThat(capturedTx.getBalanceAfter()).isEqualByComparingTo("700.00");
      assertThat(capturedTx.getRequestId()).isEqualTo("dep_req_001");
    }
  }

  @Nested
  @DisplayName("completeWithdrawal")
  class CompleteWithdrawalTests {

    @Test
    @DisplayName("提款完成 → 訂單 SUCCESS + 錢包 debit + unlock")
    void completeWithdrawal_success() {
      PaymentOrderEntity order = buildOrder("ORD006");
      order.setAmount(new BigDecimal("100.00"));
      order.setPspCode("TEST_PSP");
      when(paymentOrderDao.updateById(order)).thenReturn(1);

      WalletEntity wallet = buildWallet(new BigDecimal("500.00"));
      wallet.setLockedAmount(new BigDecimal("100.00"));

      WalletTransactionEntity mockTx = new WalletTransactionEntity();
      when(walletManager.debit(eq(wallet), any(WalletTransactionEntity.class))).thenReturn(mockTx);

      WalletLockEntity lockEntity = new WalletLockEntity();
      lockEntity.setLockAmount(new BigDecimal("100.00"));
      when(walletLockDao.selectById(10L)).thenReturn(lockEntity);

      WalletTransactionEntity result =
          paymentManager.completeWithdrawal(order, wallet, "wd_req_001", 10L);

      assertThat(result).isEqualTo(mockTx);
      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.SUCCESS.getValue());

      ArgumentCaptor<WalletTransactionEntity> txCaptor =
          ArgumentCaptor.forClass(WalletTransactionEntity.class);
      verify(walletManager).debit(eq(wallet), txCaptor.capture());

      WalletTransactionEntity capturedTx = txCaptor.getValue();
      assertThat(capturedTx.getTransactionType())
          .isEqualTo(TransactionTypeEnum.WITHDRAW.getValue());
      assertThat(capturedTx.getAmount()).isEqualByComparingTo("-100.00");

      verify(walletManager).unlockFunds(wallet, 10L);
    }
  }

  @Nested
  @DisplayName("failOrder")
  class FailOrderTests {

    @Test
    @DisplayName("設定 FAILED 狀態 → 驗證 updateById")
    void failOrder_success() {
      PaymentOrderEntity order = buildOrder("ORD007");

      paymentManager.failOrder(order, PaymentOrderStatusEnum.FAILED);

      assertThat(order.getStatus()).isEqualTo(PaymentOrderStatusEnum.FAILED.getValue());
      verify(paymentOrderDao).updateById(order);
    }
  }

  // casUpdateStatus uses LambdaUpdateWrapper which requires MyBatis-Plus lambda cache
  // (not available in pure Mockito unit tests). Covered by integration tests instead.

  // --- Helpers ---

  private PaymentOrderEntity buildOrder(String orderNo) {
    PaymentOrderEntity order = new PaymentOrderEntity();
    order.setOrderNo(orderNo);
    order.setPlayerId(1L);
    order.setAmount(new BigDecimal("100.00"));
    order.setStatus(PaymentOrderStatusEnum.PENDING.getValue());
    order.setPspCode("TEST_PSP");
    return order;
  }

  private WalletEntity buildWallet(BigDecimal balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setBalance(balance);
    wallet.setLockedAmount(BigDecimal.ZERO);
    return wallet;
  }
}
