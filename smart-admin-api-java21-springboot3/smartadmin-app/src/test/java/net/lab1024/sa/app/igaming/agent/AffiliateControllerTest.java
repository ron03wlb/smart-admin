package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.affiliate.controller.AffiliateController;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.AffiliateRegisterForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.AffiliateAgentVO;
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AffiliateController unit tests — delegation verification.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AffiliateController 單元測試")
class AffiliateControllerTest {

  @Mock private AffiliateService affiliateService;

  @InjectMocks private AffiliateController affiliateController;

  @Test
  @DisplayName("register → 委派至 affiliateService.registerAgent")
  void register_delegatesToService() {
    AffiliateRegisterForm form = new AffiliateRegisterForm();
    ResponseDTO<AffiliateAgentVO> expected = ResponseDTO.ok(new AffiliateAgentVO());
    when(affiliateService.registerAgent(form)).thenReturn(expected);

    ResponseDTO<AffiliateAgentVO> result = affiliateController.register(form);

    assertThat(result).isEqualTo(expected);
    verify(affiliateService).registerAgent(form);
  }
}
