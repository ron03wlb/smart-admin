package net.lab1024.sa.igaming.activity.turnover.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverStatusFactorRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverStatusFactorRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverStatusFactorRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverStatusFactorRuleVO;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverStatusFactorRuleManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Turnover status factor rule service — CRUD for status factor rule configuration.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Service
@RequiredArgsConstructor
public class TurnoverStatusFactorRuleService {

  private final TurnoverStatusFactorRuleDao statusFactorRuleDao;
  private final TurnoverStatusFactorRuleManager statusFactorRuleManager;

  public Option<TurnoverStatusFactorRuleVO> getRule(Long ruleId) {
    TurnoverStatusFactorRuleEntity entity = statusFactorRuleDao.selectById(ruleId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, TurnoverStatusFactorRuleVO.class));
  }

  public ResponseDTO<PageResult<TurnoverStatusFactorRuleVO>> queryRules(
      TurnoverStatusFactorRuleQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<TurnoverStatusFactorRuleVO> list = statusFactorRuleDao.queryPage(page, form);
    PageResult<TurnoverStatusFactorRuleVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<Void> addRule(
      TurnoverStatusFactorRuleAddForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverStatusFactorRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverStatusFactorRuleEntity.class);
    try {
      statusFactorRuleManager.addRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "新增結算狀態因子規則");
      return ResponseDTO.ok();
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam("規則代碼已存在");
    }
  }

  public ResponseDTO<Void> updateRule(
      TurnoverStatusFactorRuleUpdateForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverStatusFactorRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverStatusFactorRuleEntity.class);
    try {
      statusFactorRuleManager.updateRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "更新結算狀態因子規則");
      return ResponseDTO.ok();
    } catch (org.springframework.dao.OptimisticLockingFailureException e) {
      return ResponseDTO.userErrorParam("版本衝突：規則已被其他用戶修改");
    } catch (IllegalArgumentException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }
  }

  public ResponseDTO<Void> deleteRule(
      Long ruleId, Long operatorId, String operatorName, String changeReason) {
    try {
      statusFactorRuleManager.deleteRule(
          ruleId, operatorId, operatorName, changeReason != null ? changeReason : "刪除結算狀態因子規則");
      return ResponseDTO.ok();
    } catch (IllegalArgumentException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }
  }

  public ResponseDTO<Void> evictCache() {
    statusFactorRuleManager.evictCache(TenantContext.getTenantId());
    return ResponseDTO.ok();
  }
}
