package net.lab1024.sa.igaming.player.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.KycDocumentTypeEnum;
import net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum;

/**
 * KYC document view object for API responses.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class KycDocumentVO {

  @Schema(description = "KYC Document ID")
  private Long kycDocumentId;

  @Schema(description = "Player ID")
  private Long playerId;

  @SchemaEnum(KycDocumentTypeEnum.class)
  private Integer documentType;

  @Schema(description = "Document URL")
  private String documentUrl;

  @SchemaEnum(KycVerificationStatusEnum.class)
  private Integer verificationStatus;

  @Schema(description = "Reviewer comment")
  private String reviewerComment;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;

  @Schema(description = "Updated time")
  private OffsetDateTime updateTime;
}
