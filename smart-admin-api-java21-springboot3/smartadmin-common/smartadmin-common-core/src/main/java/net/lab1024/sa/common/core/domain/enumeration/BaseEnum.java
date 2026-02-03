package net.lab1024.sa.common.core.domain.enumeration;

import java.util.Objects;

/**
 * 枚举类接口
 *
 * @author 1024创新实验室: 胡克
 * @since 2018-07-17 21:22:12 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface BaseEnum {

  /**
   * 获取枚举类的值
   *
   * @return Object
   */
  Object getValue();

  /**
   * 获取枚举类的说明
   *
   * @return String
   */
  String getDesc();

  /**
   * 比较参数是否与枚举类的value相同
   *
   * @param value value to compare
   * @return boolean
   */
  default boolean equalsValue(Object value) {
    return Objects.equals(this.getValue(), value);
  }

  /** 比较枚举类是否相同 */
  default boolean isSame(BaseEnum baseEnum) {
    return Objects.equals(getValue(), baseEnum.getValue())
        && Objects.equals(getDesc(), baseEnum.getDesc());
  }
}
