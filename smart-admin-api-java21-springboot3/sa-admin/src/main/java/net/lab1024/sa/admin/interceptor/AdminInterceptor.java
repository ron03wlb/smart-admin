package net.lab1024.sa.admin.interceptor;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.exception.SaTokenException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.strategy.SaAnnotationStrategy;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.system.login.domain.RequestEmployee;
import net.lab1024.sa.admin.module.system.login.service.LoginService;
import net.lab1024.sa.base.core.annoation.NoNeedLogin;
import net.lab1024.sa.base.web.util.SmartRequestUtil;
import net.lab1024.sa.base.web.util.SmartResponseUtil;
import net.lab1024.sa.common.core.code.SystemErrorCode;
import net.lab1024.sa.common.core.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * admin 拦截器
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/7/26 20:20:33 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Component
@Slf4j
public class AdminInterceptor implements HandlerInterceptor {

  private static class SaTokenCode {
    private static final int NO_PERMISSION_1 = 11041;
    private static final int NO_PERMISSION_2 = 11051;
    private static final int LOGIN_ACTIVE_TIMEOUT = 11016;
    private static final int LOGIN_STATE_INVALID_MIN = 11011;
    private static final int LOGIN_STATE_INVALID_MAX = 11015;
  }

  @Resource private LoginService loginService;

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {

    // OPTIONS请求直接return
    if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
      response.setStatus(HttpStatus.NO_CONTENT.value());
      return false;
    }

    boolean isHandler = handler instanceof HandlerMethod;
    if (!isHandler) {
      return true;
    }

    try {
      // --------------- 第一步： 根据token 获取用户 ---------------

      String tokenValue = StpUtil.getTokenValue();
      String loginId = (String) StpUtil.getLoginIdByToken(tokenValue);
      RequestEmployee requestEmployee = loginService.getLoginEmployee(loginId, request);

      // --------------- 第二步： 校验 登录 ---------------

      Method method = ((HandlerMethod) handler).getMethod();
      NoNeedLogin noNeedLogin = ((HandlerMethod) handler).getMethodAnnotation(NoNeedLogin.class);
      if (noNeedLogin != null) {
        updateActiveTimeout(requestEmployee);
        SmartRequestUtil.setRequestUser(requestEmployee);
        return true;
      }

      if (requestEmployee == null) {
        SmartResponseUtil.write(response, ResponseDTO.error(UserErrorCode.LOGIN_STATE_INVALID));
        return false;
      }

      // 更新活跃
      updateActiveTimeout(requestEmployee);

      // --------------- 第三步： 校验 权限 ---------------

      SmartRequestUtil.setRequestUser(requestEmployee);
      if (SaAnnotationStrategy.instance.isAnnotationPresent.apply(method, SaIgnore.class)) {
        return true;
      }

      // 如果是超级管理员的话，不需要校验权限
      if (requestEmployee.getAdministratorFlag()) {
        return true;
      }

      SaAnnotationStrategy.instance.checkMethodAnnotation.accept(method);

    } catch (SaTokenException e) {
      /*
       * sa-token 异常状态码
       * 具体请看： https://sa-token.cc/doc.html#/fun/exception-code
       */
      int code = e.getCode();
      if (code == SaTokenCode.NO_PERMISSION_1 || code == SaTokenCode.NO_PERMISSION_2) {
        SmartResponseUtil.write(response, ResponseDTO.error(UserErrorCode.NO_PERMISSION));
      } else if (code == SaTokenCode.LOGIN_ACTIVE_TIMEOUT) {
        SmartResponseUtil.write(response, ResponseDTO.error(UserErrorCode.LOGIN_ACTIVE_TIMEOUT));
      } else if (code >= SaTokenCode.LOGIN_STATE_INVALID_MIN
          && code <= SaTokenCode.LOGIN_STATE_INVALID_MAX) {
        SmartResponseUtil.write(response, ResponseDTO.error(UserErrorCode.LOGIN_STATE_INVALID));
      } else {
        SmartResponseUtil.write(response, ResponseDTO.error(UserErrorCode.PARAM_ERROR));
      }
      return false;
    } catch (Exception e) {
      SmartResponseUtil.write(response, ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR));
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
      return false;
    }

    // 通过验证
    return true;
  }

  /** 更新活跃时间 */
  private void updateActiveTimeout(RequestEmployee requestEmployee) {
    if (requestEmployee == null) {
      return;
    }
    StpUtil.updateLastActiveToNow();
  }

  @Override
  public void afterCompletion(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
      throws Exception {
    // 清除上下文
    SmartRequestUtil.remove();
  }
}
