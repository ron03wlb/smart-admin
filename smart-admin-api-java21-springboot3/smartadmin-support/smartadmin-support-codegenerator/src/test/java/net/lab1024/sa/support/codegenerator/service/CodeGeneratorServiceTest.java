package net.lab1024.sa.support.codegenerator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.support.codegenerator.dao.CodeGeneratorConfigDao;
import net.lab1024.sa.support.codegenerator.dao.CodeGeneratorDao;
import net.lab1024.sa.support.codegenerator.domain.entity.CodeGeneratorConfigEntity;
import net.lab1024.sa.support.codegenerator.domain.form.CodeGeneratorConfigForm;
import net.lab1024.sa.support.codegenerator.domain.form.CodeGeneratorPreviewForm;
import net.lab1024.sa.support.codegenerator.domain.form.TableQueryForm;
import net.lab1024.sa.support.codegenerator.domain.model.CodeBasic;
import net.lab1024.sa.support.codegenerator.domain.vo.TableColumnVO;
import net.lab1024.sa.support.codegenerator.domain.vo.TableConfigVO;
import net.lab1024.sa.support.codegenerator.domain.vo.TableVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CodeGeneratorService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getTableColumns - 獲取表列信息
 *   <li>queryTableList - 分頁查詢表列表
 *   <li>getTableConfig - 獲取配置
 *   <li>updateConfig - 更新配置
 *   <li>preview - 預覽代碼
 *   <li>download - 下載代碼
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CodeGeneratorService 單元測試")
class CodeGeneratorServiceTest {

  @Mock private CodeGeneratorDao codeGeneratorDao;

  @Mock private CodeGeneratorConfigDao codeGeneratorConfigDao;

  @Mock private CodeGeneratorTemplateService codeGeneratorTemplateService;

  @InjectMocks private CodeGeneratorService codeGeneratorService;

  private MockedStatic<JsonUtil> jsonUtilMock;

  @BeforeEach
  void setUp() {
    jsonUtilMock = mockStatic(JsonUtil.class);
    // Mock fromJson for CodeBasic
    CodeBasic mockBasic = new CodeBasic();
    mockBasic.setModuleName("employee");
    jsonUtilMock
        .when(() -> JsonUtil.fromJson(any(String.class), eq(CodeBasic.class)))
        .thenReturn(mockBasic);
    // Mock fromJsonArray
    jsonUtilMock.when(() -> JsonUtil.fromJsonArray(any(String.class), any())).thenReturn(List.of());
    // Mock toJson
    jsonUtilMock.when(() -> JsonUtil.toJson(any())).thenReturn("{}");
  }

  @AfterEach
  void tearDown() {
    if (jsonUtilMock != null) {
      jsonUtilMock.close();
    }
  }

  // ==================== getTableColumns 測試 ====================

  @Nested
  @DisplayName("getTableColumns 獲取表列信息測試")
  class GetTableColumnsTest {

    @Test
    @DisplayName("正常情況：應該返回列信息並轉換標誌")
    void shouldReturnColumnsWithFlags() {
      // Given
      String tableName = "t_employee";
      TableColumnVO column1 = createTestTableColumnVO("id", "PRI", "NO", "auto_increment");
      TableColumnVO column2 = createTestTableColumnVO("name", "", "YES", "");

      when(codeGeneratorDao.selectTableColumn(tableName)).thenReturn(List.of(column1, column2));

      // When
      List<TableColumnVO> result = codeGeneratorService.getTableColumns(tableName);

      // Then
      assertThat(result).hasSize(2);
      // 驗證主鍵標識
      assertThat(result.get(0).getPrimaryKeyFlag()).isTrue();
      assertThat(result.get(0).getNullableFlag()).isFalse();
      assertThat(result.get(0).getAutoIncreaseFlag()).isTrue();
      // 驗證普通列
      assertThat(result.get(1).getPrimaryKeyFlag()).isFalse();
      assertThat(result.get(1).getNullableFlag()).isTrue();
      assertThat(result.get(1).getAutoIncreaseFlag()).isFalse();
    }
  }

  // ==================== queryTableList 測試 ====================

