package net.lab1024.sa.admin.module.business.category.manager;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.category.dao.CategoryDao;
import net.lab1024.sa.admin.module.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.admin.module.business.category.domain.vo.CategoryTreeVO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.foundation.cache.CacheService;
import net.lab1024.sa.foundation.cache.constant.CacheKeyConst;
import net.lab1024.sa.foundation.domain.constant.StringConst;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 类目 查询 缓存
 *
 * @author 1024创新实验室: 胡克
 * @since 2021/08/05 21:26:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@Slf4j
public class CategoryCacheManager {

  @Resource private CategoryDao categoryDao;

  @Resource private CacheService cacheService;

  /** 根据类目id 移除缓存 */
  public void removeCache() {
    cacheService.clear(CacheKeyConst.Category.CATEGORY_ENTITY);
    cacheService.clear(CacheKeyConst.Category.CATEGORY_SUB);
    cacheService.clear(CacheKeyConst.Category.CATEGORY_TREE);
    log.info("clear CATEGORY , CATEGORY_SUB , CATEGORY_TREE");
  }

  /** 查詢类目 */
  @Cached(
      name = CacheKeyConst.Category.CATEGORY_ENTITY,
      key = "#categoryId",
      cacheType = CacheType.BOTH,
      localExpire = 30,
      expire = 120,
      timeUnit = TimeUnit.MINUTES)
  public CategoryEntity queryCategory(Long categoryId) {
    return categoryDao.selectById(categoryId);
  }

  /** 查询类目 子级 */
  @Cached(
      name = CacheKeyConst.Category.CATEGORY_SUB,
      key = "#categoryId",
      cacheType = CacheType.BOTH,
      localExpire = 30,
      expire = 120,
      timeUnit = TimeUnit.MINUTES)
  public List<CategoryEntity> querySubCategory(Long categoryId) {
    return categoryDao.queryByParentId(Lists.newArrayList(categoryId), false);
  }

  /** 查询类目 层级树 优先查询缓存 */
  @Cached(
      name = CacheKeyConst.Category.CATEGORY_TREE,
      key = "#parentId + '_' + #categoryType",
      cacheType = CacheType.BOTH,
      localExpire = 30,
      expire = 120,
      timeUnit = TimeUnit.MINUTES)
  public List<CategoryTreeVO> queryCategoryTree(Long parentId, Integer categoryType) {
    List<CategoryEntity> allCategoryEntityList = categoryDao.queryByType(categoryType, false);

    List<CategoryEntity> categoryEntityList =
        allCategoryEntityList.stream()
            .filter(e -> e.getParentId().equals(parentId))
            .collect(Collectors.toList());
    List<CategoryTreeVO> treeList =
        SmartBeanUtil.copyList(categoryEntityList, CategoryTreeVO.class);
    treeList.forEach(
        e -> {
          e.setLabel(e.getCategoryName());
          e.setValue(e.getCategoryId());
          e.setCategoryFullName(e.getCategoryName());
        });
    // 递归设置子类
    this.queryAndSetSubCategory(treeList, allCategoryEntityList);
    return treeList;
  }

  /** 递归查询设置类目子类 从缓存查询子类 */
  private void queryAndSetSubCategory(
      List<CategoryTreeVO> treeList, List<CategoryEntity> allCategoryEntityList) {
    if (CollectionUtils.isEmpty(treeList)) {
      return;
    }
    List<Long> parentIdList =
        treeList.stream().map(CategoryTreeVO::getValue).collect(Collectors.toList());
    List<CategoryEntity> categoryEntityList =
        allCategoryEntityList.stream()
            .filter(e -> parentIdList.contains(e.getParentId()))
            .collect(Collectors.toList());
    Map<Long, List<CategoryEntity>> categorySubMap =
        categoryEntityList.stream().collect(Collectors.groupingBy(CategoryEntity::getParentId));
    treeList.forEach(
        e -> {
          List<CategoryEntity> childrenEntityList =
              categorySubMap.getOrDefault(e.getValue(), Lists.newArrayList());
          List<CategoryTreeVO> childrenVOList =
              SmartBeanUtil.copyList(childrenEntityList, CategoryTreeVO.class);
          childrenVOList.forEach(
              item -> {
                item.setLabel(item.getCategoryName());
                item.setValue(item.getCategoryId());
                item.setCategoryFullName(
                    e.getCategoryFullName() + StringConst.SEPARATOR_SLASH + item.getCategoryName());
              });
          // 递归查询
          this.queryAndSetSubCategory(childrenVOList, allCategoryEntityList);
          e.setChildren(childrenVOList);
        });
  }
}
