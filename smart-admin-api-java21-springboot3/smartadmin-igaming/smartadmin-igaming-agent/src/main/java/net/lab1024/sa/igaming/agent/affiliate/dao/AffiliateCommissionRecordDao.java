package net.lab1024.sa.igaming.agent.affiliate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Affiliate commission settlement record DAO.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface AffiliateCommissionRecordDao extends BaseMapper<AffiliateCommissionRecordEntity> {

  @Select(
      "SELECT * FROM t_affiliate_commission_record"
          + " WHERE agent_id = #{agentId} AND settlement_date = #{date} AND tenant_id = #{tenantId}")
  AffiliateCommissionRecordEntity findByAgentAndDate(
      @Param("agentId") Long agentId,
      @Param("date") LocalDate settlementDate,
      @Param("tenantId") Long tenantId);

  @Select(
      "SELECT * FROM t_affiliate_commission_record"
          + " WHERE tenant_id = #{tenantId} AND status = 1"
          + " ORDER BY settlement_date DESC")
  List<AffiliateCommissionRecordEntity> findPendingByTenantId(@Param("tenantId") Long tenantId);
}
