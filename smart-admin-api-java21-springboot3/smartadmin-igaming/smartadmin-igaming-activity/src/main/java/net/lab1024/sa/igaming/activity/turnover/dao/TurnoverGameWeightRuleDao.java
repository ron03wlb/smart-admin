package net.lab1024.sa.igaming.activity.turnover.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverGameWeightRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverGameWeightRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Turnover game weight rule DAO.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Mapper
public interface TurnoverGameWeightRuleDao extends BaseMapper<TurnoverGameWeightRuleEntity> {

  /**
   * Query game weight rules with pagination.
   *
   * @param page MyBatis-Plus page object
   * @param queryForm query form with filter conditions
   * @return list of game weight rule VOs
   */
  List<TurnoverGameWeightRuleVO> queryPage(
      Page<?> page, @Param("query") TurnoverGameWeightRuleQueryForm queryForm);

  /**
   * Select weight percentage for a specific game category and tenant.
   *
   * @param tenantId tenant ID (from RLS context)
   * @param gameCategory game category (1-6)
   * @return weight percentage (0.00 ~ 100.00), or null if no active rule found
   */
  @Select(
      "SELECT weight_percentage FROM t_turnover_game_weight_rule "
          + "WHERE tenant_id = #{tenantId} AND game_category = #{gameCategory} "
          + "AND deleted = FALSE AND status = 1 LIMIT 1")
  BigDecimal selectGameWeight(
      @Param("tenantId") Long tenantId, @Param("gameCategory") Integer gameCategory);
}
