package net.lab1024.sa.igaming.agent.credit.service;

import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.igaming.agent.credit.dao.AgentCreditDao;
import net.lab1024.sa.igaming.agent.credit.dao.SettlementRecordDao;
import net.lab1024.sa.igaming.agent.credit.domain.entity.AgentCreditEntity;
import net.lab1024.sa.igaming.agent.credit.domain.entity.SettlementRecordEntity;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditAllocateForm;
import net.lab1024.sa.igaming.agent.credit.domain.form.CreditRecallForm;
import net.lab1024.sa.igaming.agent.credit.domain.form.SettlementTriggerForm;
import net.lab1024.sa.igaming.agent.credit.domain.vo.AgentCreditVO;
import net.lab1024.sa.igaming.agent.credit.domain.vo.SettlementRecordVO;
import net.lab1024.sa.igaming.agent.credit.manager.CreditSettlementManager;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import org.springframework.stereotype.Service;

/**
 * Credit Network Service — handles credit allocation, recall, and query operations.
 *
 * <p>Delegates transactional operations to {@link CreditSettlementManager}.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
public class CreditNetworkService {

  private final AgentCreditDao agentCreditDao;
  private final SettlementRecordDao settlementRecordDao;
  private final CreditSettlementManager creditSettlementManager;

  /**
   * Get a single agent's credit information.
   *
   * @param agentId agent ID
   * @return agent credit VO
   */
  public ResponseDTO<AgentCreditVO> getAgentCredit(Long agentId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    return Option.of(agentCreditDao.findByAgentIdAndTenantId(agentId, tenantId))
        .map(this::toAgentCreditVO)
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam(AgentErrorCode.CREDIT_NOT_FOUND.getMsg()));
  }

  /**
   * Get all downline (children) credits for a parent agent.
   *
   * @param parentId parent agent ID
   * @return list of child agent credit VOs
   */
  public ResponseDTO<List<AgentCreditVO>> getDownlineCredits(Long parentId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    List<AgentCreditEntity> children = agentCreditDao.findByParentIdAndTenantId(parentId, tenantId);
    List<AgentCreditVO> vos = children.stream().map(this::toAgentCreditVO).toList();
    return ResponseDTO.ok(vos);
  }

  /**
   * Allocate credit from parent to child agent.
   *
   * @param form allocation form
   * @param operator current operator name
   * @return success or error response
   */
  public ResponseDTO<String> allocateCredit(CreditAllocateForm form, String operator) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    AgentCreditEntity parent =
        agentCreditDao.findByAgentIdAndTenantId(form.getParentId(), tenantId);
    if (parent == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.CREDIT_NOT_FOUND.getMsg());
    }

    BigDecimal available =
        parent
            .getCreditLimit()
            .subtract(parent.getUsedCredit())
            .subtract(parent.getAllocatedToChildren());
    if (available.compareTo(form.getAmount()) < 0) {
      return ResponseDTO.userErrorParam(AgentErrorCode.CREDIT_INSUFFICIENT.getMsg());
    }

    creditSettlementManager.executeAllocation(
        form.getParentId(),
        form.getChildId(),
        form.getAmount(),
        form.getPositionPercent(),
        tenantId,
        operator,
        form.getReason());
    return ResponseDTO.ok();
  }

  /**
   * Recall (reduce) credit from a child agent.
   *
   * @param form recall form
   * @param operator current operator name
   * @return success or error response
   */
  public ResponseDTO<String> reclaimCredit(CreditRecallForm form, String operator) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    AgentCreditEntity child = agentCreditDao.findByAgentIdAndTenantId(form.getChildId(), tenantId);
    if (child == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.CREDIT_NOT_FOUND.getMsg());
    }

    if (child.getUsedCredit().compareTo(form.getNewLimit()) > 0) {
      return ResponseDTO.userErrorParam(AgentErrorCode.CREDIT_RECALL_EXCEEDS_USED.getMsg());
    }

    creditSettlementManager.executeRecall(
        form.getParentId(),
        form.getChildId(),
        form.getNewLimit(),
        tenantId,
        operator,
        form.getReason());
    return ResponseDTO.ok();
  }

  /**
   * Trigger weekly settlement for all agents in a tenant.
   *
   * @param form settlement trigger form
   * @return list of settlement record VOs
   */
  public ResponseDTO<List<SettlementRecordVO>> triggerSettlement(SettlementTriggerForm form) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    List<SettlementRecordEntity> records =
        creditSettlementManager.triggerWeeklySettlement(tenantId, form.getSettlementWeek());
    List<SettlementRecordVO> vos =
        records.stream().map(e -> SmartBeanUtil.copy(e, SettlementRecordVO.class)).toList();
    return ResponseDTO.ok(vos);
  }

  /**
   * Verify a settlement payment.
   *
   * @param settlementRecordId settlement record ID
   * @param txnId payment transaction ID
   * @return success or error response
   */
  public ResponseDTO<String> verifyPayment(Long settlementRecordId, String txnId) {
    Long tenantId = TenantContext.getTenantId();
    if (tenantId == null) {
      return ResponseDTO.userErrorParam("Tenant context not initialized");
    }
    SettlementRecordEntity record = settlementRecordDao.selectById(settlementRecordId);
    if (record == null) {
      return ResponseDTO.userErrorParam(AgentErrorCode.SETTLEMENT_NOT_FOUND.getMsg());
    }

    creditSettlementManager.verifyPayment(settlementRecordId, txnId);
    return ResponseDTO.ok();
  }

  private AgentCreditVO toAgentCreditVO(AgentCreditEntity entity) {
    AgentCreditVO vo = SmartBeanUtil.copy(entity, AgentCreditVO.class);
    vo.setAvailableCredit(
        entity
            .getCreditLimit()
            .subtract(entity.getUsedCredit())
            .subtract(entity.getAllocatedToChildren()));
    return vo;
  }
}
