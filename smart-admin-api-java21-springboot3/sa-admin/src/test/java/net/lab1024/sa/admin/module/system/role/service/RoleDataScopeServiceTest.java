package net.lab1024.sa.admin.module.system.role.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.admin.module.system.role.RoleDataScopeTestFixture;
import net.lab1024.sa.admin.module.system.role.dao.RoleDataScopeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleDataScopeEntity;
import net.lab1024.sa.admin.module.system.role.domain.form.RoleDataScopeUpdateForm;
import net.lab1024.sa.admin.module.system.role.domain.vo.RoleDataScopeVO;
import net.lab1024.sa.admin.module.system.role.manager.RoleDataScopeManager;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleDataScopeService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>获取角色数据范围列表（空结果、有结果）
 *   <li>批量更新角色数据范围（配置信息校验、Manager事务调用）
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleDataScopeService 单元测试")
class RoleDataScopeServiceTest {

  @Mock private RoleDataScopeManager roleDataScopeManager;

  @Mock private RoleDataScopeDao roleDataScopeDao;

  @InjectMocks private RoleDataScopeService roleDataScopeService;

  @BeforeEach
  void setUp() {
    RoleDataScopeTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("getRoleDataScopeList() - 获取角色数据范围列表")
  class GetRoleDataScopeListTests {

    @Test
    @DisplayName("正常获取数据范围列表 - 应返回VO列表")
    void getRoleDataScopeList_ValidRoleId_ShouldReturnVOList() {
      // Arrange
      Long roleId = 1L;
      List<RoleDataScopeEntity> entityList = RoleDataScopeTestFixture.createEntityList(roleId, 3);

      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(entityList);

      // Act
      ResponseDTO<List<RoleDataScopeVO>> response =
          roleDataScopeService.getRoleDataScopeList(roleId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertEquals(3, response.getData().size());
      verify(roleDataScopeDao, times(1)).listByRoleId(roleId);
    }

    @Test
    @DisplayName("角色无数据范围配置 - 应返回空列表")
    void getRoleDataScopeList_NoDataScope_ShouldReturnEmptyList() {
      // Arrange
      Long roleId = 2L;
      List<RoleDataScopeEntity> emptyList = Collections.emptyList();

      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(emptyList);

      // Act
      ResponseDTO<List<RoleDataScopeVO>> response =
          roleDataScopeService.getRoleDataScopeList(roleId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().isEmpty());
      verify(roleDataScopeDao, times(1)).listByRoleId(roleId);
    }

    @Test
    @DisplayName("查询结果为null - 应返回空列表")
    void getRoleDataScopeList_NullResult_ShouldReturnEmptyList() {
      // Arrange
      Long roleId = 3L;

      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(null);

      // Act
      ResponseDTO<List<RoleDataScopeVO>> response =
          roleDataScopeService.getRoleDataScopeList(roleId);

      // Assert
      assertTrue(response.getOk());
      assertNotNull(response.getData());
      assertTrue(response.getData().isEmpty());
    }

    @Test
    @DisplayName("Bean转换正确性 - VO字段应正确映射")
    void getRoleDataScopeList_BeanConversion_ShouldMapFieldsCorrectly() {
      // Arrange
      Long roleId = 1L;
      RoleDataScopeEntity entity1 =
          RoleDataScopeTestFixture.createDepartmentDataScopeEntity(roleId);
      RoleDataScopeEntity entity2 =
          RoleDataScopeTestFixture.createGoodsCategoryDataScopeEntity(roleId);
      List<RoleDataScopeEntity> entityList = Arrays.asList(entity1, entity2);

      when(roleDataScopeManager.getBaseMapper()).thenReturn(roleDataScopeDao);
      when(roleDataScopeDao.listByRoleId(roleId)).thenReturn(entityList);

      // Act
      ResponseDTO<List<RoleDataScopeVO>> response =
          roleDataScopeService.getRoleDataScopeList(roleId);

      // Assert
      assertTrue(response.getOk());
      List<RoleDataScopeVO> voList = response.getData();
      assertEquals(2, voList.size());

      // Verify first VO (Department)
      RoleDataScopeVO vo1 = voList.get(0);
      assertEquals(entity1.getDataScopeType(), vo1.getDataScopeType());
      assertEquals(entity1.getViewType(), vo1.getViewType());

      // Verify second VO (Goods Category)
      RoleDataScopeVO vo2 = voList.get(1);
      assertEquals(entity2.getDataScopeType(), vo2.getDataScopeType());
      assertEquals(entity2.getViewType(), vo2.getViewType());
    }
  }

  @Nested
  @DisplayName("updateRoleDataScopeList() - 批量更新角色数据范围")
  class UpdateRoleDataScopeListTests {

    @Test
    @DisplayName("正常更新数据范围 - 应返回成功")
    void updateRoleDataScopeList_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long roleId = 1L;
      List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> formItems =
          RoleDataScopeTestFixture.createFormItemList(3);
      RoleDataScopeUpdateForm form = RoleDataScopeTestFixture.createUpdateForm(roleId, formItems);

      doNothing()
          .when(roleDataScopeManager)
          .updateRoleDataScopeListTransaction(eq(roleId), anyList());

      // Act
      ResponseDTO<String> response = roleDataScopeService.updateRoleDataScopeList(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleDataScopeManager, times(1))
          .updateRoleDataScopeListTransaction(eq(roleId), anyList());
    }

    @Test
    @DisplayName("配置信息为空 - 应返回错误")
    void updateRoleDataScopeList_EmptyDataScopeList_ShouldReturnError() {
      // Arrange
      Long roleId = 1L;
      List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> emptyList =
          Collections.emptyList();
      RoleDataScopeUpdateForm form = RoleDataScopeTestFixture.createUpdateForm(roleId, emptyList);

      // Act
      ResponseDTO<String> response = roleDataScopeService.updateRoleDataScopeList(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("缺少配置信息"));
      verify(roleDataScopeManager, never())
          .updateRoleDataScopeListTransaction(anyLong(), anyList());
    }

    @Test
    @DisplayName("配置信息为null - 应返回错误")
    void updateRoleDataScopeList_NullDataScopeList_ShouldReturnError() {
      // Arrange
      Long roleId = 1L;
      RoleDataScopeUpdateForm form = RoleDataScopeTestFixture.createUpdateForm(roleId, null);

      // Act
      ResponseDTO<String> response = roleDataScopeService.updateRoleDataScopeList(form);

      // Assert
      assertFalse(response.getOk());
      assertTrue(response.getMsg().contains("缺少配置信息"));
      verify(roleDataScopeManager, never())
          .updateRoleDataScopeListTransaction(anyLong(), anyList());
    }

    @Test
    @DisplayName("单条配置更新 - 应返回成功")
    void updateRoleDataScopeList_SingleItem_ShouldReturnSuccess() {
      // Arrange
      Long roleId = 2L;
      RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem singleItem =
          RoleDataScopeTestFixture.createFormItem(1, 1);
      RoleDataScopeUpdateForm form =
          RoleDataScopeTestFixture.createUpdateForm(roleId, Arrays.asList(singleItem));

      doNothing()
          .when(roleDataScopeManager)
          .updateRoleDataScopeListTransaction(eq(roleId), anyList());

      // Act
      ResponseDTO<String> response = roleDataScopeService.updateRoleDataScopeList(form);

      // Assert
      assertTrue(response.getOk());
      verify(roleDataScopeManager, times(1))
          .updateRoleDataScopeListTransaction(eq(roleId), anyList());
    }

    @Test
    @DisplayName("批量配置更新 - 应正确设置roleId")
    void updateRoleDataScopeList_MultipleItems_ShouldSetRoleIdCorrectly() {
      // Arrange
      Long roleId = 3L;
      List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> formItems =
          RoleDataScopeTestFixture.createFormItemList(5);
      RoleDataScopeUpdateForm form = RoleDataScopeTestFixture.createUpdateForm(roleId, formItems);

      doNothing()
          .when(roleDataScopeManager)
          .updateRoleDataScopeListTransaction(eq(roleId), anyList());

      // Act
      ResponseDTO<String> response = roleDataScopeService.updateRoleDataScopeList(form);

      // Assert
      assertTrue(response.getOk());

      // Verify that updateRoleDataScopeListTransaction was called with correct roleId
      verify(roleDataScopeManager, times(1))
          .updateRoleDataScopeListTransaction(
              eq(roleId),
              argThat(
                  list -> {
                    // Verify all entities have the correct roleId set
                    return list.stream()
                        .allMatch(
                            entity -> roleId.equals(((RoleDataScopeEntity) entity).getRoleId()));
                  }));
    }
  }
}
