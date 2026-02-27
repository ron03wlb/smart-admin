package net.lab1024.sa.igaming.player.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.IgamingSwaggerTagConst;
import net.lab1024.sa.igaming.player.domain.form.KycReviewForm;
import net.lab1024.sa.igaming.player.domain.vo.KycDocumentVO;
import net.lab1024.sa.igaming.player.service.KycVerificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * KYC Admin Controller — admin endpoints for KYC L2 document review.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@RestController
@Tag(name = IgamingSwaggerTagConst.PLAYER)
@RequiredArgsConstructor
public class KycAdminController {

  private final KycVerificationService kycVerificationService;

  @Operation(summary = "Query pending KYC documents")
  @GetMapping("/igaming/admin/kyc/pending")
  @SaCheckPermission("player:kyc:review")
  public ResponseDTO<List<KycDocumentVO>> queryPendingDocuments() {
    return ResponseDTO.ok(kycVerificationService.queryPendingDocuments());
  }

  @Operation(summary = "Get KYC document detail")
  @GetMapping("/igaming/admin/kyc/get/{kycDocumentId}")
  @SaCheckPermission("player:kyc:review")
  public ResponseDTO<KycDocumentVO> getKycDocument(@PathVariable Long kycDocumentId) {
    return kycVerificationService.getKycDocument(kycDocumentId);
  }

  @Operation(summary = "Approve KYC L2 document")
  @PostMapping("/igaming/admin/kyc/approve")
  @SaCheckPermission("player:kyc:review")
  public ResponseDTO<Void> approveKycL2(@RequestBody @Valid KycReviewForm form) {
    return kycVerificationService.approveL2Document(form.getKycDocumentId(), form.getComment());
  }

  @Operation(summary = "Reject KYC L2 document")
  @PostMapping("/igaming/admin/kyc/reject")
  @SaCheckPermission("player:kyc:review")
  public ResponseDTO<Void> rejectKycL2(@RequestBody @Valid KycReviewForm form) {
    return kycVerificationService.rejectL2Document(form.getKycDocumentId(), form.getComment());
  }
}
