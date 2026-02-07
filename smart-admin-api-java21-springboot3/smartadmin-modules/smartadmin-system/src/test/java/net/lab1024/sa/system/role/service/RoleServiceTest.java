package net.lab1024.sa.system.role.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.role.dao.RoleDao;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.domain.entity.RoleEntity;
import net.lab1024.sa.system.role.domain.form.RoleAddForm;
import net.lab1024.sa.system.role.domain.form.RoleUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleVO;
import net.lab1024.sa.system.role.manager.RoleManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>角色 CRUD 操作
 *   <li>角色名稱/編碼重複校驗
 *   <li>刪除時員工綁定檢查
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RoleService 單元測試")
class RoleServiceTest {

  @Mock private RoleDao roleDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @Mock private RoleManager roleManager;

  @InjectMocks private RoleService roleService;

  // ==================== addRole 測試 ====================

  @Nested
  @DisplayName("addRole 新增角色測試")
  class AddRoleTest {

    @Test
    @DisplayName("正常情況：應該成功新增角色")
    void shouldAddRoleSuccess() {
      // Given
      RoleAddForm addForm = createTestAddForm("Admin", "ADMIN");
      when(roleDao.getByRoleName("Admin")).thenReturn(null);
      when(roleDao.getByRoleCode("ADMIN")).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.addRole(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleDao).insert(any(RoleEntity.class));
    }

