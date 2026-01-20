package net.lab1024.sa.base.module.support.changelog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import net.lab1024.sa.base.common.controller.SupportBaseController;
import net.lab1024.sa.base.constant.SwaggerTagConst;
import net.lab1024.sa.base.module.support.changelog.domain.form.ChangeLogQueryForm;
import net.lab1024.sa.base.module.support.changelog.domain.vo.ChangeLogVO;
import net.lab1024.sa.base.module.support.changelog.service.ChangeLogService;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统更新日志 Controller
 *
 * @author 卓大
 * @since 2022-09-26 14:53:50 Copyright 1024创新实验室
 */
@RestController
@Tag(name = SwaggerTagConst.Support.CHANGE_LOG)
public class ChangeLogController extends SupportBaseController {

  @Resource private ChangeLogService changeLogService;

  @Operation(summary = "分页查询 @author 卓大")
  @PostMapping("/changeLog/queryPage")
  public ResponseDTO<PageResult<ChangeLogVO>> queryPage(
      @RequestBody @Valid ChangeLogQueryForm queryForm) {
    return ResponseDTO.ok(changeLogService.queryPage(queryForm));
  }

  @Operation(summary = "变更内容详情 @author 卓大")
  @GetMapping("/changeLog/getDetail/{changeLogId}")
  public ResponseDTO<ChangeLogVO> getDetail(@PathVariable Long changeLogId) {
    return ResponseDTO.ok(changeLogService.getById(changeLogId));
  }
}
