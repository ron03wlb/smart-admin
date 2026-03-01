package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.redislock.LockService;
import net.lab1024.sa.igaming.common.config.IgamingProperties;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.adapter.GpSignatureVerifier;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackBalanceForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.manager.GameTransactionManager;
import net.lab1024.sa.igaming.game.service.GameCallbackService;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GameCallbackService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameCallbackService 單元測試")
class GameCallbackServiceTest {

  @Mock private GpSignatureVerifier gpSignatureVerifier;
  @Mock private GameRoundDao gameRoundDao;
  @Mock private GameTransactionManager gameTransactionManager;
  @Mock private LockService lockService;
  @Spy private IgamingProperties igamingProperties = new IgamingProperties();
  @Mock private WalletDao walletDao;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @InjectMocks private GameCallbackService gameCallbackService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setupLockService() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
    // Default: lock service delegates to supplier (transparent pass-through)
    // Use lenient() because not all tests reach the lock acquisition step
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

  @Nested
  @DisplayName("processDebit 下注回呼")
  class ProcessDebitTest {

    @Test
    @DisplayName("成功下注 — 委託 GameTransactionManager")
    void processDebit_success() {
      CallbackDebitForm form = buildDebitForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(null);

      CallbackResponseVO responseVO = new CallbackResponseVO();
      responseVO.setTransactionId("tx-001");
      responseVO.setBalance(new BigDecimal("90.00"));
      responseVO.setStatus(RoundStatusEnum.OPEN.getValue());
      when(gameTransactionManager.executeDebit(form, 1L)).thenReturn(ResponseDTO.ok(responseVO));

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processDebit(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getTransactionId()).isEqualTo("tx-001");
      verify(gameTransactionManager).executeDebit(form, 1L);
    }

    @Test
    @DisplayName("冪等重複 — Layer 1 直接返回")
    void processDebit_idempotent() {
      CallbackDebitForm form = buildDebitForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);

      GameRoundEntity existing = new GameRoundEntity();
      existing.setTransactionId("tx-001");
      existing.setStatus(RoundStatusEnum.OPEN.getValue());
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(existing);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processDebit(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getTransactionId()).isEqualTo("tx-001");
    }

    @Test
    @DisplayName("簽名錯誤 — 拒絕")
    void processDebit_invalidSignature() {
      CallbackDebitForm form = buildDebitForm();
      form.setSignature("bad-sig");
      when(gpSignatureVerifier.verify(anyString(), anyString(), eq("bad-sig"), any()))
          .thenReturn(false);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processDebit(form);
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("鎖定取得失敗 — 返回 LOCK_ACQUISITION_FAILED")
    @SuppressWarnings("unchecked")
    void processDebit_lockAcquisitionFailed() {
      CallbackDebitForm form = buildDebitForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(null);

      // Override default lock behavior to throw
      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenThrow(new IllegalStateException("Lock acquisition failed"));

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processDebit(form);
      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("processCredit 結算回呼")
  class ProcessCreditTest {

    @Test
    @DisplayName("成功結算 — 委託 GameTransactionManager")
    void processCredit_success() {
      CallbackCreditForm form = buildCreditForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);

      CallbackResponseVO responseVO = new CallbackResponseVO();
      responseVO.setTransactionId("tx-002");
      responseVO.setBalance(new BigDecimal("110.00"));
      responseVO.setStatus(RoundStatusEnum.SETTLED.getValue());
      when(gameTransactionManager.executeCredit(form, 1L)).thenReturn(ResponseDTO.ok(responseVO));

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processCredit(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());
    }

    @Test
    @DisplayName("鎖定取得失敗 — 返回錯誤")
    @SuppressWarnings("unchecked")
    void processCredit_lockAcquisitionFailed() {
      CallbackCreditForm form = buildCreditForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);

      when(lockService.executeWithLock(anyString(), anyLong(), anyLong(), any(Supplier.class)))
          .thenThrow(new IllegalStateException("Lock failed"));

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processCredit(form);
      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("processRollback 取消回呼")
  class ProcessRollbackTest {

    @Test
    @DisplayName("成功取消 — 委託 GameTransactionManager")
    void processRollback_success() {
      CallbackRollbackForm form = buildRollbackForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);

      CallbackResponseVO responseVO = new CallbackResponseVO();
      responseVO.setTransactionId("tx-001");
      responseVO.setBalance(new BigDecimal("100.00"));
      responseVO.setStatus(RoundStatusEnum.CANCELLED.getValue());
      when(gameTransactionManager.executeRollback(form, 1L)).thenReturn(ResponseDTO.ok(responseVO));

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processRollback(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.CANCELLED.getValue());
    }
  }

  @Nested
  @DisplayName("queryRound 查詢 Round 狀態")
  class QueryRoundTest {

    @Test
    @DisplayName("查詢成功 — 返回 Round 狀態")
    @SuppressWarnings("unchecked")
    void queryRound_success() {
      GameRoundEntity round = new GameRoundEntity();
      round.setTransactionId("tx-001");
      round.setStatus(RoundStatusEnum.SETTLED.getValue());
      when(gameRoundDao.selectOne(any())).thenReturn(round);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.queryRound("gp-round-001");
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getTransactionId()).isEqualTo("tx-001");
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());
    }

