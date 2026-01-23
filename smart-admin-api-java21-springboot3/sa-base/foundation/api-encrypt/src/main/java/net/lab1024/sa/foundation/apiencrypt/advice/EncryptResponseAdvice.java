package net.lab1024.sa.foundation.apiencrypt.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.foundation.apiencrypt.annotation.ApiEncrypt;
import net.lab1024.sa.foundation.apiencrypt.service.ApiEncryptService;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * 加密響應 Advice 基礎類
 *
 * <p>此類提供基礎的加密響應功能，子類可以擴展以支持特定的響應類型（如 ResponseDTO）
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/24 09:52:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Slf4j
@ControllerAdvice
public class EncryptResponseAdvice implements ResponseBodyAdvice<Object> {

  @Resource private ApiEncryptService apiEncryptService;

  @Resource private ObjectMapper objectMapper;

  @Override
  public boolean supports(
      MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return returnType.hasMethodAnnotation(ApiEncrypt.class)
        || returnType.getContainingClass().isAnnotationPresent(ApiEncrypt.class);
  }

  @Override
  public Object beforeBodyWrite(
      Object body,
      MethodParameter returnType,
      MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType,
      ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null) {
      return body;
    }

    return processEncryption(body);
  }

  /**
   * 處理加密邏輯
   *
   * <p>子類可以覆寫此方法以提供自定義的加密處理邏輯
   *
   * @param body 響應體
   * @return 處理後的響應體
   */
  protected Object processEncryption(Object body) {
    try {
      String encrypted = apiEncryptService.encrypt(objectMapper.writeValueAsString(body));
      return new EncryptedResponse(encrypted);
    } catch (JsonProcessingException e) {
      log.error("Failed to encrypt response", e);
      throw new RuntimeException(e);
    }
  }

  /**
   * 獲取加密服務
   *
   * @return ApiEncryptService
   */
  protected ApiEncryptService getApiEncryptService() {
    return apiEncryptService;
  }

  /**
   * 獲取 ObjectMapper
   *
   * @return ObjectMapper
   */
  protected ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /** 加密響應包裝類 */
  public static class EncryptedResponse {
    private final String encryptData;

    public EncryptedResponse(String encryptData) {
      this.encryptData = encryptData;
    }

    public String getEncryptData() {
      return encryptData;
    }
  }
}
