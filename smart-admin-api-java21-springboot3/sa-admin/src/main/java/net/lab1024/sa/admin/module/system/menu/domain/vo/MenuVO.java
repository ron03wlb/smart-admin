package net.lab1024.sa.admin.module.system.menu.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.admin.module.system.menu.domain.form.MenuBaseForm;

/**
 * 菜单
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-03-06 22:04:37 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuVO extends MenuBaseForm implements Serializable {

  private static final long serialVersionUID = 1L;

  @Schema(description = "菜单ID")
  private Long menuId;

  @Schema(description = "创建时间")
  private LocalDateTime createTime;

  @Schema(description = "创建人")
  private Long createUserId;

  @Schema(description = "更新时间")
  private LocalDateTime updateTime;

  @Schema(description = "更新人")
  private Long updateUserId;
}
