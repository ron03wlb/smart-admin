package net.lab1024.sa.common.web.web.config;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import net.lab1024.sa.common.web.web.json.serializer.LongJsonSerializer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;

/**
 * json 序列化配置
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2017-11-28 15:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Configuration
public class JsonConfig {

  @Bean
  public Jackson2ObjectMapperBuilderCustomizer customizer() {
    return builder -> {
      // LocalDate serializers (unchanged — dates have no timezone)
      builder.deserializers(
          new LocalDateDeserializer(DatePattern.NORM_DATE_FORMAT.getDateTimeFormatter()));
      builder.serializers(
          new LocalDateSerializer(DatePattern.NORM_DATE_FORMAT.getDateTimeFormatter()));
      // OffsetDateTime serializer — converts UTC to tenant's timezone (ISO-8601 output)
      // Deserialization handled by Spring Boot's auto-configured JavaTimeModule (ISO-8601 default)
      builder.serializerByType(
          OffsetDateTime.class,
          new net.lab1024.sa.common.web.web.json.serializer.TenantTimezoneSerializer());
      builder.serializerByType(Long.class, LongJsonSerializer.INSTANCE);
      builder.serializerByType(Long.TYPE, LongJsonSerializer.INSTANCE);
      builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
      builder.serializerByType(BigDecimal.class, ToStringSerializer.instance);
    };
  }

  /**
   * string 转为 OffsetDateTime 配置类
   *
   * @author 卓大
   */
  @Configuration
  public static class StringToOffsetDateTime implements Converter<String, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(String str) {
      if (StringUtils.isBlank(str)) {
        return null;
      }
      try {
        return OffsetDateTime.parse(str, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
      } catch (DateTimeParseException e) {
        // Fallback: try legacy format "yyyy-MM-dd HH:mm:ss" and assume UTC
        try {
          LocalDateTime ldt =
              LocalDateTimeUtil.parse(str, DatePattern.NORM_DATETIME_FORMAT.getDateTimeFormatter());
          return ldt.atOffset(ZoneOffset.UTC);
        } catch (DateTimeParseException e2) {
          throw new RuntimeException(
              "请输入正确的日期格式：ISO-8601 (e.g. 2026-01-01T00:00:00Z) 或 yyyy-MM-dd HH:mm:ss", e2);
        }
      }
    }
  }

  /**
   * string 转为 LocalDate 配置类
   *
   * @author 卓大
   */
  @Configuration
  public static class StringToLocalDate implements Converter<String, LocalDate> {

    @Override
    public LocalDate convert(String str) {
      if (StringUtils.isBlank(str)) {
        return null;
      }
      LocalDate localDate;
      try {
        localDate =
            LocalDateTimeUtil.parseDate(str, DatePattern.NORM_DATE_FORMAT.getDateTimeFormatter());
      } catch (DateTimeParseException e) {
        throw new RuntimeException("请输入正确的日期格式：yyyy-MM-dd", e);
      }
      return localDate;
    }
  }
}
