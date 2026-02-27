package net.lab1024.sa.igaming.game.domain.entity;

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
 * Game provider entity — GP configuration table.
 *
 * <p>API key is AES-256-GCM encrypted via {@link EncryptedFieldTypeHandler}.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_game_provider", autoResultMap = true)
public class GameProviderEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long providerId;

  private String providerCode;

  private String providerName;

  private String apiUrl;

  /** API key (AES-256-GCM encrypted in DB). */
  @TableField(typeHandler = EncryptedFieldTypeHandler.class)
  private String encryptedApiKey;

  private String callbackUrl;

  private String supportedGames;

  private Boolean enabled;

  /** Health status. See {@link net.lab1024.sa.igaming.common.constant.HealthStatusEnum}. */
  private Integer healthStatus;

  private OffsetDateTime lastHealthCheck;

  private Boolean deleted;

  @Version private Integer version;
}
