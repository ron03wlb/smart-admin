package net.lab1024.sa.base.module.support.helpdoc.manager;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.helpdoc.dao.HelpDocDao;
import net.lab1024.sa.base.module.support.helpdoc.domain.entity.HelpDocEntity;
import net.lab1024.sa.base.module.support.helpdoc.domain.form.HelpDocRelationForm;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帮助文档 manager
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class HelpDocManager {

  private final HelpDocDao helpDocDao;

  /**
   * 保存（事務方法）
   *
   * @param helpDocEntity
   * @param relationList
   */
  @Transactional(rollbackFor = Throwable.class)
  public void saveTransaction(
      final HelpDocEntity helpDocEntity, final List<HelpDocRelationForm> relationList) {
    helpDocDao.insert(helpDocEntity);
    final Long helpDocId = helpDocEntity.getHelpDocId();
    // 保存关联
    if (CollectionUtils.isNotEmpty(relationList)) {
      helpDocDao.insertRelation(helpDocId, relationList);
    }
  }

  /**
   * 更新（事務方法）
   *
   * @param helpDocEntity
   * @param relationList
   */
  @Transactional(rollbackFor = Throwable.class)
  public void updateTransaction(
      final HelpDocEntity helpDocEntity, final List<HelpDocRelationForm> relationList) {
    helpDocDao.updateById(helpDocEntity);
    final Long helpDocId = helpDocEntity.getHelpDocId();
    // 保存关联
    if (CollectionUtils.isNotEmpty(relationList)) {
      helpDocDao.deleteRelation(helpDocId);
      helpDocDao.insertRelation(helpDocId, relationList);
    }
  }

  /**
   * 删除（事務方法） P0-5 Fix: 從 Service 層遷移至 Manager 層
   *
   * @param helpDocId 幫助文檔 ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public void deleteTransaction(final Long helpDocId) {
    helpDocDao.deleteById(helpDocId);
    helpDocDao.deleteRelation(helpDocId);
  }
}
