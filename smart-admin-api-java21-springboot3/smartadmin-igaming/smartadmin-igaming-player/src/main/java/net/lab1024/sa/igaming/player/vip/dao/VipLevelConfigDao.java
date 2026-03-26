package net.lab1024.sa.igaming.player.vip.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.player.vip.domain.entity.VipLevelConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * VIP Level Configuration DAO.
 *
 * <p>Data access object for VIP level configuration table. Provides CRUD operations via MyBatis
 * Plus BaseMapper.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Mapper
public interface VipLevelConfigDao extends BaseMapper<VipLevelConfigEntity> {}
