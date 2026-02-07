package net.lab1024.sa.common.apiencrypt.advice;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.apiencrypt.annotation.ApiDecrypt;
import net.lab1024.sa.common.apiencrypt.constant.EncryptConst;
import net.lab1024.sa.common.apiencrypt.domain.ApiEncryptForm;
import net.lab1024.sa.common.apiencrypt.service.ApiEncryptService;
import net.lab1024.sa.common.apiencrypt.util.EncryptStringUtil;
import org.apache.commons.io.IOUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

/**
 * 解密
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/21 11:41:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
@SuppressWarnings("PMD.LooseCoupling")
public class DecryptRequestAdvice extends RequestBodyAdviceAdapter {

  private final ApiEncryptService apiEncryptService;

  private final ObjectMapper objectMapper;

  @Override
  public boolean supports(
      MethodParameter methodParameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return methodParameter.hasMethodAnnotation(ApiDecrypt.class)
        || methodParameter.hasParameterAnnotation(ApiDecrypt.class)
        || methodParameter.getContainingClass().isAnnotationPresent(ApiDecrypt.class);
  }

  @Override
  public HttpInputMessage beforeBodyRead(
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    try {
      String bodyStr = IOUtils.toString(inputMessage.getBody(), EncryptConst.CHARSET_UTF8);
      ApiEncryptForm apiEncryptForm = objectMapper.readValue(bodyStr, ApiEncryptForm.class);
      if (EncryptStringUtil.isEmpty(apiEncryptForm.getEncryptData())) {
        return inputMessage;
      }
      String decrypt = apiEncryptService.decrypt(apiEncryptForm.getEncryptData());
      return new DecryptHttpInputMessage(
          inputMessage.getHeaders(), IOUtils.toInputStream(decrypt, EncryptConst.CHARSET_UTF8));
    } catch (IOException e) {
      log.error("", e);
      return inputMessage;
    }
  }

  @Override
  public Object afterBodyRead(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return body;
  }

  @Override
  public Object handleEmptyBody(
      Object body,
      HttpInputMessage inputMessage,
      MethodParameter parameter,
      Type targetType,
      Class<? extends HttpMessageConverter<?>> converterType) {
    return body;
  }

  static class DecryptHttpInputMessage implements HttpInputMessage {
    private final HttpHeaders headers;

    private final InputStream body;

    public DecryptHttpInputMessage(HttpHeaders headers, InputStream body) {
      this.headers = headers;
      this.body = body;
    }

    @Override
    public InputStream getBody() {
      return body;
    }

    @Override
    public HttpHeaders getHeaders() {
      return headers;
    }
  }
}
