package net.lab1024.sa.business.goods.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.business.category.constant.CategoryTypeEnum;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.category.manager.CategoryCacheManager;
import net.lab1024.sa.business.goods.constant.GoodsStatusEnum;
import net.lab1024.sa.business.goods.dao.GoodsDao;
import net.lab1024.sa.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.business.goods.domain.form.GoodsQueryForm;
import net.lab1024.sa.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.business.goods.domain.vo.GoodsVO;
import net.lab1024.sa.business.goods.manager.GoodsManager;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import net.lab1024.sa.support.dict.service.DictService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * GoodsService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>商品 CRUD 操作
 *   <li>分類驗證邏輯
 *   <li>商品狀態約束（只有售罄可刪除）
 *   <li>分頁查詢和分類名稱填充
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsService 單元測試")
class GoodsServiceTest {

  @Mock private GoodsDao goodsDao;

  @Mock private CategoryCacheManager categoryCacheManager;

  @Mock private GoodsManager goodsManager;

  @Mock private DataTracerService dataTracerService;

  @Mock private DictService dictService;

  @InjectMocks private GoodsService goodsService;

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增商品測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：分類存在且類型正確時應該成功新增")
    void shouldAddSuccessWhenCategoryValid() {
      // Given
      GoodsAddForm addForm = createTestAddForm(1L, "TestGoods");
      CategoryEntity category = createTestCategory(1L, CategoryTypeEnum.GOODS.getValue());

      when(categoryCacheManager.queryCategory(1L)).thenReturn(category);

      // When
      ResponseDTO<String> result = goodsService.add(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(goodsManager).addGoodsTransaction(addForm);
    }

    @Test
    @DisplayName("異常情況：分類不存在時應該返回錯誤")
    void shouldReturnErrorWhenCategoryNotFound() {
      // Given
      GoodsAddForm addForm = createTestAddForm(999L, "TestGoods");
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = goodsService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类目不存在");
      verify(goodsManager, never()).addGoodsTransaction(any());
    }

    @Test
    @DisplayName("異常情況：分類類型不是商品類目時應該返回錯誤")
    void shouldReturnErrorWhenCategoryTypeNotGoods() {
      // Given
      GoodsAddForm addForm = createTestAddForm(1L, "TestGoods");
      // 創建非商品類型的分類
      CategoryEntity category = createTestCategory(1L, 99);

      when(categoryCacheManager.queryCategory(1L)).thenReturn(category);

      // When
      ResponseDTO<String> result = goodsService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("类目不存在");
      verify(goodsManager, never()).addGoodsTransaction(any());
    }

    @Test
    @DisplayName("異常情況：分類已刪除時應該返回錯誤")
    void shouldReturnErrorWhenCategoryDeleted() {
      // Given
      GoodsAddForm addForm = createTestAddForm(1L, "TestGoods");
      CategoryEntity category = createTestCategory(1L, CategoryTypeEnum.GOODS.getValue());
      category.setDeletedFlag(true);

      when(categoryCacheManager.queryCategory(1L)).thenReturn(category);

      // When
      ResponseDTO<String> result = goodsService.add(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(goodsManager, never()).addGoodsTransaction(any());
    }
  }

  // ==================== update 測試 ====================

  @Nested
  @DisplayName("update 更新商品測試")
  class UpdateTest {

    @Test
    @DisplayName("正常情況：商品和分類都有效時應該成功更新")
    void shouldUpdateSuccessWhenValid() {
      // Given
      GoodsUpdateForm updateForm = createTestUpdateForm(1L, 1L, "UpdatedGoods");
      CategoryEntity category = createTestCategory(1L, CategoryTypeEnum.GOODS.getValue());
      GoodsEntity originEntity = createTestGoodsEntity(1L, "OriginalGoods");

      when(categoryCacheManager.queryCategory(1L)).thenReturn(category);
      when(goodsDao.selectById(1L)).thenReturn(originEntity);

      // When
      ResponseDTO<String> result = goodsService.update(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(goodsManager).updateGoodsTransaction(updateForm, originEntity);
    }

    @Test
    @DisplayName("異常情況：分類不存在時應該返回錯誤")
    void shouldReturnErrorWhenCategoryNotFound() {
      // Given
      GoodsUpdateForm updateForm = createTestUpdateForm(1L, 999L, "UpdatedGoods");
      when(categoryCacheManager.queryCategory(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = goodsService.update(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(goodsManager, never()).updateGoodsTransaction(any(), any());
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除商品測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：售罄商品應該成功刪除")
    void shouldDeleteSuccessWhenGoodsSoldOut() {
      // Given
      Long goodsId = 1L;
      GoodsEntity entity = createTestGoodsEntity(goodsId, "SoldOutGoods");
      entity.setGoodsStatus(GoodsStatusEnum.SELL_OUT.getValue());

      when(goodsDao.selectById(goodsId)).thenReturn(entity);

      // When
      ResponseDTO<String> result = goodsService.delete(goodsId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(goodsManager).deleteGoodsTransaction(goodsId);
    }

    @Test
    @DisplayName("異常情況：商品不存在時應該返回錯誤")
    void shouldReturnErrorWhenGoodsNotFound() {
      // Given
      Long goodsId = 999L;
      when(goodsDao.selectById(goodsId)).thenReturn(null);

      // When
      ResponseDTO<String> result = goodsService.delete(goodsId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("商品不存在");
      verify(goodsManager, never()).deleteGoodsTransaction(anyLong());
    }

    @Test
    @DisplayName("異常情況：非售罄商品應該返回錯誤")
    void shouldReturnErrorWhenGoodsNotSoldOut() {
      // Given
      Long goodsId = 1L;
      GoodsEntity entity = createTestGoodsEntity(goodsId, "OnSaleGoods");
      entity.setGoodsStatus(GoodsStatusEnum.SELL.getValue());

      when(goodsDao.selectById(goodsId)).thenReturn(entity);

      // When
      ResponseDTO<String> result = goodsService.delete(goodsId);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("售罄");
      verify(goodsManager, never()).deleteGoodsTransaction(anyLong());
    }
  }

  // ==================== batchDelete 測試 ====================

  @Nested
  @DisplayName("batchDelete 批量刪除測試")
  class BatchDeleteTest {

    @Test
    @DisplayName("正常情況：應該成功批量刪除")
    void shouldBatchDeleteSuccess() {
      // Given
      List<Long> goodsIdList = Arrays.asList(1L, 2L, 3L);

      // When
      ResponseDTO<String> result = goodsService.batchDelete(goodsIdList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(goodsManager).batchDeleteGoodsTransaction(goodsIdList);
    }

    @Test
    @DisplayName("邊界情況：空列表應該直接返回成功")
    void shouldReturnOkWhenListEmpty() {
      // Given
      List<Long> goodsIdList = Collections.emptyList();

      // When
      ResponseDTO<String> result = goodsService.batchDelete(goodsIdList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(goodsManager, never()).batchDeleteGoodsTransaction(any());
    }

    @Test
    @DisplayName("邊界情況：null 列表應該直接返回成功")
    void shouldReturnOkWhenListNull() {
      // When
      ResponseDTO<String> result = goodsService.batchDelete(null);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(goodsManager, never()).batchDeleteGoodsTransaction(any());
    }
  }

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回帶分類名稱的分頁結果")
    void shouldReturnPageResultWithCategoryName() {
      // Given
      GoodsQueryForm queryForm = new GoodsQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      GoodsVO vo1 = createTestGoodsVO(1L, "Goods1", 100L);
      GoodsVO vo2 = createTestGoodsVO(2L, "Goods2", 100L);
      List<GoodsVO> voList = Arrays.asList(vo1, vo2);

      CategoryEntity category = createTestCategory(100L, CategoryTypeEnum.GOODS.getValue());
      category.setCategoryName("Electronics");

      when(goodsDao.query(any(Page.class), any(GoodsQueryForm.class))).thenReturn(voList);
      when(categoryCacheManager.queryCategory(100L)).thenReturn(category);

      // When
      ResponseDTO<PageResult<GoodsVO>> result = goodsService.query(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).hasSize(2);
      assertThat(result.getData().getList().get(0).getCategoryName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageWhenNoData() {
      // Given
      GoodsQueryForm queryForm = new GoodsQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(goodsDao.query(any(Page.class), any(GoodsQueryForm.class)))
          .thenReturn(Collections.emptyList());

      // When
      ResponseDTO<PageResult<GoodsVO>> result = goodsService.query(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getList()).isEmpty();
    }
  }

  // ==================== Helper Methods ====================

  private GoodsAddForm createTestAddForm(Long categoryId, String goodsName) {
    GoodsAddForm form = new GoodsAddForm();
    form.setCategoryId(categoryId);
    form.setGoodsName(goodsName);
    form.setGoodsStatus(GoodsStatusEnum.SELL.getValue());
    return form;
  }

  private GoodsUpdateForm createTestUpdateForm(Long goodsId, Long categoryId, String goodsName) {
    GoodsUpdateForm form = new GoodsUpdateForm();
    form.setGoodsId(goodsId);
    form.setCategoryId(categoryId);
    form.setGoodsName(goodsName);
    return form;
  }

  private GoodsEntity createTestGoodsEntity(Long id, String name) {
    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsId(id);
    entity.setGoodsName(name);
    entity.setDeletedFlag(false);
    entity.setGoodsStatus(GoodsStatusEnum.SELL.getValue());
    return entity;
  }

  private GoodsVO createTestGoodsVO(Long id, String name, Long categoryId) {
    GoodsVO vo = new GoodsVO();
    vo.setGoodsId(id);
    vo.setGoodsName(name);
    vo.setCategoryId(categoryId);
    return vo;
  }

  private CategoryEntity createTestCategory(Long id, Integer type) {
    CategoryEntity entity = new CategoryEntity();
    entity.setCategoryId(id);
    entity.setCategoryType(type);
    entity.setCategoryName("TestCategory");
    entity.setDeletedFlag(false);
    return entity;
  }
}
