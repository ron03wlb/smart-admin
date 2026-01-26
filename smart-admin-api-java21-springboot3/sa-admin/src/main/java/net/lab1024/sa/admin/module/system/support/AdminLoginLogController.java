package net.lab1024.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.loginlog.LoginLogService;
import net.lab1024.sa.base.module.support.loginlog.domain.LoginLogQueryForm;
import net.lab1024.sa.base.module.support.loginlog.domain.LoginLogVO;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.base.web.util.SmartRequestUtil;
import net.lab1024.sa.foundation.domain.request.RequestUser;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录日志
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/07/22 19:46:23 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = SwaggerTagConst.Support.LOGIN_LOG)
public class AdminLoginLogController extends SupportBaseController {

  private final LoginLogService loginLogService;

  @Operation(summary = "分页查询 @author 卓大")
  @PostMapping("/loginLog/page/query")
  @SaCheckPermission("support:loginLog:query")
  public ResponseDTO<PageResult<LoginLogVO>> queryByPage(@RequestBody LoginLogQueryForm queryForm) {
    return loginLogService.queryByPage(queryForm);
  }

  @Operation(summary = "分页查询当前登录人信息 @author 善逸")
  @PostMapping("/loginLog/page/query/login")
  public ResponseDTO<PageResult<LoginLogVO>> queryByPageLogin(
      @RequestBody LoginLogQueryForm queryForm) {
    RequestUser requestUser = SmartRequestUtil.getRequestUser();
    queryForm.setUserId(requestUser.getUserId());
    queryForm.setUserType(requestUser.getUserType().getValue());
    return loginLogService.queryByPage(queryForm);
  }
}
