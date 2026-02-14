package net.lab1024.sa.common.mybatis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.common.core.domain.exception.BusinessException;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.core.domain.response.PageResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * SmartPageUtil 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>convert2PageQuery() - 分頁參數轉換
 *   <li>convert2PageResult() - 結果轉換
 *   <li>subListPage() - 內存分頁
 *   <li>SQL 注入檢測
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@DisplayName("SmartPageUtil 單元測試")
class SmartPageUtilTest {

  // ==================== convert2PageQuery() 測試 ====================

  @Nested
  @DisplayName("convert2PageQuery 分頁參數轉換測試")
  class Convert2PageQueryTest {

    @Test
    @DisplayName("基本分頁參數：應該正確轉換頁碼和每頁數量")
    void shouldConvertBasicParams() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(2L);
      pageParam.setPageSize(20L);

      // When
      Page<?> result = SmartPageUtil.convert2PageQuery(pageParam);

      // Then
      assertThat(result.getCurrent()).isEqualTo(2);
      assertThat(result.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("帶 searchCount：應該正確設置 searchCount")
    void shouldSetSearchCount() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(1L);
      pageParam.setPageSize(10L);
      pageParam.setSearchCount(false);

      // When
      Page<?> result = SmartPageUtil.convert2PageQuery(pageParam);

      // Then
      assertThat(result.searchCount()).isFalse();
    }

    @Test
    @DisplayName("無排序字段：應該返回無排序的 Page")
    void shouldReturnPageWithoutOrders_whenNoSortItems() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(1L);
      pageParam.setPageSize(10L);
      pageParam.setSortItemList(null);

      // When
      Page<?> result = SmartPageUtil.convert2PageQuery(pageParam);

      // Then
      assertThat(result.orders()).isEmpty();
    }

    @Test
    @DisplayName("帶升序排序：應該設置正確的排序方向")
    void shouldHandleAscSortItem() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(1L);
      pageParam.setPageSize(10L);

      PageParam.SortItem sortItem = new PageParam.SortItem();
      sortItem.setColumn("create_time");
      sortItem.setIsAsc(true);
      pageParam.setSortItemList(Collections.singletonList(sortItem));

      // When
      Page<?> result = SmartPageUtil.convert2PageQuery(pageParam);

