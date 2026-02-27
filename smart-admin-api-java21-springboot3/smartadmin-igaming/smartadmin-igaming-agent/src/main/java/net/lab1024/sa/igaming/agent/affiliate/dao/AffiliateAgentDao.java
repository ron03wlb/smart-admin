package net.lab1024.sa.igaming.agent.affiliate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAgentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Affiliate agent master data DAO.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface AffiliateAgentDao extends BaseMapper<AffiliateAgentEntity> {

  @Select(
      "SELECT * FROM t_affiliate_agent"
          + " WHERE username = #{username} AND tenant_id = #{tenantId} AND deleted = FALSE")
  AffiliateAgentEntity findByUsernameAndTenantId(
      @Param("username") String username, @Param("tenantId") Long tenantId);

  @Select(
      "SELECT * FROM t_affiliate_agent"
          + " WHERE parent_agent_id = #{parentAgentId} AND tenant_id = #{tenantId} AND deleted = FALSE")
  List<AffiliateAgentEntity> findByParentAgentIdAndTenantId(
      @Param("parentAgentId") Long parentAgentId, @Param("tenantId") Long tenantId);
}
