package net.lab1024.sa.igaming.agent.affiliate.service;

import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateAgentDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionRecordDao;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionAdjustmentForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionCalculateForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.manager.AffiliateCommissionManager;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import org.springframework.stereotype.Service;

/**
 * Affiliate commission admin service — admin operations for commission management.
 *
 * <p>Delegates transactional operations to {@link AffiliateCommissionManager}.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AffiliateCommissionAdminService {

  private final AffiliateAgentDao affiliateAgentDao;
  private final AffiliateCommissionRecordDao affiliateCommissionRecordDao;
  private final AffiliateCommissionManager affiliateCommissionManager;

  /**
   * Calculate and issue commission for an agent (admin trigger).
   *
   * @param form calculation form with agentId, settlementDate, grossRevenue
   * @return created commission record VO
   */
  public ResponseDTO<CommissionRecordVO> calculateCommission(CommissionCalculateForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    if (affiliateAgentDao.selectById(form.getAgentId()) == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.AGENT_NOT_FOUND.getMsg());
    }

    AffiliateCommissionRecordEntity record =
        affiliateCommissionManager.calculateAndIssueCommission(
            form.getAgentId(), tenantId, form.getSettlementDate(), form.getGrossRevenue());

    CommissionRecordVO vo = SmartBeanUtil.copy(record, CommissionRecordVO.class);
    log.info(
        "Admin triggered commission calculation: agentId={}, tenantId={}",
        form.getAgentId(),
        tenantId);
    return ResponseDTO.ok(vo);
  }

  /**
   * Create a commission adjustment (admin operation).
   *
   * @param form adjustment form with agentId, adjustmentType, amount, reason
   * @return success response
   */
  public ResponseDTO<String> createAdjustment(CommissionAdjustmentForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    if (affiliateAgentDao.selectById(form.getAgentId()) == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.AGENT_NOT_FOUND.getMsg());
    }

    affiliateCommissionManager.createAdjustment(
        form.getAgentId(),
        tenantId,
        form.getAdjustmentType(),
        form.getAmount(),
        form.getReason(),
        "admin");

    log.info(
        "Admin created adjustment: agentId={}, type={}, amount={}, tenantId={}",
        form.getAgentId(),
        form.getAdjustmentType(),
        form.getAmount(),
        tenantId);
    return ResponseDTO.ok();
  }

  /**
   * Get a single commission record by ID.
   *
   * @param recordId commission record ID
   * @return commission record VO
   */
  public ResponseDTO<CommissionRecordVO> getCommissionRecord(Long recordId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    return Option.of(affiliateCommissionRecordDao.selectById(recordId))
        .map(e -> SmartBeanUtil.copy(e, CommissionRecordVO.class))
        .map(ResponseDTO::ok)
        .getOrElse(
            () -> ResponseDTO.userErrorParam(AgentErrorCode.COMMISSION_RECORD_NOT_FOUND.getMsg()));
  }

  /**
   * List pending commission approvals for the current tenant.
   *
   * @return list of pending commission record VOs
   */
  public ResponseDTO<List<CommissionRecordVO>> listPendingApprovals() {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }

    List<AffiliateCommissionRecordEntity> records =
        affiliateCommissionRecordDao.findPendingByTenantId(tenantId);
    List<CommissionRecordVO> vos =
        records.stream().map(e -> SmartBeanUtil.copy(e, CommissionRecordVO.class)).toList();
    return ResponseDTO.ok(vos);
  }
}
