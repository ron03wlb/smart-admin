package net.lab1024.sa.igaming.agent.affiliate.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.List;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateHierarchyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Affiliate hierarchy closure table DAO.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Mapper
public interface AffiliateHierarchyDao extends BaseMapper<AffiliateHierarchyEntity> {

  @Select(
      "SELECT * FROM t_affiliate_hierarchy"
          + " WHERE ancestor_id = #{ancestorId} AND tenant_id = #{tenantId}"
          + " ORDER BY depth")
  List<AffiliateHierarchyEntity> findDescendants(
      @Param("ancestorId") Long ancestorId, @Param("tenantId") Long tenantId);

  @Select(
      "SELECT * FROM t_affiliate_hierarchy"
          + " WHERE descendant_id = #{descendantId} AND tenant_id = #{tenantId}"
          + " ORDER BY depth DESC")
  List<AffiliateHierarchyEntity> findAncestors(
      @Param("descendantId") Long descendantId, @Param("tenantId") Long tenantId);

  @Select(
      "SELECT COALESCE(MAX(depth), 0) FROM t_affiliate_hierarchy"
          + " WHERE ancestor_id = #{ancestorId} AND tenant_id = #{tenantId}")
  Integer getMaxDepth(@Param("ancestorId") Long ancestorId, @Param("tenantId") Long tenantId);
}
