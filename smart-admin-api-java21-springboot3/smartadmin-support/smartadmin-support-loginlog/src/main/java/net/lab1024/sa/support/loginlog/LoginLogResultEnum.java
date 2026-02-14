package net.lab1024.sa.support.loginlog;

import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * 登录类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/07/22 19:46:23 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public enum LoginLogResultEnum implements BaseEnum {
  LOGIN_SUCCESS(0, "登录成功"),
  LOGIN_FAIL(1, "登录失败"),
  LOGIN_OUT(2, "退出登录");

  private Integer type;
  private String desc;

  LoginLogResultEnum(Integer type, String desc) {
    this.type = type;
    this.desc = desc;
  }

  @Override
  public Integer getValue() {
    return type;
  }

  @Override
  public String getDesc() {
    return desc;
  }
}
