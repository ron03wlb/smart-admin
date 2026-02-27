package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskProposalReviewForm;
import net.lab1024.sa.igaming.risk.service.RiskProposalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskProposalService 測試")
class RiskProposalServiceTest {

  @Mock private RiskProposalDao riskProposalDao;
  @InjectMocks private RiskProposalService riskProposalService;

  @Nested
  @DisplayName("approve 測試")
  class ApproveTest {

    @Test
    @DisplayName("審核通過 PENDING 狀態的工單")
    void approve_pending_proposal() {
      RiskProposalEntity entity = new RiskProposalEntity();
      entity.setProposalId(1L);
      entity.setStatus(RiskProposalStatusEnum.PENDING.getValue());
      when(riskProposalDao.selectById(1L)).thenReturn(entity);

      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(1L);
      form.setReviewComment("Looks safe");

      ResponseDTO<String> result = riskProposalService.approve(form);

      assertThat(result.getOk()).isTrue();
      assertThat(entity.getStatus()).isEqualTo(RiskProposalStatusEnum.APPROVED.getValue());
      assertThat(entity.getReviewComment()).isEqualTo("Looks safe");
      assertThat(entity.getResolvedAt()).isNotNull();
      verify(riskProposalDao).updateById(entity);
    }

    @Test
    @DisplayName("審核通過 ASSIGNED 狀態的工單")
    void approve_assigned_proposal() {
      RiskProposalEntity entity = new RiskProposalEntity();
      entity.setProposalId(2L);
      entity.setStatus(RiskProposalStatusEnum.ASSIGNED.getValue());
      when(riskProposalDao.selectById(2L)).thenReturn(entity);

      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(2L);

      ResponseDTO<String> result = riskProposalService.approve(form);

      assertThat(result.getOk()).isTrue();
      assertThat(entity.getStatus()).isEqualTo(RiskProposalStatusEnum.APPROVED.getValue());
    }

    @Test
    @DisplayName("審核已完成的工單 - 返回錯誤")
    void approve_already_reviewed() {
      RiskProposalEntity entity = new RiskProposalEntity();
      entity.setProposalId(3L);
      entity.setStatus(RiskProposalStatusEnum.APPROVED.getValue());
      when(riskProposalDao.selectById(3L)).thenReturn(entity);

      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(3L);

      ResponseDTO<String> result = riskProposalService.approve(form);

      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("審核不存在的工單 - 返回錯誤")
    void approve_nonexistent_proposal() {
      when(riskProposalDao.selectById(999L)).thenReturn(null);

      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(999L);

      ResponseDTO<String> result = riskProposalService.approve(form);

      assertThat(result.getOk()).isFalse();
    }
  }

  @Nested
  @DisplayName("reject 測試")
  class RejectTest {

    @Test
    @DisplayName("駁回 PENDING 狀態的工單")
    void reject_pending_proposal() {
      RiskProposalEntity entity = new RiskProposalEntity();
      entity.setProposalId(1L);
      entity.setStatus(RiskProposalStatusEnum.PENDING.getValue());
      when(riskProposalDao.selectById(1L)).thenReturn(entity);

      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(1L);
      form.setReviewComment("Confirmed fraud");

      ResponseDTO<String> result = riskProposalService.reject(form);

      assertThat(result.getOk()).isTrue();
      assertThat(entity.getStatus()).isEqualTo(RiskProposalStatusEnum.REJECTED.getValue());
      assertThat(entity.getReviewComment()).isEqualTo("Confirmed fraud");
      assertThat(entity.getResolvedAt()).isNotNull();
      verify(riskProposalDao).updateById(entity);
    }

    @Test
    @DisplayName("駁回已完成的工單 - 返回錯誤")
    void reject_already_reviewed() {
      RiskProposalEntity entity = new RiskProposalEntity();
      entity.setProposalId(3L);
      entity.setStatus(RiskProposalStatusEnum.REJECTED.getValue());
      when(riskProposalDao.selectById(3L)).thenReturn(entity);

      RiskProposalReviewForm form = new RiskProposalReviewForm();
      form.setProposalId(3L);

      ResponseDTO<String> result = riskProposalService.reject(form);

      assertThat(result.getOk()).isFalse();
    }
  }
}
