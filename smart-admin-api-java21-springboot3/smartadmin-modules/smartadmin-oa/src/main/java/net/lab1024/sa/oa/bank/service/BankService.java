package net.lab1024.sa.oa.bank.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.oa.bank.dao.BankDao;
import net.lab1024.sa.oa.bank.domain.BankCreateForm;
import net.lab1024.sa.oa.bank.domain.BankEntity;
import net.lab1024.sa.oa.bank.domain.BankQueryForm;
import net.lab1024.sa.oa.bank.domain.BankUpdateForm;
import net.lab1024.sa.oa.bank.domain.BankVO;
import net.lab1024.sa.oa.bank.manager.BankManager;
import net.lab1024.sa.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import org.springframework.stereotype.Service;

/**
 * OA办公-OA银行信息
 *
 * @author 1024创新实验室:善逸
 * @since 2022/6/23 21:59:22 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BankService {

  private final BankDao bankDao;

  private final EnterpriseDao enterpriseDao;

  private final DataTracerService dataTracerService;

  private final BankManager bankManager;

  /** 分页查询银行信息 */
  public ResponseDTO<PageResult<BankVO>> queryByPage(BankQueryForm queryForm) {
    queryForm.setDeletedFlag(Boolean.FALSE);
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<BankVO> bankList = bankDao.queryPage(page, queryForm);
    PageResult<BankVO> pageResult = SmartPageUtil.convert2PageResult(page, bankList);
    return ResponseDTO.ok(pageResult);
  }

  /** 根据企业ID查询不分页的银行列表 */
  public ResponseDTO<List<BankVO>> queryList(Long enterpriseId) {
    BankQueryForm queryForm = new BankQueryForm();
    queryForm.setEnterpriseId(enterpriseId);
    queryForm.setDeletedFlag(Boolean.FALSE);
    List<BankVO> bankList = bankDao.queryPage(null, queryForm);
    return ResponseDTO.ok(bankList);
  }

  /** 查询银行信息详情 */
  public ResponseDTO<BankVO> getDetail(Long bankId) {
    // 校验银行信息是否存在
    BankVO bankVO = bankDao.getDetail(bankId, Boolean.FALSE);
    if (Objects.isNull(bankVO)) {
      return ResponseDTO.userErrorParam("银行信息不存在");
    }
    return ResponseDTO.ok(bankVO);
  }

  /** 新建银行信息 */
  public ResponseDTO<String> createBank(BankCreateForm createVO) {
    Long enterpriseId = createVO.getEnterpriseId();
    // 校验企业是否存在
    EnterpriseEntity enterpriseDetail = enterpriseDao.selectById(enterpriseId);
    if (Objects.isNull(enterpriseDetail) || enterpriseDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("企业不存在");
    }
    // 验证银行信息账号是否重复
    BankEntity validateBank =
        bankDao.queryByAccountNumber(
            enterpriseId, createVO.getAccountNumber(), null, Boolean.FALSE);
    if (Objects.nonNull(validateBank)) {
      return ResponseDTO.userErrorParam("银行信息账号重复");
    }
    // 调用 Manager 执行事务
    bankManager.createBankTransaction(createVO, enterpriseId);
    return ResponseDTO.ok();
  }

  /** 编辑银行信息 */
  public ResponseDTO<String> updateBank(BankUpdateForm updateVO) {
    Long enterpriseId = updateVO.getEnterpriseId();
    // 校验企业是否存在
    EnterpriseEntity enterpriseDetail = enterpriseDao.selectById(enterpriseId);
    if (Objects.isNull(enterpriseDetail) || enterpriseDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("企业不存在");
    }
    Long bankId = updateVO.getBankId();
    // 校验银行信息是否存在
    BankEntity bankDetail = bankDao.selectById(bankId);
    if (Objects.isNull(bankDetail) || bankDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("银行信息不存在");
    }
    // 验证银行信息账号是否重复
    BankEntity validateBank =
        bankDao.queryByAccountNumber(
            updateVO.getEnterpriseId(), updateVO.getAccountNumber(), bankId, Boolean.FALSE);
    if (Objects.nonNull(validateBank)) {
      return ResponseDTO.userErrorParam("银行信息账号重复");
    }
    // 调用 Manager 执行事务
    bankManager.updateBankTransaction(updateVO, bankDetail, enterpriseId);
    return ResponseDTO.ok();
  }

  /** 删除银行信息 */
  public ResponseDTO<String> deleteBank(Long bankId) {
    // 校验银行信息是否存在
    BankEntity bankDetail = bankDao.selectById(bankId);
    if (Objects.isNull(bankDetail) || bankDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("银行信息不存在");
    }
    // 调用 Manager 执行事务
    bankManager.deleteBankTransaction(bankId, bankDetail);
    return ResponseDTO.ok();
  }
}
