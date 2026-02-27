package net.lab1024.sa.app.igaming.risk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import net.lab1024.sa.igaming.common.constant.RiskLevelEnum;
import net.lab1024.sa.igaming.common.constant.RiskProposalStatusEnum;
import net.lab1024.sa.igaming.risk.dao.RiskProposalDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskProposalEntity;
import net.lab1024.sa.igaming.risk.manager.RiskProposalManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("RiskProposalManager 測試")
class RiskProposalManagerTest {

  @Mock private RiskProposalDao riskProposalDao;
  @InjectMocks private RiskProposalManager riskProposalManager;

  @Nested
  @DisplayName("createReviewProposal 測試")
  class CreateReviewProposalTest {

    @Test
    @DisplayName("建立標準審核工單")
    void create_standard_review() {
      RiskProposalEntity result =
          riskProposalManager.createReviewProposal(100L, 1L, 500L, RiskLevelEnum.MEDIUM);

      ArgumentCaptor<RiskProposalEntity> captor = ArgumentCaptor.forClass(RiskProposalEntity.class);
      verify(riskProposalDao).insert(captor.capture());
      RiskProposalEntity saved = captor.getValue();
      assertThat(saved.getPlayerId()).isEqualTo(100L);
      assertThat(saved.getTenantId()).isEqualTo(1L);
      assertThat(saved.getAssessmentId()).isEqualTo(500L);
      assertThat(saved.getStatus()).isEqualTo(RiskProposalStatusEnum.PENDING.getValue());
      assertThat(saved.getPriority()).isEqualTo(RiskLevelEnum.MEDIUM.getValue());
      assertThat(saved.getSlaDeadline()).isNotNull();
    }
  }

  @Nested
  @DisplayName("createUrgentProposal 測試")
  class CreateUrgentProposalTest {

    @Test
    @DisplayName("建立緊急審核工單")
    void create_urgent_proposal() {
      RiskProposalEntity result = riskProposalManager.createUrgentProposal(100L, 1L, 600L);

      ArgumentCaptor<RiskProposalEntity> captor = ArgumentCaptor.forClass(RiskProposalEntity.class);
      verify(riskProposalDao).insert(captor.capture());
      RiskProposalEntity saved = captor.getValue();
      assertThat(saved.getStatus()).isEqualTo(RiskProposalStatusEnum.PENDING.getValue());
      assertThat(saved.getPriority()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());
      assertThat(saved.getSlaDeadline()).isNotNull();
    }
  }

  @Nested
  @DisplayName("escalateOverdueProposals 測試")
  class EscalateOverdueTest {

    @Test
    @DisplayName("升級逾期工單")
    void escalate_overdue_proposals() {
      RiskProposalEntity p1 = new RiskProposalEntity();
      p1.setProposalId(1L);
      p1.setPlayerId(100L);
      p1.setStatus(RiskProposalStatusEnum.PENDING.getValue());
      p1.setPriority(RiskLevelEnum.MEDIUM.getValue());

      RiskProposalEntity p2 = new RiskProposalEntity();
      p2.setProposalId(2L);
      p2.setPlayerId(200L);
      p2.setStatus(RiskProposalStatusEnum.ASSIGNED.getValue());
      p2.setPriority(RiskLevelEnum.HIGH.getValue());

      when(riskProposalDao.findOverduePending()).thenReturn(List.of(p1, p2));

      int count = riskProposalManager.escalateOverdueProposals();

      assertThat(count).isEqualTo(2);
      assertThat(p1.getStatus()).isEqualTo(RiskProposalStatusEnum.ESCALATED.getValue());
      assertThat(p1.getPriority()).isEqualTo(RiskLevelEnum.CRITICAL.getValue());
      assertThat(p2.getStatus()).isEqualTo(RiskProposalStatusEnum.ESCALATED.getValue());
      verify(riskProposalDao).updateById(p1);
      verify(riskProposalDao).updateById(p2);
    }

    @Test
    @DisplayName("無逾期工單返回 0")
    void no_overdue_returns_zero() {
      when(riskProposalDao.findOverduePending()).thenReturn(List.of());

      int count = riskProposalManager.escalateOverdueProposals();

      assertThat(count).isZero();
    }
  }
}
