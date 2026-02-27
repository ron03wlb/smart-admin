package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.form.CallbackCreditForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackDebitForm;
import net.lab1024.sa.igaming.game.domain.form.CallbackRollbackForm;
import net.lab1024.sa.igaming.game.domain.vo.CallbackResponseVO;
import net.lab1024.sa.igaming.game.manager.GameTransactionManager;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

/**
 * GameTransactionManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameTransactionManager 單元測試")
@SuppressWarnings("UnusedVariable")
class GameTransactionManagerTest {

  @Mock private GameRoundDao gameRoundDao;
  @Mock private GameDao gameDao;
  @Mock private GameWeightConfigDao gameWeightConfigDao;
  @Mock private WalletDao walletDao;
  @Mock private WalletManager walletManager;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private GameTransactionManager gameTransactionManager;

  @Nested
  @DisplayName("executeDebit 下注扣款")
  class ExecuteDebitTest {

    @Test
    @DisplayName("成功 — 建立 round + 扣款")
    @SuppressWarnings("unchecked")
    void executeDebit_success() {
      CallbackDebitForm form = buildDebitForm();

      WalletEntity wallet = buildWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("90.00"));
      when(walletManager.debit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBalance()).isEqualByComparingTo("90.00");
      verify(gameRoundDao).insert(any(GameRoundEntity.class));
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.GAME_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("錢包不存在 — 失敗")
    @SuppressWarnings("unchecked")
    void executeDebit_walletNotFound() {
      CallbackDebitForm form = buildDebitForm();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("DuplicateKey — Layer 2 冪等性")
    @SuppressWarnings("unchecked")
    void executeDebit_duplicateKey() {
      CallbackDebitForm form = buildDebitForm();

      WalletEntity wallet = buildWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("90.00"));
      when(walletManager.debit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);
      doThrow(new DuplicateKeyException("Duplicate"))
          .when(gameRoundDao)
          .insert(any(GameRoundEntity.class));

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      // Should still succeed (idempotent)
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("餘額不足 — 失敗")
    @SuppressWarnings("unchecked")
    void executeDebit_insufficientBalance() {
      CallbackDebitForm form = buildDebitForm();
      form.setAmount(new BigDecimal("200.00")); // More than wallet balance

      WalletEntity wallet = buildWallet(); // balance=100, locked=0
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("executeCredit 結算")
  class ExecuteCreditTest {

    @Test
    @DisplayName("成功 — 更新 round + 入款")
    @SuppressWarnings("unchecked")
    void executeCredit_success() {
      CallbackCreditForm form = buildCreditForm();

      GameRoundEntity round = buildRound();
      when(gameRoundDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(round);

      WalletEntity wallet = buildWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("120.00"));
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeCredit(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());
      verify(gameRoundDao).updateById(round);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.GAME_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("Round 不存在 — 失敗")
    @SuppressWarnings("unchecked")
    void executeCredit_roundNotFound() {
      CallbackCreditForm form = buildCreditForm();
      when(gameRoundDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeCredit(form, 1L);

      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("executeRollback 取消退款")
  class ExecuteRollbackTest {

    @Test
    @DisplayName("成功 — 退款 + CANCELLED")
    @SuppressWarnings("unchecked")
    void executeRollback_success() {
      CallbackRollbackForm form = buildRollbackForm();

      GameRoundEntity round = buildRound();
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(round);

      WalletEntity wallet = buildWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(wallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("110.00"));
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeRollback(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.CANCELLED.getValue());
      verify(gameRoundDao).updateById(round);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.GAME_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("原始 Round 不存在 — 失敗")
    void executeRollback_roundNotFound() {
      CallbackRollbackForm form = buildRollbackForm();
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeRollback(form, 1L);

      assertThat(result.getOk()).isFalse();
    }
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

  private WalletEntity buildWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setBalance(new BigDecimal("100.00"));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }

  private GameRoundEntity buildRound() {
    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(1L);
    round.setPlayerId(1L);
    round.setTenantId(1L);
    round.setProviderCode("mock");
    round.setGpRoundId("round-001");
    round.setGameCode("slot-001");
    round.setTransactionId("tx-001");
    round.setBetAmount(new BigDecimal("10.00"));
    round.setPayoutAmount(BigDecimal.ZERO);
    round.setStatus(RoundStatusEnum.OPEN.getValue());
    round.setDeleted(false);
    return round;
  }
}
