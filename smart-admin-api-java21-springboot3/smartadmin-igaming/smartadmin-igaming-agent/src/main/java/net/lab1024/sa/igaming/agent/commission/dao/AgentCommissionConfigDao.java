package net.lab1024.sa.igaming.agent.commission.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent commission config DAO - Commission rate configuration repository.
 *
 * <p>Provides database access for commission rate configurations by agent level and product type.
 * Supports effective date ranges for historical rate tracking.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Mapper
public interface AgentCommissionConfigDao extends BaseMapper<AgentCommissionConfigEntity> {}
