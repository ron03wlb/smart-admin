package net.lab1024.sa.common.datamasking.util;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.datamasking.annotation.DataMasking;
import net.lab1024.sa.common.datamasking.constant.DataMaskingTypeEnum;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * 脱敏工具类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2024-07-23 21:38:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@SuppressWarnings("PMD.AvoidAccessibilityAlteration")
@SuppressFBWarnings("DP_DO_INSIDE_DO_PRIVILEGED")
public class SmartDataMaskingUtil {

  /** 类 加注解字段缓存 */
  private static final Map<Class<?>, List<Field>> fieldMap = new ConcurrentHashMap<>();

  // Length threshold constants for masking
  private static final int LENGTH_THRESHOLD_4 = 4;
  private static final int LENGTH_THRESHOLD_6 = 6;
  private static final int LENGTH_THRESHOLD_10 = 10;
  private static final int LENGTH_THRESHOLD_18 = 18;
  private static final int LENGTH_THRESHOLD_27 = 27;
  private static final int LENGTH_THRESHOLD_34 = 34;
  private static final int LENGTH_THRESHOLD_41 = 41;

  public static String dataMasking(String value) {
    if (StringUtils.isBlank(value)) {
      return value;
    }

    if (value.length() < LENGTH_THRESHOLD_4) {
      return StrUtil.hide(value, 0, value.length());
    }

    int valueLength = value.length();
    int startHideIndex = getHideStartIndex(valueLength);
    int endHideIndex = getHideEndIndex(valueLength);
    return StrUtil.hide(value, startHideIndex, endHideIndex);
  }

  public static Object dataMasking(Object value, DataMaskingTypeEnum dataType) {

    if (value == null) {
      return null;
    }

    if (dataType == null) {
      return dataMasking(String.valueOf(value));
    }

    switch (dataType) {
      case PHONE:
        return DesensitizedUtil.mobilePhone(String.valueOf(value));
      case CHINESE_NAME:
        return DesensitizedUtil.chineseName(String.valueOf(value));
      case ID_CARD:
        return DesensitizedUtil.idCardNum(String.valueOf(value), 6, 2);
      case FIXED_PHONE:
        return DesensitizedUtil.fixedPhone(String.valueOf(value));
      case ADDRESS:
        return StrUtil.hide(String.valueOf(value), 6, String.valueOf(value).length() - 1);
      case EMAIL:
        return DesensitizedUtil.email(String.valueOf(value));
      case PASSWORD:
        return DesensitizedUtil.password(String.valueOf(value));
      case CAR_LICENSE:
        return DesensitizedUtil.carLicense(String.valueOf(value));
      case BANK_CARD:
        return DesensitizedUtil.bankCard(String.valueOf(value));
      case USER_ID:
        return DesensitizedUtil.userId();
      default:
        return dataMasking(String.valueOf(value));
    }
  }

  /** 批量脱敏 */
  public static <T> void dataMasking(Collection<T> objectList)
      throws IntrospectionException, InvocationTargetException, IllegalAccessException {
    if (CollectionUtils.isEmpty(objectList)) {
      return;
    }

    for (T object : objectList) {
      dataMasking(object);
    }
  }

  /** 单个脱敏 */
  public static <T> void dataMasking(T object)
      throws IntrospectionException, InvocationTargetException, IllegalAccessException {
    Class<?> tClass = object.getClass();
    List<Field> fieldList = getField(object);
    for (Field field : fieldList) {
      field.setAccessible(true);
      String fieldValue = null;
      try {
        PropertyDescriptor pd = new PropertyDescriptor(field.getName(), tClass);
        Method getMethod = pd.getReadMethod();
        Object value = getMethod.invoke(object);
        if (value != null) {
          fieldValue = value.toString();
        }
      } catch (IntrospectionException | ReflectiveOperationException e) {
        throw new RuntimeException(e);
      }
      if (StringUtils.isBlank(fieldValue)) {
        continue;
      }
      int valueLength = fieldValue.length();
      int startHideIndex = getHideStartIndex(valueLength);
      int endHideIndex = getHideEndIndex(valueLength);
      try {
        field.set(object, StrUtil.hide(fieldValue, startHideIndex, endHideIndex));
      } catch (IllegalAccessException e1) {
        throw new RuntimeException(e1);
      }
    }
  }

  private static int getHideStartIndex(int totalLength) {
    if (totalLength <= LENGTH_THRESHOLD_4) {
      return 1;
    } else if (totalLength <= LENGTH_THRESHOLD_6) {
      return 1;
    } else if (totalLength <= LENGTH_THRESHOLD_10) {
      return 2;
    } else if (totalLength <= LENGTH_THRESHOLD_18) {
      return 3;
    } else if (totalLength <= LENGTH_THRESHOLD_27) {
      return 5;
    } else if (totalLength <= LENGTH_THRESHOLD_34) {
      return 7;
    } else if (totalLength <= LENGTH_THRESHOLD_41) {
      return 9;
    } else {
      return 15;
    }
  }

  private static int getHideEndIndex(int totalLength) {
    if (totalLength <= LENGTH_THRESHOLD_4) {
      return totalLength - 1;
    } else if (totalLength <= LENGTH_THRESHOLD_6) {
      return totalLength - 2;
    } else if (totalLength <= LENGTH_THRESHOLD_10) {
      return totalLength - 2;
    } else if (totalLength <= LENGTH_THRESHOLD_18) {
      return totalLength - 4;
    } else if (totalLength <= LENGTH_THRESHOLD_27) {
      return totalLength - 6;
    } else if (totalLength <= LENGTH_THRESHOLD_34) {
      return totalLength - 8;
    } else if (totalLength <= LENGTH_THRESHOLD_41) {
      return totalLength - 10;
    } else {
      return totalLength - 16;
    }
  }

  public static List<Field> getField(Object object) throws IntrospectionException {
    // 从缓存中查询
    Class<?> tClass = object.getClass();
    List<Field> fieldList = fieldMap.get(tClass);
    if (null != fieldList) {
      return fieldList;
    }

    // 这一段递归代码 是为了 从父类中获取属性
    Class<?> tempClass = tClass;
    fieldList = new ArrayList<>();
    while (tempClass != null) {
      Field[] declaredFields = tempClass.getDeclaredFields();
      for (Field field : declaredFields) {
        boolean stringField;
        try {
          PropertyDescriptor pd = new PropertyDescriptor(field.getName(), tClass);
          Method getMethod = pd.getReadMethod();
          Type returnType = getMethod.getGenericReturnType();
          stringField = "java.lang.String".equals(returnType.getTypeName());
        } catch (IntrospectionException e) {
          stringField = false;
        }

        if (field.isAnnotationPresent(DataMasking.class) && stringField) {
          field.setAccessible(true);
          fieldList.add(field);
        }
      }
      tempClass = tempClass.getSuperclass();
    }
    fieldMap.put(tClass, fieldList);
    return fieldList;
  }

  public static void main(String[] args) {
    // empty
  }
}
