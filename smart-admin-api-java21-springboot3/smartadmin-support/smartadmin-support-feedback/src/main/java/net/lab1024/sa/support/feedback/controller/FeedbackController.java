package net.lab1024.sa.support.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartRequestUtil;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.support.feedback.domain.FeedbackAddForm;
import net.lab1024.sa.support.feedback.domain.FeedbackQueryForm;
import net.lab1024.sa.support.feedback.domain.FeedbackVO;
import net.lab1024.sa.support.feedback.service.FeedbackService;
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
@RequiredArgsConstructor
public class FeedbackController extends SupportBaseController {

  private final FeedbackService feedbackService;

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
