package net.lab1024.sa.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.support.changelog.domain.form.ChangeLogAddForm;
import net.lab1024.sa.support.changelog.domain.form.ChangeLogUpdateForm;
import net.lab1024.sa.support.changelog.service.ChangeLogService;
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
@RequiredArgsConstructor
@RestController
@Tag(name = SwaggerTagConst.Support.CHANGE_LOG)
public class AdminChangeLogController extends SupportBaseController {

  private final ChangeLogService changeLogService;

  @Operation(summary = "添加 @author 卓大")
  @PostMapping("/changeLog/add")
  @SaCheckPermission("support:changeLog:add")
  public ResponseDTO<String> add(@RequestBody @Valid ChangeLogAddForm addForm) {
    return changeLogService.add(addForm);
  }

  @Operation(summary = "更新 @author 卓大")
  @PostMapping("/changeLog/update")
  @SaCheckPermission("support:changeLog:update")
  public ResponseDTO<String> update(@RequestBody @Valid ChangeLogUpdateForm updateForm) {
    return changeLogService.update(updateForm);
  }

  @Operation(summary = "批量删除 @author 卓大")
  @PostMapping("/changeLog/batchDelete")
  @SaCheckPermission("support:changeLog:batchDelete")
  public ResponseDTO<String> batchDelete(@RequestBody List<Long> idList) {
    return changeLogService.batchDelete(idList);
  }

  @Operation(summary = "单个删除 @author 卓大")
  @GetMapping("/changeLog/delete/{changeLogId}")
  @SaCheckPermission("support:changeLog:delete")
  public ResponseDTO<String> batchDelete(@PathVariable Long changeLogId) {
    return changeLogService.delete(changeLogId);
  }
}
