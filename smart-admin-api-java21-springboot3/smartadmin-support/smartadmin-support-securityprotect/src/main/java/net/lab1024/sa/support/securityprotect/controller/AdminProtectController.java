package net.lab1024.sa.support.securityprotect.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.domain.Level3ProtectConfigForm;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.support.config.ConfigKeyEnum;
import net.lab1024.sa.support.config.ConfigService;
import net.lab1024.sa.support.securityprotect.domain.form.LoginFailQueryForm;
import net.lab1024.sa.support.securityprotect.domain.vo.LoginFailVO;
import net.lab1024.sa.support.securityprotect.service.Level3ProtectConfigService;
import net.lab1024.sa.support.securityprotect.service.SecurityLoginService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 网络安全
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/17 19:07:27 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@RestController
@Tag(name = SwaggerTagConst.Support.PROTECT)
@RequiredArgsConstructor
public class AdminProtectController extends SupportBaseController {

  private final SecurityLoginService securityLoginService;
  private final Level3ProtectConfigService level3ProtectConfigService;
  private final ConfigService configService;

  @Operation(summary = "分页查询 @author 1024创新实验室-主任-卓大")
  @PostMapping("/protect/loginFail/queryPage")
  public ResponseDTO<PageResult<LoginFailVO>> queryPage(
      @RequestBody @Valid LoginFailQueryForm queryForm) {
    return ResponseDTO.ok(securityLoginService.queryPage(queryForm));
  }

  @Operation(summary = "批量删除 @author 1024创新实验室-主任-卓大")
  @PostMapping("/protect/loginFail/batchDelete")
  public ResponseDTO<String> batchDelete(@RequestBody List<Long> idList) {
    return securityLoginService.batchDelete(idList);
  }

  @Operation(summary = "更新三级等保配置 @author 1024创新实验室-主任-卓大")
  @PostMapping("/protect/level3protect/updateConfig")
  public ResponseDTO<String> updateConfig(@RequestBody @Valid Level3ProtectConfigForm configForm) {
    return level3ProtectConfigService.updateLevel3Config(configForm);
  }

  @Operation(summary = "查询 三级等保配置 @author 1024创新实验室-主任-卓大")
  @GetMapping("/protect/level3protect/getConfig")
  public ResponseDTO<String> getConfig() {
    return ResponseDTO.ok(configService.getConfigValue(ConfigKeyEnum.LEVEL3_PROTECT_CONFIG));
  }
}
