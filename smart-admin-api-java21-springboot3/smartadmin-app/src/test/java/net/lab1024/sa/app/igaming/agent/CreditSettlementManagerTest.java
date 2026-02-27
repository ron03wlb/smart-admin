package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.agent.credit.dao.AgentCreditDao;
import net.lab1024.sa.igaming.agent.credit.dao.CreditAllocationAuditDao;
import net.lab1024.sa.igaming.agent.credit.dao.SettlementRecordDao;
import net.lab1024.sa.igaming.agent.credit.domain.entity.AgentCreditEntity;
import net.lab1024.sa.igaming.agent.credit.domain.entity.CreditAllocationAuditEntity;
import net.lab1024.sa.igaming.agent.credit.domain.entity.SettlementRecordEntity;
import net.lab1024.sa.igaming.agent.credit.manager.CreditSettlementManager;
import net.lab1024.sa.igaming.common.constant.PaymentVerifyStatusEnum;
import net.lab1024.sa.igaming.common.constant.SettlementPhaseEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditSettlementManager 測試")
class CreditSettlementManagerTest {

  @Mock private AgentCreditDao agentCreditDao;
  @Mock private SettlementRecordDao settlementRecordDao;
  @Mock private CreditAllocationAuditDao creditAllocationAuditDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private CreditSettlementManager creditSettlementManager;

  private static final Long TENANT_ID = 1L;

  private AgentCreditEntity buildCreditEntity(
      Long agentId, Long parentId, String limit, String used, String allocated) {
    AgentCreditEntity e = new AgentCreditEntity();
    e.setAgentCreditId(agentId * 10);
    e.setAgentId(agentId);
    e.setParentId(parentId);
    e.setTenantId(TENANT_ID);
    e.setCreditLimit(new BigDecimal(limit));
    e.setUsedCredit(new BigDecimal(used));
    e.setAllocatedToChildren(new BigDecimal(allocated));
    e.setPositionPercent(new BigDecimal("30.0000"));
    e.setMaxPosition(new BigDecimal("100.0000"));
    e.setStatus(1);
    e.setDeleted(false);
    e.setVersion(0);
    return e;
  }

  // ========== executeAllocation ==========

  @Nested
  @DisplayName("executeAllocation 測試")
  class ExecuteAllocationTest {

