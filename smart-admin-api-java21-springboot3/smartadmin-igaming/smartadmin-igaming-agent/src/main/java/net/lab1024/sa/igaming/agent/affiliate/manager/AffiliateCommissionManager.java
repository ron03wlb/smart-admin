package net.lab1024.sa.igaming.agent.affiliate.manager;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateAdjustmentDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateAgentDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionPlanDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionRecordDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateHierarchyDao;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAdjustmentEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAgentEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionPlanEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateHierarchyEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.AffiliateRegisterForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionApprovalForm;
import net.lab1024.sa.igaming.common.constant.AgentStatusEnum;
import net.lab1024.sa.igaming.common.constant.CommissionPlanTypeEnum;
import net.lab1024.sa.igaming.common.constant.CommissionRecordStatusEnum;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Affiliate Commission Manager — handles transactional agent and commission operations.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AffiliateCommissionManager {

  private final AffiliateAgentDao affiliateAgentDao;
  private final AffiliateHierarchyDao affiliateHierarchyDao;
  private final AffiliateCommissionPlanDao affiliateCommissionPlanDao;
  private final AffiliateCommissionRecordDao affiliateCommissionRecordDao;
  private final AffiliateAdjustmentDao affiliateAdjustmentDao;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Create agent with closure table hierarchy entries.
   *
   * @param form registration form
   * @return created agent entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public AffiliateAgentEntity createAgentWithHierarchy(AffiliateRegisterForm form) {
    // Build hierarchy path and level
    int level = 1;
    String hierarchyPath = "/";
    if (form.getParentAgentId() != null) {
      AffiliateAgentEntity parent = affiliateAgentDao.selectById(form.getParentAgentId());
      if (parent != null) {
        level = parent.getAgentLevel() + 1;
        hierarchyPath = parent.getHierarchyPath() + parent.getAgentId() + "/";
      }
    }

    // Insert agent
    AffiliateAgentEntity agent = new AffiliateAgentEntity();
    agent.setTenantId(form.getTenantId());
    agent.setUsername(form.getUsername());
    agent.setParentAgentId(form.getParentAgentId());
    agent.setHierarchyPath(hierarchyPath);
    agent.setAgentLevel(level);
    agent.setCommissionPlanId(form.getCommissionPlanId());
    agent.setTotalPlayers(0);
    agent.setActivePlayers(0);
    agent.setTotalCommission(BigDecimal.ZERO);
    agent.setStatus(AgentStatusEnum.ACTIVE.getValue());
    agent.setReferralCode(form.getReferralCode());
    agent.setDeleted(false);
    agent.setVersion(0);
    affiliateAgentDao.insert(agent);

    // Insert self-reference in closure table
    AffiliateHierarchyEntity self = new AffiliateHierarchyEntity();
    self.setTenantId(form.getTenantId());
    self.setAncestorId(agent.getAgentId());
    self.setDescendantId(agent.getAgentId());
    self.setDepth(0);
    affiliateHierarchyDao.insert(self);

    // Insert ancestor links from closure table
    if (form.getParentAgentId() != null) {
      List<AffiliateHierarchyEntity> ancestors =
          affiliateHierarchyDao.findAncestors(form.getParentAgentId(), form.getTenantId());
      for (AffiliateHierarchyEntity ancestor : ancestors) {
        AffiliateHierarchyEntity link = new AffiliateHierarchyEntity();
        link.setTenantId(form.getTenantId());
        link.setAncestorId(ancestor.getAncestorId());
        link.setDescendantId(agent.getAgentId());
        link.setDepth(ancestor.getDepth() + 1);
        affiliateHierarchyDao.insert(link);
      }
    }

    log.info(
        "Agent registered: agentId={}, username={}, level={}, parent={}",
        agent.getAgentId(),
        agent.getUsername(),
        level,
        form.getParentAgentId());

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("agentId", agent.getAgentId());
    node.put("username", agent.getUsername());
    node.put("level", level);
    if (form.getParentAgentId() != null) {
      node.put("parentAgentId", form.getParentAgentId());
    }
    publishAgentEvent(
        "AGENT_REGISTERED", "AffiliateAgent", String.valueOf(agent.getAgentId()), node);

    return agent;
  }

  /**
   * Calculate and issue commission for an agent.
   *
   * @param agentId agent ID
   * @param tenantId tenant ID
   * @param settlementDate settlement date
   * @param grossRevenue gross revenue amount for calculation
   * @return created commission record
   */
  @Transactional(rollbackFor = Throwable.class)
  public AffiliateCommissionRecordEntity calculateAndIssueCommission(
      Long agentId, Long tenantId, LocalDate settlementDate, BigDecimal grossRevenue) {

    AffiliateAgentEntity agent = affiliateAgentDao.selectById(agentId);
    AffiliateCommissionPlanEntity plan =
        affiliateCommissionPlanDao.selectById(agent.getCommissionPlanId());

    BigDecimal grossAmount = calculateGrossCommission(plan, grossRevenue);

    // Check for previous period carryover (negative carryover support)
    BigDecimal carryover = BigDecimal.ZERO;
    if (Boolean.TRUE.equals(plan.getNegativeCarryover())) {
      AffiliateCommissionRecordEntity lastRecord = findLastApprovedRecord(agentId, tenantId);
      if (lastRecord != null && lastRecord.getNetAmount().compareTo(BigDecimal.ZERO) < 0) {
        carryover = lastRecord.getNetAmount();
      }
    }

    BigDecimal netAmount = grossAmount.add(carryover);

    AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
    record.setTenantId(tenantId);
    record.setAgentId(agentId);
    record.setPlanId(plan.getPlanId());
    record.setSettlementDate(settlementDate);
    record.setGrossAmount(grossAmount);
    record.setAdjustmentAmount(BigDecimal.ZERO);
    record.setCarryoverAmount(carryover);
    record.setNetAmount(netAmount);
    record.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
    affiliateCommissionRecordDao.insert(record);

    log.info(
        "Commission issued: agentId={}, gross={}, carryover={}, net={}",
        agentId,
        grossAmount,
        carryover,
        netAmount);

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("agentId", agentId);
    node.put("netAmount", netAmount.toPlainString());
    node.put("planType", plan.getPlanType());
    node.put("settlementDate", settlementDate.toString());
    publishAgentEvent("COMMISSION_ISSUED", "Commission", String.valueOf(agentId), node);

    return record;
  }

  /**
   * Approve a pending commission record.
   *
   * @param form approval form
   */
  @Transactional(rollbackFor = Throwable.class)
  public void approveCommission(CommissionApprovalForm form) {
    AffiliateCommissionRecordEntity record =
        affiliateCommissionRecordDao.selectById(form.getRecordId());
    record.setStatus(CommissionRecordStatusEnum.APPROVED.getValue());
    record.setApprovedBy(form.getApprovedBy());
    record.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    affiliateCommissionRecordDao.updateById(record);

    // Update agent total commission
    if (record.getNetAmount().compareTo(BigDecimal.ZERO) > 0) {
      AffiliateAgentEntity agent = affiliateAgentDao.selectById(record.getAgentId());
      agent.setTotalCommission(agent.getTotalCommission().add(record.getNetAmount()));
      affiliateAgentDao.updateById(agent);
    }

    log.info(
        "Commission approved: recordId={}, agentId={}", form.getRecordId(), record.getAgentId());

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("recordId", form.getRecordId());
    node.put("agentId", record.getAgentId());
    node.put("netAmount", record.getNetAmount().toPlainString());
    publishAgentEvent(
        "COMMISSION_APPROVED", "Commission", String.valueOf(form.getRecordId()), node);
  }

  /**
   * Reject a pending commission record.
   *
   * @param form approval form
   */
  @Transactional(rollbackFor = Throwable.class)
  public void rejectCommission(CommissionApprovalForm form) {
    AffiliateCommissionRecordEntity record =
        affiliateCommissionRecordDao.selectById(form.getRecordId());
    record.setStatus(CommissionRecordStatusEnum.REJECTED.getValue());
    record.setApprovedBy(form.getApprovedBy());
    record.setApprovedAt(OffsetDateTime.now(ZoneOffset.UTC));
    affiliateCommissionRecordDao.updateById(record);

    log.info(
        "Commission rejected: recordId={}, agentId={}", form.getRecordId(), record.getAgentId());

    ObjectNode node = JsonNodeFactory.instance.objectNode();
    node.put("recordId", form.getRecordId());
    node.put("agentId", record.getAgentId());
    publishAgentEvent(
        "COMMISSION_REJECTED", "Commission", String.valueOf(form.getRecordId()), node);
  }

  /**
   * Create a commission adjustment.
   *
   * @param agentId agent ID
   * @param tenantId tenant ID
   * @param adjustmentType type enum value
   * @param amount adjustment amount
   * @param reason reason
   * @param createdBy creator
   * @return created adjustment entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public AffiliateAdjustmentEntity createAdjustment(
      Long agentId,
      Long tenantId,
      Integer adjustmentType,
      BigDecimal amount,
      String reason,
      String createdBy) {

    AffiliateAdjustmentEntity adjustment = new AffiliateAdjustmentEntity();
    adjustment.setTenantId(tenantId);
    adjustment.setAgentId(agentId);
    adjustment.setAdjustmentType(adjustmentType);
    adjustment.setAmount(amount);
    adjustment.setReason(reason);
    adjustment.setCreatedBy(createdBy);
    affiliateAdjustmentDao.insert(adjustment);

    log.info("Adjustment created: agentId={}, type={}, amount={}", agentId, adjustmentType, amount);
    return adjustment;
  }

  private BigDecimal calculateGrossCommission(
      AffiliateCommissionPlanEntity plan, BigDecimal grossRevenue) {
    if (CommissionPlanTypeEnum.REVENUE_SHARE.getValue().equals(plan.getPlanType())) {
      // Simplified: use a fixed 30% rate (real implementation reads tiers_json)
      return grossRevenue.multiply(new BigDecimal("0.30")).setScale(4, RoundingMode.HALF_UP);
    } else if (CommissionPlanTypeEnum.TURNOVER_REBATE.getValue().equals(plan.getPlanType())) {
      return grossRevenue.multiply(new BigDecimal("0.005")).setScale(4, RoundingMode.HALF_UP);
    } else if (CommissionPlanTypeEnum.CPA.getValue().equals(plan.getPlanType())) {
      // CPA: fixed per acquisition, return grossRevenue as-is (represents count * rate)
      return grossRevenue;
    }
    return BigDecimal.ZERO;
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

  private AffiliateCommissionRecordEntity findLastApprovedRecord(Long agentId, Long tenantId) {
    // Use MyBatis-Plus wrapper for latest approved record
    return affiliateCommissionRecordDao
        .selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<
                    AffiliateCommissionRecordEntity>()
                .eq(AffiliateCommissionRecordEntity::getAgentId, agentId)
                .eq(AffiliateCommissionRecordEntity::getTenantId, tenantId)
                .eq(
                    AffiliateCommissionRecordEntity::getStatus,
                    CommissionRecordStatusEnum.APPROVED.getValue())
                .orderByDesc(AffiliateCommissionRecordEntity::getSettlementDate)
                .last("LIMIT 1"))
        .stream()
        .findFirst()
        .orElse(null);
  }
}
