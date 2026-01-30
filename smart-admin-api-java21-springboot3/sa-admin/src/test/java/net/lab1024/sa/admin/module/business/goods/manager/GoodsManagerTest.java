package net.lab1024.sa.admin.module.business.goods.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.goods.dao.GoodsDao;
import net.lab1024.sa.admin.module.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * GoodsManager Unit Tests
 *
 * <p>Test Coverage: 4 @Transactional methods (add, update, delete, batchDelete)
 *
 * <p>Focus Areas: 1. Transaction pattern with DataTracer integration 2. Insert + DataTracer.insert
 * ordering 3. Update + DataTracer.update with origin entity 4. Delete (soft delete) +
 * DataTracer.batchDelete 5. Batch operations
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("GoodsManager Unit Tests")
class GoodsManagerTest extends BaseUnitTest {

  @Mock private GoodsDao goodsDao;

  @Mock private DataTracerService dataTracerService;

  private GoodsManager goodsManager;

  // Test constants
  private static final Long TEST_GOODS_ID = 1001L;
  private static final String TEST_GOODS_NAME = "測試商品";
  private static final Long TEST_CATEGORY_ID = 2001L;

  @BeforeEach
  void setUp() {
    goodsManager = new GoodsManager(goodsDao, dataTracerService);
  }

  // ==================== addGoodsTransaction() Tests ====================

  @Nested
  @DisplayName("addGoodsTransaction() Tests - Insert + DataTracer Pattern")
  class AddGoodsTransactionTests {

    @Test
    @DisplayName("Should insert goods and record data tracer")
    void addGoods_ValidForm_InsertsAndTraces() {
      // Given
      GoodsAddForm addForm = new GoodsAddForm();
      addForm.setGoodsName(TEST_GOODS_NAME);
      addForm.setCategoryId(TEST_CATEGORY_ID);

      when(goodsDao.insert(any(GoodsEntity.class)))
          .thenAnswer(
              invocation -> {
                GoodsEntity entity = invocation.getArgument(0);
                entity.setGoodsId(TEST_GOODS_ID); // Simulate auto-generated ID
                return 1;
              });
      doNothing().when(dataTracerService).insert(TEST_GOODS_ID, DataTracerTypeEnum.GOODS);

      // When
      goodsManager.addGoodsTransaction(addForm);

      // Then
      verify(goodsDao, times(1)).insert(any(GoodsEntity.class));
      verify(dataTracerService, times(1)).insert(TEST_GOODS_ID, DataTracerTypeEnum.GOODS);
    }

    @Test
    @DisplayName("Should insert before data tracer (transaction order)")
    void addGoods_TransactionOrder_InsertBeforeTrace() {
      // Given
      GoodsAddForm addForm = new GoodsAddForm();
      addForm.setGoodsName(TEST_GOODS_NAME);

      when(goodsDao.insert(any(GoodsEntity.class)))
          .thenAnswer(
              invocation -> {
                GoodsEntity entity = invocation.getArgument(0);
                entity.setGoodsId(TEST_GOODS_ID);
                return 1;
              });
      doNothing().when(dataTracerService).insert(TEST_GOODS_ID, DataTracerTypeEnum.GOODS);

      // When
      goodsManager.addGoodsTransaction(addForm);

      // Then - verify insert called before dataTracer
      var inOrder = inOrder(goodsDao, dataTracerService);
      inOrder.verify(goodsDao).insert(any(GoodsEntity.class));
      inOrder.verify(dataTracerService).insert(TEST_GOODS_ID, DataTracerTypeEnum.GOODS);
    }

    @Test
    @DisplayName("Should set deletedFlag to false")
    void addGoods_DeletedFlag_SetToFalse() {
      // Given
      GoodsAddForm addForm = new GoodsAddForm();
      addForm.setGoodsName(TEST_GOODS_NAME);

      when(goodsDao.insert(any(GoodsEntity.class)))
          .thenAnswer(
              invocation -> {
                GoodsEntity entity = invocation.getArgument(0);
                assertEquals(Boolean.FALSE, entity.getDeletedFlag());
                entity.setGoodsId(TEST_GOODS_ID);
                return 1;
              });
      doNothing().when(dataTracerService).insert(anyLong(), any(DataTracerTypeEnum.class));

      // When
      goodsManager.addGoodsTransaction(addForm);

      // Then - verify deletedFlag was set
      verify(goodsDao, times(1)).insert(any(GoodsEntity.class));
    }
  }

  // ==================== updateGoodsTransaction() Tests ====================

  @Nested
  @DisplayName("updateGoodsTransaction() Tests - Update + DataTracer with Origin Entity")
  class UpdateGoodsTransactionTests {

