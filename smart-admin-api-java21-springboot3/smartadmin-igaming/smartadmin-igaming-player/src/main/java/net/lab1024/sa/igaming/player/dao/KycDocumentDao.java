package net.lab1024.sa.igaming.player.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.player.domain.entity.KycDocumentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * KYC document data access object.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface KycDocumentDao extends BaseMapper<KycDocumentEntity> {}
