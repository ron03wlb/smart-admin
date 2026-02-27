package net.lab1024.sa.igaming.game.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.game.domain.entity.GameWeightConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Game weight config DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface GameWeightConfigDao extends BaseMapper<GameWeightConfigEntity> {

  BigDecimal selectWeight(
      @Param("tenantId") Long tenantId, @Param("gameCategory") Integer gameCategory);
}
