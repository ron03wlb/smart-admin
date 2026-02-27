package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
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
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateService;
import net.lab1024.sa.igaming.common.code.AgentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AffiliateService 測試")
class AffiliateServiceTest {

  @Mock private AffiliateAgentDao affiliateAgentDao;
  @Mock private AffiliateHierarchyDao affiliateHierarchyDao;
  @Mock private AffiliateCommissionPlanDao affiliateCommissionPlanDao;
  @Mock private AffiliateCommissionRecordDao affiliateCommissionRecordDao;
  @Mock private AffiliateCommissionManager affiliateCommissionManager;
  @InjectMocks private AffiliateService affiliateService;

  private static final Long TENANT_ID = 1L;

  private AffiliateAgentEntity buildAgent(Long agentId, String username) {
    AffiliateAgentEntity e = new AffiliateAgentEntity();
    e.setAgentId(agentId);
    e.setTenantId(TENANT_ID);
    e.setUsername(username);
    e.setAgentLevel(1);
    e.setTotalPlayers(0);
    e.setActivePlayers(0);
    e.setTotalCommission(BigDecimal.ZERO);
    e.setStatus(1);
    e.setDeleted(false);
    e.setVersion(0);
    return e;
  }

  // ========== registerAgent ==========

  @Nested
  @DisplayName("registerAgent 測試")
  class RegisterAgentTest {

    @Test
    @DisplayName("註冊成功返回 VO")
    void register_success() {
      when(affiliateAgentDao.findByUsernameAndTenantId("agent01", TENANT_ID)).thenReturn(null);
      AffiliateAgentEntity created = buildAgent(100L, "agent01");
      when(affiliateCommissionManager.createAgentWithHierarchy(any())).thenReturn(created);

      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("agent01");
      form.setTenantId(TENANT_ID);

      ResponseDTO<AffiliateAgentVO> result = affiliateService.registerAgent(form);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getUsername()).isEqualTo("agent01");
    }

