package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import io.vavr.control.Option;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;
import net.lab1024.sa.igaming.player.controller.PlayerController;
import net.lab1024.sa.igaming.player.domain.vo.PlayerVO;
import net.lab1024.sa.igaming.player.service.KycVerificationService;
import net.lab1024.sa.igaming.player.service.PlayerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PlayerController unit tests — enum mapping, Option handling, and KYC delegation.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlayerController 單元測試")
class PlayerControllerTest {

  @Mock private PlayerService playerService;
  @Mock private KycVerificationService kycVerificationService;

  @InjectMocks private PlayerController playerController;

  @Nested
  @DisplayName("getPlayer")
  class GetPlayerTests {

    @Test
    @DisplayName("玩家存在 → ok(PlayerVO)")
    void getPlayer_found() {
      PlayerVO vo = new PlayerVO();
      when(playerService.getPlayer(1L)).thenReturn(Option.some(vo));

      ResponseDTO<PlayerVO> result = playerController.getPlayer(1L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEqualTo(vo);
    }

    @Test
    @DisplayName("玩家不存在 → PLAYER_NOT_FOUND")
    void getPlayer_notFound() {
      when(playerService.getPlayer(999L)).thenReturn(Option.none());

      ResponseDTO<PlayerVO> result = playerController.getPlayer(999L);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }
  }

  @Nested
  @DisplayName("changePlayerStatus")
  class ChangePlayerStatusTests {

    @Test
    @DisplayName("有效 enum 值 → 委派 Service")
    void changePlayerStatus_validEnum() {
      when(playerService.changePlayerStatus(1L, PlayerStatusEnum.LOCKED, "admin", "violation"))
          .thenReturn(ResponseDTO.ok());

      ResponseDTO<Void> result =
          playerController.changePlayerStatus(
              1L, PlayerStatusEnum.LOCKED.getValue(), "admin", "violation");

      assertThat(result.getOk()).isTrue();
      verify(playerService).changePlayerStatus(1L, PlayerStatusEnum.LOCKED, "admin", "violation");
    }

    @Test
    @DisplayName("無效 enum 值 → Invalid status value")
    void changePlayerStatus_invalidEnum() {
      ResponseDTO<Void> result = playerController.changePlayerStatus(1L, 999, "admin", "violation");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PlayerErrorCode.INVALID_STATUS_VALUE.getMsg());
      verifyNoInteractions(playerService);
    }
  }

  @Nested
  @DisplayName("changeVipLevel")
  class ChangeVipLevelTests {

    @Test
    @DisplayName("有效 VIP 等級 → 委派 Service")
    void changeVipLevel_validEnum() {
      when(playerService.changeVipLevel(1L, VipLevelEnum.GOLD, "promotion"))
          .thenReturn(ResponseDTO.ok());

      ResponseDTO<Void> result =
          playerController.changeVipLevel(1L, VipLevelEnum.GOLD.getValue(), "promotion");

      assertThat(result.getOk()).isTrue();
      verify(playerService).changeVipLevel(1L, VipLevelEnum.GOLD, "promotion");
    }

    @Test
    @DisplayName("無效 VIP 值 → Invalid VIP level value")
    void changeVipLevel_invalidEnum() {
      ResponseDTO<Void> result = playerController.changeVipLevel(1L, 999, "promotion");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PlayerErrorCode.INVALID_VIP_LEVEL_VALUE.getMsg());
      verifyNoInteractions(playerService);
    }
  }

  @Nested
  @DisplayName("submitKycDocument")
  class SubmitKycDocumentTests {

    @Test
    @DisplayName("無效文件類型 → Invalid document type")
    void submitKycDocument_invalidType() {
      ResponseDTO<Void> result =
          playerController.submitKycDocument(1L, 999, "https://example.com/doc.jpg");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(PlayerErrorCode.INVALID_DOCUMENT_TYPE.getMsg());
      verifyNoInteractions(kycVerificationService);
    }
  }
}
