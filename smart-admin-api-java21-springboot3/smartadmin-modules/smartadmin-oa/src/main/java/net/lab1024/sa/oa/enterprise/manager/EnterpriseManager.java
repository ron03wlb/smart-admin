package net.lab1024.sa.oa.enterprise.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.support.datatracer.domain.form.DataTracerForm;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业 Manager
 *
 * @author 1024创新实验室: 开云
 * @since 2022-07-28 20:37:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class EnterpriseManager {

  private final EnterpriseDao enterpriseDao;

  private final DataTracerService dataTracerService;

  /** 新建企业（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void createEnterpriseTransaction(EnterpriseCreateForm createVO) {
    // 数据插入
    EnterpriseEntity insertEnterprise = SmartBeanUtil.copy(createVO, EnterpriseEntity.class);
    enterpriseDao.insert(insertEnterprise);
    dataTracerService.insert(insertEnterprise.getEnterpriseId(), DataTracerTypeEnum.OA_ENTERPRISE);
  }

  /** 编辑企业（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void updateEnterpriseTransaction(
      EnterpriseUpdateForm updateVO, EnterpriseEntity enterpriseDetail) {
    // 数据编辑
    EnterpriseEntity updateEntity = SmartBeanUtil.copy(enterpriseDetail, EnterpriseEntity.class);
    SmartBeanUtil.copyProperties(updateVO, updateEntity);
    enterpriseDao.updateById(updateEntity);

    // 变更记录
    DataTracerForm dataTracerForm =
        DataTracerForm.builder()
            .dataId(updateVO.getEnterpriseId())
            .type(DataTracerTypeEnum.OA_ENTERPRISE)
            .content("修改企业信息")
            .diffOld(dataTracerService.getChangeContent(enterpriseDetail))
            .diffNew(dataTracerService.getChangeContent(updateEntity))
            .build();

    dataTracerService.addTrace(dataTracerForm);
  }

  /** 删除企业（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void deleteEnterpriseTransaction(Long enterpriseId) {
    enterpriseDao.deleteEnterprise(enterpriseId, Boolean.TRUE);
    dataTracerService.delete(enterpriseId, DataTracerTypeEnum.OA_ENTERPRISE);
  }
}
