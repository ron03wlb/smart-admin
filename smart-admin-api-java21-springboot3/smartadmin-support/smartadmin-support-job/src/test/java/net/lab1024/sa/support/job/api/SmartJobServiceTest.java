package net.lab1024.sa.support.job.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.job.api.domain.SmartJobAddForm;
import net.lab1024.sa.support.job.api.domain.SmartJobEnabledUpdateForm;
import net.lab1024.sa.support.job.api.domain.SmartJobExecuteForm;
import net.lab1024.sa.support.job.api.domain.SmartJobLogQueryForm;
import net.lab1024.sa.support.job.api.domain.SmartJobLogVO;
import net.lab1024.sa.support.job.api.domain.SmartJobQueryForm;
import net.lab1024.sa.support.job.api.domain.SmartJobUpdateForm;
import net.lab1024.sa.support.job.api.domain.SmartJobVO;
import net.lab1024.sa.support.job.constant.SmartJobTriggerTypeEnum;
import net.lab1024.sa.support.job.repository.SmartJobDao;
import net.lab1024.sa.support.job.repository.SmartJobLogDao;
import net.lab1024.sa.support.job.repository.domain.SmartJobEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SmartJobService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryJobInfo - 查詢任務詳情
 *   <li>queryJob - 分頁查詢任務
 *   <li>queryJobLog - 分頁查詢執行日誌
 *   <li>addJob - 新增任務
 *   <li>updateJob - 更新任務
 *   <li>updateJobEnabled - 啟用/禁用任務
 *   <li>execute - 立即執行任務
 *   <li>deleteJob - 刪除任務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmartJobService 單元測試")
class SmartJobServiceTest {

  @Mock private SmartJobDao jobDao;

  @Mock private SmartJobLogDao jobLogDao;

  @Mock private SmartJobClientManager jobClientManager;

  @InjectMocks private SmartJobService smartJobService;

  @Captor private ArgumentCaptor<SmartJobEntity> jobEntityCaptor;

  // ==================== queryJobInfo 測試 ====================

  @Nested
  @DisplayName("queryJobInfo 查詢任務詳情測試")
  class QueryJobInfoTest {

    @Test
    @DisplayName("正常情況：應該返回任務詳情")
    void shouldReturnJobInfo() {
      // Given
      Integer jobId = 1;
      SmartJobEntity entity = createTestSmartJobEntity();
      when(jobDao.selectById(jobId)).thenReturn(entity);

      // When
      ResponseDTO<SmartJobVO> result = smartJobService.queryJobInfo(jobId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getJobId()).isEqualTo(jobId);
    }

    @Test
    @DisplayName("數據不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      Integer jobId = 999;
      when(jobDao.selectById(jobId)).thenReturn(null);

      // When
      ResponseDTO<SmartJobVO> result = smartJobService.queryJobInfo(jobId);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== queryJob 測試 ====================

  @Nested
  @DisplayName("queryJob 分頁查詢測試")
  class QueryJobTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      SmartJobQueryForm form = new SmartJobQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      SmartJobVO vo = createTestSmartJobVO();
      when(jobDao.query(any(Page.class), any(SmartJobQueryForm.class))).thenReturn(List.of(vo));

      // When
      ResponseDTO<PageResult<SmartJobVO>> result = smartJobService.queryJob(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }
  }

  // ==================== queryJobLog 測試 ====================

