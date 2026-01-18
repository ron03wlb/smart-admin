package net.lab1024.sa.base.common.enumeration;

import com.google.common.base.CaseFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.lab1024.sa.base.common.json.JsonUtil;

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
   * @return
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
   * @param value
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

  static String getInfo(Class<? extends BaseEnum> clazz) {
    BaseEnum[] enums = clazz.getEnumConstants();
    Map<String, Object> json = new LinkedHashMap<>(enums.length);
    for (BaseEnum e : enums) {
      Map<String, Object> jsonObject = new LinkedHashMap<>();
      jsonObject.put("value", formatValue(e.getValue()));
      jsonObject.put("desc", formatValue(e.getDesc()));
      json.put(e.toString(), jsonObject);
    }

    String enumJson = JsonUtil.toPrettyJson(json);
    if (enumJson == null) {
      enumJson = "{}";
    }
    enumJson = enumJson.replaceAll("\"", "");
    enumJson = enumJson.replaceAll("\t", "&nbsp;&nbsp;");
    enumJson = enumJson.replaceAll("\n", "<br>");
    enumJson = enumJson.replaceAll("  ", "&nbsp;&nbsp;");
    String prefix =
        "  <br>  export const "
            + CaseFormat.UPPER_CAMEL.to(
                CaseFormat.UPPER_UNDERSCORE, clazz.getSimpleName() + " = <br> ");
    return prefix + enumJson + " <br>";
  }

  /** Format value for display - wrap strings with single quotes */
  private static String formatValue(Object value) {
    if (value instanceof String) {
      return "'" + value + "'";
    }
    return value == null ? "" : value.toString();
  }
}
