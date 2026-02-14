package net.lab1024.sa.business.goods.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.business.goods.dao.GoodsDao;
import net.lab1024.sa.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
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
 * GoodsManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有 @Transactional 方法
 *   <li>DataTracer 記錄驗證
 *   <li>軟刪除邏輯
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GoodsManager 單元測試")
class GoodsManagerTest {

  @Mock private GoodsDao goodsDao;

  @Mock private DataTracerService dataTracerService;

  @InjectMocks private GoodsManager goodsManager;

  @Captor private ArgumentCaptor<GoodsEntity> entityCaptor;

  // ==================== addGoodsTransaction 測試 ====================

  @Nested
  @DisplayName("addGoodsTransaction 新增商品事務測試")
  class AddGoodsTransactionTest {

    @Test
    @DisplayName("正常情況：應該設置 deletedFlag=false 並記錄變更追蹤")
    void shouldInsertAndTraceData() {
      // Given
      GoodsAddForm addForm = createTestAddForm("NewGoods");
      when(goodsDao.insert(any(GoodsEntity.class))).thenReturn(1);

      // When
      goodsManager.addGoodsTransaction(addForm);

      // Then
      verify(goodsDao).insert(entityCaptor.capture());
      GoodsEntity capturedEntity = entityCaptor.getValue();
      assertThat(capturedEntity.getDeletedFlag()).isFalse();
      assertThat(capturedEntity.getGoodsName()).isEqualTo("NewGoods");

      // goodsId is null in unit test (set by MyBatis Plus after insert)
      verify(dataTracerService).insert(any(), eq(DataTracerTypeEnum.GOODS));
    }
  }

  // ==================== updateGoodsTransaction 測試 ====================

  @Nested
  @DisplayName("updateGoodsTransaction 更新商品事務測試")
  class UpdateGoodsTransactionTest {

    @Test
    @DisplayName("正常情況：應該更新商品並記錄變更追蹤")
    void shouldUpdateAndTraceData() {
      // Given
      GoodsUpdateForm updateForm = createTestUpdateForm(1L, "UpdatedGoods");
      GoodsEntity originEntity = createTestGoodsEntity(1L, "OriginalGoods");
      when(goodsDao.updateById(any(GoodsEntity.class))).thenReturn(1);

      // When
      goodsManager.updateGoodsTransaction(updateForm, originEntity);

      // Then
      verify(goodsDao).updateById(entityCaptor.capture());
      GoodsEntity capturedEntity = entityCaptor.getValue();
      assertThat(capturedEntity.getGoodsName()).isEqualTo("UpdatedGoods");

      verify(dataTracerService)
          .update(eq(1L), eq(DataTracerTypeEnum.GOODS), eq(originEntity), any(GoodsEntity.class));
    }
  }

  // ==================== deleteGoodsTransaction 測試 ====================

  @Nested
  @DisplayName("deleteGoodsTransaction 刪除商品事務測試")
  class DeleteGoodsTransactionTest {

    @Test
    @DisplayName("正常情況：應該執行軟刪除並記錄變更追蹤")
    void shouldSoftDeleteAndTraceData() {
      // Given
      Long goodsId = 1L;
      // batchUpdateDeleted returns void - no stubbing needed

      // When
      goodsManager.deleteGoodsTransaction(goodsId);

      // Then
      verify(goodsDao).batchUpdateDeleted(Collections.singletonList(goodsId), Boolean.TRUE);
      verify(dataTracerService)
          .batchDelete(Collections.singletonList(goodsId), DataTracerTypeEnum.GOODS);
    }
  }

  // ==================== batchDeleteGoodsTransaction 測試 ====================

  @Nested
  @DisplayName("batchDeleteGoodsTransaction 批量刪除事務測試")
  class BatchDeleteGoodsTransactionTest {

    @Test
    @DisplayName("正常情況：應該批量軟刪除並記錄變更追蹤")
    void shouldBatchSoftDeleteAndTraceData() {
      // Given
      List<Long> goodsIdList = Arrays.asList(1L, 2L, 3L);
      // batchUpdateDeleted returns void - no stubbing needed

      // When
      goodsManager.batchDeleteGoodsTransaction(goodsIdList);

      // Then
      verify(goodsDao).batchUpdateDeleted(goodsIdList, Boolean.TRUE);
      verify(dataTracerService).batchDelete(goodsIdList, DataTracerTypeEnum.GOODS);
    }

    @Test
    @DisplayName("單個 ID：應該正確處理單個 ID")
    void shouldHandleSingleId() {
      // Given
      List<Long> goodsIdList = Collections.singletonList(1L);
      // batchUpdateDeleted returns void - no stubbing needed

      // When
      goodsManager.batchDeleteGoodsTransaction(goodsIdList);

      // Then
      verify(goodsDao).batchUpdateDeleted(goodsIdList, Boolean.TRUE);
      verify(dataTracerService).batchDelete(goodsIdList, DataTracerTypeEnum.GOODS);
    }
  }

  // ==================== Helper Methods ====================

  private GoodsAddForm createTestAddForm(String goodsName) {
    GoodsAddForm form = new GoodsAddForm();
    form.setGoodsName(goodsName);
    form.setCategoryId(1L);
    return form;
  }

  private GoodsUpdateForm createTestUpdateForm(Long goodsId, String goodsName) {
    GoodsUpdateForm form = new GoodsUpdateForm();
    form.setGoodsId(goodsId);
    form.setGoodsName(goodsName);
    return form;
  }

  private GoodsEntity createTestGoodsEntity(Long id, String name) {
    GoodsEntity entity = new GoodsEntity();
    entity.setGoodsId(id);
    entity.setGoodsName(name);
    entity.setDeletedFlag(false);
    return entity;
  }
}
