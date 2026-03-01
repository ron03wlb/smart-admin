package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.controller.RiskScoreAdminController;
import net.lab1024.sa.igaming.risk.domain.form.RiskResetAutoLockForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskAssessmentVO;
import net.lab1024.sa.igaming.risk.service.RiskScoreAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RiskScoreAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreAdminController 單元測試")
class RiskScoreAdminControllerTest {

  @Mock private RiskScoreAdminService riskScoreAdminService;
  @InjectMocks private RiskScoreAdminController riskScoreAdminController;

  @Test
  @DisplayName("resetLock — 委託 Service 成功")
  void resetLock_success() {
    when(riskScoreAdminService.resetAutoLock(any())).thenReturn(ResponseDTO.ok());

    RiskResetAutoLockForm form = new RiskResetAutoLockForm();
    form.setPlayerId(200L);
    form.setReason("Manual review completed");

    ResponseDTO<String> result = riskScoreAdminController.resetLock(form);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("queryAssessments — 委託 Service 成功")
  void queryAssessments_success() {
    RiskAssessmentVO vo = new RiskAssessmentVO();
    vo.setAssessmentId(1L);
    when(riskScoreAdminService.queryPlayerAssessments(200L))
        .thenReturn(ResponseDTO.ok(List.of(vo)));

    ResponseDTO<List<RiskAssessmentVO>> result = riskScoreAdminController.queryAssessments(200L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
  }
}
