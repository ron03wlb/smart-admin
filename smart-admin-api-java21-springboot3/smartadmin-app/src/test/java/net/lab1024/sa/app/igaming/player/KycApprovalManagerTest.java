package net.lab1024.sa.app.igaming.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum;
import net.lab1024.sa.igaming.player.dao.KycDocumentDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.KycDocumentEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.manager.KycApprovalManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * KycApprovalManager unit tests.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KycApprovalManager 單元測試")
class KycApprovalManagerTest {

  @Mock private PlayerDao playerDao;
  @Mock private KycDocumentDao kycDocumentDao;
  @Mock private DomainEventPublisher domainEventPublisher;
  @InjectMocks private KycApprovalManager kycApprovalManager;

  @Nested
  @DisplayName("KYC L1 審核")
  class KycL1Test {

    @Test
    @DisplayName("approveKycL1 — 成功核准 + 升級 KYC Level + 發佈事件")
    void approveKycL1_success() {
      PlayerEntity player = new PlayerEntity();
      player.setPlayerId(1L);
      player.setKycLevel(0);

      KycDocumentEntity document = new KycDocumentEntity();
      document.setKycDocumentId(10L);
      document.setPlayerId(1L);
      document.setVerificationStatus(KycVerificationStatusEnum.PENDING.getValue());

      kycApprovalManager.approveKycL1(player, document);

      assertThat(document.getVerificationStatus())
          .isEqualTo(KycVerificationStatusEnum.APPROVED.getValue());
      assertThat(player.getKycLevel()).isEqualTo(KycLevelEnum.L1.getValue());
      verify(kycDocumentDao).updateById(document);
      verify(playerDao).updateById(player);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.PLAYER_EVENTS), any(DomainEvent.class));
    }
  }

  @Nested
  @DisplayName("KYC L2 審核")
  class KycL2Test {

    @Test
    @DisplayName("approveKycL2 — 成功核准 + 升級至 L2 + 發佈事件")
    void approveKycL2_success() {
      KycDocumentEntity document = new KycDocumentEntity();
      document.setKycDocumentId(20L);
      document.setPlayerId(1L);
      document.setVerificationStatus(KycVerificationStatusEnum.PENDING.getValue());

      PlayerEntity player = new PlayerEntity();
      player.setPlayerId(1L);
      player.setKycLevel(KycLevelEnum.L1.getValue());
      when(playerDao.selectById(1L)).thenReturn(player);

      kycApprovalManager.approveKycL2(document, "Documents verified");

      assertThat(document.getVerificationStatus())
          .isEqualTo(KycVerificationStatusEnum.APPROVED.getValue());
      assertThat(document.getReviewerComment()).isEqualTo("Documents verified");
      assertThat(player.getKycLevel()).isEqualTo(KycLevelEnum.L2.getValue());
      verify(kycDocumentDao).updateById(document);
      verify(playerDao).updateById(player);
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.PLAYER_EVENTS), any(DomainEvent.class));
    }

    @Test
    @DisplayName("rejectKycL2 — 拒絕文件 + 記錄評論 + 發佈事件")
    void rejectKycL2_success() {
      KycDocumentEntity document = new KycDocumentEntity();
      document.setKycDocumentId(21L);
      document.setPlayerId(2L);
      document.setVerificationStatus(KycVerificationStatusEnum.PENDING.getValue());

      kycApprovalManager.rejectKycL2(document, "Blurry photo");

      assertThat(document.getVerificationStatus())
          .isEqualTo(KycVerificationStatusEnum.REJECTED.getValue());
      assertThat(document.getReviewerComment()).isEqualTo("Blurry photo");
      verify(kycDocumentDao).updateById(document);
      verify(playerDao, never()).updateById(any(PlayerEntity.class));
      verify(domainEventPublisher)
          .publish(eq(IgamingKafkaConst.Topic.PLAYER_EVENTS), any(DomainEvent.class));
    }
  }
}
