package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.controller.KycAdminController;
import net.lab1024.sa.igaming.player.domain.vo.KycDocumentVO;
import net.lab1024.sa.igaming.player.service.KycVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KycAdminController unit tests — delegation with ResponseDTO.ok() wrapping.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KycAdminController 單元測試")
class KycAdminControllerTest {

  @Mock private KycVerificationService kycVerificationService;

  @InjectMocks private KycAdminController kycAdminController;

  @Test
  @DisplayName("queryPendingDocuments → ResponseDTO.ok() 包裝 service 結果")
  void queryPendingDocuments_wrapsInOk() {
    KycDocumentVO doc = new KycDocumentVO();
    when(kycVerificationService.queryPendingDocuments()).thenReturn(List.of(doc));

    ResponseDTO<List<KycDocumentVO>> result = kycAdminController.queryPendingDocuments();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(1);
    verify(kycVerificationService).queryPendingDocuments();
  }
}
