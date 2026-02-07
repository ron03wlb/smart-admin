package net.lab1024.sa.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SmartStringUtil 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>splitConvertToSet/List - 字符串分割轉集合
 *   <li>splitConvertToIntList/Set/Array - 整數類型轉換
 *   <li>splitConvertToLongList/Array - Long 類型轉換
 *   <li>upperCaseFirstChar - 首字母大寫
 *   <li>replace - 範圍替換
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("SmartStringUtil 單元測試")
class SmartStringUtilTest {

  // ==================== splitConvertToSet/List 測試 ====================

  @Nested
  @DisplayName("splitConvertToSet/List 測試")
  class SplitToCollectionTest {

    @Test
    @DisplayName("splitConvertToSet：字符串轉 Set")
    void splitConvertToSet_shouldConvertToSet() {
      // When
      Set<String> result = SmartStringUtil.splitConvertToSet("a,b,c,a", ",");

      // Then
      assertThat(result).containsExactlyInAnyOrder("a", "b", "c");
    }

    @Test
    @DisplayName("splitConvertToSet：空字符串返回空 Set")
    void splitConvertToSet_shouldReturnEmptyForEmpty() {
      // When
      Set<String> result = SmartStringUtil.splitConvertToSet("", ",");

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("splitConvertToList：字符串轉 List")
    void splitConvertToList_shouldConvertToList() {
      // When
      List<String> result = SmartStringUtil.splitConvertToList("a,b,c", ",");

      // Then
      assertThat(result).containsExactly("a", "b", "c");
    }
  }

  // ==================== splitConvertToInt 測試 ====================

  @Nested
  @DisplayName("splitConvertToInt 測試")
  class SplitToIntTest {

    @Test
    @DisplayName("splitConvertToIntList：數字字符串轉 List")
    void splitConvertToIntList_shouldConvertToIntList() {
      // When
      List<Integer> result = SmartStringUtil.splitConvertToIntList("1,2,3", ",");

      // Then
      assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("splitConvertToIntList：無效數字使用默認值")
    void splitConvertToIntList_shouldUseDefaultForInvalid() {
      // When
      List<Integer> result = SmartStringUtil.splitConvertToIntList("1,abc,3", ",", -1);

      // Then
      assertThat(result).containsExactly(1, -1, 3);
    }

    @Test
    @DisplayName("splitConvertToIntSet：數字字符串轉 Set")
    void splitConvertToIntSet_shouldConvertToIntSet() {
      // When
      Set<Integer> result = SmartStringUtil.splitConvertToIntSet("1,2,3,1", ",");

      // Then
      assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    @DisplayName("splitConvertToIntArray：數字字符串轉數組")
    void splitConvertToIntArray_shouldConvertToArray() {
      // When
      int[] result = SmartStringUtil.splitConvertToIntArray("1,2,3", ",");

      // Then
      assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("splitConvertToIntList：空字符串返回空列表")
    void splitConvertToIntList_shouldReturnEmptyForEmpty() {
      // When
      List<Integer> result = SmartStringUtil.splitConvertToIntList("", ",");

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== splitConvertToLong 測試 ====================

  @Nested
  @DisplayName("splitConvertToLong 測試")
  class SplitToLongTest {

    @Test
    @DisplayName("splitConvertToLongList：Long 類型轉換")
    void splitConvertToLongList_shouldConvert() {
      // When
      List<Long> result = SmartStringUtil.splitConvertToLongList("1,2,3", ",");

      // Then
      assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("splitConvertToLongArray：Long 數組轉換")
    void splitConvertToLongArray_shouldConvert() {
      // When
      long[] result = SmartStringUtil.splitConvertToLongArray("100,200,300", ",");

      // Then
      assertThat(result).containsExactly(100L, 200L, 300L);
    }
  }

  // ==================== splitConvertToByte 測試 ====================

  @Nested
  @DisplayName("splitConvertToByte 測試")
  class SplitToByteTest {

    @Test
    @DisplayName("splitConvertToByteList：Byte 類型轉換")
    void splitConvertToByteList_shouldConvert() {
      // When
      List<Byte> result = SmartStringUtil.splitConvertToByteList("1,2,3", ",");

      // Then
      assertThat(result).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }
  }

  // ==================== splitConvertToDouble 測試 ====================

  @Nested
  @DisplayName("splitConvertToDouble 測試")
  class SplitToDoubleTest {

    @Test
    @DisplayName("splitConvertToDoubleList：Double 類型轉換")
    void splitConvertToDoubleList_shouldConvert() {
      // When
      List<Double> result = SmartStringUtil.splitConvertToDoubleList("1.1,2.2,3.3", ",");

      // Then
      assertThat(result).containsExactly(1.1, 2.2, 3.3);
    }
  }

  // ==================== splitConvertToFloat 測試 ====================

  @Nested
  @DisplayName("splitConvertToFloat 測試")
  class SplitToFloatTest {

    @Test
    @DisplayName("splitConvertToFloatList：Float 類型轉換")
    void splitConvertToFloatList_shouldConvert() {
      // When
      List<Float> result = SmartStringUtil.splitConvertToFloatList("1.1,2.2,3.3", ",");

      // Then
      assertThat(result).containsExactly(1.1f, 2.2f, 3.3f);
    }
  }

  // ==================== upperCaseFirstChar 測試 ====================

  @Nested
  @DisplayName("upperCaseFirstChar 測試")
  class UpperCaseFirstCharTest {

    @Test
    @DisplayName("upperCaseFirstChar：首字母大寫")
    void upperCaseFirstChar_shouldCapitalizeFirst() {
      // When
      String result = SmartStringUtil.upperCaseFirstChar("hello");

      // Then
      assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("upperCaseFirstChar：已大寫不變")
    void upperCaseFirstChar_shouldNotChangeIfAlreadyUpper() {
      // When
      String result = SmartStringUtil.upperCaseFirstChar("Hello");

      // Then
      assertThat(result).isEqualTo("Hello");
    }

    @Test
    @DisplayName("upperCaseFirstChar：空字符串不變")
    void upperCaseFirstChar_shouldHandleEmpty() {
      // When
      String result = SmartStringUtil.upperCaseFirstChar("");

      // Then
      assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("upperCaseFirstChar：null 返回 null")
    void upperCaseFirstChar_shouldHandleNull() {
      // When
      String result = SmartStringUtil.upperCaseFirstChar(null);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== replace 測試 ====================

  @Nested
  @DisplayName("replace 測試")
  class ReplaceTest {

    @Test
    @DisplayName("replace：替換指定範圍")
    void replace_shouldReplaceRange() {
      // When
      String result = SmartStringUtil.replace("1234567890", 3, 7, "*");

      // Then
      assertThat(result).isEqualTo("123****890");
    }

    @Test
    @DisplayName("replace：begin > end 返回原字符串")
    void replace_shouldReturnOriginalForInvalidRange() {
      // When
      String result = SmartStringUtil.replace("hello", 5, 2, "*");

      // Then
      assertThat(result).isEqualTo("hello");
    }

    @Test
    @DisplayName("replace：begin 超出範圍返回原字符串")
    void replace_shouldReturnOriginalWhenBeginOutOfBounds() {
      // When
      String result = SmartStringUtil.replace("hello", 10, 15, "*");

      // Then
      assertThat(result).isEqualTo("hello");
    }
  }
}
