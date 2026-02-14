package net.lab1024.sa.support.helpdoc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.helpdoc.dao.HelpDocCatalogDao;
import net.lab1024.sa.support.helpdoc.dao.HelpDocDao;
import net.lab1024.sa.support.helpdoc.domain.entity.HelpDocCatalogEntity;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocCatalogAddForm;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocCatalogUpdateForm;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocCatalogVO;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HelpDocCatalogService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getAll - 查詢所有目錄
 *   <li>add - 新增目錄
 *   <li>update - 更新目錄
 *   <li>delete - 刪除目錄
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HelpDocCatalogService 單元測試")
class HelpDocCatalogServiceTest {

  @Mock private HelpDocCatalogDao helpDocCatalogDao;

  @Mock private HelpDocDao helpDocDao;

  @InjectMocks private HelpDocCatalogService helpDocCatalogService;

  // ==================== getAll 測試 ====================

  @Nested
  @DisplayName("getAll 查詢所有目錄測試")
  class GetAllTest {

    @Test
    @DisplayName("正常情況：應該返回目錄列表")
    void shouldReturnCatalogList() {
      // Given
      HelpDocCatalogEntity entity = createTestHelpDocCatalogEntity();
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(entity));

      // When
      List<HelpDocCatalogVO> result = helpDocCatalogService.getAll();

      // Then
      assertThat(result).hasSize(1);
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增目錄測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增目錄")
    void shouldAddSuccessfully() {
      // Given
      HelpDocCatalogAddForm form = createTestHelpDocCatalogAddForm();
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of());

      // When
      ResponseDTO<String> result = helpDocCatalogService.add(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDocCatalogDao).insert(any(HelpDocCatalogEntity.class));
    }

    @Test
    @DisplayName("名稱重複：應該返回錯誤")
    void shouldReturnErrorWhenNameExists() {
      // Given
      HelpDocCatalogAddForm form = createTestHelpDocCatalogAddForm();
      HelpDocCatalogEntity existing = createTestHelpDocCatalogEntity();
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(existing));

      // When
      ResponseDTO<String> result = helpDocCatalogService.add(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("相同名称");
      verify(helpDocCatalogDao, never()).insert(any(HelpDocCatalogEntity.class));
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新目錄測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新目錄")
    void shouldUpdateSuccessfully() {
      // Given
      HelpDocCatalogUpdateForm form = createTestHelpDocCatalogUpdateForm();
      HelpDocCatalogEntity entity = createTestHelpDocCatalogEntity();

      when(helpDocCatalogDao.selectById(form.getHelpDocCatalogId())).thenReturn(entity);
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(entity));

      // When
      ResponseDTO<String> result = helpDocCatalogService.update(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDocCatalogDao).updateById(any(HelpDocCatalogEntity.class));
    }

    @Test
    @DisplayName("目錄不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      HelpDocCatalogUpdateForm form = createTestHelpDocCatalogUpdateForm();
      when(helpDocCatalogDao.selectById(form.getHelpDocCatalogId())).thenReturn(null);

      // When
      ResponseDTO<String> result = helpDocCatalogService.update(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("不存在");
    }

    @Test
    @DisplayName("名稱與其他目錄重複：應該返回錯誤")
    void shouldReturnErrorWhenNameConflict() {
      // Given
      HelpDocCatalogUpdateForm form = createTestHelpDocCatalogUpdateForm();
      form.setName("其他目錄");

      HelpDocCatalogEntity currentEntity = createTestHelpDocCatalogEntity();
      HelpDocCatalogEntity otherEntity = createTestHelpDocCatalogEntity();
      otherEntity.setHelpDocCatalogId(2L);
      otherEntity.setName("其他目錄");

      when(helpDocCatalogDao.selectById(form.getHelpDocCatalogId())).thenReturn(currentEntity);
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(currentEntity, otherEntity));

      // When
      ResponseDTO<String> result = helpDocCatalogService.update(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("相同名称");
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除目錄測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除目錄")
    void shouldDeleteSuccessfully() {
      // Given
      Long catalogId = 1L;
      HelpDocCatalogEntity entity = createTestHelpDocCatalogEntity();

      when(helpDocCatalogDao.selectById(catalogId)).thenReturn(entity);
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(entity));
      when(helpDocDao.queryHelpDocByCatalogId(catalogId)).thenReturn(List.of());

      // When
      ResponseDTO<String> result = helpDocCatalogService.delete(catalogId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDocCatalogDao).deleteById(catalogId);
    }

    @Test
    @DisplayName("catalogId 為空：應該返回成功")
    void shouldReturnOkWhenIdIsNull() {
      // When
      ResponseDTO<String> result = helpDocCatalogService.delete(null);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDocCatalogDao, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("目錄不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      Long catalogId = 999L;
      when(helpDocCatalogDao.selectById(catalogId)).thenReturn(null);

      // When
      ResponseDTO<String> result = helpDocCatalogService.delete(catalogId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("不存在");
    }

    @Test
    @DisplayName("存在子目錄：應該返回錯誤")
    void shouldReturnErrorWhenHasChildren() {
      // Given
      Long catalogId = 1L;
      HelpDocCatalogEntity entity = createTestHelpDocCatalogEntity();
      HelpDocCatalogEntity childEntity = createTestHelpDocCatalogEntity();
      childEntity.setHelpDocCatalogId(2L);
      childEntity.setParentId(catalogId);

      when(helpDocCatalogDao.selectById(catalogId)).thenReturn(entity);
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(entity, childEntity));

      // When
      ResponseDTO<String> result = helpDocCatalogService.delete(catalogId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("子目录");
    }

    @Test
    @DisplayName("存在文檔：應該返回錯誤")
    void shouldReturnErrorWhenHasDocs() {
      // Given
      Long catalogId = 1L;
      HelpDocCatalogEntity entity = createTestHelpDocCatalogEntity();
      HelpDocVO docVO = new HelpDocVO();
      docVO.setHelpDocId(1L);

      when(helpDocCatalogDao.selectById(catalogId)).thenReturn(entity);
      when(helpDocCatalogDao.selectList(null)).thenReturn(List.of(entity));
      when(helpDocDao.queryHelpDocByCatalogId(catalogId)).thenReturn(List.of(docVO));

      // When
      ResponseDTO<String> result = helpDocCatalogService.delete(catalogId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("存在文档");
    }
  }

  // ==================== Helper Methods ====================

  private HelpDocCatalogEntity createTestHelpDocCatalogEntity() {
    HelpDocCatalogEntity entity = new HelpDocCatalogEntity();
    entity.setHelpDocCatalogId(1L);
    entity.setName("測試目錄");
    entity.setParentId(0L);
    entity.setSort(1);
    return entity;
  }

  private HelpDocCatalogAddForm createTestHelpDocCatalogAddForm() {
    HelpDocCatalogAddForm form = new HelpDocCatalogAddForm();
    form.setName("測試目錄");
    form.setParentId(0L);
    form.setSort(1);
    return form;
  }

  private HelpDocCatalogUpdateForm createTestHelpDocCatalogUpdateForm() {
    HelpDocCatalogUpdateForm form = new HelpDocCatalogUpdateForm();
    form.setHelpDocCatalogId(1L);
    form.setName("測試目錄");
    form.setParentId(0L);
    form.setSort(1);
    return form;
  }
}
