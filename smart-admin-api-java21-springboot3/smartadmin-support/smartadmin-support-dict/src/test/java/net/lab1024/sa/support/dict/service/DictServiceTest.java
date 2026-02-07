package net.lab1024.sa.support.dict.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.common.cache.CacheService;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.dict.dao.DictDao;
import net.lab1024.sa.support.dict.dao.DictDataDao;
import net.lab1024.sa.support.dict.domain.entity.DictDataEntity;
import net.lab1024.sa.support.dict.domain.entity.DictEntity;
import net.lab1024.sa.support.dict.domain.form.DictAddForm;
import net.lab1024.sa.support.dict.domain.form.DictDataAddForm;
import net.lab1024.sa.support.dict.domain.form.DictDataUpdateForm;
import net.lab1024.sa.support.dict.domain.form.DictQueryForm;
import net.lab1024.sa.support.dict.domain.form.DictUpdateForm;
import net.lab1024.sa.support.dict.domain.vo.DictDataVO;
import net.lab1024.sa.support.dict.domain.vo.DictVO;
import net.lab1024.sa.support.dict.manager.DictManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DictService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>add/update/delete - 字典 CRUD
 *   <li>queryPage - 分頁查詢
 *   <li>addDictData/updateDictData/deleteDictData - 字典數據 CRUD
 *   <li>getDictData/getDictDataLabel - 獲取字典數據
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DictService 單元測試")
class DictServiceTest {

  @Mock private DictDao dictDao;

  @Mock private DictDataDao dictDataDao;

  @Mock private CacheService cacheService;

  @Mock private DictManager dictManager;

  @InjectMocks private DictService dictService;

  @Captor private ArgumentCaptor<DictEntity> dictEntityCaptor;

  @Captor private ArgumentCaptor<DictDataEntity> dictDataEntityCaptor;

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增字典測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增")
    void shouldAddSuccess() {
      // Given
      DictAddForm form = createTestAddForm();
      when(dictDao.selectByCode(form.getDictCode())).thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.add(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDao).insert(dictEntityCaptor.capture());
      DictEntity captured = dictEntityCaptor.getValue();
      assertThat(captured.getDictCode()).isEqualTo(form.getDictCode());
    }

