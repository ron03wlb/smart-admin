package net.lab1024.sa.system.role.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 角色
 *
 * @author 1024创新实验室: 胡克
 * @since 2022-03-07 18:54:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_role")
public class RoleEntity extends SmartAdminBaseEntity {
  /** 主键id */
  @TableId(type = IdType.AUTO)
  private Long roleId;

  /** 角色名称 */
  private String roleName;

  /** 角色编码 */
  private String roleCode;

  /** 角色备注 */
  private String remark;
}
