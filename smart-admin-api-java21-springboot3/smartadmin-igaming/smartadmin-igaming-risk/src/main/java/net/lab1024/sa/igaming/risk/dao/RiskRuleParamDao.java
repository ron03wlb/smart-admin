package net.lab1024.sa.igaming.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.risk.domain.entity.RiskRuleParamEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Risk rule parameter configuration DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface RiskRuleParamDao extends BaseMapper<RiskRuleParamEntity> {

  /**
   * Find all enabled rules by type for a given tenant.
   *
   * @param tenantId tenant ID
   * @param ruleType rule type code
   * @return list of enabled rule parameters
   */
  @Select(
      "SELECT * FROM t_risk_rule_param WHERE tenant_id = #{tenantId} AND rule_type = #{ruleType}"
          + " AND enabled = TRUE AND deleted = FALSE")
  List<RiskRuleParamEntity> findEnabledByType(
      @Param("tenantId") Long tenantId, @Param("ruleType") Integer ruleType);

  /**
   * Find all enabled rules for a given tenant.
   *
   * @param tenantId tenant ID
   * @return list of enabled rule parameters
   */
  @Select(
      "SELECT * FROM t_risk_rule_param WHERE tenant_id = #{tenantId}"
          + " AND enabled = TRUE AND deleted = FALSE")
  List<RiskRuleParamEntity> findAllEnabled(@Param("tenantId") Long tenantId);
}
