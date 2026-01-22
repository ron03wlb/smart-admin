package net.lab1024.sa.admin.util;

import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.base.web.util.SmartRequestUtil;
import net.lab1024.sa.common.core.domain.RequestUser;

/**
 * admin 端的请求工具类
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/7/28 19:39:21 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
public final class AdminRequestUtil {

  public static RequestEmployee getRequestUser() {
    return (RequestEmployee) SmartRequestUtil.getRequestUser();
  }

  public static Long getRequestUserId() {
    RequestUser requestUser = getRequestUser();
    return null == requestUser ? null : requestUser.getUserId();
  }
}
