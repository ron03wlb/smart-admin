package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
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
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private WalletManager walletManager;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private GameTransactionManager gameTransactionManager;

  @Nested
  @DisplayName("executeDebit 下注扣款")
  class ExecuteDebitTest {

    @Test
    @DisplayName("成功 — 建立 round + 扣款")
    void executeDebit_success() {
      CallbackDebitForm form = buildDebitForm();

      WalletEntity wallet = buildWallet();
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

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
    void executeDebit_walletNotFound() {
      CallbackDebitForm form = buildDebitForm();
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(null);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("DuplicateKey — Layer 2 冪等性")
    void executeDebit_duplicateKey() {
      CallbackDebitForm form = buildDebitForm();

      WalletEntity wallet = buildWallet();
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

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
    void executeDebit_insufficientBalance() {
      CallbackDebitForm form = buildDebitForm();
      form.setAmount(new BigDecimal("200.00")); // More than wallet balance

      WalletEntity wallet = buildWallet(); // balance=100, locked=0
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("BONUS 優先 — 全額 BONUS 扣款，不動 CASH")
    @SuppressWarnings("unchecked")
    void executeDebit_bonusFirst() {
      CallbackDebitForm form = buildDebitForm(); // amount=10.00

      WalletEntity cashWallet = buildWallet(); // balance=100
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

      WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("50.00"));
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

      WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
      bonusExt.setWalletId(2L);
      bonusExt.setBalance(new BigDecimal("50.00"));
      bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
      bonusExt.setGameRestriction(null); // all games eligible
      when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBalance()).isEqualByComparingTo("100.00"); // CASH untouched
      assertThat(result.getData().getBonusBalance()).isEqualByComparingTo("40.00"); // 50-10
      verify(walletManager).debitBonus(any(), any(), any());
      verify(walletManager, never()).debit(any(), any());
    }

    @Test
    @DisplayName("BONUS 部分 + CASH 部分 — 雙錢包扣款")
    @SuppressWarnings("unchecked")
    void executeDebit_bonusPartialCash() {
      CallbackDebitForm form = buildDebitForm();
      form.setAmount(new BigDecimal("30.00")); // bonus=20, cash=10

      WalletEntity cashWallet = buildWallet(); // balance=100
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

      WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("20.00"));
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

      WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
      bonusExt.setWalletId(2L);
      bonusExt.setBalance(new BigDecimal("20.00"));
      bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
      bonusExt.setGameRestriction(null);
      when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("90.00"));
      when(walletManager.debit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBalance()).isEqualByComparingTo("90.00"); // 100-10
      assertThat(result.getData().getBonusBalance()).isEqualByComparingTo("0.00"); // 20-20
      verify(walletManager).debitBonus(any(), any(), any());
      verify(walletManager).debit(any(), any());
    }

    @Test
    @DisplayName("遊戲限制不符 — BONUS 不可用，全額 CASH")
    @SuppressWarnings("unchecked")
    void executeDebit_gameNotEligible() {
      CallbackDebitForm form = buildDebitForm(); // gameCode=slot-001, amount=10

      WalletEntity cashWallet = buildWallet(); // balance=100
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

      WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("50.00"));
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

      WalletBonusExtEntity bonusExt = new WalletBonusExtEntity();
      bonusExt.setWalletId(2L);
      bonusExt.setBalance(new BigDecimal("50.00"));
      bonusExt.setStatus(BonusStatusEnum.ACTIVE.getValue());
      bonusExt.setGameRestriction("{\"allowedGames\": [\"live-001\"]}"); // slot-001 NOT eligible
      when(walletBonusExtDao.selectList(any())).thenReturn(List.of(bonusExt));

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("90.00"));
      when(walletManager.debit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeDebit(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getBalance()).isEqualByComparingTo("90.00"); // 100-10
      assertThat(result.getData().getBonusBalance()).isEqualByComparingTo("0.00"); // no bonus used
      verify(walletManager, never()).debitBonus(any(), any(), any());
      verify(walletManager).debit(any(), any());
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
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

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
    void executeRollback_success() {
      CallbackRollbackForm form = buildRollbackForm();

      GameRoundEntity round = buildRound();
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(round);

      WalletEntity wallet = buildWallet();
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

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

    @Test
    @DisplayName("雙錢包退款 — BONUS + CASH 均退回")
    @SuppressWarnings("unchecked")
    void executeRollback_dualWallet() {
      CallbackRollbackForm form = buildRollbackForm();

      GameRoundEntity round = buildRound();
      round.setBetAmount(new BigDecimal("30.00"));
      when(gameRoundDao.selectByTransactionId("tx-001")).thenReturn(round);

      // Original debit transactions: bonus=-20, cash=-10
      WalletTransactionEntity bonusOrigTx = new WalletTransactionEntity();
      bonusOrigTx.setAmount(new BigDecimal("-20.00"));
      WalletTransactionEntity cashOrigTx = new WalletTransactionEntity();
      cashOrigTx.setAmount(new BigDecimal("-10.00"));
      when(walletTransactionDao.selectOne(any())).thenReturn(bonusOrigTx).thenReturn(cashOrigTx);

      // Wallets
      WalletEntity bonusWallet = buildBonusWallet(new BigDecimal("30.00"));
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.BONUS.getValue())).thenReturn(bonusWallet);

      WalletEntity cashWallet = buildWallet(); // balance=100
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(cashWallet);

      WalletTransactionEntity refundTx = new WalletTransactionEntity();
      refundTx.setBalanceAfter(BigDecimal.ZERO);
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(refundTx);

      ResponseDTO<CallbackResponseVO> result = gameTransactionManager.executeRollback(form, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.CANCELLED.getValue());
      assertThat(result.getData().getBonusBalance()).isEqualByComparingTo("50.00"); // 30+20
      assertThat(result.getData().getBalance()).isEqualByComparingTo("110.00"); // 100+10
      verify(walletManager, times(2)).credit(any(), any());
    }
  }

  @Nested
  @DisplayName("executeTimeout 逾時處理")
  class ExecuteTimeoutTest {

    @Test
    @DisplayName("OPEN -> TIMEOUT — 成功")
    void executeTimeout_success() {
      GameRoundEntity round = buildRound();
      round.setRoundId(100L);
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      gameTransactionManager.executeTimeout(100L);

      assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.TIMEOUT.getValue());
      verify(gameRoundDao).updateById(round);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.GAME_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("已非 OPEN — 跳過")
    void executeTimeout_notOpen_skip() {
      GameRoundEntity round = buildRound();
      round.setRoundId(100L);
      round.setStatus(RoundStatusEnum.SETTLED.getValue());
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      gameTransactionManager.executeTimeout(100L);

      verify(gameRoundDao, never()).updateById(any(GameRoundEntity.class));
    }

    @Test
    @DisplayName("Round 不存在 — 跳過")
    void executeTimeout_notFound_skip() {
      when(gameRoundDao.selectById(100L)).thenReturn(null);

      gameTransactionManager.executeTimeout(100L);

      verify(gameRoundDao, never()).updateById(any(GameRoundEntity.class));
    }
  }

  @Nested
  @DisplayName("executeResettlement 重新結算")
  class ExecuteResettlementTest {

    @Test
    @DisplayName("增加派彩 — credit wallet + ADJUSTED")
    void executeResettlement_creditDelta() {
      GameRoundEntity round = buildSettledRound();
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      WalletEntity wallet = buildWallet();
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("110.00"));
      when(walletManager.credit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      // Original payout = 20, new = 30, delta = +10 (credit)
      ResponseDTO<CallbackResponseVO> result =
          gameTransactionManager.executeResettlement(100L, new BigDecimal("30.00"), "req-001", 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
      verify(walletManager).credit(any(), any());
      verify(gameRoundDao).updateById(round);
    }

    @Test
    @DisplayName("減少派彩 — debit wallet + ADJUSTED")
    void executeResettlement_debitDelta() {
      GameRoundEntity round = buildSettledRound();
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      WalletEntity wallet = buildWallet();
      when(walletDao.selectForUpdate(1L, WalletTypeEnum.CASH.getValue())).thenReturn(wallet);

      WalletTransactionEntity txEntity = new WalletTransactionEntity();
      txEntity.setBalanceAfter(new BigDecimal("90.00"));
      when(walletManager.debit(any(WalletEntity.class), any(WalletTransactionEntity.class)))
          .thenReturn(txEntity);

      // Original payout = 20, new = 10, delta = -10 (debit)
      ResponseDTO<CallbackResponseVO> result =
          gameTransactionManager.executeResettlement(100L, new BigDecimal("10.00"), "req-002", 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
      verify(walletManager).debit(any(), any());
    }

    @Test
    @DisplayName("非 SETTLED 狀態 — 拒絕")
    void executeResettlement_invalidState() {
      GameRoundEntity round = buildRound(); // status = OPEN
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      ResponseDTO<CallbackResponseVO> result =
          gameTransactionManager.executeResettlement(100L, new BigDecimal("30.00"), "req-003", 1L);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("delta=0 — 直接返回 ADJUSTED（無錢包操作）")
    void executeResettlement_zeroDelta() {
      GameRoundEntity round = buildSettledRound();
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      // Same payout amount, delta = 0
      ResponseDTO<CallbackResponseVO> result =
          gameTransactionManager.executeResettlement(100L, new BigDecimal("20.00"), "req-004", 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getStatus()).isEqualTo(RoundStatusEnum.ADJUSTED.getValue());
      verify(walletManager, never()).credit(any(), any());
      verify(walletManager, never()).debit(any(), any());
    }
  }

  @Nested
  @DisplayName("markPendingReview 待審核標記")
  class MarkPendingReviewTest {

    @Test
    @DisplayName("成功 — 設定 PENDING_REVIEW + 發佈事件")
    void markPendingReview_success() {
      GameRoundEntity round = buildRound();
      round.setRoundId(100L);
      when(gameRoundDao.selectById(100L)).thenReturn(round);

      gameTransactionManager.markPendingReview(100L, "GP unavailable");

      assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.PENDING_REVIEW.getValue());
      verify(gameRoundDao).updateById(round);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.GAME_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("Round 不存在 — 跳過")
    void markPendingReview_notFound_skip() {
      when(gameRoundDao.selectById(100L)).thenReturn(null);

      gameTransactionManager.markPendingReview(100L, "GP unavailable");

      verify(gameRoundDao, never()).updateById(any(GameRoundEntity.class));
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

  private WalletEntity buildBonusWallet(BigDecimal balance) {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(2L);
    wallet.setPlayerId(1L);
    wallet.setBalance(balance);
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

  private GameRoundEntity buildSettledRound() {
    GameRoundEntity round = buildRound();
    round.setRoundId(100L);
    round.setStatus(RoundStatusEnum.SETTLED.getValue());
    round.setPayoutAmount(new BigDecimal("20.00"));
    return round;
  }
}
