package net.lab1024.sa.admin.module.system.employee.manager;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.module.system.employee.dao.EmployeeDao;
import net.lab1024.sa.admin.module.system.employee.domain.entity.EmployeeEntity;
import net.lab1024.sa.admin.module.system.role.dao.RoleEmployeeDao;
import net.lab1024.sa.admin.module.system.role.domain.entity.RoleEmployeeEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 员工 manager
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-12-29 21:52:46 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class EmployeeManager extends ServiceImpl<EmployeeDao, EmployeeEntity> {

  private final EmployeeDao employeeDao;

  private final RoleEmployeeDao roleEmployeeDao;

  /** 保存员工 */
  @Transactional(rollbackFor = Throwable.class)
  public void saveEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    // 保存员工 获得id
    employeeDao.insert(employee);

    if (CollectionUtils.isNotEmpty(roleIdList)) {
      // 直接调用 DAO，避免 Manager 互调
      roleIdList.forEach(
          roleId ->
              roleEmployeeDao.insert(new RoleEmployeeEntity(roleId, employee.getEmployeeId())));
    }
  }

  /** 更新员工 */
  @Transactional(rollbackFor = Throwable.class)
  public void updateEmployee(EmployeeEntity employee, List<Long> roleIdList) {
    // 保存员工 获得id
    employeeDao.updateById(employee);

    // 若为空，则删除所有角色
    if (CollectionUtils.isEmpty(roleIdList)) {
      roleEmployeeDao.deleteByEmployeeId(employee.getEmployeeId());
      return;
    }

    List<RoleEmployeeEntity> roleEmployeeList =
        roleIdList.stream()
            .map(e -> new RoleEmployeeEntity(e, employee.getEmployeeId()))
            .collect(Collectors.toList());
    this.updateEmployeeRole(employee.getEmployeeId(), roleEmployeeList);
  }

  /** 更新员工角色 */
  @Transactional(rollbackFor = Throwable.class)
  public void updateEmployeeRole(Long employeeId, List<RoleEmployeeEntity> roleEmployeeList) {

    roleEmployeeDao.deleteByEmployeeId(employeeId);

    if (CollectionUtils.isNotEmpty(roleEmployeeList)) {
      // 直接调用 DAO，避免 Manager 互调
      roleEmployeeList.forEach(roleEmployeeDao::insert);
    }
  }

  /** 更新密码（事务方法） */
  @Transactional(rollbackFor = Throwable.class)
  public void updatePasswordTransaction(Long employeeId, String newEncryptPassword) {
    EmployeeEntity updateEntity = new EmployeeEntity();
    updateEntity.setEmployeeId(employeeId);
    updateEntity.setLoginPwd(newEncryptPassword);
    employeeDao.updateById(updateEntity);
  }
}
