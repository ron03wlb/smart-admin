package net.lab1024.sa.admin.module.business.brand.service;

import static org.junit.jupiter.api.Assertions.*;

import io.vavr.control.Option;
import net.lab1024.sa.admin.BaseIntegrationTest;
import net.lab1024.sa.admin.module.business.brand.dao.BrandDao;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandAddForm;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandQueryForm;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandUpdateForm;
import net.lab1024.sa.admin.module.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * BrandService Integration Tests
 *
 * <p>Demonstrates BrandTestFixture usage in real integration tests
 *
 * @author Claude Code (test-fixture-generator skill)
 * @since 2026-01-25
 */
@SpringBootTest
@Transactional
@DisplayName("BrandService Integration Tests")
class BrandServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired private BrandService brandService;
  @Autowired private BrandDao brandDao;

  @Test
  @DisplayName("Should add brand and persist to database")
  void addBrand_ValidForm_PersistsToDatabase() {
    // Given - ONE-LINER fixture usage!
    BrandAddForm form = BrandTestFixture.createAddForm();

    // When
    ResponseDTO<String> response = brandService.addBrand(form);

    // Then
    assertTrue(response.getOk());

    // Verify DB persistence
    BrandEntity saved = brandDao.getByBrandName(form.getBrandName(), null);
    assertNotNull(saved);
    assertEquals(form.getBrandName(), saved.getBrandName());
    assertEquals(form.getBrandLogo(), saved.getBrandLogo());
    assertEquals(form.getDescription(), saved.getDescription());
    assertEquals(form.getSort(), saved.getSort());
    assertEquals(form.getStatus(), saved.getStatus());
    assertEquals(false, saved.getDeletedFlag());
  }

  @Test
  @DisplayName("Should reject duplicate brand name")
  void addBrand_DuplicateName_ReturnsError() {
    // Given - Add first brand
    BrandAddForm form1 = BrandTestFixture.createAddForm();
    form1.setBrandName("UniqueBrandForTest");
    brandService.addBrand(form1);

    // When - Try to add duplicate
    BrandAddForm form2 = BrandTestFixture.createAddForm();
    form2.setBrandName("UniqueBrandForTest"); // Same name

    // Then
    ResponseDTO<String> response = brandService.addBrand(form2);
    assertFalse(response.getOk());
    assertTrue(response.getMsg().contains("already exists"));
  }

  @Test
  @DisplayName("Should update brand and persist changes")
  void updateBrand_ValidForm_PersistsChanges() {
    // Given - Create initial brand
    BrandAddForm addForm = BrandTestFixture.createAddForm();
    brandService.addBrand(addForm);
    BrandEntity saved = brandDao.getByBrandName(addForm.getBrandName(), null);

    // Create update form with custom data
    BrandUpdateForm updateForm = BrandTestFixture.createUpdateForm(saved.getBrandId());
    updateForm.setDescription("New description after update");

    // When
    ResponseDTO<String> updateResponse = brandService.updateBrand(updateForm);

    // Then
    assertTrue(updateResponse.getOk());

    // Verify DB state
    BrandEntity updated = brandDao.selectById(saved.getBrandId());
    assertEquals(updateForm.getBrandName(), updated.getBrandName());
    assertEquals("New description after update", updated.getDescription());
  }

  @Test
  @DisplayName("Should return error when updating non-existent brand")
  void updateBrand_NonExistentId_ReturnsError() {
    // Given
    BrandUpdateForm updateForm = BrandTestFixture.createUpdateForm(99999L);

    // When
    ResponseDTO<String> response = brandService.updateBrand(updateForm);

    // Then
    assertFalse(response.getOk());
    assertTrue(response.getMsg().contains("does not exist"));
  }

  @Test
  @DisplayName("Should query brands with pagination")
  void queryBrand_DefaultPagination_ReturnsResults() {
    // Given - Create 3 brands using fixture
    brandService.addBrand(BrandTestFixture.createAddForm());
    brandService.addBrand(BrandTestFixture.createAddForm());
    brandService.addBrand(BrandTestFixture.createAddForm());

    // When
    BrandQueryForm queryForm = BrandTestFixture.createQueryForm();
    ResponseDTO<PageResult<BrandVO>> response = brandService.queryBrand(queryForm);

    // Then
    assertTrue(response.getOk());
    assertNotNull(response.getData());
    assertTrue(response.getData().getTotal() >= 3);
  }

  @Test
  @DisplayName("Should get brand by ID")
  void getById_ExistingBrand_ReturnsVO() {
    // Given - Create brand
    BrandAddForm form = BrandTestFixture.createAddForm();
    brandService.addBrand(form);
    BrandEntity saved = brandDao.getByBrandName(form.getBrandName(), null);

    // When
    Option<BrandVO> result = brandService.getById(saved.getBrandId());

    // Then
    assertTrue(result.isDefined(), "Expected brand to be found");
    BrandVO brandVO = result.get();
    assertNotNull(brandVO);
    assertEquals(saved.getBrandName(), brandVO.getBrandName());
  }

  @Test
  @DisplayName("Should return empty when getting non-existent brand")
  void getById_NonExistentId_ReturnsEmpty() {
    // When
    Option<BrandVO> result = brandService.getById(99999L);

    // Then
    assertTrue(result.isEmpty(), "Expected empty result for non-existent brand");
  }
}
