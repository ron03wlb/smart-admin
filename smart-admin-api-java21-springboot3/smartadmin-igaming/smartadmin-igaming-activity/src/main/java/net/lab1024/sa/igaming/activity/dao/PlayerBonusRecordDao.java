package net.lab1024.sa.igaming.activity.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Player bonus record DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface PlayerBonusRecordDao extends BaseMapper<PlayerBonusRecordEntity> {

  List<PlayerBonusRecordVO> queryPage(
      Page<?> page, @Param("query") WageringProgressQueryForm queryForm);

  List<PlayerBonusRecordEntity> selectActiveByPlayerId(@Param("playerId") Long playerId);

  int countClaimsByPlayerAndRule(@Param("playerId") Long playerId, @Param("ruleId") Long ruleId);

  List<PlayerBonusRecordEntity> selectExpiredActive(@Param("batchSize") int batchSize);
}
