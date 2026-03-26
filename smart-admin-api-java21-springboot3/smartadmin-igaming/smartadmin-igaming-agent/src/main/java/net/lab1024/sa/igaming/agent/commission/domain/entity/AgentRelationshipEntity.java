package net.lab1024.sa.igaming.agent.commission.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Agent relationship entity - Multi-level agent hierarchy (up to 5 levels).
 *
 * <p>Represents the relationship between a player and their agent. Supports up to 5 levels of agent
 * hierarchy for commission calculation.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_agent_relationship")
public class AgentRelationshipEntity extends SmartAdminBaseEntity {

  /** Relationship ID (auto-generated). */
  @TableId(type = IdType.AUTO)
  private Long relationshipId;

  /** Player ID (FK → t_player). */
  private Long playerId;

  /** Agent ID (FK → t_player). */
  private Long agentId;

  /** Agent level (1-5). Level 1 = direct agent, Level 5 = highest parent. */
  private Integer level;

  /** Binding timestamp. */
  private OffsetDateTime bindTime;

  /**
   * Relationship status.
   *
   * <p>Possible values:
   *
   * <ul>
   *   <li>ACTIVE - Active relationship
   *   <li>SUSPENDED - Temporarily suspended
   *   <li>TERMINATED - Permanently terminated
   * </ul>
   */
  private String status;

  /** Logical delete flag (MyBatis Plus). */
  @TableLogic(value = "false", delval = "true")
  private Boolean deleted;
}