    @Test
    @DisplayName("分配給新子代理 — 建立新記錄")
    void allocate_to_new_child_creates_record() {
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000", "0", "0");
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(null);

      creditSettlementManager.executeAllocation(
          100L, 200L, new BigDecimal("10000"), new BigDecimal("30"), TENANT_ID, "admin", "Init");

      // Parent allocated should increase
      assertThat(parent.getAllocatedToChildren()).isEqualByComparingTo("10000");
      verify(agentCreditDao).updateById(parent);

      // New child inserted
      ArgumentCaptor<AgentCreditEntity> childCaptor =
          ArgumentCaptor.forClass(AgentCreditEntity.class);
      verify(agentCreditDao).insert(childCaptor.capture());
      AgentCreditEntity child = childCaptor.getValue();
      assertThat(child.getAgentId()).isEqualTo(200L);
      assertThat(child.getParentId()).isEqualTo(100L);
      assertThat(child.getCreditLimit()).isEqualByComparingTo("10000");
      assertThat(child.getPositionPercent()).isEqualByComparingTo("30");

      // Audit inserted
      ArgumentCaptor<CreditAllocationAuditEntity> auditCaptor =
          ArgumentCaptor.forClass(CreditAllocationAuditEntity.class);
      verify(creditAllocationAuditDao).insert(auditCaptor.capture());
      CreditAllocationAuditEntity audit = auditCaptor.getValue();
      assertThat(audit.getOldLimit()).isEqualByComparingTo("0");
      assertThat(audit.getNewLimit()).isEqualByComparingTo("10000");
      assertThat(audit.getDelta()).isEqualByComparingTo("10000");
      assertThat(audit.getOperator()).isEqualTo("admin");

      // Event published
      ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), eventCaptor.capture());
      DomainEvent event = eventCaptor.getValue();
      assertThat(event.getEventType()).isEqualTo("CREDIT_ALLOCATED");
      assertThat(event.getAggregateType()).isEqualTo("AgentCredit");
      assertThat(event.getAggregateId()).isEqualTo("200");
    }

    @Test
    @DisplayName("分配給已存在子代理 — 增加額度")
    void allocate_to_existing_child_increases_limit() {
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000", "0", "10000");
      AgentCreditEntity child = buildCreditEntity(200L, 100L, "10000", "3000", "0");
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(child);

      creditSettlementManager.executeAllocation(
          100L, 200L, new BigDecimal("5000"), null, TENANT_ID, "admin", "Top up");

      assertThat(parent.getAllocatedToChildren()).isEqualByComparingTo("15000");
      assertThat(child.getCreditLimit()).isEqualByComparingTo("15000");
      verify(agentCreditDao).updateById(parent);
      verify(agentCreditDao).updateById(child);
    }

    @Test
    @DisplayName("審計記錄包含完整變動資訊")
    void audit_contains_full_change_info() {
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000", "0", "0");
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(null);

      creditSettlementManager.executeAllocation(
          100L,
          200L,
          new BigDecimal("8000"),
          new BigDecimal("25"),
          TENANT_ID,
          "manager1",
          "Branch setup");

      ArgumentCaptor<CreditAllocationAuditEntity> captor =
          ArgumentCaptor.forClass(CreditAllocationAuditEntity.class);
      verify(creditAllocationAuditDao).insert(captor.capture());

      CreditAllocationAuditEntity audit = captor.getValue();
      assertThat(audit.getParentId()).isEqualTo(100L);
      assertThat(audit.getChildId()).isEqualTo(200L);
      assertThat(audit.getReason()).isEqualTo("Branch setup");
      assertThat(audit.getOperator()).isEqualTo("manager1");
      assertThat(audit.getNewPosition()).isEqualByComparingTo("25");
    }
  }

  // ========== executeRecall ==========

  @Nested
  @DisplayName("executeRecall 測試")
  class ExecuteRecallTest {

    @Test
    @DisplayName("回收成功 — 更新子代理和父代理")
    void recall_updates_both_parent_and_child() {
      AgentCreditEntity child = buildCreditEntity(200L, 100L, "20000", "5000", "0");
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000", "0", "20000");
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(child);
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);

      creditSettlementManager.executeRecall(
          100L, 200L, new BigDecimal("10000"), TENANT_ID, "admin", "Reduce");

      assertThat(child.getCreditLimit()).isEqualByComparingTo("10000");
      assertThat(parent.getAllocatedToChildren()).isEqualByComparingTo("10000");
      verify(agentCreditDao).updateById(child);
      verify(agentCreditDao).updateById(parent);
    }

    @Test
    @DisplayName("回收產生負 delta 審計記錄")
    void recall_produces_negative_delta_audit() {
      AgentCreditEntity child = buildCreditEntity(200L, 100L, "20000", "5000", "0");
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000", "0", "20000");
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(child);
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);

      creditSettlementManager.executeRecall(
          100L, 200L, new BigDecimal("12000"), TENANT_ID, "admin", "Trim");

      ArgumentCaptor<CreditAllocationAuditEntity> captor =
          ArgumentCaptor.forClass(CreditAllocationAuditEntity.class);
      verify(creditAllocationAuditDao).insert(captor.capture());
      CreditAllocationAuditEntity audit = captor.getValue();
      assertThat(audit.getOldLimit()).isEqualByComparingTo("20000");
      assertThat(audit.getNewLimit()).isEqualByComparingTo("12000");
      assertThat(audit.getDelta()).isEqualByComparingTo("-8000");

      // Event published
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), any(DomainEvent.class));
    }
  }

  // ========== triggerWeeklySettlement ==========

  @Nested
  @DisplayName("triggerWeeklySettlement 測試")
  class TriggerWeeklySettlementTest {

    @Test
    @DisplayName("為所有頂級代理建立結算紀錄")
    void creates_records_for_all_top_agents() {
      AgentCreditEntity a1 = buildCreditEntity(100L, null, "50000", "10000", "20000");
      AgentCreditEntity a2 = buildCreditEntity(101L, null, "30000", "5000", "10000");
      when(agentCreditDao.findByParentIdAndTenantId(null, TENANT_ID)).thenReturn(List.of(a1, a2));
      when(settlementRecordDao.findByAgentAndWeek(any(), any(), any())).thenReturn(null);

      List<SettlementRecordEntity> results =
          creditSettlementManager.triggerWeeklySettlement(TENANT_ID, "2026-W09");

      assertThat(results).hasSize(2);
      verify(settlementRecordDao).insert(results.get(0));
      verify(settlementRecordDao).insert(results.get(1));
    }

    @Test
    @DisplayName("結算紀錄包含正確的持倉計算")
    void settlement_has_correct_position_calculation() {
      AgentCreditEntity agent = buildCreditEntity(100L, null, "50000", "10000", "0");
      agent.setPositionPercent(new BigDecimal("40.0000"));
      when(agentCreditDao.findByParentIdAndTenantId(null, TENANT_ID)).thenReturn(List.of(agent));
      when(settlementRecordDao.findByAgentAndWeek(any(), any(), any())).thenReturn(null);

      List<SettlementRecordEntity> results =
          creditSettlementManager.triggerWeeklySettlement(TENANT_ID, "2026-W09");

      SettlementRecordEntity record = results.get(0);
      assertThat(record.getPlayerLoss()).isEqualByComparingTo("10000");
      assertThat(record.getOwnShare()).isEqualByComparingTo("4000.0000");
      assertThat(record.getToParent()).isEqualByComparingTo("6000.0000");
      assertThat(record.getSettlementPhase())
          .isEqualTo(SettlementPhaseEnum.FREEZE_CALCULATE.getValue());
      assertThat(record.getPaymentStatus()).isEqualTo(PaymentVerifyStatusEnum.PENDING.getValue());
    }

    @Test
    @DisplayName("已存在結算紀錄不重複建立")
    void skips_existing_settlement_record() {
      AgentCreditEntity agent = buildCreditEntity(100L, null, "50000", "10000", "0");
      SettlementRecordEntity existing = new SettlementRecordEntity();
      existing.setSettlementRecordId(999L);

      when(agentCreditDao.findByParentIdAndTenantId(null, TENANT_ID)).thenReturn(List.of(agent));
      when(settlementRecordDao.findByAgentAndWeek(100L, "2026-W09", TENANT_ID))
          .thenReturn(existing);

      List<SettlementRecordEntity> results =
          creditSettlementManager.triggerWeeklySettlement(TENANT_ID, "2026-W09");

      assertThat(results).isEmpty();
      verify(settlementRecordDao, never()).insert(any(SettlementRecordEntity.class));

      // Settlement event still published (with 0 records)
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), any(DomainEvent.class));
    }
  }

  // ========== verifyPayment ==========

  @Nested
  @DisplayName("verifyPayment 測試")
  class VerifyPaymentTest {

    @Test
    @DisplayName("驗證付款設定正確狀態和交易ID")
    void sets_verified_status_and_txn_id() {
      SettlementRecordEntity record = new SettlementRecordEntity();
      record.setSettlementRecordId(1L);
      record.setAgentId(100L);
      record.setPaymentStatus(PaymentVerifyStatusEnum.PENDING.getValue());
      when(settlementRecordDao.selectById(1L)).thenReturn(record);

      creditSettlementManager.verifyPayment(1L, "TXN-ABC-123");

      assertThat(record.getPaymentStatus()).isEqualTo(PaymentVerifyStatusEnum.VERIFIED.getValue());
      assertThat(record.getPaymentTxnId()).isEqualTo("TXN-ABC-123");
      assertThat(record.getVerifiedAt()).isNotNull();
      verify(settlementRecordDao).updateById(record);

      // Event published
      ArgumentCaptor<DomainEvent> eventCaptor = ArgumentCaptor.forClass(DomainEvent.class);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.AGENT_EVENTS), eventCaptor.capture());
      assertThat(eventCaptor.getValue().getEventType()).isEqualTo("PAYMENT_VERIFIED");
    }
  }
}