  @Nested
  @DisplayName("queryJobLog 分頁查詢日誌測試")
  class QueryJobLogTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      SmartJobLogQueryForm form = new SmartJobLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      SmartJobLogVO vo = createTestSmartJobLogVO();
      when(jobLogDao.query(any(Page.class), any(SmartJobLogQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      ResponseDTO<PageResult<SmartJobLogVO>> result = smartJobService.queryJobLog(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }
  }

  // ==================== addJob 測試 ====================
  // 注意: addJob 和 updateJob 的正常場景需要 Spring 上下文來驗證 job class
  // 這裡只測試不依賴 SmartJobUtil.checkJobClass() 的驗證場景

  @Nested
  @DisplayName("addJob 新增任務測試")
  class AddJobTest {

    @Test
    @DisplayName("CRON 表達式錯誤：應該返回錯誤")
    void shouldReturnErrorWhenCronInvalid() {
      // Given
      SmartJobAddForm form = createTestSmartJobAddForm();
      form.setTriggerType(SmartJobTriggerTypeEnum.CRON.getValue());
      form.setTriggerValue("invalid cron");

      // When
      ResponseDTO<String> result = smartJobService.addJob(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("cron表达式错误");
    }

    @Test
    @DisplayName("固定間隔無效：應該返回錯誤")
    void shouldReturnErrorWhenFixedDelayInvalid() {
      // Given
      SmartJobAddForm form = createTestSmartJobAddForm();
      form.setTriggerType(SmartJobTriggerTypeEnum.FIXED_DELAY.getValue());
      form.setTriggerValue("-1"); // 必須大於 0

      // When
      ResponseDTO<String> result = smartJobService.addJob(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("固定间隔配置错误");
    }
  }

  // ==================== updateJob 測試 ====================

  @Nested
  @DisplayName("updateJob 更新任務測試")
  class UpdateJobTest {

    @Test
    @DisplayName("數據不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      SmartJobUpdateForm form = createTestSmartJobUpdateForm();
      when(jobDao.selectById(form.getJobId())).thenReturn(null);

      // When
      ResponseDTO<String> result = smartJobService.updateJob(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("CRON 表達式錯誤：更新時應返回錯誤")
    void shouldReturnErrorWhenCronInvalidOnUpdate() {
      // Given
      SmartJobUpdateForm form = createTestSmartJobUpdateForm();
      form.setTriggerType(SmartJobTriggerTypeEnum.CRON.getValue());
      form.setTriggerValue("invalid cron");

      SmartJobEntity existingJob = createTestSmartJobEntity();
      when(jobDao.selectById(form.getJobId())).thenReturn(existingJob);

      // When
      ResponseDTO<String> result = smartJobService.updateJob(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("cron表达式错误");
    }
  }

  // ==================== updateJobEnabled 測試 ====================

  @Nested
  @DisplayName("updateJobEnabled 啟用/禁用測試")
  class UpdateJobEnabledTest {

    @Test
    @DisplayName("正常情況：應該成功更新啟用狀態")
    void shouldUpdateEnabledFlagSuccessfully() {
      // Given
      SmartJobEnabledUpdateForm form = new SmartJobEnabledUpdateForm();
      form.setJobId(1);
      form.setEnabledFlag(false);
      form.setUpdateName("admin");

      SmartJobEntity existingJob = createTestSmartJobEntity();
      existingJob.setEnabledFlag(true);
      when(jobDao.selectById(form.getJobId())).thenReturn(existingJob);
      when(jobDao.updateById(any(SmartJobEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = smartJobService.updateJobEnabled(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(jobDao).updateById(any(SmartJobEntity.class));
      verify(jobClientManager).publishToClient(any());
    }

    @Test
    @DisplayName("狀態未改變：應該直接返回成功")
    void shouldReturnOkWhenStatusUnchanged() {
      // Given
      SmartJobEnabledUpdateForm form = new SmartJobEnabledUpdateForm();
      form.setJobId(1);
      form.setEnabledFlag(true);

      SmartJobEntity existingJob = createTestSmartJobEntity();
      existingJob.setEnabledFlag(true);
      when(jobDao.selectById(form.getJobId())).thenReturn(existingJob);

      // When
      ResponseDTO<String> result = smartJobService.updateJobEnabled(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(jobDao, never()).updateById(any(SmartJobEntity.class));
    }
  }

  // ==================== execute 測試 ====================

  @Nested
  @DisplayName("execute 立即執行測試")
  class ExecuteTest {

    @Test
    @DisplayName("正常情況：應該成功發送執行消息")
    void shouldExecuteJobSuccessfully() {
      // Given
      SmartJobExecuteForm form = new SmartJobExecuteForm();
      form.setJobId(1);
      form.setParam("{\"key\":\"value\"}");
      form.setUpdateName("admin");

      SmartJobEntity existingJob = createTestSmartJobEntity();
      when(jobDao.selectById(form.getJobId())).thenReturn(existingJob);

      // When
      ResponseDTO<String> result = smartJobService.execute(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(jobClientManager).publishToClient(any());
    }

    @Test
    @DisplayName("數據不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      SmartJobExecuteForm form = new SmartJobExecuteForm();
      form.setJobId(999);
      when(jobDao.selectById(form.getJobId())).thenReturn(null);

      // When
      ResponseDTO<String> result = smartJobService.execute(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== deleteJob 測試 ====================

  @Nested
  @DisplayName("deleteJob 刪除任務測試")
  class DeleteJobTest {

    @Test
    @DisplayName("正常情況：應該成功軟刪除")
    void shouldDeleteJobSuccessfully() {
      // Given
      Integer jobId = 1;
      RequestUser requestUser = mock(RequestUser.class);
      when(requestUser.getUserName()).thenReturn("admin");

      // When
      ResponseDTO<String> result = smartJobService.deleteJob(jobId, requestUser);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(jobDao).updateDeletedFlag(jobId, Boolean.TRUE);
      verify(jobClientManager).publishToClient(any());
    }
  }

  // ==================== Helper Methods ====================

  private SmartJobEntity createTestSmartJobEntity() {
    SmartJobEntity entity = new SmartJobEntity();
    entity.setJobId(1);
    entity.setJobName("測試任務");
    entity.setJobClass("net.lab1024.sa.support.job.sample.SampleJob");
    entity.setTriggerType(SmartJobTriggerTypeEnum.CRON.getValue());
    entity.setTriggerValue("0 0 * * * ?");
    entity.setEnabledFlag(true);
    entity.setDeletedFlag(false);
    entity.setSort(1);
    entity.setCreateTime(LocalDateTime.now());
    return entity;
  }

  private SmartJobVO createTestSmartJobVO() {
    SmartJobVO vo = new SmartJobVO();
    vo.setJobId(1);
    vo.setJobName("測試任務");
    vo.setJobClass("net.lab1024.sa.support.job.sample.SampleJob");
    vo.setTriggerType(SmartJobTriggerTypeEnum.CRON.getValue());
    vo.setTriggerValue("0 0 * * * ?");
    vo.setEnabledFlag(true);
    vo.setSort(1);
    return vo;
  }

  private SmartJobLogVO createTestSmartJobLogVO() {
    SmartJobLogVO vo = new SmartJobLogVO();
    vo.setLogId(1L);
    vo.setJobId(1);
    vo.setJobName("測試任務");
    vo.setSuccessFlag(1);
    vo.setExecuteStartTime(LocalDateTime.now());
    vo.setExecuteTimeMillis(100L);
    return vo;
  }

  private SmartJobAddForm createTestSmartJobAddForm() {
    SmartJobAddForm form = new SmartJobAddForm();
    form.setJobName("測試任務");
    form.setJobClass("net.lab1024.sa.support.job.sample.SampleJob");
    form.setTriggerType(SmartJobTriggerTypeEnum.CRON.getValue());
    form.setTriggerValue("0 0 * * * ?");
    form.setEnabledFlag(true);
    form.setSort(1);
    form.setUpdateName("admin");
    return form;
  }

  private SmartJobUpdateForm createTestSmartJobUpdateForm() {
    SmartJobUpdateForm form = new SmartJobUpdateForm();
    form.setJobId(1);
    form.setJobName("更新任務");
    form.setJobClass("net.lab1024.sa.support.job.sample.SampleJob");
    form.setTriggerType(SmartJobTriggerTypeEnum.CRON.getValue());
    form.setTriggerValue("0 0 * * * ?");
    form.setEnabledFlag(true);
    form.setSort(1);
    form.setUpdateName("admin");
    return form;
  }
}
