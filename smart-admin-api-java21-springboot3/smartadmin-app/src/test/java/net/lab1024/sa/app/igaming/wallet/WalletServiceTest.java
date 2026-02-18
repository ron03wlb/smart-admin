package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.LockReasonEnum;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletLockEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreateForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletCreditForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletDebitForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletLockForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletLockVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * WalletService unit tests.
 *
 * <p>Uses Mockito to mock Dao and Manager layers — pure unit tests without Spring context.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletService 單元測試")
class WalletServiceTest {

  @Mock private WalletDao walletDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private WalletLockDao walletLockDao;
  @Mock private WalletManager walletManager;

  @InjectMocks private WalletService walletService;

  // ==================== getWallet ====================

  @Nested
  @DisplayName("getWallet 查詢錢包測試")
  class GetWalletTest {

    @Test
    @DisplayName("存在且未刪除的錢包：返回 Some(WalletVO)")
    void shouldReturnSomeWhenWalletExists() {
      WalletEntity entity = buildWalletEntity(1L, new BigDecimal("1000.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(entity);

      Option<WalletVO> result = walletService.getWallet(1L);

      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getWalletId()).isEqualTo(1L);
      assertThat(result.get().getBalance()).isEqualByComparingTo("1000.0000");
      assertThat(result.get().getAvailableBalance()).isEqualByComparingTo("1000.0000");
    }

    @Test
    @DisplayName("不存在的錢包：返回 None")
    void shouldReturnNoneWhenWalletNotFound() {
      when(walletDao.selectById(999L)).thenReturn(null);

      Option<WalletVO> result = walletService.getWallet(999L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("已刪除的錢包：返回 None")
    void shouldReturnNoneWhenWalletDeleted() {
      WalletEntity entity = buildWalletEntity(1L, BigDecimal.ZERO, BigDecimal.ZERO);
      entity.setDeleted(true);
      when(walletDao.selectById(1L)).thenReturn(entity);

      Option<WalletVO> result = walletService.getWallet(1L);

      assertThat(result.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("可用餘額計算：balance - lockedAmount")
    void shouldCalculateAvailableBalance() {
      WalletEntity entity =
          buildWalletEntity(1L, new BigDecimal("1000.0000"), new BigDecimal("300.0000"));
      when(walletDao.selectById(1L)).thenReturn(entity);

      Option<WalletVO> result = walletService.getWallet(1L);

      assertThat(result.get().getAvailableBalance()).isEqualByComparingTo("700.0000");
    }
  }

  // ==================== createWallet ====================

  @Nested
  @DisplayName("createWallet 建立錢包測試")
  class CreateWalletTest {

    @Test
    @DisplayName("正常建立錢包：返回成功")
    void shouldCreateWalletSuccessfully() {
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      WalletCreateForm form = new WalletCreateForm();
      form.setPlayerId(100L);
      form.setWalletType(WalletTypeEnum.CASH.getValue());
      form.setCurrencyCode("USD");

      ResponseDTO<WalletVO> result = walletService.createWallet(form);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getData().getPlayerId()).isEqualTo(100L);
      assertThat(result.getData().getBalance()).isEqualByComparingTo("0");
      verify(walletManager).createWallet(any(WalletEntity.class));
    }

    @Test
    @DisplayName("重複建立錢包：返回錯誤")
    void shouldFailWhenWalletAlreadyExists() {
      WalletEntity existing = buildWalletEntity(1L, BigDecimal.ZERO, BigDecimal.ZERO);
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

      WalletCreateForm form = new WalletCreateForm();
      form.setPlayerId(100L);
      form.setWalletType(WalletTypeEnum.CASH.getValue());

      ResponseDTO<WalletVO> result = walletService.createWallet(form);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).createWallet(any());
    }

    @Test
    @DisplayName("未指定幣種時使用默認 USD")
    void shouldDefaultToUsdCurrency() {
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      WalletCreateForm form = new WalletCreateForm();
      form.setPlayerId(100L);
      form.setWalletType(WalletTypeEnum.CASH.getValue());
      // currencyCode not set

      ResponseDTO<WalletVO> result = walletService.createWallet(form);

      assertThat(result.getSuccess()).isTrue();
      verify(walletManager).createWallet(argThat(entity -> "USD".equals(entity.getCurrencyCode())));
    }

    @Test
    @DisplayName("TOCTOU 並發競爭：DuplicateKeyException 返回錯誤而非拋異常")
    void shouldHandleDuplicateKeyOnConcurrentCreate() {
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      doThrow(new DuplicateKeyException("Duplicate entry"))
          .when(walletManager)
          .createWallet(any(WalletEntity.class));

      WalletCreateForm form = new WalletCreateForm();
      form.setPlayerId(100L);
      form.setWalletType(WalletTypeEnum.CASH.getValue());
      form.setCurrencyCode("USD");

      ResponseDTO<WalletVO> result = walletService.createWallet(form);

      assertThat(result.getSuccess()).isFalse();
    }
  }

  // ==================== credit ====================

  @Nested
  @DisplayName("credit 加款測試")
  class CreditTest {

    @Test
    @DisplayName("正常加款：餘額增加，返回交易記錄")
    void shouldCreditSuccessfully() {
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      WalletEntity wallet = buildWalletEntity(1L, new BigDecimal("1000.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(wallet);
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenAnswer(invocation -> invocation.getArgument(1));

      WalletCreditForm form = buildCreditForm(1L, "500.0000", "req-001");

      ResponseDTO<WalletTransactionVO> result = walletService.credit(form);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getData().getBalanceBefore()).isEqualByComparingTo("1000.0000");
      assertThat(result.getData().getBalanceAfter()).isEqualByComparingTo("1500.0000");
      verify(walletManager).credit(any(WalletEntity.class), any(WalletTransactionEntity.class));
    }

    @Test
    @DisplayName("冪等檢查：相同 requestId 返回已存在的交易")
    void shouldReturnExistingTransactionForDuplicateRequestId() {
      WalletTransactionEntity existing = new WalletTransactionEntity();
      existing.setTransactionId(10L);
      existing.setRequestId("req-dup");
      existing.setAmount(new BigDecimal("500.0000"));
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

      WalletCreditForm form = buildCreditForm(1L, "500.0000", "req-dup");

      ResponseDTO<WalletTransactionVO> result = walletService.credit(form);

      assertThat(result.getSuccess()).isTrue();
      verify(walletManager, never()).credit(any(), any());
    }

    @Test
    @DisplayName("錢包不存在：返回錯誤")
    void shouldFailWhenWalletNotFound() {
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      when(walletDao.selectById(999L)).thenReturn(null);

      WalletCreditForm form = buildCreditForm(999L, "100.0000", "req-002");

      ResponseDTO<WalletTransactionVO> result = walletService.credit(form);

      assertThat(result.getSuccess()).isFalse();
    }

    @Test
    @DisplayName("無效交易類型：WITHDRAW 不允許用於 credit")
    void shouldRejectInvalidTransactionTypeForCredit() {
      WalletCreditForm form = new WalletCreditForm();
      form.setWalletId(1L);
      form.setAmount(new BigDecimal("100.0000"));
      form.setTransactionType(TransactionTypeEnum.WITHDRAW.getValue());
      form.setRequestId("req-invalid-type");

      ResponseDTO<WalletTransactionVO> result = walletService.credit(form);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).credit(any(), any());
    }

    @Test
    @DisplayName("樂觀鎖衝突：Manager 拋出 OptimisticLockingFailureException")
    void shouldPropagateOptimisticLockException() {
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      WalletEntity wallet = buildWalletEntity(1L, new BigDecimal("1000.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(wallet);
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenThrow(new OptimisticLockingFailureException("Wallet version conflict"));

      WalletCreditForm form = buildCreditForm(1L, "500.0000", "req-lock-fail");

      assertThatThrownBy(() -> walletService.credit(form))
          .isInstanceOf(OptimisticLockingFailureException.class);
    }
  }

  // ==================== debit ====================

  @Nested
  @DisplayName("debit 扣款測試")
  class DebitTest {

    @Test
    @DisplayName("正常扣款：餘額減少，交易金額為負")
    void shouldDebitSuccessfully() {
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      WalletEntity wallet = buildWalletEntity(1L, new BigDecimal("1000.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(wallet);
      when(walletManager.debit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenAnswer(invocation -> invocation.getArgument(1));

      WalletDebitForm form = buildDebitForm(1L, "300.0000", "req-003");

      ResponseDTO<WalletTransactionVO> result = walletService.debit(form);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getData().getBalanceBefore()).isEqualByComparingTo("1000.0000");
      assertThat(result.getData().getBalanceAfter()).isEqualByComparingTo("700.0000");
      assertThat(result.getData().getAmount()).isEqualByComparingTo("-300.0000");
      verify(walletManager).debit(any(WalletEntity.class), any(WalletTransactionEntity.class));
    }

    @Test
    @DisplayName("餘額不足：返回錯誤")
    void shouldFailWhenInsufficientBalance() {
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      WalletEntity wallet = buildWalletEntity(1L, new BigDecimal("100.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(wallet);

      WalletDebitForm form = buildDebitForm(1L, "500.0000", "req-004");

      ResponseDTO<WalletTransactionVO> result = walletService.debit(form);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).debit(any(), any());
    }

    @Test
    @DisplayName("鎖定金額影響可用餘額：locked funds reduce available balance")
    void shouldConsiderLockedAmountForAvailableBalance() {
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
      WalletEntity wallet =
          buildWalletEntity(1L, new BigDecimal("1000.0000"), new BigDecimal("800.0000"));
      when(walletDao.selectById(1L)).thenReturn(wallet);

      // Available = 1000 - 800 = 200, trying to debit 300
      WalletDebitForm form = buildDebitForm(1L, "300.0000", "req-005");

      ResponseDTO<WalletTransactionVO> result = walletService.debit(form);

      assertThat(result.getSuccess()).isFalse();
    }

    @Test
    @DisplayName("冪等檢查：相同 requestId 返回已存在的交易")
    void shouldReturnExistingTransactionForDuplicateRequestId() {
      WalletTransactionEntity existing = new WalletTransactionEntity();
      existing.setTransactionId(20L);
      existing.setRequestId("req-dup-debit");
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

      WalletDebitForm form = buildDebitForm(1L, "100.0000", "req-dup-debit");

      ResponseDTO<WalletTransactionVO> result = walletService.debit(form);

      assertThat(result.getSuccess()).isTrue();
      verify(walletManager, never()).debit(any(), any());
    }

    @Test
    @DisplayName("無效交易類型：DEPOSIT 不允許用於 debit")
    void shouldRejectInvalidTransactionTypeForDebit() {
      WalletDebitForm form = new WalletDebitForm();
      form.setWalletId(1L);
      form.setAmount(new BigDecimal("100.0000"));
      form.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
      form.setRequestId("req-invalid-debit-type");

      ResponseDTO<WalletTransactionVO> result = walletService.debit(form);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).debit(any(), any());
    }
  }

  // ==================== lockFunds ====================

  @Nested
  @DisplayName("lockFunds 鎖定資金測試")
  class LockFundsTest {

    @Test
    @DisplayName("正常鎖定：返回 lock VO")
    void shouldLockFundsSuccessfully() {
      WalletEntity wallet = buildWalletEntity(1L, new BigDecimal("1000.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(wallet);

      WalletLockForm form = new WalletLockForm();
      form.setWalletId(1L);
      form.setLockAmount(new BigDecimal("500.0000"));
      form.setLockReason(LockReasonEnum.BET_PENDING.getValue());
      form.setReferenceId("bet-001");

      ResponseDTO<WalletLockVO> result = walletService.lockFunds(form);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getData().getLockAmount()).isEqualByComparingTo("500.0000");
      verify(walletManager).lockFunds(any(WalletEntity.class), any(WalletLockEntity.class));
    }

    @Test
    @DisplayName("可用餘額不足：返回錯誤")
    void shouldFailWhenInsufficientAvailableBalance() {
      WalletEntity wallet = buildWalletEntity(1L, new BigDecimal("100.0000"), BigDecimal.ZERO);
      when(walletDao.selectById(1L)).thenReturn(wallet);

      WalletLockForm form = new WalletLockForm();
      form.setWalletId(1L);
      form.setLockAmount(new BigDecimal("500.0000"));
      form.setLockReason(LockReasonEnum.BET_PENDING.getValue());
      form.setReferenceId("bet-002");

      ResponseDTO<WalletLockVO> result = walletService.lockFunds(form);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).lockFunds(any(), any());
    }
  }

  // ==================== unlockFunds ====================

  @Nested
  @DisplayName("unlockFunds 解鎖資金測試")
  class UnlockFundsTest {

    @Test
    @DisplayName("正常解鎖：返回成功")
    void shouldUnlockFundsSuccessfully() {
      WalletLockEntity lockEntity = new WalletLockEntity();
      lockEntity.setLockId(10L);
      lockEntity.setWalletId(1L);
      lockEntity.setLockAmount(new BigDecimal("500.0000"));
      when(walletLockDao.selectById(10L)).thenReturn(lockEntity);

      WalletEntity wallet =
          buildWalletEntity(1L, new BigDecimal("1000.0000"), new BigDecimal("500.0000"));
      when(walletDao.selectById(1L)).thenReturn(wallet);

      ResponseDTO<String> result = walletService.unlockFunds(10L);

      assertThat(result.getSuccess()).isTrue();
      verify(walletManager).unlockFunds(any(WalletEntity.class), eq(10L));
    }

    @Test
    @DisplayName("鎖定記錄不存在：返回錯誤")
    void shouldFailWhenLockRecordNotFound() {
      when(walletLockDao.selectById(999L)).thenReturn(null);

      ResponseDTO<String> result = walletService.unlockFunds(999L);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).unlockFunds(any(), anyLong());
    }

    @Test
    @DisplayName("lockedAmount 不一致（會變成負數）：返回錯誤")
    void shouldFailWhenLockedAmountWouldGoNegative() {
      WalletLockEntity lockEntity = new WalletLockEntity();
      lockEntity.setLockId(10L);
      lockEntity.setWalletId(1L);
      lockEntity.setLockAmount(new BigDecimal("500.0000"));
      when(walletLockDao.selectById(10L)).thenReturn(lockEntity);

      // Wallet has only 100 locked but lock record says 500 — data inconsistency
      WalletEntity wallet =
          buildWalletEntity(1L, new BigDecimal("1000.0000"), new BigDecimal("100.0000"));
      when(walletDao.selectById(1L)).thenReturn(wallet);

      ResponseDTO<String> result = walletService.unlockFunds(10L);

      assertThat(result.getSuccess()).isFalse();
      verify(walletManager, never()).unlockFunds(any(), anyLong());
    }
  }

  // ==================== Helper Methods ====================

  private WalletEntity buildWalletEntity(Long walletId, BigDecimal balance, BigDecimal locked) {
    WalletEntity entity = new WalletEntity();
    entity.setWalletId(walletId);
    entity.setPlayerId(100L);
    entity.setCurrencyCode("USD");
    entity.setWalletType(WalletTypeEnum.CASH.getValue());
    entity.setBalance(balance);
    entity.setLockedAmount(locked);
    entity.setVersion(0);
    entity.setDeleted(false);
    return entity;
  }

  private WalletCreditForm buildCreditForm(Long walletId, String amount, String requestId) {
    WalletCreditForm form = new WalletCreditForm();
    form.setWalletId(walletId);
    form.setAmount(new BigDecimal(amount));
    form.setTransactionType(TransactionTypeEnum.DEPOSIT.getValue());
    form.setRequestId(requestId);
    return form;
  }

  private WalletDebitForm buildDebitForm(Long walletId, String amount, String requestId) {
    WalletDebitForm form = new WalletDebitForm();
    form.setWalletId(walletId);
    form.setAmount(new BigDecimal(amount));
    form.setTransactionType(TransactionTypeEnum.WITHDRAW.getValue());
    form.setRequestId(requestId);
    return form;
  }
}
