package net.lab1024.sa.admin.module.business.brand.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import java.util.List;
import net.lab1024.sa.admin.BaseUnitTest;
import net.lab1024.sa.admin.module.business.brand.dao.BrandDao;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

/**
 * BrandManager Unit Tests
 *
 * <p>Test Coverage: 3 @Transactional methods (save, update, batchDelete)
 *
 * <p>Focus Areas: 1. Transaction pattern without DataTracer 2. Soft delete with
 * Wrappers.lambdaUpdate 3. Insert with deletedFlag initialization
 *
 * @author Claude Code
 * @since 2026-01-30
 */
@DisplayName("BrandManager Unit Tests")
class BrandManagerTest extends BaseUnitTest {

  @Mock private BrandDao brandDao;

  private BrandManager brandManager;

  // Test constants
  private static final Long TEST_BRAND_ID = 1001L;
  private static final String TEST_BRAND_NAME = "測試品牌";
  private static final String TEST_BRAND_LOGO = "https://example.com/logo.png";

  @BeforeEach
  void setUp() {
    brandManager = new BrandManager(brandDao);
  }

  // ==================== saveBrandTransaction() Tests ====================

  @Nested
  @DisplayName("saveBrandTransaction() Tests - Insert with DeletedFlag Initialization")
  class SaveBrandTransactionTests {

    @Test
    @DisplayName("Should insert brand with deletedFlag set to false")
    void saveBrand_ValidEntity_InsertsWithDeletedFlagFalse() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandName(TEST_BRAND_NAME);
      entity.setBrandLogo(TEST_BRAND_LOGO);

      when(brandDao.insert(any(BrandEntity.class)))
          .thenAnswer(
              invocation -> {
                BrandEntity inserted = invocation.getArgument(0);
                assertEquals(Boolean.FALSE, inserted.getDeletedFlag());
                inserted.setBrandId(TEST_BRAND_ID); // Simulate auto-generated ID
                return 1;
              });