    @Test
    @DisplayName("編碼重複時：應該返回錯誤")
    void shouldReturnErrorWhenCodeExists() {
      // Given
      DictAddForm form = createTestAddForm();
      when(dictDao.selectByCode(form.getDictCode())).thenReturn(createTestDictEntity());

      // When
      ResponseDTO<String> result = dictService.add(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(dictDao, never()).insert(dictEntityCaptor.capture());
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新字典測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：應該成功更新")
    void shouldUpdateSuccess() {
      // Given
      DictUpdateForm form = createTestUpdateForm();
      when(dictDao.selectByCode(form.getDictCode())).thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.update(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDao).updateById(dictEntityCaptor.capture());
    }

    @Test
    @DisplayName("編碼被其他字典佔用時：應該返回錯誤")
    void shouldReturnErrorWhenCodeUsedByOther() {
      // Given
      DictUpdateForm form = createTestUpdateForm();
      DictEntity existingDict = createTestDictEntity();
      existingDict.setDictId(999L); // 不同的 ID
      when(dictDao.selectByCode(form.getDictCode())).thenReturn(existingDict);

      // When
      ResponseDTO<String> result = dictService.update(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(dictDao, never()).updateById(dictEntityCaptor.capture());
    }

    @Test
    @DisplayName("更新自己的編碼：應該成功")
    void shouldSucceedWhenUpdatingSameCode() {
      // Given
      DictUpdateForm form = createTestUpdateForm();
      DictEntity existingDict = createTestDictEntity();
      existingDict.setDictId(form.getDictId()); // 同一個 ID
      when(dictDao.selectByCode(form.getDictCode())).thenReturn(existingDict);

      // When
      ResponseDTO<String> result = dictService.update(form);

      // Then
      assertThat(result.getOk()).isTrue();
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除字典測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除")
    void shouldDeleteSuccess() {
      // Given
      Long dictId = 1L;

      // When
      ResponseDTO<String> result = dictService.delete(dictId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDao).deleteById(dictId);
    }

    @Test
    @DisplayName("ID 為空時：應該返回成功（無操作）")
    void shouldReturnOkWhenIdIsNull() {
      // When
      ResponseDTO<String> result = dictService.delete(null);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDao, never()).deleteById(anyLong());
    }
  }

  // ==================== batchDelete 測試 ====================

  @Nested
  @DisplayName("batchDelete 批量刪除測試")
  class BatchDeleteTest {

    @Test
    @DisplayName("正常情況：應該成功批量刪除")
    @SuppressWarnings("deprecation")
    void shouldBatchDeleteSuccess() {
      // Given
      List<Long> idList = List.of(1L, 2L, 3L);

      // When
      ResponseDTO<String> result = dictService.batchDelete(idList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDao).deleteBatchIds(idList);
    }

    @Test
    @DisplayName("列表為空時：應該返回成功（無操作）")
    @SuppressWarnings("deprecation")
    void shouldReturnOkWhenListEmpty() {
      // When
      ResponseDTO<String> result = dictService.batchDelete(List.of());

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDao, never()).deleteBatchIds(any());
    }
  }

  // ==================== updateDisabled 測試 ====================

  @Nested
  @DisplayName("updateDisabled 禁用/啟用測試")
  class UpdateDisabledTest {

    @Test
    @DisplayName("正常情況：應該切換禁用狀態")
    void shouldToggleDisabledStatus() {
      // Given
      Long dictId = 1L;
      DictEntity entity = createTestDictEntity();
      entity.setDisabledFlag(false);
      when(dictDao.selectById(dictId)).thenReturn(entity);

      // When
      ResponseDTO<String> result = dictService.updateDisabled(dictId);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(entity.getDisabledFlag()).isTrue();
    }

    @Test
    @DisplayName("字典不存在時：應該返回錯誤")
    void shouldReturnErrorWhenDictNotExists() {
      // Given
      Long dictId = 999L;
      when(dictDao.selectById(dictId)).thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.updateDisabled(dictId);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== queryPage 測試 ====================

  @Nested
  @DisplayName("queryPage 分頁查詢測試")
  class QueryPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      DictQueryForm form = new DictQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      DictVO vo = new DictVO();
      vo.setDictId(1L);
      vo.setDictName("Test Dict");
      when(dictDao.queryPage(any(Page.class), any(DictQueryForm.class))).thenReturn(List.of(vo));

      // When
      PageResult<DictVO> result = dictService.queryPage(form);

      // Then
      assertThat(result).isNotNull();
      verify(dictDao).queryPage(any(Page.class), any(DictQueryForm.class));
    }
  }

  // ==================== addDictData 測試 ====================

  @Nested
  @DisplayName("addDictData 新增字典數據測試")
  class AddDictDataTest {

    @Test
    @DisplayName("正常情況：應該成功新增")
    void shouldAddDictDataSuccess() {
      // Given
      DictDataAddForm form = createTestDictDataAddForm();
      when(dictDao.selectById(form.getDictId())).thenReturn(createTestDictEntity());
      when(dictDataDao.selectByDictIdAndValue(form.getDictId(), form.getDataValue()))
          .thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.addDictData(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(dictDataDao).insert(dictDataEntityCaptor.capture());
    }

    @Test
    @DisplayName("字典不存在時：應該返回錯誤")
    void shouldReturnErrorWhenDictNotExists() {
      // Given
      DictDataAddForm form = createTestDictDataAddForm();
      when(dictDao.selectById(form.getDictId())).thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.addDictData(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(dictDataDao, never()).insert(dictDataEntityCaptor.capture());
    }

    @Test
    @DisplayName("Value 重複時：應該返回錯誤")
    void shouldReturnErrorWhenValueExists() {
      // Given
      DictDataAddForm form = createTestDictDataAddForm();
      when(dictDao.selectById(form.getDictId())).thenReturn(createTestDictEntity());
      when(dictDataDao.selectByDictIdAndValue(form.getDictId(), form.getDataValue()))
          .thenReturn(new DictDataEntity());

      // When
      ResponseDTO<String> result = dictService.addDictData(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(dictDataDao, never()).insert(dictDataEntityCaptor.capture());
    }
  }

  // ==================== getDictData 測試 ====================

  @Nested
  @DisplayName("getDictData 獲取字典數據測試")
  class GetDictDataTest {

    @Test
    @DisplayName("正常情況：應該委托給 Manager")
    void shouldDelegateToManager() {
      // Given
      String dictCode = "gender";
      String dataValue = "1";
      DictDataVO expected = new DictDataVO();
      expected.setDataLabel("Male");
      when(dictManager.getDictData(dictCode, dataValue)).thenReturn(expected);

      // When
      DictDataVO result = dictService.getDictData(dictCode, dataValue);

      // Then
      assertThat(result).isEqualTo(expected);
      verify(dictManager).getDictData(dictCode, dataValue);
    }
  }

  // ==================== getDictDataLabel 測試 ====================

  @Nested
  @DisplayName("getDictDataLabel 獲取字典標籤測試")
  class GetDictDataLabelTest {

    @Test
    @DisplayName("正常情況：應該返回 Label")
    void shouldReturnLabel() {
      // Given
      String dictCode = "gender";
      String dataValue = "1";
      DictDataVO dictData = new DictDataVO();
      dictData.setDataLabel("Male");
      when(dictManager.getDictData(dictCode, dataValue)).thenReturn(dictData);

      // When
      String result = dictService.getDictDataLabel(dictCode, dataValue);

      // Then
      assertThat(result).isEqualTo("Male");
    }

    @Test
    @DisplayName("數據不存在時：應該返回空字符串")
    void shouldReturnEmptyWhenNotExists() {
      // Given
      String dictCode = "gender";
      String dataValue = "999";
      when(dictManager.getDictData(dictCode, dataValue)).thenReturn(null);

      // When
      String result = dictService.getDictDataLabel(dictCode, dataValue);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== updateDictData 測試 ====================

  @Nested
  @DisplayName("updateDictData 更新字典數據測試")
  class UpdateDictDataTest {

    @Test
    @DisplayName("正常情況：應該成功更新")
    void shouldUpdateDictDataSuccess() {
      // Given
      DictDataUpdateForm form = createTestDictDataUpdateForm();
      when(dictDao.selectById(form.getDictId())).thenReturn(createTestDictEntity());
      when(dictDataDao.selectByDictIdAndValue(form.getDictId(), form.getDataValue()))
          .thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.updateDictData(form);

      // Then
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("字典不存在時：應該返回錯誤")
    void shouldReturnErrorWhenDictNotExists() {
      // Given
      DictDataUpdateForm form = createTestDictDataUpdateForm();
      when(dictDao.selectById(form.getDictId())).thenReturn(null);

      // When
      ResponseDTO<String> result = dictService.updateDictData(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== Helper Methods ====================

  private DictAddForm createTestAddForm() {
    DictAddForm form = new DictAddForm();
    form.setDictName("Test Dict");
    form.setDictCode("test_dict");
    form.setRemark("Test remark");
    return form;
  }

  private DictUpdateForm createTestUpdateForm() {
    DictUpdateForm form = new DictUpdateForm();
    form.setDictId(1L);
    form.setDictName("Updated Dict");
    form.setDictCode("test_dict");
    form.setRemark("Updated remark");
    return form;
  }

  private DictEntity createTestDictEntity() {
    DictEntity entity = new DictEntity();
    entity.setDictId(1L);
    entity.setDictName("Test Dict");
    entity.setDictCode("test_dict");
    entity.setDisabledFlag(false);
    return entity;
  }

  private DictDataAddForm createTestDictDataAddForm() {
    DictDataAddForm form = new DictDataAddForm();
    form.setDictId(1L);
    form.setDataLabel("Male");
    form.setDataValue("1");
    return form;
  }

  private DictDataUpdateForm createTestDictDataUpdateForm() {
    DictDataUpdateForm form = new DictDataUpdateForm();
    form.setDictDataId(1L);
    form.setDictId(1L);
    form.setDictCode("gender");
    form.setDataLabel("Male");
    form.setDataValue("1");
    return form;
  }
}
