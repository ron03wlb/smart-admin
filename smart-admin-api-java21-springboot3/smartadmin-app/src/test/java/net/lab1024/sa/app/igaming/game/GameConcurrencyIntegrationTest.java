package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.common.constant.WalletTypeEnum;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.manager.GameTransactionManager;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Concurrency and lifecycle integration tests for GameTransactionManager.
 *
 * <p>These tests verify the 6-step round lifecycle and concurrency behavior at the Manager layer.
 * Distributed lock (Layer 1) is tested in GameCallbackServiceTest; this focuses on Layer 2 (SELECT
 * FOR UPDATE via mock) and Layer 3 (@Version optimistic lock).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Tag("integration")
@ExtendWith(MockitoExtension.class)
@DisplayName("Game 並發 + 生命週期整合測試")
class GameConcurrencyIntegrationTest {

  @Mock private GameRoundDao gameRoundDao;
  @Mock private GameDao gameDao;
  @Mock private GameWeightConfigDao gameWeightConfigDao;
  @Mock private WalletDao walletDao;
  @Mock private WalletManager walletManager;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  private GameTransactionManager gameTransactionManager;

  @BeforeEach
  void setUp() {
    gameTransactionManager =
        new GameTransactionManager(
            gameRoundDao,
            gameDao,
            gameWeightConfigDao,
            walletDao,
            walletBonusExtDao,
            walletTransactionDao,
            walletManager,
            Optional.of(domainEventPublisher));
  }