    @Test
    @DisplayName("帳號重複返回錯誤")
    void duplicate_username_returns_error() {
      AffiliateAgentEntity existing = buildAgent(100L, "agent01");
      when(affiliateAgentDao.findByUsernameAndTenantId("agent01", TENANT_ID)).thenReturn(existing);

      AffiliateRegisterForm form = new AffiliateRegisterForm();
      form.setUsername("agent01");
      form.setTenantId(TENANT_ID);

      ResponseDTO<AffiliateAgentVO> result = affiliateService.registerAgent(form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.AGENT_USERNAME_DUPLICATE.getMsg());
      verify(affiliateCommissionManager, never()).createAgentWithHierarchy(any());
    }
  }

  // ========== getAgentById ==========

  @Nested
  @DisplayName("getAgentById 測試")
  class GetAgentByIdTest {

    @Test
    @DisplayName("查詢成功返回 VO")
    void found_returns_vo() {
      AffiliateAgentEntity entity = buildAgent(100L, "agent01");
      when(affiliateAgentDao.selectById(100L)).thenReturn(entity);

      ResponseDTO<AffiliateAgentVO> result = affiliateService.getAgentById(100L, TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getAgentId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("不存在返回錯誤")
    void not_found_returns_error() {
      when(affiliateAgentDao.selectById(999L)).thenReturn(null);

      ResponseDTO<AffiliateAgentVO> result = affiliateService.getAgentById(999L, TENANT_ID);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.AGENT_NOT_FOUND.getMsg());
    }

    @Test
    @DisplayName("租戶不匹配返回錯誤")
    void wrong_tenant_returns_error() {
      AffiliateAgentEntity entity = buildAgent(100L, "agent01");
      entity.setTenantId(999L);
      when(affiliateAgentDao.selectById(100L)).thenReturn(entity);

      ResponseDTO<AffiliateAgentVO> result = affiliateService.getAgentById(100L, TENANT_ID);

      assertThat(result.getOk()).isFalse();
    }
  }

  // ========== getDownlineTree ==========

  @Nested
  @DisplayName("getDownlineTree 測試")
  class GetDownlineTreeTest {

    @Test
    @DisplayName("返回下級代理列表 (排除自身)")
    void returns_descendants_excluding_self() {
      AffiliateHierarchyEntity self = new AffiliateHierarchyEntity();
      self.setAncestorId(100L);
      self.setDescendantId(100L);
      self.setDepth(0);

      AffiliateHierarchyEntity child = new AffiliateHierarchyEntity();
      child.setAncestorId(100L);
      child.setDescendantId(200L);
      child.setDepth(1);

      when(affiliateHierarchyDao.findDescendants(100L, TENANT_ID)).thenReturn(List.of(self, child));
      AffiliateAgentEntity agent200 = buildAgent(200L, "child01");
      when(affiliateAgentDao.selectBatchIds(List.of(200L))).thenReturn(List.of(agent200));

      ResponseDTO<List<AffiliateAgentVO>> result =
          affiliateService.getDownlineTree(100L, TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
      assertThat(result.getData().get(0).getAgentId()).isEqualTo(200L);
    }

    @Test
    @DisplayName("無下級返回空列表")
    void no_descendants_returns_empty() {
      AffiliateHierarchyEntity self = new AffiliateHierarchyEntity();
      self.setAncestorId(100L);
      self.setDescendantId(100L);
      self.setDepth(0);

      when(affiliateHierarchyDao.findDescendants(100L, TENANT_ID)).thenReturn(List.of(self));

      ResponseDTO<List<AffiliateAgentVO>> result =
          affiliateService.getDownlineTree(100L, TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isEmpty();
    }
  }

  // ========== getPendingApprovals ==========

  @Nested
  @DisplayName("getPendingApprovals 測試")
  class GetPendingApprovalsTest {

    @Test
    @DisplayName("返回待審核列表")
    void returns_pending_list() {
      AffiliateCommissionRecordEntity r1 = new AffiliateCommissionRecordEntity();
      r1.setRecordId(1L);
      r1.setAgentId(100L);
      r1.setStatus(1);
      when(affiliateCommissionRecordDao.findPendingByTenantId(TENANT_ID)).thenReturn(List.of(r1));

      ResponseDTO<List<CommissionRecordVO>> result =
          affiliateService.getPendingApprovals(TENANT_ID);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).hasSize(1);
    }
  }

  // ========== approveCommission ==========

  @Nested
  @DisplayName("approveCommission 測試")
  class ApproveCommissionTest {

    @Test
    @DisplayName("審核通過委託 Manager")
    void approve_delegates_to_manager() {
      AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
      record.setRecordId(1L);
      record.setStatus(1);
      when(affiliateCommissionRecordDao.selectById(1L)).thenReturn(record);

      CommissionApprovalForm form = new CommissionApprovalForm();
      form.setRecordId(1L);
      form.setApprovedBy("admin");

      ResponseDTO<String> result = affiliateService.approveCommission(form);

      assertThat(result.getOk()).isTrue();
      verify(affiliateCommissionManager).approveCommission(form);
    }

    @Test
    @DisplayName("紀錄不存在返回錯誤")
    void record_not_found_returns_error() {
      when(affiliateCommissionRecordDao.selectById(999L)).thenReturn(null);

      CommissionApprovalForm form = new CommissionApprovalForm();
      form.setRecordId(999L);

      ResponseDTO<String> result = affiliateService.approveCommission(form);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.COMMISSION_RECORD_NOT_FOUND.getMsg());
    }
  }

  // ========== rejectCommission ==========

  @Nested
  @DisplayName("rejectCommission 測試")
  class RejectCommissionTest {

    @Test
    @DisplayName("拒絕委託 Manager")
    void reject_delegates_to_manager() {
      AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
      record.setRecordId(1L);
      record.setStatus(1);
      when(affiliateCommissionRecordDao.selectById(1L)).thenReturn(record);

      CommissionApprovalForm form = new CommissionApprovalForm();
      form.setRecordId(1L);

      ResponseDTO<String> result = affiliateService.rejectCommission(form);

      assertThat(result.getOk()).isTrue();
      verify(affiliateCommissionManager).rejectCommission(form);
    }
  }

  // ========== getCommissionPlan ==========

  @Nested
  @DisplayName("getCommissionPlan 測試")
  class GetCommissionPlanTest {

    @Test
    @DisplayName("查詢計畫成功")
    void plan_found() {
      AffiliateCommissionPlanEntity plan = new AffiliateCommissionPlanEntity();
      plan.setPlanId(10L);
      plan.setPlanName("Standard");
      when(affiliateCommissionPlanDao.selectById(10L)).thenReturn(plan);

      ResponseDTO<AffiliateCommissionPlanEntity> result = affiliateService.getCommissionPlan(10L);

      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getPlanName()).isEqualTo("Standard");
    }

    @Test
    @DisplayName("計畫不存在返回錯誤")
    void plan_not_found() {
      when(affiliateCommissionPlanDao.selectById(999L)).thenReturn(null);

      ResponseDTO<AffiliateCommissionPlanEntity> result = affiliateService.getCommissionPlan(999L);

      assertThat(result.getOk()).isFalse();
      assertThat(result.getMsg()).isEqualTo(AgentErrorCode.COMMISSION_PLAN_NOT_FOUND.getMsg());
    }
  }
}
