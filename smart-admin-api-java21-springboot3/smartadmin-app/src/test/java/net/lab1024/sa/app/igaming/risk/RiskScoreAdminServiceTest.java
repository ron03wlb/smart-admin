package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.risk.dao.RiskAssessmentDao;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskResetAutoLockForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskAssessmentVO;
import net.lab1024.sa.igaming.risk.service.RiskScoreAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RiskScoreAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreAdminService 單元測試")
class RiskScoreAdminServiceTest {

  @Mock private RiskScoreDao riskScoreDao;
  @Mock private RiskAssessmentDao riskAssessmentDao;
  @InjectMocks private RiskScoreAdminService riskScoreAdminService;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("resetAutoLock — 成功解除自動鎖定")
  void resetAutoLock_success() {
    RiskScoreEntity profile = new RiskScoreEntity();
    profile.setPlayerId(200L);
    profile.setAutoLocked(true);
    when(riskScoreDao.findByPlayerIdAndTenantId(200L, 1L)).thenReturn(profile);

    RiskResetAutoLockForm form = new RiskResetAutoLockForm();
    form.setPlayerId(200L);
    form.setReason("Manual review completed");

    ResponseDTO<String> result = riskScoreAdminService.resetAutoLock(form);

    assertThat(result.getOk()).isTrue();
    assertThat(profile.getAutoLocked()).isFalse();
  }

  @Test
  @DisplayName("resetAutoLock — 不存在返回錯誤")
  void resetAutoLock_notFound() {
    when(riskScoreDao.findByPlayerIdAndTenantId(999L, 1L)).thenReturn(null);

    RiskResetAutoLockForm form = new RiskResetAutoLockForm();
    form.setPlayerId(999L);
    form.setReason("test");

    ResponseDTO<String> result = riskScoreAdminService.resetAutoLock(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("resetAutoLock — 非鎖定狀態返回錯誤")
  void resetAutoLock_notLocked() {
    RiskScoreEntity profile = new RiskScoreEntity();
    profile.setPlayerId(200L);
    profile.setAutoLocked(false);
    when(riskScoreDao.findByPlayerIdAndTenantId(200L, 1L)).thenReturn(profile);

    RiskResetAutoLockForm form = new RiskResetAutoLockForm();
    form.setPlayerId(200L);
    form.setReason("test");

    ResponseDTO<String> result = riskScoreAdminService.resetAutoLock(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("queryPlayerAssessments — 成功返回評估歷史")
  void queryPlayerAssessments_success() {
    RiskAssessmentEntity entity = new RiskAssessmentEntity();
    entity.setAssessmentId(1L);
    entity.setPlayerId(200L);
    entity.setTenantId(1L);
    entity.setRiskScore(new BigDecimal("45"));
    entity.setCreateTime(OffsetDateTime.of(2026, 2, 28, 10, 0, 0, 0, ZoneOffset.UTC));
    when(riskAssessmentDao.selectList(any())).thenReturn(List.of(entity));

    ResponseDTO<List<RiskAssessmentVO>> result = riskScoreAdminService.queryPlayerAssessments(200L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
    assertThat(result.getData().get(0).getAssessmentId()).isEqualTo(1L);
    assertThat(result.getData().get(0).getRiskScore()).isEqualByComparingTo(new BigDecimal("45"));
  }
}
