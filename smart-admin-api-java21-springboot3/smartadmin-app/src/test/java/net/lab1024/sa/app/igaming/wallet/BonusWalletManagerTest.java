package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
import net.lab1024.sa.igaming.wallet.dao.WalletLockDao;
import net.lab1024.sa.igaming.wallet.dao.WalletTransactionDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletEntity;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletTransactionEntity;
import net.lab1024.sa.igaming.wallet.manager.WalletManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Bonus wallet operations unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-27
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletManager Bonus 操作單元測試")
@SuppressWarnings({"UnusedVariable", "unchecked"})
class BonusWalletManagerTest {

  @Mock private WalletDao walletDao;
  @Mock private WalletTransactionDao walletTransactionDao;
  @Mock private WalletLockDao walletLockDao;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  private WalletManager walletManager;

  @BeforeEach
  void setUp() {
    walletManager =
        new WalletManager(
            walletDao,
            walletTransactionDao,
            walletLockDao,
            walletBonusExtDao,
            Optional.of(domainEventPublisher));
  }

  @Nested
  @DisplayName("creditBonus 紅利入款")
  class CreditBonusTest {

    @Test
    @DisplayName("成功 — 更新錢包餘額 + 插入 BonusExt + 插入交易 + 發佈事件")
    void creditBonus_success() {
      WalletEntity wallet = buildBonusWallet();
      WalletTransactionEntity tx = buildTransaction("0.00", "50.00", "50.00");
      WalletBonusExtEntity ext = buildBonusExt(wallet.getWalletId(), "50.00");
      when(walletDao.updateById(wallet)).thenReturn(1);

      WalletTransactionEntity result = walletManager.creditBonus(wallet, tx, ext);

      assertThat(result).isSameAs(tx);
      verify(walletBonusExtDao).insert(ext);
      verify(walletTransactionDao).insert(tx);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.WALLET_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("樂觀鎖衝突 — 拋出異常")
    void creditBonus_versionConflict() {
      WalletEntity wallet = buildBonusWallet();
      WalletTransactionEntity tx = buildTransaction("0.00", "50.00", "50.00");
      WalletBonusExtEntity ext = buildBonusExt(wallet.getWalletId(), "50.00");
      when(walletDao.updateById(wallet)).thenReturn(0);

      assertThatThrownBy(() -> walletManager.creditBonus(wallet, tx, ext))
          .isInstanceOf(OptimisticLockingFailureException.class);
      verify(walletBonusExtDao, never()).insert(any(WalletBonusExtEntity.class));
    }
  }

  @Nested
  @DisplayName("debitBonus FIFO 扣款")
  class DebitBonusTest {

    @Test
    @DisplayName("FIFO 順序 — 先到期的先扣")
    void debitBonus_fifoOrder() {
      WalletEntity wallet = buildBonusWallet();
      wallet.setBalance(new BigDecimal("100.00"));
      WalletTransactionEntity tx = buildTransaction("100.00", "30.00", "-70.00");
      when(walletDao.updateById(wallet)).thenReturn(1);

      // Two bonus records: ext1 expires sooner (30.00), ext2 later (70.00)
      WalletBonusExtEntity ext1 = buildBonusExt(1L, "30.00");
      ext1.setId(10L);
      ext1.setExpiresAt(OffsetDateTime.of(2026, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC));

      WalletBonusExtEntity ext2 = buildBonusExt(1L, "70.00");
      ext2.setId(11L);
      ext2.setExpiresAt(OffsetDateTime.of(2026, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC));

      when(walletBonusExtDao.selectList(any(LambdaQueryWrapper.class)))
          .thenReturn(List.of(ext1, ext2));

      WalletTransactionEntity result =
          walletManager.debitBonus(wallet, tx, new BigDecimal("70.00"));

      assertThat(result).isSameAs(tx);
      // ext1 fully consumed → balance=0, status=COMPLETED
      assertThat(ext1.getBalance()).isEqualByComparingTo("0.00");
      assertThat(ext1.getStatus()).isEqualTo(BonusStatusEnum.COMPLETED.getValue());
      // ext2 partially consumed → balance=30.00, status still ACTIVE
      assertThat(ext2.getBalance()).isEqualByComparingTo("30.00");
      assertThat(ext2.getStatus()).isEqualTo(BonusStatusEnum.ACTIVE.getValue());
      verify(walletBonusExtDao, times(2)).updateById(any(WalletBonusExtEntity.class));
    }
  }

  @Nested
  @DisplayName("convertBonusToCash BONUS→CASH 轉換")
  class ConvertBonusToCashTest {

    @Test
    @DisplayName("成功 — BONUS 扣款 + CASH 入款 + 記錄兩筆交易")
    void convertBonusToCash_success() {
      WalletBonusExtEntity ext = buildBonusExt(1L, "100.00");
      ext.setId(42L);

      WalletEntity bonusWallet = buildBonusWallet();
      bonusWallet.setBalance(new BigDecimal("100.00"));

      WalletEntity cashWallet = new WalletEntity();
      cashWallet.setWalletId(2L);
      cashWallet.setPlayerId(1L);
      cashWallet.setBalance(new BigDecimal("500.00"));
      cashWallet.setLockedAmount(BigDecimal.ZERO);
      cashWallet.setDeleted(false);

      when(walletDao.updateById(bonusWallet)).thenReturn(1);
      when(walletDao.updateById(cashWallet)).thenReturn(1);

      walletManager.convertBonusToCash(ext, bonusWallet, cashWallet);

      // BONUS wallet deducted
      assertThat(bonusWallet.getBalance()).isEqualByComparingTo("0.00");
      // CASH wallet credited
      assertThat(cashWallet.getBalance()).isEqualByComparingTo("600.00");
      // BonusExt zeroed
      assertThat(ext.getBalance()).isEqualByComparingTo("0.00");
      assertThat(ext.getStatus()).isEqualTo(BonusStatusEnum.COMPLETED.getValue());
      // Two transactions inserted (debit + credit)
      verify(walletTransactionDao, times(2)).insert(any(WalletTransactionEntity.class));
      verify(walletBonusExtDao).updateById(ext);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.WALLET_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("餘額為零 — 跳過轉換")
    void convertBonusToCash_zeroBalance_skipped() {
      WalletBonusExtEntity ext = buildBonusExt(1L, "0.00");
      ext.setId(43L);
      WalletEntity bonusWallet = buildBonusWallet();
      WalletEntity cashWallet = new WalletEntity();

      walletManager.convertBonusToCash(ext, bonusWallet, cashWallet);

      verify(walletDao, never()).updateById(any(WalletEntity.class));
      verify(walletTransactionDao, never()).insert(any(WalletTransactionEntity.class));
    }
  }

  // ==================== helpers ====================

  private WalletEntity buildBonusWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setWalletType(2); // BONUS
    wallet.setBalance(BigDecimal.ZERO);
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }

  private WalletBonusExtEntity buildBonusExt(Long walletId, String balance) {
    WalletBonusExtEntity ext = new WalletBonusExtEntity();
    ext.setWalletId(walletId);
    ext.setBonusId(1L);
    ext.setBalance(new BigDecimal(balance));
    ext.setWageringRequirement(new BigDecimal("500.00"));
    ext.setWageredAmount(BigDecimal.ZERO);
    ext.setExpiresAt(OffsetDateTime.of(2026, 12, 31, 0, 0, 0, 0, ZoneOffset.UTC));
    ext.setStatus(BonusStatusEnum.ACTIVE.getValue());
    return ext;
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
    tx.setTransactionType(5); // BONUS
    return tx;
  }
}
