package net.lab1024.sa.system.role.domain.vo;

import lombok.Data;

/**
 * 角色的员工
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class RoleEmployeeVO {

  private Long roleId;

  private Long employeeId;

  private String roleName;
}
