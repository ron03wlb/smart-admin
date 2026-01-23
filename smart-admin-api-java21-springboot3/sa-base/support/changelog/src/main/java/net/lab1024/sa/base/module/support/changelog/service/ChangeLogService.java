package net.lab1024.sa.base.module.support.changelog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import java.util.List;
import net.lab1024.sa.base.module.support.changelog.dao.ChangeLogDao;
import net.lab1024.sa.base.module.support.changelog.domain.entity.ChangeLogEntity;
import net.lab1024.sa.base.module.support.changelog.domain.form.ChangeLogAddForm;
import net.lab1024.sa.base.module.support.changelog.domain.form.ChangeLogQueryForm;
import net.lab1024.sa.base.module.support.changelog.domain.form.ChangeLogUpdateForm;
import net.lab1024.sa.base.module.support.changelog.domain.vo.ChangeLogVO;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 系统更新日志 Service
 *
 * @author 卓大
 * @since 2022-09-26 14:53:50 Copyright 1024创新实验室
 */
@Service
public class ChangeLogService {

  @Resource private ChangeLogDao changeLogDao;

  /** 分页查询 */
  public PageResult<ChangeLogVO> queryPage(final ChangeLogQueryForm queryForm) {
    final Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    final List<ChangeLogVO> list = changeLogDao.queryPage(page, queryForm);
    return SmartPageUtil.convert2PageResult(page, list);
  }

  /** 添加 */
  public synchronized ResponseDTO<String> add(final ChangeLogAddForm addForm) {
    final ChangeLogEntity existVersion = changeLogDao.selectByVersion(addForm.getUpdateVersion());
    if (existVersion != null) {
      return ResponseDTO.userErrorParam("此版本已经存在");
    }

    final ChangeLogEntity changeLogEntity = SmartBeanUtil.copy(addForm, ChangeLogEntity.class);
    changeLogDao.insert(changeLogEntity);
    return ResponseDTO.ok();
  }

  /** 更新 */
  public synchronized ResponseDTO<String> update(final ChangeLogUpdateForm updateForm) {
    final ChangeLogEntity existVersion =
        changeLogDao.selectByVersion(updateForm.getUpdateVersion());
    if (existVersion != null
        && !updateForm.getChangeLogId().equals(existVersion.getChangeLogId())) {
      return ResponseDTO.userErrorParam("此版本已经存在");
    }
    final ChangeLogEntity changeLogEntity = SmartBeanUtil.copy(updateForm, ChangeLogEntity.class);
    changeLogDao.updateById(changeLogEntity);
    return ResponseDTO.ok();
  }

  /** 批量删除 */
  public synchronized ResponseDTO<String> batchDelete(final List<Long> idList) {
    if (CollectionUtils.isEmpty(idList)) {
      return ResponseDTO.ok();
    }

    changeLogDao.deleteBatchIds(idList);
    return ResponseDTO.ok();
  }

  /** 单个删除 */
  public synchronized ResponseDTO<String> delete(final Long changeLogId) {
    if (null == changeLogId) {
      return ResponseDTO.ok();
    }

    changeLogDao.deleteById(changeLogId);
    return ResponseDTO.ok();
  }

  public ChangeLogVO getById(final Long changeLogId) {
    return SmartBeanUtil.copy(changeLogDao.selectById(changeLogId), ChangeLogVO.class);
  }
}
