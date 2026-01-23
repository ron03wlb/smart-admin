package net.lab1024.sa.constant;

import net.lab1024.sa.foundation.domain.enumeration.BaseEnum;

/**
 * 登录设备类型
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-11-29 19:48:35 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public enum LoginDeviceEnum implements BaseEnum {
  PC(1, "电脑端"),

  ANDROID(2, "安卓"),

  APPLE(3, "苹果"),

  H5(4, "H5"),

  WEIXIN_MP(5, "微信小程序");

  LoginDeviceEnum(Integer value, String desc) {
    this.value = value;
    this.desc = desc;
  }

  private Integer value;
  private String desc;

  @Override
  public Integer getValue() {
    return value;
  }

  @Override
  public String getDesc() {
    return desc;
  }
}
