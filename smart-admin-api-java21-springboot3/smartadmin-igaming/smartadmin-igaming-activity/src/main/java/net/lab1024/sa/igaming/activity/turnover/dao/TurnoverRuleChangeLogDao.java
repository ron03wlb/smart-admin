package net.lab1024.sa.igaming.activity.turnover.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Turnover rule change log DAO (audit trail).
 *
 * <p>This DAO provides access to the audit trail of all turnover rule modifications. No custom
 * queries needed - all operations use standard MyBatis Plus methods.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Mapper
public interface TurnoverRuleChangeLogDao extends BaseMapper<TurnoverRuleChangeLogEntity> {
  // No custom methods - uses BaseMapper CRUD operations only
}
