package net.lab1024.sa.foundation.domain.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 性别枚举类
 *
 * @author 1024创新实验室: 胡克
 * @since 2019/09/24 16:50 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AllArgsConstructor
@Getter
public enum GenderEnum implements BaseEnum {

  /** 0 未知 */
  UNKNOWN(0, "未知"),

  /** 男 1 奇数为阳 */
  MAN(1, "男"),

  /** 女 2 偶数为阴 */
  WOMAN(2, "女");

  private final Integer value;

  private final String desc;
}
