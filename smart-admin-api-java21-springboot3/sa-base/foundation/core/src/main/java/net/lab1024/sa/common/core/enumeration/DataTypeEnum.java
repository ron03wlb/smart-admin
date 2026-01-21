package net.lab1024.sa.common.core.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/25 09:47:13 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Getter
@AllArgsConstructor
public enum DataTypeEnum implements BaseEnum {

  /** 普通数据 */
  NORMAL(1, "普通数据"),

  /** 加密数据 */
  ENCRYPT(10, "加密数据"),
  ;
  private final Integer value;

  private final String desc;
}
