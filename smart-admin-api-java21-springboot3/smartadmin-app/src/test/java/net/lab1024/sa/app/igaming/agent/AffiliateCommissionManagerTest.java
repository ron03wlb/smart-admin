package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import net.lab1024.sa.igaming.agent.affiliate.manager.AffiliateCommissionManager;
import net.lab1024.sa.igaming.common.constant.AgentStatusEnum;
import net.lab1024.sa.igaming.common.constant.CommissionPlanTypeEnum;
import net.lab1024.sa.igaming.common.constant.CommissionRecordStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AffiliateCommissionManager 測試")
class AffiliateCommissionManagerTest {

  @Mock private AffiliateAgentDao affiliateAgentDao;
  @Mock private AffiliateHierarchyDao affiliateHierarchyDao;
  @Mock private AffiliateCommissionPlanDao affiliateCommissionPlanDao;
  @Mock private AffiliateCommissionRecordDao affiliateCommissionRecordDao;
  @Mock private AffiliateAdjustmentDao affiliateAdjustmentDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  private AffiliateCommissionManager affiliateCommissionManager;

  @BeforeEach
  void setUp() {
    affiliateCommissionManager =
        new AffiliateCommissionManager(
            affiliateAgentDao,
            affiliateHierarchyDao,
            affiliateCommissionPlanDao,
            affiliateCommissionRecordDao,
            affiliateAdjustmentDao,
            Optional.of(domainEventPublisher));
  }

  private static final Long TENANT_ID = 1L;

  // ========== createAgentWithHierarchy ==========

  @Nested
  @DisplayName("createAgentWithHierarchy 測試")
  class CreateAgentWithHierarchyTest {

