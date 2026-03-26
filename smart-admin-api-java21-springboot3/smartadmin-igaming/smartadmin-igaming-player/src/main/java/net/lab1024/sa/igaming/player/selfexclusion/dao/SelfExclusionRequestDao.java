package net.lab1024.sa.igaming.player.selfexclusion.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionRequestEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Self-Exclusion Request DAO.
 *
 * <p>Data access object for t_self_exclusion_request table.
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Mapper
public interface SelfExclusionRequestDao extends BaseMapper<SelfExclusionRequestEntity> {}
