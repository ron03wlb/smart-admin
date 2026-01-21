package net.lab1024.sa.common.core.enumeration;

/**
 * 用户类型
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2022/10/19 21:46:24 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public enum UserTypeEnum implements BaseEnum {

  /** 管理端 员工用户 */
  ADMIN_EMPLOYEE(1, "员工");

  private final Integer type;

  private final String desc;

  UserTypeEnum(Integer type, String desc) {
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
