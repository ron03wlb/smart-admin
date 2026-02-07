package net.lab1024.sa.business.brand.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.business.brand.dao.BrandDao;
import net.lab1024.sa.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.business.brand.domain.form.BrandAddForm;
import net.lab1024.sa.business.brand.domain.form.BrandQueryForm;
import net.lab1024.sa.business.brand.domain.form.BrandUpdateForm;
import net.lab1024.sa.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.business.brand.manager.BrandManager;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BrandService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有公開方法的正常流程 (Happy Path)
 *   <li>參數驗證和邊界條件
 *   <li>錯誤處理和異常情況
 *   <li>Vavr Option 正確使用
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BrandService 單元測試")
class BrandServiceTest {

  @Mock private BrandDao brandDao;

  @Mock private BrandManager brandManager;

  @InjectMocks private BrandService brandService;

  // ==================== queryBrand 測試 ====================

  @Nested
  @DisplayName("queryBrand 分頁查詢測試")
  class QueryBrandTest {

    @Test
    @DisplayName("正常情況：應該成功返回分頁結果")
    void shouldReturnPageResultWhenQuerySuccess() {
      // Given
      BrandQueryForm queryForm = new BrandQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);
      queryForm.setKeyword("Apple");

      BrandVO vo1 = createTestBrandVO(1L, "Apple");
      BrandVO vo2 = createTestBrandVO(2L, "Apple Watch");
      List<BrandVO> voList = Arrays.asList(vo1, vo2);

      when(brandDao.queryBrand(any(Page.class), eq(queryForm))).thenReturn(voList);

      // When
      ResponseDTO<PageResult<BrandVO>> result = brandService.queryBrand(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getList()).hasSize(2);
      verify(brandDao).queryBrand(any(Page.class), eq(queryForm));
    }

