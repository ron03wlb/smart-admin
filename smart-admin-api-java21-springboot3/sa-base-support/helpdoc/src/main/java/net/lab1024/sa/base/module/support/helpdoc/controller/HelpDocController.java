package net.lab1024.sa.base.module.support.helpdoc.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import net.lab1024.sa.base.core.util.SmartRequestUtil;
import net.lab1024.sa.base.module.support.helpdoc.domain.form.HelpDocViewRecordQueryForm;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocCatalogVO;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocDetailVO;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocVO;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocViewRecordVO;
import net.lab1024.sa.base.module.support.helpdoc.service.HelpDocCatalogService;
import net.lab1024.sa.base.module.support.helpdoc.service.HelpDocUserService;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.repeatsubmit.annotation.RepeatSubmit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帮助文档
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Tag(name = SwaggerTagConst.Support.HELP_DOC)
@RestController
@SuppressWarnings("PMD.LongVariable")
public class HelpDocController extends SupportBaseController {

  @Resource private HelpDocCatalogService helpDocCatalogService;

  @Resource private HelpDocUserService helpDocUserService;

  // --------------------- 帮助文档 【目录】 -------------------------

  @Operation(summary = "帮助文档目录-获取全部 @author 卓大")
  @GetMapping("/helpDoc/helpDocCatalog/getAll")
  public ResponseDTO<List<HelpDocCatalogVO>> getAll() {
    return ResponseDTO.ok(helpDocCatalogService.getAll());
  }

  // --------------------- 帮助文档 【用户】-------------------------

  @Operation(summary = "【用户】帮助文档-查看详情 @author 卓大")
  @GetMapping("/helpDoc/user/view/{helpDocId}")
  @RepeatSubmit
  public ResponseDTO<HelpDocDetailVO> view(
      @PathVariable final Long helpDocId, final HttpServletRequest request) {
    return helpDocUserService.view(SmartRequestUtil.getRequestUser(), helpDocId);
  }

  @Operation(summary = "【用户】帮助文档-查询全部 @author 卓大")
  @GetMapping("/helpDoc/user/queryAllHelpDocList")
  @RepeatSubmit
  public ResponseDTO<List<HelpDocVO>> queryAllHelpDocList() {
    return helpDocUserService.queryAllHelpDocList();
  }

  @Operation(summary = "【用户】帮助文档-查询 查看记录 @author 卓大")
  @PostMapping("/helpDoc/user/queryViewRecord")
  @RepeatSubmit
  public ResponseDTO<PageResult<HelpDocViewRecordVO>> queryViewRecord(
      @RequestBody @Valid final HelpDocViewRecordQueryForm helpDocViewRecordQueryForm) {
    return ResponseDTO.ok(helpDocUserService.queryViewRecord(helpDocViewRecordQueryForm));
  }
}
