package net.lab1024.sa.igaming.agent.commission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.commission.dao.AgentRelationshipDao;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentRelationshipEntity;
import org.springframework.stereotype.Service;

/**
 * Agent Relationship Service - Manages agent hierarchy and player-agent bindings.
 *
 * <p>Handles multi-level agent tree structure (up to 5 levels), provides methods for binding
 * players to agents, querying agent chains, and managing relationship status.
 *
 * <p>Uses PostgreSQL Recursive CTE for efficient agent tree traversal.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRelationshipService {

  private final AgentRelationshipDao agentRelationshipDao;

  /**
   * Bind a player to an agent.
   *
   * <p>Validates agent hierarchy constraints: - Maximum 5 levels - Agent ID must be valid - Player
   * cannot already have an active agent
   *
   * @param playerId player ID to bind
   * @param agentId agent ID to bind to
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> bindPlayerToAgent(Long playerId, Long agentId) {
    // Check if player already has an active agent binding
    Option<AgentRelationshipEntity> existingBinding = getActiveBinding(playerId);
    if (existingBinding.isDefined()) {
      return ResponseDTO.userErrorParam("玩家已綁定代理，無法重複綁定");
    }

    // Check agent level (maximum 5 levels)
    int agentLevel = calculateAgentLevel(agentId);
    if (agentLevel >= 5) {
      return ResponseDTO.userErrorParam("代理層級已達上限（最多 5 層），無法綁定");
    }

    // Create new binding
    AgentRelationshipEntity relationship =
        AgentRelationshipEntity.builder()
            .playerId(playerId)
            .agentId(agentId)
            .level(agentLevel + 1)
            .bindTime(OffsetDateTime.now(ZoneId.systemDefault()))
            .status("ACTIVE")
            .build();

    agentRelationshipDao.insert(relationship);
    log.info(
        "Successfully bound player {} to agent {} at level {}",
        playerId,
        agentId,
        relationship.getLevel());

    return ResponseDTO.ok();
  }

  /**
   * Get active agent binding for a player.
   *
   * @param playerId player ID
   * @return Option containing active binding if found, None otherwise
   */
  public Option<AgentRelationshipEntity> getActiveBinding(Long playerId) {
    LambdaQueryWrapper<AgentRelationshipEntity> wrapper =
        new LambdaQueryWrapper<AgentRelationshipEntity>()
            .eq(AgentRelationshipEntity::getPlayerId, playerId)
            .eq(AgentRelationshipEntity::getStatus, "ACTIVE");

    return Option.of(agentRelationshipDao.selectOne(wrapper));
  }

  /**
   * Calculate the current level of an agent in the hierarchy.
   *
   * <p>Level 1 = direct agent (no upstream) Level 2-5 = downstream agents
   *
   * @param agentId agent ID
   * @return agent level (1-5)
   */
  public int calculateAgentLevel(Long agentId) {
    Option<AgentRelationshipEntity> agentBinding = getActiveBinding(agentId);
    return agentBinding.map(AgentRelationshipEntity::getLevel).getOrElse(0);
  }

  /**
   * Suspend agent relationship.
   *
   * <p>Used for temporary suspension (e.g., investigation, compliance review).
   *
   * @param relationshipId relationship ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> suspendRelationship(Long relationshipId) {
    AgentRelationshipEntity relationship = agentRelationshipDao.selectById(relationshipId);
    if (relationship == null) {
      return ResponseDTO.userErrorParam("代理關係不存在");
    }

    relationship.setStatus("SUSPENDED");
    agentRelationshipDao.updateById(relationship);

    log.info("Suspended agent relationship {}", relationshipId);
    return ResponseDTO.ok();
  }

  /**
   * Terminate agent relationship.
   *
   * <p>Permanent termination (cannot be reactivated).
   *
   * @param relationshipId relationship ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> terminateRelationship(Long relationshipId) {
    AgentRelationshipEntity relationship = agentRelationshipDao.selectById(relationshipId);
    if (relationship == null) {
      return ResponseDTO.userErrorParam("代理關係不存在");
    }

    relationship.setStatus("TERMINATED");
    agentRelationshipDao.updateById(relationship);

    log.info("Terminated agent relationship {}", relationshipId);
    return ResponseDTO.ok();
  }

  /**
   * Reactivate a suspended agent relationship.
   *
   * @param relationshipId relationship ID
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> reactivateRelationship(Long relationshipId) {
    AgentRelationshipEntity relationship = agentRelationshipDao.selectById(relationshipId);
    if (relationship == null) {
      return ResponseDTO.userErrorParam("代理關係不存在");
    }

    if (!"SUSPENDED".equals(relationship.getStatus())) {
      return ResponseDTO.userErrorParam("只能重新激活 SUSPENDED 狀態的代理關係");
    }

    relationship.setStatus("ACTIVE");
    agentRelationshipDao.updateById(relationship);

    log.info("Reactivated agent relationship {}", relationshipId);
    return ResponseDTO.ok();
  }
}
