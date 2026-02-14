package net.lab1024.sa.support.heartbeat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.heartbeat.dao.HeartBeatRecordDao;
import net.lab1024.sa.support.heartbeat.domain.HeartBeatRecordQueryForm;
import net.lab1024.sa.support.heartbeat.domain.HeartBeatRecordVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HeartBeatService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>pageQuery - 分頁查詢心跳記錄
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HeartBeatService 單元測試")
class HeartBeatServiceTest {

  @Mock private HeartBeatRecordDao heartBeatRecordDao;

  @InjectMocks private HeartBeatService heartBeatService;

  // ==================== pageQuery 測試 ====================

  @Nested
  @DisplayName("pageQuery 分頁查詢測試")
  class PageQueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      HeartBeatRecordQueryForm form = new HeartBeatRecordQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      HeartBeatRecordVO vo = createTestHeartBeatRecordVO();
      when(heartBeatRecordDao.pageQuery(any(Page.class), any(HeartBeatRecordQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      ResponseDTO<PageResult<HeartBeatRecordVO>> result = heartBeatService.pageQuery(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      HeartBeatRecordQueryForm form = new HeartBeatRecordQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(heartBeatRecordDao.pageQuery(any(Page.class), any(HeartBeatRecordQueryForm.class)))
          .thenReturn(List.of());

      // When
      ResponseDTO<PageResult<HeartBeatRecordVO>> result = heartBeatService.pageQuery(form);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== Helper Methods ====================

  private HeartBeatRecordVO createTestHeartBeatRecordVO() {
    HeartBeatRecordVO vo = new HeartBeatRecordVO();
    vo.setHeartBeatRecordId(1);
    vo.setProjectPath("/app");
    vo.setServerIp("192.168.1.1");
    vo.setProcessNo(12345);
    vo.setProcessStartTime(OffsetDateTime.now(ZoneOffset.UTC));
    vo.setHeartBeatTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }
}