      // When
      brandManager.saveBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).insert(any(BrandEntity.class));
      assertEquals(Boolean.FALSE, entity.getDeletedFlag());
    }

    @Test
    @DisplayName("Should call insert once")
    void saveBrand_ValidEntity_CallsInsertOnce() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandName(TEST_BRAND_NAME);

      when(brandDao.insert(any(BrandEntity.class))).thenReturn(1);

      // When
      brandManager.saveBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).insert(entity);
    }

    @Test
    @DisplayName("Should set deletedFlag to false before insert")
    void saveBrand_DeletedFlag_SetBeforeInsert() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandName(TEST_BRAND_NAME);
      entity.setDeletedFlag(true); // Intentionally set to true to verify override

      when(brandDao.insert(any(BrandEntity.class)))
          .thenAnswer(
              invocation -> {
                BrandEntity inserted = invocation.getArgument(0);
                assertEquals(
                    Boolean.FALSE,
                    inserted.getDeletedFlag(),
                    "deletedFlag should be overridden to false");
                return 1;
              });

      // When
      brandManager.saveBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).insert(any(BrandEntity.class));
    }

    @Test
    @DisplayName("Should handle entity with full fields")
    void saveBrand_FullFields_InsertsCorrectly() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandName("Full Brand Name");
      entity.setBrandLogo("https://example.com/full-logo.png");
      entity.setSort(100);

      when(brandDao.insert(any(BrandEntity.class))).thenReturn(1);

      // When
      brandManager.saveBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).insert(entity);
      assertEquals(Boolean.FALSE, entity.getDeletedFlag());
      assertEquals("Full Brand Name", entity.getBrandName());
      assertEquals(100, entity.getSort());
    }
  }

  // ==================== updateBrandTransaction() Tests ====================

  @Nested
  @DisplayName("updateBrandTransaction() Tests - Simple Update Transaction")
  class UpdateBrandTransactionTests {

    @Test
    @DisplayName("Should update brand correctly")
    void updateBrand_ValidEntity_UpdatesCorrectly() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandId(TEST_BRAND_ID);
      entity.setBrandName("Updated Name");

      when(brandDao.updateById(entity)).thenReturn(1);

      // When
      brandManager.updateBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).updateById(entity);
    }

    @Test
    @DisplayName("Should call updateById once")
    void updateBrand_ValidEntity_CallsUpdateOnce() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandId(TEST_BRAND_ID);

      when(brandDao.updateById(entity)).thenReturn(1);

      // When
      brandManager.updateBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).updateById(entity);
      verify(brandDao, never()).insert(any(BrandEntity.class));
    }

    @Test
    @DisplayName("Should handle entity with updated fields")
    void updateBrand_UpdatedFields_UpdatesCorrectly() {
      // Given
      BrandEntity entity = new BrandEntity();
      entity.setBrandId(TEST_BRAND_ID);
      entity.setBrandName("New Brand Name");
      entity.setBrandLogo("https://example.com/new-logo.png");
      entity.setSort(200);

      when(brandDao.updateById(entity)).thenReturn(1);

      // When
      brandManager.updateBrandTransaction(entity);

      // Then
      verify(brandDao, times(1)).updateById(entity);
      assertEquals("New Brand Name", entity.getBrandName());
      assertEquals(200, entity.getSort());
    }

    @Test
    @DisplayName("Should handle multiple update transactions")
    void updateBrand_MultipleCalls_HandlesSequentially() {
      // Given
      BrandEntity entity1 = new BrandEntity();
      entity1.setBrandId(1001L);
      BrandEntity entity2 = new BrandEntity();
      entity2.setBrandId(1002L);

      when(brandDao.updateById(any(BrandEntity.class))).thenReturn(1);

      // When
      brandManager.updateBrandTransaction(entity1);
      brandManager.updateBrandTransaction(entity2);

      // Then
      verify(brandDao, times(2)).updateById(any(BrandEntity.class));
      verify(brandDao, times(1)).updateById(entity1);
      verify(brandDao, times(1)).updateById(entity2);
    }
  }

  // ==================== batchDeleteTransaction() Tests ====================

  @Nested
  @DisplayName("batchDeleteTransaction() Tests - Soft Delete with Wrappers.lambdaUpdate Pattern")
  class BatchDeleteTransactionTests {

    @Test
    @DisplayName("Should soft delete brands using lambdaUpdate")
    void batchDelete_ValidList_SoftDeletesWithLambdaUpdate() {
      // Given
      List<Long> brandIdList = Lists.newArrayList(1001L, 1002L, 1003L);
      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(3);

      // When
      brandManager.batchDeleteTransaction(brandIdList);

      // Then
      verify(brandDao, times(1)).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("Should use deletedFlag=true in update entity")
    void batchDelete_UpdateEntity_HasDeletedFlagTrue() {
      // Given
      List<Long> brandIdList = Lists.newArrayList(1001L);

      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class)))
          .thenAnswer(
              invocation -> {
                BrandEntity updateEntity = invocation.getArgument(0);
                assertEquals(
                    Boolean.TRUE,
                    updateEntity.getDeletedFlag(),
                    "deletedFlag should be true for soft delete");
                return 1;
              });

      // When
      brandManager.batchDeleteTransaction(brandIdList);

      // Then
      verify(brandDao, times(1)).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("Should handle single item in batch")
    void batchDelete_SingleItem_HandlesCorrectly() {
      // Given
      List<Long> singleList = Lists.newArrayList(TEST_BRAND_ID);
      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

      // When
      brandManager.batchDeleteTransaction(singleList);

      // Then
      verify(brandDao, times(1)).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
      assertEquals(1, singleList.size());
    }

    @Test
    @DisplayName("Should handle large batch delete")
    void batchDelete_LargeBatch_HandlesCorrectly() {
      // Given - 50 brand IDs
      List<Long> largeBatch = Lists.newArrayList();
      for (long i = 1; i <= 50; i++) {
        largeBatch.add(i);
      }
      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(50);

      // When
      brandManager.batchDeleteTransaction(largeBatch);

      // Then
      verify(brandDao, times(1)).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
      assertEquals(50, largeBatch.size());
    }

    @Test
    @DisplayName("Should handle multiple batch deletes for different brand lists")
    void batchDelete_DifferentLists_HandlesIndependently() {
      // Given
      List<Long> list1 = Lists.newArrayList(1001L, 1002L);
      List<Long> list2 = Lists.newArrayList(2001L, 2002L, 2003L);
      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class)))
          .thenReturn(2, 3);

      // When
      brandManager.batchDeleteTransaction(list1);
      brandManager.batchDeleteTransaction(list2);

      // Then
      verify(brandDao, times(2)).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("Should only update deletedFlag field")
    void batchDelete_UpdateEntity_OnlyHasDeletedFlag() {
      // Given
      List<Long> brandIdList = Lists.newArrayList(1001L);

      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class)))
          .thenAnswer(
              invocation -> {
                BrandEntity updateEntity = invocation.getArgument(0);
                assertEquals(Boolean.TRUE, updateEntity.getDeletedFlag());
                assertNull(
                    updateEntity.getBrandName(), "brandName should not be set in update entity");
                assertNull(
                    updateEntity.getBrandLogo(), "brandLogo should not be set in update entity");
                return 1;
              });

      // When
      brandManager.batchDeleteTransaction(brandIdList);

      // Then
      verify(brandDao, times(1)).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
    }
  }
}
