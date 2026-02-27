package net.lab1024.sa.igaming.player.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * VIP change log entity — records VIP level transitions.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_vip_change_log")
public class VipChangeLogEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long logId;

  private Long playerId;

  private Integer oldLevel;

  private Integer newLevel;

  private String reason;
}
