package net.lab1024.sa.base.module.support.liteflow.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.base.module.support.liteflow.domain.entity.LiteFlowExecutionMetricsEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * LiteFlow 執行指標 Dao
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Mapper
public interface LiteFlowExecutionMetricsDao extends BaseMapper<LiteFlowExecutionMetricsEntity> {
  // MyBatis-Plus 提供基礎 CRUD
  // 額外方法可在此聲明
}
