package net.lab1024.sa.support.helpdoc.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.helpdoc.dao.HelpDocDao;
import net.lab1024.sa.support.helpdoc.domain.entity.HelpDocEntity;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocAddForm;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocQueryForm;
import net.lab1024.sa.support.helpdoc.domain.form.HelpDocUpdateForm;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocDetailVO;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocRelationVO;
import net.lab1024.sa.support.helpdoc.domain.vo.HelpDocVO;
import net.lab1024.sa.support.helpdoc.manager.HelpDocManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * HelpDocService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>query - 分頁查詢幫助文檔
 *   <li>add - 新增文檔
 *   <li>update - 更新文檔
 *   <li>delete - 刪除文檔
 *   <li>getDetail - 獲取詳情
 *   <li>queryHelpDocByRelationId - 按關聯查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HelpDocService 單元測試")
class HelpDocServiceTest {

  @Mock private HelpDocDao helpDocDao;

  @Mock private HelpDocManager helpDaoManager;

  @InjectMocks private HelpDocService helpDocService;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      HelpDocQueryForm form = new HelpDocQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      HelpDocVO vo = createTestHelpDocVO();
      when(helpDocDao.query(any(Page.class), any(HelpDocQueryForm.class))).thenReturn(List.of(vo));

      // When
      PageResult<HelpDocVO> result = helpDocService.query(form);

      // Then
      assertThat(result).isNotNull();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增文檔測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增文檔")
    void shouldAddSuccessfully() {
      // Given
      HelpDocAddForm form = createTestHelpDocAddForm();

      // When
      ResponseDTO<String> result = helpDocService.add(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDaoManager).saveTransaction(any(HelpDocEntity.class), eq(form.getRelationList()));
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新文檔測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新文檔")
    void shouldUpdateSuccessfully() {
      // Given
      HelpDocUpdateForm form = createTestHelpDocUpdateForm();

      // When
      ResponseDTO<String> result = helpDocService.update(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDaoManager)
          .updateTransaction(any(HelpDocEntity.class), eq(form.getRelationList()));
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除文檔測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除文檔")
    void shouldDeleteSuccessfully() {
      // Given
      Long helpDocId = 1L;
      HelpDocEntity entity = createTestHelpDocEntity();
      when(helpDocDao.selectById(helpDocId)).thenReturn(entity);

      // When
      ResponseDTO<String> result = helpDocService.delete(helpDocId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDaoManager).deleteTransaction(helpDocId);
    }

    @Test
    @DisplayName("數據不存在：應該直接返回成功")
    void shouldReturnOkWhenNotExists() {
      // Given
      Long helpDocId = 999L;
      when(helpDocDao.selectById(helpDocId)).thenReturn(null);

      // When
      ResponseDTO<String> result = helpDocService.delete(helpDocId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(helpDaoManager, never()).deleteTransaction(any());
    }
  }

  // ==================== getDetail 測試 ====================

  @Nested
  @DisplayName("getDetail 獲取詳情測試")
  class GetDetailTest {

    @Test
    @DisplayName("正常情況：應該返回詳情")
    void shouldReturnDetail() {
      // Given
      Long helpDocId = 1L;
      HelpDocEntity entity = createTestHelpDocEntity();
      List<HelpDocRelationVO> relationList = List.of(createTestHelpDocRelationVO());

      when(helpDocDao.selectById(helpDocId)).thenReturn(entity);
      when(helpDocDao.queryRelationByHelpDoc(helpDocId)).thenReturn(relationList);

      // When
      HelpDocDetailVO result = helpDocService.getDetail(helpDocId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getHelpDocId()).isEqualTo(helpDocId);
      assertThat(result.getRelationList()).hasSize(1);
    }

    @Test
    @DisplayName("數據不存在：應該返回 null")
    void shouldReturnNullWhenNotExists() {
      // Given
      Long helpDocId = 999L;
      when(helpDocDao.selectById(helpDocId)).thenReturn(null);

      // When
      HelpDocDetailVO result = helpDocService.getDetail(helpDocId);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== queryHelpDocByRelationId 測試 ====================

  @Nested
  @DisplayName("queryHelpDocByRelationId 按關聯查詢測試")
  class QueryByRelationIdTest {

    @Test
    @DisplayName("正常情況：應該返回文檔列表")
    void shouldReturnDocList() {
      // Given
      Long relationId = 1L;
      HelpDocVO vo = createTestHelpDocVO();
      when(helpDocDao.queryHelpDocByRelationId(relationId)).thenReturn(List.of(vo));

      // When
      List<HelpDocVO> result = helpDocService.queryHelpDocByRelationId(relationId);

      // Then
      assertThat(result).hasSize(1);
    }
  }

  // ==================== Helper Methods ====================

  private HelpDocVO createTestHelpDocVO() {
    HelpDocVO vo = new HelpDocVO();
    vo.setHelpDocId(1L);
    vo.setTitle("測試文檔");
    vo.setHelpDocCatalogId(1L);
    vo.setSort(1);
    vo.setCreateTime(LocalDateTime.now());
    return vo;
  }

  private HelpDocAddForm createTestHelpDocAddForm() {
    HelpDocAddForm form = new HelpDocAddForm();
    form.setTitle("測試文檔");
    form.setContentText("測試內容");
    form.setContentHtml("<p>測試內容</p>");
    form.setHelpDocCatalogId(1L);
    form.setSort(1);
    form.setRelationList(List.of());
    return form;
  }

  private HelpDocUpdateForm createTestHelpDocUpdateForm() {
    HelpDocUpdateForm form = new HelpDocUpdateForm();
    form.setHelpDocId(1L);
    form.setTitle("更新文檔");
    form.setContentText("更新內容");
    form.setContentHtml("<p>更新內容</p>");
    form.setHelpDocCatalogId(1L);
    form.setSort(1);
    form.setRelationList(List.of());
    return form;
  }

  private HelpDocEntity createTestHelpDocEntity() {
    HelpDocEntity entity = new HelpDocEntity();
    entity.setHelpDocId(1L);
    entity.setTitle("測試文檔");
    entity.setContentText("測試內容");
    entity.setContentHtml("<p>測試內容</p>");
    entity.setHelpDocCatalogId(1L);
    entity.setSort(1);
    entity.setCreateTime(LocalDateTime.now());
    return entity;
  }

  private HelpDocRelationVO createTestHelpDocRelationVO() {
    HelpDocRelationVO vo = new HelpDocRelationVO();
    vo.setRelationId(1L);
    vo.setRelationName("相關模組");
    return vo;
  }
}
