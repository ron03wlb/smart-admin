package net.lab1024.sa.support.liteflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.dao.LiteFlowScriptDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowScriptEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowScriptUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowScriptVO;
import net.lab1024.sa.support.liteflow.manager.LiteFlowScriptManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LiteFlowScriptService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryPage - 分頁查詢腳本
 *   <li>add/update/delete - 委托給 Manager
 *   <li>getDetail - 獲取腳本詳情
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiteFlowScriptService 單元測試")
class LiteFlowScriptServiceTest {

  @Mock private LiteFlowScriptDao scriptDao;

  @Mock private LiteFlowScriptManager scriptManager;

  @InjectMocks private LiteFlowScriptService scriptService;

  private static final Long TEST_USER_ID = 1L;
  private static final String TEST_USER_NAME = "test-user";

  // ==================== queryPage 測試 ====================

  @Nested
  @DisplayName("queryPage 分頁查詢測試")
  class QueryPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      LiteFlowScriptQueryForm form = new LiteFlowScriptQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      LiteFlowScriptEntity entity = createTestEntity(1L, "test-script");
      Page<LiteFlowScriptEntity> page = new Page<>(1, 10);
      page.setRecords(List.of(entity));
      page.setTotal(1);

      when(scriptDao.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      // When
      ResponseDTO<PageResult<LiteFlowScriptVO>> result = scriptService.queryPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      verify(scriptDao).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("帶查詢條件：應該正確過濾")
    void shouldFilterByConditions() {
      // Given
      LiteFlowScriptQueryForm form = new LiteFlowScriptQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      form.setScriptName("test");
      form.setScriptType("groovy");
      form.setStatus(1);

      Page<LiteFlowScriptEntity> page = new Page<>(1, 10);
      page.setRecords(List.of());
      page.setTotal(0);

      when(scriptDao.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      // When
      ResponseDTO<PageResult<LiteFlowScriptVO>> result = scriptService.queryPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(scriptDao).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該委托給 Manager")
    void shouldDelegateToManager() {
      // Given
      LiteFlowScriptAddForm form = new LiteFlowScriptAddForm();
      form.setScriptName("Test Script");
      form.setScriptCode("test-script");

      when(scriptManager.add(form, TEST_USER_ID, TEST_USER_NAME)).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = scriptService.add(form, TEST_USER_ID, TEST_USER_NAME);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(scriptManager).add(form, TEST_USER_ID, TEST_USER_NAME);
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該委托給 Manager")
    void shouldDelegateToManager() {
      // Given
      LiteFlowScriptUpdateForm form = new LiteFlowScriptUpdateForm();
      form.setScriptId(1L);
      form.setScriptName("Updated Script");

      when(scriptManager.update(form, TEST_USER_ID, TEST_USER_NAME)).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = scriptService.update(form, TEST_USER_ID, TEST_USER_NAME);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(scriptManager).update(form, TEST_USER_ID, TEST_USER_NAME);
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該委托給 Manager")
    void shouldDelegateToManager() {
      // Given
      Long scriptId = 1L;

      when(scriptManager.delete(scriptId, TEST_USER_ID, TEST_USER_NAME))
          .thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = scriptService.delete(scriptId, TEST_USER_ID, TEST_USER_NAME);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(scriptManager).delete(scriptId, TEST_USER_ID, TEST_USER_NAME);
    }
  }

  // ==================== getDetail 測試 ====================

  @Nested
  @DisplayName("getDetail 獲取詳情測試")
  class GetDetailTest {

    @Test
    @DisplayName("正常情況：應該返回腳本詳情")
    void shouldReturnScriptDetail() {
      // Given
      Long scriptId = 1L;
      LiteFlowScriptEntity entity = createTestEntity(scriptId, "test-script");
      entity.setDeletedFlag(0);

      when(scriptDao.selectById(scriptId)).thenReturn(entity);

      // When
      ResponseDTO<LiteFlowScriptVO> result = scriptService.getDetail(scriptId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("腳本不存在時：應該返回錯誤")
    void shouldReturnErrorWhenScriptNotExists() {
      // Given
      Long scriptId = 999L;
      when(scriptDao.selectById(scriptId)).thenReturn(null);

      // When
      ResponseDTO<LiteFlowScriptVO> result = scriptService.getDetail(scriptId);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("腳本已刪除時：應該返回錯誤")
    void shouldReturnErrorWhenScriptDeleted() {
      // Given
      Long scriptId = 1L;
      LiteFlowScriptEntity entity = createTestEntity(scriptId, "test-script");
      entity.setDeletedFlag(1);

      when(scriptDao.selectById(scriptId)).thenReturn(entity);

      // When
      ResponseDTO<LiteFlowScriptVO> result = scriptService.getDetail(scriptId);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== Helper Methods ====================

  private LiteFlowScriptEntity createTestEntity(Long scriptId, String scriptCode) {
    LiteFlowScriptEntity entity = new LiteFlowScriptEntity();
    entity.setScriptId(scriptId);
    entity.setScriptCode(scriptCode);
    entity.setScriptName("Test Script");
    entity.setScriptType("groovy");
    entity.setStatus(1);
    entity.setDeletedFlag(0);
    return entity;
  }
}
