package net.lab1024.sa.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import net.lab1024.sa.common.core.fixture.TestEnums.GenderEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SmartEnumUtil 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>checkEnum() - 枚舉值校驗
 *   <li>getEnumByValue() - 根據值獲取枚舉
 *   <li>getEnumDescByValue() - 獲取枚舉描述
 *   <li>differenceValueList() - 排除指定枚舉值
 *   <li>getEnumByDesc() - 根據描述獲取枚舉
 *   <li>getEnumByName() - 根據名稱獲取枚舉
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("SmartEnumUtil 單元測試")
class SmartEnumUtilTest {

  // ==================== checkEnum() 測試 ====================

  @Nested
  @DisplayName("checkEnum 枚舉值校驗測試")
  class CheckEnumTest {

    @Test
    @DisplayName("有效值：應該返回 true")
    void shouldReturnTrue_whenValueValid() {
      // When & Then
      assertThat(SmartEnumUtil.checkEnum(1, GenderEnum.class)).isTrue();
      assertThat(SmartEnumUtil.checkEnum(2, GenderEnum.class)).isTrue();
      assertThat(SmartEnumUtil.checkEnum(0, GenderEnum.class)).isTrue();
    }

    @Test
    @DisplayName("無效值：應該返回 false")
    void shouldReturnFalse_whenValueInvalid() {
      // When & Then
      assertThat(SmartEnumUtil.checkEnum(99, GenderEnum.class)).isFalse();
      assertThat(SmartEnumUtil.checkEnum(-1, GenderEnum.class)).isFalse();
    }

    @Test
    @DisplayName("null 值：應該返回 false")
    void shouldReturnFalse_whenValueNull() {
      // When & Then
      assertThat(SmartEnumUtil.checkEnum(null, GenderEnum.class)).isFalse();
    }
  }

  // ==================== getEnumByValue() 測試 ====================

  @Nested
  @DisplayName("getEnumByValue 根據值獲取枚舉測試")
  class GetEnumByValueTest {

    @Test
    @DisplayName("找到匹配：應該返回對應枚舉")
    void shouldReturnEnum_whenFound() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByValue(1, GenderEnum.class);

      // Then
      assertThat(result).isEqualTo(GenderEnum.MALE);
    }

    @Test
    @DisplayName("未找到匹配：應該返回 null")
    void shouldReturnNull_whenNotFound() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByValue(99, GenderEnum.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("null 值：應該返回 null")
    void shouldReturnNull_whenValueNull() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByValue(null, GenderEnum.class);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== getEnumDescByValue() 測試 ====================

  @Nested
  @DisplayName("getEnumDescByValue 獲取枚舉描述測試")
  class GetEnumDescByValueTest {

    @Test
    @DisplayName("找到匹配：應該返回描述")
    void shouldReturnDesc_whenFound() {
      // When
      String result = SmartEnumUtil.getEnumDescByValue(1, GenderEnum.class);

      // Then
      assertThat(result).isEqualTo("男");
    }

    @Test
    @DisplayName("未找到匹配：應該返回 null")
    void shouldReturnNull_whenNotFound() {
      // When
      String result = SmartEnumUtil.getEnumDescByValue(99, GenderEnum.class);

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("null 值：應該返回 null")
    void shouldReturnNull_whenValueNull() {
      // When
      String result = SmartEnumUtil.getEnumDescByValue(null, GenderEnum.class);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== getEnumDescByValueList() 測試 ====================

  @Nested
  @DisplayName("getEnumDescByValueList 多值描述測試")
  class GetEnumDescByValueListTest {

    @Test
    @DisplayName("多值：應該用逗號連接描述")
    void shouldJoinDescriptions() {
      // Given
      List<Integer> values = Arrays.asList(1, 2);

      // When
      String result = SmartEnumUtil.getEnumDescByValueList(values, GenderEnum.class);

      // Then
      assertThat(result).contains("男");
      assertThat(result).contains("女");
    }

    @Test
    @DisplayName("空列表：應該返回空字串")
    void shouldReturnEmpty_whenListEmpty() {
      // When
      String result = SmartEnumUtil.getEnumDescByValueList(List.of(), GenderEnum.class);

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("null 列表：應該返回空字串")
    void shouldReturnEmpty_whenListNull() {
      // When
      String result = SmartEnumUtil.getEnumDescByValueList(null, GenderEnum.class);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== differenceValueList() 測試 ====================

  @Nested
  @DisplayName("differenceValueList 排除枚舉值測試")
  class DifferenceValueListTest {

    @Test
    @DisplayName("排除指定枚舉：應該返回剩餘值")
    void shouldExcludeSpecified() {
      // When
      List<Object> result = SmartEnumUtil.differenceValueList(GenderEnum.class, GenderEnum.UNKNOWN);

      // Then
      assertThat(result).containsExactlyInAnyOrder(1, 2);
      assertThat(result).doesNotContain(0);
    }

    @Test
    @DisplayName("排除多個枚舉：應該返回剩餘值")
    void shouldExcludeMultiple() {
      // When
      List<Object> result =
          SmartEnumUtil.differenceValueList(GenderEnum.class, GenderEnum.MALE, GenderEnum.FEMALE);

      // Then
      assertThat(result).containsExactly(0);
    }

    @Test
    @DisplayName("不排除任何：應該返回所有值")
    void shouldReturnAll_whenNoExclusion() {
      // When
      List<Object> result = SmartEnumUtil.differenceValueList(GenderEnum.class);

      // Then
      assertThat(result).containsExactlyInAnyOrder(1, 2, 0);
    }
  }

  // ==================== getEnumByDesc() 測試 ====================

  @Nested
  @DisplayName("getEnumByDesc 根據描述獲取枚舉測試")
  class GetEnumByDescTest {

    @Test
    @DisplayName("找到匹配：應該返回對應枚舉")
    void shouldFindByDescription() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByDesc("男", GenderEnum.class);

      // Then
      assertThat(result).isEqualTo(GenderEnum.MALE);
    }

    @Test
    @DisplayName("未找到匹配：應該返回 null")
    void shouldReturnNull_whenDescNotFound() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByDesc("不存在", GenderEnum.class);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== getEnumByName() 測試 ====================

  @Nested
  @DisplayName("getEnumByName 根據名稱獲取枚舉測試")
  class GetEnumByNameTest {

    @Test
    @DisplayName("精確匹配：應該返回對應枚舉")
    void shouldFindByName() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByName("MALE", GenderEnum.class);

      // Then
      assertThat(result).isEqualTo(GenderEnum.MALE);
    }

    @Test
    @DisplayName("忽略大小寫：應該返回對應枚舉")
    void shouldIgnoreCase() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByName("male", GenderEnum.class);

      // Then
      assertThat(result).isEqualTo(GenderEnum.MALE);
    }

    @Test
    @DisplayName("未找到匹配：應該返回 null")
    void shouldReturnNull_whenNameNotFound() {
      // When
      GenderEnum result = SmartEnumUtil.getEnumByName("NOT_EXIST", GenderEnum.class);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== getEnumDesc() 測試 ====================

  @Nested
  @DisplayName("getEnumDesc 獲取枚舉說明測試")
  class GetEnumDescTest {

    @Test
    @DisplayName("應該返回所有枚舉的 value:desc 格式說明")
    void shouldReturnAllEnumDescriptions() {
      // When
      String result = SmartEnumUtil.getEnumDesc(GenderEnum.class);

      // Then
      assertThat(result).contains("1：男");
      assertThat(result).contains("2：女");
      assertThat(result).contains("0：未知");
    }
  }
}
