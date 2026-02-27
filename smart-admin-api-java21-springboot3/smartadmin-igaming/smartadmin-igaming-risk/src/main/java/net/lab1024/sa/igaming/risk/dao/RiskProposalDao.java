package net.lab1024.sa.igaming.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * Risk review proposal DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface RiskProposalDao extends BaseMapper<RiskProposalEntity> {

  /**
   * Find proposals that are overdue (past SLA deadline and still pending/assigned).
   *
   * @return list of overdue proposals
   */
  @Select(
      "SELECT * FROM t_risk_proposal WHERE status IN (1, 2)"
          + " AND sla_deadline IS NOT NULL AND sla_deadline < NOW()")
  List<RiskProposalEntity> findOverduePending();
}
