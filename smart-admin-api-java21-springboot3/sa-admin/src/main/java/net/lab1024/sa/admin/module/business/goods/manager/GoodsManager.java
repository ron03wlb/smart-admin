package net.lab1024.sa.admin.module.business.goods.manager;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.goods.dao.GoodsDao;
import net.lab1024.sa.admin.module.business.goods.domain.entity.GoodsEntity;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsAddForm;
import net.lab1024.sa.admin.module.business.goods.domain.form.GoodsUpdateForm;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品 Manager
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class GoodsManager {

  private final GoodsDao goodsDao;

  private final DataTracerService dataTracerService;

  /** 添加商品（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void addGoodsTransaction(GoodsAddForm addForm) {
    GoodsEntity goodsEntity = SmartBeanUtil.copy(addForm, GoodsEntity.class);
    goodsEntity.setDeletedFlag(Boolean.FALSE);
    goodsDao.insert(goodsEntity);
    dataTracerService.insert(goodsEntity.getGoodsId(), DataTracerTypeEnum.GOODS);
  }

  /** 更新商品（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void updateGoodsTransaction(GoodsUpdateForm updateForm, GoodsEntity originEntity) {
    GoodsEntity goodsEntity = SmartBeanUtil.copy(updateForm, GoodsEntity.class);
    goodsDao.updateById(goodsEntity);
    dataTracerService.update(
        updateForm.getGoodsId(), DataTracerTypeEnum.GOODS, originEntity, goodsEntity);
  }

  /** 删除商品（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void deleteGoodsTransaction(Long goodsId) {
    goodsDao.batchUpdateDeleted(Collections.singletonList(goodsId), Boolean.TRUE);
    dataTracerService.batchDelete(Collections.singletonList(goodsId), DataTracerTypeEnum.GOODS);
  }

  /** 批量删除商品（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void batchDeleteGoodsTransaction(java.util.List<Long> goodsIdList) {
    goodsDao.batchUpdateDeleted(goodsIdList, Boolean.TRUE);
    dataTracerService.batchDelete(goodsIdList, DataTracerTypeEnum.GOODS);
  }
}
