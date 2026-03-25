package net.lab1024.sa.igaming.agent.commission.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.commission.domain.entity.AgentCommissionRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Agent commission record DAO - Commission calculation result repository.
 *
 * <p>Provides database access for commission calculation records, including bet/payout amounts,
 * commission rates, and final commission amounts for each agent.
 *
 * @author iGaming Team
 * @since 2026-03-25
 */
@Mapper
public interface AgentCommissionRecordDao extends BaseMapper<AgentCommissionRecordEntity> {}
