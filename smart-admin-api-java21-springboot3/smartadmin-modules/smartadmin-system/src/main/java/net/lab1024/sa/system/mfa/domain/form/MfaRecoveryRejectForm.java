package net.lab1024.sa.system.mfa.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * MFA Recovery Reject Form
 *
 * <p>Form for rejecting a recovery request (Super Admin only).
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Data
@Schema(description = "MFA 恢復請求拒絕表單")
public class MfaRecoveryRejectForm {

  @Schema(description = "恢復請求 ID", example = "12345", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "恢復請求 ID 不能為空")
  private Long recoveryId;

  @Schema(
      description = "拒絕原因",
      example = "身份驗證信息不足，請聯繫 IT 部門",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "拒絕原因不能為空")
  @Length(min = 10, max = 500, message = "拒絕原因長度必須在10-500字符之間")
  private String rejectionReason;
}
