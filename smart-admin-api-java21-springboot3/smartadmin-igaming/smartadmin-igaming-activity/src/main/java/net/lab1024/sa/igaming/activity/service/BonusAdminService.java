package net.lab1024.sa.igaming.activity.service;

import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import net.lab1024.sa.igaming.activity.domain.form.BonusForfeitForm;
import net.lab1024.sa.igaming.activity.domain.form.WageringProgressQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.PlayerBonusRecordVO;
import net.lab1024.sa.igaming.activity.manager.BonusLifecycleManager;
import net.lab1024.sa.igaming.common.constant.BonusRecordStatusEnum;
import org.springframework.stereotype.Service;

/**
 * Bonus admin service — admin operations for bonus lifecycle management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BonusAdminService {

  private final PlayerBonusRecordDao playerBonusRecordDao;
  private final BonusLifecycleManager bonusLifecycleManager;

  /**
   * Query bonus records with pagination (admin view).
   *
   * @param form query form with optional playerId and status filters
   * @return paginated bonus records
   */
  public ResponseDTO<PageResult<PlayerBonusRecordVO>> queryBonusRecords(
      WageringProgressQueryForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    var page = SmartPageUtil.convert2PageQuery(form);
    var list = playerBonusRecordDao.queryPage(page, form);
    PageResult<PlayerBonusRecordVO> pageResult = SmartPageUtil.convert2PageResult(page, list);
    return ResponseDTO.ok(pageResult);
  }

  /**
   * Get a single bonus record by ID.
   *
   * @param recordId bonus record ID
   * @return bonus record VO
   */
  public ResponseDTO<PlayerBonusRecordVO> getBonusDetail(Long recordId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    return Option.of(playerBonusRecordDao.selectById(recordId))
        .map(e -> SmartBeanUtil.copy(e, PlayerBonusRecordVO.class))
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("Bonus record does not exist"));
  }

  /**
   * Forfeit an active bonus record.
   *
   * @param form forfeit form with recordId and reason
   * @return success response
   */
  public ResponseDTO<String> forfeitBonus(BonusForfeitForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(form.getRecordId());
    if (record == null) {
      return ResponseDTO.userErrorParam("Bonus record does not exist");
    }
    if (!BonusRecordStatusEnum.ACTIVE.getValue().equals(record.getStatus())) {
      return ResponseDTO.userErrorParam("Only ACTIVE bonuses can be forfeited");
    }

    bonusLifecycleManager.forfeitBonus(form.getRecordId());

    log.info(
        "Bonus forfeited by admin: recordId={}, reason={}, tenantId={}",
        form.getRecordId(),
        form.getReason(),
        tenantId);
    return ResponseDTO.ok();
  }

  /**
   * Trigger batch expiration of overdue bonuses.
   *
   * @param batchSize maximum records to process
   * @return number of expired records
   */
  public ResponseDTO<Integer> triggerExpireBatch(int batchSize) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    int expiredCount = bonusLifecycleManager.expireActiveBonuses(batchSize);
    log.info("Batch expiration triggered: expired={}, tenantId={}", expiredCount, tenantId);
    return ResponseDTO.ok(expiredCount);
  }
}
