package net.lab1024.sa.igaming.activity.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Form for claiming a bonus.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class BonusClaimForm {

  @Schema(description = "Promotion code")
  @NotBlank
  @Size(max = 64)
  private String promotionCode;

  @Schema(description = "Idempotency claim ID")
  @NotBlank
  @Size(max = 64)
  private String claimId;
}
