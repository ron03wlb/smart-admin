package net.lab1024.sa.system.mfa.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * MFA Recovery Request VO
 *
 * <p>View object for displaying recovery request details to Super Admin.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-04
 */
@Data
@Schema(description = "MFA 恢復請求查詢響應")
public class MfaRecoveryRequestVO {

  @Schema(description = "恢復請求 ID")
  private Long recoveryId;

  @Schema(description = "員工 ID")
  private Long employeeId;

  @Schema(description = "員工姓名")
  private String employeeName;

  @Schema(description = "部門 ID")
  private Long departmentId;

  @Schema(description = "請求郵箱")
  private String requestEmail;

  @Schema(description = "請求原因")
  private String requestReason;

  @Schema(description = "請求 IP 地址")
  private String requestIp;

  @Schema(description = "狀態: 1=PENDING, 2=APPROVED, 3=REJECTED, 4=EXPIRED")
  private Integer status;

  @Schema(description = "審批人 ID")
  private Long approverId;

  @Schema(description = "審批時間")
  private OffsetDateTime approvedAt;

  @Schema(description = "拒絕原因")
  private String rejectionReason;

  @Schema(description = "恢復碼過期時間")
  private OffsetDateTime expiresAt;

  @Schema(description = "創建時間")
  private OffsetDateTime createTime;
}
