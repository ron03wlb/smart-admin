package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
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
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * WalletManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletManager 單元測試")
@SuppressWarnings("UnusedVariable")
class WalletManagerTest {

  @Mock private WalletDao walletDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private WalletLockDao walletLockDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private WalletManager walletManager;

  @Nested
  @DisplayName("credit 入款")
  class CreditTest {

    @Test
    @DisplayName("成功 — 更新餘額 + 插入交易 + 發佈事件")
    void credit_success() {
      WalletEntity wallet = buildWallet();
      WalletTransactionEntity tx = buildTransaction("100.00", "120.00", "20.00");
      when(walletDao.updateById(wallet)).thenReturn(1);

      WalletTransactionEntity result = walletManager.credit(wallet, tx);

      assertThat(result).isSameAs(tx);
      verify(walletTransactionDao).insert(any(WalletTransactionEntity.class));
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.WALLET_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("樂觀鎖衝突 — 拋出 OptimisticLockingFailureException")
    void credit_versionConflict() {
      WalletEntity wallet = buildWallet();
      WalletTransactionEntity tx = buildTransaction("100.00", "120.00", "20.00");
      when(walletDao.updateById(wallet)).thenReturn(0);

      assertThatThrownBy(() -> walletManager.credit(wallet, tx))
          .isInstanceOf(OptimisticLockingFailureException.class);
      verify(walletTransactionDao, never()).insert(any(WalletTransactionEntity.class));
      verify(domainEventPublisher, never()).publish(anyString(), any(DomainEvent.class));
    }

    @Test
    @DisplayName("DuplicateKey — Layer 2 冪等性")
    @SuppressWarnings("unchecked")
    void credit_duplicateKey() {
      WalletEntity wallet = buildWallet();
      WalletTransactionEntity tx = buildTransaction("100.00", "120.00", "20.00");
      tx.setRequestId("req-001");
      when(walletDao.updateById(wallet)).thenReturn(1);
      doThrow(new DuplicateKeyException("Duplicate"))
          .when(walletTransactionDao)
          .insert(any(WalletTransactionEntity.class));

      WalletTransactionEntity existing = new WalletTransactionEntity();
      existing.setRequestId("req-001");
      when(walletTransactionDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

      WalletTransactionEntity result = walletManager.credit(wallet, tx);

      assertThat(result.getRequestId()).isEqualTo("req-001");
      // No event published for idempotent duplicate
      verify(domainEventPublisher, never()).publish(anyString(), any(DomainEvent.class));
    }
  }

  @Nested
  @DisplayName("debit 扣款")
  class DebitTest {

    @Test
    @DisplayName("成功 — 更新餘額 + 插入交易 + 發佈事件")
    void debit_success() {
      WalletEntity wallet = buildWallet();
      WalletTransactionEntity tx = buildTransaction("100.00", "90.00", "-10.00");
      when(walletDao.updateById(wallet)).thenReturn(1);

      WalletTransactionEntity result = walletManager.debit(wallet, tx);

      assertThat(result).isSameAs(tx);
      verify(walletTransactionDao).insert(any(WalletTransactionEntity.class));
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.WALLET_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("樂觀鎖衝突 — 拋出 OptimisticLockingFailureException")
    void debit_versionConflict() {
      WalletEntity wallet = buildWallet();
      WalletTransactionEntity tx = buildTransaction("100.00", "90.00", "-10.00");
      when(walletDao.updateById(wallet)).thenReturn(0);

      assertThatThrownBy(() -> walletManager.debit(wallet, tx))
          .isInstanceOf(OptimisticLockingFailureException.class);
      verify(domainEventPublisher, never()).publish(anyString(), any(DomainEvent.class));
    }
  }

  // ==================== helpers ====================

  private WalletEntity buildWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setBalance(new BigDecimal("100.00"));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }

  private WalletTransactionEntity buildTransaction(
      String balanceBefore, String balanceAfter, String amount) {
    WalletTransactionEntity tx = new WalletTransactionEntity();
    tx.setWalletId(1L);
    tx.setPlayerId(1L);
    tx.setBalanceBefore(new BigDecimal(balanceBefore));
    tx.setBalanceAfter(new BigDecimal(balanceAfter));
    tx.setAmount(new BigDecimal(amount));
    tx.setRequestId("req-" + System.nanoTime());
    tx.setTransactionType(1);
    return tx;
  }
}
