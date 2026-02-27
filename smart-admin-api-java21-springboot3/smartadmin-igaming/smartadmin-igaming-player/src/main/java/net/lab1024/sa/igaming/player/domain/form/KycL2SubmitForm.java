package net.lab1024.sa.igaming.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;

/**
 * KYC L2 document submission form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class KycL2SubmitForm {

  @Schema(description = "Player ID")
  @NotNull(message = "playerId required")
  private Long playerId;

  @SchemaEnum(KycDocumentTypeEnum.class)
  @CheckEnum(message = "documentType invalid", value = KycDocumentTypeEnum.class)
  @NotNull(message = "documentType required")
  private Integer documentType;

  @Schema(description = "Document URL (S3/MinIO)")
  @NotBlank(message = "documentUrl required")
  private String documentUrl;
}
