package net.lab1024.sa.base.module.support.helpdoc.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.base.module.support.helpdoc.dao.HelpDocDao;
import net.lab1024.sa.base.module.support.helpdoc.domain.entity.HelpDocEntity;
import net.lab1024.sa.base.module.support.helpdoc.domain.form.HelpDocAddForm;
import net.lab1024.sa.base.module.support.helpdoc.domain.form.HelpDocQueryForm;
import net.lab1024.sa.base.module.support.helpdoc.domain.form.HelpDocUpdateForm;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocDetailVO;
import net.lab1024.sa.base.module.support.helpdoc.domain.vo.HelpDocVO;
import net.lab1024.sa.base.module.support.helpdoc.manager.HelpDocManager;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 后台管理业务
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-20 23:11:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class HelpDocService {

  private final HelpDocDao helpDocDao;

  private final HelpDocManager helpDaoManager;

  /**
   * 查询 帮助文档
   *
   * @param queryForm
   * @return
   */
  public PageResult<HelpDocVO> query(final HelpDocQueryForm queryForm) {
    final Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    final List<HelpDocVO> list = helpDocDao.query(page, queryForm);
    return SmartPageUtil.convert2PageResult(page, list);
  }

  /**
   * 添加
   *
   * @param addForm
   * @return
   */
  public ResponseDTO<String> add(final HelpDocAddForm addForm) {
    final HelpDocEntity helpDaoEntity = SmartBeanUtil.copy(addForm, HelpDocEntity.class);
    helpDaoManager.save(helpDaoEntity, addForm.getRelationList());
    return ResponseDTO.ok();
  }

  /**
   * 更新
   *
   * @param updateForm
   * @return
   */
  public ResponseDTO<String> update(final HelpDocUpdateForm updateForm) {
    // 更新
    final HelpDocEntity helpDaoEntity = SmartBeanUtil.copy(updateForm, HelpDocEntity.class);
    helpDaoManager.update(helpDaoEntity, updateForm.getRelationList());
    return ResponseDTO.ok();
  }

  /**
   * 删除 P0-5 Fix: 移除 Service 層的 @Transactional，委派給 Manager 層處理
   *
   * @param helpDocId
   * @return
   */
  public ResponseDTO<String> delete(final Long helpDocId) {
    final HelpDocEntity helpDaoEntity = helpDocDao.selectById(helpDocId);
    if (helpDaoEntity != null) {
      helpDaoManager.deleteTransaction(helpDocId);
    }
    return ResponseDTO.ok();
  }

  /**
   * 获取详情
   *
   * @param helpDocId
   * @return
   */
  public HelpDocDetailVO getDetail(final Long helpDocId) {
    final HelpDocEntity helpDaoEntity = helpDocDao.selectById(helpDocId);
    final HelpDocDetailVO detail = SmartBeanUtil.copy(helpDaoEntity, HelpDocDetailVO.class);
    if (detail != null) {
      detail.setRelationList(helpDocDao.queryRelationByHelpDoc(helpDocId));
    }
    return detail;
  }

  /**
   * 获取详情
   *
   * @param relationId
   * @return
   */
  public List<HelpDocVO> queryHelpDocByRelationId(final Long relationId) {
    return helpDocDao.queryHelpDocByRelationId(relationId);
  }
}
