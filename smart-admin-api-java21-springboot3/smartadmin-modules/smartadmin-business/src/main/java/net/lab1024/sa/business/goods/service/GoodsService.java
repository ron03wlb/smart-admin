package net.lab1024.sa.business.goods.service;

import cn.idev.excel.FastExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.business.category.constant.CategoryTypeEnum;
import net.lab1024.sa.business.category.domain.entity.CategoryEntity;
import net.lab1024.sa.business.category.manager.CategoryCacheManager;
import net.lab1024.sa.business.goods.constant.GoodsStatusEnum;
import net.lab1024.sa.business.goods.dao.GoodsDao;
import net.lab1024.sa.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.business.goods.domain.form.GoodsImportForm;
import net.lab1024.sa.business.goods.domain.form.GoodsQueryForm;
import net.lab1024.sa.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.business.goods.domain.vo.GoodsExcelVO;
import net.lab1024.sa.business.goods.domain.vo.GoodsVO;
import net.lab1024.sa.business.goods.manager.GoodsManager;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.exception.BusinessException;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.json.util.JsonUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.common.validation.util.SmartEnumUtil;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import net.lab1024.sa.support.dict.service.DictService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 商品
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoodsService {

  private final GoodsDao goodsDao;

  private final CategoryCacheManager categoryCacheManager;

  private final GoodsManager goodsManager;

  private final DataTracerService dataTracerService;

  private final DictService dictService;

  /** 查询未删除的类目（直接调用 Manager，避免 Service 互调） */
  private Option<CategoryEntity> queryCategory(Long categoryId) {
    if (categoryId == null) {
      return Option.none();
    }
    CategoryEntity entity = categoryCacheManager.queryCategory(categoryId);
    if (entity == null || entity.getDeletedFlag()) {
      return Option.none();
    }
    return Option.of(entity);
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
  public ResponseDTO<String> add(GoodsAddForm addForm) {
    // 商品校验
    ResponseDTO<String> res = this.checkGoods(addForm);
    if (!res.getOk()) {
      return res;
    }
    // 调用 Manager 执行事务
    goodsManager.addGoodsTransaction(addForm);
    return ResponseDTO.ok();
  }

  /** 更新商品 */
  public ResponseDTO<String> update(GoodsUpdateForm updateForm) {
    // 商品校验
    ResponseDTO<String> res = this.checkGoods(updateForm);
    if (!res.getOk()) {
      return res;
    }
    GoodsEntity originEntity = goodsDao.selectById(updateForm.getGoodsId());
    // 调用 Manager 执行事务
    goodsManager.updateGoodsTransaction(updateForm, originEntity);
    return ResponseDTO.ok();
  }

  /** 添加/更新 商品校验 */
  private ResponseDTO<String> checkGoods(GoodsAddForm addForm) {
    // 校验类目id
    Long categoryId = addForm.getCategoryId();
    Option<CategoryEntity> optional = this.queryCategory(categoryId);
    if (!optional.isDefined()
        || !CategoryTypeEnum.GOODS.equalsValue(optional.get().getCategoryType())) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST, "商品类目不存在~");
    }

    return ResponseDTO.ok();
  }

  /** 删除 */
  public ResponseDTO<String> delete(Long goodsId) {
    GoodsEntity goodsEntity = goodsDao.selectById(goodsId);
    if (goodsEntity == null) {
      return ResponseDTO.userErrorParam("商品不存在");
    }

    if (!goodsEntity.getGoodsStatus().equals(GoodsStatusEnum.SELL_OUT.getValue())) {
      return ResponseDTO.userErrorParam("只有售罄的商品才可以删除");
    }

    // 调用 Manager 执行事务
    goodsManager.deleteGoodsTransaction(goodsId);
    return ResponseDTO.ok();
  }

  /** 批量删除 */
  public ResponseDTO<String> batchDelete(List<Long> goodsIdList) {
    if (CollectionUtils.isEmpty(goodsIdList)) {
      return ResponseDTO.ok();
    }

    goodsManager.batchDeleteGoodsTransaction(goodsIdList);
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
   * 商品导入 P1-2 Fix: 使用 try-with-resources 確保 InputStream 正確關閉，避免資源泄漏
   *
   * @param file 上传文件
   * @return 结果
   */
  public ResponseDTO<String> importGoods(MultipartFile file) {
    List<GoodsImportForm> dataList;
    try (InputStream is = file.getInputStream()) {
      dataList = FastExcel.read(is).head(GoodsImportForm.class).sheet().doReadSync();
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new BusinessException("数据格式存在问题，无法读取", e);
    }

    if (CollectionUtils.isEmpty(dataList)) {
      return ResponseDTO.userErrorParam("数据为空");
    }

    return ResponseDTO.okMsg("成功导入" + dataList.size() + "条，具体数据为：" + JsonUtil.toJson(dataList));
  }

  /** 商品导出 - 優化版本：使用分頁查詢 + 批量載入分類 */
  public List<GoodsExcelVO> getAllGoods() {
    // 1. 分頁查詢商品（避免 OOM）
    List<GoodsEntity> allGoods = new ArrayList<>();
    int pageSize = 1000;
    int pageNum = 1;

    while (true) {
      Page<GoodsEntity> page =
          goodsDao.selectPage(
              new Page<>(pageNum, pageSize),
              new LambdaQueryWrapper<GoodsEntity>().eq(GoodsEntity::getDeletedFlag, false));
      if (page.getRecords().isEmpty()) {
        break;
      }
      allGoods.addAll(page.getRecords());
      if (!page.hasNext()) {
        break;
      }
      pageNum++;
    }

    // 2. 批量載入分類（解決 N+1 查詢）
    List<Long> categoryIdList =
        allGoods.stream().map(GoodsEntity::getCategoryId).distinct().collect(Collectors.toList());
    Map<Long, CategoryEntity> categoryMap = this.queryCategoryList(categoryIdList);

    // 3. 組裝結果（使用 Map 查詢）
    String dictCode = "GOODS_PLACE";
    return allGoods.stream()
        .map(
            e -> {
              String categoryName =
                  Option.of(categoryMap.get(e.getCategoryId()))
                      .map(CategoryEntity::getCategoryName)
                      .getOrElse("");
              return GoodsExcelVO.builder()
                  .categoryName(categoryName)
                  .goodsStatus(
                      SmartEnumUtil.getEnumDescByValue(e.getGoodsStatus(), GoodsStatusEnum.class))
                  .place(
                      // P0-3 Fix: 使用 Vavr Option 避免 NullPointerException
                      Option.of(e.getPlace())
                          .map(
                              place ->
                                  Arrays.stream(place.split(","))
                                      .map(code -> dictService.getDictDataLabel(dictCode, code))
                                      .collect(Collectors.joining(",")))
                          .getOrElse(""))
                  .price(e.getPrice())
                  .goodsName(e.getGoodsName())
                  .remark(e.getRemark())
                  .build();
            })
        .collect(Collectors.toList());
  }
}
