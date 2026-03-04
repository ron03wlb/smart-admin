package net.lab1024.sa.system.mfa.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * MFA 狀態查詢響應
 *
 * <p>用於查詢用戶當前的 MFA 配置狀態，包括：
 *
 * <ul>
 *   <li>是否已啟用 MFA
 *   <li>MFA 類型（TOTP、SMS、Email等）
 *   <li>備份碼狀態（是否已生成、剩餘數量）
 *   <li>最後驗證時間
 *   <li>是否由角色強制要求 MFA（無法禁用）
 * </ul>
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
public class MfaStatusVO implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "是否已啟用 MFA", example = "true")
  private Boolean mfaEnabled;

  @Schema(description = "MFA 類型", example = "TOTP")
  private String mfaType;

  @Schema(description = "是否已生成備份碼", example = "true")
  private Boolean backupCodesGenerated;

  @Schema(description = "剩餘備份碼數量", example = "8")
  private Integer remainingBackupCodes;

  @Schema(description = "最後驗證時間", example = "2026-03-03T10:30:00Z")
  private OffsetDateTime lastVerifiedAt;

  @Schema(description = "是否由角色強制要求 MFA（無法禁用）", example = "false")
  private Boolean enforcedByRole;

  @Schema(description = "是否需要重新生成備份碼（剩餘數量≤2）", example = "false")
  private Boolean needRegenerateBackupCodes;

  @Schema(description = "QR碼是否已確認（首次設定時需掃描確認）", example = "true")
  private Boolean qrCodeConfirmed;
}