  @Nested
  @DisplayName("queryTableList 分頁查詢表列表測試")
  class QueryTableListTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      TableQueryForm form = new TableQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      TableVO vo = createTestTableVO();
      when(codeGeneratorDao.queryTableList(any(Page.class), any(TableQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      PageResult<TableVO> result = codeGeneratorService.queryTableList(form);

      // Then
      assertThat(result).isNotNull();
    }
  }

  // ==================== getTableConfig 測試 ====================

  @Nested
  @DisplayName("getTableConfig 獲取配置測試")
  class GetTableConfigTest {

    @Test
    @DisplayName("正常情況：應該返回配置")
    void shouldReturnConfig() {
      // Given
      String tableName = "t_employee";
      CodeGeneratorConfigEntity entity = createTestCodeGeneratorConfigEntity();
      when(codeGeneratorConfigDao.selectById(tableName)).thenReturn(entity);

      // When
      TableConfigVO result = codeGeneratorService.getTableConfig(tableName);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getBasic()).isNotNull();
    }

    @Test
    @DisplayName("配置不存在：應該返回空配置")
    void shouldReturnEmptyConfigWhenNotExists() {
      // Given
      String tableName = "t_not_exists";
      when(codeGeneratorConfigDao.selectById(tableName)).thenReturn(null);

      // When
      TableConfigVO result = codeGeneratorService.getTableConfig(tableName);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getBasic()).isNull();
    }
  }

  // ==================== updateConfig 測試 ====================

  @Nested
  @DisplayName("updateConfig 更新配置測試")
  class UpdateConfigTest {

    @Test
    @DisplayName("表不存在：應該返回錯誤")
    void shouldReturnErrorWhenTableNotExists() {
      // Given
      CodeGeneratorConfigForm form = createTestCodeGeneratorConfigForm();
      when(codeGeneratorDao.countByTableName(form.getTableName())).thenReturn(0L);

      // When
      ResponseDTO<String> result = codeGeneratorService.updateConfig(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("表不存在");
    }

    @Test
    @DisplayName("正常情況 - 新增配置：應該成功插入")
    void shouldInsertWhenConfigNotExists() {
      // Given
      CodeGeneratorConfigForm form = createTestCodeGeneratorConfigFormWithPrimaryKey();

      when(codeGeneratorDao.countByTableName(form.getTableName())).thenReturn(1L);
      when(codeGeneratorConfigDao.selectById(form.getTableName())).thenReturn(null);

      TableColumnVO pkColumn = createTestTableColumnVO("id", "PRI", "NO", "auto_increment");
      when(codeGeneratorDao.selectTableColumn(form.getTableName())).thenReturn(List.of(pkColumn));

      // When
      ResponseDTO<String> result = codeGeneratorService.updateConfig(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(codeGeneratorConfigDao).insert(any(CodeGeneratorConfigEntity.class));
    }

    @Test
    @DisplayName("正常情況 - 更新配置：應該成功更新")
    void shouldUpdateWhenConfigExists() {
      // Given
      CodeGeneratorConfigForm form = createTestCodeGeneratorConfigFormWithPrimaryKey();
      CodeGeneratorConfigEntity existingEntity = createTestCodeGeneratorConfigEntity();

      when(codeGeneratorDao.countByTableName(form.getTableName())).thenReturn(1L);
      when(codeGeneratorConfigDao.selectById(form.getTableName())).thenReturn(existingEntity);

      TableColumnVO pkColumn = createTestTableColumnVO("id", "PRI", "NO", "auto_increment");
      when(codeGeneratorDao.selectTableColumn(form.getTableName())).thenReturn(List.of(pkColumn));

      // When
      ResponseDTO<String> result = codeGeneratorService.updateConfig(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(codeGeneratorConfigDao).updateById(any(CodeGeneratorConfigEntity.class));
    }
  }

  // ==================== preview 測試 ====================

  @Nested
  @DisplayName("preview 預覽代碼測試")
  class PreviewTest {

    @Test
    @DisplayName("表不存在：應該返回錯誤")
    void shouldReturnErrorWhenTableNotExists() {
      // Given
      CodeGeneratorPreviewForm form = createTestCodeGeneratorPreviewForm();
      when(codeGeneratorDao.countByTableName(form.getTableName())).thenReturn(0L);

      // When
      ResponseDTO<String> result = codeGeneratorService.preview(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("表不存在");
    }

    @Test
    @DisplayName("配置不存在：應該返回錯誤")
    void shouldReturnErrorWhenConfigNotExists() {
      // Given
      CodeGeneratorPreviewForm form = createTestCodeGeneratorPreviewForm();
      when(codeGeneratorDao.countByTableName(form.getTableName())).thenReturn(1L);
      when(codeGeneratorConfigDao.selectById(form.getTableName())).thenReturn(null);

      // When
      ResponseDTO<String> result = codeGeneratorService.preview(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("配置信息不存在");
    }

    @Test
    @DisplayName("表沒有列：應該返回錯誤")
    void shouldReturnErrorWhenNoColumns() {
      // Given
      CodeGeneratorPreviewForm form = createTestCodeGeneratorPreviewForm();
      CodeGeneratorConfigEntity configEntity = createTestCodeGeneratorConfigEntity();

      when(codeGeneratorDao.countByTableName(form.getTableName())).thenReturn(1L);
      when(codeGeneratorConfigDao.selectById(form.getTableName())).thenReturn(configEntity);
      when(codeGeneratorDao.selectTableColumn(form.getTableName())).thenReturn(List.of());

      // When
      ResponseDTO<String> result = codeGeneratorService.preview(form);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("没有列信息");
    }
  }

  // ==================== download 測試 ====================

  @Nested
  @DisplayName("download 下載代碼測試")
  class DownloadTest {

    @Test
    @DisplayName("表名為空：應該返回錯誤")
    void shouldReturnErrorWhenTableNameBlank() {
      // When
      ResponseDTO<byte[]> result = codeGeneratorService.download("");

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("表名不能为空");
    }

    @Test
    @DisplayName("表不存在：應該返回錯誤")
    void shouldReturnErrorWhenTableNotExists() {
      // Given
      String tableName = "t_not_exists";
      when(codeGeneratorDao.countByTableName(tableName)).thenReturn(0L);

      // When
      ResponseDTO<byte[]> result = codeGeneratorService.download(tableName);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("表不存在");
    }
  }

  // ==================== Helper Methods ====================

  private TableColumnVO createTestTableColumnVO(
      String columnName, String columnKey, String isNullable, String extra) {
    TableColumnVO vo = new TableColumnVO();
    vo.setColumnName(columnName);
    vo.setColumnKey(columnKey);
    vo.setIsNullable(isNullable);
    vo.setExtra(extra);
    vo.setDataType("varchar");
    vo.setColumnComment("comment");
    return vo;
  }

  private TableVO createTestTableVO() {
    TableVO vo = new TableVO();
    vo.setTableName("t_employee");
    vo.setTableComment("員工表");
    return vo;
  }

  private CodeGeneratorConfigEntity createTestCodeGeneratorConfigEntity() {
    CodeGeneratorConfigEntity entity = new CodeGeneratorConfigEntity();
    entity.setTableName("t_employee");
    entity.setBasic("{\"moduleName\":\"employee\",\"businessName\":\"員工\"}");
    entity.setFields("[]");
    entity.setInsertAndUpdate("{}");
    entity.setDeleteInfo("{}");
    entity.setQueryFields("[]");
    entity.setTableFields("[]");
    return entity;
  }

  private CodeGeneratorConfigForm createTestCodeGeneratorConfigForm() {
    CodeGeneratorConfigForm form = new CodeGeneratorConfigForm();
    form.setTableName("t_employee");
    return form;
  }

  private CodeGeneratorConfigForm createTestCodeGeneratorConfigFormWithPrimaryKey() {
    CodeGeneratorConfigForm form = new CodeGeneratorConfigForm();
    form.setTableName("t_employee");
    // 不設置 deleteInfo 或設置為物理刪除，避免觸發 deleted_flag 驗證
    return form;
  }

  private CodeGeneratorPreviewForm createTestCodeGeneratorPreviewForm() {
    CodeGeneratorPreviewForm form = new CodeGeneratorPreviewForm();
    form.setTableName("t_employee");
    form.setTemplateFile("Entity.java.vm");
    return form;
  }
}
