package net.lab1024.sa.oa.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Maps;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.constant.StringConst;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import net.lab1024.sa.oa.notice.dao.NoticeDao;
import net.lab1024.sa.oa.notice.dao.NoticeTypeDao;
import net.lab1024.sa.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.oa.notice.domain.entity.NoticeTypeEntity;
import net.lab1024.sa.oa.notice.domain.form.NoticeAddForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeQueryForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeUpdateForm;
import net.lab1024.sa.oa.notice.domain.form.NoticeVisibleRangeForm;
import net.lab1024.sa.oa.notice.domain.vo.NoticeUpdateFormVO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeVO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeVisibleRangeVO;
import net.lab1024.sa.oa.notice.manager.NoticeManager;
import net.lab1024.sa.support.datatracer.constant.DataTracerTypeEnum;
import net.lab1024.sa.support.datatracer.service.DataTracerService;
import net.lab1024.sa.system.department.dao.DepartmentDao;
import net.lab1024.sa.system.department.domain.entity.DepartmentEntity;
import net.lab1024.sa.system.employee.dao.EmployeeDao;
import net.lab1024.sa.system.employee.domain.entity.EmployeeEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

/**
 * 通知。公告 后台管理业务
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class NoticeService {

  private final NoticeDao noticeDao;

  private final NoticeManager noticeManager;

  private final EmployeeDao employeeDao;

  private final DepartmentDao departmentDao;

  private final NoticeTypeDao noticeTypeDao;

  private final DataTracerService dataTracerService;

  /** 查询 通知、公告 */
  public PageResult<NoticeVO> query(NoticeQueryForm queryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(queryForm);
    List<NoticeVO> list = noticeDao.query(page, queryForm);
    LocalDateTime now = LocalDateTime.now();
    list.forEach(e -> e.setPublishFlag(e.getPublishTime().isBefore(now)));
    return SmartPageUtil.convert2PageResult(page, list);
  }

  /** 添加 */
  public ResponseDTO<String> add(NoticeAddForm addForm) {
    // 校验并获取可见范围
    ResponseDTO<String> validate = this.checkAndBuildVisibleRange(addForm);
    if (!validate.getOk()) {
      return ResponseDTO.error(validate);
    }

    // build 资讯
    NoticeEntity noticeEntity = SmartBeanUtil.copy(addForm, NoticeEntity.class);
    // 发布时间：不是定时发布时 默认为 当前
    if (!addForm.getScheduledPublishFlag()) {
      noticeEntity.setPublishTime(LocalDateTime.now());
    }
    // 保存数据
    noticeManager.saveTransaction(noticeEntity, addForm.getVisibleRangeList());
    // 记录数据追踪
    dataTracerService.insert(noticeEntity.getNoticeId(), DataTracerTypeEnum.OA_NOTICE);
    return ResponseDTO.ok();
  }

  /** 校验并返回可见范围 */
  private ResponseDTO<String> checkAndBuildVisibleRange(NoticeAddForm form) {
    // 校验资讯分类
    NoticeTypeEntity noticeType = noticeTypeDao.selectById(form.getNoticeTypeId());
    if (noticeType == null) {
      return ResponseDTO.userErrorParam("分类不存在");
    }

    if (form.getAllVisibleFlag()) {
      return ResponseDTO.ok();
    }

    /*
     * 校验可见范围
     * 非全部可见时 校验选择的员工|部门
     */
    List<NoticeVisibleRangeForm> visibleRangeUpdateList = form.getVisibleRangeList();
    if (CollectionUtils.isEmpty(visibleRangeUpdateList)) {
      return ResponseDTO.userErrorParam("未设置可见范围");
    }

    // 校验可见范围-> 员工
    List<Long> employeeIdList =
        visibleRangeUpdateList.stream()
            .filter(e -> NoticeVisibleRangeDataTypeEnum.EMPLOYEE.equalsValue(e.getDataType()))
            .map(NoticeVisibleRangeForm::getDataId)
            .distinct()
            .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(employeeIdList)) {
      employeeIdList = employeeIdList.stream().distinct().collect(Collectors.toList());
      List<Long> dbEmployeeIdList =
          employeeDao.selectBatchIds(employeeIdList).stream()
              .map(EmployeeEntity::getEmployeeId)
              .collect(Collectors.toList());
      Collection<Long> subtract = CollectionUtils.subtract(employeeIdList, dbEmployeeIdList);
      if (!subtract.isEmpty()) {
        return ResponseDTO.userErrorParam("员工id不存在：" + subtract);
      }
    }

    // 校验可见范围-> 部门
    List<Long> deptIdList =
        visibleRangeUpdateList.stream()
            .filter(e -> NoticeVisibleRangeDataTypeEnum.DEPARTMENT.equalsValue(e.getDataType()))
            .map(NoticeVisibleRangeForm::getDataId)
            .distinct()
            .collect(Collectors.toList());
    if (CollectionUtils.isNotEmpty(deptIdList)) {
      deptIdList = deptIdList.stream().distinct().collect(Collectors.toList());
      List<Long> dbDeptIdList =
          departmentDao.selectBatchIds(deptIdList).stream()
              .map(DepartmentEntity::getDepartmentId)
              .collect(Collectors.toList());
      Collection<Long> subtract = CollectionUtils.subtract(deptIdList, dbDeptIdList);
      if (!subtract.isEmpty()) {
        return ResponseDTO.userErrorParam("部门id不存在：" + subtract);
      }
    }
    return ResponseDTO.ok();
  }

  /** 更新 */
  public ResponseDTO<String> update(NoticeUpdateForm updateForm) {

    NoticeEntity oldNoticeEntity = noticeDao.selectById(updateForm.getNoticeId());
    if (oldNoticeEntity == null) {
      return ResponseDTO.userErrorParam("通知不存在");
    }

    // 校验并获取可见范围
    ResponseDTO<String> res = this.checkAndBuildVisibleRange(updateForm);
    if (!res.getOk()) {
      return ResponseDTO.error(res);
    }

    // 更新
    NoticeEntity noticeEntity = SmartBeanUtil.copy(updateForm, NoticeEntity.class);
    noticeManager.updateTransaction(
        oldNoticeEntity, noticeEntity, updateForm.getVisibleRangeList());
    // 记录数据追踪
    dataTracerService.update(
        noticeEntity.getNoticeId(), DataTracerTypeEnum.OA_NOTICE, oldNoticeEntity, noticeEntity);
    return ResponseDTO.ok();
  }

  /** 删除 */
  public ResponseDTO<String> delete(Long noticeId) {
    NoticeEntity noticeEntity = noticeDao.selectById(noticeId);
    if (null == noticeEntity || noticeEntity.getDeletedFlag()) {
      return ResponseDTO.userErrorParam("通知公告不存在");
    }
    // 更新删除状态
    noticeDao.updateDeletedFlag(noticeId);
    dataTracerService.delete(noticeId, DataTracerTypeEnum.OA_NOTICE);
    return ResponseDTO.ok();
  }

  /** 获取更新表单用的详情 */
  public NoticeUpdateFormVO getUpdateFormVO(Long noticeId) {
    NoticeEntity noticeEntity = noticeDao.selectById(noticeId);
    if (null == noticeEntity) {
      return null;
    }

    NoticeUpdateFormVO updateFormVO = SmartBeanUtil.copy(noticeEntity, NoticeUpdateFormVO.class);
    NoticeTypeEntity noticeType = noticeTypeDao.selectById(noticeEntity.getNoticeTypeId());
    updateFormVO.setNoticeTypeName(noticeType != null ? noticeType.getNoticeTypeName() : null);
    updateFormVO.setPublishFlag(
        updateFormVO.getPublishTime() != null
            && updateFormVO.getPublishTime().isBefore(LocalDateTime.now()));

    if (!updateFormVO.getAllVisibleFlag()) {
      List<NoticeVisibleRangeVO> noticeVisibleRangeList = noticeDao.queryVisibleRange(noticeId);

      // 收集员工ID列表
      List<Long> employeeIdList =
          noticeVisibleRangeList.stream()
              .filter(
                  e -> NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue().equals(e.getDataType()))
              .map(NoticeVisibleRangeVO::getDataId)
              .collect(Collectors.toList());

      // 收集部门ID列表
      List<Long> departmentIdList =
          noticeVisibleRangeList.stream()
              .filter(
                  e -> NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue().equals(e.getDataType()))
              .map(NoticeVisibleRangeVO::getDataId)
              .collect(Collectors.toList());

      // 批量查询员工并构建Map（避免N+1查询）
      Map<Long, EmployeeEntity> employeeMap;
      if (CollectionUtils.isNotEmpty(employeeIdList)) {
        employeeMap =
            employeeDao.selectBatchIds(employeeIdList).stream()
                .collect(Collectors.toMap(EmployeeEntity::getEmployeeId, Function.identity()));
      } else {
        employeeMap = Maps.newHashMap();
      }

      // 批量查询部门并构建Map（避免N+1查询）
      Map<Long, DepartmentEntity> departmentMap;
      if (CollectionUtils.isNotEmpty(departmentIdList)) {
        departmentMap =
            departmentDao.selectBatchIds(departmentIdList).stream()
                .collect(Collectors.toMap(DepartmentEntity::getDepartmentId, Function.identity()));
      } else {
        departmentMap = Maps.newHashMap();
      }

      // 填充可见范围数据名称
      for (NoticeVisibleRangeVO noticeVisibleRange : noticeVisibleRangeList) {
        if (noticeVisibleRange
            .getDataType()
            .equals(NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue())) {
          EmployeeEntity employeeEntity = employeeMap.get(noticeVisibleRange.getDataId());
          noticeVisibleRange.setDataName(
              employeeEntity == null ? StringConst.EMPTY : employeeEntity.getActualName());
        } else {
          DepartmentEntity departmentEntity = departmentMap.get(noticeVisibleRange.getDataId());
          noticeVisibleRange.setDataName(
              departmentEntity == null ? StringConst.EMPTY : departmentEntity.getDepartmentName());
        }
      }
      updateFormVO.setVisibleRangeList(noticeVisibleRangeList);
    }
    return updateFormVO;
  }
}
