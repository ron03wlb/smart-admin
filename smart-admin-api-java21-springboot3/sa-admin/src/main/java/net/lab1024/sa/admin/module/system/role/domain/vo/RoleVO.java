package net.lab1024.sa.admin.module.system.role.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class RoleVO {

  @Schema(description = "角色ID")
  private Long roleId;

  @Schema(description = "角色名称")
  private String roleName;

  @Schema(description = "角色编码")
  private String roleCode;

  @Schema(description = "角色备注")
  private String remark;
}
