package net.lab1024.sa.support.dict.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import net.lab1024.sa.support.dict.dao.DictDao;
import net.lab1024.sa.support.dict.dao.DictDataDao;
import net.lab1024.sa.support.dict.domain.entity.DictDataEntity;
import net.lab1024.sa.support.dict.domain.entity.DictEntity;
import net.lab1024.sa.support.dict.domain.vo.DictDataVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DictManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getDictData - 獲取字典數據（帶 @Cached 快取）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DictManager 單元測試")
class DictManagerTest {

  @Mock private DictDao dictDao;

  @Mock private DictDataDao dictDataDao;

  @InjectMocks private DictManager dictManager;

  // ==================== getDictData 測試 ====================

  @Nested
  @DisplayName("getDictData 獲取字典數據測試")
  class GetDictDataTest {

    @Test
    @DisplayName("正常情況：應該返回字典數據")
    void shouldReturnDictData() {
      // Given
      String dictCode = "gender";
      String dataValue = "1";
      DictEntity dictEntity = createTestDictEntity();
      DictDataEntity dataEntity = createTestDictDataEntity();

      when(dictDao.selectByCode(dictCode)).thenReturn(dictEntity);
      when(dictDataDao.selectByDictIdAndValue(dictEntity.getDictId(), dataValue))
          .thenReturn(dataEntity);

      // When
      DictDataVO result = dictManager.getDictData(dictCode, dataValue);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getDataLabel()).isEqualTo("Male");
      assertThat(result.getDataValue()).isEqualTo("1");
    }

    @Test
    @DisplayName("字典編碼不存在時：應該返回 null")
    void shouldReturnNullWhenDictCodeNotExists() {
      // Given
      String dictCode = "non_existent";
      String dataValue = "1";

      when(dictDao.selectByCode(dictCode)).thenReturn(null);

      // When
      DictDataVO result = dictManager.getDictData(dictCode, dataValue);

      // Then
      assertThat(result).isNull();
      verify(dictDataDao, never()).selectByDictIdAndValue(any(), any());
    }

    @Test
    @DisplayName("字典數據不存在時：應該返回 null")
    void shouldReturnNullWhenDataValueNotExists() {
      // Given
      String dictCode = "gender";
      String dataValue = "999";
      DictEntity dictEntity = createTestDictEntity();

      when(dictDao.selectByCode(dictCode)).thenReturn(dictEntity);
      when(dictDataDao.selectByDictIdAndValue(dictEntity.getDictId(), dataValue)).thenReturn(null);

      // When
      DictDataVO result = dictManager.getDictData(dictCode, dataValue);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("多次調用：應該正確查詢（快取由 JetCache 處理）")
    void shouldQueryCorrectlyOnMultipleCalls() {
      // Given
      String dictCode = "gender";
      String dataValue = "1";
      DictEntity dictEntity = createTestDictEntity();
      DictDataEntity dataEntity = createTestDictDataEntity();

      when(dictDao.selectByCode(dictCode)).thenReturn(dictEntity);
      when(dictDataDao.selectByDictIdAndValue(dictEntity.getDictId(), dataValue))
          .thenReturn(dataEntity);

      // When - 多次調用（實際快取由 Spring AOP 處理，單元測試中每次都會調用）
      DictDataVO result1 = dictManager.getDictData(dictCode, dataValue);
      DictDataVO result2 = dictManager.getDictData(dictCode, dataValue);

      // Then
      assertThat(result1).isNotNull();
      assertThat(result2).isNotNull();
      // 在單元測試中沒有 AOP，所以會調用兩次
      verify(dictDao, times(2)).selectByCode(dictCode);
    }
  }

  // ==================== Helper Methods ====================

  private DictEntity createTestDictEntity() {
    DictEntity entity = new DictEntity();
    entity.setDictId(1L);
    entity.setDictName("Gender");
    entity.setDictCode("gender");
    entity.setDisabledFlag(false);
    return entity;
  }

  private DictDataEntity createTestDictDataEntity() {
    DictDataEntity entity = new DictDataEntity();
    entity.setDictDataId(1L);
    entity.setDictId(1L);
    entity.setDataLabel("Male");
    entity.setDataValue("1");
    entity.setDisabledFlag(false);
    return entity;
  }
}
