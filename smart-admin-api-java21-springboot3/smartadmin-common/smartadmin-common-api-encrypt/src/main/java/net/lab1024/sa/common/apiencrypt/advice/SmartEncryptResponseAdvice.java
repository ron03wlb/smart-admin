package net.lab1024.sa.common.apiencrypt.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.apiencrypt.service.ApiEncryptService;
import net.lab1024.sa.common.core.domain.enumeration.DataTypeEnum;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Smart Admin 加密響應 Advice
 *
 * <p>擴展自 sa-common 的 EncryptResponseAdvice，支持 ResponseDTO 類型
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/24 09:52:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Slf4j
@Component
public class SmartEncryptResponseAdvice extends EncryptResponseAdvice {

  public SmartEncryptResponseAdvice(
      ApiEncryptService apiEncryptService, ObjectMapper objectMapper) {
    super(apiEncryptService, objectMapper);
  }

  @Override
  @SuppressWarnings("unchecked")
  protected Object processEncryption(Object body) {
    if (body instanceof ResponseDTO<?> responseDTO) {
      if (responseDTO.getData() == null) {
        return body;
      }

      try {
        String encrypt =
            getApiEncryptService()
                .encrypt(getObjectMapper().writeValueAsString(responseDTO.getData()));
        ResponseDTO<Object> result = (ResponseDTO<Object>) responseDTO;
        result.setData(encrypt);
        result.setDataType((Integer) DataTypeEnum.ENCRYPT.getValue());
        return result;
      } catch (JsonProcessingException e) {
        log.error("Failed to encrypt response", e);
        throw new RuntimeException(e);
      }
    }

    // 非 ResponseDTO 類型，使用父類預設處理
    return super.processEncryption(body);
  }
}
