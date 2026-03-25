package net.lab1024.sa.igaming.agent.commission.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionSettlementEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent commission settlement batch DAO - Settlement batch repository.
 *
 * <p>Provides database access for commission settlement batches, preventing duplicate settlements
 * through unique constraints and distributed locking.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Mapper
public interface AgentCommissionSettlementDao extends BaseMapper<AgentCommissionSettlementEntity> {}
