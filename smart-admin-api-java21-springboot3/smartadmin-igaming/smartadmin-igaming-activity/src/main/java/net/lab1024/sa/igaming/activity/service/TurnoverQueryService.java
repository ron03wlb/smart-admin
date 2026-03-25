package net.lab1024.sa.igaming.activity.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.vavr.control.Option;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.igaming.activity.dao.PlayerBonusRecordDao;
import net.lab1024.sa.igaming.activity.domain.entity.PlayerBonusRecordEntity;
import org.springframework.stereotype.Service;

/**
 * Turnover query service - provides cumulative turnover calculations for risk assessment.
 *
 * <p>This service aggregates player wagering data from bonus records. In production, this should be
 * replaced with queries against a pre-aggregated summary table (e.g., t_player_stats or
 * t_wagering_progress) for better performance.
 *
 * @author iGaming Team
 * @since 2026-03-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnoverQueryService {

  private final PlayerBonusRecordDao playerBonusRecordDao;

  /**
   * Calculate player's cumulative turnover (lifetime).
   *
   * <p>Current implementation: Aggregates wagering_completed from t_player_bonus_record. This
   * includes all wagering progress across all bonus records (active, completed, expired).
   *
   * <p>Future enhancement: Query from a dedicated wagering summary table with pre-aggregated
   * activity_valid_turnover for better performance.
   *
   * <p>Data source: t_player_bonus_record.wagering_completed (NUMERIC(19,4))
   *
   * @param playerId player ID
   * @param tenantId tenant ID (for multi-tenant isolation)
   * @return cumulative turnover (all-time), or 0.00 if no records exist
   */
  public BigDecimal calculateCumulativeTurnover(Long playerId, Long tenantId) {
    if (playerId == null || tenantId == null) {
      log.warn("[TURNOVER_QUERY] Invalid parameters: playerId={}, tenantId={}", playerId, tenantId);
      return BigDecimal.ZERO;
    }

    List<PlayerBonusRecordEntity> records =
        playerBonusRecordDao.selectList(
            Wrappers.<PlayerBonusRecordEntity>lambdaQuery()
                .eq(PlayerBonusRecordEntity::getPlayerId, playerId)
                .eq(PlayerBonusRecordEntity::getTenantId, tenantId)
                .eq(PlayerBonusRecordEntity::getDeleted, false));

    if (records.isEmpty()) {
      log.info("[TURNOVER_QUERY] No bonus records found for playerId={}", playerId);
      return BigDecimal.ZERO;
    }

    BigDecimal totalTurnover =
        records.stream()
            .map(PlayerBonusRecordEntity::getWageringCompleted)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    log.info(
        "[TURNOVER_QUERY] Cumulative turnover calculated: playerId={}, tenantId={}, recordCount={},"
            + " totalTurnover={}",
        playerId,
        tenantId,
        records.size(),
        totalTurnover);

    return totalTurnover;
  }

  /**
   * Calculate player's wagering progress for a specific bonus record.
   *
   * @param recordId bonus record ID
   * @return Optional containing wagering progress (completed/required), or none if record not found
   */
  public Option<WageringProgress> getWageringProgress(Long recordId) {
    if (recordId == null) {
      return Option.none();
    }

    PlayerBonusRecordEntity record = playerBonusRecordDao.selectById(recordId);
    if (record == null || record.getDeleted()) {
      return Option.none();
    }

    WageringProgress progress =
        new WageringProgress(
            record.getRecordId(),
            record.getPlayerId(),
            record.getWageringRequired(),
            record.getWageringCompleted(),
            calculateProgressPercentage(
                record.getWageringCompleted(), record.getWageringRequired()));

    return Option.of(progress);
  }

  private BigDecimal calculateProgressPercentage(BigDecimal completed, BigDecimal required) {
    if (required.compareTo(BigDecimal.ZERO) == 0) {
      return BigDecimal.ZERO;
    }
    return completed
        .divide(required, 4, java.math.RoundingMode.HALF_UP)
        .multiply(new BigDecimal("100"));
  }

  /**
   * Wagering progress VO.
   *
   * @param recordId bonus record ID
   * @param playerId player ID
   * @param wageringRequired total wagering required
   * @param wageringCompleted wagering completed so far
   * @param progressPercentage progress percentage (0-100)
   */
  public record WageringProgress(
      Long recordId,
      Long playerId,
      BigDecimal wageringRequired,
      BigDecimal wageringCompleted,
      BigDecimal progressPercentage) {}
}
