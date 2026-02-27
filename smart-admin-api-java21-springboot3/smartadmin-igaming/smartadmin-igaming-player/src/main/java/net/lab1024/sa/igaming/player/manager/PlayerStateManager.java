package net.lab1024.sa.igaming.player.manager;

import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.common.code.PlayerErrorCode;
import net.lab1024.sa.igaming.common.constant.PlayerStatusEnum;
import net.lab1024.sa.igaming.player.dao.PlayerAuditLogDao;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerAuditLogEntity;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Player State Manager — handles player status transitions with audit logging.
 *
 * <p>All public methods MUST be annotated with {@code @Transactional(rollbackFor =
 * Throwable.class)} per SmartAdmin architecture rules.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class PlayerStateManager {

  private static final Map<PlayerStatusEnum, Set<PlayerStatusEnum>> VALID_TRANSITIONS =
      Map.of(
          PlayerStatusEnum.ACTIVE,
              Set.of(PlayerStatusEnum.LOCKED, PlayerStatusEnum.SUSPENDED, PlayerStatusEnum.CLOSED),
          PlayerStatusEnum.LOCKED, Set.of(PlayerStatusEnum.ACTIVE, PlayerStatusEnum.SUSPENDED),
          PlayerStatusEnum.SUSPENDED, Set.of(PlayerStatusEnum.ACTIVE, PlayerStatusEnum.CLOSED),
          PlayerStatusEnum.PENDING_VERIFICATION,
              Set.of(PlayerStatusEnum.ACTIVE, PlayerStatusEnum.CLOSED));

  private final PlayerDao playerDao;
  private final PlayerAuditLogDao playerAuditLogDao;

  /**
   * Transition player status with validation and audit logging.
   *
   * @param player player entity
   * @param newStatus target status
   * @param operator operator performing the change
   * @param reason reason for the change
   * @return ResponseDTO indicating success or error
   */
  @Transactional(rollbackFor = Throwable.class)
  public ResponseDTO<Void> transitionStatus(
      PlayerEntity player, PlayerStatusEnum newStatus, String operator, String reason) {
    PlayerStatusEnum currentStatus = resolveStatus(player.getStatus());
    if (currentStatus == null) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_STATUS_TRANSITION.getMsg());
    }

    Set<PlayerStatusEnum> allowed = VALID_TRANSITIONS.get(currentStatus);
    if (allowed == null || !allowed.contains(newStatus)) {
      return ResponseDTO.userErrorParam(PlayerErrorCode.INVALID_STATUS_TRANSITION.getMsg());
    }

    Integer oldStatusValue = player.getStatus();
    player.setStatus(newStatus.getValue());
    playerDao.updateById(player);

    PlayerAuditLogEntity auditLog = new PlayerAuditLogEntity();
    auditLog.setPlayerId(player.getPlayerId());
    auditLog.setOldStatus(oldStatusValue);
    auditLog.setNewStatus(newStatus.getValue());
    auditLog.setOperator(operator);
    auditLog.setReason(reason);
    playerAuditLogDao.insert(auditLog);

    return ResponseDTO.ok();
  }

  private PlayerStatusEnum resolveStatus(Integer value) {
    if (value == null) {
      return null;
    }
    for (PlayerStatusEnum e : PlayerStatusEnum.values()) {
      if (e.getValue().equals(value)) {
        return e;
      }
    }
    return null;
  }
}
