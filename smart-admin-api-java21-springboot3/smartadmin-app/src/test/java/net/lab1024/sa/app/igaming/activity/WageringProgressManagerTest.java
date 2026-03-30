package net.lab1024.sa.app.igaming.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import net.lab1024.sa.igaming.common.constant.BonusStatusEnum;
import net.lab1024.sa.igaming.game.dao.GameDao;
import net.lab1024.sa.igaming.game.dao.GameWeightConfigDao;
import net.lab1024.sa.igaming.wallet.dao.WalletBonusExtDao;
import net.lab1024.sa.igaming.wallet.domain.entity.WalletBonusExtEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WageringProgressManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WageringProgressManager 單元測試")
@SuppressWarnings("unchecked")
class WageringProgressManagerTest {

  @Mock private PlayerBonusRecordDao playerBonusRecordDao;
  @Mock private WalletBonusExtDao walletBonusExtDao;
  @Mock private GameDao gameDao;
  @Mock private GameWeightConfigDao gameWeightConfigDao;
  @Mock private DomainEventPublisher domainEventPublisher;

  private net.lab1024.sa.igaming.activity.manager.WageringProgressManager wageringProgressManager;

  @BeforeEach
  void setUp() {
    wageringProgressManager =
        new net.lab1024.sa.igaming.activity.manager.WageringProgressManager(
            playerBonusRecordDao,
            walletBonusExtDao,
            gameDao,
            gameWeightConfigDao,
            Optional.of(domainEventPublisher));
  }

  @Nested
  @DisplayName("updateWageringProgress")
  class UpdateWageringProgressTest {

    @Test
    @DisplayName("成功更新流水進度")
    void updateProgress_success() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWageringRequired(new BigDecimal("2000.00"));
      record.setWageringCompleted(new BigDecimal("500.00"));
      record.setWalletBonusExtId(10L);
      when(playerBonusRecordDao.selectActiveByPlayerId(1L)).thenReturn(List.of(record));

      WalletBonusExtEntity ext = buildBonusExt();
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      int updated =
          wageringProgressManager.updateWageringProgress(
              1L, "SLOT001", new BigDecimal("100.00"), 1L);

      assertThat(updated).isEqualTo(1);
      assertThat(record.getWageringCompleted()).isEqualByComparingTo("600.00");
      verify(playerBonusRecordDao).updateById(record);
      verify(walletBonusExtDao).updateById(ext);
    }

    @Test
    @DisplayName("流水達標 — 自動完成")
    void updateProgress_completes() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWageringRequired(new BigDecimal("2000.00"));
      record.setWageringCompleted(new BigDecimal("1950.00"));
      record.setWalletBonusExtId(10L);
      when(playerBonusRecordDao.selectActiveByPlayerId(1L)).thenReturn(List.of(record));

      WalletBonusExtEntity ext = buildBonusExt();
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      int updated =
          wageringProgressManager.updateWageringProgress(
              1L, "SLOT001", new BigDecimal("100.00"), 1L);

      assertThat(updated).isEqualTo(1);
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.COMPLETED.getValue());
      assertThat(record.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("無活躍獎金 — 返回 0")
    void updateProgress_noActiveRecords() {
      when(playerBonusRecordDao.selectActiveByPlayerId(1L)).thenReturn(Collections.emptyList());

      int updated =
          wageringProgressManager.updateWageringProgress(
              1L, "SLOT001", new BigDecimal("100.00"), 1L);

      assertThat(updated).isEqualTo(0);
      verify(playerBonusRecordDao, never()).updateById(any(PlayerBonusRecordEntity.class));
    }

    @Test
    @DisplayName("精確達標 (2000 == 2000) — 自動完成")
    void updateProgress_exactThreshold() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWageringRequired(new BigDecimal("2000.00"));
      record.setWageringCompleted(new BigDecimal("1900.00"));
      record.setWalletBonusExtId(10L);
      when(playerBonusRecordDao.selectActiveByPlayerId(1L)).thenReturn(List.of(record));

      WalletBonusExtEntity ext = buildBonusExt();
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      int updated =
          wageringProgressManager.updateWageringProgress(
              1L, "SLOT001", new BigDecimal("100.00"), 1L);

      assertThat(updated).isEqualTo(1);
      assertThat(record.getWageringCompleted()).isEqualByComparingTo("2000.00");
      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.COMPLETED.getValue());
    }

    @Test
    @DisplayName("遊戲權重放大有效投注")
    void updateProgress_gameWeight() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWageringRequired(new BigDecimal("2000.00"));
      record.setWageringCompleted(BigDecimal.ZERO);
      when(playerBonusRecordDao.selectActiveByPlayerId(1L)).thenReturn(List.of(record));

      int updated =
          wageringProgressManager.updateWageringProgress(
              1L, "LIVE001", new BigDecimal("200.00"), 1L);

      assertThat(updated).isEqualTo(1);
      // betAmount is passed directly (game weight already applied upstream in
      // TurnoverCalculationService)
      assertThat(record.getWageringCompleted()).isEqualByComparingTo("200.00");
    }
  }

  @Nested
  @DisplayName("completeBonus")
  class CompleteBonusTest {

    @Test
    @DisplayName("成功完成獎金")
    void completeBonus_success() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWalletBonusExtId(10L);

      WalletBonusExtEntity ext = buildBonusExt();
      when(walletBonusExtDao.selectById(10L)).thenReturn(ext);

      wageringProgressManager.completeBonus(record);

      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.COMPLETED.getValue());
      assertThat(record.getCompletedAt()).isNotNull();
      assertThat(ext.getStatus()).isEqualTo(BonusStatusEnum.COMPLETED.getValue());
      verify(playerBonusRecordDao).updateById(record);
      verify(walletBonusExtDao).updateById(ext);
    }

    @Test
    @DisplayName("無 walletBonusExtId — 僅更新記錄")
    void completeBonus_noExtId() {
      PlayerBonusRecordEntity record = buildActiveRecord();
      record.setWalletBonusExtId(null);

      wageringProgressManager.completeBonus(record);

      assertThat(record.getStatus()).isEqualTo(BonusRecordStatusEnum.COMPLETED.getValue());
      verify(playerBonusRecordDao).updateById(record);
      verify(walletBonusExtDao, never()).selectById(any());
    }
  }

  private PlayerBonusRecordEntity buildActiveRecord() {
    PlayerBonusRecordEntity record = new PlayerBonusRecordEntity();
    record.setRecordId(1L);
    record.setTenantId(1L);
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

  private WalletBonusExtEntity buildBonusExt() {
    WalletBonusExtEntity ext = new WalletBonusExtEntity();
    ext.setId(10L);
    ext.setBalance(new BigDecimal("100.00"));
    ext.setWageredAmount(BigDecimal.ZERO);
    ext.setStatus(BonusStatusEnum.ACTIVE.getValue());
    return ext;
  }
}
