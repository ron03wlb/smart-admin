package net.lab1024.sa.business.brand.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Arrays;
import java.util.List;
import net.lab1024.sa.business.brand.dao.BrandDao;
import net.lab1024.sa.business.brand.domain.entity.BrandEntity;
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
 * BrandManager 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有 @Transactional 方法的正常流程
 *   <li>驗證 DAO 調用順序和參數
 *   <li>軟刪除邏輯驗證
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandManager 單元測試")
class BrandManagerTest {

  @Mock private BrandDao brandDao;

  @InjectMocks private BrandManager brandManager;

  @Captor private ArgumentCaptor<BrandEntity> entityCaptor;

  // ==================== saveBrandTransaction 測試 ====================

  @Nested
  @DisplayName("saveBrandTransaction 保存品牌事務測試")
  class SaveBrandTransactionTest {

    @Test
    @DisplayName("正常情況：應該設置 deletedFlag 為 false 並插入")
    void shouldSetDeletedFlagFalseAndInsert() {
      // Given
      BrandEntity entity = createTestEntity(null, "NewBrand");
      when(brandDao.insert(any(BrandEntity.class))).thenReturn(1);

      // When
      brandManager.saveBrandTransaction(entity);

      // Then
      verify(brandDao).insert(entityCaptor.capture());
      BrandEntity capturedEntity = entityCaptor.getValue();
      assertThat(capturedEntity.getDeletedFlag()).isFalse();
      assertThat(capturedEntity.getBrandName()).isEqualTo("NewBrand");
    }

    @Test
    @DisplayName("邊界情況：即使傳入 deletedFlag=true 也應該設置為 false")
    void shouldOverrideDeletedFlagToFalse() {
      // Given
      BrandEntity entity = createTestEntity(null, "TestBrand");
      entity.setDeletedFlag(true); // Intentionally set to true

      when(brandDao.insert(any(BrandEntity.class))).thenReturn(1);

      // When
      brandManager.saveBrandTransaction(entity);

      // Then
      verify(brandDao).insert(entityCaptor.capture());
      assertThat(entityCaptor.getValue().getDeletedFlag()).isFalse();
    }
  }

  // ==================== updateBrandTransaction 測試 ====================

  @Nested
  @DisplayName("updateBrandTransaction 更新品牌事務測試")
  class UpdateBrandTransactionTest {

    @Test
    @DisplayName("正常情況：應該調用 updateById")
    void shouldCallUpdateById() {
      // Given
      BrandEntity entity = createTestEntity(1L, "UpdatedBrand");
      when(brandDao.updateById(any(BrandEntity.class))).thenReturn(1);

      // When
      brandManager.updateBrandTransaction(entity);

      // Then
      verify(brandDao).updateById(entityCaptor.capture());
      BrandEntity capturedEntity = entityCaptor.getValue();
      assertThat(capturedEntity.getBrandId()).isEqualTo(1L);
      assertThat(capturedEntity.getBrandName()).isEqualTo("UpdatedBrand");
    }
  }

  // ==================== batchDeleteTransaction 測試 ====================

  @Nested
  @DisplayName("batchDeleteTransaction 批量刪除事務測試")
  class BatchDeleteTransactionTest {

    @Test
    @DisplayName("正常情況：應該執行軟刪除（設置 deletedFlag=true）")
    void shouldSoftDeleteBySettingDeletedFlagTrue() {
      // Given
      List<Long> brandIdList = Arrays.asList(1L, 2L, 3L);
      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(3);

      // When
      brandManager.batchDeleteTransaction(brandIdList);

      // Then
      verify(brandDao)
          .update(
              argThat(
                  entity -> {
                    assertThat(entity.getDeletedFlag()).isTrue();
                    return true;
                  }),
              any(LambdaUpdateWrapper.class));
    }

    @Test
    @DisplayName("單個 ID：應該正確處理單個 ID 的刪除")
    void shouldHandleSingleIdDelete() {
      // Given
      List<Long> brandIdList = Arrays.asList(1L);
      when(brandDao.update(any(BrandEntity.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

      // When
      brandManager.batchDeleteTransaction(brandIdList);

      // Then
      verify(brandDao).update(any(BrandEntity.class), any(LambdaUpdateWrapper.class));
    }
  }

  // ==================== Helper Methods ====================

  private BrandEntity createTestEntity(Long id, String name) {
    BrandEntity entity = new BrandEntity();
    entity.setBrandId(id);
    entity.setBrandName(name);
    entity.setDeletedFlag(false);
    entity.setSort(1);
    entity.setStatus(1);
    return entity;
  }
}
