package net.lab1024.sa.igaming.risk.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import net.lab1024.sa.igaming.risk.domain.entity.GeoRestrictionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Restricted jurisdiction DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface GeoRestrictionDao extends BaseMapper<GeoRestrictionEntity> {

  /**
   * Check whether a country is restricted for a given tenant.
   *
   * @param countryCode ISO 3166-1 country code
   * @param tenantId tenant ID
   * @return true if restricted
   */
  @Select(
      "SELECT EXISTS(SELECT 1 FROM t_geo_restriction"
          + " WHERE country_code = #{countryCode} AND tenant_id = #{tenantId} AND enabled = TRUE)")
  boolean isRestrictedCountry(
      @Param("countryCode") String countryCode, @Param("tenantId") Long tenantId);
}
