package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.function.Supplier;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.common.constant.TransactionTypeEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.domain.form.WalletAdminAdjustForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletFreezeForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import net.lab1024.sa.igaming.wallet.service.WalletAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WalletAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletAdminService 單元測試")
class WalletAdminServiceTest {

  @Mock private WalletDao walletDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private WalletManager walletManager;
  @Mock private LockService lockService;
  @Spy private IgamingProperties igamingProperties = new IgamingProperties();
  @InjectMocks private WalletAdminService walletAdminService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
    // Default: lock service delegates to supplier (transparent pass-through)
    lenient()
        .when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
        .thenAnswer(
            invocation -> {
              Supplier<?> supplier = invocation.getArgument(3);
              return supplier.get();
            });
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("凍結玩家錢包 — 成功")
  void freezePlayerWallets_success() {
    WalletFreezeForm form = new WalletFreezeForm();
    form.setPlayerId(100L);
    form.setReason("Self-exclusion request");

    when(walletManager.freezePlayerWallets(100L)).thenReturn(2);

    ResponseDTO<Integer> result = walletAdminService.freezePlayerWallets(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(2);
    verify(walletManager).freezePlayerWallets(100L);
  }

  @Test
  @DisplayName("Admin 加款 — 成功")
  void adminAdjust_credit_success() {
    WalletAdminAdjustForm form = buildAdjustForm(new BigDecimal("50.00"));

    WalletEntity wallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectById(1L)).thenReturn(wallet);
    when(walletTransactionDao.selectOne(any())).thenReturn(null);

    WalletTransactionEntity txResult = new WalletTransactionEntity();
    txResult.setTransactionId(1L);
    txResult.setAmount(new BigDecimal("50.00"));
    txResult.setBalanceBefore(new BigDecimal("100.00"));
    txResult.setBalanceAfter(new BigDecimal("150.00"));
    txResult.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
    when(walletManager.credit(any(), any())).thenReturn(txResult);

    ResponseDTO<WalletTransactionVO> result = walletAdminService.adminAdjust(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getBalanceAfter()).isEqualByComparingTo("150.00");
    verify(walletManager).credit(any(), any());
    verify(walletManager, never()).debit(any(), any());
  }

  @Test
  @DisplayName("Admin 扣款 — 成功")
  void adminAdjust_debit_success() {
    WalletAdminAdjustForm form = buildAdjustForm(new BigDecimal("-20.00"));

    WalletEntity wallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectById(1L)).thenReturn(wallet);
    when(walletTransactionDao.selectOne(any())).thenReturn(null);

    WalletTransactionEntity txResult = new WalletTransactionEntity();
    txResult.setTransactionId(2L);
    txResult.setAmount(new BigDecimal("-20.00"));
    txResult.setBalanceBefore(new BigDecimal("100.00"));
    txResult.setBalanceAfter(new BigDecimal("80.00"));
    txResult.setTransactionType(TransactionTypeEnum.ADJUSTMENT.getValue());
    when(walletManager.debit(any(), any())).thenReturn(txResult);

    ResponseDTO<WalletTransactionVO> result = walletAdminService.adminAdjust(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getBalanceAfter()).isEqualByComparingTo("80.00");
    verify(walletManager).debit(any(), any());
    verify(walletManager, never()).credit(any(), any());
  }

  @Test
  @DisplayName("Admin 調整 — 錢包不存在")
  void adminAdjust_walletNotFound() {
    WalletAdminAdjustForm form = buildAdjustForm(new BigDecimal("10.00"));

    when(walletDao.selectById(1L)).thenReturn(null);
    when(walletTransactionDao.selectOne(any())).thenReturn(null);

    ResponseDTO<WalletTransactionVO> result = walletAdminService.adminAdjust(form);
    assertThat(result.getOk()).isFalse();
    verifyNoInteractions(walletManager);
  }

  @Test
  @DisplayName("Admin 調整 — 冪等性返回已存在交易")
  void adminAdjust_idempotent() {
    WalletAdminAdjustForm form = buildAdjustForm(new BigDecimal("50.00"));

    WalletTransactionEntity existing = new WalletTransactionEntity();
    existing.setTransactionId(99L);
    existing.setAmount(new BigDecimal("50.00"));
    existing.setBalanceBefore(new BigDecimal("100.00"));
    existing.setBalanceAfter(new BigDecimal("150.00"));
    existing.setRequestId("req-001");
    when(walletTransactionDao.selectOne(any())).thenReturn(existing);

    ResponseDTO<WalletTransactionVO> result = walletAdminService.adminAdjust(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getTransactionId()).isEqualTo(99L);
    verifyNoInteractions(walletManager);
  }

  private WalletAdminAdjustForm buildAdjustForm(BigDecimal amount) {
    WalletAdminAdjustForm form = new WalletAdminAdjustForm();
    form.setWalletId(1L);
    form.setAmount(amount);
    form.setRequestId("req-001");
    form.setReason("Admin adjustment");
    return form;
  }

  private WalletEntity buildWallet(BigDecimal balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(100L);
    wallet.setBalance(balance);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }
}
