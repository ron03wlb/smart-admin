package net.lab1024.sa.igaming.agent.affiliate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAdjustmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Affiliate adjustment ledger DAO (immutable, INSERT only).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface AffiliateAdjustmentDao extends BaseMapper<AffiliateAdjustmentEntity> {}
