package net.lab1024.sa.igaming.activity.turnover.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverOddsThresholdRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverOddsThresholdRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Turnover odds threshold rule DAO.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Mapper
public interface TurnoverOddsThresholdRuleDao extends BaseMapper<TurnoverOddsThresholdRuleEntity> {

  /**
   * Query odds threshold rules with pagination.
   *
   * @param page MyBatis-Plus page object
   * @param queryForm query form with filter conditions
   * @return list of odds threshold rule VOs
   */
  List<TurnoverOddsThresholdRuleVO> queryPage(
      Page<?> page, @Param("query") TurnoverOddsThresholdRuleQueryForm queryForm);

  /**
   * Verify if odds meet threshold requirements.
   *
   * @param tenantId tenant ID (from RLS context)
   * @param oddsType odds type (1-4)
   * @param oddsValue actual odds value to check
   * @return true if odds meet threshold, false otherwise
   */
  @Select(
      "SELECT COUNT(*) > 0 FROM t_turnover_odds_threshold_rule "
          + "WHERE tenant_id = #{tenantId} AND odds_type = #{oddsType} "
          + "AND #{oddsValue} >= min_odds_threshold "
          + "AND deleted = FALSE AND status = 1")
  boolean verifyOddsThreshold(
      @Param("tenantId") Long tenantId,
      @Param("oddsType") Integer oddsType,
      @Param("oddsValue") BigDecimal oddsValue);
}
