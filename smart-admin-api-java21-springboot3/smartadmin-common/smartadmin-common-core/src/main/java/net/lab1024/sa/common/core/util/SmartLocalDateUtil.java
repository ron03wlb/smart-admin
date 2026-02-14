package net.lab1024.sa.common.core.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

/**
 * @author 1024创新实验室:胡克
 * @since 2023/12/5 22:25:43 1024创新实验室 （ https://1024lab.net ），2012-2023
 */
public class SmartLocalDateUtil {

  /**
   * 格式化 OffsetDateTime 返回对应格式字符串
   *
   * @param time
   * @param formatterEnum {@link SmartDateFormatterEnum}
   * @return
   */
  public static String format(OffsetDateTime time, SmartDateFormatterEnum formatterEnum) {
    return time.format(formatterEnum.getFormatter());
  }

  /**
   * 格式化 LocalDate返回对应格式字符串
   *
   * @param date
   * @param formatterEnum {@link SmartDateFormatterEnum}
   * @return
   */
  public static String format(LocalDate date, SmartDateFormatterEnum formatterEnum) {
    return date.format(formatterEnum.getFormatter());
  }

  /**
   * 解析时间字符串 返回 OffsetDateTime (UTC)
   *
   * @param time
   * @param formatterEnum {@link SmartDateFormatterEnum}
   * @return
   */
  public static OffsetDateTime parse(String time, SmartDateFormatterEnum formatterEnum) {
    return OffsetDateTime.parse(time, formatterEnum.getFormatter());
  }

  /**
   * 解析时间字符串 返回 LocalDate
   *
   * @param time
   * @param formatterEnum {@link SmartDateFormatterEnum}
   * @return
   */
  public static LocalDate parseDate(String time, SmartDateFormatterEnum formatterEnum) {
    return LocalDate.parse(time, formatterEnum.getFormatter());
  }

  /**
   * 获取指定日期时间戳
   *
   * @param time
   * @return
   */
  public static Long getTimestamp(OffsetDateTime time) {
    return time.toInstant().toEpochMilli();
  }

  /**
   * 获取当前时间戳(秒)
   *
   * @return
   */
  public static long nowSecond() {
    return System.currentTimeMillis() / 1000;
  }

  /**
   * 将时间格式化为 星期几，例：星期一 ... 星期日
   *
   * @param localDate
   * @return
   */
  public static String formatToChineseWeek(LocalDate localDate) {
    return localDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.CHINESE);
  }

  /**
   * 将时间格式化为 周几，例：周一 ... 周日
   *
   * @param localDate
   * @return
   */
  public static String formatToChineseWeekZhou(LocalDate localDate) {
    return formatToChineseWeek(localDate).replace("星期", "周");
  }

  public static OffsetDateTime toOffsetDateTime(Date date) {
    return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toOffsetDateTime();
  }

  /**
   * 获取当天剩余时间 单位
   *
   * @param unit 时间单位
   * @return
   */
  public static Long getDayBalanceTime(ChronoUnit unit) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    return Duration.between(now, now.plusDays(1L).with(LocalTime.MIN)).get(unit);
  }
}
