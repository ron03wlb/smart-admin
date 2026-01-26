package net.lab1024.sa.admin.module.business.brand.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.brand.dao.BrandDao;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandAddForm;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandQueryForm;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandUpdateForm;
import net.lab1024.sa.admin.module.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.admin.module.business.brand.manager.BrandManager;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * Brand Service Business logic layer
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Service
@RequiredArgsConstructor
public class BrandService {

  private final BrandDao brandDao;
  private final BrandManager brandManager;

  /**
   * Query brands with pagination
   *
   * @param queryForm Query parameters
   * @return Paginated brand list
   */
  public ResponseDTO<PageResult<BrandVO>> queryBrand(BrandQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<BrandVO> list = brandDao.queryBrand(page, queryForm);
    PageResult<BrandVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * Add brand
   *
   * @param addForm Add form
   * @return Success message
   */
  public ResponseDTO<String> addBrand(BrandAddForm addForm) {
    // Validate unique brand name
    BrandEntity existing = brandDao.getByBrandName(addForm.getBrandName(), null);
    if (existing != null) {
      return ResponseDTO.userErrorParam("Brand name already exists");
    }

    BrandEntity entity = SmartBeanUtil.copy(addForm, BrandEntity.class);
    brandManager.saveBrandTransaction(entity);
    return ResponseDTO.ok();
  }

  /**
   * Update brand
   *
   * @param updateForm Update form
   * @return Success message
   */
  public ResponseDTO<String> updateBrand(BrandUpdateForm updateForm) {
    // Validate brand exists
    BrandEntity brand = brandDao.selectById(updateForm.getBrandId());
    if (brand == null || brand.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("Brand does not exist");
    }

    // Validate unique brand name
    BrandEntity existing =
        brandDao.getByBrandName(updateForm.getBrandName(), updateForm.getBrandId());
    if (existing != null) {
      return ResponseDTO.userErrorParam("Brand name already exists");
    }

    BrandEntity entity = SmartBeanUtil.copy(updateForm, BrandEntity.class);
    brandManager.updateBrandTransaction(entity);
    return ResponseDTO.ok();
  }

  /**
   * Batch delete brands
   *
   * @param brandIdList Brand ID list
   * @return Success message
   */
  public ResponseDTO<String> batchDelete(List<Long> brandIdList) {
    if (brandIdList == null || brandIdList.isEmpty()) {
      return ResponseDTO.userErrorParam("Brand ID list cannot be empty");
    }

    brandManager.batchDeleteTransaction(brandIdList);
    return ResponseDTO.ok();
  }

  /**
   * Get brand by ID
   *
   * @param brandId Brand ID
   * @return Option<BrandVO> - Some(brand) if exists and not deleted, None otherwise
   */
  public Option<BrandVO> getById(Long brandId) {
    return Option.of(brandDao.selectById(brandId))
        .filter(entity -> !entity.getDeletedFlag())
        .map(entity -> SmartBeanUtil.copy(entity, BrandVO.class));
  }
}
