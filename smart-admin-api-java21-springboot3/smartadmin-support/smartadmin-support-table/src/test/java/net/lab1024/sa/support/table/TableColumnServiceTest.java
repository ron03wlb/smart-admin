package net.lab1024.sa.support.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.support.table.domain.TableColumnEntity;
import net.lab1024.sa.support.table.domain.TableColumnItemForm;
import net.lab1024.sa.support.table.domain.TableColumnUpdateForm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * TableColumnService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getTableColumns - 獲取表格列
 *   <li>updateTableColumns - 更新表格列
 *   <li>deleteTableColumn - 刪除表格列
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TableColumnService 單元測試")
class TableColumnServiceTest {

  @Mock private TableColumnDao tableColumnDao;

  @InjectMocks private TableColumnService tableColumnService;

  @Captor private ArgumentCaptor<TableColumnEntity> entityCaptor;

  private MockedStatic<JsonUtil> jsonUtilMock;

  @BeforeEach
  void setUp() {
    jsonUtilMock = mockStatic(JsonUtil.class);
    jsonUtilMock.when(() -> JsonUtil.toJson(any())).thenReturn("[{\"columnKey\":\"col1\"}]");
  }

  @AfterEach
  void tearDown() {
    if (jsonUtilMock != null) {
      jsonUtilMock.close();
    }
  }

  // ==================== getTableColumns 測試 ====================

  @Nested
  @DisplayName("getTableColumns 獲取表格列測試")
  class GetTableColumnsTest {

    @Test
    @DisplayName("正常情況：應該返回列配置")
    void shouldReturnColumns() {
      // Given
      RequestUser requestUser = createTestRequestUser();
      Integer tableId = 1;
      TableColumnEntity entity = createTestTableColumnEntity();

      when(tableColumnDao.selectByUserIdAndTableId(
              requestUser.getUserId(), requestUser.getUserType().getValue(), tableId))
          .thenReturn(entity);

      // When
      String result = tableColumnService.getTableColumns(requestUser, tableId);

      // Then
      assertThat(result).isEqualTo("[\"col1\",\"col2\"]");
    }

    @Test
    @DisplayName("數據不存在：應該返回 null")
    void shouldReturnNullWhenNotExists() {
      // Given
      RequestUser requestUser = createTestRequestUser();
      Integer tableId = 999;

      when(tableColumnDao.selectByUserIdAndTableId(
              requestUser.getUserId(), requestUser.getUserType().getValue(), tableId))
          .thenReturn(null);

      // When
      String result = tableColumnService.getTableColumns(requestUser, tableId);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== updateTableColumns 測試 ====================

  @Nested
  @DisplayName("updateTableColumns 更新表格列測試")
  class UpdateTableColumnsTest {

    @Test
    @DisplayName("正常情況 - 新增：應該插入新記錄")
    void shouldInsertWhenNotExists() {
      // Given
      RequestUser requestUser = createTestRequestUser();
      TableColumnUpdateForm form = createTestTableColumnUpdateForm();

      when(tableColumnDao.selectByUserIdAndTableId(
              requestUser.getUserId(), requestUser.getUserType().getValue(), form.getTableId()))
          .thenReturn(null);

      // When
      ResponseDTO<String> result = tableColumnService.updateTableColumns(requestUser, form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(tableColumnDao).insert(entityCaptor.capture());
      TableColumnEntity captured = entityCaptor.getValue();
      assertThat(captured.getTableId()).isEqualTo(form.getTableId());
    }

    @Test
    @DisplayName("正常情況 - 更新：應該更新現有記錄")
    void shouldUpdateWhenExists() {
      // Given
      RequestUser requestUser = createTestRequestUser();
      TableColumnUpdateForm form = createTestTableColumnUpdateForm();
      TableColumnEntity existingEntity = createTestTableColumnEntity();

      when(tableColumnDao.selectByUserIdAndTableId(
              requestUser.getUserId(), requestUser.getUserType().getValue(), form.getTableId()))
          .thenReturn(existingEntity);

      // When
      ResponseDTO<String> result = tableColumnService.updateTableColumns(requestUser, form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(tableColumnDao).updateById(any(TableColumnEntity.class));
    }

    @Test
    @DisplayName("列表為空：應該直接返回成功")
    void shouldReturnOkWhenColumnListEmpty() {
      // Given
      RequestUser requestUser = mock(RequestUser.class);
      TableColumnUpdateForm form = new TableColumnUpdateForm();
      form.setTableId(1);
      form.setColumnList(List.of());

      // When
      ResponseDTO<String> result = tableColumnService.updateTableColumns(requestUser, form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(tableColumnDao, never()).insert(any(TableColumnEntity.class));
      verify(tableColumnDao, never()).updateById(any(TableColumnEntity.class));
    }
  }

  // ==================== deleteTableColumn 測試 ====================

  @Nested
  @DisplayName("deleteTableColumn 刪除表格列測試")
  class DeleteTableColumnTest {

    @Test
    @DisplayName("正常情況：應該成功刪除")
    void shouldDeleteSuccessfully() {
      // Given
      RequestUser requestUser = createTestRequestUser();
      Integer tableId = 1;

      // When
      ResponseDTO<String> result = tableColumnService.deleteTableColumn(requestUser, tableId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(tableColumnDao)
          .deleteTableColumn(
              requestUser.getUserId(), requestUser.getUserType().getValue(), tableId);
    }
  }

  // ==================== Helper Methods ====================

  private RequestUser createTestRequestUser() {
    RequestUser user = mock(RequestUser.class);
    lenient().when(user.getUserType()).thenReturn(UserTypeEnum.ADMIN_EMPLOYEE);
    lenient().when(user.getUserId()).thenReturn(1L);
    return user;
  }

  private TableColumnEntity createTestTableColumnEntity() {
    TableColumnEntity entity = new TableColumnEntity();
    entity.setTableColumnId(1L);
    entity.setTableId(1);
    entity.setUserId(1L);
    entity.setUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
    entity.setColumns("[\"col1\",\"col2\"]");
    return entity;
  }

  private TableColumnUpdateForm createTestTableColumnUpdateForm() {
    TableColumnUpdateForm form = new TableColumnUpdateForm();
    form.setTableId(1);
    TableColumnItemForm item1 = new TableColumnItemForm();
    item1.setColumnKey("col1");
    item1.setWidth(100);
    form.setColumnList(List.of(item1));
    return form;
  }
}
