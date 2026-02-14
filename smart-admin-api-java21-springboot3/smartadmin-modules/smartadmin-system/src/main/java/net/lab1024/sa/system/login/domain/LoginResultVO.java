package net.lab1024.sa.system.login.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;

/**
 * 登录结果信息
 *
 * @author 1024创新实验室: 开云
 * @since 2021-12-19 11:49:45 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@EqualsAndHashCode(callSuper = true)
@Data
public class LoginResultVO extends RequestEmployee {

  private static final long serialVersionUID = 1L;

  @Schema(description = "token")
  private String token;

  @Schema(description = "菜单列表")
  private List<MenuVO> menuList;

  @Schema(description = "是否需要修改密码")
  private Boolean needUpdatePwdFlag;

  @Schema(description = "上次登录ip")
  private String lastLoginIp;

  @Schema(description = "上次登录ip地区")
  private String lastLoginIpRegion;

  @Schema(description = "上次登录user-agent")
  private String lastLoginUserAgent;

  @Schema(description = "上次登录时间")
  private OffsetDateTime lastLoginTime;
}
