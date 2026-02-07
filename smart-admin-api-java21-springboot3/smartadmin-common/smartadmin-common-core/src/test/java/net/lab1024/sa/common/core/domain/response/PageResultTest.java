package net.lab1024.sa.common.core.domain.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PageResult 單元測試
 *
 * <p>測試覆蓋所有屬性的 getter/setter
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("PageResult 單元測試")
class PageResultTest {

  @Test
  @DisplayName("所有屬性：應該正確設置和獲取")
  void shouldSetAndGetAllProperties() {
    // Given
    PageResult<String> pageResult = new PageResult<>();

    // When
    pageResult.setPageNum(1L);
    pageResult.setPageSize(10L);
    pageResult.setTotal(100L);
    pageResult.setPages(10L);
    pageResult.setList(Arrays.asList("item1", "item2", "item3"));
    pageResult.setEmptyFlag(false);

    // Then
    assertThat(pageResult.getPageNum()).isEqualTo(1L);
    assertThat(pageResult.getPageSize()).isEqualTo(10L);
    assertThat(pageResult.getTotal()).isEqualTo(100L);
    assertThat(pageResult.getPages()).isEqualTo(10L);
    assertThat(pageResult.getList()).hasSize(3);
    assertThat(pageResult.getEmptyFlag()).isFalse();
  }

  @Test
  @DisplayName("emptyFlag：空列表時應該為 true")
  void emptyFlag_shouldBeTrueWhenListEmpty() {
    // Given
    PageResult<String> pageResult = new PageResult<>();

    // When
    pageResult.setList(Collections.emptyList());
    pageResult.setEmptyFlag(true);

    // Then
    assertThat(pageResult.getEmptyFlag()).isTrue();
    assertThat(pageResult.getList()).isEmpty();
  }

  @Test
  @DisplayName("pages：應該正確記錄總頁數")
  void pages_shouldRecordTotalPages() {
    // Given
    PageResult<Integer> pageResult = new PageResult<>();

    // When
    pageResult.setTotal(95L);
    pageResult.setPageSize(10L);
    pageResult.setPages(10L); // 95/10 = 9.5 -> 10 pages

    // Then
    assertThat(pageResult.getPages()).isEqualTo(10L);
  }

  @Test
  @DisplayName("list 為 null：應該允許 null 列表")
  void list_shouldAllowNull() {
    // Given
    PageResult<String> pageResult = new PageResult<>();

    // When
    pageResult.setList(null);

    // Then
    assertThat(pageResult.getList()).isNull();
  }

  @Test
  @DisplayName("泛型類型：應該正確處理不同類型")
  void genericType_shouldHandleDifferentTypes() {
    // Given
    PageResult<Integer> intResult = new PageResult<>();
    PageResult<String> stringResult = new PageResult<>();

    // When
    intResult.setList(Arrays.asList(1, 2, 3));
    stringResult.setList(Arrays.asList("a", "b", "c"));

    // Then
    assertThat(intResult.getList()).containsExactly(1, 2, 3);
    assertThat(stringResult.getList()).containsExactly("a", "b", "c");
  }
}
