package net.lab1024.sa.system.menu.domain.form;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 菜单功能点操作Form
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-03-06 22:04:37 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Data
public class MenuPointsOperateForm {

  @Schema(description = "菜单ID")
  private Long menuId;

  @Schema(description = "功能点名称")
  @NotBlank(message = "功能点不能为空")
  @Length(max = 30, message = "功能点最多30个字符")
  private String menuName;

  @Schema(description = "禁用状态")
  @NotNull(message = "禁用状态不能为空")
  private Boolean disabledFlag;

  @Schema(description = "后端接口权限集合")
  private List<String> apiPermsList;

  @Schema(description = "权限字符串")
  private String webPerms;

  @Schema(description = "功能点关联菜单ID")
  private Long contextMenuId;
}
