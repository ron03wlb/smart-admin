package net.lab1024.sa.system.role.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.core.domain.request.PageParam;

/**
 * 角色 查询
 *
 * @author 1024创新实验室: 胡克
 * @since 2022-02-26 19:09:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleQueryForm extends PageParam {

  @Schema(description = "角色名称")
  private String roleName;

  @Schema(description = "角色id")
  private String roleId;
}
