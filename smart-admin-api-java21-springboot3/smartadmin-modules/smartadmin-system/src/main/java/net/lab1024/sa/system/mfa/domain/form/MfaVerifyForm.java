package net.lab1024.sa.system.mfa.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * MFA 驗證表單
 *
 * <p>用於 MFA 驗證場景（登入、敏感操作等）。支持兩種 token 類型：
 *
 * <ul>
 *   <li>6位 TOTP token（來自 Google Authenticator 等）
 *   <li>8位備份碼（一次性使用）
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
public class MfaVerifyForm {

  @Schema(hidden = true)
  private Long employeeId;

  @Schema(
      description = "MFA 驗證碼（6位TOTP或8位備份碼）",
      example = "123456",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "MFA 驗證碼不能為空")
  @Pattern(regexp = "^[0-9]{6,8}$", message = "MFA 驗證碼必須為6位或8位數字")
  private String mfaToken;

  @Schema(description = "是否信任此設備（30天內免MFA驗證）", example = "false")
  private Boolean trustDevice = false;

  @Schema(description = "設備名稱（例如：我的iPhone 15）", example = "我的筆記本電腦")
  @Length(max = 100, message = "設備名稱最多100字符")
  private String deviceName;
}
