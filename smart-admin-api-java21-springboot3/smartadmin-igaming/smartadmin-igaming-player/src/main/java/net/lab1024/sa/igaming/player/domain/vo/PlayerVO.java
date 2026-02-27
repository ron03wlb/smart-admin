package net.lab1024.sa.igaming.player.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;

/**
 * Player view object for API responses.
 *
 * <p>PII fields (email, phone) are masked for display.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
public class PlayerVO {

  @Schema(description = "Player ID")
  private Long playerId;

  @Schema(description = "Username")
  private String username;

  @Schema(description = "Email (masked)")
  private String email;

  @Schema(description = "Phone (masked)")
  private String phone;

  @SchemaEnum(PlayerStatusEnum.class)
  private Integer status;

  @SchemaEnum(KycLevelEnum.class)
  private Integer kycLevel;

  @SchemaEnum(VipLevelEnum.class)
  private Integer vipLevel;

  @Schema(description = "Registration IP")
  private String registrationIp;

  @Schema(description = "Last login time")
  private OffsetDateTime lastLoginTime;

  @Schema(description = "Created time")
  private OffsetDateTime createTime;

  @Schema(description = "Updated time")
  private OffsetDateTime updateTime;
}