    @Test
    @DisplayName("Round 不存在 — 返回錯誤")
    @SuppressWarnings("unchecked")
    void queryRound_notFound() {
      when(gameRoundDao.selectOne(any())).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.queryRound("gp-round-999");
      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("processBalance 餘額查詢")
  class ProcessBalanceTest {

    @Test
    @DisplayName("成功 — 返回 CASH + BONUS 餘額")
    @SuppressWarnings("unchecked")
    void processBalance_success() {
      CallbackBalanceForm form = buildBalanceForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);

      WalletEntity cashWallet = new WalletEntity();
      cashWallet.setWalletId(1L);
      cashWallet.setPlayerId(1L);
      cashWallet.setBalance(new BigDecimal("100.00"));
      cashWallet.setLockedAmount(BigDecimal.ZERO);
      when(walletDao.selectOne(any())).thenReturn(cashWallet).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processBalance(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("簽名錯誤 — 拒絕")
    void processBalance_invalidSignature() {
      CallbackBalanceForm form = buildBalanceForm();
      form.setSignature("bad-sig");
      when(gpSignatureVerifier.verify(anyString(), anyString(), eq("bad-sig"), any()))
          .thenReturn(false);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processBalance(form);
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("錢包不存在 — 返回錯誤")
    @SuppressWarnings("unchecked")
    void processBalance_walletNotFound() {
      CallbackBalanceForm form = buildBalanceForm();
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);
      when(walletDao.selectOne(any())).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processBalance(form);
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("有 BONUS — 返回合併餘額")
    @SuppressWarnings("unchecked")
    void processBalance_withBonus() {
      CallbackBalanceForm form = buildBalanceForm();
      form.setGameCode("slot-001");
      when(gpSignatureVerifier.verify(anyString(), anyString(), any(), any())).thenReturn(true);

      WalletEntity cashWallet = new WalletEntity();
      cashWallet.setWalletId(1L);
      cashWallet.setPlayerId(1L);
      cashWallet.setBalance(new BigDecimal("100.00"));
      cashWallet.setLockedAmount(BigDecimal.ZERO);

      WalletEntity bonusWallet = new WalletEntity();
      bonusWallet.setWalletId(2L);
      bonusWallet.setPlayerId(1L);
      bonusWallet.setBalance(new BigDecimal("50.00"));
      bonusWallet.setLockedAmount(BigDecimal.ZERO);

      // First selectOne call = CASH wallet, second = BONUS wallet
      when(walletDao.selectOne(any())).thenReturn(cashWallet).thenReturn(bonusWallet);

      WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
      bonusExt.setWalletId(2L);
      bonusExt.setBalance(new BigDecimal("50.00"));
      bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
      bonusExt.setGameRestriction(null);
      when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

      ResponseDTO<CallbackResponseVO> result = gameCallbackService.processBalance(form);
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBalance()).isEqualByComparingTo("150.00");
      assertThat(result.getData().getBonusBalance()).isEqualByComparingTo("50.00");
    }
  }

  private CallbackBalanceForm buildBalanceForm() {
    CallbackBalanceForm form = new CallbackBalanceForm();
    form.setProviderCode("mock");
    form.setPlayerId(1L);
    return form;
  }

  private CallbackDebitForm buildDebitForm() {
    CallbackDebitForm form = new CallbackDebitForm();
    form.setProviderCode("mock");
    form.setPlayerId(1L);
    form.setTransactionId("tx-001");
    form.setRoundId("round-001");
    form.setGameCode("slot-001");
    form.setAmount(new BigDecimal("10.00"));
    return form;
  }

  private CallbackCreditForm buildCreditForm() {
    CallbackCreditForm form = new CallbackCreditForm();
    form.setProviderCode("mock");
    form.setPlayerId(1L);
    form.setTransactionId("tx-002");
    form.setRoundId("round-001");
    form.setPayoutAmount(new BigDecimal("20.00"));
    return form;
  }

  private CallbackRollbackForm buildRollbackForm() {
    CallbackRollbackForm form = new CallbackRollbackForm();
    form.setProviderCode("mock");
    form.setPlayerId(1L);
    form.setOriginalTransactionId("tx-001");
    return form;
  }
}
