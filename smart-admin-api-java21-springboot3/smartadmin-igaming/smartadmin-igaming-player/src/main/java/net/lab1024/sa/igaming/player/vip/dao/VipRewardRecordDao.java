package net.lab1024.sa.igaming.player.vip.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipRewardRecordEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP Reward Record DAO.
 *
 * <p>Data access object for VIP reward record table. Provides CRUD operations via MyBatis Plus
 * BaseMapper.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Mapper
public interface VipRewardRecordDao extends BaseMapper<VipRewardRecordEntity> {}
