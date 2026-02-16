package net.lab1024.sa.system.login.controller;

import cn.hutool.extra.servlet.JakartaServletUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.captcha.CaptchaVO;
import net.lab1024.sa.common.core.annotation.NoNeedLogin;
import net.lab1024.sa.common.core.domain.constant.RequestHeaderConst;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.token.admin.StpAdminUtil;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.support.securityprotect.service.Level3ProtectConfigService;
import net.lab1024.sa.system.constant.AdminSwaggerTagConst;
import net.lab1024.sa.system.login.domain.LoginForm;
import net.lab1024.sa.system.login.domain.LoginResultVO;
import net.lab1024.sa.system.login.domain.RequestEmployee;
import net.lab1024.sa.system.login.service.LoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 员工登录
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2021-12-15 21:05:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_LOGIN)
public class LoginController {

  private final LoginService loginService;

  private final Level3ProtectConfigService level3ProtectConfigService;

  @NoNeedLogin
  @PostMapping("/login")
  @Operation(summary = "登录 @author 卓大")
  public ResponseDTO<LoginResultVO> login(
      @Valid @RequestBody LoginForm loginForm, HttpServletRequest request) {
    String ip = JakartaServletUtil.getClientIP(request);
    String userAgent =
        JakartaServletUtil.getHeaderIgnoreCase(request, RequestHeaderConst.USER_AGENT);
    return loginService.login(loginForm, ip, userAgent);
  }

  @GetMapping("/login/getLoginInfo")
  @Operation(summary = "获取登录结果信息  @author 卓大")
  public ResponseDTO<LoginResultVO> getLoginInfo() {
    String tokenValue = StpAdminUtil.getTokenValue();
    LoginResultVO loginResult =
        loginService.getLoginResult(
            (RequestEmployee) SmartRequestUtil.getRequestUser(), tokenValue);
    loginResult.setToken(tokenValue);
    return ResponseDTO.ok(loginResult);
  }

  @Operation(summary = "退出登录  @author 卓大")
  @GetMapping("/login/logout")
  public ResponseDTO<String> logout() {
    return loginService.logout(SmartRequestUtil.getRequestUser());
  }

  @Operation(summary = "获取验证码  @author 卓大")
  @GetMapping("/login/getCaptcha")
  @NoNeedLogin
  public ResponseDTO<CaptchaVO> getCaptcha() {
    return loginService.getCaptcha();
  }

  @NoNeedLogin
  @GetMapping("/login/sendEmailCode/{loginName}")
  @Operation(summary = "获取邮箱登录验证码 @author 卓大")
  public ResponseDTO<String> sendEmailCode(@PathVariable String loginName) {
    return loginService.sendEmailCode(loginName);
  }

  @NoNeedLogin
  @GetMapping("/login/getTwoFactorLoginFlag")
  @Operation(summary = "获取双因子登录标识 @author 卓大")
  public ResponseDTO<Boolean> getTwoFactorLoginFlag() {
    // 双因子登录
    boolean twoFactorLoginEnabled = level3ProtectConfigService.isTwoFactorLoginEnabled();
    return ResponseDTO.ok(twoFactorLoginEnabled);
  }
}
