package net.lab1024.sa.base.module.support.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.common.controller.SupportBaseController;
import net.lab1024.sa.base.constant.SwaggerTagConst;
import net.lab1024.sa.base.core.util.SmartRequestUtil;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackAddForm;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackQueryForm;
import net.lab1024.sa.base.module.support.feedback.domain.FeedbackVO;
import net.lab1024.sa.base.module.support.feedback.service.FeedbackService;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.RequestUser;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 意见反馈
 *
 * @author 1024创新实验室: 开云
 * @since 2022-08-11 20:48:09 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Tag(name = SwaggerTagConst.Support.FEEDBACK)
@RestController
public class FeedbackController extends SupportBaseController {

  @Resource private FeedbackService feedbackService;

  @Operation(summary = "意见反馈-分页查询 @author 开云")
  @PostMapping("/feedback/query")
  public ResponseDTO<PageResult<FeedbackVO>> query(
      @RequestBody @Valid final FeedbackQueryForm queryForm) {
    return feedbackService.query(queryForm);
  }

  @Operation(summary = "意见反馈-新增 @author 开云")
  @PostMapping("/feedback/add")
  public ResponseDTO<String> add(@RequestBody @Valid final FeedbackAddForm addForm) {
    final RequestUser employee = SmartRequestUtil.getRequestUser();
    return feedbackService.add(addForm, employee);
  }
}
