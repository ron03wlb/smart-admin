package net.lab1024.sa.app.igaming.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.WalletErrorCode;
import net.lab1024.sa.igaming.wallet.controller.WalletController;
import net.lab1024.sa.igaming.wallet.domain.form.WalletQueryForm;
import net.lab1024.sa.igaming.wallet.domain.vo.WalletVO;
import net.lab1024.sa.igaming.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * WalletController unit tests — Option handling and delegation verification.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WalletController 單元測試")
class WalletControllerTest {

  @Mock private WalletService walletService;

  @InjectMocks private WalletController walletController;

  @Nested
  @DisplayName("getWallet")
  class GetWalletTests {

    @Test
    @DisplayName("錢包存在 → ok(WalletVO)")
    void getWallet_found() {
      WalletVO vo = new WalletVO();
      when(walletService.getWallet(1L)).thenReturn(Option.some(vo));

      ResponseDTO<WalletVO> result = walletController.getWallet(1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEqualTo(vo);
    }

    @Test
    @DisplayName("錢包不存在 → WALLET_NOT_FOUND")
    void getWallet_notFound() {
      when(walletService.getWallet(999L)).thenReturn(Option.none());

      ResponseDTO<WalletVO> result = walletController.getWallet(999L);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(WalletErrorCode.WALLET_NOT_FOUND.getMsg());
    }
  }

  @Nested
  @DisplayName("queryWallets")
  class QueryWalletsTests {

    @Test
    @DisplayName("委派至 walletService.queryWallets")
    void queryWallets_delegatesToService() {
      WalletQueryForm form = new WalletQueryForm();
      ResponseDTO<PageResult<WalletVO>> expected = ResponseDTO.ok(new PageResult<>());
      when(walletService.queryWallets(form)).thenReturn(expected);

      ResponseDTO<PageResult<WalletVO>> result = walletController.queryWallets(form);

      assertThat(result).isEqualTo(expected);
      verify(walletService).queryWallets(form);
    }
  }
}