    @Test
    @DisplayName("空結果：應該返回空列表")
    void shouldReturnEmptyListWhenNoData() {
      // Given
      BrandQueryForm queryForm = new BrandQueryForm();
      queryForm.setPageNum(1L);
      queryForm.setPageSize(10L);

      when(brandDao.queryBrand(any(Page.class), eq(queryForm))).thenReturn(Collections.emptyList());

      // When
      ResponseDTO<PageResult<BrandVO>> result = brandService.queryBrand(queryForm);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
      assertThat(result.getData().getList()).isEmpty();
    }
  }

  // ==================== addBrand 測試 ====================

  @Nested
  @DisplayName("addBrand 新增品牌測試")
  class AddBrandTest {

    @Test
    @DisplayName("正常情況：品牌名稱唯一時應該成功新增")
    void shouldAddSuccessWhenBrandNameUnique() {
      // Given
      BrandAddForm addForm = createTestAddForm("NewBrand");
      when(brandDao.getByBrandName("NewBrand", null)).thenReturn(null);

      // When
      ResponseDTO<String> result = brandService.addBrand(addForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(brandDao).getByBrandName("NewBrand", null);
      verify(brandManager).saveBrandTransaction(any(BrandEntity.class));
    }

    @Test
    @DisplayName("異常情況：品牌名稱已存在時應該返回錯誤")
    void shouldReturnErrorWhenBrandNameExists() {
      // Given
      BrandAddForm addForm = createTestAddForm("ExistingBrand");
      BrandEntity existingEntity = createTestEntity(1L, "ExistingBrand");
      when(brandDao.getByBrandName("ExistingBrand", null)).thenReturn(existingEntity);

      // When
      ResponseDTO<String> result = brandService.addBrand(addForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("already exists");
      verify(brandManager, never()).saveBrandTransaction(any(BrandEntity.class));
    }
  }

  // ==================== updateBrand 測試 ====================

  @Nested
  @DisplayName("updateBrand 更新品牌測試")
  class UpdateBrandTest {

    @Test
    @DisplayName("正常情況：品牌存在且名稱唯一時應該成功更新")
    void shouldUpdateSuccessWhenValidInput() {
      // Given
      BrandUpdateForm updateForm = createTestUpdateForm(1L, "UpdatedBrand");
      BrandEntity existingEntity = createTestEntity(1L, "OldBrand");
      existingEntity.setDeletedFlag(false);

      when(brandDao.selectById(1L)).thenReturn(existingEntity);
      when(brandDao.getByBrandName("UpdatedBrand", 1L)).thenReturn(null);

      // When
      ResponseDTO<String> result = brandService.updateBrand(updateForm);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(brandDao).selectById(1L);
      verify(brandDao).getByBrandName("UpdatedBrand", 1L);
      verify(brandManager).updateBrandTransaction(any(BrandEntity.class));
    }

    @Test
    @DisplayName("異常情況：品牌不存在時應該返回錯誤")
    void shouldReturnErrorWhenBrandNotFound() {
      // Given
      BrandUpdateForm updateForm = createTestUpdateForm(999L, "AnyBrand");
      when(brandDao.selectById(999L)).thenReturn(null);

      // When
      ResponseDTO<String> result = brandService.updateBrand(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("does not exist");
      verify(brandManager, never()).updateBrandTransaction(any(BrandEntity.class));
    }

    @Test
    @DisplayName("異常情況：品牌已刪除時應該返回錯誤")
    void shouldReturnErrorWhenBrandDeleted() {
      // Given
      BrandUpdateForm updateForm = createTestUpdateForm(1L, "AnyBrand");
      BrandEntity deletedEntity = createTestEntity(1L, "DeletedBrand");
      deletedEntity.setDeletedFlag(true);

      when(brandDao.selectById(1L)).thenReturn(deletedEntity);

      // When
      ResponseDTO<String> result = brandService.updateBrand(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("does not exist");
      verify(brandManager, never()).updateBrandTransaction(any(BrandEntity.class));
    }

    @Test
    @DisplayName("異常情況：品牌名稱與其他品牌重複時應該返回錯誤")
    void shouldReturnErrorWhenBrandNameDuplicate() {
      // Given
      BrandUpdateForm updateForm = createTestUpdateForm(1L, "DuplicateName");
      BrandEntity existingEntity = createTestEntity(1L, "OldBrand");
      existingEntity.setDeletedFlag(false);
      BrandEntity anotherEntity = createTestEntity(2L, "DuplicateName");

      when(brandDao.selectById(1L)).thenReturn(existingEntity);
      when(brandDao.getByBrandName("DuplicateName", 1L)).thenReturn(anotherEntity);

      // When
      ResponseDTO<String> result = brandService.updateBrand(updateForm);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("already exists");
      verify(brandManager, never()).updateBrandTransaction(any(BrandEntity.class));
    }
  }

  // ==================== batchDelete 測試 ====================

  @Nested
  @DisplayName("batchDelete 批量刪除測試")
  class BatchDeleteTest {

    @Test
    @DisplayName("正常情況：應該成功批量刪除")
    void shouldDeleteSuccessWhenValidInput() {
      // Given
      List<Long> brandIdList = Arrays.asList(1L, 2L, 3L);

      // When
      ResponseDTO<String> result = brandService.batchDelete(brandIdList);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(brandManager).batchDeleteTransaction(brandIdList);
    }

    @Test
    @DisplayName("異常情況：ID 列表為空時應該返回錯誤")
    void shouldReturnErrorWhenIdListEmpty() {
      // Given
      List<Long> brandIdList = Collections.emptyList();

      // When
      ResponseDTO<String> result = brandService.batchDelete(brandIdList);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("cannot be empty");
      verify(brandManager, never()).batchDeleteTransaction(any());
    }

    @Test
    @DisplayName("異常情況：ID 列表為 null 時應該返回錯誤")
    void shouldReturnErrorWhenIdListNull() {
      // When
      ResponseDTO<String> result = brandService.batchDelete(null);

      // Then
      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).contains("cannot be empty");
      verify(brandManager, never()).batchDeleteTransaction(any());
    }
  }

  // ==================== getById 測試 ====================

  @Nested
  @DisplayName("getById Vavr Option 測試")
  class GetByIdTest {

    @Test
    @DisplayName("正常情況：品牌存在且未刪除時應該返回 Some")
    void shouldReturnSomeWhenBrandExistsAndNotDeleted() {
      // Given
      Long brandId = 1L;
      BrandEntity entity = createTestEntity(brandId, "Apple");
      entity.setDeletedFlag(false);

      when(brandDao.selectById(brandId)).thenReturn(entity);

      // When
      Option<BrandVO> result = brandService.getById(brandId);

      // Then
      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getBrandId()).isEqualTo(brandId);
      assertThat(result.get().getBrandName()).isEqualTo("Apple");
      verify(brandDao).selectById(brandId);
    }

    @Test
    @DisplayName("異常情況：品牌不存在時應該返回 None")
    void shouldReturnNoneWhenBrandNotFound() {
      // Given
      Long brandId = 999L;
      when(brandDao.selectById(brandId)).thenReturn(null);

      // When
      Option<BrandVO> result = brandService.getById(brandId);

      // Then
      assertThat(result.isEmpty()).isTrue();
      verify(brandDao).selectById(brandId);
    }

    @Test
    @DisplayName("異常情況：品牌已刪除時應該返回 None")
    void shouldReturnNoneWhenBrandDeleted() {
      // Given
      Long brandId = 1L;
      BrandEntity entity = createTestEntity(brandId, "DeletedBrand");
      entity.setDeletedFlag(true);

      when(brandDao.selectById(brandId)).thenReturn(entity);

      // When
      Option<BrandVO> result = brandService.getById(brandId);

      // Then
      assertThat(result.isEmpty()).isTrue();
      verify(brandDao).selectById(brandId);
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

  private BrandVO createTestBrandVO(Long id, String name) {
    BrandVO vo = new BrandVO();
    vo.setBrandId(id);
    vo.setBrandName(name);
    return vo;
  }

  private BrandAddForm createTestAddForm(String name) {
    BrandAddForm form = new BrandAddForm();
    form.setBrandName(name);
    form.setSort(1);
    form.setStatus(1);
    return form;
  }

  private BrandUpdateForm createTestUpdateForm(Long id, String name) {
    BrandUpdateForm form = new BrandUpdateForm();
    form.setBrandId(id);
    form.setBrandName(name);
    form.setSort(1);
    form.setStatus(1);
    return form;
  }
}
