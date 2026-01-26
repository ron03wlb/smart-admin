package net.lab1024.sa.base.module.support.loginlog;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.loginlog.domain.LoginLogEntity;
import net.lab1024.sa.base.module.support.loginlog.domain.LoginLogQueryForm;
import net.lab1024.sa.base.module.support.loginlog.domain.LoginLogVO;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

/**
 * 登录日志
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/07/22 19:46:23 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLogService {

  private final LoginLogDao loginLogDao;

  /**
   * @author 卓大
   * @description 分页查询
   */
  public ResponseDTO<PageResult<LoginLogVO>> queryByPage(LoginLogQueryForm queryForm) {
    Page page = SmartPageUtil.convert2PageQuery(queryForm);
    List<LoginLogVO> logList = loginLogDao.queryByPage(page, queryForm);
    PageResult<LoginLogVO> pageResult = SmartPageUtil.convert2PageResult(page, logList);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * @author 卓大
   * @description 添加
   */
  public void log(LoginLogEntity loginLogEntity) {
    try {
      loginLogDao.insert(loginLogEntity);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * 查询上一个登录记录
   *
   * @author 卓大
   * @description 查询上一个登录记录
   */
  public LoginLogVO queryLastByUserId(
      Long userId, UserTypeEnum userTypeEnum, LoginLogResultEnum loginLogResultEnum) {
    return loginLogDao.queryLastByUserId(
        userId, userTypeEnum.getValue(), loginLogResultEnum.getValue());
  }
}
