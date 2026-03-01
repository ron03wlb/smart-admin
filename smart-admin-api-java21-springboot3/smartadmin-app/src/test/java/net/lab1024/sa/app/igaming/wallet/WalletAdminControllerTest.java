package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.wallet.controller.WalletAdminController;
import net.lab1024.sa.igaming.wallet.domain.form.WalletAdminAdjustForm;
import net.lab1024.sa.igaming.wallet.domain.form.WalletFreezeForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletTransactionVO;
import net.lab1024.sa.igaming.wallet.service.WalletAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WalletAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletAdminController 單元測試")
class WalletAdminControllerTest {

  @Mock private WalletAdminService walletAdminService;
  @InjectMocks private WalletAdminController walletAdminController;

  @Test
  @DisplayName("freeze — 委託 Service 成功")
  void freeze_success() {
    WalletFreezeForm form = new WalletFreezeForm();
    form.setPlayerId(100L);
    form.setReason("Fraud detected");
    when(walletAdminService.freezePlayerWallets(any())).thenReturn(ResponseDTO.ok(3));

    ResponseDTO<Integer> result = walletAdminController.freeze(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(3);
  }

  @Test
  @DisplayName("adjust — 委託 Service 成功")
  void adjust_success() {
    WalletAdminAdjustForm form = new WalletAdminAdjustForm();
    form.setWalletId(1L);
    form.setAmount(new BigDecimal("25.00"));
    form.setRequestId("req-001");
    form.setReason("Compensation");

    WalletTransactionVO vo = new WalletTransactionVO();
    vo.setTransactionId(1L);
    vo.setAmount(new BigDecimal("25.00"));
    when(walletAdminService.adminAdjust(any())).thenReturn(ResponseDTO.ok(vo));

    ResponseDTO<WalletTransactionVO> result = walletAdminController.adjust(form);
    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getAmount()).isEqualByComparingTo("25.00");
  }
}
