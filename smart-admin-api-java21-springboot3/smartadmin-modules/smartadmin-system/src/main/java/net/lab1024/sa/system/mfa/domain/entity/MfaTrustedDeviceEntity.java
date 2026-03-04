package net.lab1024.sa.system.mfa.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * MFA Trusted Device Entity
 *
 * <p>Stores trusted devices that can bypass MFA verification for 30 days. Device fingerprint is
 * generated from IP + User-Agent + Device UUID and hashed with SHA256.
 *
 * <p>Security: Device fingerprint is one-way hashed to prevent reverse engineering.
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_mfa_trusted_device")
public class MfaTrustedDeviceEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long deviceId;

  /** Employee ID (foreign key to t_employee) */
  private Long employeeId;

  /** Device fingerprint (SHA256 hash of IP + User-Agent + UUID) */
  private String deviceFingerprint;

  /** Device name (user-provided, e.g., "My iPhone 15") */
  private String deviceName;

  /** IP address when device was trusted */
  private String ipAddress;

  /** User agent string */
  private String userAgent;

  /** Trust expiry timestamp (30 days from creation) */
  private OffsetDateTime trustedUntil;

  /** Soft delete flag */
  private Boolean deleted;
}
