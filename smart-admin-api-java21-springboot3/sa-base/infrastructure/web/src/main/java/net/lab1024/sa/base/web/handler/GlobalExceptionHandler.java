package net.lab1024.sa.base.web.handler;

import cn.dev33.satoken.exception.NotPermissionException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.domain.SystemEnvironment;
import net.lab1024.sa.enumeration.SystemEnvironmentEnum;
import net.lab1024.sa.foundation.domain.code.SystemErrorCode;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.exception.BusinessException;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.repeatsubmit.exception.RepeatSubmitException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 全局异常拦截
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2020/8/25 21:57 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final SystemEnvironment systemEnvironment;

  /** json 格式错误 缺少请求体 */
  @ResponseBody
  @ExceptionHandler({HttpMessageNotReadableException.class})
  public ResponseDTO<?> jsonFormatExceptionHandler(Exception e) {
    if (!systemEnvironment.isProd()) {
      if (log.isErrorEnabled()) {
        log.error("全局JSON格式错误异常, URL:{}", getCurrentRequestUrl(), e);
      }
    }
    return ResponseDTO.error(UserErrorCode.PARAM_ERROR, "参数JSON格式错误");
  }

  /** json 格式错误 缺少请求体 */
  @ResponseBody
  @ExceptionHandler({TypeMismatchException.class, BindException.class})
  public ResponseDTO<?> paramExceptionHandler(Exception e) {
    if (e instanceof BindException) {
      if (e instanceof MethodArgumentNotValidException) {
        List<FieldError> fieldErrors =
            ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors();
        List<String> msgList =
            fieldErrors.stream().map(FieldError::getDefaultMessage).collect(Collectors.toList());
        return ResponseDTO.error(UserErrorCode.PARAM_ERROR, String.join(",", msgList));
      }

      List<FieldError> fieldErrors = ((BindException) e).getFieldErrors();
      List<String> error =
          fieldErrors.stream()
              .map(field -> field.getField() + ":" + field.getRejectedValue())
              .collect(Collectors.toList());
      String errorMsg = UserErrorCode.PARAM_ERROR.getMsg() + ":" + error;
      return ResponseDTO.error(UserErrorCode.PARAM_ERROR, errorMsg);
    }
    return ResponseDTO.error(UserErrorCode.PARAM_ERROR);
  }

  /**
   * sa-token 权限异常处理
   *
   * @param e 权限异常
   * @return 错误结果
   */
  @ResponseBody
  @ExceptionHandler(NotPermissionException.class)
  public ResponseDTO<String> permissionException(NotPermissionException e) {
    // 开发环境 方便调试
    if (SystemEnvironmentEnum.PROD != systemEnvironment.getCurrentEnvironment()) {
      return ResponseDTO.error(UserErrorCode.NO_PERMISSION, e.getMessage());
    }
    return ResponseDTO.error(UserErrorCode.NO_PERMISSION);
  }

  /** 业务异常 */
  @ResponseBody
  @ExceptionHandler(BusinessException.class)
  public ResponseDTO<?> businessExceptionHandler(BusinessException e) {
    if (!systemEnvironment.isProd()) {
      if (log.isErrorEnabled()) {
        log.error("全局业务异常, URL:{}", getCurrentRequestUrl(), e);
      }
    }
    return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, e.getMessage());
  }

  /** 重复提交异常 */
  @ResponseBody
  @ExceptionHandler(RepeatSubmitException.class)
  public ResponseDTO<?> repeatSubmitExceptionHandler(RepeatSubmitException e) {
    return ResponseDTO.error(UserErrorCode.REPEAT_SUBMIT);
  }

  /**
   * 其他全部异常
   *
   * @param e 全局异常
   * @return 错误结果
   */
  @ResponseBody
  @ExceptionHandler({Exception.class, Error.class})
  public ResponseDTO<?> errorHandler(Throwable e) {
    if (log.isErrorEnabled()) {
      log.error("捕获全局异常, URL:{}", getCurrentRequestUrl(), e);
    }
    return ResponseDTO.error(
        SystemErrorCode.SYSTEM_ERROR, systemEnvironment.isProd() ? null : e.toString());
  }

  /** 获取当前请求url */
  private String getCurrentRequestUrl() {
    RequestAttributes request = RequestContextHolder.getRequestAttributes();
    if (null == request) {
      return null;
    }
    ServletRequestAttributes servletRequest = (ServletRequestAttributes) request;
    return servletRequest.getRequest().getRequestURI();
  }
}
