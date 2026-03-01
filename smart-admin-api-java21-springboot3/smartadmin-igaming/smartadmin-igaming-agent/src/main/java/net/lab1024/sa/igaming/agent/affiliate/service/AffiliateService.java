package net.lab1024.sa.igaming.agent.affiliate.service;

import io.vavr.control.Option;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateAgentDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionPlanDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionRecordDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateHierarchyDao;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAgentEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionPlanEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateHierarchyEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.AffiliateRegisterForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionApprovalForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.AffiliateAgentVO;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.manager.AffiliateCommissionManager;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import org.springframework.stereotype.Service;

/**
 * Affiliate Service — handles agent registration, hierarchy query, and commission operations.
 *
 * <p>Delegates transactional operations to {@link AffiliateCommissionManager}.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
public class AffiliateService {

  private final AffiliateAgentDao affiliateAgentDao;
  private final AffiliateHierarchyDao affiliateHierarchyDao;
  private final AffiliateCommissionPlanDao affiliateCommissionPlanDao;
  private final AffiliateCommissionRecordDao affiliateCommissionRecordDao;
  private final AffiliateCommissionManager affiliateCommissionManager;

  /**
   * Register a new affiliate agent.
   *
   * @param form registration form
   * @return created agent VO
   */
  public ResponseDTO<AffiliateAgentVO> registerAgent(AffiliateRegisterForm form) {
    // Check username uniqueness
    AffiliateAgentEntity existing =
        affiliateAgentDao.findByUsernameAndTenantId(form.getUsername(), form.getTenantId());
    if (existing != null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.AGENT_USERNAME_DUPLICATE.getMsg());
    }

    AffiliateAgentEntity entity = affiliateCommissionManager.createAgentWithHierarchy(form);
    AffiliateAgentVO vo = SmartBeanUtil.copy(entity, AffiliateAgentVO.class);
    return ResponseDTO.ok(vo);
  }

  /**
   * Get agent by ID.
   *
   * @param agentId agent ID
   * @return agent VO
   */
  public ResponseDTO<AffiliateAgentVO> getAgentById(Long agentId) {
    Long tenantId = TenantContext.getTenantId();
    return Option.of(affiliateAgentDao.selectById(agentId))
        .filter(e -> tenantId.equals(e.getTenantId()))
        .map(e -> SmartBeanUtil.copy(e, AffiliateAgentVO.class))
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(AgentErrorCode.AGENT_NOT_FOUND.getMsg()));
  }

  /**
   * Get downline tree for an agent (via closure table).
   *
   * @param agentId ancestor agent ID
   * @return list of descendant agent VOs
   */
  public ResponseDTO<List<AffiliateAgentVO>> getDownlineTree(Long agentId) {
    Long tenantId = TenantContext.getTenantId();
    List<AffiliateHierarchyEntity> descendants =
        affiliateHierarchyDao.findDescendants(agentId, tenantId);
    List<Long> descendantIds =
        descendants.stream()
            .filter(h -> h.getDepth() > 0)
            .map(AffiliateHierarchyEntity::getDescendantId)
            .toList();

    if (descendantIds.isEmpty()) {
      return ResponseDTO.ok(List.of());
    }

    List<AffiliateAgentEntity> agents = affiliateAgentDao.selectBatchIds(descendantIds);
    List<AffiliateAgentVO> vos =
        agents.stream().map(e -> SmartBeanUtil.copy(e, AffiliateAgentVO.class)).toList();
    return ResponseDTO.ok(vos);
  }

  /**
   * Get pending commission records for a tenant.
   *
   * @return list of pending commission record VOs
   */
  public ResponseDTO<List<CommissionRecordVO>> getPendingApprovals() {
    Long tenantId = TenantContext.getTenantId();
    List<AffiliateCommissionRecordEntity> records =
        affiliateCommissionRecordDao.findPendingByTenantId(tenantId);
    List<CommissionRecordVO> vos =
        records.stream().map(e -> SmartBeanUtil.copy(e, CommissionRecordVO.class)).toList();
    return ResponseDTO.ok(vos);
  }

  /**
   * Approve a commission record.
   *
   * @param form approval form
   * @return success or error response
   */
  public ResponseDTO<String> approveCommission(CommissionApprovalForm form) {
    AffiliateCommissionRecordEntity record =
        affiliateCommissionRecordDao.selectById(form.getRecordId());
    if (record == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.COMMISSION_RECORD_NOT_FOUND.getMsg());
    }

    affiliateCommissionManager.approveCommission(form);
    return ResponseDTO.ok();
  }

  /**
   * Reject a commission record.
   *
   * @param form approval form
   * @return success or error response
   */
  public ResponseDTO<String> rejectCommission(CommissionApprovalForm form) {
    AffiliateCommissionRecordEntity record =
        affiliateCommissionRecordDao.selectById(form.getRecordId());
    if (record == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.COMMISSION_RECORD_NOT_FOUND.getMsg());
    }

    affiliateCommissionManager.rejectCommission(form);
    return ResponseDTO.ok();
  }

  /**
   * Get a commission plan by ID.
   *
   * @param planId plan ID
   * @return plan entity or error
   */
  public ResponseDTO<AffiliateCommissionPlanEntity> getCommissionPlan(Long planId) {
    return Option.of(affiliateCommissionPlanDao.selectById(planId))
        .map(ResponseDTO::ok)
        .getOrElse(
            () -> ResponseDTO.userErrorParam(AgentErrorCode.COMMISSION_PLAN_NOT_FOUND.getMsg()));
  }
}