  @Test
  @DisplayName("完整 6-step 生命週期: Bet → Win → Resettlement → Adjusted")
  @SuppressWarnings("unchecked")
  void fullRoundLifecycle_betWinResettlement() {
    // Step 1: BET (OPEN)
    CallbackDebitForm debitForm = new CallbackDebitForm();
    debitForm.setProviderCode("mock");
    debitForm.setPlayerId(1L);
    debitForm.setTransactionId("tx-001");
    debitForm.setRoundId("round-001");
    debitForm.setGameCode("slot-001");
    debitForm.setAmount(new BigDecimal("10.00"));

    WalletEntity wallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

    WalletTransactionEntity debitTx = new WalletTransactionEntity();
    debitTx.setBalanceAfter(new BigDecimal("90.00"));
    when(walletManager.debit(any(), any())).thenReturn(debitTx);

    ResponseDTO<CallbackResponseVO> debitResult =
        gameTransactionManager.executeDebit(debitForm, 1L);
    assertThat(debitResult.getOk()).isTrue();
    assertThat(debitResult.getData().getStatus()).isEqualTo(RoundStatusEnum.OPEN.getValue());

    // Step 2: WIN (SETTLED)
    CallbackCreditForm creditForm = new CallbackCreditForm();
    creditForm.setProviderCode("mock");
    creditForm.setPlayerId(1L);
    creditForm.setTransactionId("tx-002");
    creditForm.setRoundId("round-001");
    creditForm.setPayoutAmount(new BigDecimal("25.00"));

    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(1L);
    round.setPlayerId(1L);
    round.setTenantId(1L);
    round.setGpRoundId("round-001");
    round.setTransactionId("tx-001");
    round.setBetAmount(new BigDecimal("10.00"));
    round.setPayoutAmount(BigDecimal.ZERO);
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    when(gameRoundDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(round);

    WalletEntity walletForCredit = buildWallet(new BigDecimal("90.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(walletForCredit);

    WalletTransactionEntity creditTx = new WalletTransactionEntity();
    creditTx.setBalanceAfter(new BigDecimal("115.00"));
    when(walletManager.credit(any(), any())).thenReturn(creditTx);

    ResponseDTO<CallbackResponseVO> creditResult =
        gameTransactionManager.executeCredit(creditForm, 1L);
    assertThat(creditResult.getOk()).isTrue();
    assertThat(creditResult.getData().getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());

    // Step 3: RESETTLEMENT (ADJUSTED)
    round.setStatus(RoundStatusEnum.SETTLED.getValue());
    round.setPayoutAmount(new BigDecimal("25.00"));
    when(gameRoundDao.selectById(1L)).thenReturn(round);

    WalletEntity walletForResettle = buildWallet(new BigDecimal("115.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue()))
        .thenReturn(walletForResettle);

    WalletTransactionEntity resettleTx = new WalletTransactionEntity();
    resettleTx.setBalanceAfter(new BigDecimal("120.00"));
    when(walletManager.credit(any(), any())).thenReturn(resettleTx);

    ResponseDTO<CallbackResponseVO> resettleResult =
        gameTransactionManager.executeResettlement(1L, new BigDecimal("30.00"), "resettle-001", 1L);
    assertThat(resettleResult.getOk()).isTrue();
    assertThat(resettleResult.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
  }

  @Test
  @DisplayName("Optimistic lock 衝突 — 返回錯誤")
  void optimisticLockConflict_returnsError() {
    CallbackDebitForm form = new CallbackDebitForm();
    form.setProviderCode("mock");
    form.setPlayerId(1L);
    form.setTransactionId("tx-olc");
    form.setRoundId("round-olc");
    form.setGameCode("slot-001");
    form.setAmount(new BigDecimal("10.00"));

    WalletEntity wallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

    when(walletManager.debit(any(), any()))
        .thenThrow(new OptimisticLockingFailureException("Version conflict"));

    ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);
    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("Bet → Timeout → PendingReview 生命週期")
  void betTimeoutPendingReview_lifecycle() {
    // Step 1: Create a round (simulate debit)
    WalletEntity wallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

    WalletTransactionEntity debitTx = new WalletTransactionEntity();
    debitTx.setBalanceAfter(new BigDecimal("90.00"));
    when(walletManager.debit(any(), any())).thenReturn(debitTx);

    CallbackDebitForm debitForm = new CallbackDebitForm();
    debitForm.setProviderCode("mock");
    debitForm.setPlayerId(1L);
    debitForm.setTransactionId("tx-timeout");
    debitForm.setRoundId("round-timeout");
    debitForm.setGameCode("slot-001");
    debitForm.setAmount(new BigDecimal("10.00"));

    ResponseDTO<CallbackResponseVO> debitResult =
        gameTransactionManager.executeDebit(debitForm, 1L);
    assertThat(debitResult.getOk()).isTrue();

    // Step 2: Timeout
    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(200L);
    round.setPlayerId(1L);
    round.setTenantId(1L);
    round.setGpRoundId("round-timeout");
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    when(gameRoundDao.selectById(200L)).thenReturn(round);

    gameTransactionManager.executeTimeout(200L);
    assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.TIMEOUT.getValue());

    // Step 3: Pending Review
    round.setStatus(RoundStatusEnum.TIMEOUT.getValue());
    when(gameRoundDao.selectById(200L)).thenReturn(round);

    gameTransactionManager.markPendingReview(200L, "GP health check failed");
    assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.PENDING_REVIEW.getValue());
  }

  @Test
  @DisplayName("並發扣款 — 5 線程同時執行 debit")
  void concurrentDebits_multipleThreads() throws InterruptedException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    WalletEntity wallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

    WalletTransactionEntity txEntity = new WalletTransactionEntity();
    txEntity.setBalanceAfter(new BigDecimal("80.00"));
    when(walletManager.debit(any(), any())).thenReturn(txEntity);

    for (int i = 0; i < threadCount; i++) {
      final int idx = i;
      executor.submit(
          () -> {
            try {
              startLatch.await();
              CallbackDebitForm form = new CallbackDebitForm();
              form.setProviderCode("mock");
              form.setPlayerId(1L);
              form.setTransactionId("tx-concurrent-" + idx);
              form.setRoundId("round-concurrent-" + idx);
              form.setGameCode("slot-001");
              form.setAmount(new BigDecimal("20.00"));

              ResponseDTO<CallbackResponseVO> result =
                  gameTransactionManager.executeDebit(form, 1L);
              if (result.getOk()) {
                successCount.incrementAndGet();
              } else {
                failCount.incrementAndGet();
              }
            } catch (Exception e) {
              failCount.incrementAndGet();
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown(); // Release all threads simultaneously
    doneLatch.await();
    executor.shutdown();

    // All should succeed since mock doesn't enforce real locking
    assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    assertThat(successCount.get()).isGreaterThan(0);
  }

  @Test
  @DisplayName("多錢包並發扣款 — 5 線程同時 BONUS+CASH debit")
  @SuppressWarnings("unchecked")
  void concurrentMultiWalletDebits_fiveThreads() throws InterruptedException {
    int threadCount = 5;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failCount = new AtomicInteger(0);

    WalletEntity cashWallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

    WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("50.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

    WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
    bonusExt.setWalletId(2L);
    bonusExt.setBalance(new BigDecimal("50.00"));
    bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
    bonusExt.setGameRestriction(null);
    when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

    WalletTransactionEntity bonusTx = new WalletTransactionEntity();
    bonusTx.setBalanceAfter(new BigDecimal("40.00"));
    when(walletManager.debitBonus(any(), any(), any())).thenReturn(bonusTx);

    WalletTransactionEntity cashTx = new WalletTransactionEntity();
    cashTx.setBalanceAfter(new BigDecimal("90.00"));
    when(walletManager.debit(any(), any())).thenReturn(cashTx);

    for (int i = 0; i < threadCount; i++) {
      final int idx = i;
      executor.submit(
          () -> {
            try {
              startLatch.await();
              CallbackDebitForm form = new CallbackDebitForm();
              form.setProviderCode("mock");
              form.setPlayerId(1L);
              form.setTransactionId("tx-mw-" + idx);
              form.setRoundId("round-mw-" + idx);
              form.setGameCode("slot-001");
              form.setAmount(new BigDecimal("20.00"));

              ResponseDTO<CallbackResponseVO> result =
                  gameTransactionManager.executeDebit(form, 1L);
              if (result.getOk()) {
                successCount.incrementAndGet();
              } else {
                failCount.incrementAndGet();
              }
            } catch (Exception e) {
              failCount.incrementAndGet();
            } finally {
              doneLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
    assertThat(successCount.get()).isGreaterThan(0);
    verify(walletManager, atLeast(1)).debitBonus(any(), any(), any());
    verify(walletManager, atLeast(1)).debit(any(), any());
  }

  @Test
  @DisplayName("並發 debit + rollback — 雙錢包一致性")
  @SuppressWarnings("unchecked")
  void concurrentDebitAndRollback_dualWallet() throws InterruptedException {
    // Setup: debit with BONUS+CASH
    WalletEntity cashWallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

    WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("50.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

    WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
    bonusExt.setWalletId(2L);
    bonusExt.setBalance(new BigDecimal("50.00"));
    bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
    bonusExt.setGameRestriction(null);
    when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

    WalletTransactionEntity bonusTx = new WalletTransactionEntity();
    bonusTx.setBalanceAfter(new BigDecimal("30.00"));
    when(walletManager.debitBonus(any(), any(), any())).thenReturn(bonusTx);

    WalletTransactionEntity cashTx = new WalletTransactionEntity();
    cashTx.setBalanceAfter(new BigDecimal("90.00"));
    when(walletManager.debit(any(), any())).thenReturn(cashTx);

    // Rollback setup
    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(1L);
    round.setPlayerId(1L);
    round.setTenantId(1L);
    round.setGpRoundId("round-dr-001");
    round.setTransactionId("tx-dr-debit");
    round.setBetAmount(new BigDecimal("30.00"));
    round.setPayoutAmount(BigDecimal.ZERO);
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    when(gameRoundDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(round);

    // Dual-wallet rollback: BONUS tx + CASH tx
    WalletTransactionEntity bonusRollbackTx = new WalletTransactionEntity();
    bonusRollbackTx.setWalletId(2L);
    bonusRollbackTx.setAmount(new BigDecimal("20.00"));
    bonusRollbackTx.setRequestId("tx-dr-debit:bonus");
    WalletTransactionEntity cashRollbackTx = new WalletTransactionEntity();
    cashRollbackTx.setWalletId(1L);
    cashRollbackTx.setAmount(new BigDecimal("10.00"));
    cashRollbackTx.setRequestId("tx-dr-debit:cash");
    when(walletTransactionDao.selectList(any()))
        .thenReturn(List.of(bonusRollbackTx, cashRollbackTx));

    WalletTransactionEntity creditTx = new WalletTransactionEntity();
    creditTx.setBalanceAfter(new BigDecimal("100.00"));
    when(walletManager.credit(any(), any())).thenReturn(creditTx);

    // Execute debit and rollback concurrently
    int threadCount = 2;
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(threadCount);
    AtomicInteger completedCount = new AtomicInteger(0);

    executor.submit(
        () -> {
          try {
            startLatch.await();
            CallbackDebitForm form = new CallbackDebitForm();
            form.setProviderCode("mock");
            form.setPlayerId(1L);
            form.setTransactionId("tx-dr-debit");
            form.setRoundId("round-dr-001");
            form.setGameCode("slot-001");
            form.setAmount(new BigDecimal("30.00"));
            gameTransactionManager.executeDebit(form, 1L);
            completedCount.incrementAndGet();
          } catch (Exception e) {
            // Expected: concurrent access may fail
          } finally {
            doneLatch.countDown();
          }
        });

    executor.submit(
        () -> {
          try {
            startLatch.await();
            CallbackRollbackForm rollbackForm = new CallbackRollbackForm();
            rollbackForm.setProviderCode("mock");
            rollbackForm.setPlayerId(1L);
            rollbackForm.setOriginalTransactionId("tx-dr-debit");
            gameTransactionManager.executeRollback(rollbackForm, 1L);
            completedCount.incrementAndGet();
          } catch (Exception e) {
            // Expected: concurrent access may fail
          } finally {
            doneLatch.countDown();
          }
        });

    startLatch.countDown();
    doneLatch.await();
    executor.shutdown();

    // At least one operation should complete
    assertThat(completedCount.get()).isGreaterThanOrEqualTo(1);
  }

  @Test
  @DisplayName("多錢包完整生命週期: Bet(BONUS+CASH) → Win → Resettlement")
  @SuppressWarnings("unchecked")
  void fullLifecycleMultiWallet_betWinResettlement() {
    // Step 1: BET with BONUS+CASH
    CallbackDebitForm debitForm = new CallbackDebitForm();
    debitForm.setProviderCode("mock");
    debitForm.setPlayerId(1L);
    debitForm.setTransactionId("tx-mwl-001");
    debitForm.setRoundId("round-mwl-001");
    debitForm.setGameCode("slot-001");
    debitForm.setAmount(new BigDecimal("30.00"));

    WalletEntity cashWallet = buildWallet(new BigDecimal("100.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

    WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("20.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

    WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
    bonusExt.setWalletId(2L);
    bonusExt.setBalance(new BigDecimal("20.00"));
    bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
    bonusExt.setGameRestriction(null);
    when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

    WalletTransactionEntity bonusTx = new WalletTransactionEntity();
    bonusTx.setBalanceAfter(BigDecimal.ZERO);
    when(walletManager.debitBonus(any(), any(), any())).thenReturn(bonusTx);

    WalletTransactionEntity cashDebitTx = new WalletTransactionEntity();
    cashDebitTx.setBalanceAfter(new BigDecimal("90.00"));
    when(walletManager.debit(any(), any())).thenReturn(cashDebitTx);

    ResponseDTO<CallbackResponseVO> debitResult =
        gameTransactionManager.executeDebit(debitForm, 1L);
    assertThat(debitResult.getOk()).isTrue();
    assertThat(debitResult.getData().getStatus()).isEqualTo(RoundStatusEnum.OPEN.getValue());

    // Step 2: WIN (SETTLED) — credit goes to CASH only
    CallbackCreditForm creditForm = new CallbackCreditForm();
    creditForm.setProviderCode("mock");
    creditForm.setPlayerId(1L);
    creditForm.setTransactionId("tx-mwl-002");
    creditForm.setRoundId("round-mwl-001");
    creditForm.setPayoutAmount(new BigDecimal("50.00"));

    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(1L);
    round.setPlayerId(1L);
    round.setTenantId(1L);
    round.setGpRoundId("round-mwl-001");
    round.setTransactionId("tx-mwl-001");
    round.setBetAmount(new BigDecimal("30.00"));
    round.setPayoutAmount(BigDecimal.ZERO);
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    when(gameRoundDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(round);

    WalletEntity walletForCredit = buildWallet(new BigDecimal("90.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(walletForCredit);

    WalletTransactionEntity creditTx = new WalletTransactionEntity();
    creditTx.setBalanceAfter(new BigDecimal("140.00"));
    when(walletManager.credit(any(), any())).thenReturn(creditTx);

    ResponseDTO<CallbackResponseVO> creditResult =
        gameTransactionManager.executeCredit(creditForm, 1L);
    assertThat(creditResult.getOk()).isTrue();
    assertThat(creditResult.getData().getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());

    // Step 3: RESETTLEMENT (ADJUSTED)
    round.setStatus(RoundStatusEnum.SETTLED.getValue());
    round.setPayoutAmount(new BigDecimal("50.00"));
    when(gameRoundDao.selectById(1L)).thenReturn(round);

    WalletEntity walletForResettle = buildWallet(new BigDecimal("140.00"));
    when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue()))
        .thenReturn(walletForResettle);

    WalletTransactionEntity resettleTx = new WalletTransactionEntity();
    resettleTx.setBalanceAfter(new BigDecimal("150.00"));
    when(walletManager.credit(any(), any())).thenReturn(resettleTx);

    ResponseDTO<CallbackResponseVO> resettleResult =
        gameTransactionManager.executeResettlement(
            1L, new BigDecimal("60.00"), "resettle-mwl-001", 1L);
    assertThat(resettleResult.getOk()).isTrue();
    assertThat(resettleResult.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
  }

  private WalletEntity buildWallet(BigDecimal balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setBalance(balance);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }

  private WalletEntity buildBonusWallet(BigDecimal balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(2L);
    wallet.setPlayerId(1L);
    wallet.setBalance(balance);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }
}
