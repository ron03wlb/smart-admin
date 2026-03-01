package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
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
import net.lab1024.sa.igaming.agent.credit.service.CreditNetworkService;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreditNetworkService 測試")
class CreditNetworkServiceTest {

  @Mock private AgentCreditDao agentCreditDao;
  @Mock private SettlementRecordDao settlementRecordDao;
  @Mock private CreditSettlementManager creditSettlementManager;
  @InjectMocks private CreditNetworkService creditNetworkService;

  private static final Long TENANT_ID = 1L;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  private AgentCreditEntity buildCreditEntity(Long agentId, Long parentId, String limit) {
    AgentCreditEntity e = new AgentCreditEntity();
    e.setAgentCreditId(agentId * 10);
    e.setAgentId(agentId);
    e.setParentId(parentId);
    e.setCreditLimit(new BigDecimal(limit));
    e.setUsedCredit(BigDecimal.ZERO);
    e.setAllocatedToChildren(BigDecimal.ZERO);
    e.setPositionPercent(new BigDecimal("30.0000"));
    e.setMaxPosition(new BigDecimal("100.0000"));
    e.setStatus(1);
    e.setDeleted(false);
    e.setVersion(0);
    return e;
  }

  // ========== getAgentCredit ==========

  @Nested
  @DisplayName("getAgentCredit 測試")
  class GetAgentCreditTest {

    @Test
    @DisplayName("查詢成功返回 VO")
    void found_returns_vo() {
      AgentCreditEntity entity = buildCreditEntity(100L, null, "50000");
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(entity);

      ResponseDTO<AgentCreditVO> result = creditNetworkService.getAgentCredit(100L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getAgentId()).isEqualTo(100L);
      assertThat(result.getData().getCreditLimit()).isEqualByComparingTo("50000");
      assertThat(result.getData().getAvailableCredit()).isEqualByComparingTo("50000");
    }

    @Test
    @DisplayName("查詢不存在返回錯誤")
    void not_found_returns_error() {
      when(agentCreditDao.findByAgentIdAndTenantId(999L, TENANT_ID)).thenReturn(null);

      ResponseDTO<AgentCreditVO> result = creditNetworkService.getAgentCredit(999L);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.CREDIT_NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("可用額度 = 限額 - 已用 - 已分配")
    void available_credit_calculated_correctly() {
      AgentCreditEntity entity = buildCreditEntity(100L, null, "50000");
      entity.setUsedCredit(new BigDecimal("10000"));
      entity.setAllocatedToChildren(new BigDecimal("20000"));
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(entity);

      ResponseDTO<AgentCreditVO> result = creditNetworkService.getAgentCredit(100L);

      assertThat(result.getData().getAvailableCredit()).isEqualByComparingTo("20000");
    }
  }

  // ========== getDownlineCredits ==========

  @Nested
  @DisplayName("getDownlineCredits 測試")
  class GetDownlineCreditsTest {

