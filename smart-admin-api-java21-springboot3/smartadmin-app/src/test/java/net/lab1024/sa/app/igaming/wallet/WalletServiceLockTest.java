package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.function.Supplier;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.constant.LockReasonEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletBonusCreditForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreditForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletDebitForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletLockForm;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WalletService distributed lock integration tests.
 *
 * <p>Verifies that WalletService correctly calls LockService with the expected lock keys and
 * timeout parameters for all wallet operations.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService 分佈式鎖測試")
class WalletServiceLockTest {

  private static final long EXPECTED_ACQUIRE_TIMEOUT = 3000L;
  private static final long EXPECTED_EXPIRE = 10000L;

  @Mock private WalletDao walletDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private WalletLockDao walletLockDao;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private WalletManager walletManager;
  @Mock private LockService lockService;

  @InjectMocks private WalletService walletService;

  @Nested
  @DisplayName("credit 加款 — 鎖行為")
  class CreditLockTest {

    @Test
    @DisplayName("使用正確的 lockKey: wallet:lock:{walletId}")
    @SuppressWarnings("unchecked")
    void shouldUseCreditLockKey() {
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenReturn(null);

      WalletCreditForm form = new WalletCreditForm();
      form.setWalletId(42L);
      form.setAmount(new BigDecimal("100.0000"));
      form.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
      form.setRequestId("req-001");

      walletService.credit(form);

      verify(lockService)
          .executeWithLock(
              eq("wallet:lock:42"),
              eq(EXPECTED_ACQUIRE_TIMEOUT),
              eq(EXPECTED_EXPIRE),
              any(Supplier.class));
    }
  }

  @Nested
  @DisplayName("debit 扣款 — 鎖行為")
  class DebitLockTest {

    @Test
    @DisplayName("使用正確的 lockKey: wallet:lock:{walletId}")
    @SuppressWarnings("unchecked")
    void shouldUseDebitLockKey() {
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenReturn(null);

      WalletDebitForm form = new WalletDebitForm();
      form.setWalletId(55L);
      form.setAmount(new BigDecimal("50.0000"));
      form.setTransactionType(TransactionTypeEnum.WITHDRAW.getValue());
      form.setRequestId("req-002");

      walletService.debit(form);

      verify(lockService)
          .executeWithLock(
              eq("wallet:lock:55"),
              eq(EXPECTED_ACQUIRE_TIMEOUT),
              eq(EXPECTED_EXPIRE),
              any(Supplier.class));
    }
  }

  @Nested
  @DisplayName("lockFunds 鎖定資金 — 鎖行為")
  class LockFundsLockTest {

    @Test
    @DisplayName("使用正確的 lockKey: wallet:lock:{walletId}")
    @SuppressWarnings("unchecked")
    void shouldUseLockFundsLockKey() {
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenReturn(null);

      WalletLockForm form = new WalletLockForm();
      form.setWalletId(77L);
      form.setLockAmount(new BigDecimal("200.0000"));
      form.setLockReason(LockReasonEnum.BET_PENDING.getValue());
      form.setReferenceId("bet-001");

      walletService.lockFunds(form);

      verify(lockService)
          .executeWithLock(
              eq("wallet:lock:77"),
              eq(EXPECTED_ACQUIRE_TIMEOUT),
              eq(EXPECTED_EXPIRE),
              any(Supplier.class));
    }
  }

  @Nested
  @DisplayName("unlockFunds 解鎖資金 — 鎖行為")
  class UnlockFundsLockTest {

    @Test
    @DisplayName("使用正確的 lockKey: wallet:lock:{walletId} (from lock entity)")
    @SuppressWarnings("unchecked")
    void shouldUseUnlockFundsLockKey() {
      WalletLockEntity lockEntity = new WalletLockEntity();
      lockEntity.setLockId(10L);
      lockEntity.setWalletId(88L);
      lockEntity.setLockAmount(new BigDecimal("300.0000"));
      when(walletLockDao.selectById(10L)).thenReturn(lockEntity);

      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenReturn(null);

      walletService.unlockFunds(10L);

      verify(lockService)
          .executeWithLock(
              eq("wallet:lock:88"),
              eq(EXPECTED_ACQUIRE_TIMEOUT),
              eq(EXPECTED_EXPIRE),
              any(Supplier.class));
    }

    @Test
    @DisplayName("鎖定記錄不存在時不嘗試加鎖")
    void shouldNotLockWhenLockRecordNotFound() {
      when(walletLockDao.selectById(999L)).thenReturn(null);

      walletService.unlockFunds(999L);

      verify(lockService, never())
          .executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class));
    }
  }

  @Nested
  @DisplayName("creditBonus 紅利加款 — 鎖行為")
  class CreditBonusLockTest {

    @Test
    @DisplayName("使用正確的 lockKey: wallet:bonus:lock:{playerId}")
    @SuppressWarnings("unchecked")
    void shouldUseBonusLockKey() {
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenReturn(null);

      WalletBonusCreditForm form = new WalletBonusCreditForm();
      form.setPlayerId(123L);
      form.setAmount(new BigDecimal("50.0000"));
      form.setBonusId(1L);
      form.setWageringRequirement(new BigDecimal("500.0000"));
      form.setRequestId("bonus-req-001");

      walletService.creditBonus(form);

      verify(lockService)
          .executeWithLock(
              eq("wallet:bonus:lock:123"),
              eq(EXPECTED_ACQUIRE_TIMEOUT),
              eq(EXPECTED_EXPIRE),
              any(Supplier.class));
    }
  }

  @Nested
  @DisplayName("鎖獲取失敗 — IllegalStateException 傳播")
  class LockAcquisitionFailureTest {

    @Test
    @DisplayName("credit: LockService 拋出 IllegalStateException")
    @SuppressWarnings("unchecked")
    void creditShouldPropagateIllegalStateException() {
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenThrow(new IllegalStateException("业务繁忙,请稍后重试~"));

      WalletCreditForm form = new WalletCreditForm();
      form.setWalletId(1L);
      form.setAmount(new BigDecimal("100.0000"));
      form.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
      form.setRequestId("req-lock-fail");

      assertThatThrownBy(() -> walletService.credit(form))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("debit: LockService 拋出 IllegalStateException")
    @SuppressWarnings("unchecked")
    void debitShouldPropagateIllegalStateException() {
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenThrow(new IllegalStateException("业务繁忙,请稍后重试~"));

      WalletDebitForm form = new WalletDebitForm();
      form.setWalletId(1L);
      form.setAmount(new BigDecimal("100.0000"));
      form.setTransactionType(TransactionTypeEnum.WITHDRAW.getValue());
      form.setRequestId("req-lock-fail-debit");

      assertThatThrownBy(() -> walletService.debit(form)).isInstanceOf(IllegalStateException.class);
    }
  }
}
