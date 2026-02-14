package net.lab1024.sa.system.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.role.dao.RoleDataScopeDao;
import net.lab1024.sa.system.role.domain.entity.RoleDataScopeEntity;
import net.lab1024.sa.system.role.domain.form.RoleDataScopeUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleDataScopeVO;
import net.lab1024.sa.system.role.manager.RoleDataScopeManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleDataScopeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>角色數據範圍查詢
 *   <li>批量更新角色數據範圍
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleDataScopeService 單元測試")
class RoleDataScopeServiceTest {

  @Mock private RoleDataScopeManager roleDataScopeManager;

  @Mock private RoleDataScopeDao roleDataScopeDao;

  @InjectMocks private RoleDataScopeService roleDataScopeService;

  // ==================== getRoleDataScopeList 測試 ====================

  @Nested
  @DisplayName("getRoleDataScopeList 角色數據範圍查詢測試")
  class GetRoleDataScopeListTest {

    @Test
    @DisplayName("正常情況：應該返回角色數據範圍列表")
    void shouldReturnDataScopeList() {
      // Given
      Long roleId = 1L;
      RoleDataScopeEntity entity1 = createTestEntity(1L, roleId, 1);
      RoleDataScopeEntity entity2 = createTestEntity(2L, roleId, 2);
      List<RoleDataScopeEntity> entityList = Arrays.asList(entity1, entity2);

      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(entityList);

      // When
      ResponseDTO<List<RoleDataScopeVO>> result = roleDataScopeService.getRoleDataScopeList(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("空結果：應該返回空列表")
    void shouldReturnEmptyListWhenNoData() {
      // Given
      Long roleId = 1L;
      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<List<RoleDataScopeVO>> result = roleDataScopeService.getRoleDataScopeList(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEmpty();
    }

    @Test
    @DisplayName("null 結果：應該返回空列表")
    void shouldReturnEmptyListWhenNull() {
      // Given
      Long roleId = 1L;
      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(null);

      // When
      ResponseDTO<List<RoleDataScopeVO>> result = roleDataScopeService.getRoleDataScopeList(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEmpty();
    }
  }

  // ==================== updateRoleDataScopeList 測試 ====================

  @Nested
  @DisplayName("updateRoleDataScopeList 批量更新測試")
  class UpdateRoleDataScopeListTest {

    @Test
    @DisplayName("正常情況：應該成功更新數據範圍")
    void shouldUpdateSuccess() {
      // Given
      RoleDataScopeUpdateForm updateForm = new RoleDataScopeUpdateForm();
      updateForm.setRoleId(1L);

      RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem item1 =
          new RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem();
      item1.setDataScopeType(1);
      item1.setViewType(1);

      RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem item2 =
          new RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem();
      item2.setDataScopeType(2);
      item2.setViewType(2);

      updateForm.setDataScopeItemList(Arrays.asList(item1, item2));

      // When
      ResponseDTO<String> result = roleDataScopeService.updateRoleDataScopeList(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleDataScopeManager).updateRoleDataScopeListTransaction(eq(1L), any(List.class));
    }

    @Test
    @DisplayName("異常情況：配置信息為空時應該返回錯誤")
    void shouldReturnErrorWhenConfigEmpty() {
      // Given
      RoleDataScopeUpdateForm updateForm = new RoleDataScopeUpdateForm();
      updateForm.setRoleId(1L);
      updateForm.setDataScopeItemList(Collections.emptyList());

      // When
      ResponseDTO<String> result = roleDataScopeService.updateRoleDataScopeList(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("配置信息");
      verify(roleDataScopeManager, never()).updateRoleDataScopeListTransaction(any(), any());
    }

    @Test
    @DisplayName("異常情況：配置信息為 null 時應該返回錯誤")
    void shouldReturnErrorWhenConfigNull() {
      // Given
      RoleDataScopeUpdateForm updateForm = new RoleDataScopeUpdateForm();
      updateForm.setRoleId(1L);
      updateForm.setDataScopeItemList(null);

      // When
      ResponseDTO<String> result = roleDataScopeService.updateRoleDataScopeList(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(roleDataScopeManager, never()).updateRoleDataScopeListTransaction(any(), any());
    }
  }

  // ==================== Helper Methods ====================

  private RoleDataScopeEntity createTestEntity(Long id, Long roleId, Integer dataScopeType) {
    RoleDataScopeEntity entity = new RoleDataScopeEntity();
    entity.setId(id);
    entity.setRoleId(roleId);
    entity.setDataScopeType(dataScopeType);
    entity.setViewType(1);
    return entity;
  }
}
