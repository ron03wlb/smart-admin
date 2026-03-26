package net.lab1024.sa.igaming.player.vip.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.player.vip.domain.entity.PlayerVipHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Player VIP History DAO.
 *
 * <p>Data access object for player VIP history table. Provides CRUD operations via MyBatis Plus
 * BaseMapper.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Mapper
public interface PlayerVipHistoryDao extends BaseMapper<PlayerVipHistoryEntity> {}
