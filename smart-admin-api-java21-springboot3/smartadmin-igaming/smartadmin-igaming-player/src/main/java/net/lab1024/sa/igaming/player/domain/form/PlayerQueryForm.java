package net.lab1024.sa.igaming.player.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.common.constant.VipLevelEnum;

/**
 * Player paginated query form.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerQueryForm extends PageParam {

  @Schema(description = "Username (fuzzy match)")
  private String username;

  @SchemaEnum(PlayerStatusEnum.class)
  @CheckEnum(message = "status invalid", value = PlayerStatusEnum.class, required = false)
  private Integer status;

  @SchemaEnum(KycLevelEnum.class)
  @CheckEnum(message = "kycLevel invalid", value = KycLevelEnum.class, required = false)
  private Integer kycLevel;

  @SchemaEnum(VipLevelEnum.class)
  @CheckEnum(message = "vipLevel invalid", value = VipLevelEnum.class, required = false)
  private Integer vipLevel;
}
