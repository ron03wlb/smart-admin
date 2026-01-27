package net.lab1024.sa.admin.module.support.securityprotect.service;

import io.vavr.control.Option;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.support.securityprotect.dao.PasswordLogDao;
import net.lab1024.sa.admin.module.support.securityprotect.domain.entity.PasswordLogEntity;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.foundation.domain.request.RequestUser;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.securityprotect.constant.SecurityConst;
import net.lab1024.sa.foundation.securityprotect.service.PasswordComplexityService;
import net.lab1024.sa.foundation.securityprotect.service.PasswordEncryptService;
import net.lab1024.sa.foundation.securityprotect.service.SecurityConfigProvider;
import org.springframework.stereotype.Service;

/**
 * 三级等保 密码 相关
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/11 19:25:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityPasswordService {

  private final PasswordEncryptService passwordEncryptService;
  private final PasswordComplexityService passwordComplexityService;
  private final SecurityConfigProvider securityConfigProvider;
  private final PasswordLogDao passwordLogDao;

  /**
   * 校验密码复杂度
   *
   * @param password 密码
   * @return 校验结果
   */
  public ResponseDTO<String> validatePasswordComplexity(String password) {
    Option<String> errorMsg =
        passwordComplexityService.validateComplexity(
            password, securityConfigProvider.isPasswordComplexityEnabled());
    return errorMsg.map(ResponseDTO::<String>userErrorParam).getOrElse(() -> ResponseDTO.ok());
  }

  /**
   * 校验密码重复次数
   *
   * @param requestUser 请求用户
   * @param newPassword 新密码
   * @return 校验结果
   */
  public ResponseDTO<String> validatePasswordRepeatTimes(
      RequestUser requestUser, String newPassword) {

    // 密码重复次数小于1 无需校验
    if (securityConfigProvider.getRegularChangePasswordNotAllowRepeatTimes()
        < SecurityConst.MIN_ALLOWED_REPEAT_TIMES) {
      return ResponseDTO.ok();
    }

    // 检查最近几次是否有重复密码（使用 Vavr Option 安全處理 getUserType()）
    Integer userTypeValue =
        io.vavr.control.Option.of(requestUser.getUserType())
            .map(userType -> userType.getValue())
            .getOrElse(
                () -> {
                  log.error("User type is null for user: {}", requestUser.getUserId());
                  // 默認使用管理員員工類型
                  return UserTypeEnum.ADMIN_EMPLOYEE.getValue();
                });

    List<String> oldPasswords =
        passwordLogDao.selectOldPassword(
            userTypeValue,
            requestUser.getUserId(),
            securityConfigProvider.getRegularChangePasswordNotAllowRepeatTimes());
    boolean isDuplicate =
        oldPasswords.stream()
            .anyMatch(oldPassword -> passwordEncryptService.matches(newPassword, oldPassword));
    if (isDuplicate) {
      return ResponseDTO.userErrorParam(
          String.format(
              "与前%d个历史密码重复，请换个密码!",
              securityConfigProvider.getRegularChangePasswordNotAllowRepeatTimes()));
    }

    return ResponseDTO.ok();
  }

  /**
   * 随机生成密码
   *
   * @return 随机密码
   */
  public String randomPassword() {
    return passwordComplexityService.generateRandomPassword(
        securityConfigProvider.isPasswordComplexityEnabled());
  }

  /**
   * 保存修改密码日志
   *
   * @param requestUser 请求用户
   * @param newPassword 新密码（已加密）
   * @param oldPassword 旧密码（已加密）
   */
  public void saveUserChangePasswordLog(
      RequestUser requestUser, String newPassword, String oldPassword) {

    PasswordLogEntity passwordLogEntity = new PasswordLogEntity();
    passwordLogEntity.setNewPassword(newPassword);
    passwordLogEntity.setOldPassword(oldPassword);
    passwordLogEntity.setUserId(requestUser.getUserId());
    passwordLogEntity.setUserType(requestUser.getUserType().getValue());
    passwordLogDao.insert(passwordLogEntity);
  }

  /**
   * 检查是否需要修改密码
   *
   * @param userType 用户类型
   * @param userId 用户ID
   * @return true 需要修改, false 不需要
   */
  public boolean checkNeedChangePassword(Integer userType, Long userId) {

    if (securityConfigProvider.getRegularChangePasswordDays()
        < SecurityConst.MIN_CHANGE_PASSWORD_DAYS) {
      return false;
    }

    PasswordLogEntity passwordLogEntity =
        passwordLogDao.selectLastByUserTypeAndUserId(userType, userId);
    if (passwordLogEntity == null) {
      return false;
    }

    LocalDateTime nextUpdateTime =
        passwordLogEntity
            .getCreateTime()
            .plusDays(securityConfigProvider.getRegularChangePasswordDays());
    return nextUpdateTime.isBefore(LocalDateTime.now());
  }

  /**
   * 获取加密后的密码
   *
   * @param password 原始密码
   * @return 加密后的密码
   */
  public String getEncryptPwd(String password) {
    return passwordEncryptService.encrypt(password);
  }

  /**
   * 校验密码是否匹配
   *
   * @param password 原始密码
   * @param encodedPassword 加密后的密码
   * @return true 匹配, false 不匹配
   */
  public boolean matchesPwd(String password, String encodedPassword) {
    return passwordEncryptService.matches(password, encodedPassword);
  }
}
