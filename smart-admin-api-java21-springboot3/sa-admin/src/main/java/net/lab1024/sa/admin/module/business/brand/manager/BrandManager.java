package net.lab1024.sa.admin.module.business.brand.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.brand.dao.BrandDao;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Brand Manager Handles transactional operations and cache management
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Service
@RequiredArgsConstructor
public class BrandManager {

  private final BrandDao brandDao;

  /**
   * Save brand (transactional)
   *
   * @param entity Brand entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public void saveBrand(BrandEntity entity) {
    entity.setDeletedFlag(false);
    brandDao.insert(entity);
  }

  /**
   * Update brand (transactional)
   *
   * @param entity Brand entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateBrand(BrandEntity entity) {
    brandDao.updateById(entity);
  }

  /**
   * Batch delete brands (soft delete, transactional)
   *
   * @param brandIdList Brand ID list
   */
  @Transactional(rollbackFor = Throwable.class)
  public void batchDelete(java.util.List<Long> brandIdList) {
    BrandEntity updateEntity = new BrandEntity();
    updateEntity.setDeletedFlag(true);
    brandDao.update(
        updateEntity,
        Wrappers.<BrandEntity>lambdaUpdate().in(BrandEntity::getBrandId, brandIdList));
  }
}
