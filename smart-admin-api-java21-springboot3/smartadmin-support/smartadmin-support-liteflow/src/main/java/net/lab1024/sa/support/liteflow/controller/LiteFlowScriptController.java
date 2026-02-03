package net.lab1024.sa.support.liteflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowScriptVO;
import net.lab1024.sa.support.liteflow.service.LiteFlowScriptService;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.repeatsubmit.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LiteFlow 腳本管理 Controller
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/liteflow/script")
@RequiredArgsConstructor
@Tag(name = "LiteFlow腳本管理")
public class LiteFlowScriptController {

  private final LiteFlowScriptService scriptService;

  @Operation(summary = "分頁查詢腳本")
  @PostMapping("/queryPage")
  @SaCheckPermission("liteflow:script:query")
  public ResponseDTO<PageResult<LiteFlowScriptVO>> queryPage(
      @RequestBody @Valid LiteFlowScriptQueryForm form) {
    return scriptService.queryPage(form);
  }

  @Operation(summary = "創建腳本")
  @PostMapping("/add")
  @SaCheckPermission("liteflow:script:add")
  @RepeatSubmit
  public ResponseDTO<String> add(@RequestBody @Valid LiteFlowScriptAddForm form) {
    RequestUser user = SmartRequestUtil.getRequestUser();
    return scriptService.add(form, user.getUserId(), user.getUserName());
  }

  @Operation(summary = "更新腳本")
  @PostMapping("/update")
  @SaCheckPermission("liteflow:script:update")
  @RepeatSubmit
  public ResponseDTO<String> update(@RequestBody @Valid LiteFlowScriptUpdateForm form) {
    RequestUser user = SmartRequestUtil.getRequestUser();
    return scriptService.update(form, user.getUserId(), user.getUserName());
  }

  @Operation(summary = "刪除腳本")
  @GetMapping("/delete/{scriptId}")
  @SaCheckPermission("liteflow:script:delete")
  public ResponseDTO<String> delete(@PathVariable Long scriptId) {
    RequestUser user = SmartRequestUtil.getRequestUser();
    return scriptService.delete(scriptId, user.getUserId(), user.getUserName());
  }

  @Operation(summary = "獲取腳本詳情")
  @GetMapping("/detail/{scriptId}")
  @SaCheckPermission("liteflow:script:query")
  public ResponseDTO<LiteFlowScriptVO> getDetail(@PathVariable Long scriptId) {
    return scriptService.getDetail(scriptId);
  }
}
