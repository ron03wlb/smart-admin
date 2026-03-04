package net.lab1024.sa.system.mfa.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * MFA 啟用表單
 *
 * <p>用於用戶首次啟用 MFA 時的驗證。需要輸入 TOTP token 以確認用戶已正確綁定 Authenticator。
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
public class MfaEnableForm {

  @Schema(hidden = true)
  private Long employeeId;

  @Schema(description = "TOTP 驗證碼（6位數字）", example = "123456")
  @NotBlank(message = "TOTP 驗證碼不能為空")
  @Pattern(regexp = "^[0-9]{6}$", message = "TOTP 驗證碼必須為6位數字")
  private String totpToken;

  @Schema(description = "是否信任此設備（30天內免MFA驗證）", example = "false")
  private Boolean trustDevice = false;

  @Schema(description = "設備名稱（例如：我的iPhone 15）", example = "我的筆記本電腦")
  @Length(max = 100, message = "設備名稱最多100字符")
  private String deviceName;
}
