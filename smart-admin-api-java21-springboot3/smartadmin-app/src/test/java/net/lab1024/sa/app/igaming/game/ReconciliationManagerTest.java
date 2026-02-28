package net.lab1024.sa.app.igaming.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import io.vavr.control.Try;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.igaming.common.constant.ReconciliationStatusEnum;
import net.lab1024.sa.igaming.common.constant.RoundStatusEnum;
import net.lab1024.sa.igaming.game.adapter.GPAdapterFactory;
import net.lab1024.sa.igaming.game.adapter.GameProviderAdapter;
import net.lab1024.sa.igaming.game.dao.GameProviderDao;
import net.lab1024.sa.igaming.game.dao.GameRoundDao;
import net.lab1024.sa.igaming.game.dao.ReconciliationDao;
import net.lab1024.sa.igaming.game.domain.dto.GameRoundDetail;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import net.lab1024.sa.igaming.game.domain.entity.GameRoundEntity;
import net.lab1024.sa.igaming.game.domain.entity.ReconciliationEntity;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReconciliationManager unit tests — three-layer game reconciliation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReconciliationManager 單元測試")
class ReconciliationManagerTest {

  @Mock private GameRoundDao gameRoundDao;
  @Mock private ReconciliationDao reconciliationDao;
  @Mock private GameProviderDao gameProviderDao;
  @Mock private GPAdapterFactory gpAdapterFactory;

  @InjectMocks private ReconciliationManager reconciliationManager;

  @Nested
  @DisplayName("Layer 1: verifyTransaction")
  class VerifyTransactionTests {

    @Test
    @DisplayName("金額一致 → 狀態 VERIFIED")
    void verifyTransaction_match() {
      GameRoundEntity round = buildRound("TX001");
      when(gameRoundDao.selectByTransactionId("TX001")).thenReturn(round);

      reconciliationManager.verifyTransaction(
          "TX001", new BigDecimal("50.00"), new BigDecimal("50.00"));

      assertThat(round.getReconciliationStatus())
          .isEqualTo(ReconciliationStatusEnum.VERIFIED.getValue());
      verify(gameRoundDao).updateById(round);
    }

    @Test
    @DisplayName("金額不一致 → 狀態 MISMATCH")
    void verifyTransaction_mismatch() {
      GameRoundEntity round = buildRound("TX002");
      when(gameRoundDao.selectByTransactionId("TX002")).thenReturn(round);

      reconciliationManager.verifyTransaction(
          "TX002", new BigDecimal("50.00"), new BigDecimal("55.00"));

      assertThat(round.getReconciliationStatus())
          .isEqualTo(ReconciliationStatusEnum.MISMATCH.getValue());
      verify(gameRoundDao).updateById(round);
    }

    @Test
    @DisplayName("回合不存在 → 不更新")
    void verifyTransaction_roundNotFound() {
      when(gameRoundDao.selectByTransactionId("TX003")).thenReturn(null);

      reconciliationManager.verifyTransaction(
          "TX003", new BigDecimal("50.00"), new BigDecimal("50.00"));

      verify(gameRoundDao, never()).updateById(any(GameRoundEntity.class));
    }
  }

  @Nested
  @DisplayName("Layer 2: pollMissingSettlements")
  class PollMissingSettlementsTests {

    @Test
    @DisplayName("GP 已完成但平台未結算 → 補償")
    void pollMissingSettlements_compensated() {
      GameRoundEntity round = buildRound("TX010");
      round.setProviderCode("GP_A");
      round.setGpRoundId("GP_ROUND_1");
      round.setStatus(RoundStatusEnum.OPEN.getValue());
      when(gameRoundDao.selectUnsettledRoundsOlderThan(10)).thenReturn(List.of(round));

      GameProviderAdapter adapter = mock(GameProviderAdapter.class);
      when(gpAdapterFactory.getAdapter("GP_A")).thenReturn(Option.of(adapter));

      GameRoundDetail detail = new GameRoundDetail();
      detail.setStatus("COMPLETED");
      detail.setPayoutAmount(new BigDecimal("120.00"));
      when(adapter.queryRound("GP_ROUND_1")).thenReturn(Try.success(detail));

      int result = reconciliationManager.pollMissingSettlements();

      assertThat(result).isEqualTo(1);
      assertThat(round.getStatus()).isEqualTo(RoundStatusEnum.SETTLED.getValue());
      assertThat(round.getReconciliationStatus())
          .isEqualTo(ReconciliationStatusEnum.COMPENSATED.getValue());
      assertThat(round.getPayoutAmount()).isEqualByComparingTo("120.00");
      verify(gameRoundDao).updateById(round);
    }

