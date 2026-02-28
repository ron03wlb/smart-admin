package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.controller.RiskProposalController;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalQueryForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.igaming.risk.service.RiskProposalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RiskProposalController unit tests — delegation verification.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskProposalController 單元測試")
class RiskProposalControllerTest {

  @Mock private RiskProposalService riskProposalService;

  @InjectMocks private RiskProposalController riskProposalController;

  @Test
  @DisplayName("queryPage → 委派至 riskProposalService.queryPage")
  void queryPage_delegatesToService() {
    RiskProposalQueryForm form = new RiskProposalQueryForm();
    ResponseDTO<PageResult<RiskProposalVO>> expected = ResponseDTO.ok(new PageResult<>());
    when(riskProposalService.queryPage(form)).thenReturn(expected);

    ResponseDTO<PageResult<RiskProposalVO>> result = riskProposalController.queryPage(form);

    assertThat(result).isEqualTo(expected);
    verify(riskProposalService).queryPage(form);
  }
}
