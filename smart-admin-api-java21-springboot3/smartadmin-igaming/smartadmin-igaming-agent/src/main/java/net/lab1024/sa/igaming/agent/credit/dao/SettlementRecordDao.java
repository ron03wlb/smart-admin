package net.lab1024.sa.igaming.agent.credit.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.agent.credit.domain.entity.SettlementRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Settlement record DAO.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface SettlementRecordDao extends BaseMapper<SettlementRecordEntity> {

  @Select(
      "SELECT * FROM t_settlement_record"
          + " WHERE agent_id = #{agentId} AND settlement_week = #{week} AND tenant_id = #{tenantId}")
  SettlementRecordEntity findByAgentAndWeek(
      @Param("agentId") Long agentId,
      @Param("week") String settlementWeek,
      @Param("tenantId") Long tenantId);

  @Select(
      "SELECT * FROM t_settlement_record"
          + " WHERE settlement_week = #{week} AND tenant_id = #{tenantId}"
          + " ORDER BY agent_id")
  List<SettlementRecordEntity> findByWeekAndTenantId(
      @Param("week") String settlementWeek, @Param("tenantId") Long tenantId);
}
