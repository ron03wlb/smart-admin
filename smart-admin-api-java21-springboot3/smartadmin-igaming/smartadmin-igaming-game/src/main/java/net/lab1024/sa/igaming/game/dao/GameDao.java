package net.lab1024.sa.igaming.game.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.game.domain.entity.GameEntity;
import net.lab1024.sa.igaming.game.domain.form.GameQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Game catalog DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface GameDao extends BaseMapper<GameEntity> {

  List<GameVO> queryPage(Page<?> page, @Param("query") GameQueryForm query);

  List<GameVO> selectEnabledGamesByTenant(@Param("tenantId") Long tenantId);

  List<GameVO> selectGamesByTenantAndCategory(
      @Param("tenantId") Long tenantId, @Param("category") Integer category);
}
