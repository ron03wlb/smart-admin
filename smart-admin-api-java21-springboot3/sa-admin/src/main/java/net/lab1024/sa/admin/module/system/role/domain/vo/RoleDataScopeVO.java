package net.lab1024.sa.admin.module.system.role.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色的数据范围
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-04-08 21:53:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class RoleDataScopeVO {

  @Schema(description = "数据范围id")
  private Integer dataScopeType;

  @Schema(description = "可见范围")
  private Integer viewType;
}
