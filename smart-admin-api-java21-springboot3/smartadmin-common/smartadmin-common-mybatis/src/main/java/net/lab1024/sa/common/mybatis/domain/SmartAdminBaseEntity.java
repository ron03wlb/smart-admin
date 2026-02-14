package net.lab1024.sa.common.mybatis.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import java.time.OffsetDateTime;
import lombok.Data;

/**
 * SmartAdmin Base Entity
 *
 * <p>Abstract base class for all entities. Provides common audit fields (createTime, updateTime)
 * with automatic fill support via MyBatis-Plus MetaObjectHandler.
 *
 * @author 1024创新实验室
 * @since 2026-02-14
 */
@Data
public abstract class SmartAdminBaseEntity {

  @TableField(fill = FieldFill.INSERT)
  private OffsetDateTime createTime;

  @TableField(fill = FieldFill.INSERT_UPDATE)
  private OffsetDateTime updateTime;
}
