package net.lab1024.sa.igaming.game.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Game weight config entity — per-tenant game category weights for turnover calculation.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_game_weight_config")
public class GameWeightConfigEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long configId;

  /** Game category. See {@link net.lab1024.sa.igaming.common.constant.GameCategoryEnum}. */
  private Integer gameCategory;

  /** Weight factor 0.0000~1.0000. */
  private BigDecimal weight;

  private Boolean deleted;
}
