package net.lab1024.sa.igaming.player.selfexclusion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Self-Exclusion History DAO.
 *
 * <p>Data access object for t_self_exclusion_history table.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Mapper
public interface SelfExclusionHistoryDao extends BaseMapper<SelfExclusionHistoryEntity> {}
