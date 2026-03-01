package net.lab1024.sa.igaming.risk.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.risk.dao.RiskAssessmentDao;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskAssessmentEntity;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskResetAutoLockForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskAssessmentVO;
import org.springframework.stereotype.Service;

/**
 * Risk score admin service — admin operations for risk score management.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskScoreAdminService {

  private final RiskScoreDao riskScoreDao;
  private final RiskAssessmentDao riskAssessmentDao;

  /**
   * Reset auto-lock on a player's risk score profile.
   *
   * @param form reset form with playerId and reason
   * @return success response
   */
  public ResponseDTO<String> resetAutoLock(RiskResetAutoLockForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    RiskScoreEntity profile = riskScoreDao.findByPlayerIdAndTenantId(form.getPlayerId(), tenantId);
    if (profile == null) {
      return ResponseDTO.userErrorParam("Risk score profile does not exist");
    }
    if (!Boolean.TRUE.equals(profile.getAutoLocked())) {
      return ResponseDTO.userErrorParam("Player is not auto-locked");
    }

    profile.setAutoLocked(false);
    riskScoreDao.updateById(profile);

    log.info(
        "Auto-lock reset: playerId={}, reason={}, tenantId={}",
        form.getPlayerId(),
        form.getReason(),
        tenantId);
    return ResponseDTO.ok();
  }

  /**
   * Query a player's risk assessment history.
   *
   * @param playerId player ID
   * @return list of assessment VOs
   */
  public ResponseDTO<List<RiskAssessmentVO>> queryPlayerAssessments(Long playerId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    List<RiskAssessmentEntity> entities =
        riskAssessmentDao.selectList(
            Wrappers.<RiskAssessmentEntity>lambdaQuery()
                .eq(RiskAssessmentEntity::getPlayerId, playerId)
                .eq(RiskAssessmentEntity::getTenantId, tenantId)
                .orderByDesc(RiskAssessmentEntity::getCreateTime));

    List<RiskAssessmentVO> voList = SmartBeanUtil.copyList(entities, RiskAssessmentVO.class);
    return ResponseDTO.ok(voList);
  }
}
