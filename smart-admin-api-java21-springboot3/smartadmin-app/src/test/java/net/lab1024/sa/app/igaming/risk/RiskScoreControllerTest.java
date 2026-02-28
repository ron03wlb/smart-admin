package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.controller.RiskScoreController;
import net.lab1024.sa.igaming.risk.domain.form.RiskScoreQueryForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskScoreVO;
import net.lab1024.sa.igaming.risk.service.RiskScoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RiskScoreController unit tests — delegation verification.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskScoreController 單元測試")
class RiskScoreControllerTest {

  @Mock private RiskScoreService riskScoreService;

  @InjectMocks private RiskScoreController riskScoreController;

  @Test
  @DisplayName("queryPage → 委派至 riskScoreService.queryPage")
  void queryPage_delegatesToService() {
    RiskScoreQueryForm form = new RiskScoreQueryForm();
    ResponseDTO<PageResult<RiskScoreVO>> expected = ResponseDTO.ok(new PageResult<>());
    when(riskScoreService.queryPage(form)).thenReturn(expected);

    ResponseDTO<PageResult<RiskScoreVO>> result = riskScoreController.queryPage(form);

    assertThat(result).isEqualTo(expected);
    verify(riskScoreService).queryPage(form);
  }
}
