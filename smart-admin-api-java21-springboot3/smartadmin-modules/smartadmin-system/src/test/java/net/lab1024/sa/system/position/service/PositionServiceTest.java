package net.lab1024.sa.system.position.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.position.dao.PositionDao;
import net.lab1024.sa.system.position.domain.entity.PositionEntity;
import net.lab1024.sa.system.position.domain.form.PositionAddForm;
import net.lab1024.sa.system.position.domain.form.PositionQueryForm;
import net.lab1024.sa.system.position.domain.form.PositionUpdateForm;
import net.lab1024.sa.system.position.domain.vo.PositionVO;
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
 * PositionService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>職務 CRUD 操作
 *   <li>分頁查詢
 *   <li>批量刪除邏輯
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PositionService 單元測試")
class PositionServiceTest {

  @Mock private PositionDao positionDao;

  @InjectMocks private PositionService positionService;

  @Captor private ArgumentCaptor<PositionEntity> entityCaptor;

  // ==================== queryPage 測試 ====================

  @Nested
  @DisplayName("queryPage 分頁查詢測試")
  class QueryPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      PositionQueryForm queryForm = new PositionQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      PositionVO vo1 = createTestPositionVO(1L, "Manager");
      PositionVO vo2 = createTestPositionVO(2L, "Engineer");
      List<PositionVO> voList = Arrays.asList(vo1, vo2);

      when(positionDao.queryPage(any(Page.class), any(PositionQueryForm.class))).thenReturn(voList);

      // When
      PageResult<PositionVO> result = positionService.queryPage(queryForm);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getList()).hasSize(2);
      verify(positionDao).queryPage(any(Page.class), any(PositionQueryForm.class));
    }

    @Test
    @DisplayName("空結果：應該返回空列表")
    void shouldReturnEmptyList() {
      // Given
      PositionQueryForm queryForm = new PositionQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(positionDao.queryPage(any(Page.class), any(PositionQueryForm.class)))
          .thenReturn(Collections.emptyList());

      // When
      PageResult<PositionVO> result = positionService.queryPage(queryForm);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getList()).isEmpty();
    }

    @Test
    @DisplayName("驗證：應該設置 deletedFlag 為 false")
    void shouldSetDeletedFlagFalse() {
      // Given
      PositionQueryForm queryForm = new PositionQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setDeletedFlag(null); // Initially null

      when(positionDao.queryPage(any(Page.class), any(PositionQueryForm.class)))
          .thenReturn(Collections.emptyList());

      // When
      positionService.queryPage(queryForm);

      // Then
      assertThat(queryForm.getDeletedFlag()).isFalse();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增職務測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增職務")
    void shouldAddSuccess() {
      // Given
      PositionAddForm addForm = createTestAddForm("Developer");
      when(positionDao.insert(any(PositionEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = positionService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(positionDao).insert(entityCaptor.capture());
      assertThat(entityCaptor.getValue().getPositionName()).isEqualTo("Developer");
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新職務測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新職務")
    void shouldUpdateSuccess() {
      // Given
      PositionUpdateForm updateForm = createTestUpdateForm(1L, "Senior Developer");
      when(positionDao.updateById(any(PositionEntity.class))).thenReturn(1);

      // When
      ResponseDTO<String> result = positionService.update(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(positionDao).updateById(entityCaptor.capture());
      assertThat(entityCaptor.getValue().getPositionId()).isEqualTo(1L);
      assertThat(entityCaptor.getValue().getPositionName()).isEqualTo("Senior Developer");
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 單個刪除測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除")
    void shouldDeleteSuccess() {
      // Given
      Long positionId = 1L;
      when(positionDao.deleteById(positionId)).thenReturn(1);

      // When
      ResponseDTO<String> result = positionService.delete(positionId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(positionDao).deleteById(positionId);
    }

    @Test
    @DisplayName("邊界情況：null ID 應該直接返回成功")
    void shouldReturnOkWhenIdNull() {
      // When
      ResponseDTO<String> result = positionService.delete(null);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(positionDao, never()).deleteById(anyLong());
    }
  }

  // ==================== batchDelete 測試 ====================

  @Nested
  @DisplayName("batchDelete 批量刪除測試")
  class BatchDeleteTest {

    @Test
    @DisplayName("正常情況：應該成功批量刪除")
    void shouldBatchDeleteSuccess() {
      // Given
      List<Long> idList = Arrays.asList(1L, 2L, 3L);
      when(positionDao.deleteBatchIds(idList)).thenReturn(3);

      // When
      ResponseDTO<String> result = positionService.batchDelete(idList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(positionDao).deleteBatchIds(idList);
    }

    @Test
    @DisplayName("邊界情況：空列表應該直接返回成功")
    void shouldReturnOkWhenListEmpty() {
      // When
      ResponseDTO<String> result = positionService.batchDelete(Collections.emptyList());

      // Then
      assertThat(result.getOk()).isTrue();
      verify(positionDao, never()).deleteBatchIds(any());
    }
  }

  // ==================== queryList 測試 ====================

  @Nested
  @DisplayName("queryList 列表查詢測試")
  class QueryListTest {

    @Test
    @DisplayName("正常情況：應該返回職務列表")
    void shouldReturnList() {
      // Given
      PositionVO vo1 = createTestPositionVO(1L, "Manager");
      PositionVO vo2 = createTestPositionVO(2L, "Engineer");
      List<PositionVO> voList = Arrays.asList(vo1, vo2);

      when(positionDao.queryList(Boolean.FALSE)).thenReturn(voList);

      // When
      List<PositionVO> result = positionService.queryList();

      // Then
      assertThat(result).hasSize(2);
      verify(positionDao).queryList(Boolean.FALSE);
    }

    @Test
    @DisplayName("空結果：應該返回空列表")
    void shouldReturnEmptyList() {
      // Given
      when(positionDao.queryList(Boolean.FALSE)).thenReturn(Collections.emptyList());

      // When
      List<PositionVO> result = positionService.queryList();

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== Helper Methods ====================

  private PositionVO createTestPositionVO(Long id, String name) {
    PositionVO vo = new PositionVO();
    vo.setPositionId(id);
    vo.setPositionName(name);
    return vo;
  }

  private PositionAddForm createTestAddForm(String name) {
    PositionAddForm form = new PositionAddForm();
    form.setPositionName(name);
    return form;
  }

  private PositionUpdateForm createTestUpdateForm(Long id, String name) {
    PositionUpdateForm form = new PositionUpdateForm();
    form.setPositionId(id);
    form.setPositionName(name);
    return form;
  }
}
