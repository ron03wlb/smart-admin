package net.lab1024.sa.base.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON 工具类，基于 Jackson 实现
 *
 * <p>提供与 Fastjson 类似的静态方法 API，方便项目从 Fastjson 迁移到 Jackson
 *
 * @author 1024创新实验室
 * @since 2024/01/18 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Component
public class JsonUtil {

  @Resource private ObjectMapper objectMapper;

  private static ObjectMapper staticMapper;

  @PostConstruct
  public void init() {
    staticMapper = this.objectMapper;
  }

  /**
   * 对象转 JSON 字符串
   *
   * @param obj 要序列化的对象
   * @return JSON 字符串，如果对象为 null 或序列化失败则返回 null
   */
  public static String toJson(Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return staticMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("JSON序列化失败: {}", e.getMessage(), e);
      }
      return null;
    }
  }

  /**
   * 对象转格式化的 JSON 字符串（美化输出）
   *
   * @param obj 要序列化的对象
   * @return 格式化后的 JSON 字符串，如果对象为 null 或序列化失败则返回 null
   */
  public static String toPrettyJson(Object obj) {
    if (obj == null) {
      return null;
    }
    try {
      return staticMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("JSON序列化失败: {}", e.getMessage(), e);
      }
      return null;
    }
  }

  /**
   * JSON 字符串转对象
   *
   * @param json JSON 字符串
   * @param clazz 目标类型
   * @param <T> 泛型类型
   * @return 反序列化后的对象，如果 JSON 为空或反序列化失败则返回 null
   */
  public static <T> T fromJson(String json, Class<T> clazz) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return staticMapper.readValue(json, clazz);
    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("JSON反序列化失败: {}", e.getMessage(), e);
      }
      return null;
    }
  }

  /**
   * JSON 字符串转对象列表
   *
   * @param json JSON 数组字符串
   * @param elementClass 列表元素类型
   * @param <T> 泛型类型
   * @return 反序列化后的列表，如果 JSON 为空或反序列化失败则返回空列表
   */
  public static <T> List<T> fromJsonArray(String json, Class<T> elementClass) {
    if (json == null || json.isEmpty()) {
      return Collections.emptyList();
    }
    try {
      return staticMapper.readValue(
          json, staticMapper.getTypeFactory().constructCollectionType(List.class, elementClass));
    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("JSON数组反序列化失败: {}", e.getMessage(), e);
      }
      return Collections.emptyList();
    }
  }

  /**
   * JSON 字符串转复杂泛型对象
   *
   * @param json JSON 字符串
   * @param typeReference 类型引用
   * @param <T> 泛型类型
   * @return 反序列化后的对象
   */
  public static <T> T fromJson(String json, TypeReference<T> typeReference) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return staticMapper.readValue(json, typeReference);
    } catch (JsonProcessingException e) {
      if (log.isErrorEnabled()) {
        log.error("JSON反序列化失败: {}", e.getMessage(), e);
      }
      return null;
    }
  }
}
