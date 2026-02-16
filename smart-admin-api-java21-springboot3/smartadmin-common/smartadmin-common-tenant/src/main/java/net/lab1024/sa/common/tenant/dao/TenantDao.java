package net.lab1024.sa.common.tenant.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.common.tenant.domain.TenantEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Tenant data access.
 *
 * <p>Uses @InterceptorIgnore to bypass tenant filtering since this IS the tenant registry.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface TenantDao extends BaseMapper<TenantEntity> {

  @Select("SELECT * FROM t_tenant WHERE tenant_code = #{tenantCode}")
  TenantEntity getByCode(@Param("tenantCode") String tenantCode);
}
