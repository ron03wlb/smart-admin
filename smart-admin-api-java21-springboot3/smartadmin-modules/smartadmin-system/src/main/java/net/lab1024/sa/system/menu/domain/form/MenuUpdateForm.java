package net.lab1024.sa.system.menu.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单 更新Form
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-03-06 22:04:37 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuUpdateForm extends MenuBaseForm {

  @Schema(description = "菜单ID")
  @NotNull(message = "菜单ID不能为空")
  private Long menuId;

  @Schema(hidden = true)
  private Long updateUserId;
}
