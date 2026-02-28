package net.lab1024.sa.igaming.agent.credit.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.agent.credit.domain.entity.AgentCreditEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Agent credit limit DAO.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface AgentCreditDao extends BaseMapper<AgentCreditEntity> {

  @Select(
      "SELECT * FROM t_agent_credit WHERE agent_id = #{agentId} AND tenant_id = #{tenantId} AND deleted = FALSE")
  AgentCreditEntity findByAgentIdAndTenantId(
      @Param("agentId") Long agentId, @Param("tenantId") Long tenantId);

  @Select(
      "<script>"
          + "SELECT * FROM t_agent_credit WHERE "
          + "<if test='parentId != null'>parent_id = #{parentId}</if>"
          + "<if test='parentId == null'>parent_id IS NULL</if>"
          + " AND tenant_id = #{tenantId} AND deleted = FALSE"
          + "</script>")
  List<AgentCreditEntity> findByParentIdAndTenantId(
      @Param("parentId") Long parentId, @Param("tenantId") Long tenantId);
}
