package net.lab1024.sa.system.mfa.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * MFA 設定初始化響應
 *
 * <p>包含初始化 MFA 所需的所有信息：
 *
 * <ul>
 *   <li>TOTP 密鑰（Base32編碼，用於手動輸入）
 *   <li>QR碼 URL（otpauth:// 協議，用於掃描）
 *   <li>QR碼 Data URL（Base64編碼的圖片，前端可直接顯示）
 *   <li>備份碼列表（10個8位數字，僅顯示一次）
 * </ul>
 *
 * <p><strong>安全提示</strong>：此響應僅在首次啟用 MFA 時返回一次，備份碼不會被持久化為明文。
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class MfaSetupInitVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "TOTP 密鑰（Base32編碼）", example = "JBSWY3DPEHPK3PXP")
  private String secret;

  @Schema(
      description = "QR碼 URL（otpauth:// 協議）",
      example =
          "otpauth://totp/SmartAdmin:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=SmartAdmin")
  private String qrCodeUrl;

  @Schema(description = "QR碼 Data URL（Base64編碼圖片）", example = "data:image/png;base64,iVBOR...")
  private String qrCodeDataUrl;

  @Schema(description = "備份碼列表（10個8位數字，僅顯示一次）", example = "[\"12345678\", \"87654321\"]")
  private List<String> backupCodes;

  @Schema(description = "賬號標識（顯示在 Authenticator 中）", example = "user@example.com")
  private String accountName;

  @Schema(description = "發行者名稱（顯示在 Authenticator 中）", example = "SmartAdmin")
  private String issuer;
}
