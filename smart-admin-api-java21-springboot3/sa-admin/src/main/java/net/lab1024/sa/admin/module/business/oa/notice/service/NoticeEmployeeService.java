package net.lab1024.sa.admin.module.business.oa.notice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.business.oa.notice.constant.NoticeVisibleRangeDataTypeEnum;
import net.lab1024.sa.admin.module.business.oa.notice.dao.NoticeDao;
import net.lab1024.sa.admin.module.business.oa.notice.domain.entity.NoticeEntity;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeEmployeeQueryForm;
import net.lab1024.sa.admin.module.business.oa.notice.domain.form.NoticeViewRecordQueryForm;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeDetailVO;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeEmployeeVO;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeViewRecordVO;
import net.lab1024.sa.admin.module.business.oa.notice.domain.vo.NoticeVisibleRangeVO;
import net.lab1024.sa.admin.module.system.department.manager.DepartmentCacheManager;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.base.mybatis.util.SmartPageUtil;
import net.lab1024.sa.foundation.domain.response.PageResult;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartBeanUtil;
import org.springframework.stereotype.Service;

/**
 * 员工查看 通知。公告
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-08-12 21:40:39 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class NoticeEmployeeService {

  private final NoticeDao noticeDao;

  private final DepartmentCacheManager departmentCacheManager;

  private final EmployeeDao employeeDao;

  /** 查询我的 通知、公告清单 */
  public ResponseDTO<PageResult<NoticeEmployeeVO>> queryList(
      Long requestEmployeeId, NoticeEmployeeQueryForm noticeEmployeeQueryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(noticeEmployeeQueryForm);

    List<Long> employeeDepartmentIdList = Lists.newArrayList();
    EmployeeEntity employeeEntity = employeeDao.selectById(requestEmployeeId);
    // 如果不是管理员 则获取请求人的 部门及其子部门
    if (!employeeEntity.getAdministratorFlag() && employeeEntity.getDepartmentId() != null) {
      employeeDepartmentIdList =
          departmentCacheManager.getDepartmentSelfAndChildren(employeeEntity.getDepartmentId());
    }

    List<NoticeEmployeeVO> noticeList;
    // 只查询未读的
    if (noticeEmployeeQueryForm.getNotViewFlag() != null
        && noticeEmployeeQueryForm.getNotViewFlag()) {
      noticeList =
          noticeDao.queryEmployeeNotViewNotice(
              page,
              requestEmployeeId,
              noticeEmployeeQueryForm,
              employeeDepartmentIdList,
              false,
              employeeEntity.getAdministratorFlag(),
              NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue(),
              NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue());
    } else {
      // 查询全部
      noticeList =
          noticeDao.queryEmployeeNotice(
              page,
              requestEmployeeId,
              noticeEmployeeQueryForm,
              employeeDepartmentIdList,
              false,
              employeeEntity.getAdministratorFlag(),
              NoticeVisibleRangeDataTypeEnum.DEPARTMENT.getValue(),
              NoticeVisibleRangeDataTypeEnum.EMPLOYEE.getValue());
    }
    // 设置发布日期
    noticeList.forEach(notice -> notice.setPublishDate(notice.getPublishTime().toLocalDate()));

    return ResponseDTO.ok(SmartPageUtil.convert2PageResult(page, noticeList));
  }

  /** 查询我的 待查看的 通知、公告清单 */
  public ResponseDTO<NoticeDetailVO> view(
      Long requestEmployeeId, Long noticeId, String ip, String userAgent) {
    NoticeEntity noticeEntity = noticeDao.selectById(noticeId);
    if (noticeEntity == null || Boolean.TRUE.equals(noticeEntity.getDeletedFlag())) {
      return ResponseDTO.userErrorParam("通知公告不存在");
    }

    EmployeeEntity employeeEntity = employeeDao.selectById(requestEmployeeId);
    if (!noticeEntity.getAllVisibleFlag()) {
      List<NoticeVisibleRangeVO> visibleRangeList = noticeDao.queryVisibleRange(noticeId);
      if (!checkVisibleRange(
          visibleRangeList, requestEmployeeId, employeeEntity.getDepartmentId())) {
        return ResponseDTO.userErrorParam("对不起，您没有权限查看内容");
      }
    }

    NoticeDetailVO noticeDetailVO = SmartBeanUtil.copy(noticeEntity, NoticeDetailVO.class);
    long viewCount = noticeDao.viewRecordCount(noticeId, requestEmployeeId);
    if (viewCount == 0) {
      noticeDao.insertViewRecord(noticeId, requestEmployeeId, ip, userAgent, 1);
      // 该员工对于这个通知是第一次查看 页面浏览量+1 用户浏览量+1
      noticeDao.updateViewCount(noticeId, 1, 1);
      noticeDetailVO.setPageViewCount(noticeDetailVO.getPageViewCount() + 1);
      noticeDetailVO.setUserViewCount(noticeDetailVO.getUserViewCount() + 1);
    } else {
      noticeDao.updateViewRecord(noticeId, requestEmployeeId, ip, userAgent);
      // 该员工对于这个通知不是第一次查看 页面浏览量+1 用户浏览量+0
      noticeDao.updateViewCount(noticeId, 1, 0);
      noticeDetailVO.setPageViewCount(noticeDetailVO.getPageViewCount() + 1);
    }

    return ResponseDTO.ok(noticeDetailVO);
  }

  /** 校验是否有查看权限的范围 */
  public boolean checkVisibleRange(
      List<NoticeVisibleRangeVO> visibleRangeList, Long employeeId, Long departmentId) {
    // 员工范围
    boolean anyMatch =
        visibleRangeList.stream()
            .anyMatch(
                e ->
                    NoticeVisibleRangeDataTypeEnum.EMPLOYEE.equalsValue(e.getDataType())
                        && Objects.equals(e.getDataId(), employeeId));
    if (anyMatch) {
      return true;
    }

    // 部门范围
    List<Long> visibleDepartmentIdList =
        visibleRangeList.stream()
            .filter(e -> NoticeVisibleRangeDataTypeEnum.DEPARTMENT.equalsValue(e.getDataType()))
            .map(NoticeVisibleRangeVO::getDataId)
            .collect(Collectors.toList());

    for (Long visibleDepartmentId : visibleDepartmentIdList) {
      List<Long> departmentIdList =
          departmentCacheManager.getDepartmentSelfAndChildren(visibleDepartmentId);
      if (departmentIdList.contains(departmentId)) {
        return true;
      }
    }
    return false;
  }

  /** 分页查询 查看记录 */
  public PageResult<NoticeViewRecordVO> queryViewRecord(
      NoticeViewRecordQueryForm noticeViewRecordQueryForm) {
    Page<?> page = SmartPageUtil.convert2PageQuery(noticeViewRecordQueryForm);
    List<NoticeViewRecordVO> noticeViewRecordList =
        noticeDao.queryNoticeViewRecordList(page, noticeViewRecordQueryForm);
    return SmartPageUtil.convert2PageResult(page, noticeViewRecordList);
  }
}
