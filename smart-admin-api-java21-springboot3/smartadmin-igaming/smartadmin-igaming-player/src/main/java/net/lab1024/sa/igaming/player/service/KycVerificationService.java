package net.lab1024.sa.igaming.player.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum;
import net.lab1024.sa.igaming.player.dao.KycDocumentDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.KycDocumentEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.vo.KycDocumentVO;
import net.lab1024.sa.igaming.player.manager.KycApprovalManager;
import org.springframework.stereotype.Service;

/**
 * KYC Verification Service — handles KYC level checks and document submission.
 *
 * <p>POC phase: L1 documents are auto-approved. L2 is a placeholder.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class KycVerificationService {

  /** Monthly withdrawal limits by KYC level. */
  private static final BigDecimal L0_MONTHLY_LIMIT = new BigDecimal("500");

  private static final BigDecimal L1_MONTHLY_LIMIT = new BigDecimal("5000");

  private final PlayerDao playerDao;
  private final KycDocumentDao kycDocumentDao;
  private final KycApprovalManager kycApprovalManager;

  /**
   * Check if a player is eligible to withdraw a given amount based on KYC level.
   *
   * @param playerId player ID
   * @param amount withdrawal amount
   * @return ResponseDTO.ok() if eligible, error otherwise
   */
  public ResponseDTO<Void> checkWithdrawalEligibility(Long playerId, BigDecimal amount) {
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }

    Integer kycLevel = player.getKycLevel();
    if (kycLevel.equals(KycLevelEnum.L2.getValue())) {
      return ResponseDTO.ok();
    }

    BigDecimal limit =
        kycLevel.equals(KycLevelEnum.L1.getValue()) ? L1_MONTHLY_LIMIT : L0_MONTHLY_LIMIT;

    if (amount.compareTo(limit) > 0) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.KYC_LEVEL_INSUFFICIENT.getMsg());
    }
    return ResponseDTO.ok();
  }

  /**
   * Submit a KYC L1 document.
   *
   * <p>POC phase: auto-approves and upgrades KYC level to L1.
   *
   * @param playerId player ID
   * @param documentType document type
   * @param documentUrl document URL
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> submitL1Document(
      Long playerId, KycDocumentTypeEnum documentType, String documentUrl) {
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }

    KycDocumentEntity document = new KycDocumentEntity();
    document.setPlayerId(playerId);
    document.setDocumentType(documentType.getValue());
    document.setDocumentUrl(documentUrl);
    document.setVerificationStatus(KycVerificationStatusEnum.PENDING.getValue());
    kycDocumentDao.insert(document);

    // POC: auto-approve L1 documents
    kycApprovalManager.approveKycL1(player, document);

    return ResponseDTO.ok();
  }

  /**
   * Submit a KYC L2 document for admin review.
   *
   * <p>Unlike L1, L2 documents require manual admin review before approval.
   *
   * @param playerId player ID
   * @param documentType document type
   * @param documentUrl document URL
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> submitL2Document(
      Long playerId, KycDocumentTypeEnum documentType, String documentUrl) {
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null || player.getDeleted()) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.PLAYER_NOT_FOUND.getMsg());
    }

    if (!KycLevelEnum.L1.getValue().equals(player.getKycLevel())) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.KYC_LEVEL_INSUFFICIENT.getMsg());
    }

    KycDocumentEntity document = new KycDocumentEntity();
    document.setPlayerId(playerId);
    document.setDocumentType(documentType.getValue());
    document.setDocumentUrl(documentUrl);
    document.setVerificationStatus(KycVerificationStatusEnum.PENDING.getValue());
    kycDocumentDao.insert(document);

    return ResponseDTO.ok();
  }

  /**
   * Query pending KYC documents for admin review.
   *
   * @return list of pending KYC document VOs
   */
  public List<KycDocumentVO> queryPendingDocuments() {
    List<KycDocumentEntity> entities =
        kycDocumentDao.selectList(
            Wrappers.<KycDocumentEntity>lambdaQuery()
                .eq(
                    KycDocumentEntity::getVerificationStatus,
                    KycVerificationStatusEnum.PENDING.getValue())
                .orderByAsc(KycDocumentEntity::getCreateTime));
    return entities.stream().map(this::toKycDocumentVO).toList();
  }

  /**
   * Get KYC document by ID for review.
   *
   * @param kycDocumentId document ID
   * @return ResponseDTO containing KycDocumentVO or error
   */
  public ResponseDTO<KycDocumentVO> getKycDocument(Long kycDocumentId) {
    KycDocumentEntity document = kycDocumentDao.selectById(kycDocumentId);
    if (document == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.KYC_DOCUMENT_NOT_FOUND.getMsg());
    }
    return ResponseDTO.ok(toKycDocumentVO(document));
  }

  /**
   * Approve a KYC L2 document.
   *
   * @param kycDocumentId document ID
   * @param comment reviewer comment
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> approveL2Document(Long kycDocumentId, String comment) {
    KycDocumentEntity document = kycDocumentDao.selectById(kycDocumentId);
    if (document == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.KYC_DOCUMENT_NOT_FOUND.getMsg());
    }
    if (!KycVerificationStatusEnum.PENDING.getValue().equals(document.getVerificationStatus())) {
      return ResponseDTO.userErrorParam("Document already reviewed");
    }

    kycApprovalManager.approveKycL2(document, comment);
    return ResponseDTO.ok();
  }

  /**
   * Reject a KYC L2 document.
   *
   * @param kycDocumentId document ID
   * @param comment reviewer comment
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> rejectL2Document(Long kycDocumentId, String comment) {
    KycDocumentEntity document = kycDocumentDao.selectById(kycDocumentId);
    if (document == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.KYC_DOCUMENT_NOT_FOUND.getMsg());
    }
    if (!KycVerificationStatusEnum.PENDING.getValue().equals(document.getVerificationStatus())) {
      return ResponseDTO.userErrorParam("Document already reviewed");
    }

    kycApprovalManager.rejectKycL2(document, comment);
    return ResponseDTO.ok();
  }

  private KycDocumentVO toKycDocumentVO(KycDocumentEntity entity) {
    KycDocumentVO vo = new KycDocumentVO();
    vo.setKycDocumentId(entity.getKycDocumentId());
    vo.setPlayerId(entity.getPlayerId());
    vo.setDocumentType(entity.getDocumentType());
    vo.setDocumentUrl(entity.getDocumentUrl());
    vo.setVerificationStatus(entity.getVerificationStatus());
    vo.setReviewerComment(entity.getReviewerComment());
    vo.setCreateTime(entity.getCreateTime());
    vo.setUpdateTime(entity.getUpdateTime());
    return vo;
  }
}
