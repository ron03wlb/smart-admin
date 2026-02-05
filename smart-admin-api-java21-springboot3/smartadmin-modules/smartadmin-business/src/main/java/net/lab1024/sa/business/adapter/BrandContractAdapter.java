package net.lab1024.sa.business.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.business.contract.BrandContract;
import net.lab1024.sa.api.business.dto.BrandDTO;
import net.lab1024.sa.business.brand.dao.BrandDao;
import net.lab1024.sa.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.business.brand.service.BrandService;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Component;

/**
 * 品牌契約適配器
 *
 * <p>Adapter Pattern: 將 BrandService 適配到 BrandContract API 契約。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 替代 null 返回
 *   <li>參數驗證拋出 IllegalArgumentException
 *   <li>使用 SmartBeanUtil 進行對象轉換
 *   <li>無副作用的查詢操作
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class BrandContractAdapter implements BrandContract {

  private final BrandService brandService;
  private final BrandDao brandDao;

  /**
   * 查詢所有品牌（非刪除）
   *
   * @return 品牌列表（非空，可能為空列表）
   */
  @Override
  public List<BrandDTO> listAll() {
    LambdaQueryWrapper<BrandEntity> queryWrapper =
        new LambdaQueryWrapper<BrandEntity>().eq(BrandEntity::getDeletedFlag, Boolean.FALSE);
    return brandDao.selectList(queryWrapper).stream()
        .map(entity -> SmartBeanUtil.copy(entity, BrandDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 根據 ID 查詢品牌
   *
   * @param brandId 品牌 ID
   * @return Option 包裝的品牌對象（不存在返回 Option.none()）
   * @throws IllegalArgumentException 如果 brandId 為 null
   */
  @Override
  public Option<BrandDTO> getById(Long brandId) {
    if (brandId == null) {
      throw new IllegalArgumentException("brandId cannot be null");
    }
    return brandService.getById(brandId).map(vo -> SmartBeanUtil.copy(vo, BrandDTO.class));
  }

  /**
   * 根據品牌名稱關鍵字查詢
   *
   * @param nameKeyword 名稱關鍵字
   * @return 品牌列表
   * @throws IllegalArgumentException 如果 nameKeyword 為 null 或空白
   */
  @Override
  public List<BrandDTO> queryByNameKeyword(String nameKeyword) {
    if (nameKeyword == null || nameKeyword.isBlank()) {
      throw new IllegalArgumentException("nameKeyword cannot be null or blank");
    }
    LambdaQueryWrapper<BrandEntity> queryWrapper =
        new LambdaQueryWrapper<BrandEntity>()
            .eq(BrandEntity::getDeletedFlag, Boolean.FALSE)
            .like(BrandEntity::getBrandName, nameKeyword);
    return brandDao.selectList(queryWrapper).stream()
        .map(entity -> SmartBeanUtil.copy(entity, BrandDTO.class))
        .collect(Collectors.toList());
  }
}
