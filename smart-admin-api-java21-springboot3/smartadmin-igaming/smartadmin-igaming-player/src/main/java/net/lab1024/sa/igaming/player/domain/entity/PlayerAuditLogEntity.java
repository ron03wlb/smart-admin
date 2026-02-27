package net.lab1024.sa.igaming.player.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Player audit log entity — records player status transitions.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_player_audit_log")
public class PlayerAuditLogEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long auditId;

  private Long playerId;

  private Integer oldStatus;

  private Integer newStatus;

  private String operator;

  private String reason;
}
