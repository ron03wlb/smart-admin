package net.lab1024.sa.system.role.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 选择角色
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RoleSelectedVO extends RoleVO {

  @Schema(description = "角色名称")
  private Boolean selected;
}
