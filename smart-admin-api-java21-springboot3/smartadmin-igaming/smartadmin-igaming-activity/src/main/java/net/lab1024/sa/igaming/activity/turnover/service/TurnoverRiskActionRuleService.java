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
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRiskActionRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverRiskActionRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverRiskActionRuleVO;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverRiskActionRuleManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Turnover risk action rule service — CRUD for risk action rule configuration.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Service
@RequiredArgsConstructor
public class TurnoverRiskActionRuleService {

  private final TurnoverRiskActionRuleDao riskActionRuleDao;
  private final TurnoverRiskActionRuleManager riskActionRuleManager;

  public Option<TurnoverRiskActionRuleVO> getRule(Long ruleId) {
    TurnoverRiskActionRuleEntity entity = riskActionRuleDao.selectById(ruleId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, TurnoverRiskActionRuleVO.class));
  }

  public ResponseDTO<PageResult<TurnoverRiskActionRuleVO>> queryRules(
      TurnoverRiskActionRuleQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<TurnoverRiskActionRuleVO> list = riskActionRuleDao.queryPage(page, form);
    PageResult<TurnoverRiskActionRuleVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<Void> addRule(
      TurnoverRiskActionRuleAddForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverRiskActionRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverRiskActionRuleEntity.class);
    try {
      riskActionRuleManager.addRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "新增風控動作規則");
      return ResponseDTO.ok();
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam("規則代碼已存在");
    }
  }

  public ResponseDTO<Void> updateRule(
      TurnoverRiskActionRuleUpdateForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverRiskActionRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverRiskActionRuleEntity.class);
    try {
      riskActionRuleManager.updateRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "更新風控動作規則");
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
      riskActionRuleManager.deleteRule(
          ruleId, operatorId, operatorName, changeReason != null ? changeReason : "刪除風控動作規則");
      return ResponseDTO.ok();
    } catch (IllegalArgumentException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }
  }

  public ResponseDTO<Void> evictCache() {
    riskActionRuleManager.evictCache(TenantContext.getTenantId());
    return ResponseDTO.ok();
  }
}
