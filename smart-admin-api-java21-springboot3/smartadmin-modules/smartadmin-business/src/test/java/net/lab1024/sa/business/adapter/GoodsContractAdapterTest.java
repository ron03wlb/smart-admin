package net.lab1024.sa.business.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.business.dto.GoodsDTO;
import net.lab1024.sa.business.goods.dao.GoodsDao;
import net.lab1024.sa.business.goods.domain.entity.GoodsEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GoodsContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到、已刪除商品）
 *   <li>Vavr Option 正確使用
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class GoodsContractAdapterTest {

  @Mock private GoodsDao goodsDao;

  @InjectMocks private GoodsContractAdapter adapter;

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long goodsId = 1L;
    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsId(goodsId);
    entity.setGoodsName("iPhone 15");
    entity.setPrice(new BigDecimal("999.99"));
    entity.setDeletedFlag(Boolean.FALSE);

    when(goodsDao.selectById(goodsId)).thenReturn(entity);

    // When
    Option<GoodsDTO> result = adapter.getById(goodsId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getGoodsId()).isEqualTo(goodsId);
    assertThat(result.get().getGoodsName()).isEqualTo("iPhone 15");
    assertThat(result.get().getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
    verify(goodsDao).selectById(goodsId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long goodsId = 999L;
    when(goodsDao.selectById(goodsId)).thenReturn(null);

    // When
    Option<GoodsDTO> result = adapter.getById(goodsId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(goodsDao).selectById(goodsId);
  }

  @Test
  void testGetById_DeletedGoods() {
    // Given
    Long goodsId = 1L;
    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsId(goodsId);
    entity.setGoodsName("Deleted Item");
    entity.setDeletedFlag(Boolean.TRUE);

    when(goodsDao.selectById(goodsId)).thenReturn(entity);

    // When
    Option<GoodsDTO> result = adapter.getById(goodsId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(goodsDao).selectById(goodsId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("goodsId cannot be null");
  }

  // ==================== queryByCategoryId 測試 ====================

  @Test
  void testQueryByCategoryId_Success() {
    // Given
    Long categoryId = 10L;
    GoodsEntity entity1 = new GoodsEntity();
    entity1.setGoodsId(1L);
    entity1.setGoodsName("Product A");
    entity1.setCategoryId(categoryId);
    entity1.setDeletedFlag(Boolean.FALSE);

    GoodsEntity entity2 = new GoodsEntity();
    entity2.setGoodsId(2L);
    entity2.setGoodsName("Product B");
    entity2.setCategoryId(categoryId);
    entity2.setDeletedFlag(Boolean.FALSE);

    List<GoodsEntity> entityList = Arrays.asList(entity1, entity2);
    when(goodsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(entityList);

    // When
    List<GoodsDTO> result = adapter.queryByCategoryId(categoryId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getGoodsName()).isEqualTo("Product A");
    assertThat(result.get(1).getGoodsName()).isEqualTo("Product B");
    verify(goodsDao).selectList(any(LambdaQueryWrapper.class));
  }

  @Test
  void testQueryByCategoryId_EmptyResult() {
    // Given
    Long categoryId = 999L;
    when(goodsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

    // When
    List<GoodsDTO> result = adapter.queryByCategoryId(categoryId);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(goodsDao).selectList(any(LambdaQueryWrapper.class));
  }

  @Test
  void testQueryByCategoryId_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByCategoryId(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("categoryId cannot be null");
  }

  // ==================== listAllOnShelf 測試 ====================

  @Test
  void testListAllOnShelf_Success() {
    // Given
    GoodsEntity entity1 = new GoodsEntity();
    entity1.setGoodsId(1L);
    entity1.setGoodsName("On Shelf Product 1");
    entity1.setDeletedFlag(Boolean.FALSE);
    entity1.setShelvesFlag(Boolean.TRUE);

    GoodsEntity entity2 = new GoodsEntity();
    entity2.setGoodsId(2L);
    entity2.setGoodsName("On Shelf Product 2");
    entity2.setDeletedFlag(Boolean.FALSE);
    entity2.setShelvesFlag(Boolean.TRUE);

    List<GoodsEntity> entityList = Arrays.asList(entity1, entity2);
    when(goodsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(entityList);

    // When
    List<GoodsDTO> result = adapter.listAllOnShelf();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getGoodsName()).isEqualTo("On Shelf Product 1");
    assertThat(result.get(1).getGoodsName()).isEqualTo("On Shelf Product 2");
    verify(goodsDao).selectList(any(LambdaQueryWrapper.class));
  }

  @Test
  void testListAllOnShelf_EmptyResult() {
    // Given
    when(goodsDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

    // When
    List<GoodsDTO> result = adapter.listAllOnShelf();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(goodsDao).selectList(any(LambdaQueryWrapper.class));
  }
}
