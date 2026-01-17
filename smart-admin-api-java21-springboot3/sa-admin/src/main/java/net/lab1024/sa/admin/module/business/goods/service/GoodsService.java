package net.lab1024.sa.admin.module.business.goods.service;

import cn.idev.excel.FastExcel;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.category.constant.CategoryTypeEnum;
import net.lab1024.sa.admin.module.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.admin.module.business.category.manager.CategoryCacheManager;
import net.lab1024.sa.admin.module.business.goods.constant.GoodsStatusEnum;
import net.lab1024.sa.admin.module.business.goods.dao.GoodsDao;
import net.lab1024.sa.admin.module.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsImportForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsQueryForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.admin.module.business.goods.domain.vo.GoodsExcelVO;
import net.lab1024.sa.admin.module.business.goods.domain.vo.GoodsVO;
import net.lab1024.sa.base.common.code.UserErrorCode;
import net.lab1024.sa.base.common.domain.PageResult;
import net.lab1024.sa.base.common.domain.ResponseDTO;
import net.lab1024.sa.base.common.exception.BusinessException;
import net.lab1024.sa.base.common.util.SmartBeanUtil;
import net.lab1024.sa.base.common.util.SmartEnumUtil;
import net.lab1024.sa.base.common.util.SmartPageUtil;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.base.module.support.dict.service.DictService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@Slf4j
public class GoodsService {

  @Resource private GoodsDao goodsDao;

  @Resource private CategoryCacheManager categoryCacheManager;

  @Resource private DataTracerService dataTracerService;

  @Resource private DictService dictService;

  /** 查询未删除的类目（直接调用 Manager，避免 Service 互调） */
  private Optional<CategoryEntity> queryCategory(Long categoryId) {
    if (categoryId == null) {
      return Optional.empty();
    }
    CategoryEntity entity = categoryCacheManager.queryCategory(categoryId);
    if (entity == null || entity.getDeletedFlag()) {
      return Optional.empty();
    }
    return Optional.of(entity);
  }

  /** 查询类目名称（直接调用 Manager，避免 Service 互调） */
  private String queryCategoryName(Long categoryId) {
    CategoryEntity entity = categoryCacheManager.queryCategory(categoryId);
    if (entity == null || entity.getDeletedFlag()) {
      return null;
    }
    return entity.getCategoryName();
  }

  /** 批量查询类目（直接调用 Manager，避免 Service 互调） */
  private Map<Long, CategoryEntity> queryCategoryList(List<Long> categoryIdList) {
    if (CollectionUtils.isEmpty(categoryIdList)) {
      return Collections.emptyMap();
    }
    return categoryIdList.stream()
        .distinct()
        .map(categoryCacheManager::queryCategory)
        .filter(e -> e != null && !e.getDeletedFlag())
        .collect(Collectors.toMap(CategoryEntity::getCategoryId, e -> e));
  }

  /** 添加商品 */
  @Transactional(rollbackFor = Exception.class)
  public ResponseDTO<String> add(GoodsAddForm addForm) {
    // 商品校验
    ResponseDTO<String> res = this.checkGoods(addForm);
    if (!res.getOk()) {
      return res;
    }
    GoodsEntity goodsEntity = SmartBeanUtil.copy(addForm, GoodsEntity.class);
    goodsEntity.setDeletedFlag(Boolean.FALSE);
    goodsDao.insert(goodsEntity);
    dataTracerService.insert(goodsEntity.getGoodsId(), DataTracerTypeEnum.GOODS);
    return ResponseDTO.ok();
  }

  /** 更新商品 */
  @Transactional(rollbackFor = Exception.class)
  public ResponseDTO<String> update(GoodsUpdateForm updateForm) {
    // 商品校验
    ResponseDTO<String> res = this.checkGoods(updateForm);
    if (!res.getOk()) {
      return res;
    }
    GoodsEntity originEntity = goodsDao.selectById(updateForm.getGoodsId());
    GoodsEntity goodsEntity = SmartBeanUtil.copy(updateForm, GoodsEntity.class);
    goodsDao.updateById(goodsEntity);
    dataTracerService.update(
        updateForm.getGoodsId(), DataTracerTypeEnum.GOODS, originEntity, goodsEntity);
    return ResponseDTO.ok();
  }

