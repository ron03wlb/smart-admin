package net.lab1024.sa.system.login.service;

import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import io.vavr.control.Option;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.securityprotect.domain.entity.LoginFailEntity;
import net.lab1024.sa.admin.module.support.securityprotect.service.Level3ProtectConfigService;
import net.lab1024.sa.admin.module.support.securityprotect.service.SecurityLoginService;
import net.lab1024.sa.admin.module.support.securityprotect.service.SecurityPasswordService;
import net.lab1024.sa.common.apiencrypt.service.ApiEncryptService;
import net.lab1024.sa.common.cache.CacheService;
import net.lab1024.sa.common.cache.constant.CacheKeyConst;
import net.lab1024.sa.common.captcha.CaptchaException;
import net.lab1024.sa.common.captcha.CaptchaService;
import net.lab1024.sa.common.captcha.CaptchaVO;
import net.lab1024.sa.common.core.constant.LoginDeviceEnum;
import net.lab1024.sa.common.core.domain.UserPermission;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.constant.RequestHeaderConst;
import net.lab1024.sa.common.core.domain.constant.StringConst;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.ipgeo.util.IpGeolocationUtil;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.core.util.SmartStringUtil;
import net.lab1024.sa.common.validation.util.SmartEnumUtil;
import net.lab1024.sa.support.config.ConfigKeyEnum;
import net.lab1024.sa.support.config.ConfigService;
import net.lab1024.sa.support.loginlog.LoginLogResultEnum;
import net.lab1024.sa.support.loginlog.LoginLogService;
import net.lab1024.sa.support.loginlog.domain.LoginLogEntity;
import net.lab1024.sa.support.loginlog.domain.LoginLogVO;
import net.lab1024.sa.support.mail.constant.MailTemplateCodeEnum;
import net.lab1024.sa.support.mail.service.MailService;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.login.domain.LoginForm;
import net.lab1024.sa.system.login.domain.LoginResultVO;
import net.lab1024.sa.system.login.domain.RequestEmployee;
import net.lab1024.sa.system.login.manager.LoginManager;
import net.lab1024.sa.system.menu.domain.entity.MenuEntity;
import net.lab1024.sa.system.menu.domain.vo.MenuVO;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.dao.RoleMenuDao;
import net.lab1024.sa.system.role.domain.vo.RoleVO;
import org.springframework.stereotype.Service;

