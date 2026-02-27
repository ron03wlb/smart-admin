package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.risk.dao.RiskAssessmentDao;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.manager.RiskScoreManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreManager 測試")
class RiskScoreManagerTest {

  @Mock private RiskAssessmentDao riskAssessmentDao;
  @Mock private RiskScoreDao riskScoreDao;
  @InjectMocks private RiskScoreManager riskScoreManager;

  @Nested
  @DisplayName("saveTransactionScore 測試")
  class SaveTransactionScoreTest {

    @Test
    @DisplayName("儲存風險評估記錄")
    void save_assessment() {
      RiskAssessmentEntity assessment = new RiskAssessmentEntity();
      assessment.setPlayerId(100L);
      assessment.setRiskScore(BigDecimal.valueOf(50));
      assessment.setDecision(2);

      riskScoreManager.saveTransactionScore(assessment);

      verify(riskAssessmentDao).insert(assessment);
    }
  }

  @Nested
  @DisplayName("updateProfileScore 測試")
  class UpdateProfileScoreTest {

    @Test
    @DisplayName("新玩家建立風險檔案")
    void new_player_creates_profile() {
      when(riskScoreDao.findByPlayerIdAndTenantId(100L, 1L)).thenReturn(null);

      RiskScoreEntity result = riskScoreManager.updateProfileScore(100L, 1L, 25);

      ArgumentCaptor<RiskScoreEntity> captor = ArgumentCaptor.forClass(RiskScoreEntity.class);
      verify(riskScoreDao).insert(captor.capture());
      RiskScoreEntity saved = captor.getValue();
      assertThat(saved.getPlayerId()).isEqualTo(100L);
      assertThat(saved.getCumulativeScore()).isEqualByComparingTo(BigDecimal.valueOf(25));
      assertThat(saved.getRiskLevel()).isEqualTo(RiskLevelEnum.LOW.getValue());
      assertThat(saved.getTotalAssessments()).isEqualTo(1);
      assertThat(saved.getAutoLocked()).isFalse();
      assertThat(result).isEqualTo(saved);
    }

    @Test
    @DisplayName("加權移動平均更新 (70% old + 30% new)")
    void weighted_moving_average_update() {
      RiskScoreEntity existing = buildProfile(100L, 1L, new BigDecimal("40"), 5, false);
      when(riskScoreDao.findByPlayerIdAndTenantId(100L, 1L)).thenReturn(existing);

      riskScoreManager.updateProfileScore(100L, 1L, 60);

      // 40 * 0.70 + 60 * 0.30 = 28 + 18 = 46
      assertThat(existing.getCumulativeScore()).isEqualByComparingTo(new BigDecimal("46.0000"));
      assertThat(existing.getTotalAssessments()).isEqualTo(6);
      assertThat(existing.getRiskLevel()).isEqualTo(RiskLevelEnum.MEDIUM.getValue());
      verify(riskScoreDao).updateById(existing);
    }

    @Test
    @DisplayName("累積分數 >= 80 自動凍結")
    void auto_lock_when_score_exceeds_80() {
      RiskScoreEntity existing = buildProfile(100L, 1L, new BigDecimal("90"), 10, false);
      when(riskScoreDao.findByPlayerIdAndTenantId(100L, 1L)).thenReturn(existing);

      riskScoreManager.updateProfileScore(100L, 1L, 95);

      // 90 * 0.70 + 95 * 0.30 = 63 + 28.5 = 91.5
      assertThat(existing.getCumulativeScore()).isEqualByComparingTo(new BigDecimal("91.5000"));
      assertThat(existing.getAutoLocked()).isTrue();
      assertThat(existing.getRiskLevel()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());
    }

    @Test
    @DisplayName("已凍結玩家不重複設定 autoLocked")
    void already_locked_stays_locked() {
      RiskScoreEntity existing = buildProfile(100L, 1L, new BigDecimal("85"), 20, true);
      when(riskScoreDao.findByPlayerIdAndTenantId(100L, 1L)).thenReturn(existing);

      riskScoreManager.updateProfileScore(100L, 1L, 50);

      // 85 * 0.70 + 50 * 0.30 = 59.5 + 15 = 74.5 (below 80 but already locked)
      assertThat(existing.getAutoLocked()).isTrue();
      verify(riskScoreDao).updateById(existing);
    }

    @Test
    @DisplayName("低分保持 LOW 等級")
    void low_score_stays_low() {
      RiskScoreEntity existing = buildProfile(100L, 1L, new BigDecimal("10"), 3, false);
      when(riskScoreDao.findByPlayerIdAndTenantId(100L, 1L)).thenReturn(existing);

      riskScoreManager.updateProfileScore(100L, 1L, 5);

      // 10 * 0.70 + 5 * 0.30 = 7 + 1.5 = 8.5
      assertThat(existing.getCumulativeScore()).isEqualByComparingTo(new BigDecimal("8.5000"));
      assertThat(existing.getRiskLevel()).isEqualTo(RiskLevelEnum.LOW.getValue());
      assertThat(existing.getAutoLocked()).isFalse();
    }

    @Test
    @DisplayName("新玩家高分自動凍結")
    void new_player_high_score_auto_locks() {
      when(riskScoreDao.findByPlayerIdAndTenantId(200L, 1L)).thenReturn(null);

      RiskScoreEntity result = riskScoreManager.updateProfileScore(200L, 1L, 85);

      ArgumentCaptor<RiskScoreEntity> captor = ArgumentCaptor.forClass(RiskScoreEntity.class);
      verify(riskScoreDao).insert(captor.capture());
      assertThat(captor.getValue().getAutoLocked()).isTrue();
      assertThat(captor.getValue().getRiskLevel()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());
    }

    @Test
    @DisplayName("邊界值 score=30 為 MEDIUM")
    void boundary_score_30_is_medium() {
      when(riskScoreDao.findByPlayerIdAndTenantId(300L, 1L)).thenReturn(null);

      riskScoreManager.updateProfileScore(300L, 1L, 30);

      ArgumentCaptor<RiskScoreEntity> captor = ArgumentCaptor.forClass(RiskScoreEntity.class);
      verify(riskScoreDao).insert(captor.capture());
      assertThat(captor.getValue().getRiskLevel()).isEqualTo(RiskLevelEnum.MEDIUM.getValue());
    }

    @Test
    @DisplayName("邊界值 score=50 為 HIGH")
    void boundary_score_50_is_high() {
      when(riskScoreDao.findByPlayerIdAndTenantId(400L, 1L)).thenReturn(null);

      riskScoreManager.updateProfileScore(400L, 1L, 50);

      ArgumentCaptor<RiskScoreEntity> captor = ArgumentCaptor.forClass(RiskScoreEntity.class);
      verify(riskScoreDao).insert(captor.capture());
      assertThat(captor.getValue().getRiskLevel()).isEqualTo(RiskLevelEnum.HIGH.getValue());
    }
  }

  private RiskScoreEntity buildProfile(
      Long playerId, Long tenantId, BigDecimal score, int assessments, boolean locked) {
    RiskScoreEntity e = new RiskScoreEntity();
    e.setRiskScoreId(1L);
    e.setPlayerId(playerId);
    e.setTenantId(tenantId);
    e.setCumulativeScore(score);
    e.setTotalAssessments(assessments);
    e.setAutoLocked(locked);
    e.setRiskLevel(RiskLevelEnum.LOW.getValue());
    return e;
  }
}
