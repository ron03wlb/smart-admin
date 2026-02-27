package net.lab1024.sa.igaming.player.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.common.security.encrypt.EncryptedFieldTypeHandler;

/**
 * Player entity — master player table with PII encryption.
 *
 * <p>Email and phone fields use AES-256-GCM transparent encryption via {@link
 * EncryptedFieldTypeHandler}. Blind index fields (HMAC-SHA256) enable equality lookups on encrypted
 * PII without decryption.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_player", autoResultMap = true)
public class PlayerEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long playerId;

  private String username;

  private String passwordHash;

  /** Email (AES-256-GCM encrypted in DB). */
  @TableField(typeHandler = EncryptedFieldTypeHandler.class)
  private String emailEncrypted;

  /** Email blind index (HMAC-SHA256) for equality lookup. */
  private String emailBlindIdx;

  /** Phone (AES-256-GCM encrypted in DB). */
  @TableField(typeHandler = EncryptedFieldTypeHandler.class)
  private String phoneEncrypted;

  /** Phone blind index (HMAC-SHA256) for equality lookup. */
  private String phoneBlindIdx;

  /** Player status. See {@link net.lab1024.sa.igaming.common.constant.PlayerStatusEnum}. */
  private Integer status;

  /** KYC verification level. See {@link net.lab1024.sa.igaming.common.constant.KycLevelEnum}. */
  private Integer kycLevel;

  /** VIP level. See {@link net.lab1024.sa.igaming.common.constant.VipLevelEnum}. */
  private Integer vipLevel;

  private String registrationIp;

  private String lastLoginIp;

  private OffsetDateTime lastLoginTime;

  private Boolean deleted;

  /** Optimistic lock version. */
  @Version private Integer version;
}
