package net.lab1024.sa.system.mfa.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * MFA Recovery Request Form
 *
 * <p>Form for creating a device loss recovery request.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Data
@Schema(description = "MFA 恢復請求表單")
public class MfaRecoveryRequestForm {

  @Schema(description = "請求郵箱（用於驗證身份）", example = "user@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "郵箱不能為空")
  @Email(message = "郵箱格式不正確")
  private String email;

  @Schema(description = "請求原因（說明為何需要恢復 MFA）", example = "我的手機遺失，無法訪問 Google Authenticator", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "請求原因不能為空")
  @Length(min = 10, max = 500, message = "請求原因長度必須在10-500字符之間")
  private String reason;
}
