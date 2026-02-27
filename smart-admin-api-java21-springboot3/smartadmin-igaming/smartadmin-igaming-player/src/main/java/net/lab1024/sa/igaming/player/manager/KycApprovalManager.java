package net.lab1024.sa.igaming.player.manager;

import lombok.RequiredArgsConstructor;
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
  }
}
