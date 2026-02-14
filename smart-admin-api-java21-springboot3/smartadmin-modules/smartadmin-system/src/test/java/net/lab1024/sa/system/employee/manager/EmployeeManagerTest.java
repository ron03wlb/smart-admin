package net.lab1024.sa.system.employee.manager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.system.role.domain.entity.RoleEmployeeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EmployeeManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>保存員工事務
 *   <li>更新員工事務
 *   <li>更新員工角色事務
 *   <li>更新密碼事務
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeManager 單元測試")
class EmployeeManagerTest {

  @Mock private EmployeeDao employeeDao;

  @Mock private RoleEmployeeDao roleEmployeeDao;

  @InjectMocks private EmployeeManager employeeManager;

  // ==================== saveEmployeeTransaction 測試 ====================

  @Nested
  @DisplayName("saveEmployeeTransaction 保存員工事務測試")
  class SaveEmployeeTransactionTest {

    @Test
    @DisplayName("正常情況：有角色時應該保存員工和角色")
    void shouldSaveEmployeeAndRoles() {
      // Given
      EmployeeEntity employee = createTestEmployee(null, "admin");
      List<Long> roleIdList = List.of(1L, 2L);

      // When
      employeeManager.saveEmployeeTransaction(employee, roleIdList);

      // Then
      verify(employeeDao).insert(employee);
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("邊界情況：無角色時只保存員工")
    void shouldOnlySaveEmployeeWhenNoRoles() {
      // Given
      EmployeeEntity employee = createTestEmployee(null, "admin");
      List<Long> roleIdList = Collections.emptyList();

      // When
      employeeManager.saveEmployeeTransaction(employee, roleIdList);

      // Then
      verify(employeeDao).insert(employee);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("邊界情況：角色列表為null時只保存員工")
    void shouldOnlySaveEmployeeWhenRolesNull() {
      // Given
      EmployeeEntity employee = createTestEmployee(null, "admin");

      // When
      employeeManager.saveEmployeeTransaction(employee, null);

      // Then
      verify(employeeDao).insert(employee);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }
  }

  // ==================== updateEmployeeTransaction 測試 ====================

  @Nested
  @DisplayName("updateEmployeeTransaction 更新員工事務測試")
  class UpdateEmployeeTransactionTest {

    @Test
    @DisplayName("正常情況：有角色時應該更新員工和角色")
    void shouldUpdateEmployeeAndRoles() {
      // Given
      EmployeeEntity employee = createTestEmployee(1L, "admin");
      List<Long> roleIdList = List.of(1L, 2L);

      // When
      employeeManager.updateEmployeeTransaction(employee, roleIdList);

      // Then
      verify(employeeDao).updateById(employee);
      verify(roleEmployeeDao).deleteByEmployeeId(1L);
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("邊界情況：無角色時應刪除所有角色")
    void shouldDeleteAllRolesWhenEmpty() {
      // Given
      EmployeeEntity employee = createTestEmployee(1L, "admin");
      List<Long> roleIdList = Collections.emptyList();

      // When
      employeeManager.updateEmployeeTransaction(employee, roleIdList);

      // Then
      verify(employeeDao).updateById(employee);
      verify(roleEmployeeDao).deleteByEmployeeId(1L);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }
  }

  // ==================== updateEmployeeRoleTransaction 測試 ====================

  @Nested
  @DisplayName("updateEmployeeRoleTransaction 更新員工角色事務測試")
  class UpdateEmployeeRoleTransactionTest {

    @Test
    @DisplayName("正常情況：應該先刪除舊角色再插入新角色")
    void shouldDeleteAndInsertRoles() {
      // Given
      Long employeeId = 1L;
      List<RoleEmployeeEntity> roleEmployeeList =
          List.of(new RoleEmployeeEntity(1L, employeeId), new RoleEmployeeEntity(2L, employeeId));

      // When
      employeeManager.updateEmployeeRoleTransaction(employeeId, roleEmployeeList);

      // Then
      verify(roleEmployeeDao).deleteByEmployeeId(employeeId);
      verify(roleEmployeeDao, times(2)).insert(any(RoleEmployeeEntity.class));
    }

    @Test
    @DisplayName("邊界情況：空角色列表應只刪除不插入")
    void shouldOnlyDeleteWhenEmptyRoleList() {
      // Given
      Long employeeId = 1L;
      List<RoleEmployeeEntity> roleEmployeeList = Collections.emptyList();

      // When
      employeeManager.updateEmployeeRoleTransaction(employeeId, roleEmployeeList);

      // Then
      verify(roleEmployeeDao).deleteByEmployeeId(employeeId);
      verify(roleEmployeeDao, never()).insert(any(RoleEmployeeEntity.class));
    }
  }

  // ==================== updatePasswordTransaction 測試 ====================

  @Nested
  @DisplayName("updatePasswordTransaction 更新密碼事務測試")
  class UpdatePasswordTransactionTest {

    @Test
    @DisplayName("正常情況：應該更新員工密碼")
    void shouldUpdatePassword() {
      // Given
      Long employeeId = 1L;
      String newEncryptPassword = "newEncryptedPwd";

      // When
      employeeManager.updatePasswordTransaction(employeeId, newEncryptPassword);

      // Then
      verify(employeeDao).updateById(any(EmployeeEntity.class));
    }
  }

  // ==================== Helper Methods ====================

  private EmployeeEntity createTestEmployee(Long id, String loginName) {
    EmployeeEntity entity = new EmployeeEntity();
    entity.setEmployeeId(id);
    entity.setLoginName(loginName);
    entity.setActualName("員工" + (id != null ? id : "New"));
    return entity;
  }
}
