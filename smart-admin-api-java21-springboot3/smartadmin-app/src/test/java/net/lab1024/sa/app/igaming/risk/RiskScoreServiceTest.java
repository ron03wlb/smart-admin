package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.domain.vo.RiskScoreVO;
import net.lab1024.sa.igaming.risk.service.RiskScoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreService 測試")
class RiskScoreServiceTest {

  @Mock private RiskScoreDao riskScoreDao;
  @InjectMocks private RiskScoreService riskScoreService;

  @Nested
  @DisplayName("getByPlayerId 測試")
  class GetByPlayerIdTest {

    @Test
    @DisplayName("查詢存在的玩家風控分數")
    void get_existing_score() {
      RiskScoreEntity entity = new RiskScoreEntity();
      entity.setRiskScoreId(1L);
      entity.setPlayerId(100L);
      entity.setTenantId(1L);
      entity.setCumulativeScore(new BigDecimal("45.00"));
      entity.setRiskLevel(2);
      entity.setAutoLocked(false);
      when(riskScoreDao.findByPlayerIdAndTenantId(100L, 1L)).thenReturn(entity);

      ResponseDTO<RiskScoreVO> result = riskScoreService.getByPlayerId(100L, 1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getPlayerId()).isEqualTo(100L);
      assertThat(result.getData().getCumulativeScore()).isEqualByComparingTo("45.00");
    }

    @Test
    @DisplayName("查詢不存在的玩家風控分數 - 返回錯誤")
    void get_nonexistent_score() {
      when(riskScoreDao.findByPlayerIdAndTenantId(999L, 1L)).thenReturn(null);

      ResponseDTO<RiskScoreVO> result = riskScoreService.getByPlayerId(999L, 1L);

      assertThat(result.getOk()).isFalse();
    }
  }
}
