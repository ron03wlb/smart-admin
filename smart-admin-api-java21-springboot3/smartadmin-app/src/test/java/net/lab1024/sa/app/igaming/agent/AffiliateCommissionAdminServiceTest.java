package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateAgentDao;
import net.lab1024.sa.igaming.agent.affiliate.dao.AffiliateCommissionRecordDao;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateAgentEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.entity.AffiliateCommissionRecordEntity;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionAdjustmentForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionCalculateForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.manager.AffiliateCommissionManager;
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateCommissionAdminService;
import net.lab1024.sa.igaming.common.constant.CommissionRecordStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AffiliateCommissionAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AffiliateCommissionAdminService 單元測試")
class AffiliateCommissionAdminServiceTest {

  @Mock private AffiliateAgentDao affiliateAgentDao;
  @Mock private AffiliateCommissionRecordDao affiliateCommissionRecordDao;
  @Mock private AffiliateCommissionManager affiliateCommissionManager;
  @InjectMocks private AffiliateCommissionAdminService service;

  private MockedStatic<TenantContext> tenantContextMock;

  @BeforeEach
  void setUp() {
    tenantContextMock = Mockito.mockStatic(TenantContext.class);
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(1L);
  }

  @AfterEach
  void tearDown() {
    tenantContextMock.close();
  }

  @Test
  @DisplayName("calculateCommission — 成功觸發佣金計算")
  void calculateCommission_success() {
    AffiliateAgentEntity agent = new AffiliateAgentEntity();
    agent.setAgentId(10L);
    when(affiliateAgentDao.selectById(10L)).thenReturn(agent);

    AffiliateCommissionRecordEntity record = new AffiliateCommissionRecordEntity();
    record.setRecordId(1L);
    record.setAgentId(10L);
    record.setNetAmount(new BigDecimal("300.0000"));
    record.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
    when(affiliateCommissionManager.calculateAndIssueCommission(
            eq(10L), eq(1L), any(LocalDate.class), any(BigDecimal.class)))
        .thenReturn(record);

    CommissionCalculateForm form = new CommissionCalculateForm();
    form.setAgentId(10L);
    form.setSettlementDate(LocalDate.of(2026, 3, 1));
    form.setGrossRevenue(new BigDecimal("1000"));

    ResponseDTO<CommissionRecordVO> result = service.calculateCommission(form);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getRecordId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("calculateCommission — Agent 不存在返回錯誤")
  void calculateCommission_agentNotFound() {
    when(affiliateAgentDao.selectById(999L)).thenReturn(null);

    CommissionCalculateForm form = new CommissionCalculateForm();
    form.setAgentId(999L);
    form.setSettlementDate(LocalDate.of(2026, 3, 1));
    form.setGrossRevenue(new BigDecimal("1000"));

    ResponseDTO<CommissionRecordVO> result = service.calculateCommission(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("calculateCommission — Tenant 為空返回錯誤")
  void calculateCommission_tenantNull() {
    tenantContextMock.when(TenantContext::getTenantId).thenReturn(null);

    CommissionCalculateForm form = new CommissionCalculateForm();
    form.setAgentId(10L);
    form.setSettlementDate(LocalDate.of(2026, 3, 1));
    form.setGrossRevenue(new BigDecimal("1000"));

    ResponseDTO<CommissionRecordVO> result = service.calculateCommission(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("createAdjustment — 成功建立調帳")
  void createAdjustment_success() {
    AffiliateAgentEntity agent = new AffiliateAgentEntity();
    agent.setAgentId(10L);
    when(affiliateAgentDao.selectById(10L)).thenReturn(agent);

    CommissionAdjustmentForm form = new CommissionAdjustmentForm();
    form.setAgentId(10L);
    form.setAdjustmentType(3);
    form.setAmount(new BigDecimal("50"));
    form.setReason("Manual correction");

    ResponseDTO<String> result = service.createAdjustment(form);

    assertThat(result.getOk()).isTrue();
    verify(affiliateCommissionManager)
        .createAdjustment(
            eq(10L), eq(1L), eq(3), eq(new BigDecimal("50")), eq("Manual correction"), eq("admin"));
  }

  @Test
  @DisplayName("createAdjustment — Agent 不存在返回錯誤")
  void createAdjustment_agentNotFound() {
    when(affiliateAgentDao.selectById(999L)).thenReturn(null);

    CommissionAdjustmentForm form = new CommissionAdjustmentForm();
    form.setAgentId(999L);
    form.setAdjustmentType(3);
    form.setAmount(new BigDecimal("50"));
    form.setReason("Manual correction");

    ResponseDTO<String> result = service.createAdjustment(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("getCommissionRecord — 成功返回佣金記錄詳情")
  void getCommissionRecord_success() {
    AffiliateCommissionRecordEntity entity = new AffiliateCommissionRecordEntity();
    entity.setRecordId(1L);
    entity.setAgentId(10L);
    entity.setNetAmount(new BigDecimal("300.0000"));
    entity.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
    when(affiliateCommissionRecordDao.selectById(1L)).thenReturn(entity);

    ResponseDTO<CommissionRecordVO> result = service.getCommissionRecord(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getRecordId()).isEqualTo(1L);
    assertThat(result.getData().getAgentId()).isEqualTo(10L);
  }

  @Test
  @DisplayName("getCommissionRecord — 不存在返回錯誤")
  void getCommissionRecord_notFound() {
    when(affiliateCommissionRecordDao.selectById(999L)).thenReturn(null);

    ResponseDTO<CommissionRecordVO> result = service.getCommissionRecord(999L);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("listPendingApprovals — 成功返回待審列表")
  void listPendingApprovals_success() {
    AffiliateCommissionRecordEntity r1 = new AffiliateCommissionRecordEntity();
    r1.setRecordId(1L);
    r1.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
    AffiliateCommissionRecordEntity r2 = new AffiliateCommissionRecordEntity();
    r2.setRecordId(2L);
    r2.setStatus(CommissionRecordStatusEnum.PENDING.getValue());
    when(affiliateCommissionRecordDao.findPendingByTenantId(1L)).thenReturn(List.of(r1, r2));

    ResponseDTO<List<CommissionRecordVO>> result = service.listPendingApprovals();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(2);
  }
}
