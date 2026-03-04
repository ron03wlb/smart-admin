package net.lab1024.sa.system.mfa.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * MFA Configuration Entity
 *
 * <p>Stores MFA configuration per employee including encrypted TOTP secret, QR code confirmation
 * status, and role-based enforcement settings.
 *
 * <p>Security: TOTP secret is encrypted using AES-256-GCM before storage.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_mfa_config")
public class MfaConfigEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long mfaConfigId;

  /** Employee ID (foreign key to t_employee) */
  private Long employeeId;

  /** MFA enabled flag */
  private Boolean mfaEnabled;

  /** MFA type: TOTP, SMS, EMAIL */
  private String mfaType;

  /** Encrypted TOTP secret (AES-256-GCM encrypted) */
  private String secretEncrypted;

  /** Backup codes generated flag */
  private Boolean backupCodesGenerated;

  /** QR code confirmed flag (user has scanned QR code) */
  private Boolean qrCodeConfirmed;

  /** Enforced by role flag (MFA mandatory for high-privilege roles) */
  private Boolean enforcedByRole;

  /** Last verified timestamp (when user last successfully passed MFA check) */
  private OffsetDateTime lastVerifiedAt;

  /** Soft delete flag */
  private Boolean deleted;

  /** Optimistic lock version */
  @Version private Integer version;
}
