package net.lab1024.sa.app.igaming.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.agent.affiliate.controller.AffiliateCommissionAdminController;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionAdjustmentForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.form.CommissionCalculateForm;
import net.lab1024.sa.igaming.agent.affiliate.domain.vo.CommissionRecordVO;
import net.lab1024.sa.igaming.agent.affiliate.service.AffiliateCommissionAdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * AffiliateCommissionAdminController unit tests.
 *
 * @author iGaming Team
 * @since 2026-03-01
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AffiliateCommissionAdminController 單元測試")
class AffiliateCommissionAdminControllerTest {

  @Mock private AffiliateCommissionAdminService affiliateCommissionAdminService;
  @InjectMocks private AffiliateCommissionAdminController controller;

  @Test
  @DisplayName("calculate — 委託 Service 成功")
  void calculate_success() {
    CommissionRecordVO vo = new CommissionRecordVO();
    vo.setRecordId(1L);
    vo.setNetAmount(new BigDecimal("300"));
    when(affiliateCommissionAdminService.calculateCommission(any())).thenReturn(ResponseDTO.ok(vo));

    CommissionCalculateForm form = new CommissionCalculateForm();
    form.setAgentId(10L);
    form.setSettlementDate(LocalDate.of(2026, 3, 1));
    form.setGrossRevenue(new BigDecimal("1000"));

    ResponseDTO<CommissionRecordVO> result = controller.calculate(form);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getRecordId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("createAdjustment — 委託 Service 成功")
  void createAdjustment_success() {
    when(affiliateCommissionAdminService.createAdjustment(any())).thenReturn(ResponseDTO.ok());

    CommissionAdjustmentForm form = new CommissionAdjustmentForm();
    form.setAgentId(10L);
    form.setAdjustmentType(3);
    form.setAmount(new BigDecimal("50"));
    form.setReason("Manual correction");

    ResponseDTO<String> result = controller.createAdjustment(form);

    assertThat(result.getOk()).isTrue();
  }

  @Test
  @DisplayName("getRecord — 委託 Service 成功")
  void getRecord_success() {
    CommissionRecordVO vo = new CommissionRecordVO();
    vo.setRecordId(1L);
    when(affiliateCommissionAdminService.getCommissionRecord(1L)).thenReturn(ResponseDTO.ok(vo));

    ResponseDTO<CommissionRecordVO> result = controller.getRecord(1L);

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData().getRecordId()).isEqualTo(1L);
  }

  @Test
  @DisplayName("listPending — 委託 Service 成功")
  void listPending_success() {
    CommissionRecordVO vo1 = new CommissionRecordVO();
    vo1.setRecordId(1L);
    CommissionRecordVO vo2 = new CommissionRecordVO();
    vo2.setRecordId(2L);
    when(affiliateCommissionAdminService.listPendingApprovals())
        .thenReturn(ResponseDTO.ok(List.of(vo1, vo2)));

    ResponseDTO<List<CommissionRecordVO>> result = controller.listPending();

    assertThat(result.getOk()).isTrue();
    assertThat(result.getData()).hasSize(2);
  }
}
