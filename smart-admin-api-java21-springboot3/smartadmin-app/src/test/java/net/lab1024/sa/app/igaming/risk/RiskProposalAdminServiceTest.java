package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalAssignForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskProposalVO;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import net.lab1024.sa.igaming.risk.service.RiskProposalAdminService;
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
 * RiskProposalAdminService unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskProposalAdminService 單元測試")
class RiskProposalAdminServiceTest {

  @Mock private RiskProposalDao riskProposalDao;
  @Mock private RiskProposalManager riskProposalManager;
  @InjectMocks private RiskProposalAdminService riskProposalAdminService;

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
  @DisplayName("getProposalById — 成功返回提案 VO")
  void getProposalById_success() {
    RiskProposalEntity entity = new RiskProposalEntity();
    entity.setProposalId(100L);
    entity.setPlayerId(200L);
    entity.setStatus(RiskProposalStatusEnum.PENDING.getValue());
    when(riskProposalDao.selectById(100L)).thenReturn(entity);

    ResponseDTO<RiskProposalVO> result = riskProposalAdminService.getProposalById(100L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isNotNull();
    assertThat(result.getData().getProposalId()).isEqualTo(100L);
  }

  @Test
  @DisplayName("getProposalById — 不存在返回錯誤")
  void getProposalById_notFound() {
    when(riskProposalDao.selectById(999L)).thenReturn(null);

    ResponseDTO<RiskProposalVO> result = riskProposalAdminService.getProposalById(999L);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("assignProposal — 成功指派審核人")
  void assignProposal_success() {
    RiskProposalEntity entity = new RiskProposalEntity();
    entity.setProposalId(100L);
    entity.setStatus(RiskProposalStatusEnum.PENDING.getValue());
    when(riskProposalDao.selectById(100L)).thenReturn(entity);

    RiskProposalAssignForm form = new RiskProposalAssignForm();
    form.setProposalId(100L);
    form.setAssignee("reviewer01");

    ResponseDTO<String> result = riskProposalAdminService.assignProposal(form);

    assertThat(result.getOk()).isTrue();
    assertThat(entity.getAssignee()).isEqualTo("reviewer01");
    assertThat(entity.getStatus()).isEqualTo(RiskProposalStatusEnum.ASSIGNED.getValue());
  }

  @Test
  @DisplayName("assignProposal — 非 PENDING 狀態返回錯誤")
  void assignProposal_notPending() {
    RiskProposalEntity entity = new RiskProposalEntity();
    entity.setProposalId(100L);
    entity.setStatus(RiskProposalStatusEnum.APPROVED.getValue());
    when(riskProposalDao.selectById(100L)).thenReturn(entity);

    RiskProposalAssignForm form = new RiskProposalAssignForm();
    form.setProposalId(100L);
    form.setAssignee("reviewer01");

    ResponseDTO<String> result = riskProposalAdminService.assignProposal(form);

    assertThat(result.getOk()).isFalse();
  }

  @Test
  @DisplayName("escalateProposal — 成功升級提案")
  void escalateProposal_success() {
    RiskProposalEntity entity = new RiskProposalEntity();
    entity.setProposalId(100L);
    entity.setStatus(RiskProposalStatusEnum.ASSIGNED.getValue());
    when(riskProposalDao.selectById(100L)).thenReturn(entity);

    ResponseDTO<String> result = riskProposalAdminService.escalateProposal(100L);

    assertThat(result.getOk()).isTrue();
    assertThat(entity.getStatus()).isEqualTo(RiskProposalStatusEnum.ESCALATED.getValue());
    assertThat(entity.getPriority()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());
  }

  @Test
  @DisplayName("triggerOverdueEscalation — 成功觸發批次升級")
  void triggerOverdueEscalation_success() {
    when(riskProposalManager.escalateOverdueProposals()).thenReturn(7);

    ResponseDTO<Integer> result = riskProposalAdminService.triggerOverdueEscalation();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).isEqualTo(7);
  }
}
