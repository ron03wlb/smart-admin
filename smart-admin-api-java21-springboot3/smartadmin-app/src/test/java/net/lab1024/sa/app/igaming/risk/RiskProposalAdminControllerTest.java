package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.risk.controller.RiskProposalAdminController;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalAssignForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.igaming.risk.service.RiskProposalAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RiskProposalAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskProposalAdminController 單元測試")
class RiskProposalAdminControllerTest {

  @Mock private RiskProposalAdminService riskProposalAdminService;
  @InjectMocks private RiskProposalAdminController riskProposalAdminController;

  @Test
  @DisplayName("getById — 委託 Service 成功")
  void getById_success() {
    RiskProposalVO vo = new RiskProposalVO();
    vo.setProposalId(100L);
    when(riskProposalAdminService.getProposalById(100L)).thenReturn(ResponseDTO.ok(vo));

    ResponseDTO<RiskProposalVO> result = riskProposalAdminController.getById(100L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getProposalId()).isEqualTo(100L);
  }

  @Test
  @DisplayName("assign — 委託 Service 成功")
  void assign_success() {
    when(riskProposalAdminService.assignProposal(any())).thenReturn(ResponseDTO.ok());

    RiskProposalAssignForm form = new RiskProposalAssignForm();
    form.setProposalId(100L);
    form.setAssignee("reviewer01");

    ResponseDTO<String> result = riskProposalAdminController.assign(form);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("escalate — 委託 Service 成功")
  void escalate_success() {
    when(riskProposalAdminService.escalateProposal(100L)).thenReturn(ResponseDTO.ok());

    ResponseDTO<String> result = riskProposalAdminController.escalate(100L);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("triggerEscalation — 委託 Service 成功")
  void triggerEscalation_success() {
    when(riskProposalAdminService.triggerOverdueEscalation()).thenReturn(ResponseDTO.ok(5));

    ResponseDTO<Integer> result = riskProposalAdminController.triggerEscalation();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(5);
  }
}
