package net.lab1024.sa.igaming.agent.credit.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.agent.credit.domain.entity.CreditAllocationAuditEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Credit allocation audit trail DAO (immutable, INSERT only).
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface CreditAllocationAuditDao extends BaseMapper<CreditAllocationAuditEntity> {}
