package net.lab1024.sa.support.liteflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainQueryForm;
import net.lab1024.sa.support.liteflow.domain.form.LiteFlowChainUpdateForm;
import net.lab1024.sa.support.liteflow.domain.vo.LiteFlowChainVO;
import net.lab1024.sa.support.liteflow.manager.LiteFlowChainManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LiteFlowChainService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryPage - 分頁查詢流程
 *   <li>add/update/delete - 委托給 Manager
 *   <li>getDetail - 獲取流程詳情
 *   <li>reloadAll - 重載所有規則
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiteFlowChainService 單元測試")
class LiteFlowChainServiceTest {

  @Mock private LiteFlowChainDao chainDao;

  @Mock private LiteFlowChainManager chainManager;

  @InjectMocks private LiteFlowChainService chainService;

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
      LiteFlowChainQueryForm form = new LiteFlowChainQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      LiteFlowChainEntity entity = createTestEntity(1L, "test-chain");
      Page<LiteFlowChainEntity> page = new Page<>(1, 10);
      page.setRecords(List.of(entity));
      page.setTotal(1);

      when(chainDao.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      // When
      ResponseDTO<PageResult<LiteFlowChainVO>> result = chainService.queryPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      verify(chainDao).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("帶查詢條件：應該正確過濾")
    void shouldFilterByConditions() {
      // Given
      LiteFlowChainQueryForm form = new LiteFlowChainQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);
      form.setChainName("test");
      form.setStatus(1);

      Page<LiteFlowChainEntity> page = new Page<>(1, 10);
      page.setRecords(List.of());
      page.setTotal(0);

      when(chainDao.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

      // When
      ResponseDTO<PageResult<LiteFlowChainVO>> result = chainService.queryPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(chainDao).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
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
      LiteFlowChainAddForm form = new LiteFlowChainAddForm();
      form.setChainName("Test Chain");
      form.setChainCode("test-chain");

      when(chainManager.add(form, TEST_USER_ID, TEST_USER_NAME)).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = chainService.add(form, TEST_USER_ID, TEST_USER_NAME);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(chainManager).add(form, TEST_USER_ID, TEST_USER_NAME);
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
      LiteFlowChainUpdateForm form = new LiteFlowChainUpdateForm();
      form.setChainId(1L);
      form.setChainName("Updated Chain");

      when(chainManager.update(form, TEST_USER_ID, TEST_USER_NAME)).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = chainService.update(form, TEST_USER_ID, TEST_USER_NAME);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(chainManager).update(form, TEST_USER_ID, TEST_USER_NAME);
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
      Long chainId = 1L;

      when(chainManager.delete(chainId, TEST_USER_ID, TEST_USER_NAME)).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = chainService.delete(chainId, TEST_USER_ID, TEST_USER_NAME);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(chainManager).delete(chainId, TEST_USER_ID, TEST_USER_NAME);
    }
  }

  // ==================== getDetail 測試 ====================

  @Nested
  @DisplayName("getDetail 獲取詳情測試")
  class GetDetailTest {

    @Test
    @DisplayName("正常情況：應該返回流程詳情")
    void shouldReturnChainDetail() {
      // Given
      Long chainId = 1L;
      LiteFlowChainEntity entity = createTestEntity(chainId, "test-chain");
      entity.setDeletedFlag(0);

      when(chainDao.selectById(chainId)).thenReturn(entity);

      // When
      ResponseDTO<LiteFlowChainVO> result = chainService.getDetail(chainId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getChainCode()).isEqualTo("test-chain");
    }

    @Test
    @DisplayName("流程不存在時：應該返回錯誤")
    void shouldReturnErrorWhenChainNotExists() {
      // Given
      Long chainId = 999L;
      when(chainDao.selectById(chainId)).thenReturn(null);

      // When
      ResponseDTO<LiteFlowChainVO> result = chainService.getDetail(chainId);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("流程已刪除時：應該返回錯誤")
    void shouldReturnErrorWhenChainDeleted() {
      // Given
      Long chainId = 1L;
      LiteFlowChainEntity entity = createTestEntity(chainId, "test-chain");
      entity.setDeletedFlag(1);

      when(chainDao.selectById(chainId)).thenReturn(entity);

      // When
      ResponseDTO<LiteFlowChainVO> result = chainService.getDetail(chainId);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== reloadAll 測試 ====================

  @Nested
  @DisplayName("reloadAll 重載規則測試")
  class ReloadAllTest {

    @Test
    @DisplayName("正常情況：應該委托給 Manager")
    void shouldDelegateToManager() {
      // Given
      when(chainManager.reloadAll()).thenReturn(ResponseDTO.ok());

      // When
      ResponseDTO<String> result = chainService.reloadAll();

      // Then
      assertThat(result.getOk()).isTrue();
      verify(chainManager).reloadAll();
    }
  }

  // ==================== Helper Methods ====================

  private LiteFlowChainEntity createTestEntity(Long chainId, String chainCode) {
    LiteFlowChainEntity entity = new LiteFlowChainEntity();
    entity.setChainId(chainId);
    entity.setChainCode(chainCode);
    entity.setChainName("Test Chain");
    entity.setChainType(1);
    entity.setStatus(1);
    entity.setDeletedFlag(0);
    return entity;
  }
}
