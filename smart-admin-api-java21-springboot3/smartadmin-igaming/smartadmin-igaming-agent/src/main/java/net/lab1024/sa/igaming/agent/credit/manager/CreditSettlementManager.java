package net.lab1024.sa.igaming.agent.credit.manager;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.agent.credit.dao.AgentCreditDao;
import net.lab1024.sa.igaming.agent.credit.dao.CreditAllocationAuditDao;
import net.lab1024.sa.igaming.agent.credit.dao.SettlementRecordDao;
import net.lab1024.sa.igaming.agent.credit.domain.entity.AgentCreditEntity;
import net.lab1024.sa.igaming.agent.credit.domain.entity.CreditAllocationAuditEntity;
import net.lab1024.sa.igaming.agent.credit.domain.entity.SettlementRecordEntity;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.common.constant.PaymentVerifyStatusEnum;
import net.lab1024.sa.igaming.common.constant.SettlementPhaseEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credit Settlement Manager — handles transactional credit operations.
 *
 * <p>All methods that modify credit data must be annotated with {@code @Transactional}. The Manager
 * layer owns the transaction boundary per SmartAdmin architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditSettlementManager {

  private final AgentCreditDao agentCreditDao;
  private final SettlementRecordDao settlementRecordDao;
  private final CreditAllocationAuditDao creditAllocationAuditDao;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Execute credit allocation from parent to child agent.
   *
   * @param parentId parent agent ID
   * @param childId child agent ID
   * @param amount amount to allocate
   * @param positionPct position percent for child (nullable)
   * @param tenantId tenant ID
   * @param operator current operator
   * @param reason allocation reason
   */
  @Transactional(rollbackFor = Throwable.class)
  public void executeAllocation(
      Long parentId,
      Long childId,
      BigDecimal amount,
      BigDecimal positionPct,
      Long tenantId,
      String operator,
      String reason) {

    // Update parent: increase allocated_to_children
    AgentCreditEntity parent = agentCreditDao.findByAgentIdAndTenantId(parentId, tenantId);
    parent.setAllocatedToChildren(parent.getAllocatedToChildren().add(amount));
    agentCreditDao.updateById(parent);

    // Update or create child credit record
    AgentCreditEntity child = agentCreditDao.findByAgentIdAndTenantId(childId, tenantId);
    BigDecimal oldLimit;
    BigDecimal oldPosition;
    if (child == null) {
      child = new AgentCreditEntity();
      child.setAgentId(childId);
      child.setParentId(parentId);
      child.setTenantId(tenantId);
      child.setCreditLimit(amount);
      child.setUsedCredit(BigDecimal.ZERO);
      child.setAllocatedToChildren(BigDecimal.ZERO);
      child.setPositionPercent(positionPct != null ? positionPct : BigDecimal.ZERO);
      child.setMaxPosition(new BigDecimal("100.0000"));
      child.setStatus(1);
      child.setDeleted(false);
      child.setVersion(0);
      oldLimit = BigDecimal.ZERO;
      oldPosition = BigDecimal.ZERO;
      agentCreditDao.insert(child);
    } else {
      oldLimit = child.getCreditLimit();
      oldPosition = child.getPositionPercent();
      child.setCreditLimit(child.getCreditLimit().add(amount));
      if (positionPct != null) {
        child.setPositionPercent(positionPct);
      }
      agentCreditDao.updateById(child);
    }

    // Insert immutable audit record
    CreditAllocationAuditEntity audit = new CreditAllocationAuditEntity();
    audit.setTenantId(tenantId);
    audit.setParentId(parentId);
    audit.setChildId(childId);
    audit.setOldLimit(oldLimit);
    audit.setNewLimit(child.getCreditLimit());
    audit.setDelta(amount);
    audit.setOldPosition(oldPosition);
    audit.setNewPosition(child.getPositionPercent());
    audit.setReason(reason);
    audit.setOperator(operator);
    creditAllocationAuditDao.insert(audit);

    log.info(
        "Credit allocated: parent={}, child={}, amount={}, newLimit={}",
        parentId,
        childId,
        amount,
        child.getCreditLimit());

    publishAgentEvent(
        DomainEventTypeConst.CREDIT_ALLOCATED,
        "AgentCredit",
        String.valueOf(childId),
        buildAllocatePayload(parentId, childId, amount, positionPct));
  }

  /**
   * Execute credit recall — reduce child's credit limit.
   *
   * @param parentId parent agent ID
   * @param childId child agent ID
   * @param newLimit new credit limit for child
   * @param tenantId tenant ID
   * @param operator current operator
   * @param reason recall reason
   */
  @Transactional(rollbackFor = Throwable.class)
  public void executeRecall(
      Long parentId,
      Long childId,
      BigDecimal newLimit,
      Long tenantId,
      String operator,
      String reason) {

    AgentCreditEntity child = agentCreditDao.findByAgentIdAndTenantId(childId, tenantId);
    BigDecimal oldLimit = child.getCreditLimit();
    BigDecimal delta = oldLimit.subtract(newLimit);

    // Update child limit
    child.setCreditLimit(newLimit);
    agentCreditDao.updateById(child);

    // Return delta to parent's allocated pool
    AgentCreditEntity parent = agentCreditDao.findByAgentIdAndTenantId(parentId, tenantId);
    parent.setAllocatedToChildren(parent.getAllocatedToChildren().subtract(delta));
    agentCreditDao.updateById(parent);

    // Insert immutable audit record
    CreditAllocationAuditEntity audit = new CreditAllocationAuditEntity();
    audit.setTenantId(tenantId);
    audit.setParentId(parentId);
    audit.setChildId(childId);
    audit.setOldLimit(oldLimit);
    audit.setNewLimit(newLimit);
    audit.setDelta(delta.negate());
    audit.setOldPosition(child.getPositionPercent());
    audit.setNewPosition(child.getPositionPercent());
    audit.setReason(reason);
    audit.setOperator(operator);
    creditAllocationAuditDao.insert(audit);

    log.info(
        "Credit recalled: parent={}, child={}, oldLimit={}, newLimit={}",
        parentId,
        childId,
        oldLimit,
        newLimit);

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("parentId", parentId);
    node.put("childId", childId);
    node.put("newLimit", newLimit.toPlainString());
    node.put("delta", delta.negate().toPlainString());
    publishAgentEvent(
        DomainEventTypeConst.CREDIT_RECALLED, "AgentCredit", String.valueOf(childId), node);
  }

  /**
   * Trigger weekly settlement for all agents in a tenant.
   *
   * @param tenantId tenant ID
   * @param settlementWeek week identifier (e.g. 2026-W09)
   * @return created settlement records
   */
  @Transactional(rollbackFor = Throwable.class)
  public List<SettlementRecordEntity> triggerWeeklySettlement(
      Long tenantId, String settlementWeek) {
    // Find all agent credits for this tenant (top-level agents have null parentId)
    // For simplicity, process all agents
    List<AgentCreditEntity> agents = agentCreditDao.findByParentIdAndTenantId(null, tenantId);
    List<SettlementRecordEntity> results = new ArrayList<>();

    for (AgentCreditEntity agent : agents) {
      SettlementRecordEntity existing =
          settlementRecordDao.findByAgentAndWeek(agent.getAgentId(), settlementWeek, tenantId);
      if (existing != null) {
        continue;
      }

      SettlementRecordEntity record = new SettlementRecordEntity();
      record.setTenantId(tenantId);
      record.setAgentId(agent.getAgentId());
      record.setParentId(agent.getParentId());
      record.setSettlementWeek(settlementWeek);
      record.setSettlementPhase(SettlementPhaseEnum.FREEZE_CALCULATE.getValue());
      record.setPlayerLoss(agent.getUsedCredit());
      record.setOwnShare(
          agent
              .getUsedCredit()
              .multiply(agent.getPositionPercent())
              .divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
      record.setToParent(agent.getUsedCredit().subtract(record.getOwnShare()));
      record.setToPlatform(BigDecimal.ZERO);
      record.setPaymentStatus(PaymentVerifyStatusEnum.PENDING.getValue());
      settlementRecordDao.insert(record);
      results.add(record);
    }

    log.info(
        "Weekly settlement triggered: tenantId={}, week={}, records={}",
        tenantId,
        settlementWeek,
        results.size());

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("tenantId", tenantId);
    node.put("settlementWeek", settlementWeek);
    node.put("recordCount", results.size());
    publishAgentEvent(
        DomainEventTypeConst.SETTLEMENT_COMPLETED, "Settlement", settlementWeek, node);

    return results;
  }

  /**
   * Verify a settlement payment proof.
   *
   * @param settlementRecordId settlement record ID
   * @param txnId payment transaction ID
   */
  @Transactional(rollbackFor = Throwable.class)
  public void verifyPayment(Long settlementRecordId, String txnId) {
    SettlementRecordEntity record = settlementRecordDao.selectById(settlementRecordId);
    record.setPaymentStatus(PaymentVerifyStatusEnum.VERIFIED.getValue());
    record.setPaymentTxnId(txnId);
    record.setVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
    settlementRecordDao.updateById(record);

    log.info(
        "Payment verified: recordId={}, txnId={}, agentId={}",
        settlementRecordId,
        txnId,
        record.getAgentId());

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("settlementRecordId", settlementRecordId);
    node.put("txnId", txnId);
    node.put("agentId", record.getAgentId());
    publishAgentEvent(
        DomainEventTypeConst.PAYMENT_VERIFIED,
        "Settlement",
        String.valueOf(settlementRecordId),
        node);
  }

  private ObjectNode buildAllocatePayload(
      Long parentId, Long childId, BigDecimal amount, BigDecimal positionPct) {
    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("parentId", parentId);
    node.put("childId", childId);
    node.put("amount", amount.toPlainString());
    if (positionPct != null) {
      node.put("positionPercent", positionPct.toPlainString());
    }
    return node;
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishAgentEvent(
      String eventType, String aggregateType, String aggregateId, ObjectNode payload) {
    domainEventPublisher.publish(
        IgamingKafkaConst.Topic.AGENT_EVENTS,
        DomainEvent.builder()
            .eventType(eventType)
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .payload(payload)
            .build());
  }
}
