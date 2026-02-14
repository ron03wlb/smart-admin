package net.lab1024.sa.support.serialnumber.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.lab1024.sa.common.core.domain.enumeration.BaseEnum;

/**
 * 单据序列号 枚举
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@AllArgsConstructor
@Getter
public enum SerialNumberIdEnum implements BaseEnum {
  ORDER(1, "订单id"),

  CONTRACT(2, "合同id"),
  ;

  private final Integer serialNumberId;

  private final String desc;

  @Override
  public Integer getValue() {
    return serialNumberId;
  }

  @Override
  public String toString() {
    return "SerialNumberIdEnum{"
        + "serialNumberId="
        + serialNumberId
        + ", desc='"
        + desc
        + '\''
        + '}';
  }
}
