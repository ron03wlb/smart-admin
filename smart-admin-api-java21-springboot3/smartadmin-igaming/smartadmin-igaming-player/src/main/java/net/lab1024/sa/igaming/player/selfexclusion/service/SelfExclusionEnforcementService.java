package net.lab1024.sa.igaming.player.selfexclusion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.igaming.player.selfexclusion.dao.SelfExclusionRequestDao;
import net.lab1024.sa.igaming.player.selfexclusion.domain.entity.SelfExclusionRequestEntity;
import org.springframework.stereotype.Service;

/**
 * Self-Exclusion Enforcement Service.
 *
 * <p>Enforces self-exclusion restrictions by checking if player actions are allowed.
 *
 * <p>Provides enforcement methods for:
 *
 * <ul>
 *   <li>Deposit restrictions
 *   <li>Betting restrictions
 *   <li>Login restrictions
 *   <li>Full block (all activities)
 * </ul>
 *
 * @author iGaming Team
 * @since 2026-03-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfExclusionEnforcementService {

  private final SelfExclusionRequestDao selfExclusionRequestDao;

  /**
   * Check if player can make a deposit.
   *
   * @param playerId Player ID
   * @return ResponseDTO with error if deposit is blocked
   */
  public ResponseDTO<Void> checkDepositAllowed(Long playerId) {
    // Check for DEPOSIT or FULL_BLOCK exclusions
    Option<SelfExclusionRequestEntity> depositBlock =
        getActiveExclusionByTypes(playerId, List.of("DEPOSIT", "FULL_BLOCK"));

    if (depositBlock.isDefined()) {
      SelfExclusionRequestEntity exclusion = depositBlock.get();
      String message =
          String.format(
              "您已設置自我排除（類型：%s），目前無法進行存款操作。有效期至：%s",
              exclusion.getExclusionType(), formatEndDate(exclusion.getEndDate()));

      log.warn(
          "Deposit blocked for player {} due to {} self-exclusion (Request: {})",
          playerId,
          exclusion.getExclusionType(),
          exclusion.getRequestId());

      return ResponseDTO.userErrorParam(message);
    }

    return ResponseDTO.ok();
  }

  /**
   * Check if player can place a bet.
   *
   * @param playerId Player ID
   * @return ResponseDTO with error if betting is blocked
   */
  public ResponseDTO<Void> checkBettingAllowed(Long playerId) {
    // Check for BETTING or FULL_BLOCK exclusions
    Option<SelfExclusionRequestEntity> bettingBlock =
        getActiveExclusionByTypes(playerId, List.of("BETTING", "FULL_BLOCK"));

    if (bettingBlock.isDefined()) {
      SelfExclusionRequestEntity exclusion = bettingBlock.get();
      String message =
          String.format(
              "您已設置自我排除（類型：%s），目前無法進行投注操作。有效期至：%s",
              exclusion.getExclusionType(), formatEndDate(exclusion.getEndDate()));

      log.warn(
          "Betting blocked for player {} due to {} self-exclusion (Request: {})",
          playerId,
          exclusion.getExclusionType(),
          exclusion.getRequestId());

      return ResponseDTO.userErrorParam(message);
    }

    return ResponseDTO.ok();
  }

  /**
   * Check if player can login.
   *
   * @param playerId Player ID
   * @return ResponseDTO with error if login is blocked
   */
  public ResponseDTO<Void> checkLoginAllowed(Long playerId) {
    // Check for LOGIN or FULL_BLOCK exclusions
    Option<SelfExclusionRequestEntity> loginBlock =
        getActiveExclusionByTypes(playerId, List.of("LOGIN", "FULL_BLOCK"));

    if (loginBlock.isDefined()) {
      SelfExclusionRequestEntity exclusion = loginBlock.get();
      String message =
          String.format(
              "您已設置自我排除（類型：%s），目前無法登入。有效期至：%s",
              exclusion.getExclusionType(), formatEndDate(exclusion.getEndDate()));

      log.warn(
          "Login blocked for player {} due to {} self-exclusion (Request: {})",
          playerId,
          exclusion.getExclusionType(),
          exclusion.getRequestId());

      return ResponseDTO.userErrorParam(message);
    }

    return ResponseDTO.ok();
  }

  /**
   * Check if player has any active self-exclusion.
   *
   * @param playerId Player ID
   * @return true if player has any active exclusion
   */
  public boolean hasActiveExclusion(Long playerId) {
    long count =
        selfExclusionRequestDao.selectCount(
            new LambdaQueryWrapper<SelfExclusionRequestEntity>()
                .eq(SelfExclusionRequestEntity::getPlayerId, playerId)
                .eq(SelfExclusionRequestEntity::getStatus, "ACTIVE")
                .eq(SelfExclusionRequestEntity::getDeleted, false));

    return count > 0;
  }

  /**
   * Get active exclusion by multiple types (OR condition).
   *
   * @param playerId Player ID
   * @param exclusionTypes List of exclusion types to check
   * @return Option containing exclusion if found
   */
  private Option<SelfExclusionRequestEntity> getActiveExclusionByTypes(
      Long playerId, List<String> exclusionTypes) {

    SelfExclusionRequestEntity exclusion =
        selfExclusionRequestDao.selectOne(
            new LambdaQueryWrapper<SelfExclusionRequestEntity>()
                .eq(SelfExclusionRequestEntity::getPlayerId, playerId)
                .in(SelfExclusionRequestEntity::getExclusionType, exclusionTypes)
                .eq(SelfExclusionRequestEntity::getStatus, "ACTIVE")
                .eq(SelfExclusionRequestEntity::getDeleted, false)
                .orderByDesc(SelfExclusionRequestEntity::getCreateTime)
                .last("LIMIT 1"));

    return Option.of(exclusion);
  }

  /**
   * Format end date for display.
   *
   * @param endDate End date (NULL for permanent)
   * @return Formatted string
   */
  private String formatEndDate(OffsetDateTime endDate) {
    if (endDate == null) {
      return "永久";
    }
    return endDate.toString();
  }

  /**
   * Expire self-exclusion requests that have passed their end date.
   *
   * <p>This method should be called by a scheduled job (daily).
   *
   * @return Number of expired requests
   */
  public int expireOutdatedRequests() {
    OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());

    // Find all ACTIVE requests with end_date in the past
    List<SelfExclusionRequestEntity> outdatedRequests =
        selfExclusionRequestDao.selectList(
            new LambdaQueryWrapper<SelfExclusionRequestEntity>()
                .eq(SelfExclusionRequestEntity::getStatus, "ACTIVE")
                .eq(SelfExclusionRequestEntity::getDeleted, false)
                .isNotNull(SelfExclusionRequestEntity::getEndDate)
                .le(SelfExclusionRequestEntity::getEndDate, now));

    int expiredCount = 0;
    for (SelfExclusionRequestEntity request : outdatedRequests) {
      request.setStatus("EXPIRED");
      selfExclusionRequestDao.updateById(request);
      expiredCount++;

      log.info(
          "Self-exclusion request {} for player {} auto-expired",
          request.getRequestId(),
          request.getPlayerId());
    }

    if (expiredCount > 0) {
      log.info("Expired {} self-exclusion requests", expiredCount);
    }

    return expiredCount;
  }
}