/**
 * 登录
 *
 * @author 1024创新实验室: 卓大
 * @since 2025-05-03 22:56:34 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class LoginService implements StpInterface {

  /** 万能密码的 sa token loginId 前缀 */
  private static final String SUPER_PASSWORD_LOGIN_ID_PREFIX = "S";

  private final EmployeeDao employeeDao;

  private final CaptchaService captchaService;

  private final ConfigService configService;

  private final LoginLogService loginLogService;

  private final RoleEmployeeDao roleEmployeeDao;

  private final RoleMenuDao roleMenuDao;

  private final SecurityLoginService securityLoginService;

  private final SecurityPasswordService protectPasswordService;

  private final ApiEncryptService apiEncryptService;

  private final Level3ProtectConfigService level3ProtectConfigService;

  private final MailService mailService;

  private final CacheService cacheService;

  private final LoginManager loginManager;

  /** 获取验证码 */
  public ResponseDTO<CaptchaVO> getCaptcha() {
    return ResponseDTO.ok(captchaService.generateCaptcha());
  }

  /**
   * 员工登录
   *
   * @return 返回用户登录信息
   */
  public ResponseDTO<LoginResultVO> login(LoginForm loginForm, String ip, String userAgent) {

    LoginDeviceEnum loginDeviceEnum =
        SmartEnumUtil.getEnumByValue(loginForm.getLoginDevice(), LoginDeviceEnum.class);
    if (loginDeviceEnum == null) {
      return ResponseDTO.userErrorParam("登录设备暂不支持！");
    }

    // 校验 图形验证码
    try {
      captchaService.checkCaptcha(loginForm);
    } catch (CaptchaException e) {
      return ResponseDTO.error(UserErrorCode.PARAM_ERROR, e.getMessage());
    }

    // 验证登录名
    EmployeeEntity employeeEntity = employeeDao.getByLoginName(loginForm.getLoginName(), false);
    if (null == employeeEntity) {
      return ResponseDTO.userErrorParam("登录名或密码错误！");
    }

    // 验证账号状态
    if (employeeEntity.getDeletedFlag()) {
      saveLoginLog(
          employeeEntity, ip, userAgent, "账号已删除", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
      return ResponseDTO.userErrorParam("您的账号已被删除,请联系工作人员！");
    }

    if (employeeEntity.getDisabledFlag()) {
      saveLoginLog(
          employeeEntity, ip, userAgent, "账号已禁用", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
      return ResponseDTO.userErrorParam("您的账号已被禁用,请联系工作人员！");
    }

    // 解密前端加密的密码（使用 Vavr Option 模式確保安全）
    io.vavr.control.Option<String> decryptedPasswordOpt =
        Option.of(apiEncryptService.decrypt(loginForm.getPassword()));
    if (!decryptedPasswordOpt.isDefined()) {
      saveLoginLog(
          employeeEntity, ip, userAgent, "密码解密失败", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
      return ResponseDTO.userErrorParam("密码解密失败，请重试");
    }
    String requestPassword = decryptedPasswordOpt.get();

    // 验证密码 是否为万能密码
    String superPassword = configService.getConfigValue(ConfigKeyEnum.SUPER_PASSWORD);
    boolean superPasswordFlag = superPassword.equals(requestPassword);

    // 校验双因子登录
    ResponseDTO<String> validateEmailCode =
        validateEmailCode(loginForm, employeeEntity, superPasswordFlag);
    if (!validateEmailCode.getOk()) {
      return ResponseDTO.error(validateEmailCode);
    }

    // 万能密码特殊操作
    if (superPasswordFlag) {

      // 对于万能密码：受限制sa token 要求loginId唯一，万能密码只能插入一段uuid
      String saTokenLoginId =
          SUPER_PASSWORD_LOGIN_ID_PREFIX
              + StringConst.COLON
              + UUID.randomUUID().toString().replace("-", "")
              + StringConst.COLON
              + employeeEntity.getEmployeeId();
      // 万能密码登录只能登录30分钟
      StpUtil.login(saTokenLoginId, 1800);

    } else {

      // 按照等保登录要求，进行登录失败次数校验
      ResponseDTO<LoginFailEntity> loginFailEntityResponseDTO =
          securityLoginService.checkLogin(
              employeeEntity.getEmployeeId(), UserTypeEnum.ADMIN_EMPLOYEE);
      if (!loginFailEntityResponseDTO.getOk()) {
        return ResponseDTO.error(loginFailEntityResponseDTO);
      }

      // 密码错误
      String saltPassword =
          requestPassword
              + StringConst.UNDERLINE
              + employeeEntity.getEmployeeUid().toUpperCase(Locale.ROOT)
              + StringConst.UNDERLINE
              + employeeEntity.getEmployeeUid().toLowerCase(Locale.ROOT);
      if (!protectPasswordService.matchesPwd(saltPassword, employeeEntity.getLoginPwd())) {
        // 记录登录失败
        saveLoginLog(
            employeeEntity, ip, userAgent, "密码错误", LoginLogResultEnum.LOGIN_FAIL, loginDeviceEnum);
        // 记录等级保护次数
        String msg =
            securityLoginService.recordLoginFail(
                employeeEntity.getEmployeeId(),
                UserTypeEnum.ADMIN_EMPLOYEE,
                employeeEntity.getLoginName(),
                loginFailEntityResponseDTO.getData());
        return msg == null
            ? ResponseDTO.userErrorParam("登录名或密码错误！")
            : ResponseDTO.error(UserErrorCode.LOGIN_FAIL_WILL_LOCK, msg);
      }

      String saTokenLoginId =
          UserTypeEnum.ADMIN_EMPLOYEE.getValue()
              + StringConst.COLON
              + employeeEntity.getEmployeeId();

      // 登录
      StpUtil.login(saTokenLoginId, String.valueOf(loginDeviceEnum.getDesc()));

      // 移除邮箱验证码
      deleteEmailCode(employeeEntity.getEmployeeId());
    }

    // 获取员工信息
    RequestEmployee requestEmployee = loginManager.loadLoginInfo(employeeEntity);

    // 移除登录失败
    securityLoginService.removeLoginFail(
        employeeEntity.getEmployeeId(), UserTypeEnum.ADMIN_EMPLOYEE);

    // 获取登录结果信息
    String token = StpUtil.getTokenValue();
    LoginResultVO loginResultVO = getLoginResult(requestEmployee, token);

    // 保存登录记录
    saveLoginLog(
        employeeEntity,
        ip,
        userAgent,
        superPasswordFlag ? "万能密码登录" : StringConst.EMPTY,
        LoginLogResultEnum.LOGIN_SUCCESS,
        loginDeviceEnum);

    // 设置 token
    loginResultVO.setToken(token);

    // 更新用户权限
    loginManager.loadUserPermission(employeeEntity.getEmployeeId());

    return ResponseDTO.ok(loginResultVO);
  }

  /** 获取登录结果信息 */
  public LoginResultVO getLoginResult(RequestEmployee requestEmployee, String token) {

    // 基础信息
    LoginResultVO loginResultVO = SmartBeanUtil.copy(requestEmployee, LoginResultVO.class);

    // 前端菜单和功能点清单
    List<RoleVO> roleList = roleEmployeeDao.selectRoleByEmployeeId(requestEmployee.getEmployeeId());
    List<MenuVO> menuAndPointsList =
        getMenuList(
            roleList.stream().map(RoleVO::getRoleId).collect(Collectors.toList()),
            requestEmployee.getAdministratorFlag());
    loginResultVO.setMenuList(menuAndPointsList);

    // 上次登录信息
    LoginLogVO loginLogVO =
        loginLogService.queryLastByUserId(
            requestEmployee.getEmployeeId(),
            UserTypeEnum.ADMIN_EMPLOYEE,
            LoginLogResultEnum.LOGIN_SUCCESS);
    if (loginLogVO != null) {
      loginResultVO.setLastLoginIp(loginLogVO.getLoginIp());
      loginResultVO.setLastLoginIpRegion(loginLogVO.getLoginIpRegion());
      loginResultVO.setLastLoginTime(loginLogVO.getCreateTime());
      loginResultVO.setLastLoginUserAgent(loginLogVO.getUserAgent());
    }

    // 是否需要强制修改密码
    boolean needChangePasswordFlag =
        protectPasswordService.checkNeedChangePassword(
            requestEmployee.getUserType().getValue(), requestEmployee.getUserId());
    loginResultVO.setNeedUpdatePwdFlag(needChangePasswordFlag);

    // 万能密码登录，则不需要设置强制修改密码
    String loginIdByToken = (String) StpUtil.getLoginIdByToken(token);
    if (loginIdByToken != null && loginIdByToken.startsWith(SUPER_PASSWORD_LOGIN_ID_PREFIX)) {
      loginResultVO.setNeedUpdatePwdFlag(false);
    }

    return loginResultVO;
  }

  /** 根据登录token 获取员请求工信息 - P1 Fix: 使用 Vavr Option 處理 null 安全 */
  public RequestEmployee getLoginEmployee(String loginId, HttpServletRequest request) {
    return Option.of(loginId)
        .flatMap(id -> Option.of(getEmployeeIdByLoginId(id)))
        .flatMap(employeeId -> Option.of(loginManager.getRequestEmployee(employeeId)))
        .peek(
            requestEmployee -> {
              // 更新请求ip和user agent
              requestEmployee.setUserAgent(
                  JakartaServletUtil.getHeaderIgnoreCase(request, RequestHeaderConst.USER_AGENT));
              requestEmployee.setIp(JakartaServletUtil.getClientIP(request));
            })
        .getOrNull();
  }

  /**
   * 根据 loginId 获取员工id（使用 Vavr 函數式風格）
   *
   * <p>使用 Vavr Option/Try 模式統一錯誤處理，避免 null 返回和異常捕獲
   *
   * @param loginId 登錄 ID
   * @return 員工 ID（可能為 null）
   */
  Long getEmployeeIdByLoginId(String loginId) {
    return Option.of(loginId)
        .flatMap(this::parseEmployeeIdStr)
        .flatMap(this::parseEmployeeId)
        .getOrNull();
  }

  /**
   * 從 loginId 中提取員工 ID 字符串（使用 Vavr Option 模式）
   *
   * @param loginId 登錄 ID
   * @return 員工 ID 字符串的 Option
   */
  private Option<String> parseEmployeeIdStr(String loginId) {
    // 萬能密碼登錄格式: S:uuid:employeeId
    if (loginId.startsWith(SUPER_PASSWORD_LOGIN_ID_PREFIX)) {
      return parseSuperPasswordLoginId(loginId);
    }
    // 普通登錄格式: userType:employeeId
    return parseNormalLoginId(loginId);
  }

  /**
   * 解析萬能密碼登錄 ID
   *
   * @param loginId 登錄 ID
   * @return 員工 ID 字符串的 Option
   */
  private Option<String> parseSuperPasswordLoginId(String loginId) {
    return io.vavr.control.Try.of(
            () -> {
              String[] parts = loginId.split(StringConst.COLON);
              if (parts.length < 3) {
                throw new IllegalArgumentException("Invalid super password loginId format");
              }
              return parts[2];
            })
        .onFailure(e -> log.error("Invalid super password loginId format: {}", loginId, e))
        .toOption();
  }

  /**
   * 解析普通登錄 ID
   *
   * @param loginId 登錄 ID
   * @return 員工 ID 字符串的 Option
   */
  private Option<String> parseNormalLoginId(String loginId) {
    return io.vavr.control.Try.of(
            () -> {
              // 檢查長度
              if (loginId.length() <= 2) {
                throw new IllegalArgumentException("LoginId too short (need at least 3 chars)");
              }
              // 檢查冒號分隔符
              int colonIndex = loginId.indexOf(StringConst.COLON);
              if (colonIndex <= 0 || colonIndex >= loginId.length() - 1) {
                throw new IllegalArgumentException("Missing or invalid colon separator");
              }
              // 從冒號後提取 employeeId
              return loginId.substring(colonIndex + 1);
            })
        .onFailure(e -> log.error("Invalid normal loginId format: {}", loginId, e))
        .toOption();
  }

  /**
   * 將員工 ID 字符串解析為 Long（使用 Vavr Try 模式）
   *
   * @param employeeIdStr 員工 ID 字符串
   * @return Long 類型員工 ID 的 Option
   */
  private Option<Long> parseEmployeeId(String employeeIdStr) {
    return io.vavr.control.Try.of(() -> Long.parseLong(employeeIdStr))
        .onFailure(e -> log.error("Failed to parse employeeId: {}", employeeIdStr, e))
        .toOption();
  }

  /** 退出登录 */
  public ResponseDTO<String> logout(RequestUser requestUser) {

    // sa token 登出
    StpUtil.logout();

    // 清除用户登录信息缓存和权限信息
    this.clearLoginEmployeeCache(requestUser.getUserId());

    // 保存登出日志
    LoginLogEntity loginEntity =
        LoginLogEntity.builder()
            .userId(requestUser.getUserId())
            .userType(requestUser.getUserType().getValue())
            .userName(requestUser.getUserName())
            .userAgent(requestUser.getUserAgent())
            .loginIp(requestUser.getIp())
            .loginIpRegion(IpGeolocationUtil.getRegion(requestUser.getIp()))
            .loginResult(LoginLogResultEnum.LOGIN_OUT.getValue())
            .createTime(LocalDateTime.now())
            .build();
    loginLogService.log(loginEntity);

    return ResponseDTO.ok();
  }

  /** 保存登录日志 */
  private void saveLoginLog(
      EmployeeEntity employeeEntity,
      String ip,
      String userAgent,
      String remark,
      LoginLogResultEnum result,
      LoginDeviceEnum loginDeviceEnum) {
    LoginLogEntity loginEntity =
        LoginLogEntity.builder()
            .userId(employeeEntity.getEmployeeId())
            .userType(UserTypeEnum.ADMIN_EMPLOYEE.getValue())
            .userName(employeeEntity.getActualName())
            .userAgent(userAgent)
            .loginIp(ip)
            .loginIpRegion(IpGeolocationUtil.getRegion(ip))
            .remark(remark)
            .loginDevice(loginDeviceEnum.getDesc())
            .loginResult(result.getValue())
            .createTime(LocalDateTime.now())
            .build();
    loginLogService.log(loginEntity);
  }

  @Override
  public List<String> getPermissionList(Object loginId, String loginType) {
    Long employeeId = this.getEmployeeIdByLoginId((String) loginId);
    if (employeeId == null) {
      return Collections.emptyList();
    }

    UserPermission userPermission = loginManager.getUserPermission(employeeId);
    return userPermission.getPermissionList();
  }

  @Override
  public List<String> getRoleList(Object loginId, String loginType) {
    Long employeeId = this.getEmployeeIdByLoginId((String) loginId);
    if (employeeId == null) {
      return Collections.emptyList();
    }

    UserPermission userPermission = loginManager.getUserPermission(employeeId);
    return userPermission.getRoleList();
  }

  /** 发送 邮箱 验证码 */
  public ResponseDTO<String> sendEmailCode(String loginName) {

    // P1 Fix: 統一驗證登錄名，防止 SQL 注入和惡意輸入
    if (SmartStringUtil.isBlank(loginName) || !loginName.matches("^[a-zA-Z0-9_-]{3,50}$")) {
      return ResponseDTO.userErrorParam("登錄名必須為 3-50 個字符，僅包含字母、數字、下劃線和連字符");
    }

    // 开启双因子登录
    if (!level3ProtectConfigService.isTwoFactorLoginEnabled()) {
      return ResponseDTO.userErrorParam("无需使用邮箱验证码");
    }

    // 验证登录名
    EmployeeEntity employeeEntity = employeeDao.getByLoginName(loginName, false);
    if (null == employeeEntity) {
      return ResponseDTO.ok();
    }

    // 验证账号状态
    if (employeeEntity.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("您的账号已被删除,请联系工作人员！");
    }

    if (employeeEntity.getDisabledFlag()) {
      return ResponseDTO.userErrorParam("您的账号已被禁用,请联系工作人员！");
    }

    String mail = employeeEntity.getEmail();
    if (SmartStringUtil.isBlank(mail)) {
      return ResponseDTO.userErrorParam("您暂未配置邮箱地址，请联系管理员配置邮箱");
    }

    // 校验验证码发送时间，60秒内不能重复发生
    String cacheKey = UserTypeEnum.ADMIN_EMPLOYEE.getValue() + ":" + employeeEntity.getEmployeeId();
    Option<String> emailCodeOpt =
        cacheService.get(CacheKeyConst.Support.LOGIN_VERIFICATION_CODE, cacheKey, String.class);
    String emailCode = emailCodeOpt.getOrNull();
    long sendCodeTimeMills = -1;
    if (!SmartStringUtil.isEmpty(emailCode)) {
      String[] codeParts = emailCode.split(StringConst.UNDERLINE);
      if (codeParts.length < 2) {
        // P1 Fix: 清除無效緩存並重置，允許重新發送
        log.error("Invalid email code format in cache: {}", emailCode);
        cacheService.remove(CacheKeyConst.Support.LOGIN_VERIFICATION_CODE, cacheKey);
        sendCodeTimeMills = -1;
      } else {
        sendCodeTimeMills = NumberUtil.parseLong(codeParts[1]);
      }
    }

    if (System.currentTimeMillis() - sendCodeTimeMills < 60 * 1000) {
      return ResponseDTO.userErrorParam("邮箱验证码已发送，一分钟内请勿重复发送");
    }

    // 生成验证码
    long currentTimeMillis = System.currentTimeMillis();
    String verificationCode = RandomUtil.randomNumbers(4);
    cacheService.put(
        CacheKeyConst.Support.LOGIN_VERIFICATION_CODE,
        cacheKey,
        verificationCode + StringConst.UNDERLINE + currentTimeMillis,
        300,
        TimeUnit.SECONDS);

    // 发送邮件验证码
    Map<String, Object> mailParams = new HashMap<>();
    mailParams.put("code", verificationCode);
    return mailService.sendMail(
        MailTemplateCodeEnum.LOGIN_VERIFICATION_CODE,
        mailParams,
        Collections.singletonList(employeeEntity.getEmail()));
  }

  /** 校验邮箱验证码 */
  private ResponseDTO<String> validateEmailCode(
      LoginForm loginForm, EmployeeEntity employeeEntity, boolean superPasswordFlag) {
    // 万能密码则不校验
    if (superPasswordFlag) {
      return ResponseDTO.ok();
    }

    // 未开启双因子登录
    if (!level3ProtectConfigService.isTwoFactorLoginEnabled()) {
      return ResponseDTO.ok();
    }

    if (SmartStringUtil.isEmpty(loginForm.getEmailCode())) {
      return ResponseDTO.userErrorParam("请输入邮箱验证码");
    }

    // 校验验证码
    String cacheKey = UserTypeEnum.ADMIN_EMPLOYEE.getValue() + ":" + employeeEntity.getEmployeeId();
    Option<String> emailCodeOpt =
        cacheService.get(CacheKeyConst.Support.LOGIN_VERIFICATION_CODE, cacheKey, String.class);
    String emailCode = emailCodeOpt.getOrNull();
    if (SmartStringUtil.isEmpty(emailCode)) {
      return ResponseDTO.userErrorParam("邮箱验证码已失效，请重新发送");
    }

    String[] codeParts = emailCode.split(StringConst.UNDERLINE);
    if (codeParts.length < 1) {
      log.error("Invalid email code format in cache: {}", emailCode);
      return ResponseDTO.userErrorParam("验证码格式错误，请重新发送");
    }

    if (!codeParts[0].equals(loginForm.getEmailCode().trim())) {
      return ResponseDTO.userErrorParam("邮箱验证码错误，请重新填写");
    }

    return ResponseDTO.ok();
  }

  /** 移除邮箱验证码 */
  private void deleteEmailCode(Long employeeId) {
    String cacheKey = UserTypeEnum.ADMIN_EMPLOYEE.getValue() + ":" + employeeId;
    cacheService.remove(CacheKeyConst.Support.LOGIN_VERIFICATION_CODE, cacheKey);
  }

  public void clearLoginEmployeeCache(Long employeeId) {
    loginManager.clearUserPermission(employeeId);
    loginManager.clearUserLoginInfo(employeeId);
  }

  /** 根据角色id集合，查询其所有的菜单权限 */
  private List<MenuVO> getMenuList(List<Long> roleIdList, Boolean administratorFlag) {
    // 管理员返回所有菜单
    if (administratorFlag) {
      List<MenuEntity> menuEntityList =
          roleMenuDao.selectMenuListByRoleIdList(new ArrayList<>(), false);
      return SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
    }
    // 非管理员 无角色 返回空菜单
    if (roleIdList == null || roleIdList.isEmpty()) {
      return new ArrayList<>();
    }
    List<MenuEntity> menuEntityList = roleMenuDao.selectMenuListByRoleIdList(roleIdList, false);
    return SmartBeanUtil.copyList(menuEntityList, MenuVO.class);
  }
}
