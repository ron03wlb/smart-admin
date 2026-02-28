package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.controller.RiskRuleController;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleQueryForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskRuleVO;
import net.lab1024.sa.igaming.risk.service.RiskRuleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RiskRuleController unit tests — delegation verification.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskRuleController 單元測試")
class RiskRuleControllerTest {

  @Mock private RiskRuleService riskRuleService;

  @InjectMocks private RiskRuleController riskRuleController;

  @Test
  @DisplayName("queryPage → 委派至 riskRuleService.queryPage")
  void queryPage_delegatesToService() {
    RiskRuleQueryForm form = new RiskRuleQueryForm();
    ResponseDTO<PageResult<RiskRuleVO>> expected = ResponseDTO.ok(new PageResult<>());
    when(riskRuleService.queryPage(form)).thenReturn(expected);

    ResponseDTO<PageResult<RiskRuleVO>> result = riskRuleController.queryPage(form);

    assertThat(result).isEqualTo(expected);
    verify(riskRuleService).queryPage(form);
  }
}
