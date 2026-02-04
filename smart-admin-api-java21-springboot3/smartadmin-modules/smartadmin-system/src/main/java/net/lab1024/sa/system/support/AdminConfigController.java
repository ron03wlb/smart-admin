package net.lab1024.sa.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.support.config.ConfigService;
import net.lab1024.sa.support.config.domain.ConfigAddForm;
import net.lab1024.sa.support.config.domain.ConfigQueryForm;
import net.lab1024.sa.support.config.domain.ConfigUpdateForm;
import net.lab1024.sa.support.config.domain.ConfigVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-14 20:46:27 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CONFIG)
@RequiredArgsConstructor
@RestController
public class AdminConfigController extends SupportBaseController {

  private final ConfigService configService;

  @Operation(summary = "分页查询系统配置 @author 卓大")
  @PostMapping("/config/query")
  @SaCheckPermission("support:config:query")
  public ResponseDTO<PageResult<ConfigVO>> queryConfigPage(
      @RequestBody @Valid ConfigQueryForm queryForm) {
    return configService.queryConfigPage(queryForm);
  }

  @Operation(summary = "添加配置参数 @author 卓大")
  @PostMapping("/config/add")
  @SaCheckPermission("support:config:add")
  public ResponseDTO<String> addConfig(@RequestBody @Valid ConfigAddForm configAddForm) {
    return configService.add(configAddForm);
  }

  @Operation(summary = "修改配置参数 @author 卓大")
  @PostMapping("/config/update")
  @SaCheckPermission("support:config:update")
  public ResponseDTO<String> updateConfig(@RequestBody @Valid ConfigUpdateForm updateForm) {
    return configService.updateConfig(updateForm);
  }
}
