package net.lab1024.sa.business.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.api.business.contract.GoodsContract;
import net.lab1024.sa.api.business.dto.GoodsDTO;
import net.lab1024.sa.business.goods.dao.GoodsDao;
import net.lab1024.sa.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.springframework.stereotype.Component;

/**
 * 商品契約適配器
 *
 * <p>Adapter Pattern: 將 GoodsDao 適配到 GoodsContract API 契約。
 *
 * <p>設計原則：
 *
 * <ul>
 *   <li>使用 Vavr Option 替代 null 返回
 *   <li>參數驗證拋出 IllegalArgumentException
 *   <li>使用 SmartBeanUtil 進行對象轉換
 *   <li>無副作用的查詢操作
 *   <li>直接調用 Dao 層進行簡單查詢（SmartAdmin 架構允許）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@Component
@RequiredArgsConstructor
public class GoodsContractAdapter implements GoodsContract {

  private final GoodsDao goodsDao;

  /**
   * 根據 ID 查詢商品
   *
   * @param goodsId 商品 ID
   * @return Option 包裝的商品對象
   * @throws IllegalArgumentException 如果 goodsId 為 null
   */
  @Override
  public Option<GoodsDTO> getById(Long goodsId) {
    if (goodsId == null) {
      throw new IllegalArgumentException("goodsId cannot be null");
    }
    return Option.of(goodsDao.selectById(goodsId))
        .filter(entity -> !entity.getDeletedFlag())
        .map(entity -> SmartBeanUtil.copy(entity, GoodsDTO.class));
  }

  /**
   * 根據分類 ID 查詢商品列表
   *
   * @param categoryId 分類 ID
   * @return 商品列表（非空，可能為空列表）
   * @throws IllegalArgumentException 如果 categoryId 為 null
   */
  @Override
  public List<GoodsDTO> queryByCategoryId(Long categoryId) {
    if (categoryId == null) {
      throw new IllegalArgumentException("categoryId cannot be null");
    }
    LambdaQueryWrapper<GoodsEntity> queryWrapper =
        new LambdaQueryWrapper<GoodsEntity>()
            .eq(GoodsEntity::getDeletedFlag, Boolean.FALSE)
            .eq(GoodsEntity::getCategoryId, categoryId);
    return goodsDao.selectList(queryWrapper).stream()
        .map(entity -> SmartBeanUtil.copy(entity, GoodsDTO.class))
        .collect(Collectors.toList());
  }

  /**
   * 查詢所有上架商品（非刪除且上架）
   *
   * @return 商品列表
   */
  @Override
  public List<GoodsDTO> listAllOnShelf() {
    LambdaQueryWrapper<GoodsEntity> queryWrapper =
        new LambdaQueryWrapper<GoodsEntity>()
            .eq(GoodsEntity::getDeletedFlag, Boolean.FALSE)
            .eq(GoodsEntity::getShelvesFlag, Boolean.TRUE);
    return goodsDao.selectList(queryWrapper).stream()
        .map(entity -> SmartBeanUtil.copy(entity, GoodsDTO.class))
        .collect(Collectors.toList());
  }
}