    @Test
    @DisplayName("異常情況：角色名稱重複時應返回錯誤")
    void shouldReturnErrorWhenNameExists() {
      // Given
      RoleAddForm addForm = createTestAddForm("Admin", "ADMIN");
      RoleEntity existingRole = createTestRoleEntity(1L, "Admin", "ADMIN");
      when(roleDao.getByRoleName("Admin")).thenReturn(existingRole);

      // When
      ResponseDTO<String> result = roleService.addRole(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("角色名称重复");
      verify(roleDao, never()).insert(any(RoleEntity.class));
    }

    @Test
    @DisplayName("異常情況：角色編碼重複時應返回錯誤")
    void shouldReturnErrorWhenCodeExists() {
      // Given
      RoleAddForm addForm = createTestAddForm("NewAdmin", "ADMIN");
      RoleEntity existingRole = createTestRoleEntity(1L, "OldAdmin", "ADMIN");
      when(roleDao.getByRoleName("NewAdmin")).thenReturn(null);
      when(roleDao.getByRoleCode("ADMIN")).thenReturn(existingRole);

      // When
      ResponseDTO<String> result = roleService.addRole(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("角色编码重复");
      verify(roleDao, never()).insert(any(RoleEntity.class));
    }
  }

  // ==================== updateRole 測試 ====================

  @Nested
  @DisplayName("updateRole 更新角色測試")
  class UpdateRoleTest {

    @Test
    @DisplayName("正常情況：應該成功更新角色")
    void shouldUpdateRoleSuccess() {
      // Given
      RoleUpdateForm updateForm = createTestUpdateForm(1L, "UpdatedAdmin", "ADMIN");
      RoleEntity existingRole = createTestRoleEntity(1L, "Admin", "ADMIN");

      when(roleDao.selectById(1L)).thenReturn(existingRole);
      when(roleDao.getByRoleName("UpdatedAdmin")).thenReturn(null);
      when(roleDao.getByRoleCode("ADMIN")).thenReturn(existingRole); // 同一個角色，允許

      // When
      ResponseDTO<String> result = roleService.updateRole(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleManager).updateRoleTransaction(any(RoleEntity.class));
    }

    @Test
    @DisplayName("異常情況：角色不存在時應返回錯誤")
    void shouldReturnErrorWhenRoleNotFound() {
      // Given
      RoleUpdateForm updateForm = createTestUpdateForm(999L, "Admin", "ADMIN");
      when(roleDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.updateRole(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(roleManager, never()).updateRoleTransaction(any());
    }

    @Test
    @DisplayName("異常情況：角色名稱與其他角色重複時應返回錯誤")
    void shouldReturnErrorWhenNameConflict() {
      // Given
      RoleUpdateForm updateForm = createTestUpdateForm(1L, "Admin", "ADMIN");
      RoleEntity currentRole = createTestRoleEntity(1L, "OldName", "ADMIN");
      RoleEntity conflictRole = createTestRoleEntity(2L, "Admin", "OTHER");

      when(roleDao.selectById(1L)).thenReturn(currentRole);
      when(roleDao.getByRoleName("Admin")).thenReturn(conflictRole);

      // When
      ResponseDTO<String> result = roleService.updateRole(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("角色名称重复");
    }

    @Test
    @DisplayName("異常情況：角色編碼與其他角色重複時應返回錯誤")
    void shouldReturnErrorWhenCodeConflict() {
      // Given
      RoleUpdateForm updateForm = createTestUpdateForm(1L, "Admin", "ADMIN");
      RoleEntity currentRole = createTestRoleEntity(1L, "Admin", "OLD_CODE");
      RoleEntity conflictRole = createTestRoleEntity(2L, "Other", "ADMIN");

      when(roleDao.selectById(1L)).thenReturn(currentRole);
      when(roleDao.getByRoleName("Admin")).thenReturn(null);
      when(roleDao.getByRoleCode("ADMIN")).thenReturn(conflictRole);

      // When
      ResponseDTO<String> result = roleService.updateRole(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("角色编码重复");
    }
  }

  // ==================== deleteRole 測試 ====================

  @Nested
  @DisplayName("deleteRole 刪除角色測試")
  class DeleteRoleTest {

    @Test
    @DisplayName("正常情況：無員工綁定時應成功刪除")
    void shouldDeleteRoleSuccess() {
      // Given
      Long roleId = 1L;
      RoleEntity roleEntity = createTestRoleEntity(roleId, "Admin", "ADMIN");

      when(roleDao.selectById(roleId)).thenReturn(roleEntity);
      when(roleEmployeeDao.existsByRoleId(roleId)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.deleteRole(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(roleManager).deleteRoleWithCascadeTransaction(roleId);
    }

    @Test
    @DisplayName("異常情況：角色不存在時應返回錯誤")
    void shouldReturnErrorWhenRoleNotFound() {
      // Given
      when(roleDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = roleService.deleteRole(999L);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(roleManager, never()).deleteRoleWithCascadeTransaction(any());
    }

    @Test
    @DisplayName("異常情況：有員工綁定時應返回錯誤")
    void shouldReturnErrorWhenHasEmployees() {
      // Given
      Long roleId = 1L;
      RoleEntity roleEntity = createTestRoleEntity(roleId, "Admin", "ADMIN");

      when(roleDao.selectById(roleId)).thenReturn(roleEntity);
      when(roleEmployeeDao.existsByRoleId(roleId)).thenReturn(1); // 存在員工

      // When
      ResponseDTO<String> result = roleService.deleteRole(roleId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("存在员工");
      verify(roleManager, never()).deleteRoleWithCascadeTransaction(any());
    }
  }

  // ==================== getRoleById 測試 ====================

  @Nested
  @DisplayName("getRoleById 查詢角色測試")
  class GetRoleByIdTest {

    @Test
    @DisplayName("正常情況：應該返回角色詳情")
    void shouldReturnRoleDetail() {
      // Given
      Long roleId = 1L;
      RoleEntity roleEntity = createTestRoleEntity(roleId, "Admin", "ADMIN");

      when(roleDao.selectById(roleId)).thenReturn(roleEntity);

      // When
      ResponseDTO<RoleVO> result = roleService.getRoleById(roleId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getRoleName()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("異常情況：角色不存在時應返回錯誤")
    void shouldReturnErrorWhenNotFound() {
      // Given
      when(roleDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<RoleVO> result = roleService.getRoleById(999L);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== getAllRole 測試 ====================

  @Nested
  @DisplayName("getAllRole 查詢所有角色測試")
  class GetAllRoleTest {

    @Test
    @DisplayName("正常情況：應該返回所有角色列表")
    void shouldReturnAllRoles() {
      // Given
      RoleEntity role1 = createTestRoleEntity(1L, "Admin", "ADMIN");
      RoleEntity role2 = createTestRoleEntity(2L, "User", "USER");

      when(roleDao.selectList(null)).thenReturn(Arrays.asList(role1, role2));

      // When
      ResponseDTO<List<RoleVO>> result = roleService.getAllRole();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("邊界情況：無角色時應返回空列表")
    void shouldReturnEmptyWhenNoRoles() {
      // Given
      when(roleDao.selectList(null)).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<List<RoleVO>> result = roleService.getAllRole();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEmpty();
    }
  }

  // ==================== Helper Methods ====================

  private RoleAddForm createTestAddForm(String name, String code) {
    RoleAddForm form = new RoleAddForm();
    form.setRoleName(name);
    form.setRoleCode(code);
    return form;
  }

  private RoleUpdateForm createTestUpdateForm(Long id, String name, String code) {
    RoleUpdateForm form = new RoleUpdateForm();
    form.setRoleId(id);
    form.setRoleName(name);
    form.setRoleCode(code);
    return form;
  }

  private RoleEntity createTestRoleEntity(Long id, String name, String code) {
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(id);
    entity.setRoleName(name);
    entity.setRoleCode(code);
    return entity;
  }
}
