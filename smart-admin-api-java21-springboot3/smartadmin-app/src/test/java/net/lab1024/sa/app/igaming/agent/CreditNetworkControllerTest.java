package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.credit.controller.CreditNetworkController;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditAllocateForm;
import net.lab1024.sa.igaming.agent.credit.service.CreditNetworkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CreditNetworkController unit tests — delegation with hardcoded operator.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreditNetworkController 單元測試")
class CreditNetworkControllerTest {

  @Mock private CreditNetworkService creditNetworkService;

  @InjectMocks private CreditNetworkController creditNetworkController;

  @Test
  @DisplayName("allocateCredit → 委派含硬編碼 operator=\"system\"")
  void allocateCredit_delegatesWithSystemOperator() {
    CreditAllocateForm form = new CreditAllocateForm();
    ResponseDTO<String> expected = ResponseDTO.ok("OK");
    when(creditNetworkService.allocateCredit(form, "system")).thenReturn(expected);

    ResponseDTO<String> result = creditNetworkController.allocateCredit(form);

    assertThat(result).isEqualTo(expected);
    verify(creditNetworkService).allocateCredit(form, "system");
  }
}
