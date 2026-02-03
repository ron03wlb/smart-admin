package net.lab1024.sa.system.role.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色更新修改
 *
 * @author 1024创新实验室: 胡克
 * @since 2022-02-26 19:09:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleUpdateForm extends RoleAddForm {

  /** 角色id */
  @Schema(description = "角色id")
  @NotNull(message = "角色id不能为空")
  protected Long roleId;
}
