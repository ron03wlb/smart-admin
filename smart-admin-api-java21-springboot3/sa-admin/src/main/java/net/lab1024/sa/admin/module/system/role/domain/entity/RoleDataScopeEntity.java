package net.lab1024.sa.admin.module.system.role.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import net.lab1024.sa.admin.module.system.datascope.constant.DataScopeTypeEnum;
import net.lab1024.sa.admin.module.system.datascope.constant.DataScopeViewTypeEnum;

/**
 * 数据范围与角色关系
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-03-07 18:54:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_role_data_scope")
public class RoleDataScopeEntity {
  /** 主键id */
  @TableId(type = IdType.AUTO)
  private Long id;

  /** 数据范围id {@link DataScopeTypeEnum} */
  private Integer dataScopeType;

  /** 数据范围类型 {@link DataScopeViewTypeEnum} */
  private Integer viewType;

  /** 角色id */
  private Long roleId;

  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 创建时间 */
  private LocalDateTime createTime;
}
