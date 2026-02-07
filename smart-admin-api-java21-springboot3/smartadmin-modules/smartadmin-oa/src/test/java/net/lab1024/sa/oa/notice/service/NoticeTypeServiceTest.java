package net.lab1024.sa.oa.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.oa.notice.dao.NoticeTypeDao;
import net.lab1024.sa.oa.notice.domain.entity.NoticeTypeEntity;
import net.lab1024.sa.oa.notice.domain.vo.NoticeTypeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NoticeTypeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>通知類型 CRUD 操作
 *   <li>類型名稱重複校驗
 *   <li>Vavr Option 使用模式
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeTypeService 單元測試")
class NoticeTypeServiceTest {

  @Mock private NoticeTypeDao noticeTypeDao;

  @InjectMocks private NoticeTypeService noticeTypeService;

  // ==================== getAll 測試 ====================

  @Nested
  @DisplayName("getAll 查詢所有類型測試")
  class GetAllTest {

    @Test
    @DisplayName("正常情況：應該返回所有類型列表")
    void shouldReturnAllTypes() {
      // Given
      NoticeTypeEntity type1 = createTestNoticeTypeEntity(1L, "通知");
      NoticeTypeEntity type2 = createTestNoticeTypeEntity(2L, "公告");
      when(noticeTypeDao.selectList(null)).thenReturn(Arrays.asList(type1, type2));

      // When
      List<NoticeTypeVO> result = noticeTypeService.getAll();

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("邊界情況：無類型時應返回空列表")
    void shouldReturnEmptyWhenNoTypes() {
      // Given
      when(noticeTypeDao.selectList(null)).thenReturn(Collections.emptyList());

      // When
      List<NoticeTypeVO> result = noticeTypeService.getAll();

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== getByNoticeTypeId 測試 ====================

  @Nested
  @DisplayName("getByNoticeTypeId 根據ID查詢測試")
  class GetByNoticeTypeIdTest {

    @Test
    @DisplayName("正常情況：應該返回類型詳情")
    void shouldReturnTypeDetail() {
      // Given
      Long noticeTypeId = 1L;
      NoticeTypeEntity entity = createTestNoticeTypeEntity(noticeTypeId, "通知");
      when(noticeTypeDao.selectById(noticeTypeId)).thenReturn(entity);

      // When
      NoticeTypeVO result = noticeTypeService.getByNoticeTypeId(noticeTypeId);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getNoticeTypeName()).isEqualTo("通知");
    }

    @Test
    @DisplayName("邊界情況：ID不存在時應返回null")
    void shouldReturnNullWhenNotFound() {
      // Given
      Long noticeTypeId = 999L;
      when(noticeTypeDao.selectById(noticeTypeId)).thenReturn(null);

      // When
      NoticeTypeVO result = noticeTypeService.getByNoticeTypeId(noticeTypeId);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增類型測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增類型")
    void shouldAddTypeSuccess() {
      // Given
      String name = "新類型";
      when(noticeTypeDao.selectList(null)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<String> result = noticeTypeService.add(name);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeTypeDao).insert(any(NoticeTypeEntity.class));
    }

    @Test
    @DisplayName("異常情況：名稱為空時應返回錯誤")
    void shouldReturnErrorWhenNameEmpty() {
      // When
      ResponseDTO<String> result = noticeTypeService.add("");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类型名称不能为空");
      verify(noticeTypeDao, never()).insert(any(NoticeTypeEntity.class));
    }

    @Test
    @DisplayName("異常情況：名稱為null時應返回錯誤")
    void shouldReturnErrorWhenNameNull() {
      // When
      ResponseDTO<String> result = noticeTypeService.add(null);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类型名称不能为空");
    }

    @Test
    @DisplayName("異常情況：名稱已存在時應返回錯誤")
    void shouldReturnErrorWhenNameExists() {
      // Given
      String name = "已存在類型";
      NoticeTypeEntity existingType = createTestNoticeTypeEntity(1L, name);
      when(noticeTypeDao.selectList(null)).thenReturn(Collections.singletonList(existingType));

      // When
      ResponseDTO<String> result = noticeTypeService.add(name);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类型名称已经存在");
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新類型測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新類型")
    void shouldUpdateTypeSuccess() {
      // Given
      Long noticeTypeId = 1L;
      String newName = "更新後名稱";
      NoticeTypeEntity existingEntity = createTestNoticeTypeEntity(noticeTypeId, "原名稱");

      when(noticeTypeDao.selectById(noticeTypeId)).thenReturn(existingEntity);
      when(noticeTypeDao.selectList(null)).thenReturn(Collections.singletonList(existingEntity));

      // When
      ResponseDTO<String> result = noticeTypeService.update(noticeTypeId, newName);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeTypeDao).updateById(any(NoticeTypeEntity.class));
    }

    @Test
    @DisplayName("異常情況：名稱為空時應返回錯誤")
    void shouldReturnErrorWhenNameEmpty() {
      // When
      ResponseDTO<String> result = noticeTypeService.update(1L, "");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类型名称不能为空");
    }

    @Test
    @DisplayName("異常情況：類型不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      Long noticeTypeId = 999L;
      when(noticeTypeDao.selectById(noticeTypeId)).thenReturn(null);

      // When
      ResponseDTO<String> result = noticeTypeService.update(noticeTypeId, "新名稱");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类型名称不存在");
    }

    @Test
    @DisplayName("異常情況：更新時名稱與其他類型重複應返回錯誤")
    void shouldReturnErrorWhenNameConflict() {
      // Given
      Long noticeTypeId = 1L;
      String conflictName = "衝突名稱";
      NoticeTypeEntity currentEntity = createTestNoticeTypeEntity(noticeTypeId, "原名稱");
      NoticeTypeEntity conflictEntity = createTestNoticeTypeEntity(2L, conflictName);

      when(noticeTypeDao.selectById(noticeTypeId)).thenReturn(currentEntity);
      when(noticeTypeDao.selectList(null)).thenReturn(Arrays.asList(currentEntity, conflictEntity));

      // When
      ResponseDTO<String> result = noticeTypeService.update(noticeTypeId, conflictName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类型名称已经存在");
    }

    @Test
    @DisplayName("正常情況：更新為相同名稱應該成功（同一個類型）")
    void shouldSuccessWhenSameName() {
      // Given
      Long noticeTypeId = 1L;
      String sameName = "相同名稱";
      NoticeTypeEntity currentEntity = createTestNoticeTypeEntity(noticeTypeId, sameName);

      when(noticeTypeDao.selectById(noticeTypeId)).thenReturn(currentEntity);
      when(noticeTypeDao.selectList(null)).thenReturn(Collections.singletonList(currentEntity));

      // When
      ResponseDTO<String> result = noticeTypeService.update(noticeTypeId, sameName);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除類型測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除類型")
    void shouldDeleteTypeSuccess() {
      // Given
      Long noticeTypeId = 1L;

      // When
      ResponseDTO<String> result = noticeTypeService.delete(noticeTypeId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(noticeTypeDao).deleteById(noticeTypeId);
    }
  }

  // ==================== Helper Methods ====================

  private NoticeTypeEntity createTestNoticeTypeEntity(Long id, String name) {
    NoticeTypeEntity entity = new NoticeTypeEntity();
    entity.setNoticeTypeId(id);
    entity.setNoticeTypeName(name);
    return entity;
  }
}
