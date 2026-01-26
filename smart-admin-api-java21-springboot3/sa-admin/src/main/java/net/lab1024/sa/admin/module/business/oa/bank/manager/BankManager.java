package net.lab1024.sa.admin.module.business.oa.bank.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.oa.bank.dao.BankDao;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankEntity;
import net.lab1024.sa.admin.module.business.oa.bank.domain.BankUpdateForm;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerConst;
import net.lab1024.sa.base.module.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OA银行信息 Manager
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-06-23 21:59:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class BankManager {

  private final BankDao bankDao;

  private final DataTracerService dataTracerService;

  /** 新建银行信息（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void createBankTransaction(BankCreateForm createVO, Long enterpriseId) {
    // 数据插入
    BankEntity insertBank = SmartBeanUtil.copy(createVO, BankEntity.class);
    bankDao.insert(insertBank);
    dataTracerService.addTrace(
        enterpriseId,
        DataTracerTypeEnum.OA_ENTERPRISE,
        "新增银行:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(insertBank));
  }

  /** 编辑银行信息（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void updateBankTransaction(
      BankUpdateForm updateVO, BankEntity bankDetail, Long enterpriseId) {
    // 数据编辑
    BankEntity updateBank = SmartBeanUtil.copy(updateVO, BankEntity.class);
    bankDao.updateById(updateBank);
    dataTracerService.addTrace(
        enterpriseId,
        DataTracerTypeEnum.OA_ENTERPRISE,
        "更新银行:"
            + DataTracerConst.HTML_BR
            + dataTracerService.getChangeContent(bankDetail, updateBank));
  }

  /** 删除银行信息（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void deleteBankTransaction(Long bankId, BankEntity bankDetail) {
    bankDao.deleteBank(bankId, Boolean.TRUE);
    dataTracerService.addTrace(
        bankDetail.getEnterpriseId(),
        DataTracerTypeEnum.OA_ENTERPRISE,
        "删除银行:" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(bankDetail));
  }
}
