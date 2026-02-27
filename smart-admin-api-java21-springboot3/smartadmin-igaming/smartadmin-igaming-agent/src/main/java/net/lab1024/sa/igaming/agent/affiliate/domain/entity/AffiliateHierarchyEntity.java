package net.lab1024.sa.igaming.agent.affiliate.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * Affiliate hierarchy closure table entity.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_affiliate_hierarchy")
public class AffiliateHierarchyEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long hierarchyId;

  private Long ancestorId;

  private Long descendantId;

  /** Depth (0 = self). */
  private Integer depth;
}
