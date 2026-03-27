package net.lab1024.sa.igaming.player.selfexclusion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.dao.PlayerDao;
import net.lab1024.sa.igaming.player.domain.entity.PlayerEntity;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionHistoryDao;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionRequestDao;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionHistoryEntity;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionRequestEntity;
import org.springframework.stereotype.Service;

/**
 * Self-Exclusion Request Service.
 *
 * <p>Manages self-exclusion requests including:
 *
 * <ul>
 *   <li>Creating new self-exclusion requests
 *   <li>Calculating cooling-off periods
 *   <li>Recording request history
 *   <li>Querying active exclusions
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfExclusionRequestService {

  private final SelfExclusionRequestDao selfExclusionRequestDao;
  private final SelfExclusionHistoryDao selfExclusionHistoryDao;
  private final PlayerDao playerDao;

  /**
   * Create self-exclusion request.
   *
   * <p>Creates a new self-exclusion request for a player with automatic cooling-off period
   * calculation.
   *
   * @param playerId Player ID
   * @param exclusionType Exclusion type (DEPOSIT, BETTING, LOGIN, FULL_BLOCK)
   * @param durationType Duration type (24_HOURS, 7_DAYS, 30_DAYS, 6_MONTHS, PERMANENT)
   * @param requestReason Player's reason (optional)
   * @param ipAddress Request IP address
   * @param userAgent Request user agent
   * @param complianceAcknowledgement Whether player acknowledged policy
   * @return ResponseDTO with request ID
   */
  public ResponseDTO<Long> createSelfExclusionRequest(
      Long playerId,
      String exclusionType,
      String durationType,
      String requestReason,
      String ipAddress,
      String userAgent,
      Boolean complianceAcknowledgement) {

    // Validate player exists
    PlayerEntity player = playerDao.selectById(playerId);
    if (player == null) {
      return ResponseDTO.userErrorParam("玩家不存在");
    }

    // Check if player already has an active self-exclusion request for this type
    LambdaQueryWrapper<SelfExclusionRequestEntity> activeCheck =
        new LambdaQueryWrapper<SelfExclusionRequestEntity>()
            .eq(SelfExclusionRequestEntity::getPlayerId, playerId)
            .eq(SelfExclusionRequestEntity::getExclusionType, exclusionType)
            .eq(SelfExclusionRequestEntity::getStatus, "ACTIVE")
            .eq(SelfExclusionRequestEntity::getDeleted, false);

    SelfExclusionRequestEntity existingActive = selfExclusionRequestDao.selectOne(activeCheck);
    if (existingActive != null) {
      return ResponseDTO.userErrorParam(String.format("玩家已有 %s 類型的有效自我排除請求", exclusionType));
    }

    // Calculate end date and cooling-off period based on duration type
    OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
    OffsetDateTime endDate = calculateEndDate(now, durationType);
    OffsetDateTime coolingOffPeriodEnd = calculateCoolingOffPeriod(now, durationType);

    // Create request
    SelfExclusionRequestEntity request =
        SelfExclusionRequestEntity.builder()
            .tenantId(player.getTenantId())
            .playerId(playerId)
            .exclusionType(exclusionType)
            .durationType(durationType)
            .startDate(now)
            .endDate(endDate)
            .coolingOffPeriodEnd(coolingOffPeriodEnd)
            .status("ACTIVE")
            .requestReason(requestReason)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .complianceAcknowledgement(complianceAcknowledgement)
            .build();

    selfExclusionRequestDao.insert(request);

    // Record history
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("ip_address", ipAddress);
    metadata.put("user_agent", userAgent);
    metadata.put("compliance_acknowledgement", complianceAcknowledgement);

    recordHistory(
        request.getRequestId(),
        playerId,
        "CREATED",
        null,
        "Player created self-exclusion request",
        null,
        "ACTIVE",
        metadata);

    log.info(
        "Self-exclusion request created for player {} (Type: {}, Duration: {}, ID: {})",
        playerId,
        exclusionType,
        durationType,
        request.getRequestId());

    return ResponseDTO.ok(request.getRequestId());
  }

  /**
   * Get active self-exclusion requests for a player.
   *
   * @param playerId Player ID
   * @return List of active self-exclusion requests
   */
  public List<SelfExclusionRequestEntity> getActiveExclusions(Long playerId) {
    return selfExclusionRequestDao.selectList(
        new LambdaQueryWrapper<SelfExclusionRequestEntity>()
            .eq(SelfExclusionRequestEntity::getPlayerId, playerId)
            .eq(SelfExclusionRequestEntity::getStatus, "ACTIVE")
            .eq(SelfExclusionRequestEntity::getDeleted, false)
            .orderByDesc(SelfExclusionRequestEntity::getCreateTime));
  }

  /**
   * Check if player has active exclusion of specific type.
   *
   * @param playerId Player ID
   * @param exclusionType Exclusion type
   * @return Option containing active exclusion if exists
   */
  public Option<SelfExclusionRequestEntity> getActiveExclusionByType(
      Long playerId, String exclusionType) {
    SelfExclusionRequestEntity request =
        selfExclusionRequestDao.selectOne(
            new LambdaQueryWrapper<SelfExclusionRequestEntity>()
                .eq(SelfExclusionRequestEntity::getPlayerId, playerId)
                .eq(SelfExclusionRequestEntity::getExclusionType, exclusionType)
                .eq(SelfExclusionRequestEntity::getStatus, "ACTIVE")
                .eq(SelfExclusionRequestEntity::getDeleted, false)
                .last("LIMIT 1"));

    return Option.of(request);
  }

  /**
   * Record self-exclusion history.
   *
   * @param requestId Request ID
   * @param playerId Player ID
   * @param actionType Action type
   * @param actionBy Employee ID (NULL for system actions)
   * @param actionReason Reason for action
   * @param previousStatus Previous status (NULL for CREATED)
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

  /**
   * Calculate end date based on duration type.
   *
   * @param startDate Start date
   * @param durationType Duration type
   * @return End date (NULL for PERMANENT)
   */
  private OffsetDateTime calculateEndDate(OffsetDateTime startDate, String durationType) {
    return switch (durationType) {
      case "24_HOURS" -> startDate.plusHours(24);
      case "7_DAYS" -> startDate.plusDays(7);
      case "30_DAYS" -> startDate.plusDays(30);
      case "6_MONTHS" -> startDate.plusMonths(6);
      case "PERMANENT" -> null; // NULL for permanent exclusions
      default -> throw new IllegalArgumentException("Invalid duration type: " + durationType);
    };
  }

  /**
   * Calculate cooling-off period end based on duration type.
   *
   * <p>Cooling-off periods prevent impulsive removal:
   *
   * <ul>
   *   <li>24_HOURS: 24 hours (same as duration)
   *   <li>7_DAYS: 7 days (same as duration)
   *   <li>30_DAYS: 7 days
   *   <li>6_MONTHS: 30 days
   *   <li>PERMANENT: 90 days
   * </ul>
   *
   * @param startDate Start date
   * @param durationType Duration type
   * @return Cooling-off period end date
   */
  private OffsetDateTime calculateCoolingOffPeriod(OffsetDateTime startDate, String durationType) {
    return switch (durationType) {
      case "24_HOURS" -> startDate.plusHours(24);
      case "7_DAYS" -> startDate.plusDays(7);
      case "30_DAYS" -> startDate.plusDays(7); // Shorter cooling-off for 30-day exclusions
      case "6_MONTHS" -> startDate.plusDays(30); // 1 month cooling-off
      case "PERMANENT" -> startDate.plusDays(90); // 3 months cooling-off
      default -> throw new IllegalArgumentException("Invalid duration type: " + durationType);
    };
  }
}
