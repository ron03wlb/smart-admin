package net.lab1024.sa.support.reload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.reload.dao.ReloadItemDao;
import net.lab1024.sa.support.reload.dao.ReloadResultDao;
import net.lab1024.sa.support.reload.domain.ReloadForm;
import net.lab1024.sa.support.reload.domain.ReloadItemEntity;
import net.lab1024.sa.support.reload.domain.ReloadItemVO;
import net.lab1024.sa.support.reload.domain.ReloadResultVO;
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
 * ReloadService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>query - 查詢重載項目列表
 *   <li>queryReloadItemResult - 查詢重載結果
 *   <li>updateByTag - 更新標識符
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReloadService 單元測試")
class ReloadServiceTest {

  @Mock private ReloadItemDao reloadItemDao;

  @Mock private ReloadResultDao reloadResultDao;

  @InjectMocks private ReloadService reloadService;

  @Captor private ArgumentCaptor<ReloadItemEntity> entityCaptor;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 查詢列表測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回重載項目列表")
    void shouldReturnReloadItemList() {
      // Given
      ReloadItemVO vo = createTestReloadItemVO();
      when(reloadItemDao.query()).thenReturn(List.of(vo));

      // When
      ResponseDTO<List<ReloadItemVO>> result = reloadService.query();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }

    @Test
    @DisplayName("空結果：應該返回空列表")
    void shouldReturnEmptyList() {
      // Given
      when(reloadItemDao.query()).thenReturn(List.of());

      // When
      ResponseDTO<List<ReloadItemVO>> result = reloadService.query();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEmpty();
    }
  }

  // ==================== queryReloadItemResult 測試 ====================

  @Nested
  @DisplayName("queryReloadItemResult 查詢結果測試")
  class QueryReloadItemResultTest {

    @Test
    @DisplayName("正常情況：應該返回重載結果")
    void shouldReturnReloadResultList() {
      // Given
      String tag = "DICT_RELOAD";
      ReloadResultVO vo = createTestReloadResultVO();
      when(reloadResultDao.query(tag)).thenReturn(List.of(vo));

      // When
      ResponseDTO<List<ReloadResultVO>> result = reloadService.queryReloadItemResult(tag);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }
  }

  // ==================== updateByTag 測試 ====================

  @Nested
  @DisplayName("updateByTag 更新標識符測試")
  class UpdateByTagTest {

    @Test
    @DisplayName("正常情況：應該成功更新標識符")
    void shouldUpdateSuccessfully() {
      // Given
      ReloadForm form = createTestReloadForm();
      ReloadItemEntity entity = createTestReloadItemEntity();
      when(reloadItemDao.selectById(form.getTag())).thenReturn(entity);

      // When
      ResponseDTO<String> result = reloadService.updateByTag(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(reloadItemDao).updateById(entityCaptor.capture());
      ReloadItemEntity captured = entityCaptor.getValue();
      assertThat(captured.getIdentification()).isEqualTo("new-identification");
      assertThat(captured.getArgs()).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("數據不存在：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      ReloadForm form = createTestReloadForm();
      when(reloadItemDao.selectById(form.getTag())).thenReturn(null);

      // When
      ResponseDTO<String> result = reloadService.updateByTag(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(reloadItemDao, never()).updateById(any(ReloadItemEntity.class));
    }
  }

  // ==================== Helper Methods ====================

  private ReloadItemVO createTestReloadItemVO() {
    ReloadItemVO vo = new ReloadItemVO();
    vo.setTag("DICT_RELOAD");
    vo.setIdentification("v1");
    vo.setArgs("{}");
    return vo;
  }

  private ReloadResultVO createTestReloadResultVO() {
    ReloadResultVO vo = new ReloadResultVO();
    vo.setTag("DICT_RELOAD");
    vo.setArgs("{}");
    vo.setResult(true);
    vo.setCreateTime(LocalDateTime.now());
    return vo;
  }

  private ReloadForm createTestReloadForm() {
    ReloadForm form = new ReloadForm();
    form.setTag("DICT_RELOAD");
    form.setIdentification("new-identification");
    form.setArgs("{\"key\":\"value\"}");
    return form;
  }

  private ReloadItemEntity createTestReloadItemEntity() {
    ReloadItemEntity entity = new ReloadItemEntity();
    entity.setTag("DICT_RELOAD");
    entity.setIdentification("v1");
    entity.setArgs("{}");
    entity.setUpdateTime(LocalDateTime.now());
    return entity;
  }
}
