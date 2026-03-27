package net.lab1024.sa.igaming.player.selfexclusion.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionHistoryDao;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionRequestDao;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionHistoryEntity;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionRequestEntity;
import org.springframework.stereotype.Service;

/**
 * Self-Exclusion Review Service.
 *
 * <p>Manages self-exclusion removal review process including:
 *
 * <ul>
 *   <li>Player removal requests
 *   <li>Admin approval/rejection
 *   <li>Admin cancellation
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfExclusionReviewService {

  private final SelfExclusionRequestDao selfExclusionRequestDao;
  private final SelfExclusionHistoryDao selfExclusionHistoryDao;

  /**
   * Player requests to remove self-exclusion.
   *
   * <p>Validates that cooling-off period has passed before allowing removal request.
   *
   * @param requestId Request ID
   * @param playerId Player ID (for validation)
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> requestRemoval(Long requestId, Long playerId) {
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);

    if (request == null) {
      return ResponseDTO.userErrorParam("自我排除請求不存在");
    }

    if (!request.getPlayerId().equals(playerId)) {
      return ResponseDTO.userErrorParam("無權操作此請求");
    }

    if (!"ACTIVE".equals(request.getStatus())) {
      return ResponseDTO.userErrorParam("只能對 ACTIVE 狀態的請求申請解除");
    }

    if (request.getRemovalRequestDate() != null) {
      return ResponseDTO.userErrorParam("已經提交解除申請，請等待審核");
    }

    // Check if cooling-off period has passed
    OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
    if (now.isBefore(request.getCoolingOffPeriodEnd())) {
      return ResponseDTO.userErrorParam(
          String.format("冷靜期未結束，最早可申請解除日期：%s", request.getCoolingOffPeriodEnd().toString()));
    }

    // Update request with removal request date
    request.setRemovalRequestDate(now);
    selfExclusionRequestDao.updateById(request);

    // Record history
    recordHistory(
        requestId,
        playerId,
        "REMOVAL_REQUESTED",
        null,
        "Player requested removal after cooling-off period",
        "ACTIVE",
        "ACTIVE",
        null);

    log.info("Player {} requested removal of self-exclusion request {}", playerId, requestId);

    return ResponseDTO.ok();
  }

  /**
   * Admin approves removal request.
   *
   * @param requestId Request ID
   * @param adminId Admin employee ID
   * @param approvalReason Reason for approval
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> approveRemoval(Long requestId, Long adminId, String approvalReason) {
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);

    if (request == null) {
      return ResponseDTO.userErrorParam("自我排除請求不存在");
    }

    if (!"ACTIVE".equals(request.getStatus())) {
      return ResponseDTO.userErrorParam("只能批准 ACTIVE 狀態的請求");
    }

    if (request.getRemovalRequestDate() == null) {
      return ResponseDTO.userErrorParam("玩家尚未提交解除申請");
    }

    if (request.getRemovalApprovedAt() != null) {
      return ResponseDTO.userErrorParam("此請求已經處理過");
    }

    // Update request status to REMOVED
    OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
    request.setStatus("REMOVED");
    request.setRemovalApprovedBy(adminId);
    request.setRemovalApprovedAt(now);
    request.setRemovalReason(approvalReason);
    selfExclusionRequestDao.updateById(request);

    // Record history
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("admin_id", adminId);
    metadata.put("approval_date", now.toString());

    recordHistory(
        requestId,
        request.getPlayerId(),
        "REMOVAL_APPROVED",
        adminId,
        approvalReason,
        "ACTIVE",
        "REMOVED",
        metadata);

    log.info(
        "Self-exclusion request {} approved for removal by admin {} (Player: {}, Reason: {})",
        requestId,
        adminId,
        request.getPlayerId(),
        approvalReason);

    return ResponseDTO.ok();
  }

  /**
   * Admin rejects removal request.
   *
   * @param requestId Request ID
   * @param adminId Admin employee ID
   * @param rejectionReason Reason for rejection
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> rejectRemoval(Long requestId, Long adminId, String rejectionReason) {
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);

    if (request == null) {
      return ResponseDTO.userErrorParam("自我排除請求不存在");
    }

    if (!"ACTIVE".equals(request.getStatus())) {
      return ResponseDTO.userErrorParam("只能拒絕 ACTIVE 狀態的請求");
    }

    if (request.getRemovalRequestDate() == null) {
      return ResponseDTO.userErrorParam("玩家尚未提交解除申請");
    }

    if (request.getRemovalApprovedAt() != null) {
      return ResponseDTO.userErrorParam("此請求已經處理過");
    }

    // Update request - clear removal request date and set rejection info
    // Use UpdateWrapper to explicitly set null values (MyBatis Plus doesn't update null by default)
    com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<SelfExclusionRequestEntity>
        updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
    updateWrapper
        .eq(SelfExclusionRequestEntity::getRequestId, requestId)
        .set(SelfExclusionRequestEntity::getRemovalRequestDate, null) // Explicitly set to null
        .set(SelfExclusionRequestEntity::getRemovalApprovedBy, adminId)
        .set(
            SelfExclusionRequestEntity::getRemovalApprovedAt,
            OffsetDateTime.now(ZoneId.systemDefault()))
        .set(SelfExclusionRequestEntity::getRemovalReason, rejectionReason);

    selfExclusionRequestDao.update(null, updateWrapper);

    // Record history
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("admin_id", adminId);

    recordHistory(
        requestId,
        request.getPlayerId(),
        "REMOVAL_REJECTED",
        adminId,
        rejectionReason,
        "ACTIVE",
        "ACTIVE",
        metadata);

    log.info(
        "Self-exclusion removal request {} rejected by admin {} (Player: {}, Reason: {})",
        requestId,
        adminId,
        request.getPlayerId(),
        rejectionReason);

    return ResponseDTO.ok();
  }

  /**
   * Admin cancels self-exclusion request before it takes effect.
   *
   * @param requestId Request ID
   * @param adminId Admin employee ID
   * @param cancellationReason Reason for cancellation
   * @return ResponseDTO indicating success or error
   */
  public ResponseDTO<Void> cancelRequest(Long requestId, Long adminId, String cancellationReason) {
    SelfExclusionRequestEntity request = selfExclusionRequestDao.selectById(requestId);

    if (request == null) {
      return ResponseDTO.userErrorParam("自我排除請求不存在");
    }

    if (!"ACTIVE".equals(request.getStatus())) {
      return ResponseDTO.userErrorParam("只能取消 ACTIVE 狀態的請求");
    }

    // Update request status to CANCELLED
    request.setStatus("CANCELLED");
    request.setRemovalReason(cancellationReason);
    selfExclusionRequestDao.updateById(request);

    // Record history
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("admin_id", adminId);

    recordHistory(
        requestId,
        request.getPlayerId(),
        "CANCELLED",
        adminId,
        cancellationReason,
        "ACTIVE",
        "CANCELLED",
        metadata);

    log.info(
        "Self-exclusion request {} cancelled by admin {} (Player: {}, Reason: {})",
        requestId,
        adminId,
        request.getPlayerId(),
        cancellationReason);

    return ResponseDTO.ok();
  }

  /**
   * Record self-exclusion history.
   *
   * @param requestId Request ID
   * @param playerId Player ID
   * @param actionType Action type
   * @param actionBy Employee ID (NULL for system actions)
   * @param actionReason Reason for action
   * @param previousStatus Previous status
   * @param newStatus New status
   * @param metadata Additional metadata
   */
  private void recordHistory(
      Long requestId,
      Long playerId,
      String actionType,
      Long actionBy,
      String actionReason,
      String previousStatus,
      String newStatus,
      Map<String, Object> metadata) {

    SelfExclusionHistoryEntity history =
        SelfExclusionHistoryEntity.builder()
            .tenantId(1L) // TODO: Get from context
            .requestId(requestId)
            .playerId(playerId)
            .actionType(actionType)
            .actionBy(actionBy)
            .actionReason(actionReason)
            .actionAt(OffsetDateTime.now(ZoneId.systemDefault()))
            .previousStatus(previousStatus)
            .newStatus(newStatus)
            .metadata(metadata)
            .build();

    selfExclusionHistoryDao.insert(history);
  }
}
