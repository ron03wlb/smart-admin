package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.player.dao.KycDocumentDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.KycDocumentEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.KycApprovalManager;
import net.lab1024.sa.igaming.player.service.KycVerificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KycVerificationService unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KycVerificationService 單元測試")
class KycVerificationServiceTest {

  @Mock private PlayerDao playerDao;
  @Mock private KycDocumentDao kycDocumentDao;
  @Mock private KycApprovalManager kycApprovalManager;

  @InjectMocks private KycVerificationService kycVerificationService;

  // ==================== checkWithdrawalEligibility ====================

  @Nested
  @DisplayName("checkWithdrawalEligibility 提款資格檢查")
  class WithdrawalEligibilityTest {

    @Test
    @DisplayName("L0 玩家 $500 以內允許提款")
    void l0_withinLimit() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L0);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("400"));

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("L0 玩家超過 $500 拒絕提款")
    void l0_exceedsLimit() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L0);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("600"));

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("L1 玩家 $5000 以內允許提款")
    void l1_withinLimit() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L1);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("4000"));

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("L2 玩家無限額")
    void l2_unlimited() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L2);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("1000000"));

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("玩家不存在拒絕")
    void playerNotFound() {
      when(playerDao.selectById(999L)).thenReturn(null);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(999L, new BigDecimal("100"));

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== submitL1Document ====================

  @Nested
  @DisplayName("submitL1Document L1 文件提交")
  class SubmitL1Test {

    @Test
    @DisplayName("成功提交 L1 文件 (POC 自動審核)")
    void submitL1_success() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L0);
      when(playerDao.selectById(1L)).thenReturn(player);
      when(kycDocumentDao.insert(any(KycDocumentEntity.class))).thenReturn(1);

      ResponseDTO<Void> result =
          kycVerificationService.submitL1Document(
              1L, KycDocumentTypeEnum.ID_CARD, "https://example.com/doc.jpg");

      assertThat(result.getOk()).isTrue();
      verify(kycApprovalManager).approveKycL1(any(), any());
    }

    @Test
    @DisplayName("玩家不存在拒絕提交")
    void submitL1_playerNotFound() {
      when(playerDao.selectById(999L)).thenReturn(null);

      ResponseDTO<Void> result =
          kycVerificationService.submitL1Document(
              999L, KycDocumentTypeEnum.PASSPORT, "https://example.com/doc.jpg");

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== helpers ====================

  private PlayerEntity buildPlayer(KycLevelEnum kycLevel) {
    PlayerEntity player = new PlayerEntity();
    player.setPlayerId(1L);
    player.setUsername("testplayer");
    player.setKycLevel(kycLevel.getValue());
    player.setDeleted(false);
    return player;
  }
}
