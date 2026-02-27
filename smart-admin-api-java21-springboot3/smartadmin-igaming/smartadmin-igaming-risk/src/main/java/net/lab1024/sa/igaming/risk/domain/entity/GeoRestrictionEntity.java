package net.lab1024.sa.igaming.risk.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Restricted jurisdiction entity.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_geo_restriction")
public class GeoRestrictionEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long geoRestrictionId;

  /** ISO 3166-1 country code. */
  private String countryCode;

  private String countryName;

  /** Restriction type: 1=FULL_BAN, 2=VIEW_ONLY. */
  private Integer restrictionType;

  private String reason;

  private Boolean enabled;
}
