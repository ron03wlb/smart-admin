package net.lab1024.sa.admin.module.business.oa.invoice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.admin.module.business.oa.invoice.dao.InvoiceDao;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceAddForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceEntity;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceQueryForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceUpdateForm;
import net.lab1024.sa.admin.module.business.oa.invoice.domain.InvoiceVO;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.stereotype.Service;

/**
 * OA发票信息
 *
 * @author 1024创新实验室: 善逸
 * @since 2022-06-23 19:32:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class InvoiceService {

  private final InvoiceDao invoiceDao;

  private final EnterpriseDao enterpriseDao;

  private final DataTracerService dataTracerService;

  private final net.lab1024.sa.admin.module.business.oa.invoice.manager.InvoiceManager
      invoiceManager;

  /** 分页查询发票信息 */
  public ResponseDTO<PageResult<InvoiceVO>> queryByPage(InvoiceQueryForm queryForm) {
    queryForm.setDeletedFlag(Boolean.FALSE);
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<InvoiceVO> invoiceList = invoiceDao.queryPage(page, queryForm);
    PageResult<InvoiceVO> pageResult = SmartPageUtil.convert2PageResult(page, invoiceList);
    return ResponseDTO.ok(pageResult);
  }

  public ResponseDTO<List<InvoiceVO>> queryList(Long enterpriseId) {
    InvoiceQueryForm queryForm = new InvoiceQueryForm();
    queryForm.setDeletedFlag(Boolean.FALSE);
    queryForm.setDisabledFlag(Boolean.FALSE);
    queryForm.setEnterpriseId(enterpriseId);
    List<InvoiceVO> invoiceList = invoiceDao.queryPage(null, queryForm);
    return ResponseDTO.ok(invoiceList);
  }

  /** 查询发票信息详情 */
  public ResponseDTO<InvoiceVO> getDetail(Long invoiceId) {
    // 校验发票信息是否存在
    InvoiceVO invoiceVO = invoiceDao.getDetail(invoiceId, Boolean.FALSE);
    if (Objects.isNull(invoiceVO)) {
      return ResponseDTO.userErrorParam("发票信息不存在");
    }
    return ResponseDTO.ok(invoiceVO);
  }

  /** 新建发票信息 */
  public ResponseDTO<String> createInvoice(InvoiceAddForm createVO) {
    Long enterpriseId = createVO.getEnterpriseId();
    // 校验企业是否存在
    EnterpriseVO enterpriseVO = enterpriseDao.getDetail(enterpriseId, Boolean.FALSE);
    if (Objects.isNull(enterpriseVO)) {
      return ResponseDTO.userErrorParam("企业不存在");
    }
    // 验证发票信息账号是否重复
    InvoiceEntity validateInvoice =
        invoiceDao.queryByAccountNumber(
            enterpriseId, createVO.getAccountNumber(), null, Boolean.FALSE);
    if (Objects.nonNull(validateInvoice)) {
      return ResponseDTO.userErrorParam("发票信息账号重复");
    }
    // 调用 Manager 执行事务
    invoiceManager.createInvoiceTransaction(createVO, enterpriseId);
    return ResponseDTO.ok();
  }

  /** 编辑发票信息 */
  public ResponseDTO<String> updateInvoice(InvoiceUpdateForm updateVO) {
    Long enterpriseId = updateVO.getEnterpriseId();
    // 校验企业是否存在
    EnterpriseVO enterpriseVO = enterpriseDao.getDetail(enterpriseId, Boolean.FALSE);
    if (Objects.isNull(enterpriseVO)) {
      return ResponseDTO.userErrorParam("企业不存在");
    }
    Long invoiceId = updateVO.getInvoiceId();
    // 校验发票信息是否存在
    InvoiceEntity invoiceDetail = invoiceDao.selectById(invoiceId);
    if (Objects.isNull(invoiceDetail) || invoiceDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("发票信息不存在");
    }
    // 验证发票信息账号是否重复
    InvoiceEntity validateInvoice =
        invoiceDao.queryByAccountNumber(
            updateVO.getEnterpriseId(), updateVO.getAccountNumber(), invoiceId, Boolean.FALSE);
    if (Objects.nonNull(validateInvoice)) {
      return ResponseDTO.userErrorParam("发票信息账号重复");
    }
    // 调用 Manager 执行事务
    invoiceManager.updateInvoiceTransaction(updateVO, invoiceDetail, enterpriseId);
    return ResponseDTO.ok();
  }

  /** 删除发票信息 */
  public ResponseDTO<String> deleteInvoice(Long invoiceId) {
    // 校验发票信息是否存在
    InvoiceEntity invoiceDetail = invoiceDao.selectById(invoiceId);
    if (Objects.isNull(invoiceDetail) || invoiceDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("发票信息不存在");
    }
    // 调用 Manager 执行事务
    invoiceManager.deleteInvoiceTransaction(invoiceId, invoiceDetail);
    return ResponseDTO.ok();
  }
}
