package net.lab1024.sa.igaming.game.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.game.domain.entity.GameProviderEntity;
import net.lab1024.sa.igaming.game.domain.form.GameProviderQueryForm;
import net.lab1024.sa.igaming.game.domain.vo.GameProviderVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Game provider DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface GameProviderDao extends BaseMapper<GameProviderEntity> {

  List<GameProviderVO> queryPage(Page<?> page, @Param("query") GameProviderQueryForm query);

  List<GameProviderEntity> selectEnabledProviders();
}
