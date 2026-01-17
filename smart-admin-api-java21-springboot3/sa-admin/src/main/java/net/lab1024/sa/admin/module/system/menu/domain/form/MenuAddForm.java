package net.lab1024.sa.admin.module.system.menu.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单 添加表单
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-03-06 22:04:37 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuAddForm extends MenuBaseForm {

  @Schema(hidden = true)
  private Long createUserId;
}
