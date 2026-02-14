package net.lab1024.sa.common.core.util;

import cn.hutool.core.util.StrUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 独有的字符串工具类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-09-02 20:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class SmartStringUtil extends StrUtil {

  // ===============split =======================

  public static Set<String> splitConvertToSet(String str, String split) {
    if (isEmpty(str)) {
      return new HashSet<>();
    }
    String[] splitArr = str.split(split);
    Set<String> set = new HashSet<>(splitArr.length);
    Collections.addAll(set, splitArr);
    return set;
  }

  public static List<String> splitConvertToList(String str, String split) {
    if (isEmpty(str)) {
      return new ArrayList<>();
    }
    String[] splitArr = str.split(split);
    List<String> list = new ArrayList<>(splitArr.length);
    list.addAll(Arrays.asList(splitArr));
    return list;
  }

  // ===============split Integer=======================

  public static List<Integer> splitConvertToIntList(String str, String split, int defaultVal) {
    if (isEmpty(str)) {
      return new ArrayList<>();
    }
    String[] strArr = str.split(split);
    List<Integer> list = new ArrayList<>(strArr.length);
    for (String s : strArr) {
      try {
        int parseInt = Integer.parseInt(s);
        list.add(parseInt);
      } catch (NumberFormatException e) {
        list.add(defaultVal);
      }
    }
    return list;
  }

  public static Set<Integer> splitConvertToIntSet(String str, String split, int defaultVal) {
    if (isEmpty(str)) {
      return new HashSet<>();
    }
    String[] strArr = str.split(split);
    Set<Integer> set = new HashSet<>(strArr.length);
    for (String s : strArr) {
      try {
        int parseInt = Integer.parseInt(s);
        set.add(parseInt);
      } catch (NumberFormatException e) {
        set.add(defaultVal);
      }
    }
    return set;
  }

  public static Set<Integer> splitConvertToIntSet(String str, String split) {
    return splitConvertToIntSet(str, split, 0);
  }

  public static List<Integer> splitConvertToIntList(String str, String split) {
    return splitConvertToIntList(str, split, 0);
  }

  public static int[] splitConvertToIntArray(String str, String split, int defaultVal) {
    if (isEmpty(str)) {
      return new int[0];
    }
    String[] strArr = str.split(split);
    int[] result = new int[strArr.length];
    for (int i = 0; i < strArr.length; i++) {
      try {
        result[i] = Integer.parseInt(strArr[i]);
      } catch (NumberFormatException e) {
        result[i] = defaultVal;
        continue;
      }
    }
    return result;
  }

  public static int[] splitConvertToIntArray(String str, String split) {
    return splitConvertToIntArray(str, split, 0);
  }

  // ===============split 2 Long=======================

  public static List<Long> splitConvertToLongList(String str, String split, long defaultVal) {
    if (isEmpty(str)) {
      return new ArrayList<>();
    }
    String[] strArr = str.split(split);
    List<Long> list = new ArrayList<>(strArr.length);
    for (String s : strArr) {
      try {
        long parseLong = Long.parseLong(s);
        list.add(parseLong);
      } catch (NumberFormatException e) {
        list.add(defaultVal);
      }
    }
    return list;
  }

  public static List<Long> splitConvertToLongList(String str, String split) {
    return splitConvertToLongList(str, split, 0L);
  }

  public static long[] splitConvertToLongArray(String str, String split, long defaultVal) {
    if (isEmpty(str)) {
      return new long[0];
    }
    String[] strArr = str.split(split);
    long[] result = new long[strArr.length];
    for (int i = 0; i < strArr.length; i++) {
      try {
        result[i] = Long.parseLong(strArr[i]);
      } catch (NumberFormatException e) {
        result[i] = defaultVal;
        continue;
      }
    }
    return result;
  }

  public static long[] splitConvertToLongArray(String str, String split) {
    return splitConvertToLongArray(str, split, 0L);
  }

  // ===============split convert byte=======================

  public static List<Byte> splitConvertToByteList(String str, String split, byte defaultVal) {
    if (isEmpty(str)) {
      return new ArrayList<>();
    }
    String[] strArr = str.split(split);
    List<Byte> list = new ArrayList<>(strArr.length);
    for (String s : strArr) {
      try {
        byte parseByte = Byte.parseByte(s);
        list.add(parseByte);
      } catch (NumberFormatException e) {
        list.add(defaultVal);
      }
    }
    return list;
  }

  public static List<Byte> splitConvertToByteList(String str, String split) {
    return splitConvertToByteList(str, split, (byte) 0);
  }

  public static byte[] splitConvertToByteArray(String str, String split, byte defaultVal) {
    if (isEmpty(str)) {
      return new byte[0];
    }
    String[] strArr = str.split(split);
    byte[] result = new byte[strArr.length];
    for (int i = 0; i < strArr.length; i++) {
      try {
        result[i] = Byte.parseByte(strArr[i]);
      } catch (NumberFormatException e) {
        result[i] = defaultVal;
        continue;
      }
    }
    return result;
  }

  public static byte[] splitConvertToByteArray(String str, String split) {
    return splitConvertToByteArray(str, split, (byte) 0);
  }

  // ===============split convert double=======================

  public static List<Double> splitConvertToDoubleList(String str, String split, double defaultVal) {
    if (isEmpty(str)) {
      return new ArrayList<>();
    }
    String[] strArr = str.split(split);
    List<Double> list = new ArrayList<>(strArr.length);
    for (String s : strArr) {
      try {
        double parseByte = Double.parseDouble(s);
        list.add(parseByte);
      } catch (NumberFormatException e) {
        list.add(defaultVal);
      }
    }
    return list;
  }

  public static List<Double> splitConvertToDoubleList(String str, String split) {
    return splitConvertToDoubleList(str, split, 0);
  }

  public static double[] splitConvertToDoubleArray(String str, String split, double defaultVal) {
    if (isEmpty(str)) {
      return new double[0];
    }
    String[] strArr = str.split(split);
    double[] result = new double[strArr.length];
    for (int i = 0; i < strArr.length; i++) {
      try {
        result[i] = Double.parseDouble(strArr[i]);
      } catch (NumberFormatException e) {
        result[i] = defaultVal;
        continue;
      }
    }
    return result;
  }

  public static double[] splitConvertToDoubleArray(String str, String split) {
    return splitConvertToDoubleArray(str, split, 0);
  }

  // ===============split convert float=======================

  public static List<Float> splitConvertToFloatList(String str, String split, float defaultVal) {
    if (isEmpty(str)) {
      return new ArrayList<>();
    }
    String[] strArr = str.split(split);
    List<Float> list = new ArrayList<>(strArr.length);
    for (String s : strArr) {
      try {
        float parseByte = Float.parseFloat(s);
        list.add(parseByte);
      } catch (NumberFormatException e) {
        list.add(defaultVal);
      }
    }
    return list;
  }

  public static List<Float> splitConvertToFloatList(String str, String split) {
    return splitConvertToFloatList(str, split, 0f);
  }

  public static float[] splitConvertToFloatArray(String str, String split, float defaultVal) {
    if (isEmpty(str)) {
      return new float[0];
    }
    String[] strArr = str.split(split);
    float[] result = new float[strArr.length];
    for (int i = 0; i < strArr.length; i++) {
      try {
        result[i] = Float.parseFloat(strArr[i]);
      } catch (NumberFormatException e) {
        result[i] = defaultVal;
        continue;
      }
    }
    return result;
  }

  public static float[] splitConvertToFloatArray(String str, String split) {
    return splitConvertToFloatArray(str, split, 0f);
  }

  public static String upperCaseFirstChar(String str) {
    if (str != null && !str.isEmpty()) {
      char firstChar = str.charAt(0);
      if (Character.isUpperCase(firstChar)) {
        return str;
      } else {
        char[] values = str.toCharArray();
        values[0] = Character.toUpperCase(firstChar);
        return new String(values);
      }
    } else {
      return str;
    }
  }

  public static String replace(String content, int begin, int end, String newStr) {
    if (begin < content.length() && begin >= 0) {
      if (end <= content.length() && end >= 0) {
        if (begin > end) {
          return content;
        } else {
          StringBuilder starStr = new StringBuilder();

          for (int i = begin; i < end; ++i) {
            starStr.append(newStr);
          }

          return content.substring(0, begin) + starStr + content.substring(end);
        }
      } else {
        return content;
      }
    } else {
      return content;
    }
  }
}
