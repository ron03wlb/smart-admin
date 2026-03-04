package net.lab1024.sa.system.mfa.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * MFA Recovery Reset Form
 *
 * <p>Form for resetting MFA using recovery code.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Data
@Schema(description = "MFA 恢復碼重置表單")
public class MfaRecoveryResetForm {

  @Schema(
      description = "恢復碼（8位數字，從 Email 中獲取）",
      example = "12345678",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "恢復碼不能為空")
  @Pattern(regexp = "^[0-9]{8}$", message = "恢復碼必須為8位數字")
  private String recoveryCode;
}
