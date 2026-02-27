package net.lab1024.sa.igaming.agent.affiliate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionPlanEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Affiliate commission plan configuration DAO.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface AffiliateCommissionPlanDao extends BaseMapper<AffiliateCommissionPlanEntity> {

  @Select(
      "SELECT * FROM t_affiliate_commission_plan"
          + " WHERE plan_type = #{planType} AND tenant_id = #{tenantId} AND deleted = FALSE")
  List<AffiliateCommissionPlanEntity> findByPlanTypeAndTenantId(
      @Param("planType") Integer planType, @Param("tenantId") Long tenantId);
}