    @Test
    @DisplayName("Should update goods and record data tracer with origin entity")
    void updateGoods_ValidForm_UpdatesAndTraces() {
      // Given
      GoodsUpdateForm updateForm = new GoodsUpdateForm();
      updateForm.setGoodsId(TEST_GOODS_ID);
      updateForm.setGoodsName("Updated Name");

      GoodsEntity originEntity = new GoodsEntity();
      originEntity.setGoodsId(TEST_GOODS_ID);
      originEntity.setGoodsName(TEST_GOODS_NAME);

      when(goodsDao.updateById(any(GoodsEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .update(eq(TEST_GOODS_ID), eq(DataTracerTypeEnum.GOODS), eq(originEntity), any());

      // When
      goodsManager.updateGoodsTransaction(updateForm, originEntity);

      // Then
      verify(goodsDao, times(1)).updateById(any(GoodsEntity.class));
      verify(dataTracerService, times(1))
          .update(eq(TEST_GOODS_ID), eq(DataTracerTypeEnum.GOODS), eq(originEntity), any());
    }

    @Test
    @DisplayName("Should update before data tracer (transaction order)")
    void updateGoods_TransactionOrder_UpdateBeforeTrace() {
      // Given
      GoodsUpdateForm updateForm = new GoodsUpdateForm();
      updateForm.setGoodsId(TEST_GOODS_ID);

      GoodsEntity originEntity = new GoodsEntity();
      originEntity.setGoodsId(TEST_GOODS_ID);

      when(goodsDao.updateById(any(GoodsEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .update(anyLong(), any(DataTracerTypeEnum.class), any(), any());

      // When
      goodsManager.updateGoodsTransaction(updateForm, originEntity);

      // Then - verify update called before dataTracer
      var inOrder = inOrder(goodsDao, dataTracerService);
      inOrder.verify(goodsDao).updateById(any(GoodsEntity.class));
      inOrder
          .verify(dataTracerService)
          .update(anyLong(), any(DataTracerTypeEnum.class), any(), any());
    }

    @Test
    @DisplayName("Should pass origin entity to data tracer")
    void updateGoods_OriginEntity_PassedToTracer() {
      // Given
      GoodsUpdateForm updateForm = new GoodsUpdateForm();
      updateForm.setGoodsId(TEST_GOODS_ID);

      GoodsEntity originEntity = new GoodsEntity();
      originEntity.setGoodsId(TEST_GOODS_ID);
      originEntity.setGoodsName("Original Name");

      when(goodsDao.updateById(any(GoodsEntity.class))).thenReturn(1);
      doNothing()
          .when(dataTracerService)
          .update(anyLong(), any(DataTracerTypeEnum.class), any(), any());

      // When
      goodsManager.updateGoodsTransaction(updateForm, originEntity);

      // Then - verify originEntity passed to dataTracer
      verify(dataTracerService, times(1))
          .update(eq(TEST_GOODS_ID), eq(DataTracerTypeEnum.GOODS), eq(originEntity), any());
    }
  }

  // ==================== deleteGoodsTransaction() Tests ====================

  @Nested
  @DisplayName("deleteGoodsTransaction() Tests - Soft Delete + DataTracer")
  class DeleteGoodsTransactionTests {

    @Test
    @DisplayName("Should soft delete goods and record data tracer")
    void deleteGoods_ValidId_SoftDeletesAndTraces() {
      // Given
      doNothing()
          .when(goodsDao)
          .batchUpdateDeleted(eq(Collections.singletonList(TEST_GOODS_ID)), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .batchDelete(eq(Collections.singletonList(TEST_GOODS_ID)), eq(DataTracerTypeEnum.GOODS));

      // When
      goodsManager.deleteGoodsTransaction(TEST_GOODS_ID);

      // Then
      verify(goodsDao, times(1))
          .batchUpdateDeleted(eq(Collections.singletonList(TEST_GOODS_ID)), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .batchDelete(eq(Collections.singletonList(TEST_GOODS_ID)), eq(DataTracerTypeEnum.GOODS));
    }

    @Test
    @DisplayName("Should soft delete before data tracer (transaction order)")
    void deleteGoods_TransactionOrder_DeleteBeforeTrace() {
      // Given
      doNothing().when(goodsDao).batchUpdateDeleted(anyList(), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).batchDelete(anyList(), any(DataTracerTypeEnum.class));

      // When
      goodsManager.deleteGoodsTransaction(TEST_GOODS_ID);

      // Then - verify delete called before dataTracer
      var inOrder = inOrder(goodsDao, dataTracerService);
      inOrder.verify(goodsDao).batchUpdateDeleted(anyList(), eq(Boolean.TRUE));
      inOrder.verify(dataTracerService).batchDelete(anyList(), any(DataTracerTypeEnum.class));
    }

    @Test
    @DisplayName("Should convert single ID to singleton list")
    void deleteGoods_SingleId_ConvertedToList() {
      // Given
      Long specificId = 9999L;
      doNothing().when(goodsDao).batchUpdateDeleted(anyList(), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).batchDelete(anyList(), any(DataTracerTypeEnum.class));

      // When
      goodsManager.deleteGoodsTransaction(specificId);

      // Then - verify singleton list used
      verify(goodsDao, times(1))
          .batchUpdateDeleted(eq(Collections.singletonList(specificId)), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .batchDelete(eq(Collections.singletonList(specificId)), eq(DataTracerTypeEnum.GOODS));
    }
  }

  // ==================== batchDeleteGoodsTransaction() Tests ====================

  @Nested
  @DisplayName("batchDeleteGoodsTransaction() Tests - Batch Soft Delete + DataTracer")
  class BatchDeleteGoodsTransactionTests {

    @Test
    @DisplayName("Should batch soft delete goods and record data tracer")
    void batchDeleteGoods_ValidList_DeletesAndTraces() {
      // Given
      List<Long> goodsIdList = Lists.newArrayList(1001L, 1002L, 1003L);
      doNothing().when(goodsDao).batchUpdateDeleted(eq(goodsIdList), eq(Boolean.TRUE));
      doNothing()
          .when(dataTracerService)
          .batchDelete(eq(goodsIdList), eq(DataTracerTypeEnum.GOODS));

      // When
      goodsManager.batchDeleteGoodsTransaction(goodsIdList);

      // Then
      verify(goodsDao, times(1)).batchUpdateDeleted(eq(goodsIdList), eq(Boolean.TRUE));
      verify(dataTracerService, times(1))
          .batchDelete(eq(goodsIdList), eq(DataTracerTypeEnum.GOODS));
    }

    @Test
    @DisplayName("Should delete before data tracer (transaction order)")
    void batchDeleteGoods_TransactionOrder_DeleteBeforeTrace() {
      // Given
      List<Long> goodsIdList = Lists.newArrayList(1001L, 1002L);
      doNothing().when(goodsDao).batchUpdateDeleted(anyList(), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).batchDelete(anyList(), any(DataTracerTypeEnum.class));

      // When
      goodsManager.batchDeleteGoodsTransaction(goodsIdList);

      // Then - verify delete called before dataTracer
      var inOrder = inOrder(goodsDao, dataTracerService);
      inOrder.verify(goodsDao).batchUpdateDeleted(anyList(), eq(Boolean.TRUE));
      inOrder.verify(dataTracerService).batchDelete(anyList(), any(DataTracerTypeEnum.class));
    }

    @Test
    @DisplayName("Should handle large batch delete")
    void batchDeleteGoods_LargeBatch_HandlesCorrectly() {
      // Given - 50 goods IDs
      List<Long> largeBatch = Lists.newArrayList();
      for (long i = 1; i <= 50; i++) {
        largeBatch.add(i);
      }
      doNothing().when(goodsDao).batchUpdateDeleted(eq(largeBatch), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).batchDelete(eq(largeBatch), eq(DataTracerTypeEnum.GOODS));

      // When
      goodsManager.batchDeleteGoodsTransaction(largeBatch);

      // Then
      verify(goodsDao, times(1)).batchUpdateDeleted(eq(largeBatch), eq(Boolean.TRUE));
      verify(dataTracerService, times(1)).batchDelete(eq(largeBatch), eq(DataTracerTypeEnum.GOODS));
      assertEquals(50, largeBatch.size());
    }

    @Test
    @DisplayName("Should handle single item in batch delete")
    void batchDeleteGoods_SingleItem_HandlesCorrectly() {
      // Given
      List<Long> singleList = Lists.newArrayList(TEST_GOODS_ID);
      doNothing().when(goodsDao).batchUpdateDeleted(eq(singleList), eq(Boolean.TRUE));
      doNothing().when(dataTracerService).batchDelete(eq(singleList), eq(DataTracerTypeEnum.GOODS));

      // When
      goodsManager.batchDeleteGoodsTransaction(singleList);

      // Then
      verify(goodsDao, times(1)).batchUpdateDeleted(eq(singleList), eq(Boolean.TRUE));
      verify(dataTracerService, times(1)).batchDelete(eq(singleList), eq(DataTracerTypeEnum.GOODS));
      assertEquals(1, singleList.size());
    }
  }
}
