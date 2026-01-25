package net.lab1024.sa.admin.module.business.brand.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.admin.module.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.admin.module.business.brand.domain.form.BrandQueryForm;
import net.lab1024.sa.admin.module.business.brand.domain.vo.BrandVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Brand Dao
 *
 * @author SmartAdmin CRUD Generator
 * @since 2026-01-24
 */
@Mapper
public interface BrandDao extends BaseMapper<BrandEntity> {

  /**
   * Query brands with pagination
   *
   * @param page Page object
   * @param queryForm Query parameters
   * @return List of BrandVO
   */
  List<BrandVO> queryBrand(Page<?> page, @Param("queryForm") BrandQueryForm queryForm);

  /**
   * Get brand by name (for uniqueness check)
   *
   * @param brandName Brand name
   * @param excludeBrandId Brand ID to exclude (for update validation)
   * @return BrandEntity or null
   */
  BrandEntity getByBrandName(
      @Param("brandName") String brandName, @Param("excludeBrandId") Long excludeBrandId);
}
