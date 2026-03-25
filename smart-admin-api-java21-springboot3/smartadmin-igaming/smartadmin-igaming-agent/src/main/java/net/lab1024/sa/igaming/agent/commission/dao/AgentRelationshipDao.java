package net.lab1024.sa.igaming.agent.commission.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentRelationshipEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent relationship DAO - Multi-level agent hierarchy repository.
 *
 * <p>Provides database access for agent-player relationships, supporting up to 5 levels of agent
 * hierarchy for commission calculation.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Mapper
public interface AgentRelationshipDao extends BaseMapper<AgentRelationshipEntity> {}
