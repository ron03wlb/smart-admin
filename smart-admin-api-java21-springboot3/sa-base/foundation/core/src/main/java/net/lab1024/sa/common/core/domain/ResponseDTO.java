package net.lab1024.sa.common.core.domain;

import net.lab1024.sa.foundation.domain.code.ErrorCode;

/**
 * @deprecated Use {@link net.lab1024.sa.foundation.domain.response.ResponseDTO} instead.
 */
@Deprecated(since = "3.6.0", forRemoval = true)
public class ResponseDTO<T> extends net.lab1024.sa.foundation.domain.response.ResponseDTO<T> {

  public ResponseDTO(Integer code, String level, boolean success, String msg, T data) {
    super(code, level, success, msg, data);
  }

  public ResponseDTO(Integer code, String level, boolean success, String msg) {
    super(code, level, success, msg);
  }

  public ResponseDTO(ErrorCode errorCode, boolean success, String msg, T data) {
    super((net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, success, msg, data);
  }

  @SuppressWarnings("PMD.ShortMethodName")
  public static <T> ResponseDTO<T> ok() {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.ok();
  }

  @SuppressWarnings("PMD.ShortMethodName")
  public static <T> ResponseDTO<T> ok(T data) {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.ok(data);
  }

  public static <T> ResponseDTO<T> okMsg(String msg) {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.okMsg(msg);
  }

  public static <T> ResponseDTO<T> userErrorParam() {
    return (ResponseDTO<T>) net.lab1024.sa.foundation.domain.response.ResponseDTO.userErrorParam();
  }

  public static <T> ResponseDTO<T> userErrorParam(String msg) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.userErrorParam(msg);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode, boolean success) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, success);
  }

  public static <T> ResponseDTO<T> error(ResponseDTO<?> responseDTO) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.response.ResponseDTO<?>) responseDTO);
  }

  public static <T> ResponseDTO<T> error(ErrorCode errorCode, String msg) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.error(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, msg);
  }

  public static <T> ResponseDTO<T> errorData(ErrorCode errorCode, T data) {
    return (ResponseDTO<T>)
        net.lab1024.sa.foundation.domain.response.ResponseDTO.errorData(
            (net.lab1024.sa.foundation.domain.code.ErrorCode) errorCode, data);
  }
}
