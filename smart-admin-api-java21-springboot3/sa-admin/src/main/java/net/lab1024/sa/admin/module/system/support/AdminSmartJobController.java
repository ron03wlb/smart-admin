package net.lab1024.sa.admin.module.system.support;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.job.api.SmartJobService;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobAddForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobEnabledUpdateForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobExecuteForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobLogQueryForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobLogVO;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobQueryForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobUpdateForm;
import net.lab1024.sa.base.module.support.job.api.domain.SmartJobVO;
import net.lab1024.sa.base.module.support.job.config.SmartJobAutoConfiguration;
import net.lab1024.sa.base.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.base.web.base.SupportBaseController;
import net.lab1024.sa.base.web.util.SmartRequestUtil;
import net.lab1024.sa.foundation.domain.request.RequestUser;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.foundation.repeatsubmit.annotation.RepeatSubmit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 定时任务 管理接口
 *
 * @author huke
 * @since 2024/6/17 20:41
 */
@Tag(name = SwaggerTagConst.Support.JOB)
@RestController
@RequiredArgsConstructor
@ConditionalOnBean(SmartJobAutoConfiguration.class)
public class AdminSmartJobController extends SupportBaseController {

  private final SmartJobService jobService;

  @Operation(summary = "定时任务-立即执行 @huke")
  @PostMapping("/job/execute")
  @RepeatSubmit
  public ResponseDTO<String> execute(@RequestBody @Valid SmartJobExecuteForm executeForm) {
    RequestUser requestUser = SmartRequestUtil.getRequestUser();
    executeForm.setUpdateName(requestUser.getUserName());
    return jobService.execute(executeForm);
  }

  @Operation(summary = "定时任务-查询详情 @huke")
  @GetMapping("/job/{jobId}")
  public ResponseDTO<SmartJobVO> queryJobInfo(@PathVariable Integer jobId) {
    return jobService.queryJobInfo(jobId);
  }

  @Operation(summary = "定时任务-分页查询 @huke")
  @PostMapping("/job/query")
  public ResponseDTO<PageResult<SmartJobVO>> queryJob(
      @RequestBody @Valid SmartJobQueryForm queryForm) {
    return jobService.queryJob(queryForm);
  }

  @Operation(summary = "定时任务-添加任务 @huke")
  @PostMapping("/job/add")
  @RepeatSubmit
  public ResponseDTO<String> addJob(@RequestBody @Valid SmartJobAddForm addForm) {
    RequestUser requestUser = SmartRequestUtil.getRequestUser();
    addForm.setUpdateName(requestUser.getUserName());
    return jobService.addJob(addForm);
  }

  @Operation(summary = "定时任务-更新-任务信息 @huke")
  @PostMapping("/job/update")
  @RepeatSubmit
  public ResponseDTO<String> updateJob(@RequestBody @Valid SmartJobUpdateForm updateForm) {
    RequestUser requestUser = SmartRequestUtil.getRequestUser();
    updateForm.setUpdateName(requestUser.getUserName());
    return jobService.updateJob(updateForm);
  }

  @Operation(summary = "定时任务-更新-开启状态 @huke")
  @PostMapping("/job/update/enabled")
  @RepeatSubmit
  public ResponseDTO<String> updateJobEnabled(
      @RequestBody @Valid SmartJobEnabledUpdateForm updateForm) {
    RequestUser requestUser = SmartRequestUtil.getRequestUser();
    updateForm.setUpdateName(requestUser.getUserName());
    return jobService.updateJobEnabled(updateForm);
  }

  @Operation(summary = "定时任务-删除 @zhuoda")
  @GetMapping("/job/delete")
  @RepeatSubmit
  public ResponseDTO<String> deleteJob(@RequestParam Integer jobId) {
    return jobService.deleteJob(jobId, SmartRequestUtil.getRequestUser());
  }

  @Operation(summary = "定时任务-执行记录-分页查询 @huke")
  @PostMapping("/job/log/query")
  public ResponseDTO<PageResult<SmartJobLogVO>> queryJobLog(
      @RequestBody @Valid SmartJobLogQueryForm queryForm) {
    return jobService.queryJobLog(queryForm);
  }
}
