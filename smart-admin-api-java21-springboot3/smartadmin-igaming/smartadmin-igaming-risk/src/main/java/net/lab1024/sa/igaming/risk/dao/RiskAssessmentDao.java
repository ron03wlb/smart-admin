package net.lab1024.sa.igaming.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Per-transaction risk assessment log DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface RiskAssessmentDao extends BaseMapper<RiskAssessmentEntity> {}
