package net.lab1024.sa.system.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.system.dto.RoleDTO;
import net.lab1024.sa.system.role.dao.RoleDao;
import net.lab1024.sa.system.role.domain.entity.RoleEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * RoleContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到）
 *   <li>Vavr Option 正確使用
 *   <li>批量查詢處理
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class RoleContractAdapterTest {

  @Mock private RoleDao roleDao;

  @InjectMocks private RoleContractAdapter adapter;

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long roleId = 1L;
    RoleEntity entity = new RoleEntity();
    entity.setRoleId(roleId);
    entity.setRoleName("管理員");
    entity.setRoleCode("ADMIN");

    when(roleDao.selectById(roleId)).thenReturn(entity);

    // When
    Option<RoleDTO> result = adapter.getById(roleId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getRoleId()).isEqualTo(roleId);
    assertThat(result.get().getRoleName()).isEqualTo("管理員");
    assertThat(result.get().getRoleCode()).isEqualTo("ADMIN");
    verify(roleDao).selectById(roleId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long roleId = 999L;
    when(roleDao.selectById(roleId)).thenReturn(null);

    // When
    Option<RoleDTO> result = adapter.getById(roleId);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roleId cannot be null");
  }

  // ==================== queryByIds 測試 ====================

  @Test
  void testQueryByIds_Success() {
    // Given
    List<Long> roleIds = Arrays.asList(1L, 2L, 3L);

    RoleEntity entity1 = new RoleEntity();
    entity1.setRoleId(1L);
    entity1.setRoleName("管理員");

    RoleEntity entity2 = new RoleEntity();
    entity2.setRoleId(2L);
    entity2.setRoleName("操作員");

    RoleEntity entity3 = new RoleEntity();
    entity3.setRoleId(3L);
    entity3.setRoleName("訪客");

    List<RoleEntity> entities = Arrays.asList(entity1, entity2, entity3);
    when(roleDao.selectBatchIds(roleIds)).thenReturn(entities);

    // When
    List<RoleDTO> result = adapter.queryByIds(roleIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getRoleId()).isEqualTo(1L);
    assertThat(result.get(0).getRoleName()).isEqualTo("管理員");
    assertThat(result.get(1).getRoleId()).isEqualTo(2L);
    assertThat(result.get(2).getRoleId()).isEqualTo(3L);
    verify(roleDao).selectBatchIds(roleIds);
  }

  @Test
  void testQueryByIds_EmptyList() {
    // Given
    List<Long> roleIds = Collections.emptyList();

    // When
    List<RoleDTO> result = adapter.queryByIds(roleIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testQueryByIds_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByIds(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("roleIds cannot be null");
  }

  @Test
  void testQueryByIds_PartialResults() {
    // Given - 部分 ID 不存在
    List<Long> roleIds = Arrays.asList(1L, 999L, 2L);

    RoleEntity entity1 = new RoleEntity();
    entity1.setRoleId(1L);
    entity1.setRoleName("管理員");

    RoleEntity entity2 = new RoleEntity();
    entity2.setRoleId(2L);
    entity2.setRoleName("操作員");

    List<RoleEntity> entities = Arrays.asList(entity1, entity2);
    when(roleDao.selectBatchIds(roleIds)).thenReturn(entities);

    // When
    List<RoleDTO> result = adapter.queryByIds(roleIds);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2); // 只返回存在的角色
  }

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    RoleEntity entity1 = new RoleEntity();
    entity1.setRoleId(1L);
    entity1.setRoleName("管理員");
    entity1.setRoleCode("ADMIN");

    RoleEntity entity2 = new RoleEntity();
    entity2.setRoleId(2L);
    entity2.setRoleName("操作員");
    entity2.setRoleCode("OPERATOR");

    List<RoleEntity> entities = Arrays.asList(entity1, entity2);
    when(roleDao.selectList(null)).thenReturn(entities);

    // When
    List<RoleDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getRoleId()).isEqualTo(1L);
    assertThat(result.get(0).getRoleName()).isEqualTo("管理員");
    assertThat(result.get(0).getRoleCode()).isEqualTo("ADMIN");
    assertThat(result.get(1).getRoleId()).isEqualTo(2L);
    verify(roleDao).selectList(null);
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    when(roleDao.selectList(null)).thenReturn(Collections.emptyList());

    // When
    List<RoleDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }
}
