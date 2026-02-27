package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.dao.WalletDao;
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

/**
 * BonusLifecycleManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BonusLifecycleManager 單元測試")
@SuppressWarnings("unchecked")
class BonusLifecycleManagerTest {

  @Mock private PlayerBonusRecordDao playerBonusRecordDao;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private WalletDao walletDao;
  @Mock private WalletManager walletManager;

  @InjectMocks
  private net.lab1024.sa.igaming.activity.manager.BonusLifecycleManager bonusLifecycleManager;

  @Nested
  @DisplayName("expireActiveBonuses")
  class ExpireActiveBonusesTest {

    @Test
    @DisplayName("批次過期 — 成功處理")
    void expire_success() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWalletBonusExtId(10L);
      when(playerBonusRecordDao.selectExpiredActive(100)).thenReturn(List.of(record));

      WalletBonusExtEntity ext = buildBonusExt(new BigDecimal("50.00"));
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      WalletEntity bonusWallet = buildBonusWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(bonusWallet);

      int count = bonusLifecycleManager.expireActiveBonuses(100);

      assertThat(count).isEqualTo(1);
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.EXPIRED.getValue());
      assertThat(ext.getStatus()).isEqualTo(BonusStatusEnum.EXPIRED.getValue());
      assertThat(ext.getBalance()).isEqualByComparingTo("0");
      verify(walletManager).debit(any(WalletEntity.class), any(WalletTransactionEntity.class));
    }

    @Test
    @DisplayName("無過期記錄 — 返回 0")
    void expire_noRecords() {
      when(playerBonusRecordDao.selectExpiredActive(100)).thenReturn(Collections.emptyList());

      int count = bonusLifecycleManager.expireActiveBonuses(100);

      assertThat(count).isEqualTo(0);
      verify(walletManager, never()).debit(any(), any());
    }

    @Test
    @DisplayName("ext 餘額為零 — 不扣款")
    void expire_zeroBalance() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWalletBonusExtId(10L);
      when(playerBonusRecordDao.selectExpiredActive(100)).thenReturn(List.of(record));

      WalletBonusExtEntity ext = buildBonusExt(BigDecimal.ZERO);
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      int count = bonusLifecycleManager.expireActiveBonuses(100);

      assertThat(count).isEqualTo(1);
      assertThat(ext.getStatus()).isEqualTo(BonusStatusEnum.EXPIRED.getValue());
      verify(walletManager, never()).debit(any(), any());
    }
  }

  @Nested
  @DisplayName("forfeitBonus")
  class ForfeitBonusTest {

    @Test
    @DisplayName("成功沒收")
    void forfeit_success() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWalletBonusExtId(10L);
      when(playerBonusRecordDao.selectById(1L)).thenReturn(record);

      WalletBonusExtEntity ext = buildBonusExt(new BigDecimal("75.00"));
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      WalletEntity bonusWallet = buildBonusWallet();
      when(walletDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(bonusWallet);

      bonusLifecycleManager.forfeitBonus(1L);

      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.FORFEITED.getValue());
      verify(playerBonusRecordDao).updateById(record);
      verify(walletManager).debit(any(WalletEntity.class), any(WalletTransactionEntity.class));
    }

    @Test
    @DisplayName("記錄不存在 — 忽略")
    void forfeit_notFound() {
      when(playerBonusRecordDao.selectById(1L)).thenReturn(null);

      bonusLifecycleManager.forfeitBonus(1L);

      verify(playerBonusRecordDao, never()).updateById(any(PlayerBonusRecordEntity.class));
    }

    @Test
    @DisplayName("非 ACTIVE 狀態 — 忽略")
    void forfeit_notActive() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setStatus(BonusRecordStatusEnum.COMPLETED.getValue());
      when(playerBonusRecordDao.selectById(1L)).thenReturn(record);

      bonusLifecycleManager.forfeitBonus(1L);

      verify(playerBonusRecordDao, never()).updateById(any(PlayerBonusRecordEntity.class));
    }
  }

  private PlayerBonusRecordEntity buildActiveRecord() {
    PlayerBonusRecordEntity record = new PlayerBonusRecordEntity();
    record.setRecordId(1L);
    record.setPlayerId(1L);
    record.setRuleId(1L);
    record.setClaimId("claim-001");
    record.setBonusAmount(new BigDecimal("100.00"));
    record.setWageringRequired(new BigDecimal("2000.00"));
    record.setWageringCompleted(BigDecimal.ZERO);
    record.setStatus(BonusRecordStatusEnum.ACTIVE.getValue());
    record.setClaimedAt(OffsetDateTime.now(ZoneOffset.UTC));
    record.setDeleted(false);
    return record;
  }

  private WalletBonusExtEntity buildBonusExt(BigDecimal balance) {
    WalletBonusExtEntity ext = new WalletBonusExtEntity();
    ext.setId(10L);
    ext.setBalance(balance);
    ext.setWageredAmount(BigDecimal.ZERO);
    ext.setStatus(BonusStatusEnum.ACTIVE.getValue());
    return ext;
  }

  private WalletEntity buildBonusWallet() {
    WalletEntity wallet = new WalletEntity();
    wallet.setWalletId(1L);
    wallet.setPlayerId(1L);
    wallet.setBalance(new BigDecimal("100.00"));
    wallet.setLockedAmount(BigDecimal.ZERO);
    wallet.setDeleted(false);
    return wallet;
  }
}
