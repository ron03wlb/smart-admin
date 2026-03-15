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
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverOddsThresholdRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverOddsThresholdRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverOddsThresholdRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverOddsThresholdRuleVO;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverOddsThresholdRuleManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Turnover odds threshold rule service — CRUD for odds threshold rule configuration.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Service
@RequiredArgsConstructor
public class TurnoverOddsThresholdRuleService {

  private final TurnoverOddsThresholdRuleDao oddsThresholdRuleDao;
  private final TurnoverOddsThresholdRuleManager oddsThresholdRuleManager;

  public Option<TurnoverOddsThresholdRuleVO> getRule(Long ruleId) {
    TurnoverOddsThresholdRuleEntity entity = oddsThresholdRuleDao.selectById(ruleId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, TurnoverOddsThresholdRuleVO.class));
  }

  public ResponseDTO<PageResult<TurnoverOddsThresholdRuleVO>> queryRules(
      TurnoverOddsThresholdRuleQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<TurnoverOddsThresholdRuleVO> list = oddsThresholdRuleDao.queryPage(page, form);
    PageResult<TurnoverOddsThresholdRuleVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<Void> addRule(
      TurnoverOddsThresholdRuleAddForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverOddsThresholdRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverOddsThresholdRuleEntity.class);
    try {
      oddsThresholdRuleManager.addRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "新增賠率閾值規則");
      return ResponseDTO.ok();
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam("規則代碼已存在");
    }
  }

  public ResponseDTO<Void> updateRule(
      TurnoverOddsThresholdRuleUpdateForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverOddsThresholdRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverOddsThresholdRuleEntity.class);
    try {
      oddsThresholdRuleManager.updateRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "更新賠率閾值規則");
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
      oddsThresholdRuleManager.deleteRule(
          ruleId, operatorId, operatorName, changeReason != null ? changeReason : "刪除賠率閾值規則");
      return ResponseDTO.ok();
    } catch (IllegalArgumentException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }
  }

  public ResponseDTO<Void> evictCache() {
    oddsThresholdRuleManager.evictCache(TenantContext.getTenantId());
    return ResponseDTO.ok();
  }
}
