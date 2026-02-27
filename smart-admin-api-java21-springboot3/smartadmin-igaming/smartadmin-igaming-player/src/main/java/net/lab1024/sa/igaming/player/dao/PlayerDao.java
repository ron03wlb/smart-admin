package net.lab1024.sa.igaming.player.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.domain.form.PlayerQueryForm;
import net.lab1024.sa.igaming.player.domain.vo.PlayerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Player data access object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface PlayerDao extends BaseMapper<PlayerEntity> {

  /**
   * Paginated player query.
   *
   * @param page pagination parameter
   * @param query query conditions
   * @return player VO list
   */
  List<PlayerVO> queryPage(Page<?> page, @Param("query") PlayerQueryForm query);
}
