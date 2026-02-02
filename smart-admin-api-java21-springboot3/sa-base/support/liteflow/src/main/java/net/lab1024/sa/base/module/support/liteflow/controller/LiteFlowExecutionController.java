package net.lab1024.sa.base.module.support.liteflow.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowExecutionForm;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowExecutionLogQueryForm;
import net.lab1024.sa.base.module.support.liteflow.domain.vo.LiteFlowExecutionLogVO;
import net.lab1024.sa.base.module.support.liteflow.domain.vo.LiteFlowExecutionResultVO;
import net.lab1024.sa.base.module.support.liteflow.service.LiteFlowExecutionService;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.repeatsubmit.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LiteFlow 執行管理 Controller
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@RestController
@RequestMapping("/liteflow/execution")
@RequiredArgsConstructor
@Tag(name = "LiteFlow執行管理")
public class LiteFlowExecutionController {

  private final LiteFlowExecutionService executionService;

  @Operation(summary = "執行流程")
  @PostMapping("/execute")
  @SaCheckPermission("liteflow:execution:execute")
  @RepeatSubmit
  public ResponseDTO<LiteFlowExecutionResultVO> execute(
      @RequestBody @Valid LiteFlowExecutionForm form) {
    return executionService.execute(form);
  }

  @Operation(summary = "分頁查詢執行日誌")
  @PostMapping("/queryLog")
  @SaCheckPermission("liteflow:execution:query")
  public ResponseDTO<PageResult<LiteFlowExecutionLogVO>> queryLog(
      @RequestBody @Valid LiteFlowExecutionLogQueryForm form) {
    return executionService.queryLog(form);
  }

  @Operation(summary = "獲取執行日誌詳情")
  @GetMapping("/logDetail/{logId}")
  @SaCheckPermission("liteflow:execution:query")
  public ResponseDTO<LiteFlowExecutionLogVO> getLogDetail(@PathVariable Long logId) {
    return executionService.getLogDetail(logId);
  }
}
