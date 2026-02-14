package net.lab1024.sa.support.liteflow.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowScriptEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * LiteFlow 腳本節點定義 Dao
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@Mapper
public interface LiteFlowScriptDao extends BaseMapper<LiteFlowScriptEntity> {
  // MyBatis-Plus 提供基礎 CRUD
  // 額外方法可在此聲明
}
