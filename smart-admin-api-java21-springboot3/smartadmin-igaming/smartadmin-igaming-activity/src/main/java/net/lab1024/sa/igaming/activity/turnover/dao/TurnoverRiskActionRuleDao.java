package net.lab1024.sa.igaming.activity.turnover.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverRiskActionRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Turnover risk action rule DAO.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Mapper
public interface TurnoverRiskActionRuleDao extends BaseMapper<TurnoverRiskActionRuleEntity> {

  /**
   * Query risk action rules with pagination.
   *
   * @param page MyBatis-Plus page object
   * @param queryForm query form with filter conditions
   * @return list of risk action rule VOs
   */
  List<TurnoverRiskActionRuleVO> queryPage(
      Page<?> page, @Param("query") TurnoverRiskActionRuleQueryForm queryForm);

  /**
   * Select risk action rule for a specific risk level and tenant.
   *
   * @param tenantId tenant ID (from RLS context)
   * @param riskLevel risk level (1-4)
   * @return risk action rule entity, or null if no active rule found
   */
  @Select(
      "SELECT * FROM t_turnover_risk_action_rule "
          + "WHERE tenant_id = #{tenantId} AND risk_level = #{riskLevel} "
          + "AND deleted = FALSE AND status = 1 LIMIT 1")
  TurnoverRiskActionRuleEntity selectByRiskLevel(
      @Param("tenantId") Long tenantId, @Param("riskLevel") Integer riskLevel);
}
