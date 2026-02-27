package net.lab1024.sa.igaming.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * KYC L2 review form (approve/reject).
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class KycReviewForm {

  @Schema(description = "KYC Document ID")
  @NotNull(message = "kycDocumentId required")
  private Long kycDocumentId;

  @Schema(description = "Reviewer comment")
  @NotBlank(message = "comment required")
  private String comment;
}
