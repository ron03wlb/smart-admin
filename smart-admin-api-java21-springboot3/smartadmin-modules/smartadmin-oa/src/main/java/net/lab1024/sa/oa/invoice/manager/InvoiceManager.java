package net.lab1024.sa.oa.invoice.manager;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.oa.invoice.dao.InvoiceDao;
import net.lab1024.sa.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.support.datatracer.constant.DataTracerConst;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OA发票信息 Manager
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-06-23 19:32:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class InvoiceManager {

  private final InvoiceDao invoiceDao;

  private final DataTracerService dataTracerService;

  /** 新建发票信息（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void createInvoiceTransaction(InvoiceAddForm createVO, Long enterpriseId) {
    // 数据插入
    InvoiceEntity insertInvoice = SmartBeanUtil.copy(createVO, InvoiceEntity.class);
    invoiceDao.insert(insertInvoice);
    dataTracerService.addTrace(
        enterpriseId,
        DataTracerTypeEnum.OA_ENTERPRISE,
        "新增发票：" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(insertInvoice));
  }

  /** 编辑发票信息（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void updateInvoiceTransaction(
      InvoiceUpdateForm updateVO, InvoiceEntity invoiceDetail, Long enterpriseId) {
    // 数据编辑
    InvoiceEntity updateInvoice = SmartBeanUtil.copy(updateVO, InvoiceEntity.class);
    invoiceDao.updateById(updateInvoice);
    dataTracerService.addTrace(
        enterpriseId,
        DataTracerTypeEnum.OA_ENTERPRISE,
        "更新发票："
            + DataTracerConst.HTML_BR
            + dataTracerService.getChangeContent(invoiceDetail, updateInvoice));
  }

  /** 删除发票信息（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void deleteInvoiceTransaction(Long invoiceId, InvoiceEntity invoiceDetail) {
    invoiceDao.deleteInvoice(invoiceId, Boolean.TRUE);
    dataTracerService.addTrace(
        invoiceDetail.getEnterpriseId(),
        DataTracerTypeEnum.OA_ENTERPRISE,
        "删除发票：" + DataTracerConst.HTML_BR + dataTracerService.getChangeContent(invoiceDetail));
  }
}