    @Test
    @DisplayName("無未結算回合 → 返回 0")
    void pollMissingSettlements_noUnsettled() {
      when(gameRoundDao.selectUnsettledRoundsOlderThan(10)).thenReturn(Collections.emptyList());

      int result = reconciliationManager.pollMissingSettlements();

      assertThat(result).isEqualTo(0);
      verify(gameRoundDao, never()).updateById(any(GameRoundEntity.class));
    }
  }

  @Nested
  @DisplayName("Layer 3: dailyBatchReconciliation")
  class DailyBatchTests {

    @Test
    @DisplayName("全部驗證通過 → 狀態 VERIFIED")
    void dailyBatchReconciliation_allVerified() {
      GameProviderEntity provider = buildProvider("GP_X");
      when(gameProviderDao.selectEnabledProviders()).thenReturn(List.of(provider));

      GameRoundEntity round1 = buildRoundWithReconStatus(ReconciliationStatusEnum.VERIFIED);
      GameRoundEntity round2 = buildRoundWithReconStatus(ReconciliationStatusEnum.VERIFIED);
      when(gameRoundDao.selectByProviderAndDate(eq("GP_X"), any(LocalDate.class)))
          .thenReturn(List.of(round1, round2));

      reconciliationManager.dailyBatchReconciliation(LocalDate.of(2026, 2, 17));

      ArgumentCaptor<ReconciliationEntity> captor =
          ArgumentCaptor.forClass(ReconciliationEntity.class);
      verify(reconciliationDao).insert(captor.capture());

      ReconciliationEntity recon = captor.getValue();
      assertThat(recon.getMatchedCount()).isEqualTo(2);
      assertThat(recon.getMismatchCount()).isEqualTo(0);
      assertThat(recon.getMissingCount()).isEqualTo(0);
      assertThat(recon.getStatus()).isEqualTo(ReconciliationStatusEnum.VERIFIED.getValue());
    }

    @Test
    @DisplayName("有不一致 → 狀態 MISMATCH")
    void dailyBatchReconciliation_hasMismatch() {
      GameProviderEntity provider = buildProvider("GP_Y");
      when(gameProviderDao.selectEnabledProviders()).thenReturn(List.of(provider));

      GameRoundEntity round1 = buildRoundWithReconStatus(ReconciliationStatusEnum.VERIFIED);
      GameRoundEntity round2 = buildRoundWithReconStatus(ReconciliationStatusEnum.MISMATCH);
      when(gameRoundDao.selectByProviderAndDate(eq("GP_Y"), any(LocalDate.class)))
          .thenReturn(List.of(round1, round2));

      reconciliationManager.dailyBatchReconciliation(LocalDate.of(2026, 2, 17));

      ArgumentCaptor<ReconciliationEntity> captor =
          ArgumentCaptor.forClass(ReconciliationEntity.class);
      verify(reconciliationDao).insert(captor.capture());

      ReconciliationEntity recon = captor.getValue();
      assertThat(recon.getMatchedCount()).isEqualTo(1);
      assertThat(recon.getMismatchCount()).isEqualTo(1);
      assertThat(recon.getStatus()).isEqualTo(ReconciliationStatusEnum.MISMATCH.getValue());
    }
  }

  // --- Helpers ---

  private GameRoundEntity buildRound(String transactionId) {
    GameRoundEntity round = new GameRoundEntity();
    round.setRoundId(1L);
    round.setTransactionId(transactionId);
    round.setBetAmount(new BigDecimal("50.00"));
    return round;
  }

  private GameRoundEntity buildRoundWithReconStatus(ReconciliationStatusEnum status) {
    GameRoundEntity round = new GameRoundEntity();
    round.setReconciliationStatus(status.getValue());
    return round;
  }

  private GameProviderEntity buildProvider(String providerCode) {
    GameProviderEntity provider = new GameProviderEntity();
    provider.setProviderCode(providerCode);
    provider.setTenantId(1L);
    provider.setEnabled(true);
    return provider;
  }
}
