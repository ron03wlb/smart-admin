package net.lab1024.sa.business.brand.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Brand Entity
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_brand")
public class BrandEntity extends SmartAdminBaseEntity {

  /** Brand ID */
  @TableId(type = IdType.AUTO)
  private Long brandId;

  /** Brand name */
  private String brandName;

  /** Brand logo URL */
  private String brandLogo;

  /** Description */
  private String description;

  /** Display sort order */
  private Integer sort;

  /** Status (1=Enabled, 0=Disabled) */
  private Integer status;

  /** Soft delete flag */
  private Boolean deletedFlag;
}
