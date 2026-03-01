package net.lab1024.sa.igaming.activity.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.dao.PromotionRuleDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.form.BonusClaimForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.BonusClaimResultVO;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.domain.vo.WageringProgressVO;
import net.lab1024.sa.igaming.activity.manager.BonusDistributionManager;
import net.lab1024.sa.igaming.activity.manager.PromotionCacheManager;
import net.lab1024.sa.igaming.common.code.ActivityErrorCode;
import net.lab1024.sa.igaming.common.constant.PromotionStatusEnum;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

/**
 * Bonus claim service — handles bonus claim flow and wagering progress queries.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BonusClaimService {

  private static final BigDecimal HUNDRED = new BigDecimal("100");

  private final PromotionRuleDao promotionRuleDao;
  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final PromotionCacheManager promotionCacheManager;
  private final BonusDistributionManager bonusDistributionManager;

  public ResponseDTO<BonusClaimResultVO> claimBonus(Long playerId, BonusClaimForm form) {
    Long tenantId = TenantContext.getTenantId();

    // Look up promotion rule
    PromotionRuleEntity rule =
        promotionCacheManager.getRuleByCode(tenantId, form.getPromotionCode());
    if (rule == null) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_NOT_FOUND.getMsg());
    }
    if (!PromotionStatusEnum.ACTIVE.getValue().equals(rule.getStatus())) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_DISABLED.getMsg());
    }
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    if (now.isBefore(rule.getStartTime()) || now.isAfter(rule.getEndTime())) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.PROMOTION_EXPIRED.getMsg());
    }

    // Distribute bonus (Manager handles transaction + atomic max-claims check)
    PlayerBonusRecordEntity record;
    try {
      record =
          bonusDistributionManager.distributeBonus(playerId, rule, form.getClaimId(), tenantId);
    } catch (DuplicateKeyException e) {
      return ResponseDTO.userErrorParam(ActivityErrorCode.ALREADY_CLAIMED.getMsg());
    } catch (IllegalStateException e) {
      return ResponseDTO.userErrorParam(e.getMessage());
    }

    // Build response
    BonusClaimResultVO result = new BonusClaimResultVO();
    result.setRecordId(record.getRecordId());
    result.setBonusAmount(record.getBonusAmount());
    result.setWageringRequired(record.getWageringRequired());
    result.setStatus(record.getStatus());
    return ResponseDTO.ok(result);
  }

  public ResponseDTO<PageResult<PlayerBonusRecordVO>> queryBonusRecords(
      Long playerId, WageringProgressQueryForm form) {
    form.setPlayerId(playerId);
    Page<?> page = SmartPageUtil.convert2PageQuery(form);
    List<PlayerBonusRecordVO> list = playerBonusRecordDao.queryPage(page, form);
    PageResult<PlayerBonusRecordVO> result = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(result);
  }

  public Option<WageringProgressVO> getWageringProgress(Long playerId, Long recordId) {
    PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
    if (record == null || record.getDeleted() || !record.getPlayerId().equals(playerId)) {
      return Option.none();
    }

    PromotionRuleEntity rule = promotionRuleDao.selectById(record.getRuleId());

    WageringProgressVO vo = new WageringProgressVO();
    vo.setRecordId(record.getRecordId());
    vo.setPromotionName(rule != null ? rule.getPromotionName() : "Unknown");
    vo.setWageringRequired(record.getWageringRequired());
    vo.setWageringCompleted(record.getWageringCompleted());
    vo.setStatus(record.getStatus());

    if (record.getWageringRequired().compareTo(BigDecimal.ZERO) > 0) {
      BigDecimal percent =
          record
              .getWageringCompleted()
              .multiply(HUNDRED)
              .divide(record.getWageringRequired(), 2, RoundingMode.HALF_UP);
      vo.setProgressPercent(percent.min(HUNDRED));
    } else {
      vo.setProgressPercent(HUNDRED);
    }

    return Option.of(vo);
  }
}
