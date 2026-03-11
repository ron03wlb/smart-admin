package net.lab1024.sa.support.securityprotect.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.security.constant.SecurityConst;
import net.lab1024.sa.common.security.service.SecurityConfigProvider;
import net.lab1024.sa.support.securityprotect.dao.LoginFailDao;
import net.lab1024.sa.support.securityprotect.domain.entity.LoginFailEntity;
import net.lab1024.sa.support.securityprotect.domain.form.LoginFailQueryForm;
import net.lab1024.sa.support.securityprotect.domain.vo.LoginFailVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 三级等保 登录 相关
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/11 19:25:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Service
@RequiredArgsConstructor
public class SecurityLoginService {

  private static final String LOGIN_LOCK_MSG = "您已连续登录失败%s次，账号锁定%s分钟，解锁时间为：%s，请您耐心等待！";

  private static final String LOGIN_FAIL_MSG = "登录名或密码错误！连续登录失败%s次，账号将锁定%s分钟！您还可以再尝试%s次！";

  private final SecurityConfigProvider securityConfigProvider;
  private final LoginFailDao loginFailDao;

  /**
   * 检查是否可以登录
   *
   * @param userId 用户ID
   * @param userType 用户类型
   * @return 检查结果
   */
  public ResponseDTO<LoginFailEntity> checkLogin(Long userId, UserTypeEnum userType) {

    // 若登录最大失败次数小于1，无需校验
    if (securityConfigProvider.getLoginFailMaxTimes() < SecurityConst.MIN_FAIL_TIMES_THRESHOLD) {
      return ResponseDTO.ok();
    }

    LoginFailEntity loginFailEntity =
        loginFailDao.selectByUserIdAndUserType(userId, userType.getValue());
    if (loginFailEntity == null) {
      return ResponseDTO.ok();
    }

    // 校验登录失败次数
    if (loginFailEntity.getLoginFailCount() < securityConfigProvider.getLoginFailMaxTimes()) {
      return ResponseDTO.ok(loginFailEntity);
    }

    // 校验是否锁定
    if (loginFailEntity.getLoginLockBeginTime() == null) {
      return ResponseDTO.ok(loginFailEntity);
    }

    // 校验锁定时长
    if (loginFailEntity
        .getLoginLockBeginTime()
        .plusSeconds(securityConfigProvider.getLoginFailLockSeconds())
        .isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
      // 过了锁定时间
      return ResponseDTO.ok(loginFailEntity);
    }

    OffsetDateTime unlockTime =
        loginFailEntity
            .getLoginLockBeginTime()
            .plusSeconds(securityConfigProvider.getLoginFailLockSeconds());
    return ResponseDTO.error(
        UserErrorCode.LOGIN_FAIL_LOCK,
        String.format(
            LOGIN_LOCK_MSG,
            loginFailEntity.getLoginFailCount(),
            securityConfigProvider.getLoginFailLockSeconds() / 60,
            unlockTime.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
  }

  /**
   * 登录失败后记录
   *
   * @param userId 用户ID
   * @param userType 用户类型
   * @param loginName 登录名
   * @param tenantId 租户ID
   * @param loginFailEntity 登录失败实体
   * @return 提示消息
   */
  public String recordLoginFail(
      Long userId,
      UserTypeEnum userType,
      String loginName,
      Long tenantId,
      LoginFailEntity loginFailEntity) {

    // 若登录最大失败次数小于1，无需记录
    if (securityConfigProvider.getLoginFailMaxTimes() < SecurityConst.MIN_FAIL_TIMES_THRESHOLD) {
      return null;
    }

    // 登录失败
    int loginFailCount = loginFailEntity == null ? 1 : loginFailEntity.getLoginFailCount() + 1;
    boolean lockFlag = loginFailCount >= securityConfigProvider.getLoginFailMaxTimes();
    OffsetDateTime lockBeginTime = lockFlag ? OffsetDateTime.now(ZoneOffset.UTC) : null;

    LoginFailEntity loginFail = loginFailEntity;
    if (loginFail == null) {
      loginFail =
          LoginFailEntity.builder()
              .userId(userId)
              .userType(userType.getValue())
              .loginName(loginName)
              .tenantId(tenantId)
              .loginFailCount(loginFailCount)
              .lockFlag(lockFlag)
              .loginLockBeginTime(lockBeginTime)
              .build();
      loginFailDao.insert(loginFail);
    } else {
      loginFail.setLoginLockBeginTime(lockBeginTime);
      loginFail.setLoginFailCount(loginFailCount);
      loginFail.setLockFlag(lockFlag);
      loginFail.setLoginName(loginName);
      loginFailDao.updateById(loginFail);
    }

    // 提示信息
    if (lockFlag) {
      OffsetDateTime unlockTime =
          loginFail
              .getLoginLockBeginTime()
              .plusSeconds(securityConfigProvider.getLoginFailLockSeconds());
      return String.format(
          LOGIN_LOCK_MSG,
          loginFail.getLoginFailCount(),
          securityConfigProvider.getLoginFailLockSeconds() / 60,
          unlockTime.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    } else {
      return String.format(
          LOGIN_FAIL_MSG,
          securityConfigProvider.getLoginFailMaxTimes(),
          securityConfigProvider.getLoginFailLockSeconds() / 60,
          securityConfigProvider.getLoginFailMaxTimes() - loginFail.getLoginFailCount());
    }
  }

  /**
   * 清除登录失败
   *
   * @param userId 用户ID
   * @param userType 用户类型
   */
  public void removeLoginFail(Long userId, UserTypeEnum userType) {

    // 若登录最大失败次数小于1，无需校验
    if (securityConfigProvider.getLoginFailMaxTimes() < SecurityConst.MIN_FAIL_TIMES_THRESHOLD) {
      return;
    }

    loginFailDao.deleteByUserIdAndUserType(userId, userType.getValue());
  }

  /**
   * 分页查询
   *
   * @param queryForm 查询表单
   * @return 分页结果
   */
  public PageResult<LoginFailVO> queryPage(LoginFailQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<LoginFailVO> list = loginFailDao.queryPage(page, queryForm);
    return SmartPageUtil.convert2PageResult(page, list);
  }

  /**
   * 批量删除
   *
   * @param idList ID列表
   * @return 操作结果
   */
  public ResponseDTO<String> batchDelete(List<Long> idList) {
    if (CollectionUtils.isEmpty(idList)) {
      return ResponseDTO.ok();
    }

    loginFailDao.deleteBatchIds(idList);
    return ResponseDTO.ok();
  }
}
