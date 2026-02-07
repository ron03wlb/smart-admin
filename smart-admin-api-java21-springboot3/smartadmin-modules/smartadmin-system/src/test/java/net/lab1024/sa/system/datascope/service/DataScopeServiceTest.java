package net.lab1024.sa.system.datascope.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.system.datascope.domain.DataScopeAndViewTypeVO;
import net.lab1024.sa.system.datascope.domain.DataScopeDTO;
import net.lab1024.sa.system.datascope.domain.DataScopeViewTypeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * DataScopeService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>枚舉類型轉換為 VO 列表
 *   <li>數據範圍配置查詢
 *   <li>可見範圍類型查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataScopeService 單元測試")
class DataScopeServiceTest {

  @InjectMocks private DataScopeService dataScopeService;

  // ==================== dataScopeList 測試 ====================

  @Nested
  @DisplayName("dataScopeList 數據範圍列表測試")
  class DataScopeListTest {

    @Test
    @DisplayName("正常情況：應該返回所有數據範圍和可見類型")
    void shouldReturnAllDataScopeAndViewTypes() {
      // When
      ResponseDTO<List<DataScopeAndViewTypeVO>> result = dataScopeService.dataScopeList();

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData()).isNotEmpty();

      // 驗證每個數據範圍都有可見類型列表
      result.getData().forEach(dto -> assertThat(dto.getViewTypeList()).isNotEmpty());
    }
  }

  // ==================== getViewType 測試 ====================

  @Nested
  @DisplayName("getViewType 可見範圍類型測試")
  class GetViewTypeTest {

    @Test
    @DisplayName("正常情況：應該返回所有可見範圍類型")
    void shouldReturnAllViewTypes() {
      // When
      List<DataScopeViewTypeVO> result = dataScopeService.getViewType();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();

      // 驗證每個類型都有必要屬性
      result.forEach(
          vo -> {
            assertThat(vo.getViewType()).isNotNull();
            assertThat(vo.getViewTypeName()).isNotNull();
            assertThat(vo.getViewTypeLevel()).isNotNull();
          });
    }

    @Test
    @DisplayName("排序驗證：應該按 level 升序排列")
    void shouldBeSortedByLevel() {
      // When
      List<DataScopeViewTypeVO> result = dataScopeService.getViewType();

      // Then
      for (int i = 0; i < result.size() - 1; i++) {
        assertThat(result.get(i).getViewTypeLevel())
            .isLessThanOrEqualTo(result.get(i + 1).getViewTypeLevel());
      }
    }
  }

  // ==================== getDataScopeType 測試 ====================

  @Nested
  @DisplayName("getDataScopeType 數據範圍類型測試")
  class GetDataScopeTypeTest {

    @Test
    @DisplayName("正常情況：應該返回所有數據範圍類型")
    void shouldReturnAllDataScopeTypes() {
      // When
      List<DataScopeDTO> result = dataScopeService.getDataScopeType();

      // Then
      assertThat(result).isNotNull();
      assertThat(result).isNotEmpty();

      // 驗證每個類型都有必要屬性
      result.forEach(
          dto -> {
            assertThat(dto.getDataScopeType()).isNotNull();
            assertThat(dto.getDataScopeTypeName()).isNotNull();
            assertThat(dto.getDataScopeTypeDesc()).isNotNull();
            assertThat(dto.getDataScopeTypeSort()).isNotNull();
          });
    }

    @Test
    @DisplayName("排序驗證：應該按 sort 升序排列")
    void shouldBeSortedBySort() {
      // When
      List<DataScopeDTO> result = dataScopeService.getDataScopeType();

      // Then
      for (int i = 0; i < result.size() - 1; i++) {
        assertThat(result.get(i).getDataScopeTypeSort())
            .isLessThanOrEqualTo(result.get(i + 1).getDataScopeTypeSort());
      }
    }
  }
}