      // Then
      assertThat(result.orders()).hasSize(1);
      assertThat(result.orders().get(0).getColumn()).isEqualTo("create_time");
      assertThat(result.orders().get(0).isAsc()).isTrue();
    }

    @Test
    @DisplayName("帶降序排序：應該設置正確的排序方向")
    void shouldHandleDescSortItem() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(1L);
      pageParam.setPageSize(10L);

      PageParam.SortItem sortItem = new PageParam.SortItem();
      sortItem.setColumn("update_time");
      sortItem.setIsAsc(false);
      pageParam.setSortItemList(Collections.singletonList(sortItem));

      // When
      Page<?> result = SmartPageUtil.convert2PageQuery(pageParam);

      // Then
      assertThat(result.orders()).hasSize(1);
      assertThat(result.orders().get(0).getColumn()).isEqualTo("update_time");
      assertThat(result.orders().get(0).isAsc()).isFalse();
    }

    @Test
    @DisplayName("空排序字段：應該跳過空的 column")
    void shouldSkipEmptyColumn() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(1L);
      pageParam.setPageSize(10L);

      PageParam.SortItem sortItem = new PageParam.SortItem();
      sortItem.setColumn("");
      sortItem.setIsAsc(true);
      pageParam.setSortItemList(Collections.singletonList(sortItem));

      // When
      Page<?> result = SmartPageUtil.convert2PageQuery(pageParam);

      // Then
      assertThat(result.orders()).isEmpty();
    }

    @Test
    @DisplayName("SQL 注入檢測：應該拋出 BusinessException")
    void shouldThrowOnSqlInjection() {
      // Given
      PageParam pageParam = new PageParam();
      pageParam.setPageNum(1L);
      pageParam.setPageSize(10L);

      PageParam.SortItem sortItem = new PageParam.SortItem();
      sortItem.setColumn("id; DROP TABLE users; --");
      sortItem.setIsAsc(true);
      pageParam.setSortItemList(Collections.singletonList(sortItem));

      // When & Then
      assertThatThrownBy(() -> SmartPageUtil.convert2PageQuery(pageParam))
          .isInstanceOf(BusinessException.class)
          .hasMessageContaining("SQL注入");
    }
  }

  // ==================== convert2PageResult() 測試 ====================

  @Nested
  @DisplayName("convert2PageResult 結果轉換測試")
  class Convert2PageResultTest {

    @Test
    @DisplayName("Page 轉 PageResult：應該正確轉換所有屬性")
    void shouldConvertFromPage() {
      // Given
      Page<String> page = new Page<>(1, 10);
      page.setTotal(100);
      List<String> data = Arrays.asList("item1", "item2", "item3");

      // When
      PageResult<String> result = SmartPageUtil.convert2PageResult(page, data);

      // Then
      assertThat(result.getPageNum()).isEqualTo(1);
      assertThat(result.getPageSize()).isEqualTo(10);
      assertThat(result.getTotal()).isEqualTo(100);
      assertThat(result.getList()).hasSize(3);
      assertThat(result.getEmptyFlag()).isFalse();
    }

    @Test
    @DisplayName("空列表：應該設置 emptyFlag 為 true")
    void shouldSetEmptyFlag() {
      // Given
      Page<String> page = new Page<>(1, 10);
      page.setTotal(0);
      List<String> data = Collections.emptyList();

      // When
      PageResult<String> result = SmartPageUtil.convert2PageResult(page, data);

      // Then
      assertThat(result.getEmptyFlag()).isTrue();
      assertThat(result.getList()).isEmpty();
    }
  }

  // ==================== subListPage() 測試 ====================

  @Nested
  @DisplayName("subListPage 內存分頁測試")
  class SubListPageTest {

    @Test
    @DisplayName("基本分頁：應該返回正確的分頁數據")
    void shouldReturnCorrectPage() {
      // Given
      List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

      // When
      PageResult<Integer> result = SmartPageUtil.subListPage(1, 3, list);

      // Then
      assertThat(result.getList()).containsExactly(1, 2, 3);
      assertThat(result.getPageNum()).isEqualTo(1);
      assertThat(result.getTotal()).isEqualTo(10);
      assertThat(result.getPages()).isEqualTo(4); // 10/3 = 3.33 -> 4 pages
    }

    @Test
    @DisplayName("第二頁：應該返回正確的數據")
    void shouldReturnSecondPage() {
      // Given
      List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

      // When
      PageResult<Integer> result = SmartPageUtil.subListPage(2, 3, list);

      // Then
      assertThat(result.getList()).containsExactly(4, 5, 6);
    }

    @Test
    @DisplayName("最後一頁部分數據：應該返回剩餘數據")
    void shouldHandleLastPagePartialData() {
      // Given
      List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

      // When
      PageResult<Integer> result = SmartPageUtil.subListPage(4, 3, list);

      // Then
      assertThat(result.getList()).containsExactly(10);
    }

    @Test
    @DisplayName("超出總頁數：應該返回空列表")
    void shouldReturnEmptyWhenExceedPages() {
      // Given
      List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

      // When
      PageResult<Integer> result = SmartPageUtil.subListPage(10, 3, list);

      // Then
      assertThat(result.getList()).isEmpty();
      assertThat(result.getTotal()).isEqualTo(5);
    }

    @Test
    @DisplayName("剛好整除：應該計算正確的頁數")
    void shouldCalculatePagesCorrectly() {
      // Given
      List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);

      // When
      PageResult<Integer> result = SmartPageUtil.subListPage(1, 3, list);

      // Then
      assertThat(result.getPages()).isEqualTo(2); // 6/3 = 2 pages exactly
    }
  }
}
