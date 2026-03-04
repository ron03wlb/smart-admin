package net.lab1024.sa.system.mfa.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * MFA Backup Code Entity
 *
 * <p>Stores MFA backup codes for account recovery. Each employee has 10 backup codes (8-digit
 * numbers), each usable only once.
 *
 * <p>Security: Backup codes are hashed using Argon2id before storage (similar to password hashing).
 *
 * @author SmartAdmin MFA Team
 * @since 2026-03-03
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_mfa_backup_code")
public class MfaBackupCodeEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long backupCodeId;

  /** Employee ID (foreign key to t_employee) */
  private Long employeeId;

  /** Backup code hash (Argon2id hashed, similar to password) */
  private String codeHash;

  /** Used flag (each backup code can only be used once) */
  private Boolean used;

  /** Used timestamp */
  private OffsetDateTime usedAt;

  /** IP address when backup code was used */
  private String usedIp;

  /** Soft delete flag */
  private Boolean deleted;
}
