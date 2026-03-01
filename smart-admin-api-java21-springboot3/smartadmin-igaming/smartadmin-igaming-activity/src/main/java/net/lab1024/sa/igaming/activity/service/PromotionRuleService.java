package net.lab1024.sa.igaming.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleAddForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleQueryForm;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleUpdateForm;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Promotion rule service — CRUD for promotion configuration.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PromotionRuleService {

  private final PromotionRuleDao promotionRuleDao;
  private final PromotionCacheManager promotionCacheManager;

  public Option<PromotionRuleVO> getRule(Long ruleId) {
    PromotionRuleEntity entity = promotionRuleDao.selectById(ruleId);
    if (entity == null || entity.getDeleted()) {
      return Option.none();
    }
    return Option.of(SmartBeanUtil.copy(entity, PromotionRuleVO.class));
  }

  public ResponseDTO<PageResult<PromotionRuleVO>> queryRules(PromotionRuleQueryForm form) {
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<PromotionRuleVO> list = promotionRuleDao.queryPage(page, form);
    PageResult<PromotionRuleVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<PromotionRuleVO> addRule(PromotionRuleAddForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    PromotionRuleEntity entity = SmartBeanUtil.copy(form, PromotionRuleEntity.class);
    entity.setStatus(PromotionStatusEnum.ACTIVE.getValue());
    entity.setDeleted(false);
    try {
      promotionRuleDao.insert(entity);
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_CODE_EXISTS.getMsg());
    }

    promotionCacheManager.evictCache(tenantId);
    return ResponseDTO.ok(SmartBeanUtil.copy(entity, PromotionRuleVO.class));
  }

  public ResponseDTO<Void> updateRule(PromotionRuleUpdateForm form) {
    PromotionRuleEntity entity = promotionRuleDao.selectById(form.getRuleId());
    if (entity == null || entity.getDeleted()) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
    }

    Long tenantId = entity.getTenantId();

    if (!entity.getPromotionCode().equals(form.getPromotionCode())) {
      PromotionRuleEntity existing =
          promotionRuleDao.selectByCode(tenantId, form.getPromotionCode());
      if (existing != null && !existing.getRuleId().equals(entity.getRuleId())) {
        return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_CODE_EXISTS.getMsg());
      }
    }

    entity.setPromotionCode(form.getPromotionCode());
    entity.setPromotionName(form.getPromotionName());
    entity.setPromotionType(form.getPromotionType());
    entity.setStartTime(form.getStartTime());
    entity.setEndTime(form.getEndTime());
    if (form.getMinDeposit() != null) {
      entity.setMinDeposit(form.getMinDeposit());
    }
    if (form.getBonusRate() != null) {
      entity.setBonusRate(form.getBonusRate());
    }
    if (form.getMaxBonus() != null) {
      entity.setMaxBonus(form.getMaxBonus());
    }
    if (form.getWageringMultiplier() != null) {
      entity.setWageringMultiplier(form.getWageringMultiplier());
    }
    if (form.getMaxClaimsPerPlayer() != null) {
      entity.setMaxClaimsPerPlayer(form.getMaxClaimsPerPlayer());
    }
    if (form.getBonusExpiryDays() != null) {
      entity.setBonusExpiryDays(form.getBonusExpiryDays());
    }
    if (form.getGameRestriction() != null) {
      entity.setGameRestriction(form.getGameRestriction());
    }

    promotionRuleDao.updateById(entity);
    promotionCacheManager.evictCache(tenantId);
    return ResponseDTO.ok();
  }

  public ResponseDTO<Void> toggleStatus(Long ruleId, Integer status) {
    if (PromotionStatusEnum.of(status) == null) {
      return ResponseDTO.userErrorParam("Invalid promotion status: " + status);
    }
    PromotionRuleEntity entity = promotionRuleDao.selectById(ruleId);
    if (entity == null || entity.getDeleted()) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
    }
    entity.setStatus(status);
    promotionRuleDao.updateById(entity);
    promotionCacheManager.evictCache(entity.getTenantId());
    return ResponseDTO.ok();
  }
}
