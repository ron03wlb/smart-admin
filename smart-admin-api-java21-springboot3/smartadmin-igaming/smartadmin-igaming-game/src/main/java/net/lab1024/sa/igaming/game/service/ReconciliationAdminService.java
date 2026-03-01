package net.lab1024.sa.igaming.game.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.game.dao.ReconciliationDao;
import net.lab1024.sa.igaming.game.domain.entity.ReconciliationEntity;
import net.lab1024.sa.igaming.game.domain.form.ReconciliationTriggerForm;
import net.lab1024.sa.igaming.game.domain.vo.ReconciliationVO;
import net.lab1024.sa.igaming.game.manager.ReconciliationManager;
import org.springframework.stereotype.Service;

/**
 * Reconciliation admin service — exposes reconciliation manager operations to admin API.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconciliationAdminService {

  private final ReconciliationManager reconciliationManager;
  private final ReconciliationDao reconciliationDao;

  /**
   * Trigger daily batch reconciliation for the specified date.
   *
   * @param form trigger form containing reconciliation date
   * @return success response
   */
  public ResponseDTO<String> triggerDailyReconciliation(ReconciliationTriggerForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    reconciliationManager.dailyBatchReconciliation(form.getDate());
    log.info("Daily reconciliation triggered: date={}, tenantId={}", form.getDate(), tenantId);
    return ResponseDTO.ok();
  }

  /**
   * Trigger poll for missing settlements.
   *
   * @return number of compensated rounds
   */
  public ResponseDTO<Integer> triggerMissingSettlementPoll() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    int compensatedCount = reconciliationManager.pollMissingSettlements();
    log.info(
        "Missing settlement poll completed: compensated={}, tenantId={}",
        compensatedCount,
        tenantId);
    return ResponseDTO.ok(compensatedCount);
  }

  /**
   * Query reconciliation status for a specific date.
   *
   * @param date reconciliation date
   * @return list of reconciliation records
   */
  public ResponseDTO<List<ReconciliationVO>> queryReconciliationStatus(LocalDate date) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    List<ReconciliationEntity> entities =
        reconciliationDao.selectList(
            Wrappers.<ReconciliationEntity>lambdaQuery()
                .eq(ReconciliationEntity::getReconciliationDate, date)
                .eq(ReconciliationEntity::getTenantId, tenantId)
                .eq(ReconciliationEntity::getDeleted, false));

    List<ReconciliationVO> voList = SmartBeanUtil.copyList(entities, ReconciliationVO.class);
    return ResponseDTO.ok(voList);
  }
}
