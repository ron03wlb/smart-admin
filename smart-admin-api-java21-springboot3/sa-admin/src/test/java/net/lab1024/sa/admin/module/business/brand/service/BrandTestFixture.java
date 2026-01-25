package net.lab1024.sa.admin.module.business.brand.service;

import java.util.concurrent.atomic.AtomicInteger;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandAddForm;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandQueryForm;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandUpdateForm;

/**
 * Test fixtures for Brand integration tests
 *
 * <p>Provides reusable test data builders for Brand entities, forms, and VOs.
 *
 * <p><b>Usage:</b>
 *
 * <pre>{@code
 * // Create entity with defaults
 * BrandEntity entity = BrandTestFixture.createEntity();
 *
 * // Create add form
 * BrandAddForm form = BrandTestFixture.createAddForm();
 *
 * // Create update form
 * BrandUpdateForm updateForm = BrandTestFixture.createUpdateForm(brandId);
 * }</pre>
 *
 * @author Claude Code (test-fixture-generator skill)
 * @since 2026-01-25
 */
public class BrandTestFixture {

  private static final AtomicInteger counter = new AtomicInteger(0);

  /**
   * Create BrandEntity with unique test data
   *
   * @return Entity with all required fields set (unpersisted - caller must insert via Dao)
   */
  public static BrandEntity createEntity() {
    int id = counter.incrementAndGet();

    BrandEntity entity = new BrandEntity();
    entity.setBrandName("Brand-" + id);
    entity.setBrandLogo("https://example.com/logo-" + id + ".png");
    entity.setDescription("Test brand description " + id);
    entity.setSort(id);
    entity.setStatus(1); // 1=Enabled
    entity.setDeletedFlag(false);

    // Auto-fields: brandId, createTime, updateTime handled by DB/MyBatis
    return entity;
  }

  /**
   * Create BrandAddForm with default test data
   *
   * @return Form ready for service.addBrand()
   */
  public static BrandAddForm createAddForm() {
    int id = counter.incrementAndGet();

    BrandAddForm form = new BrandAddForm();
    form.setBrandName("Brand-" + id);
    form.setBrandLogo("https://example.com/logo-" + id + ".png");
    form.setDescription("Test brand description " + id);
    form.setSort(id);
    form.setStatus(1);

    return form;
  }

  /**
   * Create BrandUpdateForm for updating existing brand
   *
   * @param brandId ID of brand to update (required)
   * @return Form ready for service.updateBrand()
   */
  public static BrandUpdateForm createUpdateForm(Long brandId) {
    int id = counter.incrementAndGet();

    BrandUpdateForm form = new BrandUpdateForm();
    form.setBrandId(brandId);
    form.setBrandName("Updated Brand-" + id);
    form.setBrandLogo("https://example.com/updated-logo-" + id + ".png");
    form.setDescription("Updated brand description " + id);
    form.setSort(id);
    form.setStatus(1);

    return form;
  }

  /**
   * Create BrandQueryForm with pagination defaults
   *
   * @return Form ready for service.queryBrand()
   */
  public static BrandQueryForm createQueryForm() {
    BrandQueryForm form = new BrandQueryForm();
    form.setPageNum(1L);
    form.setPageSize(10L);
    return form;
  }

  /** Reset counter (useful in @BeforeEach for test isolation) */
  public static void resetCounter() {
    counter.set(0);
  }
}
