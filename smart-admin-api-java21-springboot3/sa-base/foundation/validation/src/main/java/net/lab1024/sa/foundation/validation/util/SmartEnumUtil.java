package net.lab1024.sa.foundation.validation.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.lab1024.sa.foundation.domain.enumeration.BaseEnum;

/**
 * 枚举工具类
 *
 * @author 1024创新实验室: 胡克
 * @since 2017/10/10 18:17 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class SmartEnumUtil {

  /**
   * 校验参数与枚举类比较是否合法
   *
   * @param value 参数
   * @param enumClass 枚举类必须实现BaseEnum接口
   * @return boolean
   */
  public static boolean checkEnum(Object value, Class<? extends BaseEnum> enumClass) {
    if (null == value) {
      return false;
    }
    return Stream.of(enumClass.getEnumConstants()).anyMatch(e -> e.equalsValue(value));
  }

  /**
   * 创建一个具有唯一array值的数组，每个值不包含在其他给定的数组中。
   *
   * @param enumClass enum class
   * @param exclude excluded values
   * @param <T> enum type
   * @return List
   */
  @SafeVarargs
  public static <T extends BaseEnum> List<Object> differenceValueList(
      Class<? extends BaseEnum> enumClass, T... exclude) {
    java.util.Set<Object> valueSet = new HashSet<>();
    if (exclude != null) {
      valueSet.addAll(Stream.of(exclude).map(BaseEnum::getValue).collect(Collectors.toSet()));
    }

    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> !valueSet.contains(e.getValue()))
        .map(BaseEnum::getValue)
        .collect(Collectors.toList());
  }

  /**
   * 获取枚举类的说明 value : info 的形式
   *
   * @param enumClass enum class
   * @return String
   */
  public static String getEnumDesc(Class<? extends BaseEnum> enumClass) {
    BaseEnum[] enums = enumClass.getEnumConstants();
    // value : info 的形式
    StringBuilder sb = new StringBuilder();
    for (BaseEnum baseEnum : enums) {
      sb.append(baseEnum.getValue()).append('：').append(baseEnum.getDesc()).append('，');
    }
    return sb.toString();
  }

  /**
   * 获取与参数相匹配的枚举类实例的 说明
   *
   * @param value 参数
   * @param enumClass 枚举类必须实现BaseEnum接口
   * @return String 如无匹配枚举则返回null
   */
  public static String getEnumDescByValue(Object value, Class<? extends BaseEnum> enumClass) {
    if (null == value) {
      return null;
    }
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> e.equalsValue(value))
        .findFirst()
        .map(BaseEnum::getDesc)
        .orElse(null);
  }

  public static <T> String getEnumDescByValueList(
      Collection<T> values, Class<? extends BaseEnum> enumClass) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> values.contains(e.getValue()))
        .map(BaseEnum::getDesc)
        .collect(Collectors.joining(","));
  }

  /**
   * 根据参数获取枚举类的实例
   *
   * @param value 参数
   * @param enumClass 枚举类必须实现BaseEnum接口
   * @param <T> enum type
   * @return BaseEnum 无匹配值返回null
   */
  public static <T extends BaseEnum> T getEnumByValue(Object value, Class<T> enumClass) {
    if (null == value) {
      return null;
    }
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> e.equalsValue(value))
        .findFirst()
        .orElse(null);
  }

  /**
   * 根据实例描述与获取枚举类的实例
   *
   * @param desc 参数描述
   * @param enumClass 枚举类必须实现BaseEnum接口
   * @param <T> enum type
   * @return BaseEnum 无匹配值返回null
   */
  public static <T extends BaseEnum> T getEnumByDesc(String desc, Class<T> enumClass) {
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> Objects.equals(e.getDesc(), desc))
        .findFirst()
        .orElse(null);
  }

  public static <T extends BaseEnum> T getEnumByName(String name, Class<T> enumClass) {
    return Stream.of(enumClass.getEnumConstants())
        .filter(e -> name != null && name.equalsIgnoreCase(e.toString()))
        .findFirst()
        .orElse(null);
  }

  /**
   * 根据lambda getter/setter 注入
   *
   * @param list list to process
   * @param getter value getter
   * @param setter desc setter
   * @param enumClass enum class
   * @param <T> element type
   */
  public static <T> void inject(
      List<T> list,
      Function<T, Integer> getter,
      BiConsumer<T, String> setter,
      Class<? extends BaseEnum> enumClass) {
    if (list == null || list.isEmpty()) {
      return;
    }
    for (T t : list) {
      Integer enumValue = getter.apply(t);
      if (enumValue != null) {
        setter.accept(t, getEnumDescByValue(enumValue, enumClass));
      }
    }
  }
}