  /** 添加/更新 商品校验 */
  private ResponseDTO<String> checkGoods(GoodsAddForm addForm) {
    // 校验类目id
    Long categoryId = addForm.getCategoryId();
    Optional<CategoryEntity> optional = this.queryCategory(categoryId);
    if (!optional.isPresent()
        || !CategoryTypeEnum.GOODS.equalsValue(optional.get().getCategoryType())) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST, "商品类目不存在~");
    }

    return ResponseDTO.ok();
  }

  /** 删除 */
  @Transactional(rollbackFor = Exception.class)
  public ResponseDTO<String> delete(Long goodsId) {
    GoodsEntity goodsEntity = goodsDao.selectById(goodsId);
    if (goodsEntity == null) {
      return ResponseDTO.userErrorParam("商品不存在");
    }

    if (!goodsEntity.getGoodsStatus().equals(GoodsStatusEnum.SELL_OUT.getValue())) {
      return ResponseDTO.userErrorParam("只有售罄的商品才可以删除");
    }

    batchDelete(Collections.singletonList(goodsId));
    dataTracerService.batchDelete(Collections.singletonList(goodsId), DataTracerTypeEnum.GOODS);
    return ResponseDTO.ok();
  }

  /** 批量删除 */
  public ResponseDTO<String> batchDelete(List<Long> goodsIdList) {
    if (CollectionUtils.isEmpty(goodsIdList)) {
      return ResponseDTO.ok();
    }

    goodsDao.batchUpdateDeleted(goodsIdList, Boolean.TRUE);
    return ResponseDTO.ok();
  }

  /** 分页查询 */
  public ResponseDTO<PageResult<GoodsVO>> query(GoodsQueryForm queryForm) {
    queryForm.setDeletedFlag(false);
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<GoodsVO> list = goodsDao.query(page, queryForm);
    PageResult<GoodsVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    if (pageResult.getEmptyFlag()) {
      return ResponseDTO.ok(pageResult);
    }
    // 查询分类名称
    List<Long> categoryIdList =
        list.stream().map(GoodsVO::getCategoryId).distinct().collect(Collectors.toList());
    Map<Long, CategoryEntity> categoryMap = this.queryCategoryList(categoryIdList);
    list.forEach(
        e -> {
          CategoryEntity categoryEntity = categoryMap.get(e.getCategoryId());
          if (categoryEntity != null) {
            e.setCategoryName(categoryEntity.getCategoryName());
          }
        });
    return ResponseDTO.ok(pageResult);
  }

  /**
   * 商品导入
   *
   * @param file 上传文件
   * @return 结果
   */
  public ResponseDTO<String> importGoods(MultipartFile file) {
    List<GoodsImportForm> dataList;
    try {
      dataList =
          FastExcel.read(file.getInputStream()).head(GoodsImportForm.class).sheet().doReadSync();
    } catch (IOException e) {
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
      throw new BusinessException("数据格式存在问题，无法读取", e);
    }

    if (CollectionUtils.isEmpty(dataList)) {
      return ResponseDTO.userErrorParam("数据为空");
    }

    return ResponseDTO.okMsg("成功导入" + dataList.size() + "条，具体数据为：" + JSON.toJSONString(dataList));
  }

  /** 商品导出 */
  public List<GoodsExcelVO> getAllGoods() {
    List<GoodsEntity> goodsEntityList = goodsDao.selectList(null);
    String dictCode = "GOODS_PLACE";
    return goodsEntityList.stream()
        .map(
            e ->
                GoodsExcelVO.builder()
                    .goodsStatus(
                        SmartEnumUtil.getEnumDescByValue(e.getGoodsStatus(), GoodsStatusEnum.class))
                    .categoryName(this.queryCategoryName(e.getCategoryId()))
                    .place(
                        Arrays.stream(e.getPlace().split(","))
                            .map(code -> dictService.getDictDataLabel(dictCode, code))
                            .collect(Collectors.joining(",")))
                    .price(e.getPrice())
                    .goodsName(e.getGoodsName())
                    .remark(e.getRemark())
                    .build())
        .collect(Collectors.toList());
  }
}