    @Test
    @DisplayName("建立頂級代理")
    void create_top_level_agent() {
      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("top_agent");
      form.setTenantId(TENANT_ID);
      form.setReferralCode("REF001");

      AffiliateAgentEntity result = affiliateCommissionManager.createAgentWithHierarchy(form);

      ArgumentCaptor<AffiliateAgentEntity> agentCaptor =
          ArgumentCaptor.forClass(AffiliateAgentEntity.class);
      verify(affiliateAgentDao).insert(agentCaptor.capture());
      AffiliateAgentEntity saved = agentCaptor.getValue();
      assertThat(saved.getUsername()).isEqualTo("top_agent");
      assertThat(saved.getAgentLevel()).isEqualTo(1);
      assertThat(saved.getHierarchyPath()).isEqualTo("/");
      assertThat(saved.getStatus()).isEqualTo(AgentStatusEnum.ACTIVE.getValue());

      // Self-reference in closure table
      ArgumentCaptor<AffiliateHierarchyEntity> hierCaptor =
          ArgumentCaptor.forClass(AffiliateHierarchyEntity.class);
      verify(affiliateHierarchyDao).insert(hierCaptor.capture());
      AffiliateHierarchyEntity self = hierCaptor.getValue();
      assertThat(self.getDepth()).isEqualTo(0);

      // Event published
      ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), eventCaptor.capture());
      DomainEvent event = eventCaptor.getValue();
      assertThat(event.getEventType()).isEqualTo("AGENT_REGISTERED");
      assertThat(event.getAggregateType()).isEqualTo("AffiliateAgent");
    }

    @Test
    @DisplayName("建立子代理 — 繼承父級路徑和層級")
    void create_child_agent_inherits_hierarchy() {
      AffiliateAgentEntity parent = new AffiliateAgentEntity();
      parent.setAgentId(100L);
      parent.setTenantId(TENANT_ID);
      parent.setAgentLevel(1);
      parent.setHierarchyPath("/");
      when(affiliateAgentDao.selectById(100L)).thenReturn(parent);

      AffiliateHierarchyEntity parentSelf = new AffiliateHierarchyEntity();
      parentSelf.setAncestorId(100L);
      parentSelf.setDescendantId(100L);
      parentSelf.setDepth(0);
      when(affiliateHierarchyDao.findAncestors(100L, TENANT_ID)).thenReturn(List.of(parentSelf));

      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("child_agent");
      form.setParentAgentId(100L);
      form.setTenantId(TENANT_ID);

      affiliateCommissionManager.createAgentWithHierarchy(form);

      ArgumentCaptor<AffiliateAgentEntity> agentCaptor =
          ArgumentCaptor.forClass(AffiliateAgentEntity.class);
      verify(affiliateAgentDao).insert(agentCaptor.capture());
      AffiliateAgentEntity saved = agentCaptor.getValue();
      assertThat(saved.getAgentLevel()).isEqualTo(2);
      assertThat(saved.getHierarchyPath()).isEqualTo("/100/");
      assertThat(saved.getParentAgentId()).isEqualTo(100L);

      // 2 closure entries: self + parent→child
      ArgumentCaptor<AffiliateHierarchyEntity> hierCaptor =
          ArgumentCaptor.forClass(AffiliateHierarchyEntity.class);
      verify(affiliateHierarchyDao, times(2)).insert(hierCaptor.capture());
      List<AffiliateHierarchyEntity> entries = hierCaptor.getAllValues();
      assertThat(entries).hasSize(2);
      assertThat(entries.get(0).getDepth()).isEqualTo(0); // self
      assertThat(entries.get(1).getDepth()).isEqualTo(1); // parent→child
    }
  }

  // ========== calculateAndIssueCommission ==========

  @Nested
  @DisplayName("calculateAndIssueCommission 測試")
  class CalculateAndIssueCommissionTest {

    private AffiliateCommissionPlanEntity buildPlan(Integer planType) {
      AffiliateCommissionPlanEntity plan = new AffiliateCommissionPlanEntity();
      plan.setPlanId(10L);
      plan.setPlanType(planType);
      plan.setNegativeCarryover(false);
      return plan;
    }

    @Test
    @DisplayName("Revenue Share 佣金計算 (30%)")
    void revenue_share_calculation() {
      AffiliateAgentEntity agent = new AffiliateAgentEntity();
      agent.setAgentId(100L);
      agent.setCommissionPlanId(10L);
      when(affiliateAgentDao.selectById(100L)).thenReturn(agent);
      when(affiliateCommissionPlanDao.selectById(10L))
          .thenReturn(buildPlan(CommissionPlanTypeEnum.REVENUE_SHARE.getValue()));

      AffiliateCommissionRecordEntity result =
          affiliateCommissionManager.calculateAndIssueCommission(
              100L, TENANT_ID, LocalDate.of(2026, 2, 17), new BigDecimal("100000"));

      assertThat(result.getGrossAmount()).isEqualByComparingTo("30000.0000");
      assertThat(result.getCarryoverAmount()).isEqualByComparingTo("0");
      assertThat(result.getNetAmount()).isEqualByComparingTo("30000.0000");
      assertThat(result.getStatus()).isEqualTo(CommissionRecordStatusEnum.PENDING.getValue());
      verify(affiliateCommissionRecordDao).insert(any(AffiliateCommissionRecordEntity.class));

      // Event published
      ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), eventCaptor.capture());
      assertThat(eventCaptor.getValue().getEventType()).isEqualTo("COMMISSION_ISSUED");
    }

    @Test
    @DisplayName("Turnover Rebate 佣金計算 (0.5%)")
    void turnover_rebate_calculation() {
      AffiliateAgentEntity agent = new AffiliateAgentEntity();
      agent.setAgentId(100L);
      agent.setCommissionPlanId(10L);
      when(affiliateAgentDao.selectById(100L)).thenReturn(agent);
      when(affiliateCommissionPlanDao.selectById(10L))
          .thenReturn(buildPlan(CommissionPlanTypeEnum.TURNOVER_REBATE.getValue()));

      AffiliateCommissionRecordEntity result =
          affiliateCommissionManager.calculateAndIssueCommission(
              100L, TENANT_ID, LocalDate.of(2026, 2, 17), new BigDecimal("1000000"));

      assertThat(result.getGrossAmount()).isEqualByComparingTo("5000.0000");
      assertThat(result.getNetAmount()).isEqualByComparingTo("5000.0000");
    }

    @Test
    @DisplayName("CPA 佣金透傳")
    void cpa_pass_through() {
      AffiliateAgentEntity agent = new AffiliateAgentEntity();
      agent.setAgentId(100L);
      agent.setCommissionPlanId(10L);
      when(affiliateAgentDao.selectById(100L)).thenReturn(agent);
      when(affiliateCommissionPlanDao.selectById(10L))
          .thenReturn(buildPlan(CommissionPlanTypeEnum.CPA.getValue()));

      AffiliateCommissionRecordEntity result =
          affiliateCommissionManager.calculateAndIssueCommission(
              100L, TENANT_ID, LocalDate.of(2026, 2, 17), new BigDecimal("500"));

      assertThat(result.getGrossAmount()).isEqualByComparingTo("500");
    }

    @Test
    @DisplayName("負數結轉 — 上期虧損結轉到當期")
    void negative_carryover_from_previous_period() {
      AffiliateAgentEntity agent = new AffiliateAgentEntity();
      agent.setAgentId(100L);
      agent.setCommissionPlanId(10L);
      when(affiliateAgentDao.selectById(100L)).thenReturn(agent);

      AffiliateCommissionPlanEntity plan =
          buildPlan(CommissionPlanTypeEnum.REVENUE_SHARE.getValue());
      plan.setNegativeCarryover(true);
      when(affiliateCommissionPlanDao.selectById(10L)).thenReturn(plan);

      // Previous period had negative net
      AffiliateCommissionRecordEntity lastRecord = new AffiliateCommissionRecordEntity();
      lastRecord.setNetAmount(new BigDecimal("-5000"));
      when(affiliateCommissionRecordDao.selectList(any())).thenReturn(List.of(lastRecord));

      AffiliateCommissionRecordEntity result =
          affiliateCommissionManager.calculateAndIssueCommission(
              100L, TENANT_ID, LocalDate.of(2026, 2, 17), new BigDecimal("100000"));

      assertThat(result.getGrossAmount()).isEqualByComparingTo("30000.0000");
      assertThat(result.getCarryoverAmount()).isEqualByComparingTo("-5000");
      assertThat(result.getNetAmount()).isEqualByComparingTo("25000.0000");
    }
  }

  // ========== approveCommission ==========

  @Nested
  @DisplayName("approveCommission 測試")
  class ApproveCommissionTest {

    @Test
    @DisplayName("審核通過 — 更新狀態和代理累計佣金")
    void approve_updates_status_and_agent_total() {
      AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
      record.setRecordId(1L);
      record.setAgentId(100L);
      record.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
      record.setNetAmount(new BigDecimal("10000"));
      when(affiliateCommissionRecordDao.selectById(1L)).thenReturn(record);

      AffiliateAgentEntity agent = new AffiliateAgentEntity();
      agent.setAgentId(100L);
      agent.setTotalCommission(new BigDecimal("50000"));
      when(affiliateAgentDao.selectById(100L)).thenReturn(agent);

      CommissionApprovalForm form = new CommissionApprovalForm();
      form.setRecordId(1L);
      form.setApprovedBy("admin");

      affiliateCommissionManager.approveCommission(form);

      assertThat(record.getStatus()).isEqualTo(CommissionRecordStatusEnum.APPROVED.getValue());
      assertThat(record.getApprovedBy()).isEqualTo("admin");
      assertThat(record.getApprovedAt()).isNotNull();
      assertThat(agent.getTotalCommission()).isEqualByComparingTo("60000");
      verify(affiliateCommissionRecordDao).updateById(record);
      verify(affiliateAgentDao).updateById(agent);

      // Event published
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("負淨額不更新代理累計")
    void negative_net_does_not_update_agent_total() {
      AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
      record.setRecordId(1L);
      record.setAgentId(100L);
      record.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
      record.setNetAmount(new BigDecimal("-3000"));
      when(affiliateCommissionRecordDao.selectById(1L)).thenReturn(record);

      CommissionApprovalForm form = new CommissionApprovalForm();
      form.setRecordId(1L);
      form.setApprovedBy("admin");

      affiliateCommissionManager.approveCommission(form);

      assertThat(record.getStatus()).isEqualTo(CommissionRecordStatusEnum.APPROVED.getValue());
      verify(affiliateCommissionRecordDao).updateById(record);
      // Agent total should NOT be updated for negative net
      verify(affiliateAgentDao, times(0)).selectById(any());
    }
  }

  // ========== rejectCommission ==========

  @Nested
  @DisplayName("rejectCommission 測試")
  class RejectCommissionTest {

    @Test
    @DisplayName("拒絕設定正確狀態")
    void reject_sets_correct_status() {
      AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
      record.setRecordId(1L);
      record.setAgentId(100L);
      record.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
      when(affiliateCommissionRecordDao.selectById(1L)).thenReturn(record);

      CommissionApprovalForm form = new CommissionApprovalForm();
      form.setRecordId(1L);
      form.setApprovedBy("admin");

      affiliateCommissionManager.rejectCommission(form);

      assertThat(record.getStatus()).isEqualTo(CommissionRecordStatusEnum.REJECTED.getValue());
      assertThat(record.getApprovedAt()).isNotNull();
      verify(affiliateCommissionRecordDao).updateById(record);

      // Event published
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), any(DomainEvent.class));
    }
  }

  // ========== createAdjustment ==========

  @Nested
  @DisplayName("createAdjustment 測試")
  class CreateAdjustmentTest {

    @Test
    @DisplayName("建立調整記錄")
    void creates_adjustment_record() {
      AffiliateAdjustmentEntity result =
          affiliateCommissionManager.createAdjustment(
              100L, TENANT_ID, 1, new BigDecimal("500"), "Late arrival", "admin");

      ArgumentCaptor<AffiliateAdjustmentEntity> captor =
          ArgumentCaptor.forClass(AffiliateAdjustmentEntity.class);
      verify(affiliateAdjustmentDao).insert(captor.capture());
      AffiliateAdjustmentEntity saved = captor.getValue();
      assertThat(saved.getAgentId()).isEqualTo(100L);
      assertThat(saved.getAdjustmentType()).isEqualTo(1);
      assertThat(saved.getAmount()).isEqualByComparingTo("500");
      assertThat(saved.getReason()).isEqualTo("Late arrival");
      assertThat(saved.getCreatedBy()).isEqualTo("admin");
    }
  }
}