    @Test
    @DisplayName("返回下級代理列表")
    void returns_children_list() {
      AgentCreditEntity c1 = buildCreditEntity(200L, 100L, "10000");
      AgentCreditEntity c2 = buildCreditEntity(201L, 100L, "20000");
      when(agentCreditDao.findByParentIdAndTenantId(100L, TENANT_ID)).thenReturn(List.of(c1, c2));

      ResponseDTO<List<AgentCreditVO>> result = creditNetworkService.getDownlineCredits(100L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("無下級返回空列表")
    void no_children_returns_empty() {
      when(agentCreditDao.findByParentIdAndTenantId(100L, TENANT_ID)).thenReturn(List.of());

      ResponseDTO<List<AgentCreditVO>> result = creditNetworkService.getDownlineCredits(100L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEmpty();
    }
  }

  // ========== allocateCredit ==========

  @Nested
  @DisplayName("allocateCredit 測試")
  class AllocateCreditTest {

    @Test
    @DisplayName("分配成功委託 Manager")
    void successful_allocation_delegates_to_manager() {
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000");
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);

      CreditAllocateForm form = new CreditAllocateForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setAmount(new BigDecimal("10000"));
      form.setPositionPercent(new BigDecimal("30"));
      form.setTenantId(TENANT_ID);
      form.setReason("Initial allocation");

      ResponseDTO<String> result = creditNetworkService.allocateCredit(form, "admin");

      assertThat(result.getOk()).isTrue();
      verify(creditSettlementManager)
          .executeAllocation(
              100L,
              200L,
              new BigDecimal("10000"),
              new BigDecimal("30"),
              TENANT_ID,
              "admin",
              "Initial allocation");
    }

    @Test
    @DisplayName("父代理不存在返回錯誤")
    void parent_not_found_returns_error() {
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(null);

      CreditAllocateForm form = new CreditAllocateForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setAmount(new BigDecimal("10000"));
      form.setTenantId(TENANT_ID);

      ResponseDTO<String> result = creditNetworkService.allocateCredit(form, "admin");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.CREDIT_NOT_FOUND.getMsg());
      verify(creditSettlementManager, never())
          .executeAllocation(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("額度不足返回錯誤")
    void insufficient_credit_returns_error() {
      AgentCreditEntity parent = buildCreditEntity(100L, null, "50000");
      parent.setUsedCredit(new BigDecimal("30000"));
      parent.setAllocatedToChildren(new BigDecimal("15000"));
      when(agentCreditDao.findByAgentIdAndTenantId(100L, TENANT_ID)).thenReturn(parent);

      CreditAllocateForm form = new CreditAllocateForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setAmount(new BigDecimal("10000"));
      form.setTenantId(TENANT_ID);

      ResponseDTO<String> result = creditNetworkService.allocateCredit(form, "admin");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.CREDIT_INSUFFICIENT.getMsg());
    }
  }

  // ========== reclaimCredit ==========

  @Nested
  @DisplayName("reclaimCredit 測試")
  class ReclaimCreditTest {

    @Test
    @DisplayName("回收成功委託 Manager")
    void successful_recall_delegates_to_manager() {
      AgentCreditEntity child = buildCreditEntity(200L, 100L, "20000");
      child.setUsedCredit(new BigDecimal("5000"));
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(child);

      CreditRecallForm form = new CreditRecallForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setNewLimit(new BigDecimal("10000"));
      form.setTenantId(TENANT_ID);
      form.setReason("Reduce allocation");

      ResponseDTO<String> result = creditNetworkService.reclaimCredit(form, "admin");

      assertThat(result.getOk()).isTrue();
      verify(creditSettlementManager)
          .executeRecall(
              100L, 200L, new BigDecimal("10000"), TENANT_ID, "admin", "Reduce allocation");
    }

    @Test
    @DisplayName("子代理不存在返回錯誤")
    void child_not_found_returns_error() {
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(null);

      CreditRecallForm form = new CreditRecallForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setNewLimit(new BigDecimal("10000"));
      form.setTenantId(TENANT_ID);

      ResponseDTO<String> result = creditNetworkService.reclaimCredit(form, "admin");

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("新限額低於已用額度返回錯誤")
    void recall_below_used_returns_error() {
      AgentCreditEntity child = buildCreditEntity(200L, 100L, "20000");
      child.setUsedCredit(new BigDecimal("15000"));
      when(agentCreditDao.findByAgentIdAndTenantId(200L, TENANT_ID)).thenReturn(child);

      CreditRecallForm form = new CreditRecallForm();
      form.setParentId(100L);
      form.setChildId(200L);
      form.setNewLimit(new BigDecimal("10000"));
      form.setTenantId(TENANT_ID);

      ResponseDTO<String> result = creditNetworkService.reclaimCredit(form, "admin");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.CREDIT_RECALL_EXCEEDS_USED.getMsg());
    }
  }

  // ========== triggerSettlement ==========

  @Nested
  @DisplayName("triggerSettlement 測試")
  class TriggerSettlementTest {

    @Test
    @DisplayName("觸發結算返回紀錄列表")
    void trigger_returns_records() {
      SettlementRecordEntity record = new SettlementRecordEntity();
      record.setSettlementRecordId(1L);
      record.setAgentId(100L);
      record.setSettlementWeek("2026-W09");
      record.setPlayerLoss(new BigDecimal("10000"));
      record.setOwnShare(new BigDecimal("3000"));
      record.setToParent(new BigDecimal("7000"));
      record.setToPlatform(BigDecimal.ZERO);
      record.setPaymentStatus(1);

      when(creditSettlementManager.triggerWeeklySettlement(TENANT_ID, "2026-W09"))
          .thenReturn(List.of(record));

      SettlementTriggerForm form = new SettlementTriggerForm();
      form.setTenantId(TENANT_ID);
      form.setSettlementWeek("2026-W09");

      ResponseDTO<List<SettlementRecordVO>> result = creditNetworkService.triggerSettlement(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
      assertThat(result.getData().get(0).getSettlementWeek()).isEqualTo("2026-W09");
    }
  }

  // ========== verifyPayment ==========

  @Nested
  @DisplayName("verifyPayment 測試")
  class VerifyPaymentTest {

    @Test
    @DisplayName("驗證成功")
    void verify_success() {
      SettlementRecordEntity record = new SettlementRecordEntity();
      record.setSettlementRecordId(1L);
      when(settlementRecordDao.selectById(1L)).thenReturn(record);

      ResponseDTO<String> result = creditNetworkService.verifyPayment(1L, "TXN-001");

      assertThat(result.getOk()).isTrue();
      verify(creditSettlementManager).verifyPayment(1L, "TXN-001");
    }

    @Test
    @DisplayName("紀錄不存在返回錯誤")
    void record_not_found_returns_error() {
      when(settlementRecordDao.selectById(999L)).thenReturn(null);

      ResponseDTO<String> result = creditNetworkService.verifyPayment(999L, "TXN-001");

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.SETTLEMENT_NOT_FOUND.getMsg());
    }
  }
}
