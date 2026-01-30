package net.lab1024.sa.admin.module.system.position.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.*;
import net.lab1024.sa.admin.module.system.position.PositionTestFixture;
import net.lab1024.sa.admin.module.system.position.dao.PositionDao;
import net.lab1024.sa.admin.module.system.position.domain.entity.PositionEntity;
import net.lab1024.sa.admin.module.system.position.domain.form.PositionAddForm;
import net.lab1024.sa.admin.module.system.position.domain.form.PositionQueryForm;
import net.lab1024.sa.admin.module.system.position.domain.form.PositionUpdateForm;
import net.lab1024.sa.admin.module.system.position.domain.vo.PositionVO;
import net.lab1024.sa.foundation.domain.response.PageResult;
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
 * PositionService 单元测试
 *
 * <p>测试覆盖范围：
 *
 * <ul>
 *   <li>职务查询（分页查询、列表查询）
 *   <li>职务CRUD操作（添加、更新、删除、批量删除）
 *   <li>空参数处理
 * </ul>
 *
 * @author Claude Code (Service/Manager Test Coverage Plan)
 * @since 2026-01-30
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PositionService 单元测试")
class PositionServiceTest {

  @Mock private PositionDao positionDao;

  @InjectMocks private PositionService positionService;

  @BeforeEach
  void setUp() {
    PositionTestFixture.resetCounter();
  }

  @Nested
  @DisplayName("queryPage() - 分页查询职务")
  class QueryPageTests {

    @Test
    @DisplayName("正常分页查询 - 应返回分页结果")
    void queryPage_ValidForm_ShouldReturnPageResult() {
      // Arrange
      PositionQueryForm form = PositionTestFixture.createQueryForm();
      List<PositionVO> voList = PositionTestFixture.createVOList(5);
      Page<PositionVO> page = new Page<>(1, 10);
      page.setRecords(voList);
      page.setTotal(5);

      when(positionDao.queryPage(any(Page.class), eq(form))).thenReturn(voList);

      // Act
      PageResult<PositionVO> result = positionService.queryPage(form);

      // Assert
      assertNotNull(result);
      assertEquals(5, result.getList().size());
      assertEquals(Boolean.FALSE, form.getDeletedFlag());
      verify(positionDao, times(1)).queryPage(any(Page.class), eq(form));
    }

    @Test
    @DisplayName("空结果查询 - 应返回空列表")
    void queryPage_NoResults_ShouldReturnEmptyList() {
      // Arrange
      PositionQueryForm form = PositionTestFixture.createQueryForm();
      List<PositionVO> emptyList = Collections.emptyList();

      when(positionDao.queryPage(any(Page.class), eq(form))).thenReturn(emptyList);

      // Act
      PageResult<PositionVO> result = positionService.queryPage(form);

      // Assert
      assertNotNull(result);
      assertTrue(result.getList().isEmpty());
    }
  }

  @Nested
  @DisplayName("add() - 添加职务")
  class AddTests {

    @Test
    @DisplayName("正常添加职务 - 应返回成功")
    void add_ValidForm_ShouldReturnSuccess() {
      // Arrange
      PositionAddForm form = PositionTestFixture.createAddForm();

      when(positionDao.insert(any(PositionEntity.class))).thenReturn(1);

      // Act
      ResponseDTO<String> response = positionService.add(form);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, times(1)).insert(any(PositionEntity.class));
    }
  }

  @Nested
  @DisplayName("update() - 更新职务")
  class UpdateTests {

    @Test
    @DisplayName("正常更新职务 - 应返回成功")
    void update_ValidForm_ShouldReturnSuccess() {
      // Arrange
      Long positionId = 100L;
      PositionUpdateForm form = PositionTestFixture.createUpdateForm(positionId);

      when(positionDao.updateById(any(PositionEntity.class))).thenReturn(1);

      // Act
      ResponseDTO<String> response = positionService.update(form);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, times(1)).updateById(any(PositionEntity.class));
    }
  }

  @Nested
  @DisplayName("batchDelete() - 批量删除职务")
  class BatchDeleteTests {

    @Test
    @DisplayName("正常批量删除 - 应返回成功")
    void batchDelete_ValidIdList_ShouldReturnSuccess() {
      // Arrange
      List<Long> idList = Arrays.asList(1L, 2L, 3L);

      when(positionDao.deleteBatchIds(idList)).thenReturn(3);

      // Act
      ResponseDTO<String> response = positionService.batchDelete(idList);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, times(1)).deleteBatchIds(idList);
    }

    @Test
    @DisplayName("空ID列表 - 应返回成功不执行删除")
    void batchDelete_EmptyIdList_ShouldReturnSuccessWithoutDelete() {
      // Arrange
      List<Long> emptyList = Collections.emptyList();

      // Act
      ResponseDTO<String> response = positionService.batchDelete(emptyList);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, never()).deleteBatchIds(anyList());
    }

    @Test
    @DisplayName("null ID列表 - 应返回成功不执行删除")
    void batchDelete_NullIdList_ShouldReturnSuccessWithoutDelete() {
      // Arrange & Act
      ResponseDTO<String> response = positionService.batchDelete(null);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, never()).deleteBatchIds(anyList());
    }
  }

  @Nested
  @DisplayName("delete() - 删除职务")
  class DeleteTests {

    @Test
    @DisplayName("正常删除职务 - 应返回成功")
    void delete_ValidId_ShouldReturnSuccess() {
      // Arrange
      Long positionId = 100L;

      when(positionDao.deleteById(positionId)).thenReturn(1);

      // Act
      ResponseDTO<String> response = positionService.delete(positionId);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, times(1)).deleteById(positionId);
    }

    @Test
    @DisplayName("null ID - 应返回成功不执行删除")
    void delete_NullId_ShouldReturnSuccessWithoutDelete() {
      // Arrange & Act
      ResponseDTO<String> response = positionService.delete(null);

      // Assert
      assertTrue(response.getOk());
      verify(positionDao, never()).deleteById(anyLong());
    }
  }

  @Nested
  @DisplayName("queryList() - 查询职务列表")
  class QueryListTests {

    @Test
    @DisplayName("正常查询列表 - 应返回职务列表")
    void queryList_ShouldReturnList() {
      // Arrange
      List<PositionVO> voList = PositionTestFixture.createVOList(10);

      when(positionDao.queryList(Boolean.FALSE)).thenReturn(voList);

      // Act
      List<PositionVO> result = positionService.queryList();

      // Assert
      assertNotNull(result);
      assertEquals(10, result.size());
      verify(positionDao, times(1)).queryList(Boolean.FALSE);
    }

    @Test
    @DisplayName("空结果查询 - 应返回空列表")
    void queryList_NoResults_ShouldReturnEmptyList() {
      // Arrange
      List<PositionVO> emptyList = Collections.emptyList();

      when(positionDao.queryList(Boolean.FALSE)).thenReturn(emptyList);

      // Act
      List<PositionVO> result = positionService.queryList();

      // Assert
      assertNotNull(result);
      assertTrue(result.isEmpty());
    }
  }
}
