package net.lab1024.sa.admin.module.system.menu.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-03-06 22:04:37 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class MenuTreeVO extends MenuVO {

  private static final long serialVersionUID = 1L;

  @Schema(description = "菜单子集")
  private List<MenuTreeVO> children;
}
