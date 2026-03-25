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
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverGameWeightRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverGameWeightRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleAddForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleQueryForm;
import net.lab1024.sa.igaming.activity.turnover.domain.form.TurnoverGameWeightRuleUpdateForm;
import net.lab1024.sa.igaming.activity.turnover.domain.vo.TurnoverGameWeightRuleVO;
import net.lab1024.sa.igaming.activity.turnover.manager.TurnoverGameWeightRuleManager;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Turnover game weight rule service — CRUD for game weight rule configuration.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Service
@RequiredArgsConstructor
public class TurnoverGameWeightRuleService {

  private final TurnoverGameWeightRuleDao gameWeightRuleDao;
  private final TurnoverGameWeightRuleManager gameWeightRuleManager;

  /**
   * Get a single game weight rule by ID.
   *
   * @param ruleId rule ID
   * @return rule VO, or Option.none() if not found
   */
  public Option<TurnoverGameWeightRuleVO> getRule(Long ruleId) {
    TurnoverGameWeightRuleEntity entity = gameWeightRuleDao.selectById(ruleId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, TurnoverGameWeightRuleVO.class));
  }

  /**
   * Query game weight rules with pagination.
   *
   * @param form query form with pagination parameters
   * @return paginated result
   */
  public ResponseDTO<PageResult<TurnoverGameWeightRuleVO>> queryRules(
      TurnoverGameWeightRuleQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<TurnoverGameWeightRuleVO> list = gameWeightRuleDao.queryPage(page, form);
    PageResult<TurnoverGameWeightRuleVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  /**
   * Add a new game weight rule.
   *
   * @param form add form
   * @param operatorId operator ID
   * @param operatorName operator name
   * @param changeReason reason for change
   * @return success response
   * @throws DuplicateKeyException if rule_code already exists for tenant
   */
  public ResponseDTO<Void> addRule(
      TurnoverGameWeightRuleAddForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverGameWeightRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverGameWeightRuleEntity.class);

    try {
      gameWeightRuleManager.addRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "新增遊戲權重規則");
      return ResponseDTO.ok();
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam("規則代碼已存在");
    }
  }

  /**
   * Update an existing game weight rule.
   *
   * @param form update form (must include ruleId + version)
   * @param operatorId operator ID
   * @param operatorName operator name
   * @param changeReason reason for change
   * @return success response
   * @throws org.springframework.dao.OptimisticLockingFailureException if version conflict
   */
  public ResponseDTO<Void> updateRule(
      TurnoverGameWeightRuleUpdateForm form,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverGameWeightRuleEntity entity =
        SmartBeanUtil.copy(form, TurnoverGameWeightRuleEntity.class);

    try {
      gameWeightRuleManager.updateRule(
          entity, operatorId, operatorName, changeReason != null ? changeReason : "更新遊戲權重規則");
      return ResponseDTO.ok();
    } catch (org.springframework.dao.OptimisticLockingFailureException e) {
      return ResponseDTO.userErrorParam("版本衝突：規則已被其他用戶修改");
    } catch (IllegalArgumentException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }
  }

  /**
   * Delete a game weight rule (soft delete).
   *
   * @param ruleId rule ID
   * @param operatorId operator ID
   * @param operatorName operator name
   * @param changeReason reason for deletion
   * @return success response
   */
  public ResponseDTO<Void> deleteRule(
      Long ruleId, Long operatorId, String operatorName, String changeReason) {
    try {
      gameWeightRuleManager.deleteRule(
          ruleId, operatorId, operatorName, changeReason != null ? changeReason : "刪除遊戲權重規則");
      return ResponseDTO.ok();
    } catch (IllegalArgumentException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }
  }

  /**
   * Evict cache for game weight rules.
   *
   * @return success response
   */
  public ResponseDTO<Void> evictCache() {
    gameWeightRuleManager.evictCache(TenantContext.getTenantId());
    return ResponseDTO.ok();
  }

  /**
   * Get active game weight rule for a specific game category (cached).
   *
   * <p>Delegates to {@link TurnoverGameWeightRuleManager#getActiveRule} which uses Redis cache
   * (key: turnover:game-weight:tenant:{tenantId}:category:{gameCategory}).
   *
   * <p>Used by LiteFlow turnover calculation components to fetch game weight percentage.
   *
   * @param tenantId tenant ID
   * @param gameCategory game category (1=Slots, 2=Live, 3=Sports, 4=Poker, 5=Table, 6=Lottery)
   * @return active rule entity, or Option.none() if no active rule found
   */
  public Option<TurnoverGameWeightRuleEntity> getRuleByGameCategory(
      Long tenantId, Integer gameCategory) {
    return gameWeightRuleManager.getActiveRule(tenantId, gameCategory);
  }
}
