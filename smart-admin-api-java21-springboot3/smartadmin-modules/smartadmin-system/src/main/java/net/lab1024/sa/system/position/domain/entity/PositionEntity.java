package net.lab1024.sa.system.position.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 职务表 实体类
 *
 * @author kaiyun
 * @since 2024-06-23 23:31:38 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_position")
public class PositionEntity extends SmartAdminBaseEntity {

  /** 职务ID */
  @TableId(type = IdType.AUTO)
  private Long positionId;

  /** 职务名称 */
  private String positionName;

  /** 职级 */
  private String positionLevel;

  /** 排序 */
  private Integer sort;

  /** 备注 */
  private String remark;

  private Boolean deletedFlag;
}
