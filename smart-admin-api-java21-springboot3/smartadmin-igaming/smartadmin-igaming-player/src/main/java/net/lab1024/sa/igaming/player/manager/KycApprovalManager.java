package net.lab1024.sa.igaming.player.manager;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.mq.kafka.constant.IgamingKafkaConst;
import net.lab1024.sa.common.mq.kafka.event.DomainEvent;
import net.lab1024.sa.common.mq.kafka.event.DomainEventPublisher;
import net.lab1024.sa.igaming.common.constant.DomainEventTypeConst;
import net.lab1024.sa.igaming.common.constant.KycLevelEnum;
import net.lab1024.sa.igaming.common.constant.KycVerificationStatusEnum;
import net.lab1024.sa.igaming.player.dao.KycDocumentDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.KycDocumentEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KYC Approval Manager — handles KYC document approval and player level upgrade.
 *
 * <p>All public methods MUST be annotated with {@code @Transactional(rollbackFor =
 * Throwable.class)} per SmartAdmin architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class KycApprovalManager {

  private final PlayerDao playerDao;
  private final KycDocumentDao kycDocumentDao;
  private final DomainEventPublisher domainEventPublisher;

  /**
   * Approve KYC L1 document and upgrade player KYC level.
   *
   * @param player player entity
   * @param document KYC document entity
   */
  @Transactional(rollbackFor = Throwable.class)
  public void approveKycL1(PlayerEntity player, KycDocumentEntity document) {
    document.setVerificationStatus(KycVerificationStatusEnum.APPROVED.getValue());
    kycDocumentDao.updateById(document);

    player.setKycLevel(KycLevelEnum.L1.getValue());
    playerDao.updateById(player);

    publishKycEvent(player.getPlayerId(), KycLevelEnum.L1, document.getKycDocumentId());
  }

  /**
   * Approve KYC L2 document and upgrade player KYC level to L2.
   *
   * @param document KYC document entity
   * @param comment reviewer comment
   */
  @Transactional(rollbackFor = Throwable.class)
  public void approveKycL2(KycDocumentEntity document, String comment) {
    document.setVerificationStatus(KycVerificationStatusEnum.APPROVED.getValue());
    document.setReviewerComment(comment);
    kycDocumentDao.updateById(document);

    PlayerEntity player = playerDao.selectById(document.getPlayerId());
    player.setKycLevel(KycLevelEnum.L2.getValue());
    playerDao.updateById(player);

    publishKycEvent(player.getPlayerId(), KycLevelEnum.L2, document.getKycDocumentId());
  }

  /**
   * Reject KYC L2 document.
   *
   * @param document KYC document entity
   * @param comment reviewer comment
   */
  @Transactional(rollbackFor = Throwable.class)
  public void rejectKycL2(KycDocumentEntity document, String comment) {
    document.setVerificationStatus(KycVerificationStatusEnum.REJECTED.getValue());
    document.setReviewerComment(comment);
    kycDocumentDao.updateById(document);

    publishKycEvent(document.getPlayerId(), null, document.getKycDocumentId());
  }

  @SuppressWarnings("FutureReturnValueIgnored")
  private void publishKycEvent(Long playerId, KycLevelEnum kycLevel, Long kycDocumentId) {
    ObjectNode payload = JsonNodeFactory.instance.objectNode();
    payload.put("playerId", playerId);
    payload.put("kycLevel", kycLevel != null ? kycLevel.getValue() : -1);
    payload.put("kycDocumentId", kycDocumentId);
    domainEventPublisher.publish(
        IgamingKafkaConst.Topic.PLAYER_EVENTS,
        DomainEvent.builder()
            .eventType(DomainEventTypeConst.KYC_UPDATED)
            .aggregateType("Player")
            .aggregateId(String.valueOf(playerId))
            .payload(payload)
            .build());
  }
}
