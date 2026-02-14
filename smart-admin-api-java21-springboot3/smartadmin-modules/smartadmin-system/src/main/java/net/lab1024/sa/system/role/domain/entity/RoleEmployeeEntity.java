package net.lab1024.sa.system.role.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 角色 员工关系
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-03-07 18:54:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_role_employee")
public class RoleEmployeeEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long roleId;

  private Long employeeId;

  public RoleEmployeeEntity() {}

  public RoleEmployeeEntity(Long roleId, Long employeeId) {
    this.roleId = roleId;
    this.employeeId = employeeId;
  }
}
