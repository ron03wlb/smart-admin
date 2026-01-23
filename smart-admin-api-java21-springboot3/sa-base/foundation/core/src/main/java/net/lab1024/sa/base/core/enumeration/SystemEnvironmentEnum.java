package net.lab1024.sa.foundation.core.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.foundation.domain.enumeration.BaseEnum;

/**
 * 系统环境枚举类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2020-10-15 22:45:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AllArgsConstructor
@Getter
public enum SystemEnvironmentEnum implements BaseEnum {
  /** dev */
  DEV(SystemEnvironmentNameConst.DEV, "开发环境"),

  /** test */
  TEST(SystemEnvironmentNameConst.TEST, "测试环境"),

  /** pre */
  PRE(SystemEnvironmentNameConst.PRE, "预发布环境"),

  /** prod */
  PROD(SystemEnvironmentNameConst.PROD, "生产环境");

  private final String value;

  private final String desc;

  public static final class SystemEnvironmentNameConst {
    public static final String DEV = "dev";
    public static final String TEST = "test";
    public static final String PRE = "pre";
    public static final String PROD = "prod";
  }
}
