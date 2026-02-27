package net.lab1024.sa.igaming.game.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Game entity — game catalog table.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_game")
public class GameEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long gameId;

  private Long providerId;

  private String gameCode;

  private String gameName;

  /** Game category. See {@link net.lab1024.sa.igaming.common.constant.GameCategoryEnum}. */
  private Integer category;

  private String thumbnailUrl;

  private Long playCount;

  private Boolean enabled;

  private Boolean deleted;

  @Version private Integer version;
}
