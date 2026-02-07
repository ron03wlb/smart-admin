package net.lab1024.sa.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.fixture.TestDTOs.SourceDTO;
import net.lab1024.sa.common.core.fixture.TestDTOs.TargetVO;
import net.lab1024.sa.common.core.fixture.TestDTOs.ValidatedDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SmartBeanUtil 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>copy() - 對象複製
 *   <li>copyList() - 列表複製
 *   <li>copyProperties() - 屬性複製
 *   <li>verify() - 對象驗證
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("SmartBeanUtil 單元測試")
class SmartBeanUtilTest {

  // ==================== copy() 測試 ====================

  @Nested
  @DisplayName("copy 對象複製測試")
  class CopyTest {

    @Test
    @DisplayName("正常情況：應該正確複製對象屬性")
    void shouldCopyProperties_whenValidSourceAndTarget() {
      // Given
      SourceDTO source =
          SourceDTO.builder().id(1L).name("張三").age(25).email("zhangsan@example.com").build();

      // When
      TargetVO result = SmartBeanUtil.copy(source, TargetVO.class);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getName()).isEqualTo("張三");
      assertThat(result.getAge()).isEqualTo(25);
      assertThat(result.getEmail()).isEqualTo("zhangsan@example.com");
    }

    @Test
    @DisplayName("source 為 null 時：應該返回 null")
    void shouldReturnNull_whenSourceIsNull() {
      // When
      TargetVO result = SmartBeanUtil.copy(null, TargetVO.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("target 為 null 時：應該返回 null")
    void shouldReturnNull_whenTargetIsNull() {
      // Given
      SourceDTO source = SourceDTO.builder().id(1L).name("張三").build();

      // When
      TargetVO result = SmartBeanUtil.copy(source, null);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("部分屬性為 null 時：應該正確複製包含 null 的屬性")
    void shouldCopyNullProperties_whenSomePropertiesAreNull() {
      // Given
      SourceDTO source =
          SourceDTO.builder().id(1L).name(null).age(null).email("test@test.com").build();

      // When
      TargetVO result = SmartBeanUtil.copy(source, TargetVO.class);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getName()).isNull();
      assertThat(result.getAge()).isNull();
      assertThat(result.getEmail()).isEqualTo("test@test.com");
    }
  }

  // ==================== copyList() 測試 ====================

  @Nested
  @DisplayName("copyList 列表複製測試")
  class CopyListTest {

    @Test
    @DisplayName("正常情況：應該正確複製列表所有元素")
    void shouldCopyAllElements_whenValidList() {
      // Given
      List<SourceDTO> sourceList =
          Arrays.asList(
              SourceDTO.builder().id(1L).name("張三").age(25).build(),
              SourceDTO.builder().id(2L).name("李四").age(30).build(),
              SourceDTO.builder().id(3L).name("王五").age(35).build());

      // When
      List<TargetVO> result = SmartBeanUtil.copyList(sourceList, TargetVO.class);

      // Then
      assertThat(result).hasSize(3);
      assertThat(result.get(0).getId()).isEqualTo(1L);
      assertThat(result.get(0).getName()).isEqualTo("張三");
      assertThat(result.get(1).getId()).isEqualTo(2L);
      assertThat(result.get(1).getName()).isEqualTo("李四");
      assertThat(result.get(2).getId()).isEqualTo(3L);
      assertThat(result.get(2).getName()).isEqualTo("王五");
    }

    @Test
    @DisplayName("空列表時：應該返回空列表")
    void shouldReturnEmptyList_whenSourceIsEmpty() {
      // Given
      List<SourceDTO> sourceList = Collections.emptyList();

      // When
      List<TargetVO> result = SmartBeanUtil.copyList(sourceList, TargetVO.class);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 列表時：應該返回空列表")
    void shouldReturnEmptyList_whenSourceIsNull() {
      // When
      List<TargetVO> result = SmartBeanUtil.copyList(null, TargetVO.class);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("單元素列表時：應該正確複製")
    void shouldCopySingleElement_whenListHasOneElement() {
      // Given
      List<SourceDTO> sourceList =
          Collections.singletonList(SourceDTO.builder().id(1L).name("獨苗").build());

      // When
      List<TargetVO> result = SmartBeanUtil.copyList(sourceList, TargetVO.class);

      // Then
      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("獨苗");
    }
  }

  // ==================== copyProperties() 測試 ====================

  @Nested
  @DisplayName("copyProperties 屬性複製測試")
  class CopyPropertiesTest {

    @Test
    @DisplayName("正常情況：應該直接複製屬性到目標對象")
    void shouldCopyDirectly_whenValidObjects() {
      // Given
      SourceDTO source = SourceDTO.builder().id(1L).name("張三").age(25).build();
      TargetVO target = new TargetVO();

      // When
      SmartBeanUtil.copyProperties(source, target);

      // Then
      assertThat(target.getId()).isEqualTo(1L);
      assertThat(target.getName()).isEqualTo("張三");
      assertThat(target.getAge()).isEqualTo(25);
    }
  }

  // ==================== verify() 測試 ====================

  @Nested
  @DisplayName("verify 對象驗證測試")
  class VerifyTest {

    @Test
    @DisplayName("有效對象時：應該返回 null")
    void shouldReturnNull_whenObjectIsValid() {
      // Given
      ValidatedDTO dto = ValidatedDTO.builder().id(1L).name("張三").age(25).build();

      // When
      String result = SmartBeanUtil.verify(dto);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("單個屬性無效時：應該返回錯誤信息")
    void shouldReturnError_whenSinglePropertyInvalid() {
      // Given
      ValidatedDTO dto = ValidatedDTO.builder().id(null).name("張三").age(25).build();

      // When
      String result = SmartBeanUtil.verify(dto);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).contains("ID不能為空");
    }

    @Test
    @DisplayName("多個屬性無效時：應該返回所有錯誤信息")
    void shouldReturnAllErrors_whenMultipleViolations() {
      // Given
      ValidatedDTO dto = ValidatedDTO.builder().id(null).name("").age(null).build();

      // When
      String result = SmartBeanUtil.verify(dto);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).contains("ID不能為空");
      assertThat(result).contains("年齡不能為空");
      // name 為空字串會觸發 @NotBlank
    }

    @Test
    @DisplayName("名稱長度不符時：應該返回長度錯誤信息")
    void shouldReturnSizeError_whenNameTooShort() {
      // Given
      ValidatedDTO dto = ValidatedDTO.builder().id(1L).name("A").age(25).build();

      // When
      String result = SmartBeanUtil.verify(dto);

      // Then
      assertThat(result).isNotNull();
      assertThat(result).contains("名稱長度必須在2-50之間");
    }
  }
}
