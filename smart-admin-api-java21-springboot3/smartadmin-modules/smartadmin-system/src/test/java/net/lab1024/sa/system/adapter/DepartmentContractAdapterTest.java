package net.lab1024.sa.system.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.system.dto.DepartmentDTO;
import net.lab1024.sa.api.system.dto.DepartmentTreeDTO;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.system.department.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DepartmentContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到）
 *   <li>Vavr Option 正確使用
 *   <li>部門樹結構處理
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class DepartmentContractAdapterTest {

  @Mock private DepartmentService departmentService;

  @Mock private DepartmentCacheManager departmentCacheManager;

  @Mock private DepartmentDao departmentDao;

  @InjectMocks private DepartmentContractAdapter adapter;

  // ==================== getSelfAndChildrenIds 測試 ====================

  @Test
  void testGetSelfAndChildrenIds_Success() {
    // Given
    Long departmentId = 10L;
    List<Long> expectedIds = Arrays.asList(10L, 11L, 12L, 13L);
    when(departmentService.selfAndChildrenIdList(departmentId)).thenReturn(expectedIds);

    // When
    List<Long> result = adapter.getSelfAndChildrenIds(departmentId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(4);
    assertThat(result).containsExactlyElementsOf(expectedIds);
    assertThat(result.get(0)).isEqualTo(10L);
    verify(departmentService).selfAndChildrenIdList(departmentId);
  }

  @Test
  void testGetSelfAndChildrenIds_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getSelfAndChildrenIds(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("departmentId cannot be null");
  }

  @Test
  void testGetSelfAndChildrenIds_SingleDepartment() {
    // Given - 部門沒有子部門
    Long departmentId = 100L;
    List<Long> expectedIds = Collections.singletonList(100L);
    when(departmentService.selfAndChildrenIdList(departmentId)).thenReturn(expectedIds);

    // When
    List<Long> result = adapter.getSelfAndChildrenIds(departmentId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(100L);
  }

  // ==================== getDepartmentTree 測試 ====================

  @Test
  void testGetDepartmentTree_Success() {
    // Given
    DepartmentTreeDTO tree1 = new DepartmentTreeDTO();
    tree1.setDepartmentId(1L);
    tree1.setDepartmentName("總公司");

    DepartmentTreeDTO tree2 = new DepartmentTreeDTO();
    tree2.setDepartmentId(2L);
    tree2.setDepartmentName("研發部");

    List<DepartmentTreeDTO> treeList = Arrays.asList(tree1, tree2);
    when(departmentCacheManager.getDepartmentTree()).thenReturn(treeList);

    // When
    List<DepartmentTreeDTO> result = adapter.getDepartmentTree();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getDepartmentName()).isEqualTo("總公司");
    assertThat(result.get(1).getDepartmentName()).isEqualTo("研發部");
    verify(departmentCacheManager).getDepartmentTree();
  }

  @Test
  void testGetDepartmentTree_EmptyResult() {
    // Given
    when(departmentCacheManager.getDepartmentTree()).thenReturn(Collections.emptyList());

    // When
    List<DepartmentTreeDTO> result = adapter.getDepartmentTree();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testGetDepartmentTree_WithHierarchy() {
    // Given - 測試層級結構
    DepartmentTreeDTO parent = new DepartmentTreeDTO();
    parent.setDepartmentId(1L);
    parent.setDepartmentName("總公司");

    DepartmentTreeDTO child = new DepartmentTreeDTO();
    child.setDepartmentId(2L);
    child.setDepartmentName("研發部");
    child.setParentId(1L);

    parent.setChildren(Collections.singletonList(child));

    List<DepartmentTreeDTO> treeList = Collections.singletonList(parent);
    when(departmentCacheManager.getDepartmentTree()).thenReturn(treeList);

    // When
    List<DepartmentTreeDTO> result = adapter.getDepartmentTree();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getChildren()).hasSize(1);
    assertThat(result.get(0).getChildren().get(0).getDepartmentName()).isEqualTo("研發部");
  }

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    DepartmentEntity entity1 = new DepartmentEntity();
    entity1.setDepartmentId(1L);
    entity1.setDepartmentName("總公司");

    DepartmentEntity entity2 = new DepartmentEntity();
    entity2.setDepartmentId(2L);
    entity2.setDepartmentName("研發部");

    List<DepartmentEntity> entities = Arrays.asList(entity1, entity2);
    when(departmentDao.selectList(null)).thenReturn(entities);

    // When
    List<DepartmentDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getDepartmentId()).isEqualTo(1L);
    assertThat(result.get(0).getDepartmentName()).isEqualTo("總公司");
    assertThat(result.get(1).getDepartmentId()).isEqualTo(2L);
    verify(departmentDao).selectList(null);
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    when(departmentDao.selectList(null)).thenReturn(Collections.emptyList());

    // When
    List<DepartmentDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long departmentId = 1L;
    DepartmentEntity entity = new DepartmentEntity();
    entity.setDepartmentId(departmentId);
    entity.setDepartmentName("總公司");
    entity.setManagerId(100L);

    when(departmentDao.selectById(departmentId)).thenReturn(entity);

    // When
    Option<DepartmentDTO> result = adapter.getById(departmentId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getDepartmentId()).isEqualTo(departmentId);
    assertThat(result.get().getDepartmentName()).isEqualTo("總公司");
    assertThat(result.get().getManagerId()).isEqualTo(100L);
    verify(departmentDao).selectById(departmentId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long departmentId = 999L;
    when(departmentDao.selectById(departmentId)).thenReturn(null);

    // When
    Option<DepartmentDTO> result = adapter.getById(departmentId);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("departmentId cannot be null");
  }

  // ==================== getDepartmentPath 測試 ====================

  @Test
  void testGetDepartmentPath_Found() {
    // Given
    Long departmentId = 3L;
    String expectedPath = "總公司/研發部/後端組";
    when(departmentCacheManager.getDepartmentPath(departmentId)).thenReturn(expectedPath);

    // When
    Option<String> result = adapter.getDepartmentPath(departmentId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get()).isEqualTo(expectedPath);
    verify(departmentCacheManager).getDepartmentPath(departmentId);
  }

  @Test
  void testGetDepartmentPath_NotFound() {
    // Given
    Long departmentId = 999L;
    when(departmentCacheManager.getDepartmentPath(departmentId)).thenReturn(null);

    // When
    Option<String> result = adapter.getDepartmentPath(departmentId);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetDepartmentPath_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getDepartmentPath(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("departmentId cannot be null");
  }

  @Test
  void testGetDepartmentPath_RootDepartment() {
    // Given - 根部門沒有父部門
    Long departmentId = 1L;
    String expectedPath = "總公司";
    when(departmentCacheManager.getDepartmentPath(departmentId)).thenReturn(expectedPath);

    // When
    Option<String> result = adapter.getDepartmentPath(departmentId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get()).isEqualTo("總公司");
  }
}
