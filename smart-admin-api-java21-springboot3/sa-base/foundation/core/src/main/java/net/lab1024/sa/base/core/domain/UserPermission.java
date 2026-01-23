package net.lab1024.sa.foundation.core.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * sa-token 所需的权限信息
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/8/26 15:23:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UserPermission implements Serializable {
  private static final long serialVersionUID = 1L;

  /** 权限列表 */
  private List<String> permissionList;

  /** 角色列表 */
  private List<String> roleList;
}
