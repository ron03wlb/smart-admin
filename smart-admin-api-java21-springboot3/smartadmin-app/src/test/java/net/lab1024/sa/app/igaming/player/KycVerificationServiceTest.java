package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum;
import net.lab1024.sa.igaming.player.dao.KycDocumentDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.KycDocumentEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.vo.KycDocumentVO;
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
@SuppressWarnings("unchecked")
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

  // ==================== submitL2Document ====================

  @Nested
  @DisplayName("submitL2Document L2 文件提交")
  class SubmitL2Test {

    @Test
    @DisplayName("L1 玩家提交 L2 文件 — 成功 (需人工審核)")
    void submitL2_success() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L1);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.submitL2Document(
              1L, KycDocumentTypeEnum.PASSPORT, "https://cdn/doc.jpg");

      assertThat(result.getOk()).isTrue();
      verify(kycDocumentDao).insert(any(KycDocumentEntity.class));
      // L2 documents are NOT auto-approved
      verifyNoInteractions(kycApprovalManager);
    }

    @Test
    @DisplayName("L0 玩家提交 L2 文件 — 被拒 (需先完成 L1)")
    void submitL2_kycLevelInsufficient() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L0);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.submitL2Document(
              1L, KycDocumentTypeEnum.PASSPORT, "https://cdn/doc.jpg");

      assertThat(result.getOk()).isFalse();
      verify(kycDocumentDao, never()).insert(any(KycDocumentEntity.class));
    }

    @Test
    @DisplayName("玩家不存在 — 回傳錯誤")
    void submitL2_playerNotFound() {
      when(playerDao.selectById(99L)).thenReturn(null);

      ResponseDTO<Void> result =
          kycVerificationService.submitL2Document(
              99L, KycDocumentTypeEnum.PASSPORT, "https://cdn/doc.jpg");

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== L2 review ====================

  @Nested
  @DisplayName("L2 文件審核")
  class ReviewL2Test {

    @Test
    @DisplayName("核准 L2 文件 — 成功")
    void approveL2_success() {
      KycDocumentEntity document = buildDocument(KycVerificationStatusEnum.PENDING);
      when(kycDocumentDao.selectById(20L)).thenReturn(document);

      ResponseDTO<Void> result = kycVerificationService.approveL2Document(20L, "Looks good");

      assertThat(result.getOk()).isTrue();
      verify(kycApprovalManager).approveKycL2(document, "Looks good");
    }

    @Test
    @DisplayName("拒絕 L2 文件 — 成功")
    void rejectL2_success() {
      KycDocumentEntity document = buildDocument(KycVerificationStatusEnum.PENDING);
      when(kycDocumentDao.selectById(20L)).thenReturn(document);

      ResponseDTO<Void> result = kycVerificationService.rejectL2Document(20L, "Blurry photo");

      assertThat(result.getOk()).isTrue();
      verify(kycApprovalManager).rejectKycL2(document, "Blurry photo");
    }

    @Test
    @DisplayName("文件不存在 — 回傳錯誤")
    void approveL2_documentNotFound() {
      when(kycDocumentDao.selectById(99L)).thenReturn(null);

      ResponseDTO<Void> result = kycVerificationService.approveL2Document(99L, "Looks good");

      assertThat(result.getOk()).isFalse();
      verifyNoInteractions(kycApprovalManager);
    }

    @Test
    @DisplayName("已審核文件 — 回傳錯誤")
    void approveL2_alreadyReviewed() {
      KycDocumentEntity document = buildDocument(KycVerificationStatusEnum.APPROVED);
      when(kycDocumentDao.selectById(20L)).thenReturn(document);

      ResponseDTO<Void> result = kycVerificationService.approveL2Document(20L, "Looks good");

      assertThat(result.getOk()).isFalse();
      verifyNoInteractions(kycApprovalManager);
    }
  }

  // ==================== getKycDocument ====================

  @Nested
  @DisplayName("getKycDocument 取得 KYC 文件")
  class GetKycDocumentTest {

    @Test
    @DisplayName("成功取得文件")
    void getKycDocument_success() {
      KycDocumentEntity document = buildDocument(KycVerificationStatusEnum.PENDING);
      when(kycDocumentDao.selectById(20L)).thenReturn(document);

      ResponseDTO<KycDocumentVO> result = kycVerificationService.getKycDocument(20L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getKycDocumentId()).isEqualTo(20L);
      assertThat(result.getData().getPlayerId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("文件不存在 — 回傳錯誤")
    void getKycDocument_notFound() {
      when(kycDocumentDao.selectById(99L)).thenReturn(null);

      ResponseDTO<KycDocumentVO> result = kycVerificationService.getKycDocument(99L);

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== withdrawal boundary ====================

  @Nested
  @DisplayName("提款邊界值測試")
  class WithdrawalBoundaryTest {

    @Test
    @DisplayName("L0 玩家剛好 $500 允許提款")
    void l0_exactlyAtLimit() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L0);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("500"));

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("L1 玩家剛好 $5000 允許提款")
    void l1_exactlyAtLimit() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L1);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("5000"));

      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("L1 玩家 $5001 超限拒絕提款")
    void l1_exceedsLimit() {
      PlayerEntity player = buildPlayer(KycLevelEnum.L1);
      when(playerDao.selectById(1L)).thenReturn(player);

      ResponseDTO<Void> result =
          kycVerificationService.checkWithdrawalEligibility(1L, new BigDecimal("5001"));

      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== queryPendingDocuments ====================

  @Nested
  @DisplayName("查詢待審核文件")
  class QueryPendingTest {

    @Test
    @DisplayName("queryPendingDocuments — 回傳 VO 列表")
    void queryPending_returnsList() {
      KycDocumentEntity doc1 = buildDocument(KycVerificationStatusEnum.PENDING);
      doc1.setKycDocumentId(1L);
      doc1.setPlayerId(10L);

      when(kycDocumentDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(doc1));

      List<KycDocumentVO> result = kycVerificationService.queryPendingDocuments();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getKycDocumentId()).isEqualTo(1L);
      assertThat(result.get(0).getPlayerId()).isEqualTo(10L);
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

  private KycDocumentEntity buildDocument(KycVerificationStatusEnum status) {
    KycDocumentEntity document = new KycDocumentEntity();
    document.setKycDocumentId(20L);
    document.setPlayerId(1L);
    document.setDocumentType(KycDocumentTypeEnum.PASSPORT.getValue());
    document.setDocumentUrl("https://cdn/doc.jpg");
    document.setVerificationStatus(status.getValue());
    return document;
  }
}
