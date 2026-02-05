package net.lab1024.sa.support.liteflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.repeatsubmit.annotation.RepeatSubmit;
import net.lab1024.sa.common.web.web.util.SmartRequestUtil;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowChainVO;
import net.lab1024.sa.support.liteflow.service.LiteFlowChainService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LiteFlow 流程管理 Controller
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/liteflow/chain")
@RequiredArgsConstructor
@Tag(name = "LiteFlow流程管理")
public class LiteFlowChainController {

  private final LiteFlowChainService chainService;

  @Operation(summary = "分頁查詢流程")
  @PostMapping("/queryPage")
  @SaCheckPermission("liteflow:chain:query")
  public ResponseDTO<PageResult<LiteFlowChainVO>> queryPage(
      @RequestBody @Valid LiteFlowChainQueryForm form) {
    return chainService.queryPage(form);
  }

  @Operation(summary = "創建流程")
  @PostMapping("/add")
  @SaCheckPermission("liteflow:chain:add")
  @RepeatSubmit
  public ResponseDTO<String> add(@RequestBody @Valid LiteFlowChainAddForm form) {
    RequestUser user = SmartRequestUtil.getRequestUser();
    return chainService.add(form, user.getUserId(), user.getUserName());
  }

  @Operation(summary = "更新流程")
  @PostMapping("/update")
  @SaCheckPermission("liteflow:chain:update")
  @RepeatSubmit
  public ResponseDTO<String> update(@RequestBody @Valid LiteFlowChainUpdateForm form) {
    RequestUser user = SmartRequestUtil.getRequestUser();
    return chainService.update(form, user.getUserId(), user.getUserName());
  }

  @Operation(summary = "刪除流程")
  @GetMapping("/delete/{chainId}")
  @SaCheckPermission("liteflow:chain:delete")
  public ResponseDTO<String> delete(@PathVariable Long chainId) {
    RequestUser user = SmartRequestUtil.getRequestUser();
    return chainService.delete(chainId, user.getUserId(), user.getUserName());
  }

  @Operation(summary = "獲取流程詳情")
  @GetMapping("/detail/{chainId}")
  @SaCheckPermission("liteflow:chain:query")
  public ResponseDTO<LiteFlowChainVO> getDetail(@PathVariable Long chainId) {
    return chainService.getDetail(chainId);
  }

  @Operation(summary = "重載所有流程")
  @PostMapping("/reloadAll")
  @SaCheckPermission("liteflow:chain:reload")
  public ResponseDTO<String> reloadAll() {
    return chainService.reloadAll();
  }
}
