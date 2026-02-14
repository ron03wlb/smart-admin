package net.lab1024.sa.support.codegenerator.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.common.web.web.util.SmartResponseUtil;
import net.lab1024.sa.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.support.codegenerator.domain.form.CodeGeneratorPreviewForm;
import net.lab1024.sa.support.codegenerator.domain.form.TableQueryForm;
import net.lab1024.sa.support.codegenerator.domain.vo.TableColumnVO;
import net.lab1024.sa.support.codegenerator.domain.vo.TableConfigVO;
import net.lab1024.sa.support.codegenerator.domain.vo.TableVO;
import net.lab1024.sa.support.codegenerator.service.CodeGeneratorService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 代码生成
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-06-29 20:23:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.CODE_GENERATOR)
@Controller
@RequiredArgsConstructor
public class CodeGeneratorController extends SupportBaseController {

  private final CodeGeneratorService codeGeneratorService;

  // ------------------- 查询 -------------------

  @Operation(summary = "获取表的列 @author 卓大")
  @GetMapping("/codeGenerator/table/getTableColumns/{table}")
  @ResponseBody
  public ResponseDTO<List<TableColumnVO>> getTableColumns(@PathVariable String table) {
    return ResponseDTO.ok(codeGeneratorService.getTableColumns(table));
  }

  @Operation(summary = "查询数据库的表 @author 卓大")
  @PostMapping("/codeGenerator/table/queryTableList")
  @ResponseBody
  public ResponseDTO<PageResult<TableVO>> queryTableList(
      @RequestBody @Valid TableQueryForm tableQueryForm) {
    return ResponseDTO.ok(codeGeneratorService.queryTableList(tableQueryForm));
  }

  // ------------------- 配置 -------------------

  @Operation(summary = "获取表的配置信息 @author 卓大")
  @GetMapping("/codeGenerator/table/getConfig/{table}")
  @ResponseBody
  public ResponseDTO<TableConfigVO> getTableConfig(@PathVariable String table) {
    return ResponseDTO.ok(codeGeneratorService.getTableConfig(table));
  }

  @Operation(summary = "更新配置信息 @author 卓大")
  @PostMapping("/codeGenerator/table/updateConfig")
  @ResponseBody
  public ResponseDTO<String> updateConfig(@RequestBody @Valid CodeGeneratorConfigForm form) {
    return codeGeneratorService.updateConfig(form);
  }

  // ------------------- 生成 -------------------

  @Operation(summary = "代码预览 @author 卓大")
  @PostMapping("/codeGenerator/code/preview")
  @ResponseBody
  public ResponseDTO<String> preview(@RequestBody @Valid CodeGeneratorPreviewForm form) {
    return codeGeneratorService.preview(form);
  }

  @Operation(summary = "代码下载 @author 卓大")
  @GetMapping(
      value = "/codeGenerator/code/download/{tableName}",
      produces = "application/octet-stream")
  public void download(@PathVariable String tableName, HttpServletResponse response)
      throws IOException {

    ResponseDTO<byte[]> download = codeGeneratorService.download(tableName);

    if (download.getOk()) {
      SmartResponseUtil.setDownloadFileHeader(
          response, tableName + "_code.zip", (long) download.getData().length);
      response.getOutputStream().write(download.getData());
    } else {
      SmartResponseUtil.write(response, download);
    }
  }
}
