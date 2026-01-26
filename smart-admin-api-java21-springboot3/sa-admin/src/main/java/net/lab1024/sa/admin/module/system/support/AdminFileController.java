package net.lab1024.sa.admin.module.system.support;

import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.file.domain.form.FileQueryForm;
import net.lab1024.sa.base.module.support.file.domain.vo.FileVO;
import net.lab1024.sa.base.module.support.file.service.FileService;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 文件服务
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@RestController
@Tag(name = SwaggerTagConst.Support.FILE)
public class AdminFileController extends SupportBaseController {

  private final FileService fileService;

  @Operation(summary = "分页查询 @author 1024创新实验室-主任-卓大")
  @PostMapping("/file/queryPage")
  @SaCheckPermission("support:file:query")
  public ResponseDTO<PageResult<FileVO>> queryPage(@RequestBody @Valid FileQueryForm queryForm) {
    return ResponseDTO.ok(fileService.queryPage(queryForm));
  }
}
