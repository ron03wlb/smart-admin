package net.lab1024.sa.support.changelog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.changelog.dao.ChangeLogDao;
import net.lab1024.sa.support.changelog.domain.entity.ChangeLogEntity;
import net.lab1024.sa.support.changelog.domain.form.ChangeLogAddForm;
import net.lab1024.sa.support.changelog.domain.form.ChangeLogQueryForm;
import net.lab1024.sa.support.changelog.domain.form.ChangeLogUpdateForm;
import net.lab1024.sa.support.changelog.domain.vo.ChangeLogVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ChangeLogService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryPage - 分頁查詢
 *   <li>add - 新增
 *   <li>update - 更新
 *   <li>batchDelete - 批量刪除
 *   <li>delete - 單個刪除
 *   <li>getById - 獲取詳情
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeLogService 單元測試")
class ChangeLogServiceTest {

  @Mock private ChangeLogDao changeLogDao;

  @InjectMocks private ChangeLogService changeLogService;

  // ==================== queryPage 測試 ====================

  @Nested
  @DisplayName("queryPage 分頁查詢測試")
  class QueryPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      ChangeLogQueryForm form = new ChangeLogQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      ChangeLogVO vo = createTestChangeLogVO();
      when(changeLogDao.queryPage(any(Page.class), any(ChangeLogQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      PageResult<ChangeLogVO> result = changeLogService.queryPage(form);

      // Then
      assertThat(result).isNotNull();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增")
    void shouldAddSuccessfully() {
      // Given
      ChangeLogAddForm form = createTestChangeLogAddForm();
      when(changeLogDao.selectByVersion(form.getUpdateVersion())).thenReturn(null);

      // When
      ResponseDTO<String> result = changeLogService.add(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(changeLogDao).insert(any(ChangeLogEntity.class));
    }

    @Test
    @DisplayName("版本重複：應該返回錯誤")
    void shouldReturnErrorWhenVersionExists() {
      // Given
      ChangeLogAddForm form = createTestChangeLogAddForm();
      ChangeLogEntity existingEntity = createTestChangeLogEntity();
      when(changeLogDao.selectByVersion(form.getUpdateVersion())).thenReturn(existingEntity);

      // When
      ResponseDTO<String> result = changeLogService.add(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("版本已经存在");
      verify(changeLogDao, never()).insert(any(ChangeLogEntity.class));
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新")
    void shouldUpdateSuccessfully() {
      // Given
      ChangeLogUpdateForm form = createTestChangeLogUpdateForm();
      when(changeLogDao.selectByVersion(form.getUpdateVersion())).thenReturn(null);

      // When
      ResponseDTO<String> result = changeLogService.update(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(changeLogDao).updateById(any(ChangeLogEntity.class));
    }

    @Test
    @DisplayName("版本與其他記錄重複：應該返回錯誤")
    void shouldReturnErrorWhenVersionConflict() {
      // Given
      ChangeLogUpdateForm form = createTestChangeLogUpdateForm();
      ChangeLogEntity otherEntity = createTestChangeLogEntity();
      otherEntity.setChangeLogId(2L); // 不同的 ID
      when(changeLogDao.selectByVersion(form.getUpdateVersion())).thenReturn(otherEntity);

      // When
      ResponseDTO<String> result = changeLogService.update(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("版本已经存在");
    }

    @Test
    @DisplayName("更新同一記錄的版本號：應該成功")
    void shouldSucceedWhenSameRecord() {
      // Given
      ChangeLogUpdateForm form = createTestChangeLogUpdateForm();
      ChangeLogEntity sameEntity = createTestChangeLogEntity();
      sameEntity.setChangeLogId(form.getChangeLogId()); // 相同的 ID
      when(changeLogDao.selectByVersion(form.getUpdateVersion())).thenReturn(sameEntity);

      // When
      ResponseDTO<String> result = changeLogService.update(form);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== batchDelete 測試 ====================

  @Nested
  @DisplayName("batchDelete 批量刪除測試")
  class BatchDeleteTest {

    @Test
    @DisplayName("正常情況：應該成功批量刪除")
    void shouldBatchDeleteSuccessfully() {
      // Given
      List<Long> idList = List.of(1L, 2L, 3L);

      // When
      ResponseDTO<String> result = changeLogService.batchDelete(idList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(changeLogDao).deleteBatchIds(idList);
    }

    @Test
    @DisplayName("列表為空：應該直接返回成功")
    void shouldReturnOkWhenListEmpty() {
      // When
      ResponseDTO<String> result = changeLogService.batchDelete(List.of());

      // Then
      assertThat(result.getOk()).isTrue();
      verify(changeLogDao, never()).deleteBatchIds(any());
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 單個刪除測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除")
    void shouldDeleteSuccessfully() {
      // Given
      Long changeLogId = 1L;

      // When
      ResponseDTO<String> result = changeLogService.delete(changeLogId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(changeLogDao).deleteById(changeLogId);
    }

    @Test
    @DisplayName("ID 為空：應該直接返回成功")
    void shouldReturnOkWhenIdIsNull() {
      // When
      ResponseDTO<String> result = changeLogService.delete(null);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(changeLogDao, never()).deleteById(any(Long.class));
    }
  }

  // ==================== getById 測試 ====================

  @Nested
  @DisplayName("getById 獲取詳情測試")
  class GetByIdTest {

    @Test
    @DisplayName("正常情況：應該返回詳情")
    void shouldReturnDetail() {
      // Given
      Long changeLogId = 1L;
      ChangeLogEntity entity = createTestChangeLogEntity();
      when(changeLogDao.selectById(changeLogId)).thenReturn(entity);

      // When
      ChangeLogVO result = changeLogService.getById(changeLogId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getChangeLogId()).isEqualTo(changeLogId);
    }

    @Test
    @DisplayName("數據不存在：應該返回 null")
    void shouldReturnNullWhenNotExists() {
      // Given
      Long changeLogId = 999L;
      when(changeLogDao.selectById(changeLogId)).thenReturn(null);

      // When
      ChangeLogVO result = changeLogService.getById(changeLogId);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== Helper Methods ====================

  private ChangeLogVO createTestChangeLogVO() {
    ChangeLogVO vo = new ChangeLogVO();
    vo.setChangeLogId(1L);
    vo.setUpdateVersion("v1.0.0");
    vo.setContent("功能更新");
    vo.setPublicDate(LocalDate.now());
    vo.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return vo;
  }

  private ChangeLogAddForm createTestChangeLogAddForm() {
    ChangeLogAddForm form = new ChangeLogAddForm();
    form.setUpdateVersion("v1.0.0");
    form.setContent("功能更新");
    form.setPublicDate(LocalDate.now());
    form.setType(1);
    form.setPublishAuthor("測試人員");
    return form;
  }

  private ChangeLogUpdateForm createTestChangeLogUpdateForm() {
    ChangeLogUpdateForm form = new ChangeLogUpdateForm();
    form.setChangeLogId(1L);
    form.setUpdateVersion("v1.0.0");
    form.setContent("功能更新");
    form.setPublicDate(LocalDate.now());
    form.setType(1);
    form.setPublishAuthor("測試人員");
    return form;
  }

  private ChangeLogEntity createTestChangeLogEntity() {
    ChangeLogEntity entity = new ChangeLogEntity();
    entity.setChangeLogId(1L);
    entity.setUpdateVersion("v1.0.0");
    entity.setContent("功能更新");
    entity.setPublicDate(LocalDate.now());
    entity.setType(1);
    entity.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    return entity;
  }
}
