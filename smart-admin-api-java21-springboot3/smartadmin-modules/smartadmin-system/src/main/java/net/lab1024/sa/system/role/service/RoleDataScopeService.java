package net.lab1024.sa.system.role.service;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.code.UserErrorCode;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.system.role.domain.entity.RoleDataScopeEntity;
import net.lab1024.sa.system.role.domain.form.RoleDataScopeUpdateForm;
import net.lab1024.sa.system.role.domain.vo.RoleDataScopeVO;
import net.lab1024.sa.system.role.manager.RoleDataScopeManager;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 角色-数据范围
 *
 * @author 1024创新实验室: 善逸
 * @since 2021-10-22 23:17:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@RequiredArgsConstructor
@Service
public class RoleDataScopeService {

  private final RoleDataScopeManager roleDataScopeManager;

  /** 获取某个角色的数据范围设置信息 */
  public ResponseDTO<List<RoleDataScopeVO>> getRoleDataScopeList(Long roleId) {
    List<RoleDataScopeEntity> roleDataScopeEntityList =
        roleDataScopeManager.getBaseMapper().listByRoleId(roleId);
    if (CollectionUtils.isEmpty(roleDataScopeEntityList)) {
      return ResponseDTO.ok(Lists.newArrayList());
    }
    List<RoleDataScopeVO> roleDataScopeList =
        SmartBeanUtil.copyList(roleDataScopeEntityList, RoleDataScopeVO.class);
    return ResponseDTO.ok(roleDataScopeList);
  }

  /** 批量设置某个角色的数据范围设置信息 */
  public ResponseDTO<String> updateRoleDataScopeList(
      RoleDataScopeUpdateForm roleDataScopeUpdateForm) {
    List<RoleDataScopeUpdateForm.RoleUpdateDataScopeListFormItem> batchSetList =
        roleDataScopeUpdateForm.getDataScopeItemList();
    if (CollectionUtils.isEmpty(batchSetList)) {
      return ResponseDTO.error(UserErrorCode.PARAM_ERROR, "缺少配置信息");
    }
    List<RoleDataScopeEntity> roleDataScopeEntityList =
        SmartBeanUtil.copyList(batchSetList, RoleDataScopeEntity.class);
    roleDataScopeEntityList.forEach(e -> e.setRoleId(roleDataScopeUpdateForm.getRoleId()));
    // 调用 Manager 执行事务
    roleDataScopeManager.updateRoleDataScopeListTransaction(
        roleDataScopeUpdateForm.getRoleId(), roleDataScopeEntityList);
    return ResponseDTO.ok();
  }
}
