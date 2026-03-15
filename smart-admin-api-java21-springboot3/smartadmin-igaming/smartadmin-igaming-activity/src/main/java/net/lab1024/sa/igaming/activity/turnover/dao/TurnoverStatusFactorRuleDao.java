package net.lab1024.sa.igaming.activity.turnover.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverStatusFactorRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverStatusFactorRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Turnover status factor rule DAO.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Mapper
public interface TurnoverStatusFactorRuleDao extends BaseMapper<TurnoverStatusFactorRuleEntity> {

  /**
   * Query status factor rules with pagination.
   *
   * @param page MyBatis-Plus page object
   * @param queryForm query form with filter conditions
   * @return list of status factor rule VOs
   */
  List<TurnoverStatusFactorRuleVO> queryPage(
      Page<?> page, @Param("query") TurnoverStatusFactorRuleQueryForm queryForm);

  /**
   * Select factor percentage for a specific settlement status and tenant.
   *
   * @param tenantId tenant ID (from RLS context)
   * @param settlementStatus settlement status (1-9)
   * @return factor percentage (0.00 ~ 100.00), or null if no active rule found
   */
  @Select(
      "SELECT factor_percentage FROM t_turnover_status_factor_rule "
          + "WHERE tenant_id = #{tenantId} AND settlement_status = #{settlementStatus} "
          + "AND deleted = FALSE AND status = 1 LIMIT 1")
  BigDecimal selectStatusFactor(
      @Param("tenantId") Long tenantId, @Param("settlementStatus") Integer settlementStatus);
}
