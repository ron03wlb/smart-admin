package net.lab1024.sa.admin.module.business.oa.enterprise.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.dao.EnterpriseEmployeeDao;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEmployeeEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.entity.EnterpriseEntity;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseCreateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseEmployeeForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseEmployeeQueryForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseQueryForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.form.EnterpriseUpdateForm;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseEmployeeVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseExcelVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseListVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.domain.vo.EnterpriseVO;
import net.lab1024.sa.admin.module.business.oa.enterprise.manager.EnterpriseEmployeeManager;
import net.lab1024.sa.admin.module.business.oa.enterprise.manager.EnterpriseManager;
import net.lab1024.sa.admin.module.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.base.module.support.datatracer.service.DataTracerService;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 企业
 *
 * @author 1024创新实验室: 开云
 * @since 2022/7/28 20:37:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class EnterpriseService {

  private final EnterpriseDao enterpriseDao;

  private final EnterpriseEmployeeDao enterpriseEmployeeDao;

  private final EnterpriseEmployeeManager enterpriseEmployeeManager;

  private final EnterpriseManager enterpriseManager;

  private final DataTracerService dataTracerService;

  private final DepartmentCacheManager departmentCacheManager;

  /** 分页查询企业模块 */
  public ResponseDTO<PageResult<EnterpriseVO>> queryByPage(EnterpriseQueryForm queryForm) {
    queryForm.setDeletedFlag(Boolean.FALSE);
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<EnterpriseVO> enterpriseList = enterpriseDao.queryPage(page, queryForm);
    PageResult<EnterpriseVO> pageResult = SmartPageUtil.convert2PageResult(page, enterpriseList);
    return ResponseDTO.ok(pageResult);
  }

  /** 获取导出数据 */
  public List<EnterpriseExcelVO> getExcelExportData(EnterpriseQueryForm queryForm) {
    queryForm.setDeletedFlag(false);
    return enterpriseDao.selectExcelExportData(queryForm);
  }

  /** 查询企业详情 */
  public EnterpriseVO getDetail(Long enterpriseId) {
    return enterpriseDao.getDetail(enterpriseId, Boolean.FALSE);
  }

  /** 新建企业 */
  public ResponseDTO<String> createEnterprise(EnterpriseCreateForm createVO) {
    // 验证企业名称是否重复
    EnterpriseEntity validateEnterprise =
        enterpriseDao.queryByEnterpriseName(createVO.getEnterpriseName(), null, Boolean.FALSE);
    if (Objects.nonNull(validateEnterprise)) {
      return ResponseDTO.userErrorParam("企业名称重复");
    }
    // 调用 Manager 执行事务
    enterpriseManager.createEnterpriseTransaction(createVO);
    return ResponseDTO.ok();
  }

  /** 编辑企业 */
  public ResponseDTO<String> updateEnterprise(EnterpriseUpdateForm updateVO) {
    Long enterpriseId = updateVO.getEnterpriseId();
    // 校验企业是否存在
    EnterpriseEntity enterpriseDetail = enterpriseDao.selectById(enterpriseId);
    if (Objects.isNull(enterpriseDetail) || enterpriseDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("企业不存在");
    }
    // 验证企业名称是否重复
    EnterpriseEntity validateEnterprise =
        enterpriseDao.queryByEnterpriseName(
            updateVO.getEnterpriseName(), enterpriseId, Boolean.FALSE);
    if (Objects.nonNull(validateEnterprise)) {
      return ResponseDTO.userErrorParam("企业名称重复");
    }
    // 调用 Manager 执行事务
    enterpriseManager.updateEnterpriseTransaction(updateVO, enterpriseDetail);
    return ResponseDTO.ok();
  }

  /** 删除企业 */
  public ResponseDTO<String> deleteEnterprise(Long enterpriseId) {
    // 校验企业是否存在
    EnterpriseEntity enterpriseDetail = enterpriseDao.selectById(enterpriseId);
    if (Objects.isNull(enterpriseDetail) || enterpriseDetail.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("企业不存在");
    }
    // 调用 Manager 执行事务
    enterpriseManager.deleteEnterpriseTransaction(enterpriseId);
    return ResponseDTO.ok();
  }

  /** 企业列表查询 */
  public ResponseDTO<List<EnterpriseListVO>> queryList(Integer type) {
    List<EnterpriseListVO> enterpriseList =
        enterpriseDao.queryList(type, Boolean.FALSE, Boolean.FALSE);
    return ResponseDTO.ok(enterpriseList);
  }

  // ----------------------------------------- 以下为员工相关--------------------------------------------

  /** 企业添加员工 */
  public synchronized ResponseDTO<String> addEmployee(
      EnterpriseEmployeeForm enterpriseEmployeeForm) {
    Long enterpriseId = enterpriseEmployeeForm.getEnterpriseId();
    EnterpriseEntity enterpriseEntity = enterpriseDao.selectById(enterpriseId);
    if (enterpriseEntity == null || enterpriseEntity.getDeletedFlag()) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }
    // 过滤掉已存在的员工
    List<Long> waitAddEmployeeIdList = enterpriseEmployeeForm.getEmployeeIdList();
    List<EnterpriseEmployeeEntity> enterpriseEmployeeEntityList =
        enterpriseEmployeeDao.selectByEnterpriseAndEmployeeIdList(
            enterpriseId, waitAddEmployeeIdList);
    if (CollectionUtils.isNotEmpty(enterpriseEmployeeEntityList)) {
      List<Long> existEmployeeIdList =
          enterpriseEmployeeEntityList.stream()
              .map(EnterpriseEmployeeEntity::getEmployeeId)
              .collect(Collectors.toList());
      waitAddEmployeeIdList =
          waitAddEmployeeIdList.stream()
              .filter(e -> !existEmployeeIdList.contains(e))
              .collect(Collectors.toList());
    }
    if (CollectionUtils.isEmpty(waitAddEmployeeIdList)) {
      return ResponseDTO.ok();
    }
    List<EnterpriseEmployeeEntity> batchAddList = Lists.newArrayList();
    for (Long employeeId : waitAddEmployeeIdList) {
      EnterpriseEmployeeEntity enterpriseEmployeeEntity = new EnterpriseEmployeeEntity();
      enterpriseEmployeeEntity.setEnterpriseId(enterpriseId);
      enterpriseEmployeeEntity.setEmployeeId(employeeId);
      batchAddList.add(enterpriseEmployeeEntity);
    }
    enterpriseEmployeeManager.saveBatch(batchAddList);
    return ResponseDTO.ok();
  }

  /** 企业删除员工 */
  public synchronized ResponseDTO<String> deleteEmployee(
      EnterpriseEmployeeForm enterpriseEmployeeForm) {
    Long enterpriseId = enterpriseEmployeeForm.getEnterpriseId();
    EnterpriseEntity enterpriseEntity = enterpriseDao.selectById(enterpriseId);
    if (enterpriseEntity == null || enterpriseEntity.getDeletedFlag()) {
      return ResponseDTO.error(UserErrorCode.DATA_NOT_EXIST);
    }
    List<Long> waitDeleteEmployeeIdList = enterpriseEmployeeForm.getEmployeeIdList();
    enterpriseEmployeeDao.deleteByEnterpriseAndEmployeeIdList(
        enterpriseId, waitDeleteEmployeeIdList);
    return ResponseDTO.ok();
  }

  /** 企业下员工列表 */
  public List<EnterpriseEmployeeVO> employeeList(List<Long> enterpriseIdList) {
    if (CollectionUtils.isEmpty(enterpriseIdList)) {
      return Lists.newArrayList();
    }
    return enterpriseEmployeeDao.selectByEnterpriseIdList(enterpriseIdList);
  }

  /** 分页查询企业员工 */
  public PageResult<EnterpriseEmployeeVO> queryPageEmployeeList(
      EnterpriseEmployeeQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<EnterpriseEmployeeVO> enterpriseEmployeeVOList =
        enterpriseEmployeeDao.queryPageEmployeeList(page, queryForm);
    Map<Long, String> departmentPathMap = departmentCacheManager.getDepartmentPathMap();
    for (EnterpriseEmployeeVO enterpriseEmployeeVO : enterpriseEmployeeVOList) {
      enterpriseEmployeeVO.setDepartmentName(
          departmentPathMap.get(enterpriseEmployeeVO.getDepartmentId()));
    }
    return SmartPageUtil.convert2PageResult(page, enterpriseEmployeeVOList);
  }
}
