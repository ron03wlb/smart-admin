package net.lab1024.sa.igaming.agent.commission.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentPerformanceSnapshotEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent performance snapshot DAO - Weekly/monthly performance metrics repository.
 *
 * <p>Provides database access for agent performance snapshots, used for rankings, reports, and
 * dashboard analytics.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Mapper
public interface AgentPerformanceSnapshotDao extends BaseMapper<AgentPerformanceSnapshotEntity> {}
