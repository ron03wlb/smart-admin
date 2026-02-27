package net.lab1024.sa.igaming.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Player cumulative risk profile DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface RiskScoreDao extends BaseMapper<RiskScoreEntity> {

  /**
   * Find a player's risk score profile.
   *
   * @param playerId player ID
   * @param tenantId tenant ID
   * @return risk score entity or null
   */
  @Select("SELECT * FROM t_risk_score WHERE player_id = #{playerId} AND tenant_id = #{tenantId}")
  RiskScoreEntity findByPlayerIdAndTenantId(
      @Param("playerId") Long playerId, @Param("tenantId") Long tenantId);
}
