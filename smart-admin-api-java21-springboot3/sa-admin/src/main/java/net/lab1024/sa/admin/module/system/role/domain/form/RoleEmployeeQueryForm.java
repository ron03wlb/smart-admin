package net.lab1024.sa.admin.module.system.role.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.PageParam;

/**
 * 角色的员工查询
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleEmployeeQueryForm extends PageParam {

  @Schema(description = "关键字")
  private String keywords;

  @Schema(description = "角色id")
  private String roleId;
}
